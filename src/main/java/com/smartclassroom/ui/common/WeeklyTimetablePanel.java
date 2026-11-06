package com.smartclassroom.ui.common;

import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Weekly timetable grid: one row per time slot, one column per day (Monday to Saturday). */
public class WeeklyTimetablePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final DayOfWeek[] DAYS = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

    private final boolean showFaculty;
    private final JLabel emptyLabel = UiFactory.muted("No classes are scheduled.");
    private final DefaultTableModel model;
    private final JTable table;

    /** @param showFaculty true to print the teacher's name in each cell (students need it, teachers do not) */
    public WeeklyTimetablePanel(boolean showFaculty) {
        super(new BorderLayout(0, 8));
        this.showFaculty = showFaculty;
        setOpaque(false);

        String[] columns = new String[DAYS.length + 1];
        columns[0] = "Time";
        for (int i = 0; i < DAYS.length; i++) {
            columns[i + 1] = DAYS[i].getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
        model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(84);
        table.setShowGrid(true);
        table.setGridColor(Theme.BORDER);
        table.setRowSelectionAllowed(false);
        table.setFont(Theme.FONT_BASE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.FONT_BOLD);
        table.setDefaultRenderer(Object.class, new CellRenderer());

        add(UiFactory.scroll(table), BorderLayout.CENTER);
        add(emptyLabel, BorderLayout.SOUTH);
        emptyLabel.setVisible(false);
    }

    /** Rebuilds the grid. Rows are the distinct time slots found in the entries, earliest first. */
    public void setEntries(List<TimetableEntry> entries) {
        // slot label -> day -> entries (a TreeMap keeps slots in time order because labels are HH:mm-HH:mm)
        Map<String, Map<DayOfWeek, List<TimetableEntry>>> grid = new TreeMap<>();
        for (TimetableEntry e : entries) {
            grid.computeIfAbsent(e.getSlotLabel(), k -> new EnumMap<>(DayOfWeek.class))
                    .computeIfAbsent(e.getDayOfWeek(), k -> new ArrayList<>()).add(e);
        }
        model.setRowCount(0);
        for (Map.Entry<String, Map<DayOfWeek, List<TimetableEntry>>> slot : grid.entrySet()) {
            Object[] row = new Object[DAYS.length + 1];
            row[0] = slot.getKey();
            for (int i = 0; i < DAYS.length; i++) {
                row[i + 1] = describe(slot.getValue().get(DAYS[i]));
            }
            model.addRow(row);
        }
        table.getColumnModel().getColumn(0).setMinWidth(120);
        table.getColumnModel().getColumn(0).setMaxWidth(140);
        emptyLabel.setVisible(entries.isEmpty());
    }

    private String describe(List<TimetableEntry> cellEntries) {
        if (cellEntries == null || cellEntries.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<html>");
        for (int i = 0; i < cellEntries.size(); i++) {
            TimetableEntry e = cellEntries.get(i);
            if (i > 0) {
                html.append("<hr>");
            }
            html.append("<b>").append(escape(e.getSubject().getSubjectCode())).append("</b> ")
                    .append(escape(e.getSubject().getSubjectName())).append("<br>")
                    .append("Room ").append(escape(e.getClassroom().getRoomName()))
                    .append(" &middot; Sec ").append(escape(e.getSection()));
            if (showFaculty) {
                html.append("<br>").append(escape(e.getFaculty().getFullName()));
            }
        }
        return html.append("</html>").toString();
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(t, value, false, false, row, column);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            setVerticalAlignment(SwingConstants.TOP);
            boolean filled = value != null && !value.toString().isEmpty();
            setBackground(column == 0 ? Theme.ROW_ALT : (filled ? Theme.CELL_FILLED : Theme.SURFACE));
            setForeground(Theme.TEXT);
            if (column == 0) {
                setFont(Theme.FONT_BOLD);
            }
            return this;
        }
    }
}
