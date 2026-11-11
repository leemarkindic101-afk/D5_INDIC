package com.sagiphayop.model;

/**
 * A stray or owned animal.
 * Status values: STRAY, IN_SHELTER, OWNED, ADOPTED, RELEASED, DECEASED.
 * Text columns are never null (blank means "not recorded").
 */
public record Animal(int id, Integer ownerId, String ownerName, String name, String species,
                     String breed, String sex, String approxAge, String color, String status) {

    public static String label(String name, String species) {
        return (name == null || name.isBlank()) ? "unnamed " + species : name;
    }

    public String label() {
        return label(name, species);
    }
}
