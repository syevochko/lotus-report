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
import lotus.domino.NotesError;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;
import lotus.domino.Session;
import lotus.domino.View;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.log.InternalException;
import com.fuib.lotus.log.LNDbLog;
import com.fuib.lotus.log.LNDbLogEntry;
import com.fuib.lotus.log.LNDbLogSingle;
import com.fuib.lotus.log.LNLog;
import com.fuib.lotus.log.LogEx;
import com.fuib.lotus.utils.Tools;

public abstract class LNAgentBase extends AgentBase {
	// constants
	public static final int ERR_USER = LogEx.ERRc1222;
	public static final int ERR_UKNOWN = LogEx.ERRc1111;
	
	// parameter field names
	public static final String ITEM_PROFILENAME = "fdName";
	public static final String ITEM_ENDPOINT = "fdWSEP";
	public static final String ITEM_ISAUTH = "fdIsAuth";
	public static final String ITEM_AUTHTYPE = "fdAuthType";
	public static final String ITEM_LOGIN = "UserID";
	public static final String ITEM_PASSWORD = "Password";
	public static final String ITEM_LOGEXPIRED = "fdnLogExpired";
	
	private static final String LOG_CATEGORY_DEFAULT = "Agents";
	
	// use WSClients
	public static final String ITEM_WSCUSEFLAG = "fdIsWSClientUse";
	public static final String ITEM_WSCLIST = "fdWSClients";
	
	public static final String ITEM_PARAM = "fdParam";
	public static final String ITEM_PARAMVAL = "fdParamVal";
	public static final String ITEM_PARAMVAL_DELIM = "fdParamDelim";
	public static final String ITEM_PARAM_DESCR = "fdParamDescr";
	
	public static final String LIST_DUMMYELEMENT = " ";
	
//	public static final String AUTH_SESSION = "sso";
	public static final String AUTH_BASIC   = "basic";
	protected final String CREDENTIALS_VIEWNAME = "credentials";
	protected final String ITEM_KEY_CREDENTIAL = "fdCredential";
	
	// LN variables
	public Session m_session = null;
	public AgentContext m_agentContext = null;
	public Agent m_agent = null;
	public Database m_dbCurrent = null;
	public Document m_docContext = null;
	protected LNEnvironment m_env = null;
	
	/**
	 * true - при вызове метода для считывания конфигурации агента
	 */
	private boolean m_bInitFromConfig = false;
	private Log m_agentLog = null;
	
	private LNLog m_log = null;
	/**
	 * Категория для custom-лога;
	 * если не задано в документе настроек, то пишем в общий лог "Agents"
	 */
	protected String m_sLogCategory = LOG_CATEGORY_DEFAULT;
	protected String m_sLogDb = null;
	
	// other variables
	public PrintWriter m_pw = null;
	public String m_sAgName = null;
	
	/**
	 * Задание возможности вывода в консоль и log.nsf методом logAction
	 */
	protected boolean m_bIsSystemLog = true;
	/*
	 * Для возможности отключать вывод в консоль времени работы агента, т.к. некоторые агенты очень много спамят, потому что вызываются из web-сервисов
	 */
	private boolean m_bIsWorktimeOutput = true;
	protected boolean m_bIsCustomLog = false;
	protected boolean m_bIsContextLog = false;
	protected String m_sLogItemName;
	private boolean m_bIsDebug = false;
	protected boolean m_bIsSendAlert = true;
	protected boolean m_bIsPWLog = false;
	
	private TimerInfo m_oTimerGlobal = null;
	
