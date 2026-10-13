package com.sarbondhu.app.ui.fertilizerlist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.HomeData;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.session.Session;
import com.sarbondhu.app.ui.admin.AdminActivity;
import com.sarbondhu.app.ui.dealer.DealerActivity;
import com.sarbondhu.app.ui.fertilizerdetail.FertilizerDetailActivity;
import com.sarbondhu.app.ui.login.LoginActivity;
import com.sarbondhu.app.util.BanglaNumber;

public class FertilizerListActivity extends AppCompatActivity {

    private static final long SLOGAN_INTERVAL_MS = 4000;
    private static final int GRID_COLUMNS = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable nextSlogan = new Runnable() {
        @Override
        public void run() {
            sloganIndex = (sloganIndex + 1) % slogans.length;
            slogan.setText(slogans[sloganIndex]);
            handler.postDelayed(this, SLOGAN_INTERVAL_MS);
        }
    };

    private String[] slogans;
    private int sloganIndex;
    private TextSwitcher slogan;
    private TextView clockTime;
    private TextView clockDay;
    private TextView clockDate;
    private TextView rainProbability;
    private LottieAnimationView skyAnimation;
    private Boolean showingDay;

    private RecyclerView list;
    private ProgressBar progress;
    private TextView emptyText;
    private View errorGroup;
    private TextView errorText;
    private FertilizerAdapter adapter;
    private FertilizerListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fertilizer_list);

        slogans = getResources().getStringArray(R.array.home_slogans);
        slogan = findViewById(R.id.slogan);
        slogan.setCurrentText(slogans[sloganIndex]);
        findViewById(R.id.login_button).setOnClickListener(v -> {
            Class<?> target = Session.get().isAdmin() ? AdminActivity.class
                    : Session.get().isDealer() ? DealerActivity.class : LoginActivity.class;
            startActivity(new Intent(this, target));
        });

        clockTime = findViewById(R.id.clock_time);
        clockDay = findViewById(R.id.clock_day);
        clockDate = findViewById(R.id.clock_date);
        rainProbability = findViewById(R.id.rain_probability);
        skyAnimation = findViewById(R.id.sky_animation);

        list = findViewById(R.id.fertilizer_list);
        progress = findViewById(R.id.progress);
        emptyText = findViewById(R.id.empty_text);
        errorGroup = findViewById(R.id.error_group);
        errorText = findViewById(R.id.error_text);

        adapter = new FertilizerAdapter(item -> startActivity(
                FertilizerDetailActivity.newIntent(this, item.getFertilizer().getId())));
        list.setLayoutManager(new GridLayoutManager(this, GRID_COLUMNS));
        list.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(FertilizerListViewModel.class);
        findViewById(R.id.retry_button).setOnClickListener(v -> viewModel.load());
        viewModel.getClock().observe(this, this::renderClock);
        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.postDelayed(nextSlogan, SLOGAN_INTERVAL_MS);
        // Admin edits (stock, fertilizers, dealers) should show up when returning here.
        viewModel.refresh();
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(nextSlogan);
        super.onStop();
    }

    private void renderClock(ClockState clock) {
        clockTime.setText(clock.getTime());
        clockDay.setText(clock.getDay());
        clockDate.setText(clock.getDate());
        if (showingDay == null || showingDay != clock.isDay()) {
            showingDay = clock.isDay();
            skyAnimation.setAnimation(clock.isDay() ? R.raw.sun : R.raw.moon);
            skyAnimation.playAnimation();
        }
    }

    private void render(UiState<HomeData> state) {
        UiState.Status status = state.getStatus();
        progress.setVisibility(status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        errorGroup.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        if (status == UiState.Status.ERROR) {
            errorText.setText(state.getMessageRes());
        }

        boolean success = status == UiState.Status.SUCCESS;
        boolean empty = success && state.getData().getFertilizers().isEmpty();
        list.setVisibility(success && !empty ? View.VISIBLE : View.GONE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        rainProbability.setVisibility(success ? View.VISIBLE : View.GONE);
        if (success) {
            HomeData data = state.getData();
            adapter.submitList(data.getFertilizers());
            rainProbability.setText(getString(R.string.rain_probability,
                    BanglaNumber.format(data.getWeather().getRainProbability())));
        }
    }
}
