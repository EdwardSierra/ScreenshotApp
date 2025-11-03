package com.example.screenshotapp.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.screenshotapp.R
import com.example.screenshotapp.logging.AppLogger
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Renders the user-drawn selection overlay and dispatches geometry information.
 *
 * Inputs: Touch events defining the desired shape.
 * Outputs: Selection callbacks producing [SelectionShape] instances.
 */
class SelectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var selectionMode: SelectionMode = SelectionMode.RECTANGLE
    var onSelectionFinished: ((SelectionShape) -> Unit)? = null
    var onSelectionCancelled: (() -> Unit)? = null

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.selectionStroke)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.selectionFill)
    }

    private val startPoint = PointF()
    private val endPoint = PointF()
    private var drawing = false

    /**
     * Resets the overlay to its initial state by clearing any drawn shapes.
     *
     * Inputs: None.
     * Outputs: View invalidated without a drawn selection.
     */
    fun reset() {
        drawing = false
        invalidate()
    }

    /**
     * Handles touch interactions to capture the user's drawn shape.
     *
     * Inputs: [event] - Motion event describing the current gesture.
     * Outputs: Updates view state and notifies listeners when finished.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawing = true
                startPoint.set(event.x, event.y)
                endPoint.set(event.x, event.y)
                AppLogger.logInfo("SelectionOverlayView", "Selection started at ${startPoint.x}, ${startPoint.y}")
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (drawing) {
                    endPoint.set(event.x, event.y)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (drawing) {
                    endPoint.set(event.x, event.y)
                    drawing = false
                    invalidate()
                    produceSelection()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                drawing = false
                invalidate()
                onSelectionCancelled?.invoke()
                AppLogger.logInfo("SelectionOverlayView", "Selection cancelled.")
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Draws the selection overlay onto the provided canvas.
     *
     * Inputs: [canvas] - Canvas to render onto.
     * Outputs: Visual representation of the current selection.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawing) {
            when (selectionMode) {
                SelectionMode.RECTANGLE -> drawRectangle(canvas)
                SelectionMode.CIRCLE -> drawCircle(canvas)
            }
        }
    }

    /**
     * Renders the rectangle overlay corresponding to the drag gesture.
     *
     * Inputs: [canvas] - Canvas to draw on.
     * Outputs: Rectangle with fill and stroke.
     */
    private fun drawRectangle(canvas: Canvas) {
        val rect = RectF(
            min(startPoint.x, endPoint.x),
            min(startPoint.y, endPoint.y),
            max(startPoint.x, endPoint.x),
            max(startPoint.y, endPoint.y)
        )
        canvas.drawRect(rect, fillPaint)
        canvas.drawRect(rect, strokePaint)
    }

    /**
     * Renders the circle overlay corresponding to the drag gesture.
     *
     * Inputs: [canvas] - Canvas to draw on.
     * Outputs: Circle with fill and stroke.
     */
    private fun drawCircle(canvas: Canvas) {
        val radius = hypot(
            abs(endPoint.x - startPoint.x).toDouble(),
            abs(endPoint.y - startPoint.y).toDouble()
        ).toFloat() / 2f
        val centerX = (startPoint.x + endPoint.x) / 2f
        val centerY = (startPoint.y + endPoint.y) / 2f
        canvas.drawCircle(centerX, centerY, radius, fillPaint)
        canvas.drawCircle(centerX, centerY, radius, strokePaint)
    }

    /**
     * Constructs the appropriate selection object based on the completed gesture.
     *
     * Inputs: None.
     * Outputs: Invokes callbacks with either rectangle or circle selection data.
     */
    private fun produceSelection() {
        val selection = when (selectionMode) {
            SelectionMode.RECTANGLE -> {
                val rect = RectF(
                    min(startPoint.x, endPoint.x),
                    min(startPoint.y, endPoint.y),
                    max(startPoint.x, endPoint.x),
                    max(startPoint.y, endPoint.y)
                )
                SelectionShape.Rectangle(rect)
            }
            SelectionMode.CIRCLE -> {
                val center = PointF(
                    (startPoint.x + endPoint.x) / 2f,
                    (startPoint.y + endPoint.y) / 2f
                )
                val radius = hypot(
                    abs(endPoint.x - startPoint.x).toDouble(),
                    abs(endPoint.y - startPoint.y).toDouble()
                ).toFloat() / 2f
                SelectionShape.Circle(center, radius)
            }
        }
        AppLogger.logInfo("SelectionOverlayView", "Selection completed in mode $selectionMode")
        onSelectionFinished?.invoke(selection)
    }
}
