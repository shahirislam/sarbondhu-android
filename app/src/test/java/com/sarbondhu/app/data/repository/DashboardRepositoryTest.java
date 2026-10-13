package com.sarbondhu.app.data.repository;

import static org.junit.Assert.assertEquals;

import com.sarbondhu.app.data.model.DashboardData;
import com.sarbondhu.app.data.store.MockDataStore;

import org.junit.Test;

import java.io.IOException;

public class DashboardRepositoryTest {

    @Test
    public void summarisesStore() throws Exception {
        MockDataStore store = TestStores.store();
        new StockRepository(store).adjustStock(10, 1, 10);

        DashboardData dashboard = new DashboardRepository(store).getDashboard();

        assertEquals(2, dashboard.getFertilizerCount());
        assertEquals(1, dashboard.getActiveDealerCount());
        assertEquals(550.0, dashboard.getTotalStock(), 0.001);
        assertEquals(1, dashboard.getLowStock().size());
        assertEquals(1, dashboard.getRecentActivity().size());
    }

    @Test
    public void activityLogIsNewestFirstAndCapped() throws IOException {
        MockDataStore store = TestStores.store();
        for (int i = 0; i < ActivityLog.MAX_ENTRIES + 5; i++) {
            ActivityLog.add(store, "কাজ " + i);
        }

        assertEquals(ActivityLog.MAX_ENTRIES, store.activities().size());
        assertEquals("কাজ " + (ActivityLog.MAX_ENTRIES + 4), store.activities().get(0).getMessage());
    }
}
