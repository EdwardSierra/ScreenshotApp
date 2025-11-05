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

package com.example.screenshotapp.ui.tile

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the manifest configuration for [ScreenshotTileService].
 *
 * Inputs: Robolectric application context for manifest inspection.
 * Outputs: Assertions covering exported state and required binding permission.
 */
@RunWith(RobolectricTestRunner::class)
class ScreenshotTileServiceManifestTest {

    /**
     * Confirms the tile service is exported so System UI can bind while still protected.
     *
     * Inputs: None.
     * Outputs: Assertion results guaranteeing exported flag and permission string.
     */
    @Test
    fun serviceIsExportedAndProtectedByPermission() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val componentName = ComponentName(context, ScreenshotTileService::class.java)
        val serviceInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getServiceInfo(
                componentName,
                PackageManager.ComponentInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getServiceInfo(componentName, 0)
        }

        assertTrue("Quick Settings tile service must be exported.", serviceInfo.exported)
        assertEquals(
            "android.permission.BIND_QUICK_SETTINGS_TILE",
            serviceInfo.permission
        )
    }
}
