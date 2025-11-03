package com.example.screenshotapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Supplies helper methods for managing overlay permissions required by the app.
 *
 * Inputs: Application or activity context.
 * Outputs: Overlay permission status checks and navigation intents.
 */
object PermissionHelper {

    private const val PACKAGE_SCHEME = "package"

    /**
     * Determines whether the app currently holds drawing-over-other-apps permission.
     *
     * Inputs: [context] - Any valid context.
     * Outputs: True when overlay permission is granted, otherwise false.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Launches the system settings screen to request overlay permission from the user.
     *
     * Inputs: [context] - Activity context used to start the settings intent.
     * Outputs: Opens the overlay permission screen.
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("$PACKAGE_SCHEME:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
