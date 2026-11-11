package com.sagiphayop.util;

import java.util.ArrayList;
import java.util.List;

/** Prints auto-sized ASCII tables so every list in the app looks the same. */
public final class TablePrinter {
    private static final int MAX_CELL = 28;

    private TablePrinter() { }

    public static void print(List<String> headers, List<List<String>> rows) {
        List<List<String>> cells = new ArrayList<>();
        int[] widths = new int[headers.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = headers.get(i).length();
        }
        for (List<String> row : rows) {
            List<String> clean = new ArrayList<>();
            for (int i = 0; i < widths.length; i++) {
                String cell = format(i < row.size() ? row.get(i) : null);
                clean.add(cell);
                widths[i] = Math.max(widths[i], cell.length());
            }
            cells.add(clean);
        }
        StringBuilder border = new StringBuilder("+");
        for (int w : widths) {
            border.append("-".repeat(w + 2)).append('+');
        }
        System.out.println(border);
        printRow(headers, widths);
        System.out.println(border);
        for (List<String> row : cells) {
            printRow(row, widths);
        }
        System.out.println(border);
        System.out.println("Total: " + rows.size() + " record(s)");
    }

    private static void printRow(List<String> row, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            sb.append(' ').append(String.format("%-" + widths[i] + "s", row.get(i))).append(" |");
        }
        System.out.println(sb);
    }

    /** Blank/null becomes "-"; very long text is shortened with "...". */
    private static String format(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String v = value.replace('\n', ' ').trim();
        return v.length() > MAX_CELL ? v.substring(0, MAX_CELL - 3) + "..." : v;
    }
}
