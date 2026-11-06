package com.smartclassroom.model;

import java.time.LocalDate;

/** One attendance mark: a student, in a subject, on a date. studentName/rollNo/subjectName are filled by joins for display. */
public class AttendanceRecord {
    private int attendanceId;
    private int studentId;
    private int subjectId;
    private LocalDate date;
    private AttendanceStatus status;
    private int markedBy;

    private String studentName;
    private String rollNo;
    private String subjectName;

    public AttendanceRecord() {
    }

    public AttendanceRecord(int studentId, int subjectId, LocalDate date, AttendanceStatus status, int markedBy) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.date = date;
        this.status = status;
        this.markedBy = markedBy;
    }

    public int getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(int attendanceId) {
        this.attendanceId = attendanceId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public int getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(int markedBy) {
        this.markedBy = markedBy;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
