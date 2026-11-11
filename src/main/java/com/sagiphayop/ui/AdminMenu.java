package com.sagiphayop.ui;

import com.sagiphayop.model.ActivityLog;
import com.sagiphayop.model.Owner;
import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;
import com.sagiphayop.util.Lookups;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/** Admin console: accounts, audit logs, and access to all operational records. No SQL here. */
public class AdminMenu {
    private static final int DESC_WIDTH = 35;

    private final AppContext ctx;
    private final Scanner scanner;
    private final User currentAdmin;
    private final OwnerScreens ownerScreens;

    public AdminMenu(AppContext ctx, User currentAdmin) {
        this.ctx = ctx;
        this.scanner = ctx.scanner();
        this.currentAdmin = currentAdmin;
        this.ownerScreens = new OwnerScreens(ctx, currentAdmin);
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== ADMIN MENU (" + currentAdmin.username() + ") ===");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Update User Role and Status");
            System.out.println("5. Create Staff / Pet Owner Account");
            System.out.println("6. Owners, Animals, Vaccinations & Rescues");
            System.out.println("7. View System Audit Logs");
            System.out.println("0. Logout");
            int choice = InputUtil.readInt(scanner, "Choose: ");
            switch (choice) {
                case 1 -> printUserTable(ctx.userDao().getAllUsers());
                case 2 -> viewPending();
                case 3 -> approveUser();
                case 4 -> updateUser();
                case 5 -> createAccount();
                case 6 -> new RecordsMenu(ctx, currentAdmin, "RECORDS MANAGEMENT", "Back").show();
                case 7 -> auditLogMenu();
                case 0 -> running = false;
                default -> System.out.println("Invalid choice. Please select 0-7.");
            }
        }
    }

    // ---------- Account management ----------

    private void viewPending() {
        List<User> pending = ctx.userDao().getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("No pending registrations.");
        } else {
            printUserTable(pending);
        }
    }

    private void approveUser() {
        List<User> pending = ctx.userDao().getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }
        printUserTable(pending);
        while (true) {
            int targetId = InputUtil.readInt(scanner, "Enter User ID to approve (0 to cancel): ");
            if (targetId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }
            boolean inPending = pending.stream().anyMatch(u -> u.id() == targetId);
            if (!inPending) {
                System.out.println("User ID " + targetId + " is not in the pending list. Try again.");
                continue;
            }
            if (ctx.userDao().approveUser(targetId)) {
                System.out.println("SUCCESS: User ID " + targetId + " has been APPROVED.");
                ctx.activityLogDao().logAction(currentAdmin.id(), "USER_APPROVED",
                        "Admin " + currentAdmin.id() + " approved account for User ID " + targetId);
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    private void updateUser() {
        List<User> all = ctx.userDao().getAllUsers();
        printUserTable(all);
        while (true) {
            int targetId = InputUtil.readInt(scanner, "Enter User ID to update (0 to cancel): ");
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
            String newRole = InputUtil.readChoice(scanner,
                    "Enter new role " + Lookups.ROLES + " (0 to cancel): ", Lookups.ROLES, null);
            if (newRole == null) {
                System.out.println("Update cancelled.");
                return;
            }
            String newStatus = InputUtil.readChoice(scanner,
                    "Enter new status " + Lookups.USER_STATUSES + " (0 to cancel): ", Lookups.USER_STATUSES, null);
            if (newStatus == null) {
                System.out.println("Update cancelled.");
                return;
            }
            User old = target.get();
            if (ctx.userDao().updateUserRoleAndStatus(targetId, newRole, newStatus)) {
                System.out.println("SUCCESS: User ID " + targetId + " updated to role "
                        + newRole + " with status " + newStatus + ".");
                ctx.activityLogDao().logAction(currentAdmin.id(), "USER_UPDATED",
                        "Admin " + currentAdmin.id() + " changed User " + targetId + " ("
                                + old.username() + ") role " + old.role() + " -> " + newRole
                                + ", status " + old.status() + " -> " + newStatus);
            } else {
                System.out.println("Update failed. Please try again.");
            }
            return;
        }
    }

    /** CREATE feature 5: the admin creates an account with an assigned role; it is APPROVED at once. */
    private void createAccount() {
        System.out.println("\n--- Create Account ---");
        String username;
        while (true) {
            username = InputUtil.readLine(scanner, "Username (0 to cancel): ");
            if (username.equals("0")) {
                System.out.println("Cancelled.");
                return;
            }
            if (!Lookups.USERNAME.matcher(username).matches()) {
                System.out.println(Lookups.USERNAME_RULE);
            } else if (ctx.userDao().usernameExists(username)) {
                System.out.println("That username is already taken.");
            } else {
                break;
            }
        }
        String password;
        while (true) {
            password = InputUtil.readLine(scanner, "Password (min 6 characters, 0 to cancel): ");
            if (password.equals("0")) {
                System.out.println("Cancelled.");
                return;
            }
            if (password.length() < 6) {
                System.out.println("Password is too short.");
            } else if (!password.equals(InputUtil.readLine(scanner, "Confirm password: "))) {
                System.out.println("Passwords do not match.");
            } else {
                break;
            }
        }
        String role = InputUtil.readChoice(scanner, "Role " + Lookups.ROLES + " (0 to cancel): ",
                Lookups.ROLES, null);
        if (role == null) {
            System.out.println("Cancelled.");
            return;
        }
        int newId = ctx.userDao().createApprovedUser(username, password, role);
        if (newId < 0) {
            System.out.println("Could not create the account. Please try again.");
            return;
        }
        System.out.println("SUCCESS: Account '" + username + "' (User ID " + newId + ") created as "
                + role + " and APPROVED.");
        ctx.activityLogDao().logAction(currentAdmin.id(), "USER_CREATED",
                "Admin " + currentAdmin.id() + " created " + role + " account '" + username
                        + "' (User ID " + newId + ")");
        if (role.equals("PET_OWNER")) {
            offerOwnerLink(newId, username);
        }
    }

    /** A new PET_OWNER login is useless without an owner record, so offer to connect one now. */
    private void offerOwnerLink(int newUserId, String username) {
        if (!InputUtil.confirm(scanner, "Link '" + username + "' to a pet owner record now?")) {
            System.out.println("Skipped. The owner can create their own profile after logging in.");
            return;
        }
        List<Owner> unlinked = ctx.ownerDao().getAll().stream().filter(o -> o.userId() == null).toList();
        System.out.println("Choose an existing owner without a login (N registers a new one linked to this account):");
        Owner owner = ownerScreens.pick("Owner", unlinked, Owner::id, Tables::owners,
                (o, kw) -> o.fullName().toLowerCase().contains(kw.toLowerCase()),
                () -> ownerScreens.registerOwner(newUserId));
        if (owner == null) {
            return;
        }
        if (owner.userId() != null || ctx.ownerDao().linkUser(owner.id(), newUserId)) {
            System.out.println("Owner #" + owner.id() + " (" + owner.fullName() + ") is linked to '" + username + "'.");
            ctx.activityLogDao().logAction(currentAdmin.id(), "OWNER_LINKED",
                    "Linked Owner #" + owner.id() + " (" + owner.fullName() + ") to User ID " + newUserId);
        } else {
            System.out.println("Could not link the owner record.");
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
            int choice = InputUtil.readInt(scanner, "Choose: ");
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
        List<ActivityLog> logs = ctx.activityLogDao().getAllLogs();
        if (logs.isEmpty()) {
            System.out.println("No audit logs recorded yet.");
        } else {
            printLogTable(logs);
        }
    }

    private void filterLogsByUser() {
        int userId = InputUtil.readInt(scanner, "Enter User ID to filter (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }
        List<ActivityLog> logs = ctx.activityLogDao().getLogsByUserId(userId);
        if (logs.isEmpty()) {
            System.out.println("No logs found for User ID " + userId + ".");
        } else {
            printLogTable(logs);
        }
    }

    /** Safe clear: needs the exact word YES; the clearing itself is then written to the new log. */
    private void clearLogs() {
        int total = ctx.activityLogDao().getAllLogs().size();
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
        int deleted = ctx.activityLogDao().clearAllLogs();
        if (deleted < 0) {
            System.out.println("Could not clear logs. Please try again.");
            return;
        }
        System.out.println("SUCCESS: " + deleted + " log entries cleared.");
        ctx.activityLogDao().logAction(currentAdmin.id(), "LOGS_CLEARED",
                "Admin " + currentAdmin.id() + " cleared " + deleted + " log entries");
    }

    private void printLogTable(List<ActivityLog> logs) {
        String rowFormat = "%-5s | %-8s | %-22s | %-35s | %-20s%n";
        String line = "-".repeat(5 + 3 + 8 + 3 + 22 + 3 + DESC_WIDTH + 3 + 20);
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
