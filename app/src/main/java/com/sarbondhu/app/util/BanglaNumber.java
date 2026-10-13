package com.sarbondhu.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts numbers to Bangla digits for display. */
public final class BanglaNumber {

    private static final char[] DIGITS = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};

    private BanglaNumber() {
    }

    public static String toBangla(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            out.append(c >= '0' && c <= '9' ? DIGITS[c - '0'] : c);
        }
        return out.toString();
    }

    /** Formats with at most two decimals and no trailing zeros, e.g. 500.0 → ৫০০, 27.5 → ২৭.৫. */
    public static String format(double value) {
        return toBangla(plain(value));
    }

    /** Same rounding as {@link #format(double)} but with ASCII digits, for editable number fields. */
    public static String plain(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static String format(int value) {
        return toBangla(String.valueOf(value));
    }
}
