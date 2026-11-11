package com.sagiphayop.ui;

import com.sagiphayop.model.Animal;
import com.sagiphayop.model.Owner;
import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;

import java.util.List;
import java.util.Optional;

/** Pet Owner console: own profile, own pets, and own pets' vaccination history. */
public class PetOwnerMenu {
    private final AppContext ctx;
    private final User currentUser;
    private final OwnerScreens ownerScreens;
    private final AnimalScreens animalScreens;
    private final VaccinationScreens vaccinationScreens;

    public PetOwnerMenu(AppContext ctx, User currentUser) {
        this.ctx = ctx;
        this.currentUser = currentUser;
        this.ownerScreens = new OwnerScreens(ctx, currentUser);
        this.animalScreens = new AnimalScreens(ctx, currentUser, ownerScreens);
        this.vaccinationScreens = new VaccinationScreens(ctx, currentUser, animalScreens);
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== PET OWNER MENU (" + currentUser.username() + ") ===");
            System.out.println("1. My Owner Profile");
            System.out.println("2. My Pets");
            System.out.println("3. Register My Pet");
            System.out.println("4. My Pet's Vaccination History");
            System.out.println("0. Logout");
            int choice = InputUtil.readInt(ctx.scanner(), "Choose: ");
            switch (choice) {
                case 1 -> profile();
                case 2 -> withProfile(this::myPets);
                case 3 -> withProfile(owner -> animalScreens.registerPetFor(owner));
                case 4 -> withProfile(this::vaccinationHistory);
                case 0 -> running = false;
                default -> System.out.println("Invalid choice. Please select 0-4.");
            }
        }
    }

    private Optional<Owner> myOwnerRecord() {
        return ctx.ownerDao().findByUserId(currentUser.id());
    }

    /** Runs the action if this account has an owner profile; otherwise explains how to create one. */
    private void withProfile(java.util.function.Consumer<Owner> action) {
        Optional<Owner> owner = myOwnerRecord();
        if (owner.isPresent()) {
            action.accept(owner.get());
        } else {
            System.out.println("You have no owner profile yet. Choose option 1 to create it first.");
        }
    }

    private void profile() {
        Optional<Owner> owner = myOwnerRecord();
        if (owner.isPresent()) {
            Tables.owners(List.of(owner.get()));
            return;
        }
        System.out.println("You have no owner profile yet. Let's create it.");
        ownerScreens.registerOwner(currentUser.id());
    }

    private void myPets(Owner owner) {
        List<Animal> pets = ctx.animalDao().getByOwnerId(owner.id());
        if (pets.isEmpty()) {
            System.out.println("You have no registered pets yet. Choose option 3 to add one.");
        } else {
            Tables.animals(pets);
        }
    }

    private void vaccinationHistory(Owner owner) {
        List<Animal> pets = ctx.animalDao().getByOwnerId(owner.id());
        if (pets.isEmpty()) {
            System.out.println("You have no registered pets yet.");
            return;
        }
        Animal pet = animalScreens.pickAnimal(pets);
        if (pet != null) {
            vaccinationScreens.showHistory(pet);
        }
    }
}
