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

package com.example.screenshotapp.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.screenshotapp.R
import com.example.screenshotapp.capture.ProjectionPermissionRepository
import com.example.screenshotapp.logging.AppLogger
import com.example.screenshotapp.ui.capture.ScreenshotCaptureService

/**
 * Requests media projection permission and stores the result for future captures.
 *
 * Inputs: Launch triggers from the quick settings tile or onboarding flow.
 * Outputs: Cached permission tokens and optional capture service start.
 */
class ProjectionRequestActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleProjectionResult(result.resultCode, result.data)
    }

    /**
     * Begins the permission request immediately when the activity is created.
     *
     * Inputs: [savedInstanceState] - Previously saved state.
     * Outputs: Media projection dialog displayed.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            ?: run {
                Toast.makeText(this, R.string.screen_capture_denied, Toast.LENGTH_SHORT).show()
                AppLogger.logError("ProjectionRequestActivity", "MediaProjectionManager unavailable.")
                finish()
                return
            }
        AppLogger.logInfo("ProjectionRequestActivity", "Requesting media projection permission.")
        projectionLauncher.launch(createCaptureIntent())
    }

    /**
     * Stores the permission data and optionally restarts the capture workflow.
     *
     * Inputs: [resultCode] - System result code, [data] - Returned intent.
     * Outputs: Foreground capture service started or an error toast.
     */
    private fun handleProjectionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            ProjectionPermissionRepository.store(resultCode, Intent(data))
            val triggeredCapture = intent.getBooleanExtra(EXTRA_TRIGGER_CAPTURE, false)
            if (shouldShowPermissionGrantedToast(triggeredCapture)) {
                Toast.makeText(this, getString(R.string.capture_permission_granted), Toast.LENGTH_SHORT).show()
                AppLogger.logInfo("ProjectionRequestActivity", "Media projection permission granted with toast.")
            } else {
                AppLogger.logInfo(
                    "ProjectionRequestActivity",
                    "Media projection permission granted; skipping toast to avoid overlaying capture."
                )
            }
            if (triggeredCapture) {
                val serviceIntent = ScreenshotCaptureService.createIntent(this)
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        } else {
            Toast.makeText(this, R.string.screen_capture_denied, Toast.LENGTH_SHORT).show()
            AppLogger.logError("ProjectionRequestActivity", "Media projection permission denied.")
        }
        finish()
    }

    companion object {
        const val EXTRA_TRIGGER_CAPTURE = "extra_trigger_capture"

        /**
         * Determines whether the success toast should be displayed after permission grant.
         *
         * Inputs: [triggeredCapture] - True when the request originated from the quick settings tile.
         * Outputs: True to show the toast, false when it should be suppressed.
         */
        @VisibleForTesting
        internal fun shouldShowPermissionGrantedToast(triggeredCapture: Boolean): Boolean =
            !triggeredCapture
    }

    /**
     * Builds the screen capture intent, preferring full-display capture on Android 14+.
     *
     * Inputs: None.
     * Outputs: Intent preconfigured for the system capture dialog.
     */
    private fun createCaptureIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            AppLogger.logInfo("ProjectionRequestActivity", "Defaulting to full-display capture region.")
            mediaProjectionManager.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay()
            )
        } else {
            mediaProjectionManager.createScreenCaptureIntent()
        }
    }
}
