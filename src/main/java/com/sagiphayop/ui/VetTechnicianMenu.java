package com.sagiphayop.ui;

import com.sagiphayop.model.User;

import java.util.Scanner;

/** Placeholder menu for Vet Technicians; features arrive in later deliverables. */
public class VetTechnicianMenu {
    private final Scanner scanner;
    private final User currentUser;

    public VetTechnicianMenu(Scanner scanner, User currentUser) {
        this.scanner = scanner;
        this.currentUser = currentUser;
    }

    public void show() {
        while (true) {
            System.out.println("\n=== VET TECHNICIAN MENU (" + currentUser.username() + ") ===");
            System.out.println("(Owner, animal, vaccination and rescue features coming soon)");
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
