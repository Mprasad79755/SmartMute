package com.example.smartmute;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class EmergencyAlertService extends Service {
    private static final String TAG = "EmergencyAlertService";
    private static final int EMERGENCY_NOTIFICATION_ID = 1001;

    private Ringtone ringtone;
    private Vibrator vibrator;
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String contactName = intent.getStringExtra("emergency_contact_name");
        String contactPhone = intent.getStringExtra("emergency_contact_phone");

        Log.d(TAG, "Starting emergency alert for: " + contactName);

        // Ensure phone is not silent
        ensurePhoneIsAudible();

        // Play emergency ringtone
        playEmergencyRingtone();

        // Create emergency notification
        showEmergencyNotification(contactName, contactPhone);

        // Stop service after 30 seconds to prevent battery drain
        stopSelfAfterDelay();

        return START_NOT_STICKY;
    }

    private void ensurePhoneIsAudible() {
        try {
            // Set to normal mode
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

            // Set volumes to max
            int maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            int maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRingVolume, AudioManager.FLAG_SHOW_UI);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMediaVolume, 0);

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception ensuring phone is audible: " + e.getMessage());
        }
    }

    private void playEmergencyRingtone() {
        try {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(this, alarmSound);
            if (ringtone != null) {
                ringtone.play();
            }

            // Vibrate pattern: long bursts
            long[] pattern = {0, 1000, 500, 1000, 500, 1000};
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing emergency ringtone: " + e.getMessage());
        }
    }

    private void showEmergencyNotification(String contactName, String contactPhone) {
        createEmergencyChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "emergency_channel")
                .setContentTitle("🚨 Emergency Call")
                .setContentText(contactName + " is calling repeatedly!")
                .setSmallIcon(R.drawable.ic_emergency)
                .setColor(getResources().getColor(R.color.electric_blue))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setOngoing(true);

        startForeground(EMERGENCY_NOTIFICATION_ID, builder.build());
    }

    private void createEmergencyChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "emergency_channel",
                    "Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Emergency call override notifications");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void stopSelfAfterDelay() {
        new android.os.Handler().postDelayed(() -> {
            stopSelf();
        }, 30000); // Stop after 30 seconds
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}