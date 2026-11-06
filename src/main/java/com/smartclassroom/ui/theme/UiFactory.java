package com.smartclassroom.ui.theme;

import com.smartclassroom.service.exception.AppException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.function.IntPredicate;

/** Builds consistently styled buttons, labels, tables and message dialogs. */
public final class UiFactory {
    private UiFactory() {
    }

    // ---------- buttons ----------

    public static JButton primaryButton(String text) {
        return button(text, Theme.PRIMARY, Color.WHITE, false);
    }

    public static JButton dangerButton(String text) {
        return button(text, Theme.DANGER, Color.WHITE, false);
    }

    public static JButton secondaryButton(String text) {
        return button(text, Color.WHITE, Theme.TEXT, true);
    }

    private static JButton button(String text, Color background, Color foreground, boolean outlined) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(Theme.FONT_BOLD);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        CompoundBorder padded = new CompoundBorder(
                outlined ? new LineBorder(Theme.BORDER, 1, true) : BorderFactory.createEmptyBorder(),
                BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setBorder(padded);
        return button;
    }

    // ---------- labels ----------

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_TITLE);
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_HEADING);
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_BASE);
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    // ---------- tables ----------

    /** A flat, striped, sortable table. */
    public static JTable table(TableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setSelectionBackground(Theme.ROW_SELECTED);
        table.setSelectionForeground(Theme.TEXT);
        table.setFont(Theme.FONT_BASE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setFont(Theme.FONT_BOLD);
        table.getTableHeader().setBackground(Theme.ROW_ALT);
        table.getTableHeader().setForeground(Theme.TEXT_MUTED);
        table.setDefaultRenderer(Object.class, new StripedRenderer(null, null));
        return table;
    }

    /** Colours the rows chosen by the predicate (the argument is the row index in the model). */
    public static void highlightRows(JTable table, IntPredicate rowInModel, Color color) {
        table.setDefaultRenderer(Object.class, new StripedRenderer(rowInModel, color));
    }

    public static JScrollPane scroll(JComponent component) {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(new LineBorder(Theme.BORDER, 1, true));
        pane.getViewport().setBackground(Theme.SURFACE);
        return pane;
    }

    /** Alternating row colours, cell padding and optional highlighted rows. */
    private static final class StripedRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final transient IntPredicate highlight;
        private final Color highlightColor;

        StripedRenderer(IntPredicate highlight, Color highlightColor) {
            this.highlight = highlight;
            this.highlightColor = highlightColor;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(SwingConstants.LEFT);
            setForeground(Theme.TEXT);
            if (isSelected) {
                setBackground(Theme.ROW_SELECTED);
            } else if (highlight != null && highlight.test(table.convertRowIndexToModel(row))) {
                setBackground(highlightColor);
            } else {
                setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.ROW_ALT);
            }
            return this;
        }
    }

    // ---------- dialogs ----------

    public static void success(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    public static void warning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    /** Shows our own exceptions as readable messages; anything unexpected is logged and reported generically. */
    public static void showException(Component parent, Exception e) {
        if (e instanceof AppException) {
            error(parent, e.getMessage());
        } else {
            e.printStackTrace();
            error(parent, "An unexpected error occurred: " + e.getMessage());
        }
    }

    public static boolean confirm(Component parent, String message, String title) {
        Object[] options = {"Yes, continue", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice == 0;
    }
}
