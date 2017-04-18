package com.fuib.lotus.utils;


import java.util.Iterator;

import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.ViewEntryCollection;
import lotus.domino.ViewNavigator;

public class LNIterator implements Iterator {
	public static final String VIEW = "View";
	public static final String VIEW_NAVIGATOR = "ViewNavigator";
	public static final String DOC_COLLECTION = "DocumentCollection";
	public static final String ENTRY_COLLECTION = "ViewEntryCollection";	
	
	protected lotus.domino.Base m_col = null;
	protected lotus.domino.Base m_curObj = null;
	protected lotus.domino.Base m_usedObj = null;
	protected String m_sClassName;
	protected boolean m_bIsFirst = true;
	protected boolean m_bIsRecycleCollection = false;

	
	public LNIterator(lotus.domino.Base col) 		{	this.create(col, false); }
	public LNIterator(lotus.domino.Base col, boolean bIsRecycleCollection) { 
		this.create(col, bIsRecycleCollection);
	}
	
	
	protected void create(lotus.domino.Base col, boolean bIsRecycleCollection) {
		m_bIsRecycleCollection = bIsRecycleCollection;
		m_col = col;
		m_sClassName = col.getClass().getName();
		m_sClassName = m_sClassName.substring(m_sClassName.lastIndexOf(".") + 1);		
	}
	
	
	public boolean hasNext() {		
		boolean bNextExist = false;
		
		try {
			if ( m_sClassName.equals(DOC_COLLECTION) )			bNextExist = hasNext_DocumentCollection();
			else if ( m_sClassName.equals(ENTRY_COLLECTION) )	bNextExist = hasNext_EntryCollection();
			else if ( m_sClassName.equals(VIEW) )				bNextExist = hasNext_View();
			else if ( m_sClassName.equals(VIEW_NAVIGATOR) )		bNextExist = hasNext_ViewNavigator();			
		} catch (NotesException e) {
			e.printStackTrace();
		}
		m_bIsFirst = false;
		
		if (!bNextExist && m_bIsRecycleCollection )		// end of collection was reached -  
			recycleObj(m_col);							// recycle corresponded object if requared
		
		return bNextExist;
	}

	public Object next() {		
		return m_curObj;
	}

	public void remove() {
		System.out.println("LNIterator.remove - NOT IMPLEMENTED!");
	}

	
	// --------------------------------------------- private realization hasNext for particular classes
	//--- DOC_COLLECTION
	protected boolean hasNext_DocumentCollection() throws NotesException {
		if (m_bIsFirst)		
			m_curObj = ((DocumentCollection)m_col).getFirstDocument();
		else {
			m_usedObj = m_curObj;
			m_curObj = ((DocumentCollection)m_col).getNextDocument((Document)m_curObj);
			recycleObj(m_usedObj);
		}
		
		return ( m_curObj != null );
	}
	
	
	//--- ENTRY_COLLECTION
	protected boolean hasNext_EntryCollection() throws NotesException {
		if (m_bIsFirst)		
			m_curObj = ((ViewEntryCollection)m_col).getFirstEntry();
		else {
			m_usedObj = m_curObj;
			m_curObj = ((ViewEntryCollection)m_col).getNextEntry();
			recycleObj(m_usedObj);
		}
		
		return ( m_curObj != null );
	}
	
	
	//--- VIEW_NAVIGATOR
	protected boolean hasNext_ViewNavigator() throws NotesException {
		if (m_bIsFirst)		
			m_curObj = ((ViewNavigator)m_col).getFirstDocument();
		else {
			m_usedObj = m_curObj;
			m_curObj = ((ViewNavigator)m_col).getNextDocument();
			recycleObj(m_usedObj);
		}
		
		return ( m_curObj != null );
	}
	
	
	//--- VIEW
	protected boolean hasNext_View() throws NotesException {
		if (m_bIsFirst)		
			m_curObj = ((View)m_col).getFirstDocument();
		else {
			m_usedObj = m_curObj;
			m_curObj = ((View)m_col).getNextDocument((Document)m_curObj);
			recycleObj(m_usedObj);
		}
		
		return ( m_curObj != null );
	}
	
	
// -------------------------------------------------------------------
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
	
	public void recycle() {
		recycleObj(m_usedObj);
		recycleObj(m_curObj);
		if ( m_bIsRecycleCollection )
			recycleObj(m_col);
	}
}
