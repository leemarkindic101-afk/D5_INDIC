package com.sagiphayop.ui;

import com.sagiphayop.dao.ActivityLogDao;
import com.sagiphayop.dao.UserDao;
import com.sagiphayop.model.ActivityLog;
import com.sagiphayop.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/** Admin console. Uses DAOs for all data access; no SQL here. */
public class AdminMenu {
    private static final List<String> VALID_ROLES = List.of("ADMIN", "VET_TECHNICIAN", "PET_OWNER");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");
    private static final int DESC_WIDTH = 35;

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;
    private final User currentAdmin;

    public AdminMenu(UserDao userDao, ActivityLogDao activityLogDao, Scanner scanner, User currentAdmin) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
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
            System.out.println("4. Update User Role and Status");
            System.out.println("5. View System Audit Logs");
            System.out.println("0. Logout");
            int choice = readIntInput("Choose: ");
            switch (choice) {
                case 1 -> printUserTable(userDao.getAllUsers());
                case 2 -> viewPending();
                case 3 -> approveUser();
                case 4 -> updateUser();
                case 5 -> auditLogMenu();
                case 0 -> {
                    System.out.println("Logged out.");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please select 0-5.");
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
                activityLogDao.logAction(currentAdmin.id(), "USER_APPROVED",
                        "Admin " + currentAdmin.id() + " approved account for User ID " + targetId);
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    private void updateUser() {
        List<User> all = userDao.getAllUsers();
        printUserTable(all);
        while (true) {
            int targetId = readIntInput("Enter User ID to update (0 to cancel): ");
            if (targetId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            if (targetId == currentAdmin.id()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }
            Optional<User> target = all.stream().filter(u -> u.id() == targetId).findFirst();
            if (target.isEmpty()) {
                System.out.println("User ID " + targetId + " does not exist. Try again.");
                continue;
            }
            String newRole = readValidRole();
            if (newRole == null) {
                System.out.println("Update cancelled.");
                return;
            }
            String newStatus = readValidStatus();
            if (newStatus == null) {
                System.out.println("Update cancelled.");
                return;
            }
            User old = target.get();
            if (userDao.updateUserRoleAndStatus(targetId, newRole, newStatus)) {
                System.out.println("SUCCESS: User ID " + targetId + " updated to role "
                        + newRole + " with status " + newStatus + ".");
                activityLogDao.logAction(currentAdmin.id(), "USER_UPDATED",
                        "Admin " + currentAdmin.id() + " changed User " + targetId + " ("
                                + old.username() + ") role " + old.role() + " -> " + newRole
                                + ", status " + old.status() + " -> " + newStatus);
            } else {
                System.out.println("Update failed. Please try again.");
            }
            return;
        }
    }

    // ---------- Audit log interface ----------

    private void auditLogMenu() {
        while (true) {
            System.out.println("\n--- SYSTEM AUDIT LOGS ---");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear All Logs");
            System.out.println("0. Back");
            int choice = readIntInput("Choose: ");
            switch (choice) {
                case 1 -> viewAllLogs();
                case 2 -> filterLogsByUser();
                case 3 -> clearLogs();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid choice. Please select 0-3.");
            }
        }
    }

    private void viewAllLogs() {
        List<ActivityLog> logs = activityLogDao.getAllLogs();
        if (logs.isEmpty()) {
            System.out.println("No audit logs recorded yet.");
        } else {
            printLogTable(logs);
        }
    }

    private void filterLogsByUser() {
        int userId = readIntInput("Enter User ID to filter (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }
        List<ActivityLog> logs = activityLogDao.getLogsByUserId(userId);
        if (logs.isEmpty()) {
            System.out.println("No logs found for User ID " + userId + ".");
        } else {
            printLogTable(logs);
        }
    }

    /** Safe clear: needs the exact word YES; the clearing itself is then written to the new log. */
    private void clearLogs() {
        int total = activityLogDao.getAllLogs().size();
        if (total == 0) {
            System.out.println("There are no logs to clear.");
            return;
        }
        System.out.print("This permanently deletes all " + total
                + " log entries. Type YES to confirm (anything else cancels): ");
        if (!scanner.nextLine().trim().equals("YES")) {
            System.out.println("Clear cancelled. No logs were deleted.");
            return;
        }
        int deleted = activityLogDao.clearAllLogs();
        if (deleted < 0) {
            System.out.println("Could not clear logs. Please try again.");
            return;
        }
        System.out.println("SUCCESS: " + deleted + " log entries cleared.");
        activityLogDao.logAction(currentAdmin.id(), "LOGS_CLEARED",
                "Admin " + currentAdmin.id() + " cleared " + deleted + " log entries");
    }

    private void printLogTable(List<ActivityLog> logs) {
        String rowFormat = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
        String line = "-".repeat(5 + 3 + 8 + 3 + 18 + 3 + DESC_WIDTH + 3 + 20);
        System.out.println(line);
        System.out.printf(rowFormat, "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(line);
        for (ActivityLog log : logs) {
            List<String> parts = wrap(log.description(), DESC_WIDTH);
            System.out.printf(rowFormat, log.id(), log.userId(), log.actionType(),
                    parts.get(0), log.timestamp());
            for (int i = 1; i < parts.size(); i++) {
                System.out.printf(rowFormat, "", "", "", parts.get(i), "");
            }
        }
        System.out.println(line);
        System.out.println("Total: " + logs.size() + " log(s)");
    }

    /** Splits text into lines of at most width characters so long descriptions stay aligned. */
    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
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

    /** Returns a valid role, or null if the admin enters 0 to cancel. */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role " + VALID_ROLES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_ROLES.contains(input)) {
                return input;
            }
            System.out.println("Invalid role. Choose from: " + VALID_ROLES);
        }
    }

    /** Returns a valid status, or null if the admin enters 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status " + VALID_STATUSES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("Invalid status. Choose from: " + VALID_STATUSES);
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
