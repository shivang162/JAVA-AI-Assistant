package com.aiassistant;

import com.aiassistant.command.CommandHandler;
import com.aiassistant.command.CommandResult;
import com.aiassistant.config.AssistantConfig;
import com.aiassistant.conversation.ConversationManager;
import com.aiassistant.nlp.IntentAnalysis;
import com.aiassistant.nlp.NlpEngine;
import com.aiassistant.response.ResponseGenerator;
import com.aiassistant.util.ResponseFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/** Main entry point for the interactive Java AI Assistant CLI application. */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        configureLogging();

        AssistantConfig config = new AssistantConfig();
        ConversationManager conversationManager = new ConversationManager(config.getContextWindow());
        NlpEngine nlpEngine = new NlpEngine(config.getIntentKeywords());
        ResponseGenerator responseGenerator = new ResponseGenerator(config);
        CommandHandler commandHandler = new CommandHandler(config, conversationManager);

        System.out.println("Java AI Assistant started. Type /help for commands.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You> ");
                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }

                CommandResult commandResult = commandHandler.handle(input);
                if (commandResult.isHandled()) {
                    System.out.println(ResponseFormatter.formatForConsole(commandResult.getMessage()));
                    if (commandResult.isShouldExit()) {
                        break;
                    }
                    continue;
                }

                conversationManager.addUserMessage(input);
                IntentAnalysis analysis = nlpEngine.analyze(input);
                String response = responseGenerator.generateResponse(analysis, input, conversationManager);
                conversationManager.addAssistantMessage(response);
                System.out.println(ResponseFormatter.formatForConsole(response));
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Assistant stopped due to an unexpected error", ex);
            System.err.println("An unexpected error occurred. Please restart the assistant.");
        }
    }

    private static void configureLogging() {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties")) {
            if (input != null) {
                LogManager.getLogManager().readConfiguration(input);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to load logging configuration", ex);
        }
    }
}
