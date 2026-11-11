package com.sagiphayop.dao;

import com.sagiphayop.model.Animal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AnimalDao {

    private static final String BASE =
            "SELECT a.animal_id, a.owner_id, o.full_name AS owner_name, a.name, a.species, a.breed, "
          + "a.sex, a.approx_age, a.color, a.status "
          + "FROM animals a LEFT JOIN owners o ON o.owner_id = a.owner_id";

    /** Inserts an animal (the id field of the argument is ignored). Returns the new ID, or -1. */
    public int addAnimal(Animal a) {
        String sql = "INSERT INTO animals (owner_id, name, species, breed, sex, approx_age, color, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (a.ownerId() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, a.ownerId());
            }
            ps.setString(2, SqlUtil.blankToNull(a.name()));
            ps.setString(3, a.species().toUpperCase());
            ps.setString(4, SqlUtil.blankToNull(a.breed()));
            ps.setString(5, SqlUtil.blankToNull(a.sex()) == null ? null : a.sex().toUpperCase());
            ps.setString(6, SqlUtil.blankToNull(a.approxAge()));
            ps.setString(7, SqlUtil.blankToNull(a.color()));
            ps.setString(8, a.status().toUpperCase());
            return ps.executeUpdate() == 1 ? SqlUtil.generatedKey(ps) : -1;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return -1;
        }
    }

    public List<Animal> getAllAnimals() {
        return search(null, null, null, null, null);
    }

    public List<Animal> getByOwnerId(int ownerId) {
        return search(null, null, null, null, ownerId);
    }

    public List<Animal> getByStatus(String status) {
        return search(null, null, null, status, null);
    }

    /**
     * Searches animals. Every filter is optional (null/blank = ignore).
     * Name matches partial text; species and status match exactly.
     * Barangay matches the owner's barangay OR the barangay where the animal was rescued.
     */
    public List<Animal> search(String name, String species, String barangay, String status, Integer ownerId) {
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (SqlUtil.has(name)) {
            where.add("a.name LIKE ?");
            params.add("%" + name.trim() + "%");
        }
        if (SqlUtil.has(species)) {
            where.add("a.species = ?");
            params.add(species.trim().toUpperCase());
        }
        if (SqlUtil.has(barangay)) {
            where.add("(EXISTS (SELECT 1 FROM barangays b WHERE b.barangay_id = o.barangay_id "
                    + "AND b.barangay_name LIKE ?) "
                    + "OR EXISTS (SELECT 1 FROM rescues r JOIN barangays rb ON rb.barangay_id = r.barangay_id "
                    + "WHERE r.animal_id = a.animal_id AND rb.barangay_name LIKE ?))");
            params.add("%" + barangay.trim() + "%");
            params.add("%" + barangay.trim() + "%");
        }
        if (SqlUtil.has(status)) {
            where.add("a.status = ?");
            params.add(status.trim().toUpperCase());
        }
        if (ownerId != null) {
            where.add("a.owner_id = ?");
            params.add(ownerId);
        }
        String sql = BASE + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                   + " ORDER BY a.animal_id";
        List<Animal> animals = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    animals.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return animals;
    }

    public Optional<Animal> findById(int animalId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE + " WHERE a.animal_id = ?")) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Lifecycle change: updates the animal's status. */
    public boolean updateAnimalStatus(int animalId, String newStatus) {
        String sql = "UPDATE animals SET status = ? WHERE animal_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.toUpperCase());
            ps.setInt(2, animalId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    private Animal map(ResultSet rs) throws SQLException {
        return new Animal(rs.getInt("animal_id"), SqlUtil.nullableInt(rs, "owner_id"),
                SqlUtil.text(rs, "owner_name"), SqlUtil.text(rs, "name"), rs.getString("species"),
                SqlUtil.text(rs, "breed"), SqlUtil.text(rs, "sex"), SqlUtil.text(rs, "approx_age"),
                SqlUtil.text(rs, "color"), rs.getString("status"));
    }
}
