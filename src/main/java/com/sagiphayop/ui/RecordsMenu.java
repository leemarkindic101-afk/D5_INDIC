package com.sagiphayop.ui;

import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;

import java.util.List;

/**
 * Operational console shared by Vet Technicians (as their main menu) and Admins (as a sub-menu):
 * owners, animals, vaccinations, rescues and adoptions. No SQL here.
 */
public class RecordsMenu {
    private final AppContext ctx;
    private final String title;
    private final String exitLabel;

    private final OwnerScreens owners;
    private final AnimalScreens animals;
    private final VaccinationScreens vaccinations;
    private final RescueScreens rescues;

    public RecordsMenu(AppContext ctx, User user, String title, String exitLabel) {
        this.ctx = ctx;
        this.title = title;
        this.exitLabel = exitLabel;
        this.owners = new OwnerScreens(ctx, user);
        this.animals = new AnimalScreens(ctx, user, owners);
        this.vaccinations = new VaccinationScreens(ctx, user, animals);
        this.rescues = new RescueScreens(ctx, user, owners, animals);
    }

    public void show() {
        run(title, exitLabel,
                List.of("Pet Owners", "Animals", "Vaccinations", "Rescues & Adoptions"),
                List.of(this::ownersMenu, this::animalsMenu, this::vaccinationsMenu, this::rescuesMenu));
    }

    private void ownersMenu() {
        run("PET OWNERS", "Back",
                List.of("Register New Pet Owner", "Search / Filter Owners"),
                List.of(() -> owners.registerOwner(null), owners::searchOwners));
    }

    private void animalsMenu() {
        run("ANIMALS", "Back",
                List.of("Register Pet / Stray Animal", "Search / Filter Animals", "Update Animal Status"),
                List.of(animals::registerAnimal, animals::searchAnimals, animals::updateStatus));
    }

    private void vaccinationsMenu() {
        run("VACCINATIONS", "Back",
                List.of("Record Rabies Vaccination Dose", "View Animal Vaccination History",
                        "View Overdue Boosters (all animals)"),
                List.of(vaccinations::recordDose, vaccinations::viewHistory, vaccinations::viewOverdue));
    }

    private void rescuesMenu() {
        run("RESCUES & ADOPTIONS", "Back",
                List.of("Log New Stray Rescue", "Record Adoption", "Update Rescue Outcome (Released / Deceased)",
                        "View Rescue Records by Status", "View Adoption List"),
                List.of(rescues::logRescue, rescues::recordAdoption, rescues::closeRescue,
                        rescues::viewRescues, rescues::viewAdoptions));
    }

    /** Generic numbered menu: options 1..n run the matching action; 0 returns. */
    private void run(String heading, String zeroLabel, List<String> labels, List<Runnable> actions) {
        while (true) {
            System.out.println("\n=== " + heading + " ===");
            for (int i = 0; i < labels.size(); i++) {
                System.out.println((i + 1) + ". " + labels.get(i));
            }
            System.out.println("0. " + zeroLabel);
            int choice = InputUtil.readInt(ctx.scanner(), "Choose: ");
            if (choice == 0) {
                return;
            }
            if (choice < 1 || choice > actions.size()) {
                System.out.println("Invalid choice. Please select 0-" + actions.size() + ".");
                continue;
            }
            actions.get(choice - 1).run();
        }
    }
}
