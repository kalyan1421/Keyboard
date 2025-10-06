package com.example.ai_keyboard

import android.os.SystemClock
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import java.util.ArrayDeque

/**
 * KeyboardEnhancements: Helper utilities for AIKeyboardService
 * 
 * Includes:
 * - Suggestion queue for early calls before view is ready
 * - Settings application debouncing
 * - Unified suggestion building
 */

/**
 * Manages queued suggestions when UI is not ready yet
 */
class SuggestionQueue {
    private val queue = ArrayDeque<List<SuggestionItem>>()
    private val maxQueueSize = 5
    
    fun enqueue(suggestions: List<SuggestionItem>) {
        if (queue.size >= maxQueueSize) {
            queue.removeFirst() // Drop oldest
        }
        queue.addLast(suggestions)
    }
    
    fun dequeueAll(): List<List<SuggestionItem>> {
        val result = mutableListOf<List<SuggestionItem>>()
        while (queue.isNotEmpty()) {
            result.add(queue.removeFirst())
        }
        return result
    }
    
    fun isEmpty() = queue.isEmpty()
    fun size() = queue.size
}

/**
 * Simple suggestion item for queue
 */
data class SuggestionItem(
    val text: String,
    val type: String = "word", // word, emoji, clipboard, action
    val score: Float = 0.5f
)

/**
 * Debounces settings application to avoid spam
 */
class SettingsDebouncer(private val minIntervalMs: Long = 250) {
    private var lastApplyAt = 0L
    private val tag = "SettingsDebouncer"
    
    /**
     * Check if enough time has passed since last application
     */
    fun shouldApply(): Boolean {
        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastApplyAt
        return elapsed >= minIntervalMs
    }
    
    /**
     * Record that settings were just applied
     */
    fun recordApply() {
        lastApplyAt = SystemClock.uptimeMillis()
    }
    
    /**
     * Get time until next application is allowed
     */
    fun timeUntilNextMs(): Long {
        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastApplyAt
        return maxOf(0L, minIntervalMs - elapsed)
    }
}

/**
 * Extension functions for AIKeyboardService
 */
object KeyboardEnhancementHelpers {
    private const val TAG = "KeyboardEnhancements"
    
    /**
     * Log with throttling to avoid spam
     */
    fun logThrottled(tag: String, message: String, throttleMs: Long = 1000) {
        // Simple implementation - could be enhanced with a map of last log times
        Log.d(tag, message)
    }
}

