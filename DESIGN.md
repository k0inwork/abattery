# Battery Monitor Native Android Design

## Goal
A lightweight Android application to monitor battery level and play an alert sound when it falls below a threshold.

## Architecture
- **Language**: Java (Native Android)
- **Minimum Memory Footprint**: Uses only standard Android APIs, no external libraries.
- **Background Persistence**: Uses a `Foreground Service` to ensure the system doesn't kill the monitoring process.
- **Alert Logic**: Registers a `BroadcastReceiver` for `Intent.ACTION_BATTERY_CHANGED`.
- **Sound Generation**: Uses `android.media.ToneGenerator` to produce a beep without needing audio assets.

## Components
1. **MainActivity**:
   - Provides a simple UI to set the threshold.
   - Starts and stops the `BatteryService`.
   - Requests `POST_NOTIFICATIONS` permission on Android 13+.
2. **BatteryService**:
   - Runs in the foreground with a persistent notification.
   - Monitors battery percentage and charging state.
   - Triggers the alert beep.
3. **GitHub Actions**:
   - Automates the build process to provide an APK without requiring a local Android SDK setup.

## Permissions
- `android.permission.FOREGROUND_SERVICE`: To run the monitor in background.
- `android.permission.POST_NOTIFICATIONS`: Required for the foreground service notification on Android 13+.
