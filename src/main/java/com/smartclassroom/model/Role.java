package com.smartclassroom.model;

/** The three kinds of users. Stored in the database as the enum name (ADMIN, FACULTY, STUDENT). */
public enum Role {
    ADMIN("Admin"),
    FACULTY("Faculty"),
    STUDENT("Student");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
