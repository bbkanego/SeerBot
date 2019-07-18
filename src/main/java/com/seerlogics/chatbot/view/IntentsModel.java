package com.seerlogics.chatbot.view;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * Created by bkane on 10/11/18.
 */
@Component
@Scope(scopeName = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class IntentsModel {
    private Map<String, List<String>> intentsToUtterance;

    public Map<String, List<String>> getIntentsToUtterance() {
        return intentsToUtterance;
    }

    public void setIntentsToUtterance(Map<String, List<String>> intentsToUtterance) {
        this.intentsToUtterance = intentsToUtterance;
    }
}
