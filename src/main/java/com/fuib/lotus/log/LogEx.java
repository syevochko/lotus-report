package com.fuib.lotus.log;

import java.util.Vector;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Agent;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.Log;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;
import lotus.domino.Session;


public class LogEx implements lotus.domino.Base {
	/**
	 * Предопределённый код ошибки - "для разработчика"
	 */
	static public final int ERRc1111 = 1111;
	/**
	 * Предопределённый код ошибки - "для пользователя", которая прерывая текущую итерацию не прерывает общий процесс (вставляйте обработку в обработчиках выше по стэку)
	 */
	static public final int ERRc1220 = 1220;
	/**
	 * Предопределённый код ошибки - "для пользователя" БЕЗ "обратитесь к ..."
	 * Этот же код используется при исполнении на сервере для того, чтобы не генерировать критическую ошибку, а логировать как обычную, без отправки на почту
	 */
	static public final int ERRc1221 = 1221;
	/**
	 * Предопределённый код ошибки - "для QA" С "обратитесь в службу поддержки Lotus-ЭДБ...";
	 * вывод в лог стэка ошибок при данном коде не производится
	 */
	static public final int ERRc1222 = 1222;
	/**
	 * Предопределённый код ошибки - "для админа" С "обратитесь к администратору...";
	 * вывод в лог стэка ошибок при данном коде не производится
	 */
	static public final int ERRc1223 = 1223;
	
	static final int PRIORITY_HIGH = 1;
	static final int PRIORITY_NORMAL = 2;
	static final int PRIORITY_LOW = 3;
	
	static private final String RECIPIENTS_DEVELOPERS = "APP.Developers";
	
	static private Session m_session = null;
	static private Log m_mailLog = null;
	
	
	static public void initialize(Session session) {
		m_session = session;
	}
	
	
	static public void addErrorToLog(String sModule, NotesException e, Database dbLog) {
		addErrorToLog(sModule, e.id, e.text, getStackTrace(e), dbLog);
	}
	
	static public void addErrorToLog(String sModule, Throwable te, Database dbLog) {
		addErrorToLog(sModule, ERRc1111, te.toString(), getStackTrace(te), dbLog);
	}
	
	static public void addErrorToLog(String sModule, int nErrCode, String sBriefDescription, String sAddDescription, Database dbLog) {
		RichTextItem itemFullDescription = null;
		if (!(sAddDescription == null || sAddDescription.isEmpty())) {
			try {
				if (dbLog != null) {
					itemFullDescription = dbLog.createDocument().createRichTextItem("Body");
					itemFullDescription.appendText(sAddDescription);
				}
				else
					sBriefDescription += "\n\n" + sAddDescription;
			}
			catch (NotesException e) {
				System.err.println("Error code: " + e.id);
				e.printStackTrace();
			}
		}
		addErrorToLog(sModule, nErrCode, sBriefDescription, itemFullDescription, dbLog);
	}
	
	
	/**
	 * Запись сообщения об ошибке в лог с отправкой письма об этом. Если нужна отправка без создания записи лога, передавайте null в параметр dbLog
	 * @param sModule - имя модуля, где возникла ошибка; если не указано, то будет использовано имя текущего агента
	 * @param nErrCode
	 * @param sBriefDescription
	 * @param itemFullDescription
	 * @param dbLog
	 * @throws NotesException
	 */
	static public void addErrorToLog(String sModule, int nErrCode, String sBriefDescription, RichTextItem itemFullDescription, Database dbLog) {
		// Создание записи лога
		Name oName = null;
		Document logDoc = null;
		DateTime oDateTime = null;
		RichTextItem rtItem = null;
		
		try {
			if (sModule == null || sModule.isEmpty()) {
				Agent agent = m_session.getAgentContext().getCurrentAgent();
				sModule = (agent != null) ? agent.getName() : "";
			}
			
			if (dbLog != null) {
				logDoc = dbLog.createDocument();
				logDoc.replaceItemValue("Form", "LogDocument");
				
				oDateTime = m_session.createDateTime("Today");
				oDateTime.setNow();
				logDoc.replaceItemValue("fdDateTime", oDateTime);
				
				logDoc.replaceItemValue("fdWho", sModule);
				logDoc.replaceItemValue("fdBriefDescription", sBriefDescription);
				logDoc.replaceItemValue("fdPriority", new Integer(1));		// all error -> HIGH
				
				String IN_fdFULLDESCRIPTION = "fdFullDescription";
				rtItem = logDoc.createRichTextItem(IN_fdFULLDESCRIPTION);
				Database dbCurrent = m_session.getCurrentDatabase();
				oName = m_session.createName(dbCurrent.getServer());
				addAgentInfo(rtItem, oName.getAbbreviated(), dbCurrent, sModule, itemFullDescription);
				
				logDoc.save(false);
			}
			else {
				if (itemFullDescription != null)
					sBriefDescription += itemFullDescription.getUnformattedText();
			}
			
			sendErrorMemo(sModule, nErrCode, null, sBriefDescription, logDoc);
		}
		catch (NotesException e) {
			System.err.println("Error code: " + e.id);
			e.printStackTrace();
		}
		finally {
			Tools.recycleObj(oName);
			Tools.recycleObj(rtItem);
			Tools.recycleObj(logDoc);
			Tools.recycleObj(oDateTime);
		}
	}
	
