package com.example.ai_keyboard.utils

import android.util.Log
import com.example.ai_keyboard.BuildConfig

/**
 * Centralized logging utility for AI Keyboard
 * - Debug logs are only enabled in DEBUG builds
 * - Error logs are always enabled
 * - Provides consistent logging across the application
 */
object LogUtil {
    private const val ENABLED = BuildConfig.DEBUG
    
    /**
     * Log debug message (only in DEBUG builds)
     * @param tag Log tag (usually class name)
     * @param message Message to log
     */
    fun d(tag: String, message: String) {
        if (ENABLED) Log.d(tag, message)
    }
    
    /**
     * Log error message (always enabled)
     * @param tag Log tag (usually class name)
     * @param message Error message
     * @param tr Optional throwable to log
     */
    fun e(tag: String, message: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.e(tag, message, tr)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log warning message (only in DEBUG builds)
     * @param tag Log tag (usually class name)
     * @param message Warning message
     */
    fun w(tag: String, message: String) {
        if (ENABLED) Log.w(tag, message)
    }
    
    /**
     * Log info message (only in DEBUG builds)
     * @param tag Log tag (usually class name)
     * @param message Info message
     */
    fun i(tag: String, message: String) {
        if (ENABLED) Log.i(tag, message)
    }
}

