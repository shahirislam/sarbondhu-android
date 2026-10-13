package com.sarbondhu.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.sarbondhu.app.R;
import com.sarbondhu.app.ui.fertilizerlist.FertilizerListActivity;

/** Branded splash, matching the Flutter app: fade in for 2 s, open home after 4 s. */
public class SplashActivity extends AppCompatActivity {

    private static final long ANIMATION_MS = 2000;
    private static final long SPLASH_MS = 4000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openHome = () -> {
        startActivity(new Intent(this, FertilizerListActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        fadeIn(findViewById(R.id.splash_image));
        fadeIn(findViewById(R.id.splash_title));

        View tagline = findViewById(R.id.splash_tagline);
        // Slide down from half its height above, like Flutter's Offset(0, -0.5).
        tagline.post(() -> {
            tagline.setTranslationY(-tagline.getHeight() / 2f);
            tagline.animate().translationY(0).setDuration(ANIMATION_MS)
                    .setInterpolator(new DecelerateInterpolator()).start();
            fadeIn(tagline);
        });

        handler.postDelayed(openHome, SPLASH_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openHome);
        super.onDestroy();
    }

    private static void fadeIn(View view) {
        view.animate().alpha(1f).setDuration(ANIMATION_MS)
                .setInterpolator(new AccelerateInterpolator()).start();
    }
}
