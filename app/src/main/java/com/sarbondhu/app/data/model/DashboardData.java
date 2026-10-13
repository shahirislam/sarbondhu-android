package com.sarbondhu.app.data.model;

import java.util.List;

/** Everything the admin dashboard shows. */
public class DashboardData {

    private final int fertilizerCount;
    private final int activeDealerCount;
    private final double totalStock;
    private final List<LowStockRow> lowStock;
    private final List<ActivityEntry> recentActivity;

    public DashboardData(int fertilizerCount, int activeDealerCount, double totalStock,
                         List<LowStockRow> lowStock, List<ActivityEntry> recentActivity) {
        this.fertilizerCount = fertilizerCount;
        this.activeDealerCount = activeDealerCount;
        this.totalStock = totalStock;
        this.lowStock = lowStock;
        this.recentActivity = recentActivity;
    }

    public int getFertilizerCount() {
        return fertilizerCount;
    }

    public int getActiveDealerCount() {
        return activeDealerCount;
    }

    public double getTotalStock() {
        return totalStock;
    }

    public List<LowStockRow> getLowStock() {
        return lowStock;
    }

    public List<ActivityEntry> getRecentActivity() {
        return recentActivity;
    }
}
