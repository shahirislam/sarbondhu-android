package com.sarbondhu.app.ui.fertilizerdetail;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.util.BanglaNumber;

public class ShopStockAdapter extends ListAdapter<ShopStock, ShopStockAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<ShopStock> DIFF =
            new DiffUtil.ItemCallback<ShopStock>() {
                @Override
                public boolean areItemsTheSame(@NonNull ShopStock a, @NonNull ShopStock b) {
                    return a.getShop().getId() == b.getShop().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ShopStock a, @NonNull ShopStock b) {
                    return a.getQuantity() == b.getQuantity()
                            && a.getSellingPrice() == b.getSellingPrice();
                }
            };

    private final String unit;

    public ShopStockAdapter(String unit) {
        super(DIFF);
        this.unit = unit;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), unit);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView shopName;
        private final TextView owner;
        private final TextView address;
        private final TextView sellingPrice;
        private final TextView stockBadge;
        private final ImageButton callButton;

        ViewHolder(View itemView) {
            super(itemView);
            shopName = itemView.findViewById(R.id.shop_name);
            owner = itemView.findViewById(R.id.owner);
            address = itemView.findViewById(R.id.address);
            sellingPrice = itemView.findViewById(R.id.selling_price);
            stockBadge = itemView.findViewById(R.id.stock_badge);
            callButton = itemView.findViewById(R.id.call_button);
        }

        void bind(ShopStock shopStock, String unit) {
            Context context = itemView.getContext();
            Shop shop = shopStock.getShop();

            shopName.setText(shop.getShopName());
            owner.setText(shop.getOwnerName());
            address.setText(context.getString(R.string.location, shop.getAddress(), shop.getDistrict()));
            sellingPrice.setText(context.getString(R.string.shop_price,
                    BanglaNumber.format(shopStock.getSellingPrice()), unit));

            ShopStock.Status status = shopStock.getStatus();
            stockBadge.setText(status == ShopStock.Status.OUT_OF_STOCK
                    ? context.getString(R.string.stock_out)
                    : context.getString(R.string.stock_quantity,
                            BanglaNumber.format(shopStock.getQuantity()), unit));
            stockBadge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, badgeColor(status))));

            String phone = shop.getPhone().trim();
            if (phone.isEmpty()) {
                callButton.setVisibility(View.GONE);
            } else {
                callButton.setVisibility(View.VISIBLE);
                callButton.setContentDescription(
                        context.getString(R.string.call_shop, shop.getShopName()));
                callButton.setOnClickListener(v -> {
                    try {
                        context.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                    } catch (ActivityNotFoundException ignored) {
                        // Device has no dialer (e.g. tablet); nothing to do.
                    }
                });
            }
        }

        @ColorRes
        private static int badgeColor(ShopStock.Status status) {
            switch (status) {
                case IN_STOCK:
                    return R.color.stock_in;
                case LOW_STOCK:
                    return R.color.stock_low;
                default:
                    return R.color.stock_out;
            }
        }
    }
}
