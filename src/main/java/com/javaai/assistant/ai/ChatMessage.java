package com.javaai.assistant.ai;

/**
 * Immutable representation of a single chat turn (role + content).
 * Roles follow the OpenAI convention: "system", "user", "assistant".
 */
public final class ChatMessage {

    private final String role;
    private final String content;

    public ChatMessage(String role, String content) {
        this.role    = role;
        this.content = content;
    }

    public String getRole()    { return role; }
    public String getContent() { return content; }
}
