package com.seerlogics.chatbot.statemachine.restaurant;

import com.seerlogics.chatbot.statemachine.UberStateMachine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import java.util.EnumSet;

public class ReservationStateMachine extends UberStateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationStateMachine.class);

    public enum ReservationStates implements UberStates {
        START_RESERVATION,
        // number of guests handling
        PROVIDE_NUM_OF_GUESTS, INVALID_NUM_OF_GUESTS_PROVIDED, VALID_NUM_OF_GUESTS_PROVIDED,
        // lunch or dinner
        IS_LUNCH_OR_DINNER, INVALID_IS_LUNCH_OR_DINNER, VALID_IS_LUNCH_OR_DINNER,
        // What time for reservation
        PROVIDE_TIME, INVALID_TIME_PROVIDED, VALID_TIME_PROVIDED,
        // final reservation
        DO_RESERVATION,
        // exit the reservation
        RES_QUIT
    }

    /**
     * Any time the user takes action on the UI, its an event.
     */
    public enum ReservationEvents implements UberEvents {
        USER_PROVIDES_NUM_OF_GUESTS, USER_PROVIDES_TIME, QUIT
    }

    static {
        currentStateToNextEvent.put(ReservationStates.START_RESERVATION, ReservationEvents.USER_PROVIDES_NUM_OF_GUESTS);
        currentStateToNextEvent.put(ReservationStates.PROVIDE_NUM_OF_GUESTS, ReservationEvents.USER_PROVIDES_NUM_OF_GUESTS);
        currentStateToNextEvent.put(ReservationStates.VALID_NUM_OF_GUESTS_PROVIDED, ReservationEvents.USER_PROVIDES_TIME);
        currentStateToNextEvent.put(ReservationStates.INVALID_NUM_OF_GUESTS_PROVIDED, ReservationEvents.USER_PROVIDES_NUM_OF_GUESTS);
        currentStateToNextEvent.put(ReservationStates.INVALID_TIME_PROVIDED, ReservationEvents.USER_PROVIDES_TIME);
    }

    public StateMachine<ReservationStates, ReservationEvents> createStateMachine() throws Exception {
        StateMachineBuilder.Builder<ReservationStates, ReservationEvents> builder = StateMachineBuilder.builder();

        // https://docs.spring.io/autorepo/docs/spring-statemachine/1.2.x-SNAPSHOT/reference/html/state-machine-concepts.html
        builder.configureStates()
                .withStates()
                .initial(ReservationStates.START_RESERVATION)
                /**
                 * Transition is configured using withChoice() where you define source state and first/then/last
                 * structure which is equivalent to normal if/elseif/else. With first and then you can specify
                 * a guard just like you’d use a condition with if/elseif clauses.
                 */
                .choice(ReservationStates.PROVIDE_NUM_OF_GUESTS)
                .choice(ReservationStates.IS_LUNCH_OR_DINNER)
                .choice(ReservationStates.PROVIDE_TIME)
                .end(ReservationStates.DO_RESERVATION)
                .end(ReservationStates.RES_QUIT)
                .states(EnumSet.allOf(ReservationStates.class));

        /**
         * Transitions:
         * Happy path: none to START_RESERVATION to VALID_NUM_OF_GUESTS_PROVIDED to DO_RESERVATION
         * Unhappy path: none to START_RESERVATION to
         *             INVALID_NUM_OF_GUESTS_PROVIDED to INVALID_NUM_OF_GUESTS_PROVIDED (if the user provides wrong guests)
         *             to VALID_NUM_OF_GUESTS_PROVIDED (after correct num of guests provided) to INVALID_TIME_PROVIDED (if incorrect time)
         *             to DO_RESERVATION (when valid time provided)
         */
        builder.configureTransitions()
                // ************* Start Reservation ****************//
                .withExternal()
                // transition from START_RESERVATION to NUM_OF_GUESTS
                .source(ReservationStates.START_RESERVATION).target(ReservationStates.PROVIDE_NUM_OF_GUESTS)
                // on Event USER_PROVIDES_NUM_OF_GUESTS
                .event(ReservationEvents.USER_PROVIDES_NUM_OF_GUESTS)
                .and()
                // ******** start num of guest if then else
                .withChoice()
                /**
                 * This is if then else
                 * Source condition: User provides NUM of guests
                 */
                .source(ReservationStates.PROVIDE_NUM_OF_GUESTS)
                // if (Bad_num_of_Guests) -> asks user again by going to state INVALID_NUM_OF_GUESTS_PROVIDED
                .first(ReservationStates.INVALID_NUM_OF_GUESTS_PROVIDED, isInCorrectNumOfGuestsProvidedGuard(), null)
                // if (Good num_of_Guests) -> Then go to VALID_NUM_OF_GUESTS_PROVIDED
                .last(ReservationStates.VALID_NUM_OF_GUESTS_PROVIDED)
                // ************* Start Reservation ^^^^^^^^^^^^^^^^^^^^//
                .and()
                // if invalid num of guests provided ask for num of guests again. To do this take the user back to "PROVIDE_NUM_OF_GUESTS"
                .withExternal()
                .source(ReservationStates.INVALID_NUM_OF_GUESTS_PROVIDED).target(ReservationStates.PROVIDE_NUM_OF_GUESTS)
                .event(ReservationEvents.USER_PROVIDES_NUM_OF_GUESTS)
                .and()
                /**
                 * Start the Time if then else
                 */
                .withExternal().source(ReservationStates.VALID_NUM_OF_GUESTS_PROVIDED).target(ReservationStates.PROVIDE_TIME)
                .event(ReservationEvents.USER_PROVIDES_TIME)
                .and()
                .withChoice()
                .source(ReservationStates.PROVIDE_TIME)
                .first(ReservationStates.INVALID_TIME_PROVIDED, isInvalidTimeProvidedGuard())
                .last(ReservationStates.DO_RESERVATION, doReservation())
                .and()
                // if invalid time provided ask for time again
                .withExternal().source(ReservationStates.INVALID_TIME_PROVIDED).target(ReservationStates.PROVIDE_TIME)
                .event(ReservationEvents.USER_PROVIDES_TIME);

        StateMachine<ReservationStates, ReservationEvents> stateMachine = builder.build();
        stateMachine.addStateListener(new DoReservationStateMachineListener());
        return stateMachine;
    }

    private static Guard<ReservationStates, ReservationEvents> isInCorrectNumOfGuestsProvidedGuard() {
        return new Guard<ReservationStates, ReservationEvents>() {
            @Override
            public boolean evaluate(StateContext<ReservationStates, ReservationEvents> context) {
                String numOfGuests = (String) context.getMessageHeaders().get("data");
                if (StringUtils.isBlank(numOfGuests)) return true;
                int numOfGuestInt = 0;
                try {
                    numOfGuestInt = Integer.parseInt(numOfGuests);
                } catch (NumberFormatException e) {
                    return true;
                }
                // https://www.oreilly.com/library/view/regular-expressions-cookbook/9781449327453/ch04s14.html
                return numOfGuestInt > 10;
            }
        };
    }

    private static Guard<ReservationStates, ReservationEvents> isInvalidTimeProvidedGuard() {
        return new Guard<ReservationStates, ReservationEvents>() {
            @Override
            public boolean evaluate(StateContext<ReservationStates, ReservationEvents> context) {
                String time = (String) context.getMessageHeaders().get("data");
                if (StringUtils.isBlank(time)) return true;

                int timeInt = 0;
                try {
                    timeInt = Integer.parseInt(time);
                } catch (NumberFormatException e) {
                    return true;
                }

                return timeInt > 10;
            }
        };
    }

    private static Action<ReservationStates, ReservationEvents> doReservation() {
        return new Action<ReservationStates, ReservationEvents>() {
            @Override
            public void execute(StateContext<ReservationStates, ReservationEvents> context) {
                // stop the state machine after the accoun is unlocked
                resetStateMachine(context, ReservationStates.START_RESERVATION, false);
                setStopStateMachineFlag(context);
            }
        };
    }

    private static void resetStateMachine(StateContext<ReservationStates, ReservationEvents> context,
                                          ReservationStates resetToState, boolean copyState) {
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

    public static class DoReservationStateMachineListener extends StateMachineListenerAdapter<ReservationStates, ReservationEvents> {
        @Override
        public void stateContext(StateContext<ReservationStates, ReservationEvents> context) {
            String data = (String) context.getMessageHeaders().get("data");
            if (QUIT.equalsIgnoreCase(data)) {
                setStopStateMachineFlag(context);
                resetStateMachine(context, ReservationStates.RES_QUIT, false);
            }
        }

        @Override
        public void stateChanged(State from, State to) {
            LOGGER.debug("***** Transitioned from {} to {}", from == null ?
                    "none" : from.getId(), to.getId());
        }

        @Override
        public void stateEntered(State<ReservationStates, ReservationEvents> state) {
            LOGGER.debug("***** State entered {}", state == null ? "none" : state.getId());
        }

        @Override
        public void stateExited(State<ReservationStates, ReservationEvents> state) {
            LOGGER.debug("***** State exited {}", state == null ? "none" : state.getId());
        }

        @Override
        public void stateMachineStarted(StateMachine<ReservationStates, ReservationEvents> stateMachine) {
            super.stateMachineStarted(stateMachine);
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(sma ->
                            sma.resetStateMachine(
                                    new DefaultStateMachineContext<>(ReservationStates.START_RESERVATION, null,
                                            null, stateMachine.getExtendedState())));
        }
    }
}
