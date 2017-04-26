package com.fuib.lotus.utils.iterators;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Base;
import lotus.domino.Document;

public abstract class LNIteratorDocumentA extends LNIteratorA {
	private Document m_ndCurrent = null;
	private Document m_ndTemp = null;
	protected long m_lCount = 0;
	
	
	protected abstract Document getFirstDocument();
	
	protected abstract Document getNextDocument(Document document);
	
	
	public boolean hasNext() {
		return (m_lCount > m_position);
	}

	public Base next() {
		if (m_position > 0) {
			Tools.recycleObj(m_ndCurrent);
			m_ndCurrent = m_ndTemp;
		}
		else
			m_ndCurrent = getFirstDocument();

		if (m_ndCurrent != null) {
			m_ndTemp = getNextDocument(m_ndCurrent);
			m_position += 1;
		}
		return m_ndCurrent;
	}
	
	
	/**
	 * Нужен для recycle()
	 * @return базовый объект для инициализированного класса: NotesDocumentCollection, NotesViewEntry и т.д.
	 */
	protected abstract Base getInstance();
	
	
	public void recycle() {
		Tools.recycleObj(m_ndTemp);
		Tools.recycleObj(m_ndCurrent);
		if (m_bRecycle) recycle(getInstance());
	}
	
}
