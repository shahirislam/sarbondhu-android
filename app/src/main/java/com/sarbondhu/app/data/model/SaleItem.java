package com.sarbondhu.app.data.model;

/**
 * One line of a sale. Name, unit and price are a snapshot taken when the sale was made,
 * so later price changes or a deleted fertilizer don't rewrite the invoice.
 */
public class SaleItem {

    private int fertilizerId;
    private double quantity;
    private double unitPrice;
    private String fertilizerName;
    private String unit;

    public SaleItem() {
    }

    public SaleItem(int fertilizerId, double quantity, double unitPrice) {
        this(fertilizerId, quantity, unitPrice, null, null);
    }

    public SaleItem(int fertilizerId, double quantity, double unitPrice,
                    String fertilizerName, String unit) {
        this.fertilizerId = fertilizerId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.fertilizerName = fertilizerName;
        this.unit = unit;
    }

    public int getFertilizerId() {
        return fertilizerId;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public String getFertilizerName() {
        return fertilizerName == null ? "" : fertilizerName;
    }

    public String getUnit() {
        return unit == null ? "" : unit;
    }

    public double getLineTotal() {
        return quantity * unitPrice;
    }

    public boolean hasSnapshot() {
        return fertilizerName != null;
    }

    public SaleItem withSnapshot(String name, String newUnit) {
        return new SaleItem(fertilizerId, quantity, unitPrice, name, newUnit);
    }
}
