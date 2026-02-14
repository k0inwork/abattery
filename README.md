# Battery Monitor (Native Android)

A lightweight, high-efficiency Android application to monitor battery levels and alert the user with a sound when the percentage drops below a specific threshold.

## Features
- **Minimum Memory Footprint**: Built with Native Java and standard Android APIs (no heavy frameworks like Python or Flutter).
- **Persistent Monitoring**: Uses a Foreground Service to ensure monitoring continues even when the app is in the background.
- **Smart Alerts**: Automatically suppresses alerts when the device is charging.
- **Self-Contained**: Generates alert tones programmatically (no audio assets required).

## Architecture
- **Language**: Java
- **Target SDK**: 33 (Android 13)
- **Monitoring**: `BroadcastReceiver` for `ACTION_BATTERY_CHANGED`.
- **Alerts**: `android.media.ToneGenerator`.

## How to Get the APK

To avoid the hassle of setting up a local Android build environment (SDK, NDK, Gradle), this project uses **GitHub Actions** to build the APK in the cloud.

### 1. Upload to GitHub
Push this project to a GitHub repository.

### 2. Automatic Build
Every push to the `main` branch triggers a build. Go to the **Actions** tab in your GitHub repository to see the progress.

### 3. Download APK
Once the build is complete:
1. Click on the successful workflow run.
2. Scroll down to the **Artifacts** section.
3. Download the `battery-monitor-apk` zip file.
4. Extract and install the `app-debug.apk` on your Android device.

## Local Development
If you prefer to build locally:
1. Install [Android Studio](https://developer.android.com/studio).
2. Open this project folder.
3. Click "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)".

## Why Native Java?
- **APK Size**: ~20KB vs 20MB+ (Python/Kivy).
- **RAM Usage**: ~5MB vs 100MB+ (Python/Kivy).
- **Stability**: Uses official Android APIs directly, reducing the risk of background process termination.

yes
no
