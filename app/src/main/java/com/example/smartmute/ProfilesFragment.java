package com.example.smartmute;



import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;


public class ProfilesFragment extends Fragment {

    private RecyclerView profilesRecyclerView;
    private ProfilesAdapter profilesAdapter;
    private List<Profile> profilesList;
    private SmartMuteDatabaseHelper databaseHelper;

    private Button btnAddProfile, btnTestProfile;
    private EditText etProfileName;
    private SeekBar sbCallVolume, sbMediaVolume, sbNotificationVolume;
    private TextView tvCallVolume, tvMediaVolume, tvNotificationVolume;
    private CheckBox cbVibrate, cbDnd;

    private AudioManager audioManager;

    public ProfilesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profiles, container, false);

        initializeViews(view);
        setupDatabase();
        setupAudioManager();
        setupRecyclerView();
        loadProfiles();
        setupClickListeners();
        setupSeekBars();

        return view;
    }

    private void initializeViews(View view) {
        profilesRecyclerView = view.findViewById(R.id.profiles_recycler_view);
        btnAddProfile = view.findViewById(R.id.btn_add_profile);
        btnTestProfile = view.findViewById(R.id.btn_test_profile);
        etProfileName = view.findViewById(R.id.et_profile_name);

        // Volume seekbars
        sbCallVolume = view.findViewById(R.id.sb_call_volume);
        sbMediaVolume = view.findViewById(R.id.sb_media_volume);
        sbNotificationVolume = view.findViewById(R.id.sb_notification_volume);

        // Volume labels
        tvCallVolume = view.findViewById(R.id.tv_call_volume);
        tvMediaVolume = view.findViewById(R.id.tv_media_volume);
        tvNotificationVolume = view.findViewById(R.id.tv_notification_volume);

        // Checkboxes
        cbVibrate = view.findViewById(R.id.cb_vibrate);
        cbDnd = view.findViewById(R.id.cb_dnd);
    }


    private void applyProfileSettings(Profile profile) {
        SoundProfileManager soundProfileManager = new SoundProfileManager(requireContext());

        try {
            if (profile.isDnd()) {
                if (soundProfileManager.hasDndPermission()) {
                    soundProfileManager.applyProfile(profile);
                    Toast.makeText(requireContext(), "DND mode activated", Toast.LENGTH_SHORT).show();
                } else {
                    // Request DND permission
                    showDndPermissionDialog();
                    return; // Don't apply profile until permission is granted
                }
            } else {
                soundProfileManager.applyProfile(profile);
                Toast.makeText(requireContext(), "Profile settings applied", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Permission denied for audio settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDndPermissionDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Do Not Disturb Permission Required")
                .setMessage("This app needs permission to manage Do Not Disturb mode. Please grant the permission in the next screen.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    SoundProfileManager soundProfileManager = new SoundProfileManager(requireContext());
                    if (requireActivity() instanceof AppCompatActivity) {
                        soundProfileManager.requestDndPermission((AppCompatActivity) requireActivity());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDatabase() {
        databaseHelper = new SmartMuteDatabaseHelper(requireContext());
    }

    private void setupAudioManager() {
        audioManager = (AudioManager) requireContext().getSystemService(requireContext().AUDIO_SERVICE);
    }

    private void setupRecyclerView() {
        profilesList = new ArrayList<>();
        profilesAdapter = new ProfilesAdapter(profilesList);
        profilesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        profilesRecyclerView.setAdapter(profilesAdapter);
    }

    private void loadProfiles() {
        profilesList.clear();
        profilesList.addAll(databaseHelper.getAllProfiles());
        profilesAdapter.notifyDataSetChanged();

        // If no profiles exist, create default ones
        if (profilesList.isEmpty()) {
            createDefaultProfiles();
        }
    }

    private void createDefaultProfiles() {
        // Create Normal profile
        Profile normal = new Profile();
        normal.setName("Normal");
        normal.setCallVolume(audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        normal.setMediaVolume(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        normal.setNotificationVolume(audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
        normal.setVibrate(false);
        normal.setDnd(false);
        databaseHelper.addProfile(normal);

        // Create Vibrate profile
        Profile vibrate = new Profile();
        vibrate.setName("Vibrate");
        vibrate.setCallVolume(0);
        vibrate.setMediaVolume(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2);
        vibrate.setNotificationVolume(0);
        vibrate.setVibrate(true);
        vibrate.setDnd(false);
        databaseHelper.addProfile(vibrate);

        // Create Silent profile
        Profile silent = new Profile();
        silent.setName("Silent");
        silent.setCallVolume(0);
        silent.setMediaVolume(0);
        silent.setNotificationVolume(0);
        silent.setVibrate(false);
        silent.setDnd(true);
        databaseHelper.addProfile(silent);

        loadProfiles(); // Reload to show new profiles
    }

    private void setupClickListeners() {
        btnAddProfile.setOnClickListener(v -> addNewProfile());
        btnTestProfile.setOnClickListener(v -> testCurrentProfile());
    }

    private void setupSeekBars() {
        int maxCallVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int maxNotificationVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);

        sbCallVolume.setMax(maxCallVolume);
        sbMediaVolume.setMax(maxMediaVolume);
        sbNotificationVolume.setMax(maxNotificationVolume);

        // Set default values
        sbCallVolume.setProgress(maxCallVolume);
        sbMediaVolume.setProgress(maxMediaVolume);
        sbNotificationVolume.setProgress(maxNotificationVolume);

        updateVolumeLabels();

        // Volume change listeners
        sbCallVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateVolumeLabels();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbMediaVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateVolumeLabels();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbNotificationVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateVolumeLabels();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateVolumeLabels() {
        tvCallVolume.setText("Call: " + sbCallVolume.getProgress());
        tvMediaVolume.setText("Media: " + sbMediaVolume.getProgress());
        tvNotificationVolume.setText("Notification: " + sbNotificationVolume.getProgress());
    }

    private void addNewProfile() {
        String name = etProfileName.getText().toString().trim();

        if (name.isEmpty()) {
            etProfileName.setError("Profile name is required");
            return;
        }

        Profile profile = new Profile();
        profile.setName(name);
        profile.setCallVolume(sbCallVolume.getProgress());
        profile.setMediaVolume(sbMediaVolume.getProgress());
        profile.setNotificationVolume(sbNotificationVolume.getProgress());
        profile.setVibrate(cbVibrate.isChecked());
        profile.setDnd(cbDnd.isChecked());
        profile.setRingtone("default"); // Default ringtone

        long id = databaseHelper.addProfile(profile);
        if (id != -1) {
            Toast.makeText(requireContext(), "Profile added successfully", Toast.LENGTH_SHORT).show();
            clearForm();
            loadProfiles();
        } else {
            Toast.makeText(requireContext(), "Failed to add profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void testCurrentProfile() {
        // Create a temporary profile with current settings and apply it
        Profile testProfile = new Profile();
        testProfile.setCallVolume(sbCallVolume.getProgress());
        testProfile.setMediaVolume(sbMediaVolume.getProgress());
        testProfile.setNotificationVolume(sbNotificationVolume.getProgress());
        testProfile.setVibrate(cbVibrate.isChecked());
        testProfile.setDnd(cbDnd.isChecked());

        // Apply the profile (you would use your SoundProfileManager here)
        applyProfileSettings(testProfile);
        Toast.makeText(requireContext(), "Profile settings applied for testing", Toast.LENGTH_SHORT).show();
    }



    private void clearForm() {
        etProfileName.setText("");
        int maxCallVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int maxNotificationVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);

        sbCallVolume.setProgress(maxCallVolume);
        sbMediaVolume.setProgress(maxMediaVolume);
        sbNotificationVolume.setProgress(maxNotificationVolume);
        cbVibrate.setChecked(false);
        cbDnd.setChecked(false);

        updateVolumeLabels();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    // Profiles Adapter
    private class ProfilesAdapter extends RecyclerView.Adapter<ProfilesAdapter.ProfileViewHolder> {
        private List<Profile> profiles;

        public ProfilesAdapter(List<Profile> profiles) {
            this.profiles = profiles;
        }

        @NonNull
        @Override
        public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile, parent, false);
            return new ProfileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
            Profile profile = profiles.get(position);
            holder.bind(profile);
        }

        @Override
        public int getItemCount() {
            return profiles.size();
        }

        class ProfileViewHolder extends RecyclerView.ViewHolder {
            private TextView tvName, tvSettings;
            private Button btnApply, btnEdit, btnDelete;

            public ProfileViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_profile_name);
                tvSettings = itemView.findViewById(R.id.tv_profile_settings);
                btnApply = itemView.findViewById(R.id.btn_apply_profile);
                btnEdit = itemView.findViewById(R.id.btn_edit_profile);
                btnDelete = itemView.findViewById(R.id.btn_delete_profile);
            }

            public void bind(Profile profile) {
                tvName.setText(profile.getName());
                tvSettings.setText(getSettingsString(profile));

                btnApply.setOnClickListener(v -> applyProfile(profile));
                btnEdit.setOnClickListener(v -> editProfile(profile));
                btnDelete.setOnClickListener(v -> deleteProfile(profile));
            }

            private String getSettingsString(Profile profile) {
                StringBuilder settings = new StringBuilder();
                settings.append("Call: ").append(profile.getCallVolume())
                        .append(" | Media: ").append(profile.getMediaVolume())
                        .append(" | Notif: ").append(profile.getNotificationVolume());

                if (profile.isVibrate()) settings.append(" | Vibrate");
                if (profile.isDnd()) settings.append(" | DND");

                return settings.toString();
            }

            private void applyProfile(Profile profile) {
                applyProfileSettings(profile);
                Toast.makeText(requireContext(), "Applied profile: " + profile.getName(), Toast.LENGTH_SHORT).show();
            }

            private void editProfile(Profile profile) {
                // Populate form with profile data for editing
                etProfileName.setText(profile.getName());
                sbCallVolume.setProgress(profile.getCallVolume());
                sbMediaVolume.setProgress(profile.getMediaVolume());
                sbNotificationVolume.setProgress(profile.getNotificationVolume());
                cbVibrate.setChecked(profile.isVibrate());
                cbDnd.setChecked(profile.isDnd());
                updateVolumeLabels();

                // Change add button to update
                btnAddProfile.setText("Update Profile");
                btnAddProfile.setOnClickListener(v -> updateProfile(profile));

                Toast.makeText(requireContext(), "Editing profile: " + profile.getName(), Toast.LENGTH_SHORT).show();
            }

            private void updateProfile(Profile profile) {
                profile.setName(etProfileName.getText().toString().trim());
                profile.setCallVolume(sbCallVolume.getProgress());
                profile.setMediaVolume(sbMediaVolume.getProgress());
                profile.setNotificationVolume(sbNotificationVolume.getProgress());
                profile.setVibrate(cbVibrate.isChecked());
                profile.setDnd(cbDnd.isChecked());

                boolean updated = databaseHelper.updateProfile(profile);
                if (updated) {
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadProfiles();
                    // Reset add button
                    btnAddProfile.setText("Add Profile");
                    btnAddProfile.setOnClickListener(v -> addNewProfile());
                }
            }

            private void deleteProfile(Profile profile) {
                // Check if profile is being used
                boolean isUsed = databaseHelper.isProfileInUse(profile.getId());
                if (isUsed) {
                    Toast.makeText(requireContext(),
                            "Cannot delete profile - it's being used by schedules or locations",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                boolean deleted = databaseHelper.deleteProfile(profile.getId());
                if (deleted) {
                    profiles.remove(profile);
                    notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Profile deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}