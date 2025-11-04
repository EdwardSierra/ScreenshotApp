package com.example.screenshotapp.capture

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.screenshotapp.logging.AppLogger

/**
 * Crops full screen screenshots to a rectangular region selected by the user.
 *
 * Inputs: Full-screen [Bitmap] instances and integer [Rect] bounds.
 * Outputs: Cropped bitmaps containing only the selected area.
 */
class ScreenshotProcessor {

    /**
     * Returns a bitmap containing only the pixels inside [selection].
     *
     * Inputs: [bitmap] - Source screenshot, [selection] - Bounds in bitmap coordinates.
     * Outputs: Newly created bitmap representing the cropped area.
     */
    fun crop(bitmap: Bitmap, selection: Rect): Bitmap {
        val sanitized = selection.sanitize(bitmap.width, bitmap.height)
        val width = sanitized.width()
        val height = sanitized.height()
        if (width <= 0 || height <= 0) {
            AppLogger.logError("ScreenshotProcessor", "Invalid selection bounds: $sanitized")
            throw IllegalArgumentException("Selection bounds must define a visible area.")
        }
        return Bitmap.createBitmap(bitmap, sanitized.left, sanitized.top, width, height)
    }

    /**
     * Ensures the selection rectangle stays within the bitmap boundaries.
     *
     * Inputs: [maxWidth], [maxHeight] - Dimensions of the source bitmap.
     * Outputs: Clamped [Rect] that is safe for cropping.
     */
    private fun Rect.sanitize(maxWidth: Int, maxHeight: Int): Rect {
        val left = left.coerceIn(0, maxWidth)
        val top = top.coerceIn(0, maxHeight)
        val right = right.coerceIn(left + 1, maxWidth)
        val bottom = bottom.coerceIn(top + 1, maxHeight)
        return Rect(left, top, right, bottom)
    }
}

