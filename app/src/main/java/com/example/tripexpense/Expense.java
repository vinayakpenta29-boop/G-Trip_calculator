package com.example.tripexpense;

import java.util.List;

public class Expense {
    private int id;
    private String title;
    private double amount;
    private int payerId;
    private String payerName;
    private List<Member> involvedMembers;

    public Expense(int id, String title, double amount, int payerId, String payerName, List<Member> involvedMembers) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.payerId = payerId;
        this.payerName = payerName;
        this.involvedMembers = involvedMembers;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public int getPayerId() { return payerId; }
    public String getPayerName() { return payerName; }
    public List<Member> getInvolvedMembers() { return involvedMembers; }
}
