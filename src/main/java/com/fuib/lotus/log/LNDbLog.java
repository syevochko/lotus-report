package com.fuib.lotus.log;

import java.io.IOException;
import java.util.Vector;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;

public class LNDbLog extends LNLog implements lotus.domino.Base {
	// --- constants
	public static final String 		PROP_ICON = "cfdIcon";	
	public static final String 		PROP_EXPIRED = "fdnLogExpired";
	protected static final String 	LOG_FORM_SINGLE = "fmLogEntry";
	protected static final String 	LOG_FORM_APPEND = "fmTmpLogEntry";	
	protected int 					m_nLogType;				// m_nLogType = LOGTYPE_SINGLE | LOGTYPE_APPEND
		
	// --- variables	
	// LN variables
	protected Database		m_dbLog = null;
	protected Document		m_docLog = null;					// lotus document containing log
	protected RichTextItem	m_itemLog = null;					// item of lotus document containing log
	protected DateTime		m_dtNow = null;						// must be used for catching 'Now' date/time
	

	// --- constructors
	public LNDbLog(String sCategory) 					{ super(sCategory); }
	public LNDbLog(String sCategory, String sModule)	{ super(sCategory, sModule); }	
	
	
	// --- open log to LN database
	public boolean open(Database db, int nLogType) throws Exception {
		if ( !isLogOpened() ) {
			m_nLogType = nLogType;			
			
			if ( db == null )	throw new Exception("LNDbLog.open: Input parameter is null!");
			if ( !db.isOpen() )	throw new Exception("LNDbLog.open: Target database is not opened!");

			m_dbLog = db;		
			if ( m_dtNow == null )	m_dtNow = m_dbLog.getCreated();
			
			m_bIsLogOpened = (nLogType == LOGTYPE_SINGLE)?createDbLog():true;
		}
		
		return isLogOpened(); 
	}
		
	
	public  void close() throws IOException {
		if ( isLogOpened() ) {
			try {
				if (m_nLogType == LOGTYPE_SINGLE) {
					m_dtNow.setNow();
					m_docLog.replaceItemValue("fddEnd", m_dtNow);
				}

				if (m_docLog != null)	{
					try {
						m_docLog.save(true, true);
					} catch (NotesException ne)	{
						try {
							System.err.println(this.getClass().getName() + ".addErrorToLog - database <"+m_docLog.getParentDatabase().getFilePath()+"> - error while saving log: "+ ne.id + ", " + ne.text);
							m_docLog.getParentDatabase().getParent().evaluate("@MailSend('DHO.Admins';'APP.Developers';''; '['+@Name([Abbreviate]; @ServerName)+'] "+m_docLog.getParentDatabase().getFileName()+" - error while saving log: "+ne.text+"'; ''; ''; [PRIORITYHIGH])");
							Document docProfile = m_docLog.getParentDatabase().getProfileDocument("Configuration", "AdminUser");
							m_docLog.send(docProfile.getItemValue("fdLogDbAddr"));
							docProfile.recycle();
						} catch (NotesException e) {
							throw new IOException(ne.text + "{" + ne.id + "}");
						}
					}
				}					

			} catch (NotesException ne) {
				throw new IOException(ne.text + "{" + ne.id + "}");
			}

			super.close();

			recycle();
		}
	}

	
	protected boolean createDbLog() throws NotesException {		
		m_dtNow.setNow();

		if ( m_docLog == null )	m_docLog = m_dbLog.createDocument();

		m_docLog.replaceItemValue("fdCategory", m_sCategory);			// set category of log entry
		if ( m_sModule.length() > 0 )
			m_docLog.replaceItemValue("fdModule", m_sModule);			// set module name if exist
		m_docLog.replaceItemValue("fddStart", m_dtNow);					// set begin of logging
		m_docLog.replaceItemValue("cfdServer", m_dbLog.getServer());	// set current server name

		if ( hasProperty(PROP_ICON) )									// name of resource that expose a icon in notes document
			m_docLog.replaceItemValue(PROP_ICON, m_properties.get(PROP_ICON));

		if ( hasProperty(PROP_EXPIRED) )								// expired range (in days) after that log is expired
			m_docLog.replaceItemValue(PROP_EXPIRED, m_properties.get(PROP_EXPIRED));

		if ( m_nLogType == LOGTYPE_SINGLE ) {
			m_docLog.replaceItemValue("form", LOG_FORM_SINGLE);
			m_itemLog = m_docLog.createRichTextItem("fdrEvent");

			return ( m_itemLog != null );
		}
		
		m_docLog.replaceItemValue("form", LOG_FORM_APPEND);
		m_docLog.replaceItemValue("fdEvent", "");

		return true;

	}
		

	
	public void logError(int nErr, String sMsg) throws Exception {
		if ( isLogOpened() ) {
			super.logError(nErr, sMsg);
			
			if ( m_nLogType == LOGTYPE_SINGLE ) {
				m_docLog.replaceItemValue("fdnErr", new Integer(nErr));
				m_docLog.replaceItemValue("fdError", sMsg);
			}			
			
			close();			// auto-closing log entry after error
		}
	}
	
	
	public void recycle() {
		try {
			if ( m_itemLog != null )	{ m_itemLog.recycle(); m_itemLog = null; }
			if ( m_docLog != null )		{ m_docLog.recycle(); m_docLog = null; }
//			if ( m_dbLog != null )		{ m_dbLog.recycle(); m_dbLog = null; }
			if ( m_dtNow != null )		{ m_dtNow.recycle(); m_dtNow = null; }						
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public String toString() {
		if ( isLogOpened() ) {
			try {
				return ( m_itemLog != null )?m_itemLog.getText():super.toString();
			} catch (NotesException e) {
				return e.getMessage();
			}		
		}
		
		return "";
	}
	
	
	public void flush() throws IOException {
		if ( isLogOpened() ) {
			try {
				if ( m_nLogType == LOGTYPE_APPEND && m_docLog != null ) {
					try {
						m_docLog.save(true, true);
					} catch (NotesException ne)	{
						try {
							System.err.println(this.getClass().getName() + ".addErrorToLog - database <"+m_docLog.getParentDatabase().getFilePath()+"> - error while saving log: "+ ne.id + ", " + ne.text);
							m_docLog.getParentDatabase().getParent().evaluate("@MailSend('DHO.Admins';'APP.Developers';''; '['+@Name([Abbreviate]; @ServerName)+'] "+m_docLog.getParentDatabase().getFileName()+" - error while saving log: "+ne.text+"'; ''; ''; [PRIORITYHIGH])");
							Document docProfile = m_docLog.getParentDatabase().getProfileDocument("Configuration", "AdminUser");
							m_docLog.send(docProfile.getItemValue("fdLogDbAddr"));
							docProfile.recycle();
						} catch (NotesException e) {
							throw new IOException(ne.text + "{" + ne.id + "}");
						}
					}				
					
					m_docLog.recycle();
					m_docLog = null;			
				}
			} catch (NotesException ne) {				
					throw new IOException(ne.text + "{" + ne.id + "}");
			}
		}
	}
	
	
	public void write(char[] arg0, int arg1, int arg2) throws IOException {
		if ( isLogOpened() ) {
			String sText = new String(arg0, arg1, arg2);			

			try {
				if (m_nLogType == LOGTYPE_SINGLE) {
					m_itemLog.appendText(sText);
				} else {
					if (m_docLog == null)	createDbLog();
					
					if ( sText.endsWith("\n") )	sText = sText.substring(0, sText.length() - 1);					
					m_docLog.replaceItemValue("fdEvent", m_docLog.getItemValueString("fdEvent") + sText);
				}
			} catch (NotesException ne) {
				throw new IOException(ne.text + "{" + ne.id + "}");
			}
		}
	}
	
	public void recycle(Vector arg0) { }
}
