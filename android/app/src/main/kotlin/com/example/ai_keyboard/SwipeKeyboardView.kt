package com.example.ai_keyboard

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.*

class SwipeKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {
    
    companion object {
        private const val MIN_SWIPE_TIME = 300L // Minimum time for swipe (ms)
        private const val MIN_SWIPE_DISTANCE = 100f // Minimum distance for swipe (pixels)
        private const val SWIPE_START_THRESHOLD = 50f // Movement threshold to start swipe
        
        // Adaptive sizing constants
        private const val SCREEN_SIZE_SMALL = 720
        private const val SCREEN_SIZE_NORMAL = 1080
        private const val SCREEN_SIZE_LARGE = 1440
        
        // Spacebar gesture constants
        private const val SPACEBAR_GESTURE_THRESHOLD = 50f
        private const val CURSOR_CONTROL_SENSITIVITY = 15f
        private const val WORD_DELETE_VELOCITY_THRESHOLD = -500f
        private const val PERIOD_INSERT_VELOCITY_THRESHOLD = 500f
    }
    
    interface SwipeListener {
        fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String, keySequence: List<Int> = swipedKeys)
        fun onSwipeStarted()
        fun onSwipeEnded()
    }
    
    private var swipeEnabled = true
    private var isSwipeInProgress = false
    private val swipePoints = mutableListOf<FloatArray>()
    private val swipePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val swipePath = Path()
    private var swipeListener: SwipeListener? = null
    private var swipeStartTime = 0L
    
    // Theme manager integration
    private var themeManager: ThemeManager? = null
    
    // Theme-aware paint objects (will be created by ThemeManager)
    private var keyTextPaint: Paint? = null
    private var suggestionTextPaint: Paint? = null
    private var spaceLabelPaint: Paint? = null
    
    // Enhanced features
    private var isAdaptiveSizingEnabled = true
    private var isFloatingMode = false
    private var isSplitMode = false
    private var isSmallScreenOptimized = false
    private var spacebarGestureEnabled = true
    
    // Adaptive sizing
    private var adaptiveKeyWidth = 0f
    private var adaptiveKeyHeight = 0f
    private var touchTargetExpansion = 0f
    
    // Spacebar gesture detection
    private var isSpacebarPressed = false
    private var spacebarStartX = 0f
    private var spacebarStartY = 0f
    private var lastSpacebarX = 0f
    
    // Floating mode properties
    private var floatingX = 0f
    private var floatingY = 0f
    private var isDragging = false
    
    // Theme change listener removed - using static default colors
    
    // Enhanced special key state tracking
    private var isVoiceKeyActive = false
    private var isEmojiKeyActive = false
    private var specialKeyHighlightPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#1A73E8") // Default, will be updated by refreshTheme()
    }
    
    // Enhanced gesture recognition
    private var backspaceSlideStartX = 0f
    private var isSlideToDeleteActive = false
    private var spacebarSwipeStartX = 0f
    private var isCursorControlActive = false
    private var gestureStartTime = 0L
    private val SLIDE_THRESHOLD = 80f
    private val CURSOR_THRESHOLD = 30f
    
    // Special key detection and theming
    private fun isSpecialKey(code: Int): Boolean = when (code) {
        Keyboard.KEYCODE_SHIFT, Keyboard.KEYCODE_DELETE, Keyboard.KEYCODE_DONE,
        -10 /* ?123 */, -11 /* ABC */, -12 /* ?123 */, -13 /* Mic */, 
        -14 /* Globe */, -15 /* Emoji */, -16 /* Voice */, 
        10 /* Enter */, -4 /* Enter variant */ -> true
        else -> false
    }
    
    // Clipboard layout state
    private var isClipboardMode = false
    private var clipboardItems = listOf<ClipboardItem>()
    private var clipboardKeyRects = mutableListOf<RectF>()
    private var clipboardService: AIKeyboardService? = null
    
    init {
        // Initialize with ThemeManager V2 - no hardcoded colors
        initializeFromTheme()
    }
    
    /**
     * Set the theme manager V2 for dynamic theming
     */
    fun setThemeManager(manager: ThemeManager) {
        themeManager = manager
        initializeFromTheme()
        manager.addThemeChangeListener(object : ThemeManager.ThemeChangeListener {
            override fun onThemeChanged(theme: com.example.ai_keyboard.themes.KeyboardThemeV2, palette: com.example.ai_keyboard.themes.ThemePaletteV2) {
                refreshTheme()
            }
        })
    }
    
    /**
     * Refresh theme colors and invalidate view for live theme updates
     */
    fun refreshTheme() {
        initializeFromTheme()
        updateSwipePaint()
        
        // Explicitly update background color from current palette
        val manager = themeManager
        if (manager != null) {
            val palette = manager.getCurrentPalette()
            this.setBackgroundColor(palette.keyboardBg)
            android.util.Log.d("SwipeKeyboardView", "[AIKeyboard] Swipe background updated with current palette color: ${Integer.toHexString(palette.keyboardBg)}")
        }
        
        invalidateAllKeys()
        invalidate()
        requestLayout()
    }
    
    // ========================================
    // CleverType Configuration Methods
    // ========================================
    
    private var labelScaleMultiplier = 1.0f
    private var borderlessMode = false
    private var showLanguageOnSpace = true
    private var currentLanguageLabel = "English"
    private var previewEnabled = true
    private var keySpacingVerticalDp = 5
    private var keySpacingHorizontalDp = 2
    private var soundEnabled = true
    private var soundIntensityLevel = 1
    private var hapticIntensityLevel = 2
    private var longPressDelayMs = 200
    
    // One-handed mode state
    private var oneHandedModeEnabled = false
    private var oneHandedModeSide = "right"
    private var oneHandedModeWidthPct = 0.75f
    
    /**
     * Set the font scale multiplier for key labels
     */
    fun setLabelScale(multiplier: Float) {
        labelScaleMultiplier = multiplier.coerceIn(0.8f, 1.3f)
        android.util.Log.d("SwipeKeyboardView", "Label scale set to: $labelScaleMultiplier")
        // Recreate paint with new scale from theme manager
        themeManager?.let { manager ->
            keyTextPaint = manager.createKeyTextPaint()
            spaceLabelPaint = manager.createSpaceLabelPaint()
        }
        invalidate()
    }
    
    /**
     * Enable or disable borderless key mode
     */
    fun setBorderless(enabled: Boolean) {
        borderlessMode = enabled
        android.util.Log.d("SwipeKeyboardView", "Borderless mode set to: $enabled")
        // Borderless mode now removes padding in drawThemedKey
        invalidate()
        requestLayout()
    }
    
    /**
     * Show or hide language label on spacebar
     */
    fun setShowLanguageOnSpace(enabled: Boolean) {
        showLanguageOnSpace = enabled
        android.util.Log.d("SwipeKeyboardView", "Show language on space set to: $enabled")
        invalidate()
    }
    
    /**
     * Set the current language label to display on spacebar
     */
    fun setCurrentLanguage(languageLabel: String) {
        currentLanguageLabel = languageLabel
        invalidate()
    }
    
    /**
     * Enable or disable one-handed mode with Gboard-style behavior
     * @param enabled Whether one-handed mode is active
     * @param side "left" or "right" - which side to dock the keyboard
     * @param widthPct Percentage of screen width to use (0.6 - 0.9)
     */
    fun setOneHandedMode(enabled: Boolean, side: String = "right", widthPct: Float = 0.75f) {
        oneHandedModeEnabled = enabled
        oneHandedModeSide = side
        oneHandedModeWidthPct = widthPct.coerceIn(0.6f, 0.9f)
        
        android.util.Log.d("SwipeKeyboardView", 
            "One-handed mode set to: enabled=$enabled, side=$side, width=${(widthPct * 100).toInt()}%")
        
        // Apply layout changes
        if (enabled) {
            val screenWidth = context.resources.displayMetrics.widthPixels
            val targetWidth = (screenWidth * widthPct).toInt()
            
            // Use layout params to constrain width
            layoutParams = layoutParams?.apply {
                width = targetWidth
            } ?: android.view.ViewGroup.LayoutParams(targetWidth, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            
            // Calculate translation to shift keyboard to the correct side
            val translation = when (side) {
                "left" -> -((screenWidth - targetWidth) / 2f)
                "right" -> (screenWidth - targetWidth) / 2f
                else -> 0f
            }
            
            // Apply translation animation
            animate()
                .translationX(translation)
                .setDuration(200)
                .start()
            
            android.util.Log.d("SwipeKeyboardView", 
                "Applied one-handed: width=${targetWidth}px, translation=${translation}px")
        } else {
            // Reset to full width
            layoutParams = layoutParams?.apply {
                width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            } ?: android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            
            // Reset translation
            animate()
                .translationX(0f)
                .setDuration(200)
                .start()
            
            android.util.Log.d("SwipeKeyboardView", "Reset to full-width mode")
        }
        
        requestLayout()
        invalidate()
    }
    
    /**
     * Enable or disable key preview popups
     */
    override fun setPreviewEnabled(enabled: Boolean) {
        super.setPreviewEnabled(enabled)
        previewEnabled = enabled
    }
    
    /**
     * Set key spacing (vertical and horizontal)
     */
    fun setKeySpacing(verticalDp: Int, horizontalDp: Int) {
        keySpacingVerticalDp = verticalDp
        keySpacingHorizontalDp = horizontalDp
        android.util.Log.d("SwipeKeyboardView", "Key spacing set to: V=${verticalDp}dp, H=${horizontalDp}dp")
        // Key spacing now applied in drawThemedKey
        invalidate()
        requestLayout()
    }
    
    /**
     * Set long press delay in milliseconds
     */
    fun setLongPressDelay(delayMs: Int) {
        longPressDelayMs = delayMs
        // Long press delay will be used in touch event handling
    }
    
    /**
     * Enable/disable sound feedback with intensity level
     */
    fun setSoundEnabled(enabled: Boolean, intensityLevel: Int) {
        soundEnabled = enabled
        soundIntensityLevel = intensityLevel
        // Intensity: 0=off, 1=light, 2=medium, 3=strong
        // This would be used when playing click sounds
    }
    
    /**
     * Set haptic feedback intensity level
     */
    fun setHapticIntensity(intensityLevel: Int) {
        hapticIntensityLevel = intensityLevel
        // Intensity: 0=off, 1=light, 2=medium, 3=strong
        // This would be used when performing haptic feedback
        // Stored for use in onKey events
    }
    
    /**
     * Refresh suggestions strip (called after config changes)
     */
    fun refresh() {
        invalidate()
        requestLayout()
    }
    
    /**
     * Initialize all theme-dependent objects from ThemeManager V2
     */
    private fun initializeFromTheme() {
        val manager = themeManager
        if (manager == null) {
            // Fallback to default theme color (light gray)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            return
        }
        
        val palette = manager.getCurrentPalette()
        
        // Initialize cached paints from factory
        keyTextPaint = manager.createKeyTextPaint()
        suggestionTextPaint = manager.createSuggestionTextPaint() 
        spaceLabelPaint = manager.createSpaceLabelPaint()
        
        // Set background from theme - use solid color for consistency
        setBackgroundColor(palette.keyboardBg)
        
        // Update swipe trail paint
        updateSwipePaint()
    }
    
    /**
     * Update swipe trail paint with theme colors
     */
    private fun updateSwipePaint() {
        val manager = themeManager
        if (manager != null) {
            val palette = manager.getCurrentPalette()
            swipePaint.apply {
                color = palette.specialAccent
                strokeWidth = 8f * context.resources.displayMetrics.density
                alpha = (palette.rippleAlpha * 255 * 2).toInt() // Make swipe trail more visible
            }
        } else {
            // Fallback to default blue
            swipePaint.apply {
                color = Color.parseColor("#2196F3") // Default blue for swipe trail
                strokeWidth = 8f * context.resources.displayMetrics.density
                alpha = 180
            }
        }
    }
    
    
    fun setSwipeEnabled(enabled: Boolean) {
        swipeEnabled = enabled
    }
    
    fun setSwipeListener(listener: SwipeListener?) {
        swipeListener = listener
    }
    
    // Theme application methods removed - using single default theme only
    
    /**
     * Set voice key active state for visual feedback
     */
    fun setVoiceKeyActive(active: Boolean) {
        isVoiceKeyActive = active
        android.util.Log.d("SwipeKeyboardView", "Voice key active: $active")
        invalidate()
    }
    
    /**
     * Set emoji key active state for visual feedback
     */
    fun setEmojiKeyActive(active: Boolean) {
        isEmojiKeyActive = active
        android.util.Log.d("SwipeKeyboardView", "Emoji key active: $active")
        invalidate()
    }
    
    
    
    /**
     * Adjust color brightness for visual hierarchy
     */
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }
    
    override fun onDraw(canvas: Canvas) {
        if (isClipboardMode) {
            drawClipboardLayout(canvas)
        } else {
            try {
                // Draw custom themed keys
                keyboard?.let { kbd ->
                    val keys = kbd.keys
                    keys?.forEach { key ->
                        try {
                            drawThemedKey(canvas, key)
                        } catch (e: StringIndexOutOfBoundsException) {
                            // Skip keys that cause issues with empty labels
                        }
                    }
                }
                
                // Draw swipe trail if in progress
                if (isSwipeInProgress && swipePoints.isNotEmpty()) {
                    canvas.drawPath(swipePath, swipePaint)
                }
            } catch (e: Exception) {
                // General drawing exception handling
                // Continue with basic functionality
            }
        }
    }
    
    private fun drawThemedKey(canvas: Canvas, key: Keyboard.Key) {
        val manager = themeManager
        if (manager == null) {
            // Fallback to basic key drawing
            drawBasicKey(canvas, key)
            return
        }
        
        val palette = manager.getCurrentPalette()
        
        // Calculate density
        val density = context.resources.displayMetrics.density
        
        // Key rectangle with padding - apply custom spacing if set
        val basePadding = if (borderlessMode) 0f else (1f * density)
        val verticalSpacing = keySpacingVerticalDp * density / 2f
        val horizontalSpacing = keySpacingHorizontalDp * density / 2f
        
        val keyRect = RectF(
            key.x.toFloat() + basePadding + horizontalSpacing,
            key.y.toFloat() + basePadding + verticalSpacing,
            (key.x + key.width).toFloat() - basePadding - horizontalSpacing,
            (key.y + key.height).toFloat() - basePadding - verticalSpacing
        )
        
        // Identify key type using centralized logic
        val keyCode = key.codes[0]
        val keyType = getKeyType(keyCode)
        
        // Get appropriate drawable from factory
        val keyDrawable = when {
            keyType == "enter" && manager.shouldUseAccentForEnter() -> manager.createSpecialKeyDrawable()
            keyType in listOf("voice", "emoji") && isKeyActive(keyType) -> manager.createSpecialKeyDrawable()
            manager.shouldUseAccentForKey(keyType) -> manager.createSpecialKeyDrawable()
            else -> manager.createKeyDrawable()
        }
        
        // Draw key background using cached drawable
        keyDrawable.setBounds(keyRect.left.toInt(), keyRect.top.toInt(), keyRect.right.toInt(), keyRect.bottom.toInt())
        keyDrawable.draw(canvas)
        
        // Draw key content (icon or text)
        val centerX = keyRect.centerX()
        val centerY = keyRect.centerY()
        
        if (key.icon != null) {
            // Draw icon with proper tinting
            drawKeyIcon(canvas, key, centerX, centerY, keyType)
        } else if (key.label != null) {
            // Draw text using themed paint
            drawKeyText(canvas, key, centerX, centerY, keyType)
        }
    }
    
    /**
     * Get key type for theme application
     */
    private fun getKeyType(keyCode: Int): String = when (keyCode) {
        Keyboard.KEYCODE_SHIFT, -1 -> "shift"
        Keyboard.KEYCODE_DELETE, -5 -> "backspace"
        Keyboard.KEYCODE_DONE, 10, -4 -> "enter"
        32 -> "space"
        -13, -16 -> "mic"
        -15 -> "emoji"
        -14 -> "globe"
        -10, -11, -12 -> "symbols"
        else -> "regular"
    }
    
    /**
     * Check if a special key is currently active
     */
    private fun isKeyActive(keyType: String): Boolean = when (keyType) {
        "voice", "mic" -> isVoiceKeyActive
        "emoji" -> isEmojiKeyActive
        else -> false
    }
    
    /**
     * Draw key icon with proper theming
     */
    private fun drawKeyIcon(canvas: Canvas, key: Keyboard.Key, centerX: Float, centerY: Float, keyType: String) {
        val manager = themeManager ?: return
        val palette = manager.getCurrentPalette()
        
        val iconDrawable = key.icon.mutate()
        val iconSize = minOf(key.width, key.height) * 0.4f // 40% of key size
        
        // Apply tint based on key type and state
            val tintColor = when {
            keyType == "space" -> palette.spaceLabelColor
            manager.shouldUseAccentForKey(keyType) -> Color.WHITE // Intentional: White text for contrast on accent background
            else -> palette.keyText
        }
        
        iconDrawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        iconDrawable.setBounds(
            (centerX - iconSize/2).toInt(),
            (centerY - iconSize/2).toInt(),
            (centerX + iconSize/2).toInt(),
            (centerY + iconSize/2).toInt()
        )
            iconDrawable.draw(canvas)
        }
        
    /**
     * Draw key text with proper theming
     */
    private fun drawKeyText(canvas: Canvas, key: Keyboard.Key, centerX: Float, centerY: Float, keyType: String) {
        val manager = themeManager ?: return
        val palette = manager.getCurrentPalette()
        
        val textPaint = when (keyType) {
            "space" -> spaceLabelPaint ?: manager.createSpaceLabelPaint()
            else -> keyTextPaint ?: manager.createKeyTextPaint()
        }
        
        // Apply font scale multiplier
        val basePaint = Paint(textPaint)
        basePaint.textSize = textPaint.textSize * labelScaleMultiplier
        
        // Override text color for special keys with accent background
        if (manager.shouldUseAccentForKey(keyType) || 
            (keyType == "enter" && manager.shouldUseAccentForEnter()) ||
            isKeyActive(keyType)) {
            basePaint.color = Color.WHITE // Intentional: Ensures readability on accent background
        } else {
            basePaint.color = palette.keyText
        }
        
        val text = if (keyType == "space") {
            // Show language label on spacebar if enabled
            if (showLanguageOnSpace) {
                currentLanguageLabel
            } else {
                "" // Don't show any label
            }
        } else {
            key.label?.toString() ?: ""
        }
        
        // Center the text
        val textHeight = basePaint.descent() - basePaint.ascent()
        val textOffset = (textHeight / 2) - basePaint.descent()
        canvas.drawText(text, centerX, centerY + textOffset, basePaint)
        
        // Draw popup hint for number keys
                if (key.popupCharacters != null && key.popupCharacters.isNotEmpty()) {
            val hintPaint = Paint(basePaint).apply {
                textSize = basePaint.textSize * 0.5f
                alpha = (255 * 0.7f).toInt() // 70% opacity
            }
            val hintX = centerX - (key.width * 0.3f)
            val hintY = centerY - (key.height * 0.2f)
                    canvas.drawText(key.popupCharacters[0].toString(), hintX, hintY, hintPaint)
                }
            }
    
    /**
     * Fallback key drawing when no theme manager available
     */
    private fun drawBasicKey(canvas: Canvas, key: Keyboard.Key) {
        val keyRect = RectF(
            key.x.toFloat(), key.y.toFloat(),
            (key.x + key.width).toFloat(), (key.y + key.height).toFloat()
        )
        
        val fillPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = Color.WHITE // Intentional: Fallback white for swipe dots visibility
                }
        
        val textPaint = Paint().apply {
                    isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 18f * context.resources.displayMetrics.scaledDensity
            color = Color.BLACK // Intentional: Fallback default for debugging text
        }
        
        canvas.drawRoundRect(keyRect, 8f, 8f, fillPaint)
        
        if (key.label != null) {
            val centerX = keyRect.centerX()
            val centerY = keyRect.centerY()
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textOffset = (textHeight / 2) - textPaint.descent()
            canvas.drawText(key.label.toString(), centerX, centerY + textOffset, textPaint)
        }
    }
    
    override fun onTouchEvent(me: MotionEvent): Boolean {
        // Handle clipboard mode first
        if (isClipboardMode) {
            when (me.action) {
                MotionEvent.ACTION_UP -> {
                    handleClipboardTouch(me.x, me.y)
                    return true
                }
            }
            return true
        }
        
        return try {
            // Initialize adaptive sizing on first touch
            if (isAdaptiveSizingEnabled && adaptiveKeyWidth == 0f) {
                initializeAdaptiveSizing()
            }
            
            // Handle spacebar gestures first
            val spacebarHandled = handleSpacebarGesture(me)
            if (spacebarHandled) return true
            
            // Handle enhanced gestures first
            val gestureHandled = handleEnhancedGestures(me)
            if (gestureHandled) return true
            
            // Handle shift key long press detection
            val shiftKeyHandled = handleShiftKeyTouch(me)
            if (shiftKeyHandled) return true
            
            if (!swipeEnabled) {
                return super.onTouchEvent(me)
            }
            
            val handled = handleSwipeTouch(me)
            if (handled) true else super.onTouchEvent(me)
        } catch (e: StringIndexOutOfBoundsException) {
            // Handle the case where KeyboardView tries to access empty key labels
            // This prevents crashes when caps lock is pressed
            false
        } catch (e: Exception) {
            // General exception handling for touch events
            false
        }
    }
    
    /**
     * Handle enhanced gestures for backspace slide-to-delete and spacebar cursor control
     */
    private fun handleEnhancedGestures(event: MotionEvent): Boolean {
        val keys = keyboard?.keys ?: return false
        val key = getKeyAtPosition(event.x, event.y, keys)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartTime = System.currentTimeMillis()
                when (key?.codes?.firstOrNull()) {
                    Keyboard.KEYCODE_DELETE -> {
                        backspaceSlideStartX = event.x
                        isSlideToDeleteActive = false
                    }
                    32 -> { // Space key
                        spacebarSwipeStartX = event.x
                        isCursorControlActive = false
                    }
                }
                return false
            }
            
            MotionEvent.ACTION_MOVE -> {
                when (key?.codes?.firstOrNull()) {
                    Keyboard.KEYCODE_DELETE -> {
                        val deltaX = kotlin.math.abs(event.x - backspaceSlideStartX)
                        if (deltaX > SLIDE_THRESHOLD && !isSlideToDeleteActive && 
                            System.currentTimeMillis() - gestureStartTime > 200) {
                            isSlideToDeleteActive = true
                            (context as? AIKeyboardService)?.activateSlideToDelete()
                            return true
                        }
                    }
                    32 -> { // Space key
                        val deltaX = kotlin.math.abs(event.x - spacebarSwipeStartX)
                        if (deltaX > CURSOR_THRESHOLD && !isCursorControlActive) {
                            isCursorControlActive = true
                            return true
                        } else if (isCursorControlActive) {
                            handleSpacebarCursorControl(event.x - spacebarSwipeStartX)
                            return true
                        }
                    }
                }
                return false
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isSlideToDeleteActive) {
                    isSlideToDeleteActive = false
                    (context as? AIKeyboardService)?.deactivateSlideToDelete()
                    return true
                }
                if (isCursorControlActive) {
                    isCursorControlActive = false
                    return true
                }
                return false
            }
        }
        
        return false
    }
    

    /**
     * Handle shift key touch events for long press detection
     */
    private fun handleShiftKeyTouch(event: MotionEvent): Boolean {
        val keys = keyboard?.keys ?: return false
        val key = getKeyAtPosition(event.x, event.y, keys)
        
        // Check if this is the shift key
        if (key?.codes?.firstOrNull() == Keyboard.KEYCODE_SHIFT) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Notify AIKeyboardService to start long press detection
                    (context as? AIKeyboardService)?.let { service ->
                        service.startShiftKeyLongPressDetection()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Cancel long press detection
                    (context as? AIKeyboardService)?.let { service ->
                        service.cancelShiftKeyLongPressDetection()
                    }
                }
            }
        }
        
        return false // Don't consume the event, let normal processing continue
    }
    
    /**
     * Get the key at the specified position
     */
    private fun getKeyAtPosition(x: Float, y: Float, keys: List<Keyboard.Key>): Keyboard.Key? {
        for (key in keys) {
            if (key.isInside(x.toInt(), y.toInt())) {
                return key
            }
        }
        return null
    }
    
    private fun handleSwipeTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startSwipe(x, y)
                false // Let normal key press handling occur
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isSwipeInProgress) {
                    continueSwipe(x, y)
                    true // Consume the event
                } else {
                    // Check if user has moved enough to start swipe
                    if (swipePoints.isNotEmpty()) {
                        val startPoint = swipePoints[0]
                        val distance = sqrt(
                            (x - startPoint[0]).pow(2) + (y - startPoint[1]).pow(2)
                        )
                        
                        if (distance > SWIPE_START_THRESHOLD) {
                            isSwipeInProgress = true
                            swipeListener?.onSwipeStarted()
                            continueSwipe(x, y)
                            return true
                        }
                    }
                    false
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isSwipeInProgress) {
                    endSwipe(x, y)
                    true
                } else {
                    // Reset swipe data for normal key press
                    resetSwipe()
                    false
                }
            }
            
            MotionEvent.ACTION_CANCEL -> {
                resetSwipe()
                isSwipeInProgress
            }
            
            else -> false
        }
    }
    
    private fun startSwipe(x: Float, y: Float) {
        swipePoints.clear()
        swipePoints.add(floatArrayOf(x, y))
        swipeStartTime = System.currentTimeMillis()
        swipePath.reset()
        swipePath.moveTo(x, y)
        isSwipeInProgress = false // Will be set to true when movement is detected
    }
    
    private fun continueSwipe(x: Float, y: Float) {
        swipePoints.add(floatArrayOf(x, y))
        swipePath.lineTo(x, y)
        invalidate() // Redraw to show swipe path
    }
    
    private fun endSwipe(x: Float, y: Float) {
        if (!isSwipeInProgress) return
        
        swipePoints.add(floatArrayOf(x, y))
        val swipeDuration = System.currentTimeMillis() - swipeStartTime
        
        // Calculate total swipe distance
        val totalDistance = calculateTotalDistance()
        
        // Only process as swipe if it meets minimum criteria
        if (swipeDuration >= MIN_SWIPE_TIME && totalDistance >= MIN_SWIPE_DISTANCE) {
            processSwipe()
        }
        
        resetSwipe()
        swipeListener?.onSwipeEnded()
    }
    
    private fun calculateTotalDistance(): Float {
        if (swipePoints.size < 2) return 0f
        
        var totalDistance = 0f
        for (i in 1 until swipePoints.size) {
            val prev = swipePoints[i - 1]
            val curr = swipePoints[i]
            totalDistance += sqrt(
                (curr[0] - prev[0]).pow(2) + (curr[1] - prev[1]).pow(2)
            )
        }
        return totalDistance
    }
    
    private fun processSwipe() {
        if (swipeListener == null || swipePoints.isEmpty()) return
        
        // Enhanced swipe processing for better gesture recognition
        val swipedKeys = mutableListOf<Int>()
        val swipePattern = generateSwipePattern()
        
        // Use path sampling for better accuracy - sample every 10-15 pixels
        val sampledPoints = sampleSwipePath(swipePoints)
        
        // Convert sampled points to key sequence
        val keySequence = mutableListOf<Int>()
        var lastKeyCode = -1
        
        sampledPoints.forEach { point ->
            val keyIndex = keyboard?.let { findKeyAtPoint(point[0].toInt(), point[1].toInt()) } ?: -1
            if (keyIndex >= 0) {
                try {
                    keyboard?.keys?.get(keyIndex)?.codes?.get(0)?.let { keyCode ->
                        if (Character.isLetter(keyCode)) {
                            // Only add if it's a different key from the last one
                            if (keyCode != lastKeyCode) {
                                keySequence.add(keyCode)
                                lastKeyCode = keyCode
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore errors in key detection
                }
            }
        }
        
        // Also provide unique keys for backward compatibility
        swipedKeys.addAll(keySequence.distinct())
        
        swipeListener?.onSwipeDetected(swipedKeys, swipePattern, keySequence)
    }
    
    /**
     * Sample swipe path for better gesture recognition
     * Reduces noise and provides more accurate key sequence
     */
    private fun sampleSwipePath(points: List<FloatArray>): List<FloatArray> {
        if (points.size <= 2) return points
        
        val sampledPoints = mutableListOf<FloatArray>()
        sampledPoints.add(points.first()) // Always include start point
        
        var totalDistance = 0f
        val samplingDistance = 15f // Sample every 15 pixels
        
        for (i in 1 until points.size) {
            val prevPoint = points[i - 1]
            val currPoint = points[i]
            
            val segmentDistance = sqrt(
                (currPoint[0] - prevPoint[0]).pow(2) + (currPoint[1] - prevPoint[1]).pow(2)
            )
            
            totalDistance += segmentDistance
            
            // Sample point if we've traveled enough distance
            if (totalDistance >= samplingDistance) {
                sampledPoints.add(currPoint)
                totalDistance = 0f
            }
        }
        
        // Always include end point
        if (sampledPoints.last() != points.last()) {
            sampledPoints.add(points.last())
        }
        
        return sampledPoints
    }
    
    private fun generateSwipePattern(): String {
        if (swipePoints.size < 2) return ""
        
        val start = swipePoints[0]
        val end = swipePoints[swipePoints.size - 1]
        
        // Simple pattern based on start and end positions
        val deltaX = end[0] - start[0]
        val deltaY = end[1] - start[1]
        
        // Determine general direction
        return if (abs(deltaX) > abs(deltaY)) {
            if (deltaX > 0) "right" else "left"
        } else {
            if (deltaY > 0) "down" else "up"
        }
    }
    
    private fun resetSwipe() {
        isSwipeInProgress = false
        swipePoints.clear()
        swipePath.reset()
        invalidate() // Clear the drawn path
    }
    
    
    /**
     * Cleanup when view is detached
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Theme listener removed - using static default colors
    }
    
    private fun findKeyAtPoint(x: Int, y: Int): Int {
        keyboard?.let { kbd ->
            try {
                kbd.keys.forEachIndexed { index, key ->
                    if (x >= key.x && x < key.x + key.width && 
                        y >= key.y && y < key.y + key.height) {
                        return index
                    }
                }
            } catch (e: Exception) {
                // Ignore errors in key detection
            }
        }
        return -1
    }
    
    /**
     * Set the keyboard service reference for clipboard functionality
     */
    fun setKeyboardService(service: AIKeyboardService) {
        clipboardService = service
    }
    
    /**
     * Show clipboard layout
     */
    fun showClipboardLayout(items: List<ClipboardItem>) {
        isClipboardMode = true
        clipboardItems = items
        calculateClipboardKeyLayout()
        invalidate()
    }
    
    /**
     * Show normal layout
     */
    fun showNormalLayout() {
        isClipboardMode = false
        clipboardItems = emptyList()
        clipboardKeyRects.clear()
        invalidate()
    }
    
    /**
     * Refresh clipboard UI
     */
    fun refreshClipboardUI() {
        if (isClipboardMode) {
            calculateClipboardKeyLayout()
            invalidate()
        }
    }
    
    /**
     * Calculate clipboard key layout
     */
    private fun calculateClipboardKeyLayout() {
        clipboardKeyRects.clear()
        
        val padding = 16f
        val keyMargin = 8f
        val backButtonHeight = 80f
        val availableWidth = width - (padding * 2)
        val availableHeight = height - (padding * 2) - backButtonHeight - keyMargin
        
        // Calculate grid layout
        val columns = 2
        val rows = minOf(((clipboardItems.size + columns - 1) / columns), 5) // Max 5 rows
        
        val keyWidth = (availableWidth - (keyMargin * (columns - 1))) / columns
        val keyHeight = if (rows > 0) (availableHeight - (keyMargin * (rows - 1))) / rows else 0f
        
        // Create key rectangles
        for (i in clipboardItems.indices) {
            val row = i / columns
            val col = i % columns
            
            if (row < 5) { // Only show first 10 items (5 rows × 2 columns)
                val left = padding + (col * (keyWidth + keyMargin))
                val top = padding + (row * (keyHeight + keyMargin))
                val right = left + keyWidth
                val bottom = top + keyHeight
                
                clipboardKeyRects.add(RectF(left, top, right, bottom))
            }
        }
        
        // Add back button rectangle at the bottom
        val backButtonTop = height - backButtonHeight - padding
        clipboardKeyRects.add(RectF(
            padding, 
            backButtonTop, 
            padding + availableWidth, 
            backButtonTop + backButtonHeight
        ))
    }
    
    
    /**
     * Draw clipboard layout
     */
    private fun drawClipboardLayout(canvas: Canvas) {
        
        val manager = themeManager
        if (manager == null) {
            // Fallback colors if no theme manager
            canvas.drawColor(Color.parseColor("#F5F5F5"))
            return
        }
        val palette = manager.getCurrentPalette()
        val backgroundColor = palette.keyboardBg
        val keyBackgroundColor = palette.keyBg
        val keyTextColor = palette.keyText
        val accentColor = palette.specialAccent
        
        // Draw background
        canvas.drawColor(backgroundColor)
        
        // Draw clipboard items
        for (i in clipboardItems.indices.take(clipboardKeyRects.size - 1)) {
            val item = clipboardItems[i]
            val rect = clipboardKeyRects[i]
            
            // Choose background color (accent for pinned/template items)
            val bgColor = if (item.isPinned || item.isTemplate) {
                adjustColorAlpha(accentColor, 0.1f)
            } else {
                keyBackgroundColor
            }
            
            // Draw key background
            val keyPaint = Paint().apply {
                color = bgColor
                isAntiAlias = true
            }
            val cornerRadius = 8f
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyPaint)
            
            // Draw key border
            val borderPaint = Paint().apply {
                color = adjustColorAlpha(keyTextColor, 0.2f)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
            
            // Draw text
            val textPaint = Paint().apply {
                color = keyTextColor
                textSize = palette.keyFontSize
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            
            // Prepare text (truncate if needed)
            val prefix = if (item.isOTP()) "🔢 " else if (item.isTemplate) "📌 " else ""
            val displayText = prefix + item.getPreview(20)
            
            // Draw text centered in rect
            val textX = rect.centerX()
            val textY = rect.centerY() + (textPaint.textSize / 3)
            canvas.drawText(displayText, textX, textY, textPaint)
            
            // Draw category for templates
            if (item.isTemplate && item.category != null) {
                val categoryPaint = Paint().apply {
                    color = adjustColorAlpha(keyTextColor, 0.6f)
                    textSize = palette.keyFontSize * 0.75f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val categoryY = rect.bottom - 12f
                canvas.drawText(item.category, textX, categoryY, categoryPaint)
            }
        }
        
        // Draw back button
        if (clipboardKeyRects.isNotEmpty()) {
            val backRect = clipboardKeyRects.last()
            
            // Draw back button background
            val backPaint = Paint().apply {
                color = accentColor
                isAntiAlias = true
            }
            val cornerRadius = 8f
            canvas.drawRoundRect(backRect, cornerRadius, cornerRadius, backPaint)
            
            // Draw back button text
            val backTextPaint = Paint().apply {
                color = Color.WHITE // Intentional: White text for delete key icon contrast
                textSize = palette.keyFontSize * 1.2f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            
            val backTextX = backRect.centerX()
            val backTextY = backRect.centerY() + (backTextPaint.textSize / 3)
            canvas.drawText("⬅ Back to Keyboard", backTextX, backTextY, backTextPaint)
        }
    }
    
    
    /**
     * Handle touch in clipboard mode
     */
    private fun handleClipboardTouch(x: Float, y: Float) {
        // Check clipboard item touches
        for (i in clipboardItems.indices.take(clipboardKeyRects.size - 1)) {
            val rect = clipboardKeyRects[i]
            if (rect.contains(x, y)) {
                val item = clipboardItems[i]
                // Call back to service to handle clipboard key tap
                (context as? AIKeyboardService)?.let { service ->
                    try {
                        val method = service.javaClass.getDeclaredMethod("handleClipboardKeyTap", ClipboardItem::class.java)
                        method.isAccessible = true
                        method.invoke(service, item)
                    } catch (e: Exception) {
                        android.util.Log.e("SwipeKeyboardView", "Error calling handleClipboardKeyTap", e)
                    }
                }
                return
            }
        }
        
        // Check back button touch
        if (clipboardKeyRects.isNotEmpty()) {
            val backRect = clipboardKeyRects.last()
            if (backRect.contains(x, y)) {
                // Call back to service to handle back button
                (context as? AIKeyboardService)?.let { service ->
                    try {
                        val method = service.javaClass.getDeclaredMethod("handleClipboardBackTap")
                        method.isAccessible = true
                        method.invoke(service)
                    } catch (e: Exception) {
                        android.util.Log.e("SwipeKeyboardView", "Error calling handleClipboardBackTap", e)
                    }
                }
                return
            }
        }
    }
    
    /**
     * Utility method to adjust color alpha
     */
    private fun adjustColorAlpha(color: Int, alpha: Float): Int {
        val a = (Color.alpha(color) * alpha).toInt()
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(a, r, g, b)
    }
    
    /**
     * Initialize adaptive sizing based on screen dimensions
     */
    private fun initializeAdaptiveSizing() {
        if (!isAdaptiveSizingEnabled) return
        
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val density = displayMetrics.density
        
        // Determine screen size category
        val screenWidthDp = screenWidth / density
        
        when {
            screenWidthDp < SCREEN_SIZE_SMALL -> {
                isSmallScreenOptimized = true
                touchTargetExpansion = context.resources.getDimension(R.dimen.small_screen_touch_expansion)
            }
            else -> {
                touchTargetExpansion = context.resources.getDimension(R.dimen.touch_target_expansion)
            }
        }
    }
    
    /**
     * Enable floating keyboard mode
     */
    fun enableFloatingMode(enable: Boolean) {
        isFloatingMode = enable
        if (enable) {
            elevation = 12f
        } else {
            elevation = 0f
        }
        invalidate()
    }
    
    /**
     * Handle spacebar gesture detection
     */
    private fun handleSpacebarGesture(event: MotionEvent): Boolean {
        if (!spacebarGestureEnabled) return false
        
        val key = getKeyAtPosition(event.x, event.y)
        if (key?.codes?.firstOrNull() != 32) { // Not spacebar
            isSpacebarPressed = false
            return false
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isSpacebarPressed = true
                spacebarStartX = event.x
                lastSpacebarX = event.x
                return false
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (!isSpacebarPressed) return false
                
                val deltaX = event.x - lastSpacebarX
                val totalDeltaX = event.x - spacebarStartX
                
                if (Math.abs(totalDeltaX) > SPACEBAR_GESTURE_THRESHOLD) {
                    handleSpacebarCursorControl(deltaX)
                    lastSpacebarX = event.x
                    return true
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (!isSpacebarPressed) return false
                
                val deltaX = event.x - spacebarStartX
                val distance = Math.abs(deltaX)
                
                if (distance > SPACEBAR_GESTURE_THRESHOLD) {
                    when {
                        deltaX < WORD_DELETE_VELOCITY_THRESHOLD -> {
                            handleSpacebarSwipeLeft()
                            return true
                        }
                        deltaX > PERIOD_INSERT_VELOCITY_THRESHOLD -> {
                            handleSpacebarSwipeRight()
                            return true
                        }
                    }
                }
                
                isSpacebarPressed = false
            }
        }
        
        return false
    }
    
    /**
     * Handle spacebar swipe left - delete word
     */
    private fun handleSpacebarSwipeLeft() {
        (context as? AIKeyboardService)?.let { service ->
            service.currentInputConnection?.let { ic ->
                val beforeCursor = ic.getTextBeforeCursor(50, 0)
                if (!beforeCursor.isNullOrEmpty()) {
                    val words = beforeCursor.toString().split(Regex("\\s+"))
                    if (words.isNotEmpty()) {
                        val lastWord = words.last()
                        if (lastWord.isNotEmpty()) {
                            ic.deleteSurroundingText(lastWord.length, 0)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handle spacebar swipe right - insert period and space
     */
    private fun handleSpacebarSwipeRight() {
        (context as? AIKeyboardService)?.let { service ->
            service.currentInputConnection?.commitText(". ", 1)
        }
    }
    
    /**
     * Handle spacebar cursor control
     */
    private fun handleSpacebarCursorControl(deltaX: Float) {
        val cursorMoves = (deltaX / CURSOR_CONTROL_SENSITIVITY).toInt()
        
        (context as? AIKeyboardService)?.let { service ->
            service.currentInputConnection?.let { ic ->
                try {
                    val extractedText = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                    val currentPos = extractedText?.selectionStart ?: 0
                    val textLength = extractedText?.text?.length ?: 0
                    val newPos = (currentPos + cursorMoves).coerceIn(0, textLength)
                    
                    ic.setSelection(newPos, newPos)
                } catch (e: Exception) {
                    // Handle cursor control error
                }
            }
        }
    }
    
    /**
     * Get key at specific position with enhanced touch target detection
     */
    private fun getKeyAtPosition(x: Float, y: Float): Keyboard.Key? {
        keyboard?.keys?.forEach { key ->
            val expandedBounds = if (isSmallScreenOptimized) {
                RectF(
                    key.x.toFloat() - touchTargetExpansion,
                    key.y.toFloat() - touchTargetExpansion,
                    (key.x + key.width).toFloat() + touchTargetExpansion,
                    (key.y + key.height).toFloat() + touchTargetExpansion
                )
            } else {
                RectF(
                    key.x.toFloat(),
                    key.y.toFloat(),
                    (key.x + key.width).toFloat(),
                    (key.y + key.height).toFloat()
                )
            }
            
            if (expandedBounds.contains(x, y)) {
                return key
            }
        }
        return null
    }
}
