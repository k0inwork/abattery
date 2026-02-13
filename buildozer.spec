[app]
title = Battery Monitor
package.name = batterymonitor
package.domain = org.example
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,wav
version = 0.1
requirements = python3,kivy,plyer
orientation = portrait
fullscreen = 0
android.permissions = BATTERY_STATS, WAKE_LOCK
android.api = 31
android.minapi = 21
android.ndk = 25b
android.archs = arm64-v8a, armeabi-v7a
android.allow_backup = True

[buildozer]
log_level = 2
warn_on_root = 1
