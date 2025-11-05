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

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.screenshotapp.logging.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persists captured screenshots to disk and exposes shareable URIs.
 *
 * Inputs: Bitmaps captured from screen content.
 * Outputs: File-backed URIs accessible via a FileProvider.
 */
class ScreenshotStorage(private val context: Context) {

    private val folder = File(context.getExternalFilesDir(null), "screenshots")
    private val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Saves the provided bitmap as a PNG file under the screenshots directory.
     *
     * Inputs: [bitmap] - Cropped screenshot to persist.
     * Outputs: Content [Uri] referencing the stored image.
     */
    fun saveBitmap(bitmap: Bitmap): Uri {
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val fileName = "screenshot_${formatter.format(Date())}.png"
        val imageFile = File(folder, fileName)
        try {
            FileOutputStream(imageFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } catch (ioException: IOException) {
            AppLogger.logError("ScreenshotStorage", "Failed to save screenshot", ioException)
            throw ioException
        }
        val authority = "${context.packageName}.provider"
        val uri =
            FileProvider.getUriForFile(context, authority, imageFile)
        AppLogger.logInfo("ScreenshotStorage", "Saved screenshot to ${imageFile.absolutePath}")
        return uri
    }
}
