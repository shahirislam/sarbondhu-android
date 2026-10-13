package com.sarbondhu.app.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShopStockTest {

    private static ShopStock withQuantity(double quantity) {
        return new ShopStock(new Shop(1, "s", "o", "017", "d", "u", "a"), quantity, 20.0);
    }

    @Test
    public void zeroIsOutOfStock() {
        assertEquals(ShopStock.Status.OUT_OF_STOCK, withQuantity(0).getStatus());
    }

    @Test
    public void belowThresholdIsLowStock() {
        assertEquals(ShopStock.Status.LOW_STOCK, withQuantity(99.5).getStatus());
    }

    @Test
    public void atThresholdIsInStock() {
        assertEquals(ShopStock.Status.IN_STOCK, withQuantity(ShopStock.LOW_STOCK_THRESHOLD).getStatus());
    }
}
