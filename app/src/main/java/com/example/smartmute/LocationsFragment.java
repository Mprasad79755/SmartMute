package com.example.smartmute;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class LocationsFragment extends Fragment {

    private RecyclerView locationsRecyclerView;
    private LocationsAdapter locationsAdapter;
    private List<SmartLocation> locationsList;
    private List<Profile> profilesList;
    private SmartMuteDatabaseHelper databaseHelper;
    private FusedLocationProviderClient fusedLocationClient;

    private Button btnAddLocation, btnGetCurrentLocation, btnUseManualCoordinates;
    private EditText etLocationName, etRadius, etLatitude, etLongitude;
    private Spinner spinnerEnterProfile, spinnerExitProfile;
    private TextView tvCoordinates;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private boolean useManualCoordinates = false;

    public LocationsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_locations, container, false);

        initializeViews(view);
        setupDatabase();
        setupRecyclerView();
        loadProfiles();
        loadLocations();
        setupClickListeners();
        getCurrentLocation();

        return view;
    }

    private void initializeViews(View view) {
        locationsRecyclerView = view.findViewById(R.id.locations_recycler_view);
        btnAddLocation = view.findViewById(R.id.btn_add_location);
        btnGetCurrentLocation = view.findViewById(R.id.btn_get_current_location);
        btnUseManualCoordinates = view.findViewById(R.id.btn_use_manual);
        etLocationName = view.findViewById(R.id.et_location_name);
        etRadius = view.findViewById(R.id.et_radius);
        etLatitude = view.findViewById(R.id.et_latitude);
        etLongitude = view.findViewById(R.id.et_longitude);
        spinnerEnterProfile = view.findViewById(R.id.spinner_enter_profile);
        spinnerExitProfile = view.findViewById(R.id.spinner_exit_profile);
        tvCoordinates = view.findViewById(R.id.tv_coordinates);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Hide manual coordinates initially
        etLatitude.setVisibility(View.GONE);
        etLongitude.setVisibility(View.GONE);
    }

    private void setupDatabase() {
        databaseHelper = new SmartMuteDatabaseHelper(requireContext());
    }

    private void setupRecyclerView() {
        locationsList = new ArrayList<>();
        locationsAdapter = new LocationsAdapter(locationsList);
        locationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        locationsRecyclerView.setAdapter(locationsAdapter);
    }

    private void loadProfiles() {
        profilesList = databaseHelper.getAllProfiles();

        // Create adapter for spinners
        List<String> profileNames = new ArrayList<>();
        for (Profile profile : profilesList) {
            profileNames.add(profile.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                profileNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEnterProfile.setAdapter(adapter);
        spinnerExitProfile.setAdapter(adapter);

        // Set default selections
        if (profilesList.size() > 0) {
            spinnerEnterProfile.setSelection(0); // First profile
            if (profilesList.size() > 1) {
                spinnerExitProfile.setSelection(1); // Second profile
            }
        }
    }

    private void loadLocations() {
        locationsList.clear();
        locationsList.addAll(databaseHelper.getAllLocations());
        locationsAdapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        btnGetCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        btnUseManualCoordinates.setOnClickListener(v -> toggleManualCoordinates());

        btnAddLocation.setOnClickListener(v -> addNewLocation());
    }

    private void toggleManualCoordinates() {
        useManualCoordinates = !useManualCoordinates;

        if (useManualCoordinates) {
            etLatitude.setVisibility(View.VISIBLE);
            etLongitude.setVisibility(View.VISIBLE);
            btnUseManualCoordinates.setText("Use Current Location");
            tvCoordinates.setText("Enter coordinates manually");
            tvCoordinates.setTextColor(getResources().getColor(R.color.metallic_silver));
        } else {
            etLatitude.setVisibility(View.GONE);
            etLongitude.setVisibility(View.GONE);
            btnUseManualCoordinates.setText("Enter Coordinates Manually");
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (useManualCoordinates) {
            return; // Don't get location if using manual coordinates
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                            tvCoordinates.setText(String.format("Lat: %.6f, Lng: %.6f",
                                    currentLatitude, currentLongitude));
                            tvCoordinates.setTextColor(getResources().getColor(R.color.aqua_glow));

                            // Auto-fill manual fields for reference
                            etLatitude.setText(String.valueOf(currentLatitude));
                            etLongitude.setText(String.valueOf(currentLongitude));
                        } else {
                            Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addNewLocation() {
        String name = etLocationName.getText().toString().trim();
        String radiusStr = etRadius.getText().toString().trim();

        if (name.isEmpty()) {
            etLocationName.setError("Location name is required");
            return;
        }

        if (radiusStr.isEmpty()) {
            etRadius.setError("Radius is required");
            return;
        }

        double latitude, longitude;

        if (useManualCoordinates) {
            String latStr = etLatitude.getText().toString().trim();
            String lngStr = etLongitude.getText().toString().trim();

            if (latStr.isEmpty() || lngStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both latitude and longitude", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                latitude = Double.parseDouble(latStr);
                longitude = Double.parseDouble(lngStr);

                // Validate coordinates
                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    Toast.makeText(requireContext(), "Invalid coordinates. Latitude: -90 to 90, Longitude: -180 to 180", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid coordinate format", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (currentLatitude == 0.0 || currentLongitude == 0.0) {
                Toast.makeText(requireContext(), "Please get current location first", Toast.LENGTH_SHORT).show();
                return;
            }
            latitude = currentLatitude;
            longitude = currentLongitude;
        }

        int radius = Integer.parseInt(radiusStr);

        // Get selected profiles
        int enterProfilePosition = spinnerEnterProfile.getSelectedItemPosition();
        int exitProfilePosition = spinnerExitProfile.getSelectedItemPosition();

        if (enterProfilePosition < 0 || enterProfilePosition >= profilesList.size() ||
                exitProfilePosition < 0 || exitProfilePosition >= profilesList.size()) {
            Toast.makeText(requireContext(), "Please select both enter and exit profiles", Toast.LENGTH_SHORT).show();
            return;
        }

        Profile enterProfile = profilesList.get(enterProfilePosition);
        Profile exitProfile = profilesList.get(exitProfilePosition);

        // Create new location
        SmartLocation location = new SmartLocation();
        location.setName(name);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setRadius(radius);
        location.setProfileId(enterProfile.getId());
        location.setRevertProfileId(exitProfile.getId());
        location.setEnabled(true);

        long id = databaseHelper.addLocation(location);
        if (id != -1) {
            Toast.makeText(requireContext(), "Location rule added successfully", Toast.LENGTH_SHORT).show();
            clearForm();
            loadLocations();

            // Start location monitoring service
            startLocationMonitoringService();
        } else {
            Toast.makeText(requireContext(), "Failed to add location rule", Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationMonitoringService() {
        try {
            Intent serviceIntent = new Intent(requireContext(), LocationMonitoringService.class);
            requireContext().startService(serviceIntent);
        } catch (Exception e) {
            Log.e("LocationsFragment", "Failed to start location monitoring service: " + e.getMessage());
        }
    }

    private void clearForm() {
        etLocationName.setText("");
        etRadius.setText("");
        etLatitude.setText("");
        etLongitude.setText("");
        tvCoordinates.setText("No coordinates selected");
        tvCoordinates.setTextColor(getResources().getColor(R.color.metallic_silver));
        currentLatitude = 0.0;
        currentLongitude = 0.0;
        useManualCoordinates = false;
        etLatitude.setVisibility(View.GONE);
        etLongitude.setVisibility(View.GONE);
        btnUseManualCoordinates.setText("Enter Coordinates Manually");

        // Reset spinners to default
        if (profilesList.size() > 0) {
            spinnerEnterProfile.setSelection(0);
            if (profilesList.size() > 1) {
                spinnerExitProfile.setSelection(1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    // Locations Adapter
    private class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationViewHolder> {
        private List<SmartLocation> locations;

        public LocationsAdapter(List<SmartLocation> locations) {
            this.locations = locations;
        }

        @NonNull
        @Override
        public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
            return new LocationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
            SmartLocation location = locations.get(position);
            holder.bind(location);
        }

        @Override
        public int getItemCount() {
            return locations.size();
        }

        class LocationViewHolder extends RecyclerView.ViewHolder {
            private TextView tvName, tvCoordinates, tvRadius, tvProfiles;
            private Button btnEdit, btnDelete, btnToggle;

            public LocationViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_location_name);
                tvCoordinates = itemView.findViewById(R.id.tv_location_coordinates);
                tvRadius = itemView.findViewById(R.id.tv_location_radius);
                tvProfiles = itemView.findViewById(R.id.tv_location_profiles);
                btnEdit = itemView.findViewById(R.id.btn_edit_location);
                btnDelete = itemView.findViewById(R.id.btn_delete_location);
                btnToggle = itemView.findViewById(R.id.btn_toggle_location);
            }

            public void bind(SmartLocation location) {
                tvName.setText(location.getName());
                tvCoordinates.setText(String.format("Lat: %.6f, Lng: %.6f",
                        location.getLatitude(), location.getLongitude()));
                tvRadius.setText("Radius: " + location.getRadius() + "m");

                // Get profile names
                Profile enterProfile = databaseHelper.getProfile(location.getProfileId());
                Profile exitProfile = databaseHelper.getProfile(location.getRevertProfileId());

                String enterProfileName = enterProfile != null ? enterProfile.getName() : "Unknown";
                String exitProfileName = exitProfile != null ? exitProfile.getName() : "Unknown";
                tvProfiles.setText("Enter: " + enterProfileName + " | Exit: " + exitProfileName);

                // Set toggle button state
                btnToggle.setText(location.isEnabled() ? "Disable" : "Enable");
                btnToggle.setBackgroundTintList(getResources().getColorStateList(
                        location.isEnabled() ? R.color.electric_blue : R.color.metallic_silver
                ));

                btnEdit.setOnClickListener(v -> editLocation(location));
                btnDelete.setOnClickListener(v -> deleteLocation(location));
                btnToggle.setOnClickListener(v -> toggleLocation(location));
            }

            private void editLocation(SmartLocation location) {
                // Populate form with location data for editing
                etLocationName.setText(location.getName());
                etRadius.setText(String.valueOf(location.getRadius()));
                etLatitude.setText(String.valueOf(location.getLatitude()));
                etLongitude.setText(String.valueOf(location.getLongitude()));

                // Set profile spinners
                setProfileSpinner(spinnerEnterProfile, location.getProfileId());
                setProfileSpinner(spinnerExitProfile, location.getRevertProfileId());

                // Show manual coordinates
                useManualCoordinates = true;
                etLatitude.setVisibility(View.VISIBLE);
                etLongitude.setVisibility(View.VISIBLE);
                btnUseManualCoordinates.setText("Use Current Location");
                tvCoordinates.setText("Editing coordinates");
                tvCoordinates.setTextColor(getResources().getColor(R.color.aqua_glow));

                // Change add button to update
                btnAddLocation.setText("Update Location");
                btnAddLocation.setOnClickListener(v -> updateLocation(location));

                Toast.makeText(requireContext(), "Editing location: " + location.getName(), Toast.LENGTH_SHORT).show();
            }

            private void setProfileSpinner(Spinner spinner, int profileId) {
                for (int i = 0; i < profilesList.size(); i++) {
                    if (profilesList.get(i).getId() == profileId) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }

            private void updateLocation(SmartLocation location) {
                location.setName(etLocationName.getText().toString().trim());
                location.setRadius(Integer.parseInt(etRadius.getText().toString().trim()));
                location.setLatitude(Double.parseDouble(etLatitude.getText().toString().trim()));
                location.setLongitude(Double.parseDouble(etLongitude.getText().toString().trim()));

                // Get selected profiles
                int enterProfilePosition = spinnerEnterProfile.getSelectedItemPosition();
                int exitProfilePosition = spinnerExitProfile.getSelectedItemPosition();

                if (enterProfilePosition >= 0 && enterProfilePosition < profilesList.size()) {
                    location.setProfileId(profilesList.get(enterProfilePosition).getId());
                }
                if (exitProfilePosition >= 0 && exitProfilePosition < profilesList.size()) {
                    location.setRevertProfileId(profilesList.get(exitProfilePosition).getId());
                }

                boolean updated = databaseHelper.updateLocation(location);
                if (updated) {
                    Toast.makeText(requireContext(), "Location updated", Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadLocations();
                    // Reset add button
                    btnAddLocation.setText("Add Location Rule");
                    btnAddLocation.setOnClickListener(v -> addNewLocation());

                    // Restart location monitoring service
                    startLocationMonitoringService();
                }
            }

            private void deleteLocation(SmartLocation location) {
                boolean deleted = databaseHelper.deleteLocation(location.getId());
                if (deleted) {
                    locations.remove(location);
                    notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Location deleted", Toast.LENGTH_SHORT).show();

                    // Restart location monitoring service
                    startLocationMonitoringService();
                }
            }

            private void toggleLocation(SmartLocation location) {
                location.setEnabled(!location.isEnabled());
                boolean updated = databaseHelper.updateLocation(location);
                if (updated) {
                    notifyDataSetChanged();
                    Toast.makeText(requireContext(),
                            "Location " + (location.isEnabled() ? "enabled" : "disabled"),
                            Toast.LENGTH_SHORT).show();

                    // Restart location monitoring service
                    startLocationMonitoringService();
                }
            }
        }
    }
}