package com.sarbondhu.app.data.store;

import android.content.Context;

import com.sarbondhu.app.data.model.ActivityEntry;
import com.sarbondhu.app.data.model.Admin;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.MockData;
import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.data.model.SaleItem;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.model.Weather;
import com.sarbondhu.app.data.source.DataLoader;
import com.sarbondhu.app.data.source.JsonDataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * The app's only data holder: mock_data.json loaded once into mutable in-memory lists.
 * Edits last for the running session; nothing is ever written to disk.
 * Callers synchronise on the store for every read or write.
 */
public class MockDataStore {

    private static volatile MockDataStore instance;

    private final DataLoader loader;
    private final LongSupplier clock;
    private boolean loaded;
    private List<Fertilizer> fertilizers;
    private List<Shop> shops;
    private List<Stock> stocks;
    private List<Admin> admins;
    private List<Sale> sales;
    private int nextSaleId;
    private int nextInvoiceNumber;
    private Weather weather;
    private final List<ActivityEntry> activities = new ArrayList<>();
    private int nextFertilizerId;
    private int nextShopId;

    public MockDataStore(DataLoader loader) {
        this(loader, System::currentTimeMillis);
    }

    /** @param clock "now", used to turn seeded relative sale dates into timestamps */
    public MockDataStore(DataLoader loader, LongSupplier clock) {
        this.loader = loader;
        this.clock = clock;
    }

    public static MockDataStore getInstance(Context context) {
        if (instance == null) {
            synchronized (MockDataStore.class) {
                if (instance == null) {
                    instance = new MockDataStore(
                            new JsonDataSource(context.getApplicationContext().getAssets()));
                }
            }
        }
        return instance;
    }

    public synchronized void ensureLoaded() throws IOException {
        if (loaded) {
            return;
        }
        MockData data = loader.load();
        fertilizers = new ArrayList<>(data.getFertilizers());
        shops = new ArrayList<>(data.getShops());
        stocks = new ArrayList<>(data.getStocks());
        admins = new ArrayList<>(data.getAdmins());
        weather = data.getWeather();
        sales = new ArrayList<>();
        for (Sale sale : data.getSales()) {
            sales.add(resolve(sale));
            nextSaleId = Math.max(nextSaleId, sale.getId());
            nextInvoiceNumber = Math.max(nextInvoiceNumber, invoiceNumber(sale.getInvoiceNo()));
        }
        for (Fertilizer fertilizer : fertilizers) {
            nextFertilizerId = Math.max(nextFertilizerId, fertilizer.getId());
        }
        for (Shop shop : shops) {
            nextShopId = Math.max(nextShopId, shop.getId());
        }
        loaded = true;
    }

    public List<Fertilizer> fertilizers() {
        return fertilizers;
    }

    public List<Shop> shops() {
        return shops;
    }

    public List<Stock> stocks() {
        return stocks;
    }

    public List<Admin> admins() {
        return admins;
    }

    public Weather weather() {
        return weather;
    }

    /** In load/insert order; callers sort as needed. */
    public List<Sale> sales() {
        return sales;
    }

    public int nextSaleId() {
        return ++nextSaleId;
    }

    /** Next invoice number, e.g. SB-0013. Never reused. */
    public String nextInvoiceNo() {
        return String.format(java.util.Locale.ROOT, "SB-%04d", ++nextInvoiceNumber);
    }

    public long now() {
        return clock.getAsLong();
    }

    /** Newest first. */
    public List<ActivityEntry> activities() {
        return activities;
    }

    /** Allocates a fertilizer id; ids are never reused, even after a delete. */
    public int nextFertilizerId() {
        return ++nextFertilizerId;
    }

    /** Allocates a shop id; ids are never reused. */
    public int nextShopId() {
        return ++nextShopId;
    }

    /** Seeded sale → absolute timestamp and item name/unit snapshot. */
    private Sale resolve(Sale sale) {
        long now = clock.getAsLong();
        long timestamp = sale.getTimestamp();
        if (timestamp == 0 && sale.getMinutesAgo() != null) {
            // "Today" seeds stay today even when the app is opened just after midnight.
            timestamp = Math.max(now - sale.getMinutesAgo() * 60_000L, startOfDay(now));
        } else if (timestamp == 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(clock.getAsLong());
            calendar.add(Calendar.DAY_OF_MONTH, -sale.getDaysAgo());
            int[] hourMinute = parseTime(sale.getTime());
            calendar.set(Calendar.HOUR_OF_DAY, hourMinute[0]);
            calendar.set(Calendar.MINUTE, hourMinute[1]);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            timestamp = Math.min(calendar.getTimeInMillis(), now);
        }
        List<SaleItem> items = new ArrayList<>();
        for (SaleItem item : sale.getItems()) {
            Fertilizer fertilizer = findFertilizer(item.getFertilizerId());
            items.add(item.hasSnapshot() || fertilizer == null ? item
                    : item.withSnapshot(fertilizer.getName(), fertilizer.getUnit()));
        }
        return sale.withResolved(timestamp, items);
    }

    private Fertilizer findFertilizer(int id) {
        for (Fertilizer fertilizer : fertilizers) {
            if (fertilizer.getId() == id) {
                return fertilizer;
            }
        }
        return null;
    }

    /** "HH:mm" → {hour, minute}; anything else → 10:00. */
    private static int[] parseTime(String time) {
        String[] parts = time.split(":");
        try {
            if (parts.length == 2) {
                return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
            }
        } catch (NumberFormatException ignored) {
            // fall through to the default
        }
        return new int[]{10, 0};
    }

    private static int invoiceNumber(String invoiceNo) {
        String digits = invoiceNo.replaceAll("\\D", "");
        try {
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
