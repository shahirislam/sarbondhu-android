package com.sarbondhu.app.ui.fertilizerlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.FertilizerItem;

public class FertilizerAdapter extends ListAdapter<FertilizerItem, FertilizerAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FertilizerItem item);
    }

    private static final DiffUtil.ItemCallback<FertilizerItem> DIFF =
            new DiffUtil.ItemCallback<FertilizerItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull FertilizerItem a, @NonNull FertilizerItem b) {
                    return a.getFertilizer().getId() == b.getFertilizer().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull FertilizerItem a, @NonNull FertilizerItem b) {
                    return a.getFertilizer().getName().equals(b.getFertilizer().getName());
                }
            };

    private final OnItemClickListener listener;

    public FertilizerAdapter(OnItemClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fertilizer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FertilizerItem item = getItem(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
        }

        void bind(FertilizerItem item) {
            name.setText(item.getFertilizer().getName());
        }
    }
}
