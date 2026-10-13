package com.sarbondhu.app.data.model;

import java.util.Collections;
import java.util.List;

/**
 * One order. In mock_data.json the date is relative — {@code minutesAgo} for today's sales,
 * {@code daysAgo} + {@code time} for older ones — and is turned into {@code timestamp} when the
 * store loads, so seeded "today" sales are always today and never in the future.
 */
public class Sale {

    private int id;
    private String invoiceNo;
    private int shopId;
    private String customerName;
    private String customerPhone;
    private Integer minutesAgo;
    private Integer daysAgo;
    private String time;
    private long timestamp;
    private List<SaleItem> items;

    public Sale() {
    }

    /** Seed-style sale with a relative date. */
    public Sale(int id, String invoiceNo, int shopId, String customerName, String customerPhone,
                int daysAgo, String time, List<SaleItem> items) {
        this.id = id;
        this.invoiceNo = invoiceNo;
        this.shopId = shopId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.daysAgo = daysAgo;
        this.time = time;
        this.items = items;
    }

    /** Seed-style sale made a number of minutes before load time (used for "today"). */
    public static Sale minutesAgo(int id, String invoiceNo, int shopId, String customerName,
                                  String customerPhone, int minutes, List<SaleItem> items) {
        Sale sale = new Sale(id, invoiceNo, shopId, customerName, customerPhone, 0, null, items);
        sale.daysAgo = null;
        sale.minutesAgo = minutes;
        return sale;
    }

    /** A sale made now, with an absolute timestamp. */
    public static Sale made(int id, String invoiceNo, int shopId, String customerName,
                            String customerPhone, long timestamp, List<SaleItem> items) {
        Sale sale = new Sale(id, invoiceNo, shopId, customerName, customerPhone, 0, null, items);
        sale.daysAgo = null;
        sale.timestamp = timestamp;
        return sale;
    }

    public int getId() {
        return id;
    }

    public String getInvoiceNo() {
        return invoiceNo == null ? "" : invoiceNo;
    }

    public int getShopId() {
        return shopId;
    }

    public String getCustomerName() {
        return customerName == null ? "" : customerName;
    }

    public String getCustomerPhone() {
        return customerPhone == null ? "" : customerPhone;
    }

    /** Minutes before load time, or null when the seed uses daysAgo/time instead. */
    public Integer getMinutesAgo() {
        return minutesAgo;
    }

    /** Days before load time, or 0 when absent. */
    public int getDaysAgo() {
        return daysAgo == null ? 0 : daysAgo;
    }

    /** "HH:mm" from the seed, or "" when absent. */
    public String getTime() {
        return time == null ? "" : time;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<SaleItem> getItems() {
        return items == null ? Collections.emptyList() : items;
    }

    public double getTotal() {
        double total = 0;
        for (SaleItem item : getItems()) {
            total += item.getLineTotal();
        }
        return total;
    }

    public Sale withResolved(long newTimestamp, List<SaleItem> newItems) {
        return made(id, invoiceNo, shopId, customerName, customerPhone, newTimestamp, newItems);
    }
}
