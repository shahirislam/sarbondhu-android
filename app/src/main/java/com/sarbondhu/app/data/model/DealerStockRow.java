package com.sarbondhu.app.data.model;

/** One fertilizer as held by one dealer (quantity 0 when the dealer has none). */
public class DealerStockRow {

    private final Fertilizer fertilizer;
    private final double quantity;
    private final double sellingPrice;
    private final boolean hasStock;

    public DealerStockRow(Fertilizer fertilizer, double quantity, double sellingPrice, boolean hasStock) {
        this.fertilizer = fertilizer;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
        this.hasStock = hasStock;
    }

    public Fertilizer getFertilizer() {
        return fertilizer;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    /** True when the dealer has a stock row for this fertilizer, even at quantity 0. */
    public boolean hasStock() {
        return hasStock;
    }
}
