package com.example.tripexpense;

public class Trip {
    private String id;
    private String name;
    private String shareCode; // THE NEW JOIN CODE FEATURE!
    private String memberNames;
    private double totalExpense;
    private int memberCount;

    // 1. REQUIRED BY FIREBASE
    public Trip() {}

    // 2. Constructor for creating a brand new trip
    public Trip(String id, String name, String shareCode) {
        this.id = id;
        this.name = name;
        this.shareCode = shareCode;
        this.memberNames = "";
        this.totalExpense = 0.0;
        this.memberCount = 0;
    }

    // 3. Constructor for reading existing trips with full details
    public Trip(String id, String name, String shareCode, String memberNames, double totalExpense, int memberCount) {
        this.id = id;
        this.name = name;
        this.shareCode = shareCode;
        this.memberNames = memberNames;
        this.totalExpense = totalExpense;
        this.memberCount = memberCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getShareCode() { return shareCode; }
    public String getMemberNames() { return memberNames; }
    public double getTotalExpense() { return totalExpense; }
    public int getMemberCount() { return memberCount; }
}
