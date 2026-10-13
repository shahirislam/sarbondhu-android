package com.sarbondhu.app.ui.dealer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.ui.admin.AdminLists;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaNumber;

import java.util.Map;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        EditText current = view.findViewById(R.id.current_password);
        EditText newPassword = view.findViewById(R.id.new_password);
        EditText confirm = view.findViewById(R.id.confirm_password);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            AdminLists.renderStatus(view, state);
            if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                ((TextView) view.findViewById(R.id.details)).setText(details(state.getData()));
            }
        });
        viewModel.getFieldErrors().observe(getViewLifecycleOwner(), errors -> showErrors(view, errors));
        viewModel.getMessage().observe(getViewLifecycleOwner(), event -> {
            Integer text = event.take();
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getChanged().observe(getViewLifecycleOwner(), event -> {
            if (event.take() != null) {
                current.setText("");
                newPassword.setText("");
                confirm.setText("");
            }
        });
        view.findViewById(R.id.change_password).setOnClickListener(v -> viewModel.changePassword(
                text(current), text(newPassword), text(confirm)));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private String details(Shop shop) {
        return line(R.string.field_shop_name, shop.getShopName())
                + line(R.string.field_owner_name, shop.getOwnerName())
                + line(R.string.field_phone, BanglaNumber.toBangla(shop.getPhone()))
                + line(R.string.field_address, shop.getAddress())
                + line(R.string.field_upazila, shop.getUpazila())
                + line(R.string.field_district, shop.getDistrict())
                + getString(R.string.profile_line, getString(R.string.username), shop.getUsername());
    }

    private String line(@StringRes int label, String value) {
        return getString(R.string.profile_line, getString(label), value) + "\n";
    }

    private static void showErrors(View root, Map<ProfileViewModel.Field, Integer> errors) {
        setError(root, R.id.current_password_layout, errors.get(ProfileViewModel.Field.CURRENT));
        setError(root, R.id.new_password_layout, errors.get(ProfileViewModel.Field.NEW));
        setError(root, R.id.confirm_password_layout, errors.get(ProfileViewModel.Field.CONFIRM));
    }

    private static void setError(View root, int layoutId, Integer message) {
        TextInputLayout layout = root.findViewById(layoutId);
        layout.setError(message == null ? null : root.getContext().getString(message));
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
