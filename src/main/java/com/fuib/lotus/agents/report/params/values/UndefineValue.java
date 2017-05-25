package com.fuib.lotus.agents.report.params.values;

import com.fuib.lotus.agents.report.params.ParamDocColSet;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

public class UndefineValue extends AbstractColumnValue {
    private static final String UNDEFINE_COL_VALUE = "Column value undefined: ";

    public UndefineValue(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        Vector v = new Vector(1);
        v.add(UNDEFINE_COL_VALUE + colValue);
        return v;
    }
}
