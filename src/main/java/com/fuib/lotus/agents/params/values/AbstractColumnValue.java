package com.fuib.lotus.agents.params.values;

import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

public abstract class AbstractColumnValue {
    protected final String colValue;

    public AbstractColumnValue(String value) {
        colValue = value;
    }

    public abstract Vector getColumnValue(Document doc) throws NotesException;
}
