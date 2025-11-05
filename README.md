# ScreenshotApp

ScreenshotApp adds a Quick Settings tile that grabs the current screen, lets you outline a rectangle, and copies the cropped result directly to the clipboard. The full workflow runs without any floating overlays so it stays out of the way until you need it.

## Features
- Quick Settings tile for one-tap captures.
- Full-screen grab followed by an on-device cropping surface.
- Cropped image automatically saved and placed on the clipboard.
- Screen capture permission cached so repeated captures do not re-prompt.
- Persistent logging written to `Android/data/<package>/files/logs/screenshot_app.log`.

## Setup
1. Install the app on your device.
2. Launch **ScreenshotApp** from the launcher.
3. Tap **Grant Screen Capture Access** to approve the MediaProjection permission. A confirmation toast appears when the permission is stored.
4. Tap **Open Quick Settings Editor** (or manually open the editor) and add the **Screenshot** tile. Position it wherever it is convenient.

## Taking a Screenshot
1. Pull down the Quick Settings shade and tap the **Screenshot** tile.
2. The app captures the current screen and opens the cropping view.
3. Drag to select the rectangle you want to keep. When you release, the cropped image is saved and copied to the clipboard automatically. Use the **Cancel** button if you want to discard the capture.

## Logs
The capture service and crop activity log detailed events to:
```
Android/data/com.example.screenshotapp/files/logs/screenshot_app.log
```
You can retrieve the log file with:
```bash
adb pull /sdcard/Android/data/com.example.screenshotapp/files/logs/screenshot_app.log
```

## Testing
Run unit tests before committing changes:
```bash
./gradlew test
```
The suite runs on Robolectric so Android framework classes (e.g., `Bitmap`, `Intent`) behave like they do on device.

## Notes for Developers
- `ProjectionPermissionRepository` caches the most recent MediaProjection approval. Call `store` after any successful permission flow and `clear` when tokens become invalid.
- `ProjectionRequestActivity` requests a full-display capture on Android 14+ using `MediaProjectionConfig.createConfigForDefaultDisplay()` so users skip the partial-share picker.
- `ScreenshotCaptureService` performs the foreground capture and launches `CropScreenshotActivity` with a cached PNG. The crop view scales the bitmap using a center-fit matrix; keep scaling logic in sync if you change rendering behaviour.
- Clipboard integration still uses a FileProvider-backed URI. Update `file_paths.xml` if the storage location changes.
- `ScreenshotTileService` must stay exported so System UI can bind; the `android.permission.BIND_QUICK_SETTINGS_TILE` permission continues guarding access.
- Remember to keep `.gitignore` in sync when new build artifacts appear.
