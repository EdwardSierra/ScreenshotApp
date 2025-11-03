# Developer Notes

- Overlay coordinates are calculated directly from display dimensions, so any future density-scaling logic should adjust `SelectionOverlayView` and `ScreenshotProcessor` in tandem.
- Clipboard helper grants read/write permissions to `com.android.systemui` so the Android clipboard service can access the stored image.
- Logging is file-based; rotate or truncate the log in `AppLogger` if the file grows beyond acceptable limits.
- The simplified Gradle wrapper scripts assume a standard Java setup. If you encounter launch issues, regenerate the wrapper with `gradle wrapper` using a full Gradle installation.