	public void NotesMain() {
		try {
			m_session = getSession();
			m_agentContext = m_session.getAgentContext();
			m_agent = m_agentContext.getCurrentAgent();
			m_dbCurrent = m_agentContext.getCurrentDatabase();
			m_sAgName = m_agent.getName();
			m_docContext = m_agentContext.getDocumentContext();
			m_oTimerGlobal = new TimerInfo("Общее время работы");
			m_pw = getAgentOutput();
			LogEx.initialize(m_session);
			m_env = new LNEnvironment(m_session);
			
			main();												// <-- entry point to agent
		}
		catch(NotesException ne) {								// catch NOTESEXCEPTION
			// всё, что logError(ne) - логируем без отправки на почту
			switch (ne.id) {
			case LogEx.ERRc1221:
				// ничего не делаем
				break;
			case NotesError.NOTES_ERR_ERROR2:		// 4005
				if (ne.text.contains("database compaction in progress")) {
					// ошибки недоступности БД при сжатии и т.п.
					logError(ne);
					break;
				}
				if (ne.text.contains("rebuilt")) {
					// Notes error: The full text index needs to be rebuilt (...)
					logError(ne);
					LogEx.sendWarningMemo(LogEx.ERRc1223, ne.text);
					break;
				}
			case NotesError.NOTES_ERR_ERROR:		// 4000
				if (ne.text.contains("no longer")) {
					// Notes error: Remote system no longer responding {4000}
					// Notes error: The specified network name is no longer available {4000}
					logError(ne);
					break;
				}
			default:
				logFatalError(ne, ne.id, ne.text);
			}
		}
		catch(InternalException ie) {							// catch INTERNALEXCEPTION
			logError(ie);
		}
		catch(java.sql.SQLException a_sqle) {					// catch SQLEXCEPTION
			SQLException sqle = a_sqle;
			while (sqle != null) {
				logFatalError(sqle, LogEx.ERRc1223, sqle.getMessage() + " {" + sqle.getErrorCode() + "}");
				sqle = sqle.getNextException();
			}
		}
		catch (java.net.SocketException e) {
			// непредвиденные ошибки сброса/разрыва соединения
			logFatalError(e, LogEx.ERRc1223, LogEx.getMessage(e));
		}
		catch(java.lang.Exception e) {							// catch EXCEPTION
			logFatalError(e, LogEx.ERRc1111, LogEx.getMessage(e));
		}
		catch(java.lang.Throwable te) {							// catch THROWABLE
			logFatalError(te, LogEx.ERRc1111, LogEx.getMessage(te));
		}
		finally {												// FINAL CLEANUP
			String sWorkTimeInfo = m_oTimerGlobal.toString();
			if (m_bIsWorktimeOutput)
				logActionResult(sWorkTimeInfo);
			else {
				try { log2AgentLog(sWorkTimeInfo); }
				catch (NotesException e) {
					System.err.println(LogEx.getErrInfo(e, false));
					LogEx.printStackTrace(e);
				}
			}
			
			try {
				if (m_log != null)			m_log.close();
				if (m_agentLog != null)		m_agentLog.close();
			}
			catch (Exception e) {
				System.err.println(LogEx.getErrInfo(e, false));
				LogEx.printStackTrace(e);
			}
			
			// closing other domino objects if exist
			recycle();       // старый коммент, который здесь был: -- unrequared ...
			
			if (isDebugMode()) logMemoryUsage();
		}
	}
	
	
	// ------------ ABSTRACT methods
	protected abstract void main() throws NotesException, Exception, Throwable;
	
	
	protected void logMemoryUsage() {
		outToConsole("current size: " + Runtime.getRuntime().totalMemory());
		outToConsole("max size: " + Runtime.getRuntime().maxMemory());
		outToConsole("free size: " + Runtime.getRuntime().freeMemory());
	}
	
	
	/**
	 * Старый коммент: Если переданный лог не открыт, то пишем в текущую БД с типом LOGTYPE_SINGLE
	 * Сюда нужно передавать уже открытый лог!
	 */
	protected void setCustomLog(LNLog a_log) throws Exception {
		m_log = a_log;
		m_bIsCustomLog = true;
		m_bIsSystemLog = false;		// если custom-лог (wf-лог в БД) установлен, то автоматом отключаем вывод в консоль!
	}
	
	/**
	 * инициализация подсистемы логирования
	 * это была initCustomLog с параметром LNDbLog.LOGTYPE_APPEND)
	 */
	protected void setCustomLog(int iLogType, Object nDaysLogExpired) throws NotesException, Exception {
		setCustomLog(m_sLogCategory, m_sAgName, iLogType, nDaysLogExpired);
	}
	
