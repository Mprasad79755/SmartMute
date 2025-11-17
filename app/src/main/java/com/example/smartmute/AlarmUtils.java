package com.example.smartmute;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

public class AlarmUtils {
    private static final String TAG = "AlarmUtils";

    private Context context;
    private AlarmManager alarmManager;
    private SmartMuteDatabaseHelper databaseHelper;

    public AlarmUtils(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.databaseHelper = new SmartMuteDatabaseHelper(context);
    }

    public void scheduleAlarmsForSchedule(Schedule schedule) {
        scheduleStartAlarm(schedule);
        scheduleEndAlarm(schedule);
    }

    void scheduleStartAlarm(Schedule schedule) {
        try {
            Calendar triggerTime = calculateNextTriggerTime(schedule, true); // Start time
            if (triggerTime != null) {
                scheduleAlarm(schedule.getId(), schedule.getName(), triggerTime.getTimeInMillis(),
                        schedule.getDaysMask(), true, schedule.getProfileId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule start alarm for: " + schedule.getName());
        }
    }

    void scheduleEndAlarm(Schedule schedule) {
        try {
            Calendar triggerTime = calculateNextTriggerTime(schedule, false); // End time
            if (triggerTime != null) {
                // For end time, we need to revert to normal profile
                // You might want to store a "default" profile ID or use profile ID 1 for normal
                int normalProfileId = 1; // Assuming profile ID 1 is Normal
                scheduleAlarm(schedule.getId(), schedule.getName(), triggerTime.getTimeInMillis(),
                        schedule.getDaysMask(), false, normalProfileId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule end alarm for: " + schedule.getName());
        }
    }

    public void scheduleAlarm(int scheduleId, String scheduleName, long triggerTime,
                              int daysMask, boolean isStartAlarm, int profileId) {
        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction("ALARM_ACTION_SCHEDULE_" + scheduleId + "_" + (isStartAlarm ? "START" : "END"));
            intent.putExtra("schedule_id", scheduleId);
            intent.putExtra("schedule_name", scheduleName);
            intent.putExtra("is_start_alarm", isStartAlarm);
            intent.putExtra("profile_id", profileId);

            // Use different request codes for start and end alarms
            int requestCode = scheduleId * 10 + (isStartAlarm ? 1 : 2);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        setExactAlarm(alarmManager, triggerTime, pendingIntent);
                    } else {
                        // Request exact alarm permission
                        Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(settingsIntent);
                        return;
                    }
                } else {
                    setExactAlarm(alarmManager, triggerTime, pendingIntent);
                }
            }

            Log.d(TAG, "Scheduled " + (isStartAlarm ? "start" : "end") + " alarm for: " +
                    scheduleName + " at " + triggerTime + " with profile: " + profileId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm: " + e.getMessage());
        }
    }

    private void setExactAlarm(AlarmManager alarmManager, long triggerTime, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public void cancelAlarmsForSchedule(int scheduleId) {
        cancelAlarm(scheduleId, true);  // Cancel start alarm
        cancelAlarm(scheduleId, false); // Cancel end alarm
    }

    public void cancelAlarm(int scheduleId, boolean isStartAlarm) {
        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction("ALARM_ACTION_SCHEDULE_" + scheduleId + "_" + (isStartAlarm ? "START" : "END"));

            int requestCode = scheduleId * 10 + (isStartAlarm ? 1 : 2);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            pendingIntent.cancel();

            Log.d(TAG, "Cancelled " + (isStartAlarm ? "start" : "end") + " alarm for schedule ID: " + scheduleId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel alarm: " + e.getMessage());
        }
    }

    private Calendar calculateNextTriggerTime(Schedule schedule, boolean isStartTime) {
        try {
            Calendar now = Calendar.getInstance();
            Calendar triggerTime = Calendar.getInstance();

            // Parse time (format: "HH:mm")
            String timeString = isStartTime ? schedule.getStartTime() : schedule.getEndTime();
            String[] timeParts = timeString.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            triggerTime.set(Calendar.HOUR_OF_DAY, hour);
            triggerTime.set(Calendar.MINUTE, minute);
            triggerTime.set(Calendar.SECOND, 0);
            triggerTime.set(Calendar.MILLISECOND, 0);

            // If the time has already passed today, move to next day
            if (triggerTime.before(now)) {
                triggerTime.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Find the next day that matches the days mask
            while (!isDayInMask(triggerTime.get(Calendar.DAY_OF_WEEK), schedule.getDaysMask())) {
                triggerTime.add(Calendar.DAY_OF_YEAR, 1);

                // Safety check to prevent infinite loop
                if (triggerTime.get(Calendar.YEAR) > now.get(Calendar.YEAR) + 1) {
                    return null;
                }
            }

            return triggerTime;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating next trigger time: " + e.getMessage());
            return null;
        }
    }

    private boolean isDayInMask(int dayOfWeek, int daysMask) {
        // Convert Calendar day of week to our mask bit (Sunday = 1, Monday = 2, etc.)
        int bitPosition = dayOfWeek - 1; // Calendar.SUNDAY = 1, so bit 0 for Sunday
        return (daysMask & (1 << bitPosition)) != 0;
    }

    public void restoreAllScheduledAlarms() {
        try {
            List<Schedule> activeSchedules = databaseHelper.getActiveSchedules();
            for (Schedule schedule : activeSchedules) {
                scheduleAlarmsForSchedule(schedule);
            }
            Log.i(TAG, "Restored " + activeSchedules.size() + " scheduled alarms");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore scheduled alarms: " + e.getMessage());
        }
    }

    public void cleanup() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}