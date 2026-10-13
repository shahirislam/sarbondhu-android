package com.sarbondhu.app.data.model;

import java.util.Collections;
import java.util.List;

public class Fertilizer {

    private int id;
    private String name;
    private String category;
    private String npkRatio;
    private String description;
    private String usage;
    private List<String> recommendedCrops;
    private double unitPrice;
    private String unit;

    public Fertilizer() {
    }

    public Fertilizer(int id, String name, String category, String npkRatio, String description,
                      String usage, List<String> recommendedCrops, double unitPrice, String unit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.npkRatio = npkRatio;
        this.description = description;
        this.usage = usage;
        this.recommendedCrops = recommendedCrops;
        this.unitPrice = unitPrice;
        this.unit = unit;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return orEmpty(name);
    }

    public String getCategory() {
        return orEmpty(category);
    }

    public String getNpkRatio() {
        return orEmpty(npkRatio);
    }

    public String getDescription() {
        return orEmpty(description);
    }

    public String getUsage() {
        return orEmpty(usage);
    }

    public List<String> getRecommendedCrops() {
        return recommendedCrops == null ? Collections.emptyList() : recommendedCrops;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public String getUnit() {
        return orEmpty(unit);
    }

    public Fertilizer withId(int newId) {
        return new Fertilizer(newId, name, category, npkRatio, description, usage,
                recommendedCrops, unitPrice, unit);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
