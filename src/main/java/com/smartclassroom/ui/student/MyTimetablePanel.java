package com.smartclassroom.ui.student;

import com.smartclassroom.model.Student;
import com.smartclassroom.service.TimetableService;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.common.WeeklyTimetablePanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** The student's weekly timetable with classroom allocation. */
public class MyTimetablePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final transient Student student;
    private final transient TimetableService service = new TimetableService();
    private final WeeklyTimetablePanel grid = new WeeklyTimetablePanel(true);

    public MyTimetablePanel(Student student) {
        super(new BorderLayout(0, 16));
        this.student = student;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("My Timetable"), BorderLayout.NORTH);
        titles.add(UiFactory.muted(student.getBranch() + " semester " + student.getSemester() + ", section "
                + student.getSection()), BorderLayout.SOUTH);
        add(titles, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            grid.setEntries(service.getForStudent(student));
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
