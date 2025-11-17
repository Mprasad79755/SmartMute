package com.example.smartmute;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.example.smartmute.R;
import com.example.smartmute.SmartMuteDatabaseHelper;

import java.util.List;

public class SmartMuteService extends Service {
    private static final String TAG = "SmartMuteService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "smartmute_service_channel";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private SmartMuteDatabaseHelper databaseHelper;
    private IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        acquireWakeLock();
        databaseHelper = new SmartMuteDatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationUpdates();

        // Start foreground service with proper permissions
        startForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service starting...");
        startLocationUpdates();
        return START_STICKY;
    }

    private void startForegroundService() {
        createNotificationChannel();

        Notification notification = createNotification();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            Log.d(TAG, "Foreground service started successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException starting foreground service: " + e.getMessage());
            // Fallback: stop service if we can't start foreground
            stopSelf();
        }
    }

    private void setupLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(30000); // 30 seconds to save battery
        locationRequest.setFastestInterval(15000); // 15 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setMaxWaitTime(60000); // 1 minute

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "Location result is null");
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    checkLocationBasedRules(location);
                }
            }

            @Override
            public void onLocationAvailability(com.google.android.gms.location.LocationAvailability locationAvailability) {
                if (!locationAvailability.isLocationAvailable()) {
                    Log.w(TAG, "Location services became unavailable");
                }
            }
        };
    }

    private void startLocationUpdates() {
        try {
            if (checkLocationPermission()) {
                fusedLocationClient.requestLocationUpdates(
                        createLocationRequest(),
                        locationCallback,
                        null
                );
                Log.d(TAG, "Location updates started");
            } else {
                Log.w(TAG, "Location permission not granted, cannot start updates");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException starting location updates: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to start location updates: " + e.getMessage());
        }
    }

    private boolean checkLocationPermission() {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private LocationRequest createLocationRequest() {
        return LocationRequest.create()
                .setInterval(30000)
                .setFastestInterval(15000)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxWaitTime(60000);
    }

    private void checkLocationBasedRules(Location currentLocation) {
        Log.d(TAG, "Location update: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

        try {
            List<SmartLocation> activeLocations = databaseHelper.getActiveLocations();
            if (activeLocations.isEmpty()) {
                Log.d(TAG, "No active locations configured");
                return;
            }

            boolean insideAnyGeofence = false;
            SmartLocation currentGeofence = null;

            // Check if we're inside any geofence
            for (SmartLocation location : activeLocations) {
                if (isInsideGeofence(currentLocation, location)) {
                    insideAnyGeofence = true;
                    currentGeofence = location;
                    Log.d(TAG, "Inside geofence: " + location.getName());
                    break;
                }
            }

            // Handle geofence transitions
            handleGeofenceTransition(insideAnyGeofence, currentGeofence, currentLocation);

        } catch (Exception e) {
            Log.e(TAG, "Error checking location rules: " + e.getMessage());
        }
    }

    private boolean isInsideGeofence(Location currentLocation, SmartLocation geofence) {
        if (currentLocation == null || geofence == null) {
            return false;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                geofence.getLatitude(),
                geofence.getLongitude(),
                results
        );

        float distance = results[0]; // Distance in meters
        boolean inside = distance <= geofence.getRadius();

        Log.d(TAG, String.format("Distance to %s: %.1fm (radius: %dm) - Inside: %s",
                geofence.getName(), distance, geofence.getRadius(), inside));

        return inside;
    }

    private void handleGeofenceTransition(boolean insideGeofence, SmartLocation currentGeofence, Location currentLocation) {
        SharedPreferences prefs = getSharedPreferences("SmartMutePrefs", MODE_PRIVATE);
        boolean wasInsideGeofence = prefs.getBoolean("was_inside_geofence", false);
        int lastGeofenceId = prefs.getInt("last_geofence_id", -1);

        // Check if we entered a geofence
        if (insideGeofence && !wasInsideGeofence) {
            onGeofenceEnter(currentGeofence, currentLocation);
        }
        // Check if we exited a geofence
        else if (!insideGeofence && wasInsideGeofence) {
            onGeofenceExit(lastGeofenceId, currentLocation);
        }
        // Still inside the same geofence
        else if (insideGeofence && wasInsideGeofence &&
                currentGeofence != null && currentGeofence.getId() == lastGeofenceId) {
            // Already applied profile, no action needed
            Log.d(TAG, "Still inside geofence: " + currentGeofence.getName());
        }
        // Switched between different geofences
        else if (insideGeofence && wasInsideGeofence &&
                currentGeofence != null && currentGeofence.getId() != lastGeofenceId) {
            Log.d(TAG, "Switched geofences from " + lastGeofenceId + " to " + currentGeofence.getId());
            onGeofenceEnter(currentGeofence, currentLocation);
        }

        // Update shared preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("was_inside_geofence", insideGeofence);
        if (currentGeofence != null) {
            editor.putInt("last_geofence_id", currentGeofence.getId());
        } else if (!insideGeofence) {
            editor.putInt("last_geofence_id", -1);
        }
        editor.apply();
    }

    private void onGeofenceEnter(SmartLocation geofence, Location location) {
        Log.i(TAG, "ENTERED geofence: " + geofence.getName());

        try {
            // Get the profile to apply
            Profile profile = databaseHelper.getProfile(geofence.getProfileId());
            if (profile != null) {
                // Apply the sound profile
                applySoundProfile(profile);

                // Log the event
                databaseHelper.addLog("GEOFENCE_ENTER",
                        "Entered: " + geofence.getName() + " | Applied: " + profile.getName());

                // Show notification
                showGeofenceNotification("Entered " + geofence.getName(),
                        "Sound profile set to: " + profile.getName(), true);
            } else {
                Log.e(TAG, "Profile not found for ID: " + geofence.getProfileId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error entering geofence: " + e.getMessage());
        }
    }

    private void onGeofenceExit(int lastGeofenceId, Location location) {
        Log.i(TAG, "EXITED geofence with ID: " + lastGeofenceId);

        try {
            // Get the last geofence to find revert profile
            SmartLocation lastGeofence = databaseHelper.getLocation(lastGeofenceId);
            if (lastGeofence != null) {
                Profile revertProfile = databaseHelper.getProfile(lastGeofence.getRevertProfileId());
                if (revertProfile != null) {
                    // Apply the revert profile
                    applySoundProfile(revertProfile);

                    // Log the event
                    databaseHelper.addLog("GEOFENCE_EXIT",
                            "Exited: " + lastGeofence.getName() + " | Reverted to: " + revertProfile.getName());

                    // Show notification
                    showGeofenceNotification("Left " + lastGeofence.getName(),
                            "Sound profile reverted to: " + revertProfile.getName(), false);
                } else {
                    // Fallback to default normal profile
                    applyDefaultProfile();
                    databaseHelper.addLog("GEOFENCE_EXIT",
                            "Exited: " + lastGeofence.getName() + " | Reverted to default");
                }
            } else {
                // No previous geofence info, apply default profile
                applyDefaultProfile();
                databaseHelper.addLog("GEOFENCE_EXIT", "Exited unknown geofence | Reverted to default");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exiting geofence: " + e.getMessage());
            applyDefaultProfile(); // Safety fallback
        }
    }

    private void applySoundProfile(Profile profile) {
        Log.i(TAG, "Applying sound profile: " + profile.getName());

        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            SoundProfileManager soundManager = new SoundProfileManager(this);
            soundManager.applyProfile(profile);

            // Broadcast profile change for UI updates
            Intent intent = new Intent("PROFILE_CHANGED_ACTION");
            intent.putExtra("profile_name", profile.getName());
            sendBroadcast(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error applying sound profile: " + e.getMessage());
        }
    }

    private void applyDefaultProfile() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

            // Set reasonable default volumes
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0);

            Log.i(TAG, "Applied default normal profile");
        } catch (Exception e) {
            Log.e(TAG, "Error applying default profile: " + e.getMessage());
        }
    }

    private void showGeofenceNotification(String title, String message, boolean isEnter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "geofence_channel";
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Create notification channel if needed
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Geofence Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for geofence entries and exits");
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(isEnter ? R.drawable.ic_location_enter : R.drawable.ic_location_exit)
                    .setColor(getResources().getColor(R.color.electric_blue))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SmartMute")
                .setContentText("Monitoring location and schedules")
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(getResources().getColor(R.color.electric_blue))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SmartMute Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for SmartMute location monitoring");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public SmartMuteService getService() {
            return SmartMuteService.this;
        }
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartMute:WakeLock");
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
            Log.d(TAG, "Wake lock acquired");
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire wake lock: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroying...");

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Wake lock released");
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped");
        }

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    // Add this field to your class
    private PowerManager.WakeLock wakeLock;
}
