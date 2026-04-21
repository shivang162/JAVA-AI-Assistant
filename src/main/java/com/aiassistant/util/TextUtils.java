package com.aiassistant.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Utility methods for text normalization and tokenization. */
public final class TextUtils {
    private TextUtils() {
    }

    public static String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    public static List<String> tokenize(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(normalized.split(" ")).collect(Collectors.toList());
    }
}
