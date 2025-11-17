package com.example.smartmute;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class SoundProfileManager {
    private static final String TAG = "SoundProfileManager";

    private Context context;
    private AudioManager audioManager;
    private NotificationManager notificationManager;

    public SoundProfileManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public boolean hasDndPermission() {
        return notificationManager.isNotificationPolicyAccessGranted();
    }

    public void requestDndPermission(AppCompatActivity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            activity.startActivityForResult(intent, 1001);
        } catch (Exception e) {
            Log.e(TAG, "Error opening DND settings: " + e.getMessage());
            // Fallback: open app settings
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            activity.startActivity(intent);
        }
    }

    public void applyProfile(Profile profile) {
        if (profile == null) {
            Log.e(TAG, "Cannot apply null profile");
            return;
        }

        try {
            Log.d(TAG, "Applying profile: " + profile.getName() +
                    " | Vibrate: " + profile.isVibrate() +
                    " | DND: " + profile.isDnd());

            // Handle DND mode first (has highest priority)
            if (profile.isDnd()) {
                if (setDoNotDisturbMode()) {
                    Log.d(TAG, "DND mode activated successfully");
                } else {
                    Log.w(TAG, "DND permission not granted, using silent mode as fallback");
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                }
            }
            // Handle vibrate mode - TURN OFF DND first
            else if (profile.isVibrate()) {
                turnOffDndMode(); // Turn off DND if active
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                Log.d(TAG, "Vibrate mode activated");
            }
            // Handle normal mode - TURN OFF DND first
            else {
                turnOffDndMode(); // Turn off DND if active
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                Log.d(TAG, "Normal mode activated");
            }

            // Set volumes (only if not in DND mode)
            if (!profile.isDnd()) {
                setStreamVolume(AudioManager.STREAM_RING, profile.getCallVolume(), "Call");
                setStreamVolume(AudioManager.STREAM_MUSIC, profile.getMediaVolume(), "Media");
                setStreamVolume(AudioManager.STREAM_NOTIFICATION, profile.getNotificationVolume(), "Notification");
            }

            Log.i(TAG, "Successfully applied profile: " + profile.getName());

        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied while applying profile: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error applying profile: " + e.getMessage());
        }
    }

    private void setStreamVolume(int streamType, int volume, String streamName) {
        try {
            int maxVolume = audioManager.getStreamMaxVolume(streamType);
            int safeVolume = Math.min(volume, maxVolume);
            audioManager.setStreamVolume(streamType, safeVolume, 0);
            Log.d(TAG, streamName + " volume set to " + safeVolume + "/" + maxVolume);
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to set " + streamName + " volume");
        }
    }

    private boolean setDoNotDisturbMode() {
        try {
            if (hasDndPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error setting DND mode: " + e.getMessage());
            return false;
        }
    }

    private void turnOffDndMode() {
        try {
            if (hasDndPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Turn off DND by setting interruption filter to all
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                    Log.d(TAG, "DND mode turned off");
                }
            } else {
                Log.d(TAG, "No DND permission, skipping DND turn off");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to turn off DND: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error turning off DND: " + e.getMessage());
        }
    }

    public int getCurrentRingerMode() {
        return audioManager.getRingerMode();
    }

    public boolean isDndEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int filter = notificationManager.getCurrentInterruptionFilter();
            return filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                    filter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
                    filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY;
        }
        return false;
    }

    // New method to explicitly turn off DND (useful for emergency override)
    public void forceTurnOffDnd() {
        turnOffDndMode();
    }
}