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

import android.content.Context
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

/**
 * Validates helper behaviour for [ScreenCaptureManager].
 *
 * Inputs: Mocked [MediaProjection] and [ImageReader] instances.
 * Outputs: Ensures callbacks register correctly and clean up resources.
 */
@RunWith(RobolectricTestRunner::class)
class ScreenCaptureManagerTest {

    private lateinit var context: Context
    private lateinit var handlerThread: HandlerThread

    /**
     * Spins up a handler thread used by the projection callback.
     *
     * Inputs: None.
     * Outputs: Ready [HandlerThread] and application context.
     */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        handlerThread = HandlerThread("test-projection-callback").apply { start() }
    }

    /**
     * Ensures background thread resources are released between test runs.
     *
     * Inputs: None.
     * Outputs: Handler thread terminated.
     */
    @After
    fun tearDown() {
        handlerThread.quitSafely()
        handlerThread.join()
    }

    /**
     * Confirms that registering the projection callback wires listener cleanup into onStop.
     *
     * Inputs: None.
     * Outputs: Verification of register and callback behaviour.
     */
    @Test
    fun registerProjectionCallback_registersAndCleansImageReader() {
        val manager = ScreenCaptureManager(context)
        val projection = mock(MediaProjection::class.java)
        val reader = mock(ImageReader::class.java)
        val handler = Handler(handlerThread.looper)

        val callback = manager.registerProjectionCallback(projection, handler, reader)

        verify(projection).registerCallback(eq(callback), eq(handler))

        callback.onStop()

        verify(reader).setOnImageAvailableListener(null, null)
    }
}
