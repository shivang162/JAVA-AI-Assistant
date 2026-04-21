package com.javaai.assistant.ui;

import com.javaai.assistant.ai.AIAssistantService;
import com.javaai.assistant.compiler.CompilationResult;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application window for the Java AI Assistant.
 *
 * <p>Layout (top-to-bottom):
 * <ol>
 *   <li>Menu bar – File / Help</li>
 *   <li>Toolbar – app title, API-key field, Set-Key button, Clear-Chat button</li>
 *   <li>Main split pane (horizontal) – {@link CodeEditorPanel} left | {@link ChatPanel} right</li>
 *   <li>Compiler output panel (collapsible, at the bottom)</li>
 * </ol>
 */
public class MainFrame extends JFrame {

    // ---- colours -----------------------------------------------------------
    private static final Color FRAME_BG       = new Color(37,  37,  38);
    private static final Color TOOLBAR_BG     = new Color(45,  45,  45);
    private static final Color BORDER_COLOR   = new Color(62,  62,  66);
    private static final Color TITLE_FG       = new Color(255, 215,   0);  // gold
    private static final Color LABEL_FG       = new Color(200, 200, 200);
    private static final Color OUTPUT_BG      = new Color(25,  25,  25);
    private static final Color SUCCESS_COLOR  = new Color(106, 153,  85);
    private static final Color ERROR_COLOR    = new Color(220,  80,  80);
    private static final Color META_COLOR     = new Color(120, 120, 120);

    // -----------------------------------------------------------------------
    private final AIAssistantService aiService;
    private final CodeEditorPanel    editorPanel;
    private final ChatPanel          chatPanel;

    private JTextPane  outputPane;
    private JTextField apiKeyField;
    private JPanel     outputContainer;
    private boolean    outputVisible = true;

    // -----------------------------------------------------------------------
    public MainFrame(String initialApiKey) {
        super("Java AI Assistant");
        this.aiService   = new AIAssistantService(initialApiKey);
        this.editorPanel = new CodeEditorPanel();
        this.chatPanel   = new ChatPanel(aiService);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setIconImage(createAppIcon());

        getContentPane().setBackground(FRAME_BG);
        setLayout(new BorderLayout());

        setJMenuBar(buildMenuBar());
        add(buildToolbar(),         BorderLayout.NORTH);
        add(buildMainSplitPane(),   BorderLayout.CENTER);
        add(buildOutputContainer(), BorderLayout.SOUTH);

        // Wire editor → compiler output
        editorPanel.setOnCompileCallback(this::showCompilationResult);

        // Wire editor → chat
        editorPanel.setOnAskAiCallback(chatPanel::sendCodeToAi);

        // Confirm before closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(
                        MainFrame.this,
                        "Exit Java AI Assistant?", "Confirm Exit",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        // If no key was provided, focus the API key field so the user notices it
        if (initialApiKey.isBlank()) {
            SwingUtilities.invokeLater(() -> apiKeyField.requestFocusInWindow());
        }
    }

    // -----------------------------------------------------------------------
    // Menu bar
    // -----------------------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(TOOLBAR_BG);
        menuBar.setBorder(BorderFactory.createEmptyBorder());

        // ---- File ----------------------------------------------------------
        JMenu fileMenu = menu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitItem.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // ---- View ----------------------------------------------------------
        JMenu viewMenu = menu("View");
        JMenuItem toggleOutput = new JMenuItem("Toggle Compiler Output");
        toggleOutput.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        toggleOutput.addActionListener(e -> toggleOutputPanel());
        viewMenu.add(toggleOutput);
        menuBar.add(viewMenu);

        // ---- Help ----------------------------------------------------------
        JMenu helpMenu = menu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JMenu menu(String text) {
        JMenu m = new JMenu(text);
        m.setForeground(LABEL_FG);
        return m;
    }

