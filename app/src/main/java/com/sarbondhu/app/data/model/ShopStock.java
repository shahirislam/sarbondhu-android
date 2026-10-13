package com.sarbondhu.app.data.model;

/** A shop together with its stock of one particular fertilizer. */
public class ShopStock {

    public enum Status { IN_STOCK, LOW_STOCK, OUT_OF_STOCK }

    /** Quantities below this are shown as low stock. */
    public static final double LOW_STOCK_THRESHOLD = 100;

    private final Shop shop;
    private final double quantity;
    private final double sellingPrice;

    public ShopStock(Shop shop, double quantity, double sellingPrice) {
        this.shop = shop;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
    }

    public Shop getShop() {
        return shop;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public boolean isInStock() {
        return quantity > 0;
    }

    public Status getStatus() {
        if (!isInStock()) {
            return Status.OUT_OF_STOCK;
        }
        return quantity < LOW_STOCK_THRESHOLD ? Status.LOW_STOCK : Status.IN_STOCK;
    }
}
