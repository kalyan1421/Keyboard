package com.example.ai_keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Manages keyboard height calculations and navigation bar detection
 * Provides consistent keyboard height across all panels (letters, symbols, emojis, grammar)
 * Handles system UI insets for Android 11+ and fallback for older versions
 */
class KeyboardHeightManager(private val context: Context) {
    
    companion object {
        private const val TAG = "KeyboardHeightManager"
        
        // Keyboard height constants (in dp)
        private const val MIN_KEYBOARD_HEIGHT_DP = 260  // Reduced from 280 (minimized)
        private const val MAX_KEYBOARD_HEIGHT_DP = 310  // Reduced from 340 (minimized)
        private const val DEFAULT_KEYBOARD_HEIGHT_DP = 285  // Reduced from 310 (minimized)
        private const val KEYBOARD_HEIGHT_PERCENTAGE = 0.24f // 24% of screen height (minimized to eliminate space)
        
        // Toolbar and suggestion bar heights
        private const val TOOLBAR_HEIGHT_DP = 72  // Increased from 64 for better visibility
        private const val SUGGESTION_BAR_HEIGHT_DP = 44
    }
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    
    /**
     * Calculates the optimal keyboard height based on screen size and orientation
     * @param includeToolbar Whether to include toolbar height in calculation
     * @param includeSuggestions Whether to include suggestion bar height
     * @return Total keyboard height in pixels
     */
    fun calculateKeyboardHeight(
        includeToolbar: Boolean = true,
        includeSuggestions: Boolean = true
    ): Int {
        val screenHeight = getScreenHeight()
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        
        // Calculate base keyboard height as percentage of screen
        val baseHeightPercent = if (isLandscape) 0.5f else KEYBOARD_HEIGHT_PERCENTAGE
        val baseHeight = (screenHeight * baseHeightPercent).toInt()
        
        // Convert min/max to pixels
        val minHeight = dpToPx(MIN_KEYBOARD_HEIGHT_DP)
        val maxHeight = dpToPx(MAX_KEYBOARD_HEIGHT_DP)
        
        // Clamp keyboard height
        val keyboardHeight = baseHeight.coerceIn(minHeight, maxHeight)
        
        // Add toolbar and suggestion bar if needed
        var totalHeight = keyboardHeight
        if (includeToolbar) {
            totalHeight += dpToPx(TOOLBAR_HEIGHT_DP)
        }
        if (includeSuggestions) {
            totalHeight += dpToPx(SUGGESTION_BAR_HEIGHT_DP)
        }
        
        Log.d(TAG, "Calculated keyboard height: $totalHeight px (base: $keyboardHeight, " +
                "toolbar: $includeToolbar, suggestions: $includeSuggestions)")
        
        return totalHeight
    }
    
    /**
     * Gets the navigation bar height if present
     * @return Navigation bar height in pixels, 0 if not present
     */
    fun getNavigationBarHeight(): Int {
        // First check if navigation bar is present
        if (!hasNavigationBar()) {
            return 0
        }
        
        // Try to get from resources
        val resourceId = context.resources.getIdentifier(
            "navigation_bar_height", "dimen", "android"
        )
        
        if (resourceId > 0) {
            val navBarHeight = context.resources.getDimensionPixelSize(resourceId)
            Log.d(TAG, "Navigation bar height from resources: $navBarHeight px")
            return navBarHeight
        }
        
        // Fallback to default navigation bar height (48dp)
        return dpToPx(48)
    }
    
    /**
     * Checks if the device has a navigation bar
     * @return true if navigation bar is present
     */
    fun hasNavigationBar(): Boolean {
        // Check for physical navigation keys
        val hasMenuKey = android.view.ViewConfiguration.get(context).hasPermanentMenuKey()
        val hasBackKey = android.view.KeyCharacterMap.deviceHasKey(android.view.KeyEvent.KEYCODE_BACK)
        
        if (hasMenuKey || hasBackKey) {
            // Physical keys present, no navigation bar
            return false
        }
        
        // Use Android 11+ insets API or fallback to display metrics
        return hasNavigationBarAndroid11()
    }
    
    /**
     * Applies system UI insets to a view (handles navigation bar and status bar)
     * @param view The view to apply insets to
     * @param applyBottom Whether to apply bottom insets (navigation bar)
     * @param applyTop Whether to apply top insets (status bar)
     */
    fun applySystemInsets(
        view: View,
        applyBottom: Boolean = true,
        applyTop: Boolean = false,
        onInsetsApplied: ((Int, Int) -> Unit)? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            val topInset = if (applyTop) systemBars.top else 0
            val bottomInset = if (applyBottom) {
                // Use navigation bar height only when IME is not visible
                val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                if (imeVisible) 0 else navBars.bottom
            } else {
                0
            }
            
            v.setPadding(
                v.paddingLeft,
                topInset,
                v.paddingRight,
                bottomInset
            )
            
            Log.d(TAG, "Applied insets - Top: $topInset, Bottom: $bottomInset, " +
                    "IME: ${ime.bottom}, NavBar: ${navBars.bottom}")
            
            onInsetsApplied?.invoke(topInset, bottomInset)
            
            insets
        }
    }
    
    /**
     * Adjusts a keyboard panel's height to account for navigation bar
     * @param panel The panel view to adjust
     * @param baseHeight The base height in pixels (without navigation bar)
     */
    fun adjustPanelForNavigationBar(panel: View, baseHeight: Int) {
        val navBarHeight = getNavigationBarHeight()
        val adjustedHeight = baseHeight - navBarHeight
        
        panel.layoutParams = panel.layoutParams?.apply {
            height = adjustedHeight
        } ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, adjustedHeight)
        
        // Add bottom padding equal to navigation bar height
        panel.setPadding(
            panel.paddingLeft,
            panel.paddingTop,
            panel.paddingRight,
            navBarHeight
        )
        
        // Ensure content doesn't get clipped
        if (panel is ViewGroup) {
            panel.clipToPadding = false
            panel.clipChildren = false
        }
        
        Log.d(TAG, "Adjusted panel height: $adjustedHeight px, nav bar padding: $navBarHeight px")
    }
    
    // Private helper methods
    
    private fun dpToPx(dp: Int): Int {
        return (dp * density).toInt()
    }
    
    private fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.height()
        } else {
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            size.y
        }
    }
    
    private fun hasNavigationBarAndroid11(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets
            val navBarInsets = insets.getInsets(WindowInsets.Type.navigationBars())
            navBarInsets.bottom > 0
        } else {
            // Fallback for pre-Android 11: Check display dimensions
            val display = windowManager.defaultDisplay
            val realSize = Point()
            val size = Point()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(realSize)
                display.getSize(size)
                
                // Navigation bar is present if real size differs from display size
                realSize.y > size.y || realSize.x > size.x
            } else {
                false
            }
        }
    }
}
