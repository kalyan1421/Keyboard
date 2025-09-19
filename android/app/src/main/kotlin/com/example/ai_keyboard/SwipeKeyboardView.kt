package com.example.ai_keyboard

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat
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
        fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String)
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
    
    // Theme support
    private var currentTheme = "default"
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
    
    init {
        // Initialize theme colors
        updateThemeColors()
        
        // Create a completely stable key background - no state changes
        try {
            val keyBackground = createStableKeyBackground()
            // This will be used by the KeyboardView internally
            background = keyBackground
        } catch (e: Exception) {
            // If drawable creation fails, set a simple background
            setBackgroundColor(Color.WHITE)
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
    
    fun setKeyboardTheme(theme: String) {
        currentTheme = theme
        updateThemeColors()
        invalidate()
    }
    
    fun setShiftKeyHighlight(highlighted: Boolean, capsLock: Boolean) {
        isShiftHighlighted = highlighted
        isCapsLockActive = capsLock
        android.util.Log.d("SwipeKeyboardView", "Shift highlight updated - highlighted: $highlighted, capsLock: $capsLock")
        invalidate()
    }
    
    private fun updateThemeColors() {
        when (currentTheme) {
            "gboard_dark" -> {
                keyPaint.color = ContextCompat.getColor(context, R.color.gboard_dark_key_background)
                keyBorderPaint.color = ContextCompat.getColor(context, R.color.gboard_dark_key_border)
                keyTextPaint.color = ContextCompat.getColor(context, R.color.gboard_dark_key_text)
                shiftHighlightPaint.color = ContextCompat.getColor(context, R.color.shift_active_dark)
                capsLockPaint.color = ContextCompat.getColor(context, R.color.shift_caps_dark)
                setBackgroundColor(ContextCompat.getColor(context, R.color.gboard_dark_background))
            }
            "dark" -> {
                keyPaint.color = Color.parseColor("#2D2D2D")
                keyBorderPaint.color = Color.parseColor("#424242")
                keyTextPaint.color = Color.WHITE
                shiftHighlightPaint.color = Color.parseColor("#4CAF50")
                capsLockPaint.color = Color.parseColor("#FF9800")
                setBackgroundColor(Color.parseColor("#1E1E1E"))
            }
            "material_you" -> {
                keyPaint.color = Color.parseColor("#7C4DFF")
                keyBorderPaint.color = Color.parseColor("#6750A4")
                keyTextPaint.color = Color.WHITE
                shiftHighlightPaint.color = Color.parseColor("#E1BEE7")
                capsLockPaint.color = Color.parseColor("#FFC107")
                setBackgroundColor(Color.parseColor("#6750A4"))
            }
            "professional" -> {
                keyPaint.color = Color.parseColor("#455A64")
                keyBorderPaint.color = Color.parseColor("#37474F")
                keyTextPaint.color = Color.WHITE
                shiftHighlightPaint.color = Color.parseColor("#4CAF50")
                capsLockPaint.color = Color.parseColor("#FF5722")
                setBackgroundColor(Color.parseColor("#37474F"))
            }
            "colorful" -> {
                keyPaint.color = Color.parseColor("#81D4FA")
                keyBorderPaint.color = Color.parseColor("#4FC3F7")
                keyTextPaint.color = Color.parseColor("#0D47A1")
                shiftHighlightPaint.color = Color.parseColor("#4CAF50")
                capsLockPaint.color = Color.parseColor("#FF9800")
                setBackgroundColor(Color.parseColor("#E1F5FE"))
            }
            else -> { // default - Gboard light theme
                keyPaint.color = ContextCompat.getColor(context, R.color.gboard_key_background)
                keyBorderPaint.color = ContextCompat.getColor(context, R.color.gboard_key_border)
                keyTextPaint.color = ContextCompat.getColor(context, R.color.gboard_key_text)
                shiftHighlightPaint.color = ContextCompat.getColor(context, R.color.shift_active_light)
                capsLockPaint.color = ContextCompat.getColor(context, R.color.shift_caps_light)
                setBackgroundColor(ContextCompat.getColor(context, R.color.gboard_background))
            }
        }
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
        
        // Gboard corner radius - 6dp
        val cornerRadius = 6f
        
        // Draw subtle shadow
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)
        
        // Gboard-style key coloring
        val fillPaint = when {
            isShiftKey && isCapsLockActive -> {
                // Caps lock - prominent highlighting
                Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = when (currentTheme) {
                        "gboard_dark" -> Color.parseColor("#F9AB00") // Google yellow/amber
                        "dark" -> Color.parseColor("#FF9800") // Orange
                        "material_you" -> Color.parseColor("#FFC107") // Amber
                        "professional" -> Color.parseColor("#FF5722") // Deep orange
                        "colorful" -> Color.parseColor("#FF9800") // Orange
                        else -> Color.parseColor("#FEF7E0") // Light amber
                    }
                }
            }
            isShiftKey && isShiftHighlighted -> {
                // Shift active - more visible highlighting
                Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = when (currentTheme) {
                        "gboard_dark" -> ContextCompat.getColor(context, R.color.gboard_dark_key_pressed)
                        "dark" -> Color.parseColor("#4CAF50") // Green highlight
                        "material_you" -> Color.parseColor("#BB86FC") // Purple highlight
                        "professional" -> Color.parseColor("#26A69A") // Teal highlight
                        "colorful" -> Color.parseColor("#FF9800") // Orange highlight
                        else -> ContextCompat.getColor(context, R.color.gboard_key_pressed) // Light blue
                    }
                }
            }
            isActionKey -> {
                // Special keys (shift, delete, etc.) - light gray
                Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = when (currentTheme) {
                        "gboard_dark" -> ContextCompat.getColor(context, R.color.gboard_dark_special_key_background)
                        "dark" -> Color.parseColor("#424242")
                        "material_you" -> Color.parseColor("#9575CD")
                        "professional" -> Color.parseColor("#546E7A")
                        "colorful" -> Color.parseColor("#4FC3F7")
                        else -> ContextCompat.getColor(context, R.color.gboard_special_key_background)
                    }
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
            
            // Create Gboard-style text paint
            val gboardTextPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                this.textSize = textSize
                typeface = Typeface.create("sans-serif", Typeface.BOLD) // Bold for better visibility
                color = when {
                    isSpaceKey -> when (currentTheme) {
                        "gboard_dark" -> ContextCompat.getColor(context, R.color.gboard_dark_secondary_text)
                        else -> ContextCompat.getColor(context, R.color.gboard_secondary_text)
                    }
                    else -> keyTextPaint.color
                }
            }
            
            // Special handling for space bar
            if (isSpaceKey) {
                val spaceText = "English (US)" // Show current language
                val textHeight = gboardTextPaint.descent() - gboardTextPaint.ascent()
                val textOffset = (textHeight / 2) - gboardTextPaint.descent()
                canvas.drawText(spaceText, centerX, centerY + textOffset, gboardTextPaint)
            } else {
                val textHeight = gboardTextPaint.descent() - gboardTextPaint.ascent()
                val textOffset = (textHeight / 2) - gboardTextPaint.descent()
                
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
                
                canvas.drawText(displayText, centerX, centerY + textOffset, gboardTextPaint)
            }
        } else if (key.icon != null) {
            // Gboard-style icon handling
            if (isShiftKey && isCapsLockActive) {
                // Caps lock: Show icon with underline indicator
                val iconSize = 20
                val iconLeft = centerX - iconSize / 2
                val iconTop = centerY - iconSize / 2 - 4  // Move up slightly
                
                // Tint icon with text color for better visibility
                key.icon.setTint(keyTextPaint.color)
                key.icon.setBounds(
                    iconLeft.toInt(),
                    iconTop.toInt(),
                    (iconLeft + iconSize).toInt(),
                    (iconTop + iconSize).toInt()
                )
                key.icon.draw(canvas)
                
                // Draw underline to indicate caps lock
                val underlinePaint = Paint().apply {
                    color = keyTextPaint.color // Use text color for better visibility
                    strokeWidth = 3f // Make it slightly thicker
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
                
                // Tint icon appropriately - make shift highlighting more visible
                if (isShiftKey && isShiftHighlighted) {
                    // Use a more visible color for shift highlighting
                    key.icon.setTint(keyTextPaint.color) // Same as text color for consistency
                } else {
                    key.icon.setTint(keyTextPaint.color)
                }
                
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
        
        // Convert swipe points to key positions
        val swipedKeys = mutableListOf<Int>()
        val swipePattern = generateSwipePattern()
        
        // Get keys that were swiped over
        swipePoints.forEach { point ->
            val keyIndex = keyboard?.let { findKeyAtPoint(point[0].toInt(), point[1].toInt()) } ?: -1
            if (keyIndex >= 0) {
                try {
                    keyboard?.keys?.get(keyIndex)?.codes?.get(0)?.let { keyCode ->
                        if (!swipedKeys.contains(keyCode) && Character.isLetter(keyCode)) {
                            swipedKeys.add(keyCode)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore errors in key detection
                }
            }
        }
        
        swipeListener?.onSwipeDetected(swipedKeys, swipePattern)
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
