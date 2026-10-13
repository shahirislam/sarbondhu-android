package com.sarbondhu.app.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Bangla date, weekday and time for the home screen. The date uses the same
 * approximation as the Flutter app (year starts on 14 April, months of 30.5 days).
 */
public final class BanglaCalendar {

    private static final String[] MONTHS = {
            "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
            "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"};

    private static final String[] DAYS = {
            "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"};

    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    private BanglaCalendar() {
    }

    /** @param month 1–12 */
    public static String getBanglaDate(int year, int month, int day) {
        int banglaYear = year - 593;
        if (month < 4 || (month == 4 && day < 14)) {
            banglaYear--;
        }

        long dayOfYear = daysBetween(year, 4, 14, year, month, day);
        if (dayOfYear < 0) {
            dayOfYear = daysBetween(year - 1, 4, 14, year, month, day);
        }

        int monthIndex = Math.min((int) Math.floor(dayOfYear / 30.5), 11);
        int banglaDay = (int) Math.floor(dayOfYear % 30.5) + 1;

        return BanglaNumber.format(banglaDay) + " " + MONTHS[monthIndex] + ", "
                + BanglaNumber.format(banglaYear);
    }

    /** @param dayOfWeek a {@link Calendar#SUNDAY}…{@link Calendar#SATURDAY} constant */
    public static String getBanglaDay(int dayOfWeek) {
        return DAYS[dayOfWeek - Calendar.SUNDAY];
    }

    /** Formats like the Flutter app's "h:mm a", e.g. ৪:০৫ PM. */
    public static String getBanglaTime(int hourOfDay, int minute) {
        int hour = hourOfDay % 12 == 0 ? 12 : hourOfDay % 12;
        String text = hour + ":" + (minute < 10 ? "0" : "") + minute
                + (hourOfDay < 12 ? " AM" : " PM");
        return BanglaNumber.toBangla(text);
    }

    /** Daytime is 6 AM up to (not including) 6 PM. */
    public static boolean isDay(int hourOfDay) {
        return hourOfDay >= 6 && hourOfDay < 18;
    }

    private static long daysBetween(int y1, int m1, int d1, int y2, int m2, int d2) {
        return (utcMillis(y2, m2, d2) - utcMillis(y1, m1, d1)) / MILLIS_PER_DAY;
    }

    private static long utcMillis(int year, int month, int day) {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day);
        return calendar.getTimeInMillis();
    }
}