    // -----------------------------------------------------------------------
    // Toolbar
    // -----------------------------------------------------------------------

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(TOOLBAR_BG);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Title
        JLabel title = new JLabel("  ☕ Java AI Assistant");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(TITLE_FG);
        title.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 16));
        toolbar.add(title, BorderLayout.WEST);

        // API key panel (right side)
        JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        keyPanel.setBackground(TOOLBAR_BG);

        JLabel keyLabel = new JLabel("OpenAI API Key:");
        keyLabel.setForeground(LABEL_FG);
        keyLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        apiKeyField = new JTextField(28);
        apiKeyField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        apiKeyField.setBackground(new Color(50, 50, 50));
        apiKeyField.setForeground(LABEL_FG);
        apiKeyField.setCaretColor(Color.WHITE);
        apiKeyField.setToolTipText("Enter your OpenAI API key here, or set OPENAI_API_KEY env var");
        // Show masked placeholder when an initial key was already set via env
        String storedKey = System.getenv("OPENAI_API_KEY");
        if (storedKey != null && !storedKey.isBlank()) {
            apiKeyField.setText(storedKey);
        }

        JButton setKeyBtn = smallButton("Set Key", new Color(14, 99, 156));
        setKeyBtn.setToolTipText("Apply the API key");
        setKeyBtn.addActionListener(e -> {
            String key = apiKeyField.getText().trim();
            aiService.setApiKey(key);
            showOutputMessage(key.isBlank()
                    ? "API key cleared."
                    : "✔ API key set (length " + key.length() + " chars).",
                    META_COLOR);
        });

        JButton clearChatBtn = smallButton("Clear Chat", new Color(80, 50, 50));
        clearChatBtn.setToolTipText("Clear the conversation history");
        clearChatBtn.addActionListener(e -> aiService.clearHistory());

        keyPanel.add(keyLabel);
        keyPanel.add(apiKeyField);
        keyPanel.add(setKeyBtn);
        keyPanel.add(Box.createHorizontalStrut(8));
        keyPanel.add(clearChatBtn);

        toolbar.add(keyPanel, BorderLayout.EAST);
        return toolbar;
    }

    // -----------------------------------------------------------------------
    // Main split pane
    // -----------------------------------------------------------------------

    private JSplitPane buildMainSplitPane() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, chatPanel);
        split.setResizeWeight(0.62);           // editor gets 62 % by default
        split.setDividerSize(5);
        split.setBorder(null);
        split.setBackground(FRAME_BG);
        return split;
    }

    // -----------------------------------------------------------------------
    // Compiler output panel
    // -----------------------------------------------------------------------

    private JPanel buildOutputContainer() {
        outputContainer = new JPanel(new BorderLayout());
        outputContainer.setBackground(FRAME_BG);
        outputContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        // Header row
        JPanel outputHeader = new JPanel(new BorderLayout());
        outputHeader.setBackground(new Color(45, 45, 45));

        JLabel outputTitle = new JLabel("  🔨 Compiler Output");
        outputTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        outputTitle.setForeground(LABEL_FG);
        outputTitle.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        outputHeader.add(outputTitle, BorderLayout.WEST);

        JButton toggleBtn = smallButton("▲ Hide", new Color(60, 60, 60));
        toggleBtn.addActionListener(e -> {
            toggleOutputPanel();
            toggleBtn.setText(outputVisible ? "▲ Hide" : "▼ Show");
        });
        outputHeader.add(toggleBtn, BorderLayout.EAST);
        outputContainer.add(outputHeader, BorderLayout.NORTH);

        // Output text pane
        outputPane = new JTextPane();
        outputPane.setEditable(false);
        outputPane.setBackground(OUTPUT_BG);
        outputPane.setForeground(LABEL_FG);
        outputPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputPane.setMargin(new Insets(4, 6, 4, 6));

        JScrollPane outputScroll = new JScrollPane(outputPane);
        outputScroll.setPreferredSize(new Dimension(0, 130));
        outputScroll.setBorder(null);
        outputScroll.getViewport().setBackground(OUTPUT_BG);
        outputContainer.add(outputScroll, BorderLayout.CENTER);

        showOutputMessage("Ready – click ▶ Compile or press Ctrl+B to compile your Java code.", META_COLOR);
        return outputContainer;
    }

    // -----------------------------------------------------------------------
    // Callbacks
    // -----------------------------------------------------------------------

    private void showCompilationResult(CompilationResult result) {
        Color color = result.isSuccess() ? SUCCESS_COLOR : ERROR_COLOR;
        showOutputMessage(result.getOutput(), color);
    }

    private void showOutputMessage(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = outputPane.getStyledDocument();
            Style style = outputPane.addStyle("msg", null);
            StyleConstants.setForeground(style, color);
            StyleConstants.setFontFamily(style, "Monospaced");
            StyleConstants.setFontSize(style, 13);
            try {
                doc.remove(0, doc.getLength());
                doc.insertString(0, message, style);
            } catch (BadLocationException ignored) { }
        });
    }

    private void toggleOutputPanel() {
        outputVisible = !outputVisible;
        // find the scroll pane (CENTER child)
        Component center = ((BorderLayout) outputContainer.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        if (center != null) center.setVisible(outputVisible);
        outputContainer.revalidate();
        outputContainer.repaint();
    }

    // -----------------------------------------------------------------------
    // About dialog
    // -----------------------------------------------------------------------

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "<html><b>Java AI Assistant</b> v1.0<br><br>" +
                "A Swing-based Java IDE with an integrated AI chat assistant.<br><br>" +
                "<b>Features:</b><br>" +
                "• Syntax-highlighted Java code editor<br>" +
                "• Java compiler (javax.tools) – compile-only, no execution<br>" +
                "• OpenAI-powered chat assistant (Java topics only)<br><br>" +
                "<b>Requirements:</b> JDK 11+, OPENAI_API_KEY environment variable<br>" +
                "or key entered in the toolbar.</html>",
                "About Java AI Assistant",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private JButton smallButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        return btn;
    }

    /** Creates a small coffee-cup icon programmatically (no external image needed). */
    private Image createAppIcon() {
        int sz = 32;
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(sz, sz, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(30, 30, 30));
        g.fillRoundRect(0, 0, sz, sz, 8, 8);
        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        String icon = "J";
        g.drawString(icon, (sz - fm.stringWidth(icon)) / 2, (sz + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }
}
