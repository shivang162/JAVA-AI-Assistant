package com.javaai.assistant.ui;

import com.javaai.assistant.ai.AIAssistantService;
import com.javaai.assistant.compiler.CompilationResult;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/** Main application window for Java Editor Pro. */
public class MainFrame extends JFrame {

    private static final Color FRAME_BG_DARK = new Color(37, 37, 38);
    private static final Color TOOLBAR_BG_DARK = new Color(45, 45, 45);
    private static final Color BORDER_COLOR_DARK = new Color(62, 62, 66);
    private static final Color LABEL_FG_DARK = new Color(200, 200, 200);
    private static final Color OUTPUT_BG_DARK = new Color(25, 25, 25);

    private static final Color FRAME_BG_LIGHT = new Color(243, 243, 243);
    private static final Color TOOLBAR_BG_LIGHT = new Color(232, 232, 232);
    private static final Color BORDER_COLOR_LIGHT = new Color(204, 204, 204);
    private static final Color LABEL_FG_LIGHT = new Color(33, 33, 33);
    private static final Color OUTPUT_BG_LIGHT = new Color(255, 255, 255);

    private static final Color SUCCESS_COLOR = new Color(106, 153, 85);
    private static final Color ERROR_COLOR = new Color(220, 80, 80);
    private static final Color META_COLOR = new Color(120, 120, 120);

    private final AIAssistantService aiService;
    private final CodeEditorPanel editorPanel;
    private final ChatPanel aiPanel;

    private JTextPane outputPane;
    private JTextField apiKeyField;
    private JPanel outputContainer;
    private JPanel toolbar;
    private boolean outputVisible = true;
    private boolean darkTheme = true;

