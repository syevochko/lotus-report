package com.fuib.lotus.agents.params;

import com.fuib.lotus.agents.params.values.AbstractColumnValue;
import com.fuib.lotus.agents.params.values.FieldValue;
import com.fuib.lotus.agents.params.values.FormulaValue;
import com.fuib.lotus.agents.params.values.UndefineValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColumnValueFactory {
    private static final Pattern CLASS_PATTERN = Pattern.compile("#\\{([\\w.]+)\\s*(.*)\\}");

    public static AbstractColumnValue getColumnObject(String sColumnValue) {
        if (sColumnValue.startsWith("@")) {
            if (sColumnValue.length() > 1) {
                return new FormulaValue(sColumnValue);
            }

        } else if (sColumnValue.startsWith("#{")) {
            Matcher m = CLASS_PATTERN.matcher(sColumnValue);
            if (m.matches()) {
                try {
                    Class c = Class.forName("com.fuib.lotus.agents.params.values." + m.group(1));
                    Object obj = c.getConstructor(String.class).newInstance(m.group(2));
                    return (AbstractColumnValue) obj;

                } catch (Exception e) {
                    e.printStackTrace();
                    return new UndefineValue(sColumnValue + " " + e.getMessage());
                }
            }

        } else {
            return new FieldValue(sColumnValue);
        }

        return new UndefineValue(sColumnValue);
    }
}
