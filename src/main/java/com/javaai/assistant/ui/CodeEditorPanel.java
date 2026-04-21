package com.javaai.assistant.ui;

import com.javaai.assistant.compiler.CompilationResult;
import com.javaai.assistant.compiler.JavaCompilerService;
import com.javaai.assistant.editor.JavaSyntaxHighlighter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Left panel of the main split pane.
 *
 * <p>Contains:
 * <ul>
 *   <li>A dark-themed {@link JTextPane} with Java syntax highlighting and line numbers</li>
 *   <li><em>Compile</em> button – compiles via {@link JavaCompilerService} and posts
 *       the result to the compiler output area</li>
 *   <li><em>Clear</em> button – clears the editor</li>
 *   <li><em>Ask AI</em> button – sends selected (or all) code to the chat panel</li>
 * </ul>
 */
public class CodeEditorPanel extends JPanel {

    // ---- dark-theme colours ------------------------------------------------
    private static final Color EDITOR_BG      = new Color(30,  30,  30);
    private static final Color PANEL_BG        = new Color(37,  37,  38);
    private static final Color HEADER_BG       = new Color(45,  45,  45);
    private static final Color BORDER_COLOR    = new Color(62,  62,  66);
    private static final Color HEADER_FG       = new Color(200, 200, 200);
    private static final Color BTN_COMPILE_BG  = new Color(14,  99, 156);
    private static final Color BTN_ASK_BG      = new Color(60, 130,  60);
    private static final Color BTN_CLEAR_BG    = new Color(90,  40,  40);
    private static final Color BTN_FG          = Color.WHITE;

    // ---- default starter code shown on first launch -------------------------
    private static final String STARTER_CODE =
            "public class Main {\n" +
            "\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, Java AI Assistant!\");\n" +
            "    }\n" +
            "}\n";

    // -----------------------------------------------------------------------
    private final JTextPane          textPane;
    private final LineNumberComponent lineNumbers;
    private final JavaCompilerService compilerService = new JavaCompilerService();

    /** Called when compilation is requested; receives the {@link CompilationResult}. */
    private Consumer<CompilationResult> onCompileCallback;
    /** Called when the user clicks "Ask AI"; receives the code snippet. */
    private Consumer<String>            onAskAiCallback;

    // -----------------------------------------------------------------------
    public CodeEditorPanel() {
        super(new BorderLayout());
        setBackground(PANEL_BG);

        // ---- Header --------------------------------------------------------
        JLabel header = new JLabel("  ☕ Java Code Editor");
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setForeground(HEADER_FG);
        header.setBackground(HEADER_BG);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        add(header, BorderLayout.NORTH);

        // ---- Editor --------------------------------------------------------
        textPane = new JTextPane();
        textPane.setBackground(EDITOR_BG);
        textPane.setForeground(new Color(212, 212, 212));
        textPane.setCaretColor(Color.WHITE);
        textPane.setSelectionColor(new Color(38, 79, 120));
        textPane.setSelectedTextColor(Color.WHITE);
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        // basic undo shortcut (Ctrl+Z) – DefaultEditorKit has no undo constant;
        // rely on the standard action name instead
        textPane.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        textPane.setText(STARTER_CODE);

        // Syntax highlighter
        JavaSyntaxHighlighter highlighter = new JavaSyntaxHighlighter(textPane);
        highlighter.highlight();

        // Line numbers
        lineNumbers = new LineNumberComponent(textPane);

        // Repaint line numbers and update their width on document changes
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshLineNumbers(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshLineNumbers(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setRowHeaderView(lineNumbers);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(EDITOR_BG);
        add(scrollPane, BorderLayout.CENTER);

        // ---- Button bar ----------------------------------------------------
        JPanel buttonBar = buildButtonBar();
        add(buttonBar, BorderLayout.SOUTH);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** @return the raw text currently in the editor. */
    public String getCode() {
        return textPane.getText();
    }

    /** @return selected text, or {@link #getCode()} if nothing is selected. */
    public String getSelectedOrAllCode() {
        String sel = textPane.getSelectedText();
        return (sel != null && !sel.isBlank()) ? sel : getCode();
    }

    /** Registers a callback invoked after each compilation attempt. */
    public void setOnCompileCallback(Consumer<CompilationResult> cb) {
        this.onCompileCallback = cb;
    }

    /** Registers a callback invoked when the user clicks "Ask AI". */
    public void setOnAskAiCallback(Consumer<String> cb) {
        this.onAskAiCallback = cb;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setBackground(PANEL_BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JButton compileBtn = createButton("▶  Compile", BTN_COMPILE_BG, "Compile the Java code (Ctrl+B)");
        JButton clearBtn   = createButton("✕  Clear",   BTN_CLEAR_BG,   "Clear the editor");
        JButton askAiBtn   = createButton("🤖  Ask AI", BTN_ASK_BG,    "Send selected (or all) code to the AI assistant");

        compileBtn.setMnemonic('C');
        compileBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                  .put(KeyStroke.getKeyStroke("control B"), "compile");
        compileBtn.getActionMap().put("compile", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { doCompile(); }
        });

        compileBtn.addActionListener(e -> doCompile());
        clearBtn  .addActionListener(e -> doClear());
        askAiBtn  .addActionListener(e -> doAskAi());

        bar.add(compileBtn);
        bar.add(clearBtn);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(askAiBtn);
        return bar;
    }

    private void doCompile() {
        if (onCompileCallback == null) return;
        String code = getCode();
        // Run compilation off the EDT to keep the UI responsive
        new Thread(() -> {
            CompilationResult result = compilerService.compile(code);
            SwingUtilities.invokeLater(() -> onCompileCallback.accept(result));
        }, "compiler-thread").start();
    }

    private void doClear() {
        int choice = JOptionPane.showConfirmDialog(
                this, "Clear the editor?", "Confirm Clear",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            textPane.setText("");
        }
    }

    private void doAskAi() {
        if (onAskAiCallback != null) {
            onAskAiCallback.accept(getSelectedOrAllCode());
        }
    }

    private void refreshLineNumbers() {
        SwingUtilities.invokeLater(() -> {
            lineNumbers.updateWidth();
            lineNumbers.repaint();
        });
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
