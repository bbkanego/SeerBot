package com.seerlogics.chatbot.mutters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by bkane on 5/12/18.
 */
@Component
public class SeerBot {

    private SeerBotConfiguration seerBotConfiguration;

    @Autowired
    public SeerBot(SeerBotConfiguration configuration) {
        this.seerBotConfiguration = configuration;
    }

    public SeerBotConfiguration getSeerBotConfiguration() {
        return seerBotConfiguration;
    }
}
