package com.smartclassroom.ui.faculty;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.service.AnnouncementService;
import com.smartclassroom.service.AssignmentService;
import com.smartclassroom.service.SubjectService;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Faculty dashboard: summary cards, today's classes and recent announcements. */
public class FacultyHomePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final transient Faculty faculty;
    private final transient SubjectService subjectService = new SubjectService();
    private final transient TimetableService timetableService = new TimetableService();
    private final transient AssignmentService assignmentService = new AssignmentService();
    private final transient AnnouncementService announcementService = new AnnouncementService();

    private final StatCard subjects = new StatCard("MY SUBJECTS", Theme.PRIMARY);
    private final StatCard weekly = new StatCard("CLASSES PER WEEK", Theme.SUCCESS);
    private final StatCard assignments = new StatCard("ASSIGNMENTS PUBLISHED", Theme.WARNING);
    private final StatCard today = new StatCard("CLASSES TODAY", new Color(0x7C3AED));
    private final ListCard todayList = new ListCard("Today's classes");
    private final ListCard announcements = new ListCard("Recent announcements");

    public FacultyHomePanel(Faculty faculty) {
        super(new BorderLayout(0, 20));
        this.faculty = faculty;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Welcome, " + faculty.getFullName()), BorderLayout.NORTH);
        titles.add(UiFactory.muted(faculty.getDesignation() + ", " + faculty.getDepartment()), BorderLayout.SOUTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(subjects);
        cards.add(weekly);
        cards.add(assignments);
        cards.add(today);

        JPanel lists = new JPanel(new GridLayout(1, 2, 16, 0));
        lists.setOpaque(false);
        lists.add(todayList);
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
            List<TimetableEntry> mine = timetableService.getForFaculty(faculty.getFacultyId());
            DayOfWeek todayDay = LocalDate.now().getDayOfWeek();
            List<TimetableEntry> todays = mine.stream().filter(t -> t.getDayOfWeek() == todayDay).toList();

            subjects.set(String.valueOf(subjectService.getTaughtBy(faculty).size()), "assigned in timetable");
            weekly.set(String.valueOf(mine.size()), "scheduled lectures");
            assignments.set(String.valueOf(assignmentService.getForFaculty(faculty).size()), "created by you");
            today.set(String.valueOf(todays.size()), todayDay.toString().charAt(0) + todayDay.toString().substring(1).toLowerCase());

            List<String[]> rows = new ArrayList<>();
            todays.forEach(t -> rows.add(new String[]{t.getSlotLabel() + "  " + t.getSubject(),
                    "Room " + t.getClassroom().getRoomName() + ", section " + t.getSection()}));
            todayList.setItems(rows);

            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM yyyy");
            List<String[]> news = new ArrayList<>();
            announcementService.getRecent(faculty, 4).forEach(a -> news.add(new String[]{a.getTitle(),
                    a.getCreatedAt().format(format) + " - " + a.getPostedByName()}));
            announcements.setItems(news);
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
