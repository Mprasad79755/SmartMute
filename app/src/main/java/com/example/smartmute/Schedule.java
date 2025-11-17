package com.example.smartmute;



public class Schedule {
    private int id;
    private String name;
    private String startTime;
    private String endTime;
    private int daysMask;
    private int profileId;
    private boolean enabled;

    public Schedule() {}

    public Schedule(int id, String name, String startTime, String endTime, int daysMask, int profileId, boolean enabled) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.daysMask = daysMask;
        this.profileId = profileId;
        this.enabled = enabled;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getDaysMask() { return daysMask; }
    public void setDaysMask(int daysMask) { this.daysMask = daysMask; }

    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
