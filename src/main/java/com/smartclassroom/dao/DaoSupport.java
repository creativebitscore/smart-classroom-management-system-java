package com.smartclassroom.dao;

import com.smartclassroom.service.exception.DataAccessException;

import java.sql.SQLException;

/** Small helper shared by all DAOs. */
final class DaoSupport {
    private DaoSupport() {
    }

    /** Converts a JDBC exception into our own exception so upper layers never see java.sql types. */
    static DataAccessException error(SQLException e) {
        return new DataAccessException("Database error: " + e.getMessage(), e);
    }
}
