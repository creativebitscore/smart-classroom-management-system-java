package com.smartclassroom.service;

import com.smartclassroom.model.Student;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.DbConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentServiceTest {
    private Fakes.FakeStudentDao studentDao;
    private Fakes.FakeUserDao userDao;
    private StudentService service;
    private final com.smartclassroom.model.Admin admin = Fakes.admin();

    @BeforeEach
    void setUp() {
        DbConnection.useTransactionRunner(new Fakes.DirectTransactions());
        studentDao = new Fakes.FakeStudentDao();
        userDao = new Fakes.FakeUserDao();
        service = new StudentService(studentDao, userDao);
    }

    @AfterEach
    void tearDown() {
        DbConnection.resetTransactionRunner();
    }

    @Test
    void validStudentIsAddedWithLoginAndProfile() {
        Student form = Fakes.newStudentForm();
        service.addStudent(admin, form, "Student@123");
        assertEquals(1, userDao.insertedUsernames.size());
        assertEquals("nisha.gupta", userDao.insertedUsernames.get(0));
        assertEquals(1, studentDao.inserted.size());
        assertEquals(100, form.getUserId());      // id of the new users row is linked to the profile
        assertEquals(501, form.getStudentId());
    }

    @Test
    void fieldsAreCleanedBeforeSaving() {
        Student form = Fakes.newStudentForm();
        service.addStudent(admin, form, "Student@123");
        assertEquals("CSE2307", form.getRollNo());
        assertEquals("CSE", form.getBranch());
        assertEquals("A", form.getSection());
    }

    @Test
    void emptyFieldsAreRejectedAndNothingIsSaved() {
        Student noName = Fakes.newStudentForm();
        noName.setFullName("  ");
        assertThrows(ValidationException.class, () -> service.addStudent(admin, noName, "Student@123"));

        Student noRoll = Fakes.newStudentForm();
        noRoll.setRollNo("");
        assertThrows(ValidationException.class, () -> service.addStudent(admin, noRoll, "Student@123"));

        Student noBranch = Fakes.newStudentForm();
        noBranch.setBranch(null);
        assertThrows(ValidationException.class, () -> service.addStudent(admin, noBranch, "Student@123"));

        assertTrue(studentDao.inserted.isEmpty());
        assertTrue(userDao.insertedUsernames.isEmpty());
    }

    @Test
    void invalidValuesAreRejected() {
        Student badEmail = Fakes.newStudentForm();
        badEmail.setEmail("nisha-at-college");
        assertThrows(ValidationException.class, () -> service.addStudent(admin, badEmail, "Student@123"));

        Student badPhone = Fakes.newStudentForm();
        badPhone.setPhone("123");
        assertThrows(ValidationException.class, () -> service.addStudent(admin, badPhone, "Student@123"));

        Student badSemester = Fakes.newStudentForm();
        badSemester.setSemester(11);
        assertThrows(ValidationException.class, () -> service.addStudent(admin, badSemester, "Student@123"));

        Student badUsername = Fakes.newStudentForm();
        badUsername.setUsername("no spaces allowed");
        assertThrows(ValidationException.class, () -> service.addStudent(admin, badUsername, "Student@123"));

        assertThrows(ValidationException.class, () -> service.addStudent(admin, Fakes.newStudentForm(), "weak"));
        assertTrue(studentDao.inserted.isEmpty());
    }

    @Test
    void duplicateRollNumberBecomesFriendlyConflict() {
        studentDao.failOnInsert = new SQLException("Duplicate entry 'CSE2307' for key 'students.roll_no'", "23000", 1062);
        ConflictException e = assertThrows(ConflictException.class,
                () -> service.addStudent(admin, Fakes.newStudentForm(), "Student@123"));
        assertTrue(e.getMessage().contains("roll number"));
    }

    @Test
    void updateChangesTheProfile() {
        Student existing = Fakes.student(3, 7, "Rahul Verma");
        existing.setFullName("Rahul V. Verma");
        existing.setPhone("9000000003");
        service.updateStudent(admin, existing);
        assertEquals(1, studentDao.updated.size());
        assertEquals("Rahul V. Verma", studentDao.updated.get(0).getFullName());
    }

    @Test
    void updateWithoutSelectionOrWithBadDataIsRejected() {
        assertThrows(ValidationException.class, () -> service.updateStudent(admin, Fakes.newStudentForm()));
        Student existing = Fakes.student(3, 7, "Rahul Verma");
        existing.setEmail("bad");
        assertThrows(ValidationException.class, () -> service.updateStudent(admin, existing));
        assertTrue(studentDao.updated.isEmpty());
    }

    @Test
    void deleteIsPassedToTheDao() {
        service.deleteStudent(admin, 4);
        assertEquals(java.util.List.of(4), studentDao.deleted);
    }

    // ----- role-based permissions -----

    @Test
    void facultyAndStudentCannotChangeStudentRecords() {
        com.smartclassroom.model.Faculty faculty = Fakes.faculty(1, 2, "Dr. Ravi Kumar");
        Student student = Fakes.student(1, 5, "Aarav Mehta");
        assertThrows(AccessDeniedException.class, () -> service.addStudent(faculty, Fakes.newStudentForm(), "Student@123"));
        assertThrows(AccessDeniedException.class, () -> service.addStudent(student, Fakes.newStudentForm(), "Student@123"));
        assertThrows(AccessDeniedException.class, () -> service.updateStudent(faculty, Fakes.student(1, 5, "X")));
        assertThrows(AccessDeniedException.class, () -> service.deleteStudent(student, 1));
        assertTrue(studentDao.inserted.isEmpty());
        assertTrue(studentDao.deleted.isEmpty());
    }

    @Test
    void facultyMayViewListButStudentMayNot() {
        studentDao.students.add(Fakes.student(1, 5, "Aarav Mehta"));
        assertEquals(1, service.getAll(Fakes.faculty(1, 2, "Dr. Ravi Kumar")).size());
        assertEquals(1, service.getAll(admin).size());
        assertThrows(AccessDeniedException.class, () -> service.getAll(Fakes.student(1, 5, "Aarav Mehta")));
        assertThrows(AccessDeniedException.class, () -> service.getAll(null));
    }
}
