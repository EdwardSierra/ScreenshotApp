package com.example.screenshotapp.capture

import android.content.Intent
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the in-memory behaviour of [ProjectionPermissionRepository].
 *
 * Inputs: Synthetic intent data representing MediaProjection results.
 * Outputs: Assertions covering store and clear operations.
 */
@RunWith(RobolectricTestRunner::class)
class ProjectionPermissionRepositoryTest {

    @After
    fun tearDown() {
        ProjectionPermissionRepository.clear()
    }

    /**
     * Ensures storing permission data flips the repository into the ready state.
     *
     * Inputs: None.
     * Outputs: Assertion verifying [ProjectionPermissionRepository.hasPermission].
     */
    @Test
    fun storeMarksPermissionPresent() {
        assertFalse(ProjectionPermissionRepository.hasPermission())
        ProjectionPermissionRepository.store(1, Intent())
        assertTrue(ProjectionPermissionRepository.hasPermission())
    }

    /**
     * Ensures clearing permission data removes the ready state flag.
     *
     * Inputs: None.
     * Outputs: Assertion verifying [ProjectionPermissionRepository.clear].
     */
    @Test
    fun clearResetsPermissionState() {
        ProjectionPermissionRepository.store(1, Intent())
        ProjectionPermissionRepository.clear()
        assertFalse(ProjectionPermissionRepository.hasPermission())
    }
}

