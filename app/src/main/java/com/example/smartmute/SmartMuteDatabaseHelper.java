package com.example.smartmute;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;



public class SmartMuteDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "smartmute.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_PROFILES = "profiles";
    public static final String TABLE_LOCATIONS = "locations";
    public static final String TABLE_SCHEDULES = "schedules";
    public static final String TABLE_EMERGENCY_CONTACTS = "emergency_contacts";
    public static final String TABLE_LOGS = "logs";

    // Common column names
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_ENABLED = "enabled";

    // Profiles table columns
    public static final String KEY_RINGTONE = "ringtone";
    public static final String KEY_MEDIA_VOLUME = "media_volume";
    public static final String KEY_CALL_VOLUME = "call_volume";
    public static final String KEY_NOTIF_VOLUME = "notif_volume";
    public static final String KEY_VIBRATE = "vibrate";
    public static final String KEY_DND = "dnd";

    // Locations table columns
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_RADIUS = "radius";
    public static final String KEY_PROFILE_ID = "profile_id";
    public static final String KEY_REVERT_PROFILE_ID = "revert_profile_id";

    // Schedules table columns
    public static final String KEY_START_TIME = "start_time";
    public static final String KEY_END_TIME = "end_time";
    public static final String KEY_DAYS_MASK = "days_mask";

    // Emergency contacts table columns
    public static final String KEY_PHONE = "phone";
    public static final String KEY_CALL_COUNT_THRESHOLD = "call_count_threshold";
    public static final String KEY_WINDOW_MINUTES = "window_minutes";
    public static final String KEY_RING_OVERRIDE = "ring_override";

    // Logs table columns
    public static final String KEY_EVENT_TYPE = "event_type";
    public static final String KEY_DETAILS = "details";
    public static final String KEY_CREATED_AT = "created_at";

    public SmartMuteDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createProfilesTable(db);
        createLocationsTable(db);
        createSchedulesTable(db);
        createEmergencyContactsTable(db);
        createLogsTable(db);
    }

    private void createProfilesTable(SQLiteDatabase db) {
        String CREATE_PROFILES_TABLE = "CREATE TABLE " + TABLE_PROFILES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NAME + " TEXT,"
                + KEY_RINGTONE + " TEXT,"
                + KEY_MEDIA_VOLUME + " INTEGER,"
                + KEY_CALL_VOLUME + " INTEGER,"
                + KEY_NOTIF_VOLUME + " INTEGER,"
                + KEY_VIBRATE + " INTEGER,"
                + KEY_DND + " INTEGER"
                + ")";
        db.execSQL(CREATE_PROFILES_TABLE);
    }

    private void createLocationsTable(SQLiteDatabase db) {
        String CREATE_LOCATIONS_TABLE = "CREATE TABLE " + TABLE_LOCATIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NAME + " TEXT,"
                + KEY_LATITUDE + " REAL,"
                + KEY_LONGITUDE + " REAL,"
                + KEY_RADIUS + " INTEGER,"
                + KEY_PROFILE_ID + " INTEGER,"
                + KEY_REVERT_PROFILE_ID + " INTEGER,"
                + KEY_ENABLED + " INTEGER DEFAULT 1"
                + ")";
        db.execSQL(CREATE_LOCATIONS_TABLE);
    }

    private void createSchedulesTable(SQLiteDatabase db) {
        String CREATE_SCHEDULES_TABLE = "CREATE TABLE " + TABLE_SCHEDULES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NAME + " TEXT,"
                + KEY_START_TIME + " TEXT,"
                + KEY_END_TIME + " TEXT,"
                + KEY_DAYS_MASK + " INTEGER,"
                + KEY_PROFILE_ID + " INTEGER,"
                + KEY_ENABLED + " INTEGER DEFAULT 1"
                + ")";
        db.execSQL(CREATE_SCHEDULES_TABLE);
    }

    private void createEmergencyContactsTable(SQLiteDatabase db) {
        String CREATE_EMERGENCY_CONTACTS_TABLE = "CREATE TABLE " + TABLE_EMERGENCY_CONTACTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NAME + " TEXT,"
                + KEY_PHONE + " TEXT,"
                + KEY_CALL_COUNT_THRESHOLD + " INTEGER,"
                + KEY_WINDOW_MINUTES + " INTEGER,"
                + KEY_RING_OVERRIDE + " INTEGER DEFAULT 1"
                + ")";
        db.execSQL(CREATE_EMERGENCY_CONTACTS_TABLE);
    }

    private void createLogsTable(SQLiteDatabase db) {
        String CREATE_LOGS_TABLE = "CREATE TABLE " + TABLE_LOGS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_EVENT_TYPE + " TEXT,"
                + KEY_DETAILS + " TEXT,"
                + KEY_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_LOGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMERGENCY_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        onCreate(db);
    }

    // ==================== PROFILE OPERATIONS ====================

    public long addProfile(Profile profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, profile.getName());
        values.put(KEY_RINGTONE, profile.getRingtone());
        values.put(KEY_MEDIA_VOLUME, profile.getMediaVolume());
        values.put(KEY_CALL_VOLUME, profile.getCallVolume());
        values.put(KEY_NOTIF_VOLUME, profile.getNotificationVolume());
        values.put(KEY_VIBRATE, profile.isVibrate() ? 1 : 0);
        values.put(KEY_DND, profile.isDnd() ? 1 : 0);

        long id = db.insert(TABLE_PROFILES, null, values);
        db.close();
        return id;
    }

    public Profile getProfile(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PROFILES, null, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        Profile profile = null;
        if (cursor != null && cursor.moveToFirst()) {
            profile = new Profile();
            profile.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
            profile.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
            profile.setRingtone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_RINGTONE)));
            profile.setMediaVolume(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MEDIA_VOLUME)));
            profile.setCallVolume(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CALL_VOLUME)));
            profile.setNotificationVolume(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTIF_VOLUME)));
            profile.setVibrate(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_VIBRATE)) == 1);
            profile.setDnd(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DND)) == 1);
            cursor.close();
        }
        return profile;
    }

    public List<Profile> getAllProfiles() {
        List<Profile> profiles = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PROFILES, null);

        if (cursor.moveToFirst()) {
            do {
                Profile profile = new Profile();
                profile.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                profile.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                profile.setRingtone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_RINGTONE)));
                profile.setMediaVolume(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MEDIA_VOLUME)));
                profile.setCallVolume(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CALL_VOLUME)));
                profile.setNotificationVolume(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTIF_VOLUME)));
                profile.setVibrate(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_VIBRATE)) == 1);
                profile.setDnd(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DND)) == 1);
                profiles.add(profile);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return profiles;
    }

    public boolean updateProfile(Profile profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, profile.getName());
        values.put(KEY_RINGTONE, profile.getRingtone());
        values.put(KEY_MEDIA_VOLUME, profile.getMediaVolume());
        values.put(KEY_CALL_VOLUME, profile.getCallVolume());
        values.put(KEY_NOTIF_VOLUME, profile.getNotificationVolume());
        values.put(KEY_VIBRATE, profile.isVibrate() ? 1 : 0);
        values.put(KEY_DND, profile.isDnd() ? 1 : 0);

        int result = db.update(TABLE_PROFILES, values, KEY_ID + " = ?",
                new String[]{String.valueOf(profile.getId())});
        db.close();
        return result > 0;
    }

    public boolean deleteProfile(int profileId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_PROFILES, KEY_ID + " = ?", new String[]{String.valueOf(profileId)});
        db.close();
        return result > 0;
    }

    public boolean isProfileInUse(int profileId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Check if profile is used in locations
        Cursor locationCursor = db.query(TABLE_LOCATIONS, null,
                KEY_PROFILE_ID + " = ? OR " + KEY_REVERT_PROFILE_ID + " = ?",
                new String[]{String.valueOf(profileId), String.valueOf(profileId)},
                null, null, null);
        boolean inLocations = locationCursor.getCount() > 0;
        locationCursor.close();

        // Check if profile is used in schedules
        Cursor scheduleCursor = db.query(TABLE_SCHEDULES, null,
                KEY_PROFILE_ID + " = ?",
                new String[]{String.valueOf(profileId)},
                null, null, null);
        boolean inSchedules = scheduleCursor.getCount() > 0;
        scheduleCursor.close();

        return inLocations || inSchedules;
    }

    // ==================== LOCATION OPERATIONS ====================

    public long addLocation(SmartLocation location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, location.getName());
        values.put(KEY_LATITUDE, location.getLatitude());
        values.put(KEY_LONGITUDE, location.getLongitude());
        values.put(KEY_RADIUS, location.getRadius());
        values.put(KEY_PROFILE_ID, location.getProfileId());
        values.put(KEY_REVERT_PROFILE_ID, location.getRevertProfileId());
        values.put(KEY_ENABLED, location.isEnabled() ? 1 : 0);

        long id = db.insert(TABLE_LOCATIONS, null, values);
        db.close();
        return id;
    }

    public List<SmartLocation> getAllLocations() {
        List<SmartLocation> locations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LOCATIONS + " ORDER BY " + KEY_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                SmartLocation location = new SmartLocation();
                location.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                location.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                location.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LATITUDE)));
                location.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONGITUDE)));
                location.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_RADIUS)));
                location.setProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROFILE_ID)));
                location.setRevertProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_REVERT_PROFILE_ID)));
                location.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ENABLED)) == 1);
                locations.add(location);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return locations;
    }

    public SmartLocation getLocation(int locationId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOCATIONS, null, KEY_ID + "=?",
                new String[]{String.valueOf(locationId)}, null, null, null);

        SmartLocation location = null;
        if (cursor != null && cursor.moveToFirst()) {
            location = new SmartLocation();
            location.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
            location.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
            location.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LATITUDE)));
            location.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONGITUDE)));
            location.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_RADIUS)));
            location.setProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROFILE_ID)));
            location.setRevertProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_REVERT_PROFILE_ID)));
            location.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ENABLED)) == 1);
            cursor.close();
        }
        return location;
    }

    public boolean updateLocation(SmartLocation location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, location.getName());
        values.put(KEY_LATITUDE, location.getLatitude());
        values.put(KEY_LONGITUDE, location.getLongitude());
        values.put(KEY_RADIUS, location.getRadius());
        values.put(KEY_PROFILE_ID, location.getProfileId());
        values.put(KEY_REVERT_PROFILE_ID, location.getRevertProfileId());
        values.put(KEY_ENABLED, location.isEnabled() ? 1 : 0);

        int result = db.update(TABLE_LOCATIONS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(location.getId())});
        db.close();
        return result > 0;
    }

    public boolean deleteLocation(int locationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_LOCATIONS, KEY_ID + " = ?", new String[]{String.valueOf(locationId)});
        db.close();
        return result > 0;
    }

    public List<SmartLocation> getActiveLocations() {
        List<SmartLocation> locations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LOCATIONS + " WHERE " + KEY_ENABLED + " = 1", null);

        if (cursor.moveToFirst()) {
            do {
                SmartLocation location = new SmartLocation();
                location.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                location.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                location.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LATITUDE)));
                location.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONGITUDE)));
                location.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_RADIUS)));
                location.setProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROFILE_ID)));
                location.setRevertProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_REVERT_PROFILE_ID)));
                location.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ENABLED)) == 1);
                locations.add(location);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return locations;
    }

    // ==================== SCHEDULE OPERATIONS ====================

    public long addSchedule(Schedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, schedule.getName());
        values.put(KEY_START_TIME, schedule.getStartTime());
        values.put(KEY_END_TIME, schedule.getEndTime());
        values.put(KEY_DAYS_MASK, schedule.getDaysMask());
        values.put(KEY_PROFILE_ID, schedule.getProfileId());
        values.put(KEY_ENABLED, schedule.isEnabled() ? 1 : 0);

        long id = db.insert(TABLE_SCHEDULES, null, values);
        db.close();
        return id;
    }

    public List<Schedule> getAllSchedules() {
        List<Schedule> schedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SCHEDULES + " ORDER BY " + KEY_START_TIME, null);

        if (cursor.moveToFirst()) {
            do {
                Schedule schedule = new Schedule();
                schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                schedule.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                schedule.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_START_TIME)));
                schedule.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_END_TIME)));
                schedule.setDaysMask(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DAYS_MASK)));
                schedule.setProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROFILE_ID)));
                schedule.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ENABLED)) == 1);
                schedules.add(schedule);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return schedules;
    }

    public Schedule getSchedule(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCHEDULES, null, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        Schedule schedule = null;
        if (cursor != null && cursor.moveToFirst()) {
            schedule = new Schedule();
            schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
            schedule.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
            schedule.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_START_TIME)));
            schedule.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_END_TIME)));
            schedule.setDaysMask(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DAYS_MASK)));
            schedule.setProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROFILE_ID)));
            schedule.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ENABLED)) == 1);
            cursor.close();
        }
        return schedule;
    }

    public List<Schedule> getActiveSchedules() {
        List<Schedule> schedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SCHEDULES + " WHERE " + KEY_ENABLED + " = 1", null);

        if (cursor.moveToFirst()) {
            do {
                Schedule schedule = new Schedule();
                schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                schedule.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                schedule.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_START_TIME)));
                schedule.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(KEY_END_TIME)));
                schedule.setDaysMask(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DAYS_MASK)));
                schedule.setProfileId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROFILE_ID)));
                schedule.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ENABLED)) == 1);
                schedules.add(schedule);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return schedules;
    }

    public boolean updateSchedule(Schedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, schedule.getName());
        values.put(KEY_START_TIME, schedule.getStartTime());
        values.put(KEY_END_TIME, schedule.getEndTime());
        values.put(KEY_DAYS_MASK, schedule.getDaysMask());
        values.put(KEY_PROFILE_ID, schedule.getProfileId());
        values.put(KEY_ENABLED, schedule.isEnabled() ? 1 : 0);

        int result = db.update(TABLE_SCHEDULES, values, KEY_ID + " = ?",
                new String[]{String.valueOf(schedule.getId())});
        db.close();
        return result > 0;
    }

    public boolean deleteSchedule(int scheduleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_SCHEDULES, KEY_ID + " = ?", new String[]{String.valueOf(scheduleId)});
        db.close();
        return result > 0;
    }

    // ==================== EMERGENCY CONTACT OPERATIONS ====================

    public long addEmergencyContact(EmergencyContact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, contact.getName());
        values.put(KEY_PHONE, contact.getPhoneNumber());
        values.put(KEY_CALL_COUNT_THRESHOLD, contact.getCallCountThreshold());
        values.put(KEY_WINDOW_MINUTES, contact.getWindowMinutes());
        values.put(KEY_RING_OVERRIDE, contact.isRingOverride() ? 1 : 0);

        long id = db.insert(TABLE_EMERGENCY_CONTACTS, null, values);
        db.close();
        return id;
    }

    public List<EmergencyContact> getAllEmergencyContacts() {
        List<EmergencyContact> contacts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EMERGENCY_CONTACTS + " ORDER BY " + KEY_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                EmergencyContact contact = new EmergencyContact();
                contact.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                contact.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE)));
                contact.setCallCountThreshold(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CALL_COUNT_THRESHOLD)));
                contact.setWindowMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WINDOW_MINUTES)));
                contact.setRingOverride(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_RING_OVERRIDE)) == 1);
                contacts.add(contact);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return contacts;
    }

    public boolean updateEmergencyContact(EmergencyContact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, contact.getName());
        values.put(KEY_PHONE, contact.getPhoneNumber());
        values.put(KEY_CALL_COUNT_THRESHOLD, contact.getCallCountThreshold());
        values.put(KEY_WINDOW_MINUTES, contact.getWindowMinutes());
        values.put(KEY_RING_OVERRIDE, contact.isRingOverride() ? 1 : 0);

        int result = db.update(TABLE_EMERGENCY_CONTACTS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(contact.getId())});
        db.close();
        return result > 0;
    }

    public boolean deleteEmergencyContact(int contactId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_EMERGENCY_CONTACTS, KEY_ID + " = ?", new String[]{String.valueOf(contactId)});
        db.close();
        return result > 0;
    }

    public boolean isEmergencyContact(String phoneNumber) {
        String normalizedInput = normalizePhoneNumber(phoneNumber);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EMERGENCY_CONTACTS, new String[]{KEY_PHONE}, null, null, null, null, null);

        boolean isEmergency = false;
        if (cursor.moveToFirst()) {
            do {
                String storedPhone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE));
                String normalizedStored = normalizePhoneNumber(storedPhone);
                if (normalizedInput.equals(normalizedStored)) {
                    isEmergency = true;
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return isEmergency;
    }

    public EmergencyContact getEmergencyContactByPhone(String phoneNumber) {
        String normalizedInput = normalizePhoneNumber(phoneNumber);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EMERGENCY_CONTACTS, null, null, null, null, null, null);

        EmergencyContact contact = null;
        if (cursor.moveToFirst()) {
            do {
                String storedPhone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE));
                String normalizedStored = normalizePhoneNumber(storedPhone);
                if (normalizedInput.equals(normalizedStored)) {
                    contact = new EmergencyContact();
                    contact.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                    contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                    contact.setPhoneNumber(storedPhone);
                    contact.setCallCountThreshold(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CALL_COUNT_THRESHOLD)));
                    contact.setWindowMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WINDOW_MINUTES)));
                    contact.setRingOverride(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_RING_OVERRIDE)) == 1);
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return contact;
    }

    public int getCallThreshold(String phoneNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EMERGENCY_CONTACTS,
                new String[]{KEY_CALL_COUNT_THRESHOLD},
                KEY_PHONE + " = ?",
                new String[]{phoneNumber}, null, null, null);

        int threshold = 3; // default
        if (cursor.moveToFirst()) {
            threshold = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CALL_COUNT_THRESHOLD));
        }
        cursor.close();
        return threshold;
    }

    // ==================== LOG OPERATIONS ====================

    public void addLog(String eventType, String details) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_EVENT_TYPE, eventType);
        values.put(KEY_DETAILS, details);
        db.insert(TABLE_LOGS, null, values);
    }

    public void logCallEvent(String phoneNumber, Date date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_EVENT_TYPE, "INCOMING_CALL");
        values.put(KEY_DETAILS, "Call from: " + phoneNumber);
        db.insert(TABLE_LOGS, null, values);
    }

    // ==================== UTILITY METHODS ====================

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        return phoneNumber.replaceAll("[^\\d+]", "");
    }

    public int getRecentCallCount(String phoneNumber, int windowMinutes) {
        // This would query the call logs table for recent calls from this number
        // For now, return a mock value
        return 0;
    }
} // This closes the SmartMuteDatabaseHelper class

