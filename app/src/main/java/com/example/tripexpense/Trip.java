package com.example.tripexpense;

import java.util.ArrayList;
import java.util.List;

public class Trip {
    private String id;
    private String name;
    private String shareCode; 
    private String memberNames;
    private double totalExpense;
    private int memberCount;
    private String adminId; 
    private List<String> userIds; // Everyone allowed to see this trip

    public Trip() {} // Required by Firebase

    public Trip(String id, String name, String shareCode, String adminId) {
        this.id = id;
        this.name = name;
        this.shareCode = shareCode;
        this.adminId = adminId;
        this.memberNames = "";
        this.totalExpense = 0.0;
        this.memberCount = 0;
        
        this.userIds = new ArrayList<>();
        this.userIds.add(adminId); // The creator is the first allowed user
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getShareCode() { return shareCode; }
    public String getMemberNames() { return memberNames; }
    public double getTotalExpense() { return totalExpense; }
    public int getMemberCount() { return memberCount; }
    public String getAdminId() { return adminId; }
    public List<String> getUserIds() { return userIds; }
}
