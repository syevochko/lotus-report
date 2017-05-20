package com.fuib.lotus.agents.params.values;

import java.util.Date;

//#{WorkTimeDiffMinuteValue state_id='CRFUN_Created' log_contains='O=fuib'}
public class WorkTimeDiffMinuteValue extends TimeDiffMinuteValue {
    public WorkTimeDiffMinuteValue(String value) {
        super(value);
    }

    @Override
    protected long calculateDatesDifference(Date d1, Date d2) {
        try {
            return (long) datesDiff.GetWorkTimeBetweenTwoDates(d1, d2, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
