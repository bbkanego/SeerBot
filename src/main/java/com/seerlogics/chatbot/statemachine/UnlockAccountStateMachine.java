package com.seerlogics.chatbot.statemachine;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;

/**
 * Created by bkane on 10/6/18.
 */
@Configuration
public class UnlockAccountStateMachine extends UberStateMachine {
    public enum UnlockAccountStates implements UberStates {
        START_UNLOCK_ACCOUNT, DOB_PROVIDED, INVALID_DOB_PROVIDED, VALID_DOB_PROVIDED,
        PIN_PROVIDED, INVALID_PIN_PROVIDED, ACCOUNT_UNLOCKED, UA_QUIT
    }

    public enum UnlockAccountEvents implements UberEvents {
        USER_PROVIDES_DOB, USER_PROVIDES_PIN, QUIT
    }

    static {
        currentStateToNextEvent.put(UnlockAccountStates.START_UNLOCK_ACCOUNT, UnlockAccountEvents.USER_PROVIDES_DOB);
        currentStateToNextEvent.put(UnlockAccountStates.VALID_DOB_PROVIDED, UnlockAccountEvents.USER_PROVIDES_PIN);
        currentStateToNextEvent.put(UnlockAccountStates.INVALID_DOB_PROVIDED, UnlockAccountEvents.USER_PROVIDES_DOB);
        currentStateToNextEvent.put(UnlockAccountStates.INVALID_PIN_PROVIDED, UnlockAccountEvents.USER_PROVIDES_PIN);
    }

    @Bean(name = "unlockAccountSM")
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public StateMachine<UnlockAccountStates, UnlockAccountEvents> getSearchEventSM() throws Exception {
        StateMachineBuilder.Builder<UnlockAccountStates, UnlockAccountEvents> builder = StateMachineBuilder.builder();

        builder.configureStates()
                .withStates()
                .initial(UnlockAccountStates.START_UNLOCK_ACCOUNT)
                .choice(UnlockAccountStates.DOB_PROVIDED)
                .choice(UnlockAccountStates.PIN_PROVIDED)
                .end(UnlockAccountStates.ACCOUNT_UNLOCKED)
                .end(UnlockAccountStates.UA_QUIT)
                .states(EnumSet.allOf(UnlockAccountStates.class));

        builder.configureTransitions()
                .withExternal()
                .source(UnlockAccountStates.START_UNLOCK_ACCOUNT).target(UnlockAccountStates.DOB_PROVIDED)
                .event(UnlockAccountEvents.USER_PROVIDES_DOB)
                .and()
                // check if the dob provided is valid
                .withChoice()
                .source(UnlockAccountStates.DOB_PROVIDED)
                // if bad dob
                .first(UnlockAccountStates.INVALID_DOB_PROVIDED, isInCorrectDOBProvidedGuard(), null)
                // if good dob
                .last(UnlockAccountStates.VALID_DOB_PROVIDED)
                .and()
                // if bad dob was provided, ask the user to enter again
                .withExternal().source(UnlockAccountStates.INVALID_DOB_PROVIDED).target(UnlockAccountStates.DOB_PROVIDED)
                .event(UnlockAccountEvents.USER_PROVIDES_DOB)
                .and()
                // if good dob was provided, ask the user to enter pin
                .withExternal().source(UnlockAccountStates.VALID_DOB_PROVIDED).target(UnlockAccountStates.PIN_PROVIDED)
                .event(UnlockAccountEvents.USER_PROVIDES_PIN)
                .and()
                // check the pin
                .withChoice()
                .source(UnlockAccountStates.PIN_PROVIDED)
                // if bad pin
                .first(UnlockAccountStates.INVALID_PIN_PROVIDED, isInCorrectPINProvidedGuard(), null)
                // if good pin
                .last(UnlockAccountStates.ACCOUNT_UNLOCKED, unlockAccountAction())
                .and()
                // if bad pin was provided, ask the user to enter again
                .withExternal().source(UnlockAccountStates.INVALID_PIN_PROVIDED).target(UnlockAccountStates.PIN_PROVIDED)
                .event(UnlockAccountEvents.USER_PROVIDES_PIN)
                .and()
                .withExternal().target(UnlockAccountStates.UA_QUIT).event(UnlockAccountEvents.QUIT);

        StateMachine<UnlockAccountStates, UnlockAccountEvents> stateMachine = builder.build();
        stateMachine.addStateListener(new UnlockAccountStateMachineListener());
        return stateMachine;
    }

    public Guard<UnlockAccountStates, UnlockAccountEvents> isInCorrectDOBProvidedGuard() {
        return new Guard<UnlockAccountStates, UnlockAccountEvents>() {
            @Override
            public boolean evaluate(StateContext<UnlockAccountStates, UnlockAccountEvents> context) {
                String data = (String) context.getMessageHeaders().get("data");
                // parse the data using MM/DD/yyyy
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    Date dob = simpleDateFormat.parse(data);
                    Date currentDate = new Date();
                    return !dob.before(currentDate);
                } catch (ParseException e) {
                    return true;
                }
            }
        };
    }

    public Guard<UnlockAccountStates, UnlockAccountEvents> isInCorrectPINProvidedGuard() {
        return new Guard<UnlockAccountStates, UnlockAccountEvents>() {
            @Override
            public boolean evaluate(StateContext<UnlockAccountStates, UnlockAccountEvents> context) {
                String data = (String) context.getMessageHeaders().get("data");
                return !data.matches("^[0-9]{4}$");
            }
        };
    }

    public Action<UnlockAccountStates, UnlockAccountEvents> unlockAccountAction() {
        return new Action<UnlockAccountStates, UnlockAccountEvents>() {
            @Override
            public void execute(StateContext<UnlockAccountStates, UnlockAccountEvents> context) {
                // stop the state machine after the accoun is unlocked
                resetStateMachine(context, UnlockAccountStates.START_UNLOCK_ACCOUNT, false);
                setStopStateMachineFlag(context);
            }
        };
    }

    private void resetStateMachine(StateContext<UnlockAccountStates, UnlockAccountEvents> context,
                                   UnlockAccountStates resetToState, boolean copyState) {
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

    public class UnlockAccountStateMachineListener extends StateMachineListenerAdapter<UnlockAccountStates, UnlockAccountEvents> {
        @Override
        public void stateContext(StateContext<UnlockAccountStates, UnlockAccountEvents> context) {
            String data = (String) context.getMessageHeaders().get("data");
            if (QUIT.equals(data.toLowerCase())) {
                setStopStateMachineFlag(context);
                resetStateMachine(context, UnlockAccountStates.UA_QUIT, false);
            }
        }

        @Override
        public void stateMachineStarted(StateMachine<UnlockAccountStates, UnlockAccountEvents> stateMachine) {
            super.stateMachineStarted(stateMachine);
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(sma ->
                            sma.resetStateMachine(
                                    new DefaultStateMachineContext<>(UnlockAccountStates.START_UNLOCK_ACCOUNT, null,
                                            null, stateMachine.getExtendedState())));
        }
    }
}
