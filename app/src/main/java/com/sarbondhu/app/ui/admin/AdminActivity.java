package com.sarbondhu.app.ui.admin;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sarbondhu.app.R;
import com.sarbondhu.app.session.Session;

/** Admin area: four sections behind bottom navigation. Requires an admin session. */
public class AdminActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.get().isAdmin()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_admin);

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
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
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
        if (!Session.get().isAdmin()) {
            finish();
        }
    }

    private void show(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_fertilizers) {
            fragment = new FertilizerAdminFragment();
        } else if (itemId == R.id.nav_dealers) {
            fragment = new DealerAdminFragment();
        } else if (itemId == R.id.nav_stock) {
            fragment = new StockAdminFragment();
        } else {
            fragment = new DashboardFragment();
        }
        toolbar.setTitle(bottomNav.getMenu().findItem(itemId).getTitle());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.admin_content, fragment)
                .commit();
    }
}
