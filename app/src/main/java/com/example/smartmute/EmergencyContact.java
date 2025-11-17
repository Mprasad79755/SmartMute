package com.example.smartmute;


public class EmergencyContact {
    private int id;
    private String name;
    private String phoneNumber;
    private int callCountThreshold;
    private int windowMinutes;
    private boolean ringOverride;

    public EmergencyContact() {}

    public EmergencyContact(int id, String name, String phoneNumber,
                            int callCountThreshold, int windowMinutes, boolean ringOverride) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.callCountThreshold = callCountThreshold;
        this.windowMinutes = windowMinutes;
        this.ringOverride = ringOverride;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public int getCallCountThreshold() { return callCountThreshold; }
    public void setCallCountThreshold(int callCountThreshold) { this.callCountThreshold = callCountThreshold; }

    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }

    public boolean isRingOverride() { return ringOverride; }
    public void setRingOverride(boolean ringOverride) { this.ringOverride = ringOverride; }
}