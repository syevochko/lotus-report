package com.fuib.lotus.log;

import java.io.IOException;

import lotus.domino.Database;

/*
 * Обертка-транслятор методов LNDbLog. Последний нельзя напрямую использовать в XPAGES
 *
 */
public class XSPLNDbLog {
	protected LNDbLog m_log = null;
	
	// --- constructors ---
	public XSPLNDbLog(String sLogCategory, String sModuleName, int iLogType) {
		if (iLogType == LNDbLog.LOGTYPE_ENTRY)
			m_log = new LNDbLogEntry(sLogCategory, sModuleName);
		else
			m_log = new LNDbLogSingle(sLogCategory, sModuleName);
	}
	
	// methods from LNDbLog
	public boolean open(Database db) throws Exception				 	{ return m_log.open(db); }
	public void close() throws IOException 								{ m_log.close(); }
	public void log(String sMsg) throws Exception 						{ m_log.log(sMsg); }
	public void logError(int nErr, String sMsg) throws Exception 		{ m_log.logError(nErr, sMsg); }
	public String toString() 											{ return m_log.toString(); }
	public void setProperty(Object sAttrName, Object vValue)			{ m_log.setProperty(sAttrName, vValue); }
	public Object getProperty(Object sAttrName)  						{ return m_log.getProperty(sAttrName); }
	public boolean hasProperty(Object sAttrName)						{ return m_log.hasProperty(sAttrName); }
	
	public LNDbLog _getLogObj()											{ return m_log; }
	
}
