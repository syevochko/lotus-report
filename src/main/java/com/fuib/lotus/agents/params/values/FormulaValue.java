package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.agents.params.ParamDocColSet;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

import java.util.Vector;

//@@Word(@Trim(@Right(%EXECINFO; %Editor)); '##'; 2)
public class FormulaValue extends AbstractColumnValue {
    private final String formula;

    public FormulaValue(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
        formula = colValue.substring(1, colValue.length());
    }

    public Vector getColumnValue(Document doc) {
        return evaluateFormula(formula, doc);
    }

    public static Vector evaluateFormula(String formula, Document doc) {
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
