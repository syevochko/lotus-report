package com.fuib.lotus.utils;

import java.util.Date;
import java.util.Calendar;

import lotus.domino.DateTime;
import lotus.domino.NotesException;
import lotus.domino.Session;

public class JDate {
	
	
	static public DateTime toDateOnly(DateTime dt) {
		if (dt != null) {
			try {
				Session ns = dt.getParent();
				String sDate = dt.getDateOnly();
				Tools.recycleObj(dt);
				dt = ns.createDateTime(sDate);
				Tools.recycleObj(ns);
			}
			catch (NotesException e) {
				e.printStackTrace();
			}
		}
		return dt;
	}
	
	static public Date toDateOnly(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.HOUR, 0);
		return c.getTime();
	}
	
	
	static public int getDay(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.DAY_OF_MONTH);
	}
	
	static public int getMonth(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.MONTH) + 1;
	}
	
	static public int getYear(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.YEAR);
	}
	
	static public Date addDay(Date d, int nDays) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.DATE, nDays);  // number of days to add
		Date dNew = c.getTime();
		return dNew;
	}
	
	
}
