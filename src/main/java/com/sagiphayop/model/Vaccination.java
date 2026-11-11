package com.sagiphayop.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** One vaccination dose, joined with the animal and its owner for display. */
public record Vaccination(int id, int animalId, String animalName, String species, String vaccineName,
                          String batchNo, String dateGiven, String nextDueDate, String administeredBy,
                          String ownerName, String ownerContact) {

    public static final int DUE_SOON_DAYS = 30;

    /** Parsed next-due date, or null when none was recorded. */
    public LocalDate dueDate() {
        if (nextDueDate == null || nextDueDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(nextDueDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public boolean isOverdue(LocalDate today) {
        LocalDate due = dueDate();
        return due != null && due.isBefore(today);
    }

    /** Booster status of this dose: OVERDUE, DUE SOON, UP TO DATE, or NO DUE DATE. */
    public String dueStatus(LocalDate today) {
        LocalDate due = dueDate();
        if (due == null) {
            return "NO DUE DATE";
        }
        if (due.isBefore(today)) {
            return "OVERDUE (" + ChronoUnit.DAYS.between(due, today) + " days)";
        }
        return due.isAfter(today.plusDays(DUE_SOON_DAYS)) ? "UP TO DATE" : "DUE SOON";
    }
}
