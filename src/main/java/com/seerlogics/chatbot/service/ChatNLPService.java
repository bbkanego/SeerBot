package com.seerlogics.chatbot.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lingoace.util.CommonUtil;
import com.rabidgremlin.mutters.core.IntentMatch;
import com.rabidgremlin.mutters.core.IntentMatcher;
import com.seerlogics.chatbot.exception.ConversationException;
import com.seerlogics.chatbot.model.ChatData;
import com.seerlogics.chatbot.model.Transaction;
import com.seerlogics.chatbot.mutters.CustomOpenNLPIntentMatcher;
import com.seerlogics.chatbot.mutters.SeerBotConfiguration;
import com.seerlogics.chatbot.noggin.ChatSession;
import com.seerlogics.chatbot.repository.ChatRepository;
import com.seerlogics.chatbot.repository.TransactionRepository;
import com.seerlogics.commons.CommonUtils;
import com.seerlogics.commons.dto.SearchIntents;
import com.seerlogics.commons.exception.DuplicateEntitiesFoundException;
import com.seerlogics.commons.exception.NoEntityFoundException;
import com.seerlogics.commons.exception.UIDisplayException;
import com.seerlogics.commons.model.Account;
import com.seerlogics.commons.model.Bot;
import com.seerlogics.commons.model.Intent;
import com.seerlogics.commons.model.IntentResponse;
import com.seerlogics.commons.repository.BotRepository;
import com.seerlogics.commons.repository.IntentRepository;
import com.seerlogics.commons.repository.LaunchInfoRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by bkane on 4/15/18.
 */
@Transactional
@Service
public class ChatNLPService {

    private static final String CHAT_BOT = "ChatBot";
    public static final String RESOURCE_PREFIX = "res_";
    public static final String UTF_8 = "UTF-8";

    private final ChatRepository chatRepository;

    private final VelocityEngine velocityEngine;

    private final MessageSource messageSource;

    private final IntentRepository intentRepository;

    private final LaunchInfoRepository launchInfoRepository;

    private final BotRepository botRepository;

    private final TransactionRepository transactionRepository;

    // this will cache SeerBotConfiguration.
    private Cache<String, SeerBotConfiguration> seerBotConfigurationCache;

