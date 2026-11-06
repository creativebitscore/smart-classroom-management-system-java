package com.smartclassroom.ui.admin;

import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.model.User;
import com.smartclassroom.service.TimetableService;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.common.WeeklyTimetablePanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Admin timetable screen with two tabs: editable list, and the weekly grid for a chosen section. */
public class TimetableScreen extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final String ALL = "All sections";

    private final transient TimetableService service = new TimetableService();
    private final TimetablePanel listPanel;
    private final WeeklyTimetablePanel grid = new WeeklyTimetablePanel(true);
    private final JComboBox<String> sectionFilter = new JComboBox<>();
    private transient List<TimetableEntry> all = new ArrayList<>();
    private boolean updatingFilter;

    public TimetableScreen(User actor) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        listPanel = new TimetablePanel(actor, this::refresh);

        JPanel gridTab = new JPanel(new BorderLayout(0, 12));
        gridTab.setBackground(Theme.BACKGROUND);
        gridTab.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Weekly Timetable"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Monday to Saturday, by time slot"), BorderLayout.SOUTH);
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filter.setOpaque(false);
        filter.add(UiFactory.muted("Show:"));
        filter.add(sectionFilter);
        top.add(titles, BorderLayout.WEST);
        top.add(filter, BorderLayout.EAST);
        gridTab.add(top, BorderLayout.NORTH);
        gridTab.add(grid, BorderLayout.CENTER);
        sectionFilter.addActionListener(e -> {
            if (!updatingFilter) {
                showGrid();
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Schedule list", listPanel);
        tabs.addTab("Weekly grid", gridTab);
        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        listPanel.refresh();
        try {
            all = service.getAll();
            updatingFilter = true;
            Object previous = sectionFilter.getSelectedItem();
            sectionFilter.removeAllItems();
            sectionFilter.addItem(ALL);
            for (String section : new TreeSet<>(all.stream().map(TimetableEntry::getSection).toList())) {
                sectionFilter.addItem(section);
            }
            if (previous != null) {
                sectionFilter.setSelectedItem(previous);
            }
            updatingFilter = false;
            showGrid();
        } catch (RuntimeException e) {
            updatingFilter = false;
            UiFactory.showException(this, e);
        }
    }

    private void showGrid() {
        Object chosen = sectionFilter.getSelectedItem();
        if (chosen == null || ALL.equals(chosen)) {
            grid.setEntries(all);
        } else {
            grid.setEntries(all.stream().filter(t -> t.getSection().equals(chosen)).toList());
        }
    }
}
