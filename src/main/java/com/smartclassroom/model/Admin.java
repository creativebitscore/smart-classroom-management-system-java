package com.smartclassroom.model;

import java.util.List;

/** Administrator: manages everything in the system. */
public class Admin extends User {
    public static final String MENU_DASHBOARD = "Dashboard";
    public static final String MENU_STUDENTS = "Students";
    public static final String MENU_FACULTY = "Faculty";
    public static final String MENU_SUBJECTS = "Subjects";
    public static final String MENU_CLASSROOMS = "Classrooms";
    public static final String MENU_TIMETABLE = "Timetable";
    public static final String MENU_ATTENDANCE = "Attendance";
    public static final String MENU_LEAVE_REQUESTS = "Leave Requests";
    public static final String MENU_ROOM_REQUESTS = "Room Requests";
    public static final String MENU_RANKINGS = "Class Rankings";
    public static final String MENU_ANNOUNCEMENTS = "Announcements";
    public static final String MENU_REPORTS = "Reports";

    public Admin() {
        super(Role.ADMIN);
    }

    @Override
    public String getDashboardTitle() {
        return "Admin Console";
    }

    @Override
    public List<String> getMenuItems() {
        return List.of(MENU_DASHBOARD, MENU_STUDENTS, MENU_FACULTY, MENU_SUBJECTS, MENU_CLASSROOMS,
                MENU_TIMETABLE, MENU_ATTENDANCE, MENU_LEAVE_REQUESTS, MENU_ROOM_REQUESTS, MENU_RANKINGS,
                MENU_ANNOUNCEMENTS, MENU_REPORTS);
    }
}
