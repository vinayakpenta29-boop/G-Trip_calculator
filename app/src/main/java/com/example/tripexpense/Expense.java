package com.example.tripexpense;

import java.util.List;

public class Expense {
    private String id;
    private String title;
    private double amount;
    private String payerId;
    private String payerName;
    private List<Member> involvedMembers;
    private String category;
    private long timestamp;

    // 1. REQUIRED BY FIREBASE
    public Expense() {}

    // 2. Standard Constructor
    public Expense(String id, String title, double amount, String payerId, String payerName, List<Member> involvedMembers) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.payerId = payerId;
        this.payerName = payerName;
        this.involvedMembers = involvedMembers;
        this.category = category;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getPayerId() { return payerId; }
    public String getPayerName() { return payerName; }
    public List<Member> getInvolvedMembers() { return involvedMembers; }

    public String getCategory() { return category; }

    public long getTimestamp() { 
        return timestamp; 
    }

    public void setCategory(String category) { this.category = category; }
    
    public void setTimestamp(long timestamp) { 
        this.timestamp = timestamp; 
    }
}
