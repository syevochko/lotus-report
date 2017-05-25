package com.fuib.lotus.agents.report;

import java.io.IOException;

import com.fuib.lotus.agents.LNWSClient_woHTTP;
import com.fuib.lotus.agents.report.params.ParamDocColSet;
import com.fuib.lotus.agents.report.builder.AbstructFileReportBuilder;
import com.fuib.lotus.agents.report.builder.AbstructReportBuilder;
import com.fuib.lotus.log.LNDbLog;
import com.fuib.lotus.utils.LNIterator;
import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesException;

/**
 * @date Dec 28, 2014
 * @author evochko 
 * @Description базовый класс агента для формирования csv-отчета по заданному набору документов.
 * В профиле агента указывается набор параметров. Описание общих параметров:
 * <br> {@link #PARAM_SELECTION_FORMULA} - поисковая @-формула
 * <br> {@link #PARAM_DBTRG_PATH} - база или список баз, в которых выполняется отбора документов по {@link #PARAM_SELECTION_FORMULA}
 * <br> {@link #PARAM_FILE_PREFIX} - префикс имени файла отчета <br>
 * <br> Для формирования колонок отчета используется объект класса {@link ParamDocColSet} {@link #paramCols}
 *  - колонки вычитываются из профиля агента из параметров, которые начинаются с COL<№>
 * <br> 
 * <br> Для формирования файла с отчетом используется имплементация абстрактного класса {@link com.fuib.lotus.agents.report.builder.AbstructFileReportBuilder} {@link #repBuilder}
 * <br> Таким образом для формирования отчета необходимо задать member repBuilder в объекте этого класса
 * <br>  
 * <br> {@link #main()} - точка входа в агент, выполняется загрузка параметров из профиля, инициализация лога, 
 * 	загрузка параметров отчета колонок, запуск основного процесса работы {@link #process()}
 * <br> {@link #process()} - собственно основная процедура обработки данных и формирования отчета. 
 * 	Выполняется получение целевых баз для поиска, поиск по каждой базе, отправка отчета(ов) через {@link AbstructFileReportBuilder#send(String, String, Database)} 
 */
public class LNAgentCSVFileBase extends LNWSClient_woHTTP {

	protected String PARAM_FILE_PREFIX = "FILE_NAME_PREFIX";
	protected String PARAM_SELECTION_FORMULA = "SELECTION_FORMULA";
	protected String PARAM_DBTRG_PATH = "DBTRG_PATH";
	protected String PARAM_MULT_VAL_SEP = ",";
	
	protected boolean bIsFileForSQLImport = false;

	protected final int ERR_REQUIRED_PARAM = 1900;
	protected final int ERR_NO_REP_BUILDER = 1910;
	protected final int ERR_NOT_NOTES = 1999;	

	private String sReportSubject = "";
	private String sReportAppendText = "";
	
	private Database dbTrg = null;	
	private ParamDocColSet paramCols = new ParamDocColSet();
	private AbstructFileReportBuilder repBuilder = null;
	private DateTime searchCutoff = null; 

	
	// getters and setters
	public String getReportSubject() {		return sReportSubject;		}

	public void setReportSubject(String reportSubject) {		sReportSubject = reportSubject;		}

	public String getReportAppendText() {	return sReportAppendText;	}

	public void setReportAppendText(String reportAppendText) {		sReportAppendText = reportAppendText;	}
	
	public DateTime getSearchCutoff() {	return searchCutoff;	}

	public void setSearchCutoff(DateTime searchCutoff) {	this.searchCutoff = searchCutoff;	}

	public ParamDocColSet getParamCols() {	return paramCols;	}

	public void setParamCols(ParamDocColSet paramCols) {	this.paramCols = paramCols;		}
	
	public AbstructReportBuilder getRepBuilder() {	return repBuilder;	}

	public void setRepBuilder(AbstructFileReportBuilder repBuilder) {	this.repBuilder = repBuilder;	}	
	
	/**
	 * Точка входа - выполнение основного кода класса.
	 * <br>  выполняется загрузка параметров из профиля, инициализация лога, 
	 *  загрузка параметров отчета колонок, запуск основного процесса работы {@link #process()}
	 * <br> также выполняется закрытие объекта {@link #repBuilder}
	 */	
	protected void main() throws Exception, Throwable {
		DocumentCollection dc = null;

		try {		
			loadConfiguration();
			loadColumnConfiguration();
			
			setCustomLog(LNDbLog.LOGTYPE_ENTRY, m_mapConfig.get(ITEM_LOGEXPIRED));
			
			process();
		}
		catch (NotesException ne)	{
			if (ne.id == ERR_REQUIRED_PARAM)
				showRequiredParams();
			throw ne;
		}
		finally {
			if (getRepBuilder() != null)
				getRepBuilder().close();
			
			Tools.recycleObj(searchCutoff);
			Tools.recycleObj(dc);
			Tools.recycleObj(dbTrg);
		}
	}


