package com.seerlogics.chatbot.statemachine;

import com.seerlogics.chatbot.util.ApplicationConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.web.context.WebApplicationContext;

import java.util.EnumSet;

/**
 * Created by bkane on 10/6/18.
 */
@Configuration
public class DeleteEventStateMachine extends UberStateMachine {
    public enum DeleteEventsStates implements UberStates {
        START_SEARCH_EVENTS, ZIP_PROVIDED, RENTER_ZIP, SEARCH_EVENTS, SE_QUIT
    }

    public enum DeleteEventsEvents implements UberEvents {
        USER_PROVIDES_ZIP, QUIT
    }

    static {
        currentStateToNextEvent.put(DeleteEventsStates.START_SEARCH_EVENTS, DeleteEventsEvents.USER_PROVIDES_ZIP);
        currentStateToNextEvent.put(DeleteEventsStates.RENTER_ZIP, DeleteEventsEvents.USER_PROVIDES_ZIP);
    }

    @Bean(name = "deleteEventSM")
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public StateMachine<DeleteEventsStates, DeleteEventsEvents> deleteEventSM() throws Exception {
        StateMachineBuilder.Builder<DeleteEventsStates, DeleteEventsEvents> builder = StateMachineBuilder.builder();

        builder.configureStates()
                .withStates()
                .initial(DeleteEventsStates.START_SEARCH_EVENTS)
                .choice(DeleteEventsStates.ZIP_PROVIDED)
                .end(DeleteEventsStates.SEARCH_EVENTS)
                .end(DeleteEventsStates.SE_QUIT)
                .states(EnumSet.allOf(DeleteEventsStates.class));

        builder.configureTransitions()
                .withExternal()
                .source(DeleteEventsStates.START_SEARCH_EVENTS).target(DeleteEventsStates.ZIP_PROVIDED)
                .event(DeleteEventsEvents.USER_PROVIDES_ZIP).action(zipProvided())
                .and()
                        // check if the zip provided is valid and set state based on the zip
                .withChoice()
                .source(DeleteEventsStates.ZIP_PROVIDED)
                .first(DeleteEventsStates.RENTER_ZIP, isInCorrectZipProvidedGuard(), null)
                .last(DeleteEventsStates.SEARCH_EVENTS, searchEventsAction())
                .and()
                        // if bad zip was provided, ask the user to enter again
                .withExternal().source(DeleteEventsStates.RENTER_ZIP).target(DeleteEventsStates.ZIP_PROVIDED)
                .event(DeleteEventsEvents.USER_PROVIDES_ZIP).action(zipProvided())
                .and()
                .withExternal().target(DeleteEventsStates.SE_QUIT).event(DeleteEventsEvents.QUIT);

        StateMachine<DeleteEventsStates, DeleteEventsEvents> stateMachine = builder.build();
        stateMachine.addStateListener(new StateMachineListener());
        return stateMachine;
    }

    public Action<DeleteEventsStates, DeleteEventsEvents> zipProvided() {
        return new Action<DeleteEventsStates, DeleteEventsEvents>() {
            @Override
            public void execute(StateContext<DeleteEventsStates, DeleteEventsEvents> context) {
                // search events here.
            }
        };
    }

    public Guard<DeleteEventsStates, DeleteEventsEvents> isInCorrectZipProvidedGuard() {
        return new Guard<DeleteEventsStates, DeleteEventsEvents>() {
            @Override
            public boolean evaluate(StateContext<DeleteEventsStates, DeleteEventsEvents> context) {
                String data = (String) context.getMessageHeaders().get("data");
                // https://www.oreilly.com/library/view/regular-expressions-cookbook/9781449327453/ch04s14.html
                return !data.matches("^[0-9]{5}(?:-[0-9]{4})?$");
            }
        };
    }

    public Action<DeleteEventsStates, DeleteEventsEvents> searchEventsAction() {
        return new Action<DeleteEventsStates, DeleteEventsEvents>() {
            @Override
            public void execute(StateContext<DeleteEventsStates, DeleteEventsEvents> context) {
                // stop the state machine after the events are searched for the supplied zip
                addExtendedStateAttribute(context, ApplicationConstants.STOP_STATE_MACHINE, true);
            }
        };
    }

    private void resetStateMachine(StateContext<DeleteEventsStates, DeleteEventsEvents> context,
                                   DeleteEventsStates resetToState, boolean copyState) {
        final DefaultStateMachineContext defaultStateMachineContext;
        if (copyState) {
            defaultStateMachineContext
                    = new DefaultStateMachineContext<>(resetToState, null, context.getMessageHeaders(), context.getExtendedState());
        } else {
            defaultStateMachineContext = new DefaultStateMachineContext<>(resetToState, null, null, null);
        }
        context.getStateMachine().getStateMachineAccessor()
                .doWithAllRegions(sma ->
                        sma.resetStateMachine(defaultStateMachineContext));
    }

    public class StateMachineListener extends StateMachineListenerAdapter<DeleteEventsStates, DeleteEventsEvents> {
        @Override
        public void stateContext(StateContext<DeleteEventsStates, DeleteEventsEvents> context) {
            String data = (String) context.getMessageHeaders().get("data");
            if (QUIT.equals(data.toLowerCase())) {
                addExtendedStateAttribute(context, ApplicationConstants.STOP_STATE_MACHINE, true);
                resetStateMachine(context, DeleteEventsStates.SE_QUIT, false);
            }
        }
    }
}
