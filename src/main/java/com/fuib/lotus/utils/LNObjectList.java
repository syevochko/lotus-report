package com.fuib.lotus.utils;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import lotus.domino.NotesException;

public class LNObjectList extends Hashtable implements lotus.domino.Base {
	private static final long serialVersionUID = 1L;

	public Object put(Object key, lotus.domino.Base obj) { return super.put(key, obj); }
	public Object put(lotus.domino.Base obj) { return super.put(new Long(obj.hashCode()), obj); }
	public Object put(Object key, Object obj) {
		System.out.println("Not should be used. Use put(Object key, lotus.domino.Base obj) insted ...");
		return null;
	}
		
	public void recycle() {
		lotus.domino.Base obj = null;
		try {
			for (Enumeration col = this.elements(); col.hasMoreElements(); ) {
				obj = (lotus.domino.Base)col.nextElement();
				if ( obj != null )	obj.recycle();	
			}
		} catch (NotesException e) {
			e.printStackTrace();
		}
		clear();
	}
	
	public void recycle(Vector arg0) {}
}