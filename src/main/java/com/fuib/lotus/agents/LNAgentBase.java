package com.fuib.lotus.agents;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import lotus.domino.Agent;
import lotus.domino.AgentBase;
import lotus.domino.AgentContext;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.Log;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.RichTextItem;
import lotus.domino.Session;
import lotus.domino.View;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.log.LNDbLog;
import com.fuib.lotus.log.LNLog;

public abstract class LNAgentBase extends AgentBase {
	// constants
	public static final int ERR_USER = 1;
	public static final int ERR_UKNOWN = 2;
	
	// parameter field names
	public static final String ITEM_PROFILENAME = "fdName";
	public static final String ITEM_ENDPOINT = "fdWSEP";
	public static final String ITEM_ISAUTH = "fdIsAuth";
	public static final String ITEM_AUTHTYPE = "fdAuthType";
	public static final String ITEM_LOGIN = "UserID";
	public static final String ITEM_PASSWORD = "Password";
	public static final String ITEM_LOGEXPIRED = "fdnLogExpired";
	
	// use WSClients
	public static final String ITEM_WSCUSEFLAG = "fdIsWSClientUse";
	public static final String ITEM_WSCLIST = "fdWSClients";
	
	public static final String ITEM_PARAM = "fdParam";
	public static final String ITEM_PARAMVAL = "fdParamVal";
	public static final String ITEM_PARAMVAL_DELIM = "fdParamDelim";
	public static final String ITEM_PARAM_DESCR = "fdParamDescr";	
	
	public static final String LIST_DUMMYELEMENT = " ";
	
	
	//public static final String AUTH_SESSION = "sso";
	public static final String AUTH_BASIC = "basic";
	
	protected final String CREDENTIALS_VIEWNAME = "credentials";
	protected final String ITEM_KEY_CREDENTIAL = "fdCredential";
	
	// LN variables
	public Session m_session = null;
	public AgentContext m_agentContext = null;
	public Agent m_agent = null;
	public Database m_dbCurrent = null;
	public Document m_docContext = null;
	protected LNEnvironment m_env = null;
	
	private Log m_agentLog = null;
	
	public LNLog m_log = null;
	protected String m_sLogCategory = null;
	protected String m_sLogDb = null;
	
	// other variables
	public PrintWriter m_pw = null;
	public String m_sAgName = null;
	
	// log option
	protected boolean m_bIsSystemLog = true;
	protected boolean m_bIsCustomLog = false;
	protected boolean m_bIsAgentLog = false;
	protected boolean m_bIsContextLog = false;
	protected String m_sLogItemName;
	private boolean m_bIsDebug = false; 
	protected boolean m_bIsSendAlert = false;
	protected boolean m_bIsPWLog = false;
	protected Vector m_vAlertRecipient = null;
	
	private TimerInfo m_oTimerGlobal = null;
	
	public void NotesMain() {
		try {
			m_session = getSession();
			m_agentContext = m_session.getAgentContext();
			m_agent = m_agentContext.getCurrentAgent();
			m_dbCurrent = m_agentContext.getCurrentDatabase();			
			m_sAgName = m_agent.getName();
			m_oTimerGlobal = new TimerInfo(m_sAgName + " work time: ");
			m_pw = getAgentOutput();			

			main();												// <-- entry point to agent
			
		} catch(NotesException ne) {							// catch NOTESEXCEPTION
			logFatalError(ne, ne.id, ne.text);
		} catch(java.sql.SQLException a_sqle) {					// catch SQLEXCEPTION
			SQLException sqle = a_sqle;
			
			sqle.printStackTrace();			
			while ( sqle != null ) {
				logFatalError(sqle, sqle.getErrorCode(), sqle.getMessage());
				sqle = sqle.getNextException();    
			}
		} catch(java.lang.Exception e) {						// catch EXCEPTION
			logFatalError(e, ERR_UKNOWN, e.getMessage());
		} catch(java.lang.Throwable te) {						// catch THROWABLE
			logFatalError(te, ERR_UKNOWN, te.getMessage());
		} finally {												// FINAL CLEANUP			
			try { 
				if ( m_log != null )		m_log.close();
				if ( m_agentLog != null )	m_agentLog.close(); 	
			} catch (Exception e) { 
				e.printStackTrace(); 
			}

			// closing other domino objects if exist
			//recycle();        -- unrequared ...
			logMemoryUsage();
		}
	}
	
	protected void logMemoryUsage()		{
		System.out.println(m_sAgName + " current size: " + Runtime.getRuntime().totalMemory());
		System.out.println(m_sAgName + " max size: " + Runtime.getRuntime().maxMemory());
		System.out.println(m_sAgName + " free size: " + Runtime.getRuntime().freeMemory());
		System.out.println(m_oTimerGlobal.toString());
	}
		
