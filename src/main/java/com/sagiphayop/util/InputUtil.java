package com.sagiphayop.util;

import java.util.Scanner;

/** Shared console input helpers that never throw on bad input. */
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
}
