package com.aiassistant.conversation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationManagerTest {

    @Test
    void keepsOnlyConfiguredWindowSize() {
        ConversationManager manager = new ConversationManager(2);
        manager.addUserMessage("one");
        manager.addAssistantMessage("two");
        manager.addUserMessage("three");

        assertEquals(2, manager.getHistory().size());
        assertEquals("two", manager.getHistory().get(0).getContent());
        assertEquals("three", manager.getHistory().get(1).getContent());
    }
}
