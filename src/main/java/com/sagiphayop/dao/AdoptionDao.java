package com.sagiphayop.dao;

import com.sagiphayop.model.Adoption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdoptionDao {

    /**
     * Creates an adoption record and, in the same transaction, marks the animal ADOPTED under
     * the adopter and closes its open rescue (if any). Returns the new adoption ID, or -1.
     */
    public int createAdoption(int animalId, int adopterOwnerId, String dateAdopted,
                              int approvedBy, String followUpNotes) {
        String insert = "INSERT INTO adoptions (animal_id, adopter_owner_id, date_adopted, approved_by, "
                      + "follow_up_notes) VALUES (?, ?, ?, ?, ?)";
        try {
            return DatabaseConnection.inTransaction(conn -> {
                int adoptionId;
                try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, animalId);
                    ps.setInt(2, adopterOwnerId);
                    ps.setString(3, dateAdopted);
                    ps.setInt(4, approvedBy);
                    ps.setString(5, SqlUtil.blankToNull(followUpNotes));
                    ps.executeUpdate();
                    adoptionId = SqlUtil.generatedKey(ps);
                }
                if (adoptionId < 0) {
                    throw new SQLException("Adoption ID was not generated");
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE animals SET status = 'ADOPTED', owner_id = ? WHERE animal_id = ?")) {
                    ps.setInt(1, adopterOwnerId);
                    ps.setInt(2, animalId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE rescues SET outcome = 'ADOPTED' WHERE animal_id = ? AND outcome = 'IN_SHELTER'")) {
                    ps.setInt(1, animalId);
                    ps.executeUpdate();
                }
                return adoptionId;
            });
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return -1;
        }
    }

    /** Every adoption, newest first. */
    public List<Adoption> getAll() {
        String sql = "SELECT ad.adoption_id, ad.animal_id, a.name AS animal_name, a.species, "
                   + "o.full_name AS adopter_name, ad.date_adopted, u.username AS approved_by, "
                   + "ad.follow_up_notes "
                   + "FROM adoptions ad "
                   + "JOIN animals a ON a.animal_id = ad.animal_id "
                   + "JOIN owners o ON o.owner_id = ad.adopter_owner_id "
                   + "LEFT JOIN users u ON u.id = ad.approved_by "
                   + "ORDER BY ad.date_adopted DESC, ad.adoption_id DESC";
        List<Adoption> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Adoption(rs.getInt("adoption_id"), rs.getInt("animal_id"),
                        SqlUtil.text(rs, "animal_name"), rs.getString("species"),
                        rs.getString("adopter_name"), rs.getString("date_adopted"),
                        SqlUtil.text(rs, "approved_by"), SqlUtil.text(rs, "follow_up_notes")));
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return list;
    }
}
