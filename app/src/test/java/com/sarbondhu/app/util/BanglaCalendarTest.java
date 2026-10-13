package com.sarbondhu.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class BanglaCalendarTest {

    @Test
    public void date_matchesFlutterAppForOct13_2026() {
        assertEquals("৩০ আশ্বিন, ১৪৩৩", BanglaCalendar.getBanglaDate(2026, 10, 13));
    }

    @Test
    public void date_banglaNewYear() {
        assertEquals("১ বৈশাখ, ১৪৩৩", BanglaCalendar.getBanglaDate(2026, 4, 14));
    }

    @Test
    public void date_beforeNewYearUsesPreviousBanglaYear() {
        assertEquals("১৯ পৌষ, ১৪৩২", BanglaCalendar.getBanglaDate(2026, 1, 1));
    }

    @Test
    public void date_lastDaysBeforeNewYearStayInChoitro() {
        assertEquals("২৯ চৈত্র, ১৪৩২", BanglaCalendar.getBanglaDate(2026, 4, 13));
    }

    @Test
    public void day_sundayAndSaturday() {
        assertEquals("রবিবার", BanglaCalendar.getBanglaDay(Calendar.SUNDAY));
        assertEquals("মঙ্গলবার", BanglaCalendar.getBanglaDay(Calendar.TUESDAY));
        assertEquals("শনিবার", BanglaCalendar.getBanglaDay(Calendar.SATURDAY));
    }

    @Test
    public void time_afternoonUsesPmAndPadsMinutes() {
        assertEquals("৪:০৫ PM", BanglaCalendar.getBanglaTime(16, 5));
    }

    @Test
    public void time_midnightAndNoon() {
        assertEquals("১২:০০ AM", BanglaCalendar.getBanglaTime(0, 0));
        assertEquals("১২:৩০ PM", BanglaCalendar.getBanglaTime(12, 30));
    }

    @Test
    public void isDay_sixAmToBeforeSixPm() {
        assertFalse(BanglaCalendar.isDay(5));
        assertTrue(BanglaCalendar.isDay(6));
        assertTrue(BanglaCalendar.isDay(17));
        assertFalse(BanglaCalendar.isDay(18));
    }
}
