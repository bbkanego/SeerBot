package com.seerlogics.chatbot.statemachine;

import com.seerlogics.chatbot.util.ApplicationConstants;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;

import java.util.Map;

/**
 * Created by bkane on 10/6/18.
 * This class is a wrapper around the Spring State machines. This operates on the spring state machines
 */
public class StateMachineHandler {
    private StateMachine<UberStateMachine.UberStates, UberStateMachine.UberEvents> stateMachine;

    public StateMachineHandler(StateMachine<UberStateMachine.UberStates, UberStateMachine.UberEvents> stateMachine) {
        this.stateMachine = stateMachine;
        this.stateMachine.start();
    }

    public void moveToNextState(String data) {
        UberStateMachine.UberEvents matchingEvent =
                UberStateMachine.currentStateToNextEvent.get(stateMachine.getState().getId());

        Message<UberStateMachine.UberEvents> eventToFire =
                MessageBuilder.withPayload(matchingEvent).setHeader("data", data).build();
        this.stateMachine.sendEvent(eventToFire);
    }

    public void stopStateMachine() {
        this.stateMachine.getExtendedState().getVariables().clear();
        this.stateMachine.stop();
    }

    public Map<Object, Object> getVariables() {
        return this.stateMachine.getExtendedState().getVariables();
    }

    public boolean isStopStateMachine() {
        Object value = this.getVariables().get(ApplicationConstants.STOP_STATE_MACHINE);
        return value != null && (boolean) value;
    }

    public String getCurrentState() {
        return this.stateMachine.getState().getId().name();
    }
}
