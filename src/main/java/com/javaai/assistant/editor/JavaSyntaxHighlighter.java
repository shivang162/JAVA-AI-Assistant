package com.javaai.assistant.editor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Applies basic Java syntax highlighting to a {@link JTextPane} using a
 * simple state-machine that handles:
 * <ul>
 *   <li>Line comments  ({@code //…})</li>
 *   <li>Block comments ({@code /*…*\/})</li>
 *   <li>String literals ({@code "…"})</li>
 *   <li>Character literals ({@code '…'})</li>
 *   <li>Annotations ({@code @…})</li>
 *   <li>Numeric literals</li>
 *   <li>Reserved keywords / boolean / null literals</li>
 * </ul>
 *
 * Attaches a {@link DocumentListener} to the supplied text-pane so that
 * highlighting is refreshed automatically after every edit.
 */
public class JavaSyntaxHighlighter {

    // -----------------------------------------------------------------------
    // Java reserved words (including preview keywords up to Java 21)
    // -----------------------------------------------------------------------
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "var", "void", "volatile", "while",
            // modern Java
            "record", "sealed", "permits", "yield",
            // literals treated as keywords
            "null", "true", "false"
    ));

    // VS Code–inspired dark-theme palette
    private static final Color COLOR_DEFAULT    = new Color(212, 212, 212);
    private static final Color COLOR_KEYWORD    = new Color(86,  156, 214);
    private static final Color COLOR_STRING     = new Color(206, 145, 120);
    private static final Color COLOR_COMMENT    = new Color(106, 153,  85);
    private static final Color COLOR_NUMBER     = new Color(181, 206, 168);
    private static final Color COLOR_ANNOTATION = new Color(220, 220, 170);

    private static final String FONT_NAME = "Monospaced";
    private static final int    FONT_SIZE = 14;

    private final JTextPane     textPane;
    private final StyledDocument doc;

    // Styles
    private final Style defaultStyle;
    private final Style keywordStyle;
    private final Style stringStyle;
    private final Style commentStyle;
    private final Style numberStyle;
    private final Style annotationStyle;

    /** Guard against recursive document-change events triggered by setCharacterAttributes. */
    private boolean updating = false;

    public JavaSyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        this.doc      = textPane.getStyledDocument();

        defaultStyle    = createStyle("default",    COLOR_DEFAULT,    false, false);
        keywordStyle    = createStyle("keyword",    COLOR_KEYWORD,    true,  false);
        stringStyle     = createStyle("string",     COLOR_STRING,     false, false);
        commentStyle    = createStyle("comment",    COLOR_COMMENT,    false, true);
        numberStyle     = createStyle("number",     COLOR_NUMBER,     false, false);
        annotationStyle = createStyle("annotation", COLOR_ANNOTATION, false, false);

        doc.addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { scheduleHighlight(); }
            @Override public void removeUpdate(DocumentEvent e) { scheduleHighlight(); }
            // Ignore attribute-change events to avoid infinite recursion
            @Override public void changedUpdate(DocumentEvent e) { }
        });
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Force a full re-highlight of the document (safe to call from any thread). */
    public void highlight() {
        if (SwingUtilities.isEventDispatchThread()) {
            doHighlight();
        } else {
            SwingUtilities.invokeLater(this::doHighlight);
        }
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private void scheduleHighlight() {
        if (!updating) {
            SwingUtilities.invokeLater(this::doHighlight);
        }
    }

    private void doHighlight() {
        if (updating) return;
        updating = true;
        try {
            String text = doc.getText(0, doc.getLength());
            applyHighlighting(text);
        } catch (BadLocationException ignored) {
        } finally {
            updating = false;
        }
    }

    private void applyHighlighting(String text) {
        int len = text.length();

        // Reset everything to the default style first
        doc.setCharacterAttributes(0, len, defaultStyle, true);

        int i = 0;
        while (i < len) {
            char c = text.charAt(i);

            // ---- Line comment: // … \n ----
            if (c == '/' && i + 1 < len && text.charAt(i + 1) == '/') {
                int start = i;
                while (i < len && text.charAt(i) != '\n') i++;
                doc.setCharacterAttributes(start, i - start, commentStyle, false);
                continue;
            }

            // ---- Block comment: /* … */ ----
            if (c == '/' && i + 1 < len && text.charAt(i + 1) == '*') {
                int start = i;
                i += 2;
                while (i + 1 < len && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                if (i + 1 < len) i += 2; else i = len; // consume closing */
                doc.setCharacterAttributes(start, i - start, commentStyle, false);
                continue;
            }

            // ---- String literal: "…" (handles escape sequences, no multiline) ----
            if (c == '"') {
                int start = i++;
                while (i < len) {
                    char sc = text.charAt(i);
                    if (sc == '\\') { i += 2; continue; }
                    if (sc == '"')  { i++; break; }
                    if (sc == '\n') break; // unclosed string
                    i++;
                }
                doc.setCharacterAttributes(start, i - start, stringStyle, false);
                continue;
            }

            // ---- Character literal: '…' ----
            if (c == '\'') {
                int start = i++;
                while (i < len) {
                    char sc = text.charAt(i);
                    if (sc == '\\') { i += 2; continue; }
                    if (sc == '\'') { i++; break; }
                    if (sc == '\n') break;
                    i++;
                }
                doc.setCharacterAttributes(start, i - start, stringStyle, false);
                continue;
            }

            // ---- Annotation: @Identifier ----
            if (c == '@' && i + 1 < len && Character.isLetter(text.charAt(i + 1))) {
                int start = i++;
                while (i < len && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_')) {
                    i++;
                }
                doc.setCharacterAttributes(start, i - start, annotationStyle, false);
                continue;
            }

            // ---- Numeric literal ----
            if (Character.isDigit(c)) {
                int start = i++;
                while (i < len) {
                    char nc = text.charAt(i);
                    if (Character.isDigit(nc) || nc == '.' || nc == '_'
                            || "xXbBlLdDfFeE".indexOf(nc) >= 0) {
                        i++;
                    } else {
                        break;
                    }
                }
                // Floating-point suffix after digit (e.g., 1.5f, 1.5d)
                if (i < len && "+-".indexOf(text.charAt(i)) >= 0
                        && i > 0 && "eE".indexOf(text.charAt(i - 1)) >= 0) {
                    i++; // exponent sign
                    while (i < len && Character.isDigit(text.charAt(i))) i++;
                }
                doc.setCharacterAttributes(start, i - start, numberStyle, false);
                continue;
            }

            // ---- Identifier or keyword ----
            if (Character.isLetter(c) || c == '_') {
                int start = i++;
                while (i < len && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_')) {
                    i++;
                }
                String word = text.substring(start, i);
                if (KEYWORDS.contains(word)) {
                    doc.setCharacterAttributes(start, word.length(), keywordStyle, false);
                }
                continue;
            }

            i++;
        }
    }

    // -----------------------------------------------------------------------
    // Helper – create a named style with common font settings
    // -----------------------------------------------------------------------
    private Style createStyle(String name, Color fg, boolean bold, boolean italic) {
        Style style = doc.addStyle(name, null);
        StyleConstants.setFontFamily(style, FONT_NAME);
        StyleConstants.setFontSize(style,   FONT_SIZE);
        StyleConstants.setForeground(style, fg);
        StyleConstants.setBold(style,   bold);
        StyleConstants.setItalic(style, italic);
        return style;
    }
}
