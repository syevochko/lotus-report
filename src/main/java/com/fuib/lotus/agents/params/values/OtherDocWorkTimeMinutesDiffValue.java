package com.fuib.lotus.agents.params.values;

public class OtherDocWorkTimeMinutesDiffValue extends OtherDocParam {

    public OtherDocWorkTimeMinutesDiffValue(String value) {
        super(value);
    }

    @Override
    protected AbstractColumnValue getMyColumnObject() {
        return new WorkTimeDiffMinuteValue(partValue);
    }
}
