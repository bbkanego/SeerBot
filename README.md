## Seer Bot Reference Implementation

### Build Profile Option
1. Build for AWS deployment
The below will build jar/war per the ''aws-ec2'' profile
```
mvn clean install -P aws-ec2
```

2. Build for local deployment
```
mvn clean install -P local
```

### Use below commands to build and run the reference bot
The bot by default will run on port 8099. You can provide ""--seerchat.botPort=[port#]"" as a spring argument as shown below

```
java -jar -Dspring.profiles.active=local -Dseerchat.botOwnerId=2903 -Dseerchat.botId=196 seerlogics-ref-bot-1.0.0-SNAPSHOT.jar --seerchat.bottype=EVENT_BOT --seerchat.botOwnerId=2903 --seerchat.botId=196 --seerchat.trainedModelId=3941 --seerchat.allowedOrigins=http://localhost:4300,http://localhost:4320
```

### Create a Run configuration to run the reference bot in Intellij
1. In Main class enter: com.seerlogics.chatbot.ChatbotApplication
2. In VM Arg enter: -Dspring.profiles.active=local
3. In Program args enter: --seerchat.bottype=EVENT_BOT --seerchat.botOwnerId=2903 --seerchat.botId=196 --seerchat.trainedModelId=3941
4. In "Use classpath of module" select: "SeerlogicsReferenceBot" module

### Admin/Actuator URLs
1. Health URL: http://localhost:8099/chatbot/actuator/health
2. Info URL: http://localhost:8099/chatbot/actuator/info
3. Chatbots URL: http://localhost:8099/chatbot/api/chats

### Spring state machine errors to know:
1. ***java.lang.IllegalArgumentException: Payload must not be null:***
This happens when in the chat window the user enters blank or NULL info.
