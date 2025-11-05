/*
 * ScreenshotApp - Android screenshot utility
 * Copyright (C) 2025 Edward Sierra
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.example.screenshotapp.capture

import android.content.Intent
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Covers the stateful behaviour of [ProjectionPermissionRepository].
 *
 * Inputs: Fake permission results.
 * Outputs: Verification of the just-granted delay flag.
 */
@RunWith(RobolectricTestRunner::class)
class ProjectionPermissionRepositoryTest {

    /**
     * Ensures the delay flag is delivered once after storing permission data.
     *
     * Inputs: None.
     * Outputs: Assertions checking the consumption semantics.
     */
    @Test
    fun consumeJustGrantedFlag_onlyTrueImmediatelyAfterStore() {
        ProjectionPermissionRepository.store(1, Intent("test"))

        assertTrue(ProjectionPermissionRepository.consumeJustGrantedFlag())
        assertFalse(ProjectionPermissionRepository.consumeJustGrantedFlag())
    }

    /**
     * Validates that clearing the repository resets the pending delay flag.
     *
     * Inputs: None.
     * Outputs: Assertion confirming the flag is false after a clear.
     */
    @Test
    fun clear_resetsPendingDelayFlag() {
        ProjectionPermissionRepository.store(1, Intent("test"))
        ProjectionPermissionRepository.clear()

        assertFalse(ProjectionPermissionRepository.consumeJustGrantedFlag())
    }

    /**
     * Cleans repository state between tests.
     *
     * Inputs: None.
     * Outputs: Permission cache cleared.
     */
    @After
    fun tearDown() {
        ProjectionPermissionRepository.clear()
    }
}
