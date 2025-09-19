package com.example.ai_keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.setPadding

/**
 * CleverType-style Tone Selector Bottom Sheet
 * Shows tone options in a clean, organized layout
 */
class CleverTypeToneSelector(context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "CleverTypeToneSelector"
        private const val SELECTOR_HEIGHT_DP = 280
        private const val ANIMATION_DURATION = 300L
    }
    
    // UI Components
    private lateinit var headerLayout: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var closeButton: Button
    private lateinit var contentContainer: ScrollView
    private lateinit var toneGrid: LinearLayout
    private lateinit var descriptionText: TextView
    
    // Callbacks
    private var onToneSelected: ((CleverTypeAIService.ToneType) -> Unit)? = null
    private var onClose: (() -> Unit)? = null
    
    // State
    private var isVisible = false
    private var selectedTone: CleverTypeAIService.ToneType? = null
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(SELECTOR_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#ffffff"))
        elevation = dpToPx(12).toFloat()
        visibility = GONE
        
        setupHeader()
        setupContent()
    }
    
    private fun setupHeader() {
        headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(8))
            setBackgroundColor(Color.parseColor("#f8f9fa"))
        }
        
        titleText = TextView(context).apply {
            text = "🎭 Choose Tone"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#202124"))
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        
        closeButton = Button(context).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#5f6368"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
            background = createCircleBackground(Color.parseColor("#e8eaed"))
            setOnClickListener {
                hide()
                onClose?.invoke()
            }
        }
        
        headerLayout.addView(titleText)
        headerLayout.addView(closeButton)
        addView(headerLayout)
    }
    
    private fun setupContent() {
        contentContainer = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        
        val mainContent = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(16))
        }
        
        // Tone grid
        toneGrid = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        
        setupToneGrid()
        
        // Description text
        descriptionText = TextView(context).apply {
            text = "Tap a tone to adjust your text style"
            textSize = 14f
            setTextColor(Color.parseColor("#5f6368"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), 0)
            gravity = Gravity.CENTER
        }
        
        mainContent.addView(toneGrid)
        mainContent.addView(descriptionText)
        
        contentContainer.addView(mainContent)
        addView(contentContainer)
    }
    
    private fun setupToneGrid() {
        val tones = CleverTypeAIService.ToneType.values()
        val buttonsPerRow = 2
        
        for (i in tones.indices step buttonsPerRow) {
            val rowContainer = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                setPadding(0, if (i > 0) dpToPx(12) else 0, 0, 0)
            }
            
            for (j in 0 until buttonsPerRow) {
                val toneIndex = i + j
                if (toneIndex < tones.size) {
                    val tone = tones[toneIndex]
                    val toneButton = createToneButton(tone)
                    
                    val buttonParams = LinearLayout.LayoutParams(0, dpToPx(60), 1f)
                    if (j > 0) buttonParams.setMargins(dpToPx(8), 0, 0, 0)
                    toneButton.layoutParams = buttonParams
                    
                    rowContainer.addView(toneButton)
                } else {
                    // Add spacer for incomplete rows
                    val spacer = View(context)
                    spacer.layoutParams = LinearLayout.LayoutParams(0, dpToPx(60), 1f)
                    rowContainer.addView(spacer)
                }
            }
            
            toneGrid.addView(rowContainer)
        }
    }
    
    private fun createToneButton(tone: CleverTypeAIService.ToneType): LinearLayout {
        val buttonContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            background = createToneButtonBackground(false)
            isClickable = true
            isFocusable = true
            
            setOnClickListener {
                selectTone(tone)
                onToneSelected?.invoke(tone)
                hide()
            }
        }
        
        val emojiText = TextView(context).apply {
            text = tone.emoji
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setPadding(0, 0, dpToPx(12), 0)
        }
        
        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val nameText = TextView(context).apply {
            text = tone.displayName
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#202124"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        
        val descText = TextView(context).apply {
            text = tone.description
            textSize = 12f
            setTextColor(Color.parseColor("#5f6368"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(0, dpToPx(2), 0, 0)
        }
        
        textContainer.addView(nameText)
        textContainer.addView(descText)
        
        buttonContainer.addView(emojiText)
        buttonContainer.addView(textContainer)
        
        return buttonContainer
    }
    
    private fun selectTone(tone: CleverTypeAIService.ToneType) {
        selectedTone = tone
        updateToneButtonSelection()
        
        // Update description
        descriptionText.text = "Selected: ${tone.emoji} ${tone.displayName} - ${tone.description}"
        descriptionText.setTextColor(Color.parseColor("#1a73e8"))
        
        Log.d(TAG, "Tone selected: ${tone.displayName}")
    }
    
    private fun updateToneButtonSelection() {
        // Update visual selection state
        for (i in 0 until toneGrid.childCount) {
            val row = toneGrid.getChildAt(i) as LinearLayout
            for (j in 0 until row.childCount) {
                val child = row.getChildAt(j)
                if (child is LinearLayout && child.childCount > 0) {
                    val isSelected = false // We don't maintain selection state in this simple version
                    child.background = createToneButtonBackground(isSelected)
                }
            }
        }
    }
    
    /**
     * Show the tone selector
     */
    fun show() {
        if (!isVisible) {
            visibility = VISIBLE
            alpha = 0f
            translationY = dpToPx(50).toFloat()
            
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_DURATION)
                .start()
            
            isVisible = true
            Log.d(TAG, "Tone selector shown")
        }
    }
    
    /**
     * Hide the tone selector
     */
    fun hide() {
        if (isVisible) {
            animate()
                .alpha(0f)
                .translationY(dpToPx(50).toFloat())
                .setDuration(ANIMATION_DURATION)
                .withEndAction {
                    visibility = GONE
                    translationY = 0f
                }
                .start()
            
            isVisible = false
            Log.d(TAG, "Tone selector hidden")
        }
    }
    
    /**
     * Set callbacks
     */
    fun setOnToneSelectedListener(listener: (CleverTypeAIService.ToneType) -> Unit) {
        onToneSelected = listener
    }
    
    fun setOnCloseListener(listener: () -> Unit) {
        onClose = listener
    }
    
    /**
     * Create tone button background
     */
    private fun createToneButtonBackground(selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            if (selected) {
                setColor(Color.parseColor("#e3f2fd"))
                setStroke(dpToPx(2), Color.parseColor("#1a73e8"))
            } else {
                setColor(Color.parseColor("#f8f9fa"))
                setStroke(dpToPx(1), Color.parseColor("#e8eaed"))
            }
            cornerRadius = dpToPx(12).toFloat()
        }
    }
    
    /**
     * Create circle background drawable
     */
    private fun createCircleBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            shape = GradientDrawable.OVAL
        }
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
