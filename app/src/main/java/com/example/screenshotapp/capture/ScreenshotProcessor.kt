package com.example.screenshotapp.capture

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.example.screenshotapp.logging.AppLogger
import com.example.screenshotapp.ui.overlay.SelectionShape
import kotlin.math.max
import kotlin.math.min

/**
 * Handles bitmap manipulation for cropping screenshots based on user selections.
 *
 * Inputs: Full-screen bitmaps and shape selections.
 * Outputs: Cropped bitmaps ready for persistence or clipboard sharing.
 */
class ScreenshotProcessor {

    /**
     * Crops the provided bitmap according to the supplied selection.
     *
     * Inputs: [bitmap] - Original screenshot, [selection] - User defined shape.
     * Outputs: Cropped bitmap matching the selection's geometry.
     */
    fun crop(bitmap: Bitmap, selection: SelectionShape): Bitmap {
        return when (selection) {
            is SelectionShape.Rectangle -> cropRectangle(bitmap, selection.bounds)
            is SelectionShape.Circle -> cropCircle(bitmap, selection)
        }
    }

    /**
     * Executes rectangular cropping while clamping bounds to the bitmap size.
     *
     * Inputs: [bitmap] - Original screenshot, [rectF] - Floating rectangle bounds.
     * Outputs: Bitmap cropped to the rectangle area.
     */
    private fun cropRectangle(bitmap: Bitmap, rectF: RectF): Bitmap {
        val rect = rectF.toBitmapRect(bitmap.width, bitmap.height)
        val width = rect.width()
        val height = rect.height()
        if (width <= 0 || height <= 0) {
            AppLogger.logError("ScreenshotProcessor", "Invalid rectangle dimensions: $rect")
            throw IllegalArgumentException("Invalid rectangle selection.")
        }
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, width, height)
    }

    /**
     * Executes circular cropping by drawing the selected area onto a masked bitmap.
     *
     * Inputs: [bitmap] - Original screenshot, [selection] - Circle geometry.
     * Outputs: Bitmap containing only the circular selection.
     */
    private fun cropCircle(bitmap: Bitmap, selection: SelectionShape.Circle): Bitmap {
        val left = (selection.center.x - selection.radius).coerceIn(0f, bitmap.width.toFloat())
        val top = (selection.center.y - selection.radius).coerceIn(0f, bitmap.height.toFloat())
        val right = (selection.center.x + selection.radius).coerceIn(0f, bitmap.width.toFloat())
        val bottom = (selection.center.y + selection.radius).coerceIn(0f, bitmap.height.toFloat())

        val rect = Rect(
            left.toInt(),
            top.toInt(),
            max(left.toInt() + 1, right.toInt()),
            max(top.toInt() + 1, bottom.toInt())
        )

        val clampedRect = rect.clamp(bitmap.width, bitmap.height)
        val croppedWidth = clampedRect.width()
        val croppedHeight = clampedRect.height()
        val diameter = min(croppedWidth, croppedHeight)

        if (croppedWidth <= 0 || croppedHeight <= 0) {
            AppLogger.logError("ScreenshotProcessor", "Invalid circle bounds: $clampedRect")
            throw IllegalArgumentException("Invalid circle selection.")
        }

        val output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix()
        matrix.postTranslate(-clampedRect.left.toFloat(), -clampedRect.top.toFloat())
        shader.setLocalMatrix(matrix)
        paint.shader = shader

        val radius = diameter / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        return output
    }

    /**
     * Converts a floating rectangle into integer coordinates within bitmap bounds.
     *
     * Inputs: [RectF] selection bounds, [bitmapWidth], [bitmapHeight] - Bitmap dimensions.
     * Outputs: Clamped [Rect] suitable for bitmap operations.
     */
    private fun RectF.toBitmapRect(bitmapWidth: Int, bitmapHeight: Int): Rect {
        val left = left.coerceIn(0f, bitmapWidth.toFloat())
        val top = top.coerceIn(0f, bitmapHeight.toFloat())
        val right = right.coerceIn(0f, bitmapWidth.toFloat())
        val bottom = bottom.coerceIn(0f, bitmapHeight.toFloat())
        return Rect(
            min(left, right).toInt(),
            min(top, bottom).toInt(),
            max(left, right).toInt(),
            max(top, bottom).toInt()
        ).clamp(bitmapWidth, bitmapHeight)
    }

    /**
     * Restricts the rectangle to stay within the provided bitmap dimensions.
     *
     * Inputs: [maxWidth], [maxHeight] - Bitmap size constraints.
     * Outputs: New [Rect] trimmed to valid coordinates.
     */
    private fun Rect.clamp(maxWidth: Int, maxHeight: Int): Rect {
        val clampedLeft = left.coerceIn(0, maxWidth)
        val clampedTop = top.coerceIn(0, maxHeight)
        val clampedRight = right.coerceIn(clampedLeft + 1, maxWidth)
        val clampedBottom = bottom.coerceIn(clampedTop + 1, maxHeight)
        return Rect(clampedLeft, clampedTop, clampedRight, clampedBottom)
    }
}
