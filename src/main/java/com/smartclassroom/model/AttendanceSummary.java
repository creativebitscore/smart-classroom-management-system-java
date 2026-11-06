package com.smartclassroom.model;

import java.util.Collection;

/**
 * Attendance totals for one student in one subject (or across subjects).
 * The percentage rule lives here so it can be unit-tested without a database:
 * percentage = (present + late) / total classes * 100.
 */
public class AttendanceSummary {
    private final int studentId;
    private final String rollNo;
    private final String studentName;
    private final String subjectCode;
    private final String subjectName;
    private final int totalClasses;
    private final int attendedClasses;
    private final int lateClasses;

    public AttendanceSummary(int studentId, String rollNo, String studentName, String subjectCode,
                             String subjectName, int totalClasses, int attendedClasses, int lateClasses) {
        this.studentId = studentId;
        this.rollNo = rollNo;
        this.studentName = studentName;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
        this.lateClasses = lateClasses;
    }

    /** Pure calculation: returns 0 when there are no classes yet (avoids divide-by-zero). */
    public static double calculatePercentage(int attended, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return attended * 100.0 / total;
    }

    /** Combined percentage over several summaries (used for a student's overall attendance). */
    public static double overallPercentage(Collection<AttendanceSummary> summaries) {
        int attended = 0;
        int total = 0;
        for (AttendanceSummary s : summaries) {
            attended += s.attendedClasses;
            total += s.totalClasses;
        }
        return calculatePercentage(attended, total);
    }

    public double getPercentage() {
        return calculatePercentage(attendedClasses, totalClasses);
    }

    public boolean isBelowThreshold(double threshold) {
        return totalClasses > 0 && getPercentage() < threshold;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public int getAttendedClasses() {
        return attendedClasses;
    }

    public int getLateClasses() {
        return lateClasses;
    }
}
