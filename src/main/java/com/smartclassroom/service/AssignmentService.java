package com.smartclassroom.service;

import com.smartclassroom.dao.AssignmentDao;
import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.model.Assignment;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Student;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.model.User;
import com.smartclassroom.util.Validator;

import java.time.LocalDate;
import java.util.List;

/** Faculty publish assignments; students read them. */
public class AssignmentService {
    private final AssignmentDao assignmentDao;
    private final FacultyGuard guard;

    public AssignmentService() {
        this(new AssignmentDao(), new SubjectDao());
    }

    public AssignmentService(AssignmentDao assignmentDao, SubjectDao subjectDao) {
        this.assignmentDao = assignmentDao;
        this.guard = new FacultyGuard(subjectDao);
    }

    public List<Assignment> getForFaculty(User actor) {
        Faculty faculty = guard.requireFaculty(actor);
        return assignmentDao.findByFaculty(faculty.getFacultyId());
    }

    public List<Assignment> getForStudent(Student student) {
        return assignmentDao.findForClass(student.getBranch(), student.getSemester());
    }

    /** Number of assignments not yet overdue (due today or later). */
    public long countUpcoming(List<Assignment> assignments, LocalDate today) {
        return assignments.stream().filter(a -> !a.isOverdue(today)).count();
    }

    public void create(User actor, Assignment assignment) {
        Faculty faculty = guard.requireFaculty(actor);
        validate(assignment);
        if (assignment.getDueDate().isBefore(LocalDate.now())) {
            throw new ValidationException("The due date cannot be in the past.");
        }
        guard.requireTeaches(faculty, assignment.getSubjectId());
        assignment.setFacultyId(faculty.getFacultyId());
        assignment.setAssignmentId(assignmentDao.insert(assignment));
    }

    public void update(User actor, Assignment assignment) {
        Faculty faculty = guard.requireFaculty(actor);
        validate(assignment);
        requireOwner(faculty, assignment.getAssignmentId());
        guard.requireTeaches(faculty, assignment.getSubjectId());
        assignmentDao.update(assignment);
    }

    public void delete(User actor, int assignmentId) {
        Faculty faculty = guard.requireFaculty(actor);
        requireOwner(faculty, assignmentId);
        assignmentDao.delete(assignmentId);
    }

    private void requireOwner(Faculty faculty, int assignmentId) {
        Assignment existing = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new ValidationException("Assignment not found."));
        if (existing.getFacultyId() != faculty.getFacultyId()) {
            throw new AccessDeniedException("You can only change your own assignments.");
        }
    }

    private static void validate(Assignment a) {
        a.setTitle(Validator.requireText(a.getTitle(), "Title", 150));
        if (a.getDescription() != null && a.getDescription().length() > 2000) {
            throw new ValidationException("Description must be at most 2000 characters.");
        }
        if (a.getSubjectId() <= 0) {
            throw new ValidationException("Please select a subject.");
        }
        Validator.requireNotNull(a.getDueDate(), "a due date");
    }
}
