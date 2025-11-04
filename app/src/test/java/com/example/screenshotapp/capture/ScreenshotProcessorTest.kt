package com.example.screenshotapp.capture

import android.graphics.Bitmap
import android.graphics.Rect
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
        val selection = Rect(20, 30, 120, 130)

        val result = processor.crop(source, selection)

        assertNotNull(result)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }
}
