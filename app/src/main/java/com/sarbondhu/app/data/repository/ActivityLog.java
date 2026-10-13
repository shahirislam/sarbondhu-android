package com.sarbondhu.app.data.repository;

import com.sarbondhu.app.data.model.ActivityEntry;
import com.sarbondhu.app.data.store.MockDataStore;

import java.util.List;

/** Writes to the store's in-memory activity log. Caller holds the store lock. */
final class ActivityLog {

    static final int MAX_ENTRIES = 50;

    private ActivityLog() {
    }

    static void add(MockDataStore store, String message) {
        List<ActivityEntry> log = store.activities();
        log.add(0, new ActivityEntry(message, System.currentTimeMillis()));
        while (log.size() > MAX_ENTRIES) {
            log.remove(log.size() - 1);
        }
    }
}
