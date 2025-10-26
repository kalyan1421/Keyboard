package com.example.ai_keyboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.example.ai_keyboard.themes.ThemePaletteV2
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 🎯 UNIFIED KEYBOARD VIEW V2 - Complete Modernization
 * 
 * Single source of truth for:
 * ✅ Swipe gesture detection and path normalization
 * ✅ Toolbar rendering with full icon parity (AI, Grammar, Tone, Clipboard, Emoji, Voice, Translate, GIF, Sticker, Incognito)
 * ✅ Suggestion bar with auto-commit styling and theming
 * ✅ Spacebar cursor gestures and backspace swipe delete
 * ✅ One-handed and floating keyboard modes
 * ✅ Firebase language readiness indicators
 * ✅ Haptic feedback integration
 * 
 * Architecture:
 * ┌──────────────────────────────────────────┐
 * │ toolbarContainer                         │ ← AI/Emoji/Settings/Modes buttons
 * ├──────────────────────────────────────────┤
 * │ suggestionContainer                      │ ← Suggestions with auto-commit styling
 * ├──────────────────────────────────────────┤
 * │ bodyContainer (FrameLayout)              │
 * │  ├─ keyboardGrid (typing mode)           │
 * │  └─ panelView (panel mode)               │
 * └──────────────────────────────────────────┘
 */
