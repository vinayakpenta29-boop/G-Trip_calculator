package com.example.tripexpense;

public class Member {
    private String id;
    private String name;

    // 1. REQUIRED BY FIREBASE
    public Member() {} 

    // 2. Standard Constructor
    public Member(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
