package com.fuib.lotus.agents.report.params.values;

import com.fuib.lotus.agents.report.params.ParamDocColSet;
import com.fuib.lotus.agents.report.params.values.util.TimeDiffHelper;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

public class TimeDiffDaysValue extends TimeDiffMinuteValue {

    public TimeDiffDaysValue(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
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
