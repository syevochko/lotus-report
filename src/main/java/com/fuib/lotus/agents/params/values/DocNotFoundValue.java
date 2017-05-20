package com.fuib.lotus.agents.params.values;

import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

public class DocNotFoundValue extends AbstractColumnValue  {
    private static final String DOC_NOT_FOUND = "Linked document not found!";

    public DocNotFoundValue(String value) {
        super(value);
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        Vector v = new Vector(1);
        v.add("");
        return v;
    }
}
