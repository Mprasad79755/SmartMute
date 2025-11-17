package com.example.smartmute;


public class SmartLocation {
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private int radius;
    private int profileId;
    private int revertProfileId;
    private boolean enabled;

    public SmartLocation() {}

    public SmartLocation(int id, String name, double latitude, double longitude,
                         int radius, int profileId, int revertProfileId, boolean enabled) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.profileId = profileId;
        this.revertProfileId = revertProfileId;
        this.enabled = enabled;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; }

    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public int getRevertProfileId() { return revertProfileId; }
    public void setRevertProfileId(int revertProfileId) { this.revertProfileId = revertProfileId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}