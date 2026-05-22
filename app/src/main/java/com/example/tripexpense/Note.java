package com.example.tripexpense;

public class Note {
    private String id;
    private String text;
    private String authorName;
    private long timestamp;

    public Note() {} // Empty constructor required by Firebase

    public Note(String id, String text, String authorName, long timestamp) {
        this.id = id;
        this.text = text;
        this.authorName = authorName;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public String getAuthorName() { return authorName; }
    public long getTimestamp() { return timestamp; }
}
