package com.example.screenshotapp.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.screenshotapp.R
import com.example.screenshotapp.databinding.ActivityMainBinding
import com.example.screenshotapp.logging.AppLogger
import com.example.screenshotapp.ui.overlay.ScreenshotOverlayService
import com.example.screenshotapp.util.PermissionHelper

/**
 * Hosts the onboarding flow that requests permissions and launches the overlay service.
 *
 * Inputs: User interactions with the UI.
 * Outputs: Foreground service start with granted permissions.
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
     * Prepares the activity by inflating the layout and wiring button listeners.
     *
     * Inputs: [savedInstanceState] - Previously saved state bundle.
     * Outputs: Initialized UI ready for user interaction.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            ?: throw IllegalStateException("MediaProjectionManager not available.")

        binding.startOverlayButton.setOnClickListener {
            onStartOverlayTapped()
        }
        binding.openOverlayPermissionButton.setOnClickListener {
            PermissionHelper.openOverlaySettings(this)
        }
    }

    /**
     * Refreshes permission indicators each time the activity returns to the foreground.
     *
     * Inputs: None.
     * Outputs: Updated visibility for permission prompts.
     */
    override fun onResume() {
        super.onResume()
        updatePermissionState()
    }

    /**
     * Handles the start overlay button click by validating permissions and requesting capture access.
     *
     * Inputs: None.
     * Outputs: Launches the media projection permission dialog.
     */
    private fun onStartOverlayTapped() {
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, getString(R.string.request_overlay_permission), Toast.LENGTH_SHORT).show()
            updatePermissionState()
            return
        }
        requestProjection()
    }

    /**
     * Begins the media projection permission flow using the registered launcher.
     *
     * Inputs: None.
     * Outputs: System permission dialog displayed.
     */
    private fun requestProjection() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        AppLogger.logInfo("MainActivity", "Requesting media projection permission.")
        projectionLauncher.launch(captureIntent)
    }

    /**
     * Processes the result from the media projection permission activity.
     *
     * Inputs: [resultCode] - Result code from the permission activity, [data] - Returned intent.
     * Outputs: Starts the overlay service when permission is granted.
     */
    private fun handleProjectionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            AppLogger.logInfo("MainActivity", "Media projection permission granted.")
            startOverlayService(resultCode, data)
        } else {
            Toast.makeText(this, getString(R.string.screen_capture_denied), Toast.LENGTH_SHORT).show()
            AppLogger.logError("MainActivity", "Media projection permission denied.")
        }
    }

    /**
     * Starts the foreground screenshot overlay service with the required media projection data.
     *
     * Inputs: [resultCode] - Permission result code, [data] - Media projection data.
     * Outputs: Foreground service initiated.
     */
    private fun startOverlayService(resultCode: Int, data: Intent) {
        val intent = Intent(this, ScreenshotOverlayService::class.java).apply {
            putExtra(ScreenshotOverlayService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenshotOverlayService.EXTRA_RESULT_DATA, data)
        }
        AppLogger.logInfo("MainActivity", "Starting overlay service.")
        ContextCompat.startForegroundService(this, intent)
        moveTaskToBack(true)
    }

    /**
     * Updates UI elements to reflect the current overlay permission state.
     *
     * Inputs: None.
     * Outputs: Visibility changes for informational views.
     */
    private fun updatePermissionState() {
        val hasPermission = PermissionHelper.hasOverlayPermission(this)
        binding.permissionMessage.visibility = if (hasPermission) View.GONE else View.VISIBLE
        binding.openOverlayPermissionButton.visibility = binding.permissionMessage.visibility
    }
}
