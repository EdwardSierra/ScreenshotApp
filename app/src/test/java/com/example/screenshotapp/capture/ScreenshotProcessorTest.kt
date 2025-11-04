package com.example.screenshotapp.capture

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.example.screenshotapp.ui.overlay.SelectionShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Validates the bitmap cropping logic implemented by [ScreenshotProcessor].
 *
 * Inputs: Synthetic bitmaps and selection shapes.
 * Outputs: Assertions verifying cropping dimensions.
 */
@RunWith(RobolectricTestRunner::class)
class ScreenshotProcessorTest {

    private val processor = ScreenshotProcessor()

    /**
     * Ensures rectangular cropping returns a bitmap with the requested dimensions.
     *
     * Inputs: None.
     * Outputs: Assertion success when width and height match expectations.
     */
    @Test
    fun cropRectangleProducesCorrectSize() {
        val source = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val selection = SelectionShape.Rectangle(RectF(20f, 30f, 120f, 130f))

        val result = processor.crop(source, selection)

        assertNotNull(result)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    /**
     * Ensures circular cropping returns a square bitmap representing the circle's bounding box.
     *
     * Inputs: None.
     * Outputs: Assertion success when diameter equals twice the radius.
     */
    @Test
    fun cropCircleProducesSquareBitmap() {
        val source = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val selection = SelectionShape.Circle(PointF(150f, 150f), 60f)

        val result = processor.crop(source, selection)

        assertNotNull(result)
        assertEquals(120, result.width)
        assertEquals(120, result.height)
    }
}
