package com.sarbondhu.app.ui.fertilizerdetail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.FertilizerDetail;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaNumber;

public class FertilizerDetailActivity extends AppCompatActivity {

    private static final String EXTRA_FERTILIZER_ID = "fertilizer_id";

    private View content;
    private ProgressBar progress;
    private TextView errorText;
    private TextView name;
    private TextView category;
    private TextView npkRatio;
    private TextView price;
    private TextView description;
    private TextView usage;
    private TextView cropsLabel;
    private ChipGroup cropsGroup;
    private TextView noShopsText;
    private RecyclerView shopList;

    public static Intent newIntent(Context context, int fertilizerId) {
        return new Intent(context, FertilizerDetailActivity.class)
                .putExtra(EXTRA_FERTILIZER_ID, fertilizerId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fertilizer_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        content = findViewById(R.id.content);
        progress = findViewById(R.id.progress);
        errorText = findViewById(R.id.error_text);
        name = findViewById(R.id.name);
        category = findViewById(R.id.category);
        npkRatio = findViewById(R.id.npk_ratio);
        price = findViewById(R.id.price);
        description = findViewById(R.id.description);
        usage = findViewById(R.id.usage);
        cropsLabel = findViewById(R.id.crops_label);
        cropsGroup = findViewById(R.id.crops_group);
        noShopsText = findViewById(R.id.no_shops_text);
        shopList = findViewById(R.id.shop_list);
        shopList.setLayoutManager(new LinearLayoutManager(this));

        FertilizerDetailViewModel viewModel =
                new ViewModelProvider(this).get(FertilizerDetailViewModel.class);
        viewModel.getState().observe(this, this::render);
        viewModel.load(getIntent().getIntExtra(EXTRA_FERTILIZER_ID, -1));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void render(UiState<FertilizerDetail> state) {
        UiState.Status status = state.getStatus();
        progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        errorText.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        content.setVisibility(status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        if (status == UiState.Status.ERROR) {
            errorText.setText(state.getMessageRes());
        } else if (status == UiState.Status.SUCCESS) {
            bind(state.getData());
        }
    }

    private void bind(FertilizerDetail detail) {
        Fertilizer fertilizer = detail.getFertilizer();
        setTitle(fertilizer.getName());
        name.setText(fertilizer.getName());
        category.setText(getString(R.string.category, fertilizer.getCategory()));
        npkRatio.setText(getString(R.string.npk_ratio, BanglaNumber.toBangla(fertilizer.getNpkRatio())));
        price.setText(getString(R.string.fixed_price,
                BanglaNumber.format(fertilizer.getUnitPrice()), fertilizer.getUnit()));
        description.setText(fertilizer.getDescription());
        usage.setText(fertilizer.getUsage());

        cropsGroup.removeAllViews();
        for (String crop : fertilizer.getRecommendedCrops()) {
            Chip chip = new Chip(this);
            chip.setText(crop);
            chip.setClickable(false);
            cropsGroup.addView(chip);
        }
        boolean hasCrops = !fertilizer.getRecommendedCrops().isEmpty();
        cropsLabel.setVisibility(hasCrops ? View.VISIBLE : View.GONE);
        cropsGroup.setVisibility(hasCrops ? View.VISIBLE : View.GONE);

        boolean hasShops = !detail.getShopStocks().isEmpty();
        noShopsText.setVisibility(hasShops ? View.GONE : View.VISIBLE);
        shopList.setVisibility(hasShops ? View.VISIBLE : View.GONE);
        ShopStockAdapter adapter = new ShopStockAdapter(fertilizer.getUnit());
        shopList.setAdapter(adapter);
        adapter.submitList(detail.getShopStocks());
    }
}
