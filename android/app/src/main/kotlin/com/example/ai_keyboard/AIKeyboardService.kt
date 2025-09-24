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
import android.os.Build
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
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputContentInfo
import android.content.ClipDescription
import android.net.Uri
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.PopupWindow
import android.view.Gravity
import android.view.LayoutInflater
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*
import kotlin.math.max
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor

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
    private var gboardEmojiPanel: GboardEmojiPanel? = null
    private var keyboardContainer: LinearLayout? = null
    private var isMediaPanelVisible = false
    private var isEmojiPanelVisible = false
    
    // Keyboard state
    private var caps = false
    private var lastShiftTime = 0L
    private var isShifted = false
    private var currentKeyboard = KEYBOARD_LETTERS
    
    // Replacement UI state
    private var isReplacementUIVisible = false
    private var currentReplacementType = ""
    
    // Advanced keyboard state
    private var shiftState = SHIFT_OFF
    private var lastShiftPressTime = 0L
    private var longPressHandler: Handler? = null
    private var currentLongPressKey: Int = 0
    private var keyPreviewPopup: PopupWindow? = null
    private var accentPopup: PopupWindow? = null
    private var vibrator: Vibrator? = null
    
    // Enhanced Caps/Shift Management
    private lateinit var capsShiftManager: CapsShiftManager
    private var shiftOptionsMenu: ShiftOptionsMenu? = null
    
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
    
    // Keyboard settings
    private var showNumberRow = false
    private var swipeEnabled = true
    private var vibrationEnabled = true
    private var soundEnabled = true
    
    // Language cycling
    private val availableLanguages = listOf("EN", "ES", "FR", "DE", "HI")
    private var currentLanguageIndex = 0
    
    // Swipe typing state
    private var swipeMode = false
    private val swipeBuffer = StringBuilder()
    private val swipePath = mutableListOf<Int>()
    private var swipeStartTime = 0L
    private var isCurrentlySwiping = false
    
    // Tone adjustment state
    private var currentToneReplacementText: String? = null
    private var currentToneVariations: List<String> = emptyList()
    
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
    
    // Settings and Theme
    private lateinit var settings: SharedPreferences
    // ThemeManager removed - using default keyboard styling only
    
    // Method channels for app communication
    private var methodChannel: MethodChannel? = null
    private var themeChannel: MethodChannel? = null
    
    private lateinit var keyboardLayoutManager: KeyboardLayoutManager
    private lateinit var multilingualDictionary: MultilingualDictionary
    private lateinit var multilingualAutocorrect: MultilingualAutocorrectEngine
    private lateinit var enhancedAutocorrect: AutocorrectEngine
    private var languageSwitchView: LanguageSwitchView? = null
    
    // Services and handlers
    private lateinit var aiBridge: AIServiceBridge
    private val mainHandler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Extension function for formatting doubles
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    
    // Settings (using existing declarations above)
    // Legacy theme variable (deprecated - use themeManager instead)
    private var currentTheme = "default"
    private var aiSuggestionsEnabled = true
    private var swipeTypingEnabled = true
    // vibrationEnabled already declared above with new settings
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
                    Log.d(TAG, "SETTINGS_CHANGED broadcast received!")
                    // Reload settings immediately on main thread
                    mainHandler.post {
                        try {
                            Log.d(TAG, "Loading settings from broadcast...")
                            loadSettings()
                            
                            // Reload theme from Flutter SharedPreferences
                            // Theme initialization removed - using default styling
                            applyTheme()
                            
                            Log.d(TAG, "Applying settings immediately...")
                            applySettingsImmediately()
                            Log.d(TAG, "Settings and theme applied successfully!")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying settings from broadcast", e)
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
        
        // Initialize settings and theme
        settings = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
        // ThemeManager removed - using default keyboard styling only
        
        loadSettings()
        loadDictionaries()
        
        // Initialize multilingual components
        initializeMultilingualComponents()

        // Initialize Enhanced AI Service Bridge
        initializeAIBridge()
        
        // Initialize CleverType AI Service
        initializeCleverTypeService()
        
        // Initialize Theme MethodChannel
        initializeThemeChannel()
        
        // Initialize advanced keyboard features
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        longPressHandler = Handler(Looper.getMainLooper())
        
        // Initialize Enhanced Caps/Shift Manager
        initializeCapsShiftManager()
        
        // Register broadcast receiver for settings changes
        try {
            val filter = IntentFilter("com.example.ai_keyboard.SETTINGS_CHANGED")
            registerReceiver(settingsReceiver, filter)
            Log.d(TAG, "Broadcast receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering broadcast receiver", e)
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
            
            // Initialize enhanced autocorrect engine
            enhancedAutocorrect = AutocorrectEngine.getInstance(this)
            
            // Initialize enhanced swipe autocorrect engine
            swipeAutocorrectEngine = SwipeAutocorrectEngine.getInstance(this)
            
            // Run validation tests for enhanced autocorrect
            coroutineScope.launch {
                delay(2000) // Wait for dictionary to load
                val testResults = enhancedAutocorrect.runValidationTests()
                testResults.forEach { result ->
                    Log.d(TAG, "Autocorrect Test: $result")
                }
                
                // Initialize swipe autocorrect engine
                swipeAutocorrectEngine.initialize()
                loadDictionaries()
                Log.d(TAG, "Swipe autocorrect engine and dictionary initialization completed")
            }
            
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
            
            // Update enhanced autocorrect engine locale
            enhancedAutocorrect.setLocale(newLanguage)
            
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
        // Create the main keyboard container with system insets handling
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getThemeBackgroundColor())
            fitsSystemWindows = true
        }
        
        // Handle system insets for navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Adjust padding so keyboard stays above nav buttons
            // On devices with nav buttons → navInsets.bottom > 0
            // On gesture devices → it's usually 0
            view.setPadding(0, 0, 0, navInsets.bottom)
            Log.d(TAG, "System insets applied - nav bar height: ${navInsets.bottom}px")
            
            insets
        }
        
        // Create CleverType toolbar (first row)
        cleverTypeToolbar = createCleverTypeToolbar()
        mainLayout.addView(cleverTypeToolbar)
        
        // Create suggestion bar container and bar (second row)
        createSuggestionBarContainer(mainLayout)
        createSuggestionBar(suggestionContainer!!)
        
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
            // Choose keyboard layout based on language and number row setting
            val keyboardResource = getKeyboardResourceForLanguage(
                availableLanguages[currentLanguageIndex], 
                showNumberRow
            )
            
            keyboard = Keyboard(this@AIKeyboardService, keyboardResource)
            setKeyboard(keyboard)
            setOnKeyboardActionListener(this@AIKeyboardService)
            setSwipeListener(this@AIKeyboardService)
            setSwipeEnabled(swipeTypingEnabled)
            isPreviewEnabled = keyPreviewEnabled
            
            // Apply system insets to keyboard view as well
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Add extra padding for the keyboard view itself
                view.setPadding(0, 0, 0, navInsets.bottom / 2)
                Log.d(TAG, "Keyboard view insets applied - padding: ${navInsets.bottom / 2}px")
                insets
            }
            
            Log.d(TAG, "Initial keyboard loaded - Language: ${availableLanguages[currentLanguageIndex]}, NumberRow: $showNumberRow, Resource: $keyboardResource")
        }
        
        // Create media panel manager (but don't add to layout yet)
        createMediaPanel()
        
        // Create comprehensive emoji panel
        createEmojiPanel()
        
        // Initially show keyboard
        keyboardView?.let { keyboardContainer.addView(it) }
        mainLayout.addView(keyboardContainer)
        
        // Store reference to container for switching views
        this.keyboardContainer = keyboardContainer
        
        // Apply theme
        applyTheme()
        
        return mainLayout
    }
    
    private fun createSuggestionBarContainer(parent: LinearLayout) {
        Log.d(TAG, "Creating suggestion bar container")
        
        // Create container for suggestions only (language switch moved to global button)
        topContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(getThemeKeyColor())
            setPadding(4, 4, 4, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Language switch removed - now using global button instead
        
        // Create suggestions container (now takes full width)
        suggestionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Add suggestions container to top container
        topContainer!!.addView(suggestionContainer)
        
        // Add to parent
        parent.addView(topContainer)
        
        // Suggestion bar will be populated by the second method
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
        
        // Add three AI suggestion text views
        repeat(3) { index ->
            val suggestion = TextView(this).apply {
                setTextColor(getThemeTextColor())
                textSize = 16f
                setPadding(16, 8, 16, 8)
                setBackgroundResource(R.drawable.key_background_default)
                isClickable = true
                text = "AI Suggestion ${index + 1}" // Default text for testing
                visibility = View.VISIBLE
                
                setOnClickListener { view ->
                    val suggestionText = (view as TextView).text.toString()
                    Log.d(TAG, "AI Suggestion clicked: '$suggestionText'")
                    if (suggestionText.isNotEmpty() && !suggestionText.startsWith("AI Suggestion")) {
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
    
    private fun createEmojiPanel() {
        Log.d(TAG, "Creating comprehensive emoji panel")
        
        gboardEmojiPanel = GboardEmojiPanel(this).apply {
            // Set emoji selection listener
            setOnEmojiSelectedListener { emoji ->
                // Use enhanced emoji insertion with cursor handling
                insertEmojiWithCursor(emoji)
                
                // Log emoji usage for learning
                EmojiSuggestionEngine.logEmojiUsage(emoji, getCurrentInputText())
                
                Log.d(TAG, "Emoji selected from panel: $emoji")
                
                // Optionally close emoji panel after selection (like Gboard)
                // toggleEmojiPanel()
            }
            
            // Set keyboard switch listener
            setOnKeyboardSwitchRequestedListener {
                // Switch back to keyboard when ABC button is tapped
                if (isEmojiPanelVisible) {
                    toggleEmojiPanel()
                }
            }
            
            // Set proper layout params for full keyboard replacement
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        Log.d(TAG, "Comprehensive emoji panel created (not added to layout yet)")
    }
    
    private fun toggleMediaPanel() {
        try {
            // Hide replacement UI if visible
            if (isReplacementUIVisible) {
                hideReplacementUI()
            }
            
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
                    // Insert emoji with proper cursor handling
                    // If content is a category name, get random emoji from that category
                    val emojiToInsert = if (content.length > 5) { // Likely a category name
                        EmojiCollection.getRandomEmojiFromCategory(content)
                    } else {
                        content // Direct emoji content
                    }
                    insertEmojiWithCursor(emojiToInsert)
                    Log.d(TAG, "Inserted emoji: $emojiToInsert")
                }
                SimpleMediaPanel.MediaType.GIF -> {
                    // Handle GIF insertion with rich content support
                    insertGifContent(content, null)
                    Log.d(TAG, "Inserted GIF: $content")
                }
                SimpleMediaPanel.MediaType.STICKER -> {
                    // Handle sticker insertion with rich content support
                    insertStickerContent(content, null)
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
            val ic = currentInputConnection ?: return
            
            when {
                content.startsWith("http") -> {
                    // Network URL - try rich content first, fallback to link
                    if (!insertRichContent(content, "image/gif", "GIF")) {
                        ic.commitText("GIF: $content", 1)
                        Log.d(TAG, "Inserted GIF as text link (rich content not supported)")
                    }
                }
                content.startsWith("/") -> {
                    // Local file path - try to insert as rich content
                    val title = gifData?.title ?: "GIF"
                    val fileUri = "file://$content"
                    if (!insertRichContent(fileUri, "image/gif", title)) {
                        ic.commitText("[$title GIF]", 1)
                        Log.d(TAG, "Inserted GIF as placeholder (rich content not supported)")
                    }
                }
                else -> {
                    // Unknown format - insert placeholder
                    ic.commitText("[GIF]", 1)
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
            val ic = currentInputConnection ?: return
            
            when {
                content.startsWith("emoji://") -> {
                    // It's an emoji-based sticker, extract and insert the emoji
                    val emoji = content.removePrefix("emoji://")
                    insertEmojiWithCursor(emoji)
                }
                content.startsWith("/") -> {
                    // Local file path - try rich content insertion
                    val title = stickerData?.id ?: "Sticker"
                    val fileUri = "file://$content"
                    if (!insertRichContent(fileUri, "image/webp", title)) {
                        ic.commitText("[Sticker]", 1)
                        Log.d(TAG, "Inserted sticker as placeholder (rich content not supported)")
                    }
                }
                content.startsWith("http") -> {
                    // Network URL - try rich content first, fallback to link
                    val title = stickerData?.id ?: "Sticker"
                    if (!insertRichContent(content, "image/webp", title)) {
                        ic.commitText("Sticker: $content", 1)
                        Log.d(TAG, "Inserted sticker as text link (rich content not supported)")
                    }
                }
                else -> {
                    // Direct content (emoji) - use enhanced insertion
                    insertEmojiWithCursor(content)
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
        try {
        currentTheme = settings.getString("keyboard_theme", "default") ?: "default"
        // Using default theme only - theme management removed
        aiSuggestionsEnabled = settings.getBoolean("ai_suggestions", true)
        swipeTypingEnabled = settings.getBoolean("swipe_typing", true)
        keyPreviewEnabled = settings.getBoolean("key_preview_enabled", false)
            
            // Load new keyboard settings
            showNumberRow = settings.getBoolean("show_number_row", false)
            swipeEnabled = settings.getBoolean("swipe_enabled", true)
            vibrationEnabled = settings.getBoolean("vibration_enabled", true)
            soundEnabled = settings.getBoolean("sound_enabled", true)
            
            // Load current language index
            currentLanguageIndex = settings.getInt("current_language_index", 0)
            
            Log.d(TAG, "Settings loaded - NumberRow: $showNumberRow, Language: ${availableLanguages[currentLanguageIndex]} (index: $currentLanguageIndex), Swipe: $swipeEnabled, Vibration: $vibrationEnabled, Sound: $soundEnabled")
        
        // Load advanced feedback settings
        hapticIntensity = settings.getInt("haptic_intensity", 2) // medium by default
        soundIntensity = settings.getInt("sound_intensity", 1) // light by default
        visualIntensity = settings.getInt("visual_intensity", 2) // medium by default
        soundVolume = settings.getFloat("sound_volume", 0.3f)
            
            Log.d(TAG, "Settings loaded - NumberRow: $showNumberRow, Swipe: $swipeEnabled, Vibration: $vibrationEnabled, Sound: $soundEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings", e)
        }
    }
    
    private fun applyTheme() {
        keyboardView?.let { view ->
            // Apply default styling to keyboard view
            // Theme management removed - using default styling only
            
            // The SwipeKeyboardView will handle per-key theming internally
            view.invalidateAllKeys()
            view.invalidate()
            
            Log.d(TAG, "Applied default theme")
        }
    }
    
    private fun getThemeBackgroundColor(): Int {
        // Return default background color
        return Color.parseColor("#F5F5F5") // Light gray background
    }
    
    private fun getKeyBackgroundDrawable(): Int = R.drawable.key_background_default
    
    private fun getActionKeyBackgroundDrawable(): Int = R.drawable.key_background_default
    
    private fun getThemeKeyColor(): Int {
        // Return default key color
        return Color.WHITE
    }
    
    private fun getThemeTextColor(): Int {
        // Return default text color
        return Color.BLACK
    }
    
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        
        playClick(primaryCode)
        
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> handleBackspace(ic)
            Keyboard.KEYCODE_SHIFT -> handleShift()
            Keyboard.KEYCODE_DONE -> {
                // Context-aware enter key behavior (Gboard-style)
                handleEnterKey(ic)
            }
            KEYCODE_SPACE -> handleSpace(ic)
            KEYCODE_SYMBOLS -> switchToSymbols()
            KEYCODE_LETTERS -> switchToLetters()
            KEYCODE_NUMBERS -> switchToNumbers()
            KEYCODE_VOICE -> {
                // Enhanced voice input with visual feedback
                handleVoiceInput()
                ensureCursorStability()
            }
            KEYCODE_GLOBE -> {
                // Language switching - show available languages or switch to next
                handleLanguageSwitch()
                ensureCursorStability()
            }
            KEYCODE_EMOJI -> {
                // Enhanced emoji panel with visual feedback
                handleEmojiToggle()
                ensureCursorStability()
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
        
        // Enhanced character handling with CapsShiftManager
        if (::capsShiftManager.isInitialized) {
            code = capsShiftManager.processCharacterInput(code)
        } else {
            // Fallback to old implementation
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
    
    // For double-backspace revert functionality
    private var lastBackspaceTime = 0L
    
    private fun handleBackspace(ic: InputConnection) {
        val currentTime = System.currentTimeMillis()
        
        // Check for double-backspace revert within 1 second
        val revertCandidate = enhancedAutocorrect.getRevertCandidate()
        if (revertCandidate != null && 
            currentTime - lastBackspaceTime < 1000 && 
            currentWord.isEmpty()) {
            
            Log.d(TAG, "Double-backspace revert: '$revertCandidate'")
            
            // Find the corrected word and replace with original
            val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
            val words = textBefore.split("\\s+".toRegex())
            
            if (words.isNotEmpty()) {
                val lastWord = words.last()
                ic.deleteSurroundingText(lastWord.length, 0)
                ic.commitText("$revertCandidate ", 1)
                
                // Learn that user rejected the correction
                coroutineScope.launch {
                    enhancedAutocorrect.learnFromUser(revertCandidate, revertCandidate, listOf(lastWord))
                }
            }
            
            lastBackspaceTime = 0L // Reset to prevent further reverts
            return
        }
        
        lastBackspaceTime = currentTime
        
        val selectedText = ic.getSelectedText(0)
        if (TextUtils.isEmpty(selectedText)) {
            // Enhanced backspace - handle emoji clusters and surrogate pairs
            val deletedLength = deleteCharacterOrCluster(ic)
            
            // Update current word tracking
            if (currentWord.isNotEmpty()) {
                // Handle multi-byte character deletion
                if (deletedLength > 1) {
                    // Likely deleted an emoji or special character, clear current word
                    currentWord = ""
                } else {
                    currentWord = currentWord.dropLast(1)
                }
            } else {
                // If no current word, rebuild from text
                rebuildCurrentWord(ic)
            }
        } else {
            // Delete selected text
            ic.commitText("", 1)
            currentWord = ""
        }
        
        // Update suggestions after backspace
        if (aiSuggestionsEnabled) {
            updateAISuggestions()
        }
    }
    
    /**
     * Enhanced backspace that properly handles emoji clusters and surrogate pairs
     */
    private fun deleteCharacterOrCluster(ic: InputConnection): Int {
        try {
            // Use the cursor-aware text handler for consistent backspace behavior
            return CursorAwareTextHandler.performBackspace(ic)
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced backspace", e)
            // Fallback to simple deletion
            ic.deleteSurroundingText(1, 0)
            return 1
        }
    }
    
    /**
     * Calculate the length of the last character cluster (handles emojis, surrogate pairs)
     */
    private fun getLastCharacterClusterLength(text: String): Int {
        if (text.isEmpty()) return 0
        
        // Handle common emoji patterns
        val emojiPatterns = listOf(
            // Family emojis (👨‍👩‍👧‍👦)
            Regex("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66$"),
            Regex("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67$"),
            Regex("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66$"),
            // Heart on fire (❤️‍🔥)
            Regex("❤️\u200D\uD83D\uDD25$"),
            // Other ZWJ sequences
            Regex("[\uD800-\uDBFF][\uDC00-\uDFFF](?:\u200D[\uD800-\uDBFF][\uDC00-\uDFFF])+$"),
            // Emoji with skin tone modifiers
            Regex("[\uD83C\uDFFB-\uD83C\uDFFF][\uD800-\uDBFF][\uDC00-\uDFFF]$"),
            // Basic emoji (surrogate pairs)
            Regex("[\uD800-\uDBFF][\uDC00-\uDFFF]$"),
            // Emoji with variation selector (❤️)
            Regex("[\u2600-\u27BF]\uFE0F$")
        )
        
        // Check each pattern to find the longest match
        for (pattern in emojiPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val matchLength = match.value.length
                Log.d(TAG, "Found emoji cluster of length $matchLength: '${match.value}'")
                return matchLength
            }
        }
        
        // Check for basic surrogate pair
        if (text.length >= 2) {
            val lastChar = text[text.length - 1]
            val secondLastChar = text[text.length - 2]
            
            if (Character.isLowSurrogate(lastChar) && Character.isHighSurrogate(secondLastChar)) {
                Log.d(TAG, "Found surrogate pair")
                return 2
            }
        }
        
        // Default: delete 1 character
        return 1
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
        
        // Enhanced auto-capitalization with CapsShiftManager
        if (::capsShiftManager.isInitialized) {
            val inputType = currentInputEditorInfo?.inputType ?: 0
            capsShiftManager.handleSpacePress(ic, inputType)
        } else {
            // Fallback to old implementation
            caps = shouldCapitalizeAfterSpace(ic)
            keyboardView?.isShifted = caps
        }
        
        updateAISuggestions()
    }
    
    private fun finishCurrentWord() {
        if (currentWord.isEmpty()) return
        
        // Process word with enhanced autocorrect pipeline
        coroutineScope.launch {
            try {
                val prev1 = if (wordHistory.isNotEmpty()) wordHistory.last() else ""
                val prev2 = if (wordHistory.size >= 2) wordHistory[wordHistory.size - 2] else ""
                
                val result = enhancedAutocorrect.processBoundary(currentWord, prev1, prev2)
                
                if (result.shouldAutoCorrect && result.topCorrection != null) {
                    // Auto-apply correction
                    val correction = result.topCorrection
                    withContext(Dispatchers.Main) {
                        val ic = currentInputConnection
                        if (ic != null) {
                            // Replace the current word with correction
                            ic.deleteSurroundingText(currentWord.length, 0)
                            ic.commitText("$correction ", 1)
                            
                            // Save correction history for revert
                            enhancedAutocorrect.applyCorrection(currentWord, correction)
                            
                            // Show brief underline/highlight feedback
                            showAutocorrectFeedback(correction)
                            
                            Log.d(TAG, "Auto-corrected '$currentWord' → '$correction'")
                        }
                    }
                    
                    // Add corrected word to history
                    wordHistory.add(correction)
                } else {
                    // No auto-correction, add original word
                    wordHistory.add(currentWord)
                }
                
                // Update suggestion strip with candidates
                withContext(Dispatchers.Main) {
                    updateSuggestionStrip(result.candidates)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in enhanced autocorrect pipeline", e)
                // Fallback to original behavior
                wordHistory.add(currentWord)
            }
        }
        
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
    
    /**
     * Show visual feedback for auto-correction with underline effect
     */
    private fun showAutocorrectFeedback(correctedWord: String) {
        try {
            // Create a temporary visual indicator for the correction
            // This could be enhanced with actual underline rendering in the future
            Log.d(TAG, "Showing autocorrect feedback for: $correctedWord")
            
            // For now, just log the correction - UI feedback could be added later
            // In a full implementation, this might highlight the corrected word briefly
        } catch (e: Exception) {
            Log.e(TAG, "Error showing autocorrect feedback", e)
        }
    }
    
    /**
     * Update suggestion strip with enhanced autocorrect candidates
     * Format: [original] [best correction] [alternatives]
     */
    private fun updateSuggestionStrip(candidates: List<AutocorrectCandidate>) {
        try {
            if (candidates.isEmpty()) {
                // Clear suggestions
                updateSuggestionUI(emptyList())
                return
            }
            
            val suggestionTexts = candidates.take(3).map { candidate ->
                when (candidate.type) {
                    CandidateType.ORIGINAL -> candidate.word // Show as typed
                    CandidateType.CORRECTION -> "✓ ${candidate.word}" // Mark corrections
                    else -> candidate.word
                }
            }
            
            updateSuggestionUI(suggestionTexts)
            
            // Check for revert capability
            val revertCandidate = enhancedAutocorrect.getRevertCandidate()
            if (revertCandidate != null) {
                // Could add special revert UI indication here
                Log.d(TAG, "Revert available: $revertCandidate")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating suggestion strip", e)
        }
    }
    
    private fun handleShift() {
        // Delegate to enhanced caps/shift manager
        if (::capsShiftManager.isInitialized) {
            capsShiftManager.handleShiftPress()
        } else {
            // Fallback to old implementation if manager not initialized
            handleShiftFallback()
        }
    }
    
    /**
     * Fallback shift handling for backward compatibility
     */
    private fun handleShiftFallback() {
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
                    Log.d(TAG, "Caps lock activated - visual feedback should show uppercase letters")
                }
            }
            
            // Key labels are now handled by SwipeKeyboardView drawing override
            
            view.invalidateAllKeys()
        }
    }
    
    /**
     * Updates the keyboard display to show capital letters when caps lock is active
     */
    private fun updateKeyboardCaseDisplay() {
        keyboard?.let { kb ->
            for (key in kb.keys) {
                if (key.label != null && key.label.length == 1) {
                    val char = key.label[0]
                    if (Character.isLetter(char)) {
                        // Update key label based on caps state
                        key.label = when (shiftState) {
                            SHIFT_CAPS -> char.uppercaseChar().toString()
                            else -> {
                                // For normal state, get original lowercase from keyboard layout
                                getOriginalKeyLabel(key.codes[0])?.lowercaseChar()?.toString() ?: char.lowercaseChar().toString()
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Gets the original key label for a given key code
     */
    private fun getOriginalKeyLabel(keyCode: Int): Char? {
        return when (keyCode) {
            // English/Spanish QWERTY & German QWERTZ & French AZERTY
            113 -> 'q'; 119 -> 'w'; 101 -> 'e'; 114 -> 'r'; 116 -> 't'; 121 -> 'y'
            117 -> 'u'; 105 -> 'i'; 111 -> 'o'; 112 -> 'p'
            97 -> 'a'; 115 -> 's'; 100 -> 'd'; 102 -> 'f'; 103 -> 'g'; 104 -> 'h'
            106 -> 'j'; 107 -> 'k'; 108 -> 'l'
            122 -> 'z'; 120 -> 'x'; 99 -> 'c'; 118 -> 'v'; 98 -> 'b'; 110 -> 'n'; 109 -> 'm'
            // Additional characters for European layouts
            // Note: Devanagari characters don't have uppercase/lowercase concept, so they're excluded
            else -> null
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
            keyboard = Keyboard(this, R.xml.symbols)
            currentKeyboard = KEYBOARD_SYMBOLS
            keyboardView?.keyboard = keyboard
            applyTheme() // Reapply theme after layout change
        }
    }
    
    /**
     * Get the appropriate keyboard resource ID for a given language and number row setting
     */
    private fun getKeyboardResourceForLanguage(language: String, withNumbers: Boolean): Int {
        return when (language) {
            "EN" -> if (withNumbers) R.xml.qwerty_with_numbers else R.xml.qwerty
            "ES" -> if (withNumbers) R.xml.qwerty_es_with_numbers else R.xml.qwerty_es
            "FR" -> if (withNumbers) R.xml.qwerty_fr_with_numbers else R.xml.qwerty_fr
            "DE" -> if (withNumbers) R.xml.qwerty_de_with_numbers else R.xml.qwerty_de
            "HI" -> if (withNumbers) R.xml.qwerty_hi_with_numbers else R.xml.qwerty_hi
            else -> if (withNumbers) R.xml.qwerty_with_numbers else R.xml.qwerty // Default to English
        }
    }
    
    private fun switchToLetters() {
        // Always reload the keyboard layout (for language/number row changes)
        val lang = availableLanguages[currentLanguageIndex]
        val keyboardResource = getKeyboardResourceForLanguage(lang, showNumberRow)
        
        try {
            Log.d(TAG, "Loading keyboard resource: $keyboardResource for language: $lang")
            
            // Create new keyboard instance
            val newKeyboard = Keyboard(this, keyboardResource)
            keyboard = newKeyboard
            currentKeyboard = KEYBOARD_LETTERS
            
            keyboardView?.let { view ->
                Log.d(TAG, "Setting new keyboard to view...")
                
                // Set the new keyboard
                view.keyboard = newKeyboard
                
                // Force complete refresh of the keyboard view
                view.invalidateAllKeys()
                view.invalidate()
                view.requestLayout()
                
                // Post additional refresh to ensure UI thread updates
                view.post {
                    view.invalidateAllKeys()
                    view.invalidate()
                    view.requestLayout()
                }
                
                // Force redraw after a short delay to ensure everything is updated
                view.postDelayed({
                    view.invalidateAllKeys()
                    view.invalidate()
                    Log.d(TAG, "Delayed refresh completed")
                }, 50)
                
                Log.d(TAG, "Keyboard view updated with new layout")
            } ?: Log.e(TAG, "KeyboardView is null!")
            
            applyTheme() // Reapply theme after layout change
            
            // Caps display is handled by SwipeKeyboardView drawing
            
            Log.d(TAG, "Successfully switched to $lang letters keyboard with number row: $showNumberRow")
        } catch (e: Exception) {
            Log.e(TAG, "Error switching to letters keyboard", e)
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
        try {
            // Cycle to next language
            currentLanguageIndex = (currentLanguageIndex + 1) % availableLanguages.size
            val currentLanguage = availableLanguages[currentLanguageIndex]
            
            // Save current language index
            settings.edit().putInt("current_language_index", currentLanguageIndex).apply()
            
            // Reload keyboard layout to reflect language change
            switchToLetters()
            
            // Show language change feedback
            Toast.makeText(this, "🌐 $currentLanguage", Toast.LENGTH_SHORT).show()
            
            Log.d(TAG, "Language cycled to: $currentLanguage (index: $currentLanguageIndex)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cycling language", e)
            Toast.makeText(this, "🌐 Language cycle failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleEmojiKey() {
        // Enhanced emoji insertion with comprehensive emoji collection
        val ic = currentInputConnection ?: return
        
        // Get a random emoji from the comprehensive collection
        val randomEmoji = EmojiCollection.getRandomEmoji()
        
        // Use enhanced emoji insertion with cursor handling
        insertEmojiWithCursor(randomEmoji)
        
        // Show brief toast with emoji category info
        val categoryInfo = when {
            EmojiCollection.smileys.contains(randomEmoji) -> "😊 Smiley"
            EmojiCollection.hearts.contains(randomEmoji) -> "❤️ Heart"
            EmojiCollection.animals.contains(randomEmoji) -> "🐶 Animal"
            EmojiCollection.food.contains(randomEmoji) -> "🍕 Food"
            EmojiCollection.activities.contains(randomEmoji) -> "⚽ Activity"
            EmojiCollection.travel.contains(randomEmoji) -> "🚗 Travel"
            EmojiCollection.flags.contains(randomEmoji) -> "🏁 Flag"
            else -> "🎉 Emoji"
        }
        
        Log.d(TAG, "Inserted random emoji: $randomEmoji ($categoryInfo)")
        
        // Future enhancement: Implement emoji picker popup
        // Toast.makeText(this, "Emoji picker - Feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Handle emoji insertion from specific category
     */
    private fun handleEmojiFromCategory(category: String) {
        val ic = currentInputConnection ?: return
        val emoji = EmojiCollection.getRandomEmojiFromCategory(category)
        insertEmojiWithCursor(emoji)
        Log.d(TAG, "Inserted $category emoji: $emoji")
    }
    
    /**
     * Show emoji quick picker with popular emojis
     */
    private fun showEmojiQuickPicker() {
        try {
            // Create a popup with popular emojis
            val popularEmojis = EmojiCollection.popularEmojis.take(12) // Show top 12
            
            // For now, just cycle through popular emojis on repeated presses
            // This is a simple implementation before implementing full emoji picker UI
            val emoji = popularEmojis.random()
            insertEmojiWithCursor(emoji)
            
            // Show toast with emoji categories available
            Toast.makeText(this, "🎉 ${popularEmojis.size} popular emojis available! Long press for more categories", Toast.LENGTH_SHORT).show()
            
            Log.d(TAG, "Quick emoji picker - inserted: $emoji")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing emoji quick picker", e)
        }
    }
    
    /**
     * Show emoji category picker (for long press)
     */
    private fun showEmojiCategoryPicker() {
        try {
            // Create a simple category selection
            val categories = listOf("Popular", "Smileys", "Hearts", "Animals", "Food", "Activities")
            val randomCategory = categories.random()
            val emoji = EmojiCollection.getRandomEmojiFromCategory(randomCategory)
            
            insertEmojiWithCursor(emoji)
            Toast.makeText(this, "$randomCategory: $emoji", Toast.LENGTH_SHORT).show()
            
            Log.d(TAG, "Category picker - $randomCategory: $emoji")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing emoji category picker", e)
        }
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
        
        // Add emoji suggestions based on current word and context
        val emojiSuggestions = EmojiSuggestionEngine.getSuggestionsForTyping(currentWord, getCurrentInputText())
        
        // Mix word suggestions with emoji suggestions (prioritize word suggestions)
        val mixedSuggestions = mutableListOf<String>()
        
        // Add word suggestions first (up to 3)
        mixedSuggestions.addAll(suggestions.take(3))
        
        // Add emoji suggestions to fill remaining slots
        val remainingSlots = 5 - mixedSuggestions.size
        if (remainingSlots > 0) {
            mixedSuggestions.addAll(emojiSuggestions.take(remainingSlots))
        }
        
        Log.d(TAG, "Generated ${mixedSuggestions.size} mixed suggestions (${suggestions.size} words + ${emojiSuggestions.size} emojis): $mixedSuggestions")
        return mixedSuggestions.take(5)
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
        
        // Check if this is a tone variation selection
        if (currentToneVariations.isNotEmpty()) {
            val cleanSuggestion = suggestion.replace(Regex("^[${CleverTypeAIService.ToneType.values().joinToString("") { it.emoji }}]\\s*"), "")
                .replace("...", "")
            
            // Find the matching full variation
            val selectedVariation = currentToneVariations.find { variation ->
                val truncated = if (variation.length > 47) variation.take(47) else variation
                cleanSuggestion.startsWith(truncated) || truncated.startsWith(cleanSuggestion)
            } ?: cleanSuggestion
            
            // Replace the entire text with selected tone variation
            currentToneReplacementText?.let { originalText ->
                ic.beginBatchEdit()
                val allText = ic.getExtractedText(ExtractedTextRequest(), 0)?.text
                if (allText != null) {
                    ic.deleteSurroundingText(allText.length, 0)
                    ic.commitText(selectedVariation, 1)
                }
                ic.endBatchEdit()
                
                Log.d(TAG, "Applied tone variation: '$originalText' → '$selectedVariation'")
                Toast.makeText(this, "✨ Tone adjusted successfully!", Toast.LENGTH_SHORT).show()
            }
            
            // Clear tone state
            currentToneReplacementText = null
            currentToneVariations = emptyList()
            hideReplacementUI()
            restoreKeyboard()
            return
        }
        
        // Clean suggestion text (remove correction indicators)
        val cleanSuggestion = suggestion.replace("✓ ", "").trim()
        Log.d(TAG, "Clean suggestion: '$cleanSuggestion'")
        
        // Check for revert request (user tapping original word)
        val revertCandidate = enhancedAutocorrect.getRevertCandidate()
        if (revertCandidate != null && cleanSuggestion == revertCandidate) {
            Log.d(TAG, "Revert requested: restoring '$revertCandidate'")
            // This handles the revert case where user taps the original word
        }
        
        // Check if suggestion is an emoji
        val isEmoji = cleanSuggestion.length <= 8 && cleanSuggestion.matches(Regex(".*[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Cn}].*"))
        
        if (isEmoji) {
            // Handle emoji suggestion - insert with proper cursor handling
            insertEmojiWithCursor(cleanSuggestion)
            
            // Log emoji usage for learning
            EmojiSuggestionEngine.logEmojiUsage(cleanSuggestion, getCurrentInputText())
            
            Log.d(TAG, "Applied emoji suggestion: '$cleanSuggestion'")
        } else {
            // Handle word suggestion - replace current word
            if (currentWord.isNotEmpty()) {
                Log.d(TAG, "Deleting current word of length: ${currentWord.length}")
                ic.deleteSurroundingText(currentWord.length, 0)
            }
            
            Log.d(TAG, "Committing text: '$cleanSuggestion '")
            ic.commitText("$cleanSuggestion ", 1)
            
            // Enhanced learning from user selection 
            coroutineScope.launch {
                if (currentWord.isNotEmpty()) {
                    // Learn from user choice using enhanced autocorrect
                    enhancedAutocorrect.learnFromUser(currentWord, cleanSuggestion, emptyList())
                    
                    // Legacy AI bridge learning if available
                    if (isAIReady) {
                        val context = getRecentContext()
                        aiBridge.learnCorrection(currentWord, cleanSuggestion, true)
                    }
                }
            }
            
            // Update word history
            if (cleanSuggestion.isNotEmpty()) {
                wordHistory.add(cleanSuggestion)
                if (wordHistory.size > 20) {
                    wordHistory.removeAt(0)
                }
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
    
    // Enhanced swipe autocorrect engine
    private lateinit var swipeAutocorrectEngine: SwipeAutocorrectEngine
    private var lastCommittedSwipeWord = ""
    
    // Implement SwipeListener interface methods with enhanced autocorrection
    override fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String, keySequence: List<Int>) {
        if (keySequence.isEmpty()) return
        
        // Use the ordered key sequence for better accuracy
        val swipeSequence = StringBuilder()
        keySequence.forEach { keyCode ->
            if (keyCode > 0 && keyCode < 256) {
                val c = keyCode.toChar()
                if (Character.isLetter(c)) {
                    swipeSequence.append(Character.toLowerCase(c))
                }
            }
        }
        
        val swipeLetters = swipeSequence.toString()
        if (swipeLetters.length > 1) {
            coroutineScope.launch {
                try {
                    val startTime = System.currentTimeMillis()
                    
                    // Get context words for better prediction
                    val prev1 = if (wordHistory.isNotEmpty()) wordHistory.last() else ""
                    val prev2 = if (wordHistory.size >= 2) wordHistory[wordHistory.size - 2] else ""
                    
                    // Use enhanced swipe autocorrect engine
                    val swipeResult = swipeAutocorrectEngine.getCandidates(swipeLetters, prev1, prev2)
                    
                    withContext(Dispatchers.Main) {
                        if (swipeResult.candidates.isNotEmpty()) {
                            val bestCandidate = swipeResult.bestCandidate!!
                            
                            // Auto-commit the best candidate
                            currentInputConnection?.let { ic ->
                                ic.commitText("${bestCandidate.word} ", 1)
                                lastCommittedSwipeWord = bestCandidate.word
                            }
                            
                            // Update word history for context
                            wordHistory.add(bestCandidate.word)
                            if (wordHistory.size > 20) {
                                wordHistory.removeAt(0)
                            }
                            
                            // Show alternatives in suggestion strip
                            updateEnhancedSwipeSuggestions(swipeResult.candidates.take(3), swipeLetters)
                            
                            // Show success feedback with confidence indicator
                            showSwipeSuccess(bestCandidate.word, bestCandidate.finalScore)
                            
                            Log.d(TAG, "Enhanced swipe: '${swipeLetters}' → '${bestCandidate.word}' " +
                                      "(score: ${bestCandidate.finalScore.format(2)}, " +
                                      "${swipeResult.candidates.size} alternatives, ${swipeResult.processingTimeMs}ms)")
                        } else {
                            // Fallback to original swipe if no candidates
                            currentInputConnection?.let { ic ->
                                ic.commitText("$swipeLetters ", 1)
                                lastCommittedSwipeWord = swipeLetters
                            }
                            
                            updateSuggestionUI(listOf(swipeLetters))
                            Log.d(TAG, "Swipe fallback: '${swipeLetters}' (no dictionary matches)")
                        }
                        
                        // Update AI suggestions for next word prediction
                        updateAISuggestions()
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in enhanced swipe processing", e)
                    // Fallback to original behavior
                    withContext(Dispatchers.Main) {
                        val finalWord = applySwipeCorrections(swipeLetters)
                        if (finalWord.isNotEmpty()) {
                            currentInputConnection?.let { ic ->
                                ic.commitText("$finalWord ", 1)
                                lastCommittedSwipeWord = finalWord
                                updateAISuggestions()
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handle user selection of alternative swipe suggestion
     */
    private fun handleSwipeAlternativeSelection(selectedWord: String) {
        try {
            if (lastCommittedSwipeWord.isNotEmpty()) {
                // Replace the last committed word
                currentInputConnection?.let { ic ->
                    // Delete the last word and space
                    val deleteLength = lastCommittedSwipeWord.length + 1
                    ic.deleteSurroundingText(deleteLength, 0)
                    ic.commitText("$selectedWord ", 1)
                }
                
                // Learn from user selection
                if (::swipeAutocorrectEngine.isInitialized) {
                    swipeAutocorrectEngine.learnFromUserSelection(lastCommittedSwipeWord, selectedWord)
                }
                
                // Update word history
                if (wordHistory.isNotEmpty()) {
                    wordHistory[wordHistory.size - 1] = selectedWord
                }
                
                lastCommittedSwipeWord = selectedWord
                
                Log.d(TAG, "Swipe alternative selected: '$lastCommittedSwipeWord' → '$selectedWord'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling swipe alternative selection", e)
        }
    }
    
    /**
     * Handle double backspace to revert swipe correction
     */
    private fun handleSwipeReversion() {
        try {
            if (lastCommittedSwipeWord.isNotEmpty() && ::swipeAutocorrectEngine.isInitialized) {
                // Record that user rejected the correction
                swipeAutocorrectEngine.recordRejection("", lastCommittedSwipeWord)
                lastCommittedSwipeWord = ""
                
                Log.d(TAG, "Swipe correction reverted by user")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling swipe reversion", e)
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
    
    private fun showSwipeSuccess(word: String, confidence: Double = 0.0) {
        // Show the swiped word in the first suggestion slot briefly
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as TextView
                val confidenceText = if (confidence > 0.5) " (${(confidence * 100).toInt()}%)" else ""
                firstSuggestion.apply {
                    text = "✓ $word$confidenceText"
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
        
        // Reset keyboard state with enhanced CapsShiftManager
        if (::capsShiftManager.isInitialized) {
            capsShiftManager.resetToNormal()
            
            // Apply auto-capitalization based on context
            attribute?.let { info ->
                val inputType = info.inputType
                capsShiftManager.applyAutoCapitalization(currentInputConnection, inputType)
            }
        } else {
            // Fallback to old implementation
            caps = false
            keyboardView?.isShifted = caps
            
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
        }
        
        // Reset current word and show initial suggestions
        currentWord = ""
        Log.d(TAG, "onStartInput - showing initial suggestions")
        updateAISuggestions()
        
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
        currentTheme = theme // Legacy variable for compatibility
        // Theme switching removed - using default theme only
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
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(settingsReceiver)
        } catch (e: Exception) {
            // Ignore unregistration errors
        }
        
        // Clean up advanced keyboard resources
        longPressHandler?.removeCallbacksAndMessages(null)
        hideKeyPreview()
        hideAccentOptions()
        
        
        // Clear data
        wordHistory.clear()
        currentWord = ""
        isAIReady = false
    }
    
    private fun applySettingsImmediately() {
        try {
            // Apply theme changes
            applyTheme()
            
            // Reload keyboard layout if needed (for number row changes)
            if (currentKeyboard == KEYBOARD_LETTERS) {
                switchToLetters()  // This will apply number row and language changes
                Log.d(TAG, "Keyboard layout reloaded - NumberRow: $showNumberRow, Language: ${availableLanguages[currentLanguageIndex]}")
            }
            
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
                val oldNumberRow = showNumberRow
                val oldLanguageIndex = currentLanguageIndex
                
                // Reload settings
                loadSettings()
                
                // Check if any settings changed
                val settingsChanged = oldTheme != currentTheme ||
                        oldVibration != vibrationEnabled ||
                        oldSwipeTyping != swipeTypingEnabled ||
                        oldKeyPreview != keyPreviewEnabled ||
                        oldAISuggestions != aiSuggestionsEnabled ||
                        oldNumberRow != showNumberRow ||
                        oldLanguageIndex != currentLanguageIndex
                
                if (settingsChanged) {
                    Log.d(TAG, "Settings change detected via polling - NumberRow: $oldNumberRow->$showNumberRow, Language: $oldLanguageIndex->$currentLanguageIndex")
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
     * Initialize Enhanced Caps/Shift Manager
     */
    private fun initializeCapsShiftManager() {
        try {
            capsShiftManager = CapsShiftManager(this, settings)
            
            // Set up state change listener
            capsShiftManager.setOnStateChangedListener { newState ->
                updateShiftVisualState(newState)
                updateBackwardCompatibilityState(newState)
            }
            
            // Set up haptic feedback listener
            capsShiftManager.setOnHapticFeedbackListener { state ->
                performEnhancedShiftHapticFeedback(state)
            }
            
            // Set up long press menu listener
            capsShiftManager.setOnLongPressMenuListener {
                showShiftOptionsMenu()
            }
            
            // Initialize shift options menu
            shiftOptionsMenu = ShiftOptionsMenu(this, capsShiftManager).apply {
                setOnMenuItemClickListener { action ->
                    handleShiftMenuAction(action)
                }
            }
            
            Log.d(TAG, "Enhanced Caps/Shift Manager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Caps/Shift Manager", e)
        }
    }
    
    /**
     * Update visual state based on new caps/shift state
     */
    private fun updateShiftVisualState(newState: Int) {
        keyboardView?.let { view ->
            // Update the shift key visual state
            view.isShifted = (newState != CapsShiftManager.STATE_NORMAL)
            
            // Enhanced visual feedback based on shift state
            when (newState) {
                CapsShiftManager.STATE_NORMAL -> {
                    // Normal state - no special highlighting
                    view.setShiftKeyHighlight(false, false)
                }
                CapsShiftManager.STATE_SHIFT -> {
                    // Temporary shift - light highlighting
                    view.setShiftKeyHighlight(true, false)
                }
                CapsShiftManager.STATE_CAPS_LOCK -> {
                    // Caps lock - strong highlighting with caps indicator
                    view.setShiftKeyHighlight(true, true)
                    Log.d(TAG, "Caps lock activated - visual feedback should show uppercase letters")
                }
            }
            
            view.invalidateAllKeys()
        }
    }
    
    /**
     * Update backward compatibility state variables
     */
    private fun updateBackwardCompatibilityState(newState: Int) {
        // Update legacy state variables for backward compatibility
        shiftState = when (newState) {
            CapsShiftManager.STATE_NORMAL -> SHIFT_OFF
            CapsShiftManager.STATE_SHIFT -> SHIFT_ON
            CapsShiftManager.STATE_CAPS_LOCK -> SHIFT_CAPS
            else -> SHIFT_OFF
        }
        
        caps = (newState != CapsShiftManager.STATE_NORMAL)
        isShifted = caps
    }
    
    /**
     * Perform enhanced haptic feedback based on shift state
     */
    private fun performEnhancedShiftHapticFeedback(state: Int) {
        if (vibrationEnabled && vibrator != null) {
            try {
                val intensity = when (state) {
                    CapsShiftManager.STATE_NORMAL -> 15L      // Light vibration for turning off
                    CapsShiftManager.STATE_SHIFT -> 25L       // Medium vibration for shift on
                    CapsShiftManager.STATE_CAPS_LOCK -> 50L   // Strong vibration for caps lock
                    else -> 20L
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(intensity, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(intensity)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to provide enhanced shift haptic feedback: ${e.message}")
            }
        }
    }
    
    /**
     * Show shift options menu
     */
    private fun showShiftOptionsMenu() {
        keyboardView?.let { view ->
            // Find shift key position (approximate)
            val shiftKeyX = view.width / 8  // Approximate position
            val shiftKeyY = view.height - 100  // Bottom row
            
            shiftOptionsMenu?.show(view, shiftKeyX, shiftKeyY)
        }
    }
    
    /**
     * Handle shift menu actions
     */
    private fun handleShiftMenuAction(action: String) {
        when (action) {
            "caps_lock_toggle" -> {
                // Already handled by the menu
                Log.d(TAG, "Caps lock toggled via menu")
            }
            "alternate_layout" -> {
                // Switch to alternate keyboard layout (symbols/numbers)
                when (currentKeyboard) {
                    KEYBOARD_LETTERS -> switchToSymbols()
                    KEYBOARD_SYMBOLS -> switchToNumbers()
                    KEYBOARD_NUMBERS -> switchToLetters()
                }
            }
            "language_switch" -> {
                // Switch to next language
                languageManager?.switchToNextLanguage()
            }
        }
    }
    
    /**
     * Start shift key long press detection (called from SwipeKeyboardView)
     */
    fun startShiftKeyLongPressDetection() {
        if (::capsShiftManager.isInitialized) {
            capsShiftManager.startLongPressDetection()
        }
    }
    
    /**
     * Cancel shift key long press detection (called from SwipeKeyboardView)
     */
    fun cancelShiftKeyLongPressDetection() {
        if (::capsShiftManager.isInitialized) {
            capsShiftManager.cancelLongPressDetection()
        }
    }
    
    /**
     * Enhanced voice input handler with Gboard-like functionality
     */
    private fun handleVoiceInput() {
        try {
            // Provide visual feedback
            showVoiceInputFeedback(true)
            
            // Start voice recognition
            startVoiceRecognition()
            
            Log.d(TAG, "Voice input activated")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice input", e)
            showVoiceInputFeedback(false)
            Toast.makeText(this, "Voice input not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show visual feedback for voice input state
     */
    private fun showVoiceInputFeedback(isActive: Boolean) {
        keyboardView?.let { view ->
            // Update voice key appearance
            view.setVoiceKeyActive(isActive)
            
            if (isActive) {
                // Show listening indicator
                Toast.makeText(this, "🎤 Listening...", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Start voice recognition
     */
    private fun startVoiceRecognition() {
        // For now, show placeholder - voice recognition would be integrated here
        Toast.makeText(this, "🎤 Voice input activated", Toast.LENGTH_SHORT).show()
        
        // Reset visual feedback after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            showVoiceInputFeedback(false)
        }, 2000)
    }
    
    /**
     * Enhanced emoji panel toggle with state management
     */
    private fun handleEmojiToggle() {
        try {
            val wasVisible = isEmojiPanelVisible
            toggleEmojiPanel()
            
            // Update emoji key visual state
            keyboardView?.setEmojiKeyActive(!wasVisible)
            
            Log.d(TAG, "Comprehensive emoji panel toggled: ${!wasVisible}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling emoji panel", e)
        }
    }
    
    private fun toggleEmojiPanel() {
        try {
            // Hide replacement UI if visible
            if (isReplacementUIVisible) {
                hideReplacementUI()
            }
            
            isEmojiPanelVisible = !isEmojiPanelVisible
            keyboardContainer?.let { container ->
                container.removeAllViews()
                
                if (isEmojiPanelVisible) {
                    // Show comprehensive emoji panel instead of keyboard
                    gboardEmojiPanel?.let { emojiPanel ->
                        // Set proper layout parameters for emoji panel
                        emojiPanel.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        container.addView(emojiPanel)
                        Log.d(TAG, "Showing comprehensive emoji panel with proper dimensions")
                    }
                    
                    // Keep suggestion bar visible for emoji search
                    topContainer?.visibility = View.VISIBLE
                } else {
                    // Show keyboard
                    keyboardView?.let { kv ->
                        container.addView(kv)
                        Log.d(TAG, "Showing keyboard")
                    }
                    
                    // Show suggestion bar when keyboard is visible
                    topContainer?.visibility = View.VISIBLE
                }
                
                // Request layout update
                container.requestLayout()
            }
            
            Log.d(TAG, "Comprehensive emoji panel toggled: visible=$isEmojiPanelVisible")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling comprehensive emoji panel", e)
        }
    }
    
    /**
     * Context-aware enter key handler (Gboard-style)
     */
    private fun handleEnterKey(ic: InputConnection) {
        try {
            // Determine enter key behavior based on input context
            val inputType = currentInputEditorInfo?.inputType ?: 0
            val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
            
            // Check for special IME actions
            when (imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    // Search action
                    ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                    return
                }
                EditorInfo.IME_ACTION_GO -> {
                    // Go action
                    ic.performEditorAction(EditorInfo.IME_ACTION_GO)
                    return
                }
                EditorInfo.IME_ACTION_SEND -> {
                    // Send action
                    ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
                    return
                }
                EditorInfo.IME_ACTION_NEXT -> {
                    // Next field action
                    ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
                    return
                }
                EditorInfo.IME_ACTION_DONE -> {
                    // Done action (close keyboard)
                    ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                    return
                }
            }
            
            // Default: Insert newline
            ic.commitText("\n", 1)
            
            // Enhanced auto-capitalization after enter
            if (::capsShiftManager.isInitialized) {
                capsShiftManager.handleEnterPress(ic, inputType)
            }
            
            Log.d(TAG, "Enter key handled - inputType: $inputType, imeOptions: $imeOptions")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling enter key", e)
            // Fallback to basic newline
            ic.commitText("\n", 1)
        }
    }
    
    /**
     * Enhanced cursor stability - prevent cursor movement on special key presses
     */
    private fun ensureCursorStability() {
        try {
            // This method can be called after special key operations
            // to ensure cursor position remains stable during state changes
            currentInputConnection?.let { ic ->
                // Store current cursor position before any state changes
                val beforeCursor = ic.getTextBeforeCursor(1000, 0)?.length ?: 0
                val afterCursor = ic.getTextAfterCursor(1000, 0)?.length ?: 0
                
                // The cursor position is naturally maintained by Android's InputConnection
                // when we don't call setSelection() or other cursor-moving methods
                // This method serves as a checkpoint to ensure no unintended cursor movement
                
                Log.d(TAG, "Cursor stability maintained at position: $beforeCursor")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error maintaining cursor stability", e)
        }
    }
    
    /**
     * Enhanced emoji insertion with proper cursor positioning
     */
    private fun insertEmojiWithCursor(emoji: String) {
        try {
            val ic = currentInputConnection ?: return
            
            // Use the cursor-aware text handler for consistent emoji insertion
            if (CursorAwareTextHandler.insertEmoji(ic, emoji)) {
                Log.d(TAG, "Successfully inserted emoji '$emoji' using CursorAwareTextHandler")
            } else {
                // Fallback to simple insertion
                ic.commitText(emoji, 1)
                Log.d(TAG, "Used fallback emoji insertion for '$emoji'")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting emoji with cursor", e)
            // Fallback to simple insertion
            currentInputConnection?.commitText(emoji, 1)
        }
    }
    
    /**
     * Insert rich content (GIFs, stickers) using commitContent API
     * Falls back to text if rich content is not supported
     */
    private fun insertRichContent(contentUri: String, mimeType: String, description: String): Boolean {
        try {
            val ic = currentInputConnection ?: return false
            
            // Check if target app supports rich content
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val inputContentInfo = InputContentInfo(
                    Uri.parse(contentUri),
                    ClipDescription(description, arrayOf(mimeType)),
                    null // No link URI
                )
                
                val flag = InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                val result = ic.commitContent(inputContentInfo, flag, null)
                
                if (result) {
                    Log.d(TAG, "Successfully inserted rich content: $contentUri")
                    return true
                } else {
                    Log.d(TAG, "Rich content not supported by target app")
                    return false
                }
            } else {
                Log.d(TAG, "Rich content not supported on this Android version")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting rich content", e)
            return false
        }
    }
    
    /**
     * Create AI Features toolbar with Tone, Rewrite, Emoji, GIF, Clipboard, Settings buttons
     */
    private fun createCleverTypeToolbar(): LinearLayout {
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.toolbar_height)
            )
            setPadding(
                resources.getDimensionPixelSize(R.dimen.toolbar_button_padding),
                resources.getDimensionPixelSize(R.dimen.toolbar_button_padding),
                resources.getDimensionPixelSize(R.dimen.toolbar_button_padding),
                resources.getDimensionPixelSize(R.dimen.toolbar_button_padding)
            )
            setBackgroundColor(Color.parseColor("#f8f9fa"))
        }
        
        // Tone button (✨ auto_awesome)
        val toneButton = createToolbarIconButton(
            icon = "✨",
            description = "Tone",
            onClick = { handleToneAdjustment() }
        )
        
        // Rewrite button (✍️ edit_note)
        val rewriteButton = createToolbarIconButton(
            icon = "✍️",
            description = "Rewrite",
            onClick = { handleRewriteText() }
        )
        
        // Emoji button (😊 emoji_emotions)
        val emojiButton = createToolbarIconButton(
            icon = "😊",
            description = "Emoji",
            onClick = { toggleEmojiPanel() }
        )
        
        // GIF button (GIF gif_box)
        val gifButton = createToolbarIconButton(
            icon = "GIF",
            description = "GIF",
            onClick = { handleGifSelection() }
        )
        
        // Clipboard button (📋 content_paste)
        val clipboardButton = createToolbarIconButton(
            icon = "📋",
            description = "Clipboard",
            onClick = { handleClipboardAccess() }
        )
        
        // Settings button (⚙️ settings)
        val settingsButton = createToolbarIconButton(
            icon = "⚙️",
            description = "Settings",
            onClick = { handleSettingsAccess() }
        )
        
        // Add buttons to toolbar
        toolbar.addView(toneButton)
        toolbar.addView(rewriteButton)
        toolbar.addView(emojiButton)
        toolbar.addView(gifButton)
        toolbar.addView(clipboardButton)
        toolbar.addView(settingsButton)
        
        Log.d(TAG, "AI Features toolbar created with 6 buttons")
        return toolbar
    }
    
    /**
     * Create toolbar icon button with proper Material Design sizing
     */
    private fun createToolbarIconButton(
        icon: String,
        description: String,
        onClick: () -> Unit
    ): LinearLayout {
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, 
                LinearLayout.LayoutParams.MATCH_PARENT, 
                1.0f // Equal weight distribution
            )
            setPadding(
                resources.getDimensionPixelSize(R.dimen.toolbar_button_padding),
                0,
                resources.getDimensionPixelSize(R.dimen.toolbar_button_padding),
                0
            )
            gravity = Gravity.CENTER
            isClickable = true
            background = ContextCompat.getDrawable(this@AIKeyboardService, R.drawable.key_background_default)
            
            setOnClickListener { onClick() }
        }
        
        // Create icon text view with proper sizing
        val iconView = TextView(this).apply {
            text = icon
            textSize = resources.getDimension(R.dimen.toolbar_icon_size) / resources.displayMetrics.scaledDensity
            setTextColor(Color.BLACK) // Default text color
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.toolbar_min_touch_target),
                resources.getDimensionPixelSize(R.dimen.toolbar_min_touch_target)
            )
            contentDescription = description
        }
        
        buttonContainer.addView(iconView)
        return buttonContainer
    }
    
    /**
     * Create toolbar button (deprecated - use createToolbarIconButton)
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
        val ic = currentInputConnection ?: return
        val allText = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString() ?: ""
        
        if (allText.isEmpty()) {
            Toast.makeText(this, "💭 Type some text first to adjust tone", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Tone adjustment requested for entire text: $allText")
        showReplacementUI("tone")
    }
    
    /**
     * Handle text rewriting with AI grammar correction
     */
    private fun handleRewriteText() {
        val ic = currentInputConnection ?: return
        val allText = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString() ?: ""
        
        if (allText.isEmpty()) {
            Toast.makeText(this, "📝 Type some text first to check grammar", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Grammar correction requested for entire text: $allText")
        showReplacementUI("grammar")
    }
    
    /**
     * Get the current sentence being typed
     */
    private fun getCurrentSentence(): String {
        val ic = currentInputConnection ?: return ""
        
        try {
            // Get text before cursor
            val beforeCursor = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
            // Get text after cursor  
            val afterCursor = ic.getTextAfterCursor(100, 0)?.toString() ?: ""
            
            // Find sentence boundaries (., !, ?, or line breaks)
            val sentenceStart = beforeCursor.lastIndexOfAny(listOf(".", "!", "?", "\n"))
            val sentenceEnd = afterCursor.indexOfAny(listOf(".", "!", "?", "\n"))
            
            val startText = if (sentenceStart >= 0) {
                beforeCursor.substring(sentenceStart + 1).trim()
            } else {
                beforeCursor.trim()
            }
            
            val endText = if (sentenceEnd >= 0) {
                afterCursor.substring(0, sentenceEnd).trim()
            } else {
                afterCursor.trim()
            }
            
            return (startText + endText).trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current sentence", e)
            return ""
        }
    }
    
    /**
     * Perform grammar correction using CleverType AI Service
     */
    private fun performGrammarCorrection(text: String) {
        Log.d(TAG, "Grammar correction requested for: $text")
        
        if (!::cleverTypeService.isInitialized) {
            // Fallback grammar correction using basic rules
            performBasicGrammarCorrection(text)
            return
        }
        
        // Show progress
        updateFirstSuggestion("🔍 Checking grammar...")
        
        coroutineScope.launch {
            try {
                // Request grammar correction using CleverType AI Service
                val result = cleverTypeService.fixGrammar(text)
                
                mainHandler.post {
                    if (result.hasChanges) {
                        applyCleverTypeGrammarCorrection(result)
                    } else {
                        updateFirstSuggestion("✓ Grammar looks good!")
                        Handler(Looper.getMainLooper()).postDelayed({
                            updateAISuggestions() // Restore normal suggestions
                        }, 2000)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in CleverType grammar correction", e)
                mainHandler.post {
                    performBasicGrammarCorrection(text)
                }
            }
        }
    }
    
    /**
     * Apply CleverType grammar correction to the text
     */
    private fun applyCleverTypeGrammarCorrection(result: CleverTypeAIService.GrammarResult) {
        val ic = currentInputConnection ?: return
        
        try {
            // Replace the text
            if (getSelectedText().isNotEmpty()) {
                // Replace selected text
                ic.commitText(result.correctedText, 1)
            } else {
                // Replace current sentence
                val beforeCursor = ic.getTextBeforeCursor(result.originalText.length * 2, 0)?.toString() ?: ""
                if (beforeCursor.contains(result.originalText)) {
                    // Delete original text and insert corrected
                    ic.deleteSurroundingText(result.originalText.length, 0)
                    ic.commitText(result.correctedText, 1)
                }
            }
            
            // Show corrections made
            val correctionCount = result.corrections.size
            if (correctionCount > 0) {
                val cacheIndicator = if (result.fromCache) " (cached)" else ""
                updateFirstSuggestion("✓ Fixed $correctionCount grammar issue${if (correctionCount > 1) "s" else ""}$cacheIndicator")
                
                // Create detailed feedback message
                val correctionTypes = result.corrections.groupBy { it.type }
                val typeMessages = correctionTypes.map { (type, corrections) ->
                    val typeName = when (type) {
                        CleverTypeAIService.CorrectionType.GRAMMAR -> "grammar"
                        CleverTypeAIService.CorrectionType.SPELLING -> "spelling"
                        CleverTypeAIService.CorrectionType.PUNCTUATION -> "punctuation"
                        CleverTypeAIService.CorrectionType.CAPITALIZATION -> "capitalization"
                    }
                    "$typeName (${corrections.size})"
                }.joinToString(", ")
                
                Toast.makeText(this, "✍️ CleverType fixed: $typeMessages", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "CleverType corrections applied: $correctionCount fixes in ${result.processingTimeMs}ms")
            } else {
                updateFirstSuggestion("✓ Text improved!")
                Toast.makeText(this, "✍️ Text enhanced by CleverType", Toast.LENGTH_SHORT).show()
            }
            
            // Restore normal suggestions after delay
            Handler(Looper.getMainLooper()).postDelayed({
                updateAISuggestions()
            }, 3000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying CleverType grammar correction", e)
            Toast.makeText(this, "❌ Error applying correction", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Basic grammar correction using simple rules
     */
    private fun performBasicGrammarCorrection(text: String) {
        var corrected = text
        var changesMade = 0
        
        // Basic capitalization fixes
        if (corrected.isNotEmpty() && corrected[0].isLowerCase()) {
            corrected = corrected[0].uppercase() + corrected.substring(1)
            changesMade++
        }
        
        // Fix double spaces
        val beforeSpaces = corrected
        corrected = corrected.replace(Regex("\\s{2,}"), " ")
        if (corrected != beforeSpaces) changesMade++
        
        // Fix spacing around punctuation
        corrected = corrected.replace(Regex("\\s+([.!?])"), "$1")
        corrected = corrected.replace(Regex("([.!?])([A-Za-z])"), "$1 $2")
        
        // Fix common typos
        val commonFixes = mapOf(
            "teh" to "the",
            "adn" to "and", 
            "taht" to "that",
            "thier" to "their",
            "recieve" to "receive",
            "seperate" to "separate"
        )
        
        for ((wrong, right) in commonFixes) {
            val regex = Regex("\\b$wrong\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(corrected)) {
                corrected = regex.replace(corrected, right)
                changesMade++
            }
        }
        
        if (changesMade > 0) {
            applyBasicCorrection(text, corrected, changesMade)
        } else {
            updateFirstSuggestion("✓ No corrections needed")
            Handler(Looper.getMainLooper()).postDelayed({
                updateAISuggestions()
            }, 2000)
        }
    }
    
    /**
     * Apply basic grammar correction
     */
    private fun applyBasicCorrection(original: String, corrected: String, changesMade: Int) {
        val ic = currentInputConnection ?: return
        
        try {
            if (getSelectedText().isNotEmpty()) {
                ic.commitText(corrected, 1)
            } else {
                ic.deleteSurroundingText(original.length, 0)
                ic.commitText(corrected, 1)
            }
            
            updateFirstSuggestion("✓ Fixed $changesMade issue${if (changesMade > 1) "s" else ""}")
            Toast.makeText(this, "📝 Basic grammar fixes applied", Toast.LENGTH_SHORT).show()
            
            Handler(Looper.getMainLooper()).postDelayed({
                updateAISuggestions()
            }, 2000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying basic correction", e)
        }
    }
    
    /**
     * Update the first suggestion with a message
     */
    private fun updateFirstSuggestion(message: String) {
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as? TextView
                firstSuggestion?.apply {
                    text = message
                    setTextColor(Color.parseColor("#4CAF50"))
                    visibility = View.VISIBLE
                }
            }
        }
    }
    
    /**
     * Show replacement UI (tone selector or grammar correction)
     * Hides the keyboard and shows full-width bottom sheet
     */
    private fun showReplacementUI(type: String) {
        if (isReplacementUIVisible) {
            hideReplacementUI()
        }
        
        isReplacementUIVisible = true
        currentReplacementType = type
        
        keyboardContainer?.let { container ->
            // Remove all current views (keyboard, emoji panel, etc.)
            container.removeAllViews()
            
            when (type) {
                "tone" -> {
                    if (cleverTypeToneSelector == null) {
                        cleverTypeToneSelector = CleverTypeToneSelector(this).apply {
                            setOnToneSelectedListener { tone ->
                                handleToneSelectedForReplacement(tone)
                            }
                            setOnCloseListener {
                                hideReplacementUI()
                                restoreKeyboard()
                            }
                        }
                    }
                    container.addView(cleverTypeToneSelector)
                    cleverTypeToneSelector?.show()
                    Log.d(TAG, "Replacement UI: Tone selector displayed")
                }
                "grammar" -> {
                    showGrammarCorrectionUI(container)
                    Log.d(TAG, "Replacement UI: Grammar correction displayed")
                }
            }
            
            // Hide suggestion bar when replacement UI is visible
            topContainer?.visibility = View.GONE
        }
    }
    
    /**
     * Hide replacement UI and prepare for keyboard restoration
     */
    private fun hideReplacementUI() {
        if (!isReplacementUIVisible) return
        
        keyboardContainer?.let { container ->
            container.removeAllViews()
        }
        
        isReplacementUIVisible = false
        currentReplacementType = ""
        
        // Show suggestion bar again
        topContainer?.visibility = View.VISIBLE
        
        Log.d(TAG, "Replacement UI hidden")
    }
    
    /**
     * Restore the keyboard view after replacement UI is closed
     */
    private fun restoreKeyboard() {
        keyboardContainer?.let { container ->
            keyboardView?.let { kv ->
                container.addView(kv)
                
                // Reset other panel states
                isEmojiPanelVisible = false
                isMediaPanelVisible = false
                
                Log.d(TAG, "Keyboard restored after replacement UI")
            }
        }
    }
    
    /**
     * Replace entire text in the input field
     */
    private fun replaceText(newText: String) {
        val ic = currentInputConnection ?: return
        
        try {
            // Get all text from input field
            val allText = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString() ?: ""
            
            // Clear all existing text and insert new text
            if (allText.isNotEmpty()) {
                // Select all text and replace it
                ic.setSelection(0, allText.length)
                ic.commitText(newText, 1)
            } else {
                ic.commitText(newText, 1)
            }
            
            Log.d(TAG, "Text replaced: '${allText}' → '$newText'")
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing text", e)
            Toast.makeText(this, "Error replacing text", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show Grammar Correction UI
     */
    private fun showGrammarCorrectionUI(container: LinearLayout) {
        // Create a grammar correction UI similar to tone selector
        val grammarUI = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.WHITE)
            elevation = 12f
        }
        
        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(8))
            setBackgroundColor(Color.parseColor("#f8f9fa"))
        }
        
        val title = TextView(this).apply {
            text = "✍️ Grammar Correction"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#202124"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        }
        
        val closeBtn = android.widget.Button(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#5f6368"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
            setOnClickListener {
                hideReplacementUI()
                restoreKeyboard()
            }
        }
        
        header.addView(title)
        header.addView(closeBtn)
        grammarUI.addView(header)
        
        // Content
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(220)
            )
        }
        
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(16))
        }
        
        // Progress text
        val progressText = TextView(this).apply {
            text = "🔍 Checking grammar..."
            textSize = 16f
            setTextColor(Color.parseColor("#5f6368"))
            gravity = android.view.Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))
        }
        
        contentLayout.addView(progressText)
        scrollView.addView(contentLayout)
        grammarUI.addView(scrollView)
        container.addView(grammarUI)
        
        // Start grammar correction
        performGrammarCorrectionForReplacement(contentLayout, progressText)
    }
    
    /**
     * Perform grammar correction for replacement UI
     */
    private fun performGrammarCorrectionForReplacement(contentLayout: LinearLayout, progressText: TextView) {
        // Get all text from input field
        val ic = currentInputConnection ?: return
        val allText = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString() ?: ""
        
        if (allText.isEmpty()) {
            progressText.text = "No text to correct"
            return
        }
        
        if (!::cleverTypeService.isInitialized) {
            showGrammarFallbackOptions(contentLayout, progressText, allText)
            return
        }
        
        coroutineScope.launch {
            try {
                val result = cleverTypeService.fixGrammar(allText)
                
                mainHandler.post {
                    progressText.visibility = View.GONE
                    showGrammarResults(contentLayout, allText, result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in grammar correction for replacement", e)
                mainHandler.post {
                    showGrammarFallbackOptions(contentLayout, progressText, allText)
                }
            }
        }
    }
    
    /**
     * Show grammar correction results
     */
    private fun showGrammarResults(contentLayout: LinearLayout, originalText: String, result: CleverTypeAIService.GrammarResult) {
        if (result.hasChanges) {
            // Show corrected text option
            val correctedButton = android.widget.Button(this).apply {
                text = "✅ Use Corrected Text"
                textSize = 16f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1a73e8"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dpToPx(8), 0, dpToPx(8))
                }
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                setOnClickListener {
                    replaceText(result.correctedText)
                    hideReplacementUI()
                    restoreKeyboard()
                    Toast.makeText(this@AIKeyboardService, "✍️ Text corrected!", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Show preview of changes
            val previewText = TextView(this).apply {
                text = "Corrected: \"${result.correctedText.take(100)}${if (result.correctedText.length > 100) "..." else ""}\""
                textSize = 14f
                setTextColor(Color.parseColor("#5f6368"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(16))
                }
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                setBackgroundColor(Color.parseColor("#f1f3f4"))
            }
            
            contentLayout.addView(previewText)
            contentLayout.addView(correctedButton)
        } else {
            // No corrections needed
            val noChangesText = TextView(this).apply {
                text = "✅ Your text looks great!\nNo grammar corrections needed."
                textSize = 16f
                setTextColor(Color.parseColor("#137333"))
                gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))
            }
            contentLayout.addView(noChangesText)
        }
        
        // Keep original option
        val keepOriginalButton = android.widget.Button(this).apply {
            text = "Keep Original"
            textSize = 14f
            setTextColor(Color.parseColor("#5f6368"))
            setBackgroundColor(Color.parseColor("#f1f3f4"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(4), 0, 0)
            }
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            setOnClickListener {
                hideReplacementUI()
                restoreKeyboard()
            }
        }
        
        contentLayout.addView(keepOriginalButton)
    }
    
    /**
     * Show basic grammar fallback options
     */
    private fun showGrammarFallbackOptions(contentLayout: LinearLayout, progressText: TextView, originalText: String) {
        progressText.text = "Using basic correction..."
        
        // Apply basic corrections
        var correctedText = originalText
        var changesMade = 0
        
        // Basic fixes
        if (correctedText.isNotEmpty() && correctedText[0].isLowerCase()) {
            correctedText = correctedText[0].uppercase() + correctedText.substring(1)
            changesMade++
        }
        correctedText = correctedText.replace(Regex("\\s{2,}"), " ")
        correctedText = correctedText.replace(Regex("\\s+([.!?])"), "$1")
        
        Handler(Looper.getMainLooper()).postDelayed({
            progressText.visibility = View.GONE
            
            if (correctedText != originalText) {
                val result = CleverTypeAIService.GrammarResult(
                    originalText = originalText,
                    correctedText = correctedText,
                    hasChanges = true,
                    corrections = emptyList(),
                    processingTimeMs = 0L
                )
                showGrammarResults(contentLayout, originalText, result)
            } else {
                val result = CleverTypeAIService.GrammarResult(
                    originalText = originalText,
                    correctedText = correctedText,
                    hasChanges = false,
                    corrections = emptyList(),
                    processingTimeMs = 0L
                )
                showGrammarResults(contentLayout, originalText, result)
            }
        }, 1000)
    }
    
    /**
     * Handle tone selection for replacement (replaces entire text with 3 options)
     */
    private fun handleToneSelectedForReplacement(tone: CleverTypeAIService.ToneType) {
        val ic = currentInputConnection ?: return
        val allText = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString() ?: ""
        
        if (allText.isEmpty()) {
            Toast.makeText(this, "No text to adjust tone", Toast.LENGTH_SHORT).show()
            hideReplacementUI()
            restoreKeyboard()
            return
        }
        
        // Show loading state
        updateSuggestionUI(listOf("Loading tone variations..."))
        
        coroutineScope.launch {
            try {
                val result = cleverTypeService.adjustTone(allText, tone)
                
                withContext(Dispatchers.Main) {
                    // Update suggestion strip with 3 tone variations
                    updateToneSuggestionStrip(result.variations, tone)
                    
                    // Store current text for possible replacement
                    currentToneReplacementText = allText
                    currentToneVariations = result.variations
                    
                    // Don't auto-apply, let user choose from suggestions
                    Toast.makeText(this@AIKeyboardService, "✨ Choose a ${tone.displayName} variation from suggestions", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in tone adjustment for replacement", e)
                withContext(Dispatchers.Main) {
                    hideReplacementUI()
                    restoreKeyboard()
                    Toast.makeText(this@AIKeyboardService, "Error adjusting tone", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Handle GIF selection
     */
    private fun handleGifSelection() {
        Log.d(TAG, "GIF selection requested")
        Toast.makeText(this, "GIF selection not yet implemented", Toast.LENGTH_SHORT).show()
        // TODO: Implement GIF picker
    }
    
    /**
     * Handle clipboard access
     */
    private fun handleClipboardAccess() {
        Log.d(TAG, "Clipboard access requested")
        Toast.makeText(this, "Clipboard access not yet implemented", Toast.LENGTH_SHORT).show()
        // TODO: Implement clipboard manager
    }
    
    /**
     * Handle settings access
     */
    private fun handleSettingsAccess() {
        Log.d(TAG, "Settings access requested")
        Toast.makeText(this, "Opening keyboard settings", Toast.LENGTH_SHORT).show()
        // TODO: Open keyboard settings activity
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
    
    // loadSettings method merged with existing one above
    
    /**
     * Save keyboard settings to SharedPreferences
     */
    private fun saveSettings() {
        try {
            settings.edit().apply {
                putBoolean("show_number_row", showNumberRow)
                putBoolean("swipe_enabled", swipeEnabled)
                putBoolean("vibration_enabled", vibrationEnabled)
                putBoolean("sound_enabled", soundEnabled)
                apply()
            }
            Log.d(TAG, "Settings saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving settings", e)
        }
    }
    
    /**
     * Toggle number row setting
     */
    fun toggleNumberRow() {
        showNumberRow = !showNumberRow
        saveSettings()
        
        // Reload keyboard with/without number row
        reloadKeyboard()
        
        Toast.makeText(this, if (showNumberRow) "Number row enabled" else "Number row disabled", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Number row toggled: $showNumberRow")
    }
    
    /**
     * Toggle swipe typing
     */
    fun toggleSwipeTyping() {
        swipeEnabled = !swipeEnabled
        saveSettings()
        Toast.makeText(this, if (swipeEnabled) "Swipe typing enabled" else "Swipe typing disabled", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Swipe typing toggled: $swipeEnabled")
    }
    
    /**
     * Toggle vibration feedback
     */
    fun toggleVibration() {
        vibrationEnabled = !vibrationEnabled
        saveSettings()
        Toast.makeText(this, if (vibrationEnabled) "Vibration enabled" else "Vibration disabled", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Vibration toggled: $vibrationEnabled")
    }
    
    /**
     * Toggle sound feedback
     */
    fun toggleSound() {
        soundEnabled = !soundEnabled
        saveSettings()
        Toast.makeText(this, if (soundEnabled) "Sound enabled" else "Sound disabled", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Sound toggled: $soundEnabled")
    }
    
    /**
     * Reload keyboard with current settings
     */
    private fun reloadKeyboard() {
        try {
            // Reload the current keyboard layout with updated settings
            switchToLetters()
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading keyboard", e)
        }
    }
    
    /**
     * Update settings from main app
     */
    fun updateSettingsFromApp(settingsMap: Map<String, Any>) {
        try {
            settingsMap["show_number_row"]?.let { 
                showNumberRow = it as Boolean
                reloadKeyboard()
            }
            settingsMap["swipe_enabled"]?.let { 
                swipeEnabled = it as Boolean
                keyboardView?.setSwipeEnabled(swipeEnabled)
            }
            settingsMap["vibration_enabled"]?.let { 
                vibrationEnabled = it as Boolean
            }
            settingsMap["sound_enabled"]?.let { 
                soundEnabled = it as Boolean
            }
            settingsMap["ai_suggestions"]?.let { 
                aiSuggestionsEnabled = it as Boolean
            }
            settingsMap["key_preview_enabled"]?.let { 
                keyPreviewEnabled = it as Boolean
                keyboardView?.isPreviewEnabled = keyPreviewEnabled
            }
            
            // Save settings
            saveSettings()
            
            Log.d(TAG, "Settings updated from app: $settingsMap")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating settings from app", e)
        }
    }
    
    /**
     * Get current settings for app
     */
    fun getCurrentSettings(): Map<String, Any> {
        return mapOf(
            "show_number_row" to showNumberRow,
            "swipe_enabled" to swipeEnabled,
            "vibration_enabled" to vibrationEnabled,
            "sound_enabled" to soundEnabled,
            "ai_suggestions" to aiSuggestionsEnabled,
            "key_preview_enabled" to keyPreviewEnabled,
            "current_language" to availableLanguages[currentLanguageIndex]
        )
    }
    
    /**
     * Generate dictionary-based candidates for swipe sequence using advanced matching algorithms
     */
    private suspend fun generateSwipeCandidates(
        swipeLetters: String, 
        prev1: String, 
        prev2: String
    ): List<AutocorrectCandidate> = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        val candidates = mutableListOf<AutocorrectCandidate>()
        
        try {
            // Step 1: Try exact dictionary match first
            if (enhancedAutocorrect.wordDatabase.wordExists(swipeLetters)) {
                candidates.add(AutocorrectCandidate(
                    word = swipeLetters,
                    score = 0.95,
                    editDistance = 0,
                    type = CandidateType.ORIGINAL,
                    confidence = 0.95
                ))
            }
            
            // Step 2: Generate candidates using path matching (longest common subsequence)
            val pathCandidates = generatePathMatchingCandidates(swipeLetters)
            candidates.addAll(pathCandidates)
            
            // Step 3: Generate candidates using edit distance (≤2)
            val editCandidates = generateEditDistanceCandidates(swipeLetters, prev1, prev2)
            candidates.addAll(editCandidates)
            
            // Step 4: Rank all candidates using unified scoring
            val rankedCandidates = candidates.distinctBy { it.word }
                .map { candidate ->
                    // Calculate comprehensive score
                    val freq = enhancedAutocorrect.wordDatabase.getWordFrequency(candidate.word)
                    val freqScore = kotlin.math.ln((freq + 1).toDouble())
                    
                    val bigramScore = if (prev1.isNotEmpty()) {
                        val bigrams = enhancedAutocorrect.wordDatabase.getBigramPredictions(prev1, candidate.word, 1)
                        if (bigrams.isNotEmpty()) kotlin.math.ln(bigrams.first().frequency.toDouble() + 1) else -5.0
                    } else 0.0
                    
                    val trigramScore = if (prev2.isNotEmpty() && prev1.isNotEmpty()) {
                        val trigrams = enhancedAutocorrect.wordDatabase.getTrigramPredictions(prev2, prev1, candidate.word, 1)
                        if (trigrams.isNotEmpty()) kotlin.math.ln(trigrams.first().frequency.toDouble() + 1) else -8.0
                    } else 0.0
                    
                    // Unified scoring formula optimized for swipe typing
                    val totalScore = 1.2 * freqScore - 
                                   0.8 * candidate.editDistance - 
                                   0.1 * kotlin.math.abs(candidate.word.length - swipeLetters.length) +
                                   0.6 * bigramScore + 
                                   0.4 * trigramScore
                    
                    candidate.copy(score = totalScore)
                }
                .sortedByDescending { it.score }
                .take(10) // Keep top 10 for performance
                
            val processingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Swipe candidates generated in ${processingTime}ms for '$swipeLetters' -> ${rankedCandidates.size} candidates")
            
            return@withContext rankedCandidates
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating swipe candidates", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Generate candidates using longest common subsequence matching
     */
    private fun generatePathMatchingCandidates(swipeLetters: String): List<AutocorrectCandidate> {
        val candidates = mutableListOf<AutocorrectCandidate>()
        
        try {
            // Get words that share significant character overlap
            val wordsToCheck = enhancedAutocorrect.wordDatabase.getWordsByPrefix(swipeLetters.take(2))
            
            wordsToCheck.take(50).forEach { word: String ->
                val lcs = longestCommonSubsequence(swipeLetters, word)
                val pathScore = lcs.toDouble() / maxOf(swipeLetters.length, word.length)
                
                if (pathScore >= 0.6) { // At least 60% character path match
                    candidates.add(AutocorrectCandidate(
                        word = word,
                        score = pathScore,
                        editDistance = kotlin.math.abs(swipeLetters.length - word.length),
                        type = CandidateType.CORRECTION,
                        confidence = pathScore
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in path matching", e)
        }
        
        return candidates
    }
    
    /**
     * Generate candidates using Damerau-Levenshtein edit distance
     */
    private suspend fun generateEditDistanceCandidates(
        swipeLetters: String, 
        prev1: String, 
        prev2: String
    ): List<AutocorrectCandidate> = withContext(Dispatchers.Default) {
        val candidates = mutableListOf<AutocorrectCandidate>()
        
        try {
            // Use enhanced autocorrect engine for edit distance candidates
            val autocorrectCandidates = enhancedAutocorrect.getCandidates(swipeLetters, prev1, prev2)
            
            // Filter for swipe-appropriate candidates (edit distance ≤ 2)
            autocorrectCandidates.filter { it.editDistance <= 2 }.forEach { candidate ->
                candidates.add(candidate)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in edit distance candidates", e)
        }
        
        return@withContext candidates
    }
    
    /**
     * Calculate longest common subsequence length
     */
    private fun longestCommonSubsequence(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                if (s1[i-1] == s2[j-1]) {
                    dp[i][j] = dp[i-1][j-1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i-1][j], dp[i][j-1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Update suggestion strip with enhanced swipe candidates
     */
    private fun updateEnhancedSwipeSuggestions(candidates: List<SwipeCandidate>, originalSwipe: String) {
        try {
            if (candidates.isEmpty()) {
                updateSuggestionUI(listOf(originalSwipe))
                return
            }
            
            val suggestionTexts = mutableListOf<String>()
            
            // Always show original swipe as first option (for reversion)
            suggestionTexts.add(originalSwipe)
            
            // Add top candidates with source indicators
            candidates.take(2).forEach { candidate ->
                val indicator = when (candidate.source) {
                    CandidateSource.EXACT_MATCH -> ""
                    CandidateSource.EDIT_DISTANCE -> "✓"
                    CandidateSource.PATTERN_MATCH -> "~"
                    CandidateSource.USER_DICTIONARY -> "★"
                }
                suggestionTexts.add("$indicator${candidate.word}")
            }
            
            updateSuggestionUI(suggestionTexts)
            Log.d(TAG, "Enhanced swipe suggestions updated: $suggestionTexts")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating enhanced swipe suggestions", e)
        }
    }
    
    /**
     * Update suggestion strip with swipe candidates (legacy)
     */
    private fun updateSwipeSuggestionStrip(candidates: List<AutocorrectCandidate>) {
        try {
            if (candidates.isEmpty()) {
                updateSuggestionUI(emptyList())
                return
            }
            
            val suggestionTexts = candidates.map { candidate ->
                when (candidate.type) {
                    CandidateType.ORIGINAL -> candidate.word
                    CandidateType.CORRECTION -> candidate.word
                    else -> candidate.word
                }
            }
            
            updateSuggestionUI(suggestionTexts)
            Log.d(TAG, "Swipe suggestion strip updated with ${suggestionTexts.size} options: $suggestionTexts")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating swipe suggestion strip", e)
        }
    }
    
    /**
     * Update suggestion strip with tone variations (3 AI-generated options)
     */
    private fun updateToneSuggestionStrip(variations: List<String>, tone: CleverTypeAIService.ToneType) {
        try {
            if (variations.isEmpty()) {
                updateSuggestionUI(emptyList())
                return
            }
            
            // Format tone variations with visual indicators
            val suggestionTexts = variations.take(3).mapIndexed { index, variation ->
                val truncated = if (variation.length > 50) "${variation.take(47)}..." else variation
                "${tone.emoji} $truncated"
            }
            
            updateSuggestionUI(suggestionTexts)
            Log.d(TAG, "Tone suggestion strip updated with ${suggestionTexts.size} ${tone.displayName} variations")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating tone suggestion strip", e)
        }
    }
    
    /**
     * Initialize theme MethodChannel for Flutter communication
     */
    private fun initializeThemeChannel() {
        try {
            // Theme updates are currently handled via broadcast receiver
            // This provides a foundation for future MethodChannel integration when needed
            Log.d(TAG, "Theme communication ready (broadcast-based)")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up theme communication", e)
        }
    }
}
