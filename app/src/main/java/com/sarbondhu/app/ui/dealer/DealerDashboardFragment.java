package com.sarbondhu.app.ui.dealer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.DealerDashboard;
import com.sarbondhu.app.data.model.DealerStockRow;
import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.ui.admin.AdminLists;
import com.sarbondhu.app.ui.admin.AdminRow;
import com.sarbondhu.app.ui.admin.AdminRowAdapter;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaNumber;

public class DealerDashboardFragment extends Fragment {

    private DealerDashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dealer_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(DealerDashboardViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void render(View root, UiState<DealerDashboard> state) {
        AdminLists.renderStatus(root, state);
        if (state.getStatus() != UiState.Status.SUCCESS) {
            return;
        }
        DealerDashboard data = state.getData();
        setStat(root.findViewById(R.id.stat_today_count),
                BanglaNumber.format(data.getTodayCount()), R.string.stat_today_sales);
        setStat(root.findViewById(R.id.stat_today_revenue),
                BanglaNumber.format(data.getTodayRevenue()), R.string.stat_today_revenue);
        setStat(root.findViewById(R.id.stat_month_count),
                BanglaNumber.format(data.getMonthCount()), R.string.stat_month_sales);
        setStat(root.findViewById(R.id.stat_month_revenue),
                BanglaNumber.format(data.getMonthRevenue()), R.string.stat_month_revenue);

        LinearLayout stockList = root.findViewById(R.id.stock_list);
        stockList.removeAllViews();
        if (data.getStock().isEmpty()) {
            addRow(stockList, new AdminRow(0, getString(R.string.no_stock_yet), "", null, 0), null);
        }
        for (DealerStockRow row : data.getStock()) {
            String unit = row.getFertilizer().getUnit();
            boolean out = row.getQuantity() <= 0;
            addRow(stockList, new AdminRow(row.getFertilizer().getId(), row.getFertilizer().getName(),
                    getString(R.string.shop_price, BanglaNumber.format(row.getSellingPrice()), unit),
                    out ? getString(R.string.stock_out)
                            : getString(R.string.stock_quantity, BanglaNumber.format(row.getQuantity()), unit),
                    out ? R.color.stock_out
                            : row.getQuantity() < ShopStock.LOW_STOCK_THRESHOLD ? R.color.stock_low
                            : R.color.stock_in), null);
        }

        LinearLayout recent = root.findViewById(R.id.recent_list);
        recent.removeAllViews();
        if (data.getRecentSales().isEmpty()) {
            addRow(recent, new AdminRow(0, getString(R.string.no_sales_yet), "", null, 0), null);
        }
        for (Sale sale : data.getRecentSales()) {
            addRow(recent, SaleFormat.row(requireContext(), sale),
                    v -> SaleFormat.showInvoice(requireContext(), sale));
        }
    }

    private static void setStat(View card, String value, @StringRes int label) {
        ((TextView) card.findViewById(R.id.stat_value)).setText(value);
        ((TextView) card.findViewById(R.id.stat_label)).setText(label);
    }

    private void addRow(LinearLayout parent, AdminRow row, View.OnClickListener onClick) {
        View view = getLayoutInflater().inflate(R.layout.item_admin_row, parent, false);
        AdminRowAdapter.bind(view, row);
        view.setOnClickListener(onClick);
        parent.addView(view);
    }
}
