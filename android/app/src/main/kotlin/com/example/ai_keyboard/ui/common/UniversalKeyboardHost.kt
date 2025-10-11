package com.example.ai_keyboard.ui.common

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ai_keyboard.utils.KeyboardHeights
import com.example.ai_keyboard.utils.totalKeyboardHeightPx

/**
 * A unified container for all keyboard and feature panels.
 * Ensures consistent height, background, and insets across all modes.
 * 
 * This container maintains a single, consistent height for:
 * - Letter keyboard
 * - Number keyboard  
 * - Symbol keyboard
 * - Emoji panel
 * - AI panels (Grammar, Tone, Assistant)
 * - Feature panels (Clipboard, Settings)
 * 
 * Key features:
 * - Prevents layout jumps when switching panels
 * - Handles navigation bar insets consistently
 * - Maintains proper background across all panels
 * - No double inset padding issues
 */
class UniversalKeyboardHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {
        private const val TAG = "UniversalKeyboardHost"
    }

    var withToolbar = false
        set(value) {
            field = value
            applyConsistentHeight()
        }
        
    var withSuggestions = false
        set(value) {
            field = value
            applyConsistentHeight()
        }

    private var targetHeight: Int = 0

    init {
        // Prevent automatic inset handling - we'll manage it manually
        fitsSystemWindows = false
        clipToPadding = false
        clipChildren = false
        
        // Set up window insets listener to prevent navigation bar interference
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            // Don't apply any padding - the keyboard service handles positioning
            // This prevents the white space at the bottom
            Log.d(TAG, "Window insets received but not applying padding to prevent white space")
            insets
        }
        
        // Apply initial height
        applyConsistentHeight()
    }

    /**
     * Applies consistent height across all panels
     * This ensures no layout jumps when switching between panels
     */
    fun applyConsistentHeight() {
        targetHeight = context.totalKeyboardHeightPx(withToolbar, withSuggestions)
        
        // Set both layout params and minimum height to ensure consistency
        layoutParams = layoutParams?.apply { 
            height = targetHeight 
        } ?: LayoutParams(LayoutParams.MATCH_PARENT, targetHeight)
        
        minimumHeight = targetHeight
        
        Log.d(TAG, "Applied consistent height: ${targetHeight}px (toolbar: $withToolbar, suggestions: $withSuggestions)")
        
        requestLayout()
    }

    /**
     * Switches content within the host while maintaining consistent height
     * @param newContent The new panel or keyboard view to display
     */
    fun switchContent(newContent: android.view.View) {
        removeAllViews()
        
        // Force the new content to fill the entire allocated height
        newContent.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT  // Changed from WRAP_CONTENT to MATCH_PARENT
        )
        
        addView(newContent)
        
        Log.d(TAG, "Switched content to: ${newContent.javaClass.simpleName}")
    }

    /**
     * Gets the current target height
     */
    fun getTargetHeight(): Int = targetHeight

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Force our target height regardless of measure spec
        val heightSpec = MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}
