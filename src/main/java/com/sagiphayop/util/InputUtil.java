package com.sagiphayop.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Console input helpers that never throw on bad input.
 * Convention: where a method says "null on cancel", the user typed 0.
 */
public final class InputUtil {
    private InputUtil() { }

    /** Re-prompts until the user enters a whole number. */
    public static int readInt(Scanner scanner, String prompt) {
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

    /** Returns the trimmed line (possibly empty). */
    public static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Same as {@link #readLine}; named for call sites where blank is a valid answer. */
    public static String readOptional(Scanner scanner, String prompt) {
        return readLine(scanner, prompt);
    }

    /** Re-prompts until non-blank. Null on cancel. */
    public static String readRequired(Scanner scanner, String prompt) {
        while (true) {
            String line = readLine(scanner, prompt);
            if (line.equals("0")) {
                return null;
            }
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("This field is required.");
        }
    }

    /** Re-prompts until the input matches the pattern. Null on cancel. */
    public static String readMatching(Scanner scanner, String prompt, Pattern pattern, String error) {
        while (true) {
            String line = readLine(scanner, prompt);
            if (line.equals("0")) {
                return null;
            }
            if (pattern.matcher(line).matches()) {
                return line;
            }
            System.out.println(error);
        }
    }

    /**
     * Re-prompts until the (case-insensitive) input is in {@code valid}. Returns the upper-case value.
     * Blank input returns {@code blankValue} when it is non-null (otherwise blank is invalid).
     * Null on cancel.
     */
    public static String readChoice(Scanner scanner, String prompt, List<String> valid, String blankValue) {
        while (true) {
            String line = readLine(scanner, prompt).toUpperCase();
            if (line.equals("0")) {
                return null;
            }
            if (line.isEmpty() && blankValue != null) {
                return blankValue;
            }
            if (valid.contains(line)) {
                return line;
            }
            System.out.println("Invalid choice. Choose from: " + valid);
        }
    }

    /** Reads an ISO date (yyyy-MM-dd). Blank returns {@code blankValue} if given. Null on cancel. */
    public static LocalDate readDate(Scanner scanner, String prompt, LocalDate blankValue) {
        while (true) {
            String line = readLine(scanner, prompt);
            if (line.equals("0")) {
                return null;
            }
            if (line.isEmpty() && blankValue != null) {
                return blankValue;
            }
            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use the format yyyy-MM-dd (e.g. 2026-11-11).");
            }
        }
    }

    /** Yes/no question; anything other than y/yes counts as no. */
    public static boolean confirm(Scanner scanner, String prompt) {
        String line = readLine(scanner, prompt + " (y/N): ").toLowerCase();
        return line.equals("y") || line.equals("yes");
    }
}
