package com.javaai.assistant.ui;

import com.javaai.assistant.compiler.CompilationResult;
import com.javaai.assistant.compiler.JavaCompilerService;
import com.javaai.assistant.editor.JavaSyntaxHighlighter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Java-only editor with tabs, find/replace, autosave and compile/run actions. */
public class CodeEditorPanel extends JPanel {

    private static final String STARTER_CODE =
            "public class Main {\n" +
            "\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, Java Editor Pro!\");\n" +
            "    }\n" +
            "}\n";

    private static final Color EDITOR_BG_DARK = new Color(30, 30, 30);
    private static final Color PANEL_BG_DARK = new Color(37, 37, 38);
    private static final Color BORDER_COLOR_DARK = new Color(62, 62, 66);
    private static final Color EDITOR_BG_LIGHT = new Color(250, 250, 250);
    private static final Color PANEL_BG_LIGHT = new Color(242, 242, 242);
    private static final Color BORDER_COLOR_LIGHT = new Color(204, 204, 204);
    private static final int AUTO_SAVE_INTERVAL_MS = 10_000;

    private final JavaCompilerService compilerService = new JavaCompilerService();
    private final List<EditorTab> tabs = new ArrayList<>();

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JTextField argsField = new JTextField(16);
    private final JTextField stdinField = new JTextField(16);
    private final Timer autoSaveTimer;

    private Consumer<CompilationResult> onCompileCallback;
    private Consumer<String> onAskAiCallback;
    private boolean darkTheme = true;

