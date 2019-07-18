VAR eventDeleteResponse=""

// 1. Delete Event flow started. This is the entry knot. This is defined in botconfig.json
=== DeleteEventYesNoBlock ===
-> AskUserIfTheyWantToDeleteEvent

// 2. Next show the confirmation and wait for the user input
=== AskUserIfTheyWantToDeleteEvent ===
// show the yes/no confirmation first!
::PROCEED_CONFIRM

/**
    then wait for user input. Here when we show the confirmation to the user
    we want the user to click on the yes or no button. The below will take 
    any response to the confirmation and then forward it to "DeleteEventYesNoLoop" 
    to validate the user input.
*/
+ PerformEventDelete

-> PerformEventDeleteLoop

// 3. Validate the user response to delete yes or no.
=== PerformEventDeleteLoop ===
- (delete_event_loop)
{
    - eventDeleteResponse == "":
        -> AskUserIfTheyWantToDeleteEvent
    - else:
        -> PerformEventDeleteAction
}
-> END

=== PerformEventDeleteAction ===
    ::PERFORM_EVENT_DELETE
-> END

=== EventDeleted ===
    res_eventWasDeleted
-> END

=== CancelEventDelete ===
    res_cancelEventDelete
-> END