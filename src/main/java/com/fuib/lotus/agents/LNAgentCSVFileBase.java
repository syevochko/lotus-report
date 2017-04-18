package com.fuib.lotus.agents;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.agents.params.ParamDocColSet;
import com.fuib.lotus.agents.params.ParamDocColumn;
import com.fuib.lotus.agents.report.AbstructFileReportBuilder;
import com.fuib.lotus.agents.report.AbstructReportBuilder;
import com.fuib.lotus.log.LNDbLog;
import com.fuib.lotus.utils.LNIterator;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesException;

/**
 * @date Dec 28, 2014
 * @author evochko 
 * @Description ������� ����� ������ ��� ������������ csv-������ �� ��������� ������ ����������.
 * � ������� ������ ����������� ����� ����������. �������� ����� ����������:
 * <br> {@link #PARAM_SELECTION_FORMULA} - ��������� @-�������
 * <br> {@link #PARAM_DBTRG_PATH} - ���� ��� ������ ���, � ������� ����������� ������ ���������� �� {@link #PARAM_SELECTION_FORMULA}
 * <br> {@link #PARAM_FILE_PREFIX} - ������� ����� ����� ������ <br>
 * <br> ��� ������������ ������� ������ ������������ ������ ������ {@link com.fuib.lotus.agents.params.ParamDocColSet} {@link #paramCols} 
 *  - ������� ������������ �� ������� ������ �� ����������, ������� ���������� � COL<�>
 * <br> 
 * <br> ��� ������������ ����� � ������� ������������ ������������� ������������ ������ {@link com.fuib.lotus.agents.report.AbstructFileReportBuilder} {@link #repBuilder}
 * <br> ����� ������� ��� ������������ ������ ���������� ������ member repBuilder � ������� ����� ������
 * <br>  
 * <br> {@link #main()} - ����� ����� � �����, ����������� �������� ���������� �� �������, ������������� ����, 
 * 	�������� ���������� ������ �������, ������ ��������� �������� ������ {@link #process()}
 * <br> {@link #process()} - ���������� �������� ��������� ��������� ������ � ������������ ������. 
 * 	����������� ��������� ������� ��� ��� ������, ����� �� ������ ����, �������� ������(��) ����� {@link AbstructFileReportBuilder#send(String, String, Database)} 
 */
public class LNAgentCSVFileBase extends LNWSClient_woHTTP {

	protected String PARAM_FILE_PREFIX = "FILE_NAME_PREFIX";
	protected String PARAM_SELECTION_FORMULA = "SELECTION_FORMULA";
	protected String PARAM_DBTRG_PATH = "DBTRG_PATH";
	protected String PARAM_MULT_VAL_SEP = ",";
	
	protected boolean bIsFileForSQLImport = false;

