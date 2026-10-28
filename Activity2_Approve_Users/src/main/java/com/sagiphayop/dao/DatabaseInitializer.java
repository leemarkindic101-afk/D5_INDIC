package com.sagiphayop.dao;

import com.sagiphayop.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates every table from the approved ERD (if missing) before the CLI starts. */
public class DatabaseInitializer {

    private static final String[] TABLES = {
        """
        CREATE TABLE IF NOT EXISTS users (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            username      TEXT NOT NULL UNIQUE,
            password_hash TEXT NOT NULL,
            roles         TEXT NOT NULL CHECK (roles IN ('ADMIN','VET_TECHNICIAN','PET_OWNER')),
            status        TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED'))
        )""",
        """
        CREATE TABLE IF NOT EXISTS barangays (
            barangay_id   INTEGER PRIMARY KEY AUTOINCREMENT,
            barangay_name TEXT NOT NULL UNIQUE
        )""",
        """
        CREATE TABLE IF NOT EXISTS owners (
            owner_id    INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id     INTEGER UNIQUE,
            full_name   TEXT NOT NULL,
            contact_no  TEXT,
            address     TEXT,
            barangay_id INTEGER,
            status      TEXT NOT NULL DEFAULT 'ACTIVE',
            FOREIGN KEY (user_id)     REFERENCES users(id),
            FOREIGN KEY (barangay_id) REFERENCES barangays(barangay_id)
        )""",
        """
        CREATE TABLE IF NOT EXISTS animals (
            animal_id  INTEGER PRIMARY KEY AUTOINCREMENT,
            owner_id   INTEGER,
            name       TEXT,
            species    TEXT NOT NULL,
            breed      TEXT,
            sex        TEXT,
            approx_age TEXT,
            color      TEXT,
            status     TEXT NOT NULL DEFAULT 'OWNED',
            FOREIGN KEY (owner_id) REFERENCES owners(owner_id)
        )""",
        """
        CREATE TABLE IF NOT EXISTS vaccinations (
            vaccination_id  INTEGER PRIMARY KEY AUTOINCREMENT,
            animal_id       INTEGER NOT NULL,
            vaccine_name    TEXT NOT NULL,
            batch_no        TEXT,
            date_given      TEXT NOT NULL,
            next_due_date   TEXT,
            administered_by INTEGER,
            FOREIGN KEY (animal_id)       REFERENCES animals(animal_id),
            FOREIGN KEY (administered_by) REFERENCES users(id)
        )""",
        """
        CREATE TABLE IF NOT EXISTS rescues (
            rescue_id       INTEGER PRIMARY KEY AUTOINCREMENT,
            animal_id       INTEGER NOT NULL,
            barangay_id     INTEGER,
            date_rescued    TEXT NOT NULL,
            location_found  TEXT,
            condition_notes TEXT,
            outcome         TEXT NOT NULL DEFAULT 'IN_SHELTER',
            handled_by      INTEGER,
            FOREIGN KEY (animal_id)   REFERENCES animals(animal_id),
            FOREIGN KEY (barangay_id) REFERENCES barangays(barangay_id),
            FOREIGN KEY (handled_by)  REFERENCES users(id)
        )""",
        """
        CREATE TABLE IF NOT EXISTS adoptions (
            adoption_id      INTEGER PRIMARY KEY AUTOINCREMENT,
            animal_id        INTEGER NOT NULL,
            adopter_owner_id INTEGER NOT NULL,
            date_adopted     TEXT NOT NULL,
            approved_by      INTEGER,
            follow_up_notes  TEXT,
            FOREIGN KEY (animal_id)        REFERENCES animals(animal_id),
            FOREIGN KEY (adopter_owner_id) REFERENCES owners(owner_id),
            FOREIGN KEY (approved_by)      REFERENCES users(id)
        )""",
        """
        CREATE TABLE IF NOT EXISTS activity_log (
            log_id        INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id       INTEGER,
            action        TEXT NOT NULL,
            target_table  TEXT,
            record_id     INTEGER,
            log_timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )"""
    };

    private DatabaseInitializer() { }

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            for (String ddl : TABLES) {
                st.execute(ddl);
            }
            seedDefaultAdmin(conn);
            System.out.println("[DB] Database initialized (foreign keys ON).");
        } catch (SQLException e) {
            System.err.println("[DB] Initialization failed: " + e.getMessage());
            System.exit(1);   // guardrail: never start the menu without a valid database
        }
    }

    /** Ensures one APPROVED admin exists so the first registrations can be approved. */
    private static void seedDefaultAdmin(Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO users (username, password_hash, roles, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.hash("admin123"));
            ps.setString(3, "ADMIN");
            ps.setString(4, "APPROVED");
            ps.executeUpdate();
        }
    }
}
