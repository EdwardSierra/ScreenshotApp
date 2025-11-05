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

package com.example.screenshotapp.logging

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Provides file-backed logging utilities for the screenshot application.
 *
 * Inputs: Context during initialization, log statements during runtime.
 * Outputs: Log messages appended to a persistent log file.
 */
object AppLogger {

    private const val LOG_FOLDER_NAME = "logs"
    private const val LOG_FILE_NAME = "screenshot_app.log"
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()
    private var logFile: File? = null

    /**
     * Initializes the logger by creating the log directory and file.
     *
     * Inputs: [context] - Application context used to resolve the log directory.
     * Outputs: Ready-to-use logger with an open file path.
     */
    fun initialize(context: Context) {
        val directory = File(context.getExternalFilesDir(null), LOG_FOLDER_NAME)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val outputFile = File(directory, LOG_FILE_NAME)
        if (!outputFile.exists()) {
            outputFile.createNewFile()
        }
        logFile = outputFile
        logInfo("AppLogger", "Logger initialized at ${outputFile.absolutePath}")
    }

    /**
     * Writes an informational message into the persistent log.
     *
     * Inputs: [tag] - Component identifier, [message] - Details to log.
     * Outputs: Text line appended to the log file.
     */
    fun logInfo(tag: String, message: String) {
        writeLog("INFO", tag, message, null)
    }

    /**
     * Writes an error message into the persistent log.
     *
     * Inputs: [tag] - Component identifier, [message] - Details to log, [throwable] - Optional exception.
     * Outputs: Text line appended to the log file.
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        writeLog("ERROR", tag, message, throwable)
    }

    /**
     * Resolves the log file path for diagnostics.
     *
     * Inputs: None.
     * Outputs: [File] representing the log location, or null if uninitialized.
     */
    fun getLogFile(): File? = logFile

    /**
     * Appends the supplied log entry into the file in a thread-safe manner.
     *
     * Inputs: [level] - Severity string, [tag] - Component identifier, [message] - Log detail, [throwable] - Optional error.
     * Outputs: Text line written to the log file.
     */
    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable?) {
        val logFileLocal = logFile ?: return
        val timestamp = dateFormatter.format(Date())
        val builder = StringBuilder()
        builder.append(timestamp)
            .append(" ")
            .append(level)
            .append("/")
            .append(tag)
            .append(": ")
            .append(message)
        throwable?.let {
            builder.append(" | exception=").append(it::class.java.simpleName).append(": ").append(it.message)
        }
        val line = builder.toString()
        synchronized(lock) {
            try {
                FileWriter(logFileLocal, true).use { writer ->
                    writer.appendLine(line)
                }
            } catch (ioException: IOException) {
                // Last resort: print stack trace to stderr since logging failed.
                ioException.printStackTrace()
            }
        }
    }
}
