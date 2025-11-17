package com.example.smartmute;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SchedulesFragment extends Fragment {

    private RecyclerView schedulesRecyclerView;
    private SchedulesAdapter schedulesAdapter;
    private List<Schedule> schedulesList;
    private SmartMuteDatabaseHelper databaseHelper;

    private Button btnAddSchedule, btnStartTime, btnEndTime;
    private EditText etScheduleName;
    private TextView tvStartTime, tvEndTime;
    private Spinner spinnerProfile;
    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;

    private String selectedStartTime = "";
    private String selectedEndTime = "";
    private List<Profile> profilesList;

    public SchedulesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedules, container, false);

        initializeViews(view);
        setupDatabase();
        setupRecyclerView();
        loadProfiles();
        loadSchedules();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        schedulesRecyclerView = view.findViewById(R.id.schedules_recycler_view);
        btnAddSchedule = view.findViewById(R.id.btn_add_schedule);
        btnStartTime = view.findViewById(R.id.btn_start_time);
        btnEndTime = view.findViewById(R.id.btn_end_time);
        etScheduleName = view.findViewById(R.id.et_schedule_name);
        tvStartTime = view.findViewById(R.id.tv_start_time);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        spinnerProfile = view.findViewById(R.id.spinner_profile);

        // Day checkboxes
        cbMonday = view.findViewById(R.id.cb_monday);
        cbTuesday = view.findViewById(R.id.cb_tuesday);
        cbWednesday = view.findViewById(R.id.cb_wednesday);
        cbThursday = view.findViewById(R.id.cb_thursday);
        cbFriday = view.findViewById(R.id.cb_friday);
        cbSaturday = view.findViewById(R.id.cb_saturday);
        cbSunday = view.findViewById(R.id.cb_sunday);
    }

    private void setupDatabase() {
        databaseHelper = new SmartMuteDatabaseHelper(requireContext());
    }

    private void setupRecyclerView() {
        schedulesList = new ArrayList<>();
        schedulesAdapter = new SchedulesAdapter(schedulesList);
        schedulesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        schedulesRecyclerView.setAdapter(schedulesAdapter);
    }

    private void loadProfiles() {
        profilesList = databaseHelper.getAllProfiles();

        // Create adapter for spinner
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
        spinnerProfile.setAdapter(adapter);
    }

    private void loadSchedules() {
        schedulesList.clear();
        schedulesList.addAll(databaseHelper.getAllSchedules());
        schedulesAdapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        btnStartTime.setOnClickListener(v -> showTimePicker(true));
        btnEndTime.setOnClickListener(v -> showTimePicker(false));
        btnAddSchedule.setOnClickListener(v -> addNewSchedule());
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute1) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute1);
                    if (isStartTime) {
                        selectedStartTime = time;
                        tvStartTime.setText(time);
                        tvStartTime.setTextColor(getResources().getColor(R.color.aqua_glow));
                    } else {
                        selectedEndTime = time;
                        tvEndTime.setText(time);
                        tvEndTime.setTextColor(getResources().getColor(R.color.aqua_glow));
                    }
                },
                hour, minute, true
        );

        timePickerDialog.setTitle(isStartTime ? "Select Start Time" : "Select End Time");
        timePickerDialog.show();
    }

    private void addNewSchedule() {
        String name = etScheduleName.getText().toString().trim();

        if (name.isEmpty()) {
            etScheduleName.setError("Schedule name is required");
            return;
        }

        if (selectedStartTime.isEmpty()) {
            Toast.makeText(requireContext(), "Please select start time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedEndTime.isEmpty()) {
            Toast.makeText(requireContext(), "Please select end time", Toast.LENGTH_SHORT).show();
            return;
        }

        int daysMask = calculateDaysMask();
        if (daysMask == 0) {
            Toast.makeText(requireContext(), "Please select at least one day", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected profile
        int selectedPosition = spinnerProfile.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= profilesList.size()) {
            Toast.makeText(requireContext(), "Please select a profile", Toast.LENGTH_SHORT).show();
            return;
        }

        Profile selectedProfile = profilesList.get(selectedPosition);

        Schedule schedule = new Schedule();
        schedule.setName(name);
        schedule.setStartTime(selectedStartTime);
        schedule.setEndTime(selectedEndTime);
        schedule.setDaysMask(daysMask);
        schedule.setProfileId(selectedProfile.getId());
        schedule.setEnabled(true);

        long id = databaseHelper.addSchedule(schedule);
        if (id != -1) {
            schedule.setId((int) id);

            // Schedule both start and end alarms
            AlarmUtils alarmUtils = new AlarmUtils(requireContext());
            alarmUtils.scheduleAlarmsForSchedule(schedule);
            alarmUtils.cleanup();

            Toast.makeText(requireContext(), "Schedule added successfully", Toast.LENGTH_SHORT).show();
            clearForm();
            loadSchedules();
        } else {
            Toast.makeText(requireContext(), "Failed to add schedule", Toast.LENGTH_SHORT).show();
        }
    }

    private int calculateDaysMask() {
        int mask = 0;
        if (cbMonday.isChecked()) mask |= (1 << 1); // Monday = bit 1
        if (cbTuesday.isChecked()) mask |= (1 << 2);
        if (cbWednesday.isChecked()) mask |= (1 << 3);
        if (cbThursday.isChecked()) mask |= (1 << 4);
        if (cbFriday.isChecked()) mask |= (1 << 5);
        if (cbSaturday.isChecked()) mask |= (1 << 6);
        if (cbSunday.isChecked()) mask |= (1 << 0); // Sunday = bit 0
        return mask;
    }

    private void clearForm() {
        etScheduleName.setText("");
        selectedStartTime = "";
        selectedEndTime = "";
        tvStartTime.setText("Not set");
        tvEndTime.setText("Not set");
        tvStartTime.setTextColor(getResources().getColor(R.color.metallic_silver));
        tvEndTime.setTextColor(getResources().getColor(R.color.metallic_silver));

        // Clear all checkboxes
        cbMonday.setChecked(false);
        cbTuesday.setChecked(false);
        cbWednesday.setChecked(false);
        cbThursday.setChecked(false);
        cbFriday.setChecked(false);
        cbSaturday.setChecked(false);
        cbSunday.setChecked(false);

        // Reset spinner
        if (spinnerProfile.getAdapter() != null && spinnerProfile.getAdapter().getCount() > 0) {
            spinnerProfile.setSelection(0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    // Schedules Adapter
    private class SchedulesAdapter extends RecyclerView.Adapter<SchedulesAdapter.ScheduleViewHolder> {
        private List<Schedule> schedules;

        public SchedulesAdapter(List<Schedule> schedules) {
            this.schedules = schedules;
        }

        @NonNull
        @Override
        public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
            return new ScheduleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
            Schedule schedule = schedules.get(position);
            holder.bind(schedule);
        }

        @Override
        public int getItemCount() {
            return schedules.size();
        }

        class ScheduleViewHolder extends RecyclerView.ViewHolder {
            private TextView tvName, tvTime, tvDays, tvProfile;
            private Button btnEdit, btnDelete, btnToggle;

            public ScheduleViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_schedule_name);
                tvTime = itemView.findViewById(R.id.tv_schedule_time);
                tvDays = itemView.findViewById(R.id.tv_schedule_days);
                tvProfile = itemView.findViewById(R.id.tv_schedule_profile);
                btnEdit = itemView.findViewById(R.id.btn_edit_schedule);
                btnDelete = itemView.findViewById(R.id.btn_delete_schedule);
                btnToggle = itemView.findViewById(R.id.btn_toggle_schedule);
            }

            public void bind(Schedule schedule) {
                tvName.setText(schedule.getName());
                tvTime.setText(schedule.getStartTime() + " - " + schedule.getEndTime());
                tvDays.setText(getDaysString(schedule.getDaysMask()));

                // Get profile name
                Profile profile = databaseHelper.getProfile(schedule.getProfileId());
                if (profile != null) {
                    tvProfile.setText("Profile: " + profile.getName());
                } else {
                    tvProfile.setText("Profile: Unknown");
                }

                // Set toggle button state
                btnToggle.setText(schedule.isEnabled() ? "Disable" : "Enable");
                btnToggle.setBackgroundTintList(getResources().getColorStateList(
                        schedule.isEnabled() ? R.color.electric_blue : R.color.metallic_silver
                ));

                btnEdit.setOnClickListener(v -> editSchedule(schedule));
                btnDelete.setOnClickListener(v -> deleteSchedule(schedule));
                btnToggle.setOnClickListener(v -> toggleSchedule(schedule));
            }

            private String getDaysString(int daysMask) {
                StringBuilder days = new StringBuilder();
                String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

                for (int i = 0; i < 7; i++) {
                    if ((daysMask & (1 << i)) != 0) {
                        if (days.length() > 0) days.append(", ");
                        days.append(dayNames[i]);
                    }
                }
                return days.toString();
            }

            private void editSchedule(Schedule schedule) {
                // Populate form with schedule data for editing
                etScheduleName.setText(schedule.getName());
                selectedStartTime = schedule.getStartTime();
                selectedEndTime = schedule.getEndTime();
                tvStartTime.setText(selectedStartTime);
                tvEndTime.setText(selectedEndTime);
                tvStartTime.setTextColor(getResources().getColor(R.color.aqua_glow));
                tvEndTime.setTextColor(getResources().getColor(R.color.aqua_glow));

                // Set days checkboxes
                setDaysCheckboxes(schedule.getDaysMask());

                // Set profile spinner
                setProfileSpinner(schedule.getProfileId());

                // Change add button to update
                btnAddSchedule.setText("Update Schedule");
                btnAddSchedule.setOnClickListener(v -> updateSchedule(schedule));

                Toast.makeText(requireContext(), "Editing schedule: " + schedule.getName(), Toast.LENGTH_SHORT).show();
            }

            private void setDaysCheckboxes(int daysMask) {
                cbMonday.setChecked((daysMask & (1 << 1)) != 0);
                cbTuesday.setChecked((daysMask & (1 << 2)) != 0);
                cbWednesday.setChecked((daysMask & (1 << 3)) != 0);
                cbThursday.setChecked((daysMask & (1 << 4)) != 0);
                cbFriday.setChecked((daysMask & (1 << 5)) != 0);
                cbSaturday.setChecked((daysMask & (1 << 6)) != 0);
                cbSunday.setChecked((daysMask & (1 << 0)) != 0);
            }

            private void setProfileSpinner(int profileId) {
                for (int i = 0; i < profilesList.size(); i++) {
                    if (profilesList.get(i).getId() == profileId) {
                        spinnerProfile.setSelection(i);
                        break;
                    }
                }
            }

            private void updateSchedule(Schedule schedule) {
                schedule.setName(etScheduleName.getText().toString().trim());
                schedule.setStartTime(selectedStartTime);
                schedule.setEndTime(selectedEndTime);
                schedule.setDaysMask(calculateDaysMask());

                // Get selected profile
                int selectedPosition = spinnerProfile.getSelectedItemPosition();
                if (selectedPosition >= 0 && selectedPosition < profilesList.size()) {
                    schedule.setProfileId(profilesList.get(selectedPosition).getId());
                }

                boolean updated = databaseHelper.updateSchedule(schedule);
                if (updated) {
                    // Update alarms
                    AlarmUtils alarmUtils = new AlarmUtils(requireContext());
                    alarmUtils.cancelAlarmsForSchedule(schedule.getId());
                    if (schedule.isEnabled()) {
                        alarmUtils.scheduleAlarmsForSchedule(schedule);
                    }
                    alarmUtils.cleanup();

                    Toast.makeText(requireContext(), "Schedule updated", Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadSchedules();
                    // Reset add button
                    btnAddSchedule.setText("Add Schedule");
                    btnAddSchedule.setOnClickListener(v -> addNewSchedule());
                }
            }

            private void deleteSchedule(Schedule schedule) {
                // Cancel alarms first
                AlarmUtils alarmUtils = new AlarmUtils(requireContext());
                alarmUtils.cancelAlarmsForSchedule(schedule.getId());
                alarmUtils.cleanup();

                boolean deleted = databaseHelper.deleteSchedule(schedule.getId());
                if (deleted) {
                    schedules.remove(schedule);
                    notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Schedule deleted", Toast.LENGTH_SHORT).show();
                }
            }

            private void toggleSchedule(Schedule schedule) {
                schedule.setEnabled(!schedule.isEnabled());

                // Update in database
                boolean updated = databaseHelper.updateSchedule(schedule);
                if (updated) {
                    // Update alarms
                    AlarmUtils alarmUtils = new AlarmUtils(requireContext());
                    if (schedule.isEnabled()) {
                        alarmUtils.scheduleAlarmsForSchedule(schedule);
                    } else {
                        alarmUtils.cancelAlarmsForSchedule(schedule.getId());
                    }
                    alarmUtils.cleanup();

                    notifyDataSetChanged();
                    Toast.makeText(requireContext(),
                            "Schedule " + (schedule.isEnabled() ? "enabled" : "disabled"),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}