package com.sarbondhu.app.data.model;

/** A fertilizer shop. Each shop is also a dealer account that can log in. */
public class Shop {

    private int id;
    private String shopName;
    private String ownerName;
    private String phone;
    private String district;
    private String upazila;
    private String address;
    private String username;
    private String password;
    private Boolean active;

    public Shop() {
    }

    public Shop(int id, String shopName, String ownerName, String phone,
                String district, String upazila, String address) {
        this(id, shopName, ownerName, phone, district, upazila, address, null, null, true);
    }

    public Shop(int id, String shopName, String ownerName, String phone, String district,
                String upazila, String address, String username, String password, boolean active) {
        this.id = id;
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.district = district;
        this.upazila = upazila;
        this.address = address;
        this.username = username;
        this.password = password;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getShopName() {
        return orEmpty(shopName);
    }

    public String getOwnerName() {
        return orEmpty(ownerName);
    }

    public String getPhone() {
        return orEmpty(phone);
    }

    public String getDistrict() {
        return orEmpty(district);
    }

    public String getUpazila() {
        return orEmpty(upazila);
    }

    public String getAddress() {
        return orEmpty(address);
    }

    public String getUsername() {
        return orEmpty(username);
    }

    /** Plain text: this is mock data, never real credentials. */
    public String getPassword() {
        return orEmpty(password);
    }

    /** Missing in JSON means active. */
    public boolean isActive() {
        return active == null || active;
    }

    public Shop withId(int newId) {
        return new Shop(newId, shopName, ownerName, phone, district, upazila, address,
                username, password, isActive());
    }

    public Shop withPassword(String newPassword) {
        return new Shop(id, shopName, ownerName, phone, district, upazila, address,
                username, newPassword, isActive());
    }

    public Shop withActive(boolean newActive) {
        return new Shop(id, shopName, ownerName, phone, district, upazila, address,
                username, password, newActive);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
