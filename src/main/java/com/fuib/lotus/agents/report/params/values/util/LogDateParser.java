package com.fuib.lotus.agents.report.params.values.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogDateParser {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT1 = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT2 = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    public static final SimpleDateFormat[] DATE_FORMATTERS = new SimpleDateFormat[]{SIMPLE_DATE_FORMAT1, SIMPLE_DATE_FORMAT2};

    public static Date parseDate(String sDate) {
        for (SimpleDateFormat sdf : DATE_FORMATTERS) {
            try {
                Date d = sdf.parse(sDate);
                return d;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
