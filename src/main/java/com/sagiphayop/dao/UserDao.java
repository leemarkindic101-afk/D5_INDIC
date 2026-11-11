package com.sagiphayop.dao;

import com.sagiphayop.model.User;
import com.sagiphayop.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final String SELECT = "SELECT id, username, roles, status FROM users";

    /** Self-registration: always starts as PENDING. Returns false if the username is taken. */
    public boolean registerUser(String username, String plainPassword, String role) {
        return createUser(username, plainPassword, role, "PENDING") > 0;
    }

    /** Admin-created account: active immediately. Returns the new user ID, or -1 on failure. */
    public int createApprovedUser(String username, String plainPassword, String role) {
        return createUser(username, plainPassword, role, "APPROVED");
    }

    private int createUser(String username, String plainPassword, String role, String status) {
        String sql = "INSERT INTO users (username, password_hash, roles, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(plainPassword));
            ps.setString(3, role.toUpperCase());
            ps.setString(4, status);
            return ps.executeUpdate() == 1 ? SqlUtil.generatedKey(ps) : -1;
        } catch (SQLException e) {
            return -1;   // UNIQUE violation or other failure
        }
    }

    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    public Optional<User> findByUsername(String username) {
        List<User> users = query(SELECT + " WHERE username = ?", username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    /** Returns the user if the credentials match (any status); the caller checks status. */
    public Optional<User> authenticate(String username, String plainPassword) {
        String sql = "SELECT id, username, roles, status, password_hash FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(plainPassword, rs.getString("password_hash"))) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<User> getAllUsers() {
        return query(SELECT + " ORDER BY id", null);
    }

    public List<User> getPendingUsers() {
        return query(SELECT + " WHERE status = ? ORDER BY id", "PENDING");
    }

    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus) {
        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole.toUpperCase());
            ps.setString(2, newStatus.toUpperCase());
            ps.setInt(3, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    private List<User> query(String sql, String param) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return users;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"),
                        rs.getString("roles"), rs.getString("status"));
    }
}
