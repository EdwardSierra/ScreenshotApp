/*
 * ScreenshotApp - Android screenshot utility
 * Copyright (C) 2025 Edward Sierra
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.example.screenshotapp.ui.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.content.pm.ServiceInfo
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.screenshotapp.R
import com.example.screenshotapp.capture.ProjectionPermissionRepository
import com.example.screenshotapp.capture.ScreenCaptureManager
import com.example.screenshotapp.capture.ScreenshotCache
import com.example.screenshotapp.logging.AppLogger
import com.example.screenshotapp.ui.ProjectionRequestActivity
import com.example.screenshotapp.ui.crop.CropScreenshotActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs as a foreground service to capture a screenshot before launching the crop UI.
 *
 * Inputs: Tile-triggered start intents.
 * Outputs: Cached screenshot file passed to [CropScreenshotActivity].
 */
class ScreenshotCaptureService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private lateinit var captureManager: ScreenCaptureManager
    private lateinit var screenshotCache: ScreenshotCache
    private var showCapturingToast = false

    /**
     * No binding support; the service is strictly started.
     *
     * Inputs: None.
     * Outputs: Always null.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Prepares capture dependencies and registers the notification channel.
     *
     * Inputs: None.
     * Outputs: Components initialized for future capture runs.
     */
    override fun onCreate() {
        super.onCreate()
        captureManager = ScreenCaptureManager(this)
        screenshotCache = ScreenshotCache(this)
        createNotificationChannel()
        AppLogger.logInfo("ScreenshotCaptureService", "Service created.")
    }

    /**
     * Starts the foreground capture flow and handles the lifecycle of the coroutine job.
     *
     * Inputs: [intent] - Unused, [flags], [startId] - Standard service parameters.
     * Outputs: Foreground execution state.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showCapturingToast = intent?.getBooleanExtra(EXTRA_SHOW_CAPTURING_TOAST, false) ?: false
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        serviceScope.launch {
            try {
                performCapture()
            } finally {
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Cancels running coroutines when the service is destroyed.
     *
     * Inputs: None.
     * Outputs: Cleans up coroutine scope resources.
     */
    override fun onDestroy() {
        serviceScope.cancel()
        AppLogger.logInfo("ScreenshotCaptureService", "Service destroyed.")
        super.onDestroy()
    }

    /**
     * Executes the capture workflow, handling success and error states.
     *
     * Inputs: None.
     * Outputs: Crop activity launched or permission request triggered.
     */
    private suspend fun performCapture() {
        notifyCapturing()
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        if (projectionManager == null) {
            AppLogger.logError("ScreenshotCaptureService", "MediaProjectionManager unavailable.")
            notifyFailure()
            return
        }
        val projection = ProjectionPermissionRepository.getProjection(projectionManager)
        if (projection == null) {
            AppLogger.logError("ScreenshotCaptureService", "Projection token missing; requesting permission.")
            notifyPermissionRequired()
            requestProjectionPermission()
            return
        }
        if (ProjectionPermissionRepository.consumeJustGrantedFlag()) {
            AppLogger.logInfo(
                "ScreenshotCaptureService",
                "Delaying capture to allow system selection UI to dismiss."
            )
            delay(PERMISSION_DISMISS_DELAY_MS)
        }
        try {
            val bitmap = withContext(Dispatchers.IO) {
                captureManager.capture(projection)
            }
            val cachePath = withContext(Dispatchers.IO) {
                screenshotCache.write(bitmap)
            }
            withContext(Dispatchers.Default) {
                bitmap.recycle()
            }
            AppLogger.logInfo("ScreenshotCaptureService", "Screenshot cached at $cachePath, launching crop activity.")
            launchCropActivity(cachePath)
        } catch (exception: Exception) {
            ProjectionPermissionRepository.clear()
            AppLogger.logError("ScreenshotCaptureService", "Screen capture failed.", exception)
            notifyFailure()
            requestProjectionPermission()
        } finally {
            projection.stop()
        }
    }

    /**
     * Launches the cropping activity using the captured screenshot cache path.
     *
     * Inputs: [cachePath] - Absolute file path for the captured screenshot.
     * Outputs: Crop UI displayed to the user.
     */
    private fun launchCropActivity(cachePath: String) {
        val intent = Intent(this, CropScreenshotActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        intent.putExtra(CropScreenshotActivity.EXTRA_SCREENSHOT_PATH, cachePath)
        startActivity(intent)
    }

    /**
     * Requests media projection permission by launching the configured activity.
     *
     * Inputs: None.
     * Outputs: Permission prompt displayed when possible.
     */
    private fun requestProjectionPermission() {
        val intent = Intent(this, ProjectionRequestActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ProjectionRequestActivity.EXTRA_TRIGGER_CAPTURE, true)
        }
        startActivity(intent)
    }

    /**
     * Shows a toast informing the user that a capture is in progress.
     *
     * Inputs: None.
     * Outputs: Toast message on the main thread.
     */
    private fun notifyCapturing() {
        if (!showCapturingToast) {
            AppLogger.logInfo("ScreenshotCaptureService", "Capture toast suppressed for silent run.")
            return
        }
        Handler(Looper.getMainLooper()).post {
            ToastMaker.show(this, getString(R.string.capturing_screen))
        }
    }

    /**
     * Shows a toast indicating capture failure.
     *
     * Inputs: None.
     * Outputs: Toast message on the main thread.
     */
    private fun notifyFailure() {
        Handler(Looper.getMainLooper()).post {
            ToastMaker.show(this, getString(R.string.capture_failure))
        }
    }

    /**
     * Informs the user that permission is required before captures can resume.
     *
     * Inputs: None.
     * Outputs: Toast displayed to the user.
     */
    private fun notifyPermissionRequired() {
        Handler(Looper.getMainLooper()).post {
            ToastMaker.show(this, getString(R.string.capture_permission_missing))
        }
    }

    /**
     * Creates the persistent notification keeping the foreground service alive.
     *
     * Inputs: None.
     * Outputs: Configured [Notification] instance.
     */
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_service_channel_name))
            .setContentText(getString(R.string.capture_service_channel_description))
            .setSmallIcon(R.drawable.ic_overlay_button)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Registers the notification channel on Android O and above.
     *
     * Inputs: None.
     * Outputs: Notification channel created if necessary.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.capture_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.capture_service_channel_description)
            }
            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Stops the foreground service in a backward-compatible way.
     *
     * Inputs: None.
     * Outputs: Foreground state removed.
     */
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        private const val CHANNEL_ID = "screenshot_capture_channel"
        private const val NOTIFICATION_ID = 2001
        private const val PERMISSION_DISMISS_DELAY_MS = 600L
        @VisibleForTesting
        internal const val EXTRA_SHOW_CAPTURING_TOAST = "extra_show_capturing_toast"

        /**
         * Creates a start intent for this service.
         *
         * Inputs: [context] - Caller context.
         * Outputs: Configured [Intent] targeting the capture service.
         */
        fun createIntent(context: Context, showCapturingToast: Boolean = false): Intent =
            Intent(context, ScreenshotCaptureService::class.java).apply {
                putExtra(EXTRA_SHOW_CAPTURING_TOAST, showCapturingToast)
            }
    }
}

/**
 * Helper object used to centralize toast creation for capture status messages.
 *
 * Inputs: Context and string message.
 * Outputs: Short toast notifications.
 */
private object ToastMaker {

    fun show(context: Context, message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
