package com.fuib.lotus;

import java.util.Vector;

import com.fuib.lotus.utils.LNObjectList;

import lotus.domino.AgentContext;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;

public class LNEnvironment implements lotus.domino.Base {
	final public static int ERR_CUSTOM = 1;
	final public static String FUIBCONFIG_DBNAME = "fuibconfig";
	final public static String FUIBCONFIG_LOOKUPVIEWNAME = "(lookupConfig)";
	
	protected LNObjectList m_storage = new LNObjectList(); 
	
	protected Session m_session = null;
	protected AgentContext m_agentContext = null;
	protected Database m_dbCurrent = null;

	
	public LNEnvironment(Session a_session) throws NotesException {
		m_session = a_session;								// no requarement to recycle this objects - 
		m_agentContext = m_session.getAgentContext();		// they will be recycled later by calling procedure or class object
		m_dbCurrent = m_agentContext.getCurrentDatabase();
	}
	
	public String getServer() throws NotesException 	{ return m_dbCurrent.getServer(); }
	public LNAddressBook getAB() throws NotesException	{ return new LNAddressBook(m_session); }
	public Session getSession()							{ return m_session; }
	
	
	public Database getDatabase(String sDbPath) throws NotesException {
		return getDatabase(null, sDbPath);
	}
	
	
	public Database getDatabase(String a_sServer, String sDbPath) throws NotesException {
		if ( sDbPath == null || sDbPath.length() == 0 )	return null;
		
		String sServer = ( a_sServer != null )?a_sServer:m_dbCurrent.getServer();
		String sKey = sServer + "!!" + sDbPath;		
		Database db = (Database)m_storage.get(sKey); 
		

		if ( db == null ) {
			db = m_session.getDatabase(sServer, sDbPath, false);
			if ( db != null )	
				m_storage.put(sKey, db); 			 
		}

		return db;
	}

	
	public Database getFUIBMainDB() throws NotesException {
		return getFUIBMainDB("Configuration", "AdminUser", "fdRefDatabaseName"); 
	}
		
	
	public Database getFUIBConfigDB() throws NotesException {
		if ( m_dbCurrent.getFileName().indexOf(FUIBCONFIG_DBNAME) != -1 )
			return m_dbCurrent;
		
		return getFUIBMainDB("Configuration", "AdminUser", "fdGlobalConfig"); 
	}
	
	/*
	 * get databade object from pointed profile from pointed item name (name of function is obsolete)  
	 */
	public Database getFUIBMainDB(String sProfileName, String sKey, String sItemName) throws NotesException {
		Document docProfile = null;
		
		try {
			docProfile = m_dbCurrent.getProfileDocument(sProfileName, sKey);
			if ( docProfile.hasItem(sItemName) ) {
				String sFUIBMainPath = docProfile.getItemValueString(sItemName);
				if ( sFUIBMainPath != null && sFUIBMainPath.length() > 0 ) return getDatabase(sFUIBMainPath);
			} else
				System.out.println("Не найден профайл '" + sProfileName + "(" + sKey + ")' в базе " + m_dbCurrent.getFilePath());
		} finally {
			recycleObj(docProfile);
		}
		
		return null;
	}
		
	
	public View getDbView(Database db, String sViewName) throws NotesException {
		if ( db == null || !db.isOpen() || sViewName == null || sViewName.length() == 0 )
			throw new NotesException(ERR_CUSTOM, "LNEnvironment.getView: Некорректные входные параметры (база == null или имя представления == null)"); 
		
		String sKey = db.getFilePath() + "!!" + sViewName;		
		View view = (View)m_storage.get(sKey); 

		if ( view == null ) {
			view = db.getView(sViewName);
			if ( view != null )	
				m_storage.put(sKey, view); 			 
		} else
			view.refresh();
		
		return view;
	}
	
	
	public void putObj(Object key, lotus.domino.Base data) 	{ m_storage.put(key, data); }
	public lotus.domino.Base getObj(Object key) 			{ return (lotus.domino.Base)m_storage.get(key); }
	
	
	
	public void recycle() {
		m_storage.recycle();
	}
	
	protected void recycleObj(lotus.domino.Base obj) {
		if ( obj != null ) { 
			try {
				obj.recycle();
			} catch (NotesException e) {
				e.printStackTrace();
			}
			
			obj = null;
		}
	}

	public void recycle(Vector arg0) { }
	
}
