package com.seerlogics.chatbot.statemachine;

import com.seerlogics.chatbot.service.ChatDataFetchService;
import com.seerlogics.chatbot.util.ApplicationConstants;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SearchEventStateMachine extends UberStateMachine {
    public enum SearchEventsStates implements UberStates {
        START_SEARCH_EVENTS, ZIP_PROVIDED, RENTER_ZIP, SEARCH_EVENTS, SE_QUIT
    }

    public enum SearchEventsEvents implements UberEvents {
        USER_PROVIDES_ZIP, QUIT
    }

    static {
        currentStateToNextEvent.put(SearchEventsStates.START_SEARCH_EVENTS, SearchEventsEvents.USER_PROVIDES_ZIP);
        currentStateToNextEvent.put(SearchEventsStates.RENTER_ZIP, SearchEventsEvents.USER_PROVIDES_ZIP);
    }

    @Autowired
    private ChatDataFetchService chatDataFetchService;

    @Bean(name = "searchEventSM")
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public StateMachine<SearchEventsStates, SearchEventsEvents> getSearchEventSM() throws Exception {
        StateMachineBuilder.Builder<SearchEventsStates, SearchEventsEvents> builder = StateMachineBuilder.builder();

        builder.configureStates()
                .withStates()
                .initial(SearchEventsStates.START_SEARCH_EVENTS)
                .choice(SearchEventsStates.ZIP_PROVIDED)
                .end(SearchEventsStates.SEARCH_EVENTS)
                .end(SearchEventsStates.SE_QUIT)
                .states(EnumSet.allOf(SearchEventsStates.class));

        builder.configureTransitions()
                .withExternal()
                .source(SearchEventsStates.START_SEARCH_EVENTS).target(SearchEventsStates.ZIP_PROVIDED)
                .event(SearchEventsEvents.USER_PROVIDES_ZIP).action(zipProvided())
                .and()
                        // check if the zip provided is valid and set state based on the zip
                .withChoice()
                .source(SearchEventsStates.ZIP_PROVIDED)
                        // if bad zip then go to the renter zip
                .first(SearchEventsStates.RENTER_ZIP, isInCorrectZipProvidedGuard(), null)
                        // if good zip then search events near the zip
                .last(SearchEventsStates.SEARCH_EVENTS, searchEventsAction())
                .and()
                        // if bad zip was provided, ask the user to enter again
                .withExternal().source(SearchEventsStates.RENTER_ZIP).target(SearchEventsStates.ZIP_PROVIDED)
                .event(SearchEventsEvents.USER_PROVIDES_ZIP).action(zipProvided())
                .and()
                .withExternal().target(SearchEventsStates.SE_QUIT).event(SearchEventsEvents.QUIT);

        StateMachine<SearchEventsStates, SearchEventsEvents> stateMachine = builder.build();
        stateMachine.addStateListener(new SearchEventsSMListener());
        return stateMachine;
    }

    public Action<SearchEventsStates, SearchEventsEvents> zipProvided() {
        return new Action<SearchEventsStates, SearchEventsEvents>() {
            @Override
            public void execute(StateContext<SearchEventsStates, SearchEventsEvents> context) {
                // search events here.
            }
        };
    }

    public Guard<SearchEventsStates, SearchEventsEvents> isInCorrectZipProvidedGuard() {
        return new Guard<SearchEventsStates, SearchEventsEvents>() {
            @Override
            public boolean evaluate(StateContext<SearchEventsStates, SearchEventsEvents> context) {
                String data = (String) context.getMessageHeaders().get("data");
                // https://www.oreilly.com/library/view/regular-expressions-cookbook/9781449327453/ch04s14.html
                return !data.matches("^[0-9]{5}(?:-[0-9]{4})?$");
            }
        };
    }

    public Action<SearchEventsStates, SearchEventsEvents> searchEventsAction() {
        return new Action<SearchEventsStates, SearchEventsEvents>() {
            @Override
            public void execute(StateContext<SearchEventsStates, SearchEventsEvents> context) {
                String data = (String) context.getMessageHeaders().get("data");
                String response = chatDataFetchService.getNearbyEvents(data);
                addConversationAttribute(context, SearchEventsStates.SEARCH_EVENTS.name() + "CustomResponse", response);
                // stop the state machine after the events are searched for the supplied zip
                addExtendedStateAttribute(context, ApplicationConstants.STOP_STATE_MACHINE, true);
            }
        };
    }

    private void resetStateMachine(StateContext<SearchEventsStates, SearchEventsEvents> context,
                                   SearchEventsStates resetToState, boolean copyState) {
        final DefaultStateMachineContext defaultStateMachineContext;
        if (copyState) {
            defaultStateMachineContext
                    = new DefaultStateMachineContext<>(resetToState, null, context.getMessageHeaders(), context.getExtendedState());
        } else {
            defaultStateMachineContext = new DefaultStateMachineContext<>(resetToState, null, null, null);
        }
        context.getStateMachine().getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.resetStateMachine(defaultStateMachineContext);
                });
    }

    public class SearchEventsSMListener extends StateMachineListenerAdapter<SearchEventsStates, SearchEventsEvents> {
        @Override
        public void stateContext(StateContext<SearchEventsStates, SearchEventsEvents> context) {
            String data = (String) context.getMessageHeaders().get("data");
            if (QUIT.equals(data.toLowerCase())) {
                setStopStateMachineFlag(context);
                resetStateMachine(context, SearchEventsStates.SE_QUIT, false);
            }
        }

        @Override
        public void stateMachineStarted(StateMachine<SearchEventsStates, SearchEventsEvents> stateMachine) {
            super.stateMachineStarted(stateMachine);
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(sma ->
                            sma.resetStateMachine(
                                    new DefaultStateMachineContext<>(SearchEventsStates.START_SEARCH_EVENTS, null,
                                            null, stateMachine.getExtendedState())));
        }
    }
}