    public MainFrame(String initialApiKey) {
        super("Java Editor Pro");
        this.aiService = new AIAssistantService(initialApiKey);
        this.editorPanel = new CodeEditorPanel();
        this.aiPanel = new ChatPanel(aiService);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1360, 860);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        setJMenuBar(buildMenuBar());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildMainSplitPane(), BorderLayout.CENTER);
        add(buildOutputContainer(), BorderLayout.SOUTH);

        editorPanel.setOnCompileCallback(this::showCompilationResult);
        editorPanel.setOnAskAiCallback(aiPanel::sendCodeToAi);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(
                        MainFrame.this,
                        "Exit Java Editor Pro?",
                        "Confirm Exit",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        showOutputMessage("Ready. " + editorPanel.getJdkInfo(), META_COLOR);

        if (initialApiKey.isBlank()) {
            SwingUtilities.invokeLater(() -> apiKeyField.requestFocusInWindow());
        }
        applyTheme(true);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = menu("File");
        JMenuItem newItem = new JMenuItem("New Tab");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newItem.addActionListener(e -> editorPanel.createNewTab());

        JMenuItem openItem = new JMenuItem("Open Java File…");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        openItem.addActionListener(e -> doOpen());

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(e -> editorPanel.saveCurrentFile(this, false));

        JMenuItem saveAsItem = new JMenuItem("Save As…");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> editorPanel.saveCurrentFile(this, true));

        JMenuItem findReplaceItem = new JMenuItem("Find & Replace");
        findReplaceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        findReplaceItem.addActionListener(e -> editorPanel.showFindReplaceDialog(this));

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitItem.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(findReplaceItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu viewMenu = menu("View");
        JMenuItem toggleTheme = new JMenuItem("Toggle Dark/Light Theme");
        toggleTheme.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        toggleTheme.addActionListener(e -> applyTheme(!darkTheme));

        JMenuItem toggleOutput = new JMenuItem("Toggle Compiler Output");
        toggleOutput.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        toggleOutput.addActionListener(e -> toggleOutputPanel());

        viewMenu.add(toggleTheme);
        viewMenu.add(toggleOutput);
        menuBar.add(viewMenu);

        JMenu helpMenu = menu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JMenu menu(String text) {
        return new JMenu(text);
    }

    private JPanel buildToolbar() {
        toolbar = new JPanel(new BorderLayout());

        JLabel title = new JLabel("  ☕ Java Editor Pro");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 16));
        toolbar.add(title, BorderLayout.WEST);

        JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));

        JLabel keyLabel = new JLabel("OpenAI API Key:");
        keyLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        apiKeyField = new JTextField(28);
        apiKeyField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        apiKeyField.setToolTipText("Enter your OpenAI API key here, or set OPENAI_API_KEY env var");
        String storedKey = System.getenv("OPENAI_API_KEY");
        if (storedKey != null && !storedKey.isBlank()) {
            apiKeyField.setText(storedKey);
        }

        JButton setKeyBtn = smallButton("Set Key", new Color(14, 99, 156));
        setKeyBtn.addActionListener(e -> {
            String key = apiKeyField.getText().trim();
            aiService.setApiKey(key);
            showOutputMessage(key.isBlank() ? "API key cleared." : "✔ API key set.", META_COLOR);
        });

        JButton clearChatBtn = smallButton("Clear AI Chat", new Color(80, 50, 50));
        clearChatBtn.addActionListener(e -> aiService.clearHistory());

        keyPanel.add(keyLabel);
        keyPanel.add(apiKeyField);
        keyPanel.add(setKeyBtn);
        keyPanel.add(Box.createHorizontalStrut(8));
        keyPanel.add(clearChatBtn);

        toolbar.add(keyPanel, BorderLayout.EAST);
        return toolbar;
    }

    private JSplitPane buildMainSplitPane() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, buildRightPanel());
        split.setResizeWeight(0.64);
        split.setDividerSize(5);
        split.setBorder(null);
        return split;
    }

    private JComponent buildRightPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("AI Assistant", aiPanel);
        tabs.addTab("Team Chat", placeholderPanel("Team Chat", "Socket/WebSocket-ready chat panel placeholder for multi-device real-time messaging."));
        tabs.addTab("Collaboration", placeholderPanel("Live Collaboration", "Collaborative cursor/session panel scaffold for shared Java editing sessions."));
        tabs.addTab("Video", placeholderPanel("Video Conferencing", "WebRTC integration panel scaffold for room-based calls and screen sharing."));
        return tabs;
    }

    private JPanel placeholderPanel(String title, String text) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel header = new JLabel("  " + title);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        JTextArea area = new JTextArea(text + "\n\nThis build keeps the app Java-only and desktop-focused while providing integration points.");
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        area.setMargin(new Insets(8, 8, 8, 8));
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildOutputContainer() {
        outputContainer = new JPanel(new BorderLayout());

        JPanel outputHeader = new JPanel(new BorderLayout());
        JLabel outputTitle = new JLabel(" 🔨 Terminal / Compiler Output");
        outputTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        outputTitle.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        outputHeader.add(outputTitle, BorderLayout.WEST);

        JButton toggleBtn = smallButton("▲ Hide", new Color(60, 60, 60));
        toggleBtn.addActionListener(e -> {
            toggleOutputPanel();
            toggleBtn.setText(outputVisible ? "▲ Hide" : "▼ Show");
        });
        outputHeader.add(toggleBtn, BorderLayout.EAST);
        outputContainer.add(outputHeader, BorderLayout.NORTH);

        outputPane = new JTextPane();
        outputPane.setEditable(false);
        outputPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputPane.setMargin(new Insets(4, 6, 4, 6));

        JScrollPane outputScroll = new JScrollPane(outputPane);
        outputScroll.setPreferredSize(new Dimension(0, 170));
        outputScroll.setBorder(null);
        outputContainer.add(outputScroll, BorderLayout.CENTER);

        return outputContainer;
    }

    private void doOpen() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Java File");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!editorPanel.openFile(file)) {
            JOptionPane.showMessageDialog(this, "Please choose a .java file", "Open Error", JOptionPane.ERROR_MESSAGE);
        }
    }

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
            } catch (BadLocationException ignored) {
            }
        });
    }

    private void toggleOutputPanel() {
        outputVisible = !outputVisible;
        Component center = ((BorderLayout) outputContainer.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (center != null) center.setVisible(outputVisible);
        outputContainer.revalidate();
        outputContainer.repaint();
    }

    private void applyTheme(boolean dark) {
        this.darkTheme = dark;
        Color frameBg = dark ? FRAME_BG_DARK : FRAME_BG_LIGHT;
        Color toolbarBg = dark ? TOOLBAR_BG_DARK : TOOLBAR_BG_LIGHT;
        Color borderColor = dark ? BORDER_COLOR_DARK : BORDER_COLOR_LIGHT;
        Color labelFg = dark ? LABEL_FG_DARK : LABEL_FG_LIGHT;
        Color outputBg = dark ? OUTPUT_BG_DARK : OUTPUT_BG_LIGHT;

        getContentPane().setBackground(frameBg);
        toolbar.setBackground(toolbarBg);
        for (Component c : toolbar.getComponents()) {
            c.setBackground(toolbarBg);
            c.setForeground(labelFg);
        }
        outputContainer.setBackground(frameBg);
        outputContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor));
        outputPane.setBackground(outputBg);
        outputPane.setForeground(labelFg);
        apiKeyField.setBackground(dark ? new Color(50, 50, 50) : Color.WHITE);
        apiKeyField.setForeground(labelFg);
        apiKeyField.setCaretColor(dark ? Color.WHITE : Color.BLACK);

        editorPanel.applyTheme(dark);
        aiPanel.applyTheme(dark);

        repaint();
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "<html><b>Java Editor Pro</b><br><br>" +
                        "Java-only desktop IDE with:\n" +
                        "<ul>" +
                        "<li>Multi-tab Java editor with syntax highlighting</li>" +
                        "<li>Find/Replace, autosave, dark/light themes</li>" +
                        "<li>Compile + run using Java Compiler API</li>" +
                        "<li>AI assistant and collaboration/video integration panels</li>" +
                        "</ul>" +
                        "</html>",
                "About Java Editor Pro",
                JOptionPane.INFORMATION_MESSAGE);
    }

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
}
