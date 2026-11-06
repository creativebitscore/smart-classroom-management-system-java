package com.smartclassroom.ui.common;

import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/** A summary tile: small title, big number, short note. */
public class StatCard extends RoundedPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel valueLabel = new JLabel("-");
    private final JLabel noteLabel = UiFactory.muted(" ");

    public StatCard(String title, Color accent) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setPreferredSize(new Dimension(220, 110));

        JPanel accentBar = new JPanel();
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new Dimension(5, 10));
        accentBar.setOpaque(true);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0)); // gap between the coloured bar and the text
        JLabel titleLabel = UiFactory.muted(title);
        titleLabel.setFont(Theme.FONT_BOLD);
        valueLabel.setFont(Theme.FONT_BIG_NUMBER);
        valueLabel.setForeground(Theme.TEXT);
        noteLabel.setFont(Theme.FONT_SMALL);
        text.add(titleLabel);
        text.add(valueLabel);
        text.add(noteLabel);

        add(text, BorderLayout.CENTER);
        add(accentBar, BorderLayout.WEST);
    }

    public void set(String value, String note) {
        valueLabel.setText(value);
        noteLabel.setText(note == null || note.isEmpty() ? " " : note);
    }
}
