# Battery Monitor Android App

A simple Android application built with Python using the Kivy framework and Plyer library. It monitors the battery level and plays an alert sound when the percentage falls below a user-defined threshold.

## Features
- Real-time battery level monitoring.
- Adjustable threshold slider (0-100%).
- Audible alert when battery is low.
- **Smart Alerts**: Does not alert if the device is currently charging.
- Simple, clean user interface.

## Architecture
- **Kivy**: Used for the cross-platform User Interface.
- **Plyer**: Provides a platform-independent API to access Android's battery status.
- **Buildozer**: Tool used to package the Python code into an Android APK.

## Prerequisites
To run locally:
- Python 3
- Kivy: `pip install kivy`
- Plyer: `pip install plyer`

To build for Android:
- Buildozer (and its dependencies)

## Files
- `main.py`: The main application code.
- `generate_sound.py`: Utility to generate the `alert.wav` sound file.
- `buildozer.spec`: Configuration for the Android build process.
- `.gitignore`: Ensures build artifacts and temporary files are not tracked.

## Usage
1. Generate the alert sound: `python generate_sound.py`
2. Run the app: `python main.py`
3. Set your desired threshold using the slider.
4. Click "Start Monitoring".

## Known Limitations
- **Background Execution**: On modern Android versions, the app may be paused or stopped by the system when in the background. For reliable long-term monitoring, an Android Service should be implemented (see Kivy/Buildozer documentation on Services).
