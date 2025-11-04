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
import com.example.screenshotapp.logging.AppLogger
import com.example.screenshotapp.ui.overlay.ScreenshotOverlayService

/**
 * Requests screen capture permission and relays the result back to the overlay service.
 *
 * Inputs: Launch trigger from [ScreenshotOverlayService] when a new permission is required.
 * Outputs: Starts the overlay service with fresh projection data or surfaces an error.
 */
class ProjectionRequestActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleProjectionResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            ?: run {
                Toast.makeText(this, R.string.screen_capture_denied, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        AppLogger.logInfo("ProjectionRequestActivity", "Requesting new media projection permission.")
        projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun handleProjectionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            AppLogger.logInfo("ProjectionRequestActivity", "Media projection permission granted from activity.")
            val serviceIntent = Intent(this, ScreenshotOverlayService::class.java).apply {
                putExtra(ScreenshotOverlayService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenshotOverlayService.EXTRA_RESULT_DATA, data)
                putExtra(ScreenshotOverlayService.EXTRA_AUTO_START_SELECTION, true)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            Toast.makeText(this, R.string.screen_capture_denied, Toast.LENGTH_SHORT).show()
            AppLogger.logError("ProjectionRequestActivity", "Media projection permission denied.")
        }
        finish()
    }
}
