package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.LNEnvironment;

public abstract class AbstractBracesValue extends AbstractColumnValue {
    protected final LNEnvironment env;

    public AbstractBracesValue(String value, LNEnvironment environment) {
        super(value);
        env = environment;
    }

    public LNEnvironment getEnv() {
        return env;
    }
}
