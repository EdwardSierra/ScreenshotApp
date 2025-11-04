package com.example.screenshotapp.ui.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.example.screenshotapp.R
import com.example.screenshotapp.capture.ProjectionPermissionRepository
import com.example.screenshotapp.logging.AppLogger
import com.example.screenshotapp.ui.ProjectionRequestActivity
import com.example.screenshotapp.ui.capture.ScreenshotCaptureService

/**
 * Exposes a quick settings tile that triggers the screenshot capture workflow.
 *
 * Inputs: Tile click events initiated by the user.
 * Outputs: Foreground capture service launches and permission requests when needed.
 */
class ScreenshotTileService : TileService() {

    /**
     * Refreshes the tile state whenever System UI starts listening.
     *
     * Inputs: None.
     * Outputs: Updated tile icon and state reflecting permission readiness.
     */
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.icon = Icon.createWithResource(this, R.drawable.ic_overlay_button)
            tile.label = getString(R.string.tile_label)
            tile.state = if (ProjectionPermissionRepository.hasPermission()) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            tile.updateTile()
        }
    }

    /**
     * Handles tile click events by triggering capture or requesting permission first.
     *
     * Inputs: None.
     * Outputs: Capture service start or permission request activity.
     */
    override fun onClick() {
        super.onClick()
        if (!ProjectionPermissionRepository.hasPermission()) {
            AppLogger.logInfo("ScreenshotTileService", "Permission missing; launching request activity.")
            val intent = Intent(this, ProjectionRequestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(ProjectionRequestActivity.EXTRA_TRIGGER_CAPTURE, true)
            }
            launchPermissionRequest(intent)
            return
        }
        AppLogger.logInfo("ScreenshotTileService", "Starting capture service from tile.")
        unlockAndRun {
            val serviceIntent = ScreenshotCaptureService.createIntent(this)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    /**
     * Collapses quick settings and launches the permission request intent.
     *
     * Inputs: [intent] - Intent targeting [ProjectionRequestActivity].
     * Outputs: Permission UI displayed using the best available API.
     */
    private fun launchPermissionRequest(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
