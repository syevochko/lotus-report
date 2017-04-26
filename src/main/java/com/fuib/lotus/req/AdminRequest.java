package com.fuib.lotus.req;

import java.util.Date;
import java.util.Vector;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 *  ласс создани€ админ-запросов
 * @author shubniko
 * 
 * FIXME: без учЄта brca!
 */
public class AdminRequest implements lotus.domino.Base {
	private static final String IN_MODULE = "fdModule";
	private String m_sModule = null;
	
	public static final int PRIORITY_HIGH = 3;
	public static final int PRIORITY_MEDIUM = 2;
	public static final int PRIORITY_LOW = 1;
	private int m_iPriority = PRIORITY_MEDIUM;
	
	private Session m_session;
	private Database m_ndbAQ;
	private Document m_ndQuery;
	
	/**
	 * @param ndb - объект Ѕƒ "јдмин-запросы"
	 * @param dt - дата отработки запроса; если null, то будет подставлено текущее врем€
	 */
	public AdminRequest(Database ndb, DateTime dt) throws NotesException {
		m_ndbAQ = ndb;
		m_session = m_ndbAQ.getParent();
		
		m_ndQuery = m_ndbAQ.createDocument();
		m_ndQuery.replaceItemValue("Form", "AdminQuery").recycle();
		
		if (dt == null)
			dt = m_session.createDateTime(new Date(System.currentTimeMillis()));
		m_ndQuery.replaceItemValue("fdActionDT", dt);
	}
	
	/**
	 * ¬озможность установки имени модул€;
	 * 	если не задано, то будет использовано им€ текущего агента
	 */
	public void setModule(String sModuleName) throws NotesException {
		m_sModule = sModuleName;
	}
	
	/**
	 * ¬озможность задани€ приоритета выполнени€ операции;
	 * 	если не задано, то будет использовано PRIORITY_MEDIUM
	 * @param iPriority - использовать константы PRIORITY_HIGH, PRIORITY_LOW
	 */
	public void setPriority(int iPriority) throws NotesException {
		m_iPriority = iPriority;
	}
	
	/**
	 * «аписывает в поле с именем, переданным в sItemName, значение, переданное в vValue
	 */
	public void createField(String sItemName, Object vValue) throws NotesException {
		if (vValue != null) {
			Item item = m_ndQuery.replaceItemValue(sItemName, vValue);
			if (item.getValueLength() > 30000)
				item.setSummary(false);
			Tools.recycleObj(item);
		}
	}
	
	/**
	 * «аписывает в поле с именем "fd" + переданным в sItemName, значение, переданное в vValue
	 */
	public void setProperty(String sPropertyName, Object vValue) throws NotesException {
		createField("fd" + sPropertyName, vValue);
	}
	
	
	/**
	 * @param iTypeOperation - тип операции с полем:
	 * 		- 1    - replace
	 * 		- 0, 2 - append
	*/
	public void submitActionChangeField(String sTargetUNID, String sFieldName, Object vFieldValue, int iTypeOperation) throws Exception {
		setProperty("ActionType", "1");
		setProperty("TargetUNID", sTargetUNID);
		
		setProperty("TargetFieldName", sFieldName);
		
		if (iTypeOperation == 0) iTypeOperation = 2;
		setProperty("CFType", Integer.toString(iTypeOperation));
		
		createField("%$_" + sFieldName, vFieldValue);
		setProperty("ValueFieldName", "%$_" + sFieldName);
		
		submit();
	}
	
	/**
	 * «амена значени€ в поле по индексу
	*/
	public void submitActionChangeField(String sTargetUNID, String sFieldName, int iFieldIndex, Object vFieldValue) throws Exception {
		setProperty("ActionType", "1");
		setProperty("TargetUNID", sTargetUNID);
		
		setProperty("TargetFieldName", sFieldName);
		
		setProperty("CFType", "3");
		
		setProperty("TargetFieldIndex", iFieldIndex);
		setProperty("ValueFieldName", vFieldValue);
		
		submit();
	}
	
	/**
	 * ¬ыполнение @-формулы на документе
	*/
	public void submitActionFormula(String sTargetUNID, String sFormula) throws Exception {
		setProperty("ActionType", "2");
		setProperty("TargetUNID", sTargetUNID);
		
		setProperty("Formula", sFormula);
		
		submit();
	}
	
	/**
	 * ¬ыполнение LS
	*/
	public void submitActionScript(String sTargetUNID, String sScript) throws Exception {
		setProperty("ActionType", "3");
		setProperty("TargetUNID", sTargetUNID);
		
		setProperty("Script", sScript);
		
		submit();
	}
	
	
	private void submit() throws Exception {
		m_ndQuery.replaceItemValue("fdPriority", m_iPriority).recycle();
		m_ndQuery.replaceItemValue("DbSource", m_session.getCurrentDatabase().getFilePath());
		
		String sAgentName = "";
		// TODO: добавить получение имени web-сервиса
		try{
			sAgentName = m_session.getAgentContext().getCurrentAgent().getName();
		} catch (NotesException ne) {}
		m_ndQuery.replaceItemValue("AgentSource", sAgentName).recycle();
		if (m_sModule == null || m_sModule.isEmpty())
			m_sModule = sAgentName;
		createField(IN_MODULE, m_sModule);
		
		m_ndQuery.replaceItemValue("fdErrCount", new Integer(0)).recycle();
		m_ndQuery.replaceItemValue("fdServer", m_session.createName(m_ndbAQ.getServer()).getAbbreviated()).setSigned(true);
		m_ndQuery.replaceItemValue("Status", new Integer(1)).recycle();
		
		m_ndQuery.sign();
		Tools.save(m_ndQuery, false, true);
	}

	public void recycle() throws NotesException {
		Tools.recycleObj(m_ndQuery);
	}

	@SuppressWarnings("unchecked")
	public void recycle(Vector arg0) throws NotesException {
	}
	
}
