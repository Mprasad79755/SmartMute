package com.example.smartmute;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;

public class XiaomiUtils {
    private static final String TAG = "XiaomiUtils";

    public static boolean isXiaomiDevice() {
        return android.os.Build.MANUFACTURER.equalsIgnoreCase("xiaomi");
    }

    public static void showAutoStartPermissionDialog(Context context) {
        if (!isXiaomiDevice()) return;

        new AlertDialog.Builder(context)
                .setTitle("Xiaomi Auto-start Permission")
                .setMessage("For SmartMute to work properly on Xiaomi devices, please enable auto-start permission in Security app to allow background operation.")
                .setPositiveButton("Open Settings", (dialog, which) -> openAutoStartSettings(context))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void openAutoStartSettings(Context context) {
        try {
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
                intent.setPackage("com.miui.securitycenter");
                intent.putExtra("extra_pkgname", context.getPackageName());
                context.startActivity(intent);
            } catch (Exception e2) {
                Log.e(TAG, "Failed to open Xiaomi auto-start settings");
                // Open general app info as fallback
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            }
        }
    }
}