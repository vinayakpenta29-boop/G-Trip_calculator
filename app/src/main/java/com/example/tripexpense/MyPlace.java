package com.example.tripexpense;

public class MyPlace {
    private String id;
    private String placeName;
    private String state;
    private String city;
    private String villageOrArea;
    private boolean visited;
    private long timestamp;

    public MyPlace() {}

    public MyPlace(String id, String placeName, String state, String city, String villageOrArea, boolean visited, long timestamp) {
        this.id = id;
        this.placeName = placeName;
        this.state = state;
        this.city = city;
        this.villageOrArea = villageOrArea;
        this.visited = visited;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getVillageOrArea() { return villageOrArea; }
    public void setVillageOrArea(String villageOrArea) { this.villageOrArea = villageOrArea; }

    public boolean isVisited() { return visited; }
    public void setVisited(boolean visited) { this.visited = visited; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
