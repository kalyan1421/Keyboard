package com.example.ai_keyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibrationEffect
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.PopupWindow
import android.view.Gravity
import android.view.LayoutInflater
import android.graphics.drawable.ColorDrawable
import kotlinx.coroutines.*
import kotlin.math.max

class AIKeyboardService : InputMethodService(), 
    KeyboardView.OnKeyboardActionListener, 
    SwipeKeyboardView.SwipeListener {
    
    companion object {
        private const val TAG = "AIKeyboardService"
        
        // Keyboard layouts
        private const val KEYBOARD_LETTERS = 1
        private const val KEYBOARD_SYMBOLS = 2
        private const val KEYBOARD_NUMBERS = 3
        
        // Custom key codes
        private const val KEYCODE_SPACE = 32
        private const val KEYCODE_SYMBOLS = -10
        private const val KEYCODE_LETTERS = -11
        private const val KEYCODE_NUMBERS = -12
        private const val KEYCODE_VOICE = -13
        private const val KEYCODE_GLOBE = -14
        private const val KEYCODE_EMOJI = -15
        private const val KEYCODE_SHIFT = -1
        private const val KEYCODE_DELETE = -5
        
        // Swipe settings
        private const val SWIPE_START_THRESHOLD = 50f
        private const val MIN_SWIPE_TIME = 300L
        private const val MIN_SWIPE_DISTANCE = 100f
        
        // Advanced keyboard settings
        private const val LONG_PRESS_TIMEOUT = 500L
        private const val DOUBLE_TAP_TIMEOUT = 300L
        
        // Shift states
        private const val SHIFT_OFF = 0
        private const val SHIFT_ON = 1 
        private const val SHIFT_CAPS = 2
    }
    
    // UI Components
    private var keyboardView: SwipeKeyboardView? = null
    private var keyboard: Keyboard? = null
    private var suggestionContainer: LinearLayout? = null
    private var topContainer: LinearLayout? = null // Container for suggestions + language switch
    private var mediaPanelManager: SimpleMediaPanel? = null
    private var keyboardContainer: LinearLayout? = null
    private var isMediaPanelVisible = false
    
    // Keyboard state
    private var caps = false
    private var lastShiftTime = 0L
    private var isShifted = false
    private var currentKeyboard = KEYBOARD_LETTERS
    
    // Advanced keyboard state
    private var shiftState = SHIFT_OFF
    private var lastShiftPressTime = 0L
    private var longPressHandler: Handler? = null
    private var currentLongPressKey: Int = 0
    private var keyPreviewPopup: PopupWindow? = null
    private var accentPopup: PopupWindow? = null
    private var vibrator: Vibrator? = null
    
    // Accent mappings for long-press functionality
    private val accentMap = mapOf(
        'a'.code to listOf("á", "à", "â", "ä", "ã", "å", "ā", "ă", "ą"),
        'e'.code to listOf("é", "è", "ê", "ë", "ē", "ĕ", "ė", "ę", "ě"),
        'i'.code to listOf("í", "ì", "î", "ï", "ī", "ĭ", "į", "ı"),
        'o'.code to listOf("ó", "ò", "ô", "ö", "õ", "ō", "ŏ", "ő", "ø"),
        'u'.code to listOf("ú", "ù", "û", "ü", "ū", "ŭ", "ů", "ű", "ų"),
        'y'.code to listOf("ý", "ỳ", "ŷ", "ÿ"),
        'c'.code to listOf("ç", "ć", "ĉ", "ċ", "č"),
        'd'.code to listOf("ď", "đ"),
        'g'.code to listOf("ğ", "ĝ", "ġ", "ģ"),
        'l'.code to listOf("ĺ", "ļ", "ľ", "ŀ", "ł"),
        'n'.code to listOf("ñ", "ń", "ņ", "ň", "ŉ", "ŋ"),
        'r'.code to listOf("ŕ", "ŗ", "ř"),
        's'.code to listOf("ś", "ŝ", "ş", "š"),
        't'.code to listOf("ţ", "ť", "ŧ"),
        'z'.code to listOf("ź", "ż", "ž"),
        '0'.code to listOf("°", "₀", "⁰"),
        '1'.code to listOf("¹", "₁", "½", "⅓", "¼"),
        '2'.code to listOf("²", "₂", "⅔"),
        '3'.code to listOf("³", "₃", "¾"),
        '4'.code to listOf("⁴", "₄"),
        '5'.code to listOf("⁵", "₅"),
        '-'.code to listOf("–", "—", "−", "±"),
        '='.code to listOf("≠", "≈", "≤", "≥", "±"),
        '?'.code to listOf("¿", "‽"),
        '!'.code to listOf("¡", "‼", "⁉"),
        '.'.code to listOf("…", "·", "•"),
        '$'.code to listOf("¢", "£", "€", "¥", "₹", "₽", "₩")
    )
    
    // Swipe typing state
    private var swipeMode = false
    private val swipeBuffer = StringBuilder()
    private val swipePath = mutableListOf<Int>()
    private var swipeStartTime = 0L
    private var isCurrentlySwiping = false
    
    // AI and suggestion components
    private val currentSuggestions = mutableListOf<String>()
    private val wordHistory = mutableListOf<String>()
    private var currentWord = ""
    private var isAIReady = false
    
    // Dictionary data loaded from assets
    private var commonWords = listOf<String>()
    private var wordFrequencies = mapOf<String, Int>()
    private var corrections = mapOf<String, String>()
    private var contractions = mapOf<String, List<String>>()
    private var technologyWords = listOf<String>()
    private var businessWords = listOf<String>()
    private var allWords = listOf<String>()
    
    // Multilingual components
    private lateinit var languageManager: LanguageManager
    private lateinit var languageDetector: LanguageDetector
    
    // CleverType components
    private lateinit var cleverTypeService: CleverTypeAIService
    private var cleverTypePreview: CleverTypePreview? = null
    private var cleverTypeToneSelector: CleverTypeToneSelector? = null
    private var cleverTypeToolbar: LinearLayout? = null
    private lateinit var keyboardLayoutManager: KeyboardLayoutManager
    private lateinit var multilingualDictionary: MultilingualDictionary
    private lateinit var multilingualAutocorrect: MultilingualAutocorrectEngine
    private var languageSwitchView: LanguageSwitchView? = null
    
    // Services and handlers
    private lateinit var aiBridge: AIServiceBridge
    private val mainHandler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Settings
    private lateinit var settings: SharedPreferences
    private var currentTheme = "default"
    private var aiSuggestionsEnabled = true
    private var swipeTypingEnabled = true
    private var vibrationEnabled = true
    private var keyPreviewEnabled = false
    
    // Advanced feedback settings
    private var hapticIntensity = 2 // 0=off, 1=light, 2=medium, 3=strong
    private var soundIntensity = 1
    private var visualIntensity = 2
    private var soundVolume = 0.3f
    
    // Settings polling
    private var settingsPoller: Runnable? = null
    private var lastSettingsCheck = 0L
    
    // Broadcast receiver for settings changes
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if ("com.example.ai_keyboard.SETTINGS_CHANGED" == intent?.action) {
                    // Reload settings immediately on main thread
                    mainHandler.post {
                        try {
                            loadSettings()
                            applySettingsImmediately()
                        } catch (e: Exception) {
                            // Ignore errors to prevent crashes
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore broadcast errors to prevent crashes
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OpenAI configuration first (critical for AI features)
        try {
            OpenAIConfig.getInstance(this)
            Log.d(TAG, "OpenAI configuration initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OpenAI configuration", e)
        }
        
        settings = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
        loadSettings()
        loadDictionaries()
        
        // Initialize multilingual components
        initializeMultilingualComponents()

        // Initialize Enhanced AI Service Bridge
        initializeAIBridge()
        
        // Initialize CleverType AI Service
        initializeCleverTypeService()
        
        // Initialize advanced keyboard features
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        longPressHandler = Handler(Looper.getMainLooper())
        
        // Register broadcast receiver for settings changes
        try {
            val filter = IntentFilter("com.example.ai_keyboard.SETTINGS_CHANGED")
            registerReceiver(settingsReceiver, filter)
        } catch (e: Exception) {
            // Ignore registration errors to prevent crashes
        }
        
        // Start settings polling as backup
        startSettingsPolling()
    }
    
    private fun initializeAIBridge() {
        try {
            // Initialize the enhanced AI bridge with context
            AIServiceBridge.initialize(this)
            aiBridge = AIServiceBridge.getInstance()
            Log.d(TAG, "AI Bridge initialized successfully")
            
            // Check AI readiness periodically
            coroutineScope.launch {
                delay(1000)
                var retryCount = 0
                while (!isAIReady && retryCount < 5) {
                    isAIReady = aiBridge.isReady()
                    if (isAIReady) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AIKeyboardService, "🤖 AI Keyboard Ready", Toast.LENGTH_SHORT).show()
                        }
                        break
                    } else {
                        Log.d(TAG, "AI not ready yet, retry $retryCount/5")
                        delay(2000) // Retry after 2 seconds
                        retryCount++
                    }
                }
                
                if (!isAIReady) {
                    Log.w(TAG, "AI failed to initialize after 5 retries - using enhanced basic mode")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AIKeyboardService, "📝 Keyboard Ready (Basic Mode)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AI bridge", e)
        }
    }
    
    /**
     * Initialize multilingual keyboard components
     */
    private fun initializeMultilingualComponents() {
        try {
            // Initialize language manager
            languageManager = LanguageManager(this)
            
            // Initialize language detector
            languageDetector = LanguageDetector()
            
            // Initialize keyboard layout manager
            keyboardLayoutManager = KeyboardLayoutManager(this)
            
            // Initialize multilingual dictionary
            multilingualDictionary = MultilingualDictionary(this)
            
            // Initialize multilingual autocorrect engine
            multilingualAutocorrect = MultilingualAutocorrectEngine(this)
            
            // Set up language change listener
            languageManager.addLanguageChangeListener(object : LanguageManager.LanguageChangeListener {
                override fun onLanguageChanged(oldLanguage: String, newLanguage: String) {
                    handleLanguageChange(oldLanguage, newLanguage)
                }
                
                override fun onEnabledLanguagesChanged(enabledLanguages: Set<String>) {
                    handleEnabledLanguagesChange(enabledLanguages)
                }
            })
            
            // Initialize with current enabled languages
            val enabledLanguages = languageManager.getEnabledLanguages()
            coroutineScope.launch {
                multilingualAutocorrect.initialize(enabledLanguages)
            }
            
            Log.d(TAG, "Multilingual components initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing multilingual components", e)
        }
    }
    
    /**
     * Handle language change
     */
    private fun handleLanguageChange(oldLanguage: String, newLanguage: String) {
        try {
            Log.d(TAG, "Language changed from $oldLanguage to $newLanguage")
            
            // Update keyboard layout
            keyboardLayoutManager.updateCurrentLanguage(newLanguage)
            
            // Update autocorrect engine
            multilingualAutocorrect.updateCurrentLanguage(newLanguage)
            
            // Update keyboard view if available
            keyboardView?.let { kv ->
                val currentMode = when (currentKeyboard) {
                    KEYBOARD_LETTERS -> "letters"
                    KEYBOARD_SYMBOLS -> "symbols"
                    KEYBOARD_NUMBERS -> "numbers"
                    else -> "letters"
                }
                
                val newKeyboard = keyboardLayoutManager.getCurrentKeyboard(currentMode)
                if (newKeyboard != null) {
                    keyboard = newKeyboard
                    kv.keyboard = newKeyboard
                    kv.invalidateAllKeys()
                }
            }
            
            // Update language switch view
            languageSwitchView?.refreshDisplay()
            
            // Clear current word and update suggestions
            currentWord = ""
            updateAISuggestions()
            
            // Show language change notification
            Toast.makeText(this, "Language: ${languageManager.getLanguageDisplayName(newLanguage)}", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling language change", e)
        }
    }
    
    /**
     * Handle enabled languages change
     */
    private fun handleEnabledLanguagesChange(enabledLanguages: Set<String>) {
        try {
            Log.d(TAG, "Enabled languages changed: $enabledLanguages")
            
            // Update autocorrect engine
            coroutineScope.launch {
                multilingualAutocorrect.updateEnabledLanguages(enabledLanguages)
            }
            
            // Preload keyboard layouts
            keyboardLayoutManager.preloadLayouts(enabledLanguages)
            
            // Update language switch view visibility
            languageSwitchView?.refreshDisplay()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling enabled languages change", e)
        }
    }
    
    override fun onCreateInputView(): View {
        // Create the main keyboard container
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getThemeBackgroundColor())
        }
        
        // Create suggestion bar
        createSuggestionBarWithLanguageSwitch(mainLayout)
        
        // Create CleverType toolbar
        cleverTypeToolbar = createCleverTypeToolbar()
        mainLayout.addView(cleverTypeToolbar)
        
        // Create keyboard view container that will hold either keyboard or media panel
        val keyboardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Create keyboard view with Google layout and swipe support
        try {
            keyboardView = layoutInflater.inflate(R.layout.keyboard_view_google_layout, null) as SwipeKeyboardView
        } catch (e: Exception) {
            // Fallback: create programmatically with minimal setup
            keyboardView = SwipeKeyboardView(this, null, 0)
        }
        
        keyboardView?.apply {
            keyboard = Keyboard(this@AIKeyboardService, R.xml.qwerty_google)
            setKeyboard(keyboard)
            setOnKeyboardActionListener(this@AIKeyboardService)
            setSwipeListener(this@AIKeyboardService)
            setSwipeEnabled(swipeTypingEnabled)
            isPreviewEnabled = keyPreviewEnabled
        }
        
        // Create media panel manager (but don't add to layout yet)
        createMediaPanel()
        
        // Initially show keyboard
        keyboardView?.let { keyboardContainer.addView(it) }
        mainLayout.addView(keyboardContainer)
        
        // Store reference to container for switching views
        this.keyboardContainer = keyboardContainer
        
        // Apply theme
        applyTheme()
        
        return mainLayout
    }
    
    private fun createSuggestionBarWithLanguageSwitch(parent: LinearLayout) {
        Log.d(TAG, "Creating suggestion bar with language switch")
        
        // Create container for language switch and suggestions
        topContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(getThemeKeyColor())
            setPadding(4, 4, 4, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Create language switch view
        languageSwitchView = LanguageSwitchView(this).apply {
            setLanguageManager(languageManager)
            setOnLanguageChangeListener { newLanguage ->
                Log.d(TAG, "Language switched to: $newLanguage")
            }
        }
        
        // Set layout params for language switch view
        val languageSwitchParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(4, 0, 8, 0)
        }
        languageSwitchView?.layoutParams = languageSwitchParams
        
        // Create suggestions container
        suggestionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f
            )
        }
        
        // Add components to top container
        topContainer!!.addView(languageSwitchView)
        topContainer!!.addView(suggestionContainer)
        
        // Add to parent
        parent.addView(topContainer)
        
        // Now create the actual suggestion bar
        createSuggestionBar(suggestionContainer!!)
    }
    
    private fun createSuggestionBar(parent: LinearLayout) {
        Log.d(TAG, "Creating suggestion bar")
        
        suggestionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(getThemeKeyColor())
            setPadding(8, 4, 8, 4)
            visibility = View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                height = 100 // Set minimum height
            }
        }
        
        // Add five suggestion text views for more suggestions
        repeat(5) { index ->
            val suggestion = TextView(this).apply {
                setTextColor(getThemeTextColor())
                textSize = 16f
                setPadding(16, 8, 16, 8)
                setBackgroundResource(R.drawable.suggestion_background)
                isClickable = true
                text = "Suggestion ${index + 1}" // Default text for testing
                visibility = View.VISIBLE
                
                setOnClickListener { view ->
                    val suggestionText = (view as TextView).text.toString()
                    Log.d(TAG, "Suggestion clicked: '$suggestionText'")
                    if (suggestionText.isNotEmpty() && !suggestionText.startsWith("Suggestion")) {
                        applySuggestion(suggestionText)
                    }
                }
            }
            
            val params = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            ).apply {
                setMargins(4, 0, 4, 0)
            }
            suggestion.layoutParams = params
            
            suggestionContainer?.addView(suggestion)
        }
        
        suggestionContainer?.let { 
            parent.addView(it)
            Log.d(TAG, "Suggestion bar added to parent with ${it.childCount} children")
        }
        
        // Show initial suggestions immediately
        updateSuggestionUI(listOf("I", "The", "And", "Hello", "How"))
        
        // Test suggestions after a short delay
        coroutineScope.launch {
            delay(2000)
            withContext(Dispatchers.Main) {
                updateSuggestionUI(listOf("Hello", "World", "Test", "✓ Correct", "Predict"))
                Log.d(TAG, "Test suggestions updated with autocorrect example")
            }
        }
    }
    
    private fun createMediaPanel() {
        Log.d(TAG, "Creating media panel")
        
        mediaPanelManager = SimpleMediaPanel(this).apply {
            setOnMediaSelectedListener { mediaType, content, data ->
                handleMediaSelection(mediaType, content, data)
            }
            setOnKeyboardSwitchRequestedListener {
                // Switch back to keyboard when ABC button is tapped
                if (isMediaPanelVisible) {
                    toggleMediaPanel()
                }
            }
            // Set proper layout params for full keyboard replacement
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        Log.d(TAG, "Media panel created (not added to layout yet)")
    }
    
    private fun toggleMediaPanel() {
        try {
            isMediaPanelVisible = !isMediaPanelVisible
            keyboardContainer?.let { container ->
                container.removeAllViews()
                
                if (isMediaPanelVisible) {
                    // Get current text from input field for AI processing
                    val currentText = getCurrentInputText()
                    
                    // Show media panel instead of keyboard (Google Keyboard style)
                    mediaPanelManager?.let { mediaPanel ->
                        // Pass current text to AI features panel
                        mediaPanel.setInputText(currentText)
                        container.addView(mediaPanel)
                        Log.d(TAG, "Switched to media panel view with text: '${currentText.take(50)}${if (currentText.length > 50) "..." else ""}'")
                    }
                    // Hide entire suggestion bar (including language switch) in emoji mode
                    topContainer?.visibility = View.GONE
                } else {
                    // Show keyboard instead of media panel
                    keyboardView?.let { keyboard ->
                        container.addView(keyboard)
                        Log.d(TAG, "Switched back to keyboard view")
                    }
                    // Show entire suggestion bar (including language switch) in keyboard mode
                    topContainer?.visibility = View.VISIBLE
                }
                
                // Request layout update
                container.requestLayout()
            }
            
            Log.d(TAG, "Media panel toggled: visible=$isMediaPanelVisible, suggestion bar hidden=${isMediaPanelVisible}")
            Log.d(TAG, "TopContainer visibility: ${topContainer?.visibility}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling media panel", e)
        }
    }
    
    /**
     * Get current text from input field for AI processing
     */
    private fun getCurrentInputText(): String {
        return try {
            val ic = currentInputConnection ?: return ""
            
            // Try to get selected text first
            val selectedText = ic.getSelectedText(0)
            if (!selectedText.isNullOrBlank()) {
                Log.d(TAG, "Found selected text: '${selectedText.take(100)}...'")
                return selectedText.toString()
            }
            
            // If no selection, get text around cursor (last sentence/paragraph)
            val beforeCursor = ic.getTextBeforeCursor(500, 0)?.toString() ?: ""
            val afterCursor = ic.getTextAfterCursor(100, 0)?.toString() ?: ""
            
            // Try to extract the current sentence or paragraph
            val currentText = when {
                // If there's substantial text before cursor, get the last sentence/paragraph
                beforeCursor.isNotEmpty() -> {
                    val sentences = beforeCursor.split(Regex("[.!?]\\s+"))
                    val lastSentence = sentences.lastOrNull()?.trim() ?: ""
                    
                    // If last sentence is too short, get more context
                    if (lastSentence.length < 10 && sentences.size > 1) {
                        sentences.takeLast(2).joinToString(". ").trim()
                    } else {
                        lastSentence
                    }
                }
                // If there's text after cursor, include it
                afterCursor.isNotEmpty() -> {
                    val nextWords = afterCursor.split("\\s+".toRegex()).take(10).joinToString(" ")
                    "$beforeCursor$nextWords".trim()
                }
                else -> beforeCursor.trim()
            }
            
            // Clean up the text
            val cleanText = currentText.replace(Regex("\\s+"), " ").trim()
            
            Log.d(TAG, "Retrieved input text (${cleanText.length} chars): '${cleanText.take(100)}${if (cleanText.length > 100) "..." else ""}'")
            
            cleanText
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current input text", e)
            ""
        }
    }
    
    private fun handleMediaSelection(mediaType: SimpleMediaPanel.MediaType, content: String, data: Any?) {
        try {
            Log.d(TAG, "Media selected - Type: $mediaType, Content: $content")
            
            when (mediaType) {
                SimpleMediaPanel.MediaType.EMOJI -> {
                    // Insert emoji directly
                    currentInputConnection?.commitText(content, 1)
                    Log.d(TAG, "Inserted emoji: $content")
                }
                SimpleMediaPanel.MediaType.GIF -> {
                    // Handle GIF insertion
                    currentInputConnection?.commitText(content, 1)
                    Log.d(TAG, "Inserted GIF: $content")
                }
                SimpleMediaPanel.MediaType.STICKER -> {
                    // Handle sticker insertion
                    currentInputConnection?.commitText(content, 1)
                    Log.d(TAG, "Inserted sticker: $content")
                }
                SimpleMediaPanel.MediaType.AI_FEATURES -> {
                    // Handle AI-processed text insertion
                    currentInputConnection?.commitText(content, 1)
                    Log.d(TAG, "Inserted AI-processed text: $content")
                }
            }
            
            // Keep media panel visible after selection (Google Keyboard behavior)
            // User must manually switch back to keyboard
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling media selection", e)
        }
    }
    
    private fun insertGifContent(content: String, gifData: GifData?) {
        try {
            when {
                content.startsWith("http") -> {
                    // Network URL - insert as text link for now
                    currentInputConnection?.commitText("GIF: $content", 1)
                }
                content.startsWith("/") -> {
                    // Local file path - try to insert as rich content
                    // For now, insert as text placeholder
                    val title = gifData?.title ?: "GIF"
                    currentInputConnection?.commitText("[$title GIF]", 1)
                }
                else -> {
                    currentInputConnection?.commitText("[GIF]", 1)
                }
            }
            
            // Record usage if we have GIF data
            gifData?.let { gif ->
                Thread {
                    try {
                        val gifManager = GifManager(this)
                        gifManager.recordGifUsage(gif.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording GIF usage", e)
                    }
                }.start()
            }
            
            Log.d(TAG, "Inserted GIF content: $content")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting GIF content", e)
            currentInputConnection?.commitText("[GIF]", 1)
        }
    }
    
    private fun insertStickerContent(content: String, stickerData: StickerData?) {
        try {
            when {
                content.startsWith("emoji://") -> {
                    // It's an emoji-based sticker, just insert the emoji
                    currentInputConnection?.commitText(content, 1)
                }
                content.startsWith("/") -> {
                    // Local file path - insert as placeholder for now
                    currentInputConnection?.commitText("[Sticker]", 1)
                }
                content.startsWith("http") -> {
                    // Network URL - insert as text link
                    currentInputConnection?.commitText("Sticker: $content", 1)
                }
                else -> {
                    // Direct content (emoji)
                    currentInputConnection?.commitText(content, 1)
                }
            }
            
            // Record usage if we have sticker data
            stickerData?.let { sticker ->
                Thread {
                    try {
                        val stickerManager = StickerManager(this)
                        stickerManager.recordStickerUsage(sticker.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording sticker usage", e)
                    }
                }.start()
            }
            
            Log.d(TAG, "Inserted sticker content: $content")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting sticker content", e)
            currentInputConnection?.commitText("[Sticker]", 1)
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    private fun loadSettings() {
        currentTheme = settings.getString("keyboard_theme", "default") ?: "default"
        aiSuggestionsEnabled = settings.getBoolean("ai_suggestions", true)
        swipeTypingEnabled = settings.getBoolean("swipe_typing", true)
        vibrationEnabled = settings.getBoolean("vibration_enabled", true)
        keyPreviewEnabled = settings.getBoolean("key_preview_enabled", false)
        
        // Load advanced feedback settings
        hapticIntensity = settings.getInt("haptic_intensity", 2) // medium by default
        soundIntensity = settings.getInt("sound_intensity", 1) // light by default
        visualIntensity = settings.getInt("visual_intensity", 2) // medium by default
        soundVolume = settings.getFloat("sound_volume", 0.3f)
    }
    
    private fun applyTheme() {
        keyboardView?.let { view ->
            // Apply comprehensive theme to the SwipeKeyboardView
            view.setKeyboardTheme(currentTheme)
            
            // The SwipeKeyboardView will handle per-key theming internally
            view.invalidateAllKeys()
            view.invalidate()
        }
    }
    
    private fun getThemeBackgroundColor(): Int = when (currentTheme) {
        "gboard" -> getColor(R.color.gboard_background)
        "gboard_dark" -> getColor(R.color.gboard_dark_background)
        "dark" -> Color.parseColor("#1E1E1E")
        "material_you" -> Color.parseColor("#6750A4")
        "professional" -> Color.parseColor("#37474F")
        "colorful" -> Color.parseColor("#E1F5FE")
        else -> getColor(R.color.gboard_background) // Default to Gboard
    }
    
    private fun getKeyBackgroundDrawable(): Int = when (currentTheme) {
        "dark" -> R.drawable.key_background_dark
        "material_you" -> R.drawable.key_background_material_you
        "professional" -> R.drawable.key_background_professional
        "colorful" -> R.drawable.key_background_colorful
        else -> R.drawable.key_background_default
    }
    
    private fun getActionKeyBackgroundDrawable(): Int = when (currentTheme) {
        "dark" -> R.drawable.key_background_action_dark
        "material_you" -> R.drawable.key_background_action_material_you
        "professional" -> R.drawable.key_background_action_professional
        "colorful" -> R.drawable.key_background_action_colorful
        else -> R.drawable.key_background_action_default
    }
    
    private fun getThemeKeyColor(): Int = when (currentTheme) {
        "dark" -> Color.parseColor("#2D2D2D")
        "material_you" -> Color.parseColor("#7C4DFF")
        "professional" -> Color.parseColor("#455A64")
        "colorful" -> Color.parseColor("#81D4FA")
        else -> Color.WHITE
    }
    
    private fun getThemeTextColor(): Int = when (currentTheme) {
        "gboard" -> getColor(R.color.gboard_key_text)
        "gboard_dark" -> getColor(R.color.gboard_dark_key_text)
        "dark", "material_you", "professional" -> Color.WHITE
        "colorful" -> Color.parseColor("#0D47A1")
        else -> getColor(R.color.gboard_key_text) // Default to Gboard
    }
    
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        
        playClick(primaryCode)
        
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> handleBackspace(ic)
            Keyboard.KEYCODE_SHIFT -> handleShift()
            Keyboard.KEYCODE_DONE -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            KEYCODE_SPACE -> handleSpace(ic)
            KEYCODE_SYMBOLS -> switchToSymbols()
            KEYCODE_LETTERS -> switchToLetters()
            KEYCODE_NUMBERS -> switchToNumbers()
            KEYCODE_VOICE -> {
                // Voice input removed - show message
                Toast.makeText(this, "Voice input feature removed", Toast.LENGTH_SHORT).show()
            }
            KEYCODE_GLOBE -> {
                // Language switching - show available languages or switch to next
                handleLanguageSwitch()
            }
            KEYCODE_EMOJI -> {
                // Toggle media panel (emoji, GIF, stickers)
                toggleMediaPanel()
            }
            else -> handleCharacter(primaryCode, ic)
        }
        
        // Update AI suggestions after key press
        if (aiSuggestionsEnabled && primaryCode != Keyboard.KEYCODE_DELETE) {
            updateAISuggestions()
        }
    }
    
    private fun handleCharacter(primaryCode: Int, ic: InputConnection) {
        var code = primaryCode.toChar()
        
        // Enhanced character handling with advanced shift management
        if (Character.isLetter(code)) {
            code = when (shiftState) {
                SHIFT_OFF -> Character.toLowerCase(code)
                SHIFT_ON, SHIFT_CAPS -> Character.toUpperCase(code)
                else -> Character.toLowerCase(code)
            }
        }
        
        // Auto-reset shift state after character input (except for caps lock)
        if (shiftState == SHIFT_ON) {
            shiftState = SHIFT_OFF
            keyboardView?.let {
                it.isShifted = false
                it.invalidateAllKeys()
            }
            caps = false
            isShifted = false
        }
        
        // Update current word
        if (Character.isLetter(code)) {
            currentWord += Character.toLowerCase(code)
            Log.d(TAG, "Updated currentWord: '$currentWord'")
        } else if (Character.isSpaceChar(code) || code == ' ' || isPunctuation(code)) {
            // Non-letter character ends current word
            if (currentWord.isNotEmpty()) {
                Log.d(TAG, "Non-letter character '$code' - finishing word: '$currentWord'")
                finishCurrentWord()
            }
        }
        
        // Insert character with enhanced text processing
        insertCharacterWithProcessing(ic, code)
        
        // Enhanced auto-shift logic
        updateCapsState(code)
        
        // Update keyboard visual state
        updateKeyboardState()
        
        // Update suggestions in real-time as user types
        updateAISuggestions()
    }
    
    private fun insertCharacterWithProcessing(ic: InputConnection, code: Char) {
        // Get current text context for smart processing
        val textBefore = ic.getTextBeforeCursor(10, 0)
        val context = textBefore?.toString() ?: ""
        
        // Handle smart punctuation
        if (isSmartPunctuationEnabled() && isPunctuation(code)) {
            handleSmartPunctuation(ic, code, context)
        } else {
            // Standard character insertion
            ic.commitText(code.toString(), 1)
        }
        
        // Handle auto-correction if enabled
        if (isAutoCorrectEnabled() && (code == ' ' || isPunctuation(code))) {
            performAutoCorrection(ic)
        }
    }
    
    private fun updateCapsState(code: Char) {
        val wasCapitalized = caps
        
        // Auto-shift after sentence end
        when (code) {
            '.', '!', '?' -> caps = true
            else -> {
                if (caps && Character.isLetter(code) && !isCapslockEnabled()) {
                    // Turn off caps after typing a letter (unless caps lock is on)
                    caps = false
                }
            }
        }
        
        // Update visual state if changed
        if (wasCapitalized != caps) {
            keyboardView?.isShifted = caps
        }
    }
    
    private fun updateKeyboardState() {
        keyboardView?.invalidateAllKeys()
    }
    
    private fun isSmartPunctuationEnabled(): Boolean = settings.getBoolean("smart_punctuation", true)
    private fun isAutoCorrectEnabled(): Boolean = settings.getBoolean("auto_correct", true)
    private fun isCapslockEnabled(): Boolean = settings.getBoolean("caps_lock_active", false)
    private fun isPunctuation(c: Char): Boolean = ".,!?;:".contains(c)
    
    private fun shouldCapitalizeAfterSpace(ic: InputConnection): Boolean {
        val textBefore = ic.getTextBeforeCursor(10, 0)?.toString() ?: ""
        
        // Capitalize at start of input
        if (textBefore.isEmpty()) return true
        
        // Capitalize after sentence-ending punctuation
        val trimmed = textBefore.trimEnd()
        return trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")
    }
    
    private fun handleSmartPunctuation(ic: InputConnection, code: Char, context: String) {
        // Smart spacing around punctuation
        when (code) {
            '.', '!', '?' -> {
                // Remove extra spaces before sentence-ending punctuation
                if (context.endsWith(" ")) {
                    ic.deleteSurroundingText(1, 0)
                }
                ic.commitText(code.toString(), 1)
            }
            ',' -> {
                // Handle comma spacing
                ic.commitText(code.toString(), 1)
            }
            else -> ic.commitText(code.toString(), 1)
        }
    }
    
    private fun performAutoCorrection(ic: InputConnection) {
        // Get the last word typed
        val textBefore = ic.getTextBeforeCursor(50, 0) ?: return
        
        val text = textBefore.toString()
        val words = text.split("\\s+".toRegex())
        if (words.isEmpty()) return
        
        val lastWord = words.last()
        if (lastWord.length < 2) return
        
        // Simple auto-correction dictionary
        val corrected = getAutoCorrection(lastWord)
        if (corrected != null && corrected != lastWord) {
            // Replace the word with correction
            ic.deleteSurroundingText(lastWord.length, 0)
            ic.commitText(corrected, 1)
        }
    }
    
    private fun getAutoCorrection(word: String): String? = when (word.lowercase()) {
        "teh" -> "the"
        "adn" -> "and"
        "hte" -> "the"
        "taht" -> "that"
        "thier" -> "their"
        "recieve" -> "receive"
        "seperate" -> "separate"
        "definately" -> "definitely"
        "occured" -> "occurred"
        "begining" -> "beginning"
        else -> null
    }
    
    private fun handleBackspace(ic: InputConnection) {
        val selectedText = ic.getSelectedText(0)
        if (TextUtils.isEmpty(selectedText)) {
            ic.deleteSurroundingText(1, 0)
            
            // Update current word tracking
            if (currentWord.isNotEmpty()) {
                currentWord = currentWord.dropLast(1)
            } else {
                // If no current word, rebuild from text
                rebuildCurrentWord(ic)
            }
        } else {
            ic.commitText("", 1)
            currentWord = ""
        }
        
        // Update suggestions after backspace
        if (aiSuggestionsEnabled) {
            updateAISuggestions()
        }
    }
    
    private fun rebuildCurrentWord(ic: InputConnection) {
        try {
            val textBefore = ic.getTextBeforeCursor(50, 0)
            if (textBefore != null) {
                val text = textBefore.toString()
                val words = text.split("\\s+".toRegex())
                if (words.isNotEmpty() && words.last().isNotEmpty()) {
                    // Check if the last "word" contains only letters
                    val lastWord = words.last()
                    if (lastWord.matches("[a-zA-Z]+".toRegex())) {
                        currentWord = lastWord.lowercase()
                    }
                }
            }
        } catch (e: Exception) {
            // If rebuilding fails, just clear current word
            currentWord = ""
        }
    }
    
    private fun handleSpace(ic: InputConnection) {
        // Process current word before adding space
        if (currentWord.isNotEmpty()) {
            finishCurrentWord()
        }
        
        // Handle double space for period (like iOS/GBoard)
        val textBefore = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
        if (textBefore.endsWith("  ")) {
            // Replace double space with period + space
            ic.deleteSurroundingText(2, 0)
            ic.commitText(". ", 1)
        } else {
        ic.commitText(" ", 1)
        }
        
        // Update auto-capitalization after space
        caps = shouldCapitalizeAfterSpace(ic)
        keyboardView?.isShifted = caps
        
        updateAISuggestions()
    }
    
    private fun finishCurrentWord() {
        if (currentWord.isEmpty()) return
        
        // Add to word history
        wordHistory.add(currentWord)
        
        // Learn from user input if AI is ready
        if (isAIReady) {
            val context = getRecentContext()
            aiBridge.learnFromInput(currentWord, context)
        }
        
        // Clear current word
        currentWord = ""
        
        // Keep word history manageable
        if (wordHistory.size > 20) {
            wordHistory.removeAt(0)
        }
    }
    
    private fun getRecentContext(): List<String> {
        val start = max(0, wordHistory.size - 3)
        return wordHistory.subList(start, wordHistory.size)
    }
    
    private fun handleShift() {
        val now = System.currentTimeMillis()
        
        // Enhanced 3-State Shift Management: OFF -> ON -> CAPS -> OFF
        when (shiftState) {
            SHIFT_OFF -> {
                // Single tap: Activate shift for next character only
                shiftState = SHIFT_ON
                lastShiftPressTime = now
                showShiftFeedback("Shift ON - Next character uppercase")
            }
            SHIFT_ON -> {
                if (now - lastShiftPressTime < DOUBLE_TAP_TIMEOUT) {
                    // Double tap detected within timeout - activate caps lock
                    shiftState = SHIFT_CAPS
                    showShiftFeedback("CAPS LOCK - All characters uppercase")
                } else {
                    // Single tap after timeout - turn off shift
                    shiftState = SHIFT_OFF
                    lastShiftPressTime = now
                    showShiftFeedback("Shift OFF - Lowercase mode")
                }
            }
            SHIFT_CAPS -> {
                // Any tap from caps lock - turn off completely
                shiftState = SHIFT_OFF
                showShiftFeedback("CAPS LOCK OFF - Lowercase mode")
            }
        }
        
        // Update keyboard view with enhanced visual feedback
        updateShiftVisualState()
        
        // Provide haptic feedback for shift state changes
        performShiftHapticFeedback()
        
        // Maintain backward compatibility with existing caps variable
        caps = (shiftState != SHIFT_OFF)
        isShifted = caps
        
        Log.d(TAG, "Shift state changed to: ${getShiftStateName()}")
    }
    
    private fun updateShiftVisualState() {
        keyboardView?.let { view ->
            // Update the shift key visual state
            view.isShifted = (shiftState != SHIFT_OFF)
            
            // Enhanced visual feedback based on shift state
            when (shiftState) {
                SHIFT_OFF -> {
                    // Normal state - no special highlighting
                    view.setShiftKeyHighlight(false, false)
                }
                SHIFT_ON -> {
                    // Temporary shift - light highlighting
                    view.setShiftKeyHighlight(true, false)
                }
                SHIFT_CAPS -> {
                    // Caps lock - strong highlighting with caps indicator
                    view.setShiftKeyHighlight(true, true)
                }
            }
            
            view.invalidateAllKeys()
        }
    }
    
    private fun showShiftFeedback(message: String) {
        // Show brief toast feedback for shift state changes (optional)
        if (settings.getBoolean("show_shift_feedback", false)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun performShiftHapticFeedback() {
        if (vibrationEnabled && vibrator != null) {
            try {
                val intensity = when (shiftState) {
                    SHIFT_OFF -> 15L      // Light vibration for turning off
                    SHIFT_ON -> 25L       // Medium vibration for shift on
                    SHIFT_CAPS -> 50L     // Strong vibration for caps lock
                    else -> 20L
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(intensity, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(intensity)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to provide shift haptic feedback: ${e.message}")
            }
        }
    }
    
    private fun getShiftStateName(): String = when (shiftState) {
        SHIFT_OFF -> "OFF (lowercase)"
        SHIFT_ON -> "ON (next char uppercase)"
        SHIFT_CAPS -> "CAPS (all uppercase)"
        else -> "UNKNOWN"
    }
    
    private fun switchToSymbols() {
        if (currentKeyboard != KEYBOARD_SYMBOLS) {
            keyboard = Keyboard(this, R.xml.symbols_google)
            currentKeyboard = KEYBOARD_SYMBOLS
            keyboardView?.keyboard = keyboard
            applyTheme() // Reapply theme after layout change
        }
    }
    
    private fun switchToLetters() {
        if (currentKeyboard != KEYBOARD_LETTERS) {
            keyboard = Keyboard(this, R.xml.qwerty_google)
            currentKeyboard = KEYBOARD_LETTERS
            keyboardView?.keyboard = keyboard
            applyTheme() // Reapply theme after layout change
        }
    }
    
    private fun switchToNumbers() {
        if (currentKeyboard != KEYBOARD_NUMBERS) {
            keyboard = Keyboard(this, R.xml.numbers)
            currentKeyboard = KEYBOARD_NUMBERS
            keyboardView?.keyboard = keyboard
            applyTheme() // Reapply theme after layout change
        }
    }
    
    private fun handleLanguageSwitch() {
        // For now, show a simple message about language switching
        // This can be enhanced to show a language picker or cycle through languages
        Toast.makeText(this, "Language switching - Feature coming soon!", Toast.LENGTH_SHORT).show()
        
        // Future enhancement: Implement actual language switching
        // switchInputMethod() can be used to switch between input methods
        // or implement internal language switching logic
    }
    
    private fun handleEmojiKey() {
        // For now, insert a common emoji or show a simple emoji picker
        val ic = currentInputConnection ?: return
        
        // Simple emoji insertion - can be enhanced to show emoji picker
        val commonEmojis = listOf("😊", "😂", "❤️", "👍", "🎉", "🔥", "💯", "😍")
        val randomEmoji = commonEmojis.random()
        
        ic.commitText(randomEmoji, 1)
        
        // Future enhancement: Implement emoji picker popup
        // Toast.makeText(this, "Emoji picker - Feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    
    private fun updateAISuggestions() {
        Log.d(TAG, "updateAISuggestions called - aiSuggestionsEnabled: $aiSuggestionsEnabled, isAIReady: $isAIReady, currentWord: '$currentWord'")
        
        if (!aiSuggestionsEnabled) {
            Log.d(TAG, "AI suggestions disabled in settings")
            return
        }
        
        // Always try to show suggestions, even if AI is not fully ready
        if (!isAIReady) {
            Log.d(TAG, "AI not ready - showing enhanced basic suggestions with autocorrect")
            val enhancedSuggestions = generateEnhancedBasicSuggestions(currentWord)
            updateSuggestionUI(enhancedSuggestions)
            return
        }
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Get AI-powered suggestions
                val context = getRecentContext()
                Log.d(TAG, "Getting AI suggestions for word: '$currentWord', context: $context")
                
                // Also try enhanced suggestions even if AI bridge isn't fully ready
                val fallbackSuggestions = generateEnhancedBasicSuggestions(currentWord)
                
                aiBridge.getSuggestions(currentWord, context, object : AIServiceBridge.AICallback {
                    override fun onSuggestionsReady(suggestions: List<AIServiceBridge.AISuggestion>) {
                        // Convert AI suggestions to display format
                        val suggestionTexts = suggestions.map { suggestion ->
                            // Mark corrections with indicator
                            if (suggestion.isCorrection) "✓ ${suggestion.word}" else suggestion.word
                        }
                        
                        // Update UI on main thread
                        coroutineScope.launch(Dispatchers.Main) {
                            updateSuggestionUI(suggestionTexts)
                        }
                    }
                    
                    override fun onCorrectionReady(originalWord: String, correctedWord: String, confidence: Double) {
                        // Handle individual corrections
                        if (confidence > 0.8) {
                            coroutineScope.launch(Dispatchers.Main) {
                                showAutoCorrection(originalWord, correctedWord)
                            }
                        }
                    }
                    
                    override fun onError(error: String) {
                        Log.w(TAG, "AI suggestion error: $error")
                        // Fallback to enhanced basic suggestions
                        coroutineScope.launch(Dispatchers.Main) {
                            updateSuggestionUI(fallbackSuggestions)
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error updating AI suggestions", e)
            }
        }
    }
    
    private fun showAutoCorrection(original: String, corrected: String) {
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as TextView
                firstSuggestion.apply {
                    text = "✓ $corrected"
                    setTextColor(Color.parseColor("#4CAF50")) // Green for corrections
                    setBackgroundColor(Color.parseColor("#E8F5E8")) // Light green background
                    visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun generateBasicSuggestions(currentWord: String): List<String> {
        // Basic fallback suggestions when AI is not available - Google style
        val suggestions = mutableListOf<String>()
        
        Log.d(TAG, "Generating basic suggestions for: '$currentWord'")
        
        if (currentWord.isEmpty()) {
            // Default Google-style suggestions
            suggestions.addAll(listOf("I", "The", "In"))
        } else {
            val word = currentWord.lowercase()
            
            // Simple prefix matching for common words
            when {
                word.startsWith("h") -> suggestions.addAll(listOf("hello", "how", "have"))
                word.startsWith("w") -> suggestions.addAll(listOf("what", "when", "where"))
                word.startsWith("t") -> suggestions.addAll(listOf("the", "that", "this"))
                word.startsWith("i") -> suggestions.addAll(listOf("I", "in", "is"))
                word.startsWith("a") -> suggestions.addAll(listOf("and", "are", "as"))
                word.startsWith("o") -> suggestions.addAll(listOf("of", "on", "or"))
                else -> suggestions.addAll(listOf("I", "The", "In"))
            }
        }
        
        return suggestions.take(5)
    }
    
    /**
     * Load dictionaries from assets
     */
    private fun loadDictionaries() {
        try {
            // Load common words
            val commonWordsJson = assets.open("dictionaries/common_words.json").bufferedReader().use { it.readText() }
            val commonWordsData = org.json.JSONObject(commonWordsJson)
            
            // Extract words array (check for both old and new format)
            val wordsList = mutableListOf<String>()
            val frequencyMap = mutableMapOf<String, Int>()
            
            if (commonWordsData.has("basic_words")) {
                // New format with categories
                val basicWordsArray = commonWordsData.getJSONArray("basic_words")
                for (i in 0 until basicWordsArray.length()) {
                    val wordObj = basicWordsArray.getJSONObject(i)
                    val word = wordObj.getString("word")
                    val frequency = wordObj.getInt("frequency")
                    wordsList.add(word)
                    frequencyMap[word] = frequency
                }
                
                // Load business words if available
                if (commonWordsData.has("business_words")) {
                    val businessWordsArray = commonWordsData.getJSONArray("business_words")
                    val businessList = mutableListOf<String>()
                    for (i in 0 until businessWordsArray.length()) {
                        val wordObj = businessWordsArray.getJSONObject(i)
                        val word = wordObj.getString("word")
                        val frequency = wordObj.getInt("frequency")
                        businessList.add(word)
                        wordsList.add(word)
                        frequencyMap[word] = frequency
                    }
                    businessWords = businessList
                }
            } else {
                // Old format - simple array
                val wordsArray = commonWordsData.getJSONArray("words")
                for (i in 0 until wordsArray.length()) {
                    wordsList.add(wordsArray.getString(i))
                }
            }
            commonWords = wordsList
            
            // Extract frequency data if available in old format
            if (commonWordsData.has("frequency")) {
                val frequencyData = commonWordsData.getJSONObject("frequency")
                val keys = frequencyData.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    frequencyMap[key] = frequencyData.getInt(key)
                }
            }
            wordFrequencies = frequencyMap
            
            // Load corrections
            val correctionsJson = assets.open("dictionaries/corrections.json").bufferedReader().use { it.readText() }
            val correctionsData = org.json.JSONObject(correctionsJson)
            
            // Extract corrections
            val correctionsObj = correctionsData.getJSONObject("corrections")
            val correctionsMap = mutableMapOf<String, String>()
            val correctionKeys = correctionsObj.keys()
            while (correctionKeys.hasNext()) {
                val key = correctionKeys.next()
                correctionsMap[key] = correctionsObj.getString(key)
            }
            corrections = correctionsMap
            
            // Extract contractions
            val contractionsObj = correctionsData.getJSONObject("contractions")
            val contractionsMap = mutableMapOf<String, List<String>>()
            val contractionKeys = contractionsObj.keys()
            while (contractionKeys.hasNext()) {
                val key = contractionKeys.next()
                val array = contractionsObj.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                contractionsMap[key] = list
            }
            contractions = contractionsMap
            
            // Load technology words
            try {
                val techWordsJson = assets.open("dictionaries/technology_words.json").bufferedReader().use { it.readText() }
                val techWordsData = org.json.JSONObject(techWordsJson)
                val techWordsArray = techWordsData.getJSONArray("technology_words")
                val techList = mutableListOf<String>()
                for (i in 0 until techWordsArray.length()) {
                    val wordObj = techWordsArray.getJSONObject(i)
                    val word = wordObj.getString("word")
                    val frequency = wordObj.getInt("frequency")
                    techList.add(word)
                    wordsList.add(word)
                    frequencyMap[word] = frequency
                }
                technologyWords = techList
            } catch (e: Exception) {
                Log.w(TAG, "Could not load technology words: ${e.message}")
            }
            
            // Combine all words for comprehensive suggestions
            allWords = (commonWords + businessWords + technologyWords).distinct()
            
            Log.d(TAG, "Loaded ${commonWords.size} common words, ${businessWords.size} business words, ${technologyWords.size} tech words, ${corrections.size} corrections, ${contractions.size} contractions")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dictionaries from assets", e)
            // Fallback to basic words
            commonWords = listOf("the", "and", "to", "of", "in", "is", "it", "you", "that", "he", "was", "for", "on", "are", "as", "with", "his", "they", "i", "at", "be", "this", "have", "from", "or", "one", "had", "by", "word", "but", "not", "what", "all", "were", "we", "when", "your", "can", "said")
            corrections = mapOf("teh" to "the", "adn" to "and", "hte" to "the", "nad" to "and", "yuo" to "you", "taht" to "that")
        }
    }
    
    /**
     * Get built-in correction for a word using loaded corrections dictionary
     */
    private fun getBuiltInCorrection(word: String): String? {
        return corrections[word.lowercase()]
    }
    
    /**
     * Generate enhanced basic suggestions with autocorrect when AI engines aren't ready
     */
    private fun generateEnhancedBasicSuggestions(currentWord: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        Log.d(TAG, "Generating enhanced basic suggestions for: '$currentWord' using ${allWords.size} dictionary words")
        
        if (currentWord.isEmpty()) {
            // Default suggestions when no word - use most frequent words
            val topWords = if (commonWords.isNotEmpty()) {
                commonWords.sortedByDescending { wordFrequencies[it] ?: 0 }.take(5)
            } else {
                listOf("I", "The", "In", "And", "To")
            }
            suggestions.addAll(topWords)
        } else {
            val word = currentWord.lowercase()
            
            // First, check for autocorrect using loaded corrections dictionary
            val correction = getBuiltInCorrection(word)
            if (correction != null && correction != word) {
                suggestions.add("✓ $correction") // Mark as correction
                Log.d(TAG, "Added autocorrect from dictionary: $word -> $correction")
            }
            
            // Find word completions from the comprehensive dictionary
            val matchingWords = allWords.filter { dictWord ->
                dictWord.lowercase().startsWith(word) && dictWord.lowercase() != word
            }.sortedByDescending { wordFrequencies[it] ?: 0 } // Sort by frequency
            
            // Add the matching words
            suggestions.addAll(matchingWords.take(4)) // Take top 4 matches
            
            // If we don't have enough suggestions, add some fallback completions
            if (suggestions.size < 3) {
                val fallbackWords = when {
                    word.startsWith("h") -> listOf("hello", "how", "have", "here", "help")
                    word.startsWith("w") -> listOf("what", "when", "where", "why", "will")
                    word.startsWith("t") -> listOf("the", "that", "this", "they", "then")
                    word.startsWith("i") -> listOf("I", "in", "is", "it", "if")
                    word.startsWith("a") -> listOf("and", "are", "all", "about", "as")
                    word.startsWith("s") -> listOf("so", "some", "see", "should", "said")
                    word.startsWith("c") -> listOf("can", "could", "come", "call", "change")
                    word.startsWith("m") -> listOf("make", "more", "may", "might", "most")
                    word.startsWith("n") -> listOf("not", "now", "new", "need", "never")
                    word.startsWith("g") -> listOf("get", "go", "good", "give", "great")
                    word.startsWith("b") -> listOf("be", "but", "by", "been", "before")
                    word.startsWith("d") -> listOf("do", "don't", "did", "does", "down")
                    word.startsWith("f") -> listOf("for", "from", "first", "find", "feel")
                    word.startsWith("l") -> listOf("like", "look", "let", "long", "little")
                    word.startsWith("o") -> listOf("of", "on", "or", "one", "only")
                    word.startsWith("p") -> listOf("people", "put", "part", "place", "point")
                    word.startsWith("r") -> listOf("right", "really", "run", "read", "remember")
                    word.startsWith("u") -> listOf("up", "use", "used", "under", "until")
                    word.startsWith("v") -> listOf("very", "view", "visit", "voice", "value")
                    word.startsWith("y") -> listOf("you", "your", "yes", "year", "yet")
                    else -> listOf("the", "and", "to", "of", "in")
                }
                
                // Add fallback words that start with the current word and aren't already in suggestions
                val fallbackMatches = fallbackWords.filter { fallback ->
                    fallback.lowercase().startsWith(word) && 
                    !suggestions.any { existing -> existing.replace("✓ ", "").lowercase() == fallback.lowercase() }
                }
                suggestions.addAll(fallbackMatches.take(5 - suggestions.size))
            }
        }
        
        Log.d(TAG, "Generated ${suggestions.size} suggestions: $suggestions")
        return suggestions.take(5)
    }
    
    private fun updateSuggestionUI(suggestions: List<String>) {
        Log.d(TAG, "updateSuggestionUI called with suggestions: $suggestions")
        Log.d(TAG, "suggestionContainer is null: ${suggestionContainer == null}")
        
        suggestionContainer?.let { container ->
            Log.d(TAG, "Container child count: ${container.childCount}")
            for (i in 0 until minOf(container.childCount, 5)) {
                val suggestionView = container.getChildAt(i) as TextView
                if (i < suggestions.size) {
                    suggestionView.text = suggestions[i]
                    suggestionView.visibility = View.VISIBLE
                    Log.d(TAG, "Set suggestion $i: ${suggestions[i]}")
                } else {
                    suggestionView.visibility = View.INVISIBLE
                }
            }
            container.visibility = View.VISIBLE
            Log.d(TAG, "Suggestion container made visible")
        } ?: run {
            Log.e(TAG, "suggestionContainer is null - cannot update suggestions")
        }
    }
    
    private fun applySuggestion(suggestion: String) {
        Log.d(TAG, "applySuggestion called with: '$suggestion', currentWord: '$currentWord'")
        
        val ic = currentInputConnection ?: run {
            Log.e(TAG, "No input connection available")
            return
        }
        
        // Clean suggestion text (remove correction indicators)
        val cleanSuggestion = suggestion.replace("✓ ", "").trim()
        Log.d(TAG, "Clean suggestion: '$cleanSuggestion'")
        
        // Replace current word with suggestion
        if (currentWord.isNotEmpty()) {
            Log.d(TAG, "Deleting current word of length: ${currentWord.length}")
            ic.deleteSurroundingText(currentWord.length, 0)
        }
        
        Log.d(TAG, "Committing text: '$cleanSuggestion '")
        ic.commitText("$cleanSuggestion ", 1)
        
        // Learn from user selection if AI is ready
        if (isAIReady && currentWord.isNotEmpty()) {
            val context = getRecentContext()
            aiBridge.learnCorrection(currentWord, cleanSuggestion, true)
        }
        
        // Update word history
        if (cleanSuggestion.isNotEmpty()) {
            wordHistory.add(cleanSuggestion)
            if (wordHistory.size > 20) {
                wordHistory.removeAt(0)
            }
        }
        
        // Clear current word and update suggestions
        currentWord = ""
        updateAISuggestions()
    }
    
    private fun playClick(keyCode: Int) {
        val am = getSystemService(AUDIO_SERVICE) as? AudioManager
        am?.let { audioManager ->
            when (keyCode) {
                32 -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR)
                Keyboard.KEYCODE_DONE, 10 -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN)
                Keyboard.KEYCODE_DELETE -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE)
                else -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
            }
        }
    }
    
    private fun playKeySound(primaryCode: Int) {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.let { audioManager ->
                var volume = soundVolume
                
                // Adjust volume based on sound intensity
                volume *= when (soundIntensity) {
                    1 -> 0.5f // Light
                    2 -> 0.8f // Medium
                    3 -> 1.0f // Strong
                    else -> 0.8f
                }
                
                // Different sounds for different key types
                when (primaryCode) {
                    32 -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, volume)
                    10, -4 -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, volume)
                    -5 -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE, volume)
                    else -> audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, volume)
                }
            }
        } catch (e: Exception) {
            // Ignore sound errors to prevent crashes
        }
    }
    
    override fun onPress(primaryCode: Int) {
        // Handle key press feedback with advanced features
        keyboardView?.let { view ->
            // Show key preview popup
            showKeyPreview(primaryCode)
            
            // Enhanced haptic feedback
            performAdvancedHapticFeedback(primaryCode)
            
            // Sound feedback
            if (soundIntensity > 0) {
                playKeySound(primaryCode)
            }
            
            // Setup long-press detection for accent characters
            if (hasAccentVariants(primaryCode)) {
                currentLongPressKey = primaryCode
                longPressHandler?.postDelayed({
                    showAccentOptions(primaryCode)
                }, LONG_PRESS_TIMEOUT)
            }
            
            // Check if this could be the start of swipe typing
            if (swipeTypingEnabled && Character.isLetter(primaryCode) && !isCurrentlySwiping) {
                // Potential start of swipe typing - wait for movement or release
                swipeStartTime = System.currentTimeMillis()
            }
        }
    }
    
    override fun onRelease(primaryCode: Int) {
        // Clean up long-press detection
        if (currentLongPressKey == primaryCode) {
            longPressHandler?.removeCallbacksAndMessages(null)
            currentLongPressKey = 0
        }
        
        // Hide key preview but keep accent options if they're showing
        if (accentPopup?.isShowing != true) {
            hideKeyPreview()
        }
        
        // Don't hide accent popup here - let user tap to select or tap outside to dismiss
    }
    
    private fun animateKeyPress(primaryCode: Int) {
        // No animation - keep keys stable and constant
        // Removed all scaling and visual effects
    }
    
    private fun animateKeyRelease(primaryCode: Int) {
        // No animation - keep keys stable and constant
        // Removed all scaling and visual effects
    }
    
    override fun onText(text: CharSequence?) {
        val ic = currentInputConnection ?: return
        text?.let { ic.commitText(it, 1) }
    }
    
    override fun swipeDown() {
        if (swipeTypingEnabled && isCurrentlySwiping) {
            finishSwipeTyping()
        } else {
            handleClose()
        }
    }
    
    override fun swipeLeft() {
        if (swipeTypingEnabled && isCurrentlySwiping) {
            // Continue swipe typing
            processSwipeMovement(-1) // Left direction
        } else {
            handleBackspace(currentInputConnection!!)
        }
    }
    
    override fun swipeRight() {
        if (swipeTypingEnabled && isCurrentlySwiping) {
            // Continue swipe typing
            processSwipeMovement(1) // Right direction
        } else {
            // Swipe right for space
            currentInputConnection?.commitText(" ", 1)
        }
    }
    
    override fun swipeUp() {
        if (swipeTypingEnabled && isCurrentlySwiping) {
            // Continue swipe typing
            processSwipeMovement(0) // Up direction
        } else {
            // Swipe up for shift
            handleShift()
        }
    }
    
    // Enhanced swipe typing methods
    private fun startSwipeTyping(primaryCode: Int) {
        if (!swipeTypingEnabled) return
        
        isCurrentlySwiping = true
        swipeStartTime = System.currentTimeMillis()
        swipePath.clear()
        swipeBuffer.setLength(0)
        swipePath.add(primaryCode)
        
        // Visual feedback for swipe start
        keyboardView?.setBackgroundColor(getSwipeActiveColor())
    }
    
    private fun processSwipeMovement(direction: Int) {
        if (!isCurrentlySwiping || !swipeTypingEnabled) return
        
        // Add direction to swipe path for processing
        swipePath.add(direction)
        
        // Process swipe path to predict words
        val predictedWord = processSwipePath(swipePath)
        if (predictedWord.isNotEmpty()) {
            // Update swipe buffer
            swipeBuffer.setLength(0)
            swipeBuffer.append(predictedWord)
            
            // Show prediction in suggestion bar
            showSwipePrediction(predictedWord)
        }
    }
    
    private fun finishSwipeTyping() {
        if (!isCurrentlySwiping || !swipeTypingEnabled) return
        
        isCurrentlySwiping = false
        
        // Process final swipe path
        val finalWord = processSwipePath(swipePath)
        
        if (finalWord.isNotEmpty()) {
            // Commit the swiped word
            currentInputConnection?.let { ic ->
                ic.commitText("$finalWord ", 1)
                updateAISuggestions()
            }
        }
        
        // Reset visual feedback
        keyboardView?.setBackgroundColor(getThemeBackgroundColor())
        
        // Clear swipe data
        swipePath.clear()
        swipeBuffer.setLength(0)
    }
    
    private fun processSwipePath(path: List<Int>): String {
        if (path.isEmpty()) return ""
        
        // Simple swipe-to-word mapping (basic implementation)
        // In a real implementation, this would use advanced algorithms
        val word = StringBuilder()
        
        path.forEach { code ->
            if (code > 0 && code < 256) {
                val c = code.toChar()
                if (Character.isLetter(c)) {
                    word.append(c)
                }
            }
        }
        
        // Apply basic swipe word corrections
        val result = word.toString().lowercase()
        return applySwipeCorrections(result)
    }
    
    private fun applySwipeCorrections(swipeWord: String): String {
        // Enhanced swipe pattern to word mapping
        return when (swipeWord.lowercase()) {
            "hello", "helo", "hllo" -> "hello"
            "and", "adn", "nad" -> "and"
            "the", "teh", "hte" -> "the"
            "you", "yuo", "oyu" -> "you"
            "are", "aer", "rae" -> "are"
            "to", "ot" -> "to"
            "for", "fro", "ofr" -> "for"
            "with", "wiht", "whit" -> "with"
            "that", "taht", "htat" -> "that"
            "this", "tihs", "htis" -> "this"
            "have", "ahve", "haev" -> "have"
            "from", "form", "fomr" -> "from"
            "they", "tehy", "yhte" -> "they"
            "know", "konw", "nkow" -> "know"
            "want", "wnat", "awnt" -> "want"
            "been", "eben", "neeb" -> "been"
            "good", "godo", "ogod" -> "good"
            "much", "muhc", "mcuh" -> "much"
            "some", "soem", "mose" -> "some"
            "time", "tmie", "itme" -> "time"
            "very", "vrey", "yrev" -> "very"
            "when", "wehn", "hwne" -> "when"
            "come", "coem", "moce" -> "come"
            "here", "hree", "ehre" -> "here"
            "just", "jsut", "ujst" -> "just"
            "like", "lkie", "ilke" -> "like"
            "over", "ovre", "roev" -> "over"
            "also", "aslo", "laso" -> "also"
            "back", "bakc", "cabk" -> "back"
            "after", "afetr", "atfer" -> "after"
            "use", "ues", "seu" -> "use"
            "two", "tow", "wto" -> "two"
            "how", "hwo", "ohw" -> "how"
            "our", "oru", "uro" -> "our"
            "work", "wokr", "rwok" -> "work"
            "first", "frist", "fisrt" -> "first"
            "well", "wlel", "ewll" -> "well"
            "way", "wya", "awy" -> "way"
            "even", "eevn", "nev" -> "even"
            "new", "nwe", "enw" -> "new"
            "year", "yaer", "yrea" -> "year"
            "would", "woudl", "wolud" -> "would"
            "people", "poeple", "peolpe" -> "people"
            "think", "thinl", "htink" -> "think"
            "where", "wheer", "hwere" -> "where"
            "being", "beinf", "beign" -> "being"
            "now", "nwo", "onw" -> "now"
            "make", "amke", "meak" -> "make"
            "most", "mots", "omst" -> "most"
            "get", "gte", "teg" -> "get"
            "see", "ese", "ees" -> "see"
            "him", "hmi", "ihm" -> "him"
            "has", "ahs", "sha" -> "has"
            "had", "ahd", "dha" -> "had"
            else -> if (swipeWord.length > 1) swipeWord else ""
        }
    }
    
    // Implement SwipeListener interface methods
    override fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String) {
        if (swipedKeys.isEmpty()) return
        
        // Convert key codes to characters
        val swipeWord = StringBuilder()
        swipedKeys.forEach { keyCode ->
            if (keyCode > 0 && keyCode < 256) {
                val c = keyCode.toChar()
                if (Character.isLetter(c)) {
                    swipeWord.append(Character.toLowerCase(c))
                }
            }
        }
        
        val word = swipeWord.toString()
        if (word.length > 1) {
            // Apply corrections and get final word
            val finalWord = applySwipeCorrections(word)
            
            if (finalWord.isNotEmpty()) {
                // Insert the swiped word
                currentInputConnection?.let { ic ->
                    ic.commitText("$finalWord ", 1)
                    updateAISuggestions()
                }
                
                // Show success feedback
                showSwipeSuccess(finalWord)
            }
        }
    }
    
    override fun onSwipeStarted() {
        try {
            // Hide any popups that might interfere with swipe
            hideAccentOptions()
            hideKeyPreview()
            
            // Visual feedback for swipe start
            keyboardView?.setBackgroundColor(getSwipeActiveColor())
            
            // Show swipe mode indicator
            showSwipeIndicator(true)
            
            // Clear any pending long-press actions
            longPressHandler?.removeCallbacksAndMessages(null)
            currentLongPressKey = 0
        } catch (e: Exception) {
            // Ignore swipe start errors
        }
    }
    
    override fun onSwipeEnded() {
        try {
            // Reset visual feedback
            keyboardView?.setBackgroundColor(getThemeBackgroundColor())
            
            // Hide swipe mode indicator
            showSwipeIndicator(false)
        } catch (e: Exception) {
            // Ignore swipe end errors
        }
    }
    
    private fun showSwipeSuccess(word: String) {
        // Show the swiped word in the first suggestion slot briefly
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as TextView
                firstSuggestion.apply {
                    text = "✓ $word"
                    setTextColor(Color.parseColor("#4CAF50")) // Green for success
                    visibility = View.VISIBLE
                }
                
                // Reset after 1 second
                mainHandler.postDelayed({
                    updateAISuggestions()
                }, 1000)
            }
        }
    }
    
    private fun showSwipeIndicator(show: Boolean) {
        suggestionContainer?.let { container ->
            if (container.childCount > 1) {
                val indicator = container.getChildAt(1) as TextView
                if (show) {
                    indicator.apply {
                        text = "🔄 Swipe"
                        setTextColor(getSwipeTextColor())
                        visibility = View.VISIBLE
                    }
                } else {
                    indicator.visibility = View.INVISIBLE
                }
            }
        }
    }
    
    private fun showSwipePrediction(prediction: String) {
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as TextView
                firstSuggestion.apply {
                    text = prediction
                    setTextColor(getSwipeTextColor())
                    visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun getSwipeActiveColor(): Int = Color.parseColor("#E3F2FD") // Light blue for swipe mode
    private fun getSwipeTextColor(): Int = Color.parseColor("#1976D2") // Blue for swipe predictions
    
    private fun handleClose() {
        requestHideSelf(0)
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        
        // Reset keyboard state
        caps = false
        keyboardView?.isShifted = caps
        
        // Reset current word and show initial suggestions
        currentWord = ""
        Log.d(TAG, "onStartInput - showing initial suggestions")
        updateAISuggestions()
        
        // Auto-capitalize for sentence start
        attribute?.let { info ->
            if (info.inputType != 0) {
                val inputType = info.inputType and EditorInfo.TYPE_MASK_CLASS
                if (inputType == EditorInfo.TYPE_CLASS_TEXT) {
                    val variation = info.inputType and EditorInfo.TYPE_MASK_VARIATION
                    if (variation == EditorInfo.TYPE_TEXT_VARIATION_NORMAL ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT) {
                        caps = true
                        keyboardView?.isShifted = caps
                    }
                }
            }
        }
        
        // Force load fresh settings when keyboard becomes active
        checkAndUpdateSettings()
        
        // Clear suggestions
        clearSuggestions()
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        clearSuggestions()
    }
    
    private fun clearSuggestions() {
        currentSuggestions.clear()
        suggestionContainer?.let { container ->
            mainHandler.post {
                for (i in 0 until container.childCount) {
                    container.getChildAt(i).visibility = View.INVISIBLE
                }
            }
        }
    }
    
    // Method to update settings from Flutter
    fun updateSettings(
        theme: String,
        aiSuggestions: Boolean,
        swipeTyping: Boolean,
        vibration: Boolean,
        keyPreview: Boolean
    ) {
        currentTheme = theme
        aiSuggestionsEnabled = aiSuggestions
        swipeTypingEnabled = swipeTyping
        vibrationEnabled = vibration
        keyPreviewEnabled = keyPreview
        
        // Load advanced feedback settings from preferences
        hapticIntensity = settings.getInt("haptic_intensity", 2)
        soundIntensity = settings.getInt("sound_intensity", 1)
        visualIntensity = settings.getInt("visual_intensity", 2)
        soundVolume = settings.getFloat("sound_volume", 0.3f)
        
        // Save to preferences
        settings.edit().apply {
            putString("keyboard_theme", theme)
            putBoolean("ai_suggestions", aiSuggestions)
            putBoolean("swipe_typing", swipeTyping)
            putBoolean("vibration_enabled", vibration)
            putBoolean("key_preview_enabled", keyPreview)
            apply()
        }
        
        // Apply settings immediately
        applyTheme()
        keyboardView?.let { view ->
            view.isPreviewEnabled = false // Always disable key preview for stable keys
            view.setSwipeEnabled(swipeTypingEnabled)
        }
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up AI bridge
        aiBridge.destroy()
        
        coroutineScope.cancel()
        
        // Stop settings polling
        stopSettingsPolling()
        
        // Clean up advanced keyboard resources
        longPressHandler?.removeCallbacksAndMessages(null)
        hideKeyPreview()
        hideAccentOptions()
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(settingsReceiver)
        } catch (e: Exception) {
            // Receiver was not registered or other errors
        }
        
        // Clear data
        wordHistory.clear()
        currentWord = ""
        isAIReady = false
    }
    
    private fun applySettingsImmediately() {
        try {
            // Apply theme changes
            applyTheme()
            
            // Update keyboard view settings
            keyboardView?.let { view ->
                view.isPreviewEnabled = false // Always disable key preview for stable keys
                view.setSwipeEnabled(swipeTypingEnabled)
                
                // Force refresh of the keyboard view
                view.invalidateAllKeys()
            }
            
            // Show feedback to user that settings were applied
            suggestionContainer?.let { container ->
                if (container.childCount > 0) {
                    val firstSuggestion = container.getChildAt(0) as TextView
                    firstSuggestion.apply {
                        text = "⚙️ Settings Updated"
                        setTextColor(Color.parseColor("#4CAF50"))
                        visibility = View.VISIBLE
                    }
                    
                    // Reset after 2 seconds
                    mainHandler.postDelayed({
                        try {
                            suggestionContainer?.let { cont ->
                                if (cont.childCount > 0) {
                                    val suggestion = cont.getChildAt(0) as TextView
                                    suggestion.visibility = View.INVISIBLE
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore UI update errors
                        }
                    }, 2000)
                }
            }
        } catch (e: Exception) {
            // Prevent crashes from settings application
        }
    }
    
    private fun startSettingsPolling() {
        if (settingsPoller != null) return
        
        settingsPoller = object : Runnable {
            override fun run() {
                try {
                    checkAndUpdateSettings()
                    // Poll every 2 seconds
                    settingsPoller?.let { mainHandler.postDelayed(it, 2000) }
                } catch (e: Exception) {
                    // Ignore polling errors
                }
            }
        }
        
        // Start polling after 1 second
        settingsPoller?.let { mainHandler.postDelayed(it, 1000) }
    }
    
    private fun stopSettingsPolling() {
        settingsPoller?.let { poller ->
            mainHandler.removeCallbacks(poller)
            settingsPoller = null
        }
    }
    
    private fun checkAndUpdateSettings() {
        try {
            // Check if SharedPreferences file was modified
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSettingsCheck > 1000) { // Check at most once per second
                lastSettingsCheck = currentTime
                
                // Store current settings
                val oldTheme = currentTheme
                val oldVibration = vibrationEnabled
                val oldSwipeTyping = swipeTypingEnabled
                val oldKeyPreview = keyPreviewEnabled
                val oldAISuggestions = aiSuggestionsEnabled
                
                // Reload settings
                loadSettings()
                
                // Check if any settings changed
                val settingsChanged = oldTheme != currentTheme ||
                        oldVibration != vibrationEnabled ||
                        oldSwipeTyping != swipeTypingEnabled ||
                        oldKeyPreview != keyPreviewEnabled ||
                        oldAISuggestions != aiSuggestionsEnabled
                
                if (settingsChanged) {
                    applySettingsImmediately()
                }
            }
        } catch (e: Exception) {
            // Ignore polling errors
        }
    }
    
    // ===== ADVANCED KEYBOARD FEATURES =====
    
    /**
     * Enhanced haptic feedback based on key type and intensity
     */
    private fun performAdvancedHapticFeedback(primaryCode: Int) {
        if (!vibrationEnabled || vibrator == null) return
        
        try {
            val intensity = when (primaryCode) {
                Keyboard.KEYCODE_DELETE, KEYCODE_SHIFT -> VibrationEffect.DEFAULT_AMPLITUDE * 1.2f
                KEYCODE_SPACE, Keyboard.KEYCODE_DONE -> VibrationEffect.DEFAULT_AMPLITUDE * 0.8f
                else -> VibrationEffect.DEFAULT_AMPLITUDE.toFloat()
            }.toInt().coerceIn(1, 255)
            
            val duration = when (hapticIntensity) {
                1 -> 10L // Light
                2 -> 20L // Medium  
                3 -> 40L // Heavy
                else -> 20L
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, intensity))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            // Fallback to keyboard view haptic feedback
            keyboardView?.performHapticFeedback(
                android.view.HapticFeedbackConstants.KEYBOARD_TAP,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }
    
    /**
     * Check if a key has accent variants for long-press
     */
    private fun hasAccentVariants(primaryCode: Int): Boolean {
        return accentMap.containsKey(primaryCode)
    }
    
    /**
     * Show key preview popup above pressed key
     */
    private fun showKeyPreview(primaryCode: Int) {
        if (!keyPreviewEnabled) return
        
        try {
            // Hide any existing preview first
            hideKeyPreview()
            
            val previewText = when (primaryCode) {
                KEYCODE_SPACE -> "space"
                Keyboard.KEYCODE_DELETE -> "⌫"
                Keyboard.KEYCODE_DONE -> "↵"
                KEYCODE_SHIFT -> "⇧"
                else -> {
                    val char = primaryCode.toChar()
                    if (Character.isLetter(char)) {
                        when (shiftState) {
                            SHIFT_OFF -> char.lowercaseChar().toString()
                            SHIFT_ON, SHIFT_CAPS -> char.uppercaseChar().toString()
                            else -> char.toString()
                        }
                    } else {
                        char.toString()
                    }
                }
            }
            
            // Create preview popup with proper focus handling
            val previewView = TextView(this).apply {
                text = previewText
                textSize = 24f
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
                setPadding(16, 8, 16, 8)
            }
            
            keyPreviewPopup = PopupWindow(
                previewView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Critical: Don't steal focus from keyboard
                isFocusable = false
                isOutsideTouchable = false
                isTouchable = false
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                
                // Prevent input method interference
                inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            }
            
            // Show popup above keyboard with safe positioning
            keyboardView?.let { view ->
                try {
                    keyPreviewPopup?.showAsDropDown(view, 0, -view.height - 100, Gravity.CENTER_HORIZONTAL)
                } catch (e: Exception) {
                    // Fallback: try showing at location
                    keyPreviewPopup?.showAtLocation(view, Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 100)
                }
            }
            
        } catch (e: Exception) {
            // Ignore preview errors and clean up
            hideKeyPreview()
        }
    }
    
    /**
     * Hide key preview popup
     */
    private fun hideKeyPreview() {
        try {
            keyPreviewPopup?.dismiss()
            keyPreviewPopup = null
        } catch (e: Exception) {
            // Ignore dismissal errors
        }
    }
    
    /**
     * Show accent options popup for long-press
     */
    private fun showAccentOptions(primaryCode: Int) {
        val accents = accentMap[primaryCode] ?: return
        if (accents.isEmpty()) return
        
        try {
            // Hide any existing popups first
            hideAccentOptions()
            hideKeyPreview()
            
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                setPadding(8, 8, 8, 8)
            }
            
            // Add original character first
            val originalChar = when (shiftState) {
                SHIFT_OFF -> primaryCode.toChar().lowercaseChar().toString()
                SHIFT_ON, SHIFT_CAPS -> primaryCode.toChar().uppercaseChar().toString()
                else -> primaryCode.toChar().toString()
            }
            addAccentOption(container, originalChar)
            
            // Add accent variants (apply shift state to them too)
            accents.forEach { accent ->
                val adjustedAccent = if (shiftState != SHIFT_OFF && accent.length == 1) {
                    accent.uppercase()
                } else {
                    accent
                }
                addAccentOption(container, adjustedAccent)
            }
            
            accentPopup = PopupWindow(
                container,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setBackgroundDrawable(ColorDrawable(Color.WHITE))
                
                // Critical: Prevent focus stealing that causes keyboard to close
                isFocusable = false  // Changed from true to false
                isOutsideTouchable = true
                isTouchable = true
                
                // Prevent input method interference
                inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
                
                elevation = 8f
                
                // Handle outside touch to dismiss popup without affecting keyboard
                setOnDismissListener {
                    try {
                        // Clean up but don't affect keyboard focus
                        currentLongPressKey = 0
                        longPressHandler?.removeCallbacksAndMessages(null)
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }
            }
            
            // Show popup above keyboard with safer positioning
            keyboardView?.let { view ->
                try {
                    accentPopup?.showAsDropDown(view, 0, -view.height - 150, Gravity.CENTER_HORIZONTAL)
                } catch (e: Exception) {
                    // Fallback positioning
                    accentPopup?.showAtLocation(view, Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 150)
                }
            }
            
        } catch (e: Exception) {
            // Ignore accent popup errors and clean up
            hideAccentOptions()
        }
    }
    
    /**
     * Add accent option to container
     */
    private fun addAccentOption(container: LinearLayout, accent: String) {
        val textView = TextView(this).apply {
            text = accent
            textSize = 20f
            setTextColor(Color.BLACK)
            setPadding(16, 12, 16, 12)
            setBackgroundColor(Color.LTGRAY)
            
            // Use touch listener instead of click listener for better control
            setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        setBackgroundColor(Color.GRAY) // Visual feedback
                        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        setBackgroundColor(Color.LTGRAY) // Reset background
                        
                        // Insert the selected accent
                        try {
                            currentInputConnection?.commitText(accent, 1)
                            
                            // Handle shift state after character input
                            if (shiftState == SHIFT_ON) {
                                shiftState = SHIFT_OFF
                                keyboardView?.let {
                                    it.isShifted = false
                                    it.invalidateAllKeys()
                                }
                                caps = false
                                isShifted = false
                            }
                            
                            // Hide popup after selection
                            hideAccentOptions()
                            
                            // Update AI suggestions
                            if (aiSuggestionsEnabled) {
                                updateAISuggestions()
                            }
                        } catch (e: Exception) {
                            // Handle any input errors gracefully
                            hideAccentOptions()
                        }
                        true
                    }
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        setBackgroundColor(Color.LTGRAY) // Reset background
                        true
                    }
                    else -> false
                }
            }
        }
        
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(4, 0, 4, 0)
        }
        
        textView.layoutParams = params
        container.addView(textView)
    }
    
    /**
     * Hide accent options popup
     */
    private fun hideAccentOptions() {
        try {
            accentPopup?.dismiss()
            accentPopup = null
        } catch (e: Exception) {
            // Ignore dismissal errors
        }
    }
    
    /**
     * Initialize CleverType AI Service
     */
    private fun initializeCleverTypeService() {
        try {
            cleverTypeService = CleverTypeAIService(this)
            Log.d(TAG, "CleverType AI Service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing CleverType AI Service", e)
        }
    }
    
    /**
     * Create CleverType toolbar with Grammar and Tone buttons
     */
    private fun createCleverTypeToolbar(): LinearLayout {
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setBackgroundColor(Color.parseColor("#f8f9fa"))
        }
        
        // Grammar correction button
        val grammarButton = createToolbarButton(
            text = "✅ Grammar",
            description = "Fix grammar & spelling",
            onClick = { handleGrammarCorrection() }
        )
        
        // Tone adjustment button
        val toneButton = createToolbarButton(
            text = "🎭 Tone",
            description = "Adjust writing tone",
            onClick = { handleToneAdjustment() }
        )
        
        // Add buttons to toolbar
        toolbar.addView(grammarButton)
        toolbar.addView(toneButton)
        
        Log.d(TAG, "CleverType toolbar created")
        return toolbar
    }
    
    /**
     * Create toolbar button
     */
    private fun createToolbarButton(
        text: String,
        description: String,
        onClick: () -> Unit
    ): LinearLayout {
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)
            }
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            background = createToolbarButtonBackground(Color.parseColor("#ffffff"))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        
        val buttonText = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1a73e8"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
        }
        
        val descText = TextView(this).apply {
            this.text = description
            textSize = 10f
            setTextColor(Color.parseColor("#5f6368"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(2), 0, 0)
        }
        
        buttonContainer.addView(buttonText)
        buttonContainer.addView(descText)
        
        return buttonContainer
    }
    
    /**
     * Handle grammar correction
     */
    private fun handleGrammarCorrection() {
        coroutineScope.launch {
            try {
                val selectedText = getSelectedText()
                if (selectedText.isEmpty()) {
                    Toast.makeText(this@AIKeyboardService, "Select text to fix grammar", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                Log.d(TAG, "Grammar correction requested for: $selectedText")
                
                // Show loading
                showCleverTypePreview()
                cleverTypePreview?.showLoading("Fixing grammar...")
                
                // Process with AI
                val result = cleverTypeService.fixGrammar(selectedText)
                
                // Show preview
                cleverTypePreview?.hideLoading()
                cleverTypePreview?.showGrammarPreview(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in grammar correction", e)
                Toast.makeText(this@AIKeyboardService, "Grammar correction failed", Toast.LENGTH_SHORT).show()
                hideCleverTypePreview()
            }
        }
    }
    
    /**
     * Handle tone adjustment
     */
    private fun handleToneAdjustment() {
        val selectedText = getSelectedText()
        if (selectedText.isEmpty()) {
            Toast.makeText(this, "Select text to adjust tone", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Tone adjustment requested for: $selectedText")
        showCleverTypeToneSelector()
    }
    
    /**
     * Get selected text or text around cursor
     */
    private fun getSelectedText(): String {
        val ic = currentInputConnection ?: return ""
        
        try {
            // Try to get selected text first
            val selectedText = ic.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                return selectedText.toString()
            }
            
            // If no selection, get text around cursor (sentence)
            val beforeCursor = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
            val afterCursor = ic.getTextAfterCursor(100, 0)?.toString() ?: ""
            
            // Find sentence boundaries
            val sentences = (beforeCursor + afterCursor).split(Regex("[.!?]\\s*"))
            if (sentences.isNotEmpty()) {
                // Return the sentence that contains the cursor
                val cursorPos = beforeCursor.length
                var charCount = 0
                for (sentence in sentences) {
                    charCount += sentence.length + 1
                    if (charCount > cursorPos) {
                        return sentence.trim()
                    }
                }
            }
            
            // Fallback: return text around cursor
            return (beforeCursor + afterCursor).trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting selected text", e)
            return ""
        }
    }
    
    /**
     * Show CleverType preview
     */
    private fun showCleverTypePreview() {
        if (cleverTypePreview == null) {
            cleverTypePreview = CleverTypePreview(this).apply {
                setOnApplyListener { processedText ->
                    applyProcessedText(processedText)
                    hideCleverTypePreview()
                }
                setOnCancelListener {
                    hideCleverTypePreview()
                }
                setOnCloseListener {
                    hideCleverTypePreview()
                }
            }
        }
        
        // Add to keyboard container
        keyboardContainer?.addView(cleverTypePreview)
    }
    
    /**
     * Hide CleverType preview
     */
    private fun hideCleverTypePreview() {
        cleverTypePreview?.let { preview ->
            keyboardContainer?.removeView(preview)
            preview.hide()
        }
    }
    
    /**
     * Show CleverType tone selector
     */
    private fun showCleverTypeToneSelector() {
        if (cleverTypeToneSelector == null) {
            cleverTypeToneSelector = CleverTypeToneSelector(this).apply {
                setOnToneSelectedListener { tone ->
                    handleToneSelected(tone)
                }
                setOnCloseListener {
                    hideCleverTypeToneSelector()
                }
            }
        }
        
        // Add to keyboard container
        keyboardContainer?.addView(cleverTypeToneSelector)
        cleverTypeToneSelector?.show()
    }
    
    /**
     * Hide CleverType tone selector
     */
    private fun hideCleverTypeToneSelector() {
        cleverTypeToneSelector?.let { selector ->
            keyboardContainer?.removeView(selector)
            selector.hide()
        }
    }
    
    /**
     * Handle tone selection
     */
    private fun handleToneSelected(tone: CleverTypeAIService.ToneType) {
        coroutineScope.launch {
            try {
                val selectedText = getSelectedText()
                if (selectedText.isEmpty()) return@launch
                
                Log.d(TAG, "Tone selected: ${tone.displayName}")
                
                // Hide tone selector
                hideCleverTypeToneSelector()
                
                // Show loading preview
                showCleverTypePreview()
                cleverTypePreview?.showLoading("Adjusting tone to ${tone.displayName}...")
                
                // Process with AI
                val result = cleverTypeService.adjustTone(selectedText, tone)
                
                // Show preview
                cleverTypePreview?.hideLoading()
                cleverTypePreview?.showTonePreview(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in tone adjustment", e)
                Toast.makeText(this@AIKeyboardService, "Tone adjustment failed", Toast.LENGTH_SHORT).show()
                hideCleverTypePreview()
            }
        }
    }
    
    /**
     * Apply processed text to input field
     */
    private fun applyProcessedText(processedText: String) {
        val ic = currentInputConnection ?: return
        
        try {
            // Get current selection
            val selectedText = ic.getSelectedText(0)
            
            if (!selectedText.isNullOrEmpty()) {
                // Replace selected text
                ic.commitText(processedText, 1)
            } else {
                // If no selection, replace text around cursor
                val beforeCursor = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
                val afterCursor = ic.getTextAfterCursor(100, 0)?.toString() ?: ""
                
                // Simple replacement: delete some text and insert processed text
                ic.deleteSurroundingText(beforeCursor.length, afterCursor.length)
                ic.commitText(processedText, 1)
            }
            
            Log.d(TAG, "Processed text applied: $processedText")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying processed text", e)
        }
    }
    
    /**
     * Create rounded background for toolbar buttons
     */
    private fun createToolbarButtonBackground(color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dpToPx(8).toFloat()
            setStroke(dpToPx(1), Color.parseColor("#e8eaed"))
        }
    }
}
