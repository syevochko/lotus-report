package com.fuib.lotus.agents.params.values;

import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

public class UndefineValue extends AbstractColumnValue {
    private static final String UNDEFINE_COL_VALUE = "Column value undefined: ";

    public UndefineValue(String value) {
        super(value);
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        Vector v = new Vector(1);
        v.add(UNDEFINE_COL_VALUE + colValue);
        return v;
    }
}
