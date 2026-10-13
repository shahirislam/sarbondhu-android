package com.sarbondhu.app.data.repository;

import android.content.Context;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.FertilizerDetail;
import com.sarbondhu.app.data.model.FertilizerItem;
import com.sarbondhu.app.data.model.HomeData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.source.DataLoader;
import com.sarbondhu.app.data.store.MockDataStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single access point for fertilizer data. Methods are blocking — call them off the main thread.
 */
public class FertilizerRepository {

    private static volatile FertilizerRepository instance;

    private final MockDataStore store;

    public FertilizerRepository(MockDataStore store) {
        this.store = store;
    }

    public FertilizerRepository(DataLoader loader) {
        this(new MockDataStore(loader));
    }

    public static FertilizerRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (FertilizerRepository.class) {
                if (instance == null) {
                    instance = new FertilizerRepository(MockDataStore.getInstance(context));
                }
            }
        }
        return instance;
    }

    public List<FertilizerItem> getFertilizerItems() throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            List<FertilizerItem> items = new ArrayList<>();
            for (Fertilizer fertilizer : store.fertilizers()) {
                int inStock = 0;
                for (ShopStock shopStock : shopStocksFor(fertilizer.getId())) {
                    if (shopStock.isInStock()) {
                        inStock++;
                    }
                }
                items.add(new FertilizerItem(fertilizer, inStock));
            }
            return items;
        }
    }

    public HomeData getHomeData() throws IOException {
        synchronized (store) {
            List<FertilizerItem> items = getFertilizerItems();
            return new HomeData(items, store.weather());
        }
    }

    public List<Fertilizer> getAllFertilizers() throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            return new ArrayList<>(store.fertilizers());
        }
    }

    /** Returns null when no fertilizer has the given id. */
    public Fertilizer getFertilizer(int id) throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            int index = indexOf(id);
            return index < 0 ? null : store.fertilizers().get(index);
        }
    }

    /** Adds a fertilizer; the draft's id is ignored and a new one assigned. */
    public Fertilizer addFertilizer(Fertilizer draft) throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            Fertilizer created = draft.withId(store.nextFertilizerId());
            store.fertilizers().add(created);
            ActivityLog.add(store, "নতুন সার যোগ: " + created.getName());
            return created;
        }
    }

    public void updateFertilizer(Fertilizer edited) throws IOException, DataException {
        synchronized (store) {
            store.ensureLoaded();
            int index = indexOf(edited.getId());
            if (index < 0) {
                throw new DataException(DataException.Reason.NOT_FOUND);
            }
            store.fertilizers().set(index, edited);
            ActivityLog.add(store, "সার হালনাগাদ: " + edited.getName());
        }
    }

    /** Removes the fertilizer and every dealer's stock of it. */
    public void deleteFertilizer(int id) throws IOException, DataException {
        synchronized (store) {
            store.ensureLoaded();
            int index = indexOf(id);
            if (index < 0) {
                throw new DataException(DataException.Reason.NOT_FOUND);
            }
            Fertilizer removed = store.fertilizers().remove(index);
            store.stocks().removeIf(stock -> stock.getFertilizerId() == id);
            ActivityLog.add(store, "সার মুছে ফেলা: " + removed.getName());
        }
    }

    private int indexOf(int id) {
        List<Fertilizer> fertilizers = store.fertilizers();
        for (int i = 0; i < fertilizers.size(); i++) {
            if (fertilizers.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /** Returns null when no fertilizer has the given id. */
    public FertilizerDetail getFertilizerDetail(int fertilizerId) throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            for (Fertilizer fertilizer : store.fertilizers()) {
                if (fertilizer.getId() == fertilizerId) {
                    return new FertilizerDetail(fertilizer, shopStocksFor(fertilizerId));
                }
            }
            return null;
        }
    }

    /** Active shops' stock of one fertilizer, most stock first. Caller holds the store lock. */
    private List<ShopStock> shopStocksFor(int fertilizerId) {
        Map<Integer, Shop> shopsById = new HashMap<>();
        for (Shop shop : store.shops()) {
            if (shop.isActive()) {
                shopsById.put(shop.getId(), shop);
            }
        }
        List<ShopStock> result = new ArrayList<>();
        for (Stock stock : store.stocks()) {
            Shop shop = shopsById.get(stock.getShopId());
            if (stock.getFertilizerId() == fertilizerId && shop != null) {
                result.add(new ShopStock(shop, stock.getQuantity(), stock.getSellingPrice()));
            }
        }
        result.sort((a, b) -> Double.compare(b.getQuantity(), a.getQuantity()));
        return result;
    }
}
