package com.sagiphayop.ui;

import com.sagiphayop.model.Animal;
import com.sagiphayop.model.Owner;
import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;
import com.sagiphayop.util.Lookups;

import java.util.List;

/** CREATE feature 2 (register pet / stray) and READ feature 1 (search animals), plus status updates. */
class AnimalScreens extends Screens {
    private final OwnerScreens ownerScreens;

    AnimalScreens(AppContext ctx, User user, OwnerScreens ownerScreens) {
        super(ctx, user);
        this.ownerScreens = ownerScreens;
    }

    /** Staff entry point: asks for every detail including status (and the owner if OWNED). */
    Animal registerAnimal() {
        System.out.println("\n--- Register Animal ---");
        return collect(null, null);
    }

    /** Used by the rescue flow: a newly found stray is always IN_SHELTER and has no owner. */
    Animal registerStray() {
        System.out.println("\n--- Register Rescued Animal ---");
        return collect(null, "IN_SHELTER");
    }

    /** Used by pet owners: the animal belongs to them and is OWNED. */
    Animal registerPetFor(Owner owner) {
        System.out.println("\n--- Register My Pet ---");
        return collect(owner, "OWNED");
    }

    /**
     * Prompts for the animal's details and saves it.
     * fixedOwner / fixedStatus skip those prompts when given. Returns the saved animal or null.
     */
    private Animal collect(Owner fixedOwner, String fixedStatus) {
        System.out.print("Animal name (Enter for unnamed, 0 to cancel): ");
        String name = in.nextLine().trim();
        if (name.equals("0")) {
            return cancelled();
        }
        String species = InputUtil.readChoice(in, "Species " + Lookups.SPECIES + " (0 to cancel): ",
                Lookups.SPECIES, null);
        if (species == null) {
            return cancelled();
        }
        String breed = InputUtil.readOptional(in, "Breed (Enter to skip): ");
        String sex = InputUtil.readChoice(in, "Sex " + Lookups.SEXES + " (Enter = UNKNOWN, 0 to cancel): ",
                Lookups.SEXES, "UNKNOWN");
        if (sex == null) {
            return cancelled();
        }
        String age = InputUtil.readOptional(in, "Approximate age, e.g. '6 months' or '3 years' (Enter to skip): ");
        String color = InputUtil.readOptional(in, "Color / markings (Enter to skip): ");

        String status = fixedStatus;
        if (status == null) {
            status = InputUtil.readChoice(in, "Status " + Lookups.REGISTER_STATUSES + " (0 to cancel): ",
                    Lookups.REGISTER_STATUSES, null);
            if (status == null) {
                return cancelled();
            }
        }
        Owner owner = fixedOwner;
        if (owner == null && status.equals("OWNED")) {
            System.out.println("This animal is OWNED, so choose its owner:");
            owner = ownerScreens.pickOwner(ctx.ownerDao().getAll(), true);
            if (owner == null) {
                return cancelled();
            }
        }
        Animal draft = new Animal(0, owner == null ? null : owner.id(), null, name, species,
                breed, sex, age, color, status);
        int id = ctx.animalDao().addAnimal(draft);
        if (id < 0) {
            System.out.println("Registration failed. Please try again.");
            return null;
        }
        System.out.println("SUCCESS: Animal #" + id + " (" + Animal.label(name, species) + ") registered as " + status
                + (owner == null ? "." : " under " + owner.fullName() + "."));
        log("ANIMAL_REGISTERED", "Registered Animal #" + id + " (" + Animal.label(name, species) + ", "
                + species + ") as " + status + (owner == null ? "" : ", owner " + owner.fullName()));
        return ctx.animalDao().findById(id).orElse(null);
    }

    void searchAnimals() {
        System.out.println("\n--- Search / Filter Animals (press Enter to skip a filter) ---");
        String name = InputUtil.readOptional(in, "Name contains: ");
        String species = InputUtil.readChoice(in, "Species " + Lookups.SPECIES + " (Enter = any): ",
                Lookups.SPECIES, "");
        String barangay = InputUtil.readOptional(in, "Barangay contains (owner's or rescue location): ");
        String status = InputUtil.readChoice(in, "Status " + Lookups.ANIMAL_STATUSES + " (Enter = any): ",
                Lookups.ANIMAL_STATUSES, "");
        List<Animal> animals = ctx.animalDao().search(name, species == null ? "" : species, barangay,
                status == null ? "" : status, null);
        if (animals.isEmpty()) {
            System.out.println("No animals match those filters.");
        } else {
            Tables.animals(animals);
        }
    }

    void updateStatus() {
        System.out.println("\n--- Update Animal Status ---");
        Animal animal = pickAnimal(ctx.animalDao().getAllAnimals());
        if (animal == null) {
            return;
        }
        System.out.println("Animal #" + animal.id() + " (" + animal.label() + ") is currently " + animal.status() + ".");
        System.out.println("(To mark an animal ADOPTED, record an adoption under Rescues & Adoptions.)");
        String newStatus = InputUtil.readChoice(in, "New status " + Lookups.MANUAL_STATUSES + " (0 to cancel): ",
                Lookups.MANUAL_STATUSES, null);
        if (newStatus == null) {
            cancelled();
            return;
        }
        if (newStatus.equals(animal.status())) {
            System.out.println("Animal #" + animal.id() + " is already " + newStatus + ".");
            return;
        }
        if (ctx.animalDao().updateAnimalStatus(animal.id(), newStatus)) {
            System.out.println("SUCCESS: Animal #" + animal.id() + " status changed to " + newStatus + ".");
            log("ANIMAL_STATUS_UPDATED",
                    "Updated Animal #" + animal.id() + " status from " + animal.status() + " to " + newStatus);
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    /** Picks one animal out of {@code pool} by ID (S to search). Null if cancelled or pool is empty. */
    Animal pickAnimal(List<Animal> pool) {
        return pick("Animal", pool, Animal::id, Tables::animals,
                (a, kw) -> contains(a.name(), kw) || contains(a.species(), kw) || contains(a.breed(), kw)
                        || contains(a.ownerName(), kw) || contains(a.status(), kw),
                null);
    }
}
