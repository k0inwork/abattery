package com.example.batteryalert;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private SeekBar thresholdSeekBar;
    private TextView thresholdText;
    private TextView batteryLevelText;
    private Button startStopButton;
    private boolean isServiceRunning = false;

    private final BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level != -1 && scale != -1) {
                int batteryPct = (int) (level * 100 / (float) scale);
                batteryLevelText.setText(getString(R.string.current_battery, batteryPct));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thresholdSeekBar = findViewById(R.id.thresholdSeekBar);
        thresholdText = findViewById(R.id.thresholdText);
        batteryLevelText = findViewById(R.id.batteryLevelText);
        startStopButton = findViewById(R.id.startStopButton);

        // Load preferences
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        isServiceRunning = prefs.getBoolean(BatteryService.KEY_RUNNING, false);
        int savedThreshold = prefs.getInt(BatteryService.KEY_THRESHOLD, 20);

        thresholdSeekBar.setProgress(savedThreshold);
        thresholdText.setText(getString(R.string.alert_threshold, savedThreshold));
        updateButtonText();

        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdText.setText(getString(R.string.alert_threshold, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        startStopButton.setOnClickListener(v -> toggleService());

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryInfoReceiver);
    }

    private void updateButtonText() {
        if (isServiceRunning) {
            startStopButton.setText(R.string.stop_monitoring);
        } else {
            startStopButton.setText(R.string.start_monitoring);
        }
    }

    private void toggleService() {
        Intent serviceIntent = new Intent(this, BatteryService.class);
        if (!isServiceRunning) {
            int progress = thresholdSeekBar.getProgress();
            serviceIntent.putExtra("threshold", progress);

            // Save it now
            SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putInt(BatteryService.KEY_THRESHOLD, progress).apply();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            isServiceRunning = true;
        } else {
            stopService(serviceIntent);
            isServiceRunning = false;
        }
        updateButtonText();
    }
}
