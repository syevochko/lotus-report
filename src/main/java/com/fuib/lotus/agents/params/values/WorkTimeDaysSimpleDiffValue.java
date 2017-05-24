package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.agents.params.ParamDocColSet;
import com.fuib.lotus.agents.params.values.util.TimeDiffHelper;

import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

////#{WorkTimeDaysSimpleDiffValue d1='%Lastprocessed' d2='@Now'}
public class WorkTimeDaysSimpleDiffValue extends WorkTimeDiffMinuteValue {
    private static final Pattern VALUES_PATTERN = Pattern.compile("d1='([^']+)'\\s+d2='([^']+)'");

    protected final String d1Formula;
    protected final String d2Formula;

    public WorkTimeDaysSimpleDiffValue(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
        Matcher m = VALUES_PATTERN.matcher(colValue);
        if (m.matches()) {
            d1Formula = m.group(1);
            d2Formula = m.group(2);
        } else {
            d1Formula = null;
            d2Formula = null;
        }
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        Vector v = new Vector(1);
        if (d1Formula == null) {
            v.add("Can't parse formula: " + colValue);
        } else {
            Vector vd1 = FormulaValue.evaluateFormula(d1Formula, doc);
            Vector vd2 = FormulaValue.evaluateFormula(d2Formula, doc);
            double totalDaysAmount = ((Long)(calculateDatesDifference(((DateTime) vd1.get(0)).toJavaDate(), ((DateTime) vd2.get(0)).toJavaDate()))).doubleValue() / TimeDiffHelper.MINUTES_PER_WORKING_DAY;
            v.add(totalDaysAmount);
        }
        return v;
    }
}
