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
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import androidx.core.app.NotificationCompat;
import java.net.URLEncoder;
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
    private String alertNormal, alertUrgent, alertCritical;
    private String uriNormal, uriUrgent, uriCritical;
    private String customTtsUrl;

    private TextToSpeech tts;
    private boolean ttsInitialized = false;
    private MediaPlayer mediaPlayer;

    private long lastAlertTime = 0;
    private static final long ALERT_COOLDOWN = 60000;

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
        loadSettings(prefs);

        prefs.edit().putBoolean(KEY_RUNNING, true).apply();
    }

    private void loadSettings(SharedPreferences prefs) {
        threshold = prefs.getInt(KEY_THRESHOLD, 20);
        urgentOffset = prefs.getInt(KEY_URGENT_OFFSET, 5);
        criticalOffset = prefs.getInt(KEY_CRITICAL_OFFSET, 10);
        volume = prefs.getInt(KEY_VOLUME, 100) / 100f;
        alertNormal = prefs.getString(KEY_ALERT_NORMAL, getString(R.string.alert_normal));
        alertUrgent = prefs.getString(KEY_ALERT_URGENT, getString(R.string.alert_urgent));
        alertCritical = prefs.getString(KEY_ALERT_CRITICAL, getString(R.string.alert_critical));
        customTtsUrl = prefs.getString("custom_tts_url", "");
        uriNormal = prefs.getString("uri_normal", null);
        uriUrgent = prefs.getString("uri_urgent", null);
        uriCritical = prefs.getString("uri_critical", null);
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
        if (intent != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (intent.hasExtra("threshold")) {
                threshold = intent.getIntExtra("threshold", 20);
                editor.putInt(KEY_THRESHOLD, threshold);
            }
            if (intent.hasExtra("volume")) {
                int volInt = intent.getIntExtra("volume", 100);
                volume = volInt / 100f;
                editor.putInt(KEY_VOLUME, volInt);
            }
            if (intent.hasExtra("urgent_offset")) {
                urgentOffset = intent.getIntExtra("urgent_offset", 5);
                editor.putInt(KEY_URGENT_OFFSET, urgentOffset);
            }
            if (intent.hasExtra("critical_offset")) {
                criticalOffset = intent.getIntExtra("critical_offset", 10);
                editor.putInt(KEY_CRITICAL_OFFSET, criticalOffset);
            }
            if (intent.hasExtra("alert_normal")) {
                alertNormal = intent.getStringExtra("alert_normal");
                editor.putString(KEY_ALERT_NORMAL, alertNormal);
            }
            if (intent.hasExtra("alert_urgent")) {
                alertUrgent = intent.getStringExtra("alert_urgent");
                editor.putString(KEY_ALERT_URGENT, alertUrgent);
            }
            if (intent.hasExtra("alert_critical")) {
                alertCritical = intent.getStringExtra("alert_critical");
                editor.putString(KEY_ALERT_CRITICAL, alertCritical);
            }
            if (intent.hasExtra("custom_tts_url")) {
                customTtsUrl = intent.getStringExtra("custom_tts_url");
                editor.putString("custom_tts_url", customTtsUrl);
            }
            if (intent.hasExtra("uri_normal")) {
                uriNormal = intent.getStringExtra("uri_normal");
                editor.putString("uri_normal", uriNormal);
            }
            if (intent.hasExtra("uri_urgent")) {
                uriUrgent = intent.getStringExtra("uri_urgent");
                editor.putString("uri_urgent", uriUrgent);
            }
            if (intent.hasExtra("uri_critical")) {
                uriCritical = intent.getStringExtra("uri_critical");
                editor.putString("uri_critical", uriCritical);
            }
            editor.apply();
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_RUNNING, false).apply();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playAlertSound(float batteryPct) {
        String alertUriString = null;
        String textToSpeak = null;

        if (batteryPct <= threshold - criticalOffset) {
            alertUriString = uriCritical;
            textToSpeak = alertCritical;
        } else if (batteryPct <= threshold - urgentOffset) {
            alertUriString = uriUrgent;
            textToSpeak = alertUrgent;
        } else {
            alertUriString = uriNormal;
            textToSpeak = alertNormal;
        }

        if (alertUriString != null) {
            playAudioUri(Uri.parse(alertUriString));
        } else if (textToSpeak != null) {
            if (customTtsUrl != null && !customTtsUrl.trim().isEmpty() && customTtsUrl.contains("%s")) {
                try {
                    String encodedText = URLEncoder.encode(textToSpeak, "UTF-8");
                    String finalUrl = customTtsUrl.replace("%s", encodedText);
                    playAudioUri(Uri.parse(finalUrl));
                } catch (Exception e) {
                    e.printStackTrace();
                    fallbackToTts(textToSpeak);
                }
            } else {
                fallbackToTts(textToSpeak);
            }
        }
    }

    private void fallbackToTts(String text) {
        if (tts != null && ttsInitialized) {
            speakTts(text);
        }
    }

    private void playAudioUri(Uri uri) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setVolume(volume, volume);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                // If network audio fails, we could fallback to TTS here too
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void speakTts(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "battery_alert");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void stopAlertSound() {
        if (tts != null) {
            tts.stop();
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
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