	public static void sendErrorMemo(String sModule, Throwable te, Document logDoc) {
		int nErrCode = getID(te);
		String sBody = getErrInfo(getClassName(te), nErrCode, getMessage(te), null);
		sendErrorMemo(sModule, nErrCode, null, sBody, logDoc);
	}
	
	public static void sendErrorMemo(String sModule, int nErrCode, String sSubject, String sBody, Document logDoc) {
		Name oName = null;
		RichTextItem rtItem = null;
		Document mailDoc = null;
		
		try {
			Database dbCurrent = m_session.getCurrentDatabase();
			
			if (sModule == null || sModule.isEmpty()) {
				Agent agent = m_session.getAgentContext().getCurrentAgent();
				sModule = (agent != null) ? agent.getName() : "";
			}
			
			// Отправка письма
			mailDoc = dbCurrent.createDocument();
			rtItem = mailDoc.createRichTextItem("Body");

			mailDoc.replaceItemValue("DeliveryPriority", "H");
			mailDoc.replaceItemValue("Importance", "1");
			
			oName = m_session.createName(dbCurrent.getServer());
			if (sSubject == null || sSubject.isEmpty())
				sSubject = "[" + oName.getAbbreviated() + "] " + "Внимание! В ЭДБ произошла ошибка.";
			else
				sSubject = "[" + oName.getAbbreviated() + "] " + sSubject;
			if (logDoc == null)
				addAgentInfo(rtItem, oName.getAbbreviated(), dbCurrent, sModule, null);
			rtItem.appendText("Ошибка: " + sBody);
			rtItem.addNewLine(2);
			if (logDoc != null) {
				rtItem.appendDocLink(logDoc, "", "Линк к документу лога");
				sSubject += " Подробности смотри в системном логе.";
			}
			mailDoc.replaceItemValue("Subject", sSubject);
			
			Vector<String> vctTo = new Vector<String>(2);
			switch (nErrCode) {
			case ERRc1222:		// только админам
				vctTo.addElement("APP.QA");
				vctTo.addElement("DHO.Admins");
				break;
			case ERRc1223:
				vctTo.addElement("DHO.Admins");
				break;
			case ERRc1111:		// только разработчикам
				vctTo.addElement(RECIPIENTS_DEVELOPERS);
				break;
			default:
				//TODO админам и разработчикам (в последствии выделить коды для админов, остальное - разработчикам)
				vctTo.addElement("DHO.Admins");
				vctTo.addElement("APP.QA");
				vctTo.addElement(RECIPIENTS_DEVELOPERS);
				break;
			}
			mailDoc.replaceItemValue("SendTo", vctTo);
			
			Tools.send(mailDoc, false);
			vctTo.clear();
		}
		catch (NotesException e) {
			System.err.println("Error code: " + e.id);
			e.printStackTrace();
		}
		finally {
			Tools.recycleObj(oName);
			Tools.recycleObj(rtItem);
			Tools.recycleObj(mailDoc);
		}
	}
	
