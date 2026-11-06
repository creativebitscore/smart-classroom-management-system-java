package com.smartclassroom.service;

import com.smartclassroom.dao.StudentDao;
import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.dao.TimetableDao;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.Validator;

import java.util.List;

/**
 * Timetable rules. Before a class is saved we check, in this order:
 *  1. the fields are valid and the end time is after the start time
 *  2. the classroom is big enough for the class (capacity validation)
 *  3. the classroom is not already booked at that time
 *  4. the faculty member is not teaching another class at that time
 *  5. the class (branch + semester + section) has no other lecture at that time
 */
public class TimetableService {
    private final TimetableDao timetableDao;
    private final SubjectDao subjectDao;
    private final StudentDao studentDao;

    public TimetableService() {
        this(new TimetableDao(), new SubjectDao(), new StudentDao());
    }

    public TimetableService(TimetableDao timetableDao, SubjectDao subjectDao, StudentDao studentDao) {
        this.timetableDao = timetableDao;
        this.subjectDao = subjectDao;
        this.studentDao = studentDao;
    }

    public List<TimetableEntry> getAll() {
        return timetableDao.findAll();
    }

    public List<TimetableEntry> getForFaculty(int facultyId) {
        return timetableDao.findByFaculty(facultyId);
    }

    public List<TimetableEntry> getForStudent(Student student) {
        return timetableDao.findForClass(student.getBranch(), student.getSemester(), student.getSection());
    }

    public void add(User actor, TimetableEntry entry) {
        AccessControl.requireRole(actor, Role.ADMIN);
        validateEntry(entry);
        checkCapacity(entry);
        checkConflicts(entry, 0);
        entry.setTimetableId(timetableDao.insert(entry));
    }

    public void update(User actor, TimetableEntry entry) {
        AccessControl.requireRole(actor, Role.ADMIN);
        if (entry.getTimetableId() <= 0) {
            throw new ValidationException("Select a timetable entry to update.");
        }
        validateEntry(entry);
        checkCapacity(entry);
        // the entry's own old slot must not count as a clash with itself
        checkConflicts(entry, entry.getTimetableId());
        timetableDao.update(entry);
    }

    public void delete(User actor, int timetableId) {
        AccessControl.requireRole(actor, Role.ADMIN);
        timetableDao.delete(timetableId);
    }

    void validateEntry(TimetableEntry e) {
        Validator.requireNotNull(e.getSubject(), "a subject");
        Validator.requireNotNull(e.getFaculty(), "a faculty member");
        Validator.requireNotNull(e.getClassroom(), "a classroom");
        Validator.requireNotNull(e.getDayOfWeek(), "a day");
        Validator.requireNotNull(e.getStartTime(), "a start time");
        Validator.requireNotNull(e.getEndTime(), "an end time");
        e.setSection(Validator.requireText(e.getSection(), "Section", 5).toUpperCase());
        if (!e.getStartTime().isBefore(e.getEndTime())) {
            throw new ValidationException("End time must be after start time.");
        }
    }

    /** Class size = students of the subject's branch + semester in this section. It must fit in the room. */
    void checkCapacity(TimetableEntry e) {
        Subject subject = subjectDao.findById(e.getSubject().getSubjectId())
                .orElseThrow(() -> new ValidationException("Subject not found."));
        int classSize = studentDao.countByClass(subject.getBranch(), subject.getSemester(), e.getSection());
        int capacity = e.getClassroom().getCapacity();
        if (classSize > capacity) {
            throw new ValidationException("Classroom " + e.getClassroom().getRoomName() + " seats " + capacity
                    + " but " + subject.getBranch() + " semester " + subject.getSemester() + " section "
                    + e.getSection() + " has " + classSize + " students. Choose a larger room.");
        }
    }

    void checkConflicts(TimetableEntry e, int excludeId) {
        Subject subject = subjectDao.findById(e.getSubject().getSubjectId())
                .orElseThrow(() -> new ValidationException("Subject not found."));

        List<TimetableEntry> roomClash = timetableDao.findRoomConflicts(
                e.getClassroom().getClassroomId(), e.getDayOfWeek(), e.getStartTime(), e.getEndTime(), excludeId);
        if (!roomClash.isEmpty()) {
            throw new ConflictException("Room " + e.getClassroom().getRoomName() + " is already booked: "
                    + describe(roomClash.get(0)));
        }

        List<TimetableEntry> facultyClash = timetableDao.findFacultyConflicts(
                e.getFaculty().getFacultyId(), e.getDayOfWeek(), e.getStartTime(), e.getEndTime(), excludeId);
        if (!facultyClash.isEmpty()) {
            throw new ConflictException(e.getFaculty().getFullName() + " is already teaching at that time: "
                    + describe(facultyClash.get(0)));
        }

        List<TimetableEntry> classClash = timetableDao.findClassConflicts(
                subject.getBranch(), subject.getSemester(), e.getSection(),
                e.getDayOfWeek(), e.getStartTime(), e.getEndTime(), excludeId);
        if (!classClash.isEmpty()) {
            throw new ConflictException("Section " + e.getSection() + " already has a lecture at that time: "
                    + describe(classClash.get(0)));
        }
    }

    private static String describe(TimetableEntry t) {
        return t.getSubject().getSubjectCode() + " (" + t.getDayOfWeek() + " " + t.getSlotLabel()
                + ", room " + t.getClassroom().getRoomName() + ", section " + t.getSection() + ")";
    }
}
