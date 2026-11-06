package com.smartclassroom.ui.student;

import com.smartclassroom.model.Assignment;
import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.service.AnnouncementService;
import com.smartclassroom.service.AssignmentService;
import com.smartclassroom.service.AttendanceService;
import com.smartclassroom.service.TimetableService;
import com.smartclassroom.ui.common.ListCard;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.common.StatCard;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Student dashboard: attendance, upcoming deadlines, today's classes and announcements. */
public class StudentHomePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final transient Student student;
    private final transient AttendanceService attendanceService = new AttendanceService();
    private final transient AssignmentService assignmentService = new AssignmentService();
    private final transient TimetableService timetableService = new TimetableService();
    private final transient AnnouncementService announcementService = new AnnouncementService();

    private final StatCard attendance = new StatCard("MY ATTENDANCE", Theme.PRIMARY);
    private final StatCard deadlines = new StatCard("UPCOMING DEADLINES", Theme.WARNING);
    private final StatCard today = new StatCard("CLASSES TODAY", Theme.SUCCESS);
    private final StatCard section = new StatCard("MY CLASS", new Color(0x7C3AED));
    private final ListCard deadlineList = new ListCard("Upcoming deadlines");
    private final ListCard alertList = new ListCard("Attendance alerts");
    private final ListCard announcements = new ListCard("Recent announcements");

    public StudentHomePanel(Student student) {
        super(new BorderLayout(0, 20));
        this.student = student;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Welcome, " + student.getFullName()), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Roll no " + student.getRollNo()), BorderLayout.SOUTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(attendance);
        cards.add(deadlines);
        cards.add(today);
        cards.add(section);

        JPanel lists = new JPanel(new GridLayout(1, 3, 16, 0));
        lists.setOpaque(false);
        lists.add(deadlineList);
        lists.add(alertList);
        lists.add(announcements);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(lists, BorderLayout.CENTER);
        add(titles, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            int threshold = attendanceService.getThreshold();
            List<AttendanceSummary> summaries = attendanceService.getSummaryForStudent(student);
            double overall = AttendanceSummary.overallPercentage(summaries);
            attendance.set(summaries.isEmpty() ? "-" : String.format("%.1f%%", overall),
                    summaries.isEmpty() ? "no classes recorded yet" : "minimum required " + threshold + "%");

            LocalDate now = LocalDate.now();
            List<Assignment> upcoming = assignmentService.getForStudent(student).stream()
                    .filter(a -> !a.isOverdue(now)).toList();
            deadlines.set(String.valueOf(upcoming.size()), "assignments not yet due");
            List<String[]> deadlineRows = new ArrayList<>();
            for (Assignment a : upcoming) {
                long days = ChronoUnit.DAYS.between(now, a.getDueDate());
                deadlineRows.add(new String[]{a.getTitle(), a.getSubjectName() + " - due " + a.getDueDate()
                        + (days == 0 ? " (today)" : " (in " + days + " days)")});
                if (deadlineRows.size() == 5) {
                    break;
                }
            }
            deadlineList.setItems(deadlineRows);

            List<TimetableEntry> todays = timetableService.getForStudent(student).stream()
                    .filter(t -> t.getDayOfWeek() == now.getDayOfWeek()).toList();
            today.set(String.valueOf(todays.size()), "scheduled today");
            section.set(student.getBranch() + " " + student.getSection(), "semester " + student.getSemester());

            List<String[]> alertRows = new ArrayList<>();
            for (AttendanceSummary s : summaries) {
                if (s.isBelowThreshold(threshold)) {
                    alertRows.add(new String[]{s.getSubjectName(),
                            String.format("%.1f%%", s.getPercentage()) + " is below the required " + threshold + "%"});
                }
            }
            alertList.setItems(alertRows, Theme.DANGER);

            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM yyyy");
            List<String[]> news = new ArrayList<>();
            announcementService.getRecent(student, 4).forEach(a -> news.add(new String[]{a.getTitle(),
                    a.getCreatedAt().format(format) + " - " + a.getPostedByName()}));
            announcements.setItems(news);
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