    public CodeEditorPanel() {
        super(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        newTab("Main.java", STARTER_CODE, null);
        applyTheme(true);

        autoSaveTimer = new Timer(AUTO_SAVE_INTERVAL_MS, e -> autoSave());
        autoSaveTimer.start();
    }

    public void applyTheme(boolean dark) {
        this.darkTheme = dark;
        Color panelBg = dark ? PANEL_BG_DARK : PANEL_BG_LIGHT;
        Color borderColor = dark ? BORDER_COLOR_DARK : BORDER_COLOR_LIGHT;
        setBackground(panelBg);
        tabbedPane.setBackground(panelBg);
        tabbedPane.setForeground(dark ? new Color(212, 212, 212) : new Color(33, 33, 33));
        tabbedPane.setBorder(BorderFactory.createLineBorder(borderColor));
        for (EditorTab tab : tabs) {
            applyThemeToTextPane(tab.textPane);
            tab.lineNumbers.applyTheme(dark);
            tab.scrollPane.setBorder(BorderFactory.createLineBorder(borderColor));
            tab.scrollPane.getViewport().setBackground(dark ? EDITOR_BG_DARK : EDITOR_BG_LIGHT);
        }
        repaint();
    }

    public String getCode() {
        EditorTab tab = currentTab();
        return tab == null ? "" : tab.textPane.getText();
    }

    public String getSelectedOrAllCode() {
        EditorTab tab = currentTab();
        if (tab == null) return "";
        String sel = tab.textPane.getSelectedText();
        return (sel != null && !sel.isBlank()) ? sel : tab.textPane.getText();
    }

    public void setOnCompileCallback(Consumer<CompilationResult> cb) {
        this.onCompileCallback = cb;
    }

    public void setOnAskAiCallback(Consumer<String> cb) {
        this.onAskAiCallback = cb;
    }

    public String getJdkInfo() {
        return compilerService.getJdkInfo();
    }

    public void createNewTab() {
        newTab("Untitled.java", STARTER_CODE, null);
    }

    public boolean openFile(File file) {
        try {
            if (file == null || !file.getName().endsWith(".java")) {
                return false;
            }
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            newTab(file.getName(), content, file);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean saveCurrentFile(Component parent, boolean saveAs) {
        EditorTab tab = currentTab();
        if (tab == null) return false;
        File target = tab.file;
        if (saveAs || target == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Java File");
            if (target != null) chooser.setSelectedFile(target);
            int result = chooser.showSaveDialog(parent);
            if (result != JFileChooser.APPROVE_OPTION) return false;
            target = chooser.getSelectedFile();
            if (!target.getName().endsWith(".java")) {
                target = new File(target.getParentFile(), target.getName() + ".java");
            }
            tab.file = target;
            tab.title = target.getName();
        }
        try {
            Files.writeString(target.toPath(), tab.textPane.getText(), StandardCharsets.UTF_8);
            tab.dirty = false;
            refreshTabTitle(tab);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Save failed: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public void showFindReplaceDialog(Component parent) {
        EditorTab tab = currentTab();
        if (tab == null) return;

        JTextField findField = new JTextField(20);
        JTextField replaceField = new JTextField(20);
        JCheckBox regexBox = new JCheckBox("Regex");

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Find:"));
        panel.add(findField);
        panel.add(new JLabel("Replace with:"));
        panel.add(replaceField);
        panel.add(regexBox);

        int option = JOptionPane.showConfirmDialog(parent, panel, "Find & Replace", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return;

        String find = findField.getText();
        String replace = replaceField.getText();
        if (find.isBlank()) return;

        String text = tab.textPane.getText();
        String updated;
        try {
            if (regexBox.isSelected()) {
                Pattern compiledPattern = Pattern.compile(find);
                updated = compiledPattern.matcher(text).replaceAll(Matcher.quoteReplacement(replace));
            } else {
                updated = text.replace(find, replace);
            }
            tab.textPane.setText(updated);
        } catch (PatternSyntaxException ex) {
            JOptionPane.showMessageDialog(parent,
                    "Invalid regex pattern:\n" + ex.getDescription(),
                    "Find & Replace",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public Integer highlightErrorLine(Integer lineNumber) {
        EditorTab tab = currentTab();
        if (tab == null || lineNumber == null || lineNumber < 1) return null;
        try {
            String text = tab.textPane.getText();
            int start = 0;
            int line = 1;
            while (line < lineNumber && start < text.length()) {
                int nl = text.indexOf('\n', start);
                if (nl < 0) return null;
                start = nl + 1;
                line++;
            }
            int end = text.indexOf('\n', start);
            if (end < 0) end = text.length();
            Highlighter h = tab.textPane.getHighlighter();
            h.removeAllHighlights();
            h.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(new Color(120, 40, 40)));
            tab.textPane.setCaretPosition(start);
            return lineNumber;
        } catch (BadLocationException ignored) {
            return null;
        }
    }

    private JComponent buildHeader() {
        JLabel header = new JLabel("  ☕ Java Editor Pro");
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        return header;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        JButton compileBtn = createButton("▶ Compile", new Color(14, 99, 156), "Compile current Java tab (Ctrl+B)");
        JButton runBtn = createButton("▶ Run", new Color(67, 142, 74), "Compile and run current Java tab (Ctrl+R)");
        JButton askAiBtn = createButton("🤖 Ask AI", new Color(73, 91, 151), "Send selected code to AI assistant");
        JButton clearBtn = createButton("✕ Clear", new Color(90, 40, 40), "Clear current editor content");

        compileBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control B"), "compile");
        compileBtn.getActionMap().put("compile", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { doCompile(); }
        });
        compileBtn.addActionListener(compileBtn.getActionMap().get("compile"));

        runBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control R"), "run");
        runBtn.getActionMap().put("run", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { doRun(); }
        });
        runBtn.addActionListener(runBtn.getActionMap().get("run"));

        askAiBtn.addActionListener(e -> doAskAi());
        clearBtn.addActionListener(e -> doClear());

        bar.add(compileBtn);
        bar.add(runBtn);
        bar.add(new JLabel("Args:"));
        bar.add(argsField);
        bar.add(new JLabel("StdIn:"));
        bar.add(stdinField);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(askAiBtn);
        bar.add(clearBtn);
        return bar;
    }

    private void doCompile() {
        EditorTab tab = currentTab();
        if (tab == null || onCompileCallback == null) return;
        tab.textPane.getHighlighter().removeAllHighlights();
        new Thread(() -> {
            CompilationResult result = compilerService.compile(tab.textPane.getText());
            SwingUtilities.invokeLater(() -> {
                if (!result.isSuccess()) highlightErrorLine(result.getErrorLine());
                onCompileCallback.accept(result);
            });
        }, "compiler-thread").start();
    }

    private void doRun() {
        EditorTab tab = currentTab();
        if (tab == null || onCompileCallback == null) return;
        tab.textPane.getHighlighter().removeAllHighlights();
        new Thread(() -> {
            CompilationResult result = compilerService.compileAndRun(tab.textPane.getText(), argsField.getText(), stdinField.getText());
            SwingUtilities.invokeLater(() -> {
                if (!result.isSuccess()) highlightErrorLine(result.getErrorLine());
                onCompileCallback.accept(result);
            });
        }, "runner-thread").start();
    }

    private void doClear() {
        EditorTab tab = currentTab();
        if (tab == null) return;
        int choice = JOptionPane.showConfirmDialog(this, "Clear current tab?", "Confirm Clear", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            tab.textPane.setText("");
        }
    }

    private void doAskAi() {
        if (onAskAiCallback != null) {
            onAskAiCallback.accept(getSelectedOrAllCode());
        }
    }

    private void autoSave() {
        for (EditorTab tab : tabs) {
            if (tab.dirty && tab.file != null) {
                try {
                    Files.writeString(tab.file.toPath(), tab.textPane.getText(), StandardCharsets.UTF_8);
                    tab.dirty = false;
                    refreshTabTitle(tab);
                } catch (Exception ex) {
                    System.err.println("Autosave failed for " + tab.file + ": " + ex.getMessage());
                }
            }
        }
    }

    private void newTab(String title, String content, File file) {
        EditorTab tab = new EditorTab();
        tab.title = title;
        tab.file = file;

        tab.textPane = new JTextPane();
        tab.textPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        tab.textPane.setText(content);
        applyThemeToTextPane(tab.textPane);

        JavaSyntaxHighlighter highlighter = new JavaSyntaxHighlighter(tab.textPane);
        highlighter.highlight();

        tab.lineNumbers = new LineNumberComponent(tab.textPane);
        tab.lineNumbers.applyTheme(darkTheme);
        tab.textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onDocumentChange(tab); }
            @Override public void removeUpdate(DocumentEvent e) { onDocumentChange(tab); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        tab.scrollPane = new JScrollPane(tab.textPane);
        tab.scrollPane.setRowHeaderView(tab.lineNumbers);
        tab.container = new JPanel(new BorderLayout());
        tab.container.add(tab.scrollPane, BorderLayout.CENTER);

        tabs.add(tab);
        tabbedPane.addTab(tab.title, tab.container);
        tabbedPane.setSelectedComponent(tab.container);
    }

    private void onDocumentChange(EditorTab tab) {
        tab.dirty = true;
        refreshTabTitle(tab);
        SwingUtilities.invokeLater(() -> {
            tab.lineNumbers.updateWidth();
            tab.lineNumbers.repaint();
        });
    }

    private void refreshTabTitle(EditorTab tab) {
        int idx = tabbedPane.indexOfComponent(tab.container);
        if (idx >= 0) {
            tabbedPane.setTitleAt(idx, tab.dirty ? "*" + tab.title : tab.title);
        }
    }

    private EditorTab currentTab() {
        Component selected = tabbedPane.getSelectedComponent();
        for (EditorTab tab : tabs) {
            if (Objects.equals(tab.container, selected)) return tab;
        }
        return null;
    }

    private void applyThemeToTextPane(JTextComponent textPane) {
        textPane.setBackground(darkTheme ? EDITOR_BG_DARK : EDITOR_BG_LIGHT);
        textPane.setForeground(darkTheme ? new Color(212, 212, 212) : new Color(20, 20, 20));
        textPane.setCaretColor(darkTheme ? Color.WHITE : Color.BLACK);
        textPane.setSelectionColor(darkTheme ? new Color(38, 79, 120) : new Color(173, 216, 230));
    }

    private JButton createButton(String text, Color bg, String tooltip) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        return btn;
    }

    private static final class EditorTab {
        private String title;
        private File file;
        private boolean dirty;
        private JPanel container;
        private JScrollPane scrollPane;
        private JTextPane textPane;
        private LineNumberComponent lineNumbers;
    }
}
