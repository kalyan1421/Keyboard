package com.example.ai_keyboard

import android.content.Context
import android.graphics.*
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.pow
import kotlin.math.sqrt

class SwipeKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {
    
    private var swipeEnabled = true
    private var isSwipeInProgress = false
    private val swipePoints = mutableListOf<FloatArray>()
    private var swipePaint: Paint
    private var swipePath: Path
    private var swipeListener: SwipeListener? = null
    private var swipeStartTime = 0L
    
    companion object {
        private const val MIN_SWIPE_TIME = 300L // Minimum time for swipe (ms)
        private const val MIN_SWIPE_DISTANCE = 100f // Minimum distance for swipe (pixels)
    }
    
    interface SwipeListener {
        fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String)
        fun onSwipeStarted()
        fun onSwipeEnded()
    }
    
    init {
        swipePaint = Paint().apply {
            color = Color.parseColor("#2196F3")
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
            alpha = 180
        }
        swipePath = Path()
    }
    
    fun setSwipeEnabled(enabled: Boolean) {
        swipeEnabled = enabled
    }
    
    fun setSwipeListener(listener: SwipeListener?) {
        swipeListener = listener
    }
    
    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (!swipeEnabled) {
            return super.onTouchEvent(me)
        }
        
        val handled = handleSwipeTouch(me)
        return if (handled) true else super.onTouchEvent(me)
    }
    
    private fun handleSwipeTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startSwipe(x, y)
                return false // Let normal key press handling occur
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isSwipeInProgress) {
                    continueSwipe(x, y)
                    return true // Consume the event
                } else {
                    // Check if user has moved enough to start swipe
                    if (swipePoints.isNotEmpty()) {
                        val startPoint = swipePoints[0]
                        val distance = sqrt(
                            (x - startPoint[0]).pow(2) + (y - startPoint[1]).pow(2)
                        )
                        
                        if (distance > 50) { // Start swipe if moved 50 pixels
                            isSwipeInProgress = true
                            swipeListener?.onSwipeStarted()
                            continueSwipe(x, y)
                            return true
                        }
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isSwipeInProgress) {
                    endSwipe(x, y)
                    return true
                } else {
                    // Reset swipe data for normal key press
                    resetSwipe()
                    return false
                }
            }
            
            MotionEvent.ACTION_CANCEL -> {
                resetSwipe()
                return isSwipeInProgress
            }
        }
        
        return false
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
        for (point in swipePoints) {
            val keyIndex = findKeyAtPoint(point[0].toInt(), point[1].toInt())
            if (keyIndex >= 0 && keyboard != null) {
                try {
                    val keyCode = keyboard.keys[keyIndex].codes[0]
                    if (!swipedKeys.contains(keyCode) && Character.isLetter(keyCode)) {
                        swipedKeys.add(keyCode)
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
        return if (kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)) {
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
    
    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw swipe path if swiping
        if (isSwipeInProgress && !swipePath.isEmpty) {
            canvas.drawPath(swipePath, swipePaint)
        }
    }
    
    private fun findKeyAtPoint(x: Int, y: Int): Int {
        if (keyboard == null) return -1
        
        return try {
            val keys = keyboard.keys
            keys.indices.find { i ->
                val key = keys[i]
                x >= key.x && x < key.x + key.width && 
                y >= key.y && y < key.y + key.height
            } ?: -1
        } catch (e: Exception) {
            // Ignore errors in key detection
            -1
        }
    }
}
