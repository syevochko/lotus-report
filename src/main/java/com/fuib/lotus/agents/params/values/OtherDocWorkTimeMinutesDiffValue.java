package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.agents.params.ParamDocColSet;

public class OtherDocWorkTimeMinutesDiffValue extends OtherDocParam {

    public OtherDocWorkTimeMinutesDiffValue(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
    }

    @Override
    protected AbstractColumnValue createNestedColumnObject() {
        return new WorkTimeDiffMinuteValue(partValue, getParent());
    }
}
