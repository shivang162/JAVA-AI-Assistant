package com.aiassistant.command;

import com.aiassistant.config.AssistantConfig;
import com.aiassistant.conversation.ConversationManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHandlerTest {

    @Test
    void handlesHelpCommand() {
        CommandHandler handler = new CommandHandler(new AssistantConfig(), new ConversationManager(10));
        CommandResult result = handler.handle("/help");

        assertTrue(result.isHandled());
        assertTrue(result.getMessage().contains("/help"));
    }

    @Test
    void handlesUnknownSlashCommand() {
        CommandHandler handler = new CommandHandler(new AssistantConfig(), new ConversationManager(10));
        CommandResult result = handler.handle("/unknown");

        assertTrue(result.isHandled());
        assertTrue(result.getMessage().contains("Unknown command"));
    }
}
