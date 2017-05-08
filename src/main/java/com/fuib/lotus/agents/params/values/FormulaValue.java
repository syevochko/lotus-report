package com.fuib.lotus.agents.params.values;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

import java.util.Vector;

public class FormulaValue extends AbstractColumnValue {
	private final String formula;
	
    public FormulaValue(String value) {
        super(value);
        formula = colValue.substring(1, colValue.length());
    }

    public Vector getColumnValue(Document doc)  {
        Vector v;
		try {
			Session sess = doc.getParentDatabase().getParent();
			
			v = sess.evaluate(formula, doc);
		} catch (NotesException e) {
			e.printStackTrace();
			v = new Vector();
			v.add(e.getMessage());
		}
        
        return v;
    }
}
