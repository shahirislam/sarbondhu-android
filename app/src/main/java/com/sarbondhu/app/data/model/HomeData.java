package com.sarbondhu.app.data.model;

import java.util.List;

/** Everything the home screen shows. */
public class HomeData {

    private final List<FertilizerItem> fertilizers;
    private final Weather weather;

    public HomeData(List<FertilizerItem> fertilizers, Weather weather) {
        this.fertilizers = fertilizers;
        this.weather = weather;
    }

    public List<FertilizerItem> getFertilizers() {
        return fertilizers;
    }

    public Weather getWeather() {
        return weather;
    }
}
