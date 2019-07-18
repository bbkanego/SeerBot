package com.seerlogics.chatbot.mutters;

import java.util.ArrayList;
import java.util.List;

public class Intent extends com.rabidgremlin.mutters.core.Intent {

    private com.seerlogics.commons.model.Intent dbIntent;

    public Intent(String name, com.seerlogics.commons.model.Intent dbIntent) {
        super(name);
        this.dbIntent = dbIntent;
    }

    public com.seerlogics.commons.model.Intent getDbIntent() {
        return dbIntent;
    }
}
