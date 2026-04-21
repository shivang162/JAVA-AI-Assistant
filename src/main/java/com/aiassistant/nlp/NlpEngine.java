package com.aiassistant.nlp;

import com.aiassistant.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Performs lightweight NLP analysis and rule-based intent detection. */
public class NlpEngine {
    private final Map<String, List<String>> intentKeywords;

    public NlpEngine(Map<String, List<String>> intentKeywords) {
        this.intentKeywords = intentKeywords;
    }

    public IntentAnalysis analyze(String input) {
        List<String> tokens = TextUtils.tokenize(input);
        if (tokens.isEmpty()) {
            return new IntentAnalysis("general", 0.1, List.of());
        }

        String bestIntent = "general";
        List<String> matched = new ArrayList<>();
        int bestScore = 0;

        for (Map.Entry<String, List<String>> entry : intentKeywords.entrySet()) {
            List<String> localMatches = new ArrayList<>();
            for (String keyword : entry.getValue()) {
                if (tokens.contains(keyword)) {
                    localMatches.add(keyword);
                }
            }
            if (localMatches.size() > bestScore) {
                bestScore = localMatches.size();
                bestIntent = entry.getKey();
                matched = localMatches;
            }
        }

        if (bestScore == 0 && input.contains("?")) {
            bestIntent = "question";
        }

        double confidence = Math.min(1.0, bestScore / (double) Math.max(1, tokens.size()));
        if ("general".equals(bestIntent) && confidence == 0) {
            confidence = 0.2;
        }
        return new IntentAnalysis(bestIntent, confidence, matched);
    }
}
