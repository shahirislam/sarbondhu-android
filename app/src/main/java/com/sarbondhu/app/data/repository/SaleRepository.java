package com.sarbondhu.app.data.repository;

import android.content.Context;

import com.sarbondhu.app.data.model.CartLine;
import com.sarbondhu.app.data.model.DealerDashboard;
import com.sarbondhu.app.data.model.DealerStockRow;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.data.model.SaleItem;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.store.MockDataStore;
import com.sarbondhu.app.util.BanglaNumber;
import com.sarbondhu.app.util.Quantity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/** Dealer sales. A sale is all-or-nothing: every line is checked before stock changes. */
public class SaleRepository {

    private static final int RECENT_SALES = 5;

    private static volatile SaleRepository instance;

    private final MockDataStore store;
    private final LongSupplier clock;
    private final StockRepository stock;

    public SaleRepository(MockDataStore store, LongSupplier clock) {
        this.store = store;
        this.clock = clock;
        this.stock = new StockRepository(store);
    }

    public static SaleRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (SaleRepository.class) {
                if (instance == null) {
                    instance = new SaleRepository(MockDataStore.getInstance(context),
                            System::currentTimeMillis);
                }
            }
        }
        return instance;
    }

    public Sale createSale(int shopId, String customerName, String customerPhone, List<CartLine> lines)
            throws IOException, DataException {
        if (lines.isEmpty()) {
            throw new DataException(DataException.Reason.EMPTY_CART);
        }
        Map<Integer, Double> wanted = new LinkedHashMap<>();
        for (CartLine line : lines) {
            double quantity = line.getQuantity();
            if (!(quantity > 0) || Double.isInfinite(quantity)) {
                throw new DataException(DataException.Reason.INVALID_AMOUNT);
            }
            Double sum = wanted.get(line.getFertilizerId());
            wanted.put(line.getFertilizerId(), Quantity.round((sum == null ? 0 : sum) + quantity));
        }

        synchronized (store) {
            store.ensureLoaded();
            Shop shop = findShop(shopId);
            if (shop == null) {
                throw new DataException(DataException.Reason.NOT_FOUND);
            }
            if (!shop.isActive()) {
                throw new DataException(DataException.Reason.DEALER_INACTIVE);
            }

            // Check every line before changing anything.
            List<SaleItem> items = new ArrayList<>();
            for (Map.Entry<Integer, Double> entry : wanted.entrySet()) {
                Fertilizer fertilizer = findFertilizer(entry.getKey());
                if (fertilizer == null) {
                    throw new DataException(DataException.Reason.NOT_FOUND);
                }
                Stock held = findStock(shopId, entry.getKey());
                if (held == null || Quantity.exceeds(entry.getValue(), held.getQuantity())) {
                    throw new DataException(DataException.Reason.INSUFFICIENT_STOCK);
                }
                items.add(new SaleItem(fertilizer.getId(), entry.getValue(), held.getSellingPrice(),
                        fertilizer.getName(), fertilizer.getUnit()));
            }

            for (SaleItem item : items) {
                Stock held = findStock(shopId, item.getFertilizerId());
                replaceStock(held, new Stock(shopId, item.getFertilizerId(),
                        Quantity.round(held.getQuantity() - item.getQuantity()), held.getSellingPrice()));
            }
            Sale sale = Sale.made(store.nextSaleId(), store.nextInvoiceNo(), shopId,
                    customerName.trim(), customerPhone.trim(), clock.getAsLong(), items);
            store.sales().add(sale);
            ActivityLog.add(store, "বিক্রয় " + sale.getInvoiceNo() + ": " + shop.getShopName()
                    + " – ৳" + BanglaNumber.format(sale.getTotal()));
            return sale;
        }
    }

    /** This dealer's sales, newest first. */
    public List<Sale> getSales(int shopId) throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            List<Sale> result = new ArrayList<>();
            for (Sale sale : store.sales()) {
                if (sale.getShopId() == shopId) {
                    result.add(sale);
                }
            }
            result.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            return result;
        }
    }

    public DealerDashboard getDealerDashboard(int shopId) throws IOException {
        synchronized (store) {
            List<Sale> sales = getSales(shopId);
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(clock.getAsLong());
            Calendar at = Calendar.getInstance();

            int todayCount = 0;
            int monthCount = 0;
            double todayRevenue = 0;
            double monthRevenue = 0;
            for (Sale sale : sales) {
                at.setTimeInMillis(sale.getTimestamp());
                boolean sameMonth = at.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && at.get(Calendar.MONTH) == now.get(Calendar.MONTH);
                if (sameMonth) {
                    monthCount++;
                    monthRevenue += sale.getTotal();
                    if (at.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                        todayCount++;
                        todayRevenue += sale.getTotal();
                    }
                }
            }

            List<DealerStockRow> held = new ArrayList<>();
            for (DealerStockRow row : stock.getDealerStock(shopId)) {
                if (row.hasStock()) {
                    held.add(row);
                }
            }
            return new DealerDashboard(todayCount, todayRevenue, monthCount, monthRevenue, held,
                    new ArrayList<>(sales.subList(0, Math.min(RECENT_SALES, sales.size()))));
        }
    }

    /** Search by invoice number, customer name/phone or fertilizer name; blank matches all. */
    public static boolean matches(Sale sale, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return true;
        }
        if (sale.getInvoiceNo().toLowerCase(Locale.ROOT).contains(q)
                || sale.getCustomerName().toLowerCase(Locale.ROOT).contains(q)
                || sale.getCustomerPhone().contains(q)) {
            return true;
        }
        for (SaleItem item : sale.getItems()) {
            if (item.getFertilizerName().toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    private void replaceStock(Stock old, Stock replacement) {
        List<Stock> stocks = store.stocks();
        stocks.set(stocks.indexOf(old), replacement);
    }

    private Stock findStock(int shopId, int fertilizerId) {
        for (Stock s : store.stocks()) {
            if (s.getShopId() == shopId && s.getFertilizerId() == fertilizerId) {
                return s;
            }
        }
        return null;
    }

    private Shop findShop(int id) {
        for (Shop shop : store.shops()) {
            if (shop.getId() == id) {
                return shop;
            }
        }
        return null;
    }

    private Fertilizer findFertilizer(int id) {
        for (Fertilizer fertilizer : store.fertilizers()) {
            if (fertilizer.getId() == id) {
                return fertilizer;
            }
        }
        return null;
    }
}
