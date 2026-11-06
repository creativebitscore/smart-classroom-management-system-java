package com.smartclassroom.model;

/** A course taught to one branch and semester. */
public class Subject {
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private String branch;
    private int semester;
    private int credits;

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
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

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    /** Used by combo boxes so the user sees "CS301 - Data Structures". */
    @Override
    public String toString() {
        return subjectCode + " - " + subjectName;
    }
}
