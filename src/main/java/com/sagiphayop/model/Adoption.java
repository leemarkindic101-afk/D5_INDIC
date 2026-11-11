package com.sagiphayop.model;

public record Adoption(int id, int animalId, String animalName, String species, String adopterName,
                       String dateAdopted, String approvedBy, String followUpNotes) {

    public String animalLabel() {
        return "#" + animalId + " " + Animal.label(animalName, species);
    }
}
