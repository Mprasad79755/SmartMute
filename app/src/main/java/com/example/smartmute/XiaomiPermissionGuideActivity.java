package com.example.smartmute;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class XiaomiPermissionGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xiaomi_guide);

        Button btnAutoStart = findViewById(R.id.btn_auto_start_setting);
        Button btnBattery = findViewById(R.id.btn_battery_setting);
        Button btnDone = findViewById(R.id.btn_done);

        btnAutoStart.setOnClickListener(v -> openAutoStartSettings());
        btnBattery.setOnClickListener(v -> openBatterySettings());
        btnDone.setOnClickListener(v -> finish());
    }

    private void openAutoStartSettings() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to general settings
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void openBatterySettings() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new android.content.ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            ));
            intent.putExtra("package_name", getPackageName());
            intent.putExtra("package_label", getString(R.string.app_name));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to general battery settings
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        }
    }
}