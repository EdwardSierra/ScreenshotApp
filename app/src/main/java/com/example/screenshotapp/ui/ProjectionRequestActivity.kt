package com.example.screenshotapp.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
        projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
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
            Toast.makeText(this, getString(R.string.capture_permission_granted), Toast.LENGTH_SHORT).show()
            AppLogger.logInfo("ProjectionRequestActivity", "Media projection permission granted.")
            if (intent.getBooleanExtra(EXTRA_TRIGGER_CAPTURE, false)) {
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
    }
}

