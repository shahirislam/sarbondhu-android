package com.sarbondhu.app.data.model;

import java.util.List;

/** Everything the dealer dashboard shows. */
public class DealerDashboard {

    private final int todayCount;
    private final double todayRevenue;
    private final int monthCount;
    private final double monthRevenue;
    private final List<DealerStockRow> stock;
    private final List<Sale> recentSales;

    public DealerDashboard(int todayCount, double todayRevenue, int monthCount, double monthRevenue,
                           List<DealerStockRow> stock, List<Sale> recentSales) {
        this.todayCount = todayCount;
        this.todayRevenue = todayRevenue;
        this.monthCount = monthCount;
        this.monthRevenue = monthRevenue;
        this.stock = stock;
        this.recentSales = recentSales;
    }

    public int getTodayCount() {
        return todayCount;
    }

    public double getTodayRevenue() {
        return todayRevenue;
    }

    public int getMonthCount() {
        return monthCount;
    }

    public double getMonthRevenue() {
        return monthRevenue;
    }

    /** Only fertilizers this dealer holds a stock row for. */
    public List<DealerStockRow> getStock() {
        return stock;
    }

    public List<Sale> getRecentSales() {
        return recentSales;
    }
}
