package com.smartclassroom.service.exception;

/** Thrown when an operation clashes with existing data (double-booked room, duplicate attendance, duplicate roll number). */
public class ConflictException extends AppException {
    public ConflictException(String message) {
        super(message);
    }
}
