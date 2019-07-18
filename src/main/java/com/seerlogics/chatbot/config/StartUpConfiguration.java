package com.seerlogics.chatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by bkane on 3/29/19.
 * <p>
 * What is this class: This class will HOLD all the JVM properties that are supplied when the chatbot is started.
 * We can inject this compnent whereever those properties are needed.
 */
@Component
public class StartUpConfiguration {
    /**
     * This needs to provided as a Java arg like "--seerchat.allowedOrigins=http://localhost:4300,http://localhost:4320"
     */
    @Value("${seerchat.allowedOrigins}")
    private String allowedOrigins;

    /**
     * This needs to provided as a Java arg like "--seerchat.botPort=8099"
     */
    @Value("${seerchat.botPort}")
    private String botPort;

    /**
     * This needs to provided as a Java arg like "--seerchat.bottype=EVENT_BOT"
     */
    @Value("${seerchat.bottype}")
    private String botType;

    /**
     * This needs to provided as a Java arg like "--seerchat.botOwnerId=354243"
     */
    @Value("${seerchat.botOwnerId}")
    private String botOwnerId;

    @Value("${seerchat.trainedModelId}")
    private String trainedModelId;

    @Value("${seerchat.botId}")
    private String botId;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getBotPort() {
        return botPort;
    }

    public void setBotPort(String botPort) {
        this.botPort = botPort;
    }

    public String getTrainedModelId() {
        return trainedModelId;
    }

    public String getBotId() {
        return botId;
    }

    public String getBotType() {
        return botType;
    }

    public String getBotOwnerId() {
        return botOwnerId;
    }
}
