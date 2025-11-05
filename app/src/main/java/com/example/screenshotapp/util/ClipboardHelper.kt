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

package com.example.screenshotapp.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import com.example.screenshotapp.logging.AppLogger

/**
 * Manages clipboard operations for sharing captured screenshots.
 *
 * Inputs: Application context and screenshot URIs.
 * Outputs: Copied image references available to other applications.
 */
object ClipboardHelper {

    private const val CLIP_LABEL = "Screenshot"

    /**
     * Copies the supplied screenshot URI into the system clipboard.
     *
     * Inputs: [context] - Application context, [uri] - Saved screenshot location.
     * Outputs: Clipboard updated with the screenshot reference.
     */
    fun copyImageToClipboard(context: Context, uri: Uri) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(context.contentResolver, CLIP_LABEL, uri)
        clipboard.setPrimaryClip(clip)
        context.grantUriPermission(
            "com.android.systemui",
            uri,
            IntentFlags.readWriteFlags
        )
        AppLogger.logInfo("ClipboardHelper", "Image copied to clipboard: $uri")
    }
}

/**
 * Provides intent flag combinations for granting URI access.
 *
 * Inputs: None.
 * Outputs: Read/write flag bitmask.
 */
private object IntentFlags {

    /**
     * Returns the combined bitmask needed for granting read and write URI permissions.
     *
     * Inputs: None.
     * Outputs: Integer bitmask including read/write flags.
     */
    val readWriteFlags: Int
        get() = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}
