package com.example.smartmute;

import java.util.Date;

public class CallLog {
    private int id;
    private String phoneNumber;
    private Date callTime;
    private String callType; // INCOMING, OUTGOING, MISSED

    public CallLog() {}

    public CallLog(int id, String phoneNumber, Date callTime, String callType) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.callTime = callTime;
        this.callType = callType;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Date getCallTime() { return callTime; }
    public void setCallTime(Date callTime) { this.callTime = callTime; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }
}