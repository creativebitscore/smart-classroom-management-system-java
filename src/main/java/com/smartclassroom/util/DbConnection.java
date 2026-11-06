package com.smartclassroom.util;

import com.smartclassroom.service.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates database connections. Credentials are NEVER written in the source code.
 * Order of lookup (first one found wins for each key):
 *   1. environment variables  DB_URL, DB_USER, DB_PASSWORD
 *   2. a file called db.properties in the folder where the app is started
 * Copy db.properties.example to db.properties and put your own MySQL password there.
 */
public final class DbConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/smart_classroom?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String CONFIG_FILE = "db.properties";

    private static Properties fileProperties;

    // Optional override used by tests (for example to prove that a wrong URL gives a friendly error).
    private static String overrideUrl;
    private static String overrideUser;
    private static String overridePassword;

    private DbConnection() {
    }

    /** A unit of work that runs inside one database transaction. */
    @FunctionalInterface
    public interface TransactionalWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    /** Decides how a transaction is run. The default talks to MySQL; unit tests plug in a fake. */
    public interface TransactionRunner {
        <T> T run(TransactionalWork<T> work);
    }

    private static final TransactionRunner MYSQL_RUNNER = new TransactionRunner() {
        @Override
        public <T> T run(TransactionalWork<T> work) {
            return runInMySqlTransaction(work);
        }
    };
    private static TransactionRunner runner = MYSQL_RUNNER;

    /** Testing hook: run "transactions" without a database. */
    public static synchronized void useTransactionRunner(TransactionRunner testRunner) {
        runner = testRunner;
    }

    /** Testing hook: go back to real MySQL transactions. */
    public static synchronized void resetTransactionRunner() {
        runner = MYSQL_RUNNER;
    }

    /** Testing hook: use these settings instead of db.properties. */
    public static synchronized void useSettings(String url, String user, String password) {
        overrideUrl = url;
        overrideUser = user;
        overridePassword = password;
    }

    /** Testing hook: go back to db.properties / environment variables. */
    public static synchronized void clearSettingsOverride() {
        overrideUrl = null;
        overrideUser = null;
        overridePassword = null;
    }

    /** Opens a new connection. The caller must close it (use try-with-resources). */
    public static Connection getConnection() {
        String url = overrideUrl != null ? overrideUrl : read("DB_URL", "db.url", DEFAULT_URL);
        String user = overrideUser != null ? overrideUser : read("DB_USER", "db.user", DEFAULT_USER);
        String password = overridePassword != null ? overridePassword : read("DB_PASSWORD", "db.password", "");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Cannot connect to the database. Check that MySQL is running and that db.properties "
                            + "contains the correct URL, user and password. (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Runs the work inside a transaction: commit if everything succeeds, rollback if anything fails.
     * Used when several related inserts must succeed or fail together.
     */
    public static <T> T executeInTransaction(TransactionalWork<T> work) {
        TransactionRunner current;
        synchronized (DbConnection.class) {
            current = runner;
        }
        return current.run(work);
    }

    private static <T> T runInMySqlTransaction(TransactionalWork<T> work) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection);
                if (e instanceof SQLException sqlException) {
                    throw new DataAccessException("Database operation failed: " + sqlException.getMessage(), sqlException);
                }
                throw (RuntimeException) e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    /** Quick check used at start-up: true if a connection can be opened. */
    public static boolean isDatabaseAvailable() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (DataAccessException | SQLException e) {
            return false;
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // nothing more we can do; the original error is reported to the caller
        }
    }

    private static String read(String envName, String propertyName, String defaultValue) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return loadFileProperties().getProperty(propertyName, defaultValue);
    }

    private static synchronized Properties loadFileProperties() {
        if (fileProperties == null) {
            fileProperties = new Properties();
            Path path = Path.of(CONFIG_FILE);
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    fileProperties.load(in);
                } catch (IOException e) {
                    System.err.println("Could not read " + CONFIG_FILE + ": " + e.getMessage());
                }
            }
        }
        return fileProperties;
    }
}