    public ChatNLPService(LaunchInfoRepository launchInfoRepository, BotRepository botRepository,
                          ChatRepository chatRepository, VelocityEngine velocityEngine,
                          MessageSource messageSource, IntentRepository intentRepository,
                          TransactionRepository transactionRepository) {
        this.launchInfoRepository = launchInfoRepository;
        this.botRepository = botRepository;
        this.chatRepository = chatRepository;
        this.velocityEngine = velocityEngine;
        this.messageSource = messageSource;
        this.intentRepository = intentRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * https://stackoverflow.com/questions/11124856/using-guava-for-high-performance-thread-safe-caching
     * https://github.com/google/guava/wiki/CachesExplained#timed-eviction
     * https://github.com/google/guava/wiki/CachesExplained#Size-based-Eviction
     */
    @PostConstruct
    private void buildCache() {
        /**
         * The cache will be thread safe natively and will be accessed by 4 threads concurrently
         */
        seerBotConfigurationCache =
                CacheBuilder.newBuilder().concurrencyLevel(10000).maximumSize(100000)
                        // expire items after 1 hour if not accessed in that time
                        .expireAfterAccess(3600, TimeUnit.SECONDS).build();
    }

    @PreDestroy
    private void destroyCache() {
        seerBotConfigurationCache.invalidateAll();
    }

    /**
     * This will get the SeerBotConfiguration from cache if present or create a new one and put back
     * in the cache.
     *
     * @param uniqueBotId
     * @return
     */
    public SeerBotConfiguration getSeerBotConfiguration(String uniqueBotId) {
        SeerBotConfiguration seerBotConfiguration = this.seerBotConfigurationCache.asMap().get(uniqueBotId);
        if (seerBotConfiguration == null) {
            seerBotConfiguration = new SeerBotConfiguration(uniqueBotId, intentRepository,
                    launchInfoRepository, botRepository);
            this.seerBotConfigurationCache.put(uniqueBotId, seerBotConfiguration);
        }
        return seerBotConfiguration;
    }

    public void removeSeerBotConfiguration(String uniqueBotId) {
        this.seerBotConfigurationCache.asMap().remove(uniqueBotId);
    }

    public void invalidateCache() {
        this.seerBotConfigurationCache.invalidateAll();
    }

    public ChatData generateChatBotResponse(ChatData inputChatRequest, ChatSession chatSession) {
        // Save the incoming message
        String previousChatId = inputChatRequest.getPreviousChatId();
        if (StringUtils.isNotBlank(previousChatId)) {
            ChatData previousChat = chatRepository.getOne(Long.parseLong(previousChatId));
            inputChatRequest.setPreviousChat(previousChat);
            chatSession.setPreviousChat(previousChat);
        }

        Account owner = this.getSeerBotConfiguration(inputChatRequest.getAuthCode()).getTargetBot().getOwner();

        String ownerUserName = owner.getUserName();
        // for incoming chats both accountId and owner account will be same
        inputChatRequest.setAccountId(ownerUserName);
        inputChatRequest.setOwnerAccountId(ownerUserName);
        ChatData savedInputChat = chatRepository.save(inputChatRequest);

        // create new reply message and save that to the DB
        ChatData outChatData = new ChatData();

        IntentMatcher intentMatcher = this.getSeerBotConfiguration(inputChatRequest.getAuthCode()).
                getIntentMatcher();
        IntentMatch match = handleSalutation(inputChatRequest.getMessage(), intentMatcher);

        if (match == null) {
            match = intentMatcher.match(inputChatRequest.getMessage(), chatSession.getContext(),
                    null, new HashMap<>());
        }

        outChatData.setMessage(inputChatRequest.getMessage());

        boolean isConversation = false;
        // set the converation id before it gets wiped out below in case user calls it quits
        String conversationId = chatSession.getCurrentConversationId();
        if (chatSession.isConversationActive()) {
            isConversation = true;
            String responseKey = chatSession.decideNextResponseInConversation(outChatData);
            outChatData.setResponse(convertToVelocityResponse(getMessage(responseKey, chatSession), chatSession));
            /*Object customResponse = chatSession.getAttribute(RESOURCE_PREFIX
                                            + responseKey.toLowerCase() + "CustomResponse");
            if (customResponse != null) {
                String finalResponse = (String) customResponse;
                if (finalResponse.contains("widget")) {
                    finalResponse = finalResponse.replaceAll("\\n", "");
                    outChatData.setResponse(finalResponse);
                } else {
                    outChatData.setResponse(convertToVelocityResponse(getMessage(finalResponse, chatSession), chatSession));
                }
            } else {
                outChatData.setResponse(convertToVelocityResponse(getMessage(responseKey, chatSession), chatSession));
            }*/
            chatSession.endCurrentConversationIfEndStateReached();
        } else if (match != null && chatSession.isIntentConversationStarter(match.getIntent().getName())) {
            chatSession.startConversation(match.getIntent().getName());
            String responseKey = chatSession.getCurrentStateMachineHandler().getCurrentState();
            outChatData.setResponse(convertToVelocityResponse(getMessage(responseKey, chatSession), chatSession));
            isConversation = true;
        } else {
            outChatData.setResponse(convertToVelocityResponse(getMessage(match, inputChatRequest), chatSession));
        }

        // out chats will have CHAT_BOT accountId
        outChatData.setAccountId(CHAT_BOT);
        outChatData.setOwnerAccountId(ownerUserName);
        outChatData.setCurrentSessionId(chatSession.getCurrentSessionId());
        outChatData.setChatSessionId(inputChatRequest.getChatSessionId());
        outChatData.setPreviousChatId(String.valueOf(savedInputChat.getId()));
        outChatData.setPreviousChat(savedInputChat);
        outChatData.setAuthCode(inputChatRequest.getAuthCode());
        chatRepository.save(outChatData);

        // finally save the transaction.
        Transaction transaction = new Transaction();
        transaction.setAccountId(owner.getId());
        transaction.setTargetBotId(this.getSeerBotConfiguration(inputChatRequest.getAuthCode()).getTargetBot().getId());
        transaction.setSuccess(match != null);
        if (match != null && !isConversation) {
            transaction.setIntent(match.getIntent().getName());
        } else if (isConversation) {
            transaction.setIntent(conversationId);
            transaction.setSuccess(true);
        } else {
            transaction.setIntent("NO_MATCH");
        }
        transaction.setResolved(false);
        transaction.setIgnore(false);
        transaction.setUtterance(inputChatRequest.getMessage());
        transactionRepository.save(transaction);

        return outChatData;
    }

    private String buildPlainTextResponse(String response) {
        VelocityContext context = new VelocityContext();
        context.put("response", response);
        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("/velocity/simpleTexts.vm", UTF_8, context, stringWriter);
        return stringWriter.toString().replaceAll("\\n", "");
    }

    private String buildCustomVelocityResponse(String velocityTemplate, ChatSession chatSession) {
        VelocityContext context = new VelocityContext();
        context.put("attributes", chatSession.getAllAttributes());
        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("/velocity" + velocityTemplate, UTF_8, context, stringWriter);
        return stringWriter.toString().replaceAll("\\n", "").replace("\\t", "");
    }

    private Intent getIntentForIntentName(String intentName, Bot targetBot) {
        SearchIntents searchIntents = new SearchIntents();
        searchIntents.setIntentName(intentName);
        searchIntents.setCategory(targetBot.getCategory());
        searchIntents.setOwnerAccount(targetBot.getOwner());
        List<Intent> matchingIntents = this.intentRepository.findIntentsAndUtterances(searchIntents);
        if (matchingIntents.size() > 1) {
            String message = "Duplicate Intents found for intent = " + intentName;
            CommonUtils.throwUIDisplayException(message, new DuplicateEntitiesFoundException(message));
        } else if (matchingIntents.size() == 0) {
            String message = "No Intents found for for intent = " + intentName;
            CommonUtils.throwUIDisplayException(message, new NoEntityFoundException(message));
        }
        return matchingIntents.get(0);
    }

    public ChatData generateInitiateChatResponse(ChatData inChat, ChatSession chatSession) {
        Bot targetBot = this.getSeerBotConfiguration(inChat.getAuthCode()).getTargetBot();
        Intent initiateIntent = getIntentForIntentName(inChat.getMessage(), targetBot);
        ChatData initiateResponse = new ChatData();
        initiateResponse.setMessage(inChat.getMessage());
        if (initiateIntent != null) {
            List<IntentResponse> response = new ArrayList<>(initiateIntent.getResponses());
            initiateResponse.setResponse(convertToVelocityResponse(response.get(0).getResponse(), chatSession));
        } else {
            initiateResponse.setResponse(
                    convertToVelocityResponse(messageSource.getMessage("res_initialResponse",
                            new Object[]{}, Locale.getDefault()), chatSession));
        }
        initiateResponse.setChatSessionId(chatSession.getCurrentSessionId());
        initiateResponse.setCurrentSessionId(chatSession.getCurrentSessionId());
        initiateResponse.setAuthCode(inChat.getAuthCode());
        String ownerUserName =
                this.getSeerBotConfiguration(inChat.getAuthCode()).getTargetBot().getOwner().getUserName();
        initiateResponse.setAccountId(CHAT_BOT);
        initiateResponse.setOwnerAccountId(ownerUserName);
        chatRepository.save(initiateResponse);
        return initiateResponse;
    }

    /**
     * Here we will provide the matching intent. That may be a "MayBe" intent also.
     * Now based on that utterance we
     *
     * @param intent
     * @return
     */
    private String getMessage(IntentMatch intent, ChatData inputChatRequest) {
        if (intent != null) {
            Object matchingIntent = intent.getIntent();
            IntentResponse responseToSend;
            // this will occur if there is a match with intent defined by the customer.
            if (matchingIntent instanceof com.seerlogics.chatbot.mutters.Intent) {
                com.seerlogics.chatbot.mutters.Intent muttersIntent =
                        (com.seerlogics.chatbot.mutters.Intent) intent.getIntent();
                Intent dbIntent = intentRepository.getOne(muttersIntent.getDbIntent().getId());
                List<IntentResponse> intentResponses =
                        dbIntent.getResponses().stream().filter(
                                intentResponse -> intentResponse.getLocale().contains("en"))
                                .collect(Collectors.toList());
                if (intentResponses.size() > 1) {
                    throw new ConversationException("Multiple responses found...");
                }
                responseToSend = intentResponses.get(0);
            } else if (matchingIntent != null) { // this will happen when there is a MayBe match
                com.rabidgremlin.mutters.core.Intent maybeIntent = intent.getIntent();
                Bot targetBot = this.getSeerBotConfiguration(inputChatRequest.getAuthCode()).getTargetBot();
                String mayBeIntentName = maybeIntent.getName();
                Intent dbMayBeIntent = getIntentForIntentName(mayBeIntentName, targetBot);
                if (dbMayBeIntent != null) {
                    List<IntentResponse> intentResponses =
                            dbMayBeIntent.getResponses().stream().filter(
                                    intentResponse -> intentResponse.getLocale().contains("en")).
                                    collect(Collectors.toList());
                    if (intentResponses.size() > 1) {
                        throw new ConversationException("Multiple responses found...");
                    }
                    responseToSend = intentResponses.get(0);
                } else { // if no MayBe intent defined in DB.
                    responseToSend = this.getDoNotUnderstandIntent(inputChatRequest);
                }
            } else { // catch ALL
                responseToSend = this.getDoNotUnderstandIntent(inputChatRequest);
            }
            return responseToSend.getResponse();
        } else {
            return getDoNotUnderstandIntent(inputChatRequest).getResponse();
        }
    }

    private IntentResponse getDoNotUnderstandIntent(ChatData inputChatRequest) {
        Bot targetBot = this.getSeerBotConfiguration(inputChatRequest.getAuthCode()).getTargetBot();
        Intent doNotUnderstandIntent = getIntentForIntentName("DoNotUnderstandIntent", targetBot);
        List<IntentResponse> intentResponses =
                doNotUnderstandIntent.getResponses().stream().filter(intentResponse ->
                        intentResponse.getLocale().contains("en")).collect(Collectors.toList());
        if (intentResponses.size() > 1) {
            throw new ConversationException("Multiple responses found...");
        }
        return intentResponses.get(0);
    }

    private String getMessage(String key, ChatSession chatSession) {
        return messageSource.getMessage(RESOURCE_PREFIX + key.toLowerCase(), new Object[]{}, Locale.getDefault());
    }

    private String convertToVelocityResponse(String response, ChatSession chatSession) {
        if (response.contains("|")) {
            // this is an options response
            return buildOptionsResponse(response);
        } else if (response.endsWith(".vm")) { // its a plain response
            return buildCustomVelocityResponse(response, chatSession);
        } else { // its a plain response
            return buildPlainTextResponse(response);
        }
    }

    private String buildOptionsResponse(String response) {
        String[] messageParts = StringUtils.split(response, "|");
        String mainMessage = messageParts[0];
        List<Map> allOptions = new ArrayList<>(messageParts.length - 1);
        for (int i = 1, messagePartsLength = messageParts.length; i < messagePartsLength; i++) {
            Map<String, String> option = new HashMap<>();
            allOptions.add(option);
            String messagePart = messageParts[i];
            String[] optionParts = StringUtils.split(messagePart, "&");
            String firstPart = optionParts[0];
            String secondPart = optionParts[1];
            if (firstPart.contains("butt=")) {
                option.put("option", StringUtils.split(firstPart, "=")[1]);
                option.put("type", "button");
                option.put("clickResponse", StringUtils.split(secondPart, "=")[1]);
            } else if (firstPart.contains("link=")) {
                option.put("option", StringUtils.split(firstPart, "=")[1]);
                option.put("type", "link");
                option.put("clickResponse", StringUtils.split(secondPart, "=")[1]);
            }
        }
        VelocityContext context = new VelocityContext();
        context.put("allOptions", allOptions);
        context.put("message", mainMessage);
        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("/velocity/optionsResponse.vm", "UTF-8", context, stringWriter);
        return stringWriter.toString().replaceAll("\\n", "");
    }

    private String loadInitiateResponse() {
        String initResponse = messageSource.getMessage("res_initialResponse", new Object[]{}, Locale.getDefault());
        return buildOptionsResponse(initResponse);
    }

    public String getGenericConfirmMessage() {
        VelocityContext context = new VelocityContext();
        context.put("message", messageSource.getMessage("res_areYouSureYouWantToDelete",
                new Object[]{}, Locale.getDefault()));
        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("/velocity/deleteConfirm.vm", "UTF-8", context, stringWriter);
        return stringWriter.toString().replaceAll("\\n", "");
    }

    public String getSearchAllEventsOptionsMessage() {
        VelocityContext context = new VelocityContext();
        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("/velocity/searchEventsOptions.vm", "UTF-8", context, stringWriter);
        return stringWriter.toString().replaceAll("\\n", "");
    }

    public String getMayBeOptionsMessage(String intent) {
        VelocityContext context = new VelocityContext();
        context.put("message", messageSource.getMessage("res_" + intent.toLowerCase() + "_message",
                new Object[]{}, Locale.getDefault()));
        String options = messageSource.getMessage("res_" + intent.toLowerCase() + "_options", new Object[]{}, Locale.getDefault());
        String[] splitOptions = StringUtils.split(options, "*");
        List<Map> allOptions = new ArrayList<>(splitOptions.length);
        for (String splitOption : splitOptions) {
            Map<String, String> currentOption = new HashMap<>();
            String[] secSplitOptions = StringUtils.split(splitOption, "|");
            currentOption.put("whatCanBeDone", secSplitOptions[0]);
            currentOption.put("clickResponse", secSplitOptions[1]);
            allOptions.add(currentOption);
        }
        context.put("options", allOptions);
        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("/velocity/maybeOptions.vm", "UTF-8", context, stringWriter);
        return stringWriter.toString().replaceAll("\\n", "");
    }

    public IntentMatch handleSalutation(String message, IntentMatcher intentMatcher) {
        CustomOpenNLPIntentMatcher customOpenNLPIntentMatcher = (CustomOpenNLPIntentMatcher) intentMatcher;
        String[] wordsInMessage = message.split(" ");
        List<String> saluations = Arrays.asList("Hi".toUpperCase(), "Hello".toUpperCase(), "Hey".toUpperCase(),
                "Ola".toUpperCase(), "Namaste".toUpperCase(), "Chiao".toUpperCase());
        boolean containsSalutation = true;
        for (String s : wordsInMessage) {
            if (!saluations.contains(StringUtils.replaceEach(s.trim().toUpperCase(),
                                        new String[]{"!", "?", "'", "\""}, new String[]{"", "", "", ""}))) {
                containsSalutation = false;
            }
        }

        if (containsSalutation) {
            com.rabidgremlin.mutters.core.Intent intent = customOpenNLPIntentMatcher.getIntentsCopy().get("HI");
            return new IntentMatch(intent, null, message);
        }
        return null;
    }

    public List<ChatData> findByAccountId(String userName) {
        return chatRepository.findByAccountId(userName);
    }

    public List<ChatData> findAll() {
        return chatRepository.findAll();
    }

    public List<ChatData> findByChatSessionId(String chatsessionId) {
        return chatRepository.findByChatSessionId(chatsessionId);
    }

}
