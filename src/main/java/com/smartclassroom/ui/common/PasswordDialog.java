package com.smartclassroom.ui.common;

import com.smartclassroom.model.User;
import com.smartclassroom.service.AuthService;
import com.smartclassroom.service.exception.ValidationException;

import javax.swing.JPasswordField;
import java.awt.Window;

/** Two uses: a user changes their own password, or an admin resets someone else's. */
public class PasswordDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private final transient AuthService authService = new AuthService();
    private final transient User actor;
    private final int targetUserId;
    private final boolean reset;
    private JPasswordField oldPassword;
    private final JPasswordField newPassword = new JPasswordField(20);
    private final JPasswordField confirmPassword = new JPasswordField(20);

    private PasswordDialog(Window owner, User actor, int targetUserId, boolean reset, String title) {
        super(owner, title);
        this.actor = actor;
        this.targetUserId = targetUserId;
        this.reset = reset;
        if (!reset) {
            oldPassword = form.addField("Current password", new JPasswordField(20));
        }
        form.addField("New password (8+ characters, letters and digits)", newPassword);
        form.addField("Confirm new password", confirmPassword);
    }

    /** The logged-in user changes their own password. */
    public static boolean change(Window owner, User actor) {
        return new PasswordDialog(owner, actor, actor.getUserId(), false, "Change password").showDialog();
    }

    /** An admin sets a new password for another user. */
    public static boolean reset(Window owner, User admin, int userId, String personName) {
        return new PasswordDialog(owner, admin, userId, true, "Reset password for " + personName).showDialog();
    }

    @Override
    protected void save() {
        String newValue = new String(newPassword.getPassword());
        if (!newValue.equals(new String(confirmPassword.getPassword()))) {
            throw new ValidationException("The two new passwords do not match.");
        }
        if (reset) {
            authService.resetPassword(actor, targetUserId, newValue);
        } else {
            authService.changePassword(actor, new String(oldPassword.getPassword()), newValue);
        }
    }
}
