package com.fuib.lotus.agents.report.params.values;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.agents.report.params.ParamDocColSet;
import com.fuib.util.WorkTimeBetweenTwoDates;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Database;

import java.util.List;
import java.util.Vector;

public abstract class AbstractColumnValue {
    protected final String colValue;
    protected LNEnvironment env;
    protected WorkTimeBetweenTwoDates datesDiff;
    protected List<Database> targetDatabases;
    protected ParamDocColSet parent;

    public AbstractColumnValue(String value, ParamDocColSet parent) {
        colValue = value;
        this.parent = parent;
    }

    public ParamDocColSet getParent() {
        return parent;
    }

    public abstract Vector getColumnValue(Document doc) throws NotesException;

    public void setTargetDatabases(List<Database> dbs)	{
    	targetDatabases = dbs;
    }
    
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
}
