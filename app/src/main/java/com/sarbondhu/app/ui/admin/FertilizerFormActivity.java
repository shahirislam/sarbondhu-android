package com.sarbondhu.app.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.session.Session;
import com.sarbondhu.app.util.BanglaNumber;

import java.util.Map;

public class FertilizerFormActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "fertilizer_id";

    private FertilizerFormViewModel viewModel;
    private EditText name;
    private EditText category;
    private EditText npk;
    private EditText price;
    private EditText unit;
    private EditText description;
    private EditText usage;
    private EditText crops;

    /** @param fertilizerId 0 to add a new fertilizer */
    public static Intent newIntent(Context context, int fertilizerId) {
        return new Intent(context, FertilizerFormActivity.class).putExtra(EXTRA_ID, fertilizerId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.get().isAdmin()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_fertilizer_form);

        name = findViewById(R.id.name);
        category = findViewById(R.id.category);
        npk = findViewById(R.id.npk);
        price = findViewById(R.id.price);
        unit = findViewById(R.id.unit);
        description = findViewById(R.id.description);
        usage = findViewById(R.id.usage);
        crops = findViewById(R.id.crops);

        int id = getIntent().getIntExtra(EXTRA_ID, 0);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(id == 0 ? R.string.add_fertilizer : R.string.edit_fertilizer);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (id != 0) {
            toolbar.inflateMenu(R.menu.form_delete);
            toolbar.setOnMenuItemClickListener(item -> {
                confirmDelete();
                return true;
            });
        }
        if (savedInstanceState == null && id == 0) {
            unit.setText(R.string.default_unit);
        }

        viewModel = new ViewModelProvider(this).get(FertilizerFormViewModel.class);
        // Fill once; after rotation the fields restore themselves.
        boolean restoring = savedInstanceState != null;
        viewModel.getExisting().observe(this, f -> {
            if (!restoring) {
                fill(f);
            }
        });
        viewModel.getFieldErrors().observe(this, this::showErrors);
        viewModel.getSaving().observe(this, busy -> findViewById(R.id.save_button).setEnabled(!busy));
        viewModel.getDone().observe(this, event -> {
            Integer message = event.take();
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        viewModel.getFailure().observe(this, event -> {
            Integer message = event.take();
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.start(id);

        findViewById(R.id.save_button).setOnClickListener(v -> viewModel.save(
                text(name), text(category), text(npk), text(price), text(unit),
                text(description), text(usage), text(crops)));
    }

    private void fill(Fertilizer f) {
        name.setText(f.getName());
        category.setText(f.getCategory());
        npk.setText(f.getNpkRatio());
        price.setText(BanglaNumber.plain(f.getUnitPrice()));
        unit.setText(f.getUnit());
        description.setText(f.getDescription());
        usage.setText(f.getUsage());
        crops.setText(String.join(", ", f.getRecommendedCrops()));
    }

    private void showErrors(Map<FertilizerFormViewModel.Field, Integer> errors) {
        setError(R.id.name_layout, errors.get(FertilizerFormViewModel.Field.NAME));
        setError(R.id.category_layout, errors.get(FertilizerFormViewModel.Field.CATEGORY));
        setError(R.id.price_layout, errors.get(FertilizerFormViewModel.Field.PRICE));
        setError(R.id.unit_layout, errors.get(FertilizerFormViewModel.Field.UNIT));
    }

    private void setError(int layoutId, Integer message) {
        TextInputLayout layout = findViewById(layoutId);
        layout.setError(message == null ? null : getString(message));
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.confirm_delete_fertilizer, text(name)))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> viewModel.delete())
                .show();
    }

    static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
