package com.sarbondhu.app.ui.admin;

import android.app.Application;

import androidx.annotation.NonNull;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.repository.FertilizerRepository;

import java.io.IOException;
import java.util.List;

public class FertilizerAdminViewModel extends AdminListViewModel<List<Fertilizer>> {

    private final FertilizerRepository repository;

    public FertilizerAdminViewModel(@NonNull Application application) {
        super(application);
        repository = FertilizerRepository.getInstance(application);
    }

    @Override
    protected List<Fertilizer> fetch() throws IOException {
        return repository.getAllFertilizers();
    }
}
