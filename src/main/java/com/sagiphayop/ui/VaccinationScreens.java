package com.sagiphayop.ui;

import com.sagiphayop.model.Animal;
import com.sagiphayop.model.User;
import com.sagiphayop.model.Vaccination;
import com.sagiphayop.util.InputUtil;

import java.time.LocalDate;
import java.util.List;

/** CREATE feature 3 (record a dose) and READ feature 2 (history with overdue flag). */
class VaccinationScreens extends Screens {
    private final AnimalScreens animalScreens;

    VaccinationScreens(AppContext ctx, User user, AnimalScreens animalScreens) {
        super(ctx, user);
        this.animalScreens = animalScreens;
    }

    void recordDose() {
        System.out.println("\n--- Record Vaccination Dose ---");
        List<Animal> living = ctx.animalDao().getAllAnimals().stream()
                .filter(a -> !a.status().equals("DECEASED")).toList();
        Animal animal = animalScreens.pickAnimal(living);
        if (animal == null) {
            return;
        }
        String vaccine = InputUtil.readOptional(in, "Vaccine name (Enter for Anti-Rabies): ");
        if (vaccine.isEmpty()) {
            vaccine = "Anti-Rabies";
        }
        String batch = InputUtil.readOptional(in, "Batch number (Enter for N/A): ");
        if (batch.isEmpty()) {
            batch = "N/A";
        }
        LocalDate today = LocalDate.now();
        LocalDate given = readDateGiven(today);
        if (given == null) {
            cancelled();
            return;
        }
        LocalDate nextDue = readNextDue(given);
        if (nextDue == null) {
            cancelled();
            return;
        }
        if (ctx.vaccinationDao().recordVaccination(animal.id(), vaccine, batch,
                given.toString(), nextDue.toString(), user.id())) {
            System.out.println("SUCCESS: " + vaccine + " dose recorded for Animal #" + animal.id()
                    + " (" + animal.label() + "). Next due: " + nextDue + ".");
            log("VACCINATION_RECORDED", "Vaccinated Animal #" + animal.id() + " (" + animal.label() + ") with "
                    + vaccine + " - Batch " + batch + ", given " + given + ", next due " + nextDue);
        } else {
            System.out.println("Could not record vaccination. Please try again.");
        }
    }

    private LocalDate readDateGiven(LocalDate today) {
        while (true) {
            LocalDate given = InputUtil.readDate(in, "Date given yyyy-MM-dd (Enter = today, 0 to cancel): ", today);
            if (given == null || !given.isAfter(today)) {
                return given;
            }
            System.out.println("The date given cannot be in the future.");
        }
    }

    private LocalDate readNextDue(LocalDate given) {
        LocalDate suggested = given.plusYears(1);
        while (true) {
            LocalDate due = InputUtil.readDate(in,
                    "Next due date yyyy-MM-dd (Enter = " + suggested + ", 0 to cancel): ", suggested);
            if (due == null || due.isAfter(given)) {
                return due;
            }
            System.out.println("The next due date must be after the date given (" + given + ").");
        }
    }

    /** Pick any animal, then show its full history. */
    void viewHistory() {
        System.out.println("\n--- Vaccination History ---");
        Animal animal = animalScreens.pickAnimal(ctx.animalDao().getAllAnimals());
        if (animal != null) {
            showHistory(animal);
        }
    }

    /** Prints one animal's vaccination history, newest first, flagging an overdue booster. */
    void showHistory(Animal animal) {
        List<Vaccination> history = ctx.vaccinationDao().getHistory(animal.id());
        System.out.println("\nAnimal #" + animal.id() + ": " + animal.label() + " (" + animal.species()
                + ", " + animal.status() + ")"
                + (animal.ownerName().isEmpty() ? "" : " - Owner: " + animal.ownerName()));
        if (history.isEmpty()) {
            System.out.println("No vaccination on record. This animal has not been vaccinated yet.");
            return;
        }
        LocalDate today = LocalDate.now();
        Tables.vaccinationHistory(history, today);
        Vaccination latest = history.get(0);
        System.out.println("Next due date: " + (latest.dueDate() == null ? "not set" : latest.dueDate())
                + "  [" + latest.dueStatus(today) + "]");
        if (latest.isOverdue(today)) {
            System.out.println("*** OVERDUE BOOSTER: this animal needs a new rabies vaccination now. ***");
        }
    }

    /** READ feature 2, all animals: every animal whose latest dose is past its due date. */
    void viewOverdue() {
        System.out.println("\n--- Overdue Rabies Boosters ---");
        List<Vaccination> overdue = ctx.vaccinationDao().getOverdueBoosters();
        if (overdue.isEmpty()) {
            System.out.println("No overdue boosters. All vaccinated animals are up to date.");
            return;
        }
        Tables.overdue(overdue, LocalDate.now());
    }
}
