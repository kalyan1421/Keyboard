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
    
    // Base keyboard height ratios
    private const val PORTRAIT_HEIGHT_RATIO = 0.30f  // 35% of screen height in portrait
    private const val LANDSCAPE_HEIGHT_RATIO = 0.5f  // 50% of screen height in landscape
    
    // Height constraints in dp
    private const val MIN_KEYBOARD_HEIGHT_DP = 320
    private const val MAX_KEYBOARD_HEIGHT_DP = 380
    
    // Component heights in dp
    private const val TOOLBAR_HEIGHT_DP = 64
    private const val SUGGESTIONS_HEIGHT_DP = 44
    
    /**
     * Calculates the base keyboard height in pixels
     * This is the height of just the keyboard keys, without toolbar or suggestions
     */
    fun baseHeightPx(context: Context): Int {
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
        val finalHeight = calculatedHeight.coerceIn(minHeightPx, maxHeightPx)
        
        Log.d(TAG, "Base height: ${finalHeight}px (screen: ${screenHeight}px, ratio: $ratio)")
        
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
     */
    fun totalHeight(
        context: Context,
        includeToolbar: Boolean = true,
        includeSuggestions: Boolean = true
    ): Int {
        var height = baseHeightPx(context)
        
        if (includeToolbar) {
            height += toolbarPx(context)
        }
        
        if (includeSuggestions) {
            height += suggestionsPx(context)
        }
        
        Log.d(TAG, "Total height: ${height}px (toolbar: $includeToolbar, suggestions: $includeSuggestions)")
        
        return height
    }
}

/**
 * Extension function for easy access to total keyboard height
 */
fun Context.totalKeyboardHeightPx(
    withToolbar: Boolean = true,
    withSuggestions: Boolean = true
): Int {
    return KeyboardHeights.totalHeight(this, withToolbar, withSuggestions)
}

/**
 * Extension function to get just the base keyboard height
 */
fun Context.baseKeyboardHeightPx(): Int {
    return KeyboardHeights.baseHeightPx(this)
}

/**
 * Extension function to convert dp to pixels
 */
fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}
