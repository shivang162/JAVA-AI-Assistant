package com.aiassistant.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads assistant runtime configuration and static command/intent databases from resources. */
public class AssistantConfig {
    private static final Logger LOGGER = Logger.getLogger(AssistantConfig.class.getName());

    private final Properties aiProperties = loadProperties("ai.properties");
    private final Map<String, List<String>> intentKeywords = loadCsvMap("intents.properties");
    private final Map<String, String> commands = loadTextMap("commands.properties");

    private Properties loadProperties(String resourceName) {
        Properties properties = new Properties();
        try (InputStream inputStream = getResource(resourceName)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Unable to load {0}", resourceName);
        }
        return properties;
    }

    private Map<String, List<String>> loadCsvMap(String resourceName) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        Properties properties = loadProperties(resourceName);
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key, "");
            List<String> keywords = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .toList();
            map.put(key, keywords);
        }
        return map;
    }

    private Map<String, String> loadTextMap(String resourceName) {
        Map<String, String> map = new LinkedHashMap<>();
        Properties properties = loadProperties(resourceName);
        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
    }

    private InputStream getResource(String resourceName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
    }

    public double getTemperature() {
        return parseDouble(aiProperties.getProperty("ai.temperature"), 0.7d);
    }

    public int getMaxTokens() {
        return parseInt(aiProperties.getProperty("ai.maxTokens"), 256);
    }

    public int getContextWindow() {
        return parseInt(aiProperties.getProperty("ai.contextWindow"), 20);
    }

    public Map<String, List<String>> getIntentKeywords() {
        return intentKeywords;
    }

    public Map<String, String> getCommands() {
        return commands;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
