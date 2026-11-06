package com.smartclassroom.ui.common;

import com.smartclassroom.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Vertical form: a small label above each field. */
public class FormPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private int row;

    public FormPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    public <T extends JComponent> T addField(String label, T field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 4, 0);
        JLabel text = new JLabel(label);
        text.setFont(Theme.FONT_BOLD);
        text.setForeground(Theme.TEXT_MUTED);
        add(text, c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 14, 0);
        add(field, c);
        return field;
    }
}
