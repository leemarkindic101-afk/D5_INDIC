package com.sagiphayop.ui;

import com.sagiphayop.dao.ActivityLogDao;
import com.sagiphayop.dao.AnimalDao;
import com.sagiphayop.dao.VaccinationDao;
import com.sagiphayop.model.Animal;
import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/** Vet Technician console: animals, rabies vaccinations, and animal status changes. No SQL here. */
public class VetTechnicianMenu {
    private static final List<String> SPECIES = List.of("DOG", "CAT", "OTHER");
    private static final List<String> STATUSES = List.of("STRAY", "IN_SHELTER", "OWNED", "ADOPTED");

    private final AnimalDao animalDao;
    private final VaccinationDao vaccinationDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;
    private final User currentUser;

    public VetTechnicianMenu(AnimalDao animalDao, VaccinationDao vaccinationDao,
                             ActivityLogDao activityLogDao, Scanner scanner, User currentUser) {
        this.animalDao = animalDao;
        this.vaccinationDao = vaccinationDao;
        this.activityLogDao = activityLogDao;
        this.scanner = scanner;
        this.currentUser = currentUser;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== VET TECHNICIAN MENU (" + currentUser.username() + ") ===");
            System.out.println("1. Register Animal");
            System.out.println("2. View Animals");
            System.out.println("3. Record Rabies Vaccination");
            System.out.println("4. Update Animal Status");
            System.out.println("0. Logout");
            int choice = InputUtil.readInt(scanner, "Choose: ");
            switch (choice) {
                case 1 -> registerAnimal();
                case 2 -> printAnimalTable(animalDao.getAllAnimals());
                case 3 -> recordVaccination();
                case 4 -> updateAnimalStatus();
                case 0 -> {
                    System.out.println("Logged out.");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please select 0-4.");
            }
        }
    }

    private void registerAnimal() {
        System.out.print("Animal name (blank for unnamed, 0 to cancel): ");
        String name = scanner.nextLine().trim();
        if (name.equals("0")) {
            System.out.println("Registration cancelled.");
            return;
        }
        String species = readChoice("Species " + SPECIES, SPECIES);
        if (species == null) {
            System.out.println("Registration cancelled.");
            return;
        }
        String status = readChoice("Status " + STATUSES, STATUSES);
        if (status == null) {
            System.out.println("Registration cancelled.");
            return;
        }
        int newId = animalDao.addAnimal(name, species, status);
        if (newId < 0) {
            System.out.println("Registration failed. Please try again.");
            return;
        }
        System.out.println("SUCCESS: Animal #" + newId + " registered.");
        activityLogDao.logAction(currentUser.id(), "ANIMAL_REGISTERED",
                "Registered Animal #" + newId + " (" + (name.isEmpty() ? "unnamed" : name)
                        + ", " + species + ") as " + status);
    }

    private void recordVaccination() {
        Animal animal = pickAnimal("Enter Animal ID to vaccinate (0 to cancel): ");
        if (animal == null) {
            return;
        }
        System.out.print("Vaccine name (Enter for Anti-Rabies): ");
        String vaccine = scanner.nextLine().trim();
        if (vaccine.isEmpty()) {
            vaccine = "Anti-Rabies";
        }
        System.out.print("Batch number (Enter for N/A): ");
        String batch = scanner.nextLine().trim();
        if (batch.isEmpty()) {
            batch = "N/A";
        }
        LocalDate today = LocalDate.now();
        LocalDate nextDue = today.plusYears(1);
        if (vaccinationDao.recordVaccination(animal.id(), vaccine, batch,
                today.toString(), nextDue.toString(), currentUser.id())) {
            System.out.println("SUCCESS: Vaccination recorded for Animal #" + animal.id()
                    + ". Next due: " + nextDue + ".");
            activityLogDao.logAction(currentUser.id(), "VACCINATION_RECORDED",
                    "Vaccinated Animal #" + animal.id() + " (" + label(animal) + ") with "
                            + vaccine + " - Batch " + batch + ", next due " + nextDue);
        } else {
            System.out.println("Could not record vaccination. Please try again.");
        }
    }

    private void updateAnimalStatus() {
        Animal animal = pickAnimal("Enter Animal ID to update (0 to cancel): ");
        if (animal == null) {
            return;
        }
        String newStatus = readChoice("New status " + STATUSES, STATUSES);
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }
        if (newStatus.equals(animal.status())) {
            System.out.println("Animal #" + animal.id() + " is already " + newStatus + ".");
            return;
        }
        if (animalDao.updateAnimalStatus(animal.id(), newStatus)) {
            System.out.println("SUCCESS: Animal #" + animal.id() + " status changed to " + newStatus + ".");
            activityLogDao.logAction(currentUser.id(), "ANIMAL_STATUS_UPDATED",
                    "Updated Animal #" + animal.id() + " status from " + animal.status() + " to " + newStatus);
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    /** Shows the animals, then asks for an ID until valid. Returns null when cancelled or none exist. */
    private Animal pickAnimal(String prompt) {
        List<Animal> animals = animalDao.getAllAnimals();
        if (animals.isEmpty()) {
            System.out.println("No animals registered yet. Register one first.");
            return null;
        }
        printAnimalTable(animals);
        while (true) {
            int id = InputUtil.readInt(scanner, prompt);
            if (id == 0) {
                System.out.println("Cancelled.");
                return null;
            }
            Optional<Animal> found = animals.stream().filter(a -> a.id() == id).findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            System.out.println("Animal ID " + id + " does not exist. Try again.");
        }
    }

    /** Returns a valid choice from the list, or null if the user enters 0. */
    private String readChoice(String label, List<String> valid) {
        while (true) {
            System.out.print("Enter " + label + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (valid.contains(input)) {
                return input;
            }
            System.out.println("Invalid choice. Choose from: " + valid);
        }
    }

    private String label(Animal a) {
        return a.name().isEmpty() ? "unnamed " + a.species() : a.name();
    }

    private void printAnimalTable(List<Animal> animals) {
        String border = "+-------+-----------------+------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-5s | %-15s | %-10s | %-12s |%n", "ID", "Name", "Species", "Status");
        System.out.println(border);
        for (Animal a : animals) {
            System.out.printf("| %-5d | %-15s | %-10s | %-12s |%n",
                    a.id(), a.name(), a.species(), a.status());
        }
        System.out.println(border);
        System.out.println("Total: " + animals.size() + " animal(s)");
    }
}
