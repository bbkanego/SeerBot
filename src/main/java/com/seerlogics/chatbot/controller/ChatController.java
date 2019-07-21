package com.seerlogics.chatbot.controller;

import com.seerlogics.chatbot.model.chat.ChatData;
import com.seerlogics.chatbot.noggin.ChatSession;
import com.seerlogics.chatbot.service.ChatNLPService;
import com.seerlogics.commons.model.LaunchInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bkane on 5/4/18.
 */
@RestController
@RequestMapping(value = "/api")
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final ChatSession chatSession;

    private final ChatNLPService chatNLPService;

    public ChatController(ChatSession chatSession, ChatNLPService chatNLPService) {
        this.chatSession = chatSession;
        this.chatNLPService = chatNLPService;
    }

    @GetMapping("/chats")
    public ResponseEntity getChatHistory(HttpServletRequest request) {
        LOGGER.debug("Getting all chats now ----->>>>>>");
        List<ChatData> chatDataList = chatNLPService.findAll();
        return new ResponseEntity<>(chatDataList, HttpStatus.OK);
    }

    @GetMapping("/chats/{chatSessionId}")
    public ResponseEntity getChatsByChatSessionId(@PathVariable String chatSessionId, HttpServletRequest request) {
        // get the AUTH from the JWT token.
        List<ChatData> chatDataList = chatNLPService.findByChatSessionId(chatSessionId);
        return new ResponseEntity<>(chatDataList, HttpStatus.OK);
    }

    @PostMapping("/chats")
    public ResponseEntity chatMessage(@RequestBody ChatData incomingChatData, HttpServletRequest request,
                                      HttpServletResponse response) {
        LOGGER.debug(">>>> current session object = {} ", request.getSession());

        if (!this.containsValidHeaders(request)) {
            Map<String, Boolean> errorResponse = new HashMap<>();
            errorResponse.put("invalidAccess", true);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        if (chatSession.getCurrentSessionId() == null) {
            final Cookie cookie = createCookie(request);
            // add cookie to the response
            response.addCookie(cookie);
            chatSession.setAuthCode(incomingChatData.getAuthCode());
            chatSession.setCurrentSessionId(cookie.getValue());
        }
        if ("Initiate".equals(incomingChatData.getMessage())) {
            ChatData initiateResponse = chatNLPService.generateInitiateChatResponse(incomingChatData, chatSession);
            initiateResponse.setCurrentSessionId(chatSession.getCurrentSessionId());
            initiateResponse.setChatSessionId(chatSession.getCurrentSessionId());
            return new ResponseEntity<>(initiateResponse, HttpStatus.OK);
        }
        ChatData chatResponse = chatNLPService.generateChatBotResponse(incomingChatData, chatSession);
        chatResponse.setCurrentSessionId(chatSession.getCurrentSessionId());
        chatResponse.setChatSessionId(chatSession.getCurrentSessionId());
        LOGGER.debug(">>>> Response Object = {}", chatResponse);
        return new ResponseEntity<>(chatResponse, HttpStatus.OK);
    }

    private boolean containsValidHeaders(HttpServletRequest request) {
        String xBotId = request.getHeader("X-Bot-Id");
        String xCustomerOrigin = request.getHeader("X-Customer-Origin");
        LaunchInfo launchInfo = this.chatNLPService.getSeerBotConfiguration(xBotId).getLaunchInfo();
        boolean allValid = launchInfo.getUniqueBotId().equals(xBotId) &&
                xCustomerOrigin.equals(launchInfo.getAllowedOrigins());
        if (!allValid) {
            LOGGER.error("Invalid access: incoming xBotId = {} , actual botId = {} , xCustomerOrigin = {} "
                            + ", Actual Origin = {} ", xBotId, launchInfo.getUniqueBotId(),
                    xCustomerOrigin, launchInfo.getAllowedOrigins());
        }
        return allValid;
    }

    private Cookie createCookie(HttpServletRequest request) {
        final Cookie cookie = new Cookie("JSESSIONID", request.getSession().getId());
        cookie.setDomain("localhost");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        return cookie;
    }
}
