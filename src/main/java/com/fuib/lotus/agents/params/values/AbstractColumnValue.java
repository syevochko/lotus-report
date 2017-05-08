package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.LNEnvironment;
import com.fuib.util.WorkTimeBetweenTwoDates;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Map;
import java.util.Vector;

public abstract class AbstractColumnValue {
    protected final String colValue;
    protected LNEnvironment env;
    protected WorkTimeBetweenTwoDates datesDiff;
    protected Map<String, lotus.domino.Base> baseMap;

    public AbstractColumnValue(String value) {
        colValue = value;
    }

    public abstract Vector getColumnValue(Document doc) throws NotesException;

    public LNEnvironment getEnv() {
        return env;
    }

    public void setEnv(LNEnvironment env) {
        this.env = env;
    }

    public WorkTimeBetweenTwoDates getDatesDiff() {
        return datesDiff;
    }

    public void setDatesDiff(WorkTimeBetweenTwoDates datesDiff) {
        this.datesDiff = datesDiff;
    }

    public Map<String, lotus.domino.Base> getBaseMap() {
        return baseMap;
    }

    public void setBaseMap(Map<String, lotus.domino.Base> baseMap) {
        this.baseMap = baseMap;
    }
}
