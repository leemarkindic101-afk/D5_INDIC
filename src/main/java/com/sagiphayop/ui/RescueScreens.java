package com.sagiphayop.ui;

import com.sagiphayop.model.Adoption;
import com.sagiphayop.model.Animal;
import com.sagiphayop.model.Barangay;
import com.sagiphayop.model.Owner;
import com.sagiphayop.model.Rescue;
import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;
import com.sagiphayop.util.Lookups;

import java.time.LocalDate;
import java.util.List;

/** CREATE feature 4 (log rescue, record adoption) and READ feature 3 (rescue records, adoptions). */
class RescueScreens extends Screens {
    private final OwnerScreens ownerScreens;
    private final AnimalScreens animalScreens;

    RescueScreens(AppContext ctx, User user, OwnerScreens ownerScreens, AnimalScreens animalScreens) {
        super(ctx, user);
        this.ownerScreens = ownerScreens;
        this.animalScreens = animalScreens;
    }

    // ---------- CREATE: log a rescue ----------

    void logRescue() {
        System.out.println("\n--- Log New Stray Rescue ---");
        System.out.println("1. A newly found animal (register it now)");
        System.out.println("2. An animal already in the system");
        System.out.println("0. Cancel");
        int choice = InputUtil.readInt(in, "Choose: ");
        Animal existing = null;
        if (choice == 0) {
            cancelled();
            return;
        } else if (choice == 2) {
            List<Animal> pool = ctx.animalDao().getAllAnimals().stream()
                    .filter(a -> a.status().equals("STRAY") || a.status().equals("RELEASED")).toList();
            if (pool.isEmpty()) {
                System.out.println("No STRAY or RELEASED animals on file. Choose option 1 to register a new one.");
                return;
            }
            existing = animalScreens.pickAnimal(pool);
            if (existing == null) {
                return;
            }
        } else if (choice != 1) {
            System.out.println("Invalid choice.");
            return;
        }

        System.out.println("Where was the animal found?");
        Barangay barangay = ownerScreens.pickBarangay();
        if (barangay == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate rescued = readRescueDate(today);
        if (rescued == null) {
            cancelled();
            return;
        }
        String location = InputUtil.readOptional(in, "Exact location / landmark (Enter to skip): ");
        String condition = InputUtil.readOptional(in, "Condition notes, e.g. injured, thin, aggressive (Enter to skip): ");

        Animal animal = existing != null ? existing : animalScreens.registerStray();
        if (animal == null) {
            System.out.println("Rescue not logged.");
            return;
        }
        int rescueId = ctx.rescueDao().logRescue(animal.id(), barangay.id(), rescued.toString(),
                location, condition, user.id());
        if (rescueId < 0) {
            System.out.println("Could not log the rescue. Please try again.");
            return;
        }
        System.out.println("SUCCESS: Rescue #" + rescueId + " logged. Animal #" + animal.id()
                + " (" + animal.label() + ") is now IN_SHELTER.");
        log("RESCUE_LOGGED", "Logged Rescue #" + rescueId + " of Animal #" + animal.id() + " ("
                + animal.label() + ") found in " + barangay.name() + " on " + rescued);
    }

    private LocalDate readRescueDate(LocalDate today) {
        while (true) {
            LocalDate date = InputUtil.readDate(in, "Date rescued yyyy-MM-dd (Enter = today, 0 to cancel): ", today);
            if (date == null || !date.isAfter(today)) {
                return date;
            }
            System.out.println("The rescue date cannot be in the future.");
        }
    }

    // ---------- CREATE: adoption ----------

    void recordAdoption() {
        System.out.println("\n--- Record Adoption ---");
        List<Animal> inShelter = ctx.animalDao().getByStatus("IN_SHELTER");
        if (inShelter.isEmpty()) {
            System.out.println("No animals are currently IN_SHELTER, so there is nothing to adopt out.");
            return;
        }
        System.out.println("Animals available for adoption:");
        Animal animal = animalScreens.pickAnimal(inShelter);
        if (animal == null) {
            return;
        }
        System.out.println("Who is adopting Animal #" + animal.id() + " (" + animal.label() + ")?");
        Owner adopter = ownerScreens.pickOwner(ctx.ownerDao().getAll(), true);
        if (adopter == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate date;
        while (true) {
            date = InputUtil.readDate(in, "Date adopted yyyy-MM-dd (Enter = today, 0 to cancel): ", today);
            if (date == null || !date.isAfter(today)) {
                break;
            }
            System.out.println("The adoption date cannot be in the future.");
        }
        if (date == null) {
            cancelled();
            return;
        }
        String notes = InputUtil.readOptional(in, "Follow-up notes (Enter to skip): ");
        if (!InputUtil.confirm(in, "Confirm: " + animal.label() + " (#" + animal.id() + ") adopted by "
                + adopter.fullName() + " on " + date + "?")) {
            cancelled();
            return;
        }
        int adoptionId = ctx.adoptionDao().createAdoption(animal.id(), adopter.id(), date.toString(),
                user.id(), notes);
        if (adoptionId < 0) {
            System.out.println("Could not record the adoption. Nothing was changed.");
            return;
        }
        System.out.println("SUCCESS: Adoption #" + adoptionId + " recorded. Animal #" + animal.id()
                + " is now ADOPTED by " + adopter.fullName() + ".");
        log("ADOPTION_RECORDED", "Adoption #" + adoptionId + ": Animal #" + animal.id() + " ("
                + animal.label() + ") adopted by Owner #" + adopter.id() + " (" + adopter.fullName() + ")");
    }

    // ---------- UPDATE: close a rescue without adoption ----------

    void closeRescue() {
        System.out.println("\n--- Update Rescue Outcome ---");
        List<Rescue> open = ctx.rescueDao().getByOutcome("IN_SHELTER");
        Rescue rescue = pick("Rescue", open, Rescue::id, Tables::rescues,
                (r, kw) -> contains(r.animalLabel(), kw) || contains(r.barangayName(), kw)
                        || contains(r.locationFound(), kw),
                null);
        if (rescue == null) {
            return;
        }
        String outcome = InputUtil.readChoice(in, "Outcome " + Lookups.CLOSING_OUTCOMES + " (0 to cancel): ",
                Lookups.CLOSING_OUTCOMES, null);
        if (outcome == null) {
            cancelled();
            return;
        }
        if (ctx.rescueDao().closeRescue(rescue.id(), outcome)) {
            System.out.println("SUCCESS: Rescue #" + rescue.id() + " marked " + outcome + ".");
            log("RESCUE_OUTCOME_UPDATED", "Rescue #" + rescue.id() + " (" + rescue.animalLabel()
                    + ") outcome set to " + outcome);
        } else {
            System.out.println("Update failed. The rescue may no longer be open.");
        }
    }

    // ---------- READ ----------

    void viewRescues() {
        System.out.println("\n--- Rescue Records ---");
        System.out.println("1. In Shelter");
        System.out.println("2. Adopted");
        System.out.println("3. Released");
        System.out.println("4. Deceased");
        System.out.println("5. All");
        System.out.println("0. Back");
        int choice = InputUtil.readInt(in, "Choose: ");
        String outcome;
        switch (choice) {
            case 1 -> outcome = "IN_SHELTER";
            case 2 -> outcome = "ADOPTED";
            case 3 -> outcome = "RELEASED";
            case 4 -> outcome = "DECEASED";
            case 5 -> outcome = "";
            case 0 -> {
                return;
            }
            default -> {
                System.out.println("Invalid choice. Please select 0-5.");
                return;
            }
        }
        List<Rescue> rescues = ctx.rescueDao().getByOutcome(outcome);
        if (rescues.isEmpty()) {
            System.out.println("No rescue records" + (outcome.isEmpty() ? "" : " with outcome " + outcome) + ".");
        } else {
            Tables.rescues(rescues);
        }
    }

    void viewAdoptions() {
        System.out.println("\n--- Adoption Records ---");
        List<Adoption> adoptions = ctx.adoptionDao().getAll();
        if (adoptions.isEmpty()) {
            System.out.println("No adoptions recorded yet.");
        } else {
            Tables.adoptions(adoptions);
        }
    }
}
