package com.sarbondhu.app.ui.dealer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.CartLine;
import com.sarbondhu.app.data.model.DealerStockRow;
import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.data.repository.DataException;
import com.sarbondhu.app.data.repository.SaleRepository;
import com.sarbondhu.app.data.repository.StockRepository;
import com.sarbondhu.app.session.Session;
import com.sarbondhu.app.ui.common.Errors;
import com.sarbondhu.app.ui.common.Event;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.NumberInput;
import com.sarbondhu.app.util.Quantity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewSaleViewModel extends AndroidViewModel {

    /** One fertilizer in the cart. */
    public static final class CartEntry {
        final DealerStockRow row;
        final double quantity;

        CartEntry(DealerStockRow row, double quantity) {
            this.row = row;
            this.quantity = quantity;
        }

        double lineTotal() {
            return quantity * row.getSellingPrice();
        }
    }

    private final SaleRepository sales;
    private final StockRepository stock;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UiState<List<DealerStockRow>>> available = new MutableLiveData<>();
    private final MutableLiveData<List<CartEntry>> cart = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<Sale>> completed = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);

    public NewSaleViewModel(@NonNull Application application) {
        super(application);
        sales = SaleRepository.getInstance(application);
        stock = StockRepository.getInstance(application);
    }

    public LiveData<UiState<List<DealerStockRow>>> getAvailable() {
        return available;
    }

    public LiveData<List<CartEntry>> getCart() {
        return cart;
    }

    public LiveData<Event<Sale>> getCompleted() {
        return completed;
    }

    public LiveData<Event<Integer>> getMessage() {
        return message;
    }

    public LiveData<Boolean> getSaving() {
        return saving;
    }

    /** Loads the dealer's fertilizers that have stock to sell. */
    public void load() {
        if (available.getValue() == null) {
            available.setValue(UiState.loading());
        }
        executor.execute(() -> {
            try {
                available.postValue(UiState.success(sellable()));
            } catch (IOException e) {
                available.postValue(UiState.error(R.string.error_load_data));
            }
        });
    }

    /** @return an error message for the quantity field, or 0 when added */
    @StringRes
    public int addToCart(DealerStockRow row, String quantityText) {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return R.string.error_sale_in_progress;
        }
        Double parsed = NumberInput.parse(quantityText);
        double quantity = parsed == null ? 0 : Quantity.round(parsed);
        if (quantity <= 0) {
            return R.string.error_quantity_positive;
        }
        List<CartEntry> next = new ArrayList<>();
        double alreadyInCart = 0;
        for (CartEntry entry : currentCart()) {
            if (entry.row.getFertilizer().getId() == row.getFertilizer().getId()) {
                alreadyInCart += entry.quantity;
            } else {
                next.add(entry);
            }
        }
        double total = Quantity.round(alreadyInCart + quantity);
        if (Quantity.exceeds(total, row.getQuantity())) {
            return R.string.error_more_than_stock;
        }
        next.add(new CartEntry(row, total));
        cart.setValue(next);
        return 0;
    }

    public void removeFromCart(int fertilizerId) {
        List<CartEntry> next = new ArrayList<>();
        for (CartEntry entry : currentCart()) {
            if (entry.row.getFertilizer().getId() != fertilizerId) {
                next.add(entry);
            }
        }
        cart.setValue(next);
    }

    public double getCartTotal() {
        double total = 0;
        for (CartEntry entry : currentCart()) {
            total += entry.lineTotal();
        }
        return total;
    }

    public void checkout(String customerName, String customerPhone) {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        List<CartLine> lines = new ArrayList<>();
        for (CartEntry entry : currentCart()) {
            lines.add(new CartLine(entry.row.getFertilizer().getId(), entry.quantity));
        }
        if (lines.isEmpty()) {
            message.setValue(new Event<>(R.string.error_empty_cart));
            return;
        }
        saving.setValue(true);
        int dealerId = Session.get().getUserId();
        executor.execute(() -> {
            try {
                Sale sale = sales.createSale(dealerId, customerName, customerPhone, lines);
                cart.postValue(Collections.emptyList()); // adding is blocked while saving
                completed.postValue(new Event<>(sale));
            } catch (DataException e) {
                message.postValue(new Event<>(Errors.messageFor(e.getReason())));
            } catch (IOException e) {
                message.postValue(new Event<>(R.string.error_load_data));
            }
            try {
                available.postValue(UiState.success(sellable()));
            } catch (IOException e) {
                available.postValue(UiState.error(R.string.error_load_data));
            }
            saving.postValue(false);
        });
    }

    private List<CartEntry> currentCart() {
        List<CartEntry> value = cart.getValue();
        return value == null ? Collections.emptyList() : value;
    }

    private List<DealerStockRow> sellable() throws IOException {
        List<DealerStockRow> rows = new ArrayList<>();
        for (DealerStockRow row : stock.getDealerStock(Session.get().getUserId())) {
            if (row.getQuantity() > 0) {
                rows.add(row);
            }
        }
        return rows;
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
