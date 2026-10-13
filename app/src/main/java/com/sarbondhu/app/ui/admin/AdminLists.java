package com.sarbondhu.app.ui.admin;

import android.view.View;
import android.widget.TextView;

import com.sarbondhu.app.R;
import com.sarbondhu.app.ui.common.UiState;

/** Shared loading / error / content visibility for fragment_admin_list. */
public final class AdminLists {

    private AdminLists() {
    }

    public static void renderStatus(View root, UiState<?> state) {
        UiState.Status status = state.getStatus();
        root.findViewById(R.id.progress).setVisibility(
                status == UiState.Status.LOADING ? View.VISIBLE : View.GONE);
        TextView error = root.findViewById(R.id.error_text);
        error.setVisibility(status == UiState.Status.ERROR ? View.VISIBLE : View.GONE);
        if (status == UiState.Status.ERROR) {
            error.setText(state.getMessageRes());
        }
        root.findViewById(R.id.list).setVisibility(
                status == UiState.Status.SUCCESS ? View.VISIBLE : View.GONE);
    }
}
