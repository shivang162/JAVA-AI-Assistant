package com.aiassistant.conversation;

import com.aiassistant.persistence.entity.ConversationMessageEntity;
import com.aiassistant.persistence.repository.ConversationMessageRepository;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Maintains bounded assistant conversation history for context-aware responses. */
public class ConversationManager {
    private final int maxMessages;
    private final List<Message> history;
    private final ConversationMessageRepository conversationMessageRepository;

    public ConversationManager(int maxMessages) {
        this(maxMessages, null);
    }

    public ConversationManager(int maxMessages, ConversationMessageRepository conversationMessageRepository) {
        this.maxMessages = Math.max(2, maxMessages);
        this.conversationMessageRepository = conversationMessageRepository;
        this.history = conversationMessageRepository == null ? new ArrayList<>() : null;
    }

    public synchronized void addUserMessage(String content) {
        addMessage("user", content);
    }

    public synchronized void addAssistantMessage(String content) {
        addMessage("assistant", content);
    }

    private void addMessage(String role, String content) {
        if (conversationMessageRepository != null) {
            conversationMessageRepository.save(new ConversationMessageEntity(role, content, Instant.now()));
            long count = conversationMessageRepository.count();
            if (count > maxMessages) {
                int overflow = (int) (count - maxMessages);
                List<ConversationMessageEntity> oldest = conversationMessageRepository.findByOrderByCreatedAtAsc(PageRequest.of(0, overflow));
                conversationMessageRepository.deleteAllInBatch(oldest);
            }
            return;
        }
        history.add(new Message(role, content));
        while (history.size() > maxMessages) {
            history.remove(0);
        }
    }

    public synchronized List<Message> getHistory() {
        if (conversationMessageRepository != null) {
            List<ConversationMessageEntity> persisted = conversationMessageRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, maxMessages));
            List<Message> ordered = new ArrayList<>(persisted.size());
            for (int i = persisted.size() - 1; i >= 0; i--) {
                ConversationMessageEntity message = persisted.get(i);
                ordered.add(new Message(message.getRole(), message.getContent()));
            }
            return Collections.unmodifiableList(ordered);
        }
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized String getFormattedHistory() {
        List<Message> snapshot = getHistory();
        if (snapshot.isEmpty()) {
            return "No conversation history yet.";
        }
        StringBuilder builder = new StringBuilder();
        for (Message message : snapshot) {
            builder.append(message.getRole()).append(": ").append(message.getContent()).append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    public synchronized String latestUserMessageOrDefault() {
        List<Message> snapshot = getHistory();
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Message message = snapshot.get(i);
            if ("user".equals(message.getRole())) {
                return message.getContent();
            }
        }
        return "";
    }

    public synchronized void clear() {
        if (conversationMessageRepository != null) {
            conversationMessageRepository.deleteAll();
            return;
        }
        history.clear();
    }
}
