package com.example.tripexpense;

public class Trip {
    private int id;
    private String name;
    private String memberNames;
    private double totalExpense;
    private int expenseCount; // For the purple badge

    public Trip(int id, String name, String memberNames, double totalExpense, int expenseCount) {
        this.id = id;
        this.name = name;
        this.memberNames = memberNames;
        this.totalExpense = totalExpense;
        this.expenseCount = expenseCount;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getMemberNames() { return memberNames; }
    public double getTotalExpense() { return totalExpense; }
    public int getExpenseCount() { return expenseCount; }
}