	/**
	 * Запись будет вестись в категорию по умолчанию, т.е. в "Agents"
	 */
	protected void setCustomLog(int iLogType) throws Exception {
		setCustomLog(m_sLogCategory, m_sAgName, iLogType);
	}
	
	protected void setCustomLog(String sLogCategory, String sModuleName, int iLogType) throws Exception {
		setCustomLog(sLogCategory, sModuleName, iLogType, LNDbLog.LOG_EXPIRED_DAYS);
	}
	
	protected void setCustomLog(String sLogCategory, String sModuleName, int iLogType, Object nDaysLogExpired) throws Exception {
		if ((m_bInitFromConfig && !m_bIsCustomLog) || !m_bInitFromConfig) {
			if (sLogCategory == null || sLogCategory.isEmpty())
				sLogCategory = LOG_CATEGORY_DEFAULT;
			LNDbLog oLog;
			if (iLogType == LNDbLog.LOGTYPE_ENTRY)
				oLog = new LNDbLogEntry(sLogCategory, sModuleName);
			else
				oLog = new LNDbLogSingle(sLogCategory, sModuleName);
			Database dbLog = null;
			try {
				dbLog = (m_sLogDb != null) ? m_env.getDatabase(m_sLogDb) : m_env.getDbLog();
			}
			catch (NotesException e) {
				dbLog = m_dbCurrent;
				if (m_session.isOnServer() && isAlert()) {							// send alert notification to admins; при отладке на локале не нужно тревожить коллег
					LogEx.sendWarningMemo(LogEx.ERRc1111, "Не смог определить путь к БД логов, запись будет производиться в текущую БД");
				}
			}
			String sLogExpired = nDaysLogExpired.toString();
			if (!sLogExpired.isEmpty())
				oLog.open(dbLog, (int) Double.parseDouble(sLogExpired));
			else
				oLog.open(dbLog, LNDbLog.LOG_EXPIRED_DAYS);
			setCustomLog(oLog);
		}
		else {
			if (m_bInitFromConfig)
				// TODO: инициализировать лог по конфигурации в loadConfigFromDoc, -
				// в документе лога нет поля, где бы задавалось LNLog.LOGTYPE_SINGLE/LOGTYPE_APPEND
				// просто нет времени этим заниматься :(
				// Можно было бы тогда задавать custom-лог извне только для агентов, не вычитывающих параметры из конфигурационного документа
				LogEx.sendWarningMemo(LogEx.ERRc1111, "LNAgentBase: попытка ручной инициализации custom-лога для агента, инициализируемого из документа конфигурации");
			else
				LogEx.sendWarningMemo(LogEx.ERRc1111, "LNAgentBase: попытка ренициализации custom-лога");
		}
	}
	
	
	public void setLogAgentOutput(boolean bIsPW) {
		m_bIsPWLog = bIsPW;
	}
	
	
	public void setLogOptionContext(boolean bIsLog, String sItemName) {
		m_bIsContextLog = bIsLog;
		m_sLogItemName = sItemName;
	}
	
	
	private void agentLogInit() throws NotesException {
		m_agentLog = m_session.createLog(m_sAgName);
		m_agentLog.openAgentLog();
	}
	
	/**
	 * Вывод в лог агента "View Log".
	 * Если возникает ошибка переполнения 4000 "Memory allocation request exceeded 65,000 bytes",
	 * то надо или сокращать вывод в лог или отказываться от него
	 */
	public void log2AgentLog(String sText) throws NotesException {
		if (m_agentLog == null) agentLogInit();
		m_agentLog.logAction(sText);
	}
	
	public void log2AgentLog(int nErr, String sText) throws NotesException {
		if (m_agentLog == null) agentLogInit();
		m_agentLog.logError(nErr, sText);
	}
	
	public void outToConsole(String sText) {
		System.out.println(m_sAgName + " >> " + sText);
	}
	
