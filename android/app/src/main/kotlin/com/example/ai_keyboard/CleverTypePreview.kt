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
 * CleverType-style Preview System
 * Shows Before/After text comparison with Apply/Cancel actions
 */
class CleverTypePreview(context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "CleverTypePreview"
        private const val PREVIEW_HEIGHT_DP = 200
        private const val ANIMATION_DURATION = 250L
    }
    
    // UI Components
    private lateinit var headerLayout: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var closeButton: Button
    private lateinit var contentContainer: ScrollView
    private lateinit var beforeContainer: LinearLayout
    private lateinit var beforeLabel: TextView
    private lateinit var beforeText: TextView
    private lateinit var afterContainer: LinearLayout
    private lateinit var afterLabel: TextView
    private lateinit var afterText: TextView
    private lateinit var actionButtonsLayout: LinearLayout
    private lateinit var cancelButton: Button
    private lateinit var applyButton: Button
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var statusText: TextView
    private var loadingContainer: LinearLayout? = null
    
    // Callbacks
    private var onApply: ((String) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    private var onClose: (() -> Unit)? = null
    
    // State
    private var isVisible = false
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PREVIEW_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#ffffff"))
        elevation = dpToPx(8).toFloat()
        visibility = GONE
        
        setupHeader()
        setupContent()
        setupActionButtons()
        setupLoadingIndicator()
    }
    
    private fun setupHeader() {
        headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(8))
            setBackgroundColor(Color.parseColor("#f8f9fa"))
        }
        
        titleText = TextView(context).apply {
            text = "Preview Changes"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#202124"))
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        
        closeButton = Button(context).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#5f6368"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
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
            setPadding(dpToPx(16))
        }
        
        // Before section
        beforeContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            background = createRoundedBackground(Color.parseColor("#fef7e0"))
        }
        
        beforeLabel = TextView(context).apply {
            text = "📝 Before"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#f57f17"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(0, 0, 0, dpToPx(4))
        }
        
        beforeText = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#5f6368"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            background = createRoundedBackground(Color.parseColor("#ffffff"))
        }
        
        beforeContainer.addView(beforeLabel)
        beforeContainer.addView(beforeText)
        
        // Spacer
        val spacer = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(12))
        }
        
        // After section
        afterContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            background = createRoundedBackground(Color.parseColor("#e8f5e8"))
        }
        
        afterLabel = TextView(context).apply {
            text = "✨ After"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#2e7d32"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(0, 0, 0, dpToPx(4))
        }
        
        afterText = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#202124"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            background = createRoundedBackground(Color.parseColor("#ffffff"))
        }
        
        afterContainer.addView(afterLabel)
        afterContainer.addView(afterText)
        
        mainContent.addView(beforeContainer)
        mainContent.addView(spacer)
        mainContent.addView(afterContainer)
        
        contentContainer.addView(mainContent)
        addView(contentContainer)
    }
    
    private fun setupActionButtons() {
        actionButtonsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(12))
            setBackgroundColor(Color.parseColor("#f8f9fa"))
        }
        
        cancelButton = Button(context).apply {
            text = "❌ Cancel"
            textSize = 14f
            setTextColor(Color.parseColor("#ea4335"))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f).apply {
                setMargins(0, 0, dpToPx(8), 0)
            }
            background = createRoundedBackground(Color.parseColor("#fce8e6"))
            setOnClickListener {
                hide()
                onCancel?.invoke()
            }
        }
        
        applyButton = Button(context).apply {
            text = "✅ Apply Changes"
            textSize = 14f
            setTextColor(Color.parseColor("#ffffff"))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 2f)
            background = createRoundedBackground(Color.parseColor("#1a73e8"))
            setOnClickListener {
                val textToApply = afterText.text.toString()
                hide()
                onApply?.invoke(textToApply)
            }
        }
        
        actionButtonsLayout.addView(cancelButton)
        actionButtonsLayout.addView(applyButton)
        addView(actionButtonsLayout)
    }
    
    private fun setupLoadingIndicator() {
        loadingContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            gravity = Gravity.CENTER
            visibility = GONE
        }
        
        loadingIndicator = ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20))
        }
        
        statusText = TextView(context).apply {
            text = "Processing..."
            textSize = 14f
            setTextColor(Color.parseColor("#1a73e8"))
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(8), 0, 0, 0)
        }
        
        loadingContainer?.addView(loadingIndicator)
        loadingContainer?.addView(statusText)
        loadingContainer?.let { addView(it) }
    }
    
    /**
     * Show grammar correction preview
     */
    fun showGrammarPreview(result: CleverTypeAIService.GrammarResult) {
        titleText.text = if (result.hasChanges) "Grammar & Spelling Fixed" else "No Changes Needed"
        beforeLabel.text = "📝 Original"
        afterLabel.text = if (result.hasChanges) "✅ Corrected" else "✅ Already Correct"
        
        beforeText.text = result.originalText
        afterText.text = result.correctedText
        
        // Enable/disable apply button based on whether there are changes
        applyButton.isEnabled = result.hasChanges
        applyButton.alpha = if (result.hasChanges) 1.0f else 0.5f
        applyButton.text = if (result.hasChanges) "✅ Apply Corrections" else "✅ No Changes"
        
        show()
        
        Log.d(TAG, "Grammar preview shown - hasChanges: ${result.hasChanges}")
    }
    
    /**
     * Show tone adjustment preview
     */
    fun showTonePreview(result: CleverTypeAIService.ToneResult) {
        titleText.text = "${result.tone.emoji} ${result.tone.displayName} Tone"
        beforeLabel.text = "📝 Original"
        afterLabel.text = "✨ ${result.tone.displayName}"
        
        beforeText.text = result.originalText
        afterText.text = result.adjustedText
        
        applyButton.isEnabled = true
        applyButton.alpha = 1.0f
        applyButton.text = "✅ Apply ${result.tone.displayName}"
        
        show()
        
        Log.d(TAG, "Tone preview shown - tone: ${result.tone.displayName}")
    }
    
    /**
     * Show loading state
     */
    fun showLoading(message: String = "Processing...") {
        statusText.text = message
        contentContainer.visibility = GONE
        actionButtonsLayout.visibility = GONE
        loadingContainer?.visibility = VISIBLE
        
        show()
    }
    
    /**
     * Hide loading and show content
     */
    fun hideLoading() {
        loadingContainer?.visibility = GONE
        contentContainer.visibility = VISIBLE
        actionButtonsLayout.visibility = VISIBLE
    }
    
    /**
     * Show the preview
     */
    private fun show() {
        if (!isVisible) {
            visibility = VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION)
                .start()
            isVisible = true
            Log.d(TAG, "Preview shown")
        }
    }
    
    /**
     * Hide the preview
     */
    fun hide() {
        if (isVisible) {
            animate()
                .alpha(0f)
                .setDuration(ANIMATION_DURATION)
                .withEndAction {
                    visibility = GONE
                }
                .start()
            isVisible = false
            Log.d(TAG, "Preview hidden")
        }
    }
    
    /**
     * Set callbacks
     */
    fun setOnApplyListener(listener: (String) -> Unit) {
        onApply = listener
    }
    
    fun setOnCancelListener(listener: () -> Unit) {
        onCancel = listener
    }
    
    fun setOnCloseListener(listener: () -> Unit) {
        onClose = listener
    }
    
    /**
     * Create rounded background drawable
     */
    private fun createRoundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dpToPx(8).toFloat()
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
