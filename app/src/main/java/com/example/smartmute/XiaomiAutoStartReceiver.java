package com.example.smartmute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.smartmute.SmartMuteService;

public class XiaomiAutoStartReceiver extends BroadcastReceiver {
    private static final String TAG = "XiaomiAutoStart";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Xiaomi auto-start receiver triggered");

        try {
            // Start the main service
            Intent serviceIntent = new Intent(context, SmartMuteService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "SmartMuteService started from Xiaomi receiver");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service from Xiaomi receiver: " + e.getMessage());
        }
    }
}