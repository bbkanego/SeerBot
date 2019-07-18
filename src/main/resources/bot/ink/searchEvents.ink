VAR address=""
VAR validAddress=""

=== SearchEvents ===
    ::SEARCH_ALL_OPTIONS
-> END

// 1. search event step 1
== SearchEventsNear ==
-> request_address

// 2. search event step 2. Ask them the zip and wait for user input
== request_address
::ASK_USER_FOR_ADDRESS

res_whatsthezip

/* 
    When you provide the + GaveAddress, that will mean that you want a response from the user.
    Once the user enters a response like 28266, that will be matched against the intents.
*/
+ AskUserForAddressAndWait
/*
The above will accept the address which is a zip and then forward to the knot GaveAddress.
That will validate the variable "address"
*/
-> GaveAddress

// 3. search event step 3. Validate the user input.
== GaveAddress ==
//::VALIDATE_ADDRESS
- (search_events_loop)
    {
      - address == "":
        -> request_address
      - else:
        -> events_near_you
    }
-> END

== events_near_you
    ::GET_EVENTS_RESPONSE
-> END

== InvalidAddress ==
res_invalidaddress
-> END

== GaveEmbeddedAddress ==
Whats the address!!!
-> END

== ValidateAddress ==
::VALIDATE_ADDRESS
->END

==MayBeSearchEvents==
::MAY_BE_OPTIONS
->END