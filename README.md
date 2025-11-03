# ScreenshotApp

ScreenshotApp is an Android utility that provides a floating overlay for capturing rectangular or circular screenshots. After selecting a region, the cropped image is saved to local storage and placed on the clipboard for quick sharing.

## Features
- Floating overlay button that can be dragged anywhere on screen
- Rectangle or circle selection modes with real-time visual feedback
- Cropped screenshot automatically saved and copied to the clipboard
- Persistent logging written to `Android/data/<package>/files/logs/screenshot_app.log`
- Foreground service architecture to keep the overlay active

## Project Structure
- `app/` - Android app module
  - `ui/` - Activities and overlay views
  - `capture/` - Bitmap processing and storage helpers
  - `logging/` - File-backed logging utilities
  - `util/` - Permission and clipboard helpers
- `app/src/test/` - JVM unit tests for bitmap cropping logic

## Prerequisites
1. Install **Android Studio (Giraffe or newer)** from [developer.android.com/studio](https://developer.android.com/studio).
2. Install the **Android SDK Platform 34** and **Android SDK Build-Tools 34.x** using the SDK Manager within Android Studio.
3. Ensure Java 17 is available. Android Studio bundles a compatible JDK; no additional setup needed if you use the bundled version.
4. Enable Developer Options on the Galaxy S24:
   - Open **Settings → About phone → Software information**.
   - Tap **Build number** seven times (enter PIN if prompted).
5. Enable USB debugging:
   - Go to **Settings → Developer options**.
   - Toggle on **USB debugging**.
6. Install the latest USB drivers if using Windows. Samsung provides drivers at [developer.samsung.com](https://developer.samsung.com/mobile/android-usb-driver.html).

## Running on the Galaxy S24
1. Connect the phone via USB and authorize the computer when prompted on the device.
2. Verify the device connection from a terminal:
   ```bash
   adb devices
   ```
   The Galaxy S24 should appear as an authorized device.
3. Build and install the debug APK:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Launch the **ScreenshotApp** from the app drawer.
5. Grant the **Display over other apps** permission when prompted, then tap **Start Screenshot Overlay**.
6. Accept the screen capture permission dialog.
7. Use the floating button to choose **Rectangle** or **Circle**, drag to highlight the desired region, and release to capture.

## Logs
The service writes detailed logs to:
```
Android/data/com.example.screenshotapp/files/logs/screenshot_app.log
```
You can retrieve the log file using:
```bash
adb pull /sdcard/Android/data/com.example.screenshotapp/files/logs/screenshot_app.log
```

## Testing
Run unit tests before committing changes:
```bash
./gradlew test
```

## Notes for Developers
- All core actions are logged through `AppLogger` for easier debugging.
- Shape selection behavior is encapsulated in `SelectionOverlayView`; adjust visuals there if you need to tweak UX.
- Clipboard integration uses a FileProvider-backed URI. Update `file_paths.xml` if you change storage locations.
- Remember to keep `.gitignore` in sync when new build artifacts appear.
