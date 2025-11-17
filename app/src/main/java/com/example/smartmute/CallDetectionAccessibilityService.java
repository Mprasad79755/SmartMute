package com.example.smartmute;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class CallDetectionAccessibilityService extends AccessibilityService {
    private static final String TAG = "AccessibilityCallDetect";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String className = event.getClassName().toString();
            Log.d(TAG, "Window changed: " + className);

            // Detect incoming call screen
            if (className.contains("InCallScreen") ||
                    className.contains("Dialer") ||
                    className.contains("Call") ||
                    className.contains("incallui")) {

                Log.i(TAG, "Incoming call detected via accessibility");
                getCallerInfo();
            }
        }
    }

    private void getCallerInfo() {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Look for caller information in the UI
                List<AccessibilityNodeInfo> callerInfo = rootNode.findAccessibilityNodeInfosByViewId("com.android.dialer:id/contact_name");
                if (callerInfo != null && !callerInfo.isEmpty()) {
                    String callerName = callerInfo.get(0).getText().toString();
                    Log.i(TAG, "Caller name: " + callerName);
                    // You can match this with your emergency contacts
                }

                List<AccessibilityNodeInfo> callerNumber = rootNode.findAccessibilityNodeInfosByViewId("com.android.dialer:id/phone_number");
                if (callerNumber != null && !callerNumber.isEmpty()) {
                    String phoneNumber = callerNumber.get(0).getText().toString();
                    Log.i(TAG, "Caller number: " + phoneNumber);
                    handleIncomingCall(phoneNumber);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting caller info: " + e.getMessage());
        }
    }

    private void handleIncomingCall(String phoneNumber) {
        // Use your existing emergency contact logic here
        Log.i(TAG, "Processing call from: " + phoneNumber);
        // Call your existing emergency contact checking logic
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);
        Log.i(TAG, "Accessibility service connected");
    }
}