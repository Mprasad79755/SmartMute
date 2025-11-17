package com.example.smartmute;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.smartmute.*;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabEmergency;



    private void setupViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabEmergency = findViewById(R.id.fab_emergency);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_locations) {
                fragment = new LocationsFragment();
            } else if (itemId == R.id.nav_schedules) {
                fragment = new SchedulesFragment();
            } else if (itemId == R.id.nav_profiles) {
                fragment = new ProfilesFragment();
            }
            else if (itemId == R.id.nav_analytics) {
                fragment = new EmergencyContactsFragment();
            }
//            else if (itemId == R.id.nav_analytics) {
//                fragment = new AnalyticsFragment();
//            }

            return loadFragment(fragment);
        });
    }

    private void setupFAB() {
        fabEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle emergency override
                triggerEmergencyOverride();
            }
        });
    }
    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_metallic));

        setContentView(R.layout.activity_main);

        // Request permissions first
        checkXiaomiPermissions();
        requestRequiredPermissions();
        checkBatteryOptimization();
    }

    private void requestRequiredPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Check Android 12+ foreground service permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            // All permissions granted, setup app
            setupApp();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                setupApp();
            } else {
                // Show explanation and maybe try again or disable location features
                showPermissionDeniedDialog();
            }
        }
    }

    private void setupApp() {
        setupViews();
        setupBottomNavigation();
        setupFAB();
        loadFragment(new HomeFragment());

        // Start the service now that we have permissions
        startSmartMuteService();
    }

    private void startSmartMuteService() {
        try {
            Intent serviceIntent = new Intent(this, SmartMuteService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (SecurityException e) {
            Log.e("MainActivity", "SecurityException starting service: " + e.getMessage());
        }
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions Required")
                .setMessage("Some features may not work without location permissions. You can enable them in Settings.")
                .setPositiveButton("OK", null)
                .show();

        // Setup app anyway but with limited functionality
        setupViews();
        setupBottomNavigation();
        setupFAB();
        loadFragment(new HomeFragment());
    }
    private void triggerEmergencyOverride() {
        // Immediately switch to normal mode
        android.media.AudioManager audioManager =
                (android.media.AudioManager) getSystemService(AUDIO_SERVICE);

        // First turn off DND mode if it's active
        turnOffDndMode();

        // Set to normal ringer mode
        audioManager.setRingerMode(android.media.AudioManager.RINGER_MODE_NORMAL);

        // Set volumes to maximum for emergency situations
        int maxRingVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING);
        int maxMediaVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
        int maxNotifVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_NOTIFICATION);

        audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, maxRingVolume, 0);
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, maxMediaVolume, 0);
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, maxNotifVolume, 0);

        // Show confirmation
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🚨 Emergency Override")
                .setMessage("Phone sound has been set to Normal mode with maximum volume")
                .setPositiveButton("OK", null)
                .show();

        // Optional: Vibrate for feedback
        android.os.Vibrator vibrator =
                (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(200);
        }
    }

    private void turnOffDndMode() {
        try {
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (notificationManager.isNotificationPolicyAccessGranted()) {
                // Turn off DND by setting interruption filter to all
                notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
                android.util.Log.d("EmergencyOverride", "DND mode turned off");
            } else {
                android.util.Log.d("EmergencyOverride", "No DND permission, skipping DND turn off");
            }
        } catch (SecurityException e) {
            android.util.Log.e("EmergencyOverride", "No permission to turn off DND: " + e.getMessage());
        } catch (Exception e) {
            android.util.Log.e("EmergencyOverride", "Error turning off DND: " + e.getMessage());
        }
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            return true;
        }
        return false;
    }

    public void showBottomNavigation(boolean show) {
        bottomNavigationView.setVisibility(show ? View.VISIBLE : View.GONE);
        fabEmergency.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    private static final int BATTERY_OPTIMIZATION_REQUEST = 1002;


    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                // Show dialog explaining why we need this
                showBatteryOptimizationDialog();
            }
        }
    }

    private void showBatteryOptimizationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Battery Optimization")
                .setMessage("For SmartMute to work reliably in the background, please disable battery optimization for this app.")
                .setPositiveButton("Disable Optimization", (dialog, which) -> {
                    requestBatteryOptimizationExemption();
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BATTERY_OPTIMIZATION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                    Toast.makeText(this, "Battery optimization disabled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private void checkXiaomiPermissions() {
        // Check if it's a Xiaomi device
        if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {
            SharedPreferences prefs = getSharedPreferences("SmartMutePrefs", MODE_PRIVATE);
            boolean xiaomiGuideShown = prefs.getBoolean("xiaomi_guide_shown", false);

            if (!xiaomiGuideShown) {
                Intent intent = new Intent(this, XiaomiPermissionGuideActivity.class);
                startActivity(intent);

                // Mark as shown
                prefs.edit().putBoolean("xiaomi_guide_shown", true).apply();
            }
        }
    }

}