package com.example.screenshotapp.capture

import android.content.Context
import android.graphics.Bitmap
import com.example.screenshotapp.logging.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Provides temporary storage for full-screen screenshots prior to cropping.
 *
 * Inputs: Full-resolution [Bitmap] instances created by the capture flow.
 * Outputs: Cache file paths that can be read by the cropping activity.
 */
class ScreenshotCache(private val context: Context) {

    private val cacheDirectory: File by lazy {
        File(context.cacheDir, CACHE_FOLDER_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Writes the supplied bitmap to the app cache for short-term use.
     *
     * Inputs: [bitmap] - Screenshot bitmap.
     * Outputs: Absolute [String] path to the cached file.
     */
    fun write(bitmap: Bitmap): String {
        val file = File(cacheDirectory, "capture_${UUID.randomUUID()}.png")
        try {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } catch (ioException: IOException) {
            AppLogger.logError("ScreenshotCache", "Failed to persist screenshot cache file.", ioException)
            if (file.exists()) {
                file.delete()
            }
            throw ioException
        }
        AppLogger.logInfo("ScreenshotCache", "Cached screenshot at ${file.absolutePath}")
        return file.absolutePath
    }

    /**
     * Deletes the provided cache file if it still exists.
     *
     * Inputs: [path] - Absolute file path previously returned by [write].
     * Outputs: Cache file removed from disk.
     */
    fun delete(path: String?) {
        if (path.isNullOrBlank()) {
            return
        }
        val file = File(path)
        if (file.exists() && file.delete()) {
            AppLogger.logInfo("ScreenshotCache", "Deleted cached screenshot at $path")
        }
    }

    companion object {
        private const val CACHE_FOLDER_NAME = "capture_cache"
    }
}

