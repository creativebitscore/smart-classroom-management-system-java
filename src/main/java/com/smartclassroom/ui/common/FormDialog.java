package com.smartclassroom.ui.common;

import com.smartclassroom.service.exception.AppException;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Base class for every pop-up form. A subclass adds its fields to {@link #form}, then implements
 * save(). If save() throws an AppException the message is shown and the dialog stays open.
 */
public abstract class FormDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    protected final FormPanel form = new FormPanel();
    private final Window owner;
    private boolean saved;

    protected FormDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.owner = owner;
    }

    /** Validate, call the service, and throw AppException on any problem. */
    protected abstract void save();

    /** Shows the dialog and returns true if the user saved. Subclasses call this from their own show method. */
    public boolean showDialog() {
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(22, 26, 18, 26));
        content.add(form, BorderLayout.CENTER);

        JButton saveButton = UiFactory.primaryButton("Save");
        JButton cancelButton = UiFactory.secondaryButton("Cancel");
        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(saveButton);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setLocationRelativeTo(owner);
        setVisible(true);
        return saved;
    }

    private void onSave() {
        try {
            save();
            saved = true;
            dispose();
        } catch (AppException | IllegalArgumentException e) {
            UiFactory.error(this, e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            UiFactory.error(this, "An unexpected error occurred: " + e.getMessage());
        }
    }
}
