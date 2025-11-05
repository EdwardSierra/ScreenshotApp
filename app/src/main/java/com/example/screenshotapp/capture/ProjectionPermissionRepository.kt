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

package com.example.screenshotapp.capture

import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import com.example.screenshotapp.logging.AppLogger

/**
 * Stores and rehydrates media projection permissions for repeated screenshot captures.
 *
 * Inputs: Result codes and intent data returned from the system capture prompt.
 * Outputs: Ready-to-use [MediaProjection] instances or null when unavailable.
 */
object ProjectionPermissionRepository {

    private var resultCode: Int? = null
    private var projectionData: Intent? = null
    private var pendingDelay = false

    /**
     * Persists the latest permission grant so background services can reuse it.
     *
     * Inputs: [code] - Result code from the capture prompt, [data] - Intent containing projection tokens.
     * Outputs: Internal state updated for future capture requests.
     */
    fun store(code: Int, data: Intent) {
        resultCode = code
        projectionData = Intent(data)
        pendingDelay = true
        AppLogger.logInfo("ProjectionPermissionRepository", "Stored media projection permission.")
    }

    /**
     * Returns true when a permission token is currently cached.
     *
     * Inputs: None.
     * Outputs: Boolean indicating whether capture can proceed immediately.
     */
    fun hasPermission(): Boolean = resultCode != null && projectionData != null

    /**
     * Attempts to build a new [MediaProjection] from the cached permission data.
     *
     * Inputs: [manager] - Android media projection manager.
     * Outputs: Fresh [MediaProjection] or null if the cached data is invalid.
     */
    fun getProjection(manager: MediaProjectionManager): MediaProjection? {
        val code = resultCode
        val data = projectionData
        if (code == null || data == null) {
            return null
        }
        return try {
            manager.getMediaProjection(code, Intent(data))
        } catch (throwable: Throwable) {
            AppLogger.logError("ProjectionPermissionRepository", "Failed to obtain media projection from cached data.", throwable)
            clear()
            null
        }
    }

    /**
     * Clears the cached permission data after a failure or explicit revocation.
     *
     * Inputs: None.
     * Outputs: Internal cached tokens removed.
     */
    fun clear() {
        resultCode = null
        projectionData = null
        pendingDelay = false
        AppLogger.logInfo("ProjectionPermissionRepository", "Cleared stored media projection permission.")
    }

    /**
     * Returns true only once immediately after a permission grant so callers can delay work.
     *
     * Inputs: None.
     * Outputs: Boolean indicating that the permission was just stored.
     */
    fun consumeJustGrantedFlag(): Boolean {
        val wasPending = pendingDelay
        pendingDelay = false
        return wasPending
    }
}
