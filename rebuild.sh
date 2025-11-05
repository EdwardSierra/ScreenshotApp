#!/bin/bash
set -e  # Exit if any command fails

# Generate timestamped log file name
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOGFILE="screenshot_log_${TIMESTAMP}.txt"

echo "🧹 Removing old app log..."
adb shell rm "/sdcard/Android/data/com.example.screenshotapp/files/logs/screenshot_app.log" 2>/dev/null || true

echo "🔨 Building debug APK..."
./gradlew assembleDebug

echo "📦 Installing app..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "📜 Starting logcat (filter: Screenshot)... Output → ${LOGFILE}"
adb logcat -c
adb logcat -e "Screenshot" > "${LOGFILE}"
