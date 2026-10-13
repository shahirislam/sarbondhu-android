package com.sarbondhu.app.ui.admin;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sarbondhu.app.R;

import java.util.Objects;

/** Card rows with title, subtitle and an optional coloured badge. */
public class AdminRowAdapter extends ListAdapter<AdminRow, AdminRowAdapter.ViewHolder> {

    public interface OnRowClick {
        void onClick(int id);
    }

    private static final DiffUtil.ItemCallback<AdminRow> DIFF = new DiffUtil.ItemCallback<AdminRow>() {
        @Override
        public boolean areItemsTheSame(@NonNull AdminRow a, @NonNull AdminRow b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull AdminRow a, @NonNull AdminRow b) {
            return a.title.equals(b.title) && a.subtitle.equals(b.subtitle)
                    && Objects.equals(a.badgeText, b.badgeText) && a.badgeColor == b.badgeColor;
        }
    };

    private final OnRowClick onClick;

    public AdminRowAdapter(OnRowClick onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminRow row = getItem(position);
        bind(holder.itemView, row);
        holder.itemView.setOnClickListener(onClick == null ? null : v -> onClick.onClick(row.id));
    }

    /** Also used to fill rows inflated outside a RecyclerView (dashboard lists). */
    public static void bind(View view, AdminRow row) {
        ((TextView) view.findViewById(R.id.row_title)).setText(row.title);
        TextView subtitle = view.findViewById(R.id.row_subtitle);
        subtitle.setText(row.subtitle);
        subtitle.setVisibility(row.subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        TextView badge = view.findViewById(R.id.row_badge);
        if (row.badgeText == null) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.VISIBLE);
            badge.setText(row.badgeText);
            badge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(view.getContext(), row.badgeColor)));
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
