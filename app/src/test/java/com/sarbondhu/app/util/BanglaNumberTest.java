package com.sarbondhu.app.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BanglaNumberTest {

    @Test
    public void toBangla_convertsAllAsciiDigits() {
        assertEquals("০১৭১১২৩৪৫৬৭", BanglaNumber.toBangla("01711234567"));
    }

    @Test
    public void toBangla_keepsNonDigitCharacters() {
        assertEquals("১৮-৪৬-০ কেজি", BanglaNumber.toBangla("18-46-0 কেজি"));
    }

    @Test
    public void toBangla_nullBecomesEmpty() {
        assertEquals("", BanglaNumber.toBangla(null));
    }

    @Test
    public void format_wholeDoubleHasNoDecimals() {
        assertEquals("৫০০", BanglaNumber.format(500.0));
    }

    @Test
    public void format_keepsMeaningfulDecimals() {
        assertEquals("২৭.৫", BanglaNumber.format(27.5));
    }

    @Test
    public void format_roundsToTwoDecimals() {
        assertEquals("১২৩৪.৫৭", BanglaNumber.format(1234.567));
    }

    @Test
    public void format_zero() {
        assertEquals("০", BanglaNumber.format(0.0));
    }

    @Test
    public void format_int() {
        assertEquals("৩", BanglaNumber.format(3));
    }

    @Test
    public void plain_isAsciiWithoutTrailingZeros() {
        assertEquals("27", BanglaNumber.plain(27.0));
        assertEquals("27.5", BanglaNumber.plain(27.50));
    }
}
