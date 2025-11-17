package com.example.smartmute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AlternativePhoneStateReceiver extends BroadcastReceiver {

    private static final String TAG = "AltPhoneStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            Log.d(TAG, "State: " + state + ", Number: " + incomingNumber);

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                if (incomingNumber == null || incomingNumber.isEmpty()) {
                    Log.w(TAG, "Incoming number is null, trying alternative method...");

                    // Try to get number from telephony manager
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (tm != null) {
                        try {
                            // This might work on some devices
                            String lineNumber = tm.getLine1Number();
                            Log.d(TAG, "Line number: " + lineNumber);
                        } catch (SecurityException e) {
                            Log.e(TAG, "No permission to get line number");
                        }
                    }

                    // Try another approach - use the fact that we know it's ringing
                    // and check recent calls database (requires additional permissions)
                    checkRecentCalls(context);
                } else {
                    // We have the number, process normally
                    processCall(context, incomingNumber);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in receiver: " + e.getMessage());
        }
    }

    private void processCall(Context context, String number) {
        // Your existing call processing logic here
        Log.i(TAG, "Processing call from: " + number);
    }

    private void checkRecentCalls(Context context) {
        // This would require READ_CALL_LOG permission
        // Not recommended due to privacy concerns
        Log.w(TAG, "Cannot access call number without additional permissions");
    }
}