package com.smartclassroom.ui.common;

import javax.swing.table.DefaultTableModel;
import java.util.Set;

/** Table model whose cells cannot be edited, except for the columns listed in the constructor. */
public class AppTableModel extends DefaultTableModel {
    private static final long serialVersionUID = 1L;
    private final Set<Integer> editableColumns;

    public AppTableModel(String[] columns, Integer... editableColumns) {
        super(columns, 0);
        this.editableColumns = Set.of(editableColumns);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return editableColumns.contains(column);
    }

    /** Lets the table sort numbers and dates properly: the class of a column is the class of its first value. */
    @Override
    public Class<?> getColumnClass(int column) {
        for (int row = 0; row < getRowCount(); row++) {
            Object value = getValueAt(row, column);
            if (value != null) {
                return value.getClass();
            }
        }
        return Object.class;
    }
}
