package com.smartclassroom.service;

import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.AccessDeniedException;

/** Checks that the actor is a faculty member and that he/she is really assigned to teach a subject. */
class FacultyGuard {
    private final SubjectDao subjectDao;

    FacultyGuard(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
    }

    Faculty requireFaculty(User actor) {
        AccessControl.requireRole(actor, Role.FACULTY);
        return (Faculty) actor;
    }

    void requireTeaches(Faculty faculty, int subjectId) {
        boolean teaches = subjectDao.findByFaculty(faculty.getFacultyId()).stream()
                .anyMatch(s -> s.getSubjectId() == subjectId);
        if (!teaches) {
            throw new AccessDeniedException("You are not assigned to teach this subject in the timetable.");
        }
    }

    Subject requireSubject(int subjectId) {
        return subjectDao.findById(subjectId)
                .orElseThrow(() -> new com.smartclassroom.service.exception.ValidationException("Subject not found."));
    }
}