// Add Profile class definition here
class Profile {
    private int id;
    private String name;
    private String ringtone;
    private int mediaVolume;
    private int callVolume;
    private int notificationVolume;
    private boolean vibrate;
    private boolean dnd;

    public Profile() {}

    public Profile(int id, String name, String ringtone, int mediaVolume, int callVolume,
                   int notificationVolume, boolean vibrate, boolean dnd) {
        this.id = id;
        this.name = name;
        this.ringtone = ringtone;
        this.mediaVolume = mediaVolume;
        this.callVolume = callVolume;
        this.notificationVolume = notificationVolume;
        this.vibrate = vibrate;
        this.dnd = dnd;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRingtone() { return ringtone; }
    public void setRingtone(String ringtone) { this.ringtone = ringtone; }
    public int getMediaVolume() { return mediaVolume; }
    public void setMediaVolume(int mediaVolume) { this.mediaVolume = mediaVolume; }
    public int getCallVolume() { return callVolume; }
    public void setCallVolume(int callVolume) { this.callVolume = callVolume; }
    public int getNotificationVolume() { return notificationVolume; }
    public void setNotificationVolume(int notificationVolume) { this.notificationVolume = notificationVolume; }
    public boolean isVibrate() { return vibrate; }
    public void setVibrate(boolean vibrate) { this.vibrate = vibrate; }
    public boolean isDnd() { return dnd; }
    public void setDnd(boolean dnd) { this.dnd = dnd; }
}