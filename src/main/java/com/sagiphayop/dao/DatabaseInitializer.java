package com.sagiphayop.dao;

import com.sagiphayop.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates every table (if missing) and seeds reference data before the CLI starts. */
public final class DatabaseInitializer {

    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

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
            status      TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
            FOREIGN KEY (user_id)     REFERENCES users(id),
            FOREIGN KEY (barangay_id) REFERENCES barangays(barangay_id)
        )""",
        """
        CREATE TABLE IF NOT EXISTS animals (
            animal_id  INTEGER PRIMARY KEY AUTOINCREMENT,
            owner_id   INTEGER,
            name       TEXT,
            species    TEXT NOT NULL CHECK (species IN ('DOG','CAT','OTHER')),
            breed      TEXT,
            sex        TEXT,
            approx_age TEXT,
            color      TEXT,
            status     TEXT NOT NULL DEFAULT 'OWNED'
                       CHECK (status IN ('STRAY','IN_SHELTER','OWNED','ADOPTED','RELEASED','DECEASED')),
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
            outcome         TEXT NOT NULL DEFAULT 'IN_SHELTER'
                            CHECK (outcome IN ('IN_SHELTER','ADOPTED','RELEASED','DECEASED')),
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
        CREATE TABLE IF NOT EXISTS activity_logs (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id     INTEGER NOT NULL,
            action_type TEXT NOT NULL,
            description TEXT NOT NULL,
            timestamp   DATETIME DEFAULT (datetime('now', 'localtime')),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )""",
        "CREATE INDEX IF NOT EXISTS idx_animals_owner      ON animals(owner_id)",
        "CREATE INDEX IF NOT EXISTS idx_animals_status     ON animals(status)",
        "CREATE INDEX IF NOT EXISTS idx_vacc_animal        ON vaccinations(animal_id)",
        "CREATE INDEX IF NOT EXISTS idx_rescues_outcome    ON rescues(outcome)",
        "CREATE INDEX IF NOT EXISTS idx_logs_user          ON activity_logs(user_id)"
    };

    /** Sample barangays (Minglanilla, Cebu). Staff can add more from the menu at any time. */
    private static final String[] BARANGAYS = {
        "Cadulawan", "Calajo-an", "Camp 7", "Cuanos", "Guindaruhan", "Linao", "Manduang",
        "Pakigne", "Poblacion Ward I", "Poblacion Ward II", "Poblacion Ward III",
        "Poblacion Ward IV", "Tubod", "Tulay", "Tunghaan", "Tungkil", "Tungkop", "Vito"
    };

    private DatabaseInitializer() { }

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            for (String ddl : TABLES) {
                st.execute(ddl);
            }
            seedBarangays(conn);
            boolean created = seedDefaultAdmin(conn);
            System.out.println("[DB] Database ready (foreign keys ON).");
            if (created) {
                System.out.println("[DB] First run: default admin created -> username '" + DEFAULT_ADMIN_USER
                        + "', password '" + DEFAULT_ADMIN_PASSWORD + "'. Please change it after logging in.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Initialization failed: " + e.getMessage());
            System.exit(1);   // guardrail: never start the menu without a valid database
        }
    }

    private static void seedBarangays(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO barangays (barangay_name) VALUES (?)")) {
            for (String name : BARANGAYS) {
                ps.setString(1, name);
                ps.executeUpdate();
            }
        }
    }

    /** Ensures one APPROVED admin exists so the first registrations can be approved. */
    private static boolean seedDefaultAdmin(Connection conn) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM users WHERE roles = 'ADMIN' LIMIT 1");
             ResultSet rs = check.executeQuery()) {
            if (rs.next()) {
                return false;
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, roles, status) VALUES (?, ?, 'ADMIN', 'APPROVED')")) {
            ps.setString(1, DEFAULT_ADMIN_USER);
            ps.setString(2, PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD));
            ps.executeUpdate();
            return true;
        }
    }
}
