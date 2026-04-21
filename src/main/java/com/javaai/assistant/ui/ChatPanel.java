package com.javaai.assistant.ui;

import com.javaai.assistant.ai.AIAssistantService;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Right panel of the main split pane.
 *
 * <p>Provides a styled chat interface:
 * <ul>
 *   <li>Read-only {@link JTextPane} displaying the conversation history
 *       (user messages in blue, AI replies in green/white, system notes in grey)</li>
 *   <li>Multi-line input text area (Ctrl+Enter to send, Enter for newline)</li>
 *   <li><em>Send</em> and <em>Clear History</em> buttons</li>
 * </ul>
 */
public class ChatPanel extends JPanel {

    // ---- colours -----------------------------------------------------------
    private static final Color PANEL_BG       = new Color(37,  37,  38);
    private static final Color HEADER_BG      = new Color(45,  45,  45);
    private static final Color HISTORY_BG     = new Color(28,  28,  28);
    private static final Color INPUT_BG       = new Color(50,  50,  50);
    private static final Color BORDER_COLOR   = new Color(62,  62,  66);
    private static final Color HEADER_FG      = new Color(200, 200, 200);
    private static final Color USER_COLOR     = new Color(86,  156, 214);
    private static final Color AI_COLOR       = new Color(106, 153,  85);
    private static final Color META_COLOR     = new Color(120, 120, 120);
    private static final Color TEXT_COLOR     = new Color(212, 212, 212);
    private static final Color BTN_SEND_BG    = new Color(14,   99, 156);
    private static final Color BTN_CLEAR_BG   = new Color(80,   50,  50);
    private static final Color BTN_FG         = Color.WHITE;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // -----------------------------------------------------------------------
    private JTextPane historyPane;
    private JTextArea inputArea;
    private final AIAssistantService aiService;

    // Named styles
    private Style userStyle;
    private Style aiStyle;
    private Style metaStyle;
    private Style codeStyle;
    private Style normalStyle;

