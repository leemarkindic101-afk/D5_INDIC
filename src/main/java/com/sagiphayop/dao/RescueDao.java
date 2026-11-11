package com.sagiphayop.dao;

import com.sagiphayop.model.Rescue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RescueDao {

    private static final String BASE =
            "SELECT r.rescue_id, r.animal_id, a.name AS animal_name, a.species, b.barangay_name, "
          + "r.date_rescued, r.location_found, r.condition_notes, r.outcome, u.username AS handled_by "
          + "FROM rescues r "
          + "JOIN animals a ON a.animal_id = r.animal_id "
          + "LEFT JOIN barangays b ON b.barangay_id = r.barangay_id "
          + "LEFT JOIN users u ON u.id = r.handled_by";

    /**
     * Logs a rescue (outcome IN_SHELTER) and marks the animal IN_SHELTER in one transaction.
     * Returns the new rescue ID, or -1 on failure.
     */
    public int logRescue(int animalId, int barangayId, String dateRescued, String locationFound,
                         String conditionNotes, int handledBy) {
        String insert = "INSERT INTO rescues (animal_id, barangay_id, date_rescued, location_found, "
                      + "condition_notes, outcome, handled_by) VALUES (?, ?, ?, ?, ?, 'IN_SHELTER', ?)";
        try {
            return DatabaseConnection.inTransaction(conn -> {
                int rescueId;
                try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, animalId);
                    ps.setInt(2, barangayId);
                    ps.setString(3, dateRescued);
                    ps.setString(4, SqlUtil.blankToNull(locationFound));
                    ps.setString(5, SqlUtil.blankToNull(conditionNotes));
                    ps.setInt(6, handledBy);
                    ps.executeUpdate();
                    rescueId = SqlUtil.generatedKey(ps);
                }
                if (rescueId < 0) {
                    throw new SQLException("Rescue ID was not generated");
                }
                setAnimalStatus(conn, animalId, "IN_SHELTER");
                return rescueId;
            });
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Closes an open (IN_SHELTER) rescue as RELEASED or DECEASED and updates the animal's
     * status to match, atomically. Returns false if the rescue was not open.
     */
    public boolean closeRescue(int rescueId, String outcome) {
        try {
            return DatabaseConnection.inTransaction(conn -> {
                int animalId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT animal_id FROM rescues WHERE rescue_id = ? AND outcome = 'IN_SHELTER'")) {
                    ps.setInt(1, rescueId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return false;
                        }
                        animalId = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE rescues SET outcome = ? WHERE rescue_id = ?")) {
                    ps.setString(1, outcome.toUpperCase());
                    ps.setInt(2, rescueId);
                    ps.executeUpdate();
                }
                setAnimalStatus(conn, animalId, outcome.toUpperCase());
                return true;
            });
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Rescue records, newest first. A null/blank outcome returns every rescue. */
    public List<Rescue> getByOutcome(String outcome) {
        boolean filter = SqlUtil.has(outcome);
        String sql = BASE + (filter ? " WHERE r.outcome = ?" : "")
                   + " ORDER BY r.date_rescued DESC, r.rescue_id DESC";
        List<Rescue> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (filter) {
                ps.setString(1, outcome.trim().toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Rescue(rs.getInt("rescue_id"), rs.getInt("animal_id"),
                            SqlUtil.text(rs, "animal_name"), rs.getString("species"),
                            SqlUtil.text(rs, "barangay_name"), rs.getString("date_rescued"),
                            SqlUtil.text(rs, "location_found"), SqlUtil.text(rs, "condition_notes"),
                            rs.getString("outcome"), SqlUtil.text(rs, "handled_by")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return list;
    }

    public Optional<Rescue> findById(int rescueId) {
        return getByOutcome(null).stream().filter(r -> r.id() == rescueId).findFirst();
    }

    private static void setAnimalStatus(Connection conn, int animalId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE animals SET status = ? WHERE animal_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, animalId);
            ps.executeUpdate();
        }
    }
}
