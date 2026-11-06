package com.smartclassroom.ui;

import com.smartclassroom.model.Admin;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.User;
import com.smartclassroom.ui.admin.AdminHomePanel;
import com.smartclassroom.ui.admin.ClassroomPanel;
import com.smartclassroom.ui.admin.FacultyPanel;
import com.smartclassroom.ui.admin.ReportsPanel;
import com.smartclassroom.ui.admin.RoomRequestsPanel;
import com.smartclassroom.ui.admin.StudentPanel;
import com.smartclassroom.ui.admin.SubjectPanel;
import com.smartclassroom.ui.admin.TimetableScreen;
import com.smartclassroom.ui.common.AnnouncementsPanel;
import com.smartclassroom.ui.common.AttendanceOverviewPanel;
import com.smartclassroom.ui.common.LeaveReviewPanel;
import com.smartclassroom.ui.common.RankingsPanel;
import com.smartclassroom.ui.faculty.AssignmentPanel;
import com.smartclassroom.ui.faculty.FacultyHomePanel;
import com.smartclassroom.ui.faculty.MarkAttendancePanel;
import com.smartclassroom.ui.faculty.MarksPanel;
import com.smartclassroom.ui.faculty.MyClassesPanel;
import com.smartclassroom.ui.faculty.RoomBookingPanel;
import com.smartclassroom.ui.student.GradeCardPanel;
import com.smartclassroom.ui.student.MyAttendancePanel;
import com.smartclassroom.ui.student.MyLeavePanel;
import com.smartclassroom.ui.student.MyMarksPanel;
import com.smartclassroom.ui.student.MyTimetablePanel;
import com.smartclassroom.ui.student.ProfilePanel;
import com.smartclassroom.ui.student.StudentAssignmentsPanel;
import com.smartclassroom.ui.student.StudentHomePanel;

import javax.swing.JComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Connects each sidebar menu name to the screen it opens. The names come from the User subclasses
 * (User.getMenuItems()), so a menu entry and its screen can never get out of step.
 * Screens are created lazily (on first click) so the dashboard opens quickly.
 */
final class PanelFactory {
    private PanelFactory() {
    }

    static Map<String, Supplier<JComponent>> screensFor(User user) {
        Map<String, Supplier<JComponent>> screens = new LinkedHashMap<>();
        switch (user.getRole()) {
            case ADMIN -> {
                screens.put(Admin.MENU_DASHBOARD, () -> new AdminHomePanel(user));
                screens.put(Admin.MENU_STUDENTS, () -> new StudentPanel(user, false));
                screens.put(Admin.MENU_FACULTY, () -> new FacultyPanel(user));
                screens.put(Admin.MENU_SUBJECTS, () -> new SubjectPanel(user));
                screens.put(Admin.MENU_CLASSROOMS, () -> new ClassroomPanel(user));
                screens.put(Admin.MENU_TIMETABLE, () -> new TimetableScreen(user));
                screens.put(Admin.MENU_ATTENDANCE, () -> new AttendanceOverviewPanel(user));
                screens.put(Admin.MENU_LEAVE_REQUESTS, () -> new LeaveReviewPanel(user));
                screens.put(Admin.MENU_ROOM_REQUESTS, () -> new RoomRequestsPanel(user));
                screens.put(Admin.MENU_RANKINGS, () -> new RankingsPanel(user));
                screens.put(Admin.MENU_ANNOUNCEMENTS, () -> new AnnouncementsPanel(user));
                screens.put(Admin.MENU_REPORTS, () -> new ReportsPanel(user));
            }
            case FACULTY -> {
                Faculty faculty = (Faculty) user;
                screens.put(Faculty.MENU_DASHBOARD, () -> new FacultyHomePanel(faculty));
                screens.put(Faculty.MENU_MY_CLASSES, () -> new MyClassesPanel(faculty));
                screens.put(Faculty.MENU_MARK_ATTENDANCE, () -> new MarkAttendancePanel(faculty));
                screens.put(Faculty.MENU_ATTENDANCE_SUMMARY, () -> new AttendanceOverviewPanel(faculty));
                screens.put(Faculty.MENU_STUDENTS, () -> new StudentPanel(faculty, true));
                screens.put(Faculty.MENU_ASSIGNMENTS, () -> new AssignmentPanel(faculty));
                screens.put(Faculty.MENU_MARKS, () -> new MarksPanel(faculty));
                screens.put(Faculty.MENU_RANKINGS, () -> new RankingsPanel(faculty));
                screens.put(Faculty.MENU_LEAVE_REQUESTS, () -> new LeaveReviewPanel(faculty));
                screens.put(Faculty.MENU_ROOM_BOOKING, () -> new RoomBookingPanel(faculty));
                screens.put(Faculty.MENU_ANNOUNCEMENTS, () -> new AnnouncementsPanel(faculty));
            }
            case STUDENT -> {
                Student student = (Student) user;
                screens.put(Student.MENU_DASHBOARD, () -> new StudentHomePanel(student));
                screens.put(Student.MENU_PROFILE, () -> new ProfilePanel(student));
                screens.put(Student.MENU_ATTENDANCE, () -> new MyAttendancePanel(student));
                screens.put(Student.MENU_TIMETABLE, () -> new MyTimetablePanel(student));
                screens.put(Student.MENU_ASSIGNMENTS, () -> new StudentAssignmentsPanel(student));
                screens.put(Student.MENU_MARKS, () -> new MyMarksPanel(student));
                screens.put(Student.MENU_GRADE_CARD, () -> new GradeCardPanel(student));
                screens.put(Student.MENU_LEAVE, () -> new MyLeavePanel(student));
                screens.put(Student.MENU_ANNOUNCEMENTS, () -> new AnnouncementsPanel(student));
            }
        }
        return screens;
    }
}
