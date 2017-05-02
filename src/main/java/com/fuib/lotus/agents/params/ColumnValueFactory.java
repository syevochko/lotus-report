package com.fuib.lotus.agents.params;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.agents.params.values.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColumnValueFactory {
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\{(\\w+)\\}(.*)");

    public static AbstractColumnValue getColumnObject(String sColumnValue, LNEnvironment env)  {
        if (sColumnValue.startsWith("@"))   {
            if (sColumnValue.length() > 1)  {
                return new FormulaValue(sColumnValue);
            }

        } else  if (sColumnValue.startsWith("{"))   {
            Matcher m = CLASS_PATTERN.matcher(sColumnValue);
            if (m.matches())    {
                try {
                    Class c = Class.forName(m.group(1));
                    Object obj = c.getConstructor(String.class, LNEnvironment.class).newInstance(m.group(2), env);
                    return (AbstractBracesValue) obj;

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

    public static void main(String[] args) {
        Matcher m = CLASS_PATTERN.matcher("{12121}2222");
        System.out.println(m.matches());
        System.out.println(m.group(0));
        System.out.println(m.group(1));
        System.out.println(">>> " + m.group(2));
    }
}
