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
	 * ��������������� ��� ������ - "��� ������������"
	 */
	static public final int ERRc1111 = 1111;
	/**
	 * ��������������� ��� ������ - "��� ������������", ������� �������� ������� �������� �� ��������� ����� ������� (���������� ��������� � ������������ ���� �� �����)
	 */
	static public final int ERRc1220 = 1220;
	/**
	 * ��������������� ��� ������ - "��� ������������" ��� "���������� � ..."
	 * ���� �� ��� ������������ ��� ���������� �� ������� ��� ����, ����� �� ������������ ����������� ������, � ���������� ��� �������, ��� �������� �� �����
	 */
	static public final int ERRc1221 = 1221;
	/**
	 * ��������������� ��� ������ - "��� QA" � "���������� � ������ ��������� Lotus-���...";
	 * ����� � ��� ����� ������ ��� ������ ���� �� ������������
	 */
	static public final int ERRc1222 = 1222;
	/**
	 * ��������������� ��� ������ - "��� ������" � "���������� � ��������������...";
	 * ����� � ��� ����� ������ ��� ������ ���� �� ������������
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
	 * ������ ��������� �� ������ � ��� � ��������� ������ �� ����. ���� ����� �������� ��� �������� ������ ����, ����������� null � �������� dbLog
	 * @param sModule - ��� ������, ��� �������� ������; ���� �� �������, �� ����� ������������ ��� �������� ������
	 * @param nErrCode
	 * @param sBriefDescription
	 * @param itemFullDescription
	 * @param dbLog
	 * @throws NotesException
	 */
	static public void addErrorToLog(String sModule, int nErrCode, String sBriefDescription, RichTextItem itemFullDescription, Database dbLog) {
		// �������� ������ ����
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
			
			// �������� ������
			mailDoc = dbCurrent.createDocument();
			rtItem = mailDoc.createRichTextItem("Body");

			mailDoc.replaceItemValue("DeliveryPriority", "H");
			mailDoc.replaceItemValue("Importance", "1");
			
			oName = m_session.createName(dbCurrent.getServer());
			if (sSubject == null || sSubject.isEmpty())
				sSubject = "[" + oName.getAbbreviated() + "] " + "��������! � ��� ��������� ������.";
			else
				sSubject = "[" + oName.getAbbreviated() + "] " + sSubject;
			if (logDoc == null)
				addAgentInfo(rtItem, oName.getAbbreviated(), dbCurrent, sModule, null);
			rtItem.appendText("������: " + sBody);
			rtItem.addNewLine(2);
			if (logDoc != null) {
				rtItem.appendDocLink(logDoc, "", "���� � ��������� ����");
				sSubject += " ����������� ������ � ��������� ����.";
			}
			mailDoc.replaceItemValue("Subject", sSubject);
			
			Vector<String> vctTo = new Vector<String>(2);
			switch (nErrCode) {
			case ERRc1222:		// ������ �������
				vctTo.addElement("APP.QA");
				vctTo.addElement("DHO.Admins");
				break;
			case ERRc1223:
				vctTo.addElement("DHO.Admins");
				break;
			case ERRc1111:		// ������ �������������
				vctTo.addElement(RECIPIENTS_DEVELOPERS);
				break;
			default:
				//TODO ������� � ������������� (� ����������� �������� ���� ��� �������, ��������� - �������������)
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
	 * C �������� ���� ������ (��� ���������� ������� ����� �� QA ��� �������)
	 * @param sWarning - ���� ���������
	 */
	public static void sendWarningMemo(int nErrCode, String sWarning) {
		sendErrorMemo(null, nErrCode, null, sWarning, null);
	}
	
	/**
	 * ��� ������� ���� ������ - ������ ���� �� QA
	 * @param sWarning - ���� ���������
	 */
	public static void sendWarningMemo(String sWarning) {
		sendErrorMemo(null, ERRc1222, null, sWarning, null);
	}
	
	
	private static void addAgentInfo(RichTextItem rtItem, String sServerName, Database dbCurrent, String sAgentName, RichTextItem itemFullDescription) {
		try {
			rtItem.appendText("������: " + sServerName);
			rtItem.addNewLine(2);
			rtItem.appendText("������� ��: '" + dbCurrent.getTitle() + "' (" + dbCurrent.getFilePath() + ")");
			rtItem.addNewLine(2);
			if (!sAgentName.isEmpty()) {
				rtItem.appendText("�����: '" + sAgentName + "'");
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
	 * �������� ���� �� ������ � ������������� �������
	 * @param te - ����� ������ ������
	 * @param bStackTrace - �������� ���� ������� ��� ���
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
	 * ���������� ���� ������ � ���������� ��������� ���� ����� ������� ������ \n
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
	 * ������ �� ������� �����; � ������� �� ������������ ������ �������� ������ ����, ��� ������ ������ � �������, �.�. �� ��������� � ��������������
	 * @param te
	 */
	static public void printStackTrace(Throwable te) {
		StackTraceElement[] ste = te.getStackTrace();
		for (int i = 0; i < ste.length; i++)	{
			System.err.println(" 	" + ste[i].toString());
		}
	}
	
	/**
	 * ����� ������ ����� ������ � Notes-������ ����
	 * @param e - Exception, NotesException
	 * @param log - ActionLog, MailLog � �.�.
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
	 * ������������� mail-����;
	 * @param sLogCategory - ���������
	 * @param sUserName - ��� ������������ ��� ������, ������� ����� ����������� �������� ������
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
						throw new NotesException(ERRc1111, "openMailLog: �������� ��� ��������� vRecipients (" + vRecipients.getClass().getName() + "), ��������� String ��� Vector<String>");
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
	 * @param sLine - ������, ������� ����� ������������ � mail-����
	 * ���� ������� ���� ����� ��� ��������������� �������������, �� ����� ������ ��� � ����������� �� ���������:
	 * ��������� - ������� �����, ��� ������������ - ���������, ����������� �����;
	 * ���� ��������� ����������� �������� ���������� �������������, ����� ������ ����� ������������ ������ �������������
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
				// ������� ������������� �� ���������
				Agent agent = m_session.getAgentContext().getCurrentAgent();
				// ���� ����� �������� ����������� �������, - �� APP.Developers, ����� �� ���������� ������������ �����
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
