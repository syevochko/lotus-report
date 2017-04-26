package com.fuib.lotus.agents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.log.LNDbLog;
import com.fuib.lotus.utils.LNIterator;
import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;

/**
 * Реализация выгрузки данных в csv-файл в формате, заданном в параметрах профиля агента в базе "Конфигурация АС: Lotus"
 * @author evochko * 
 */
public class LNAgentCSVFile extends LNWSClient_woHTTP {
	protected final String PARAM_FILE_PREFIX = "FILE_NAME_PREFIX";
	protected final String PARAM_SELECTION_FORMULA = "SELECTION_FORMULA";
	protected final String PARAM_COL_PREFIX = "COL";
	protected final String PARAM_COLS_HAS_SQLNULL = "COLS_HAS_SQLNULL";
	protected final String PARAM_DBTRG_PATH = "DBTRG_PATH";
	
	protected final int ERR_REQUIRED_PARAM = 1900;
	
	protected final int MAX_COL_COUNT = 50;
	protected final String COL_SEP = ";";
	protected final String VALUE_SEP = ",";
	protected final String FILE_LINE_SEP = "\r";
	protected final String SQL_NULL = "#NA";
	
	private Database dbTrg = null;
	
	private final static String FORMAT_FILE_NAME_DATE = "yyyy-MM-ddHHmmss";
	private static final SimpleDateFormat oFileNameFormatter = new SimpleDateFormat(FORMAT_FILE_NAME_DATE);
	private static final SimpleDateFormat oColsDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final DecimalFormat oColsDoubleFormatter;
	static	{
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator(".".charAt(0));
		oColsDoubleFormatter = new DecimalFormat("", dfs);
		oColsDoubleFormatter.setGroupingUsed(false);
	}
	
	private Vector<ColumValueWrapper> vColumns = new Vector<ColumValueWrapper>();
	
	/**
	 * <br> Класс-обертка для обращения к свойствам колонки-параметра.
	 * <br> Класс описывает свойства колонки: 
	 * <br> - признак формулы, 
	 * <br> - признак использования SQL_NULL, если значение колонки пустое
	 * <br> - значение колонки - название поля или формула
	 */
	protected class ColumValueWrapper	{
		private boolean bIsFormula = false;
		private boolean bHasSqlNullIfEmpty = false;
		private String sColValue = "";
		private String sColDescription = "";
		
		public ColumValueWrapper(boolean isFormula, boolean bHasSqlNull, String columnValue, String columnDescr) {			
			bIsFormula = isFormula;
			sColValue = columnValue;
			bHasSqlNullIfEmpty = bHasSqlNull;			
			setColDescription(columnDescr);
		}
		
		public boolean isFormula() { return bIsFormula; }
		public void setIsFormula(boolean isFormula) { bIsFormula = isFormula; }
		public boolean isHasSqlNullIfEmpty() { return bHasSqlNullIfEmpty; }
		public void setHasSqlNullIfEmpty(boolean hasSqlNullIfEmpty) { bHasSqlNullIfEmpty = hasSqlNullIfEmpty; }
		public String getColumnValue() { return sColValue; }
		public void setColumnValue(String columnValue) { sColValue = columnValue; }
		public void setColDescription(String sColDescription) { this.sColDescription = sColDescription; }
		public String getColDescription() { return sColDescription!=null?sColDescription:""; }
	}
	
	public LNAgentCSVFile() {
		setProfileForm("AgentConfig");		// установить имя формы профиля - используется как часть ключа для поиска настроечного документа
	}

	/**
	 * Точка входа - выполнение основного кода класса.
	 * <br> 1. Загрузка параметров из профиля агента в базе Конфигурация АС: Lotus 
	 * <br> 2. Получение коллекции документов по криетирию отбора (параметр PARAM_SELECTION_FORMULA)
	 * <br> 3. Обработка коллекции документов - получение csv-файла. Для каждого документа вычисляются колонки, заданные в профиле агента 
	 * <br> 4. Создание документа в заданной базе (параметр PARAM_DBTRG_PATH) и вложение созданного файла. После вложения файл удаляется
	 * @see #showRequiredParams
	 *  
	 */	
	protected void main() throws Exception, Throwable {
		DocumentCollection dc = null;

		try {		
			this.loadConfiguration();
			
			setCustomLog(LNDbLog.LOGTYPE_ENTRY, m_mapConfig.get(ITEM_LOGEXPIRED));
			
			dc = getSelectionCollection();
			logAction(m_sAgName + ": найдено " + dc.getCount() + " документов по критерию " + (String) m_mapConfig.get(PARAM_SELECTION_FORMULA) );
			
			if (dc.getCount() > 0) {
				File fileCsv = processCollection2Csv(dc);
				logAction(m_sAgName + ": сформирован временный файл: " + fileCsv.getAbsolutePath());
				createdDocWithCsvFile(fileCsv);
				logAction(m_sAgName + ": сформирован документ с файлом (" + fileCsv.getName() + ") в базе " + (String) m_mapConfig.get(PARAM_DBTRG_PATH) );
				fileCsv.delete();
			}
		}
		catch (NotesException ne) {
			if (ne.id == ERR_REQUIRED_PARAM)
				showRequiredParams();
			throw ne;
		}
		finally {
			Tools.recycleObj(dc);
			Tools.recycleObj(dbTrg);
		}
	}
	