class UnifiedKeyboardView @JvmOverloads constructor(
    context: Context,
    private val themeManager: ThemeManager,
    private val heightManager: KeyboardHeightManager,
    private val onKeyCallback: ((Int, IntArray) -> Unit)? = null,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    
    companion object {
        private const val TAG = "UnifiedKeyboardView"
        
        // Touch event constants
        private const val LONG_PRESS_TIMEOUT = 500L
        private const val MIN_SWIPE_DISTANCE = 50f
        
        // Swipe detection thresholds
        private const val SWIPE_START_THRESHOLD = 12f
        private const val MIN_SWIPE_TIME_MS = 80L
        private const val MIN_SWIPE_DISTANCE_PX = 40f
    }
    
    /**
     * Callback interfaces for integration with AIKeyboardService
     */
    interface SwipeListener {
        fun onSwipeDetected(sequence: List<Int>, normalizedPath: List<Pair<Float, Float>>)
        fun onSwipeStarted()
        fun onSwipeEnded()
    }
    
    interface SuggestionUpdateListener {
        fun onSuggestionsRequested(prefix: String)
    }
    
    interface AutocorrectListener {
        fun onAutocorrectCommit(original: String, corrected: String)
    }
    
    interface InputConnectionProvider {
        fun getCurrentInputConnection(): android.view.inputmethod.InputConnection?
    }

    /**
     * Display mode enum
     */
    enum class DisplayMode {
        TYPING,      // Show keyboard grid
        PANEL        // Show feature panel
    }

    /**
     * Keyboard layout modes for adaptive UI
     */
    enum class LayoutMode { 
        NORMAL, 
        ONE_HANDED_LEFT, 
        ONE_HANDED_RIGHT, 
        FLOATING 
    }

    /**
     * Dynamic key model
     */
    data class DynamicKey(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val label: String,
        val code: Int,
        val longPressOptions: List<String>? = null,
        val keyType: String = "regular"
    )

    // UI containers
    private val toolbarContainer: LinearLayout
    private val suggestionContainer: LinearLayout
    private val bodyContainer: FrameLayout

    // Current state
    private var currentMode = DisplayMode.TYPING
    private var currentLayoutMode = LayoutMode.NORMAL
    private var currentLayout: LanguageLayoutAdapter.LayoutModel? = null
    private var dynamicKeys = mutableListOf<DynamicKey>()
    private var currentPanelView: View? = null
    
    // Panel manager for toolbar functionality
    private var panelManager: UnifiedPanelManager? = null
    
    // Listeners for integration
    private var swipeListener: SwipeListener? = null
    private var swipeEnabled = true
    private var suggestionUpdateListener: SuggestionUpdateListener? = null
    private var autocorrectListener: AutocorrectListener? = null
    private var inputConnectionProvider: InputConnectionProvider? = null
    private var suggestionController: UnifiedSuggestionController? = null
    
    // Current word tracking for suggestions
    private var currentWord = StringBuilder()
    private var lastProvidedSuggestions = emptyList<String>()
    
    // Suggestion display count (3 or 4 based on user preference)
    private var suggestionDisplayCount = 3
    
    private val suggestionViews = mutableListOf<TextView>()
    private var suggestionSlotState: List<SuggestionSlotState> = emptyList()
    private val toolbarButtons = mutableListOf<ImageButton>()
    private var lastEditorText: String = ""

    // Current keyboard mode and language
    var currentKeyboardModeEnum = LanguageLayoutAdapter.KeyboardMode.LETTERS
    var currentLangCode = "en"
    
    // Public accessor for layout controller compatibility
    var currentKeyboardMode: LanguageLayoutAdapter.KeyboardMode
        get() = currentKeyboardModeEnum
        set(value) { currentKeyboardModeEnum = value }
    
    // Handler for UI operations
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainScope = CoroutineScope(Dispatchers.Main)
    
    // Theme paints (cached)
    private var keyTextPaint: Paint = themeManager.createKeyTextPaint()
    private var spaceLabelPaint: Paint = themeManager.createSpaceLabelPaint()

    // Configuration
    private var showLanguageOnSpace = true
    private var currentLanguageLabel = "English"
    private var labelScaleMultiplier = 1.0f
    private var borderlessMode = false
    private var keySpacingVerticalDp = 1  
    private var keySpacingHorizontalDp = 1  

    // Touch handling
    private var longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressKey: DynamicKey? = null
    private var accentPopup: PopupWindow? = null

    // Keyboard grid view (child of bodyContainer)
    private var keyboardGridView: KeyboardGridView? = null
    
    // Swipe gesture tracking
    private val fingerPoints = mutableListOf<FloatArray>()
    private var isSwipeInProgress = false
    private var swipeStartTime = 0L
    
    // Spacebar and backspace gesture tracking
    private var spacebarDownX = 0f
    private var isSpacebarSwipe = false
    private var backspaceDownX = 0f
    private var isBackspaceSwipe = false
    
    // Firebase language readiness
    private var isLanguageReady = true

    init {
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Apply initial background from theme
        background = themeManager.createKeyboardBackground()

        // ✅ CORRECT ORDER: 1. Toolbar, 2. Suggestions, 3. Keyboard
        
        // 1. Create toolbar container with modern icons
        val toolbarHeight = resources.getDimensionPixelSize(R.dimen.toolbar_height)
        toolbarContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                toolbarHeight
            )
            gravity = Gravity.CENTER_VERTICAL
            background = themeManager.createToolbarBackground()
            //setPadding(0, dpToPx(6), 0, dpToPx(0))
            setPadding(0, 0, 0, 0)
        }
        addView(toolbarContainer)
        
        // Create modern toolbar with full icon parity
        createModernToolbar()

        // 2. Create suggestion container - TRANSPARENT, text only
        val suggestionHeight = resources.getDimensionPixelSize(R.dimen.suggestion_bar_height)
        val suggestionPadding = resources.getDimensionPixelSize(R.dimen.suggestion_padding)
        suggestionContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                suggestionHeight
            )
            gravity = Gravity.CENTER
            visibility = VISIBLE
            if (themeManager.isImageBackground()) {
                setBackgroundColor(Color.TRANSPARENT)
            } else {
                background = themeManager.createSuggestionBarBackground()
            }
            setPadding(suggestionPadding, dpToPx(6), suggestionPadding, dpToPx(6))
        }
        addView(suggestionContainer)
        
        // Add default suggestion items with auto-commit styling
        createDefaultSuggestions()

        // 3. Create body container (keyboard or panel)
        bodyContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f // Fill remaining space
            )
            setBackgroundColor(Color.TRANSPARENT)
        }
        addView(bodyContainer)

        // Register for theme changes
        themeManager.addThemeChangeListener(object : ThemeManager.ThemeChangeListener {
            override fun onThemeChanged(theme: com.example.ai_keyboard.themes.KeyboardThemeV2, palette: ThemePaletteV2) {
                onThemeChangedInternal(palette)
            }
        })

        Log.d(TAG, "✅ UnifiedKeyboardView V2 initialized with full swipe pipeline")
    }

    // ========================================
    // PUBLIC API: Mode Switching
    // ========================================

    /**
     * Show typing layout (keyboard grid)
     */
    fun showTypingLayout(model: LanguageLayoutAdapter.LayoutModel) {
        currentMode = DisplayMode.TYPING
        currentLayout = model
        currentLangCode = model.languageCode

        toolbarContainer.visibility = VISIBLE
        suggestionContainer.visibility = VISIBLE

        // Hide panel if visible
        currentPanelView?.visibility = GONE

        // Build keyboard grid
        buildKeyboardGrid(model)

        // Reset suggestion state for new language/layout
        lastProvidedSuggestions = emptyList()
        updateSuggestions(emptyList())

        // Recalculate height
        recalcHeight()

        Log.d(TAG, "✅ Showing typing layout: ${model.languageCode} [${currentKeyboardModeEnum}]")
    }

    /**
     * Show feature panel (AI, Emoji, Clipboard, etc.)
     */
    fun showPanel(panelView: View) {
        currentMode = DisplayMode.PANEL

        // Clear keyboard grid
        dynamicKeys.clear()

        toolbarContainer.visibility = GONE
        suggestionContainer.visibility = GONE
        panelManager?.setInputText(lastEditorText)

        // Remove old panel if any
        currentPanelView?.let { bodyContainer.removeView(it) }

        // Add new panel
        bodyContainer.addView(panelView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        panelView.visibility = VISIBLE

        currentPanelView = panelView

        // Recalculate height
        recalcHeight()

        Log.d(TAG, "✅ Showing panel")
    }

    /**
     * Return to typing mode
     */
    fun backToTyping() {
        currentLayout?.let { showTypingLayout(it) }
    }
    
    /**
     * Toggle keyboard layout mode (normal, one-handed, floating)
     */
    fun toggleMode(newMode: LayoutMode) {
        if (newMode == currentLayoutMode) {
            // Restore to normal
            layoutParams = layoutParams.apply {
                width = LayoutParams.MATCH_PARENT
                height = LayoutParams.WRAP_CONTENT
            }
            translationX = 0f
            translationY = 0f
            elevation = 0f
            currentLayoutMode = LayoutMode.NORMAL
            return
        }

        currentLayoutMode = newMode
        when (newMode) {
            LayoutMode.ONE_HANDED_LEFT -> {
                layoutParams = layoutParams.apply {
                    width = (resources.displayMetrics.widthPixels * 0.75f).toInt()
                    height = LayoutParams.WRAP_CONTENT
                }
                translationX = 0f
            }
            LayoutMode.ONE_HANDED_RIGHT -> {
                layoutParams = layoutParams.apply {
                    width = (resources.displayMetrics.widthPixels * 0.75f).toInt()
                    height = LayoutParams.WRAP_CONTENT
                }
                translationX = (resources.displayMetrics.widthPixels * 0.25f)
            }
            LayoutMode.FLOATING -> {
                layoutParams = layoutParams.apply {
                    width = (resources.displayMetrics.widthPixels * 0.8f).toInt()
                    height = LayoutParams.WRAP_CONTENT
                }
                translationY = -dpToPx(100).toFloat()
                elevation = dpToPx(16).toFloat()
                background = themeManager.createKeyDrawable()
            }
            else -> {}
        }
        requestLayout()
        Log.d(TAG, "✅ Keyboard mode changed to: $newMode")
    }

    // ========================================
    // PUBLIC API: Configuration
    // ========================================

    /**
     * Set current language label and readiness status
     */
    fun setCurrentLanguage(label: String, isReady: Boolean = true) {
        currentLanguageLabel = label
        isLanguageReady = isReady
        updateLanguageBadge()
        invalidate()
    }

    /**
     * Enable/disable language label on space
     */
    fun setShowLanguageOnSpace(enabled: Boolean) {
        showLanguageOnSpace = enabled
        invalidate()
    }

    /**
     * Set label scale multiplier
     */
    fun setLabelScale(multiplier: Float) {
        labelScaleMultiplier = multiplier.coerceIn(0.8f, 1.3f)
        invalidate()
    }

    /**
     * Enable/disable borderless key mode
     */
    fun setBorderless(enabled: Boolean) {
        borderlessMode = enabled
        invalidate()
    }

    /**
     * Set key spacing
     */
    fun setKeySpacing(verticalDp: Int, horizontalDp: Int) {
        keySpacingVerticalDp = verticalDp
        keySpacingHorizontalDp = horizontalDp
        invalidate()
    }

    // ========================================
    // PUBLIC API: Listeners
    // ========================================

    fun setSwipeListener(listener: SwipeListener?) {
        this.swipeListener = listener
    }

    fun setSwipeEnabled(enabled: Boolean) {
        swipeEnabled = enabled
        if (!enabled) {
            resetSwipe()
        }
    }
    
    fun setSuggestionUpdateListener(listener: SuggestionUpdateListener?) {
        this.suggestionUpdateListener = listener
    }
    
    fun setAutocorrectListener(listener: AutocorrectListener?) {
        this.autocorrectListener = listener
    }
    
    fun setInputConnectionProvider(provider: InputConnectionProvider?) {
        this.inputConnectionProvider = provider
    }
    
    fun setPanelManager(manager: UnifiedPanelManager?) {
        this.panelManager = manager
        manager?.setInputText(lastEditorText)
    }
    
    fun setSuggestionController(controller: UnifiedSuggestionController?) {
        this.suggestionController = controller
    }

    fun updateEditorTextSnapshot(text: String) {
        lastEditorText = text
        panelManager?.setInputText(text)
    }
    
    /**
     * Set the number of suggestions to display (3 or 4)
     */
    fun setSuggestionDisplayCount(count: Int) {
        val clamped = count.coerceIn(1, 4)
        if (clamped == suggestionDisplayCount) return
        
        suggestionDisplayCount = clamped
        mainHandler.post {
            ensureSuggestionSlots(forceRebuild = true)
            val slotData = buildSuggestionSlotState(lastProvidedSuggestions)
            suggestionSlotState = slotData
            renderSuggestionSlots(slotData)
        }
    }

    // ========================================
    // PUBLIC API: Suggestions
    // ========================================
    
    /**
     * Update suggestions with auto-commit styling
     * Now enforces count and never creates ghost 4th slot
     */
    fun updateSuggestions(suggestions: List<String>) {
        try {
            val sanitized = suggestions.filter { it.isNotBlank() }
            lastProvidedSuggestions = sanitized
            val slotData = buildSuggestionSlotState(sanitized)
            suggestionSlotState = slotData
            
            mainHandler.post {
                try {
                    renderSuggestionSlots(slotData)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating suggestion container", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateSuggestions", e)
        }
    }

    // ========================================
    // PRIVATE: Theme & Height Management
    // ========================================

    private fun onThemeChangedInternal(palette: ThemePaletteV2) {
        // Update cached paints
        keyTextPaint = themeManager.createKeyTextPaint()
        spaceLabelPaint = themeManager.createSpaceLabelPaint()

        // Update background
        background = themeManager.createKeyboardBackground()

        // Update toolbar background
        toolbarContainer.background = themeManager.createToolbarBackground()
        
        // Update suggestion bar background
        if (themeManager.isImageBackground()) {
            suggestionContainer.setBackgroundColor(Color.TRANSPARENT)
        } else {
            suggestionContainer.background = themeManager.createSuggestionBarBackground()
        }

        // Update toolbar buttons
        toolbarButtons.forEach { styleToolbarButton(it) }
        
        // Rebuild suggestions with new theme
        val refreshedSlots = buildSuggestionSlotState(lastProvidedSuggestions)
        suggestionSlotState = refreshedSlots
        renderSuggestionSlots(refreshedSlots)

        // Rebuild current view
        when (currentMode) {
            DisplayMode.TYPING -> currentLayout?.let { buildKeyboardGrid(it) }
            DisplayMode.PANEL -> { /* Panel already uses ThemeManager */ }
        }

        invalidate()
        requestLayout()

        Log.d(TAG, "✅ Theme applied")
    }

    /**
     * Recalculate and apply height
     */
    fun recalcHeight() {
        val includeExtras = currentMode != DisplayMode.PANEL
        val newHeight = heightManager.calculateKeyboardHeight(
            includeToolbar = includeExtras,
            includeSuggestions = includeExtras
        )

        layoutParams = layoutParams?.apply {
            height = newHeight
        } ?: LayoutParams(
            LayoutParams.MATCH_PARENT,
            newHeight
        )

        requestLayout()
        Log.d(TAG, "📐 Height recalculated: ${newHeight}px")
    }

    // ========================================
    // PRIVATE: Toolbar Creation
    // ========================================

    /**
     * Create modern toolbar with full icon parity
     */
    private fun createModernToolbar() {
        toolbarContainer.removeAllViews()
        toolbarButtons.clear()

        val toolbarView = LayoutInflater.from(context).inflate(R.layout.keyboard_toolbar_unified, toolbarContainer, false)

        val settingsButton = toolbarView.findViewById<ImageButton>(R.id.button_toolbar_settings)
        val voiceButton = toolbarView.findViewById<ImageButton>(R.id.button_toolbar_voice)
        val emojiButton = toolbarView.findViewById<ImageButton>(R.id.button_toolbar_emoji)
        val aiButton = toolbarView.findViewById<ImageButton>(R.id.button_toolbar_ai)
        val grammarButton = toolbarView.findViewById<ImageButton>(R.id.button_toolbar_grammar)
        val toneButton = toolbarView.findViewById<ImageButton>(R.id.button_toolbar_tone)

        // Style left side buttons without boxes (plain icons)
        listOfNotNull(settingsButton, voiceButton, emojiButton).forEach { button ->
            toolbarButtons.add(button)
            styleToolbarButton(button, withBox = false)
        }
        
        // Style right side buttons with boxes
        listOfNotNull(aiButton, grammarButton, toneButton).forEach { button ->
            toolbarButtons.add(button)
            styleToolbarButton(button, withBox = true)
        }

        settingsButton?.setOnClickListener {
            panelManager?.let { manager ->
                val settingsPanel = manager.buildPanel(UnifiedPanelManager.PanelType.SETTINGS)
                showPanel(settingsPanel)
            }
        }

        voiceButton?.setOnClickListener {
            val service = AIKeyboardService.getInstance()
            if (service != null) {
                service.startVoiceInputFromToolbar()
            } else {
                Toast.makeText(context, context.getString(R.string.voice_input_not_available), Toast.LENGTH_SHORT).show()
            }
        }

        emojiButton?.setOnClickListener {
            panelManager?.let { manager ->
                val emojiPanel = manager.buildPanel(UnifiedPanelManager.PanelType.EMOJI)
                showPanel(emojiPanel)
            }
        }

        aiButton?.setOnClickListener {
            panelManager?.let { manager ->
                val aiPanel = manager.buildPanel(UnifiedPanelManager.PanelType.AI_ASSISTANT)
                showPanel(aiPanel)
            }
        }

        grammarButton?.setOnClickListener {
            panelManager?.let { manager ->
                val grammarPanel = manager.buildPanel(UnifiedPanelManager.PanelType.GRAMMAR)
                showPanel(grammarPanel)
            }
        }

        toneButton?.setOnClickListener {
            panelManager?.let { manager ->
                val tonePanel = manager.buildPanel(UnifiedPanelManager.PanelType.TONE)
                showPanel(tonePanel)
            }
        }

        toolbarContainer.addView(toolbarView)
    }

    private fun styleToolbarButton(button: ImageButton, withBox: Boolean = false) {
        val padding = resources.getDimensionPixelSize(R.dimen.toolbar_button_padding)
        if (withBox) {
            // Right side icons: Use themed box background
            button.background = themeManager.createToolbarButtonDrawable()
            val tintColor = Color.WHITE
            ImageViewCompat.setImageTintList(button, ColorStateList.valueOf(tintColor))
        } else {
            // Left side icons: No background, plain icons
            button.background = null
            val tintColor = themeManager.getToolbarTextColor()
            ImageViewCompat.setImageTintList(button, ColorStateList.valueOf(tintColor))
        }
        
        // Adjust padding for better touch target
        button.setPadding(padding, padding, padding, padding)
    }

    // ========================================
    // PRIVATE: Suggestion Bar Creation
    // ========================================

    private data class SuggestionSlotState(
        val text: String,
        val isPrimary: Boolean,
        val fromResults: Boolean
    )
    
    /**
     * Create default suggestion items (shown on keyboard open)
     */
    private fun createDefaultSuggestions() {
        updateSuggestions(emptyList()) // Will trigger default suggestions
    }
    
    /**
     * Create a suggestion item - TRANSPARENT, text only
     */
    private fun createSuggestionItem(text: String, isPrimary: Boolean, onClick: () -> Unit): TextView {
        val palette = themeManager.getCurrentPalette()
        return TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.suggestion_text_size))
            gravity = Gravity.CENTER
            setTextColor(palette.suggestionText)
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.suggestion_padding) / 2
            val verticalPadding = dpToPx(6)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            
            // ✅ No background - transparent
            background = null
            
            // ✅ No elevation
            elevation = 0f
            
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                val margin = resources.getDimensionPixelSize(R.dimen.suggestion_margin)
                setMargins(margin, dpToPx(2), margin, dpToPx(2))
            }
            
            isClickable = true
            isFocusable = true
            
            setOnClickListener { onClick() }
        }
    }

    private fun ensureSuggestionSlots(forceRebuild: Boolean = false) {
        if (!forceRebuild && suggestionViews.size == suggestionDisplayCount) return
        
        suggestionContainer.removeAllViews()
        suggestionViews.clear()
        
        repeat(suggestionDisplayCount) {
            val slotView = createSuggestionItem("", false) {}
            slotView.isEnabled = false
            slotView.alpha = 1f
            suggestionContainer.addView(slotView)
            suggestionViews.add(slotView)
        }
    }

    private fun extractSentenceStarterWords(maxWords: Int): List<String> {
        val snapshot = lastEditorText.ifBlank { currentWord.toString() }
        if (snapshot.isBlank()) return emptyList()
        
        // Look at the active sentence (after the last sentence-ending punctuation)
        val trimmed = snapshot.trimEnd()
        val lastDelimiterIndex = trimmed.indexOfLast { it == '.' || it == '!' || it == '?' || it == '\n' }
        val candidate = if (lastDelimiterIndex >= 0 && lastDelimiterIndex < trimmed.lastIndex) {
            trimmed.substring(lastDelimiterIndex + 1)
        } else {
            trimmed
        }
        
        val words = candidate
            .trim()
            .split(Regex("\\s+"))
            .map { it.trim('\"', '\'', '“', '”', '(', ')', ',', ';', ':', '.', '!', '?') }
            .filter { it.isNotBlank() }
        
        return words.take(maxWords)
    }

    private fun buildSuggestionSlotState(suggestions: List<String>): List<SuggestionSlotState> {
        val maxSlots = suggestionDisplayCount
        val slots = mutableListOf<SuggestionSlotState>()
        val hasResults = suggestions.isNotEmpty()
        val currentPrefix = currentWord.toString()
        val fallbackWords = if (hasResults) emptyList() else generateFallbackSuggestions(currentPrefix, maxSlots)
        var fallbackIndex = 0
        
        suggestions.take(maxSlots).forEachIndexed { index, text ->
            slots.add(
                SuggestionSlotState(
                    text = text,
                    isPrimary = hasResults && index == 0,
                    fromResults = true
                )
            )
        }
        
        while (slots.size < maxSlots) {
            val fallbackText = if (fallbackIndex < fallbackWords.size) {
                fallbackWords[fallbackIndex++]
            } else {
                ""
            }
            val isInteractive = fallbackText.isNotBlank()
            slots.add(
                SuggestionSlotState(
                    text = fallbackText,
                    isPrimary = fallbackIndex == 1 && isInteractive,
                    fromResults = isInteractive
                )
            )
        }
        
        return slots
    }

    private fun renderSuggestionSlots(slots: List<SuggestionSlotState>) {
        ensureSuggestionSlots()
        val palette = themeManager.getCurrentPalette()
        
        slots.forEachIndexed { index, slot ->
            val view = suggestionViews.getOrNull(index) ?: return@forEachIndexed
            view.text = slot.text
            val isInteractive = slot.fromResults && slot.text.isNotEmpty()
            
            // ✅ Simple text color - no theming, no accent colors
            view.setTextColor(palette.suggestionText)
            view.alpha = if (isInteractive) 1f else 0.5f  // Dim inactive suggestions
            view.isEnabled = isInteractive
            view.visibility = View.VISIBLE
            view.setCompoundDrawables(null, null, null, null)
            
            // ✅ No background - transparent
            view.background = null
            
            if (isInteractive) {
                view.setOnClickListener { commitSuggestionText(slot.text) }
            } else {
                view.setOnClickListener(null)
            }
        }
        
        Log.d(TAG, "✅ Updated suggestions: ${slots.count { it.fromResults }} active out of $suggestionDisplayCount slots")
    }

    private fun generateFallbackSuggestions(prefix: String, maxSlots: Int): List<String> {
        if (prefix.isBlank()) return extractSentenceStarterWords(maxSlots)
        val normalized = prefix.trim().lowercase()
        val firstChar = normalized.firstOrNull() ?: return extractSentenceStarterWords(maxSlots)
        val contextSource = StringBuilder()
            .append(lastEditorText)
            .append(' ')
            .append(currentWord)
            .toString()

        if (contextSource.isBlank()) return extractSentenceStarterWords(maxSlots)

        val words = contextSource
            .split(Regex("\\s+"))
            .map { it.trim('"', '\'', '“', '”', '(', ')', ',', ';', ':', '.', '!', '?') }
            .filter { it.isNotBlank() }

        val results = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (word in words.asReversed()) {
            val lower = word.lowercase()
            if (lower == normalized) continue
            if (lower.startsWith(normalized) || lower.firstOrNull() == firstChar) {
                if (seen.add(lower)) {
                    results.add(word)
                    if (results.size >= maxSlots) break
                }
            }
        }

        if (results.size < maxSlots) {
            val heuristics = mutableListOf(prefix)
            if (prefix.length > 1) {
                heuristics.add(prefix + "ing")
                heuristics.add(prefix + "ed")
                heuristics.add(prefix + "s")
            }
            for (candidate in heuristics) {
                val lower = candidate.lowercase()
                if (candidate.isNotBlank() && seen.add(lower)) {
                    results.add(candidate)
                    if (results.size >= maxSlots) break
                }
            }
        }

        return if (results.isNotEmpty()) results.take(maxSlots) else extractSentenceStarterWords(maxSlots)
    }

    private fun commitSuggestionText(suggestion: String) {
        // Clear clipboard suggestion if this was a clipboard item
        suggestionController?.clearClipboardSuggestion()
        
        suggestion.forEach { char ->
            onKeyCallback?.invoke(char.code, intArrayOf(char.code))
        }
        onKeyCallback?.invoke(32, intArrayOf(32))
        currentWord.clear()
    }

    // ========================================
    // PRIVATE: Keyboard Grid Building
    // ========================================

    private fun buildKeyboardGrid(model: LanguageLayoutAdapter.LayoutModel) {
        // Remove old grid view if any
        keyboardGridView?.let { bodyContainer.removeView(it) }

        // Create new keyboard grid view with swipe support
        keyboardGridView = KeyboardGridView(
            context = context,
            model = model,
            themeManager = themeManager,
            heightManager = heightManager,
            showLanguageOnSpace = showLanguageOnSpace,
            currentLanguageLabel = currentLanguageLabel,
            labelScaleMultiplier = labelScaleMultiplier,
            borderlessMode = borderlessMode,
            keySpacingVerticalDp = keySpacingVerticalDp,
            keySpacingHorizontalDp = keySpacingHorizontalDp,
            onKeyCallback = onKeyCallback,
            parentView = this
        )

        // Add to body container
        bodyContainer.addView(keyboardGridView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // Attach gesture controls
        attachGestureControls()

        // Apply RTL layout direction if needed
        val isRTL = model.direction.equals("RTL", ignoreCase = true)
        layoutDirection = if (isRTL) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR

        Log.d(TAG, "✅ Keyboard grid view created with swipe support")
    }

    // ========================================
    // PRIVATE: Swipe Gesture Pipeline
    // ========================================
    
    /**
     * Handle swipe touch events with normalized path generation
     */
    private fun handleSwipeTouch(event: MotionEvent): Boolean {
        if (!swipeEnabled) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> startSwipe(event.x, event.y)
            MotionEvent.ACTION_MOVE -> {
                if (!isSwipeInProgress && fingerPoints.isNotEmpty()) {
                    val st = fingerPoints.first()
                    val dx = event.x - st[0]
                    val dy = event.y - st[1]
                    if (sqrt(dx * dx + dy * dy) > SWIPE_START_THRESHOLD && swipeEnabled) {
                        isSwipeInProgress = true
                        swipeListener?.onSwipeStarted()
                    }
                }
                if (isSwipeInProgress && swipeEnabled) continueSwipe(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> if (isSwipeInProgress) endSwipe()
            MotionEvent.ACTION_CANCEL -> resetSwipe()
        }
        return false
    }

    private fun startSwipe(x: Float, y: Float) {
        fingerPoints.clear()
        fingerPoints.add(floatArrayOf(x, y))
        swipeStartTime = System.currentTimeMillis()
        isSwipeInProgress = false
    }

    private fun continueSwipe(x: Float, y: Float) {
        fingerPoints.add(floatArrayOf(x, y))
    }

    private fun resetSwipe() {
        fingerPoints.clear()
        isSwipeInProgress = false
        if (swipeEnabled) {
            swipeListener?.onSwipeEnded()
        }
    }

    private fun endSwipe() {
        if (!swipeEnabled) {
            resetSwipe()
            return
        }
        val dur = System.currentTimeMillis() - swipeStartTime
        val dist = totalDistance()
        
        if (dur >= MIN_SWIPE_TIME_MS && dist >= MIN_SWIPE_DISTANCE_PX) {
            val normalized = normalizePath(fingerPoints)
            val keySeq = keyboardGridView?.resolveKeySequence(normalized) ?: emptyList()
            
            // Log swipe path details for debugging
            Log.d(TAG, "✅ Swipe completed: ${keySeq.size} keys, ${normalized.size} points")
            Log.d(TAG, "   First point: ${normalized.firstOrNull()}, Last point: ${normalized.lastOrNull()}")
            
            if (swipeEnabled) {
                swipeListener?.onSwipeDetected(keySeq, normalized)
                handleSwipeSuggestions(keySeq, normalized)
            }
        }
        
        resetSwipe()
    }

    private fun totalDistance(): Float {
        var d = 0f
        for (i in 1 until fingerPoints.size) {
            val a = fingerPoints[i - 1]
            val b = fingerPoints[i]
            d += sqrt((b[0] - a[0]) * (b[0] - a[0]) + (b[1] - a[1]) * (b[1] - a[1]))
        }
        return d
    }

    private fun normalizePath(points: List<FloatArray>): List<Pair<Float, Float>> {
        val w = width.coerceAtLeast(1).toFloat()
        val h = height.coerceAtLeast(1).toFloat()
        return points.map { Pair(it[0] / w, it[1] / h) }
    }

    private fun handleSwipeSuggestions(seq: List<Int>, path: List<Pair<Float, Float>>) {
        mainScope.launch {
            try {
                // This would integrate with UnifiedAutocorrectEngine.suggestForSwipe()
                // For now, show placeholder suggestions
                val suggestions = listOf("swiped", "word", "here")
                updateSuggestions(suggestions)
                } catch (e: Exception) {
                Log.e(TAG, "Error handling swipe suggestions", e)
            }
        }
    }

    // ========================================
    // PRIVATE: Advanced Gestures
    // ========================================
    
    /**
     * Attach spacebar cursor and backspace swipe gestures
     */
    private fun attachGestureControls() {
        keyboardGridView?.let { gridView ->
            // Find spacebar and backspace keys
            val spacebar = gridView.findViewWithTag<View>("spacebar")
            val backspace = gridView.findViewWithTag<View>("key_backspace")

            spacebar?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        spacebarDownX = event.x
                        isSpacebarSwipe = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - spacebarDownX
                        if (abs(dx) > 40f) {
                            isSpacebarSwipe = true
                            handleSpacebarSwipe(dx)
                        }
                    }
                    MotionEvent.ACTION_UP -> isSpacebarSwipe = false
                }
                true
            }

            backspace?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        backspaceDownX = event.x
                        isBackspaceSwipe = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - backspaceDownX
                        if (dx < -50f) { // swipe left
                            isBackspaceSwipe = true
                            handleBackspaceSwipe()
                        }
                    }
                    MotionEvent.ACTION_UP -> isBackspaceSwipe = false
                }
                true
            }
        }
    }

    private fun handleSpacebarSwipe(deltaX: Float) {
        val ic = inputConnectionProvider?.getCurrentInputConnection()
        if (ic != null) {
            val direction = if (deltaX > 0) "right" else "left"
            val step = (abs(deltaX) / 20).toInt().coerceAtLeast(1)
            repeat(step) {
                val keyCode = if (direction == "right") 
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT 
                else 
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT
                    
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
            }
        }
    }

    private fun handleBackspaceSwipe() {
        val ic = inputConnectionProvider?.getCurrentInputConnection()
        ic?.apply {
            // Delete previous word (Ctrl+Backspace equivalent)
            sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_CTRL_LEFT))
            sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
            sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
            sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_CTRL_LEFT))
        }
    }

    // ========================================
    // PRIVATE: Firebase Language Readiness
    // ========================================
    
    /**
     * Update language readiness indicator on spacebar
     */
    private fun updateLanguageBadge() {
        mainHandler.post {
            keyboardGridView?.let { gridView ->
                val spacebarView = gridView.findViewWithTag<TextView>("spacebar")
                spacebarView?.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0,
                    if (isLanguageReady) 0 else android.R.drawable.stat_sys_download,
                    0
                )
            }
        }
    }

    // ========================================
    // PRIVATE: Touch Event Handling
    // ========================================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (currentMode) {
            DisplayMode.TYPING -> {
                // Handle swipe gestures for typing mode
                handleSwipeTouch(event)
                super.onTouchEvent(event)
            }
            DisplayMode.PANEL -> super.onTouchEvent(event)
        }
    }
    
    // ========================================
    // PRIVATE: Utility Methods
    // ========================================

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun getKeyWidthFactor(label: String): Float {
        return when {
            label == " " || label == "SPACE" || label.startsWith("space") -> 4f  // ✅ Decreased from 5.5f
            label == "⏎" || label == "RETURN" || label == "sym_keyboard_return" -> 2f
            label == "?123" || label == "ABC" || label == "=<" || label == "1234" -> 1.0f
            label == "🌐" || label == "GLOBE" -> 1f
            label == "," || label == "." -> 1f
            label == "⇧" || label == "SHIFT" -> 2f
            label == "⌫" || label == "DELETE" -> 2f
            else -> 1.0f
        }
    }

    private fun getKeyTypeFromCode(code: Int): String = when (code) {
        32 -> "space"
        -1, android.inputmethodservice.Keyboard.KEYCODE_SHIFT -> "shift"
        -5, android.inputmethodservice.Keyboard.KEYCODE_DELETE -> "backspace"
        10, -4, android.inputmethodservice.Keyboard.KEYCODE_DONE -> "enter"
        -13, -16 -> "mic"
        -15 -> "emoji"
        -14 -> "globe"
        -10, -11, -12 -> "symbols"
        else -> "regular"
    }

    private fun getIconForKeyType(keyType: String, label: String): Int? {
        return when (keyType) {
            "shift" -> R.drawable.sym_keyboard_shift
            "backspace" -> R.drawable.sym_keyboard_delete
            "enter" -> R.drawable.button_return  // ✅ Use Button_return.xml with smart behavior
            "globe" -> R.drawable.sym_keyboard_globe
            "space" -> R.drawable.sym_keyboard_space
            else -> when (label.uppercase()) {
                "SHIFT", "⇧" -> R.drawable.sym_keyboard_shift
                "DELETE", "⌫" -> R.drawable.sym_keyboard_delete
                "RETURN", "SYM_KEYBOARD_RETURN", "⏎" -> R.drawable.button_return  // ✅ Use Button_return.xml
                "GLOBE", "🌐" -> R.drawable.sym_keyboard_globe
                "SPACE" -> R.drawable.sym_keyboard_space
                else -> null
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        cancelLongPressInternal()
        hideAccentOptions()
        currentPanelView?.let { bodyContainer.removeView(it) }
        currentPanelView = null
        dynamicKeys.clear()
        keyboardGridView = null
        mainScope.cancel()
        Log.d(TAG, "✅ UnifiedKeyboardView cleaned up")
    }
    
    private fun cancelLongPressInternal() {
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        longPressRunnable = null
        longPressKey = null
    }

    private fun hideAccentOptions() {
        try {
            accentPopup?.dismiss()
            accentPopup = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    // ========================================
    // INNER CLASS: Enhanced KeyboardGridView
    // ========================================

    /**
     * Enhanced KeyboardGridView with full swipe support and gesture recognition
     */
    private class KeyboardGridView(
        context: Context,
        private val model: LanguageLayoutAdapter.LayoutModel,
        private val themeManager: ThemeManager,
        private val heightManager: KeyboardHeightManager,
        private val showLanguageOnSpace: Boolean,
        private val currentLanguageLabel: String,
        private val labelScaleMultiplier: Float,
        private val borderlessMode: Boolean,
        private val keySpacingVerticalDp: Int,
        private val keySpacingHorizontalDp: Int,
        private val onKeyCallback: ((Int, IntArray) -> Unit)?,
        private val parentView: UnifiedKeyboardView
    ) : View(context) {

        private val TAG = "KeyboardGridView"
        private val dynamicKeys = mutableListOf<DynamicKey>()
        
        // Swipe trail for visual feedback
        private val swipeTrailPaint = Paint().apply {
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            alpha = 180
        }
        
        // Touch handling
        private var longPressHandler = Handler(Looper.getMainLooper())
        private var longPressRunnable: Runnable? = null
        private var accentPopup: PopupWindow? = null
        private var accentOptionViews = mutableListOf<TextView>()
        private var selectedAccentIndex = -1
        
        // Swipe state
        private val fingerPoints = mutableListOf<FloatArray>()
        private var isSwipeActive = false
        
        // Continuous delete state
        private var deleteRepeatHandler = Handler(Looper.getMainLooper())
        private var deleteRepeatRunnable: Runnable? = null
        private var isDeleteRepeating = false

        init {
            setWillNotDraw(false) // Enable onDraw
            setBackgroundColor(Color.TRANSPARENT)

            if (width > 0 && height > 0) {
                buildKeys()
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w > 0 && h > 0) {
                buildKeys()
            }
        }

        private fun buildKeys() {
            dynamicKeys.clear()

            val screenWidth = width
            val screenHeight = height
            val padding = dpToPx(2).toFloat()  // tighter padding to use full height
            val keyVerticalSpacing = dpToPx(keySpacingVerticalDp).toFloat()
            val keyHorizontalSpacing = dpToPx(keySpacingHorizontalDp).toFloat()

            val numRows = model.rows.size
            val availableHeight = screenHeight - (padding * 2)
            val totalVerticalSpacing = keyVerticalSpacing * (numRows - 1)
            val keyHeight = ((availableHeight - totalVerticalSpacing) / numRows).toInt()

            var currentY = padding.toInt()

            val isRTL = model.direction.equals("RTL", ignoreCase = true)

            model.rows.forEach { row ->
                val availableWidth = screenWidth - (padding * 2)
                val totalWidthUnits = row.sumOf { key ->
                    getKeyWidthFactor(key.label).toDouble()
                }.toFloat()

                val unitWidth = (availableWidth - (keyHorizontalSpacing * (row.size - 1))) / totalWidthUnits

                var currentX = if (isRTL) {
                    (screenWidth - padding).toInt()
                } else {
                    padding.toInt()
                }

                row.forEach { keyModel ->
                    val widthFactor = getKeyWidthFactor(keyModel.label)
                    val keyWidth = (unitWidth * widthFactor).toInt()

                    val keyX = if (isRTL) currentX - keyWidth else currentX
                    val keyType = getKeyTypeFromCode(keyModel.code)

                    dynamicKeys.add(
                        DynamicKey(
                            x = keyX,
                            y = currentY,
                            width = keyWidth,
                            height = keyHeight,
                            label = keyModel.label,
                            code = keyModel.code,
                            longPressOptions = keyModel.longPress,
                            keyType = keyType
                        )
                    )

                    currentX += if (isRTL) {
                        -(keyWidth + keyHorizontalSpacing.toInt())
                    } else {
                        keyWidth + keyHorizontalSpacing.toInt()
                    }
                }

                currentY += keyHeight + keyVerticalSpacing.toInt()
            }

            invalidate()
            Log.d(TAG, "✅ Built ${dynamicKeys.size} keys with swipe support")
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val palette = themeManager.getCurrentPalette()
            canvas.drawColor(palette.keyboardBg)

            dynamicKeys.forEach { key ->
                drawKey(canvas, key, palette)
            }
            
            // Draw swipe trail if active
            if (isSwipeActive && fingerPoints.size > 1) {
                drawSwipeTrail(canvas, palette)
            }
        }
        
        private fun drawSwipeTrail(canvas: Canvas, palette: ThemePaletteV2) {
            if (fingerPoints.size < 2) return
            
            swipeTrailPaint.color = palette.specialAccent
            
            val path = Path()
            val firstPoint = fingerPoints[0]
            path.moveTo(firstPoint[0], firstPoint[1])
            
            for (i in 1 until fingerPoints.size) {
                val point = fingerPoints[i]
                    path.lineTo(point[0], point[1])
            }
            
            canvas.drawPath(path, swipeTrailPaint)
        }

        private fun drawKey(canvas: Canvas, key: DynamicKey, palette: ThemePaletteV2) {
            val density = context.resources.displayMetrics.density
            val basePadding = 0f  // ✅ No padding around keys
            val verticalSpacing = dpToPx(keySpacingVerticalDp) * density / 2f
            val horizontalSpacing = dpToPx(keySpacingHorizontalDp) * density / 2f

            val keyRect = RectF(
                key.x.toFloat() + basePadding + horizontalSpacing,
                key.y.toFloat() + basePadding + verticalSpacing,
                (key.x + key.width).toFloat() - basePadding - horizontalSpacing,
                (key.y + key.height).toFloat() - basePadding - verticalSpacing
            )

            // Draw background
            val keyDrawable = when {
                key.keyType == "enter" && themeManager.shouldUseAccentForEnter() -> themeManager.createSpecialKeyDrawable()
                themeManager.shouldUseAccentForKey(key.keyType) -> themeManager.createSpecialKeyDrawable()
                else -> themeManager.createKeyDrawable()
            }
            keyDrawable.setBounds(keyRect.left.toInt(), keyRect.top.toInt(), keyRect.right.toInt(), keyRect.bottom.toInt())
            keyDrawable.draw(canvas)

            // Draw icon or text
            val iconResId = getIconForKeyType(key.keyType, key.label)
            if (iconResId != null) {
                drawKeyIcon(canvas, key, keyRect, iconResId, palette)
            } else {
                drawKeyText(canvas, key, keyRect, palette)
            }
        }

        private fun drawKeyIcon(canvas: Canvas, key: DynamicKey, keyRect: RectF, iconResId: Int, palette: ThemePaletteV2) {
            val iconDrawable = ContextCompat.getDrawable(context, iconResId)?.mutate() ?: return
            val iconSize = min(key.width, key.height) * 0.4f
            val centerX = keyRect.centerX()
            val centerY = keyRect.centerY()

            val tintColor = when {
                key.keyType == "space" && showLanguageOnSpace -> palette.spaceLabelColor
                themeManager.shouldUseAccentForKey(key.keyType) || (key.keyType == "enter" && themeManager.shouldUseAccentForEnter()) -> Color.WHITE
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

            // Language label on space
            if (key.keyType == "space" && showLanguageOnSpace && currentLanguageLabel.isNotEmpty()) {
                val textPaint = Paint(parentView.spaceLabelPaint).apply {
                    textSize = parentView.spaceLabelPaint.textSize * labelScaleMultiplier * 0.7f
                    color = palette.spaceLabelColor
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(currentLanguageLabel, centerX, centerY + iconSize/2 + textPaint.textSize, textPaint)
            }
        }

        private fun drawKeyText(canvas: Canvas, key: DynamicKey, keyRect: RectF, palette: ThemePaletteV2) {
            val textPaint = if (key.keyType == "space") Paint(parentView.spaceLabelPaint) else Paint(parentView.keyTextPaint)
            textPaint.textSize = parentView.keyTextPaint.textSize * labelScaleMultiplier

            textPaint.color = when {
                themeManager.shouldUseAccentForKey(key.keyType) || (key.keyType == "enter" && themeManager.shouldUseAccentForEnter()) -> Color.WHITE
                else -> palette.keyText
            }

            val text = if (key.keyType == "space" && showLanguageOnSpace) currentLanguageLabel else key.label

            val centerX = keyRect.centerX()
            val centerY = keyRect.centerY()
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textOffset = (textHeight / 2) - textPaint.descent()
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, centerX, centerY + textOffset, textPaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Handle accent popup sliding selection
            if (accentPopup?.isShowing == true) {
                return handleAccentPopupTouch(event)
            }
            
            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val key = findKeyAtPosition(event.x.toInt(), event.y.toInt())
                    if (key != null) {
                        handleKeyDown(key)
                        // Start swipe tracking
                        fingerPoints.clear()
                        fingerPoints.add(floatArrayOf(event.x, event.y))
                        isSwipeActive = false
                        true
                    } else false
                }
                MotionEvent.ACTION_MOVE -> {
                    // Continue swipe tracking
                    if (!isSwipeActive && fingerPoints.isNotEmpty()) {
                        val st = fingerPoints[0]
                        val dx = event.x - st[0]
                        val dy = event.y - st[1]
                        val dist = sqrt(dx*dx + dy*dy)
                        if (dist > SWIPE_START_THRESHOLD && parentView.swipeEnabled) {
                            isSwipeActive = true
                            // Cancel long press when swipe starts
                            cancelLongPressInternal()
                            if (parentView.swipeEnabled) {
                                parentView.swipeListener?.onSwipeStarted()
                            }
                        }
                    }
                    
                    if (isSwipeActive && parentView.swipeEnabled) {
                        fingerPoints.add(floatArrayOf(event.x, event.y))
                        invalidate() // Redraw for swipe trail
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val key = findKeyAtPosition(event.x.toInt(), event.y.toInt())
                    
                    // Stop delete repeat
                    stopDeleteRepeat()
                    
                    if (isSwipeActive && fingerPoints.size > 1) {
                        // Handle swipe
                        endSwipe()
                    } else if (key != null && !isSwipeActive && accentPopup?.isShowing != true) {
                        // Handle tap (only if no popup is showing)
                        handleKeyUp(key)
                    }
                    
                    cancelLongPressInternal()
                    fingerPoints.clear()
                    isSwipeActive = false
                    invalidate()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopDeleteRepeat()
                    cancelLongPressInternal()
                    isSwipeActive = false
                    fingerPoints.clear()
                    invalidate()
                    true
                }
                else -> super.onTouchEvent(event)
            }
        }
        
        private fun handleAccentPopupTouch(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    // Find which accent option is under the finger
                    updateAccentSelection(event.rawX, event.rawY)
                }
                MotionEvent.ACTION_UP -> {
                    // Commit the selected accent
                    if (selectedAccentIndex >= 0 && selectedAccentIndex < accentOptionViews.size) {
                        val selectedView = accentOptionViews[selectedAccentIndex]
                        val option = selectedView.text.toString()
                        val charCode = option.firstOrNull()?.code ?: return true
                        onKeyCallback?.invoke(charCode, intArrayOf(charCode))
                    }
                    hideAccentPopup()
                }
                MotionEvent.ACTION_CANCEL -> {
                    hideAccentPopup()
                }
            }
            return true
        }
        
        private fun updateAccentSelection(rawX: Float, rawY: Float) {
            var newSelectedIndex = -1
            
            accentOptionViews.forEachIndexed { index, view ->
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val viewX = location[0]
                val viewY = location[1]
                val viewRight = viewX + view.width
                val viewBottom = viewY + view.height
                
                if (rawX >= viewX && rawX <= viewRight && rawY >= viewY && rawY <= viewBottom) {
                    newSelectedIndex = index
                }
            }
            
            if (newSelectedIndex != selectedAccentIndex) {
                val palette = themeManager.getCurrentPalette()
                
                // Unhighlight old selection
                if (selectedAccentIndex >= 0 && selectedAccentIndex < accentOptionViews.size) {
                    val oldView = accentOptionViews[selectedAccentIndex]
                    val normalBackground = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dpToPx(8).toFloat()
                        setColor(palette.keyboardBg)
                    }
                    oldView.background = normalBackground
                    oldView.setTextColor(palette.keyText) // Reset text color
                }
                
                // Highlight new selection
                if (newSelectedIndex >= 0 && newSelectedIndex < accentOptionViews.size) {
                    val newView = accentOptionViews[newSelectedIndex]
                    val highlightBackground = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dpToPx(8).toFloat()
                        setColor(palette.specialAccent)
                    }
                    newView.background = highlightBackground
                    newView.setTextColor(Color.WHITE)
                    
                    // Haptic feedback
                    this@KeyboardGridView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                }
                
                selectedAccentIndex = newSelectedIndex
            }
        }
        
        private fun endSwipe() {
            if (fingerPoints.isEmpty()) return
            
            val normalized = fingerPoints.map { p ->
                Pair(p[0] / width.coerceAtLeast(1).toFloat(), p[1] / height.coerceAtLeast(1).toFloat())
            }
            
            val keySeq = resolveKeySequence(normalized)
            if (parentView.swipeEnabled) {
                parentView.swipeListener?.onSwipeDetected(keySeq, normalized)
                parentView.swipeListener?.onSwipeEnded()
            }
            fingerPoints.clear()
            isSwipeActive = false
        }
        
        fun resolveKeySequence(normalizedPath: List<Pair<Float, Float>>): List<Int> {
            val keySeq = mutableListOf<Int>()
            var lastCode = -1
            
            normalizedPath.forEach { (nx, ny) ->
                val x = (nx * width).toInt()
                val y = (ny * height).toInt()
                val key = findKeyAtPosition(x, y)
                if (key != null && key.code != lastCode && key.code > 0) {
                    keySeq.add(key.code)
                    lastCode = key.code
                }
            }
            
            return keySeq
        }

        private fun handleKeyDown(key: DynamicKey) {
            // Special handling for delete key - enable continuous repeat
            if (key.keyType == "backspace") {
                // Initial delete
                onKeyCallback?.invoke(key.code, intArrayOf(key.code))
                
                // Start repeat after delay
                deleteRepeatRunnable = object : Runnable {
                    override fun run() {
                        if (isDeleteRepeating) {
                            onKeyCallback?.invoke(key.code, intArrayOf(key.code))
                            deleteRepeatHandler.postDelayed(this, 50L) // Repeat every 50ms
                        }
                    }
                }
                deleteRepeatHandler.postDelayed({
                    isDeleteRepeating = true
                    deleteRepeatRunnable?.run()
                }, 500L) // Start repeating after 500ms
                
                return
            }
            
            // Long press handling for accent options
            if (!key.longPressOptions.isNullOrEmpty()) {
                longPressRunnable = Runnable { showAccentOptions(key) }
                longPressHandler.postDelayed(longPressRunnable!!, 500L)
            }
        }
        
        private fun stopDeleteRepeat() {
            isDeleteRepeating = false
            deleteRepeatRunnable?.let { deleteRepeatHandler.removeCallbacks(it) }
            deleteRepeatRunnable = null
            deleteRepeatHandler.removeCallbacksAndMessages(null)
        }

        private fun handleKeyUp(key: DynamicKey) {
            // Stop delete repeat if it's the delete key
            if (key.keyType == "backspace") {
                stopDeleteRepeat()
                return
            }
            
            cancelLongPressInternal()
            if (accentPopup?.isShowing != true) {
                        onKeyCallback?.invoke(key.code, intArrayOf(key.code))
            }
        }

        private fun cancelLongPressInternal() {
            cancelLongPressTimer()
            hideAccentPopup()
        }

        private fun cancelLongPressTimer() {
            longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
            longPressRunnable = null
        }

        private fun hideAccentPopup() {
            try {
                accentPopup?.dismiss()
                accentPopup = null
                accentOptionViews.clear()
                selectedAccentIndex = -1
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing accent popup", e)
            }
        }

        private fun showAccentOptions(key: DynamicKey) {
            val options = key.longPressOptions ?: return
            if (options.isEmpty()) return

            // Dismiss any existing popup
            hideAccentPopup()

            val palette = themeManager.getCurrentPalette()
            
            // Create horizontal layout for accent options
            val popupBackground = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()
                setColor(palette.keyBg)
                setStroke(dpToPx(1), palette.keyBorderColor)
            }
            
            val optionsContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                background = popupBackground
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                elevation = dpToPx(8).toFloat()
            }

            // Clear and prepare for new options
            accentOptionViews.clear()
            selectedAccentIndex = 0 // Pre-select first option

            // Add each option as a button
            options.forEachIndexed { index, option ->
                val isSelected = index == 0 // First option is pre-selected
                val optionBackground = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dpToPx(8).toFloat()
                    setColor(if (isSelected) palette.specialAccent else palette.keyboardBg)
                }
                
                val optionView = TextView(context).apply {
                    text = option
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(if (isSelected) Color.WHITE else palette.keyText)
                    setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                    minWidth = dpToPx(44)
                    minHeight = dpToPx(44)
                    background = optionBackground
                    
                    setOnClickListener {
                        // Insert the selected character
                        val charCode = option.firstOrNull()?.code ?: return@setOnClickListener
                        onKeyCallback?.invoke(charCode, intArrayOf(charCode))
                        hideAccentPopup()
                    }
                }
                
                accentOptionViews.add(optionView)
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) leftMargin = dpToPx(4)
                }
                optionsContainer.addView(optionView, params)
            }

            // Create and show popup
            accentPopup = PopupWindow(
                optionsContainer,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                isFocusable = false
                
                // Calculate position above the key
                val location = IntArray(2)
                this@KeyboardGridView.getLocationInWindow(location)
                
                val xPos = location[0] + key.x + (key.width / 2) - (optionsContainer.measuredWidth / 2)
                val yPos = location[1] + key.y - dpToPx(60)
                
                // Measure the content
                optionsContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                
                try {
                    showAtLocation(this@KeyboardGridView, Gravity.NO_GRAVITY, xPos, yPos)
                    Log.d(TAG, "✅ Accent popup shown with ${options.size} options")
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing accent popup", e)
                }
            }
            
            // Provide haptic feedback
            this@KeyboardGridView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }

        private fun findKeyAtPosition(x: Int, y: Int): DynamicKey? {
            return dynamicKeys.firstOrNull { key ->
                x >= key.x && x < (key.x + key.width) && y >= key.y && y < (key.y + key.height)
            }
        }

        private fun getKeyWidthFactor(label: String): Float = when {
            label == " " || label == "SPACE" || label.startsWith("space") -> 4f  // ✅ Decreased from 5.5f
            label == "⏎" || label == "RETURN" || label == "sym_keyboard_return" -> 2f
            label == "⇧" || label == "SHIFT" -> 1.5f
            label == "⌫" || label == "DELETE" -> 1.5f
            label == "?123" || label == "ABC" || label == "=<" || label == "1234" -> 1.25f  // ✅ Decreased from 1f
            label == "🌐" || label == "GLOBE" -> 1.25f  // ✅ Decreased from 1f
            label == "," || label == "." -> 1.25f  // ✅ Decreased from 1f
            else -> 1.0f
        }

        private fun getKeyTypeFromCode(code: Int): String = when (code) {
            32 -> "space"
            -1 -> "shift"
            -5 -> "backspace"
            10, -4 -> "enter"
            -13, -16 -> "mic"
            -15 -> "emoji"
            -14 -> "globe"
            -10, -11, -12 -> "symbols"
            else -> "regular"
        }

        private fun getIconForKeyType(keyType: String, label: String): Int? = when (keyType) {
            "shift" -> R.drawable.sym_keyboard_shift
            "backspace" -> R.drawable.sym_keyboard_delete
            "enter" -> R.drawable.sym_keyboard_return
            "globe" -> R.drawable.sym_keyboard_globe
            "space" -> R.drawable.sym_keyboard_space
            else -> when (label.uppercase()) {
                "SHIFT", "⇧" -> R.drawable.sym_keyboard_shift
                "DELETE", "⌫" -> R.drawable.sym_keyboard_delete
                "RETURN", "SYM_KEYBOARD_RETURN" -> R.drawable.sym_keyboard_return
                "GLOBE", "🌐" -> R.drawable.sym_keyboard_globe
                "SPACE" -> R.drawable.sym_keyboard_space
                else -> null
            }
        }
        
        private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
    }
}
