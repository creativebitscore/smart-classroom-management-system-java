package com.smartclassroom.service;

import com.smartclassroom.dao.FacultyDao;
import com.smartclassroom.dao.StudentDao;
import com.smartclassroom.dao.UserDao;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.AuthenticationException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.PasswordUtil;
import com.smartclassroom.util.Validator;

/** Login, password change and admin password reset. */
public class AuthService {
    private static final String BAD_CREDENTIALS = "Invalid username or password.";

    private final UserDao userDao;
    private final StudentDao studentDao;
    private final FacultyDao facultyDao;

    public AuthService() {
        this(new UserDao(), new StudentDao(), new FacultyDao());
    }

    public AuthService(UserDao userDao, StudentDao studentDao, FacultyDao facultyDao) {
        this.userDao = userDao;
        this.studentDao = studentDao;
        this.facultyDao = facultyDao;
    }

    /**
     * Checks the credentials and returns the matching Admin, Faculty or Student object (polymorphism:
     * the caller only sees a User). The same message is used for "unknown user" and "wrong password"
     * so an attacker cannot find out which usernames exist.
     */
    public User login(String username, String password, Role selectedRole) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new ValidationException("Please enter both username and password.");
        }
        if (selectedRole == null) {
            throw new ValidationException("Please select a role.");
        }
        UserDao.Credentials credentials = userDao.findCredentials(username.trim())
                .orElseThrow(() -> new AuthenticationException(BAD_CREDENTIALS));

        if (!PasswordUtil.verify(password, credentials.passwordHash())) {
            throw new AuthenticationException(BAD_CREDENTIALS);
        }
        if (!credentials.active()) {
            throw new AuthenticationException("This account has been disabled. Contact the administrator.");
        }
        if (credentials.role() != selectedRole) {
            throw new AuthenticationException("This account cannot sign in as "
                    + selectedRole.getDisplayName() + ". Choose the correct role tab.");
        }
        return loadProfile(credentials);
    }

    private User loadProfile(UserDao.Credentials credentials) {
        int userId = credentials.userId();
        return switch (credentials.role()) {
            case ADMIN -> userDao.findAdmin(userId).orElseThrow(this::missingProfile);
            case FACULTY -> facultyDao.findByUserId(userId).orElseThrow(this::missingProfile);
            case STUDENT -> studentDao.findByUserId(userId).orElseThrow(this::missingProfile);
        };
    }

    private AuthenticationException missingProfile() {
        return new AuthenticationException("Your profile record is missing. Contact the administrator.");
    }

    /** A logged-in user changes his or her own password. */
    public void changePassword(User actor, String oldPassword, String newPassword) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY, Role.STUDENT);
        UserDao.Credentials credentials = userDao.findCredentials(actor.getUsername())
                .orElseThrow(() -> new AuthenticationException(BAD_CREDENTIALS));
        if (!PasswordUtil.verify(oldPassword, credentials.passwordHash())) {
            throw new AuthenticationException("The current password is incorrect.");
        }
        Validator.requirePassword(newPassword);
        userDao.updatePasswordHash(actor.getUserId(), PasswordUtil.hash(newPassword));
    }

    /** Admin sets a new password for any user (for example a student who forgot it). */
    public void resetPassword(User actor, int userId, String newPassword) {
        AccessControl.requireRole(actor, Role.ADMIN);
        Validator.requirePassword(newPassword);
        userDao.updatePasswordHash(userId, PasswordUtil.hash(newPassword));
    }
}
