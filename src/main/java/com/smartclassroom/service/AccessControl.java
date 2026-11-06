package com.smartclassroom.service;

import com.smartclassroom.model.Role;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.AccessDeniedException;

/** Role-based access control: every service that changes data asks this class first. */
public final class AccessControl {
    private AccessControl() {
    }

    /** Throws AccessDeniedException unless the actor is logged in and has one of the allowed roles. */
    public static void requireRole(User actor, Role... allowed) {
        if (actor == null) {
            throw new AccessDeniedException("Please log in first.");
        }
        for (Role role : allowed) {
            if (actor.getRole() == role) {
                return;
            }
        }
        throw new AccessDeniedException("Your role (" + actor.getRole().getDisplayName()
                + ") is not allowed to perform this action.");
    }
}
