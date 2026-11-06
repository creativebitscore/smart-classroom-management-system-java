package com.smartclassroom.service.exception;

/**
 * Base class of every application-specific exception.
 * It is unchecked (RuntimeException) so that the UI layer can catch one type
 * and show a friendly message, without every method needing a throws clause.
 */
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