	public void outToPW(String sText) {
		if (m_pw != null)			m_pw.println(sText);
	}
	
	
	/**
	 * Внимание! Часто бывает переполнение, потому обычные действия в agentLog не пишем!
	 */
	public void logAction(String sText) {
		if (m_bIsPWLog) outToPW(sText);
		if (m_bIsCustomLog && m_log != null)
			try {
				m_log.log(sText);
			} catch (Exception e) {
				System.err.println("LNAgentBase.logAction: " + LogEx.getErrInfo(e, false));
				m_bIsSystemLog = true;
				m_bIsCustomLog = false;
			}
		if (m_bIsSystemLog)						outToConsole(sText);
	}
	
	/**
	 * Специальный метод для записи строки и в обычные логи, включённые в logAction, и в agentLog:
	 * это обычно информация о старте агента и результатах его работы;
	 * этот метод даёт ОБЯЗАТЕЛЬНЫЙ вывод на консоль (и, соответственно, в log.nsf)
	 * @param sText - логируемая строка
	 * 
	 */
	public void logActionResult(String sText) {
		boolean bToLog = m_bIsSystemLog;
		m_bIsSystemLog = true;
		try {
			log2AgentLog(sText);
			logAction(sText);
		}
		catch (Exception e) {
			System.err.println(LogEx.getErrInfo(e, false));
			LogEx.printStackTrace(e);
		}
		m_bIsSystemLog = bToLog;
	}
	
	public void log2Context(String sText) throws Exception {
		if (m_bIsContextLog) {
			if (m_docContext == null) m_docContext = m_agentContext.getDocumentContext();
			
			if (!m_docContext.hasItem(m_sLogItemName))
				m_docContext.replaceItemValue(m_sLogItemName, sText).recycle();
			else {
				Item it_log = m_docContext.getFirstItem(m_sLogItemName);
				it_log.appendToTextList(sText);
				it_log.recycle();
			}
		}
	}
	
	public void logError(Throwable te) {
		try {
			logError(LogEx.getClassName(te), LogEx.getID(te), LogEx.getMessage(te));
		} catch (Exception e) {
			System.err.println(LogEx.getErrInfo(e, false));
			LogEx.printStackTrace(e);
		}
	}
	
	public void logError(int nErr, String sText) throws Exception {
		logError(null, nErr, sText);
	}
	
	protected void logError(String sClassName, int nErr, String sText) throws Exception {
		String sErrDescription = LogEx.getErrInfo(sClassName, nErr, sText, null);
		if (m_bIsSystemLog)						System.err.println(m_sAgName + " >> " + sErrDescription);
		if (m_bIsPWLog) outToPW(sErrDescription);
		log2AgentLog(nErr, sText);					// ошибки в agent-лог пишем всегда
		if (m_bIsCustomLog && m_log != null) {
			m_log.log(sText);
		}
	}
	
