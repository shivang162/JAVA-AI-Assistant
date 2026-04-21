package com.aiassistant.conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Maintains bounded assistant conversation history for context-aware responses. */
public class ConversationManager {
    private final int maxMessages;
    private final List<Message> history = new ArrayList<>();

    public ConversationManager(int maxMessages) {
        this.maxMessages = Math.max(2, maxMessages);
    }

    public synchronized void addUserMessage(String content) {
        addMessage("user", content);
    }

    public synchronized void addAssistantMessage(String content) {
        addMessage("assistant", content);
    }

    private void addMessage(String role, String content) {
        history.add(new Message(role, content));
        while (history.size() > maxMessages) {
            history.remove(0);
        }
    }

    public synchronized List<Message> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized String getFormattedHistory() {
        if (history.isEmpty()) {
            return "No conversation history yet.";
        }
        StringBuilder builder = new StringBuilder();
        for (Message message : history) {
            builder.append(message.getRole()).append(": ").append(message.getContent()).append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    public synchronized String latestUserMessageOrDefault() {
        for (int i = history.size() - 1; i >= 0; i--) {
            Message message = history.get(i);
            if ("user".equals(message.getRole())) {
                return message.getContent();
            }
        }
        return "";
    }

    public synchronized void clear() {
        history.clear();
    }
}
