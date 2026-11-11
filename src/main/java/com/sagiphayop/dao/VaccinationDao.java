package com.sagiphayop.dao;

import com.sagiphayop.model.Vaccination;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VaccinationDao {

    private static final String BASE =
            "SELECT v.vaccination_id, v.animal_id, a.name AS animal_name, a.species, v.vaccine_name, "
          + "v.batch_no, v.date_given, v.next_due_date, u.username AS administered_by, "
          + "o.full_name AS owner_name, o.contact_no "
          + "FROM vaccinations v "
          + "JOIN animals a ON a.animal_id = v.animal_id "
          + "LEFT JOIN users u ON u.id = v.administered_by "
          + "LEFT JOIN owners o ON o.owner_id = a.owner_id";

    /** Records a vaccination dose for an animal. Dates are ISO yyyy-MM-dd. */
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

    /** Complete history for one animal, newest dose first. */
    public List<Vaccination> getHistory(int animalId) {
        return query(BASE + " WHERE v.animal_id = ? ORDER BY v.date_given DESC, v.vaccination_id DESC", animalId);
    }

    /**
     * Animals whose most recent dose is past its due date (a newer dose supersedes older ones).
     * Deceased animals are excluded. Most overdue first.
     */
    public List<Vaccination> getOverdueBoosters() {
        String sql = BASE + " WHERE v.next_due_date IS NOT NULL "
                + "AND v.next_due_date < date('now', 'localtime') "
                + "AND a.status <> 'DECEASED' "
                + "AND v.vaccination_id = (SELECT v2.vaccination_id FROM vaccinations v2 "
                + "     WHERE v2.animal_id = v.animal_id "
                + "     ORDER BY v2.date_given DESC, v2.vaccination_id DESC LIMIT 1) "
                + "ORDER BY v.next_due_date, v.animal_id";
        return query(sql, null);
    }

    private List<Vaccination> query(String sql, Integer animalId) {
        List<Vaccination> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (animalId != null) {
                ps.setInt(1, animalId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Vaccination(rs.getInt("vaccination_id"), rs.getInt("animal_id"),
                            SqlUtil.text(rs, "animal_name"), rs.getString("species"),
                            rs.getString("vaccine_name"), SqlUtil.text(rs, "batch_no"),
                            rs.getString("date_given"), SqlUtil.text(rs, "next_due_date"),
                            SqlUtil.text(rs, "administered_by"), SqlUtil.text(rs, "owner_name"),
                            SqlUtil.text(rs, "contact_no")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return list;
    }
}
