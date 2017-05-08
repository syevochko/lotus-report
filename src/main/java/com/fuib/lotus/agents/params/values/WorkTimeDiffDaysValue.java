package com.fuib.lotus.agents.params.values;

import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

public class WorkTimeDiffDaysValue extends WorkTimeDiffMinuteValue {

    public WorkTimeDiffDaysValue(String value) {
        super(value);
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        Vector v = super.getColumnValue(doc);
        double workDays = ((Integer) v.get(0)) / 480;    // 60 min per 1 hour and 8 working hours a day
        Vector<Double> vr = new Vector<Double>(1);
        vr.add(workDays);
        return vr;
    }

}
