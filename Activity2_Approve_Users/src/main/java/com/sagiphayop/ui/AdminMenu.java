package com.sagiphayop.ui;

import com.sagiphayop.dao.UserDao;
import com.sagiphayop.model.User;

import java.util.List;
import java.util.Scanner;

/** Admin console. Uses UserDao for all data access; no SQL here. */
public class AdminMenu {
    private final UserDao userDao;
    private final Scanner scanner;
    private final User currentAdmin;

    public AdminMenu(UserDao userDao, Scanner scanner, User currentAdmin) {
        this.userDao = userDao;
        this.scanner = scanner;
        this.currentAdmin = currentAdmin;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== ADMIN MENU (" + currentAdmin.username() + ") ===");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("0. Logout");
            int choice = readIntInput("Choose: ");
            switch (choice) {
                case 1 -> printUserTable(userDao.getAllUsers());
                case 2 -> viewPending();
                case 3 -> approveUser();
                case 0 -> {
                    System.out.println("Logged out.");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please select 0-3.");
            }
        }
    }

    private void viewPending() {
        List<User> pending = userDao.getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("No pending registrations.");
        } else {
            printUserTable(pending);
        }
    }

    private void approveUser() {
        List<User> pending = userDao.getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }
        printUserTable(pending);
        while (true) {
            int targetId = readIntInput("Enter User ID to approve (0 to cancel): ");
            if (targetId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }
            boolean inPending = pending.stream().anyMatch(u -> u.id() == targetId);
            if (!inPending) {
                System.out.println("User ID " + targetId + " is not in the pending list. Try again.");
                continue;
            }
            if (userDao.approveUser(targetId)) {
                System.out.println("SUCCESS: User ID " + targetId + " has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    // ---------- Input validation wrappers ----------

    /** Re-prompts until the user enters a whole number. Never throws on bad input. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // ---------- Formatted console output ----------

    private void printUserTable(List<User> users) {
        String border = "+-------+-----------------+------------------+------------+";
        System.out.println(border);
        System.out.printf("| %-5s | %-15s | %-16s | %-10s |%n", "ID", "Username", "Role", "Status");
        System.out.println(border);
        for (User u : users) {
            System.out.printf("| %-5d | %-15s | %-16s | %-10s |%n",
                    u.id(), u.username(), u.role(), u.status());
        }
        System.out.println(border);
        System.out.println("Total: " + users.size() + " user(s)");
    }
}
