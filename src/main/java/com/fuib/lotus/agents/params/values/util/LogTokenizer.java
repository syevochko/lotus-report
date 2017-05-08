package com.fuib.lotus.agents.params.values.util;

import java.util.Date;
import java.util.StringTokenizer;

public class LogTokenizer {
    private final String dateTime;
    private final String stateID;
    private final String editor;
    private final StringTokenizer tokenizer;

    public LogTokenizer(String logRecord) {
        tokenizer = new StringTokenizer(logRecord.replaceAll("\\s\\s", "#"), "#", false);
        dateTime = (String) tokenizer.nextElement();
        stateID = (String) tokenizer.nextElement();
        editor = (String) tokenizer.nextElement();
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getStateID() {
        return stateID;
    }

    public String getEditor() {
        return editor;
    }
}
