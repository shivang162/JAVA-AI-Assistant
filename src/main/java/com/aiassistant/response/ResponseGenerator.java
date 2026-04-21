package com.aiassistant.response;

import com.aiassistant.config.AssistantConfig;
import com.aiassistant.conversation.ConversationManager;
import com.aiassistant.nlp.IntentAnalysis;

import java.util.List;
import java.util.Map;
import java.util.Random;

/** Generates template-based responses with temperature-controlled variation. */
public class ResponseGenerator {
    private final double temperature;
    private final Random random;

    private final Map<String, List<String>> templates = Map.of(
            "greeting", List.of(
                    "Hello! I'm your Java AI Assistant. How can I help today?",
                    "Hi there! Ask me anything about Java code, debugging, or design.",
                    "Hey! Ready to work through your Java question."
            ),
            "code_help", List.of(
                    "I can help debug that Java issue. Share the relevant snippet and expected behavior.",
                    "Let's analyze your Java code step-by-step. Start by sharing the failing part.",
                    "Good call asking early. Provide the stack trace or compiler message and I'll guide the fix."
            ),
            "question", List.of(
                    "Great question. Based on your context, start with clear method boundaries and readable naming.",
                    "Here's a practical approach: break the problem into small Java methods and test each part.",
                    "I recommend handling edge cases first, then implementing the happy path cleanly."
            ),
            "goodbye", List.of(
                    "Goodbye! Come back anytime you need Java help.",
                    "See you next time. Happy coding!",
                    "Bye! Keep shipping clean Java code."
            ),
            "general", List.of(
                    "I'm here to help with Java development. Tell me what you're building.",
                    "Share your Java goal and I can suggest a concrete next step.",
                    "I can assist with Java architecture, debugging, testing, and best practices."
            )
    );

    public ResponseGenerator(AssistantConfig config) {
        this(config, new Random());
    }

    public ResponseGenerator(AssistantConfig config, Random random) {
        this.temperature = config.getTemperature();
        this.random = random;
    }

    public String generateResponse(IntentAnalysis analysis, String userInput, ConversationManager conversationManager) {
        List<String> options = templates.getOrDefault(analysis.getIntent(), templates.get("general"));
        String selected = pickTemplate(options);
        if (analysis.getConfidence() < 0.25 && !conversationManager.latestUserMessageOrDefault().isBlank()) {
            return selected + " I may need more details to be precise.";
        }
        if ("code_help".equals(analysis.getIntent()) && userInput.length() < 20) {
            return selected + " Include error output, code snippet, and what you expected.";
        }
        return selected;
    }

    private String pickTemplate(List<String> options) {
        if (options.size() == 1 || temperature <= 0.15) {
            return options.get(0);
        }

        int topBand = Math.max(1, (int) Math.round(options.size() * Math.min(1.0, temperature)));
        return options.get(random.nextInt(topBand));
    }
}
