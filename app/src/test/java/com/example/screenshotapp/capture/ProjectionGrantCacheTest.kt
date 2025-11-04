package com.example.screenshotapp.capture

import android.app.Activity
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the caching semantics implemented by [ProjectionGrantCache].
 *
 * Inputs: Synthetic projection consent codes and intents.
 * Outputs: Assertions that confirm defensive copies and availability tracking.
 */
@RunWith(RobolectricTestRunner::class)
class ProjectionGrantCacheTest {

    /**
     * Ensures cached code and intent can be retrieved after storing a consent grant.
     *
     * Inputs: None.
     * Outputs: Assertions validating cache availability and data integrity.
     */
    @Test
    fun storeAndRetrieveReturnsStoredValues() {
        val cache = ProjectionGrantCache()
        val original = Intent().apply { putExtra("token", "alpha") }

        cache.store(Activity.RESULT_OK, original)

        assertTrue(cache.isAvailable())
        assertEquals(Activity.RESULT_OK, cache.code())
        val cached = cache.data()
        assertNotNull(cached)
        assertEquals("alpha", cached?.getStringExtra("token"))
    }

    /**
     * Ensures the cache returns defensive copies that are insensitive to external mutations.
     *
     * Inputs: None.
     * Outputs: Assertions confirming cached state isolation from callers.
     */
    @Test
    fun retrievingIntentProvidesDefensiveCopy() {
        val cache = ProjectionGrantCache()
        val original = Intent().apply { putExtra("value", "initial") }

        cache.store(777, original)
        original.putExtra("value", "mutated")

        val firstCopy = cache.data()
        val secondCopy = cache.data()

        assertEquals("initial", firstCopy?.getStringExtra("value"))
        firstCopy?.putExtra("value", "overridden")
        assertEquals("initial", secondCopy?.getStringExtra("value"))
    }
}
