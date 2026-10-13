package com.sarbondhu.app.ui.admin;

import android.app.Application;

import androidx.annotation.NonNull;

import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.repository.DealerRepository;

import java.io.IOException;
import java.util.List;

public class DealerAdminViewModel extends AdminListViewModel<List<Shop>> {

    private final DealerRepository repository;

    public DealerAdminViewModel(@NonNull Application application) {
        super(application);
        repository = DealerRepository.getInstance(application);
    }

    @Override
    protected List<Shop> fetch() throws IOException {
        return repository.getAllDealers();
    }
}
