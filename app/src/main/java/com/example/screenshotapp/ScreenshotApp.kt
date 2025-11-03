package com.example.screenshotapp

import android.app.Application
import com.example.screenshotapp.logging.AppLogger

/**
 * Bootstraps global application dependencies including logging utilities.
 *
 * Inputs: Android application lifecycle events.
 * Outputs: Initialized shared services such as the file-backed logger.
 */
class ScreenshotApp : Application() {

    /**
     * Handles application start-up by initializing the logging infrastructure.
     *
     * Inputs: None.
     * Outputs: Ready-to-use [AppLogger] instance.
     */
    override fun onCreate() {
        super.onCreate()
        AppLogger.initialize(this)
    }
}
