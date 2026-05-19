package com.example.tripexpense;

public class Trip {
    private int id;
    private String name;
    private String memberNames;
    private double totalExpense;
    private int memberCount; // Changed from expenseCount

    public Trip(int id, String name) {
        this.id = id;
        this.name = name;
        this.memberNames = "";
        this.totalExpense = 0.0;
        this.memberCount = 0;
    }

    public Trip(int id, String name, String memberNames, double totalExpense, int memberCount) {
        this.id = id;
        this.name = name;
        this.memberNames = memberNames;
        this.totalExpense = totalExpense;
        this.memberCount = memberCount;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getMemberNames() { return memberNames; }
    public double getTotalExpense() { return totalExpense; }
    public int getMemberCount() { return memberCount; } // Changed getter
}
