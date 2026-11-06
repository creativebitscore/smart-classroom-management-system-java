package com.smartclassroom.service;

import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.DataAccessException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.Validator;

import java.util.List;

/** Business rules for subjects. */
public class SubjectService {
    private final SubjectDao subjectDao;

    public SubjectService() {
        this(new SubjectDao());
    }

    public SubjectService(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
    }

    public List<Subject> getAll() {
        return subjectDao.findAll();
    }

    /** Subjects a faculty member teaches according to the timetable. */
    public List<Subject> getTaughtBy(Faculty faculty) {
        return subjectDao.findByFaculty(faculty.getFacultyId());
    }

    public void add(User actor, Subject subject) {
        AccessControl.requireRole(actor, Role.ADMIN);
        validate(subject);
        try {
            subject.setSubjectId(subjectDao.insert(subject));
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    public void update(User actor, Subject subject) {
        AccessControl.requireRole(actor, Role.ADMIN);
        if (subject.getSubjectId() <= 0) {
            throw new ValidationException("Select a subject to update.");
        }
        validate(subject);
        try {
            subjectDao.update(subject);
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    /** Also removes the subject's timetable, attendance, assignments and marks (ON DELETE CASCADE). */
    public void delete(User actor, int subjectId) {
        AccessControl.requireRole(actor, Role.ADMIN);
        subjectDao.delete(subjectId);
    }

    static void validate(Subject s) {
        s.setSubjectCode(Validator.requireText(s.getSubjectCode(), "Subject code", 20).toUpperCase());
        s.setSubjectName(Validator.requireText(s.getSubjectName(), "Subject name", 100));
        s.setBranch(Validator.requireText(s.getBranch(), "Branch", 50).toUpperCase());
        Validator.requireIntInRange(s.getSemester(), "Semester", 1, 8);
        Validator.requireIntInRange(s.getCredits(), "Credits", 1, 6);
    }
}
