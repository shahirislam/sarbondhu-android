package com.sarbondhu.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.ui.common.UiState;

import java.util.ArrayList;
import java.util.List;

public class DealerAdminFragment extends Fragment {

    private DealerAdminViewModel viewModel;
    private AdminRowAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new AdminRowAdapter(id ->
                startActivity(DealerFormActivity.newIntent(requireContext(), id)));
        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        ExtendedFloatingActionButton add = view.findViewById(R.id.add_button);
        add.setText(R.string.add_dealer);
        add.setVisibility(View.VISIBLE);
        add.extend();
        add.setOnClickListener(v -> startActivity(DealerFormActivity.newIntent(requireContext(), 0)));

        viewModel = new ViewModelProvider(this).get(DealerAdminViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void render(View root, UiState<List<Shop>> state) {
        AdminLists.renderStatus(root, state);
        if (state.getStatus() != UiState.Status.SUCCESS) {
            return;
        }
        List<AdminRow> rows = new ArrayList<>();
        for (Shop shop : state.getData()) {
            rows.add(new AdminRow(shop.getId(), shop.getShopName(),
                    getString(R.string.dealer_row_subtitle, shop.getOwnerName(), shop.getDistrict()),
                    getString(shop.isActive() ? R.string.dealer_active : R.string.dealer_inactive),
                    shop.isActive() ? R.color.stock_in : R.color.stock_out));
        }
        adapter.submitList(rows);
    }
}
