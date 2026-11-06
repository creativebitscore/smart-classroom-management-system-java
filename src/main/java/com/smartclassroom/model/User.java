package com.smartclassroom.model;

import java.util.List;

/**
 * Abstract base class for every person who can log in.
 * Holds the data common to Admin, Faculty and Student (inheritance) and
 * declares the role-specific behaviour that each subclass must supply (polymorphism).
 */
public abstract class User {
    private int userId;
    private String username;
    private String fullName;
    private String email;
    private final Role role;

    protected User(Role role) {
        this.role = role;
    }

    // ----- role specific behaviour (overridden in subclasses) -----

    /** Title shown at the top of the sidebar / window for this kind of user. */
    public abstract String getDashboardTitle();

    /** Names of the sidebar menu entries this role may open, in display order. */
    public abstract List<String> getMenuItems();

    // ----- getters and setters (encapsulation) -----

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return fullName;
    }
}
