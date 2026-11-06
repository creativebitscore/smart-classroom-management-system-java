package com.smartclassroom.service;

import com.smartclassroom.dao.MarkDao;
import com.smartclassroom.dao.StudentDao;
import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Mark;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.DbConnection;
import com.smartclassroom.util.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Entering and reading marks. */
public class MarkService {
    private static final double MAX_ALLOWED = 1000;

    private final MarkDao markDao;
    private final StudentDao studentDao;
    private final FacultyGuard guard;

    public MarkService() {
        this(new MarkDao(), new StudentDao(), new SubjectDao());
    }

    public MarkService(MarkDao markDao, StudentDao studentDao, SubjectDao subjectDao) {
        this.markDao = markDao;
        this.studentDao = studentDao;
        this.guard = new FacultyGuard(subjectDao);
    }

    public List<Student> getRoster(User actor, int subjectId, String section) {
        Faculty faculty = guard.requireFaculty(actor);
        guard.requireTeaches(faculty, subjectId);
        Subject subject = guard.requireSubject(subjectId);
        return studentDao.findByClass(subject.getBranch(), subject.getSemester(), section);
    }

    /** Marks already saved for an exam, keyed by student id. */
    public Map<Integer, Mark> getExisting(User actor, int subjectId, String examType) {
        Faculty faculty = guard.requireFaculty(actor);
        guard.requireTeaches(faculty, subjectId);
        Map<Integer, Mark> byStudent = new HashMap<>();
        for (Mark m : markDao.findBySubjectAndExam(subjectId, normaliseExam(examType))) {
            byStudent.put(m.getStudentId(), m);
        }
        return byStudent;
    }

    public List<Mark> getForStudent(Student student) {
        return markDao.findByStudent(student.getStudentId());
    }

    /** Saves (or updates) marks for many students in one transaction. */
    public void saveMarks(User actor, int subjectId, String examType, double maxMarks,
                          Map<Integer, Double> marksByStudent) {
        Faculty faculty = guard.requireFaculty(actor);
        String exam = normaliseExam(examType);
        if (Double.isNaN(maxMarks) || maxMarks <= 0 || maxMarks > MAX_ALLOWED) {
            throw new ValidationException("Maximum marks must be between 1 and " + (int) MAX_ALLOWED + ".");
        }
        if (marksByStudent == null || marksByStudent.isEmpty()) {
            throw new ValidationException("Enter marks for at least one student.");
        }
        guard.requireTeaches(faculty, subjectId);

        List<Mark> marks = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : marksByStudent.entrySet()) {
            double value = entry.getValue();
            if (Double.isNaN(value) || value < 0 || value > maxMarks) {
                throw new ValidationException("Marks must be between 0 and " + maxMarks + ".");
            }
            Mark m = new Mark();
            m.setStudentId(entry.getKey());
            m.setSubjectId(subjectId);
            m.setExamType(exam);
            m.setMarksObtained(value);
            m.setMaxMarks(maxMarks);
            marks.add(m);
        }
        DbConnection.executeInTransaction(connection -> {
            markDao.upsertBatch(connection, marks);
            return null;
        });
    }

    private static String normaliseExam(String examType) {
        return Validator.requireText(examType, "Exam type", 30).toUpperCase();
    }
}
