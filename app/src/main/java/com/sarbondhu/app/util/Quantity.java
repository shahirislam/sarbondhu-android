package com.sarbondhu.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Stock and sale quantities are kept to 2 decimals so 0.1 + 0.2 really is 0.3. */
public final class Quantity {

    private Quantity() {
    }

    /** Non-finite values are returned unchanged so callers can reject them. */
    public static double round(double value) {
        if (!Double.isFinite(value)) {
            return value;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** True when {@code wanted} is more than {@code available}, ignoring floating-point noise. */
    public static boolean exceeds(double wanted, double available) {
        return round(wanted) > round(available);
    }
}
