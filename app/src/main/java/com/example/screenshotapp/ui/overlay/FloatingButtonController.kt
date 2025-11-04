package com.example.screenshotapp.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.screenshotapp.R
import com.example.screenshotapp.logging.AppLogger
import kotlin.math.abs

/**
 * Manages the floating capture button overlay and drag interactions.
 *
 * Inputs: Window manager for overlay placement, click listener for selection initiation.
 * Outputs: Floating button rendered on top of other apps.
 */
class FloatingButtonController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onClick: () -> Unit,
    private val onCancel: () -> Unit
) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var overlayView: View? = null

    /**
     * Displays the floating button overlay if it is not already visible.
     *
     * Inputs: None.
     * Outputs: Overlay button attached to the window manager.
     */
    fun show() {
        if (overlayView != null) {
            return
        }
        AppLogger.logInfo("FloatingButtonController", "Attempting to show floating button.")
        val button = inflater.inflate(R.layout.widget_floating_button, null)
        val captureButton: ImageView = button.findViewById(R.id.captureButton)
        val closeButton: ImageView = button.findViewById(R.id.closeButton)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 150
        }

        captureButton.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDrag = false

            /**
             * Handles drag gestures to reposition the floating button.
             *
             * Inputs: [v] - The button view, [event] - Motion event data.
             * Outputs: Updated overlay coordinates or click callback invocation.
             */
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDrag = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (abs(dx) > DRAG_THRESHOLD || abs(dy) > DRAG_THRESHOLD) {
                            isDrag = true
                            params.x = initialX + dx.toInt()
                            params.y = initialY + dy.toInt()
                            windowManager.updateViewLayout(button, params)
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!isDrag) {
                            onClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        closeButton.setOnClickListener {
            onCancel()
        }

        try {
            windowManager.addView(button, params)
            overlayView = button
            AppLogger.logInfo("FloatingButtonController", "Floating button displayed.")
        } catch (securityException: SecurityException) {
            AppLogger.logError("FloatingButtonController", "Failed to add floating button. Missing overlay permission?", securityException)
            Toast.makeText(context, context.getString(R.string.request_overlay_permission), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Removes the floating button overlay when it is no longer required.
     *
     * Inputs: None.
     * Outputs: Overlay detached from the window manager.
     */
    fun dismiss() {
        overlayView?.let {
            windowManager.removeView(it)
            AppLogger.logInfo("FloatingButtonController", "Floating button removed.")
        }
        overlayView = null
    }

    companion object {
        private const val DRAG_THRESHOLD = 10
    }
}
