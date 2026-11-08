package com.sagiphayop.dao;

import com.sagiphayop.model.Animal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AnimalDao {

    /** Inserts an animal and returns its new ID, or -1 on failure. */
    public int addAnimal(String name, String species, String status) {
        String sql = "INSERT INTO animals (name, species, status) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, species.toUpperCase());
            ps.setString(3, status.toUpperCase());
            if (ps.executeUpdate() != 1) {
                return -1;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return -1;
        }
    }

    public List<Animal> getAllAnimals() {
        List<Animal> animals = new ArrayList<>();
        String sql = "SELECT animal_id, name, species, status FROM animals ORDER BY animal_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                animals.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return animals;
    }

    public Optional<Animal> findById(int animalId) {
        String sql = "SELECT animal_id, name, species, status FROM animals WHERE animal_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        String name = rs.getString("name");
        return new Animal(rs.getInt("animal_id"), name == null ? "" : name,
                rs.getString("species"), rs.getString("status"));
    }
}
