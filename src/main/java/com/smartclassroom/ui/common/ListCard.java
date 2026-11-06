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
import java.util.List;

/** A card with a heading and a short list of items; each item has a main line and a smaller detail line. */
public class ListCard extends RoundedPanel {
    private static final long serialVersionUID = 1L;
    private final JPanel body = new JPanel();

    public ListCard(String title) {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        add(UiFactory.heading(title), BorderLayout.NORTH);
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        add(body, BorderLayout.CENTER);
    }

    /** Each element is {mainText, detailText}. */
    public void setItems(List<String[]> items) {
        setItems(items, Theme.TEXT);
    }

    public void setItems(List<String[]> items, Color mainColor) {
        body.removeAll();
        if (items.isEmpty()) {
            body.add(UiFactory.muted("Nothing to show right now."));
        }
        for (String[] item : items) {
            JLabel main = new JLabel("<html>" + escape(item[0]) + "</html>");
            main.setFont(Theme.FONT_BOLD);
            main.setForeground(mainColor);
            JLabel detail = new JLabel("<html>" + escape(item.length > 1 ? item[1] : "") + "</html>");
            detail.setFont(Theme.FONT_SMALL);
            detail.setForeground(Theme.TEXT_MUTED);
            detail.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            body.add(main);
            body.add(detail);
        }
        body.revalidate();
        body.repaint();
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
