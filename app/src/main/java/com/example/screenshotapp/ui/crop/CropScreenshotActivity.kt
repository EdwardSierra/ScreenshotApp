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

package com.example.screenshotapp.ui.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.screenshotapp.R
import com.example.screenshotapp.capture.ScreenshotCache
import com.example.screenshotapp.capture.ScreenshotProcessor
import com.example.screenshotapp.capture.ScreenshotStorage
import com.example.screenshotapp.databinding.ActivityCropScreenshotBinding
import com.example.screenshotapp.logging.AppLogger
import com.example.screenshotapp.util.ClipboardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Displays the captured screenshot and handles cropping before copying to the clipboard.
 *
 * Inputs: Cache file path supplied as an intent extra.
 * Outputs: Cropped screenshot persisted and copied for sharing.
 */
class CropScreenshotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropScreenshotBinding
    private lateinit var screenshotStorage: ScreenshotStorage
    private lateinit var screenshotCache: ScreenshotCache
    private val screenshotProcessor = ScreenshotProcessor()

    private var cachedScreenshotPath: String? = null
    private var screenshotBitmap: Bitmap? = null
    private var cropInProgress = false

    /**
     * Inflates the UI, loads the cached screenshot, and prepares listeners.
     *
     * Inputs: [savedInstanceState] - Previously saved state.
     * Outputs: Ready-to-use cropping activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropScreenshotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        screenshotStorage = ScreenshotStorage(this)
        screenshotCache = ScreenshotCache(this)

        binding.instructionsText.text = getString(R.string.crop_instructions)
        binding.cancelButton.setOnClickListener {
            AppLogger.logInfo("CropScreenshotActivity", "Cropping cancelled by user.")
            finish()
        }

        binding.cropView.onSelectionFinished = { rect ->
            handleSelection(rect)
        }
        binding.cropView.onSelectionInvalid = {
            Toast.makeText(this, getString(R.string.selection_too_small), Toast.LENGTH_SHORT).show()
        }

        cachedScreenshotPath = intent.getStringExtra(EXTRA_SCREENSHOT_PATH)
        if (cachedScreenshotPath.isNullOrEmpty()) {
            AppLogger.logError("CropScreenshotActivity", "Missing screenshot cache path.")
            Toast.makeText(this, getString(R.string.capture_failure), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadScreenshot(cachedScreenshotPath!!)
    }

    /**
     * Recycles bitmaps and deletes cache files on teardown.
     *
     * Inputs: None.
     * Outputs: Released heap and storage resources.
     */
    override fun onDestroy() {
        screenshotBitmap?.recycle()
        screenshotBitmap = null
        screenshotCache.delete(cachedScreenshotPath)
        super.onDestroy()
    }

    /**
     * Loads the cached screenshot from disk and feeds it into the crop view.
     *
     * Inputs: [path] - Absolute file path containing the screenshot PNG.
     * Outputs: Bitmap rendered in the crop view or activity termination on failure.
     */
    private fun loadScreenshot(path: String) {
        val file = File(path)
        if (!file.exists()) {
            AppLogger.logError("CropScreenshotActivity", "Screenshot cache file missing at $path")
            Toast.makeText(this, getString(R.string.capture_failure), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val bitmap = BitmapFactory.decodeFile(path)
        if (bitmap == null) {
            AppLogger.logError("CropScreenshotActivity", "Failed to decode screenshot at $path")
            Toast.makeText(this, getString(R.string.capture_failure), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        screenshotBitmap = bitmap
        binding.cropView.setScreenshot(bitmap)
        AppLogger.logInfo("CropScreenshotActivity", "Screenshot loaded for cropping.")
    }

    /**
     * Handles the rectangle selected by the user by cropping, saving, and copying to clipboard.
     *
     * Inputs: [selection] - Rectangle in bitmap coordinates.
     * Outputs: Cropped screenshot saved, clipboard updated, activity finishes.
     */
    private fun handleSelection(selection: Rect) {
        if (cropInProgress) {
            return
        }
        val sourceBitmap = screenshotBitmap ?: return
        cropInProgress = true
        lifecycleScope.launch {
            try {
                val cropped = withContext(Dispatchers.Default) {
                    screenshotProcessor.crop(sourceBitmap, selection)
                }
                val uri = withContext(Dispatchers.IO) {
                    screenshotStorage.saveBitmap(cropped)
                }
                withContext(Dispatchers.Default) {
                    cropped.recycle()
                }
                ClipboardHelper.copyImageToClipboard(applicationContext, uri)
                Toast.makeText(this@CropScreenshotActivity, getString(R.string.capture_success), Toast.LENGTH_SHORT).show()
                AppLogger.logInfo("CropScreenshotActivity", "Cropped screenshot saved and copied to clipboard: $uri")
            } catch (exception: Exception) {
                AppLogger.logError("CropScreenshotActivity", "Cropping failed.", exception)
                Toast.makeText(this@CropScreenshotActivity, getString(R.string.capture_failure), Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_SCREENSHOT_PATH = "extra_screenshot_path"
    }
}
