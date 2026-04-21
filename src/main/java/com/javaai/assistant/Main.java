package com.javaai.assistant;

import com.javaai.assistant.ui.MainFrame;

import javax.swing.*;

/**
 * Application entry point.
 * Set the OPENAI_API_KEY environment variable before launching, or enter it
 * directly in the toolbar API-key field at runtime.
 */
public class Main {

    public static void main(String[] args) {
        // Read optional API key from environment
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) apiKey = "";

        final String key = apiKey;

        SwingUtilities.invokeLater(() -> {
            // Use Nimbus look-and-feel when available
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                // fall back to system L&F
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored2) { /* use default */ }
            }
            new MainFrame(key).setVisible(true);
        });
    }
}
