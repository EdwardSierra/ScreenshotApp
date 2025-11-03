package com.example.screenshotapp.ui.overlay

/**
 * Enumerates the supported shape selection modes for screenshot cropping.
 *
 * Inputs: User shape selection preference.
 * Outputs: Mode used to interpret touch gestures.
 */
enum class SelectionMode {
    /**
     * Uses a rectangular bounding box for cropping.
     *
     * Inputs: None.
     * Outputs: Rectangular selection behavior.
     */
    RECTANGLE,

    /**
     * Uses a circular bounding shape for cropping.
     *
     * Inputs: None.
     * Outputs: Circular selection behavior.
     */
    CIRCLE
}
