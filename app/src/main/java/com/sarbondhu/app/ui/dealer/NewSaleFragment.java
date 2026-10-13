package com.sarbondhu.app.ui.dealer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.DealerStockRow;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.ui.admin.AdminLists;
import com.sarbondhu.app.ui.admin.AdminRow;
import com.sarbondhu.app.ui.admin.AdminRowAdapter;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaNumber;

import java.util.List;

public class NewSaleFragment extends Fragment {

    private NewSaleViewModel viewModel;
    private EditText customerName;
    private EditText customerPhone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_sale, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        customerName = view.findViewById(R.id.customer_name);
        customerPhone = view.findViewById(R.id.customer_phone);
        View complete = view.findViewById(R.id.complete_sale);

        // Scoped to the activity so the cart (and a sale in progress) survive switching tabs.
        viewModel = new ViewModelProvider(requireActivity()).get(NewSaleViewModel.class);
        viewModel.getAvailable().observe(getViewLifecycleOwner(), state -> renderAvailable(view, state));
        viewModel.getCart().observe(getViewLifecycleOwner(), cart -> renderCart(view, cart));
        viewModel.getSaving().observe(getViewLifecycleOwner(), busy -> complete.setEnabled(!busy));
        viewModel.getMessage().observe(getViewLifecycleOwner(), event -> {
            Integer text = event.take();
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getCompleted().observe(getViewLifecycleOwner(), event -> {
            Sale sale = event.take();
            if (sale != null) {
                customerName.setText("");
                customerPhone.setText("");
                showDone(sale);
            }
        });
        complete.setOnClickListener(v -> viewModel.checkout(text(customerName), text(customerPhone)));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void renderAvailable(View root, UiState<List<DealerStockRow>> state) {
        AdminLists.renderStatus(root, state);
        if (state.getStatus() != UiState.Status.SUCCESS) {
            return;
        }
        LinearLayout list = root.findViewById(R.id.available_list);
        list.removeAllViews();
        if (state.getData().isEmpty()) {
            addRow(list, new AdminRow(0, getString(R.string.no_sellable_stock), "", null, 0), null);
        }
        for (DealerStockRow row : state.getData()) {
            Fertilizer fertilizer = row.getFertilizer();
            addRow(list, new AdminRow(fertilizer.getId(), fertilizer.getName(),
                    getString(R.string.available_row_subtitle, BanglaNumber.format(row.getQuantity()),
                            fertilizer.getUnit(), BanglaNumber.format(row.getSellingPrice())),
                    getString(R.string.add_to_cart), R.color.primary),
                    v -> askQuantity(row));
        }
    }

    private void renderCart(View root, List<NewSaleViewModel.CartEntry> cart) {
        LinearLayout list = root.findViewById(R.id.cart_list);
        list.removeAllViews();
        if (cart.isEmpty()) {
            addRow(list, new AdminRow(0, getString(R.string.cart_empty), "", null, 0), null);
        }
        for (NewSaleViewModel.CartEntry entry : cart) {
            Fertilizer fertilizer = entry.row.getFertilizer();
            addRow(list, new AdminRow(fertilizer.getId(), fertilizer.getName(),
                    getString(R.string.cart_row_subtitle, BanglaNumber.format(entry.quantity),
                            fertilizer.getUnit(), BanglaNumber.format(entry.row.getSellingPrice())),
                    getString(R.string.money, BanglaNumber.format(entry.lineTotal())),
                    R.color.stock_in),
                    v -> confirmRemove(fertilizer.getId(), fertilizer.getName()));
        }
        ((TextView) root.findViewById(R.id.cart_total)).setText(
                getString(R.string.cart_total, BanglaNumber.format(viewModel.getCartTotal())));
    }

    private void askQuantity(DealerStockRow row) {
        Fertilizer fertilizer = row.getFertilizer();
        View content = getLayoutInflater().inflate(R.layout.dialog_stock, null);
        ((TextView) content.findViewById(R.id.current_stock)).setText(getString(
                R.string.available_quantity, BanglaNumber.format(row.getQuantity()), fertilizer.getUnit()));
        content.findViewById(R.id.price_layout).setVisibility(View.GONE);
        TextInputLayout quantityLayout = content.findViewById(R.id.quantity_layout);
        TextInputEditText quantity = content.findViewById(R.id.quantity);
        quantityLayout.setHint(getString(R.string.field_quantity, fertilizer.getUnit()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.add_to_cart_title, fertilizer.getName()))
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add_to_cart, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int error = viewModel.addToCart(row, text(quantity));
            if (error == 0) {
                dialog.dismiss();
            } else {
                quantityLayout.setError(getString(error));
            }
        }));
        dialog.show();
    }

    private void confirmRemove(int fertilizerId, String name) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(name)
                .setMessage(R.string.remove_from_cart)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (d, w) -> viewModel.removeFromCart(fertilizerId))
                .show();
    }

    private void showDone(Sale sale) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sale_done_title)
                .setMessage(getString(R.string.invoice_title, sale.getInvoiceNo()) + "\n"
                        + getString(R.string.invoice_total, BanglaNumber.format(sale.getTotal())))
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.view_invoice, (d, w) -> SaleFormat.showInvoice(requireContext(), sale))
                .show();
    }

    private void addRow(LinearLayout parent, AdminRow row, View.OnClickListener onClick) {
        View view = getLayoutInflater().inflate(R.layout.item_admin_row, parent, false);
        AdminRowAdapter.bind(view, row);
        view.setOnClickListener(onClick);
        parent.addView(view);
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
