package com.smartclassroom.ui.theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** A white "card" with rounded corners and a thin border. */
public class RoundedPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final int arc;
    private Color fill = Theme.SURFACE;

    public RoundedPanel(LayoutManager layout) {
        this(layout, 16);
    }

    public RoundedPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
