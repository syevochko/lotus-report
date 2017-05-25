package com.fuib.lotus.agents.report.params.values.util;

import java.util.Date;

public class TimeDiffHelper {
    public static final int MINUTES_PER_WORKING_DAY = 480;  // 60 min per 1 hour and 8 working hours a day

    public static long calculateTimeInMinutes(long startTime, long endTime) {
        return Math.abs(endTime - startTime) / (1000 * 60);
    }

    public static long calculateTimeInMinutes(Date startTime, Date endTime) {
        return calculateTimeInMinutes(startTime.getTime(), endTime.getTime());
    }

    public static long calculateTimeInHours(long startTime, long endTime) {
        return calculateTimeInMinutes(startTime, endTime) / 60;
    }

    public static long calculateTimeInHours(Date startTime, Date endTime) {
        return calculateTimeInHours(startTime.getTime(), endTime.getTime());
    }
}
