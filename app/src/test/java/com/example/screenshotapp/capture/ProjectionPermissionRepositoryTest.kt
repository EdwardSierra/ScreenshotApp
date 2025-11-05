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
