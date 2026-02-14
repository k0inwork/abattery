package com.example.batteryalert;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
    private Button startStopButton;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thresholdSeekBar = findViewById(R.id.thresholdSeekBar);
        thresholdText = findViewById(R.id.thresholdText);
        startStopButton = findViewById(R.id.startStopButton);

        // Load running state
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        isServiceRunning = prefs.getBoolean(BatteryService.KEY_RUNNING, false);
        updateButtonText();

        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdText.setText("Alert Threshold: " + progress + "%");
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

    private void updateButtonText() {
        if (isServiceRunning) {
            startStopButton.setText("Stop Monitoring");
        } else {
            startStopButton.setText("Start Monitoring");
        }
    }

    private void toggleService() {
        Intent serviceIntent = new Intent(this, BatteryService.class);
        if (!isServiceRunning) {
            serviceIntent.putExtra("threshold", thresholdSeekBar.getProgress());
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
