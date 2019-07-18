package com.seerlogics.chatbot.statemachine;

import com.seerlogics.chatbot.util.ApplicationConstants;
import org.springframework.statemachine.StateContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bkane on 10/6/18.
 */
public class UberStateMachine {

    public static Map<UberStates, UberEvents> currentStateToNextEvent = new HashMap<>();

    public final static String YES = "yes";
    public final static String NO = "no";
    public final static String QUIT = "quit";

    public interface UberStates {
        String name();
    }

    public interface UberEvents {
        String name();
    }

    protected void addExtendedStateAttribute(StateContext context, String attributeKey, Object attributeValue) {
        context.getStateMachine().getExtendedState().getVariables().put(attributeKey, attributeValue);
    }

    protected void setStopStateMachineFlag(StateContext context) {
        addExtendedStateAttribute(context, ApplicationConstants.STOP_STATE_MACHINE, true);
    }

    protected Object getConversationAttribute(StateContext context, String attributeKey) {
        Map conversationAttributes = (Map) context.getExtendedState().getVariables()
                .get(ApplicationConstants.CONVERSATION_ATTRIBUTES);
        if (conversationAttributes != null) {
            return conversationAttributes.get(attributeKey);
        } else {
            return null;
        }
    }

    protected void addConversationAttribute(StateContext context, String attributeKey, Object attributeValue) {
        Map conversationAttributes = (Map) context.getExtendedState().getVariables()
                .get(ApplicationConstants.CONVERSATION_ATTRIBUTES);
        if (conversationAttributes != null) {
            conversationAttributes.put(attributeKey, attributeValue);
        }
    }
}
