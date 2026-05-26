package com.example.tripexpense;

import java.util.List;

public class Expense {
    private String id;
    private String title;
    private double amount;
    private String payerId;
    private String payerName;
    private List<Member> involvedMembers;
    private long timestamp;
    private String category;
    private String receiptUrl = "";

    // Empty constructor (Required by Firebase)
    public Expense() {} 

    // 🛑 UPDATED: The constructor now accepts 7 arguments (added category at the end)
    public Expense(String id, String title, double amount, String payerId, String payerName, List<Member> involvedMembers, String category) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.payerId = payerId;
        this.payerName = payerName;
        this.involvedMembers = involvedMembers;
        this.category = category;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }

    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }

    public List<Member> getInvolvedMembers() { return involvedMembers; }
    public void setInvolvedMembers(List<Member> involvedMembers) { this.involvedMembers = involvedMembers; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // 🛑 NEW: Getter and Setter for the category
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
}
