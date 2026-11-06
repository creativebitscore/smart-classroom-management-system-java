package com.smartclassroom.ui.faculty;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.service.TimetableService;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.common.WeeklyTimetablePanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** The faculty member's own weekly timetable (the classes assigned to them). */
public class MyClassesPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final transient Faculty faculty;
    private final transient TimetableService service = new TimetableService();
    private final WeeklyTimetablePanel grid = new WeeklyTimetablePanel(false);

    public MyClassesPanel(Faculty faculty) {
        super(new BorderLayout(0, 16));
        this.faculty = faculty;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("My Classes"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Your weekly timetable with rooms and sections"), BorderLayout.SOUTH);
        add(titles, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            grid.setEntries(service.getForFaculty(faculty.getFacultyId()));
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
