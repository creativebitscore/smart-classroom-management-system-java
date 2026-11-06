package com.smartclassroom.service;

import com.smartclassroom.dao.UserDao;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.AuthenticationException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private Fakes.FakeUserDao userDao;
    private Fakes.FakeStudentDao studentDao;
    private Fakes.FakeFacultyDao facultyDao;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userDao = new Fakes.FakeUserDao();
        studentDao = new Fakes.FakeStudentDao();
        facultyDao = new Fakes.FakeFacultyDao();

        userDao.accounts.put("admin", new UserDao.Credentials(1, PasswordUtil.hash("Admin@123"), Role.ADMIN, true));
        userDao.admins.put(1, Fakes.admin());
        userDao.accounts.put("faculty1", new UserDao.Credentials(2, PasswordUtil.hash("Faculty@123"), Role.FACULTY, true));
        facultyDao.byUserId.put(2, Fakes.faculty(1, 2, "Dr. Ravi Kumar"));
        userDao.accounts.put("student1", new UserDao.Credentials(5, PasswordUtil.hash("Student@123"), Role.STUDENT, true));
        studentDao.students.add(Fakes.student(1, 5, "Aarav Mehta"));
        userDao.accounts.put("old", new UserDao.Credentials(9, PasswordUtil.hash("Student@123"), Role.STUDENT, false));
        userDao.accounts.put("fresh", new UserDao.Credentials(10, "PENDING", Role.STUDENT, true));

        service = new AuthService(userDao, studentDao, facultyDao);
    }

    @Test
    void validLoginForEachRoleReturnsTheRightObject() {
        User admin = service.login("admin", "Admin@123", Role.ADMIN);
        User faculty = service.login("faculty1", "Faculty@123", Role.FACULTY);
        User student = service.login("student1", "Student@123", Role.STUDENT);
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals("Dr. Ravi Kumar", faculty.getFullName());
        assertEquals("Aarav Mehta", student.getFullName());
        assertTrue(student instanceof com.smartclassroom.model.Student);
    }

    @Test
    void usernameIsTrimmed() {
        assertEquals("System Administrator", service.login("  admin ", "Admin@123", Role.ADMIN).getFullName());
    }

    @Test
    void wrongPasswordIsRejected() {
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> service.login("admin", "wrong", Role.ADMIN));
        assertEquals("Invalid username or password.", e.getMessage());
    }

    @Test
    void unknownUserGetsTheSameMessageAsWrongPassword() {
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> service.login("nobody", "Admin@123", Role.ADMIN));
        assertEquals("Invalid username or password.", e.getMessage());
    }

    @Test
    void correctPasswordButWrongRoleTabIsRejected() {
        assertThrows(AuthenticationException.class, () -> service.login("student1", "Student@123", Role.ADMIN));
        assertThrows(AuthenticationException.class, () -> service.login("admin", "Admin@123", Role.STUDENT));
    }

    @Test
    void disabledAccountCannotLogIn() {
        assertThrows(AuthenticationException.class, () -> service.login("old", "Student@123", Role.STUDENT));
    }

    @Test
    void unpreparedPlaceholderPasswordNeverWorks() {
        assertThrows(AuthenticationException.class, () -> service.login("fresh", "PENDING", Role.STUDENT));
    }

    @Test
    void emptyFieldsAreValidationErrors() {
        assertThrows(ValidationException.class, () -> service.login("", "Admin@123", Role.ADMIN));
        assertThrows(ValidationException.class, () -> service.login("admin", "", Role.ADMIN));
        assertThrows(ValidationException.class, () -> service.login(null, null, Role.ADMIN));
        assertThrows(ValidationException.class, () -> service.login("admin", "Admin@123", null));
    }

    @Test
    void changePasswordChecksOldPasswordAndStrength() {
        User student = service.login("student1", "Student@123", Role.STUDENT);
        assertThrows(AuthenticationException.class, () -> service.changePassword(student, "wrong", "NewPass@123"));
        assertThrows(ValidationException.class, () -> service.changePassword(student, "Student@123", "short"));
        service.changePassword(student, "Student@123", "NewPass@123");
        String stored = userDao.updatedHashes.get(5);
        assertTrue(PasswordUtil.verify("NewPass@123", stored));
        assertFalse(stored.contains("NewPass@123"));
    }

    @Test
    void onlyAdminMayResetSomeoneElsesPassword() {
        User admin = service.login("admin", "Admin@123", Role.ADMIN);
        User student = service.login("student1", "Student@123", Role.STUDENT);
        assertThrows(AccessDeniedException.class, () -> service.resetPassword(student, 5, "Another@123"));
        service.resetPassword(admin, 5, "Another@123");
        assertTrue(PasswordUtil.verify("Another@123", userDao.updatedHashes.get(5)));
    }
}
