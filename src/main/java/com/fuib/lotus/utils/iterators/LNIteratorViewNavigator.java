package com.fuib.lotus.utils.iterators;

import lotus.domino.Base;
import lotus.domino.NotesException;
import lotus.domino.ViewEntry;
import lotus.domino.ViewNavigator;

public class LNIteratorViewNavigator extends LNIteratorEntryA {
	private ViewNavigator m_nav = null;
	
	
	public LNIteratorViewNavigator(ViewNavigator nav, boolean bRecycle, boolean bReturnDocument) {
		m_nav = nav;
		m_lCount = m_nav.getCount();
		m_bRecycle = bRecycle;
		m_bReturnDocument = bReturnDocument;
	}
	
	
	protected ViewEntry getFirstEntry() {
		try {
			return m_nav.getFirst();
		}
		catch (NotesException e) {}
		return null;
	}

	protected ViewEntry getNextEntry(ViewEntry entry) {
		try {
			return m_nav.getNext(entry);
		}
		catch (NotesException e) {}
		return null;
	}
	
	
	protected Base getInstance() {
		return m_nav;
	}
	
}
