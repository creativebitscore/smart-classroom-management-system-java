package com.smartclassroom.util;

import com.smartclassroom.model.User;

/** Remembers who is logged in while the program runs. Cleared on logout. */
public final class Session {
    private static User currentUser;

    private Session() {
    }

    public static synchronized void start(User user) {
        currentUser = user;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized void end() {
        currentUser = null;
    }
}
