package com.sarbondhu.app.data.model;

public class Admin {

    private int id;
    private String username;
    private String password;
    private String name;

    public Admin() {
    }

    public Admin(int id, String username, String password, String name) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username == null ? "" : username;
    }

    /** Plain text: this is mock data, never real credentials. */
    public String getPassword() {
        return password == null ? "" : password;
    }

    public String getName() {
        return name == null ? "" : name;
    }
}
