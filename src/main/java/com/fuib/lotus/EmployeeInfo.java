/* Information(attributes) about particular employeee
  Information from fuibmain.nsf is used
*/

package com.fuib.lotus;

import lotus.domino.*;

import java.util.Vector;

import com.fuib.lotus.utils.Tools;


public class EmployeeInfo implements lotus.domino.Base {
	final public static String ITEM_ID = "Employee_ID";
	final public static String ITEM_NOTESNAME = "Employee_Notes";
	final public static String ITEM_LOGIN = "Employee_Login";
	private String m_sSourceDb = null;
	private Document m_docEmployee;
	private LNEnvironment m_env = null;
	
	
	public EmployeeInfo(LNEnvironment env) {
		m_env = env;
	}
	
	public EmployeeInfo(LNEnvironment env, String sSourceDb) {
		m_env = env;
		m_sSourceDb = sSourceDb;
	}
	
	
	/**
	 * @return Возвращает документ, полученный с помощью методов findEmployeeBy...
	 */
	public Document document() {
		return m_docEmployee;
	}
	
	
	public boolean findEmployeeByID(long nID) throws NotesException {
		Tools.recycleObj(m_docEmployee);
		m_docEmployee = m_env.getView(getSourceDb(), "(GSEmployeeIDs)").getDocumentByKey(new Long(nID), true);
		return (m_docEmployee != null);
	}
	
	
	public boolean findEmployeeByNotes(String sNotesName) throws NotesException {
		Tools.recycleObj(m_docEmployee);
		m_docEmployee = m_env.getView(getSourceDb(), "(PLUsers)").getDocumentByKey(sNotesName, true);
		return (m_docEmployee != null);
	}
	
	
	public boolean findEmployeeByLogin(String sLogin) throws NotesException {
		Tools.recycleObj(m_docEmployee);
		m_docEmployee = m_env.getView(getSourceDb(), "(GSEmployeesByLogin)").getDocumentByKey(sLogin, true);
		return (m_docEmployee != null);
	}
	
	
	public Object getEmployeeAttribute(String sAttrName) throws NotesException {
		if (m_docEmployee != null) {
			Vector v = m_docEmployee.getItemValue(sAttrName);
			return (!v.isEmpty()) ?
					(( v.size() > 1) ? v : v.firstElement()):
					null;
		}
		return null;
	}
	
	public String getID() throws NotesException {
		String sValue = null;
		if (m_docEmployee != null && m_docEmployee.hasItem(ITEM_ID))
			sValue = m_docEmployee.getItemValueString(ITEM_ID);
		return sValue;
	}
	
	public String getNotesName() throws NotesException {
		String sValue = null;
		if (m_docEmployee != null && m_docEmployee.hasItem(ITEM_NOTESNAME))
			sValue = m_docEmployee.getItemValueString(ITEM_NOTESNAME);
		return sValue;
	}
	
	public String getLogin() throws NotesException {
		String sValue = null;
		if (m_docEmployee != null && m_docEmployee.hasItem(ITEM_LOGIN))
			sValue = m_docEmployee.getItemValueString(ITEM_LOGIN);
		return sValue;
	}
	
	private Database getSourceDb() throws NotesException {
		return (m_sSourceDb != null) ? m_env.getDatabase(m_sSourceDb) : m_env.getDbRef();		
	}
	
	
	public void recycle() {
		Tools.recycleObj(m_docEmployee);
	}
	
	
	public void recycle(Vector vct) {
		Tools.recycleObj(vct);
	}
	
}
