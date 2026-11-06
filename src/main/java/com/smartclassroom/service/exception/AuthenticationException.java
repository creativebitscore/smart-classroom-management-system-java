package com.smartclassroom.service.exception;

/** Thrown when login fails. */
public class AuthenticationException extends AppException {
    public AuthenticationException(String message) {
        super(message);
    }
}
