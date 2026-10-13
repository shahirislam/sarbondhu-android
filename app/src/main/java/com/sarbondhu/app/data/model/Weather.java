package com.sarbondhu.app.data.model;

/** Weather shown on the home screen header. */
public class Weather {

    private String location;
    private int rainProbability;

    public Weather() {
    }

    public Weather(String location, int rainProbability) {
        this.location = location;
        this.rainProbability = rainProbability;
    }

    public String getLocation() {
        return location == null ? "" : location;
    }

    /** Chance of rain, 0–100. */
    public int getRainProbability() {
        return rainProbability;
    }
}
