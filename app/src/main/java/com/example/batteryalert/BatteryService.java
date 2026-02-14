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
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class BatteryService extends Service {

    private static final String CHANNEL_ID = "BatteryMonitorChannel";
    public static final String PREFS_NAME = "BatteryPrefs";
    public static final String KEY_RUNNING = "isServiceRunning";

    private int threshold = 20;
    private ToneGenerator toneGenerator;
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

            if (level != -1 && scale != -1) {
                float batteryPct = level * 100 / (float) scale;
                if (batteryPct < threshold && !isCharging) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastAlertTime > ALERT_COOLDOWN) {
                        playAlertSound();
                        lastAlertTime = currentTime;
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        createNotificationChannel();

        // Mark as running
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_RUNNING, true).apply();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            threshold = intent.getIntExtra("threshold", 20);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Monitor Active")
                .setContentText("Monitoring battery below " + threshold + "%")
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
        if (toneGenerator != null) {
            toneGenerator.release();
        }

        // Mark as stopped
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_RUNNING, false).apply();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playAlertSound() {
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
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
