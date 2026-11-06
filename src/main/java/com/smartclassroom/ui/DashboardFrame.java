package com.smartclassroom.ui;

import com.smartclassroom.model.User;
import com.smartclassroom.ui.common.PasswordDialog;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;
import com.smartclassroom.util.Session;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Main window after login: dark sidebar on the left, header on top, screens in the middle.
 * The sidebar entries come from user.getMenuItems(): each role (Admin, Faculty, Student) supplies its
 * own list (polymorphism), so this class contains no "if role == ..." logic for the menu.
 */
public class DashboardFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final transient User user;
    private final transient Map<String, Supplier<JComponent>> factories;
    private final Map<String, JComponent> created = new HashMap<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final JLabel pageTitle = new JLabel();

    public DashboardFrame(User user) {
        super("Smart Classroom Management System");
        this.user = user;
        this.factories = PanelFactory.screensFor(user);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720)); // tall enough for the longest (admin) menu
        setSize(1320, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND);

        add(buildSidebar(), BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout());
        right.add(buildHeader(), BorderLayout.NORTH);
        content.setBackground(Theme.BACKGROUND);
        right.add(content, BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);

        showScreen(user.getMenuItems().get(0));
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 16, 16));
        sidebar.setPreferredSize(new Dimension(250, 10));

        JLabel brand = new JLabel("Smart Classroom");
        brand.setFont(Theme.FONT_HEADING);
        brand.setForeground(java.awt.Color.WHITE);
        brand.setAlignmentX(LEFT_ALIGNMENT);
        JLabel role = new JLabel(user.getDashboardTitle());
        role.setFont(Theme.FONT_SMALL);
        role.setForeground(Theme.SIDEBAR_TEXT);
        role.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(role);
        sidebar.add(Box.createVerticalStrut(28));

        ButtonGroup group = new ButtonGroup();
        List<String> items = user.getMenuItems(); // polymorphic: differs for Admin, Faculty, Student
        for (String item : items) {
            NavButton button = new NavButton(item);
            button.setSelected(item.equals(items.get(0)));
            button.addActionListener(e -> showScreen(item));
            group.add(button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(2));
        }
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)));

        pageTitle.setFont(Theme.FONT_HEADING);
        header.add(pageTitle, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JLabel who = new JLabel(user.getFullName() + "  |  " + user.getRole().getDisplayName());
        who.setFont(Theme.FONT_BASE);
        who.setForeground(Theme.TEXT_MUTED);
        JButton password = UiFactory.secondaryButton("Change password");
        password.addActionListener(e -> {
            if (PasswordDialog.change(this, user)) {
                UiFactory.success(this, "Your password was changed.");
            }
        });
        JButton logout = UiFactory.secondaryButton("Logout");
        logout.addActionListener(e -> logout());
        actions.add(who);
        actions.add(password);
        actions.add(logout);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    /** Creates the screen the first time, then shows it and asks it to reload its data. */
    private void showScreen(String name) {
        JComponent screen = created.get(name);
        if (screen == null) {
            screen = factories.get(name).get();
            created.put(name, screen);
            content.add(screen, name);
        }
        pageTitle.setText(name);
        cardLayout.show(content, name);
        if (screen instanceof Refreshable refreshable) {
            refreshable.refresh();
        }
    }

    private void logout() {
        if (UiFactory.confirm(this, "Do you want to log out?", "Logout")) {
            Session.end();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
