package com.sagiphayop.dao;

import com.sagiphayop.model.Owner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OwnerDao {

    private static final String BASE =
            "SELECT o.owner_id, o.user_id, o.full_name, o.contact_no, o.address, "
          + "b.barangay_name, o.status "
          + "FROM owners o LEFT JOIN barangays b ON b.barangay_id = o.barangay_id";

    /** Inserts an owner and returns the new ID, or -1 on failure. userId may be null. */
    public int addOwner(Integer userId, String fullName, String contactNo, String address, int barangayId) {
        String sql = "INSERT INTO owners (user_id, full_name, contact_no, address, barangay_id) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, fullName);
            ps.setString(3, contactNo);
            ps.setString(4, address);
            ps.setInt(5, barangayId);
            return ps.executeUpdate() == 1 ? SqlUtil.generatedKey(ps) : -1;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return -1;
        }
    }

    /** Filters are optional (null/blank = ignore). Name and barangay match partial text. */
    public List<Owner> search(String name, String barangay, String status) {
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (SqlUtil.has(name)) {
            where.add("o.full_name LIKE ?");
            params.add("%" + name.trim() + "%");
        }
        if (SqlUtil.has(barangay)) {
            where.add("b.barangay_name LIKE ?");
            params.add("%" + barangay.trim() + "%");
        }
        if (SqlUtil.has(status)) {
            where.add("o.status = ?");
            params.add(status.trim().toUpperCase());
        }
        return query(where, params);
    }

    public List<Owner> getAll() {
        return query(List.of(), List.of());
    }

    public Optional<Owner> findById(int ownerId) {
        return first(query(List.of("o.owner_id = ?"), List.of(ownerId)));
    }

    public Optional<Owner> findByUserId(int userId) {
        return first(query(List.of("o.user_id = ?"), List.of(userId)));
    }

    /** Links an owner record to a login account. Fails if the owner is already linked. */
    public boolean linkUser(int ownerId, int userId) {
        String sql = "UPDATE owners SET user_id = ? WHERE owner_id = ? AND user_id IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, ownerId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    private List<Owner> query(List<String> where, List<Object> params) {
        String sql = BASE + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                   + " ORDER BY o.full_name COLLATE NOCASE, o.owner_id";
        List<Owner> owners = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    owners.add(new Owner(rs.getInt("owner_id"), SqlUtil.nullableInt(rs, "user_id"),
                            rs.getString("full_name"), SqlUtil.text(rs, "contact_no"),
                            SqlUtil.text(rs, "address"), SqlUtil.text(rs, "barangay_name"),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return owners;
    }

    private static <T> Optional<T> first(List<T> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
