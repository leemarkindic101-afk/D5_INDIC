package com.sagiphayop.ui;

import com.sagiphayop.model.User;

import java.util.Scanner;

/** Placeholder menu for Pet Owners; features arrive in later deliverables. */
public class PetOwnerMenu {
    private final Scanner scanner;
    private final User currentUser;

    public PetOwnerMenu(Scanner scanner, User currentUser) {
        this.scanner = scanner;
        this.currentUser = currentUser;
    }

    public void show() {
        while (true) {
            System.out.println("\n=== PET OWNER MENU (" + currentUser.username() + ") ===");
            System.out.println("(My pets and vaccination history coming soon)");
            System.out.println("0. Logout");
            System.out.print("Choose: ");
            if (scanner.nextLine().trim().equals("0")) {
                System.out.println("Logged out.");
                return;
            }
            System.out.println("Invalid choice.");
        }
    }
}
