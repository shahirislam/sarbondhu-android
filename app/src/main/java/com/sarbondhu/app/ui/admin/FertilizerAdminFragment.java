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
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaNumber;

import java.util.ArrayList;
import java.util.List;

public class FertilizerAdminFragment extends Fragment {

    private FertilizerAdminViewModel viewModel;
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
                startActivity(FertilizerFormActivity.newIntent(requireContext(), id)));
        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        ExtendedFloatingActionButton add = view.findViewById(R.id.add_button);
        add.setText(R.string.add_fertilizer);
        add.setVisibility(View.VISIBLE);
        add.extend();
        add.setOnClickListener(v ->
                startActivity(FertilizerFormActivity.newIntent(requireContext(), 0)));

        viewModel = new ViewModelProvider(this).get(FertilizerAdminViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void render(View root, UiState<List<Fertilizer>> state) {
        AdminLists.renderStatus(root, state);
        if (state.getStatus() != UiState.Status.SUCCESS) {
            return;
        }
        List<AdminRow> rows = new ArrayList<>();
        for (Fertilizer f : state.getData()) {
            rows.add(new AdminRow(f.getId(), f.getName(),
                    getString(R.string.fertilizer_row_subtitle, f.getCategory(),
                            BanglaNumber.format(f.getUnitPrice()), f.getUnit()),
                    null, 0));
        }
        adapter.submitList(rows);
    }
}
