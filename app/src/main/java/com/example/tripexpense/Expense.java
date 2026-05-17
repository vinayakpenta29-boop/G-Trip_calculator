package com.example.tripexpense;

public class Expense {
    private int id;
    private String title;
    private double amount;
    private int payerId;

    public Expense(int id, String title, double amount, int payerId) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.payerId = payerId;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public int getPayerId() { return payerId; }
}