	/**
	 * C заданием кода ошибки (для разделения потоков писем на QA или админов)
	 * @param sWarning - тело сообщения
	 */
	public static void sendWarningMemo(int nErrCode, String sWarning) {
		sendErrorMemo(null, nErrCode, null, sWarning, null);
	}
	
	/**
	 * Без задания кода ошибки - письмо уйдёт на QA
	 * @param sWarning - тело сообщения
	 */
	public static void sendWarningMemo(String sWarning) {
		sendErrorMemo(null, ERRc1222, null, sWarning, null);
	}
	
	
	private static void addAgentInfo(RichTextItem rtItem, String sServerName, Database dbCurrent, String sAgentName, RichTextItem itemFullDescription) {
		try {
			rtItem.appendText("Сервер: " + sServerName);
			rtItem.addNewLine(2);
			rtItem.appendText("Текущая БД: '" + dbCurrent.getTitle() + "' (" + dbCurrent.getFilePath() + ")");
			rtItem.addNewLine(2);
			if (!sAgentName.isEmpty()) {
				rtItem.appendText("Агент: '" + sAgentName + "'");
				rtItem.addNewLine(2);
			}
			if (itemFullDescription != null) {
				rtItem.appendRTItem(itemFullDescription);
				rtItem.addNewLine(2);
			}
		} catch (NotesException e) {
			System.err.println("Error code: " + e.id);
			e.printStackTrace();
		}
	}
	
	/**
	 * Собирает инфу об ошибке в установленном формате
	 * @param te - любой объект ошибки
	 * @param bStackTrace - включать стек вызовов или нет
	 */
	static public String getErrInfo(Throwable te, boolean bStackTrace) {
		String sStackTrace = null;
		if (bStackTrace) sStackTrace = getStackTrace(te);
		return LogEx.getErrInfo(te.getClass().getName(), LogEx.getID(te), LogEx.getMessage(te), sStackTrace);
	}
	
	static public String getErrInfo(String sClassName, int nErrCode, String sErrMessage, String aAddText) {
		String result = "'" + sErrMessage + "' {" + nErrCode + "}";
		if (sClassName != null && !sClassName.isEmpty()) result = sClassName + ": " + result;
		if (aAddText != null && !aAddText.isEmpty()) result += "\n\n" + aAddText;
		return result;
	}
	
	static public String getClassName(Throwable te) {
		String sErrClassName = te.getClass().getName();
		sErrClassName = sErrClassName.substring(sErrClassName.lastIndexOf(".") + 1);
		return sErrClassName;
	}
	
	static public int getID(Throwable te) {
		int nErr;
		if (te instanceof NotesException)
			nErr = ((NotesException) te).id;
		else if (te instanceof InternalException)
			nErr = ((InternalException) te).id;
		else
			nErr = ERRc1111;
		return nErr;
	}
	
	static public String getMessage(Throwable te) {
		String sErrText;
		if (te instanceof NotesException)
			sErrText = ((NotesException) te).text;
		else {
			sErrText = te.getMessage();
			if (sErrText == null || sErrText.isEmpty()) sErrText = te.toString();
		}
		return sErrText;
	}
	
	/**
	 * Возвращает стэк ошибок в нормальном текстовом виде через перевод строки \n
	 * @param te - Exception, NotesException
	 */
	static public String getStackTrace(Throwable te) {
		String sResult = "";
		if (te != null) {
			StackTraceElement[] ste = te.getStackTrace();
			if (ste.length > 0) {
				sResult = ste[0].toString();
				for (int i = 1; i < ste.length; i++)	{
					sResult += "\n" + ste[i].toString();
				}
			}
		}
		return sResult;
	}
	
