package com.smartclassroom.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One scheduled class. This is composition in action: a timetable entry is made of
 * (has-a) Subject, Faculty and Classroom objects.
 */
public class TimetableEntry {
    private int timetableId;
    private Subject subject;
    private Faculty faculty;
    private Classroom classroom;
    private String section;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public int getTimetableId() {
        return timetableId;
    }

    public void setTimetableId(int timetableId) {
        this.timetableId = timetableId;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    /** Text such as "09:00-10:00", also used as the row key of the weekly grid. */
    public String getSlotLabel() {
        return startTime + "-" + endTime;
    }
}
