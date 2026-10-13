package com.sarbondhu.app.data.model;

import java.util.List;

/** Everything the detail screen shows. */
public class FertilizerDetail {

    private final Fertilizer fertilizer;
    private final List<ShopStock> shopStocks;

    public FertilizerDetail(Fertilizer fertilizer, List<ShopStock> shopStocks) {
        this.fertilizer = fertilizer;
        this.shopStocks = shopStocks;
    }

    public Fertilizer getFertilizer() {
        return fertilizer;
    }

    public List<ShopStock> getShopStocks() {
        return shopStocks;
    }
}
