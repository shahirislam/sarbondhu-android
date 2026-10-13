package com.sarbondhu.app.data.repository;

import com.sarbondhu.app.data.model.Admin;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.MockData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.store.MockDataStore;

import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.data.model.SaleItem;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

/** Small in-memory dataset shared by the admin-side tests. */
final class TestStores {

    private TestStores() {
    }

    static Fertilizer fertilizer(int id, String name) {
        return new Fertilizer(id, name, "ধরন", "০-০-০", "বিবরণ", "ব্যবহার",
                Collections.singletonList("ধান"), 20.0, "কেজি");
    }

    static Shop dealer(int id, String username, boolean active) {
        return new Shop(id, "দোকান " + id, "মালিক", "0171", "জেলা", "উপজেলা", "ঠিকানা",
                username, "dealer123", active);
    }

    /** 13 Oct 2026, 15:00 local time. */
    static final long NOW = at(2026, Calendar.OCTOBER, 13, 15, 0);

    static long at(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, hour, minute);
        return calendar.getTimeInMillis();
    }

    static Sale sale(int id, int shopId, int daysAgo, String time, SaleItem... items) {
        return new Sale(id, String.format("SB-%04d", id), shopId, "ক্রেতা " + id, "", daysAgo, time,
                Arrays.asList(items));
    }

    /** A sale made {@code minutesAgo} before load time. */
    static Sale recentSale(int id, int shopId, int minutesAgo, SaleItem... items) {
        return Sale.minutesAgo(id, String.format("SB-%04d", id), shopId, "ক্রেতা " + id, "",
                minutesAgo, Arrays.asList(items));
    }

    /** Dealer 10: two sales today, one earlier this month, one last month. Dealer 20: one today. */
    static List<Sale> sampleSales() {
        return Arrays.asList(
                recentSale(1, 10, 360, new SaleItem(1, 10, 27)),      // 09:00
                recentSale(2, 10, 210, new SaleItem(1, 5, 27), new SaleItem(2, 2, 25)), // 11:30
                sale(3, 10, 5, "10:00", new SaleItem(2, 4, 25)),
                sale(4, 10, 20, "10:00", new SaleItem(1, 100, 26)),
                recentSale(5, 20, 300, new SaleItem(1, 1, 27)));
    }

    static MockDataStore storeWithSales() throws IOException {
        MockData data = baseData();
        data.setSales(sampleSales());
        MockDataStore store = new MockDataStore(() -> data, () -> NOW);
        store.ensureLoaded();
        return store;
    }

    /** Fertilizers 1, 2; dealers 10 (active), 20 (inactive); admin "admin". */
    static MockDataStore store() throws IOException {
        MockDataStore store = new MockDataStore(TestStores::baseData);
        store.ensureLoaded();
        return store;
    }

    private static MockData baseData() {
        MockData data = new MockData(
                Arrays.asList(fertilizer(1, "ইউরিয়া"), fertilizer(2, "টিএসপি")),
                Arrays.asList(dealer(10, "dealer10", true), dealer(20, "dealer20", false)),
                Arrays.asList(
                        new Stock(10, 1, 500.0, 27.0),
                        new Stock(10, 2, 40.0, 25.0),
                        new Stock(20, 1, 5.0, 27.0)));
        data.setAdmins(Collections.singletonList(new Admin(1, "admin", "admin123", "অ্যাডমিন")));
        return data;
    }
}
