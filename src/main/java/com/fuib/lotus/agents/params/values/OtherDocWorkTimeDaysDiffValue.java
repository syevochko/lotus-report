package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.agents.params.ParamDocColSet;

public class OtherDocWorkTimeDaysDiffValue extends OtherDocParam {

    public OtherDocWorkTimeDaysDiffValue(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
    }

    @Override
    protected AbstractColumnValue createNestedColumnObject() {
        return new WorkTimeDiffDaysValue(partValue, getParent());
    }
}
