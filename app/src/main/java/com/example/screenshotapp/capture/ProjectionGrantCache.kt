package com.example.screenshotapp.capture

import android.content.Intent
import com.example.screenshotapp.logging.AppLogger

/**
 * Preserves the last granted media projection consent for reuse within the service lifecycle.
 *
 * Inputs: Media projection result code and intent delivered by the system permission dialog.
 * Outputs: Cloned grant payload ready for subsequent projection reinitialization attempts.
 */
class ProjectionGrantCache {

    private var resultCode: Int? = null
    private var resultData: Intent? = null

    /**
     * Stores the projection consent details with defensive copies.
     *
     * Inputs: [code] - Result code from the projection consent, [data] - Result data intent.
     * Outputs: Cache populated with clones safe for future reuse.
     */
    fun store(code: Int, data: Intent) {
        resultCode = code
        resultData = Intent(data)
        AppLogger.logInfo("ProjectionGrantCache", "Projection consent cached.")
    }

    /**
     * Returns a cloned intent of the cached projection data when available.
     *
     * Inputs: None.
     * Outputs: Copy of the stored intent or null when cache is empty.
     */
    fun data(): Intent? = resultData?.let { Intent(it) }

    /**
     * Retrieves the cached projection result code.
     *
     * Inputs: None.
     * Outputs: Stored result code or null when unavailable.
     */
    fun code(): Int? = resultCode

    /**
     * Clears the cached projection consent data.
     *
     * Inputs: None.
     * Outputs: Cache reset to empty state.
     */
    fun clear() {
        resultCode = null
        resultData = null
        AppLogger.logInfo("ProjectionGrantCache", "Projection consent cache cleared.")
    }

    /**
     * Indicates whether the cache currently holds reusable projection consent information.
     *
     * Inputs: None.
     * Outputs: True when both code and data are populated.
     */
    fun isAvailable(): Boolean = resultCode != null && resultData != null
}