	/**
	 * отображение необходимых параметров и их расшифровка
	 */
	protected void showRequiredParams()	{		
		try {
			System.out.println("========== Агент <"+m_sAgName+"> - используется для формиривания csv-файла по данным в текущей базе - " + m_dbCurrent.getFilePath());
		} catch (NotesException e) { 	}
		
		System.out.println("Список параметров, необходимых для работы агента (параметры задаются в профиле агента в базе <Конфигурация АС: Lotus>, секция <Дополнительные параметры>)");
		System.out.println(PARAM_FILE_PREFIX + " - префикс имени создавамого файла, файл будет создаваться с именем: " + PARAM_FILE_PREFIX + FORMAT_FILE_NAME_DATE + ".csv" );
		System.out.println(PARAM_DBTRG_PATH + " - путь к базе на текущем сервере для создания документа и вложения созданного файла" );
		System.out.println(PARAM_SELECTION_FORMULA + " - @-формула отбора документов в текущей базе через db.search()" );
		System.out.println(PARAM_COL_PREFIX + "1 - необходим хотя бы один параметр - колонка для экспорта данных, колонки начинаются с 1, т.е. должен быть определен хотя параметр " + PARAM_COL_PREFIX + "1" );
	}
	
	/**
	 * Загрузка параметров из профиля 
	 * @see com.fuib.lotus.agents.LNWSClient_woHTTP#loadConfiguration()
	 * 
	 */
	protected void loadConfiguration() throws Exception {
		super.loadConfiguration();
		
		if (!m_mapConfig.containsKey(PARAM_FILE_PREFIX))
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <" + PARAM_FILE_PREFIX + "> in agent profile!");
		
		if (!m_mapConfig.containsKey(PARAM_DBTRG_PATH))
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <" + PARAM_DBTRG_PATH + "> in agent profile!");
		
		dbTrg = m_env.getDatabase((String)m_mapConfig.get(PARAM_DBTRG_PATH));
		
		if (!m_mapConfig.containsKey(PARAM_SELECTION_FORMULA))
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <"+PARAM_SELECTION_FORMULA+"> in agent profile!");	
		
		if (!m_mapConfig.containsKey(PARAM_COL_PREFIX + "1"))
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected at least one parameter-column <"+PARAM_COL_PREFIX+"1> in agent profile!");
		
		String sColsHasSqlNull = "";
		if (m_mapConfig.containsKey(PARAM_COLS_HAS_SQLNULL)) {
			sColsHasSqlNull = (String)m_mapConfig.get(PARAM_COLS_HAS_SQLNULL);
			sColsHasSqlNull += "-";		// добавляем символ - не цифру, для того чтобы в дальнейшем не усложнять регулярное выражение для проверки вхождения колонки в список
		}		
		
		initColumnValue(sColsHasSqlNull); 

	}

	protected void initColumnValue(String sSeparatedColsHasSqlNull) throws Exception	{
		
		for (int i = 1; i <= MAX_COL_COUNT && m_mapConfig.containsKey(PARAM_COL_PREFIX+i); i++)	{
			String sColName = PARAM_COL_PREFIX+i;			
			String sColumnVal = (String) m_mapConfig.get(sColName);
			String sColDescr = "";
			if (!m_addParamDescr.isEmpty())
				sColDescr = (String)m_addParamDescr.get(sColName);
			
			boolean bHasSqlNullIfEmpty = sSeparatedColsHasSqlNull.matches(".*"+sColName+"[^0-9].*");			
			boolean bIsFormula = sColumnVal.startsWith("@", 0) && (sColumnVal.length()>1);
			if (bIsFormula) sColumnVal = sColumnVal.substring(1, sColumnVal.length());
			
			vColumns.add(new ColumValueWrapper(bIsFormula, bHasSqlNullIfEmpty, sColumnVal, sColDescr));
		}
		
	}


	/**
	 * получение коллекции документов для обработки по заданному критерию поиска
	 */
	protected DocumentCollection getSelectionCollection() throws NotesException	{
		DocumentCollection dc = m_dbCurrent.search(((String) m_mapConfig.get(PARAM_SELECTION_FORMULA)));
		return dc;
	}

	protected DocumentCollection getSelectionCollection(Database dbTrg, DateTime cutoff) throws NotesException {
		DocumentCollection dc = dbTrg.search(((String) m_mapConfig.get(PARAM_SELECTION_FORMULA)), cutoff);
		return dc;
	}

