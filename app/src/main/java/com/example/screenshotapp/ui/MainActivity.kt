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
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.screenshotapp.R
import com.example.screenshotapp.capture.ProjectionPermissionRepository
import com.example.screenshotapp.databinding.ActivityMainBinding
import com.example.screenshotapp.logging.AppLogger

/**
 * Presents onboarding guidance for configuring the quick settings capture tile.
 *
 * Inputs: User taps on the request permission and quick settings editor buttons.
 * Outputs: Stored media projection permission data ready for future captures.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleProjectionResult(result.resultCode, result.data)
    }

    /**
     * Prepares the activity by inflating the layout and binding click listeners.
     *
     * Inputs: [savedInstanceState] - Previously saved state bundle.
     * Outputs: User interface ready for interaction.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            ?: throw IllegalStateException("MediaProjectionManager not available.")

        binding.requestCaptureButton.setOnClickListener {
            requestCapturePermission()
        }
        binding.openQuickSettingsButton.setOnClickListener {
            openQuickSettingsEditor()
        }
    }

    /**
     * Refreshes permission messaging whenever the activity returns to the foreground.
     *
     * Inputs: None.
     * Outputs: Updated status text reflecting the capture permission state.
     */
    override fun onResume() {
        super.onResume()
        updatePermissionState()
    }

    /**
     * Launches the system media projection prompt so the user can grant capture access.
     *
     * Inputs: None.
     * Outputs: System permission dialog displayed.
     */
    private fun requestCapturePermission() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        AppLogger.logInfo("MainActivity", "Requesting media projection permission from onboarding flow.")
        projectionLauncher.launch(captureIntent)
    }

    /**
     * Processes the result from the media projection prompt and stores valid permissions.
     *
     * Inputs: [resultCode] - System result code, [data] - Media projection intent data.
     * Outputs: Stored permission data or an error toast.
     */
    private fun handleProjectionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            ProjectionPermissionRepository.store(resultCode, Intent(data))
            Toast.makeText(this, getString(R.string.capture_permission_granted), Toast.LENGTH_SHORT).show()
            AppLogger.logInfo("MainActivity", "Media projection permission granted.")
        } else {
            Toast.makeText(this, getString(R.string.screen_capture_denied), Toast.LENGTH_SHORT).show()
            AppLogger.logError("MainActivity", "Media projection permission denied.")
        }
        updatePermissionState()
    }

    /**
     * Launches the quick settings editor so the user can pin the screenshot tile.
     *
     * Inputs: None.
     * Outputs: System UI opened when supported.
     */
    private fun openQuickSettingsEditor() {
        val editorIntent = Intent("android.settings.QUICK_SETTINGS_ADD_OTHER_TILES")
        val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
        try {
            AppLogger.logInfo("MainActivity", "Opening quick settings editor.")
            startActivity(editorIntent)
        } catch (exception: Exception) {
            AppLogger.logError("MainActivity", "Quick settings editor launch failed; falling back to Settings.", exception)
            startActivity(fallbackIntent)
        }
    }

    /**
     * Updates the permission status text to mirror the stored media projection state.
     *
     * Inputs: None.
     * Outputs: Status label text refreshed for the current permission state.
     */
    private fun updatePermissionState() {
        val hasPermission = ProjectionPermissionRepository.hasPermission()
        val message = if (hasPermission) {
            getString(R.string.capture_permission_granted)
        } else {
            getString(R.string.capture_permission_missing)
        }
        binding.permissionStatusText.text = message
    }
}
