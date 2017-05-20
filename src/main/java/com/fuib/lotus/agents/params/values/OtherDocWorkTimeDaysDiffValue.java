package com.fuib.lotus.agents.params.values;

public class OtherDocWorkTimeDaysDiffValue extends OtherDocParam {

    public OtherDocWorkTimeDaysDiffValue(String value) {
        super(value);
    }

    @Override
    protected AbstractColumnValue getMyColumnObject() {
        return new WorkTimeDiffDaysValue(partValue);
    }
}
