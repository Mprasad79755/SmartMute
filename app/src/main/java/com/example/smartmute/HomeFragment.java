package com.example.smartmute;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.airbnb.lottie.LottieAnimationView;

public class HomeFragment extends Fragment {

    private TextView currentProfileText;
    private TextView currentModeText;
    private LottieAnimationView modeAnimation;
    private ImageView btnNormal, btnVibrate, btnSilent;

    // Broadcast receiver for profile changes from service
    private BroadcastReceiver profileChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("PROFILE_CHANGED_ACTION".equals(intent.getAction())) {
                String profileName = intent.getStringExtra("profile_name");
                if (profileName != null) {
                    updateCurrentMode();
                    showProfileChangeToast(profileName);
                }
            }
        }
    };

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        setupClickListeners();
        updateCurrentMode();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register broadcast receiver
        IntentFilter filter = new IntentFilter("PROFILE_CHANGED_ACTION");
        ContextCompat.registerReceiver(requireActivity(), profileChangeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // Update mode when fragment becomes visible
        updateCurrentMode();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        try {
            requireActivity().unregisterReceiver(profileChangeReceiver);
        } catch (Exception e) {
            // Receiver might not be registered, ignore
        }
    }

    private void initializeViews(View view) {
        currentProfileText = view.findViewById(R.id.current_profile_text);
        currentModeText = view.findViewById(R.id.current_mode_text);
        modeAnimation = view.findViewById(R.id.mode_animation);
        btnNormal = view.findViewById(R.id.btn_normal);
        btnVibrate = view.findViewById(R.id.btn_vibrate);
        btnSilent = view.findViewById(R.id.btn_silent);
    }

    private void setupClickListeners() {
        btnNormal.setOnClickListener(v -> setSoundMode(AudioManager.RINGER_MODE_NORMAL));
        btnVibrate.setOnClickListener(v -> setSoundMode(AudioManager.RINGER_MODE_VIBRATE));
        btnSilent.setOnClickListener(v -> setSoundMode(AudioManager.RINGER_MODE_SILENT));
    }

    private void setSoundMode(int mode) {
        AudioManager audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

        // For silent mode, check DND permission
        if (mode == AudioManager.RINGER_MODE_SILENT) {
            if (!hasDndPermission()) {
                requestDndPermission();
                return;
            }
        }

        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                applyNormalMode(audioManager);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                applyVibrateMode(audioManager);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                applySilentMode(audioManager);
                break;
        }

        modeAnimation.playAnimation();
        logModeChange(mode);

        // Update service about manual mode change
        updateBackgroundService();
    }

    private void applyNormalMode(AudioManager audioManager) {
        // First turn off DND mode if it's active
        turnOffDndMode();

        // Set normal ringer mode
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

        // Set reasonable volumes for normal mode
        int maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int maxNotifVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);

        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRingVolume, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMediaVolume / 2, 0); // 50% media volume
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotifVolume, 0);

        modeAnimation.setAnimation(R.raw.normal_mode);
        currentModeText.setText("Normal Mode");
        currentModeText.setTextColor(getResources().getColor(R.color.electric_blue));

        // Update profile text
        if (currentProfileText != null) {
            currentProfileText.setText("Manual Control");
        }
    }

    private void applyVibrateMode(AudioManager audioManager) {
        // First turn off DND mode if it's active
        turnOffDndMode();

        // Set vibrate mode
        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);

        modeAnimation.setAnimation(R.raw.vibrate_mode);
        currentModeText.setText("Vibrate Mode");
        currentModeText.setTextColor(getResources().getColor(R.color.aqua_glow));

        // Update profile text
        if (currentProfileText != null) {
            currentProfileText.setText("Manual Control");
        }
    }

    private void applySilentMode(AudioManager audioManager) {
        // First try DND mode if permission granted
        if (hasDndPermission()) {
            try {
                NotificationManager notificationManager =
                        (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);

                modeAnimation.setAnimation(R.raw.silent_mode);
                currentModeText.setText("DND Mode");
                currentModeText.setTextColor(getResources().getColor(R.color.metallic_silver));

                // Update profile text
                if (currentProfileText != null) {
                    currentProfileText.setText("Manual Control - DND");
                }
                return;
            } catch (SecurityException e) {
                // Fall back to silent mode if DND fails
                Toast.makeText(requireContext(), "DND access failed, using silent mode", Toast.LENGTH_SHORT).show();
            }
        }

        // Fallback to regular silent mode
        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        modeAnimation.setAnimation(R.raw.silent_mode);
        currentModeText.setText("Silent Mode");
        currentModeText.setTextColor(getResources().getColor(R.color.metallic_silver));

        // Update profile text
        if (currentProfileText != null) {
            currentProfileText.setText("Manual Control");
        }
    }

    private void turnOffDndMode() {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (hasDndPermission()) {
                // Turn off DND by setting interruption filter to all
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                Log.d("HomeFragment", "DND mode turned off");
            }
        } catch (SecurityException e) {
            Log.e("HomeFragment", "No permission to turn off DND: " + e.getMessage());
        } catch (Exception e) {
            Log.e("HomeFragment", "Error turning off DND: " + e.getMessage());
        }
    }

    private boolean hasDndPermission() {
        NotificationManager notificationManager =
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager.isNotificationPolicyAccessGranted();
    }

    private void requestDndPermission() {
        Toast.makeText(requireContext(),
                "Do Not Disturb access is required for silent mode",
                Toast.LENGTH_LONG).show();

        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);

        // Show additional explanation
        Toast.makeText(requireContext(),
                "Please enable 'SmartMute' in Do Not Disturb access settings",
                Toast.LENGTH_LONG).show();
    }

    private void updateCurrentMode() {
        AudioManager audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        int currentMode = audioManager.getRingerMode();

        NotificationManager notificationManager =
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Check if DND is active (even if ringer mode is not silent)
        boolean isDndActive = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int interruptionFilter = notificationManager.getCurrentInterruptionFilter();
            isDndActive = (interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                    interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
                    interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        }

        if (isDndActive) {
            // DND mode is active
            currentModeText.setText("DND Mode");
            currentModeText.setTextColor(getResources().getColor(R.color.metallic_silver));
            modeAnimation.setAnimation(R.raw.silent_mode);
            if (currentProfileText != null) {
                currentProfileText.setText("Do Not Disturb Active");
            }
        } else {
            // Regular ringer modes
            switch (currentMode) {
                case AudioManager.RINGER_MODE_NORMAL:
                    currentModeText.setText("Normal Mode");
                    currentModeText.setTextColor(getResources().getColor(R.color.electric_blue));
                    modeAnimation.setAnimation(R.raw.normal_mode);
                    if (currentProfileText != null) {
                        currentProfileText.setText("Ready");
                    }
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    currentModeText.setText("Vibrate Mode");
                    currentModeText.setTextColor(getResources().getColor(R.color.aqua_glow));
                    modeAnimation.setAnimation(R.raw.vibrate_mode);
                    if (currentProfileText != null) {
                        currentProfileText.setText("Ready");
                    }
                    break;
                case AudioManager.RINGER_MODE_SILENT:
                    currentModeText.setText("Silent Mode");
                    currentModeText.setTextColor(getResources().getColor(R.color.metallic_silver));
                    modeAnimation.setAnimation(R.raw.silent_mode);
                    if (currentProfileText != null) {
                        currentProfileText.setText("Ready");
                    }
                    break;
            }
        }
        modeAnimation.playAnimation();
    }

    private void logModeChange(int mode) {
        String modeName = "";
        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                modeName = "Normal";
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                modeName = "Vibrate";
                break;
            case AudioManager.RINGER_MODE_SILENT:
                modeName = "Silent/DND";
                break;
        }

        // You can log this to your database if needed
        // databaseHelper.addLog("MANUAL_MODE_CHANGE", "User set mode to: " + modeName);

        Toast.makeText(requireContext(), "Mode set to: " + modeName, Toast.LENGTH_SHORT).show();
    }

    private void updateBackgroundService() {
        try {
            Intent serviceIntent = new Intent(requireContext(), SmartMuteService.class);
            serviceIntent.putExtra("manual_override", true);
            requireContext().startService(serviceIntent);
        } catch (Exception e) {
            // Service might not be available, that's okay
        }
    }

    private void showProfileChangeToast(String profileName) {
        Toast.makeText(requireContext(),
                "Auto profile: " + profileName,
                Toast.LENGTH_SHORT).show();
    }

    // Public method to update UI from other fragments if needed
    public void refreshUI() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateCurrentMode);
        }
    }
}