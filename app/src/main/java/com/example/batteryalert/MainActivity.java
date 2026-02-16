package com.example.batteryalert;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private SeekBar thresholdSeekBar;
    private TextView thresholdText;
    private SeekBar volumeSeekBar;
    private TextView volumeText;
    private SeekBar urgentOffsetSeekBar;
    private TextView urgentOffsetLabel;
    private SeekBar criticalOffsetSeekBar;
    private TextView criticalOffsetLabel;
    private EditText alertNormalEdit;
    private EditText alertUrgentEdit;
    private EditText alertCriticalEdit;
    private TextView audioNormalPath, audioUrgentPath, audioCriticalPath;
    private Button btnSelectAudioNormal, btnClearAudioNormal;
    private Button btnSelectAudioUrgent, btnClearAudioUrgent;
    private Button btnSelectAudioCritical, btnClearAudioCritical;
    private TextView batteryLevelText;
    private MaterialButton startStopButton;
    private boolean isServiceRunning = false;

    private String uriNormal, uriUrgent, uriCritical;

    private final ActivityResultLauncher<String[]> pickAudioNormal = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> handleAudioPick(uri, "normal"));
    private final ActivityResultLauncher<String[]> pickAudioUrgent = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> handleAudioPick(uri, "urgent"));
    private final ActivityResultLauncher<String[]> pickAudioCritical = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> handleAudioPick(uri, "critical"));

    private void handleAudioPick(Uri uri, String level) {
        if (uri != null) {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String uriString = uri.toString();
            if ("normal".equals(level)) {
                uriNormal = uriString;
            } else if ("urgent".equals(level)) {
                uriUrgent = uriString;
            } else if ("critical".equals(level)) {
                uriCritical = uriString;
            }
            updateAudioLabels();
            saveAudioUris();
            if (isServiceRunning) updateService();
        }
    }

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
        urgentOffsetSeekBar = findViewById(R.id.urgentOffsetSeekBar);
        urgentOffsetLabel = findViewById(R.id.urgentOffsetLabel);
        criticalOffsetSeekBar = findViewById(R.id.criticalOffsetSeekBar);
        criticalOffsetLabel = findViewById(R.id.criticalOffsetLabel);
        alertNormalEdit = findViewById(R.id.alertNormalEdit);
        alertUrgentEdit = findViewById(R.id.alertUrgentEdit);
        alertCriticalEdit = findViewById(R.id.alertCriticalEdit);
        audioNormalPath = findViewById(R.id.audioNormalPath);
        audioUrgentPath = findViewById(R.id.audioUrgentPath);
        audioCriticalPath = findViewById(R.id.audioCriticalPath);
        btnSelectAudioNormal = findViewById(R.id.btnSelectAudioNormal);
        btnClearAudioNormal = findViewById(R.id.btnClearAudioNormal);
        btnSelectAudioUrgent = findViewById(R.id.btnSelectAudioUrgent);
        btnClearAudioUrgent = findViewById(R.id.btnClearAudioUrgent);
        btnSelectAudioCritical = findViewById(R.id.btnSelectAudioCritical);
        btnClearAudioCritical = findViewById(R.id.btnClearAudioCritical);
        batteryLevelText = findViewById(R.id.batteryLevelText);
        startStopButton = findViewById(R.id.startStopButton);

        // Load preferences
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        isServiceRunning = prefs.getBoolean(BatteryService.KEY_RUNNING, false);
        int savedThreshold = prefs.getInt(BatteryService.KEY_THRESHOLD, 20);
        int savedVolume = prefs.getInt(BatteryService.KEY_VOLUME, 100);
        int savedUrgentOffset = prefs.getInt("urgent_offset", 5);
        int savedCriticalOffset = prefs.getInt("critical_offset", 10);
        String savedNormal = prefs.getString("alert_normal_text", getString(R.string.alert_normal));
        String savedUrgent = prefs.getString("alert_urgent_text", getString(R.string.alert_urgent));
        String savedCritical = prefs.getString("alert_critical_text", getString(R.string.alert_critical));
        String savedCustomTtsUrl = prefs.getString("custom_tts_url", "");

        uriNormal = prefs.getString("uri_normal", null);
        uriUrgent = prefs.getString("uri_urgent", null);
        uriCritical = prefs.getString("uri_critical", null);

        uriNormal = prefs.getString("uri_normal", null);
        uriUrgent = prefs.getString("uri_urgent", null);
        uriCritical = prefs.getString("uri_critical", null);

        thresholdSeekBar.setProgress(savedThreshold);
        thresholdText.setText(getString(R.string.alert_threshold, savedThreshold));

        volumeSeekBar.setProgress(savedVolume);
        volumeText.setText(getString(R.string.volume_label, savedVolume));

        urgentOffsetSeekBar.setProgress(savedUrgentOffset);
        urgentOffsetLabel.setText(getString(R.string.urgent_offset_label, savedUrgentOffset));

        criticalOffsetSeekBar.setProgress(savedCriticalOffset);
        criticalOffsetLabel.setText(getString(R.string.critical_offset_label, savedCriticalOffset));

        alertNormalEdit.setText(savedNormal);
        alertUrgentEdit.setText(savedUrgent);
        alertCriticalEdit.setText(savedCritical);

        updateAudioLabels();

        updateAudioLabels();

        updateButtonText();

        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdText.setText(getString(R.string.alert_threshold, progress));
                if (isServiceRunning && fromUser) updateService();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeText.setText(getString(R.string.volume_label, progress));
                if (isServiceRunning && fromUser) updateService();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        urgentOffsetSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                urgentOffsetLabel.setText(getString(R.string.urgent_offset_label, progress));
                if (isServiceRunning && fromUser) updateService();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        criticalOffsetSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                criticalOffsetLabel.setText(getString(R.string.critical_offset_label, progress));
                if (isServiceRunning && fromUser) updateService();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isServiceRunning) { updateService(); }
            }
