package com.smartclassroom.util;

import com.smartclassroom.service.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves that a database that cannot be reached produces a friendly application error, not a raw SQL crash. */
class DbConnectionTest {
    // port 1 is never a MySQL server, so the connection is refused straight away
    private static final String UNREACHABLE = "jdbc:mysql://localhost:1/smart_classroom?connectTimeout=2000";

    @AfterEach
    void restoreSettings() {
        DbConnection.clearSettingsOverride();
    }

    @Test
    void unreachableDatabaseGivesFriendlyDataAccessException() {
        DbConnection.useSettings(UNREACHABLE, "root", "wrong");
        DataAccessException e = assertThrows(DataAccessException.class, DbConnection::getConnection);
        assertTrue(e.getMessage().contains("Cannot connect to the database"));
    }

    @Test
    void availabilityCheckReturnsFalseInsteadOfThrowing() {
        DbConnection.useSettings(UNREACHABLE, "root", "wrong");
        assertFalse(DbConnection.isDatabaseAvailable());
    }

    @Test
    void transactionOnUnreachableDatabaseAlsoGivesDataAccessException() {
        DbConnection.useSettings(UNREACHABLE, "root", "wrong");
        assertThrows(DataAccessException.class, () -> DbConnection.executeInTransaction(connection -> "never runs"));
    }
}
