package com.seerlogics.chatbot.model.chat;

import com.lingoace.model.BaseModel;

import javax.persistence.*;

/**
 * Created by bkane on 4/1/18.
 */
@Entity
@Table(name = "CHAT")
public class ChatData extends BaseModel {

    @Column(nullable = false)
    private String message;

    @Column(name = "ACCOUNT_ID")
    private String accountId;

    @Column(name="CHAT_SESSION_ID", nullable = false)
    private String chatSessionId;

    @Transient
    private String previousChatId;

    @Transient
    private Object currentSessionId;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "PREVIOUS_CHAT_ID", nullable = true)
    private ChatData previousChat;

    // this can be used to pass auth keys such as JWT/oAuth keys
    @Column(name= "AUTH_CODE")
    private String authCode;

    @Column(nullable = true, length = 3000)
    private String response;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getPreviousChatId() {
        return previousChatId;
    }

    public void setPreviousChatId(String previousChatId) {
        this.previousChatId = previousChatId;
    }

    public ChatData getPreviousChat() {
        return previousChat;
    }

    public void setPreviousChat(ChatData previousChat) {
        this.previousChat = previousChat;
    }

    public String getChatSessionId() {
        return chatSessionId;
    }

    public void setChatSessionId(String chatSessionId) {
        this.chatSessionId = chatSessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Object getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(Object currentSessionId) {
        this.currentSessionId = currentSessionId;
    }
}
