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
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private SeekBar thresholdSeekBar;
    private TextView thresholdText;
    private SeekBar volumeSeekBar;
    private TextView volumeText;
    private EditText alertNormalEdit;
    private EditText alertUrgentEdit;
    private EditText alertCriticalEdit;
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
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        volumeText = findViewById(R.id.volumeText);
        alertNormalEdit = findViewById(R.id.alertNormalEdit);
        alertUrgentEdit = findViewById(R.id.alertUrgentEdit);
        alertCriticalEdit = findViewById(R.id.alertCriticalEdit);
        batteryLevelText = findViewById(R.id.batteryLevelText);
        startStopButton = findViewById(R.id.startStopButton);

        // Load preferences
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        isServiceRunning = prefs.getBoolean(BatteryService.KEY_RUNNING, false);
        int savedThreshold = prefs.getInt(BatteryService.KEY_THRESHOLD, 20);
        int savedVolume = prefs.getInt(BatteryService.KEY_VOLUME, 100);
        String savedNormal = prefs.getString("alert_normal_text", getString(R.string.alert_normal));
        String savedUrgent = prefs.getString("alert_urgent_text", getString(R.string.alert_urgent));
        String savedCritical = prefs.getString("alert_critical_text", getString(R.string.alert_critical));

        thresholdSeekBar.setProgress(savedThreshold);
        thresholdText.setText(getString(R.string.alert_threshold, savedThreshold));

        volumeSeekBar.setProgress(savedVolume);
        volumeText.setText(getString(R.string.volume_label, savedVolume));

        alertNormalEdit.setText(savedNormal);
        alertUrgentEdit.setText(savedUrgent);
        alertCriticalEdit.setText(savedCritical);

        updateButtonText();

        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdText.setText(getString(R.string.alert_threshold, progress));
                if (isServiceRunning && fromUser) {
                    updateService();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeText.setText(getString(R.string.volume_label, progress));
                if (isServiceRunning && fromUser) {
                    updateService();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isServiceRunning) {
                    updateService();
                }
            }
        };

        alertNormalEdit.addTextChangedListener(textWatcher);
        alertUrgentEdit.addTextChangedListener(textWatcher);
        alertCriticalEdit.addTextChangedListener(textWatcher);

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

        // Refresh service running state
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        isServiceRunning = prefs.getBoolean(BatteryService.KEY_RUNNING, false);
        updateButtonText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryInfoReceiver);

        // Save alert texts when leaving
        saveAlertTexts();
    }

    private void saveAlertTexts() {
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
            .putString("alert_normal_text", alertNormalEdit.getText().toString())
            .putString("alert_urgent_text", alertUrgentEdit.getText().toString())
            .putString("alert_critical_text", alertCriticalEdit.getText().toString())
            .apply();
    }

    private void updateButtonText() {
        if (isServiceRunning) {
            startStopButton.setText(R.string.stop_monitoring);
        } else {
            startStopButton.setText(R.string.start_monitoring);
        }
    }

    private void updateService() {
        saveAlertTexts();
        Intent serviceIntent = new Intent(this, BatteryService.class);
        serviceIntent.putExtra("threshold", thresholdSeekBar.getProgress());
        serviceIntent.putExtra("volume", volumeSeekBar.getProgress());
        serviceIntent.putExtra("alert_normal", alertNormalEdit.getText().toString());
        serviceIntent.putExtra("alert_urgent", alertUrgentEdit.getText().toString());
        serviceIntent.putExtra("alert_critical", alertCriticalEdit.getText().toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void toggleService() {
        Intent serviceIntent = new Intent(this, BatteryService.class);
        if (!isServiceRunning) {
            updateService();
            isServiceRunning = true;
        } else {
            stopService(serviceIntent);
            isServiceRunning = false;
        }
        updateButtonText();
    }
}
