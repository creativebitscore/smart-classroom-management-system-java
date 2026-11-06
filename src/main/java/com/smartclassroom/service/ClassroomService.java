package com.smartclassroom.service;

import com.smartclassroom.dao.ClassroomDao;
import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.DataAccessException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.Validator;

import java.util.List;

/** Business rules for classrooms and their capacities. */
public class ClassroomService {
    private static final int MAX_CAPACITY = 1000;
    private final ClassroomDao classroomDao;

    public ClassroomService() {
        this(new ClassroomDao());
    }

    public ClassroomService(ClassroomDao classroomDao) {
        this.classroomDao = classroomDao;
    }

    public List<Classroom> getAll() {
        return classroomDao.findAll();
    }

    public void add(User actor, Classroom room) {
        AccessControl.requireRole(actor, Role.ADMIN);
        validate(room);
        try {
            room.setClassroomId(classroomDao.insert(room));
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    public void update(User actor, Classroom room) {
        AccessControl.requireRole(actor, Role.ADMIN);
        if (room.getClassroomId() <= 0) {
            throw new ValidationException("Select a classroom to update.");
        }
        validate(room);
        try {
            classroomDao.update(room);
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    /** Deleting a classroom also removes the timetable entries that use it (ON DELETE CASCADE). */
    public void delete(User actor, int classroomId) {
        AccessControl.requireRole(actor, Role.ADMIN);
        classroomDao.delete(classroomId);
    }

    static void validate(Classroom room) {
        room.setRoomName(Validator.requireText(room.getRoomName(), "Room name", 30));
        Validator.requireIntInRange(room.getCapacity(), "Capacity", 1, MAX_CAPACITY);
        Validator.requireNotNull(room.getRoomType(), "a room type");
    }
}
