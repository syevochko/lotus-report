package com.fuib.lotus.utils.iterators;

import java.util.Iterator;
import java.util.Vector;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Base;
import lotus.domino.NotesException;

public abstract class LNIteratorA implements lotus.domino.Base, Iterator<lotus.domino.Base> {
	protected long m_position = 0;
	protected boolean m_bRecycle = true;
	
	protected Base getObject() {
		return null;
	}
	
	protected void recycle(lotus.domino.Base obj) {
		Tools.recycleObj(obj);
	}
	
	public void remove() {
		System.err.println("LNIteratorA.remove() - NOT IMPLEMENTED!");
	}
	
	@SuppressWarnings("unchecked")
	public void recycle(Vector vct) throws NotesException {
		Tools.recycleObj(vct);
	}
}