	/**
	 * Не вызывать для внутренних - предопределённых, т.е. известных нам ошибок (InternalException)!
	 */
	private void logFatalError(Throwable te, int nErrCode, String sErrDescription) {
		sErrDescription = LogEx.getErrInfo(LogEx.getClassName(te), nErrCode, (sErrDescription != null) ? sErrDescription : LogEx.getMessage(te), null);
		
		String sStackTrace = "";
		switch (nErrCode) {
		//для данных кодов ошибок не выводим stacktrace в лог
		case LogEx.ERRc1222:
		case LogEx.ERRc1223:
		case LNEnvironment.ERR_DB_NOT_OPEN:
			if (!m_bIsDebug) break;		// не выводим только в обычном режиме, при отладке выводим
		default:
			sStackTrace = "\n\n" + LogEx.getStackTrace(te);
		}
		
		if (m_bIsCustomLog && m_log != null) {								// print to custom log if exist
			try {
				m_log.log(sErrDescription);
				if (!sStackTrace.isEmpty()) {
					m_log.printStackTrace(te);								// print to custom log error stack
				}
			} catch (java.lang.Throwable e) {
				m_bIsSystemLog = true;
				
				// сообщение о сбое
				System.err.println("LNAgentBase.logFatalError: " + LogEx.getErrInfo(e, false));
				LogEx.printStackTrace(e);
			}
			finally {
				if (m_bIsSystemLog) {
					System.err.println(sErrDescription);								// print to standart error stream
					if (!sStackTrace.isEmpty()) {
						LogEx.printStackTrace(te);
						try {
							if (m_session.isOnServer())										// на локале строка выше делает то же самое
								if (m_bIsPWLog && m_pw != null) te.printStackTrace(m_pw);	// print to printwriter stream if exist
						} catch (NotesException e) {}
					}
				}
			}
		}
		
		try {
			log2Context(sErrDescription);										// print to context document item
			log2AgentLog(nErrCode, sErrDescription + sStackTrace);				// print to agent log
			
			if (m_session.isOnServer() && isAlert()) {							// send alert notification to admins; при отладке на локале не нужно тревожить коллег
				LogEx.sendErrorMemo(m_sAgName, nErrCode, "Внимание! В ЭДБ произошла критическая ошибка - агент завершил работу по исключению.",
						sErrDescription + sStackTrace, null);
			}
		}
		catch (java.lang.Throwable e) {
			// сообщение о сбое
			System.err.println("LNAgentBase.logFatalError: " + LogEx.getErrInfo(e, false));
			LogEx.printStackTrace(e);
		}
	}
	
	
	public void setDebugMode(boolean bMode)				{ setDebug(bMode); m_bIsDebug = bMode; }
	public boolean isDebugMode() 						{ return m_bIsDebug; }
	public void setAlert(boolean bAlert)				{ m_bIsSendAlert = bAlert; }
	public boolean isAlert()							{ return m_bIsSendAlert; }
	
	/**
	 * Возможность запрета вывода общего времени работы агента, т.к. много спама в консоли из-за того, что некоторые агенты запускаются из web-сервисов
	 * @param bWtO
	 */
	public void setWorktimeOutput(boolean bWtO)			{ m_bIsWorktimeOutput = bWtO; }
	
	public void logDebug(String sText) throws Exception {
		if (m_bIsDebug) logAction(sText);
	}
	
