package com.sagiphayop.ui;

import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;
import com.sagiphayop.util.Lookups;

import java.util.Optional;

/** Handles login, registration, and role-based routing. Contains no SQL. */
public class Authentication {
    private final AppContext ctx;

    public Authentication(AppContext ctx) {
        this.ctx = ctx;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== SagipHayop: Stray Animal Rescue and Rabies Vaccination System ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            switch (InputUtil.readLine(ctx.scanner(), "Choose: ")) {
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
        String username = InputUtil.readLine(ctx.scanner(), "Username: ");
        if (!Lookups.USERNAME.matcher(username).matches()) {
            System.out.println(Lookups.USERNAME_RULE);
            return;
        }
        if (ctx.userDao().usernameExists(username)) {
            System.out.println("That username is already taken.");
            return;
        }
        String password = InputUtil.readLine(ctx.scanner(), "Password (min 6 characters): ");
        if (password.length() < 6) {
            System.out.println("Password is too short.");
            return;
        }
        String role = chooseRole();
        if (role == null) {
            System.out.println("Registration cancelled.");
            return;
        }
        if (ctx.userDao().registerUser(username, password, role)) {
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
            switch (InputUtil.readLine(ctx.scanner(), "Choose: ")) {
                case "1": return Lookups.ROLES.get(0);
                case "2": return Lookups.ROLES.get(1);
                case "3": return Lookups.ROLES.get(2);
                case "0": return null;
                default: System.out.println("Invalid choice. Please enter 1-3, or 0 to cancel.");
            }
        }
    }

    private void login() {
        System.out.println("\n--- Login ---");
        String username = InputUtil.readLine(ctx.scanner(), "Username: ");
        System.out.print("Password: ");
        String password = ctx.scanner().nextLine();

        Optional<User> result = ctx.userDao().authenticate(username, password);
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
        ctx.activityLogDao().logAction(user.id(), "LOGIN",
                "User " + user.username() + " (ID: " + user.id() + ") logged in.");
        routeToMenu(user);
        logout(user);
    }

    /** Called when a role menu returns, i.e. the user chose Logout. */
    private void logout(User user) {
        System.out.println("Logged out.");
        ctx.activityLogDao().logAction(user.id(), "LOGOUT",
                "User " + user.username() + " (ID: " + user.id() + ") logged out.");
    }

    private void routeToMenu(User user) {
        switch (user.role()) {
            case "ADMIN" -> new AdminMenu(ctx, user).show();
            case "VET_TECHNICIAN" -> new RecordsMenu(ctx, user,
                    "VET TECHNICIAN MENU (" + user.username() + ")", "Logout").show();
            case "PET_OWNER" -> new PetOwnerMenu(ctx, user).show();
            default -> System.out.println("Unknown role: " + user.role());
        }
    }
}
