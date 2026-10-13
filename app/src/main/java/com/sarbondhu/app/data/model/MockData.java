package com.sarbondhu.app.data.model;

import java.util.Collections;
import java.util.List;

/** Root object of mock_data.json. */
public class MockData {

    private List<Fertilizer> fertilizers;
    private List<Shop> shops;
    private List<Stock> stocks;
    private Weather weather;
    private List<Admin> admins;
    private List<Sale> sales;

    public MockData() {
    }

    public MockData(List<Fertilizer> fertilizers, List<Shop> shops, List<Stock> stocks) {
        this.fertilizers = fertilizers;
        this.shops = shops;
        this.stocks = stocks;
    }

    public MockData(List<Fertilizer> fertilizers, List<Shop> shops, List<Stock> stocks,
                    Weather weather) {
        this(fertilizers, shops, stocks);
        this.weather = weather;
    }

    public List<Fertilizer> getFertilizers() {
        return fertilizers == null ? Collections.emptyList() : fertilizers;
    }

    public List<Shop> getShops() {
        return shops == null ? Collections.emptyList() : shops;
    }

    public List<Stock> getStocks() {
        return stocks == null ? Collections.emptyList() : stocks;
    }

    public Weather getWeather() {
        return weather == null ? new Weather() : weather;
    }

    public List<Admin> getAdmins() {
        return admins == null ? Collections.emptyList() : admins;
    }

    public void setAdmins(List<Admin> admins) {
        this.admins = admins;
    }

    public List<Sale> getSales() {
        return sales == null ? Collections.emptyList() : sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }
}
