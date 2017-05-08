package com.fuib.lotus.agents.params.values;

import java.util.Vector;

import lotus.domino.Document;
import lotus.domino.NotesException;

public class TimeDiffDaysValue extends TimeDiffMinuteValue {

	public TimeDiffDaysValue(String value) {
		super(value);
	}

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
    	Vector v = super.getColumnValue(doc);
    	long workDays = ((Long)v.get(0)) / 480;	// 60 min per 1 hour and 8 working hours a day
    	Vector<Long> vr = new Vector<Long>(1);
    	vr.add(workDays);
        return vr;
    }
	
}
