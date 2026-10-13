package com.sarbondhu.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sarbondhu.app.data.model.DealerStockRow;
import com.sarbondhu.app.data.model.LowStockRow;
import com.sarbondhu.app.data.store.MockDataStore;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class StockRepositoryTest {

    private MockDataStore store;
    private StockRepository stock;

    @Before
    public void setUp() throws IOException {
        store = TestStores.store();
        stock = new StockRepository(store);
    }

    @Test
    public void dealerStockListsEveryFertilizerWithDefaults() throws IOException {
        store.stocks().removeIf(s -> s.getShopId() == 10 && s.getFertilizerId() == 2);

        List<DealerStockRow> rows = stock.getDealerStock(10);

        assertEquals(2, rows.size());
        assertEquals(500.0, rows.get(0).getQuantity(), 0.001);
        assertTrue(rows.get(0).hasStock());
        assertEquals(0.0, rows.get(1).getQuantity(), 0.001);
        assertFalse(rows.get(1).hasStock());
        assertEquals(20.0, rows.get(1).getSellingPrice(), 0.001); // fertilizer's fixed price
    }

    @Test
    public void setStockReplacesQuantityAndPrice() throws Exception {
        stock.setStock(10, 1, 120, 30);

        DealerStockRow row = stock.getDealerStock(10).get(0);
        assertEquals(120.0, row.getQuantity(), 0.001);
        assertEquals(30.0, row.getSellingPrice(), 0.001);
        assertEquals(3, store.stocks().size());
        assertTrue(store.activities().get(0).getMessage().contains("ইউরিয়া"));
    }

    @Test
    public void setStockCreatesMissingRow() throws Exception {
        stock.setStock(20, 2, 60, 26);

        assertEquals(4, store.stocks().size());
    }

    @Test
    public void setStockRejectsNegativeQuantity() throws IOException {
        try {
            stock.setStock(10, 1, -1, 30);
            fail("expected DataException");
        } catch (DataException e) {
            assertEquals(DataException.Reason.NEGATIVE_STOCK, e.getReason());
        }
    }

    @Test
    public void adjustAddsAndSubtracts() throws Exception {
        stock.adjustStock(10, 1, 50);
        stock.adjustStock(10, 1, -200);

        assertEquals(350.0, stock.getDealerStock(10).get(0).getQuantity(), 0.001);
    }

    @Test
    public void adjustBelowZeroFailsAndLeavesStockUnchanged() throws IOException {
        try {
            stock.adjustStock(10, 2, -41);
            fail("expected DataException");
        } catch (DataException e) {
            assertEquals(DataException.Reason.NEGATIVE_STOCK, e.getReason());
        }
        assertEquals(40.0, stock.getDealerStock(10).get(1).getQuantity(), 0.001);
    }

    @Test
    public void adjustUnknownDealerIsNotFound() throws IOException {
        try {
            stock.adjustStock(99, 1, 5);
            fail("expected DataException");
        } catch (DataException e) {
            assertEquals(DataException.Reason.NOT_FOUND, e.getReason());
        }
    }

    @Test
    public void lowStockSkipsInactiveDealersAndHealthyStock() throws IOException {
        List<LowStockRow> low = stock.getLowStock();

        assertEquals(1, low.size()); // dealer 10 TSP 40 kg; dealer 20 is inactive, 500 kg is fine
        assertEquals("টিএসপি", low.get(0).getFertilizer().getName());
        assertEquals(40.0, low.get(0).getQuantity(), 0.001);
    }

    @Test
    public void totalStockCountsActiveDealersOnly() throws IOException {
        assertEquals(540.0, stock.getTotalStock(), 0.001);
    }

    @Test
    public void adjustThatOverflowsIsRejected() throws Exception {
        stock.setStock(10, 1, Double.MAX_VALUE, 27);
        try {
            stock.adjustStock(10, 1, Double.MAX_VALUE);
            fail("expected DataException");
        } catch (DataException e) {
            assertEquals(DataException.Reason.INVALID_AMOUNT, e.getReason());
        }
    }
}
