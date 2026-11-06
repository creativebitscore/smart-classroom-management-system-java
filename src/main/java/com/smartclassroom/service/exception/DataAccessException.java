package com.smartclassroom.service.exception;

import java.sql.SQLException;

/**
 * Wraps a low-level {@link SQLException} so the upper layers never depend on JDBC.
 * The MySQL error code is kept so services can recognise duplicate-key and foreign-key errors.
 */
public class DataAccessException extends AppException {
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MYSQL_ROW_IS_REFERENCED = 1451;

    private final int errorCode;

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
        this.errorCode = cause == null ? 0 : cause.getErrorCode();
    }

    public int getErrorCode() {
        return errorCode;
    }

    /** True when a UNIQUE / PRIMARY KEY rule was broken (e.g. duplicate username). */
    public boolean isDuplicateKey() {
        return errorCode == MYSQL_DUPLICATE_ENTRY;
    }

    /** True when a row cannot be deleted because other rows still reference it. */
    public boolean isForeignKeyViolation() {
        return errorCode == MYSQL_ROW_IS_REFERENCED;
    }
}
