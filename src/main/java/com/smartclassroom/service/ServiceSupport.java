package com.smartclassroom.service;

import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.DataAccessException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Helpers shared by the services. */
final class ServiceSupport {
    /** MySQL names the violated key in its message; map that to a friendly sentence. */
    private static final Map<String, String> DUPLICATE_MESSAGES = new LinkedHashMap<>();

    static {
        DUPLICATE_MESSAGES.put("username", "This username is already taken.");
        DUPLICATE_MESSAGES.put("email", "This email address is already registered.");
        DUPLICATE_MESSAGES.put("roll_no", "A student with this roll number already exists.");
        DUPLICATE_MESSAGES.put("employee_code", "A faculty member with this employee code already exists.");
        DUPLICATE_MESSAGES.put("subject_code", "A subject with this code already exists.");
        DUPLICATE_MESSAGES.put("room_name", "A classroom with this name already exists.");
        DUPLICATE_MESSAGES.put("uq_attendance", "Attendance for this student, subject and date already exists.");
    }

    private ServiceSupport() {
    }

    static ConflictException duplicate(DataAccessException e) {
        String text = String.valueOf(e.getMessage()).toLowerCase();
        for (Map.Entry<String, String> entry : DUPLICATE_MESSAGES.entrySet()) {
            if (text.contains(entry.getKey())) {
                return new ConflictException(entry.getValue());
            }
        }
        return new ConflictException("This record already exists.");
    }
}
