package com.sagiphayop.ui;

import com.sagiphayop.dao.UserDao;
import com.sagiphayop.model.User;

import java.util.Optional;
import java.util.Scanner;

/** Handles login, registration, and role-based routing. Contains no SQL. */
public class Authentication {
    private final UserDao userDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== SagipHayop: Stray Animal Rescue and Rabies Vaccination System ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> login();
                case "2" -> register();
                case "0" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1, 2, or 0.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- Register New Account ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty() || username.contains(" ")) {
            System.out.println("Username cannot be empty or contain spaces.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("That username is already taken.");
            return;
        }
        System.out.print("Password (min 6 characters): ");
        String password = scanner.nextLine();
        if (password.length() < 6) {
            System.out.println("Password is too short.");
            return;
        }
        String role = chooseRole();
        if (role == null) {
            System.out.println("Registration cancelled.");
            return;
        }
        if (userDao.registerUser(username, password, role)) {
            System.out.println("Registration successful! Your account is PENDING admin approval.");
        } else {
            System.out.println("Registration failed. Please try again.");
        }
    }

    /** Returns the chosen role, or null if cancelled. */
    private String chooseRole() {
        while (true) {
            System.out.println("Select role:");
            System.out.println("1. ADMIN (City Veterinary Office Head)");
            System.out.println("2. VET_TECHNICIAN");
            System.out.println("3. PET_OWNER");
            System.out.println("0. Cancel");
            System.out.print("Choose: ");
            switch (scanner.nextLine().trim()) {
                case "1": return "ADMIN";
                case "2": return "VET_TECHNICIAN";
                case "3": return "PET_OWNER";
                case "0": return null;
                default: System.out.println("Invalid choice. Please enter 1-3, or 0 to cancel.");
            }
        }
    }

    private void login() {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> result = userDao.authenticate(username, password);
        if (result.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }
        User user = result.get();
        if (!"APPROVED".equals(user.status())) {
            System.out.println("Your account is still " + user.status() + ". Please wait for admin approval.");
            return;
        }
        System.out.println("Welcome, " + user.username() + " (" + user.role() + ")!");
        routeToMenu(user);
    }

    private void routeToMenu(User user) {
        switch (user.role()) {
            case "ADMIN" -> new AdminMenu(userDao, scanner, user).show();
            case "VET_TECHNICIAN" -> new VetTechnicianMenu(scanner, user).show();
            case "PET_OWNER" -> new PetOwnerMenu(scanner, user).show();
            default -> System.out.println("Unknown role: " + user.role());
        }
    }
}
