package com.smartclassroom.model;

import java.util.List;

/** A teacher. Extra fields come from the faculty table; name and email come from the users table. */
public class Faculty extends User {
    public static final String MENU_DASHBOARD = "Dashboard";
    public static final String MENU_MY_CLASSES = "My Classes";
    public static final String MENU_MARK_ATTENDANCE = "Mark Attendance";
    public static final String MENU_ATTENDANCE_SUMMARY = "Attendance Summary";
    public static final String MENU_STUDENTS = "Student List";
    public static final String MENU_ASSIGNMENTS = "Assignments";
    public static final String MENU_MARKS = "Marks";
    public static final String MENU_RANKINGS = "Class Rankings";
    public static final String MENU_LEAVE_REQUESTS = "Leave Requests";
    public static final String MENU_ROOM_BOOKING = "Room Booking";
    public static final String MENU_ANNOUNCEMENTS = "Announcements";

    private int facultyId;
    private String employeeCode;
    private String phone;
    private String department;
    private String designation;

    public Faculty() {
        super(Role.FACULTY);
    }

    @Override
    public String getDashboardTitle() {
        return "Faculty Portal";
    }

    @Override
    public List<String> getMenuItems() {
        return List.of(MENU_DASHBOARD, MENU_MY_CLASSES, MENU_MARK_ATTENDANCE, MENU_ATTENDANCE_SUMMARY,
                MENU_STUDENTS, MENU_ASSIGNMENTS, MENU_MARKS, MENU_RANKINGS, MENU_LEAVE_REQUESTS,
                MENU_ROOM_BOOKING, MENU_ANNOUNCEMENTS);
    }

    public int getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(int facultyId) {
        this.facultyId = facultyId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
