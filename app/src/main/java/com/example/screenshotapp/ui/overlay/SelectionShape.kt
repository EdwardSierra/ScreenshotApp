package com.example.screenshotapp.ui.overlay

import android.graphics.PointF
import android.graphics.RectF

/**
 * Represents shape selections drawn by the user for cropping screenshots.
 *
 * Inputs: User touch interactions.
 * Outputs: Immutable selection geometries used during bitmap cropping.
 */
sealed class SelectionShape {

    /**
     * Encapsulates rectangular selections using floating-point bounds.
     *
     * Inputs: [bounds] - Rectangle defining the selected area.
     * Outputs: Rectangle data for cropping logic.
     */
    data class Rectangle(val bounds: RectF) : SelectionShape()

    /**
     * Encapsulates circular selections defined by a center point and radius.
     *
     * Inputs: [center] - The circle center, [radius] - Distance from center to any edge.
     * Outputs: Circle data for cropping logic.
     */
    data class Circle(val center: PointF, val radius: Float) : SelectionShape()
}