	/**
	 *	основная процедура обработки данных и формирования отчета. 
	 * <br>Выполняется получение целевых баз для поиска, поиск по каждой базе, отправка отчета(ов) через {@link AbstructFileReportBuilder#send(String, String, Database)} 
	 * @throws Exception 
	 */
	protected void process() throws Exception	{
		
		if (getRepBuilder()==null)	{
			throw new NotesException(ERR_NO_REP_BUILDER, "report builder object not defined!");
		}
		
		getRepBuilder().setAgentBase(this);
		
		if (getRepBuilder().getRepTitle().length()==0)	{
			getRepBuilder().setRepTitle( getParamCols().getTitle() );			
		}				
		
		String[] dbPaths = getTargetDbPaths();
		for(int i=0; i<dbPaths.length; i++)	{			
			processDb(dbPaths[i]);
		}
		
		getRepBuilder().send(sReportSubject, sReportAppendText, m_dbCurrent);
	}
	
	/**
	 * Получение списка путей к базам, в которых надо выполнять поиск
	 */
	protected String[] getTargetDbPaths()	{
		String sDbPaths = (String) m_mapConfig.get(PARAM_DBTRG_PATH);
		String[] dbPaths = sDbPaths.split(PARAM_MULT_VAL_SEP);
		return dbPaths;
	}

	/**
	 выполнение поиска документов для формирования отчета по одной базе 
	 * @throws Exception 
	 */
	protected void processDb(String sDbName) throws Exception	{
		dbTrg = m_env.getDatabase(sDbName);			
		
		DocumentCollection dc = getSelectionCollection(dbTrg, getSearchCutoff());
		processDocCollection(dc);
	}

	/**
	 обработка коллекции документов, полученной в результате поиска
	 */
	protected void processDocCollection(DocumentCollection dc) throws NotesException	{
		for (LNIterator dcIterator = new LNIterator(dc,true); dcIterator.hasNext(); )		{
			Document doc = (Document)dcIterator.next();
			processDoc(doc);
		}
	}

	/**
	 добавление информации по найденному документу в отчет с использованием {@link AbstructFileReportBuilder#addLineToReport(String, Document)} 
	 */
	protected void processDoc(Document doc) throws NotesException	{
		String sRow = getParamCols().createRowByDoc(doc);
		try {
			getRepBuilder().addLineToReport(sRow, doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	protected DocumentCollection getSelectionCollection(Database dbTrg, DateTime cutoff) throws Exception	{
		String sSearchCriteria = (String) m_mapConfig.get(PARAM_SELECTION_FORMULA);
		DocumentCollection dc = dbTrg.search( sSearchCriteria, cutoff );
		logAction("Search in [" + dbTrg.getFilePath() + "], criteria is ["+sSearchCriteria+"], cutoff date: " + cutoff + " - found docs: " + dc.getCount() );
		return dc;
	}

	/**
	 * Загрузка параметров колонок 
	 */
	protected void loadColumnConfiguration() throws Exception {
		checkRequiredParams();
		getParamCols().setColumns(m_mapConfig, m_addParamDescr, bIsFileForSQLImport);
	}

	protected void checkRequiredParams() throws Exception	{
		if (!m_mapConfig.containsKey(PARAM_FILE_PREFIX))
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <"+PARAM_FILE_PREFIX+"> in agent profile!");

		if (!m_mapConfig.containsKey(PARAM_DBTRG_PATH))
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <"+PARAM_DBTRG_PATH+"> in agent profile!");

		if ( !m_mapConfig.containsKey(PARAM_SELECTION_FORMULA) )
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <"+PARAM_SELECTION_FORMULA+"> in agent profile!");	
	}

	/**
	 * отображение необходимых параметров и их расшифровка
	 */
	protected void showRequiredParams()	{		
		try {
			System.out.println("========== Агент <"+m_sAgName+"> - используется для формиривания csv-файла по данным в текущей базе - " + m_dbCurrent.getFilePath());
		} catch (NotesException e) { 	}

		System.out.println("Список параметров, необходимых для работы агента (параметры задаются в профиле агента в базе <Конфигурация АС: Lotus>, секция <Дополнительные параметры>)");
		System.out.println(PARAM_DBTRG_PATH + " - список баз, разделенных <;> на текущем сервере для отбора документов" );
		System.out.println(PARAM_SELECTION_FORMULA + " - @-формула отбора документов в заданных базах через db.search()" );
	}	
	
	
	@Deprecated
	protected void ws_call() throws Exception {
	}
	
}
