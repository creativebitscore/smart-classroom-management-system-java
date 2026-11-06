package com.smartclassroom.ui;

import com.smartclassroom.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** One entry of the dark sidebar. Highlighted when selected or hovered. */
class NavButton extends JToggleButton {
    private static final long serialVersionUID = 1L;
    private boolean hover;

    NavButton(String text) {
        super(text);
        setHorizontalAlignment(LEFT);
        setFont(Theme.FONT_BOLD);
        setForeground(Theme.SIDEBAR_TEXT);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        setAlignmentX(LEFT_ALIGNMENT);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (isSelected()) {
            g2.setColor(Theme.PRIMARY);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        } else if (hover) {
            g2.setColor(Theme.SIDEBAR_HOVER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        }
        g2.dispose();
        setForeground(isSelected() ? Color.WHITE : Theme.SIDEBAR_TEXT);
        super.paintComponent(g);
    }
}
