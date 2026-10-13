package com.sarbondhu.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.store.MockDataStore;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class FertilizerAdminTest {

    private MockDataStore store;
    private FertilizerRepository repository;

    @Before
    public void setUp() throws IOException {
        store = TestStores.store();
        repository = new FertilizerRepository(store);
    }

    @Test
    public void addAssignsNextIdAndLogsActivity() throws Exception {
        Fertilizer created = repository.addFertilizer(TestStores.fertilizer(0, "ডিএপি"));

        assertEquals(3, created.getId());
        assertEquals(3, repository.getAllFertilizers().size());
        assertTrue(store.activities().get(0).getMessage().contains("ডিএপি"));
    }

    @Test
    public void updateReplacesFertilizer() throws Exception {
        repository.updateFertilizer(TestStores.fertilizer(2, "টিএসপি নতুন"));

        assertEquals("টিএসপি নতুন", repository.getFertilizer(2).getName());
    }

    @Test
    public void updateUnknownFertilizerFails() throws IOException {
        try {
            repository.updateFertilizer(TestStores.fertilizer(99, "x"));
            fail("expected DataException");
        } catch (DataException e) {
            assertEquals(DataException.Reason.NOT_FOUND, e.getReason());
        }
    }

    @Test
    public void deleteRemovesFertilizerAndItsStock() throws Exception {
        repository.deleteFertilizer(1);

        assertNull(repository.getFertilizer(1));
        for (Stock stock : store.stocks()) {
            assertTrue(stock.getFertilizerId() != 1);
        }
        assertEquals(1, store.stocks().size());
    }

    @Test
    public void idOfDeletedNewestFertilizerIsNotReused() throws Exception {
        repository.deleteFertilizer(2);

        Fertilizer created = repository.addFertilizer(TestStores.fertilizer(0, "নতুন"));

        assertEquals(3, created.getId());
    }
}
