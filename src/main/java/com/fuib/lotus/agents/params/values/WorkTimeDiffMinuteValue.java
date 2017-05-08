package com.fuib.lotus.agents.params.values;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkTimeDiffMinuteValue extends TimeDiffMinuteValue {
    protected static final Pattern VALUES_PATTERN = Pattern.compile("state_id='([^']+)'\\s*(log_contains='([^']+)')??");

    protected final String stateID;
    protected final String logContainsStr;

    public WorkTimeDiffMinuteValue(String value) {
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
    protected long calculateDatesDifference(Date d1, Date d2) {
        try {
        	System.out.println(datesDiff == null);
            return (long)datesDiff.GetWorkTimeBetweenTwoDates(d1, d2, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
