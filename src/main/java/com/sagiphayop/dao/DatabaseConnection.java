package com.sagiphayop.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    /** SQLite file, created in the working directory (the project root when run from IntelliJ). */
    private static final String URL = "jdbc:sqlite:sagiphayop.db";

    private DatabaseConnection() { }

    /** A unit of work that runs inside one transaction. */
    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection conn) throws SQLException;
    }

    /** Opens a connection with SQLite foreign key enforcement switched on. */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /** Runs the work atomically: commits on success, rolls everything back if anything fails. */
    public static <T> T inTransaction(TransactionWork<T> work) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
