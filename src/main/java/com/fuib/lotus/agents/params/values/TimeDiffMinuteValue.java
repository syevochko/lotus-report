package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.agents.params.values.util.LogDateParser;
import com.fuib.lotus.agents.params.values.util.LogTokenizer;
import com.fuib.lotus.agents.params.values.util.TimeDiffHelper;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeDiffMinuteValue extends AbstractColumnValue {
    private static final Pattern VALUES_PATTERN = Pattern.compile("state_id='([^']+)'\\s*(log_contains='([^']+)')??");

    private final String stateID;
    private final String logContainsStr;

    public TimeDiffMinuteValue(String value) {
        super(value);
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
        long totalMinutesAmount = 0;
        Vector<String> tranLog = doc.getItemValue("%TransactionLog");
        for (int i = 0; i < tranLog.size(); i++) {
            String nextRec = (i + 1 != tranLog.size()) ? tranLog.get(i + 1) : null;
            System.out.println(tranLog.get(i) + "\n" + nextRec);
            totalMinutesAmount = totalMinutesAmount + getWorkingTimeDifference(tranLog.get(i), nextRec);
        }
        Vector v = new Vector(1);
        v.add(totalMinutesAmount);
        return v;
    }

    protected long calculateDatesDifference(Date d1, Date d2) {
        return TimeDiffHelper.calculateTimeInMinutes(d1, d2);
    }

    protected long getWorkingTimeDifference(String curRec, String nextRec) {
        LogTokenizer curLogTokenizer = new LogTokenizer(curRec);
        long workMinutes = 0;

        if (stateID.equalsIgnoreCase(curLogTokenizer.getStateID()) && (logContainsStr.length() == 0 || curRec.contains(logContainsStr))) {
            Date d1 = LogDateParser.parseDate(curLogTokenizer.getDateTime());
            if (d1 != null) {
                try {
                    if (nextRec != null && nextRec.length() > 0) {
                        LogTokenizer nextLogTokenizer = new LogTokenizer(nextRec);
                        Date d2 = LogDateParser.parseDate(nextLogTokenizer.getDateTime());
                        if (d2 != null) {
                            workMinutes = calculateDatesDifference(d1, d2);
                        }
                    } else {
                        workMinutes = calculateDatesDifference(d1, Calendar.getInstance().getTime());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return workMinutes;
    }
}
