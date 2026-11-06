package com.smartclassroom.ui.admin;

import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.User;
import com.smartclassroom.service.AnnouncementService;
import com.smartclassroom.service.AttendanceService;
import com.smartclassroom.service.ReportService;
import com.smartclassroom.ui.common.ListCard;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.common.StatCard;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Admin dashboard: four summary cards, low-attendance alerts and recent announcements. */
public class AdminHomePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final transient User admin;
    private final transient ReportService reportService = new ReportService();
    private final transient AttendanceService attendanceService = new AttendanceService();
    private final transient AnnouncementService announcementService = new AnnouncementService();

    private final StatCard students = new StatCard("STUDENTS", Theme.PRIMARY);
    private final StatCard faculty = new StatCard("FACULTY", Theme.SUCCESS);
    private final StatCard classrooms = new StatCard("CLASSROOMS", Theme.WARNING);
    private final StatCard attendance = new StatCard("OVERALL ATTENDANCE", new java.awt.Color(0x7C3AED));
    private final ListCard alerts = new ListCard("Low attendance alerts");
    private final ListCard announcements = new ListCard("Recent announcements");

    public AdminHomePanel(User admin) {
        super(new BorderLayout(0, 20));
        this.admin = admin;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Welcome, " + admin.getFullName()), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Here is what is happening in your college today"), BorderLayout.SOUTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(students);
        cards.add(faculty);
        cards.add(classrooms);
        cards.add(attendance);

        JPanel lists = new JPanel(new GridLayout(1, 2, 16, 0));
        lists.setOpaque(false);
        lists.add(alerts);
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
            ReportService.DashboardStats stats = reportService.getDashboardStats(admin);
            students.set(String.valueOf(stats.students()), "registered students");
            faculty.set(String.valueOf(stats.faculty()), "teaching staff");
            classrooms.set(String.valueOf(stats.classrooms()), stats.subjects() + " subjects offered");
            attendance.set(String.format("%.1f%%", stats.overallAttendance()), "all subjects combined");

            int threshold = attendanceService.getThreshold();
            List<String[]> lowRows = new ArrayList<>();
            for (AttendanceSummary s : attendanceService.getBelowThreshold(admin, null, null)) {
                lowRows.add(new String[]{s.getStudentName() + " (" + s.getRollNo() + ")",
                        s.getSubjectCode() + ": " + String.format("%.1f%%", s.getPercentage()) + " is below " + threshold + "%"});
                if (lowRows.size() == 6) {
                    break;
                }
            }
            alerts.setItems(lowRows, Theme.DANGER);

            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM yyyy");
            List<String[]> news = new ArrayList<>();
            announcementService.getRecent(admin, 4).forEach(a -> news.add(new String[]{a.getTitle(),
                    a.getCreatedAt().format(format) + " - " + a.getPostedByName()}));
            announcements.setItems(news);
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
