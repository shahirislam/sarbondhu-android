package com.sarbondhu.app.data.repository;

import android.content.Context;

import com.sarbondhu.app.data.model.DashboardData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.store.MockDataStore;

import java.io.IOException;
import java.util.ArrayList;

public class DashboardRepository {

    private static volatile DashboardRepository instance;

    private final MockDataStore store;
    private final StockRepository stock;

    public DashboardRepository(MockDataStore store) {
        this.store = store;
        this.stock = new StockRepository(store);
    }

    public static DashboardRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (DashboardRepository.class) {
                if (instance == null) {
                    instance = new DashboardRepository(MockDataStore.getInstance(context));
                }
            }
        }
        return instance;
    }

    public DashboardData getDashboard() throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            int activeDealers = 0;
            for (Shop shop : store.shops()) {
                if (shop.isActive()) {
                    activeDealers++;
                }
            }
            return new DashboardData(store.fertilizers().size(), activeDealers,
                    stock.getTotalStock(), stock.getLowStock(), new ArrayList<>(store.activities()));
        }
    }
}
