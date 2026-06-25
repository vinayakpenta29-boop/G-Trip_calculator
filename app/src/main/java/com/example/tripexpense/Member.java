public class Member {
    private String id;
    private String name;
    
    // 🛑 1. ADD THIS NEW VARIABLE
    private boolean active = true; 

    public Member() {}

    public Member(String id, String name) {
        this.id = id;
        this.name = name;
        this.active = true; // Default to active when added
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // 🛑 2. ADD THESE AT THE VERY BOTTOM
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() { return name; }
}
