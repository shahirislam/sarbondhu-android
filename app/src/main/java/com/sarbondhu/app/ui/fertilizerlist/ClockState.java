package com.sarbondhu.app.ui.fertilizerlist;

/** Current time, weekday and Bangla date, already formatted for display. */
public class ClockState {

    private final String time;
    private final String day;
    private final String date;
    private final boolean isDay;

    public ClockState(String time, String day, String date, boolean isDay) {
        this.time = time;
        this.day = day;
        this.date = date;
        this.isDay = isDay;
    }

    public String getTime() {
        return time;
    }

    public String getDay() {
        return day;
    }

    public String getDate() {
        return date;
    }

    public boolean isDay() {
        return isDay;
    }
}
