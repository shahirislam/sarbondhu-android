package com.sarbondhu.app.ui.admin;

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
import com.sarbondhu.app.data.model.ActivityEntry;
import com.sarbondhu.app.data.model.DashboardData;
import com.sarbondhu.app.data.model.LowStockRow;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaCalendar;
import com.sarbondhu.app.util.BanglaNumber;

import java.util.Calendar;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void render(View root, UiState<DashboardData> state) {
        UiState.Status status = state.getStatus();
        root.findViewById(R.id.progress).setVisibility(
                status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        TextView error = root.findViewById(R.id.error_text);
        error.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        if (status == UiState.Status.ERROR) {
            error.setText(state.getMessageRes());
        }
        root.findViewById(R.id.content).setVisibility(
                status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        if (status != UiState.Status.SUCCESS) {
            return;
        }

        DashboardData data = state.getData();
        setStat(root.findViewById(R.id.stat_fertilizers),
                BanglaNumber.format(data.getFertilizerCount()), R.string.stat_fertilizers);
        setStat(root.findViewById(R.id.stat_dealers),
                BanglaNumber.format(data.getActiveDealerCount()), R.string.stat_active_dealers);
        setStat(root.findViewById(R.id.stat_stock),
                BanglaNumber.format(data.getTotalStock()), R.string.stat_total_stock);
        setStat(root.findViewById(R.id.stat_low),
                BanglaNumber.format(data.getLowStock().size()), R.string.stat_low_stock);

        LinearLayout lowList = root.findViewById(R.id.low_stock_list);
        lowList.removeAllViews();
        if (data.getLowStock().isEmpty()) {
            addRow(lowList, new AdminRow(0, getString(R.string.no_low_stock), "", null, 0));
        }
        for (LowStockRow row : data.getLowStock()) {
            boolean out = row.getQuantity() <= 0;
            addRow(lowList, new AdminRow(0, row.getFertilizer().getName(), row.getShop().getShopName(),
                    out ? getString(R.string.stock_out)
                            : getString(R.string.stock_quantity, BanglaNumber.format(row.getQuantity()),
                            row.getFertilizer().getUnit()),
                    out ? R.color.stock_out : R.color.stock_low));
        }

        LinearLayout activityList = root.findViewById(R.id.activity_list);
        activityList.removeAllViews();
        if (data.getRecentActivity().isEmpty()) {
            addRow(activityList, new AdminRow(0, getString(R.string.no_activity), "", null, 0));
        }
        for (ActivityEntry entry : data.getRecentActivity()) {
            Calendar at = Calendar.getInstance();
            at.setTimeInMillis(entry.getTimestamp());
            String time = BanglaCalendar.getBanglaTime(at.get(Calendar.HOUR_OF_DAY),
                    at.get(Calendar.MINUTE));
            addRow(activityList, new AdminRow(0, entry.getMessage(), time, null, 0));
        }
    }

    private static void setStat(View card, String value, @StringRes int label) {
        ((TextView) card.findViewById(R.id.stat_value)).setText(value);
        ((TextView) card.findViewById(R.id.stat_label)).setText(label);
    }

    private void addRow(LinearLayout parent, AdminRow row) {
        View view = getLayoutInflater().inflate(R.layout.item_admin_row, parent, false);
        AdminRowAdapter.bind(view, row);
        parent.addView(view);
    }
}
