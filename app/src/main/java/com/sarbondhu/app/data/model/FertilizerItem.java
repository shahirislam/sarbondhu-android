package com.sarbondhu.app.data.model;

/** One row of the fertilizer list. */
public class FertilizerItem {

    private final Fertilizer fertilizer;
    private final int shopCount;

    public FertilizerItem(Fertilizer fertilizer, int shopCount) {
        this.fertilizer = fertilizer;
        this.shopCount = shopCount;
    }

    public Fertilizer getFertilizer() {
        return fertilizer;
    }

    /** Number of shops that currently have this fertilizer in stock. */
    public int getShopCount() {
        return shopCount;
    }
}
