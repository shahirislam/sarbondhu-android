package com.sarbondhu.app.data.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.MockData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.Stock;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class MockDataStoreTest {

    private static MockData sample() {
        return new MockData(
                Arrays.asList(
                        new Fertilizer(3, "a", "", "", "", "", null, 1, "কেজি"),
                        new Fertilizer(7, "b", "", "", "", "", null, 1, "কেজি")),
                Collections.singletonList(new Shop(4, "s", "o", "p", "d", "u", "a")),
                Collections.singletonList(new Stock(4, 3, 10, 1)));
    }

    @Test
    public void loadsOnlyOnce() throws IOException {
        int[] calls = {0};
        MockDataStore store = new MockDataStore(() -> {
            calls[0]++;
            return sample();
        });

        store.ensureLoaded();
        store.ensureLoaded();

        assertEquals(1, calls[0]);
    }

    @Test
    public void failedLoadIsRetried() throws IOException {
        int[] calls = {0};
        MockDataStore store = new MockDataStore(() -> {
            if (++calls[0] == 1) {
                throw new IOException("boom");
            }
            return sample();
        });

        try {
            store.ensureLoaded();
            fail("expected IOException");
        } catch (IOException expected) {
            // first attempt fails
        }
        store.ensureLoaded();

        assertEquals(2, store.fertilizers().size());
    }

    @Test
    public void listsAreMutableCopies() throws IOException {
        MockDataStore store = new MockDataStore(MockDataStoreTest::sample);
        store.ensureLoaded();

        store.fertilizers().add(new Fertilizer(9, "c", "", "", "", "", null, 1, "কেজি"));
        store.stocks().clear();

        assertEquals(3, store.fertilizers().size());
        assertEquals(0, store.stocks().size());
    }

    @Test
    public void nextIdsAreAboveCurrentMaximum() throws IOException {
        MockDataStore store = new MockDataStore(MockDataStoreTest::sample);
        store.ensureLoaded();

        assertEquals(8, store.nextFertilizerId());
        assertEquals(5, store.nextShopId());
    }

    @Test
    public void activityLogStartsEmpty() throws IOException {
        MockDataStore store = new MockDataStore(MockDataStoreTest::sample);
        store.ensureLoaded();

        assertEquals(0, store.activities().size());
    }
}
