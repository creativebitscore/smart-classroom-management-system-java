package com.smartclassroom.service;

import com.smartclassroom.dao.FacultyDao;
import com.smartclassroom.dao.UserDao;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.DataAccessException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.DbConnection;
import com.smartclassroom.util.PasswordUtil;
import com.smartclassroom.util.Validator;

import java.util.List;

/** Business rules for faculty records. */
public class FacultyService {
    private final FacultyDao facultyDao;
    private final UserDao userDao;

    public FacultyService() {
        this(new FacultyDao(), new UserDao());
    }

    public FacultyService(FacultyDao facultyDao, UserDao userDao) {
        this.facultyDao = facultyDao;
        this.userDao = userDao;
    }

    public List<Faculty> getAll(User actor) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        return facultyDao.findAll();
    }

    /** Creates users + faculty rows in one transaction. */
    public void addFaculty(User actor, Faculty faculty, String plainPassword) {
        AccessControl.requireRole(actor, Role.ADMIN);
        validate(faculty, true);
        Validator.requirePassword(plainPassword);
        String hash = PasswordUtil.hash(plainPassword);
        try {
            DbConnection.executeInTransaction(connection -> {
                int userId = userDao.insert(connection, faculty.getUsername(), hash, Role.FACULTY,
                        faculty.getFullName(), faculty.getEmail());
                faculty.setUserId(userId);
                faculty.setFacultyId(facultyDao.insert(connection, faculty));
                return null;
            });
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    public void updateFaculty(User actor, Faculty faculty) {
        AccessControl.requireRole(actor, Role.ADMIN);
        if (faculty.getFacultyId() <= 0) {
            throw new ValidationException("Select a faculty member to update.");
        }
        validate(faculty, false);
        try {
            DbConnection.executeInTransaction(connection -> {
                userDao.updateProfile(connection, faculty.getUserId(), faculty.getFullName(), faculty.getEmail());
                facultyDao.update(connection, faculty);
                return null;
            });
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    public void deleteFaculty(User actor, int facultyId) {
        AccessControl.requireRole(actor, Role.ADMIN);
        try {
            facultyDao.delete(facultyId);
        } catch (DataAccessException e) {
            if (e.isForeignKeyViolation()) {
                throw new ConflictException("This faculty member cannot be deleted because attendance records "
                        + "were marked by them.");
            }
            throw e;
        }
    }

    static void validate(Faculty f, boolean checkUsername) {
        f.setFullName(Validator.requireText(f.getFullName(), "Full name", 100));
        f.setEmail(Validator.requireEmail(f.getEmail()));
        if (checkUsername) {
            f.setUsername(Validator.requireUsername(f.getUsername()));
        }
        f.setEmployeeCode(Validator.requireText(f.getEmployeeCode(), "Employee code", 20).toUpperCase());
        f.setPhone(Validator.optionalPhone(f.getPhone()));
        f.setDepartment(Validator.requireText(f.getDepartment(), "Department", 50).toUpperCase());
        f.setDesignation(Validator.requireText(f.getDesignation(), "Designation", 50));
    }
}
