package com.smartclassroom.ui.common;

import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reusable "list + search + Add / Edit / Delete" screen. Students, faculty, subjects, classrooms,
 * timetable and assignments all extend this class and only supply what is different:
 * how to load the items, how to turn one item into a table row, and what the buttons do.
 */
public abstract class CrudPanel<T> extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final AppTableModel model;
    private final JTable table;
    private final TableRowSorter<AppTableModel> sorter;
    private final JTextField searchField = new JTextField(22);
    private final JLabel countLabel = UiFactory.muted("");
    private final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final transient List<T> items = new ArrayList<>();

    protected CrudPanel(String title, String subtitle, String[] columns, boolean canAdd, boolean canEdit,
                        boolean canDelete) {
        super(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        model = new AppTableModel(columns);
        table = UiFactory.table(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title(title), BorderLayout.NORTH);
        titles.add(UiFactory.muted(subtitle), BorderLayout.SOUTH);
        heading.add(titles, BorderLayout.WEST);

        buttonBar.setOpaque(false);
        if (canAdd) {
            JButton add = UiFactory.primaryButton("+ Add New");
            add.addActionListener(e -> guarded(this::onAdd));
            buttonBar.add(add);
        }
        if (canEdit) {
            JButton edit = UiFactory.secondaryButton("Edit");
            edit.addActionListener(e -> withSelection(this::onEdit));
            buttonBar.add(edit);
        }
        if (canDelete) {
            JButton delete = UiFactory.dangerButton("Delete");
            delete.addActionListener(e -> withSelection(this::confirmAndDelete));
            buttonBar.add(delete);
        }
        JButton refresh = UiFactory.secondaryButton("Refresh");
        refresh.addActionListener(e -> refresh());
        buttonBar.add(refresh);
        heading.add(buttonBar, BorderLayout.EAST);

        searchField.putClientProperty("JTextField.placeholderText", "Search in the table...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.WEST);
        searchRow.add(countLabel, BorderLayout.EAST);
        searchRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(searchRow, BorderLayout.NORTH);
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);

        add(heading, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);

        if (canEdit) {
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        withSelection(item -> onEdit(item));
                    }
                }
            });
        }
    }

    // ---------- what subclasses provide ----------

    protected abstract List<T> loadItems();

    protected abstract Object[] toRow(T item);

    protected void onAdd() {
    }

    protected void onEdit(T item) {
    }

    protected void onDelete(T item) {
    }

    protected String deleteMessage(T item) {
        return "Delete the selected record? This cannot be undone.";
    }

    // ---------- helpers for subclasses ----------

    protected JTable getTable() {
        return table;
    }

    protected Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    protected void addToolbarButton(JButton button) {
        buttonBar.add(button, 0);
    }

    /** Runs an action with the selected item, or tells the user to select one first. */
    protected void withSelection(java.util.function.Consumer<T> action) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UiFactory.warning(this, "Please select a row in the table first.");
            return;
        }
        T item = items.get(table.convertRowIndexToModel(viewRow));
        guarded(() -> action.accept(item));
    }

    protected List<T> currentItems() {
        return items;
    }

    protected void guarded(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void confirmAndDelete(T item) {
        if (UiFactory.confirm(this, deleteMessage(item), "Confirm delete")) {
            onDelete(item);
            UiFactory.success(this, "The record was deleted.");
            refresh();
        }
    }

    @Override
    public void refresh() {
        try {
            List<T> loaded = loadItems();
            items.clear();
            items.addAll(loaded);
            model.setRowCount(0);
            for (T item : items) {
                model.addRow(toRow(item));
            }
            applyFilter();
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void applyFilter() {
        String text = searchField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        countLabel.setText(table.getRowCount() + " of " + items.size() + " records");
    }
}
