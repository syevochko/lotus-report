package com.fuib.lotus.agents.report.params.values;

import com.fuib.lotus.agents.report.params.ParamDocColSet;
import com.fuib.lotus.agents.report.params.values.util.LogDateParser;
import com.fuib.lotus.agents.report.params.values.util.LogTokenizer;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeDiffValue extends AbstractColumnValue {
    private static final Pattern VALUES_PATTERN = Pattern.compile("state_id='([^']+)'\\s*(log_contains='([^']+)')??");

    private final String stateID;
    private final String logContainsStr;

    public TimeDiffValue(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
        Matcher m = VALUES_PATTERN.matcher(colValue);
        if (m.matches()) {
            stateID = m.group(1);
            String logContains = m.group(3);
            logContainsStr = (logContains != null) ? logContains : "";
        } else {
            stateID = colValue;
            logContainsStr = "";
        }
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        int totalMinutesAmount = 0;
        Vector<String> tranLog = doc.getItemValue("%TransactionLog");
        for (int i = 0; i < tranLog.size(); i++) {
            String nextRec = (i + 1 != tranLog.size()) ? tranLog.get(i + 1) : null;
            totalMinutesAmount = totalMinutesAmount + getWorkingMinutes(tranLog.get(i), nextRec);
        }
        Vector v = new Vector(1);
        v.add(totalMinutesAmount);
        return v;
    }

    private int getWorkingMinutes(String curRec, String nextRec) {
        LogTokenizer curLogTokenizer = new LogTokenizer(curRec);
        int workMinutes = 0;

        if (stateID.equalsIgnoreCase(curLogTokenizer.getStateID()) && (logContainsStr.length() == 0 || curRec.contains(logContainsStr))) {
            Date d1 = LogDateParser.parseDate(curLogTokenizer.getDateTime());
            if (d1 != null) {
                try {
                    if (nextRec != null && nextRec.length() > 0) {
                        LogTokenizer nextLogTokenizer = new LogTokenizer(nextRec);
                        Date d2 = LogDateParser.parseDate(nextLogTokenizer.getDateTime());
                        if (d2 != null) {
                            workMinutes = datesDiff.GetWorkTimeBetweenTwoDates(d1, d2, true);
                        }
                    } else {
                        workMinutes = datesDiff.GetWorkTimeBetweenTwoDates(d1, Calendar.getInstance().getTime(), true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return workMinutes;
    }
}
