#!/bin/bash
# ScreenshotApp - Android screenshot utility
# Copyright (C) 2025 Edward Sierra
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.

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
