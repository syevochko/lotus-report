package com.fuib.lotus.utils.iterators;

import lotus.domino.Base;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.DocumentCollection;

public class LNIteratorDocumentCollection extends LNIteratorDocumentA {
	private DocumentCollection m_ndc = null;
	
	
	public LNIteratorDocumentCollection(DocumentCollection ndc, boolean bRecycle) throws NotesException {
		m_ndc = ndc;
		m_lCount = m_ndc.getCount();
		m_bRecycle = bRecycle;
	}
	
	
	protected Document getFirstDocument() {
		try {
			return m_ndc.getFirstDocument();
		}
		catch (NotesException e) {}
		return null;
	}

	protected Document getNextDocument(Document document) {
		try {
			return m_ndc.getNextDocument(document);
		}
		catch (NotesException e) {}
		return null;
	}
	
	
	protected Base getInstance() {
		return m_ndc;
	}
	
}
