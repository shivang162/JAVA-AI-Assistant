package com.aiassistant.nlp;

import java.util.Collections;
import java.util.List;

/** Result of NLP text analysis containing detected intent and confidence. */
public class IntentAnalysis {
    private final String intent;
    private final double confidence;
    private final List<String> matchedKeywords;

    public IntentAnalysis(String intent, double confidence, List<String> matchedKeywords) {
        this.intent = intent;
        this.confidence = confidence;
        this.matchedKeywords = matchedKeywords;
    }

    public String getIntent() {
        return intent;
    }

    public double getConfidence() {
        return confidence;
    }

    public List<String> getMatchedKeywords() {
        return Collections.unmodifiableList(matchedKeywords);
    }
}
