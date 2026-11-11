package com.sagiphayop.ui;

import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Base class for the feature screens: shared context, audit logging and the generic ID picker. */
abstract class Screens {
    protected final AppContext ctx;
    protected final User user;
    protected final Scanner in;

    Screens(AppContext ctx, User user) {
        this.ctx = ctx;
        this.user = user;
        this.in = ctx.scanner();
    }

    protected void log(String actionType, String description) {
        ctx.activityLogDao().logAction(user.id(), actionType, description);
    }

    /** Prints a cancel message and returns null, so callers can write {@code return cancelled();}. */
    protected <T> T cancelled() {
        System.out.println("Cancelled.");
        return null;
    }

    protected static boolean contains(String haystack, String keyword) {
        return haystack != null && haystack.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * Shows {@code pool} with {@code printer}, then asks for an ID until it is valid.
     * Typing S searches the pool (when {@code matcher} is given); typing N runs {@code creator}
     * to add a new record (when given). Returns null if the user cancels or nothing is available.
     */
    protected <T> T pick(String label, List<T> pool, ToIntFunction<T> idOf, Consumer<List<T>> printer,
                         BiPredicate<T, String> matcher, Supplier<T> creator) {
        if (pool.isEmpty()) {
            if (creator == null) {
                System.out.println("No " + label.toLowerCase() + " records available.");
                return null;
            }
            System.out.println("No " + label.toLowerCase() + " records yet - let's add one.");
            return creator.get();
        }
        printer.accept(pool);
        String hint = "0 to cancel" + (matcher != null ? ", S to search" : "")
                + (creator != null ? ", N to add new" : "");
        while (true) {
            String input = InputUtil.readLine(in, "Enter " + label + " ID (" + hint + "): ");
            if (input.equals("0")) {
                return cancelled();
            }
            if (matcher != null && input.equalsIgnoreCase("s")) {
                String keyword = InputUtil.readLine(in, "Search keyword: ");
                List<T> hits = pool.stream().filter(t -> matcher.test(t, keyword)).toList();
                if (hits.isEmpty()) {
                    System.out.println("No matches. Showing everything.");
                    printer.accept(pool);
                } else {
                    printer.accept(hits);
                }
                continue;
            }
            if (creator != null && input.equalsIgnoreCase("n")) {
                T created = creator.get();
                if (created != null) {
                    return created;
                }
                continue;
            }
            try {
                int id = Integer.parseInt(input);
                Optional<T> found = pool.stream().filter(t -> idOf.applyAsInt(t) == id).findFirst();
                if (found.isPresent()) {
                    return found.get();
                }
                System.out.println(label + " ID " + id + " is not in the list. Try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
}
