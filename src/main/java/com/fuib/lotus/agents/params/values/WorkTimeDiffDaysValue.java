package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.agents.params.values.util.TimeDiffHelper;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

//#{WorkTimeDiffDaysValue state_id='CRFUN_Created' log_contains='O=fuib'}
public class WorkTimeDiffDaysValue extends WorkTimeDiffMinuteValue {

    public WorkTimeDiffDaysValue(String value) {
        super(value);
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        Vector v = super.getColumnValue(doc);
        double val = ((Long) v.get(0)).doubleValue() / TimeDiffHelper.MINUTES_PER_WORKING_DAY;
        Vector<Double> v1 = new Vector<Double>();
        v1.add(val);
        return v1;
    }
}
