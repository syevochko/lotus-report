package com.fuib.lotus.utils.iterators;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.DbDirectory;
import lotus.domino.NotesException;

public class LNIteratorDbDirectory extends LNIteratorA {
	private DbDirectory m_dbDir;
	private Database m_dbCurrent;
	private Database m_dbTemp;
	private boolean m_bNext = false;
	
	public LNIteratorDbDirectory(DbDirectory dbDir, boolean bRecycle, int dbtype) {
		m_dbDir = dbDir;
		m_bRecycle = bRecycle;
		try {
			m_dbTemp = m_dbDir.getFirstDatabase(dbtype);
		}
		catch (NotesException e) {
			e.printStackTrace();
		}
	}
	
	public boolean hasNext() {
		try {
			if (m_bNext) {
				if (m_dbTemp == m_dbCurrent)
					m_dbTemp = m_dbDir.getNextDatabase();
			}
			else
				m_bNext = true;
		}
		catch (NotesException e) {
			e.printStackTrace();
		}
		return (m_dbTemp != null);
	}
	
	public Base next() {
		Tools.recycleObj(m_dbCurrent);
		if (hasNext()) {
			m_dbCurrent = m_dbTemp;
			m_position += 1;
		}
		return m_dbCurrent;
	}
	
	public void recycle() throws NotesException {
		Tools.recycleObj(m_dbTemp);
		Tools.recycleObj(m_dbCurrent);
		if (m_bRecycle) Tools.recycleObj(m_dbDir);
	}
	
}
