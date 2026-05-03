package com.worldbank.app.models;


public class Biller {
    private String id;
    private String name;
    private String category; // e.g., "Electricity", "Internet", "Subscription"
    private int logoRes;

    public Biller(String id, String name, String category, int logoRes) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.logoRes = logoRes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getLogoRes() { return logoRes; }
}
