package com.sarbondhu.app.data.repository;

import android.content.Context;

import com.sarbondhu.app.data.model.DealerStockRow;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.LowStockRow;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.store.MockDataStore;
import com.sarbondhu.app.util.BanglaNumber;
import com.sarbondhu.app.util.Quantity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Dealer stock. Only the admin side changes it; quantities never go below zero. */
public class StockRepository {

    private static volatile StockRepository instance;

    private final MockDataStore store;

    public StockRepository(MockDataStore store) {
        this.store = store;
    }

    public static StockRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (StockRepository.class) {
                if (instance == null) {
                    instance = new StockRepository(MockDataStore.getInstance(context));
                }
            }
        }
        return instance;
    }

    /** Every fertilizer, in catalogue order, with this dealer's quantity and price. */
    public List<DealerStockRow> getDealerStock(int shopId) throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            List<DealerStockRow> rows = new ArrayList<>();
            for (Fertilizer fertilizer : store.fertilizers()) {
                Stock stock = findStock(shopId, fertilizer.getId());
                rows.add(stock == null
                        ? new DealerStockRow(fertilizer, 0, fertilizer.getUnitPrice(), false)
                        : new DealerStockRow(fertilizer, stock.getQuantity(), stock.getSellingPrice(), true));
            }
            return rows;
        }
    }

    public void setStock(int shopId, int fertilizerId, double quantity, double sellingPrice)
            throws IOException, DataException {
        if (!Double.isFinite(quantity) || !Double.isFinite(sellingPrice) || sellingPrice < 0) {
            throw new DataException(DataException.Reason.INVALID_AMOUNT);
        }
        if (quantity < 0) {
            throw new DataException(DataException.Reason.NEGATIVE_STOCK);
        }
        synchronized (store) {
            store.ensureLoaded();
            Shop shop = requireShop(shopId);
            Fertilizer fertilizer = requireFertilizer(fertilizerId);
            putStock(new Stock(shopId, fertilizerId, Quantity.round(quantity), sellingPrice));
            ActivityLog.add(store, "মজুদ সেট: " + shop.getShopName() + " – " + fertilizer.getName()
                    + " " + BanglaNumber.format(quantity) + " " + fertilizer.getUnit());
        }
    }

    /** Adds (positive delta) or removes (negative delta) stock. */
    public void adjustStock(int shopId, int fertilizerId, double delta)
            throws IOException, DataException {
        synchronized (store) {
            store.ensureLoaded();
            Shop shop = requireShop(shopId);
            Fertilizer fertilizer = requireFertilizer(fertilizerId);
            Stock current = findStock(shopId, fertilizerId);
            double quantity = Quantity.round((current == null ? 0 : current.getQuantity()) + delta);
            if (!Double.isFinite(quantity)) {
                throw new DataException(DataException.Reason.INVALID_AMOUNT);
            }
            if (quantity < 0) {
                throw new DataException(DataException.Reason.NEGATIVE_STOCK);
            }
            double price = current == null ? fertilizer.getUnitPrice() : current.getSellingPrice();
            putStock(new Stock(shopId, fertilizerId, quantity, price));
            ActivityLog.add(store, "মজুদ " + (delta >= 0 ? "যোগ: " : "বিয়োগ: ") + shop.getShopName()
                    + " – " + fertilizer.getName() + " " + BanglaNumber.format(Math.abs(delta))
                    + " " + fertilizer.getUnit());
        }
    }

    /** Active dealers' stock rows below the low-stock threshold, lowest first. */
    public List<LowStockRow> getLowStock() throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            List<LowStockRow> rows = new ArrayList<>();
            for (Stock stock : store.stocks()) {
                Shop shop = findShop(stock.getShopId());
                Fertilizer fertilizer = findFertilizer(stock.getFertilizerId());
                if (shop != null && shop.isActive() && fertilizer != null
                        && stock.getQuantity() < ShopStock.LOW_STOCK_THRESHOLD) {
                    rows.add(new LowStockRow(shop, fertilizer, stock.getQuantity()));
                }
            }
            rows.sort((a, b) -> Double.compare(a.getQuantity(), b.getQuantity()));
            return rows;
        }
    }

    public double getTotalStock() throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            double total = 0;
            for (Stock stock : store.stocks()) {
                Shop shop = findShop(stock.getShopId());
                if (shop != null && shop.isActive()) {
                    total += stock.getQuantity();
                }
            }
            return total;
        }
    }

    private void putStock(Stock replacement) {
        List<Stock> stocks = store.stocks();
        for (int i = 0; i < stocks.size(); i++) {
            Stock stock = stocks.get(i);
            if (stock.getShopId() == replacement.getShopId()
                    && stock.getFertilizerId() == replacement.getFertilizerId()) {
                stocks.set(i, replacement);
                return;
            }
        }
        stocks.add(replacement);
    }

    private Stock findStock(int shopId, int fertilizerId) {
        for (Stock stock : store.stocks()) {
            if (stock.getShopId() == shopId && stock.getFertilizerId() == fertilizerId) {
                return stock;
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

    private Shop requireShop(int id) throws DataException {
        Shop shop = findShop(id);
        if (shop == null) {
            throw new DataException(DataException.Reason.NOT_FOUND);
        }
        return shop;
    }

    private Fertilizer requireFertilizer(int id) throws DataException {
        Fertilizer fertilizer = findFertilizer(id);
        if (fertilizer == null) {
            throw new DataException(DataException.Reason.NOT_FOUND);
        }
        return fertilizer;
    }
}
