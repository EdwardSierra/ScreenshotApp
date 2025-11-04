# Developer Notes

- `ScreenshotCropView` scales the captured bitmap using a center-fit matrix. Update both the scaling math and the `mapToBitmapRect` conversion together if you tweak the rendering approach.
- Clipboard helper grants read/write permissions to `com.android.systemui` so the Android clipboard service can access the stored image.
- Logging is file-based; rotate or truncate the log in `AppLogger` if the file grows beyond acceptable limits.
- The simplified Gradle wrapper scripts assume a standard Java setup. If you encounter launch issues, regenerate the wrapper with `gradle wrapper` using a full Gradle installation.
- `ProjectionPermissionRepository` keeps the latest MediaProjection tokens in memory. Clear it when a capture fails with a security exception so future runs re-request permission.
