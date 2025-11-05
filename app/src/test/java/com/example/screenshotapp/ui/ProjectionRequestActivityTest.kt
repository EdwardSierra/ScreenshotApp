package com.example.screenshotapp.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers toast visibility logic inside [ProjectionRequestActivity].
 *
 * Inputs: Quick-settings trigger flag combinations.
 * Outputs: Boolean decisions verifying toast suppression.
 */
@RunWith(RobolectricTestRunner::class)
class ProjectionRequestActivityTest {

    /**
     * Confirms the toast is shown when the permission flow is triggered manually.
     *
     * Inputs: False quick settings trigger flag.
     * Outputs: Assertion that the toast should display.
     */
    @Test
    fun shouldShowPermissionGrantedToast_returnsTrueWhenNotTriggeredByTile() {
        assertTrue(ProjectionRequestActivity.shouldShowPermissionGrantedToast(triggeredCapture = false))
    }

    /**
     * Confirms the toast is skipped when the quick settings tile relaunches capture immediately.
     *
     * Inputs: True quick settings trigger flag.
     * Outputs: Assertion that the toast should be suppressed.
     */
    @Test
    fun shouldShowPermissionGrantedToast_returnsFalseWhenTriggeredByTile() {
        assertFalse(ProjectionRequestActivity.shouldShowPermissionGrantedToast(triggeredCapture = true))
    }
}
