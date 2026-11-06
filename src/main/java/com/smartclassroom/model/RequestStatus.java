package com.smartclassroom.model;

/** Life cycle shared by leave requests and room bookings. Stored as the enum name. */
public enum RequestStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String displayName;

    RequestStatus(String displayName) {
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
