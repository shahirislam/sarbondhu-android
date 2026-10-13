package com.sarbondhu.app.ui.dealer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.ui.admin.AdminLists;
import com.sarbondhu.app.ui.admin.AdminRow;
import com.sarbondhu.app.ui.admin.AdminRowAdapter;
import com.sarbondhu.app.util.BanglaNumber;

import java.util.ArrayList;
import java.util.List;

public class SalesHistoryFragment extends Fragment {

    private SalesHistoryViewModel viewModel;
    private List<Sale> shown = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sales_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        AdminRowAdapter adapter = new AdminRowAdapter(id -> {
            for (Sale sale : shown) {
                if (sale.getId() == id) {
                    SaleFormat.showInvoice(requireContext(), sale);
                }
            }
        });
        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        TextView summary = view.findViewById(R.id.summary);

        viewModel = new ViewModelProvider(this).get(SalesHistoryViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), state -> AdminLists.renderStatus(view, state));
        viewModel.getFiltered().observe(getViewLifecycleOwner(), sales -> {
            shown = sales;
            double total = 0;
            List<AdminRow> rows = new ArrayList<>();
            for (Sale sale : sales) {
                total += sale.getTotal();
                rows.add(SaleFormat.row(requireContext(), sale));
            }
            adapter.submitList(rows);
            summary.setText(getString(R.string.history_summary,
                    BanglaNumber.format(sales.size()), BanglaNumber.format(total)));
        });

        EditText search = view.findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }
}
