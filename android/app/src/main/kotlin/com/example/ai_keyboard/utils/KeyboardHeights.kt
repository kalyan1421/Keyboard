package com.example.ai_keyboard.utils

import android.content.Context
import android.content.res.Configuration
import android.util.Log

/**
 * Centralized keyboard height calculations
 * Provides consistent heights across all keyboard modes and panels
 */
object KeyboardHeights {
    private const val TAG = "KeyboardHeights"
    
    // Base keyboard height ratios - MINIMIZED to eliminate bottom space
    private const val PORTRAIT_HEIGHT_RATIO = 0.24f  // 24% of screen height in portrait (reduced from 27%)
    private const val LANDSCAPE_HEIGHT_RATIO = 0.42f // 42% of screen height in landscape (reduced from 45%)
    
    // Height constraints in dp - MINIMIZED to fit keys exactly
    private const val MIN_KEYBOARD_HEIGHT_DP = 260  // Reduced from 280
    private const val MAX_KEYBOARD_HEIGHT_DP = 310  // Reduced from 340
    
    // Component heights in dp
    private const val TOOLBAR_HEIGHT_DP = 72  // Increased from 64 for better visibility
    private const val SUGGESTIONS_HEIGHT_DP = 44
    private const val NUMBER_ROW_HEIGHT_DP = 72  // Additional height for number row (increased from 60)
    
    /**
     * Calculates the base keyboard height in pixels
     * This is the height of just the keyboard keys, without toolbar or suggestions
     * @param withNumberRow If true, adds extra height for the number row
     */
    fun baseHeightPx(context: Context, withNumberRow: Boolean = false): Int {
        val metrics = context.resources.displayMetrics
        val screenHeight = metrics.heightPixels
        
        // Determine ratio based on orientation
        val ratio = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LANDSCAPE_HEIGHT_RATIO
        } else {
            PORTRAIT_HEIGHT_RATIO
        }
        
        // Calculate height as percentage of screen
        val calculatedHeight = (screenHeight * ratio).toInt()
        
        // Convert constraints to pixels
        val minHeightPx = (MIN_KEYBOARD_HEIGHT_DP * metrics.density).toInt()
        val maxHeightPx = (MAX_KEYBOARD_HEIGHT_DP * metrics.density).toInt()
        
        // Clamp to min/max bounds
        var finalHeight = calculatedHeight.coerceIn(minHeightPx, maxHeightPx)
        
        // Add number row height if enabled (grows upward)
        if (withNumberRow) {
            val numberRowHeightPx = (NUMBER_ROW_HEIGHT_DP * metrics.density).toInt()
            finalHeight += numberRowHeightPx
        }
        
        return finalHeight
    }
    
    /**
     * Gets the toolbar height in pixels
     */
    fun toolbarPx(context: Context): Int {
        return (TOOLBAR_HEIGHT_DP * context.resources.displayMetrics.density).toInt()
    }
    
    /**
     * Gets the suggestions bar height in pixels
     */
    fun suggestionsPx(context: Context): Int {
        return (SUGGESTIONS_HEIGHT_DP * context.resources.displayMetrics.density).toInt()
    }
    
    /**
     * Calculates total keyboard height including optional components
     * @param withNumberRow If true, increases height for number row (grows upward)
     */
    fun totalHeight(
        context: Context,
        includeToolbar: Boolean = true,
        includeSuggestions: Boolean = true,
        withNumberRow: Boolean = false
    ): Int {
        var height = baseHeightPx(context, withNumberRow)
        
        if (includeToolbar) {
            height += toolbarPx(context)
        }
        
        if (includeSuggestions) {
            height += suggestionsPx(context)
        }
        
        
        return height
    }
}

/**
 * Extension function for easy access to total keyboard height
 * @param withNumberRow If true, adds extra height for number row (grows upward)
 */
fun Context.totalKeyboardHeightPx(
    withToolbar: Boolean = true,
    withSuggestions: Boolean = true,
    withNumberRow: Boolean = false
): Int {
    return KeyboardHeights.totalHeight(this, withToolbar, withSuggestions, withNumberRow)
}

/**
 * Extension function to get just the base keyboard height
 * @param withNumberRow If true, adds extra height for number row
 */
fun Context.baseKeyboardHeightPx(withNumberRow: Boolean = false): Int {
    return KeyboardHeights.baseHeightPx(this, withNumberRow)
}

/**
 * Extension function to convert dp to pixels
 */
fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}
