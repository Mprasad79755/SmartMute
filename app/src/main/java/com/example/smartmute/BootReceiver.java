package com.example.smartmute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.smartmute.SmartMuteService;
import com.example.smartmute.AlarmUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "SmartMutePrefs";
    private static final String PREF_SERVICE_ENABLED = "service_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        Log.d(TAG, "BootReceiver triggered with action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_REBOOT.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            handleBootCompleted(context);
        } else if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            // For devices that support direct boot
            handleLockedBootCompleted(context);
        }
    }

    private void handleBootCompleted(Context context) {
        Log.i(TAG, "Device boot completed, restoring SmartMute services...");

        // Check if service was enabled before reboot
        if (wasServiceEnabled(context)) {
            startSmartMuteService(context);
            restoreScheduledAlarms(context);
            logBootEvent(context, "Service restored after boot");
        } else {
            Log.i(TAG, "Service was not enabled before reboot, skipping auto-start");
            logBootEvent(context, "Boot completed - service was disabled");
        }
    }

    private void handleLockedBootCompleted(Context context) {
        Log.i(TAG, "Locked boot completed, initializing direct boot aware components...");
        // For direct boot, we might want to start minimal services
        // that don't require user authentication
    }

    private boolean wasServiceEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_SERVICE_ENABLED, true); // Default to true for auto-start
    }

    private void startSmartMuteService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, SmartMuteService.class);
            serviceIntent.putExtra("source", "boot_receiver");

            // Use startForegroundService for Android 8.0+ to avoid Background Execution Limits
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            Log.i(TAG, "SmartMuteService started successfully after boot");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException starting service after boot: " + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException starting service after boot: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to start SmartMuteService after boot: " + e.getMessage());
        }
    }

    private void restoreScheduledAlarms(Context context) {
        try {
            AlarmUtils alarmUtils = new AlarmUtils(context);
            alarmUtils.restoreAllScheduledAlarms();
            Log.i(TAG, "All scheduled alarms restored after boot");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore scheduled alarms: " + e.getMessage());
        }
    }

    private void logBootEvent(Context context, String message) {
        // You can implement your logging mechanism here
        // For example, using your database helper
        Log.i(TAG, "Boot event: " + message);

        // Example of logging to database (uncomment when database helper is available)
        /*
        try {
            SmartMuteDatabaseHelper dbHelper = new SmartMuteDatabaseHelper(context);
            dbHelper.addLog("BOOT_EVENT", message);
            dbHelper.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to log boot event to database: " + e.getMessage());
        }
        */
    }
}
