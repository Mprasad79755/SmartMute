package com.example.smartmute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

public class PhoneStateReceiver extends BroadcastReceiver {

    private static final String TAG = "PhoneStateReceiver";

    // Track call timestamps for each emergency contact
    private static final Map<String, CallTracker> callTrackers = new HashMap<>();
    private static String lastIncomingNumber = "";

    private static class CallTracker {
        private int callCount = 0;
        private long firstCallTime = 0;

        public void addCall() {
            long currentTime = System.currentTimeMillis();
            if (callCount == 0) {
                firstCallTime = currentTime;
            }
            callCount++;
        }

        public boolean shouldTrigger(int threshold, int windowMinutes) {
            long currentTime = System.currentTimeMillis();
            long windowMillis = windowMinutes * 60 * 1000L;

            // Reset if outside time window
            if (currentTime - firstCallTime > windowMillis) {
                callCount = 0;
                firstCallTime = 0;
                return false;
            }

            return callCount >= threshold;
        }

        public void reset() {
            callCount = 0;
            firstCallTime = 0;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        Log.d(TAG, "Phone state: " + state + ", Number: " + incomingNumber);

        // Handle different states
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            handleRingingState(context, incomingNumber);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            // Call ended, we can process now
            handleIdleState(context);
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            // Call answered, reset tracking
            lastIncomingNumber = "";
        }
    }

    private void handleRingingState(Context context, String incomingNumber) {
        if (incomingNumber != null && !incomingNumber.isEmpty()) {
            // We have the number directly
            lastIncomingNumber = incomingNumber;
            processIncomingCall(context, incomingNumber);
        } else {
            // Number is null, try to get it using reflection or alternative methods
            String number = getIncomingNumberFallback();
            if (number != null && !number.isEmpty()) {
                lastIncomingNumber = number;
                processIncomingCall(context, number);
            } else {
                Log.w(TAG, "Cannot get incoming number, using last known: " + lastIncomingNumber);
                if (!lastIncomingNumber.isEmpty()) {
                    processIncomingCall(context, lastIncomingNumber);
                }
            }
        }
    }

    private void handleIdleState(Context context) {
        // When call ends, we can try to process if we have a stored number
        if (!lastIncomingNumber.isEmpty()) {
            Log.d(TAG, "Processing call from idle state: " + lastIncomingNumber);
            // Don't process here as we already processed during RINGING state
            // This is just for cleanup
            lastIncomingNumber = "";
        }
    }

    private String getIncomingNumberFallback() {
        try {
            // Try to get number using reflection (for some devices that hide the number)
            // This is a fallback method
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private void processIncomingCall(Context context, String incomingNumber) {
        try {
            SmartMuteDatabaseHelper databaseHelper = new SmartMuteDatabaseHelper(context);

            // Check if this number is an emergency contact
            boolean isEmergencyContact = databaseHelper.isEmergencyContact(incomingNumber);

            if (isEmergencyContact) {
                Log.i(TAG, "Emergency contact calling: " + incomingNumber);

                // Get emergency contact details
                EmergencyContact contact = databaseHelper.getEmergencyContactByPhone(incomingNumber);
                if (contact != null && contact.isRingOverride()) {
                    handleEmergencyCall(context, contact, incomingNumber);
                } else {
                    Log.w(TAG, "Emergency contact not found or ring override disabled for: " + incomingNumber);
                }
            } else {
                Log.d(TAG, "Not an emergency contact: " + incomingNumber);
            }

            databaseHelper.close();

        } catch (Exception e) {
            Log.e(TAG, "Error processing incoming call: " + e.getMessage());
        }
    }

    private void handleEmergencyCall(Context context, EmergencyContact contact, String incomingNumber) {
        String normalizedNumber = normalizePhoneNumber(incomingNumber);

        // Get or create tracker for this number
        CallTracker tracker = callTrackers.get(normalizedNumber);
        if (tracker == null) {
            tracker = new CallTracker();
            callTrackers.put(normalizedNumber, tracker);
        }

        // Add this call to tracker
        tracker.addCall();

        Log.d(TAG, String.format("Emergency call #%d from %s (threshold: %d in %d min)",
                tracker.callCount, contact.getName(), contact.getCallCountThreshold(), contact.getWindowMinutes()));

        // Check if emergency condition is met
        if (tracker.shouldTrigger(contact.getCallCountThreshold(), contact.getWindowMinutes())) {
            Log.i(TAG, "EMERGENCY OVERRIDE TRIGGERED for: " + contact.getName());

            // Trigger emergency override
            triggerEmergencyOverride(context, contact);

            // Reset tracker after triggering
            tracker.reset();

            // Log the event
            logEmergencyEvent(context, contact);
        }
    }

    private void triggerEmergencyOverride(Context context, EmergencyContact contact) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {
                // Override to normal mode
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

                // Set volumes to maximum
                int maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                int maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int maxNotifVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);

                audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRingVolume, AudioManager.FLAG_SHOW_UI);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMediaVolume, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotifVolume, 0);

                Log.i(TAG, "Emergency override: Phone unmuted and volumes set to max");

                // Show emergency notification
                showEmergencyNotification(context, contact);

                // Start emergency alert service
                startEmergencyAlertService(context, contact);

            } else {
                Log.e(TAG, "AudioManager is null");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while overriding sound: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in emergency override: " + e.getMessage());
        }
    }

    private void showEmergencyNotification(Context context, EmergencyContact contact) {
        // Show toast on main thread
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> {
            String message = String.format("🚨 EMERGENCY: %s is calling! Phone unmuted.", contact.getName());
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }

    private void startEmergencyAlertService(Context context, EmergencyContact contact) {
        try {
            Intent serviceIntent = new Intent(context, EmergencyAlertService.class);
            serviceIntent.putExtra("emergency_contact_name", contact.getName());
            serviceIntent.putExtra("emergency_contact_phone", contact.getPhoneNumber());
            context.startService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start emergency alert service: " + e.getMessage());
        }
    }

    private void logEmergencyEvent(Context context, EmergencyContact contact) {
        try {
            SmartMuteDatabaseHelper databaseHelper = new SmartMuteDatabaseHelper(context);
            databaseHelper.addLog("EMERGENCY_OVERRIDE",
                    "Emergency call from: " + contact.getName() + " (" + contact.getPhoneNumber() + ")");
            databaseHelper.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to log emergency event: " + e.getMessage());
        }
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        // Remove all non-digit characters except +
        return phoneNumber.replaceAll("[^\\d+]", "");
    }
}