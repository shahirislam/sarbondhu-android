package com.sarbondhu.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NumberInputTest {

    @Test
    public void parsesAsciiDigits() {
        assertEquals(27.5, NumberInput.parse(" 27.5 "), 0.0001);
    }

    @Test
    public void parsesBanglaDigits() {
        assertEquals(500.0, NumberInput.parse("৫০০"), 0.0001);
        assertEquals(12.5, NumberInput.parse("১২.৫"), 0.0001);
    }

    @Test
    public void parsesNegative() {
        assertEquals(-40.0, NumberInput.parse("-৪০"), 0.0001);
    }

    @Test
    public void rejectsEmptyAndGarbage() {
        assertNull(NumberInput.parse(""));
        assertNull(NumberInput.parse(null));
        assertNull(NumberInput.parse("abc"));
        assertNull(NumberInput.parse("NaN"));
        assertNull(NumberInput.parse("Infinity"));
    }

    @Test
    public void rejectsNumbersTooLargeToBeFinite() {
        StringBuilder huge = new StringBuilder("1");
        for (int i = 0; i < 400; i++) {
            huge.append('0');
        }
        assertNull(NumberInput.parse(huge.toString()));
    }
}
