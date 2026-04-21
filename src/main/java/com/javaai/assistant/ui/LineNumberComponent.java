package com.javaai.assistant.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A lightweight component that paints line numbers alongside a {@link JTextPane}.
 * Install it as the row-header view of the enclosing {@link JScrollPane}:
 * <pre>
 *   JScrollPane sp = new JScrollPane(textPane);
 *   sp.setRowHeaderView(new LineNumberComponent(textPane));
 * </pre>
 */
public class LineNumberComponent extends JComponent {

    private static final Color BG_COLOR_DARK   = new Color(40, 40, 40);
    private static final Color FG_COLOR_DARK   = new Color(100, 100, 100);
    private static final Color BG_COLOR_LIGHT  = new Color(240, 240, 240);
    private static final Color FG_COLOR_LIGHT  = new Color(120, 120, 120);
    private static final Font  LINE_FONT  = new Font("Monospaced", Font.PLAIN, 13);
    private static final int   PADDING    = 6;
    private static final int   MIN_DIGITS = 3; // always wide enough for 999 lines

    private final JTextPane textPane;

    public LineNumberComponent(JTextPane textPane) {
        this.textPane = textPane;
        setFont(LINE_FONT);
        setPreferredSize(computePreferredSize());
        setBackground(BG_COLOR_DARK);
        setForeground(FG_COLOR_DARK);
        setOpaque(true);
    }

    public void applyTheme(boolean dark) {
        setBackground(dark ? BG_COLOR_DARK : BG_COLOR_LIGHT);
        setForeground(dark ? FG_COLOR_DARK : FG_COLOR_LIGHT);
    }

    /** Call when the number of lines may have changed so width is recalculated. */
    public void updateWidth() {
        setPreferredSize(computePreferredSize());
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(getForeground());
        g2.setFont(LINE_FONT);

        FontMetrics fm = g2.getFontMetrics();
        Document     d = textPane.getDocument();
        Element   root = d.getDefaultRootElement();

        for (int line = 0; line < root.getElementCount(); line++) {
            Element elem = root.getElement(line);
            try {
                Rectangle2D r = textPane.modelToView2D(elem.getStartOffset());
                if (r != null) {
                    String num = String.valueOf(line + 1);
                    int xPos = getWidth() - fm.stringWidth(num) - PADDING;
                    int yPos = (int) r.getY() + fm.getAscent();
                    g2.drawString(num, xPos, yPos);
                }
            } catch (BadLocationException ignored) { }
        }
        g2.dispose();
    }

    private Dimension computePreferredSize() {
        FontMetrics fm = getFontMetrics(LINE_FONT);
        int digits = Math.max(MIN_DIGITS,
                String.valueOf(textPane.getDocument().getDefaultRootElement().getElementCount()).length());
        int width = fm.stringWidth("0".repeat(digits)) + PADDING * 2;
        return new Dimension(width, Integer.MAX_VALUE);
    }
}
