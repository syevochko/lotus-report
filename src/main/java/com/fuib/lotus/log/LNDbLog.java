package com.fuib.lotus.log;

import java.io.IOException;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;


public abstract class LNDbLog extends LNLog {
	// --- constants
	
	/**
  	 * Для создания класса LNDbLogSingle
  	 */
  	public static final int			LOGTYPE_SINGLE = 1;
  	/**
  	 * Для создания класса LNDbLogEntry; ранее называлась LOGTYPE_APPEND
  	 */
  	public static final int			LOGTYPE_ENTRY = 2;
	
	public static final String 		PROP_EXPIRED = "fdnLogExpired";
	/**
	 * Количество дней по умолчанию, через которое логи будут автоматически удалены
	 */
	public static final int			LOG_EXPIRED_DAYS = 30;
	
	// LN variables
	protected Database		m_dbLog = null;				// recycle БД не делаем, т.к. она может потом ещё использоваться в вызывающем коде!
	protected DateTime		m_dtNow = null;				// must be used for catching 'Now' date/time
	protected Document		m_docLog = null;			// lotus document containing log

	public LNDbLog(String sCategory, String sModule)	{ super(sCategory, sModule); }
	
	/**
	 * Основной метод, без задания времени жизни логов - будет использовано значение по умолчанию
	 */
	public boolean open(Database db) throws Exception {
		return open(db, LOG_EXPIRED_DAYS);
	}
	
	/**
	 * С заданием времени жизни логов
	 */
	public boolean open(Database db, int nDaysLogExpired) throws Exception {
		if (!isLogOpened()) {
			if (db == null) throw new Exception("LNDbLog.open: Input parameter is null!");
			if (!db.isOpen()) throw new Exception("LNDbLog.open: Target database is not opened!");
			m_dbLog = db;
			
			if (m_dtNow == null) m_dtNow = m_dbLog.getCreated();
			
			setProperty(PROP_EXPIRED, nDaysLogExpired);
		}
		return true;
	}
	
	/**
	 * m_bIsLogOpened задаём только в этом методе классов-наследников
	 */
	protected boolean createLogDoc() throws NotesException {
		m_dtNow.setNow();
		
		if (m_docLog == null)
			m_docLog = m_dbLog.createDocument();
		
		m_docLog.replaceItemValue("fdCategory", m_sCategory).recycle();			// set category of log entry
		if (m_sModule.length() > 0)
			m_docLog.replaceItemValue("fdModule", m_sModule).recycle();			// set module name if exist
		m_docLog.replaceItemValue("fddStart", m_docLog.getCreated()).recycle();				// set begin of logging
		m_docLog.replaceItemValue("cfdServer", m_dbLog.getServer()).recycle();	// set current server name
		
		return true;
	}
	
	
	/**
	 * Сохраняет документ лога с уже вписанной порцией записей
	 */
	protected void save() throws IOException {
		if (isLogOpened()) {
			try {
				if (hasProperty(PROP_EXPIRED))								// expired range (in days) after that log is expired
					m_docLog.replaceItemValue(PROP_EXPIRED, m_properties.get(PROP_EXPIRED)).recycle();
				
				m_docLog.save(false);
			}
			catch (NotesException ne) {
				Document docProfile = null;
				try {
					System.err.println(LogEx.getErrInfo(ne, false));
					LogEx.sendErrorMemo(null, ne, m_docLog);
					docProfile = m_docLog.getParentDatabase().getProfileDocument("Configuration", "AdminUser");
					m_docLog.send(docProfile.getItemValue("fdLogDbAddr"));
				}
				catch (NotesException e) {
					throw new IOException(ne.text + "{" + ne.id + "}");
				}
				finally {
					Tools.recycleObj(docProfile);
				}
			}
		}
	}
	
	
	public void recycle() {
		Tools.recycleObj(m_docLog);
		Tools.recycleObj(m_dtNow);
	}
	
}
