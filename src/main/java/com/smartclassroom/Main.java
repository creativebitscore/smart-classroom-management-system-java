package com.smartclassroom;

import com.smartclassroom.service.DemoDataInitializer;
import com.smartclassroom.service.exception.AppException;
import com.smartclassroom.ui.LoginFrame;
import com.smartclassroom.ui.theme.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Program entry point: applies the theme, prepares the demo accounts and opens the login window. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                int prepared = new DemoDataInitializer().initialise();
                if (prepared > 0) {
                    System.out.println("Demo accounts initialised: " + prepared);
                }
            } catch (AppException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Database connection problem",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
