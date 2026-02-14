package com.example.batteryalert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import androidx.core.app.NotificationCompat;
import java.util.Locale;

public class BatteryService extends Service implements TextToSpeech.OnInitListener {

    private static final String CHANNEL_ID = "BatteryMonitorChannel";
    public static final String PREFS_NAME = "BatteryPrefs";
    public static final String KEY_RUNNING = "isServiceRunning";
    public static final String KEY_THRESHOLD = "threshold";
    public static final String KEY_VOLUME = "volume";
    public static final String KEY_URGENT_OFFSET = "urgent_offset";
    public static final String KEY_CRITICAL_OFFSET = "critical_offset";
    public static final String KEY_ALERT_NORMAL = "alert_normal_text";
    public static final String KEY_ALERT_URGENT = "alert_urgent_text";
    public static final String KEY_ALERT_CRITICAL = "alert_critical_text";

    private int threshold = 20;
    private int urgentOffset = 5;
    private int criticalOffset = 10;
    private float volume = 1.0f;
    private String alertNormal;
    private String alertUrgent;
    private String alertCritical;
    private TextToSpeech tts;
    private boolean ttsInitialized = false;
    private long lastAlertTime = 0;
    private static final long ALERT_COOLDOWN = 60000; // 1 minute cooldown

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL;

            if (isCharging) {
                stopAlertSound();
                return;
            }

            if (level != -1 && scale != -1) {
                float batteryPct = level * 100 / (float) scale;
                if (batteryPct <= threshold) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastAlertTime > ALERT_COOLDOWN) {
                        playAlertSound(batteryPct);
                        lastAlertTime = currentTime;
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, this);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        createNotificationChannel();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        threshold = prefs.getInt(KEY_THRESHOLD, 20);
        urgentOffset = prefs.getInt(KEY_URGENT_OFFSET, 5);
        criticalOffset = prefs.getInt(KEY_CRITICAL_OFFSET, 10);
        volume = prefs.getInt(KEY_VOLUME, 100) / 100f;
        alertNormal = prefs.getString(KEY_ALERT_NORMAL, getString(R.string.alert_normal));
        alertUrgent = prefs.getString(KEY_ALERT_URGENT, getString(R.string.alert_urgent));
        alertCritical = prefs.getString(KEY_ALERT_CRITICAL, getString(R.string.alert_critical));

        prefs.edit().putBoolean(KEY_RUNNING, true).apply();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("ru"));
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsInitialized = true;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (intent != null) {
            if (intent.hasExtra("threshold")) {
                threshold = intent.getIntExtra("threshold", 20);
                prefs.edit().putInt(KEY_THRESHOLD, threshold).apply();
            }
            if (intent.hasExtra("volume")) {
                int volInt = intent.getIntExtra("volume", 100);
                volume = volInt / 100f;
                prefs.edit().putInt(KEY_VOLUME, volInt).apply();
            }
            if (intent.hasExtra("urgent_offset")) {
                urgentOffset = intent.getIntExtra("urgent_offset", 5);
                prefs.edit().putInt(KEY_URGENT_OFFSET, urgentOffset).apply();
            }
            if (intent.hasExtra("critical_offset")) {
                criticalOffset = intent.getIntExtra("critical_offset", 10);
                prefs.edit().putInt(KEY_CRITICAL_OFFSET, criticalOffset).apply();
            }
            if (intent.hasExtra("alert_normal")) {
                alertNormal = intent.getStringExtra("alert_normal");
                prefs.edit().putString(KEY_ALERT_NORMAL, alertNormal).apply();
            }
            if (intent.hasExtra("alert_urgent")) {
                alertUrgent = intent.getStringExtra("alert_urgent");
                prefs.edit().putString(KEY_ALERT_URGENT, alertUrgent).apply();
            }
            if (intent.hasExtra("alert_critical")) {
                alertCritical = intent.getStringExtra("alert_critical");
                prefs.edit().putString(KEY_ALERT_CRITICAL, alertCritical).apply();
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.service_active))
                .setContentText(getString(R.string.service_monitoring, threshold))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_RUNNING, false).apply();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playAlertSound(float batteryPct) {
        if (tts != null && ttsInitialized) {
            String textToSpeak;
            if (batteryPct <= threshold - criticalOffset) {
                textToSpeak = alertCritical;
            } else if (batteryPct <= threshold - urgentOffset) {
                textToSpeak = alertUrgent;
            } else {
                textToSpeak = alertNormal;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle params = new Bundle();
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "battery_alert");
            } else {
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private void stopAlertSound() {
        if (tts != null) {
            tts.stop();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Monitor Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
