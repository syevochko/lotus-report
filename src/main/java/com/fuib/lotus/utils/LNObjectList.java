package com.fuib.lotus.utils;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class LNObjectList extends Hashtable<Object, Object> implements lotus.domino.Base {
	private static final long serialVersionUID = 1L;

	public Object put(Object key, lotus.domino.Base obj) { return super.put(key, obj); }
	public Object put(lotus.domino.Base obj) { return super.put(new Long(obj.hashCode()), obj); }
	public Object put(Object key, Object obj) {
		System.out.println("Not should be used. Use put(Object key, lotus.domino.Base obj) insted ...");
		return null;
	}
		
	public void recycle() {
		for (Enumeration<Object> col = this.elements(); col.hasMoreElements(); ) {
			Tools.recycleObj((lotus.domino.Base) col.nextElement());
		}
		clear();
	}
	
	@SuppressWarnings("unchecked")
	public void recycle(Vector arg0) {}
}