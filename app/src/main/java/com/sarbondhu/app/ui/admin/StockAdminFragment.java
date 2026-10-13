package com.sarbondhu.app.ui.admin;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.DealerStockRow;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaNumber;
import com.sarbondhu.app.util.NumberInput;

import java.util.ArrayList;
import java.util.List;

public class StockAdminFragment extends Fragment {

    private StockAdminViewModel viewModel;
    private StockRowAdapter adapter;
    private List<Shop> dealers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stock_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new StockRowAdapter();
        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        MaterialAutoCompleteTextView picker = view.findViewById(R.id.dealer_picker);
        picker.setOnItemClickListener((parent, v, position, id) ->
                viewModel.selectDealer(dealers.get(position).getId()));

        viewModel = new ViewModelProvider(this).get(StockAdminViewModel.class);
        viewModel.getDealers().observe(getViewLifecycleOwner(), all -> {
            dealers = all;
            List<String> names = new ArrayList<>();
            String selected = "";
            for (Shop shop : all) {
                String label = shop.isActive() ? shop.getShopName()
                        : shop.getShopName() + " (" + getString(R.string.dealer_inactive) + ")";
                names.add(label);
                if (shop.getId() == viewModel.getSelectedDealerId()) {
                    selected = label;
                }
            }
            picker.setSimpleItems(names.toArray(new String[0]));
            picker.setText(selected, false);
        });
        viewModel.getRows().observe(getViewLifecycleOwner(), state -> {
            AdminLists.renderStatus(view, state);
            if (state.getStatus() == UiState.Status.SUCCESS) {
                adapter.submit(state.getData());
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), event -> {
            Integer text = event.take();
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void showDialog(DealerStockRow row, boolean adjust) {
        Fertilizer fertilizer = row.getFertilizer();
        View content = getLayoutInflater().inflate(R.layout.dialog_stock, null);
        ((TextView) content.findViewById(R.id.current_stock)).setText(getString(R.string.current_stock,
                BanglaNumber.format(row.getQuantity()), fertilizer.getUnit()));
        MaterialButtonToggleGroup direction = content.findViewById(R.id.direction_group);
        TextInputLayout quantityLayout = content.findViewById(R.id.quantity_layout);
        TextInputEditText quantity = content.findViewById(R.id.quantity);
        TextInputLayout priceLayout = content.findViewById(R.id.price_layout);
        TextInputEditText price = content.findViewById(R.id.price);
        quantityLayout.setHint(getString(R.string.field_quantity, fertilizer.getUnit()));
        if (adjust) {
            direction.setVisibility(View.VISIBLE);
            priceLayout.setVisibility(View.GONE);
        } else {
            quantity.setText(BanglaNumber.plain(row.getQuantity()));
            price.setText(BanglaNumber.plain(row.getSellingPrice()));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(adjust ? R.string.adjust_stock_title : R.string.set_stock_title,
                        fertilizer.getName()))
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String quantityText = FertilizerFormActivity.text(quantity);
            String priceText = FertilizerFormActivity.text(price);
            int quantityError = StockAdminViewModel.validateQuantity(quantityText);
            int priceError = adjust ? 0 : StockAdminViewModel.validateQuantity(priceText);
            quantityLayout.setError(quantityError == 0 ? null : getString(quantityError));
            priceLayout.setError(priceError == 0 ? null : getString(priceError));
            if (quantityError != 0 || priceError != 0) {
                return;
            }
            if (adjust) {
                boolean add = direction.getCheckedButtonId() == R.id.direction_add;
                Double amount = NumberInput.parse(quantityText);
                if (!add && amount > row.getQuantity()) {
                    quantityLayout.setError(getString(R.string.error_negative_stock));
                    return;
                }
                viewModel.adjustStock(fertilizer.getId(), quantityText, add);
            } else {
                viewModel.setStock(fertilizer.getId(), quantityText, priceText);
            }
            dialog.dismiss();
        }));
        dialog.show();
    }

    private class StockRowAdapter extends RecyclerView.Adapter<StockRowAdapter.Holder> {

        private List<DealerStockRow> items = new ArrayList<>();

        void submit(List<DealerStockRow> rows) {
            items = rows;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stock_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            DealerStockRow row = items.get(position);
            Fertilizer fertilizer = row.getFertilizer();
            holder.name.setText(fertilizer.getName());
            holder.price.setText(row.hasStock()
                    ? getString(R.string.shop_price, BanglaNumber.format(row.getSellingPrice()),
                    fertilizer.getUnit())
                    : getString(R.string.no_stock_row));
            int color;
            if (row.getQuantity() <= 0) {
                holder.badge.setText(R.string.stock_out);
                color = R.color.stock_out;
            } else {
                holder.badge.setText(getString(R.string.stock_quantity,
                        BanglaNumber.format(row.getQuantity()), fertilizer.getUnit()));
                color = row.getQuantity() < ShopStock.LOW_STOCK_THRESHOLD
                        ? R.color.stock_low : R.color.stock_in;
            }
            holder.badge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.getContext(), color)));
            holder.set.setOnClickListener(v -> showDialog(row, false));
            holder.adjust.setOnClickListener(v -> showDialog(row, true));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView badge;
            final TextView price;
            final View set;
            final View adjust;

            Holder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.fertilizer_name);
                badge = itemView.findViewById(R.id.stock_badge);
                price = itemView.findViewById(R.id.selling_price);
                set = itemView.findViewById(R.id.set_button);
                adjust = itemView.findViewById(R.id.adjust_button);
            }
        }
    }
}
