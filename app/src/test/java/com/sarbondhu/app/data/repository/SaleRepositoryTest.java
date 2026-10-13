package com.sarbondhu.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sarbondhu.app.data.model.CartLine;
import com.sarbondhu.app.data.model.DealerDashboard;
import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.data.model.SaleItem;
import com.sarbondhu.app.data.store.MockDataStore;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class SaleRepositoryTest {

    private MockDataStore store;
    private SaleRepository sales;
    private StockRepository stock;

    @Before
    public void setUp() throws IOException {
        store = TestStores.storeWithSales();
        sales = new SaleRepository(store, () -> TestStores.NOW);
        stock = new StockRepository(store);
    }

    @Test
    public void seededRelativeDatesBecomeTimestamps() throws IOException {
        Sale todaySale = sales.getSales(10).get(0); // SB-0002, today 11:30
        assertEquals("SB-0002", todaySale.getInvoiceNo());
        assertEquals(TestStores.at(2026, Calendar.OCTOBER, 13, 11, 30), todaySale.getTimestamp());

        Sale old = sales.getSales(10).get(3);
        assertEquals(TestStores.at(2026, Calendar.SEPTEMBER, 23, 10, 0), old.getTimestamp());
    }

    @Test
    public void seededSalesAreNeverInTheFuture() throws IOException {
        com.sarbondhu.app.data.model.MockData data = new com.sarbondhu.app.data.model.MockData(
                Collections.singletonList(TestStores.fertilizer(1, "ইউরিয়া")),
                Collections.singletonList(TestStores.dealer(10, "dealer10", true)),
                Collections.emptyList());
        data.setSales(Collections.singletonList(
                TestStores.sale(1, 10, 0, "23:59", new SaleItem(1, 1, 27))));
        MockDataStore early = new MockDataStore(() -> data, () -> TestStores.NOW);
        early.ensureLoaded();

        assertTrue(early.sales().get(0).getTimestamp() <= TestStores.NOW);
    }

    @Test
    public void decimalQuantitiesSellDownToExactlyZero() throws Exception {
        stock.setStock(10, 2, 0.3, 25);

        for (int i = 0; i < 3; i++) {
            sales.createSale(10, "", "", Collections.singletonList(new CartLine(2, 0.1)));
        }

        assertEquals(0.0, stock.getDealerStock(10).get(1).getQuantity(), 0.0);
    }

    @Test
    public void recentSeedNeverFallsOnYesterday() throws IOException {
        long justAfterMidnight = TestStores.at(2026, Calendar.OCTOBER, 13, 0, 30);
        com.sarbondhu.app.data.model.MockData data = new com.sarbondhu.app.data.model.MockData(
                Collections.singletonList(TestStores.fertilizer(1, "ইউরিয়া")),
                Collections.singletonList(TestStores.dealer(10, "dealer10", true)),
                Collections.emptyList());
        data.setSales(Collections.singletonList(
                TestStores.recentSale(1, 10, 240, new SaleItem(1, 1, 27))));
        MockDataStore early = new MockDataStore(() -> data, () -> justAfterMidnight);
        early.ensureLoaded();

        long ts = early.sales().get(0).getTimestamp();
        assertTrue(ts >= TestStores.at(2026, Calendar.OCTOBER, 13, 0, 0));
        assertTrue(ts <= justAfterMidnight);
    }

    @Test
    public void newSaleIsListedFirst() throws Exception {
        Sale sale = sales.createSale(10, "", "", Collections.singletonList(new CartLine(1, 1)));

        assertEquals(sale.getId(), sales.getSales(10).get(0).getId());
    }

    @Test
    public void seededItemsGetNameAndUnitSnapshot() throws IOException {
        SaleItem item = sales.getSales(10).get(3).getItems().get(0);
        assertEquals("ইউরিয়া", item.getFertilizerName());
        assertEquals("কেজি", item.getUnit());
        assertEquals(26.0, item.getUnitPrice(), 0.001);
    }

    @Test
    public void salesAreNewestFirstAndOnlyThisDealers() throws IOException {
        List<Sale> list = sales.getSales(10);
        assertEquals(4, list.size());
        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1).getTimestamp() >= list.get(i).getTimestamp());
        }
    }

    @Test
    public void createSaleDeductsStockAndAssignsNextInvoice() throws Exception {
        Sale sale = sales.createSale(10, " রহিম ", "0171",
                Arrays.asList(new CartLine(1, 100), new CartLine(2, 15)));

        assertEquals("SB-0006", sale.getInvoiceNo());
        assertEquals("রহিম", sale.getCustomerName());
        assertEquals(TestStores.NOW, sale.getTimestamp());
        assertEquals(100 * 27.0 + 15 * 25.0, sale.getTotal(), 0.001);
        assertEquals("ইউরিয়া", sale.getItems().get(0).getFertilizerName());
        assertEquals(400.0, stock.getDealerStock(10).get(0).getQuantity(), 0.001);
        assertEquals(25.0, stock.getDealerStock(10).get(1).getQuantity(), 0.001);
        assertEquals(sale.getId(), sales.getSales(10).get(0).getId());
        assertTrue(store.activities().get(0).getMessage().contains("SB-0006"));
    }

    @Test
    public void unitPriceIsCopiedSoLaterPriceChangesDontRewriteHistory() throws Exception {
        Sale sale = sales.createSale(10, "", "", Collections.singletonList(new CartLine(1, 1)));
        stock.setStock(10, 1, 300, 99);

        assertEquals(27.0, sales.getSales(10).get(0).getItems().get(0).getUnitPrice(), 0.001);
        assertEquals(sale.getId(), sales.getSales(10).get(0).getId());
    }

    @Test
    public void insufficientStockChangesNothing() throws IOException {
        int before = sales.getSales(10).size();
        assertReason(DataException.Reason.INSUFFICIENT_STOCK, () -> sales.createSale(10, "", "",
                Arrays.asList(new CartLine(1, 10), new CartLine(2, 41))));

        assertEquals(500.0, stock.getDealerStock(10).get(0).getQuantity(), 0.001);
        assertEquals(before, sales.getSales(10).size());
    }

    @Test
    public void duplicateLinesAreSummedForTheStockCheck() throws IOException {
        assertReason(DataException.Reason.INSUFFICIENT_STOCK, () -> sales.createSale(10, "", "",
                Arrays.asList(new CartLine(2, 30), new CartLine(2, 30))));
        assertEquals(40.0, stock.getDealerStock(10).get(1).getQuantity(), 0.001);
    }

    @Test
    public void emptyCartAndZeroQuantityAreRejected() {
        assertReason(DataException.Reason.EMPTY_CART,
                () -> sales.createSale(10, "", "", Collections.emptyList()));
        assertReason(DataException.Reason.INVALID_AMOUNT,
                () -> sales.createSale(10, "", "", Collections.singletonList(new CartLine(1, 0))));
    }

    @Test
    public void inactiveDealerCannotSell() {
        assertReason(DataException.Reason.DEALER_INACTIVE,
                () -> sales.createSale(20, "", "", Collections.singletonList(new CartLine(1, 1))));
    }

    @Test
    public void fertilizerTheDealerNeverStockedIsInsufficient() throws Exception {
        new FertilizerRepository(store).addFertilizer(TestStores.fertilizer(0, "নতুন"));
        assertReason(DataException.Reason.INSUFFICIENT_STOCK,
                () -> sales.createSale(10, "", "", Collections.singletonList(new CartLine(3, 1))));
    }

    @Test
    public void dashboardCountsTodayAndThisMonth() throws IOException {
        DealerDashboard dashboard = sales.getDealerDashboard(10);

        assertEquals(2, dashboard.getTodayCount());
        assertEquals(10 * 27 + 5 * 27 + 2 * 25, dashboard.getTodayRevenue(), 0.001);
        assertEquals(3, dashboard.getMonthCount());
        assertEquals(10 * 27 + 5 * 27 + 2 * 25 + 4 * 25, dashboard.getMonthRevenue(), 0.001);
        assertEquals(2, dashboard.getStock().size());
        assertEquals(4, dashboard.getRecentSales().size());
    }

    @Test
    public void searchMatchesInvoiceCustomerAndFertilizer() throws IOException {
        Sale sale = sales.getSales(10).get(0); // SB-0002: urea + TSP, customer "ক্রেতা 2"
        assertTrue(SaleRepository.matches(sale, "sb-0002"));
        assertTrue(SaleRepository.matches(sale, "ক্রেতা 2"));
        assertTrue(SaleRepository.matches(sale, "টিএসপি"));
        assertTrue(SaleRepository.matches(sale, "  "));
        assertFalse(SaleRepository.matches(sale, "জিপসাম"));
    }

    private interface Action {
        void run() throws Exception;
    }

    private static void assertReason(DataException.Reason reason, Action action) {
        try {
            action.run();
            fail("expected " + reason);
        } catch (DataException e) {
            assertEquals(reason, e.getReason());
        } catch (Exception e) {
            fail("unexpected " + e);
        }
    }
}
