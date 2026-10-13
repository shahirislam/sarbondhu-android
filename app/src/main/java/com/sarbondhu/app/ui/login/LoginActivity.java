package com.sarbondhu.app.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.repository.AuthRepository.Role;
import com.sarbondhu.app.ui.admin.AdminActivity;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.ui.dealer.DealerActivity;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private MaterialButtonToggleGroup roleGroup;
    private EditText username;
    private EditText password;
    private TextView errorText;
    private ProgressBar progress;
    private View submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        roleGroup = findViewById(R.id.role_group);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        errorText = findViewById(R.id.error_text);
        progress = findViewById(R.id.progress);
        submit = findViewById(R.id.login_submit);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        viewModel.getState().observe(this, this::render);
        viewModel.getLoggedIn().observe(this, event -> {
            Role role = event.take();
            if (role != null) {
                startActivity(new Intent(this,
                        role == Role.ADMIN ? AdminActivity.class : DealerActivity.class));
                finish();
            }
        });

        submit.setOnClickListener(v -> submit());
        password.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
    }

    private void submit() {
        // Hide the keyboard so an error message below the fields is visible.
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
        }
        Role role = roleGroup.getCheckedButtonId() == R.id.role_admin ? Role.ADMIN : Role.DEALER;
        viewModel.login(role, username.getText().toString(), password.getText().toString());
    }

    private void render(UiState<Role> state) {
        boolean loading = state.getStatus() == UiState.Status.LOADING;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        submit.setEnabled(!loading);
        boolean error = state.getStatus() == UiState.Status.ERROR;
        errorText.setVisibility(error ? View.VISIBLE : View.GONE);
        if (error) {
            errorText.setText(state.getMessageRes());
        }
    }
}
