package com.smartclassroom.service.exception;

/** Thrown when user input is empty, malformed or breaks a business rule (e.g. capacity too small). */
public class ValidationException extends AppException {
    public ValidationException(String message) {
        super(message);
    }
}
