package com.sagiphayop.dao;

import com.sagiphayop.model.Barangay;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BarangayDao {

    public List<Barangay> getAll() {
        List<Barangay> list = new ArrayList<>();
        String sql = "SELECT barangay_id, barangay_name FROM barangays ORDER BY barangay_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Barangay(rs.getInt("barangay_id"), rs.getString("barangay_name")));
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return list;
    }

    /** Returns the barangay with this name, creating it if it does not exist yet. Null on failure. */
    public Barangay findOrCreate(String name) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT OR IGNORE INTO barangays (barangay_name) VALUES (?)")) {
                ins.setString(1, name);
                ins.executeUpdate();
            }
            try (PreparedStatement sel = conn.prepareStatement(
                    "SELECT barangay_id, barangay_name FROM barangays WHERE barangay_name = ? COLLATE NOCASE")) {
                sel.setString(1, name);
                try (ResultSet rs = sel.executeQuery()) {
                    return rs.next() ? new Barangay(rs.getInt("barangay_id"), rs.getString("barangay_name")) : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return null;
        }
    }
}
