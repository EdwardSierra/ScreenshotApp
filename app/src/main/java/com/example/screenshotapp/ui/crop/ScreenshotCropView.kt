package com.example.screenshotapp.ui.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.screenshotapp.R
import com.example.screenshotapp.logging.AppLogger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Displays a captured screenshot and lets the user draw a rectangular selection.
 *
 * Inputs: Loaded [Bitmap] screenshots and touch events.
 * Outputs: Cropping callbacks containing the selected bitmap coordinates.
 */
class ScreenshotCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onSelectionFinished: ((Rect) -> Unit)? = null
    var onSelectionInvalid: (() -> Unit)? = null

    private val screenshotMatrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        color = ContextCompat.getColor(context, R.color.selectionStroke)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.selectionFill)
    }

    private val startPoint = PointF()
    private val endPoint = PointF()
    private val currentRect = RectF()

    private var screenshot: Bitmap? = null
    private var drawing = false
    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    /**
     * Assigns the screenshot that should be displayed beneath the selection overlay.
     *
     * Inputs: [bitmap] - Screenshot rendered inside the view bounds.
     * Outputs: View invalidated with the new content.
     */
    fun setScreenshot(bitmap: Bitmap) {
        screenshot = bitmap
        configureMatrix()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        configureMatrix()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = screenshot ?: return
        canvas.drawColor(ContextCompat.getColor(context, R.color.overlayBackground))
        paint.reset()
        paint.isFilterBitmap = true
        canvas.drawBitmap(bitmap, screenshotMatrix, paint)
        if (drawing) {
            canvas.drawRect(currentRect, fillPaint)
            canvas.drawRect(currentRect, strokePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (screenshot == null) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawing = true
                startPoint.set(event.x, event.y)
                endPoint.set(event.x, event.y)
                updateCurrentRect()
                AppLogger.logInfo("ScreenshotCropView", "Selection started at (${startPoint.x}, ${startPoint.y})")
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (drawing) {
                    endPoint.set(event.x, event.y)
                    updateCurrentRect()
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (drawing) {
                    drawing = false
                    endPoint.set(event.x, event.y)
                    updateCurrentRect()
                    invalidate()
                    deliverSelection()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                drawing = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Converts the current gesture rectangle into bitmap coordinates and emits the callback.
     *
     * Inputs: None.
     * Outputs: Cropping callback invoked or invalid selection warning.
     */
    private fun deliverSelection() {
        val bitmap = screenshot ?: return
        val mappedRect = mapToBitmapRect(currentRect, bitmap.width, bitmap.height)
        if (mappedRect == null) {
            AppLogger.logError("ScreenshotCropView", "Selection too small to crop.")
            onSelectionInvalid?.invoke()
        } else {
            AppLogger.logInfo("ScreenshotCropView", "Selection completed: $mappedRect")
            onSelectionFinished?.invoke(mappedRect)
        }
    }

    /**
     * Converts the visible rectangle coordinates into bitmap coordinates.
     *
     * Inputs: [rect] - Rectangle in view space, [bitmapWidth], [bitmapHeight] - Bitmap dimensions.
     * Outputs: Clamped bitmap [Rect] or null when the selection is too small.
     */
    private fun mapToBitmapRect(rect: RectF, bitmapWidth: Int, bitmapHeight: Int): Rect? {
        if (scaleFactor == 0f) {
            return null
        }
        val left = ((rect.left - offsetX) / scaleFactor).coerceIn(0f, bitmapWidth.toFloat())
        val top = ((rect.top - offsetY) / scaleFactor).coerceIn(0f, bitmapHeight.toFloat())
        val right = ((rect.right - offsetX) / scaleFactor).coerceIn(0f, bitmapWidth.toFloat())
        val bottom = ((rect.bottom - offsetY) / scaleFactor).coerceIn(0f, bitmapHeight.toFloat())
        val actualLeft = min(left, right).roundToInt()
        val actualTop = min(top, bottom).roundToInt()
        val actualRight = max(left, right).roundToInt()
        val actualBottom = max(top, bottom).roundToInt()
        val width = actualRight - actualLeft
        val height = actualBottom - actualTop
        return if (width >= MIN_SELECTION_SIZE && height >= MIN_SELECTION_SIZE) {
            Rect(actualLeft, actualTop, actualRight, actualBottom)
        } else {
            null
        }
    }

    /**
     * Updates the rectangle used for drawing during the current gesture.
     *
     * Inputs: None.
     * Outputs: RectF representing the visible selection bounds.
     */
    private fun updateCurrentRect() {
        currentRect.set(
            min(startPoint.x, endPoint.x),
            min(startPoint.y, endPoint.y),
            max(startPoint.x, endPoint.x),
            max(startPoint.y, endPoint.y)
        )
    }

    /**
     * Configures the matrix that draws the screenshot using a center-fit strategy.
     *
     * Inputs: None.
     * Outputs: Matrix values ready for rendering and coordinate conversion.
     */
    private fun configureMatrix() {
        val bitmap = screenshot ?: return
        if (width == 0 || height == 0) {
            return
        }
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scaleX = viewWidth / bitmap.width
        val scaleY = viewHeight / bitmap.height
        scaleFactor = min(scaleX, scaleY)
        offsetX = (viewWidth - bitmap.width * scaleFactor) / 2f
        offsetY = (viewHeight - bitmap.height * scaleFactor) / 2f
        screenshotMatrix.reset()
        screenshotMatrix.postScale(scaleFactor, scaleFactor)
        screenshotMatrix.postTranslate(offsetX, offsetY)
        AppLogger.logInfo("ScreenshotCropView", "Matrix configured with scale=$scaleFactor offsets=($offsetX,$offsetY)")
    }

    companion object {
        private const val MIN_SELECTION_SIZE = 8
        private const val STROKE_WIDTH = 4f
    }
}

