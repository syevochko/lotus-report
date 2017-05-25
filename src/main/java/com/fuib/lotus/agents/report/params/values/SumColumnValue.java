package com.fuib.lotus.agents.report.params.values;

import com.fuib.lotus.agents.report.params.ParamDocColSet;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

//#{SumColumnValue COL1,COL4,COL9,COL11}
public class SumColumnValue extends AbstractColumnValue {
    private String[] colsNameToSum;

    public SumColumnValue(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
        colsNameToSum = colValue.replaceAll("\\s", "").split("[,;+]");
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        if (colsNameToSum != null && colsNameToSum.length > 0) {
            Double sum = 0.0;
            for (String colName : colsNameToSum) {
                Vector v = getParent().getColumnValues().get(colName.toUpperCase());
                if (v != null && !v.isEmpty()) {
                    for (Object val : v) {
                        if (val instanceof Double)
                            sum = sum + (Double) val;
                        else if (val instanceof Integer)
                            sum = sum + (Integer) val;
                        else if (val instanceof Long)
                            sum = sum + (Long) val;
                        else if (val instanceof Byte)
                            sum = sum + (Byte) val;

                    }
                }
            }
            Vector ret = new Vector();
            ret.add(sum);
            return ret;
        }
        return null;
    }
}
