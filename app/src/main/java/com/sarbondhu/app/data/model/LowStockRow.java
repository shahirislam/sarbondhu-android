package com.sarbondhu.app.data.model;

public class LowStockRow {

    private final Shop shop;
    private final Fertilizer fertilizer;
    private final double quantity;

    public LowStockRow(Shop shop, Fertilizer fertilizer, double quantity) {
        this.shop = shop;
        this.fertilizer = fertilizer;
        this.quantity = quantity;
    }

    public Shop getShop() {
        return shop;
    }

    public Fertilizer getFertilizer() {
        return fertilizer;
    }

    public double getQuantity() {
        return quantity;
    }
}
