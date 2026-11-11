package com.sagiphayop.model;

/** A stray rescue. Outcomes: IN_SHELTER, ADOPTED, RELEASED, DECEASED. */
public record Rescue(int id, int animalId, String animalName, String species, String barangayName,
                     String dateRescued, String locationFound, String conditionNotes,
                     String outcome, String handledBy) {

    public String animalLabel() {
        return "#" + animalId + " " + Animal.label(animalName, species);
    }
}
