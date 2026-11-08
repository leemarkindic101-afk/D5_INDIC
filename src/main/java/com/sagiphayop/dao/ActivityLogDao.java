package com.sagiphayop.dao;

import com.sagiphayop.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the activity_logs audit trail lives here. */
public class ActivityLogDao {

    private static final String SELECT_COLUMNS =
            "SELECT id, user_id, action_type, description, timestamp FROM activity_logs";

    public boolean logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Audit log error: " + e.getMessage());
            return false;
        }
    }

    public List<ActivityLog> getAllLogs() {
        return query(SELECT_COLUMNS + " ORDER BY timestamp DESC, id DESC", null);
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        return query(SELECT_COLUMNS + " WHERE user_id = ? ORDER BY timestamp DESC, id DESC", userId);
    }

    /** Deletes every log entry. Returns the number of rows deleted, or -1 on error. */
    public int clearAllLogs() {
        String sql = "DELETE FROM activity_logs";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Audit log error: " + e.getMessage());
            return -1;
        }
    }

    private List<ActivityLog> query(String sql, Integer userId) {
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(rs.getInt("id"), rs.getInt("user_id"),
                            rs.getString("action_type"), rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Audit log error: " + e.getMessage());
        }
        return logs;
    }
}
