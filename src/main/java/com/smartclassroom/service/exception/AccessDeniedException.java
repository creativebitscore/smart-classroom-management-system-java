package com.smartclassroom.service.exception;

/** Thrown when a logged-in user tries an action that their role does not allow. */
public class AccessDeniedException extends AppException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
