package com.fuib.lotus.agents.params.values;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

import java.util.Vector;

public class FormulaValue extends AbstractColumnValue {
    public FormulaValue(String value) {
        super(value);
    }

    public Vector getColumnValue(Document doc) throws NotesException {
        Session sess = doc.getParentDatabase().getParent();
        return sess.evaluate(colValue, doc);
    }
}
