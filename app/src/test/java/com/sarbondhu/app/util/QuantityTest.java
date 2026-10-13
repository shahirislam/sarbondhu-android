package com.sarbondhu.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QuantityTest {

    @Test
    public void roundsToTwoDecimals() {
        assertEquals(0.3, Quantity.round(0.1 + 0.2), 0.0);
        assertEquals(12.35, Quantity.round(12.345), 0.0);
    }

    @Test
    public void exceedsIgnoresFloatingPointNoise() {
        assertFalse(Quantity.exceeds(0.1 + 0.2, 0.3));
        assertTrue(Quantity.exceeds(0.31, 0.3));
    }
}
