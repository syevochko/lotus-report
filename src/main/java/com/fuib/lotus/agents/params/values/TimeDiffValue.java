package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.LNEnvironment;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

public class TimeDiffValue extends AbstractBracesValue {
    public TimeDiffValue(String value, LNEnvironment environment) {
        super(value, environment);
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        String stateID = colValue;
        Vector tranLog = doc.getItemValue("%TransactionLog");

        Vector v = new Vector(1);
        v.add(1000);
        return v;
    }
}
