package com.sarbondhu.app.util;

/** Parses numbers typed in forms; Bangla digits (০-৯) are accepted. */
public final class NumberInput {

    private NumberInput() {
    }

    /** Returns null when the text is not a finite number. */
    public static Double parse(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder ascii = new StringBuilder();
        for (char c : text.trim().toCharArray()) {
            ascii.append(c >= '০' && c <= '৯' ? (char) ('0' + (c - '০')) : c);
        }
        String value = ascii.toString();
        if (!value.matches("-?\\d+(\\.\\d+)?")) {
            return null;
        }
        double parsed = Double.parseDouble(value);
        return Double.isInfinite(parsed) ? null : parsed;
    }
}
