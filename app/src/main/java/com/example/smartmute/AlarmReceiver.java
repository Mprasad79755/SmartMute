package com.example.smartmute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AlarmReceiver triggered");

        if (intent == null || intent.getAction() == null) {
            return;
        }

        // Handle schedule-based alarms
        if (intent.getAction().startsWith("ALARM_ACTION_SCHEDULE_")) {
            handleScheduleAlarm(context, intent);
        }
    }

    private void handleScheduleAlarm(Context context, Intent intent) {
        try {
            int scheduleId = intent.getIntExtra("schedule_id", -1);
            String scheduleName = intent.getStringExtra("schedule_name");
            boolean isStartAlarm = intent.getBooleanExtra("is_start_alarm", true);
            int profileId = intent.getIntExtra("profile_id", -1);

            if (scheduleId == -1 || profileId == -1) {
                Log.e(TAG, "Invalid schedule ID or profile ID in alarm intent");
                return;
            }

            Log.i(TAG, "Processing " + (isStartAlarm ? "start" : "end") +
                    " alarm for schedule: " + scheduleName + " (ID: " + scheduleId + ")");

            SmartMuteDatabaseHelper dbHelper = new SmartMuteDatabaseHelper(context);

            // Get the profile
            Profile profile = dbHelper.getProfile(profileId);
            if (profile == null) {
                Log.e(TAG, "Profile not found for ID: " + profileId);
                dbHelper.close();
                return;
            }

            // Apply the sound profile
            SoundProfileManager soundManager = new SoundProfileManager(context);
            soundManager.applyProfile(profile);

            // Log the event
            String action = isStartAlarm ? "SCHEDULE_START" : "SCHEDULE_END";
            dbHelper.addLog(action, "Schedule: " + scheduleName + " applied profile: " + profile.getName());

            // Reschedule next occurrence if this is a recurring schedule
            if (isStartAlarm) {
                rescheduleNextAlarm(context, scheduleId, true);
            } else {
                rescheduleNextAlarm(context, scheduleId, false);
            }

            dbHelper.close();

            Log.i(TAG, "Successfully applied profile " + profile.getName() +
                    " for schedule: " + scheduleName);

        } catch (Exception e) {
            Log.e(TAG, "Error handling schedule alarm: " + e.getMessage());
        }
    }

    private void rescheduleNextAlarm(Context context, int scheduleId, boolean isStartAlarm) {
        try {
            SmartMuteDatabaseHelper dbHelper = new SmartMuteDatabaseHelper(context);
            Schedule schedule = dbHelper.getSchedule(scheduleId);

            if (schedule != null && schedule.isEnabled()) {
                AlarmUtils alarmUtils = new AlarmUtils(context);
                if (isStartAlarm) {
                    alarmUtils.scheduleStartAlarm(schedule);
                } else {
                    alarmUtils.scheduleEndAlarm(schedule);
                }
                alarmUtils.cleanup();
            }

            dbHelper.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to reschedule alarm: " + e.getMessage());
        }
    }
}