	/**
	 * Печать на консоль стэка; в отличие от стандартного метода печатает только стэк, без первой строки с ошибкой, т.к. мы формируем её самостоятельно
	 * @param te
	 */
	static public void printStackTrace(Throwable te) {
		StackTraceElement[] ste = te.getStackTrace();
		for (int i = 0; i < ste.length; i++)	{
			System.err.println(" 	" + ste[i].toString());
		}
	}
	
	/**
	 * Метод записи стэка ошибок в Notes-объект лога
	 * @param e - Exception, NotesException
	 * @param log - ActionLog, MailLog и т.п.
	 */
	static public void logError(Throwable te, Log log) {
		if (log != null) {
			try {
				log.logError(getID(te), getMessage(te));
				log.logAction("stacktrace:");
				StackTraceElement[] ste = te.getStackTrace();
				for (int i = 0; i < ste.length; i++)	{
					log.logAction(" 	" + ste[i].toString());
				}
			}
			catch (NotesException ne) {
				System.err.println("Error code: " + ne.id);
				ne.printStackTrace();
			}
		}
	}
	
	/**
	 * Инициализация mail-лога;
	 * @param sLogCategory - категория
	 * @param sUserName - имя пользователя или группы, которым будет произведена отправка письма
	 */
	@SuppressWarnings("unchecked")
	static public Log openMailLog(String sLogCategory, Object vRecipients) {
		try {
			Vector<String> vctTo = null;
			if (vRecipients != null) {
				if (vRecipients instanceof Vector)
					vctTo = (Vector<String>) vRecipients;
				else {
					if (vRecipients instanceof String) {
						vctTo = new Vector<String>(0);
						vctTo.add((String) vRecipients);
					}
					else
						throw new NotesException(ERRc1111, "openMailLog: Неверный тип параметра vRecipients (" + vRecipients.getClass().getName() + "), ожидается String или Vector<String>");
				}
			}
			if (vctTo != null) {
				m_mailLog = m_session.createLog(sLogCategory);
				m_mailLog.openMailLog(vctTo, "[" + m_session.getCurrentDatabase().getServer() + "] " + sLogCategory);
			}
		}
		catch (NotesException e) {
			System.err.println("Err code: " + e.id);
			e.printStackTrace();
		}
		return m_mailLog;
	}
	
	static public Log getMailLog() {
		return m_mailLog;
	}
	
	static public void log2Mail(Throwable te) {
		try {
			logError(te, m_mailLog);
		}
		catch (Exception e) {
			if (createMailLog()) {
				log2Mail(te);
				return;
			}
			System.err.println(e.toString());
			e.printStackTrace();
		}
	}
	
	/**
	 * @param sLine - строка, которая будет логироваться в mail-логе
	 * Если вызвать этот метод без предварительной инициализации, то будет создан лог с параметрами по умолчанию:
	 * категория - текущий агент, имя пользователя - последний, сохранявший агент;
	 * если последний сохранявший является системмным пользователем, тогда письмо будет отправляться группе разработчиков
	 */
	static public void log2Mail(String sLine) {
		try {
			m_mailLog.logAction(sLine);
		}
		catch (Exception e) {
			if (createMailLog()) {
				log2Mail(sLine);
				return;
			}
			System.err.println(e.toString());
			e.printStackTrace();
		}
	}
	
	static private boolean createMailLog() {
		if (m_mailLog == null) {
			try {
				// поздняя инициализация по умолчанию
				Agent agent = m_session.getAgentContext().getCurrentAgent();
				// если агент подписан технической учёткой, - на APP.Developers, иначе на последнего сохранившего агент
				String sUserName = agent.getOwner();
				if (sUserName.indexOf("Lotus") != -1) sUserName = RECIPIENTS_DEVELOPERS;
				openMailLog(agent.getName(), sUserName);
				return (m_mailLog != null);
			}
			catch (NotesException ne) {
				ne.printStackTrace();
			}
		}
		return false;
	}
	
	
	public void recycle() {
		if (m_mailLog != null) {
			try {
				m_mailLog.close();
				m_mailLog.recycle();
			}
			catch (NotesException e) {
				System.err.println("Err code: " + e.id);
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void recycle(Vector arg0) throws NotesException {}
}
