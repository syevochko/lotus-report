package com.fuib.lotus.utils.iterators;

import java.util.Vector;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Base;
import lotus.domino.NotesException;

public class LNIteratorVector extends LNIteratorA {
	private Vector<Base> m_vct;
	private Base m_oCurrent = null;
	
	public LNIteratorVector(Vector<Base> vct) {
		m_vct = vct;
	}
	
	public boolean hasNext() {
		return (m_vct.size() > m_position);
	}
	
	public Base next() {
		Tools.recycleObj(m_oCurrent);
		m_oCurrent = m_vct.get((int) m_position);
		m_position += 1;
		return m_oCurrent;
	}
	
	public void recycle() throws NotesException {
		Tools.recycleObj(m_oCurrent);
	}
	
}
