package com.example.tripexpense;

public class Trip {
    private int id;
    private String name;

    public Trip(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}
