package com.fuib.lotus.utils.iterators;

import lotus.domino.Base;
import lotus.domino.ViewEntry;
import lotus.domino.NotesException;
import lotus.domino.ViewEntryCollection;

public class LNIteratorViewEntryCollection extends LNIteratorEntryA {
	private ViewEntryCollection m_nvec = null;
	
	/**
	 * 
	 * @param nvec
	 * @param bRecycle
	 * @param bReturnDocument - если true (по умолчанию при вызове из LNIterator), то метод next() будет возвращать Document вместо ViewEntry
	 * @throws NotesException 
	 */
	public LNIteratorViewEntryCollection(ViewEntryCollection nvec, boolean bRecycle, boolean bReturnDocument) throws NotesException {
		m_nvec = nvec;
		m_lCount = m_nvec.getCount();
		m_bRecycle = bRecycle;
		m_bReturnDocument = bReturnDocument;
	}
	
	
	protected ViewEntry getFirstEntry() {
		try {
			return m_nvec.getFirstEntry();
		}
		catch (NotesException e) {}
		return null;
	}
	
	
	protected ViewEntry getNextEntry(ViewEntry entry) {
		try {
			return m_nvec.getNextEntry(entry);
		}
		catch (NotesException e) {}
		return null;
	}
	
	
	protected Base getInstance() {
		return m_nvec;
	}

}
