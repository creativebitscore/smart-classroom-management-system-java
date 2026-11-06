package com.smartclassroom.ui.common;

import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.nio.file.Path;

/** Asks the user where to save a CSV file. */
public final class CsvFileChooser {
    private CsvFileChooser() {
    }

    /** Returns the chosen path, or null if the user cancelled. Asks before overwriting. */
    public static Path ask(Component parent, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File(suggestedName));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        if (file.exists() && !UiFactory.confirm(parent, file.getName() + " already exists. Replace it?", "Replace file")) {
            return null;
        }
        return file.toPath();
    }
}