	protected void setCustomLog(LNLog a_log) throws Exception {
		m_log = a_log;
		
		if ( !m_log.isLogOpened() && m_log.getClass().getName().toUpperCase().indexOf("LNDBLOG") != -1 )
			((LNDbLog)m_log).open(m_dbCurrent, LNLog.LOGTYPE_SINGLE);			
	}
	
	
	public void setLogOption(boolean bIsSystem, boolean bIsCustom, boolean bIsPW, boolean bIsAgent) {
		m_bIsSystemLog = bIsSystem;
		m_bIsPWLog = bIsPW;
		m_bIsCustomLog = bIsCustom;		
		m_bIsAgentLog  = bIsAgent;
	}
	
	
	public void setLogOptionContext(boolean bIsLog, String sItemName) {
		m_bIsContextLog = bIsLog;
		m_sLogItemName = sItemName;
	}
	
	
	public void log2AgentLog(String sText) throws NotesException {
		if ( m_agentLog == null ) {
			m_agentLog = m_session.createLog(m_sAgName);
			m_agentLog.openAgentLog();
		}
		
		m_agentLog.logAction(sText);
	}
	
	
	public void log2AgentLog(int nErr, String sText) throws NotesException {
		if ( m_agentLog == null ) {
			m_agentLog = m_session.createLog(m_sAgName);
			m_agentLog.openAgentLog();
		}
		
		m_agentLog.logError(nErr, sText);
	}
	
	
	public void logAction(String sText) throws Exception {
		if ( m_bIsSystemLog )					System.out.println(m_sAgName + " >> " + sText);
		if ( m_bIsPWLog && m_pw != null )		m_pw.println(sText);
		if ( m_bIsCustomLog && m_log != null )	m_log.log(sText);
		if ( m_bIsAgentLog )					log2AgentLog(sText);
	}
	
	
	public void logError(int nErr, String sText) throws Exception {
		if ( m_bIsSystemLog )					System.out.println(m_sAgName + " >> " + sText);
		if ( m_bIsPWLog && m_pw != null )		m_pw.println(sText);
		if ( m_bIsAgentLog )					log2AgentLog(nErr, sText);
		if ( m_bIsCustomLog && m_log != null ) {	
			m_log.logError(nErr, sText);			// at this point log will auto-close!
			m_log = null;
		}
	}
	
	
	public void log2Context(String sText) throws Exception {
		if ( m_bIsContextLog ) {
			if ( m_docContext == null )	m_docContext = m_agentContext.getDocumentContext();

			if ( !m_docContext.hasItem(m_sLogItemName) )
				m_docContext.replaceItemValue(m_sLogItemName, sText).recycle();
			else {
				Item it_log = m_docContext.getFirstItem(m_sLogItemName);
				it_log.appendToTextList(sText);
				it_log.recycle();			
			}
		}
	}
	
	private void logFatalError(Throwable te, int nErr, String sErrDescription) {
		String m_sErrClassName = te.getClass().getName();
		m_sErrClassName = m_sErrClassName.substring(m_sErrClassName.lastIndexOf(".") + 1);
		
		String sErrMsg = "Агент '" + m_sAgName + "' >> " + m_sErrClassName + ": код-" + nErr + ", Описание: " + sErrDescription;
		
		try {
			System.err.println(sErrMsg);								// print to standart error stream
			te.printStackTrace();			
			m_pw.println(sErrMsg);										// print to printwriter stream if exist
			te.printStackTrace(m_pw);	
			log2AgentLog(nErr, m_sErrClassName + " - " + sErrDescription);	// print to agent log
			log2Context(m_sErrClassName + ": код-" + nErr + ", Описание: " + sErrDescription);			
			if ( m_log != null ) {										// print to custom log if exist
				te.printStackTrace(new PrintWriter(m_log));				// print to custom log error stack
				m_log.flush();
				m_log.logError(nErr, m_sErrClassName + " - " + sErrDescription);
			}
			
			if ( isAlert() && m_vAlertRecipient != null && !m_vAlertRecipient.isEmpty() ){		// send alert notification to admins
				
				String sErr="База: "+m_session.getServerName()+"!!"+m_dbCurrent.getFilePath()+" \n\n"+ "Программный модуль: "+m_sAgName +"\n"+"Ошибка: "+m_sErrClassName + ": код-" + nErr + ", Описание: " + sErrDescription+"\n\n\n Подробности см. ДО:Логи и Log.nsf соответствующего сервера";
				sendMail(m_vAlertRecipient, null,  "["+m_session.createName(m_session.getServerName()).getAbbreviated()+"]  Критическая ошибка - агент завершил работу по исключению",
						sErr, true, null, false);
				}

		} catch (java.lang.Throwable e) { 
			e.printStackTrace();
		}
	}
	
	
	public void setDebugMode(boolean bMode)				{ setDebug(bMode); m_bIsDebug = bMode; }
	public boolean isDebugMode() 						{ return m_bIsDebug; }
	public void setAlert(boolean bAlert)				{ m_bIsSendAlert = bAlert; }
	public boolean isAlert()							{ return m_bIsSendAlert; }
	
