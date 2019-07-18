package com.seerlogics.chatbot.repository.chat;

import com.seerlogics.chatbot.model.chat.ChatData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by bkane on 4/3/18.
 */
@Repository
public interface ChatRepository extends JpaRepository<ChatData, Long> {
    List<ChatData> findByAccountId(String accountId);
    List<ChatData> findByChatSessionId(String chatSessionId);
}
