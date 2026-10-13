package com.sarbondhu.app.data.model;

/** How much of one fertilizer one shop has. */
public class Stock {

    private int shopId;
    private int fertilizerId;
    private double quantity;
    private double sellingPrice;

    public Stock() {
    }

    public Stock(int shopId, int fertilizerId, double quantity, double sellingPrice) {
        this.shopId = shopId;
        this.fertilizerId = fertilizerId;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
    }

    public int getShopId() {
        return shopId;
    }

    public int getFertilizerId() {
        return fertilizerId;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }
}
