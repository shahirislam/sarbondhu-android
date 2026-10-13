package com.sarbondhu.app.ui.dealer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sarbondhu.app.R;
import com.sarbondhu.app.session.Session;

/** Dealer area: four sections behind bottom navigation. Requires a dealer session. */
public class DealerActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.get().isDealer()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_dealer);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle(Session.get().getDisplayName());
        toolbar.inflateMenu(R.menu.admin_toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                Session.get().logout();
                finish();
                return true;
            }
            return false;
        });

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_dealer_dashboard);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // The selected tab is restored after onCreate, so set the title here.
        if (bottomNav != null) {
            toolbar.setTitle(bottomNav.getMenu().findItem(bottomNav.getSelectedItemId()).getTitle());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Session.get().isDealer()) {
            finish();
        }
    }

    private void show(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_new_sale) {
            fragment = new NewSaleFragment();
        } else if (itemId == R.id.nav_history) {
            fragment = new SalesHistoryFragment();
        } else if (itemId == R.id.nav_profile) {
            fragment = new ProfileFragment();
        } else {
            fragment = new DealerDashboardFragment();
        }
        toolbar.setTitle(bottomNav.getMenu().findItem(itemId).getTitle());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.dealer_content, fragment)
                .commit();
    }
}
