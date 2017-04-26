package com.fuib.lotus.utils.iterators;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Base;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.ViewEntry;

public abstract class LNIteratorEntryA extends LNIteratorA {
	private ViewEntry m_nveCurrent = null;
	private ViewEntry m_nveTemp = null;
	private Document m_ndEntry = null;
	protected boolean m_bReturnDocument;
	protected long m_lCount = 0;
	
	
	protected abstract ViewEntry getFirstEntry();
	
	protected abstract ViewEntry getNextEntry(ViewEntry entry);
	
	
	public boolean hasNext() {
		return (m_lCount > m_position);
	}
	
	public Base next() {
		if (m_position > 0) {
			recycleCurrentViewEntry();
			m_nveCurrent = m_nveTemp;
		}
		else
			m_nveCurrent = getFirstEntry();
		
		if (m_nveCurrent != null) {
			m_nveTemp = getNextEntry(m_nveCurrent);
			m_position += 1;
			
			if (m_bReturnDocument) {
				try {
					m_ndEntry = null;
					if (m_nveCurrent.isDocument() && m_nveCurrent.isValid()) {
						m_ndEntry = m_nveCurrent.getDocument();
						if (m_ndEntry != null)
							if (m_ndEntry.getItems().isEmpty()) m_ndEntry = null;
					}
					if (m_ndEntry == null)
						if (hasNext()) m_ndEntry = (Document) next();
				}
				catch (NotesException e) {
					System.err.println("next: ViewEntryCollection разрушен обновлением вида на позиции " + m_position + ", всего документов: " + m_lCount);
					m_position = m_lCount;		// устанавливаем верхний элемент, чтобы дальнейшие вызовы hasNext() вернули false
					return null;
				}
				return m_ndEntry;
			}
		}
		
		return m_nveCurrent;
	}
	
	
	/**
	 * Нужен для recycle()
	 * @return базовый объект для инициализированного класса: NotesDocumentCollection, NotesViewEntry и т.д.
	 */
	protected abstract Base getInstance();
	
	
	private void recycleCurrentViewEntry() {
		if (m_ndEntry != null)
			Tools.recycleObj(m_ndEntry);
		else {
			if (m_bReturnDocument)
				try {
					Tools.recycleObj(m_nveCurrent.getDocument());
				} catch (Exception e) {}
		}
		Tools.recycleObj(m_nveCurrent);
	}
	
	public void recycle() throws NotesException {
		recycleCurrentViewEntry();
		Tools.recycleObj(m_nveTemp);
		if (m_bRecycle) recycle(getInstance());
	}
}
