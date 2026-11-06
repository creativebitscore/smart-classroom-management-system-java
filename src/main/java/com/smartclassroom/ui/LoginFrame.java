package com.smartclassroom.ui;

import com.smartclassroom.model.Role;
import com.smartclassroom.model.User;
import com.smartclassroom.service.AuthService;
import com.smartclassroom.service.exception.AppException;
import com.smartclassroom.ui.common.FormPanel;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;
import com.smartclassroom.util.Session;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Login screen: choose Admin / Faculty / Student, then enter username and password. */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final transient AuthService authService = new AuthService();
    private final Map<Role, JToggleButton> roleButtons = new EnumMap<>(Role.class);
    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton signIn = UiFactory.primaryButton("Sign in");

    public LoginFrame() {
        super("Smart Classroom Management System - Login");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(980, 620);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));
        add(buildBrandPanel());
        add(buildFormPanel());
        getRootPane().setDefaultButton(signIn);
    }

    private JPanel buildBrandPanel() {
        JPanel brand = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(), Theme.SIDEBAR));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(150, 48, 40, 48));
        JLabel name = new JLabel("Smart Classroom");
        name.setFont(Theme.FONT_TITLE.deriveFont(38f));
        name.setForeground(java.awt.Color.WHITE);
        JLabel subtitle = new JLabel("<html>Attendance, timetables, assignments and marks<br>in one simple place.</html>");
        subtitle.setFont(Theme.FONT_HEADING.deriveFont(java.awt.Font.PLAIN, 17f));
        subtitle.setForeground(new java.awt.Color(0xDBEAFE));
        brand.add(name);
        brand.add(javax.swing.Box.createVerticalStrut(14));
        brand.add(subtitle);
        return brand;
    }

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new java.awt.GridBagLayout());
        outer.setBackground(Theme.BACKGROUND);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        card.setPreferredSize(new Dimension(380, 440));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiFactory.title("Sign in"), BorderLayout.NORTH);
        top.add(UiFactory.muted("Choose your role and enter your details"), BorderLayout.SOUTH);

        JPanel roles = new JPanel(new GridLayout(1, 3, 6, 0));
        roles.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Role role : Role.values()) {
            JToggleButton button = new SegmentButton(role.getDisplayName());
            button.addActionListener(e -> {
                styleRoleButtons();
                messageLabel.setText(" ");
            });
            group.add(button);
            roles.add(button);
            roleButtons.put(role, button);
        }
        roleButtons.get(Role.STUDENT).setSelected(true);
        styleRoleButtons();

        FormPanel form = new FormPanel();
        form.addField("Login as", roles);
        username.putClientProperty("JTextField.placeholderText", "Username");
        form.addField("Username", username);
        password.putClientProperty("JTextField.placeholderText", "Password");
        form.addField("Password", password);

        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false);
        char defaultEcho = password.getEchoChar();
        show.addActionListener(e -> password.setEchoChar(show.isSelected() ? (char) 0 : defaultEcho));

        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setFont(Theme.FONT_BASE);
        signIn.addActionListener(e -> attemptLogin());
        signIn.setPreferredSize(new Dimension(100, 42));

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        JPanel showRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        showRow.setOpaque(false);
        showRow.add(show);
        bottom.add(showRow, BorderLayout.NORTH);
        bottom.add(messageLabel, BorderLayout.CENTER);
        bottom.add(signIn, BorderLayout.SOUTH);

        card.add(top, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        outer.add(card);
        return outer;
    }

    private void styleRoleButtons() {
        roleButtons.values().forEach(JToggleButton::repaint);
    }

    /** Role chooser button that paints itself, so it looks the same whatever Look and Feel is active. */
    private static final class SegmentButton extends JToggleButton {
        private static final long serialVersionUID = 1L;

        SegmentButton(String text) {
            super(text);
            setFont(Theme.FONT_BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 8, 9, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            } else {
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            setForeground(isSelected() ? java.awt.Color.WHITE : Theme.TEXT);
            super.paintComponent(g);
        }
    }

    private Role selectedRole() {
        for (Map.Entry<Role, JToggleButton> entry : roleButtons.entrySet()) {
            if (entry.getValue().isSelected()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Runs the login on a background thread so the window never freezes (hashing and the database take time). */
    private void attemptLogin() {
        messageLabel.setText(" ");
        signIn.setEnabled(false);
        signIn.setText("Signing in...");
        String user = username.getText();
        String pass = new String(password.getPassword());
        Role role = selectedRole();

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return authService.login(user, pass, role);
            }

            @Override
            protected void done() {
                signIn.setEnabled(true);
                signIn.setText("Sign in");
                try {
                    User loggedIn = get();
                    Session.start(loggedIn);
                    password.setText("");
                    dispose();
                    new DashboardFrame(loggedIn).setVisible(true);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    messageLabel.setText("<html>" + (cause instanceof AppException
                            ? cause.getMessage() : "Unexpected error: " + cause) + "</html>");
                    password.setText("");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
