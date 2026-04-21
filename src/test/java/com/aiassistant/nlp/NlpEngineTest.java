package com.aiassistant.nlp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NlpEngineTest {

    @Test
    void detectsGreetingIntent() {
        NlpEngine engine = new NlpEngine(Map.of("greeting", List.of("hello", "hi")));
        IntentAnalysis analysis = engine.analyze("hello assistant");
        assertEquals("greeting", analysis.getIntent());
    }

    @Test
    void fallsBackToQuestionIntentForQuestionMark() {
        NlpEngine engine = new NlpEngine(Map.of("greeting", List.of("hello")));
        IntentAnalysis analysis = engine.analyze("Can you explain this?");
        assertEquals("question", analysis.getIntent());
    }
}
