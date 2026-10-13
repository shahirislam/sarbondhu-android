package com.sarbondhu.app.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.session.Session;

import java.util.Map;

public class DealerFormActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "dealer_id";

    private DealerFormViewModel viewModel;
    private EditText shopName;
    private EditText ownerName;
    private EditText phone;
    private EditText district;
    private EditText upazila;
    private EditText address;
    private EditText username;
    private EditText password;
    private MaterialSwitch activeSwitch;

    /** @param dealerId 0 to add a new dealer */
    public static Intent newIntent(Context context, int dealerId) {
        return new Intent(context, DealerFormActivity.class).putExtra(EXTRA_ID, dealerId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.get().isAdmin()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_dealer_form);

        shopName = findViewById(R.id.shop_name);
        ownerName = findViewById(R.id.owner_name);
        phone = findViewById(R.id.phone);
        district = findViewById(R.id.district);
        upazila = findViewById(R.id.upazila);
        address = findViewById(R.id.address);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        activeSwitch = findViewById(R.id.active_switch);

        int id = getIntent().getIntExtra(EXTRA_ID, 0);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(id == 0 ? R.string.add_dealer : R.string.edit_dealer);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (id != 0) {
            // Existing dealers change password only through "reset password".
            findViewById(R.id.password_layout).setVisibility(View.GONE);
            View reset = findViewById(R.id.reset_password_button);
            reset.setVisibility(View.VISIBLE);
            reset.setOnClickListener(v -> showResetPassword());
        }

        viewModel = new ViewModelProvider(this).get(DealerFormViewModel.class);
        boolean restoring = savedInstanceState != null;
        viewModel.getExisting().observe(this, shop -> {
            if (!restoring) {
                fill(shop);
            }
        });
        viewModel.getFieldErrors().observe(this, this::showErrors);
        viewModel.getSaving().observe(this, busy -> findViewById(R.id.save_button).setEnabled(!busy));
        viewModel.getDone().observe(this, event -> {
            Integer text = event.take();
            if (text != null) {
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        viewModel.getMessage().observe(this, event -> {
            Integer text = event.take();
            if (text != null) {
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.start(id);

        findViewById(R.id.save_button).setOnClickListener(v -> viewModel.save(
                FertilizerFormActivity.text(shopName), FertilizerFormActivity.text(ownerName),
                FertilizerFormActivity.text(phone), FertilizerFormActivity.text(district),
                FertilizerFormActivity.text(upazila), FertilizerFormActivity.text(address),
                FertilizerFormActivity.text(username), FertilizerFormActivity.text(password),
                activeSwitch.isChecked()));
    }

    private void fill(Shop shop) {
        shopName.setText(shop.getShopName());
        ownerName.setText(shop.getOwnerName());
        phone.setText(shop.getPhone());
        district.setText(shop.getDistrict());
        upazila.setText(shop.getUpazila());
        address.setText(shop.getAddress());
        username.setText(shop.getUsername());
        activeSwitch.setChecked(shop.isActive());
    }

    private void showErrors(Map<DealerFormViewModel.Field, Integer> errors) {
        setError(R.id.shop_name_layout, errors.get(DealerFormViewModel.Field.SHOP_NAME));
        setError(R.id.owner_name_layout, errors.get(DealerFormViewModel.Field.OWNER_NAME));
        setError(R.id.phone_layout, errors.get(DealerFormViewModel.Field.PHONE));
        setError(R.id.district_layout, errors.get(DealerFormViewModel.Field.DISTRICT));
        setError(R.id.upazila_layout, errors.get(DealerFormViewModel.Field.UPAZILA));
        setError(R.id.username_layout, errors.get(DealerFormViewModel.Field.USERNAME));
        setError(R.id.password_layout, errors.get(DealerFormViewModel.Field.PASSWORD));
    }

    private void setError(int layoutId, Integer text) {
        TextInputLayout layout = findViewById(layoutId);
        layout.setError(text == null ? null : getString(text));
    }

    private void showResetPassword() {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(getString(R.string.new_password));
        layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText input = new TextInputEditText(layout.getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(input);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_password)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int error = viewModel.resetPassword(FertilizerFormActivity.text(input));
                    if (error == 0) {
                        dialog.dismiss();
                    } else {
                        layout.setError(getString(error));
                    }
                }));
        dialog.show();
    }
}