	public void logDebug(Map<String, Object> map) throws Exception {
		if (m_bIsDebug) {
			if (map != null) {
				Object vKey;
				Object vValue;
				for (Iterator<String> it = map.keySet().iterator(); it.hasNext(); ) {
					vKey = it.next();
					vValue = map.get(vKey);
					logAction(vKey + " = " + ((vValue != null) ? vValue.toString() : "null"));
				}
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public void sendMail(Object oTo, String sFrom, String sSubject, String sText, boolean isImportant, Document docInclude, boolean bIsLink) throws NotesException {
		Document docSend = null;
		
		if (oTo == null) return;
		
		try {
			docSend = m_dbCurrent.createDocument();
			
			docSend.replaceItemValue("Form", "Memo");
			docSend.replaceItemValue("Subject", sSubject);
			
			if (sFrom != null && sFrom.length() > 0)
				docSend.replaceItemValue("Principal", sFrom);
			else {
				int nIndex = m_sAgName.indexOf('|');
				docSend.replaceItemValue("Principal", (nIndex != -1) ? m_sAgName.substring(0, nIndex) : m_sAgName);
			}
			
			if (isImportant)
				docSend.replaceItemValue("Importance", "1");
			
			RichTextItem rtBody = docSend.createRichTextItem("Body");
			
			if (sText != null && sText.length() > 0)
				rtBody.appendText(sText);
			
			if (docInclude != null)
				if (bIsLink)
					rtBody.appendDocLink(docInclude);
				else
					docInclude.renderToRTItem(rtBody);
			
			// send memo
			if (oTo instanceof Vector)
				docSend.send((Vector<String>) oTo);
			else
				docSend.send(oTo.toString());
		} finally {
			Tools.recycleObj(docSend);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	protected void initFromDocument(Document docProfile, Map<String, Object> vParam) throws NotesException {
		m_bInitFromConfig = true;
		
		if (docProfile.hasItem("fdIsDebug"))
			setDebugMode(docProfile.getItemValueString("fdIsDebug").equals("1"));
		
		if (docProfile.hasItem("fdLogCategory"))
			m_sLogCategory = docProfile.getItemValueString("fdLogCategory");
		else
			m_sLogCategory = null;
		
		if (docProfile.hasItem("fdIsLog") && docProfile.getItemValueString("fdIsLog").equals("1")) {
			m_sLogDb = docProfile.getItemValueString("fdLogDb");
			if (m_sLogDb.length() == 0) m_sLogDb = null;
		}
		
		if (vParam != null)
			for (Iterator<String> it = vParam.keySet().iterator(); it.hasNext(); ) {
				String sItemName = it.next();
				Vector<Object> vValue;
				if (docProfile.hasItem(sItemName)) {
					vValue = docProfile.getItemValue(sItemName);
					if (!vValue.isEmpty())
						vParam.put(sItemName, (vValue.size() > 1) ? vValue : vValue.firstElement());
				}
			}
	}

	
	protected void initFromProfile(String sProfileName, String sKey, Map<String, Object> vParam) throws NotesException {
		Document docProfile = m_env.getProfile(m_dbCurrent,
								(sProfileName != null) ? sProfileName : m_sAgName,
								(sKey != null) ? sKey : "AdminUser");
		
		initFromDocument(docProfile, vParam);
	}
	
	
	/**
	 * Базовый метод загрузки конфигурации агента из указанного документа настроек
	 * 		Также выполняет загрузку параметров WSClient'ов
	 * @param Document pdocConfig Документ настроек, из которого осуществляется загрузка параметров
	 * @param HaspMap pmapCfg HashMap, в который осуществляется загрузка параметров
	 * @param String psProfileName Имя профайла (для ошибок)
	 * @throws Exception
	 * TODO::CREATE Выполнить рефакторинг всех наследников:
	 * 		переориентировать на использование loadConfigFromDoc;
	 * 		удалить loadConfiguration
	 */
	@SuppressWarnings("unchecked")
	protected void loadConfigFromDoc(Document pdocConfig, HashMap pmapCfg) throws Exception {
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
			}
			else if (pmapCfg.get(ITEM_PARAM) instanceof String && pmapCfg.get(ITEM_PARAMVAL) instanceof String) {
				String sDelim = (String)pmapCfg.get(ITEM_PARAMVAL_DELIM);
				
				pmapCfg.put(pmapCfg.get(ITEM_PARAM),
						(sDelim.isEmpty() || sDelim.equals(LIST_DUMMYELEMENT)) ?
						pmapCfg.get(ITEM_PARAMVAL) : 															// значение параметра не содержит список
						m_session.evaluate("@Explode('" + pmapCfg.get(ITEM_PARAMVAL) + "'; '" + sDelim + "')") 	// значение параметра содержит список
						);
			}
		}
		
		// Считывание параметров аутентификации
		if (pmapCfg.get(ITEM_ISAUTH) != null && !((String) pmapCfg.get(ITEM_ISAUTH)).isEmpty() &&
				((String) pmapCfg.get(ITEM_AUTHTYPE)).equals(AUTH_BASIC) &&
				pmapCfg.get(ITEM_KEY_CREDENTIAL) != null && !((String) pmapCfg.get(ITEM_KEY_CREDENTIAL)).isEmpty()) {
			pmapCfg.put(ITEM_LOGIN, null);
			pmapCfg.put(ITEM_PASSWORD, null);
			
			View viewCredentials = m_env.getView(m_env.getDbConfig(), CREDENTIALS_VIEWNAME);
			if ( viewCredentials == null )
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found view " + CREDENTIALS_VIEWNAME + " in database " +
						m_env.getDbConfig().getServer() + "!!" + m_env.getDbConfig().getFilePath());

			Document docCredential = viewCredentials.getDocumentByKey((String) pmapCfg.get(ITEM_KEY_CREDENTIAL), true);
			if (docCredential == null)
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found credentials document for web-service: " + pdocConfig.getItemValueString(ITEM_PROFILENAME));
			
			pmapCfg.put(ITEM_LOGIN, docCredential.getItemValueString(ITEM_LOGIN));
			pmapCfg.put(ITEM_PASSWORD, docCredential.getItemValueString(ITEM_PASSWORD));
		}
		// Считывание настроек используемых web-сервисов
		else if (pmapCfg.get(ITEM_WSCUSEFLAG) != null && !((String) pmapCfg.get(ITEM_WSCUSEFLAG)).isEmpty() &&
				pmapCfg.get(ITEM_WSCLIST) != null) {
			View viewConfig = m_env.getView(m_env.getDbConfig(), LNEnvironment.FUIBCONFIG_LOOKUPVIEWNAME);
			if (viewConfig == null)
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found view " + LNEnvironment.FUIBCONFIG_LOOKUPVIEWNAME + " in database " +
						m_env.getDbConfig().getServer() + "!!" + m_env.getDbConfig().getFilePath());
			
			// Проход по всем используемым web-сервисам
			Vector keys = new Vector();
			if (pmapCfg.get(ITEM_WSCLIST).getClass().getName().equals("java.lang.String")) {
				keys.add((String) pmapCfg.get(ITEM_WSCLIST));
			} else if (pmapCfg.get(ITEM_WSCLIST).getClass().getName().equals("java.util.Vector")) {
				keys = (Vector) pmapCfg.get(ITEM_WSCLIST);
			}
			
			// Буферизируем флаг дебага
			boolean bIsDebugBuf = isDebugMode();
			for (int i=0; i < keys.size(); i++)	{
				String sWSCID = (String)keys.get(i);
				Document docWSClient = viewConfig.getDocumentByKey( "wsclient#" + sWSCID, true);
				if (docWSClient == null)
					throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found web-service settings document: " + sWSCID);
				
				HashMap tmpMap = new HashMap();
				loadConfigFromDoc(docWSClient, tmpMap);
				
				Object keysExt[] = tmpMap.keySet().toArray();
				for (int j = 0; j < keysExt.length; j++) {
					pmapCfg.put(sWSCID + "##" + (String)keysExt[j], tmpMap.get((String)keysExt[j]));
				}
			}
			setDebugMode(bIsDebugBuf);
		}
		
		logDebug("Configuration parameters are: ");
		// change value of password to its lengths
		HashMap tmpMap = new HashMap();
		Object keysExt[] = pmapCfg.keySet().toArray();
		for (int i = 0; i < keysExt.length; i++) {
			if (((String)keysExt[i]).contains(ITEM_PASSWORD))
				tmpMap.put((String) keysExt[i], String.valueOf(((String) pmapCfg.get((String) keysExt[i])).length()));
			else
				tmpMap.put((String) keysExt[i], pmapCfg.get((String) keysExt[i]));
		}
		logDebug(tmpMap);
	}
	
	public String getAuthUser() throws NotesException {
		return m_session.getEffectiveUserName();
	}
	
	public LNEnvironment getLNEnv() {
		return m_env;
	}
	
	/**
	 * Получение значения параметра (включая параметры используемых WSClient'ов)
	 * @param String psName Имя запрашиваемого параметра
	 * @param String psPrefix Префикс (имя WSClient'а, чья настройка запрашивается)
	 * @return Содержимое параметра
	 */
	protected Object getParam(HashMap<String, ?> config, String psName, String psPrefix) {
		if (!psPrefix.isEmpty())
			return config.get(psPrefix.toLowerCase() + "##" + psName);
		else
			return config.get(psName);
	}
	
	
	protected void recycle() {
		try {
			Tools.recycleObj(m_agentLog);
			Tools.recycleObj(m_log);
			Tools.recycleObj(m_docContext);
			Tools.recycleObj(m_agent);
			Tools.recycleObj(m_agentContext);
			Tools.recycleObj(m_env);
			Tools.recycleObj(m_dbCurrent);
//			Tools.recycleObj(m_session);			// чтобы web-сервисы не навернулись
		}
		catch (Exception e) {
			LogEx.getErrInfo(e, false);
			LogEx.printStackTrace(e);
		}
	}
	
}