	protected String ERR_NOTIFY_LIST = "APP.Developers";

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
	 * ����� ����� - ���������� ��������� ���� ������.
	 * <br>  ����������� �������� ���������� �� �������, ������������� ����, 
	 *  �������� ���������� ������ �������, ������ ��������� �������� ������ {@link #process()}
	 * <br> ����� ����������� �������� ������� {@link #repBuilder}
	 */	
	protected void main() throws Exception, Throwable {

		DocumentCollection dc = null;

		try {		

			m_env = new LNEnvironment(this.m_session);
			loadConfiguration();
			loadColumnConfiguration();
			initCustomLog();

			process();
			recycle();

		} catch (NotesException ne)	{
			logError(ne.id, ne.toString(), true);
			ne.printStackTrace();
			if (ne.id == ERR_REQUIRED_PARAM)
				showRequiredParams();

		} catch (Throwable ex) {
			logError(ERR_NOT_NOTES, ex.toString(), true);
			ex.printStackTrace();

		} finally	{

			if (getRepBuilder()!=null)
				getRepBuilder().close();
			
			if (dc!=null)
				dc.recycle();

			if (dbTrg!=null)
				dbTrg.recycle();

		}
	}


	/**
	 *	�������� ��������� ��������� ������ � ������������ ������. 
	 * <br>����������� ��������� ������� ��� ��� ������, ����� �� ������ ����, �������� ������(��) ����� {@link AbstructFileReportBuilder#send(String, String, Database)} 
	 */
	protected void process() throws NotesException	{
		
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
	 * ��������� ������ ����� � �����, � ������� ���� ��������� �����
	 */
	protected String[] getTargetDbPaths()	{
		String sDbPaths = (String) m_mapConfig.get(PARAM_DBTRG_PATH);
		String[] dbPaths = sDbPaths.split(PARAM_MULT_VAL_SEP);
		return dbPaths;
	}

	/**
	 ���������� ������ ���������� ��� ������������ ������ �� ����� ���� 
	 */
	protected void processDb(String sDbName) throws NotesException	{
		dbTrg = m_env.getDatabase(sDbName);			
		if (dbTrg==null)	throw new NotesException(LNEnvironment.ERR_CUSTOM, "Can't open database by path: " + sDbName);		
		
		DocumentCollection dc = getSelectionCollection(dbTrg, getSearchCutoff());
		processDocCollection(dc);
	}

	/**
	 ��������� ��������� ����������, ���������� � ���������� ������
	 */
	protected void processDocCollection(DocumentCollection dc) throws NotesException	{
		for (LNIterator dcIterator = new LNIterator(dc,true); dcIterator.hasNext(); )		{
			Document doc = (Document)dcIterator.next();
			processDoc(doc);
		}
	}

	/**
	 ���������� ���������� �� ���������� ��������� � ����� � �������������� {@link AbstructFileReportBuilder#addLineToReport(String, Document)} 
	 */
	protected void processDoc(Document doc) throws NotesException	{
		String sRow = getParamCols().createRowByDoc(doc);
		try {
			getRepBuilder().addLineToReport(sRow, doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public void logAction(String sText)		{
		try {
			super.logAction(sText);
		} catch (Exception e) 	{
			e.printStackTrace();
		}
	}
	
	protected DocumentCollection getSelectionCollection(Database dbTrg, DateTime cutoff) throws NotesException	{
		String sSearchCriteria = (String) m_mapConfig.get(PARAM_SELECTION_FORMULA);
		DocumentCollection dc = dbTrg.search( sSearchCriteria, cutoff );
		logAction("Search in [" + dbTrg.getFilePath() + "], criteria is ["+sSearchCriteria+"], cutoff date: " + cutoff + " - found docs: " + dc.getCount() );
		return dc;
	}

	/**
	 * �������� ���������� ������� 
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
	 * ����������� ����������� ���������� � �� �����������
	 */
	protected void showRequiredParams()	{		
		try {
			System.out.println("========== ����� <"+m_sAgName+"> - ������������ ��� ������������ csv-����� �� ������ � ������� ���� - " + m_dbCurrent.getFilePath());
		} catch (NotesException e) { 	}

		System.out.println("������ ����������, ����������� ��� ������ ������ (��������� �������� � ������� ������ � ���� <������������ ��: Lotus>, ������ <�������������� ���������>)");
		System.out.println(PARAM_DBTRG_PATH + " - ������ ���, ����������� <;> �� ������� ������� ��� ������ ����������" );
		System.out.println(PARAM_SELECTION_FORMULA + " - @-������� ������ ���������� � �������� ����� ����� db.search()" );
	}	

	/**
	 * ������������� ���������� �����������
	 * TODO - ���� �� ������� �� �������� ���������
	 */
	protected void initCustomLog() throws NotesException, Exception	{
		if ( m_sLogCategory != null ) {
			LNDbLog log = new LNDbLog(m_sLogCategory);

			log.open((m_sLogDb != null)?m_env.getDatabase(m_sLogDb):m_dbCurrent, LNDbLog.LOGTYPE_APPEND);
			log.setProperty(LNDbLog.PROP_EXPIRED, m_mapConfig.get(ITEM_LOGEXPIRED));

			setCustomLog(log);
		}

		setLogOption(true, (m_sLogCategory!=null && m_sLogDb != null), false, false);
	}

	protected void logError(int nErr, String sText, boolean bSendNotify) throws Exception {
		super.logError(nErr, sText);

		if (bSendNotify)	{
			Vector<String> vTo = new Vector<String>(Arrays.asList(ERR_NOTIFY_LIST.split(",")));
			String sServer = (String) m_session.evaluate("@Name([CN]; '"+m_dbCurrent.getServer()+"')").firstElement();
			String sSubject = "["+sServer+"] ��������! ��������� ������ ��� ������ ������ " + m_sAgName + " � ���� " + m_dbCurrent.getFilePath();
			String sText1 = "��������� ������:\r ���: " + nErr + "\r ��������: " + sText + "\r ����������� ��: notes://" + sServer + "/log.nsf" ;
			sendMail(vTo, "", sSubject, sText1, true, null, false);
		}
	}
	
	protected void recylce()	{
		super.recycle();
	}

	@Deprecated
	protected void ws_call() throws Exception {
	}

}
