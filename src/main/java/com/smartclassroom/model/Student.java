package com.smartclassroom.model;

import java.util.List;

/** A student. Extra fields come from the students table; name and email come from the users table. */
public class Student extends User {
    public static final String MENU_DASHBOARD = "Dashboard";
    public static final String MENU_PROFILE = "My Profile";
    public static final String MENU_ATTENDANCE = "My Attendance";
    public static final String MENU_TIMETABLE = "My Timetable";
    public static final String MENU_ASSIGNMENTS = "Assignments";
    public static final String MENU_MARKS = "My Marks";
    public static final String MENU_GRADE_CARD = "Grade Card";
    public static final String MENU_LEAVE = "My Leave";
    public static final String MENU_ANNOUNCEMENTS = "Announcements";

    private int studentId;
    private String rollNo;
    private String phone;
    private String branch;
    private int semester;
    private String section;

    public Student() {
        super(Role.STUDENT);
    }

    @Override
    public String getDashboardTitle() {
        return "Student Portal";
    }

    @Override
    public List<String> getMenuItems() {
        return List.of(MENU_DASHBOARD, MENU_PROFILE, MENU_ATTENDANCE, MENU_TIMETABLE,
                MENU_ASSIGNMENTS, MENU_MARKS, MENU_GRADE_CARD, MENU_LEAVE, MENU_ANNOUNCEMENTS);
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
