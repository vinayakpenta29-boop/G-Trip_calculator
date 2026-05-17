package com.example.tripexpense;

public class Expense {
    private int id;
    private String title;
    private double amount;

    public Expense(int id, String title, double amount) {
        this.id = id;
        this.title = title;
        this.amount = amount;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
}
