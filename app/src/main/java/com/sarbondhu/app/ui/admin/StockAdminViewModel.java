package com.sarbondhu.app.ui.admin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.DealerStockRow;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.repository.DataException;
import com.sarbondhu.app.data.repository.DealerRepository;
import com.sarbondhu.app.data.repository.StockRepository;
import com.sarbondhu.app.ui.common.Errors;
import com.sarbondhu.app.ui.common.Event;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.NumberInput;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockAdminViewModel extends AndroidViewModel {

    private final DealerRepository dealers;
    private final StockRepository stock;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<List<Shop>> dealerList = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<DealerStockRow>>> rows = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private volatile int selectedDealerId;

    public StockAdminViewModel(@NonNull Application application) {
        super(application);
        dealers = DealerRepository.getInstance(application);
        stock = StockRepository.getInstance(application);
    }

    public LiveData<List<Shop>> getDealers() {
        return dealerList;
    }

    public LiveData<UiState<List<DealerStockRow>>> getRows() {
        return rows;
    }

    public LiveData<Event<Integer>> getMessage() {
        return message;
    }

    public int getSelectedDealerId() {
        return selectedDealerId;
    }

    /** Loads dealers and, if one is selected, their stock. */
    public void load() {
        executor.execute(() -> {
            try {
                List<Shop> all = dealers.getAllDealers();
                if (selectedDealerId == 0 && !all.isEmpty()) {
                    selectedDealerId = all.get(0).getId();
                }
                dealerList.postValue(all);
                if (selectedDealerId != 0) {
                    rows.postValue(UiState.success(stock.getDealerStock(selectedDealerId)));
                }
            } catch (IOException e) {
                rows.postValue(UiState.error(R.string.error_load_data));
            }
        });
    }

    public void selectDealer(int dealerId) {
        if (dealerId == selectedDealerId) {
            return;
        }
        selectedDealerId = dealerId;
        rows.setValue(UiState.loading());
        load();
    }

    /** @return error message for the quantity field, or 0 if valid */
    public static int validateQuantity(String text) {
        Double value = NumberInput.parse(text);
        return value == null || value < 0 ? R.string.error_invalid_number : 0;
    }

    public void setStock(int fertilizerId, String quantityText, String priceText) {
        double quantity = NumberInput.parse(quantityText);
        double price = NumberInput.parse(priceText);
        run(() -> stock.setStock(selectedDealerId, fertilizerId, quantity, price));
    }

    public void adjustStock(int fertilizerId, String amountText, boolean add) {
        double amount = NumberInput.parse(amountText);
        run(() -> stock.adjustStock(selectedDealerId, fertilizerId, add ? amount : -amount));
    }

    private interface Change {
        void apply() throws IOException, DataException;
    }

    private void run(Change change) {
        executor.execute(() -> {
            try {
                change.apply();
                message.postValue(new Event<>(R.string.stock_updated));
                rows.postValue(UiState.success(stock.getDealerStock(selectedDealerId)));
            } catch (DataException e) {
                message.postValue(new Event<>(Errors.messageFor(e.getReason())));
            } catch (IOException e) {
                message.postValue(new Event<>(R.string.error_load_data));
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
