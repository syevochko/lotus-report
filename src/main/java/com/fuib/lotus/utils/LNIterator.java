package com.fuib.lotus.utils;

import java.util.Vector;

import lotus.domino.Base;
import lotus.domino.DbDirectory;
import lotus.domino.NotesException;
import lotus.domino.DocumentCollection;
import lotus.domino.View;
import lotus.domino.ViewEntryCollection;
import lotus.domino.ViewNavigator;

import com.fuib.lotus.utils.iterators.LNIteratorA;
import com.fuib.lotus.utils.iterators.LNIteratorDbDirectory;
import com.fuib.lotus.utils.iterators.LNIteratorDocumentCollection;
import com.fuib.lotus.utils.iterators.LNIteratorVector;
import com.fuib.lotus.utils.iterators.LNIteratorViewEntryCollection;
import com.fuib.lotus.utils.iterators.LNIteratorViewNavigator;

public class LNIterator extends LNIteratorA {
	private LNIteratorA m_LNIterator = null;
	
	
	public LNIterator(DocumentCollection ndc, boolean bRecycle) throws NotesException {
		checkInstanceParameter(ndc);
		m_LNIterator = new LNIteratorDocumentCollection(ndc, bRecycle);
	}
	
	public LNIterator(ViewEntryCollection nvec, boolean bRecycle, boolean bReturnDocument) throws NotesException {
		checkInstanceParameter(nvec);
		m_LNIterator = new LNIteratorViewEntryCollection(nvec, bRecycle, bReturnDocument);
	}
	
	public LNIterator(View view, boolean bReturnDocument) throws NotesException {
		checkInstanceParameter(view);
		// переключаемся на ViewEntryCollection, т.к. удаление документа из папки разрушает внутренний итератор NotesView,
		// и исправить это другим способом невозможно; причём удаление документа из базы не разрушает его...
		m_LNIterator = new LNIteratorViewEntryCollection(view.getAllEntries(), true, bReturnDocument);
	}
	
	public LNIterator(ViewNavigator nav, boolean bRecycle, boolean bReturnDocument) throws NotesException {
		checkInstanceParameter(nav);
		m_LNIterator = new LNIteratorViewNavigator(nav, bRecycle, bReturnDocument);
	}
	
	public LNIterator(DbDirectory dbDir, boolean bRecycle, int dbtype) throws NotesException {
		checkInstanceParameter(dbDir);
		m_LNIterator = new LNIteratorDbDirectory(dbDir, bRecycle, dbtype);
	}
	
	public LNIterator(Vector<Base> vct) throws NotesException {
		checkInstanceParameter(vct);
		m_LNIterator = new LNIteratorVector(vct);
	}
	
	
	private void checkInstanceParameter(Object instance) throws NotesException {
		if (instance == null)
			throw new NotesException(1111, "Некорректная передача параметра: объект для итератора = null");
	}
	
	
	public boolean hasNext() {
		return m_LNIterator.hasNext();
	}

	public Base next() {
		return m_LNIterator.next();
	}
	
	
	public String getObjectClassName() {
		String sClassName = m_LNIterator.getClass().getName();
		return sClassName.substring(sClassName.lastIndexOf(".") + 1);
	}
	
	
	public void recycle() throws NotesException {
		m_LNIterator.recycle();
	}

}
