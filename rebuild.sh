#!/bin/bash
set -e  # Stop on first error

# Step 1: Delete old log
echo "🧹 Removing old log file..."
adb shell rm "/sdcard/Android/data/com.example.screenshotapp/files/logs/screenshot_app.log" 2>/dev/null || true

# Step 2: Build APK
echo "🔨 Building debug APK..."
./gradlew assembleDebug

# Step 3: Install on connected device
echo "📦 Installing app..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Step 4: Clear and start filtered logcat
echo "📜 Starting logcat (filter: Screenshot)..."
adb logcat -c
adb logcat -e "Screenshot"