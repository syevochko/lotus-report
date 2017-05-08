package com.fuib.lotus.agents.params.values;

import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

public class FieldValue extends AbstractColumnValue {

    public FieldValue(String value) {
        super(value);
    }

    public Vector getColumnValue(Document doc) {
        try {
            if (doc.hasItem(colValue)) {
                return doc.getItemValue(colValue);
            }
        } catch (NotesException e) {
            e.printStackTrace();
        }
        return null;
    }
}