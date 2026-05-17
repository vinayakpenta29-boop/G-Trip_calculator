package com.example.tripexpense;

public class Member {
    private int id;
    private String name;

    public Member(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    // This makes the Spinner (dropdown menu) display the person's name
    @Override
    public String toString() {
        return name;
    }
}
