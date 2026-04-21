package com.aiassistant.service;

import com.aiassistant.command.CommandHandler;
import com.aiassistant.command.CommandResult;
import com.aiassistant.config.AssistantConfig;
import com.aiassistant.conversation.ConversationManager;
import com.aiassistant.conversation.Message;
import com.aiassistant.nlp.IntentAnalysis;
import com.aiassistant.nlp.NlpEngine;
import com.aiassistant.response.ResponseGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssistantService {
    private final ConversationManager conversationManager;
    private final NlpEngine nlpEngine;
    private final ResponseGenerator responseGenerator;
    private final CommandHandler commandHandler;

    public AssistantService() {
        AssistantConfig config = new AssistantConfig();
        this.conversationManager = new ConversationManager(config.getContextWindow());
        this.nlpEngine = new NlpEngine(config.getIntentKeywords());
        this.responseGenerator = new ResponseGenerator(config);
        this.commandHandler = new CommandHandler(config, conversationManager);
    }

    public synchronized String chat(String input) {
        conversationManager.addUserMessage(input);
        IntentAnalysis analysis = nlpEngine.analyze(input);
        String response = responseGenerator.generateResponse(analysis, input, conversationManager);
        conversationManager.addAssistantMessage(response);
        return response;
    }

    public synchronized CommandResult processCommand(String command) {
        return commandHandler.handle(command);
    }

    public List<Message> getHistory() {
        return conversationManager.getHistory();
    }
}
