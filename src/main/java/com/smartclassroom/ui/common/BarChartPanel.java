package com.smartclassroom.ui.common;

import com.smartclassroom.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * A small bar chart drawn with plain Java2D (no chart library). Values are percentages from 0 to 100.
 * Bars below the pass mark are drawn in red, the others in blue.
 */
public class BarChartPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient List<String> labels = new ArrayList<>();
    private final transient List<Double> values = new ArrayList<>();
    private final double passMark;

    public BarChartPanel(double passMark) {
        this.passMark = passMark;
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(400, 220));
    }

    public void setData(List<String> newLabels, List<Double> newValues) {
        labels.clear();
        values.clear();
        labels.addAll(newLabels);
        values.addAll(newValues);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(Theme.FONT_SMALL);
            FontMetrics fm = g.getFontMetrics();

            int left = 36;
            int right = 8;
            int top = 12;
            int bottom = 28;
            int width = getWidth() - left - right;
            int height = getHeight() - top - bottom;
            if (width <= 0 || height <= 0) {
                return;
            }

            // grid lines and the 0-100 scale
            g.setStroke(new BasicStroke(1f));
            for (int pct = 0; pct <= 100; pct += 25) {
                int y = top + height - (int) Math.round(height * pct / 100.0);
                g.setColor(Theme.BORDER);
                g.drawLine(left, y, left + width, y);
                g.setColor(Theme.TEXT_MUTED);
                String text = String.valueOf(pct);
                g.drawString(text, left - 6 - fm.stringWidth(text), y + fm.getAscent() / 2 - 1);
            }

            if (values.isEmpty()) {
                g.setColor(Theme.TEXT_MUTED);
                String text = "No marks to show yet";
                g.drawString(text, left + (width - fm.stringWidth(text)) / 2, top + height / 2);
                return;
            }

            int slot = width / values.size();
            int barWidth = Math.max(8, Math.min(60, slot - 14));
            for (int i = 0; i < values.size(); i++) {
                double value = Math.max(0, Math.min(100, values.get(i)));
                int barHeight = (int) Math.round(height * value / 100.0);
                int x = left + i * slot + (slot - barWidth) / 2;
                int y = top + height - barHeight;
                Color color = value < passMark ? Theme.DANGER : Theme.PRIMARY;
                g.setColor(color);
                g.fillRoundRect(x, y, barWidth, Math.max(barHeight, 2), 8, 8);

                g.setColor(Theme.TEXT);
                String valueText = String.format("%.0f", value);
                g.drawString(valueText, x + (barWidth - fm.stringWidth(valueText)) / 2, y - 3);

                g.setColor(Theme.TEXT_MUTED);
                String label = fit(labels.get(i), fm, slot - 4);
                g.drawString(label, left + i * slot + (slot - fm.stringWidth(label)) / 2, top + height + fm.getAscent() + 6);
            }
        } finally {
            g.dispose();
        }
    }

    /** Shortens a label with ... until it fits into the available width. */
    private static String fit(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String shortened = text;
        while (shortened.length() > 1 && fm.stringWidth(shortened + "...") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "...";
    }
}
