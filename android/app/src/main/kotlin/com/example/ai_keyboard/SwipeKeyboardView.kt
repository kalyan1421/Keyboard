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
        color = Color.parseColor("#2196F3")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
        alpha = 180
    }
    private val swipePath = Path()
    private var swipeListener: SwipeListener? = null
    private var swipeStartTime = 0L
    
    // Theme support removed - using default styling only
    private val keyPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val keyBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val keyTextPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 56f
        typeface = Typeface.DEFAULT
    }
    
    // Theme change listener removed - using static default colors
    
    // Advanced shift state visual feedback
    private var isShiftHighlighted = false
    private var isCapsLockActive = false
    private val shiftHighlightPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val capsLockPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Enhanced special key state tracking
    private var isVoiceKeyActive = false
    private var isEmojiKeyActive = false
    private val specialKeyHighlightPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#1A73E8") // Gboard blue
    }
    
    init {
        // Initialize with default colors
        initializeDefaultColors()
        
        // Create default key background
        try {
            val keyBackground = createStableKeyBackground()
            background = keyBackground
        } catch (e: Exception) {
            // If drawable creation fails, set a simple background
            setBackgroundColor(Color.parseColor("#F5F5F5")) // Light gray
        }
    }
    
    private fun createStableKeyBackground(): Drawable {
        // Create a single stable drawable with no state changes
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
            setStroke(1, Color.parseColor("#E0E0E0"))
            cornerRadius = 6f
        }
    }
    
    fun setSwipeEnabled(enabled: Boolean) {
        swipeEnabled = enabled
    }
    
    fun setSwipeListener(listener: SwipeListener?) {
        swipeListener = listener
    }
    
    // Theme application methods removed - using single default theme only
    
    fun setShiftKeyHighlight(highlighted: Boolean, capsLock: Boolean) {
        isShiftHighlighted = highlighted
        isCapsLockActive = capsLock
        android.util.Log.d("SwipeKeyboardView", "Shift highlight updated - highlighted: $highlighted, capsLock: $capsLock")
        invalidate()
    }
    
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
    
    private fun initializeDefaultColors() {
        // Set default colors for all paint objects
        keyPaint.color = Color.WHITE
        keyBorderPaint.color = adjustColorBrightness(Color.WHITE, 0.8f)
        keyTextPaint.color = Color.BLACK
        keyTextPaint.textSize = 18f * context.resources.displayMetrics.density
        keyTextPaint.typeface = Typeface.DEFAULT
        
        // Set default accent colors for special states
        shiftHighlightPaint.color = Color.parseColor("#1A73E8") // Blue
        capsLockPaint.color = adjustColorBrightness(Color.parseColor("#1A73E8"), 1.2f)
        
        // Set default background
        setBackgroundColor(Color.parseColor("#F5F5F5")) // Light gray
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
    
    private fun drawThemedKey(canvas: Canvas, key: Keyboard.Key) {
        // Reduced padding for tighter key spacing
        val padding = 1f
        val keyRect = RectF(
            key.x.toFloat() + padding,
            key.y.toFloat() + padding,
            (key.x + key.width).toFloat() - padding,
            (key.y + key.height).toFloat() - padding
        )
        
        // Identify key types for proper styling
        val isActionKey = key.codes[0] < 0 && key.codes[0] != -32 // space is not action key
        val isShiftKey = key.codes[0] == -1 // Shift key code
        val isSpaceKey = key.codes[0] == 32 // Space key code
        val isVoiceKey = key.codes[0] == -13 // Voice input key code
        val isEmojiKey = key.codes[0] == -15 // Emoji key code
        val isGlobeKey = key.codes[0] == -14 // Globe/Language key code
        
        // Gboard-style subtle shadow
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor("#08000000") // Very subtle shadow
        }
        
        // Draw shadow with minimal offset for modern look
        val shadowOffset = 1f
        val shadowRect = RectF(
            keyRect.left + shadowOffset,
            keyRect.top + shadowOffset,
            keyRect.right + shadowOffset,
            keyRect.bottom + shadowOffset
        )
        
        // Get corner radius from theme
        val cornerRadius = 6f // Default corner radius
        
        // Draw subtle shadow (always enabled in default theme)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)
        
        // Gboard-style key coloring
        val fillPaint = when {
            isShiftKey && isCapsLockActive -> {
                // Caps lock - prominent highlighting
                Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = adjustColorBrightness(Color.parseColor("#1A73E8"), 1.2f)
                }
            }
            isShiftKey && isShiftHighlighted -> {
                // Shift active - more visible highlighting
                Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = Color.parseColor("#1A73E8")
                }
            }
            isVoiceKey && isVoiceKeyActive -> {
                // Voice key active - special highlighting
                specialKeyHighlightPaint.apply {
                    color = Color.parseColor("#4285F4") // Google blue
                }
            }
            isEmojiKey && isEmojiKeyActive -> {
                // Emoji key active - special highlighting
                specialKeyHighlightPaint.apply {
                    color = Color.parseColor("#34A853") // Google green
                }
            }
            isActionKey -> {
                // Special keys (shift, delete, etc.) - light gray
                Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = adjustColorBrightness(Color.WHITE, 0.9f)
                }
            }
            else -> {
                // Regular letter/number keys - white
                keyPaint
            }
        }
        
        // Draw key background
        canvas.drawRoundRect(keyRect, cornerRadius, cornerRadius, fillPaint)
        
        // Gboard-style subtle border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = keyBorderPaint.color
            alpha = 40 // Very subtle border
        }
        canvas.drawRoundRect(keyRect, cornerRadius, cornerRadius, borderPaint)
        
        // Draw key text or icon with Gboard styling
        val centerX = keyRect.centerX()
        val centerY = keyRect.centerY()
        
        if (key.label != null && key.label.isNotEmpty()) {
            // Much larger text sizing for better readability
            val textSize = when {
                isSpaceKey -> 36f  // Smaller for space bar text
                key.label.length > 3 -> 36f  // Larger for labels like "123"
                key.label.length > 1 -> 36f  // Much larger for multi-char labels
                else -> 40f  // Very large for single characters (letters/numbers)
            }
            
            // Create themed text paint
            val themedTextPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                this.textSize = textSize
                typeface = Typeface.DEFAULT
                color = when {
                    isSpaceKey -> adjustColorBrightness(Color.BLACK, 0.7f)
                    else -> Color.BLACK
                }
            }
            
            // Special handling for space bar
            if (isSpaceKey) {
                val spaceText = "English (US)" // Show current language
                val textHeight = themedTextPaint.descent() - themedTextPaint.ascent()
                val textOffset = (textHeight / 2) - themedTextPaint.descent()
                canvas.drawText(spaceText, centerX, centerY + textOffset, themedTextPaint)
            } else {
                val textHeight = themedTextPaint.descent() - themedTextPaint.ascent()
                val textOffset = (textHeight / 2) - themedTextPaint.descent()
                
                // Show uppercase letters when caps lock is active
                val displayText = if (isCapsLockActive && key.label != null && key.label.length == 1) {
                    val char = key.label[0]
                    if (Character.isLetter(char)) {
                        val upperText = char.uppercaseChar().toString()
                        android.util.Log.d("SwipeKeyboardView", "Caps active: converting '$char' to '$upperText'")
                        upperText
                    } else {
                        key.label.toString()
                    }
                } else {
                    key.label.toString()
                }
                
                canvas.drawText(displayText, centerX, centerY + textOffset, themedTextPaint)
            }
        } else if (key.icon != null) {
            // Gboard-style icon handling
            if (isShiftKey && isCapsLockActive) {
                // Caps lock: Show icon with underline indicator
                val iconSize = 20
                val iconLeft = centerX - iconSize / 2
                val iconTop = centerY - iconSize / 2 - 4  // Move up slightly
                
                // Tint icon with themed text color
                key.icon.setTint(Color.BLACK)
                key.icon.setBounds(
                    iconLeft.toInt(),
                    iconTop.toInt(),
                    (iconLeft + iconSize).toInt(),
                    (iconTop + iconSize).toInt()
                )
                key.icon.draw(canvas)
                
                // Draw underline to indicate caps lock
                val underlinePaint = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                canvas.drawLine(
                    centerX - 8f, centerY + 12f,
                    centerX + 8f, centerY + 12f,
                    underlinePaint
                )
            } else {
                // Regular icon
                val iconSize = 30 // Gboard-style icon size
                val iconLeft = centerX - iconSize / 2
                val iconTop = centerY - iconSize / 2
                
                // Tint icon with themed colors
                key.icon.setTint(Color.BLACK)
                
                key.icon.setBounds(
                    iconLeft.toInt(),
                    iconTop.toInt(),
                    (iconLeft + iconSize).toInt(),
                    (iconTop + iconSize).toInt()
                )
                key.icon.draw(canvas)
            }
        }
    }
    
    override fun onTouchEvent(me: MotionEvent): Boolean {
        return try {
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
}
