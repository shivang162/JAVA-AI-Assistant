package com.aiassistant;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Main entry point for the AI Assistant Spring Boot application. */
@SpringBootApplication
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        configureLogging();
        SpringApplication.run(Main.class, args);
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
