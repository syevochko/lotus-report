/* Information(attributes) about particular employeee
  Information from fuibmain.nsf is used
*/

package com.fuib.lotus;

import lotus.domino.*;

import java.util.Vector;


public class EmployeeInfo implements lotus.domino.Base { 
	private String m_sSourceDb = null; 
	private Document m_docEmployee;
	private LNEnvironment m_env = null;
	private boolean m_bIsExtEnv = false;
	
	
	public EmployeeInfo(Session session, String sRefPath) throws NotesException {
		// sRefPath - unused, retained for compatibility
		m_env = new LNEnvironment(session);	 
	}
	
	
	public EmployeeInfo(LNEnvironment env) {
		m_env = env;
		m_bIsExtEnv = true;
	}
	
	
	public EmployeeInfo(LNEnvironment env, String sSourceDb) {
		m_env = env;
		m_sSourceDb = sSourceDb;
		m_bIsExtEnv = true;
	}
	
	
	
	public boolean findEmployeeByID(long nID) throws NotesException {
		if (m_docEmployee!=null)	{ m_docEmployee.recycle(); m_docEmployee = null; }
		m_docEmployee = m_env.getDbView(getSourceDb(), "(GSEmployeeIDs)").getDocumentByKey(new Long(nID), true);		
		return ( m_docEmployee != null );
	}

	
	public boolean findEmployeeByNotes(String sNotesName) throws NotesException {
		if (m_docEmployee!=null)	{ m_docEmployee.recycle(); m_docEmployee = null; }
		m_docEmployee = m_env.getDbView(getSourceDb(), "(PLUsers)").getDocumentByKey(sNotesName, true);		
		return ( m_docEmployee != null );
	}
	
	
	public boolean findEmployeeByLogin(String sLogin) throws NotesException {
		if (m_docEmployee!=null)	{ m_docEmployee.recycle(); m_docEmployee = null; }
		m_docEmployee = m_env.getDbView(getSourceDb(), "(GSEmployeesByLogin)").getDocumentByKey(sLogin, true);	
		return ( m_docEmployee != null );
	}

	
	public Object getEmployeeAttribute(String sAttrName) throws NotesException {	
		if ( m_docEmployee != null ) {			
			Vector v = m_docEmployee.getItemValue(sAttrName);
			
			return ( !v.isEmpty() )?					
					( ( v.size() > 1 )? v : v.firstElement() ):
					null;
		}
		
		return null;
	}	

	
	private Database getSourceDb() throws NotesException {
		return ( m_sSourceDb != null ) ? m_env.getDatabase(m_sSourceDb) : m_env.getFUIBMainDB();		
	}
		

	public void recycle() {
		if ( m_env != null && !m_bIsExtEnv )	m_env.recycle();
		if ( m_docEmployee != null )
			try { m_docEmployee.recycle();
			} catch (NotesException e) { e.printStackTrace(); }
	}


	public void recycle(Vector arg0) { }
		
}
