package com.example.ai_keyboard

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlinx.coroutines.*

/**
 * AI Features Panel for intelligent writing assistance
 */
class AIFeaturesPanel(context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "AIFeaturesPanel"
        private const val PANEL_HEIGHT_DP = 280
    }
    
    private val advancedAIService = AdvancedAIService(context)
    private val streamingAIService = StreamingAIService(context)
    private val customToneManager = CustomToneManager(context)
    // toneAdjustmentPanel removed - using normal tone selector instead
    private lateinit var toneSelector: LinearLayout
    private var selectedTone: AdvancedAIService.ToneType = AdvancedAIService.ToneType.FORMAL
    // customToneCreator removed - keeping interface simple
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // UI Components
    private lateinit var headerLayout: LinearLayout
    private lateinit var contentContainer: ScrollView
    private lateinit var textInputContainer: LinearLayout
    private lateinit var textInputField: EditText
    private lateinit var featuresGrid: LinearLayout
    private lateinit var smartRepliesContainer: LinearLayout
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var statusText: TextView
    // previewModal removed - using simple direct application instead
    
    // Callbacks
    private var onTextProcessed: ((String) -> Unit)? = null
    private var onSmartReplySelected: ((String) -> Unit)? = null
    private var currentInputText: String = ""
    
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PANEL_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#f8f9fa"))
        
        setupHeader()
        setupContent()
        setupLoadingIndicator()
        
        Log.d(TAG, "AI Features Panel initialized")
    }
    
    private fun setupHeader() {
        headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(50))
            setBackgroundColor(Color.parseColor("#ffffff"))
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            elevation = dpToPx(2).toFloat()
        }
        
        val titleText = TextView(context).apply {
            text = "🤖 AI Writing Assistant"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1a73e8"))
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val settingsButton = TextView(context).apply {
            text = "⚙️"
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.emoji_touch_feedback)
            setOnClickListener {
                showAISettings()
            }
        }
        
        headerLayout.addView(titleText)
        headerLayout.addView(settingsButton)
        addView(headerLayout)
    }
    
    private fun setupContent() {
        contentContainer = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        
        val mainContent = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }
        
        // Status text
        statusText = TextView(context).apply {
            text = "Select text to enhance with AI"
            textSize = 14f
            setTextColor(Color.parseColor("#5f6368"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(16))
        }
        
        // Text Input Section
        setupTextInput()
        
        // AI Features Grid
        setupFeaturesGrid()
        
        // Compact Tone Selector
        setupToneSelector()
        
        // Tone Adjustment Section - removed (using normal tone selector instead)
        
        // Smart Replies Section
        setupSmartReplies()
        
        mainContent.addView(statusText)
        mainContent.addView(textInputContainer)
        mainContent.addView(createSectionDivider())
        // Tone selector is now added in setupToneSelector()
        mainContent.addView(createSectionDivider())
        mainContent.addView(featuresGrid)
        mainContent.addView(createSectionDivider())
        mainContent.addView(smartRepliesContainer)
        
        contentContainer.addView(mainContent)
        addView(contentContainer)
    }
    
    private fun setupTextInput() {
        textInputContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(16))
            setBackgroundResource(R.drawable.input_text_background)
        }
        
        val inputLabel = TextView(context).apply {
            text = "📝 Text to Enhance"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1a73e8"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(4), 0, dpToPx(4), dpToPx(8))
        }
        
        textInputField = EditText(context).apply {
            hint = "Type or paste text here, or it will be auto-filled from your current input..."
            textSize = 14f
            setTextColor(Color.parseColor("#202124"))
            setHintTextColor(Color.parseColor("#9aa0a6"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(80))
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setBackgroundColor(Color.TRANSPARENT)
            maxLines = 3
            setVerticalScrollBarEnabled(true)
            
            // Update current input text when user types
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val newText = s?.toString() ?: ""
                    if (newText != currentInputText) {
                        currentInputText = newText
                        // toneAdjustmentPanel removed - using normal tone selector instead
                        updateStatusText(newText)
                    }
                }
            })
        }
        
        val buttonRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(0, dpToPx(8), 0, 0)
        }
        
        val clearButton = TextView(context).apply {
            text = "🗑️ Clear"
            textSize = 12f
            setTextColor(Color.parseColor("#ea4335"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(32), 1f)
                setBackgroundColor(Color.parseColor("#f8f9fa"))
            setOnClickListener {
                textInputField.setText("")
                currentInputText = ""
                statusText.text = "Enter text to enhance with AI"
                statusText.setTextColor(Color.parseColor("#5f6368"))
            }
        }
        
        val pasteButton = TextView(context).apply {
            text = "📋 Paste"
            textSize = 12f
            setTextColor(Color.parseColor("#1a73e8"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(32), 1f).apply {
                setMargins(dpToPx(8), 0, 0, 0)
            }
                setBackgroundColor(Color.parseColor("#1a73e8"))
            setOnClickListener {
                // Get clipboard text
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                    if (clipText.isNotEmpty()) {
                        textInputField.setText(clipText)
                        textInputField.setSelection(clipText.length)
                    }
                } else {
                    Toast.makeText(context, "No text in clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        buttonRow.addView(clearButton)
        buttonRow.addView(pasteButton)
        
        textInputContainer.addView(inputLabel)
        textInputContainer.addView(textInputField)
        textInputContainer.addView(buttonRow)
    }
    
    
    private fun setupToneSelector() {
        // Create tone selector section
        val toneSectionLabel = TextView(context).apply {
            text = "🎭 Tone Adjustment"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1a73e8"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(8))
        }
        
        // Create tone selector container
        toneSelector = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), 0, dpToPx(16), dpToPx(16))
            setBackgroundResource(R.drawable.input_text_background)
        }
        
        // Create tone buttons grid (2 rows, 4 columns)
        val toneTypes = AdvancedAIService.ToneType.values()
        val buttonsPerRow = 4
        
        for (i in toneTypes.indices step buttonsPerRow) {
            val rowContainer = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                setPadding(0, if (i > 0) dpToPx(8) else 0, 0, 0)
            }
            
            for (j in 0 until buttonsPerRow) {
                val toneIndex = i + j
                if (toneIndex < toneTypes.size) {
                    val tone = toneTypes[toneIndex]
                    val toneButton = createToneButton(tone)
                    
                    val buttonParams = LinearLayout.LayoutParams(0, dpToPx(45), 1f)
                    if (j > 0) buttonParams.setMargins(dpToPx(4), 0, 0, 0)
                    toneButton.layoutParams = buttonParams
                    
                    rowContainer.addView(toneButton)
                } else {
                    // Add spacer for incomplete rows
                    val spacer = View(context)
                    spacer.layoutParams = LinearLayout.LayoutParams(0, dpToPx(45), 1f)
                    rowContainer.addView(spacer)
                }
            }
            
            toneSelector.addView(rowContainer)
        }
        
        // Find mainContent and add tone selector components to it
        val mainContent = (contentContainer.getChildAt(0) as? LinearLayout)
        mainContent?.let {
            it.addView(createSectionDivider())
            it.addView(toneSectionLabel)
            it.addView(toneSelector)
        }
    }
    
    /**
     * Create a tone button
     */
    private fun createToneButton(tone: AdvancedAIService.ToneType): TextView {
        return TextView(context).apply {
            text = "${tone.icon} ${tone.displayName}"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(tone.color))
            
            // Add rounded corners
            background = context.getDrawable(R.drawable.key_background_default)?.apply {
                setTint(Color.parseColor(tone.color))
            }
            
            // Add click listener
            setOnClickListener {
                selectTone(tone)
                processToneAdjustment(tone)
            }
            
            // Add ripple effect
            isClickable = true
            isFocusable = true
        }
    }
    
    /**
     * Select a tone and update UI
     */
    private fun selectTone(tone: AdvancedAIService.ToneType) {
        selectedTone = tone
        updateToneButtonSelection()
        Log.d(TAG, "Tone selected: ${tone.displayName}")
    }
    
    /**
     * Update tone button selection visual state
     */
    private fun updateToneButtonSelection() {
        // Update all tone buttons to reflect current selection
        val toneTypes = AdvancedAIService.ToneType.values()
        
        for (i in 0 until toneSelector.childCount) {
            val rowContainer = toneSelector.getChildAt(i) as LinearLayout
            for (j in 0 until rowContainer.childCount) {
                val button = rowContainer.getChildAt(j)
                if (button is TextView && button.text.isNotEmpty()) {
                    val toneIndex = i * 4 + j
                    if (toneIndex < toneTypes.size) {
                        val tone = toneTypes[toneIndex]
                        val isSelected = tone == selectedTone
                        
                        button.alpha = if (isSelected) 1.0f else 0.7f
                        button.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                    }
                }
            }
        }
    }
    
    /**
     * Process tone adjustment
     */
    private fun processToneAdjustment(tone: AdvancedAIService.ToneType) {
        if (currentInputText.isBlank()) {
            statusText.text = "⚠️ Please enter some text to adjust tone"
            statusText.setTextColor(Color.parseColor("#fbbc04"))
            return
        }
        
        showLoading("Adjusting tone to ${tone.displayName}...")
        
        scope.launch {
            try {
                val result = advancedAIService.adjustTone(currentInputText, tone)
                
                if (result.success) {
                    showSimplePreview(result.text, tone.displayName)
                    
                    val cacheInfo = if (result.fromCache) " ⚡ cached" else ""
                    statusText.text = "✅ ${tone.displayName} tone ready • ${result.processingTimeMs}ms$cacheInfo"
                    statusText.setTextColor(Color.parseColor("#34a853"))
                } else {
                    statusText.text = "❌ Error: ${result.error}"
                    statusText.setTextColor(Color.parseColor("#ea4335"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in tone adjustment", e)
                statusText.text = "❌ Tone adjustment failed: ${e.message}"
                statusText.setTextColor(Color.parseColor("#ea4335"))
            } finally {
                hideLoading()
            }
        }
    }
    
    // Custom tone creator methods removed - keeping interface simple
    
    private fun showAISettings() {
        val config = OpenAIConfig.getInstance(context)
        
        // Create settings dialog
        val options = arrayOf(
            "🔑 Reinitialize API Key",
            "📊 Show API Status", 
            "🧪 Test Connection",
            "🗑️ Clear Cache",
            "❌ Cancel"
        )
        
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("AI Settings")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Reinitialize API Key
                        config.forceReinitializeApiKey()
                        Toast.makeText(context, "API Key reinitialized", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        // Show API Status
                        val hasKey = config.hasApiKey()
                        val isEnabled = config.isAIFeaturesEnabled()
                        val authHeader = config.getAuthorizationHeader()
                        
                        val status = """
                            API Key Configured: ${if (hasKey) "✅ Yes" else "❌ No"}
                            AI Features Enabled: ${if (isEnabled) "✅ Yes" else "❌ No"}
                            Authorization Ready: ${if (authHeader != null) "✅ Yes" else "❌ No"}
                        """.trimIndent()
                        
                        Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                    }
                    2 -> {
                        // Test Connection
                        testConnectionWithApiValidation()
                    }
                    3 -> {
                        // Clear Cache
                        advancedAIService.clearCache()
                        streamingAIService.cleanup()
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                    4 -> {
                        // Cancel - do nothing
                        dialog.dismiss()
                    }
                }
            }
        
        try {
            builder.show()
        } catch (e: Exception) {
            // Fallback for system keyboard context issues
            Log.e(TAG, "Could not show settings dialog", e)
            
            // Just reinitialize API key as fallback
            config.forceReinitializeApiKey()
            Toast.makeText(context, "API Key reinitialized (fallback)", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupFeaturesGrid() {
        featuresGrid = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        
        val sectionTitle = TextView(context).apply {
            text = "✨ Text Enhancement"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#202124"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(12))
        }
        featuresGrid.addView(sectionTitle)
        
        val features = advancedAIService.getAvailableFeatures()
        createFeatureButtons(features)
    }
    
    private fun createFeatureButtons(features: List<AdvancedAIService.ProcessingFeature>) {
        var currentRow: LinearLayout? = null
        
        features.forEachIndexed { index, feature ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                }
                featuresGrid.addView(currentRow)
            }
            
            val featureButton = createFeatureButton(feature)
            currentRow?.addView(featureButton)
        }
    }
    
    private fun createFeatureButton(feature: AdvancedAIService.ProcessingFeature): Button {
        return Button(context).apply {
            text = feature.displayName
            textSize = 13f
            setTextColor(Color.parseColor("#1a73e8"))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f).apply {
                setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            }
            setBackgroundResource(R.drawable.bottom_button_background)
            
            setOnClickListener {
                if (currentInputText.isNotEmpty()) {
                    processTextWithAI(feature)
                } else {
                    Toast.makeText(context, "Please enter some text first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // setupToneAdjustment removed - using normal tone selector instead
    
    private fun createSectionDivider(): View {
        return View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1))
            setBackgroundColor(Color.parseColor("#e8eaed"))
        }
    }
    
    private fun setupSmartReplies() {
        smartRepliesContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(0), dpToPx(16), dpToPx(0), dpToPx(0))
        }
        
        val sectionTitle = TextView(context).apply {
            text = "💬 Smart Replies"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#202124"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(12))
        }
        smartRepliesContainer.addView(sectionTitle)
        
        val generateButton = Button(context).apply {
            text = "Generate Smart Replies"
            textSize = 14f
            setTextColor(Color.parseColor("#ffffff"))
            setBackgroundColor(Color.parseColor("#1a73e8"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(44))
            
            setOnClickListener {
                if (currentInputText.isNotEmpty()) {
                    generateSmartReplies()
                } else {
                    Toast.makeText(context, "Please enter a message to reply to", Toast.LENGTH_SHORT).show()
                }
            }
        }
        smartRepliesContainer.addView(generateButton)
    }
    
    private fun setupLoadingIndicator() {
        loadingIndicator = ProgressBar(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            visibility = GONE
        }
        addView(loadingIndicator)
    }
    
    private fun processTextWithAI(feature: AdvancedAIService.ProcessingFeature) {
        if (currentInputText.isBlank()) {
            statusText.text = "⚠️ Please enter some text to process"
            statusText.setTextColor(Color.parseColor("#fbbc04"))
            return
        }
        
        showLoading("Processing with AI...")
        
        // Use streaming for better perceived performance
        scope.launch {
            try {
                streamingAIService.processFeatureStreaming(currentInputText, feature)
                        .collect { streamingResult ->
                            if (streamingResult.error != null) {
                                // Check if it's an API key error and try to fix it
                                if (streamingResult.error.contains("API key", ignoreCase = true)) {
                                    Log.w(TAG, "API key error detected, attempting to reinitialize")
                                    val config = OpenAIConfig.getInstance(context)
                                    config.forceReinitializeApiKey()
                                    statusText.text = "🔑 API key reinitialized. Please try again."
                                    statusText.setTextColor(Color.parseColor("#fbbc04"))
                                } else if (streamingResult.error.contains("Rate limit", ignoreCase = true)) {
                                    // Handle rate limiting with user-friendly message
                                    val waitTime = extractWaitTimeFromError(streamingResult.error)
                                    statusText.text = "⏳ Rate limit: Wait ${waitTime}s before next request"
                                    statusText.setTextColor(Color.parseColor("#fbbc04"))
                                    Log.w(TAG, "Rate limit hit: ${streamingResult.error}")
                                } else {
                                    statusText.text = "❌ Error: ${streamingResult.error}"
                                    statusText.setTextColor(Color.parseColor("#ea4335"))
                                }
                                hideLoading()
                        } else if (streamingResult.isComplete) {
                            // Show final preview modal
                            showSimplePreview(streamingResult.fullText, feature.displayName)
                            
                            val cacheInfo = if (streamingResult.fromCache) " ⚡ cached" else ""
                            statusText.text = "✅ Preview ready • ${streamingResult.processingTimeMs}ms$cacheInfo"
                            statusText.setTextColor(Color.parseColor("#34a853"))
                            hideLoading()
                        } else {
                            // Show streaming progress
                            statusText.text = "🔄 Processing... ${streamingResult.currentText.length} chars"
                            statusText.setTextColor(Color.parseColor("#1a73e8"))
                        }
                    }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in streaming processing", e)
                statusText.text = "❌ Processing failed: ${e.message}"
                statusText.setTextColor(Color.parseColor("#ea4335"))
                hideLoading()
            }
        }
    }
    
    private fun generateSmartReplies() {
        showLoading("Generating smart replies...")
        
        scope.launch {
            try {
                val result = advancedAIService.generateSmartReplies(currentInputText)
                
                if (result.success) {
                    val replies = result.text.split("\n")
                        .map { it.trim() }
                        .filter { it.startsWith("•") }
                        .map { it.removePrefix("•").trim() }
                        .filter { it.isNotEmpty() }
                    
                    displaySmartReplies(replies)
                    val cacheInfo = if (result.fromCache) " (cached)" else ""
                    statusText.text = "✅ Smart replies generated$cacheInfo • ${result.processingTimeMs}ms"
                    statusText.setTextColor(Color.parseColor("#34a853"))
                } else {
                    statusText.text = "❌ Error: ${result.error}"
                    statusText.setTextColor(Color.parseColor("#ea4335"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating smart replies", e)
                statusText.text = "❌ Generation failed: ${e.message}"
                statusText.setTextColor(Color.parseColor("#ea4335"))
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun displaySmartReplies(replies: List<String>) {
        // Remove existing reply buttons
        val childCount = smartRepliesContainer.childCount
        if (childCount > 2) {
            smartRepliesContainer.removeViews(2, childCount - 2)
        }
        
        replies.forEach { reply ->
            val replyButton = Button(context).apply {
                text = reply
                textSize = 13f
                setTextColor(Color.parseColor("#1a73e8"))
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dpToPx(0), dpToPx(4), dpToPx(0), dpToPx(4))
                }
                setBackgroundResource(R.drawable.key_background_default)
                
                setOnClickListener {
                    onSmartReplySelected?.invoke(reply)
                }
            }
            smartRepliesContainer.addView(replyButton)
        }
    }
    
    private fun showLoading(message: String) {
        statusText.text = message
        statusText.setTextColor(Color.parseColor("#1a73e8"))
        loadingIndicator.visibility = VISIBLE
        contentContainer.alpha = 0.5f
    }
    
    private fun hideLoading() {
        loadingIndicator.visibility = GONE
        contentContainer.alpha = 1.0f
    }
    
    /**
     * Show preview modal with original vs processed text
     */
    /**
     * Show simple inline preview instead of complex modal
     */
    private fun showSimplePreview(processedText: String, featureName: String) {
        try {
            // Just apply the processed text directly for simplicity
            onTextProcessed?.invoke(processedText)
            statusText.text = "✅ $featureName applied successfully"
            statusText.setTextColor(Color.parseColor("#34a853"))
            Log.d(TAG, "$featureName applied: $processedText")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying $featureName", e)
            statusText.text = "❌ Error applying $featureName"
            statusText.setTextColor(Color.parseColor("#ea4335"))
        }
    }
    
    /**
     * Remove preview modal and restore main content
     */
    // removePreviewModal method removed - using simple direct application instead
    
    /**
     * Set the input text for AI processing
     */
    fun setInputText(text: String) {
        currentInputText = text
        
        // Update the text input field
        if (::textInputField.isInitialized) {
            textInputField.setText(text)
            if (text.isNotEmpty()) {
                textInputField.setSelection(text.length) // Move cursor to end
            }
        }
        
        // Update status text based on input
        if (text.isNotEmpty()) {
            updateStatusText(text)
        } else {
            statusText.text = "Enter text to enhance with AI"
            statusText.setTextColor(Color.parseColor("#5f6368"))
        }
        
        // Tone adjustment panel removed - using normal tone selector instead
        
        // Update status text
        updateStatusText(text)
    }
    
    /**
     * Set callback for processed text
     */
    fun setOnTextProcessedListener(listener: (String) -> Unit) {
        onTextProcessed = listener
    }
    
    /**
     * Set callback for smart reply selection
     */
    fun setOnSmartReplySelectedListener(listener: (String) -> Unit) {
        onSmartReplySelected = listener
    }
    
    /**
     * Test AI connectivity
     */
    fun testConnection() {
        showLoading("Testing AI connection...")
        
        scope.launch {
            try {
                val result = advancedAIService.testConnection()
                
                if (result.success) {
                    statusText.text = "✅ AI connection successful • ${result.processingTimeMs}ms"
                    statusText.setTextColor(Color.parseColor("#34a853"))
                } else {
                    statusText.text = "❌ Connection error: ${result.error}"
                    statusText.setTextColor(Color.parseColor("#ea4335"))
                }
                
            } catch (e: Exception) {
                statusText.text = "❌ Connection test failed: ${e.message}"
                statusText.setTextColor(Color.parseColor("#ea4335"))
            } finally {
                hideLoading()
            }
        }
    }
    
    /**
     * Test connection with API key validation
     */
    private fun testConnectionWithApiValidation() {
        showLoading("Testing API key and connection...")
        
        scope.launch {
            try {
                val config = OpenAIConfig.getInstance(context)
                
                // First test the API key directly
                val apiKeyValid = config.testApiKey()
                
                if (apiKeyValid) {
                    // If API key is valid, test the AI service
                    val result = advancedAIService.testConnection()
                    
                    if (result.success) {
                        statusText.text = "✅ API key and connection successful • ${result.processingTimeMs}ms"
                        statusText.setTextColor(Color.parseColor("#34a853"))
                        Toast.makeText(context, "🎉 Everything is working perfectly!", Toast.LENGTH_LONG).show()
                    } else {
                        statusText.text = "✅ API key valid, but service error: ${result.error}"
                        statusText.setTextColor(Color.parseColor("#fbbc04"))
                    }
                } else {
                    statusText.text = "❌ API key validation failed"
                    statusText.setTextColor(Color.parseColor("#ea4335"))
                    
                    // Try to reinitialize automatically
                    Log.w(TAG, "API key invalid, attempting automatic fix...")
                    config.forceReinitializeApiKey()
                    
                    // Test again after reinitialization
                    val retestValid = config.testApiKey()
                    if (retestValid) {
                        statusText.text = "✅ API key fixed and validated!"
                        statusText.setTextColor(Color.parseColor("#34a853"))
                        Toast.makeText(context, "🔑 API key automatically fixed!", Toast.LENGTH_LONG).show()
                    } else {
                        statusText.text = "❌ API key still invalid after reset"
                        statusText.setTextColor(Color.parseColor("#ea4335"))
                        Toast.makeText(context, "⚠️ Please check your OpenAI API key", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in API validation test", e)
                statusText.text = "❌ Test failed: ${e.message}"
                statusText.setTextColor(Color.parseColor("#ea4335"))
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    /**
     * Extract wait time from rate limit error message
     */
    private fun extractWaitTimeFromError(error: String): String {
        // Try to extract number from "wait 30s" or "wait 60s" etc.
        val waitTimeRegex = """wait (\d+)s""".toRegex(RegexOption.IGNORE_CASE)
        val match = waitTimeRegex.find(error)
        return match?.groupValues?.get(1) ?: "60"
    }
    
    /**
     * Update status text with input information
     */
    private fun updateStatusText(text: String) {
        val wordCount = text.split("\\s+".toRegex()).size
        val charCount = text.length
        statusText.text = "📝 Ready: $wordCount words, $charCount chars • Select tone to enhance"
        statusText.setTextColor(Color.parseColor("#1a73e8"))
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        advancedAIService.cleanup()
        streamingAIService.cleanup()
        // toneAdjustmentPanel cleanup removed - using normal tone selector instead
        
        // Cleanup compact tone selector
        // Tone selector cleanup is handled by the parent scope
        
        // Custom tone creator cleanup removed - keeping interface simple
    }
}