	public void setAlertRecipient(Object oRecipient)  {
		if ( oRecipient != null ) {
			if ( oRecipient instanceof Vector )
				m_vAlertRecipient = (Vector) oRecipient; 
			else {
				if ( m_vAlertRecipient == null )	
					m_vAlertRecipient = new Vector();
				else 
					m_vAlertRecipient.clear();

				m_vAlertRecipient.add(oRecipient.toString());
			}

			setAlert(true);
		}
	}
	
	
	public void logDebug(String sText) throws Exception {	
		if ( isDebugMode() ) {
			boolean bMode = m_bIsAgentLog;
			
			m_bIsAgentLog = false;
			logAction(sText);
			m_bIsAgentLog = bMode;
		}
	}
	
	public void logDebug(Map map) throws Exception {
		Object vKey;
		Object vValue;
		
		if ( map != null )
			for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
				vKey = it.next();
				vValue = map.get(vKey);
				logDebug(vKey.toString() + " = " + ((vValue != null)?vValue.toString():"null"));
			}
	}
	
	
	protected void initFromDocument(Document docProfile, Map vParam) throws NotesException {
		if ( docProfile.hasItem("fdIsDebug") )
			setDebugMode(docProfile.getItemValueString("fdIsDebug").equals("1"));
		
		if ( docProfile.hasItem("fdIsLog") && docProfile.getItemValueString("fdIsLog").equals("1") ) {
			m_sLogCategory = docProfile.getItemValueString("fdLogCategory");
			if ( m_sLogCategory.length() == 0 )	m_sLogCategory = null;
			
			m_sLogDb = docProfile.getItemValueString("fdLogDb");
			if ( m_sLogDb.length() == 0 )	m_sLogDb = null;
		} else if ( docProfile.hasItem("fdLogCategory") ) {
			m_sLogCategory = docProfile.getItemValueString("fdLogCategory");			
			if ( m_sLogCategory.length() == 0 )	m_sLogCategory = null;
		}
		
		if ( vParam != null )
			for (Iterator it = vParam.keySet().iterator(); it.hasNext(); ) {
				String sItemName = (String)it.next();
				Vector vValue;
				if ( docProfile.hasItem(sItemName) ) {
					vValue = docProfile.getItemValue(sItemName);
					if ( !vValue.isEmpty() )
						vParam.put(sItemName, (vValue.size() > 1)?vValue:vValue.firstElement());					
				}
			}		
	}

	
	protected void initFromProfile(String sProfileName, String sKey, Map vParam) throws NotesException {	
		Document docProfile = m_dbCurrent.getProfileDocument(
								(sProfileName != null)?sProfileName:m_sAgName, 
								(sKey != null)?sKey:"AdminUser");
		
		initFromDocument(docProfile, vParam);
	}
	
	
	public void sendMail(Object oTo, String sFrom, String sSubject, String sText, boolean isImportant, Document docInclude, boolean bIsLink) throws NotesException {
		Document docSend = null;
		
		if ( oTo == null )	return;
		
		try {
			docSend = m_dbCurrent.createDocument();
			
			docSend.replaceItemValue("Form", "Memo");
			docSend.replaceItemValue("Subject", sSubject);
			
			if ( sFrom != null && sFrom.length() > 0 )		
				docSend.replaceItemValue("Principal", sFrom);
			else {
				int nIndex = m_sAgName.indexOf('|');
				docSend.replaceItemValue("Principal", ( nIndex != -1 )?m_sAgName.substring(0, nIndex):m_sAgName);
			}
			
			if ( isImportant )				
				docSend.replaceItemValue("Importance", "1");
			
			RichTextItem rtBody = docSend.createRichTextItem("Body");
			
			if ( sText != null && sText.length() > 0 )
				rtBody.appendText(sText);
			
			if ( docInclude != null )
				if ( bIsLink )
					rtBody.appendDocLink(docInclude);
				else
					docInclude.renderToRTItem(rtBody);
			
			// send memo
			if ( oTo instanceof Vector )
				docSend.send((Vector) oTo);
			else 
				docSend.send(oTo.toString());
		} finally {
			recycleObj(docSend);	
		}
	}

	
	
	protected void recycle() {
		recycleObj(m_agentLog);
		recycleObj(m_agent);
		recycleObj(m_dbCurrent);
		recycleObj(m_agentContext);
		recycleObj(m_docContext);
	}
	
	
	// ------------ ABSTRACT methods
	protected abstract void main() throws NotesException, Exception, Throwable;
	
	
	// ------------ STATIC methods
	public static void recycleObj(lotus.domino.Base obj) {
		if ( obj != null ) { 
			try {
				obj.recycle();
			} catch (NotesException e) {
				e.printStackTrace();
			}
			
			obj = null;
		}
	}
	
	public static void recycleObj(Vector vObj) {
		if ( vObj != null && !vObj.isEmpty() ) { 
			try {
				((lotus.domino.Base)vObj.firstElement()).recycle(vObj);
				vObj.clear();
			} catch (NotesException e) {
				e.printStackTrace();
			}
			
			vObj = null;
		}
	}

	/**
	 * Базовый метод загрузки конфигурации агента из указанного документа настроек
	 * 		Также выполняет загрузку параметров всех используемых WSClient'ов
	 * @param Document pdocConfig Документ настроек, из которого осуществляется загрузка параметров
	 * @param HaspMap pmapCfg HashMap, в который осуществляется загрузка параметров
	 * @param String psProfileName Имя профайла (для ошибок)
	 * @throws Exception
	 * TODO::CREATE Выполнить рефакторинг всех наследников:
	 * 		переориентировать на использование loadConfigFromDoc;
	 * 		удалить loadConfiguration
	 */
	protected void loadConfigFromDoc(Document pdocConfig, HashMap pmapCfg) throws Exception {		
		if (this.m_env == null) this.m_env = new LNEnvironment(this.m_session);
		
		// read from profile document configuration data				
		pmapCfg.put(ITEM_ENDPOINT, null);
		pmapCfg.put(ITEM_ISAUTH, null);
		pmapCfg.put(ITEM_AUTHTYPE, null);
		pmapCfg.put(ITEM_LOGEXPIRED, null);		
		pmapCfg.put(ITEM_KEY_CREDENTIAL, null);
		pmapCfg.put(ITEM_PARAM, null);
		pmapCfg.put(ITEM_PARAMVAL, null);
		pmapCfg.put(ITEM_PARAMVAL_DELIM, null);
		pmapCfg.put(ITEM_WSCUSEFLAG, null);
		pmapCfg.put(ITEM_WSCLIST, null);
		this.initFromDocument(pdocConfig, pmapCfg);
		
		// try to get parameter association for ITEM_PARAM (keys) and ITEM_PARAMVAL (values)
		if (pmapCfg.get(ITEM_PARAM) != null && pmapCfg.get(ITEM_PARAMVAL) != null ) {			
			if (pmapCfg.get(ITEM_PARAM) instanceof Vector && pmapCfg.get(ITEM_PARAMVAL) instanceof Vector) {
				Vector keys = (Vector)pmapCfg.get(ITEM_PARAM);
				Vector vals = (Vector)pmapCfg.get(ITEM_PARAMVAL);
				Vector delims = (Vector)pmapCfg.get(ITEM_PARAMVAL_DELIM);
				
				if (keys.size() == vals.size())	{
					for (int i=0; i < keys.size(); i++)	{
						String sDelim = delims.get(i).toString();
						
						pmapCfg.put(keys.get(i),
								( sDelim.isEmpty() || sDelim.equals(LIST_DUMMYELEMENT) ) ? 
								vals.get(i) : 																// значение параметра не содержит список
								m_session.evaluate("@Explode('" + vals.get(i) + "'; '" + sDelim + "')") 	// значение параметра содержит список
								);					
					}
				}
			} else if (pmapCfg.get(ITEM_PARAM) instanceof String && pmapCfg.get(ITEM_PARAMVAL) instanceof String)	{							 
				String sDelim = (String)pmapCfg.get(ITEM_PARAMVAL_DELIM);
				
				pmapCfg.put(pmapCfg.get(ITEM_PARAM),
						( sDelim.isEmpty() || sDelim.equals(LIST_DUMMYELEMENT) ) ? 
						pmapCfg.get(ITEM_PARAMVAL) : 															// значение параметра не содержит список
						m_session.evaluate("@Explode('" + pmapCfg.get(ITEM_PARAMVAL) + "'; '" + sDelim + "')") 	// значение параметра содержит список
						);
			}
		}		
		
		// Считывание параметров аутентификации
		if ( pmapCfg.get(ITEM_ISAUTH) != null && !((String)pmapCfg.get(ITEM_ISAUTH)).equals("") && 
				((String)pmapCfg.get(ITEM_AUTHTYPE)).equals(AUTH_BASIC) &&
				pmapCfg.get(ITEM_KEY_CREDENTIAL) != null && !((String)pmapCfg.get(ITEM_KEY_CREDENTIAL)).equals("")) {
			pmapCfg.put(ITEM_LOGIN, null);
			pmapCfg.put(ITEM_PASSWORD, null);
			
			View viewCredentials = m_env.getDbView(m_env.getFUIBConfigDB(), CREDENTIALS_VIEWNAME);
			if ( viewCredentials == null )
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found view " + CREDENTIALS_VIEWNAME + " in database " + 
						m_env.getFUIBConfigDB().getServer() + "!!" + m_env.getFUIBConfigDB().getFilePath());

			Document docCredential = viewCredentials.getDocumentByKey( (String)pmapCfg.get(ITEM_KEY_CREDENTIAL), true);
			if ( docCredential == null )
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found credentials document for web-service: " + pdocConfig.getItemValueString(ITEM_PROFILENAME));
			
			pmapCfg.put(ITEM_LOGIN, docCredential.getItemValueString(ITEM_LOGIN));
			pmapCfg.put(ITEM_PASSWORD, docCredential.getItemValueString(ITEM_PASSWORD));
		// Считывание настроек используемых web-сервисов
		} else if (pmapCfg.get(ITEM_WSCUSEFLAG) != null && !((String)pmapCfg.get(ITEM_WSCUSEFLAG)).equals("") &&
				pmapCfg.get(ITEM_WSCLIST) != null) {
			View viewConfig = m_env.getDbView(m_env.getFUIBConfigDB(), LNEnvironment.FUIBCONFIG_LOOKUPVIEWNAME);
			if ( viewConfig == null )
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found view " + LNEnvironment.FUIBCONFIG_LOOKUPVIEWNAME + " in database " + 
						m_env.getFUIBConfigDB().getServer() + "!!" + m_env.getFUIBConfigDB().getFilePath());
			
			// Проход по всем используемым web-сервисам
			Vector keys = new Vector();
			if (pmapCfg.get(ITEM_WSCLIST).getClass().getName().equals("java.lang.String")) {
				keys.add((String)pmapCfg.get(ITEM_WSCLIST));
			} else if (pmapCfg.get(ITEM_WSCLIST).getClass().getName().equals("java.util.Vector")) {
				keys = (Vector)pmapCfg.get(ITEM_WSCLIST);
			}
			
			// Буферизируем флаг дебага
			boolean bIsDebugBuf = isDebugMode();
			for (int i=0; i < keys.size(); i++)	{
				String sWSCID = (String)keys.get(i);
				Document docWSClient = viewConfig.getDocumentByKey( "wsclient#" + sWSCID, true);
				if ( docWSClient == null )
					throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found web-service settings document: " + sWSCID);
				
				HashMap tmpMap = new HashMap();
				loadConfigFromDoc(docWSClient, tmpMap);
				
				Object keysExt[] = tmpMap.keySet().toArray();
				for (int j=0; j < keysExt.length; j++)	{
					pmapCfg.put(sWSCID + "##" + (String)keysExt[j], tmpMap.get((String)keysExt[j]));
				}
			}
			setDebugMode(bIsDebugBuf);
		}
		
		logDebug("Configuration parameters are: ");
		// change value of password to its lengths 
		HashMap tmpMap = new HashMap();
		Object keysExt[] = pmapCfg.keySet().toArray();
		for (int i=0; i < keysExt.length; i++)	{
			if (((String)keysExt[i]).contains(ITEM_PASSWORD))
				tmpMap.put((String)keysExt[i], String.valueOf(((String)pmapCfg.get((String)keysExt[i])).length()));
			else
				tmpMap.put((String)keysExt[i], pmapCfg.get((String)keysExt[i]));
		}
		logDebug(tmpMap);
	}
	
	/**
	 * Получение значения параметра (включая параметры используемых WSClient'ов)
	 * @param String psName Имя запрашиваемого параметра
	 * @param String psPrefix Префикс (имя WSClient'а, чья настройка запрашивается)
	 * @return Содержимое параметра
	 */
	protected Object getParam(HashMap pmap, String psName, String psPrefix) {
		if (!psPrefix.equals(""))
			return pmap.get(psPrefix.toLowerCase() + "##" + psName);
		else
			return pmap.get(psName);
	}
}