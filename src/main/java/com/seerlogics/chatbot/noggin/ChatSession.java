package com.seerlogics.chatbot.noggin;

import com.seerlogics.chatbot.exception.ConversationException;
import com.seerlogics.chatbot.model.chat.ChatData;
import com.seerlogics.chatbot.statemachine.DeleteEventStateMachine;
import com.seerlogics.chatbot.statemachine.SearchEventStateMachine;
import com.seerlogics.chatbot.statemachine.StateMachineHandler;
import com.seerlogics.chatbot.statemachine.UnlockAccountStateMachine;
import com.seerlogics.chatbot.util.ApplicationConstants;
import com.rabidgremlin.mutters.core.Context;
import com.rabidgremlin.mutters.core.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bkane on 5/4/18.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ChatSession extends Session {
    private final String NONE = "NONE";
    private String currentConversationId = NONE;
    private StateMachineHandler currentStateMachineHandler;
    private Map<String, Object> attributes = new HashMap<>();
    private Map<String, StateMachine> conversationToStateMachineMap = new HashMap<>();
    private List<String> currentChatContext = new ArrayList<>();
    private String currentSessionId;
    private ChatData previousChat;
    private Context context = new Context();
    // this can be used to pass the authentication key to the session such a JWT/oAuth token
    private String authCode;

    @Autowired
    @Qualifier("searchEventSM")
    private StateMachine<SearchEventStateMachine.SearchEventsStates,
                                    SearchEventStateMachine.SearchEventsEvents> searchEventSM;

    @Autowired
    @Qualifier("deleteEventSM")
    private StateMachine<DeleteEventStateMachine.DeleteEventsStates,
                                    DeleteEventStateMachine.DeleteEventsEvents> deleteEventSM;
    @Autowired
    @Qualifier("unlockAccountSM")
    private StateMachine<UnlockAccountStateMachine.UnlockAccountStates,
                                    UnlockAccountStateMachine.UnlockAccountEvents> unlockAccountSM;

    @PostConstruct
    private void init() throws Exception {
        conversationToStateMachineMap.put("SearchEventsNear", searchEventSM);
        conversationToStateMachineMap.put("DeleteEvents", deleteEventSM);
        conversationToStateMachineMap.put("HaveLogInIssues", unlockAccountSM);
    }

    public Object getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }

    public void setAttributes(String attributeName, Object attributeValue) {
        this.attributes.put(attributeName, attributeValue);
    }

    public void removeAttribute(String attributeName) {
        this.attributes.remove(attributeName);
    }

    public boolean isIntentConversationStarter(String intent) {
        return conversationToStateMachineMap.keySet().contains(intent);
    }

    public String getCurrentConversationId() {
        return currentConversationId;
    }

    public boolean isConversationActive() {
        return !NONE.equals(currentConversationId);
    }

    public StateMachineHandler getCurrentStateMachineHandler() {
        return currentStateMachineHandler;
    }

    public void endCurrentConversation() {
        this.currentConversationId = NONE;
        this.currentStateMachineHandler.stopStateMachine();
    }

    public void endCurrentConversationIfEndStateReached() {
        if (currentStateMachineHandler.isStopStateMachine()) {
            this.endCurrentConversation();
        }
    }

    public void startConversation(String triggerIntent) {
        if (!isConversationActive()) {
            this.currentConversationId = triggerIntent;
            this.currentStateMachineHandler = new StateMachineHandler(conversationToStateMachineMap.get(triggerIntent));
            this.currentStateMachineHandler.getVariables().put(ApplicationConstants.CONVERSATION_ATTRIBUTES, attributes);
        } else {
            throw new ConversationException("ERROR: There is an active convseration with ID = " + currentConversationId);
        }
    }

    public String decideNextResponseInConversation(ChatData chatRequest) {
        getCurrentStateMachineHandler().moveToNextState(chatRequest.getMessage());
        String responseKey = getCurrentStateMachineHandler().getCurrentState();
        endCurrentConversationIfEndStateReached();
        String chainedStateMachine = (String)attributes.get(ApplicationConstants.CHAINED_STATE_MACHINE);
        if (chainedStateMachine != null) {
            startConversation(chainedStateMachine);
            attributes.remove(ApplicationConstants.CHAINED_STATE_MACHINE);
            responseKey = getCurrentStateMachineHandler().getCurrentState();
        }
        return responseKey;
    }

    public void setCurrentSessionId(String currentSessionId) {
        this.currentSessionId = currentSessionId;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<String> getCurrentChatContext() {
        return currentChatContext;
    }

    public void setCurrentChatContext(List<String> currentChatContext) {
        this.currentChatContext = currentChatContext;
    }

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public ChatData getPreviousChat() {
        return previousChat;
    }

    public void setPreviousChat(ChatData previousChat) {
        this.previousChat = previousChat;
    }

    @Override
    public String toString() {
        return "ChatSession{" +
                "currentChatContext=" + currentChatContext +
                ", currentSessionId='" + currentSessionId + '\'' +
                ", previousChat=" + previousChat +
                '}';
    }
}
