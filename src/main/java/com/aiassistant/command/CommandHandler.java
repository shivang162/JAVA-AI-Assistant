package com.aiassistant.command;

import com.aiassistant.config.AssistantConfig;
import com.aiassistant.conversation.ConversationManager;

import java.util.Map;
import java.util.stream.Collectors;

/** Processes slash commands such as help/history/clear/exit. */
public class CommandHandler {
    private final Map<String, String> commands;
    private final ConversationManager conversationManager;

    public CommandHandler(AssistantConfig config, ConversationManager conversationManager) {
        this.commands = config.getCommands();
        this.conversationManager = conversationManager;
    }

    public CommandResult handle(String input) {
        if (!input.startsWith("/")) {
            return CommandResult.ignored();
        }

        switch (input) {
            case "/help":
                String help = commands.entrySet().stream()
                        .map(e -> e.getKey() + " - " + e.getValue())
                        .sorted()
                        .collect(Collectors.joining(System.lineSeparator()));
                return CommandResult.handled(help);
            case "/history":
                return CommandResult.handled(conversationManager.getFormattedHistory());
            case "/clear":
                conversationManager.clear();
                return CommandResult.handled("Conversation history cleared.");
            case "/exit":
                return CommandResult.exit("Goodbye.");
            default:
                return CommandResult.handled("Unknown command. Use /help to list commands.");
        }
    }
}
