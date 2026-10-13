package com.sarbondhu.app.data.model;

/** One line of the in-memory activity log. */
public class ActivityEntry {

    private final String message;
    private final long timestamp;

    public ActivityEntry(String message, long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
