package com.example.screenshotapp.ui.capture

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider

/**
 * Validates intent helpers exposed by [ScreenshotCaptureService].
 *
 * Inputs: Boolean flags controlling toast visibility.
 * Outputs: Assertions over intent extras.
 */
@RunWith(RobolectricTestRunner::class)
class ScreenshotCaptureServiceIntentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * Ensures the helper defaults to silent capture to avoid overlay toasts.
     *
     * Inputs: None.
     * Outputs: Assertion that the extra resolves to false.
     */
    @Test
    fun createIntent_defaultsToSilentCapture() {
        val intent = ScreenshotCaptureService.createIntent(context)

        assertFalse(intent.getBooleanExtra(ScreenshotCaptureService.EXTRA_SHOW_CAPTURING_TOAST, true))
    }

    /**
     * Ensures callers can opt into showing the capturing toast via helper.
     *
     * Inputs: Explicit true flag.
     * Outputs: Assertion confirming the extra resolves to true.
     */
    @Test
    fun createIntent_allowsOptInForToast() {
        val intent = ScreenshotCaptureService.createIntent(context, showCapturingToast = true)

        assertTrue(intent.getBooleanExtra(ScreenshotCaptureService.EXTRA_SHOW_CAPTURING_TOAST, false))
    }
}
