package com.smartclassroom.ui.student;

import com.smartclassroom.model.Student;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only profile of the logged-in student. */
public class ProfilePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public ProfilePanel(Student s) {
        super(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("My Profile"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("To change these details, contact the administrator"), BorderLayout.SOUTH);

        Map<String, String> details = new LinkedHashMap<>();
        details.put("Full name", s.getFullName());
        details.put("Roll number", s.getRollNo());
        details.put("Username", s.getUsername());
        details.put("Email", s.getEmail());
        details.put("Phone", s.getPhone() == null ? "Not provided" : s.getPhone());
        details.put("Branch", s.getBranch());
        details.put("Semester", String.valueOf(s.getSemester()));
        details.put("Section", s.getSection());

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        int row = 0;
        for (Map.Entry<String, String> entry : details.entrySet()) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(8, 0, 8, 40);
            c.gridx = 0;
            JLabel key = UiFactory.muted(entry.getKey());
            key.setFont(Theme.FONT_BOLD);
            grid.add(key, c);
            c.gridx = 1;
            c.weightx = 1;
            JLabel value = new JLabel(entry.getValue());
            value.setFont(Theme.FONT_HEADING);
            grid.add(value, c);
        }

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        card.add(grid, BorderLayout.NORTH);

        add(titles, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }
}