    // -----------------------------------------------------------------------
    public ChatPanel(AIAssistantService aiService) {
        super(new BorderLayout());
        this.aiService = aiService;
        setBackground(PANEL_BG);

        // ---- Header --------------------------------------------------------
        JLabel header = new JLabel("  🤖 AI Assistant  (Java only)");
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setForeground(HEADER_FG);
        header.setBackground(HEADER_BG);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        add(header, BorderLayout.NORTH);

        // ---- Chat history pane --------------------------------------------
        historyPane = new JTextPane();
        historyPane.setEditable(false);
        historyPane.setBackground(HISTORY_BG);
        historyPane.setForeground(TEXT_COLOR);
        historyPane.setFont(new Font("SansSerif", Font.PLAIN, 13));
        historyPane.setMargin(new Insets(6, 6, 6, 6));
        initStyles();

        JScrollPane historyScroll = new JScrollPane(historyPane);
        historyScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        historyScroll.getViewport().setBackground(HISTORY_BG);
        add(historyScroll, BorderLayout.CENTER);

        // ---- Input area ----------------------------------------------------
        JPanel inputPanel = buildInputPanel();
        add(inputPanel, BorderLayout.SOUTH);

        // Show welcome message
        appendSystem("Welcome! Ask anything about Java. Press Ctrl+Enter or click Send.");
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Pre-fills the input area with a prompt asking the AI to review the given
     * Java code snippet and immediately sends it.
     */
    public void sendCodeToAi(String code) {
        String prompt = "Please review and explain the following Java code:\n\n```java\n" + code + "\n```";
        inputArea.setText(prompt);
        doSend();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        // Multi-line input
        inputArea = new JTextArea(3, 30);
        inputArea.setBackground(INPUT_BG);
        inputArea.setForeground(TEXT_COLOR);
        inputArea.setCaretColor(Color.WHITE);
        inputArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        inputArea.setToolTipText("Type a message – Ctrl+Enter to send, Enter for new line");

        // Ctrl+Enter sends; plain Enter inserts newline
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER
                        && (e.isControlDown() || e.isMetaDown())) {
                    e.consume();
                    doSend();
                }
            }
        });

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        panel.add(inputScroll, BorderLayout.CENTER);

        // Button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setBackground(PANEL_BG);

        JLabel hint = new JLabel("Ctrl+Enter to send");
        hint.setForeground(META_COLOR);
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));

        JButton sendBtn  = createButton("Send ▶",        BTN_SEND_BG,  "Send message (Ctrl+Enter)");
        JButton clearBtn = createButton("Clear History",  BTN_CLEAR_BG, "Clear the conversation history");

        sendBtn .addActionListener(e -> doSend());
        clearBtn.addActionListener(e -> doClearHistory());

        btnRow.add(hint);
        btnRow.add(clearBtn);
        btnRow.add(sendBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    private void doSend() {
        String message = inputArea.getText().trim();
        if (message.isBlank()) return;

        inputArea.setText("");
        inputArea.setEnabled(false);

        appendUser(message);
        appendSystem("⏳ Thinking…");

        aiService.sendMessageAsync(message).thenAccept(reply -> {
            SwingUtilities.invokeLater(() -> {
                removeLastSystemMessage();
                appendAi(reply);
                inputArea.setEnabled(true);
                inputArea.requestFocusInWindow();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                removeLastSystemMessage();
                appendSystem("⚠️  Error: " + ex.getMessage());
                inputArea.setEnabled(true);
            });
            return null;
        });
    }

    private void doClearHistory() {
        aiService.clearHistory();
        historyPane.setText("");
        appendSystem("Conversation history cleared.");
    }

    // -----------------------------------------------------------------------
    // Styled text helpers
    // -----------------------------------------------------------------------

    private void initStyles() {
        StyledDocument doc = historyPane.getStyledDocument();

        metaStyle = doc.addStyle("meta", null);
        StyleConstants.setForeground(metaStyle, META_COLOR);
        StyleConstants.setFontSize(metaStyle, 11);
        StyleConstants.setItalic(metaStyle, true);
        StyleConstants.setFontFamily(metaStyle, "SansSerif");

        userStyle = doc.addStyle("user", null);
        StyleConstants.setForeground(userStyle, USER_COLOR);
        StyleConstants.setFontSize(userStyle, 13);
        StyleConstants.setBold(userStyle, true);
        StyleConstants.setFontFamily(userStyle, "SansSerif");

        aiStyle = doc.addStyle("ai", null);
        StyleConstants.setForeground(aiStyle, AI_COLOR);
        StyleConstants.setFontSize(aiStyle, 13);
        StyleConstants.setBold(aiStyle, true);
        StyleConstants.setFontFamily(aiStyle, "SansSerif");

        normalStyle = doc.addStyle("normal", null);
        StyleConstants.setForeground(normalStyle, TEXT_COLOR);
        StyleConstants.setFontSize(normalStyle, 13);
        StyleConstants.setFontFamily(normalStyle, "SansSerif");

        codeStyle = doc.addStyle("code", null);
        StyleConstants.setForeground(codeStyle, new Color(206, 145, 120));
        StyleConstants.setFontFamily(codeStyle, "Monospaced");
        StyleConstants.setFontSize(codeStyle, 13);
        StyleConstants.setBackground(codeStyle, new Color(40, 40, 40));
    }

    private void appendUser(String text) {
        appendStyledLine("You [" + now() + "]", userStyle);
        appendFormattedText(text);
        appendNewline();
    }

    private void appendAi(String text) {
        appendStyledLine("AI Assistant [" + now() + "]", aiStyle);
        appendFormattedText(text);
        appendNewline();
    }

    private void appendSystem(String text) {
        appendStyledLine(text, metaStyle);
    }

    /**
     * Formats the text, rendering fenced {@code ```...```} code blocks in the
     * monospaced code style and the rest in the normal style.
     */
    private void appendFormattedText(String text) {
        // Split on ``` fences
        String[] parts = text.split("```", -1);
        boolean inCode = false;
        for (String part : parts) {
            if (inCode) {
                // Strip optional language tag on the first line
                String code = part.replaceFirst("^[a-zA-Z]*\n", "");
                appendStyled(code + "\n", codeStyle);
            } else {
                appendStyled(part, normalStyle);
            }
            inCode = !inCode;
        }
    }

    private void appendStyledLine(String text, Style style) {
        appendStyled(text + "\n", style);
        scrollToBottom();
    }

    private void appendStyled(String text, Style style) {
        try {
            StyledDocument doc = historyPane.getStyledDocument();
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException ignored) { }
    }

    private void appendNewline() {
        appendStyled("\n", normalStyle);
        scrollToBottom();
    }

    /** Removes the last line (used to delete the "⏳ Thinking…" indicator). */
    private void removeLastSystemMessage() {
        try {
            StyledDocument doc = historyPane.getStyledDocument();
            String text = doc.getText(0, doc.getLength());
            // Find the last non-empty line start
            int end = text.length();
            // Walk back over the trailing newline
            while (end > 0 && text.charAt(end - 1) == '\n') end--;
            int start = text.lastIndexOf('\n', end - 1) + 1;
            if (start >= 0 && end > start) {
                doc.remove(start, end - start + (end < text.length() ? 1 : 0));
            }
        } catch (BadLocationException ignored) { }
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() ->
                historyPane.setCaretPosition(historyPane.getDocument().getLength()));
    }

    private String now() {
        return LocalTime.now().format(TIME_FMT);
    }

    private JButton createButton(String text, Color bg, String tooltip) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(BTN_FG);
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        return btn;
    }
}
