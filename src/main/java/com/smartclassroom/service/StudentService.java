package com.smartclassroom.service;

import com.smartclassroom.dao.StudentDao;
import com.smartclassroom.dao.UserDao;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.DataAccessException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.DbConnection;
import com.smartclassroom.util.PasswordUtil;
import com.smartclassroom.util.Validator;

import java.util.List;

/** Business rules for student records. */
public class StudentService {
    private final StudentDao studentDao;
    private final UserDao userDao;

    public StudentService() {
        this(new StudentDao(), new UserDao());
    }

    public StudentService(StudentDao studentDao, UserDao userDao) {
        this.studentDao = studentDao;
        this.userDao = userDao;
    }

    public List<Student> getAll(User actor) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        return studentDao.findAll();
    }

    public List<Student> getByClass(User actor, String branch, int semester, String section) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        return studentDao.findByClass(branch, semester, section);
    }

    /**
     * Creates the login row (users) and the profile row (students) in ONE transaction:
     * if the second insert fails, the first is rolled back, so no half-created student remains.
     */
    public void addStudent(User actor, Student student, String plainPassword) {
        AccessControl.requireRole(actor, Role.ADMIN);
        validate(student, true);
        Validator.requirePassword(plainPassword);
        String hash = PasswordUtil.hash(plainPassword);
        try {
            DbConnection.executeInTransaction(connection -> {
                int userId = userDao.insert(connection, student.getUsername(), hash, Role.STUDENT,
                        student.getFullName(), student.getEmail());
                student.setUserId(userId);
                student.setStudentId(studentDao.insert(connection, student));
                return null;
            });
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    public void updateStudent(User actor, Student student) {
        AccessControl.requireRole(actor, Role.ADMIN);
        if (student.getStudentId() <= 0) {
            throw new ValidationException("Select a student to update.");
        }
        validate(student, false);
        try {
            DbConnection.executeInTransaction(connection -> {
                userDao.updateProfile(connection, student.getUserId(), student.getFullName(), student.getEmail());
                studentDao.update(connection, student);
                return null;
            });
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    public void deleteStudent(User actor, int studentId) {
        AccessControl.requireRole(actor, Role.ADMIN);
        studentDao.delete(studentId);
    }

    /** Trims and checks every field; the cleaned values are written back into the object. */
    static void validate(Student s, boolean checkUsername) {
        s.setFullName(Validator.requireText(s.getFullName(), "Full name", 100));
        s.setEmail(Validator.requireEmail(s.getEmail()));
        if (checkUsername) {
            s.setUsername(Validator.requireUsername(s.getUsername()));
        }
        s.setRollNo(Validator.requireText(s.getRollNo(), "Roll number", 20).toUpperCase());
        s.setPhone(Validator.optionalPhone(s.getPhone()));
        s.setBranch(Validator.requireText(s.getBranch(), "Branch", 50).toUpperCase());
        Validator.requireIntInRange(s.getSemester(), "Semester", 1, 8);
        s.setSection(Validator.requireText(s.getSection(), "Section", 5).toUpperCase());
    }
}
