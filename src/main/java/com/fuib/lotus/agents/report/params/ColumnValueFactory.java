package com.fuib.lotus.agents.report.params;

import com.fuib.lotus.agents.report.params.values.AbstractColumnValue;
import com.fuib.lotus.agents.report.params.values.FieldValue;
import com.fuib.lotus.agents.report.params.values.FormulaValue;
import com.fuib.lotus.agents.report.params.values.UndefineValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColumnValueFactory {
    private static final Pattern CLASS_PATTERN = Pattern.compile("#\\{([\\w.]+)\\s*(.*)\\}");

    public static AbstractColumnValue getColumnObject(String sColumnValue, ParamDocColSet parent) {
        if (sColumnValue.startsWith("@")) {
            if (sColumnValue.length() > 1) {
                return new FormulaValue(sColumnValue, parent);
            }

        } else if (sColumnValue.startsWith("#{")) {
            Matcher m = CLASS_PATTERN.matcher(sColumnValue);
            if (m.matches()) {
                try {
                    Class c = Class.forName("com.fuib.lotus.agents.params.values." + m.group(1));
                    Object obj = c.getConstructor(String.class, ParamDocColSet.class).newInstance(m.group(2), parent);
                    return (AbstractColumnValue) obj;

                } catch (Exception e) {
                    e.printStackTrace();
                    return new UndefineValue(sColumnValue + " " + e.getMessage(), parent);
                }
            }

        } else {
            return new FieldValue(sColumnValue, parent);
        }

        return new UndefineValue(sColumnValue, parent);
    }
}
