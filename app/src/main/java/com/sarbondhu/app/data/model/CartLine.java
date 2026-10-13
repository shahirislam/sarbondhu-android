package com.sarbondhu.app.data.model;

/** A fertilizer and quantity the dealer wants to sell. */
public class CartLine {

    private final int fertilizerId;
    private final double quantity;

    public CartLine(int fertilizerId, double quantity) {
        this.fertilizerId = fertilizerId;
        this.quantity = quantity;
    }

    public int getFertilizerId() {
        return fertilizerId;
    }

    public double getQuantity() {
        return quantity;
    }
}
