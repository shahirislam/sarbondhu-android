package com.sarbondhu.app.data.repository;

import android.content.Context;

import com.sarbondhu.app.data.model.Admin;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.store.MockDataStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Admin-side management of dealers (shops). */
public class DealerRepository {

    private static volatile DealerRepository instance;

    private final MockDataStore store;

    public DealerRepository(MockDataStore store) {
        this.store = store;
    }

    public static DealerRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (DealerRepository.class) {
                if (instance == null) {
                    instance = new DealerRepository(MockDataStore.getInstance(context));
                }
            }
        }
        return instance;
    }

    public List<Shop> getAllDealers() throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            return new ArrayList<>(store.shops());
        }
    }

    /** Returns null when no dealer has the id. */
    public Shop getDealer(int id) throws IOException {
        synchronized (store) {
            store.ensureLoaded();
            int index = indexOf(id);
            return index < 0 ? null : store.shops().get(index);
        }
    }

    public Shop addDealer(Shop draft) throws IOException, DataException {
        synchronized (store) {
            store.ensureLoaded();
            requireFreeUsername(draft.getUsername(), -1);
            Shop created = draft.withId(store.nextShopId()).withActive(true);
            store.shops().add(created);
            ActivityLog.add(store, "নতুন ডিলার যোগ: " + created.getShopName());
            return created;
        }
    }

    /** Updates profile fields and username; password and active status are kept. */
    public void updateDealer(Shop edited) throws IOException, DataException {
        synchronized (store) {
            store.ensureLoaded();
            int index = requireIndex(edited.getId());
            requireFreeUsername(edited.getUsername(), edited.getId());
            Shop current = store.shops().get(index);
            store.shops().set(index, edited.withPassword(current.getPassword())
                    .withActive(current.isActive()));
            ActivityLog.add(store, "ডিলার হালনাগাদ: " + edited.getShopName());
        }
    }

    public void setActive(int id, boolean active) throws IOException, DataException {
        synchronized (store) {
            store.ensureLoaded();
            int index = requireIndex(id);
            Shop updated = store.shops().get(index).withActive(active);
            store.shops().set(index, updated);
            ActivityLog.add(store, (active ? "ডিলার সক্রিয়: " : "ডিলার নিষ্ক্রিয়: ")
                    + updated.getShopName());
        }
    }

    public void resetPassword(int id, String newPassword) throws IOException, DataException {
        synchronized (store) {
            store.ensureLoaded();
            int index = requireIndex(id);
            Shop updated = store.shops().get(index).withPassword(newPassword);
            store.shops().set(index, updated);
            ActivityLog.add(store, "পাসওয়ার্ড রিসেট: " + updated.getShopName());
        }
    }

    /** Dealer changes their own password; the current one must match. */
    public void changePassword(int id, String currentPassword, String newPassword)
            throws IOException, DataException {
        synchronized (store) {
            store.ensureLoaded();
            int index = requireIndex(id);
            Shop shop = store.shops().get(index);
            if (!shop.getPassword().equals(currentPassword)) {
                throw new DataException(DataException.Reason.WRONG_PASSWORD);
            }
            store.shops().set(index, shop.withPassword(newPassword));
            ActivityLog.add(store, "ডিলার নিজের পাসওয়ার্ড পরিবর্তন করেছেন: " + shop.getShopName());
        }
    }

    private void requireFreeUsername(String username, int ownId) throws DataException {
        String wanted = AuthRepository.normalise(username);
        for (Admin admin : store.admins()) {
            if (AuthRepository.normalise(admin.getUsername()).equals(wanted)) {
                throw new DataException(DataException.Reason.USERNAME_TAKEN);
            }
        }
        for (Shop shop : store.shops()) {
            if (shop.getId() != ownId && AuthRepository.normalise(shop.getUsername()).equals(wanted)) {
                throw new DataException(DataException.Reason.USERNAME_TAKEN);
            }
        }
    }

    private int requireIndex(int id) throws DataException {
        int index = indexOf(id);
        if (index < 0) {
            throw new DataException(DataException.Reason.NOT_FOUND);
        }
        return index;
    }

    private int indexOf(int id) {
        List<Shop> shops = store.shops();
        for (int i = 0; i < shops.size(); i++) {
            if (shops.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }
}
