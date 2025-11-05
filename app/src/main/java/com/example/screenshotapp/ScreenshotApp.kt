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
