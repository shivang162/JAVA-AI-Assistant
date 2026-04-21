package com.aiassistant.util;

/** Utility class that formats assistant responses for console output. */
public final class ResponseFormatter {
    private ResponseFormatter() {
    }

    public static String formatForConsole(String response) {
        return "AI> " + response.replaceAll("\\s+", " ").trim();
    }
}
