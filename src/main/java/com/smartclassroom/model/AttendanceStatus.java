package com.smartclassroom.model;

/** Attendance values supported by the system. LATE still counts as attended when percentages are calculated. */
public enum AttendanceStatus {
    PRESENT("Present", true),
    ABSENT("Absent", false),
    LATE("Late", true);

    private final String displayName;
    private final boolean countsAsAttended;

    AttendanceStatus(String displayName, boolean countsAsAttended) {
        this.displayName = displayName;
        this.countsAsAttended = countsAsAttended;
    }

    public boolean countsAsAttended() {
        return countsAsAttended;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
