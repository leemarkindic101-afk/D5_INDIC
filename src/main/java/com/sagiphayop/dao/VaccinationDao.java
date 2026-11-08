package com.sagiphayop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class VaccinationDao {

    /** Records a rabies (or other) vaccination for an animal. Dates are ISO yyyy-MM-dd. */
    public boolean recordVaccination(int animalId, String vaccineName, String batchNo,
                                     String dateGiven, String nextDueDate, int administeredBy) {
        String sql = "INSERT INTO vaccinations "
                + "(animal_id, vaccine_name, batch_no, date_given, next_due_date, administered_by) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            ps.setString(2, vaccineName);
            ps.setString(3, batchNo);
            ps.setString(4, dateGiven);
            ps.setString(5, nextDueDate);
            ps.setInt(6, administeredBy);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }
}
