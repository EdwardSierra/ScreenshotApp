package com.example.screenshotapp.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.VisibleForTesting
import com.example.screenshotapp.logging.AppLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Executes one-off full screen captures using the media projection APIs.
 *
 * Inputs: Active [MediaProjection] instances.
 * Outputs: Full-resolution [Bitmap] images representing the current display.
 */
class ScreenCaptureManager(private val context: Context) {

    /**
     * Captures the current screen contents and returns the bitmap result.
     *
     * Inputs: [projection] - Authorized media projection instance.
     * Outputs: Bitmap of the full display or throws when capture fails.
     */
    suspend fun capture(projection: MediaProjection): Bitmap {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            IMAGE_READER_CAPACITY
        )
        val listenerThread = HandlerThread("quick-screenshot-listener").apply { start() }
        val handler = Handler(listenerThread.looper)
        val projectionCallback = registerProjectionCallback(projection, handler, imageReader)
        val virtualDisplay = projection.createVirtualDisplay(
            "quick-settings-screenshot",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            handler
        )
        AppLogger.logInfo("ScreenCaptureManager", "Virtual display created for capture.")

        try {
            imageReader.acquireLatestImage()?.close()
            val image = awaitImage(imageReader, listenerThread)
                ?: throw IllegalStateException("Timed out waiting for screen image.")
            return image.use { it.toBitmap(width, height) }
        } finally {
            imageReader.setOnImageAvailableListener(null, null)
            virtualDisplay.safeRelease()
            projection.unregisterCallback(projectionCallback)
            AppLogger.logInfo("ScreenCaptureManager", "MediaProjection callback unregistered after capture.")
            imageReader.close()
            listenerThread.quitSafely()
            try {
                listenerThread.join()
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            AppLogger.logInfo("ScreenCaptureManager", "Capture resources released.")
        }
    }

    /**
     * Suspends until the image reader receives a frame or the operation times out.
     *
     * Inputs: [reader] - Configured image reader, [thread] - Handler thread for callbacks.
     * Outputs: Captured [Image] or null when a timeout occurs.
     */
    private suspend fun awaitImage(reader: ImageReader, thread: HandlerThread): Image? {
        reader.acquireLatestImage()?.let { return it }
        val handler = Handler(thread.looper)
        return withTimeoutOrNull(CAPTURE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = ImageReader.OnImageAvailableListener { readyReader ->
                    val image = readyReader.acquireLatestImage()
                    if (image != null && !continuation.isCancelled) {
                        readyReader.setOnImageAvailableListener(null, null)
                        continuation.resume(image)
                    }
                }
                continuation.invokeOnCancellation {
                    reader.setOnImageAvailableListener(null, null)
                }
                reader.setOnImageAvailableListener(listener, handler)
            }
        }.also {
            if (it == null) {
                AppLogger.logError("ScreenCaptureManager", "Timed out waiting for virtual display image.")
            }
        }
    }

    /**
     * Converts the captured [Image] into a bitmap while handling potential padding.
     *
     * Inputs: Target [width] and [height] for the final bitmap.
     * Outputs: Bitmap representing the display contents.
     */
    private fun Image.toBitmap(width: Int, height: Int): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val paddedBitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        paddedBitmap.copyPixelsFromBuffer(buffer)
        val finalBitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
        paddedBitmap.recycle()
        return finalBitmap
    }

    /**
     * Releases the virtual display while handling API level differences.
     *
     * Inputs: None.
     * Outputs: Virtual display resources freed.
     */
    private fun VirtualDisplay.safeRelease() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.release()
        } else {
            try {
                this.release()
            } catch (throwable: Throwable) {
                AppLogger.logError("ScreenCaptureManager", "Virtual display release failed on legacy device.", throwable)
            }
        }
    }

    companion object {
        @VisibleForTesting
        internal const val IMAGE_READER_CAPACITY = 2
        private const val CAPTURE_TIMEOUT_MS = 4000L
    }
}

/**
 * Registers a projection callback that ensures resources are cleaned up when the projection stops.
 *
 * Inputs: [projection] - MediaProjection to observe, [handler] - Callback thread, [imageReader] - Capture surface.
 * Outputs: Registered [MediaProjection.Callback] instance.
 */
@VisibleForTesting
internal fun ScreenCaptureManager.registerProjectionCallback(
    projection: MediaProjection,
    handler: Handler,
    imageReader: ImageReader
): MediaProjection.Callback {
    val callback = object : MediaProjection.Callback() {
        override fun onStop() {
            AppLogger.logError(
                "ScreenCaptureManager",
                "MediaProjection stopped before capture completed; clearing image listener."
            )
            imageReader.setOnImageAvailableListener(null, null)
        }
    }
    projection.registerCallback(callback, handler)
    AppLogger.logInfo("ScreenCaptureManager", "MediaProjection callback registered for capture session.")
    return callback
}

/**
 * Executes the supplied block and closes the [Image] afterward.
 *
 * Inputs: [block] - Function to run with the open image.
 * Outputs: Result of [block] while ensuring the image is closed.
 */
private inline fun <T> Image.use(block: (Image) -> T): T {
    return try {
        block(this)
    } finally {
        close()
    }
}
