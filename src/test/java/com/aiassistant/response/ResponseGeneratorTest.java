package com.aiassistant.response;

import com.aiassistant.config.AssistantConfig;
import com.aiassistant.conversation.ConversationManager;
import com.aiassistant.nlp.IntentAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ResponseGeneratorTest {

    @Test
    void generatesResponseForKnownIntent() {
        ResponseGenerator generator = new ResponseGenerator(new AssistantConfig(), new Random(1));
        String response = generator.generateResponse(
                new IntentAnalysis("greeting", 0.8, List.of("hello")),
                "hello",
                new ConversationManager(10)
        );

        assertFalse(response.isBlank());
    }
}
