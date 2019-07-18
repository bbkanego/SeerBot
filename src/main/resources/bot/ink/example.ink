== start ==
+ OrderTaxi -> order_taxi
+ CancelTaxi -> cancel_taxi
+ WhereTaxi -> where_taxi

VAR address=""
VAR taxiNo = ""

== order_taxi ==
- (order_taxi_loop)
{
  - address == "":
    -> request_address
  - else:
    -> order_the_taxi
}
-> END

= request_address
What is the pick up address ?
::SET_REPROMPT Where would you like to be picked up ?
::SET_HINT 123 Someplace Rd
+ GaveAddress
- -> order_taxi_loop

= order_the_taxi
::ORDER_TAXI
Taxi {taxiNo} is on its way
::ADD_ATTACHMENT type::link url::http:\/\/trackcab.example.com/t/{taxiNo} title::Track your taxi here
::ADD_QUICK_REPLY Where is my taxi?
::ADD_QUICK_REPLY Cancel my taxi
-> END

== cancel_taxi ==
Your taxi has been cancelled
-> END

== where_taxi ==
Your taxi is about 7 minutes away
-> END

== stop ==
Ok
-> END

== help ==
I can help you order a taxi or find out the location of your current taxi.

Try say "Order a cab" or "Where is my cab"
-> END

== confused_bot ==
I'm sorry I'm not understanding you at all :(
If you are in a hurry, please call 555-12345 to order your taxi.
-> END

-> start