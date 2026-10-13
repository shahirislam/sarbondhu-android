package com.sarbondhu.app.ui.admin;

import android.app.Application;

import androidx.annotation.NonNull;

import com.sarbondhu.app.data.model.DashboardData;
import com.sarbondhu.app.data.repository.DashboardRepository;

import java.io.IOException;

public class DashboardViewModel extends AdminListViewModel<DashboardData> {

    private final DashboardRepository repository;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = DashboardRepository.getInstance(application);
    }

    @Override
    protected DashboardData fetch() throws IOException {
        return repository.getDashboard();
    }
}
