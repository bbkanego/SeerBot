package com.seerlogics.chatbot.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rabidgremlin.mutters.core.IntentMatch;
import com.seerlogics.chatbot.exception.ConversationException;
import com.seerlogics.chatbot.model.ChatData;
import com.seerlogics.chatbot.model.Transaction;
import com.seerlogics.chatbot.mutters.SeerBotConfiguration;
import com.seerlogics.chatbot.noggin.ChatSession;
import com.seerlogics.chatbot.repository.ChatRepository;
import com.seerlogics.chatbot.repository.TransactionRepository;
import com.seerlogics.commons.model.Account;
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
                CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(100000)
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
        //outChatData.setResponse(intentProcessor.nlpPipeline(inputChatRequest, chatSession));
        IntentMatch match = this.getSeerBotConfiguration(inputChatRequest.getAuthCode()).
                getIntentMatcher().match(inputChatRequest.getMessage(), chatSession.getContext(), null, new HashMap<>());

        outChatData.setMessage(inputChatRequest.getMessage());

        if (chatSession.isConversationActive()) {
            String responseKey = chatSession.decideNextResponseInConversation(outChatData);
            Object customResponse = chatSession.getAttribute(responseKey + "CustomResponse");
            if (customResponse != null) {
                String finalResponse = (String) customResponse;
                if (finalResponse.contains("widget")) {
                    finalResponse = finalResponse.replaceAll("\\n", "");
                    outChatData.setResponse(finalResponse);
                } else {
                    outChatData.setResponse(convertToVelocityResponse(getMessage(finalResponse)));
                }
            } else {
                outChatData.setResponse(convertToVelocityResponse(getMessage(responseKey)));
            }
            chatSession.endCurrentConversationIfEndStateReached();
        } else if (match != null && chatSession.isIntentConversationStarter(match.getIntent().getName())) {
            chatSession.startConversation(match.getIntent().getName());
            String responseKey = chatSession.getCurrentStateMachineHandler().getCurrentState();
            outChatData.setResponse(convertToVelocityResponse(getMessage(responseKey)));
        } else {
            outChatData.setResponse(convertToVelocityResponse(getMessage(match, inputChatRequest)));
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
        if (match != null) {
            transaction.setIntent(match.getIntent().getName());
        } else {
            transaction.setIntent("NO_MATCH");
        }
        transaction.setUtterance(inputChatRequest.getMessage());
        transactionRepository.save(transaction);

        return outChatData;
    }

    private String buildPlainTextResponse(String response) {
        VelocityContext context = new VelocityContext();
        context.put("response", response);
        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("/velocity/simpleTexts.vm", "UTF-8", context, stringWriter);
        return stringWriter.toString().replaceAll("\\n", "");
    }

    public ChatData generateInitiateChatResponse(ChatData inChat, ChatSession chatSession) {
        Intent initiateIntent = this.intentRepository.findByIntent(inChat.getMessage(),
                this.getSeerBotConfiguration(inChat.getAuthCode()).getTargetBot().getOwner().getId());
        ChatData initiateResponse = new ChatData();
        initiateResponse.setMessage(inChat.getMessage());
        if (initiateIntent != null) {
            List<IntentResponse> response = new ArrayList<>(initiateIntent.getResponses());
            initiateResponse.setResponse(convertToVelocityResponse(response.get(0).getResponse()));
        } else {
            initiateResponse.setResponse(
                    convertToVelocityResponse(messageSource.getMessage("res_initialResponse",
                            new Object[]{}, Locale.getDefault())));
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
                String mayBeIntentName = maybeIntent.getName();
                Intent dbMayBeIntent = intentRepository.findByIntent(mayBeIntentName,
                        this.getSeerBotConfiguration(inputChatRequest.getAuthCode()).getTargetBot().getOwner().getId());
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
        Long botOwnerId = this.getSeerBotConfiguration(inputChatRequest.getAuthCode())
                .getTargetBot().getOwner().getId();
        Intent doNotUnderstandIntent =
                intentRepository.findByIntent("DoNotUnderstandIntent", botOwnerId);
        List<IntentResponse> intentResponses =
                doNotUnderstandIntent.getResponses().stream().filter(intentResponse ->
                        intentResponse.getLocale().contains("en")).collect(Collectors.toList());
        if (intentResponses.size() > 1) {
            throw new ConversationException("Multiple responses found...");
        }
        return intentResponses.get(0);
    }

    private String getMessage(String key) {
        return messageSource.getMessage("res_" + key.toLowerCase(), new Object[]{}, Locale.getDefault());
    }

    private String convertToVelocityResponse(String response) {
        if (response.contains("|")) {
            // this is an options response
            return buildOptionsResponse(response);
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