//            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
//            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        urgentOffsetSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                urgentOffsetLabel.setText(getString(R.string.urgent_offset_label, progress));
                if (isServiceRunning && fromUser) updateService();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        criticalOffsetSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                criticalOffsetLabel.setText(getString(R.string.critical_offset_label, progress));
                if (isServiceRunning && fromUser) updateService();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        alertNormalEdit.addTextChangedListener(textWatcher);
        alertUrgentEdit.addTextChangedListener(textWatcher);
        alertCriticalEdit.addTextChangedListener(textWatcher);

        btnSelectAudioNormal.setOnClickListener(v -> pickAudioNormal.launch(new String[]{"audio/*"}));
        btnSelectAudioUrgent.setOnClickListener(v -> pickAudioUrgent.launch(new String[]{"audio/*"}));
        btnSelectAudioCritical.setOnClickListener(v -> pickAudioCritical.launch(new String[]{"audio/*"}));

        btnClearAudioNormal.setOnClickListener(v -> { uriNormal = null; updateAudioLabels(); saveAudioUris(); if (isServiceRunning) updateService(); });
        btnClearAudioUrgent.setOnClickListener(v -> { uriUrgent = null; updateAudioLabels(); saveAudioUris(); if (isServiceRunning) updateService(); });
        btnClearAudioCritical.setOnClickListener(v -> { uriCritical = null; updateAudioLabels(); saveAudioUris(); if (isServiceRunning) updateService(); });

        btnSelectAudioNormal.setOnClickListener(v -> pickAudioNormal.launch(new String[]{"audio/*"}));
        btnSelectAudioUrgent.setOnClickListener(v -> pickAudioUrgent.launch(new String[]{"audio/*"}));
        btnSelectAudioCritical.setOnClickListener(v -> pickAudioCritical.launch(new String[]{"audio/*"}));

        btnClearAudioNormal.setOnClickListener(v -> { uriNormal = null; updateAudioLabels(); saveAudioUris(); if (isServiceRunning) updateService(); });
        btnClearAudioUrgent.setOnClickListener(v -> { uriUrgent = null; updateAudioLabels(); saveAudioUris(); if (isServiceRunning) updateService(); });
        btnClearAudioCritical.setOnClickListener(v -> { uriCritical = null; updateAudioLabels(); saveAudioUris(); if (isServiceRunning) updateService(); });

        startStopButton.setOnClickListener(v -> toggleService());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void updateAudioLabels() {
        audioNormalPath.setText(uriNormal != null ? getString(R.string.audio_selected, Uri.parse(uriNormal).getLastPathSegment()) : getString(R.string.no_audio_selected));
        audioUrgentPath.setText(uriUrgent != null ? getString(R.string.audio_selected, Uri.parse(uriUrgent).getLastPathSegment()) : getString(R.string.no_audio_selected));
        audioCriticalPath.setText(uriCritical != null ? getString(R.string.audio_selected, Uri.parse(uriCritical).getLastPathSegment()) : getString(R.string.no_audio_selected));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        isServiceRunning = prefs.getBoolean(BatteryService.KEY_RUNNING, false);
        updateButtonText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryInfoReceiver);
        saveSettings();
    }

    private void saveAlertTexts() {
        saveSettings();
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
            .putString("alert_normal_text", alertNormalEdit.getText().toString())
            .putString("alert_urgent_text", alertUrgentEdit.getText().toString())
            .putString("alert_critical_text", alertCriticalEdit.getText().toString())
            .putInt("urgent_offset", urgentOffsetSeekBar.getProgress())
            .putInt("critical_offset", criticalOffsetSeekBar.getProgress())
            .putString("uri_normal", uriNormal)
            .putString("uri_urgent", uriUrgent)
            .putString("uri_critical", uriCritical)
            .apply();
    }

    private void saveAudioUris() {
        SharedPreferences prefs = getSharedPreferences(BatteryService.PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
            .putString("uri_normal", uriNormal)
            .putString("uri_urgent", uriUrgent)
            .putString("uri_critical", uriCritical)
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
        saveSettings();
        Intent serviceIntent = new Intent(this, BatteryService.class);
        serviceIntent.putExtra("threshold", thresholdSeekBar.getProgress());
        serviceIntent.putExtra("volume", volumeSeekBar.getProgress());
        serviceIntent.putExtra("urgent_offset", urgentOffsetSeekBar.getProgress());
        serviceIntent.putExtra("critical_offset", criticalOffsetSeekBar.getProgress());
        serviceIntent.putExtra("alert_normal", alertNormalEdit.getText().toString());
        serviceIntent.putExtra("alert_urgent", alertUrgentEdit.getText().toString());
        serviceIntent.putExtra("alert_critical", alertCriticalEdit.getText().toString());
        serviceIntent.putExtra("uri_normal", uriNormal);
        serviceIntent.putExtra("uri_urgent", uriUrgent);
        serviceIntent.putExtra("uri_critical", uriCritical);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void toggleService() {
        saveSettings();
        Intent serviceIntent = new Intent(this, BatteryService.class);
        serviceIntent.putExtra("threshold", thresholdSeekBar.getProgress());
        serviceIntent.putExtra("volume", volumeSeekBar.getProgress());
        serviceIntent.putExtra("urgent_offset", urgentOffsetSeekBar.getProgress());
        serviceIntent.putExtra("critical_offset", criticalOffsetSeekBar.getProgress());
        serviceIntent.putExtra("alert_normal", alertNormalEdit.getText().toString());
        serviceIntent.putExtra("alert_urgent", alertUrgentEdit.getText().toString());
        serviceIntent.putExtra("alert_critical", alertCriticalEdit.getText().toString());
        serviceIntent.putExtra("uri_normal", uriNormal);
        serviceIntent.putExtra("uri_urgent", uriUrgent);
        serviceIntent.putExtra("uri_critical", uriCritical);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
