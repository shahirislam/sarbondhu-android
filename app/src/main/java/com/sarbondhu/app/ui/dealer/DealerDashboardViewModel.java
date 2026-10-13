package com.sarbondhu.app.ui.dealer;

import android.app.Application;

import androidx.annotation.NonNull;

import com.sarbondhu.app.data.model.DealerDashboard;
import com.sarbondhu.app.data.repository.SaleRepository;
import com.sarbondhu.app.session.Session;
import com.sarbondhu.app.ui.admin.AdminListViewModel;

import java.io.IOException;

public class DealerDashboardViewModel extends AdminListViewModel<DealerDashboard> {

    private final SaleRepository sales;

    public DealerDashboardViewModel(@NonNull Application application) {
        super(application);
        sales = SaleRepository.getInstance(application);
    }

    @Override
    protected DealerDashboard fetch() throws IOException {
        return sales.getDealerDashboard(Session.get().getUserId());
    }
}
