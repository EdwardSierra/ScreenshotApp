package com.example.screenshotapp.ui.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.screenshotapp.R
import com.example.screenshotapp.capture.ScreenshotProcessor
import com.example.screenshotapp.capture.ScreenshotStorage
import com.example.screenshotapp.logging.AppLogger
import com.example.screenshotapp.ui.MainActivity
import com.example.screenshotapp.ui.overlay.SelectionShape
import com.example.screenshotapp.util.ClipboardHelper
import com.example.screenshotapp.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hosts the floating overlay button and handles screen capture workflows.
 *
 * Inputs: Media projection permission data delivered via the start intent.
 * Outputs: Cropped screenshots saved and placed on the clipboard.
 */
class ScreenshotOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButtonController: FloatingButtonController
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var screenshotStorage: ScreenshotStorage
    private val screenshotProcessor = ScreenshotProcessor()

    private var mediaProjection: MediaProjection? = null
    private var selectionOverlayView: SelectionOverlayView? = null
    private var controlsView: android.view.View? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var currentMode: SelectionMode = SelectionMode.RECTANGLE

    /**
     * Provides the binding interface; unused because the service is started.
     *
     * Inputs: [intent] - Binding intent.
     * Outputs: Always null since binding is not supported.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Sets up core service dependencies such as window and projection managers.
     *
     * Inputs: None.
     * Outputs: Ready-to-use controllers for overlay display.
     */
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        floatingButtonController = FloatingButtonController(this, windowManager) {
            enterSelectionMode()
        }
        screenshotStorage = ScreenshotStorage(this)
        createNotificationChannel()
        AppLogger.logInfo("ScreenshotOverlayService", "Service created.")
    }

    /**
     * Handles the start command by initializing media projection and showing the overlay.
     *
     * Inputs: [intent] - Contains projection result data, [flags], [startId].
     * Outputs: Foreground service start state.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (data == null || resultCode == 0) {
            Toast.makeText(this, getString(R.string.screen_capture_denied), Toast.LENGTH_SHORT).show()
            AppLogger.logError("ScreenshotOverlayService", "Missing media projection data.")
            stopSelf()
            return START_NOT_STICKY
        }
        if (mediaProjection == null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            mediaProjection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
        }
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (PermissionHelper.hasOverlayPermission(this)) {
            floatingButtonController.show()
        } else {
            AppLogger.logError("ScreenshotOverlayService", "Overlay permission missing after service start.")
            Toast.makeText(this, getString(R.string.request_overlay_permission), Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }
        AppLogger.logInfo("ScreenshotOverlayService", "Overlay service started.")
        return START_STICKY
    }

    /**
     * Cleans up overlays, virtual displays, and projection callbacks.
     *
     * Inputs: None.
     * Outputs: Released resources.
     */
    override fun onDestroy() {
        floatingButtonController.dismiss()
        removeSelectionOverlay()
        virtualDisplay?.release()
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        serviceScope.cancel()
        AppLogger.logInfo("ScreenshotOverlayService", "Service destroyed.")
        super.onDestroy()
    }

    /**
     * Switches the UI into selection mode by creating overlay views and controls.
     *
     * Inputs: None.
     * Outputs: Fullscreen overlay ready for user drawing.
     */
    private fun enterSelectionMode() {
        floatingButtonController.dismiss()
        addSelectionOverlay()
        addControls()
        Toast.makeText(this, getString(R.string.capturing_screen), Toast.LENGTH_SHORT).show()
        AppLogger.logInfo("ScreenshotOverlayService", "Selection mode entered.")
    }

    /**
     * Adds the fullscreen selection view to the window manager.
     *
     * Inputs: None.
     * Outputs: Selection overlay attached to the window.
     */
    private fun addSelectionOverlay() {
        if (selectionOverlayView != null) {
            return
        }
        val overlay = SelectionOverlayView(this).apply {
            selectionMode = currentMode
            onSelectionFinished = { selection ->
                AppLogger.logInfo("ScreenshotOverlayService", "Selection received.")
                serviceScope.launch {
                    removeSelectionOverlay()
                    delay(OVERLAY_DISMISS_DELAY_MS)
                    captureSelection(selection)
                }
            }
            onSelectionCancelled = {
                AppLogger.logInfo("ScreenshotOverlayService", "Selection cancelled by user.")
                removeSelectionOverlay()
                floatingButtonController.show()
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlay, params)
        selectionOverlayView = overlay
    }

    /**
     * Inflates and displays the shape selection controls alongside the overlay.
     *
     * Inputs: None.
     * Outputs: Control view added to the window.
     */
    private fun addControls() {
        if (controlsView != null) {
            return
        }
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_controls, null, false)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            y = 150
        }
        val rectangleToggle: android.widget.ToggleButton = view.findViewById(R.id.rectangleToggle)
        val circleToggle: android.widget.ToggleButton = view.findViewById(R.id.circleToggle)
        val cancelButton: android.widget.Button = view.findViewById(R.id.cancelButton)

        rectangleToggle.isChecked = currentMode == SelectionMode.RECTANGLE
        circleToggle.isChecked = currentMode == SelectionMode.CIRCLE

        rectangleToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                circleToggle.isChecked = false
                currentMode = SelectionMode.RECTANGLE
                selectionOverlayView?.selectionMode = currentMode
            } else if (!circleToggle.isChecked) {
                rectangleToggle.isChecked = true
            }
        }

        circleToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rectangleToggle.isChecked = false
                currentMode = SelectionMode.CIRCLE
                selectionOverlayView?.selectionMode = currentMode
            } else if (!rectangleToggle.isChecked) {
                circleToggle.isChecked = true
            }
        }

        cancelButton.setOnClickListener {
            removeSelectionOverlay()
            floatingButtonController.show()
        }

        windowManager.addView(view, params)
        controlsView = view
    }

    /**
     * Removes both the selection overlay and companion controls from the window manager.
     *
     * Inputs: None.
     * Outputs: Overlay views detached.
     */
    private fun removeSelectionOverlay() {
        selectionOverlayView?.let {
            windowManager.removeView(it)
            AppLogger.logInfo("ScreenshotOverlayService", "Selection overlay removed.")
        }
        controlsView?.let {
            windowManager.removeView(it)
        }
        selectionOverlayView = null
        controlsView = null
    }

    /**
     * Captures the selected area by requesting a screenshot from the media projection.
     *
     * Inputs: [selection] - User defined selection shape.
     * Outputs: Cropped screenshot saved and copied to clipboard.
     */
    private suspend fun captureSelection(selection: SelectionShape) {
        val projection = mediaProjection ?: run {
            AppLogger.logError("ScreenshotOverlayService", "Media projection unavailable.")
            floatingButtonController.show()
            return
        }
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        withContext(Dispatchers.IO) {
            var imageReader: ImageReader? = null
            var capturedImage: Image? = null
            try {
                imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
                virtualDisplay = projection.createVirtualDisplay(
                    "screenshot-capture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    null
                )
                delay(CAPTURE_START_DELAY_MS)
                repeat(MAX_CAPTURE_ATTEMPTS) { attempt ->
                    capturedImage = imageReader.acquireLatestImage()
                    if (capturedImage != null) {
                        return@repeat
                    }
                    delay(CAPTURE_RETRY_DELAY_MS)
                }
                val image = capturedImage ?: throw IllegalStateException("Failed to capture screen image.")
                val bitmap = image.toBitmap(width, height)
                image.close()
                val cropped = screenshotProcessor.crop(bitmap, selection)
                val uri = screenshotStorage.saveBitmap(cropped)
                ClipboardHelper.copyImageToClipboard(this@ScreenshotOverlayService, uri)
                AppLogger.logInfo("ScreenshotOverlayService", "Screenshot captured successfully.")
                notifySuccess()
                bitmap.recycle()
                cropped.recycle()
            } catch (exception: Exception) {
                AppLogger.logError("ScreenshotOverlayService", "Capture failed.", exception)
                notifyFailure()
            } finally {
                capturedImage?.close()
                imageReader?.close()
                virtualDisplay?.release()
                virtualDisplay = null
                withContext(Dispatchers.Main) {
                    floatingButtonController.show()
                }
            }
        }
    }

    /**
     * Creates a bitmap from the provided image while handling row padding.
     *
     * Inputs: [width], [height] - Display dimensions.
     * Outputs: Bitmap representing the entire screen content.
     */
    private fun Image.toBitmap(width: Int, height: Int): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }

    /**
     * Builds the persistent foreground notification keeping the service alive.
     *
     * Inputs: None.
     * Outputs: Configured [Notification] instance.
     */
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_service_channel_name))
            .setContentText(getString(R.string.overlay_service_channel_description))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_overlay_button)
            .setOngoing(true)
            .build()
    }

    /**
     * Creates the notification channel required for foreground services on modern Android versions.
     *
     * Inputs: None.
     * Outputs: Notification channel registered with the system manager.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.overlay_service_channel_description)
            }
            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a success toast on the main thread after a capture completes.
     *
     * Inputs: None.
     * Outputs: User-visible confirmation message.
     */
    private fun notifySuccess() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this@ScreenshotOverlayService, getString(R.string.capture_success), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Displays an error toast on the main thread if capture fails.
     *
     * Inputs: None.
     * Outputs: User-visible error message.
     */
    private fun notifyFailure() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this@ScreenshotOverlayService, getString(R.string.capture_failure), Toast.LENGTH_SHORT).show()
        }
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        /**
         * Handles projection stop events by shutting down the overlay service.
         *
         * Inputs: None.
         * Outputs: Stops the service upon projection loss.
         */
        override fun onStop() {
            AppLogger.logInfo("ScreenshotOverlayService", "Media projection stopped.")
            stopSelf()
        }
    }

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val CHANNEL_ID = "screenshot_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CAPTURE_START_DELAY_MS = 150L
        private const val CAPTURE_RETRY_DELAY_MS = 50L
        private const val MAX_CAPTURE_ATTEMPTS = 6
        private const val OVERLAY_DISMISS_DELAY_MS = 120L
    }
}
