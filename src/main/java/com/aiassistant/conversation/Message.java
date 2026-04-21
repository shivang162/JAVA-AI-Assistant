package com.aiassistant.conversation;

import java.time.Instant;

/** Represents a single conversation message with role and timestamp. */
public class Message {
    private final String role;
    private final String content;
    private final Instant timestamp;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = Instant.now();
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