	/**
	 * формирование файла по выбранной коллекции документов
	 * @return
	 * @throws IOException 
	 * @throws NotesException 
	 */
	protected File processCollection2Csv(DocumentCollection dc) throws IOException, NotesException {

		try {
			if (dc.getCount() == 0)
				return null;
		} catch (NotesException e) {
			e.printStackTrace();
			return null;
		}

		File res = null;
		res = createCsvFileName();
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(res)));
		
		try {
			writer = new FileWriter(res);
			for (LNIterator dcIterator = new LNIterator(dc, true); dcIterator.hasNext(); ) {
				Document doc = (Document)dcIterator.next();
				if (doc.isValid() && !doc.isDeleted()) {
					String str4csv = createCsvStrFromDoc(doc);
					writer.write(str4csv + FILE_LINE_SEP);
				}
			}
		}
		finally {			
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
		
		return res;
	}

	/**
	 * создание форматированной строки для записи в csv файл
	 * <br> к документу применяются все настройки колонок в порядке COL1, COL2 и т.д.
	 * <br> колонки разделяются символами COL_SEP
	 * @throws NotesException 
	 * @see #processDocColumn()
	 * 
	 */
	protected String createCsvStrFromDoc(Document doc) throws NotesException {
		StringBuffer strBuff = new StringBuffer();

		for(int i=0; i < vColumns.size(); i++) {
			String sColValue = processDocColumn(doc, vColumns.get(i));
			strBuff.append(sColValue).append(COL_SEP);
		}
		strBuff.setLength(strBuff.length()-COL_SEP.length());
		return strBuff.toString();
	}
	
	protected Vector<ColumValueWrapper> getReportColumns() {
		return vColumns;
	}

	/**
	 * Получения значения из документа по заданной колонке 
	 * @param doc - обрабатываемый документ
	 * @param col - настройка колонки
	 * @return строка, содержащая значение колонки в документе. Мульти-значения преобразуются в одну строку с разделителем VALUE_SEP
	 * @throws NotesException 
	 */
	@SuppressWarnings("unchecked")
	protected String processDocColumn(Document doc, ColumValueWrapper col) throws NotesException {
		String sResult = "";
		Vector<Object> v = null;
		
		try	{
			StringBuffer sb = new StringBuffer();
			
			if (col.isFormula())
				v = m_session.evaluate(col.getColumnValue(), doc);
			else if ( doc.hasItem(col.getColumnValue()) )
				v = doc.getItemValue(col.getColumnValue());				
			
			if (v != null && !v.isEmpty()) {
				for(int i=0; i<v.size(); i++)	{
					if (v.get(i) instanceof String)
						sb.append( (String)v.get(i) );
					else if (v.get(i) instanceof Double)
						sb.append( oColsDoubleFormatter.format( ((Double)v.get(i)).doubleValue() ) );
					else if (v.get(i) instanceof DateTime)
						sb.append( oColsDateFormatter.format( ((DateTime)v.get(i)).toJavaDate() ) );
					
					sb.append(VALUE_SEP);
				}

				sb.setLength(sb.length()-VALUE_SEP.length());
				sResult = sb.toString();
			}
			
			if (sResult.length()==0 && col.isHasSqlNullIfEmpty())
				sResult = SQL_NULL;
		}
		catch (NotesException e) {
			throw new NotesException(e.id, e.toString() + " (process unid: "+doc.getUniversalID()+")");
		}
		finally {}		
		
		return normalizeString(sResult);
	}
	
	/**
	 * замена недопустимых символов в строке - нормализация строки   
	 */
	protected String normalizeString(String sInput) {
		return sInput.replaceAll("[" + COL_SEP + "\\r\\n]", " ");
	}
	
	/**
	 * получение полного имени csv-файла  
	 */
	protected File createCsvFileName() {
		String sFileName = m_mapConfig.get(PARAM_FILE_PREFIX) + oFileNameFormatter.format(new Date()) + ".csv";
		File fileCsv = new File(System.getProperty("java.io.tmpdir"), sFileName);
		return fileCsv;
	}
	
	/**
	 * Создание документа в базе (dbTrg, параметр PARAM_DBTRG_PATH в профиле), 
	 * <br> добавление сформированного файла в документ
	 */
	protected void createdDocWithCsvFile(File fileCsv) throws NotesException {
		Document docFile = null;
		RichTextItem rtItem = null; 
		try	{
			docFile = dbTrg.createDocument();
			rtItem = docFile.createRichTextItem("Body");			
			docFile.replaceItemValue("Form", "Memo").recycle();
			docFile.replaceItemValue("From", dbTrg.getServer()).recycle();		
			docFile.replaceItemValue("Subject", m_sAgName + " - created file: " + fileCsv.getName()).recycle();
			docFile.replaceItemValue("$$DocType", "CSV_FILE").recycle();
			docFile.replaceItemValue("Status", Integer.valueOf(1)).recycle();
			
			rtItem.embedObject(EmbeddedObject.EMBED_ATTACHMENT, "", fileCsv.getAbsolutePath(), "");
			docFile.save(true, true);
		}
		catch(NotesException ne)	{
			throw new NotesException(LNEnvironment.ERR_CUSTOM, "Error while create doc in " + dbTrg.getFilePath() + " to attach file.");
		}
		finally	{
			Tools.recycleObj(rtItem);
			Tools.recycleObj(docFile);
		}
	}
	
	protected final void ws_call() throws Exception {
		// @deprecated nothing todo - agents not use web-service clients
	}

}
