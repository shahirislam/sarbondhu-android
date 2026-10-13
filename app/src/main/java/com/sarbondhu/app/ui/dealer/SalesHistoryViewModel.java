package com.sarbondhu.app.ui.dealer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.data.repository.SaleRepository;
import com.sarbondhu.app.session.Session;
import com.sarbondhu.app.ui.admin.AdminListViewModel;
import com.sarbondhu.app.ui.common.UiState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SalesHistoryViewModel extends AdminListViewModel<List<Sale>> {

    private final SaleRepository sales;
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MediatorLiveData<List<Sale>> filtered = new MediatorLiveData<>();

    public SalesHistoryViewModel(@NonNull Application application) {
        super(application);
        sales = SaleRepository.getInstance(application);
        filtered.addSource(getState(), s -> refilter());
        filtered.addSource(query, q -> refilter());
    }

    /** All sales matching the search, newest first. */
    public LiveData<List<Sale>> getFiltered() {
        return filtered;
    }

    public void setQuery(String text) {
        query.setValue(text);
    }

    private void refilter() {
        UiState<List<Sale>> state = getState().getValue();
        if (state == null || state.getStatus() != UiState.Status.SUCCESS) {
            return;
        }
        List<Sale> result = new ArrayList<>();
        for (Sale sale : state.getData()) {
            if (SaleRepository.matches(sale, query.getValue())) {
                result.add(sale);
            }
        }
        filtered.setValue(result);
    }

    @Override
    protected List<Sale> fetch() throws IOException {
        return sales.getSales(Session.get().getUserId());
    }
}
