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
import com.example.ai_keyboard.utils.LogUtil
import android.view.KeyEvent
import android.view.View
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputContentInfo
import android.content.ClipDescription
import android.net.Uri
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.PopupWindow
import android.view.Gravity
import android.view.LayoutInflater
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*
import kotlin.math.max
import io.flutter.embedding.engine.dart.DartExecutor
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AIKeyboardService : InputMethodService(), 
    KeyboardView.OnKeyboardActionListener, 
    SwipeKeyboardView.SwipeListener {
    
    companion object {
        private const val TAG = "AIKeyboardService"
        
        // Keyboard layouts
        private const val KEYBOARD_LETTERS = 1
        private const val KEYBOARD_SYMBOLS = 2
        private const val KEYBOARD_NUMBERS = 3
        
        // Input modes
        private const val INPUT_MODE_NORMAL = 0
        private const val INPUT_MODE_GRAMMAR = 1
        private const val INPUT_MODE_CLIPBOARD = 2
        
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
    
    /**
     * Keyboard mode enum for CleverType-style cycling
     * Letters → Numbers → Symbols → Letters
     */
    enum class KeyboardMode {
        LETTERS,
        NUMBERS,
        SYMBOLS,
        EMOJI
    }
    
    /**
     * AI Panel Type enum
     */
    enum class AIPanelType {
        GRAMMAR,
        TONE,
        ASSISTANT
    }
    
    /**
     * Unified settings container for all keyboard configuration
     */
    private data class UnifiedSettings(
        val vibrationEnabled: Boolean,
        val soundEnabled: Boolean,
        val keyPreviewEnabled: Boolean,
        val showNumberRow: Boolean,
        val swipeTypingEnabled: Boolean,
        val aiSuggestionsEnabled: Boolean,
        val currentLanguage: String,
        val enabledLanguages: List<String>,
        val autocorrectEnabled: Boolean,
        val autoCapitalization: Boolean,
        val doubleSpacePeriod: Boolean,
        val popupEnabled: Boolean
    )
    
    /**
     * Internal SettingsManager - consolidates reads from multiple SharedPreferences sources
     * Eliminates redundant I/O by reading once per load cycle
     */
    private class SettingsManager(private val context: Context) {
        private val flutterPrefs by lazy {
            context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        }
        private val nativePrefs by lazy {
            context.getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
        }
        
        /**
         * Load all settings from both preference sources in a single pass
         * @return UnifiedSettings with all keyboard configuration
         */
        fun loadAll(): UnifiedSettings {
            // Read from native preferences (MainActivity writes here via MethodChannel)
            val vibration = nativePrefs.getBoolean("vibration_enabled", true)
            val sound = nativePrefs.getBoolean("sound_enabled", true)
            val keyPreview = nativePrefs.getBoolean("key_preview_enabled", false)
            val showNumberRow = nativePrefs.getBoolean("show_number_row", false)
            val swipeTyping = nativePrefs.getBoolean("swipe_typing", true)
            val aiSuggestions = nativePrefs.getBoolean("ai_suggestions", true)
            val autocorrect = nativePrefs.getBoolean("auto_correct", true)  // ✅ Fixed: Match key used by MainActivity and isAutoCorrectEnabled()
            val autoCap = nativePrefs.getBoolean("auto_capitalization", true)
            val doubleSpace = nativePrefs.getBoolean("double_space_period", true)
            val popup = nativePrefs.getBoolean("popup_enabled", false)
            
            // Read language settings from Flutter preferences
            val currentLang = flutterPrefs.getString("flutter.current_language", "en") ?: "en"
            val enabledLangsStr = flutterPrefs.getString("flutter.enabled_languages", "en") ?: "en"
            val enabledLangs = try {
                enabledLangsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } catch (_: Exception) {
                listOf("en")
            }
            
            return UnifiedSettings(
                vibrationEnabled = vibration,
                soundEnabled = sound,
                keyPreviewEnabled = keyPreview,
                showNumberRow = showNumberRow,
                swipeTypingEnabled = swipeTyping,
                aiSuggestionsEnabled = aiSuggestions,
                currentLanguage = currentLang,
                enabledLanguages = enabledLangs,
                autocorrectEnabled = autocorrect,
                autoCapitalization = autoCap,
                doubleSpacePeriod = doubleSpace,
                popupEnabled = popup
            )
        }
    }
    
    // UI Components
    private var keyboardView: SwipeKeyboardView? = null
    private var keyboard: Keyboard? = null
    private var suggestionContainer: LinearLayout? = null
    private var topContainer: LinearLayout? = null // Container for suggestions + language switch
    private var mediaPanelManager: SimpleMediaPanel? = null
    private var gboardEmojiPanel: GboardEmojiPanel? = null
    private var emojiPanelController: EmojiPanelController? = null // New XML-based controller
    private var emojiPanelView: View? = null // Inflated emoji panel
    private var keyboardContainer: LinearLayout? = null
    private var isMediaPanelVisible = false
    private var isEmojiPanelVisible = false
    
    // Enhancements: Suggestion queue and settings debouncing
    private val suggestionQueue = SuggestionQueue()
    private val settingsDebouncer = SettingsDebouncer(minIntervalMs = 250)
    
    // AI Panel (CleverType-style)
    private var aiPanel: LinearLayout? = null
    private var aiChipContainer: LinearLayout? = null
    private var aiResultView: TextView? = null
    private var aiReplaceButton: android.widget.Button? = null
    private var aiLanguageSpinner: android.widget.Spinner? = null
    private var isAIPanelVisible = false
    private var currentAIOriginalText = ""
    
    // Keyboard state
    private var caps = false
    private var lastShiftTime = 0L
    private var isShifted = false
    private var currentKeyboard = KEYBOARD_LETTERS
    private var currentInputMode = INPUT_MODE_NORMAL
    
    // CleverType keyboard mode cycling
    private var currentKeyboardMode = KeyboardMode.LETTERS
    private var previousKeyboardMode = KeyboardMode.LETTERS  // For emoji panel return
    
    // Replacement UI state
    private var isReplacementUIVisible = false
    private var currentReplacementType = ""
    
    // Advanced keyboard state
    private var shiftState = SHIFT_OFF
    private var lastShiftPressTime = 0L
    
    // Long-press detection for accent characters (spacebar long-press removed)
    private var currentLongPressKey: Int = -1
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        if (hasAccentVariants(currentLongPressKey)) {
            showAccentOptions(currentLongPressKey)
        }
    }
    
    private var keyPreviewPopup: PopupWindow? = null
    private var accentPopup: PopupWindow? = null
    private var vibrator: Vibrator? = null
    
    // Enhanced Caps/Shift Management
    private lateinit var capsShiftManager: CapsShiftManager
    private var shiftOptionsMenu: ShiftOptionsMenu? = null
    
    // Enhanced gesture support
    private var isSlideToDeleteModeActive = false
    
    // Advanced keyboard settings from Flutter
    private lateinit var keyboardSettings: KeyboardSettings
    
    // Enhanced layout features
    private var bilingualModeEnabled = false
    private var floatingModeEnabled = false
    private var adaptiveSizingEnabled = true
    
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
    
    // Language cycling - Now managed by SharedPreferences
    private var enabledLanguages = listOf("en")
    private var currentLanguage = "en"
    private var multilingualEnabled = false
    private var currentLanguageIndex = 0
    
    // Transliteration support for Indic languages (Phase 1)
    private var transliterationEngine: TransliterationEngine? = null
    private var indicScriptHelper: IndicScriptHelper? = null
    private var transliterationEnabled = true
    private val romanBuffer = StringBuilder()
    
    // Phase 2: Feature flags
    private var reverseTransliterationEnabled = false
    
    // Suggestion retry management
    private var retryCount = 0
    
    // Settings spam prevention
    private var lastSettingsHash: Int = 0
    
    // AI suggestion debouncing
    private var lastAISuggestionUpdate = 0L
    
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
    
    // Autocorrect undo and rejection tracking
    private var lastCorrection: Pair<String, String>? = null // (original, corrected)
    private var correctionRejected: Boolean = false
    private var undoAvailable: Boolean = false
    
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
    private lateinit var themeManager: ThemeManager
    private lateinit var settingsManager: SettingsManager
    private var lastLoadedSettingsHash: Int = 0
    
    // Method channels removed for compatibility - using SharedPreferences only for theme updates
    
    private lateinit var keyboardLayoutManager: KeyboardLayoutManager
    private lateinit var multilingualDictionary: MultilingualDictionary
    private lateinit var autocorrectEngine: UnifiedAutocorrectEngine
    private var languageSwitchView: LanguageSwitchView? = null
    
    // Enhanced prediction system
    private lateinit var nextWordPredictor: com.example.ai_keyboard.predict.NextWordPredictor
    
    // Services and handlers
    private lateinit var aiBridge: AIServiceBridge
    private lateinit var advancedAIService: AdvancedAIService
    private val mainHandler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // User dictionary sync
    private var syncHandler: Handler? = null
    private var syncRunnable: Runnable? = null
    private val syncInterval = 10 * 60 * 1000L // 10 minutes
    
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
    
    // Theme update management
    private var pendingThemeUpdate = false
    
    // Clipboard history management
    private lateinit var clipboardHistoryManager: ClipboardHistoryManager
    private var clipboardPanel: ClipboardPanel? = null
    private var clipboardSuggestionEnabled = true
    private var clipboardStripView: ClipboardStripView? = null
    
    // Dictionary management
    private lateinit var dictionaryManager: DictionaryManager
    private var dictionaryEnabled = true
    
    // User dictionary management (personalized word learning)
    private lateinit var userDictionaryManager: UserDictionaryManager
    
    // Custom AI prompts
    private data class CustomPrompt(
        val title: String,
        val instruction: String
    )
    
    private var customGrammarPrompts = mutableListOf<CustomPrompt>()
    private var customTonePrompts = mutableListOf<CustomPrompt>()
    private var customAssistantPrompts = mutableListOf<CustomPrompt>()
    private var builtInActionsEnabled = mutableMapOf(
        "grammar" to true,
        "formal" to true,
        "concise" to true,
        "expand" to true
    )
    
    // Clipboard history listener
    private val clipboardHistoryListener = object : ClipboardHistoryManager.ClipboardHistoryListener {
        override fun onHistoryUpdated(items: List<ClipboardItem>) {
            // Update clipboard panel if visible
            clipboardPanel?.updateItems(items)
            // Update clipboard strip
            updateClipboardStrip()
        }
        
        override fun onNewClipboardItem(item: ClipboardItem) {
            // Update suggestions if clipboard suggestions are enabled
            if (clipboardSuggestionEnabled) {
                updateSuggestionsWithClipboard()
            }
            // Update clipboard strip
            updateClipboardStrip()
        }
    }
    
    // Dictionary listener
    private val dictionaryListener = object : DictionaryManager.DictionaryListener {
        override fun onDictionaryUpdated(entries: List<DictionaryEntry>) {
            Log.d(TAG, "Dictionary updated with ${entries.size} entries")
        }
        
        override fun onExpansionTriggered(shortcut: String, expansion: String) {
            Log.d(TAG, "Dictionary expansion: $shortcut -> $expansion")
        }
    }
    
    // Broadcast receiver for settings changes
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.action) {
                    "com.example.ai_keyboard.SETTINGS_CHANGED" -> {
                        Log.d(TAG, "SETTINGS_CHANGED broadcast received!")
                        
                        // Debounce settings application to avoid spam
                        if (!settingsDebouncer.shouldApply()) {
                            Log.d(TAG, "⏳ Settings change debounced (${settingsDebouncer.timeUntilNextMs()}ms remaining)")
                            return
                        }
                        
                        // Reload settings immediately on main thread
                        mainHandler.post {
                            try {
                                Log.d(TAG, "📥 Loading settings from broadcast...")
                                settingsDebouncer.recordApply()
                                
                                // UNIFIED SETTINGS LOAD - single read from all prefs
                                applyLoadedSettings(settingsManager.loadAll(), logSuccess = false)
                                
                                // Apply CleverType config
                                applyConfig()
                                
                                // Reload theme from Flutter SharedPreferences
                                themeManager.reload()
                                applyTheme()
                                
                                Log.d(TAG, "✅ Settings applied successfully")
                                applySettingsImmediately()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error applying settings from broadcast", e)
                            }
                        }
                    }
                    "com.example.ai_keyboard.THEME_CHANGED" -> {
                        val themeId = intent?.getStringExtra("theme_id")
                        val themeName = intent?.getStringExtra("theme_name") ?: "Unknown"
                        val hasThemeData = intent?.getBooleanExtra("has_theme_data", false) ?: false
                        val isV2Theme = intent?.getBooleanExtra("is_v2_theme", false) ?: false
                        
                        Log.d(TAG, "🎨 THEME_CHANGED broadcast received! Theme: $themeName ($themeId), V2: $isV2Theme, Has data: $hasThemeData")
                        
                        // Add small delay to ensure SharedPreferences are fully written
                        Thread.sleep(50)
                        
                        // Force reload theme from SharedPreferences
                        themeManager.reload()
                        
                        // Verify theme was actually loaded
                        val loadedTheme = themeManager.getCurrentTheme()
                        Log.d(TAG, "Loaded theme after reload: ${loadedTheme.name} (${loadedTheme.id})")
                        
                        // Check if keyboard view is ready
                        if (keyboardView != null) {
                            // Apply immediately with full refresh
                            mainHandler.post {
                                Log.d(TAG, "⚡ Applying theme update immediately - V2: $isV2Theme")
                                applyThemeImmediately() // Use the comprehensive theme application
                                
                                // Additional refresh for V2 themes
                                if (isV2Theme) {
                                    mainHandler.postDelayed({
                                        keyboardView?.let { view ->
                                            if (view is SwipeKeyboardView) {
                                                view.refreshTheme()
                                                view.invalidate()
                                            }
                                        }
                                        Log.d(TAG, "🔄 V2 theme additional refresh completed")
                                    }, 100)
                                }
                            }
                        } else {
                            // Queue for later application
                            pendingThemeUpdate = true
                            Log.d(TAG, "Keyboard view not ready, queuing V2 theme update for later")
                        }
                    }
                    "com.example.ai_keyboard.CLIPBOARD_CHANGED" -> {
                        Log.d(TAG, "CLIPBOARD_CHANGED broadcast received!")
                        mainHandler.post {
                            try {
                                Log.d(TAG, "Reloading clipboard settings from broadcast...")
                                reloadClipboardSettings()
                                Log.d(TAG, "Clipboard settings reloaded successfully!")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error reloading clipboard settings from broadcast", e)
                            }
                        }
                    }
                    "com.example.ai_keyboard.DICTIONARY_CHANGED" -> {
                        Log.d(TAG, "DICTIONARY_CHANGED broadcast received!")
                        mainHandler.post {
                            try {
                                Log.d(TAG, "Reloading dictionary settings from broadcast...")
                                reloadDictionarySettings()
                                // Reload dictionary entries
                                dictionaryManager.initialize()
                                Log.d(TAG, "Dictionary settings reloaded successfully!")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error reloading dictionary settings from broadcast", e)
                            }
                        }
                    }
                    "com.example.ai_keyboard.EMOJI_SETTINGS_CHANGED" -> {
                        Log.d(TAG, "EMOJI_SETTINGS_CHANGED broadcast received!")
                        mainHandler.post {
                            try {
                                Log.d(TAG, "Reloading emoji settings from broadcast...")
                                reloadEmojiSettings()
                                Log.d(TAG, "Emoji settings reloaded successfully!")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error reloading emoji settings from broadcast", e)
                            }
                        }
                    }
                    "com.example.ai_keyboard.CLEAR_USER_WORDS" -> {
                        Log.d(TAG, "CLEAR_USER_WORDS broadcast received!")
                        mainHandler.post {
                            try {
                                Log.d(TAG, "Clearing learned words...")
                                if (::userDictionaryManager.isInitialized) {
                                    userDictionaryManager.clearAllWords()
                                    Log.d(TAG, "✅ Learned words cleared successfully!")
                                } else {
                                    Log.w(TAG, "⚠️ UserDictionaryManager not initialized")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error clearing learned words from broadcast", e)
                            }
                        }
                    }
                    "com.example.ai_keyboard.LANGUAGE_CHANGED" -> {
                        val language = intent?.getStringExtra("language")
                        val multiEnabled = intent?.getBooleanExtra("multilingual_enabled", false)
                        Log.d(TAG, "LANGUAGE_CHANGED broadcast received! Language: $language, Multi: $multiEnabled")
                        mainHandler.post {
                            try {
                                // Reload language preferences from SharedPreferences
                                loadLanguagePreferences()
                                
                                // If we're in LETTERS mode, reload the keyboard layout
                                if (currentKeyboardMode == KeyboardMode.LETTERS) {
                                    switchKeyboardMode(KeyboardMode.LETTERS)
                                    Log.d(TAG, "✅ Keyboard layout reloaded after language change")
                                }
                                
                                if (language != null) {
                                    showLanguageToast(language)
                                }
                                Log.d(TAG, "✅ Language settings reloaded! Current: $currentLanguage, Enabled: $enabledLanguages")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error reloading language settings", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in broadcast receiver", e)
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
        settingsManager = SettingsManager(this)
        themeManager = ThemeManager(this)
        
        // Register as theme change listener for live updates
        themeManager.addThemeChangeListener(object : ThemeManager.ThemeChangeListener {
            override fun onThemeChanged(theme: com.example.ai_keyboard.themes.KeyboardThemeV2, palette: com.example.ai_keyboard.themes.ThemePaletteV2) {
                Log.d(TAG, "🎨 Theme changed: ${theme.name}, applying to keyboard...")
                mainHandler.post {
                    applyThemeImmediately()
                }
            }
        })
        
        // Initialize keyboard layout manager
        keyboardLayoutManager = KeyboardLayoutManager(this)
        
        // CRITICAL: Initialize all core components FIRST before any async operations
        initializeCoreComponents()
        
        // UNIFIED SETTINGS LOAD - replaces loadSettings(), loadEnhancedSettings(), loadKeyboardSettings()
        applyLoadedSettings(settingsManager.loadAll(), logSuccess = true)
        loadDictionariesAsync()
        
        // Load custom prompts separately (still needed for AI features)
        loadCustomPrompts()
        
        // Initialize multilingual components (now safe)
        initializeMultilingualComponents()

        // Initialize Enhanced AI Service Bridge
        initializeAIBridge()
        
        // Initialize Advanced AI Service
        advancedAIService = AdvancedAIService(this)
        Log.d(TAG, "Advanced AI Service initialized")
        
        // STEP 2: Check AI readiness after initialization
        checkAIReadiness()
        
        // Load word frequencies from Firestore for enhanced autocorrect
        if (ensureEngineReady()) {
            coroutineScope.launch {
                delay(2000) // Wait for initialization to complete
                val currentLang = currentLanguage
                autocorrectEngine.preloadLanguages(listOf(currentLang))
                Log.d(TAG, "📊 Firestore word frequencies loading for $currentLang")
            }
        }
        
        // Initialize CleverType AI Service
        initializeCleverTypeService()
        
        // Initialize Theme MethodChannel
        initializeThemeChannel()
        
        // Initialize advanced keyboard features
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        // longPressHandler is now a val initialized at declaration
        
        // Initialize Enhanced Caps/Shift Manager
        initializeCapsShiftManager()
        
        // Initialize clipboard history manager
        clipboardHistoryManager = ClipboardHistoryManager(this)
        clipboardHistoryManager.initialize()
        clipboardHistoryManager.addListener(clipboardHistoryListener)
        
        // Initialize dictionary manager
        dictionaryManager = DictionaryManager(this)
        dictionaryManager.initialize()
        dictionaryManager.addListener(dictionaryListener)
        
        // User dictionary manager already initialized in initializeCoreComponents()
        if (::userDictionaryManager.isInitialized) {
            userDictionaryManager.syncFromCloud()
            Log.d(TAG, "✅ User dictionary sync initiated")
        }
        
        Log.d(TAG, "User dictionary manager initialized")
        
        // Set up periodic cloud sync (every 10 minutes)
        setupPeriodicSync()
        
        // Initialize keyboard settings with defaults first
        keyboardSettings = KeyboardSettings()
        // Settings already loaded via unified loader above
        // loadKeyboardSettings() - REMOVED (now handled by applyLoadedSettings)
        
        // Register broadcast receiver for settings changes
        try {
            val filter = IntentFilter().apply {
                addAction("com.example.ai_keyboard.SETTINGS_CHANGED")
                addAction("com.example.ai_keyboard.THEME_CHANGED")
                addAction("com.example.ai_keyboard.CLIPBOARD_CHANGED")
                addAction("com.example.ai_keyboard.DICTIONARY_CHANGED")
                addAction("com.example.ai_keyboard.EMOJI_SETTINGS_CHANGED")
                addAction("com.example.ai_keyboard.CLEAR_USER_WORDS")
                addAction("com.example.ai_keyboard.LANGUAGE_CHANGED")
            }
            // Use RECEIVER_NOT_EXPORTED for Android 13+ compatibility
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(settingsReceiver, filter)
            }
            Log.d(TAG, "Broadcast receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering broadcast receiver", e)
        }
        
        // Start settings polling as backup
        startSettingsPolling()
        
        // Load language preferences
        loadLanguagePreferences()
        
        // Initialize transliteration for Indic languages (Phase 1)
        initializeTransliteration()
        
        // Phase 2: Initialize multilingual dictionary
        // TODO: Integrate with existing MultilingualDictionary and AutocorrectEngine
        // initializeMultilingualDictionary()
        
        // Run diagnostic audit (analysis phase)
        runDiagnosticAudit()
        
        Log.d(TAG, "✅ AIKeyboardService onCreate completed successfully")
    }
    
    /**
     * Initialize core components FIRST to prevent UninitializedPropertyAccessException
     * This must be called before any async operations that might access these components
     */
    private fun initializeCoreComponents() {
        try {
            Log.d(TAG, "🔧 Initializing core components...")
            
            // Initialize user dictionary manager first
            userDictionaryManager = UserDictionaryManager(this)
            Log.d(TAG, "✅ UserDictionaryManager initialized")
            
            // Initialize multilingual dictionary
            multilingualDictionary = MultilingualDictionary(this)
            Log.d(TAG, "✅ MultilingualDictionary initialized")
            
            // Initialize transliteration engine for default language
            transliterationEngine = TransliterationEngine(this, "en")
            Log.d(TAG, "✅ TransliterationEngine initialized")
            
            // Initialize Indic script helper
            indicScriptHelper = IndicScriptHelper()
            Log.d(TAG, "✅ IndicScriptHelper initialized")
            
            // Initialize unified autocorrect engine (depends on above components)
            autocorrectEngine = UnifiedAutocorrectEngine(
                context = this,
                dictionary = multilingualDictionary,
                transliterationEngine = transliterationEngine,
                indicScriptHelper = indicScriptHelper,
                userDictionaryManager = userDictionaryManager
            )
            Log.d(TAG, "✅ UnifiedAutocorrectEngine initialized")
            
            // Preload essential languages asynchronously
            val enabledLangs = listOf("en", "hi", "te", "ta")
            Log.d(TAG, "🔄 Starting preload for ${enabledLangs.size} languages: $enabledLangs")
            autocorrectEngine.preloadLanguages(enabledLangs)
            
            // Verify loading status asynchronously (don't block onCreate)
            coroutineScope.launch {
                delay(1000) // Wait for async loads to complete
                
                val successCount = enabledLangs.count { lang ->
                    autocorrectEngine.isLanguageLoaded(lang)
                }
                
                if (successCount == enabledLangs.size) {
                    Log.i(TAG, "✅ UnifiedAutocorrectEngine loaded $successCount/${enabledLangs.size} languages successfully")
                } else {
                    val failed = enabledLangs.filter { !autocorrectEngine.isLanguageLoaded(it) }
                    Log.w(TAG, "⚠️ UnifiedAutocorrectEngine loaded $successCount/${enabledLangs.size} languages (failed: $failed)")
                }
            }
            
            Log.d(TAG, "✅ Core components initialization COMPLETE")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing core components", e)
            throw e
        }
    }
    
    /**
     * Guard helper to ensure engine is ready before use
     */
    private fun ensureEngineReady(): Boolean {
        val componentsInitialized = ::autocorrectEngine.isInitialized && 
                   ::userDictionaryManager.isInitialized && 
                   ::multilingualDictionary.isInitialized
        if (!componentsInitialized) {
            Log.w(TAG, "⚠️ Engine not initialized yet - skipping operation")
            return false
        }
        
        // Check if autocorrect engine is fully ready (corrections + dictionaries loaded)
        if (!autocorrectEngine.isReady()) {
            // Don't log warning here as it's normal during async load
            return false
        }
        
        return true
    }
    
    /**
     * Guard helper to ensure AI Bridge is ready before use
     */
    private fun ensureAIBridge(): Boolean {
        if (!::aiBridge.isInitialized) {
            Log.w(TAG, "⚠️ AIBridge not ready")
            return false
        }
        return true
    }
    
    /**
     * Debounce guard for AI suggestion updates
     */
    private fun shouldUpdateAISuggestions(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastAISuggestionUpdate < 100) return false
        lastAISuggestionUpdate = now
        return true
    }
    
    /**
     * STEP 2: AI readiness check with unified state management
     */
    private fun checkAIReadiness() {
        // Check if AI bridge is actually ready (not just initialized)
        if (::aiBridge.isInitialized && aiBridge.isReady()) {
            isAIReady = true
            Log.d(TAG, "🟢 AI service confirmed ready")
            
            // Prewarm AI models in background
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    if (::advancedAIService.isInitialized) {
                        // Preload AI engines if method exists
                        Log.d(TAG, "🔥 Preloading AI engines in background...")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ AI preload failed: ${e.message}")
                }
            }
        } else if (::advancedAIService.isInitialized) {
            // Preload advanced AI service asynchronously with warm-up wait
            coroutineScope.launch(Dispatchers.Default) {
                try {
                    Log.d(TAG, "🧠 Waiting for AdvancedAIService warm-up...")
                    
                    // Wait for AdvancedAIService to fully initialize
                    var initialized = false
                    repeat(5) { attempt ->
                        if (advancedAIService.isInitialized()) {
                            initialized = true
                            
                            // Preload/warm up the AI service
                            advancedAIService.preloadWarmup()
                            
                            withContext(Dispatchers.Main) {
                                aiBridge = AIServiceBridge.getInstance()
                                Log.d(TAG, "🧠 AI Bridge linked successfully on attempt ${attempt + 1}")
                                isAIReady = true
                                Log.i(TAG, "🟢 AdvancedAIService ready before first key input")
                            }
                            return@launch
                        }
                        delay(400L)
                    }
                    
                    // Timeout - proceed with fallback
                    if (!initialized) {
                        Log.w(TAG, "⚠️ AdvancedAIService warm-up timeout, proceeding with fallback")
                        withContext(Dispatchers.Main) {
                            isAIReady = false
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ AI preload failed, using fallback: ${e.message}")
                    withContext(Dispatchers.Main) {
                        isAIReady = false
                    }
                }
            }
        } else {
            // Retry after delay, but don't block autocorrect - it works independently
            Log.d(TAG, "⏳ AI services still initializing... (autocorrect available)")
            mainHandler.postDelayed({ checkAIReadiness() }, 500)
        }
    }
    
    /**
     * Run one-time diagnostic audit to report current feature status
     */
    private fun runDiagnosticAudit() {
        try {
            // Check for presence of key features
            val hasUpdateAISuggestions = false // Method not found in current implementation
            val hasSuggestionContainerInflation = true // suggestionContainer is created in onCreateInputView
            val hasEmojiPipeline = true // GboardEmojiPanel exists
            val hasNextWordModel = languageManager != null
            val hasClipboardSuggester = clipboardHistoryManager != null
            val hasAutocap = capsShiftManager != null
            val hasDoubleSpacePeriod = settings.getBoolean("double_space_period", true)
            val hasPopupPreviewSetting = settings.contains("popup_enabled")
            val hasDictionaryManager = dictionaryManager != null
            val hasLanguageManager = languageManager != null
            val hasAutocorrectEngine = true // SwipeAutocorrectEngine or AIServiceBridge
            
            com.example.ai_keyboard.diagnostics.TypingSyncAuditor.report(
                hasUpdateAISuggestions = hasUpdateAISuggestions,
                hasSuggestionContainerInflation = hasSuggestionContainerInflation,
                hasEmojiPipeline = hasEmojiPipeline,
                hasNextWordModel = hasNextWordModel,
                hasClipboardSuggester = hasClipboardSuggester,
                hasAutocap = hasAutocap,
                hasDoubleSpacePeriod = hasDoubleSpacePeriod,
                hasPopupPreviewSetting = hasPopupPreviewSetting,
                hasDictionaryManager = hasDictionaryManager,
                hasLanguageManager = hasLanguageManager,
                hasAutocorrectEngine = hasAutocorrectEngine
            )
            
            // Report feature gaps
            val gaps = mutableListOf<String>()
            if (!hasUpdateAISuggestions) gaps.add("Unified updateAISuggestions method")
            if (!hasPopupPreviewSetting) gaps.add("Popup preview toggle in settings")
            
            com.example.ai_keyboard.diagnostics.TypingSyncAuditor.reportGaps(gaps)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running diagnostic audit", e)
        }
    }
    
    private fun initializeAIBridge() {
        if (!ensureEngineReady()) return
        
        try {
            // Initialize the enhanced AI bridge with context
            AIServiceBridge.initialize(this)
            aiBridge = AIServiceBridge.getInstance()
            Log.d(TAG, "AI Bridge initialized successfully")
            
            // Check AI readiness periodically - retry up to 5 times only if dictionaries are not ready
            coroutineScope.launch {
                delay(1000)
                var retryCount = 0
                val currentLang = currentLanguage
                
                while (!isAIReady && retryCount < 5) {
                    // Check if dictionaries are ready first
                    val dictionariesReady = autocorrectEngine.isLanguageLoaded(currentLang)
                    
                    if (!dictionariesReady) {
                        Log.d(TAG, "🔵 [AI] Waiting for dictionaries to load for $currentLang, retry $retryCount/5")
                    } else {
                        isAIReady = aiBridge.isReady()
                        if (isAIReady) {
                            Log.i(TAG, "✅ AI initialized for $currentLang with dictionary + bigrams")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@AIKeyboardService, "🤖 AI Keyboard Ready", Toast.LENGTH_SHORT).show()
                            }
                            break
                        } else {
                            Log.d(TAG, "🔵 [AI] AI service not ready yet, retry $retryCount/5")
                        }
                    }
                    
                    delay(2000) // Retry after 2 seconds
                    retryCount++
                }
                
                if (!isAIReady) {
                    Log.w(TAG, "⚠️ AI unavailable, running in enhanced basic mode")
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
     * Apply keyboard configuration from CleverType-standardized preferences
     * Called on startup and when notifyConfigChange is received
     */
    private fun applyConfig() {
        try {
            val p = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            
            // Read all configuration values with CleverType keys
            val numberRow = p.getBoolean("flutter.keyboard.numberRow", false)
            val hintedNumberRow = p.getBoolean("flutter.keyboard.hintedNumberRow", false) && !numberRow
            val hintedSymbols = p.getBoolean("flutter.keyboard.hintedSymbols", true)
            val utilityAction = p.getStringCompat("flutter.keyboard.utilityKeyAction", "emoji")
            val showLangOnSpace = p.getBoolean("flutter.keyboard.showLanguageOnSpace", true)
            val fontScaleP = p.getFloatCompat("flutter.keyboard.fontScalePortrait", 1.0f)
            val fontScaleL = p.getFloatCompat("flutter.keyboard.fontScaleLandscape", 1.0f)
            val borderless = p.getBoolean("flutter.keyboard.borderlessKeys", false)
            val ohEnabled = p.getBoolean("flutter.keyboard.oneHanded.enabled", false)
            val ohSide = p.getString("flutter.keyboard.oneHanded.side", "right") ?: "right"
            val ohWidth = p.getFloatCompat("flutter.keyboard.oneHanded.widthPct", 0.87f)
            val landscapeFull = p.getBoolean("flutter.keyboard.landscapeFullscreen", true)
            val scaleX = p.getFloatCompat("flutter.keyboard.scaleX", 1.0f)
            val scaleY = p.getFloatCompat("flutter.keyboard.scaleY", 1.0f)
            val spaceVdp = p.getIntCompat("flutter.keyboard.keySpacingVdp", 5)
            val spaceHdp = p.getIntCompat("flutter.keyboard.keySpacingHdp", 2)
            val bottomP = p.getIntCompat("flutter.keyboard.bottomOffsetPortraitDp", 1)
            val bottomL = p.getIntCompat("flutter.keyboard.bottomOffsetLandscapeDp", 2)
            val popupPreview = p.getBoolean("flutter.keyboard.popupPreview", true)
            val longPressDelay = p.getIntCompat("flutter.keyboard.longPressDelayMs", 200)
            val soundEnabled = p.getBoolean("flutter.sound_enabled", true)
            val soundIntensity = p.getIntCompat("flutter.sound_intensity", 1)
            val hapticIntensity = p.getIntCompat("flutter.haptic_intensity", 2)
            
            // Check if number row setting changed - need to reload keyboard layout
            val numberRowChanged = showNumberRow != numberRow
            if (numberRowChanged) {
                showNumberRow = numberRow
            }
            
            // Determine font scale and bottom offset based on orientation
            val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val fontScale = if (isLandscape) fontScaleL else fontScaleP
            val bottom = if (isLandscape) bottomL.dp else bottomP.dp
            
            // Reload keyboard if number row changed (requires different XML layout)
            if (numberRowChanged) {
                try {
                    // Reload current keyboard mode with new settings
                    val currentMode = when (currentKeyboard) {
                        KEYBOARD_LETTERS -> KeyboardMode.LETTERS
                        KEYBOARD_NUMBERS -> KeyboardMode.NUMBERS
                        KEYBOARD_SYMBOLS -> KeyboardMode.SYMBOLS
                        else -> KeyboardMode.LETTERS
                    }
                    switchKeyboardMode(currentMode)
                    Log.d(TAG, "✓ Keyboard reloaded with numberRow=$showNumberRow")
                } catch (e: Exception) {
                    Log.e(TAG, "⚠ Error reloading keyboard", e)
                }
            }
            
        // Apply to keyboard view if available
        keyboardView?.let { view ->
            if (view is SwipeKeyboardView) {
                // CRITICAL: Apply ALL settings to the view in proper order
                view.setLabelScale(fontScale)
                view.setBorderless(borderless)
                view.setShowLanguageOnSpace(showLangOnSpace)
                view.setCurrentLanguage(currentLanguage.uppercase())
                view.setPreviewEnabled(popupPreview)
                
                // One-handed mode with Gboard-style behavior
                view.setOneHandedMode(ohEnabled, ohSide, ohWidth)
                
                // Spacing and sizing
                view.setKeySpacing(spaceVdp, spaceHdp)
                view.scaleX = scaleX
                view.scaleY = scaleY
                
                // Interaction settings
                view.setLongPressDelay(longPressDelay)
                view.setSoundEnabled(soundEnabled, soundIntensity)
                view.setHapticIntensity(hapticIntensity)
                
                // CRITICAL: Force complete redraw and layout recalculation
                view.invalidateAllKeys()
                view.invalidate()
                view.requestLayout()
                
                Log.d(TAG, "✓ SwipeKeyboardView settings applied: " +
                    "fontScale=$fontScale, borderless=$borderless, showLang=$showLangOnSpace, " +
                    "preview=$popupPreview, oneHanded=$ohEnabled@$ohSide(${(ohWidth * 100).toInt()}%), " +
                    "spacing=$spaceVdp/$spaceHdp, scale=$scaleX×$scaleY, longPress=${longPressDelay}ms, " +
                    "sound=$soundEnabled@$soundIntensity, haptic=$hapticIntensity")
            }
        }
        
        // Apply bottom offset to container
        keyboardContainer?.setPadding(
            keyboardContainer?.paddingLeft ?: 0,
            keyboardContainer?.paddingTop ?: 0,
            keyboardContainer?.paddingRight ?: 0,
            bottom
        )
        
        Log.d(TAG, "✅ Config applied complete: NumRow=$numberRow HintedRow=$hintedNumberRow " +
            "HintedSymbols=$hintedSymbols Borderless=$borderless OneHanded=${ohEnabled}@${ohSide}(${(ohWidth * 100).toInt()}%) " +
            "Scale=${scaleX}x${scaleY} Spacing=${spaceVdp}dp/${spaceHdp}dp FontScale=${fontScale} " +
            "Popup=$popupPreview LP=${longPressDelay}ms Bottom=${bottom}px Utility=$utilityAction")
        } catch (e: Exception) {
            Log.e(TAG, "⚠ Error applying config", e)
        }
    }
    
    // Helper extensions for preference reading (handles all Flutter SharedPreferences types)
    // Note: Pass key WITH "flutter." prefix already included
    private fun SharedPreferences.getFloatCompat(k: String, def: Float): Float {
        val value = all[k]
        return when (value) {
            is Float -> value
            is Double -> value.toFloat()
            is Int -> value.toFloat()
            is Long -> value.toFloat()
            is String -> value.toFloatOrNull() ?: def
            null -> def
            else -> def
        }
    }
    
    private fun SharedPreferences.getIntCompat(k: String, def: Int): Int {
        val value = all[k]
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Float -> value.toInt()
            is Double -> value.toInt()
            is String -> value.toIntOrNull() ?: def
            null -> def
            else -> def
        }
    }
    
    private fun SharedPreferences.getStringCompat(k: String, def: String): String {
        return try {
            getString(k, null) ?: all[k]?.toString() ?: def
        } catch (e: ClassCastException) {
            all[k]?.toString() ?: def
        }
    }
    
    private val Int.dp: Int get() = (resources.displayMetrics.density * this).toInt()
    
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
            
            // Core components already initialized in initializeCoreComponents()
            // Just verify they're ready
            if (!ensureEngineReady()) {
                Log.e(TAG, "❌ Core components not ready in initializeMultilingualComponents")
                return
            }
            
            // Connect user dictionary manager to autocorrect engine (if initialized)
            if (::userDictionaryManager.isInitialized) {
                // User dictionary integrated in UnifiedAutocorrectEngine
                Log.d(TAG, "Connected user dictionary manager to autocorrect engine")
            }
            
            // Initialize enhanced prediction system
            nextWordPredictor = com.example.ai_keyboard.predict.NextWordPredictor(autocorrectEngine, multilingualDictionary)
            
            // Phase 2: User dictionary integration handled by UnifiedAutocorrectEngine
            Log.d(TAG, "User dictionary integration complete")
            
            Log.d(TAG, "Enhanced prediction system initialized")
            
            // Initialize enhanced swipe autocorrect engine
            swipeAutocorrectEngine = SwipeAutocorrectEngine.getInstance(this)
            
            // STEP 2: Integrate SwipeAutocorrectEngine with UnifiedAutocorrectEngine
            if (ensureEngineReady()) {
                swipeAutocorrectEngine.setUnifiedEngine(autocorrectEngine)
                Log.d(TAG, "✅ SwipeAutocorrectEngine integrated with UnifiedAutocorrectEngine")
            }
            
            // Run validation tests for enhanced autocorrect
            coroutineScope.launch {
                delay(2000) // Wait for dictionary to load
                val testResults = autocorrectEngine.getStats()
                testResults.forEach { result ->
                    Log.d(TAG, "Autocorrect Test: $result")
                }
                
                // Initialize swipe autocorrect engine
                swipeAutocorrectEngine.initialize()
                loadDictionariesAsync()
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
            if (ensureEngineReady()) {
                coroutineScope.launch {
                    autocorrectEngine.preloadLanguages(enabledLanguages.toList())
                }
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
            // Language update handled by UnifiedAutocorrectEngine.setLocale()
            
            // Update enhanced autocorrect engine locale
            autocorrectEngine.setLocale(newLanguage)
            
            // Update keyboard view if available
            keyboardView?.let { kv ->
                val mode = when (currentKeyboard) {
                    KEYBOARD_LETTERS -> "letters"
                    KEYBOARD_SYMBOLS -> "symbols"
                    KEYBOARD_NUMBERS -> "numbers"
                    else -> "letters"
                }
                val newKeyboard = keyboardLayoutManager.getCurrentKeyboard(mode)
                if (newKeyboard != null) {
                    keyboard = newKeyboard
                    kv.keyboard = keyboard
                    kv.invalidateAllKeys()
                    // NOTE: Rebind listener after keyboard reassignment
                    rebindKeyboardListener()
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
            if (ensureEngineReady()) {
                coroutineScope.launch {
                    autocorrectEngine.preloadLanguages(enabledLanguages.toList())
                }
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
            background = themeManager.createKeyboardBackground()
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
                currentLanguage.uppercase(), 
                showNumberRow
            )
            
            keyboard = Keyboard(this@AIKeyboardService, keyboardResource)
            setKeyboard(keyboard)
            setOnKeyboardActionListener(this@AIKeyboardService)
            setSwipeListener(this@AIKeyboardService)
            setSwipeEnabled(swipeTypingEnabled)
            isPreviewEnabled = keyPreviewEnabled
            
            Log.d(TAG, "✅ Initial keyboard listener bound in onCreateInputView")
            
            // Set keyboard service reference for clipboard functionality
            setKeyboardService(this@AIKeyboardService)
            
            // Apply system insets to keyboard view as well
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Add extra padding for the keyboard view itself
                view.setPadding(0, 0, 0, navInsets.bottom / 2)
                Log.d(TAG, "Keyboard view insets applied - padding: ${navInsets.bottom / 2}px")
                insets
            }
            
            Log.d(TAG, "Initial keyboard loaded - Language: $currentLanguage, NumberRow: $showNumberRow, Resource: $keyboardResource")
        }
        
        // Create media panel manager (but don't add to layout yet)
        createMediaPanel()
        
        // Create comprehensive emoji panel
        createEmojiPanel()
        
        // Create AI panel (CleverType-style)
        createAIPanel()
        
        // Initially show keyboard
        keyboardView?.let { keyboardContainer.addView(it) }
        mainLayout.addView(keyboardContainer)
        
        // Store reference to container for switching views
        this.keyboardContainer = keyboardContainer
        
        // Apply theme
        applyTheme()
        
        // After creating keyboard view, check for pending theme update
        if (pendingThemeUpdate) {
            mainHandler.postDelayed({
                applyThemeFromBroadcast()
                pendingThemeUpdate = false
            }, 100)
        }
        
        return mainLayout
    }
    
    private fun createSuggestionBarContainer(parent: LinearLayout) {
        Log.d(TAG, "Creating suggestion bar container")
        
        // Create container for suggestions only (language switch moved to global button)
        topContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = themeManager.createToolbarBackground()
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
        
        // STEP 2: Trigger first UI update after keyboard inflation
        mainHandler.postDelayed({
            Log.d(TAG, "✅ SuggestionStrip bound after layout inflation")
            updateAISuggestions()
        }, 300)
        
        Log.d(TAG, "✅ SuggestionContainer initialized safely in onCreateInputView")
        
        // Suggestion bar will be populated by the second method
    }
    
    private fun createSuggestionBar(parent: LinearLayout) {
        Log.d(TAG, "Creating suggestion bar - SIMPLIFIED: text-only, no chip backgrounds")
        
        val palette = themeManager.getCurrentPalette()
        
        suggestionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = themeManager.createSuggestionBarBackground() // Matches keyboard bg
            setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4)) // Reduced padding for compact bar
            visibility = View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            elevation = 0f // No shadow - seamless with keyboard
            tag = "suggestion_container"
        }
        
        // Create 3 plain text suggestions (CleverType/Gboard style)
        repeat(3) { index ->
            val suggestion = TextView(this).apply {
                // Text-only styling - NO CHIP BACKGROUND
                setTextColor(palette.suggestionText)  // Auto-contrast
                textSize = 14f  // Smaller for compact bar
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))  // Reduced padding
                gravity = Gravity.CENTER
                
                // NO BACKGROUND - completely transparent
                setBackgroundColor(Color.TRANSPARENT)
                
                isClickable = true
                isFocusable = true
                text = ""
                visibility = View.VISIBLE
                
                // Simple click handling
                setOnClickListener { view ->
                    val suggestionText = (view as TextView).text.toString()
                    Log.d(TAG, "Suggestion clicked: '$suggestionText'")
                    if (suggestionText.isNotEmpty()) {
                        applySuggestion(suggestionText)
                    }
                }
                
                tag = "suggestion_text_$index"
            }
            
            val params = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            ).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)  // Minimal spacing
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
    
    /**
     * Create CleverType-style AI Panel
     */
    private fun createAIPanel() {
        try {
            Log.d(TAG, "Creating CleverType-style AI Panel")
            
            val palette = themeManager.getCurrentPalette()
            
            // Main AI panel container with themed background
            aiPanel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                // Use themed background (same as keyboard)
                background = themeManager.createKeyboardBackground()
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                visibility = View.GONE
            }
            
            // === TOP ROW: Back button + Language selector ===
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dpToPx(12))
            }
            
            // Back button with themed colors
            val backButton = TextView(this).apply {
                text = "←"
                textSize = 24f
                setTextColor(palette.keyText) // Use themed text color
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(palette.keyBg) // Use themed key background
                    setStroke(dpToPx(2), palette.keyBorder)
                }
                contentDescription = "Back to keyboard"
                setOnClickListener { closeAIPanel() }
            }
            
            // Title
            val titleView = TextView(this).apply {
                text = "AI Writing Assistant"
                textSize = 18f
                setTextColor(palette.keyText) // Use themed text color
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(dpToPx(12), 0, dpToPx(12), 0)
                }
            }
            
            // Language spinner
            aiLanguageSpinner = android.widget.Spinner(this).apply {
                val languages = arrayOf("English", "Telugu", "Hindi", "Spanish", "French")
                adapter = android.widget.ArrayAdapter(
                    this@AIKeyboardService,
                    android.R.layout.simple_spinner_item,
                    languages
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(120),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            topRow.addView(backButton)
            topRow.addView(titleView)
            topRow.addView(aiLanguageSpinner)
            
            // === CHIP SCROLL VIEW ===
            val chipScrollView = android.widget.HorizontalScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                setPadding(0, dpToPx(8), 0, dpToPx(16))
            }
            
            aiChipContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(dpToPx(4), 0, dpToPx(4), 0)
            }
            
            chipScrollView.addView(aiChipContainer)
            
            // === AI RESULT CARD ===
            val resultCardContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(200)
                ).apply {
                    setMargins(0, dpToPx(8), 0, dpToPx(16))
                }
                // Use themed card background
                val cornerRadius = dpToPx(12).toFloat()
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(palette.suggestionBg) // Use suggestion bar background color
                    setCornerRadius(cornerRadius)
                    setStroke(dpToPx(1), palette.keyBorder)
                }
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }
            
            // Result label
            val resultLabel = TextView(this).apply {
                text = "AI Result"
                textSize = 12f
                setTextColor(palette.suggestionText) // Use themed text color
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 0, dpToPx(8))
            }
            
            // Result text (scrollable)
            val resultScrollView = android.widget.ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            
            aiResultView = TextView(this).apply {
                text = "Select an AI feature to get started...\n\nYour text will be processed and the result will appear here."
                textSize = 14f
                setTextColor(palette.keyText) // Use themed text color
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            }
            
            resultScrollView.addView(aiResultView)
            resultCardContainer.addView(resultLabel)
            resultCardContainer.addView(resultScrollView)
            
            // === REPLACE TEXT BUTTON ===
            aiReplaceButton = android.widget.Button(this).apply {
                text = "Replace Text"
                textSize = 16f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(48)
                )
                // Use special accent color from theme
                val cornerRadius = dpToPx(24).toFloat()
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(palette.specialAccent)
                    setCornerRadius(cornerRadius)
                }
                isEnabled = false
                alpha = 0.5f
                setOnClickListener { replaceTextFromAIPanel() }
            }
            
            // Add all sections to main panel
            aiPanel?.addView(topRow)
            aiPanel?.addView(chipScrollView)
            aiPanel?.addView(resultCardContainer)
            aiPanel?.addView(aiReplaceButton)
            
            Log.d(TAG, "✅ AI Panel created successfully (CleverType style)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating AI panel", e)
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
        Log.d(TAG, "Creating modern emoji panel with fixed toolbar")
        
        // Create new XML-based emoji panel controller
        emojiPanelController = EmojiPanelController(
            context = this,
            themeManager = themeManager,
            onBackToLetters = {
                // Return to letters layout
                if (isEmojiPanelVisible) {
                    toggleEmojiPanel()
                }
            },
            inputConnectionProvider = { currentInputConnection }
        )
        
        // Keep old GboardEmojiPanel for backward compatibility
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
            
            // Set backspace handler for emoji panel
            setOnBackspacePressedListener {
                // Handle backspace in emoji mode - should delete text/emojis
                val ic = currentInputConnection
                if (ic != null) {
                    handleBackspace(ic)
                    Log.d(TAG, "Backspace pressed in emoji panel")
                } else {
                    Log.w(TAG, "No input connection available for emoji backspace")
                }
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
    
    /**
     * Rebind OnKeyboardActionListener after layout changes
     * NOTE: Always call this after setKeyboard() or layout changes to keep spacebar long-press working
     */
    private fun rebindKeyboardListener() {
        keyboardView?.apply {
            setOnKeyboardActionListener(this@AIKeyboardService)
            setSwipeListener(this@AIKeyboardService)
        }
        // Reset long-press state
        longPressHandler.removeCallbacks(longPressRunnable)
        currentLongPressKey = -1
        Log.d(TAG, "✅ Rebound OnKeyboardActionListener after layout change. View=$keyboardView")
    }
    
    /**
     * Load language preferences from SharedPreferences
     */
    private fun loadLanguagePreferences() {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            
            // Load enabled languages (comma-separated string)
            val enabledLangsStr = prefs.getString("flutter.enabled_languages", null)
            enabledLanguages = if (enabledLangsStr != null && enabledLangsStr.isNotEmpty()) {
                enabledLangsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                listOf("en") // Default to English
            }
            
            // CRITICAL: Ensure enabled list is never empty
            if (enabledLanguages.isEmpty()) {
                enabledLanguages = listOf("en")
                Log.w(TAG, "⚠️ Enabled languages was empty, defaulting to English")
            }
            
            // Load current language
            var loadedLanguage = prefs.getString("flutter.current_language", null)?.takeIf { it.isNotEmpty() }
            
            // CRITICAL: Validate current language is in enabled list
            if (loadedLanguage != null && !enabledLanguages.contains(loadedLanguage)) {
                Log.w(TAG, "⚠️ Current language '$loadedLanguage' not in enabled list $enabledLanguages")
                loadedLanguage = null  // Force fallback
            }
            
            // Set current language with fallback
            currentLanguage = loadedLanguage ?: enabledLanguages.firstOrNull() ?: "en"
            
            // Save corrected current language back to preferences
            if (loadedLanguage == null || loadedLanguage != currentLanguage) {
                prefs.edit().putString("flutter.current_language", currentLanguage).apply()
                Log.d(TAG, "✅ Corrected current language to: $currentLanguage")
            }
            
        // Load multilingual mode
        multilingualEnabled = prefs.getBoolean("flutter.multilingual_enabled", false)
        
        // Phase 2: Load feature flags
        transliterationEnabled = prefs.getBoolean("flutter.transliteration_enabled", true)
        reverseTransliterationEnabled = prefs.getBoolean("flutter.reverse_transliteration_enabled", false)
        
        // Update current language index
        currentLanguageIndex = enabledLanguages.indexOf(currentLanguage).coerceAtLeast(0)
        
        Log.d(TAG, "✅ Language prefs loaded: enabled=$enabledLanguages, current=$currentLanguage (idx=$currentLanguageIndex), multi=$multilingualEnabled")
        Log.d(TAG, "✅ Feature flags: translit=$transliterationEnabled, reverse=$reverseTransliterationEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading language preferences", e)
            enabledLanguages = listOf("en")
            currentLanguage = "en"
            currentLanguageIndex = 0
            multilingualEnabled = false
        }
    }
    
    /**
     * Initialize transliteration engine for Indic languages (Phase 1)
     */
    private fun initializeTransliteration() {
        try {
            if (currentLanguage in listOf("hi", "te", "ta")) {
                transliterationEngine = TransliterationEngine(this, currentLanguage)
                indicScriptHelper = IndicScriptHelper()
                
                // Phase 2: Dictionary and autocorrect integration
                // TODO: Integrate with existing systems
                // Load dictionary if not already loaded
                // if (multilingualDictionary.isLoaded(currentLanguage) == false) {
                //     multilingualDictionary.loadLanguage(currentLanguage, coroutineScope)
                // }
                
                applyLanguageSpecificFont(currentLanguage)
                updateSpacebarLanguageLabel(currentLanguage)
                Log.d(TAG, "✅ Transliteration initialized for $currentLanguage")
            } else {
                // Clean up for non-Indic languages
                transliterationEngine = null
                indicScriptHelper = null
                romanBuffer.clear()
                // Font will be handled by system default
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing transliteration for $currentLanguage", e)
        }
    }
    
    /**
     * Initialize multilingual dictionary (Phase 2)
     * TODO: Integrate with existing MultilingualDictionary system
     */
    private fun initializeMultilingualDictionary() {
        // Commented out to avoid conflicts with existing MultilingualDictionary
        // New Phase 2 implementation in EnhancedAutocorrectEngine.kt is ready for integration
        Log.d(TAG, "Phase 2 dictionary integration pending - using existing system")
    }
    
    /**
     * Get merged suggestions from multiple languages (Phase 2: Multilingual Fusion)
     * TODO: Integrate with existing MultilingualAutocorrectEngine
     */
    private fun getMergedSuggestions(typed: String, context: List<String>): List<String> {
        // Placeholder - existing multilingual system handles this
        Log.d("Fusion", "Using existing multilingual autocorrect system")
        return emptyList()
    }
    
    /**
     * Update suggestion strip with real-time transliteration suggestions (Phase 2)
     */
    private fun updateTransliterationSuggestions(suggestions: List<String>) {
        try {
            // Display top 3 transliteration suggestions
            if (suggestions.isNotEmpty()) {
                Log.d(TAG, "Real-time suggestions: ${suggestions.take(3)}")
                // TODO: Update actual suggestion strip UI when available
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not update transliteration suggestions", e)
        }
    }
    
    /**
     * Clear transliteration suggestions from UI (Phase 2)
     */
    private fun clearTransliterationSuggestions() {
        try {
            // TODO: Clear suggestion strip
            Log.d(TAG, "Cleared transliteration suggestions")
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear transliteration suggestions", e)
        }
    }
    
    /**
     * Apply language-specific fonts (Noto fonts for Indic scripts)
     * Phase 2: Now uses FontManager
     */
    private fun applyLanguageSpecificFont(language: String) {
        try {
            val typeface = FontManager.getTypefaceFor(language, assets)
            if (typeface != null) {
                // TODO: Apply to keyboard view when API is available
                // keyboardView?.setTypeface(typeface)
                Log.d(TAG, "✅ Custom font loaded for $language")
            } else {
                Log.d(TAG, "Using system font for $language")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Font loading error, using system font", e)
        }
    }
    
    /**
     * Update spacebar to show language name in native script
     */
    private fun updateSpacebarLanguageLabel(language: String) {
        val label = when (language) {
            "hi" -> "हिन्दी"
            "te" -> "తెలుగు"
            "ta" -> "தமிழ்"
            "en" -> "English"
            "es" -> "Español"
            "fr" -> "Français"
            "de" -> "Deutsch"
            else -> language.uppercase()
        }
        
        try {
            keyboardView?.keyboard?.keys?.find { it.codes.firstOrNull() == KEYCODE_SPACE }?.let { spaceKey ->
                spaceKey.label = label
                val keyIndex = keyboardView?.keyboard?.keys?.indexOf(spaceKey) ?: 0
                keyboardView?.invalidateKey(keyIndex)
                Log.d(TAG, "✅ Spacebar label updated: $label")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Could not update spacebar label", e)
        }
    }
    
    /**
     * Cycle to next language in enabled list
     */
    private fun cycleLanguage() {
        // CRITICAL: Reload language preferences first to catch any changes from the app
        loadLanguagePreferences()
        
        if (enabledLanguages.isEmpty() || enabledLanguages.size < 1) {
            Log.w(TAG, "⚠️ No enabled languages to cycle")
            Toast.makeText(this, "⚠️ No languages configured", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (enabledLanguages.size == 1) {
            Log.d(TAG, "Only one language enabled: ${enabledLanguages[0]}")
            Toast.makeText(this, "Only one language configured", Toast.LENGTH_SHORT).show()
            return
        }
        
        // CRITICAL: Validate current language is still in the list
        val currentIndex = enabledLanguages.indexOf(currentLanguage)
        if (currentIndex == -1) {
            // Current language was removed, start from first language
            Log.w(TAG, "⚠️ Current language '$currentLanguage' no longer enabled, resetting to first")
            currentLanguageIndex = 0
            currentLanguage = enabledLanguages[0]
        } else {
            currentLanguageIndex = currentIndex
        }
        
        // Cycle to next language
        currentLanguageIndex = (currentLanguageIndex + 1) % enabledLanguages.size
        currentLanguage = enabledLanguages[currentLanguageIndex]
        
        // Save to SharedPreferences
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            prefs.edit().putString("flutter.current_language", currentLanguage).apply()
            Log.d(TAG, "✅ Language cycled to: $currentLanguage (${currentLanguageIndex + 1}/${enabledLanguages.size})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving language cycle", e)
        }
        
        // Reinitialize transliteration for the new language
        initializeTransliteration()
        
        // Reload keyboard layout for the new language
        if (currentKeyboardMode == KeyboardMode.LETTERS) {
            switchKeyboardMode(KeyboardMode.LETTERS)  // This will rebind listener
            Log.d(TAG, "✅ Keyboard layout reloaded for $currentLanguage")
        } else {
            Log.d(TAG, "⚠️ Not in LETTERS mode, skipping layout reload. Mode: $currentKeyboardMode")
        }
        
        // Show toast notification
        showLanguageToast(currentLanguage)
    }
    
    /**
     * Show language switch toast
     */
    private fun showLanguageToast(langCode: String) {
        try {
            val langNames = mapOf(
                "en" to "English",
                "hi" to "हिन्दी",
                "te" to "తెలుగు",
                "ta" to "தமிழ்",
                "mr" to "मराठी",
                "bn" to "বাংলা",
                "gu" to "ગુજરાતી",
                "kn" to "ಕನ್ನಡ",
                "ml" to "മലയാളം",
                "pa" to "ਪੰਜਾਬੀ",
                "ur" to "اردو",
                "es" to "Español",
                "fr" to "Français",
                "de" to "Deutsch"
            )
            val displayName = langNames[langCode] ?: langCode.uppercase()
            Toast.makeText(this, "🌐 $displayName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing language toast", e)
        }
    }
    
    /**
     * Apply unified settings loaded from SettingsManager
     * Replaces loadSettings(), loadEnhancedSettings(), and loadKeyboardSettings()
     * @param unified The consolidated settings object
     * @param logSuccess If true, logs "Settings loaded" message
     */
    private fun applyLoadedSettings(unified: UnifiedSettings, logSuccess: Boolean = false) {
        try {
            // Apply core settings to service fields
            vibrationEnabled = unified.vibrationEnabled
            soundEnabled = unified.soundEnabled
            keyPreviewEnabled = unified.keyPreviewEnabled
            showNumberRow = unified.showNumberRow
            swipeTypingEnabled = unified.swipeTypingEnabled
            aiSuggestionsEnabled = unified.aiSuggestionsEnabled
            currentLanguage = unified.currentLanguage
            enabledLanguages = unified.enabledLanguages
            // Note: autocorrect/autoCap/doubleSpace/popup fields may not exist as direct properties
            // They are read from settings but may be handled differently in the service
            
            // Compute hash to detect actual changes
            val newHash = listOf(
                unified.vibrationEnabled, unified.soundEnabled, unified.keyPreviewEnabled,
                unified.showNumberRow, unified.swipeTypingEnabled, unified.aiSuggestionsEnabled,
                unified.currentLanguage, unified.enabledLanguages.joinToString(","),
                unified.autocorrectEnabled, unified.autoCapitalization, unified.doubleSpacePeriod
            ).hashCode()
            
            val settingsChanged = newHash != lastLoadedSettingsHash
            if (settingsChanged) {
                lastLoadedSettingsHash = newHash
                // Apply side effects: theme, layout updates, etc.
                keyboardView?.isPreviewEnabled = keyPreviewEnabled
                keyboardView?.setSwipeEnabled(swipeTypingEnabled)
            }
            
            if (logSuccess) {
                Log.i(TAG, "Settings loaded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying loaded settings", e)
        }
    }
    
    /**
     * @deprecated Use applyLoadedSettings(settingsManager.loadAll()) instead
     * Kept for compatibility during refactoring
     */
    @Deprecated("Use unified settings loader")
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
            
            // Current language index is now managed by loadLanguagePreferences()
            
            // Legacy log removed - now using unified "Settings loaded" in applyLoadedSettings()
        
        // Load advanced feedback settings
        hapticIntensity = settings.getInt("haptic_intensity", 2) // medium by default
        soundIntensity = settings.getInt("sound_intensity", 1) // light by default
        visualIntensity = settings.getInt("visual_intensity", 2) // medium by default
        soundVolume = settings.getFloat("sound_volume", 0.3f)
            
            // Load custom AI prompts
            loadCustomPrompts()
            
            // Legacy duplicate log removed - unified logging now in applyLoadedSettings()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings", e)
        }
    }
    
    private fun loadCustomPrompts() {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            
            // Load built-in action toggles
            builtInActionsEnabled["grammar"] = prefs.getBoolean("flutter.ai_action_grammar", true)
            builtInActionsEnabled["formal"] = prefs.getBoolean("flutter.ai_action_formal", true)
            builtInActionsEnabled["concise"] = prefs.getBoolean("flutter.ai_action_concise", true)
            builtInActionsEnabled["expand"] = prefs.getBoolean("flutter.ai_action_expand", true)
            
            // Load custom prompts
            customGrammarPrompts = parsePromptsFromJson(prefs.getString("flutter.ai_custom_grammar", "[]") ?: "[]")
            customTonePrompts = parsePromptsFromJson(prefs.getString("flutter.ai_custom_tones", "[]") ?: "[]")
            customAssistantPrompts = parsePromptsFromJson(prefs.getString("flutter.ai_custom_assistants", "[]") ?: "[]")
            
            Log.d(TAG, "Custom prompts loaded - Grammar: ${customGrammarPrompts.size}, Tones: ${customTonePrompts.size}, Assistants: ${customAssistantPrompts.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom prompts", e)
        }
    }
    
    private fun parsePromptsFromJson(json: String): MutableList<CustomPrompt> {
        val prompts = mutableListOf<CustomPrompt>()
        try {
            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                prompts.add(CustomPrompt(
                    title = jsonObject.getString("title"),
                    instruction = jsonObject.getString("instruction")
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing prompts JSON", e)
        }
        return prompts
    }
    
    private fun applyTheme() {
        keyboardView?.let { view ->
            // Apply theme using comprehensive ThemeManager
            val theme = themeManager.getCurrentTheme()
            
            // Set keyboard background
            val backgroundDrawable = themeManager.createKeyboardBackground()
            view.background = backgroundDrawable
            
            // The SwipeKeyboardView will request paint objects from ThemeManager
            if (view is SwipeKeyboardView) {
                view.setThemeManager(themeManager)
            }
            
            // Force redraw with new theme
            view.invalidateAllKeys()
            view.invalidate()
            
            // Streamlined theme application log
            Log.d(TAG, "Applied theme: ${theme.name} (${theme.id})")
        }
    }

    // Enhanced comprehensive theme application with unified palette and smooth transitions
    private fun applyThemeImmediately() {
        try {
            val theme = themeManager.getCurrentTheme()
            val palette = themeManager.getCurrentPalette()
            val enableAnimations = true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP // Always enable animations for V2
            
            Log.d(TAG, "🎨 Applying comprehensive theme: ${theme.name}${if (enableAnimations) " (animated)" else ""}")
            
            // 1. Update keyboard view with unified palette
            keyboardView?.let { view ->
                val backgroundDrawable = themeManager.createKeyboardBackground()
                view.background = backgroundDrawable
                
                if (view is SwipeKeyboardView) {
                    view.setThemeManager(themeManager)
                    view.refreshTheme()
                    
                    // Force complete repaint of all keys
                    view.invalidateAllKeys()
                    view.invalidate()
                    view.requestLayout()
                    
                    // Additional force refresh to ensure all components update
                    mainHandler.postDelayed({
                        view.invalidate()
                        Log.d(TAG, "🔄 Additional keyboard invalidation completed")
                    }, 50)
                }
                
                Log.d(TAG, "✅ Keyboard view themed with V2 system")
            }
            
            // 2. Update suggestion bar with V2 theming (SIMPLIFIED: text-only)
            suggestionContainer?.let { container ->
                // Background matches keyboard
                container.background = themeManager.createSuggestionBarBackground()
                container.elevation = 0f
                
                // Update each suggestion - text-only, NO chip backgrounds
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i)
                    if (child is TextView) {
                        // Only update text color - auto-contrast
                        child.setTextColor(palette.suggestionText)
                        child.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                Log.d(TAG, "✅ Suggestion bar themed - text-only, no chips")
            }
            
            // 3. Update main toolbar with V2 theming (seamless with keys)
            cleverTypeToolbar?.let { toolbar ->
                // Use themed toolbar background - matches keys exactly
                toolbar.background = themeManager.createToolbarBackground()
                toolbar.elevation = 0f // Remove any shadow/elevation for seamless connection
                
                // Set height from theme
                toolbar.layoutParams?.height = (palette.toolbarHeight * resources.displayMetrics.density).toInt()
                
                // SIMPLIFIED: Toolbar icons are already PNG ImageViews with no backgrounds
                // Just ensure no filters are applied
                for (i in 0 until toolbar.childCount) {
                    val child = toolbar.getChildAt(i)
                    if (child is ImageView) {
                        // Ensure no color filter or tint
                        child.clearColorFilter()
                        child.imageTintList = null
                        child.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                toolbar.requestLayout()
            }
            
            // 4. Update emoji panel theming
            gboardEmojiPanel?.let { panel ->
                applyThemeToEmojiPanel(panel, palette)
                Log.d(TAG, "✅ Emoji panel (legacy) themed")
            }
            
            // Apply theme to new emoji panel controller
            emojiPanelController?.applyTheme()
            Log.d(TAG, "✅ Emoji panel (new) themed")
            
            // 5. Update media panel theming
            mediaPanelManager?.let { panel ->
                applyThemeToMediaPanel(panel, palette)
                Log.d(TAG, "✅ Media panel themed")
            }
            
            // 6. Update top container (toolbar + suggestions)
            topContainer?.let { container ->
                container.background = themeManager.createKeyboardBackground()
                Log.d(TAG, "✅ Top container themed")
            }
            
            // 7. Update keyboard container background
            keyboardContainer?.background = themeManager.createKeyboardBackground()
            
            Log.d(TAG, "🎨 Complete theme application finished successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error in comprehensive theme application", e)
        }
    }
    
    /**
     * Apply theme to emoji panel recursively
     */
    private fun applyThemeToEmojiPanel(panel: android.view.View, palette: com.example.ai_keyboard.themes.ThemePaletteV2) {
        try {
            if (panel is android.view.ViewGroup) {
                panel.setBackgroundColor(palette.keyboardBg)
                
                for (i in 0 until panel.childCount) {
                    val child = panel.getChildAt(i)
                    when (child) {
                        is android.widget.LinearLayout -> {
                            // Apply to category tabs and toolbar
                            if (child.tag == "emoji_categories" || child.tag == "emoji_header") {
                                child.setBackgroundColor(palette.toolbarBg)
                            }
                            applyThemeToEmojiPanel(child, palette)
                        }
                        is android.widget.TextView -> {
                            // Theme category text
                            if (child.tag?.toString()?.startsWith("category_") == true) {
                                child.setTextColor(palette.keyText)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error theming emoji panel", e)
        }
    }
    
    /**
     * Apply theme to media panel (GIF/Stickers)
     */
    private fun applyThemeToMediaPanel(panel: android.view.View, palette: com.example.ai_keyboard.themes.ThemePaletteV2) {
        try {
            if (panel is android.view.ViewGroup) {
                panel.setBackgroundColor(palette.keyboardBg)
                
                for (i in 0 until panel.childCount) {
                    val child = panel.getChildAt(i)
                    when (child) {
                        is android.widget.LinearLayout -> {
                            if (child.tag == "media_header") {
                                child.setBackgroundColor(palette.toolbarBg)
                            }
                            applyThemeToMediaPanel(child, palette)
                        }
                        is android.widget.EditText -> {
                            // Theme search box
                            child.setBackgroundColor(palette.keyBg)
                            child.setTextColor(palette.keyText)
                            child.setHintTextColor(adjustColorAlpha(palette.keyText, 0.6f))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error theming media panel", e)
        }
    }
    
    /**
     * Adjust color alpha for hint text, etc.
     */
    private fun adjustColorAlpha(color: Int, alpha: Float): Int {
        val a = (android.graphics.Color.alpha(color) * alpha).toInt()
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        return android.graphics.Color.argb(a, r, g, b)
    }
    
    /**
     * Animate background color change with smooth transition
     */
    private fun animateBackgroundColor(view: android.view.View, toColor: Int, duration: Long = 200) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                val fromColor = (view.background as? android.graphics.drawable.ColorDrawable)?.color ?: toColor
                val colorAnimation = android.animation.ValueAnimator.ofObject(
                    android.animation.ArgbEvaluator(),
                    fromColor,
                    toColor
                )
                colorAnimation.duration = duration
                colorAnimation.addUpdateListener { animator ->
                    view.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
            } catch (e: Exception) {
                // Fallback to instant color change
                view.setBackgroundColor(toColor)
            }
        } else {
            view.setBackgroundColor(toColor)
        }
    }
    
    /**
     * Animate text color change with smooth transition
     */
    private fun animateTextColor(textView: android.widget.TextView, toColor: Int, duration: Long = 200) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                val fromColor = textView.currentTextColor
                val colorAnimation = android.animation.ValueAnimator.ofObject(
                    android.animation.ArgbEvaluator(),
                    fromColor,
                    toColor
                )
                colorAnimation.duration = duration
                colorAnimation.addUpdateListener { animator ->
                    textView.setTextColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
            } catch (e: Exception) {
                // Fallback to instant color change
                textView.setTextColor(toColor)
            }
        } else {
            textView.setTextColor(toColor)
        }
    }

    // Add visual confirmation of theme change
    private fun showThemeChangeConfirmation() {
        try {
            suggestionContainer?.let { container ->
                if (container.childCount > 0) {
                    val firstSuggestion = container.getChildAt(0) as? TextView
                    firstSuggestion?.apply {
                        val themeName = themeManager.getCurrentTheme().name
                        text = "✨ Theme: $themeName"
                        val palette = themeManager.getCurrentPalette()
                        setTextColor(palette.specialAccent)
                        visibility = View.VISIBLE
                    }
                    
                    // Reset after 2 seconds
                    mainHandler.postDelayed({
                        try {
                            updateAISuggestions()
                        } catch (e: Exception) {
                            // Ignore UI update errors
                        }
                    }, 2000)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing theme confirmation", e)
        }
    }

    // Apply theme from broadcast with comprehensive updates
    private fun applyThemeFromBroadcast() {
        try {
            Log.d(TAG, "Applying theme from broadcast...")
            
            // Reload theme from SharedPreferences
            themeManager.reload()
            val reloadSuccess = true // V2 always succeeds with fallback to default
            if (!reloadSuccess) {
                Log.w(TAG, "Failed to reload theme from SharedPreferences")
                return
            }
            
            // Apply comprehensive theme updates
            applyThemeImmediately()
            
            // Show visual confirmation
            showThemeChangeConfirmation()
            
            Log.d(TAG, "Theme successfully applied from broadcast")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme from broadcast", e)
        }
    }
    
    // Theme data verification for debugging
    private fun verifyThemeData() {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val themeData = prefs.getString("flutter.current_theme_data", null)
            val themeId = prefs.getString("flutter.current_theme_id", null)
            
            Log.d(TAG, "Theme verification - ID: $themeId, Data length: ${themeData?.length}")
            
            if (themeData != null) {
                try {
                    val json = org.json.JSONObject(themeData)
                    val themeName = json.optString("name", "Unknown")
                    val backgroundColor = json.optInt("backgroundColor", 0)
                    Log.d(TAG, "Theme JSON parsed - Name: $themeName, BG Color: $backgroundColor")
                } catch (e: Exception) {
                    Log.e(TAG, "Invalid theme JSON data", e)
                }
            } else {
                Log.w(TAG, "No theme data found in SharedPreferences")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying theme data", e)
        }
    }

    // Update suggestion bar theme (SIMPLIFIED: text-only, no chips)
    private fun updateSuggestionBarTheme() {
        val palette = themeManager.getCurrentPalette()
        
        suggestionContainer?.let { container ->
            container.background = themeManager.createSuggestionBarBackground()
            container.elevation = 0f
            
            // Update text color only - NO chip backgrounds
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is TextView) {
                    child.setTextColor(palette.suggestionText)
                    child.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            container.requestLayout()
        }
        
        topContainer?.let { container ->
            container.background = themeManager.createSuggestionBarBackground()
            container.elevation = 0f
            container.requestLayout()
        }
    }

    // Show theme update confirmation
    private fun showThemeUpdateConfirmation() {
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as? TextView
                firstSuggestion?.apply {
                    val themeName = themeManager.getCurrentTheme().name
                    text = "✨ Theme: $themeName"
                    setTextColor(Color.parseColor("#4CAF50"))
                    visibility = View.VISIBLE
                }
                
                // Reset after 2 seconds
                mainHandler.postDelayed({
                    updateAISuggestions()
                }, 2000)
            }
        }
    }
    
    
    private fun getThemeTextColor(): Int {
        // Return default text color
        return Color.BLACK
    }
    
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        // ✅ Intercept separators for autocorrect BEFORE normal processing
        if (isSeparator(primaryCode)) {
            Log.d(TAG, "🔍 Separator detected: code=$primaryCode")
            // Apply autocorrect and commit separator, then return to prevent double-processing
            if (applyAutocorrectOnSeparator(primaryCode)) {
                playClick(primaryCode)
                // Clear currentWord since we just finished a word
                currentWord = ""
                // Update suggestions for next word
                if (aiSuggestionsEnabled) {
                    updateAISuggestions()
                }
                return
            } else {
                Log.w(TAG, "⚠️ applyAutocorrectOnSeparator returned false")
            }
        }
        
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
            KEYCODE_SYMBOLS -> cycleKeyboardMode()  // ?123 key cycles forward
            KEYCODE_LETTERS -> returnToLetters()    // ABC key returns to letters
            KEYCODE_NUMBERS -> cycleKeyboardMode()  // Also cycle
            -3 -> cycleKeyboardMode()  // Handle XML ?123 code
            -2 -> returnToLetters()    // Handle XML ABC code
            KEYCODE_VOICE -> {
                // Enhanced voice input with visual feedback
                handleVoiceInput()
                ensureCursorStability()
            }
            KEYCODE_GLOBE -> {
                // Language switching - cycle through enabled languages
                cycleLanguage()
                ensureCursorStability()
            }
            KEYCODE_EMOJI -> {
                // Enhanced emoji panel with visual feedback
                switchKeyboardMode(KeyboardMode.EMOJI)
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
            
            // Disable undo once user continues typing (accepts the correction)
            if (undoAvailable && lastCorrection != null) {
                val (_, corrected) = lastCorrection!!
                // Only disable if they're typing a NEW character, not continuing the corrected word
                if (!corrected.startsWith(currentWord, ignoreCase = true)) {
                    undoAvailable = false
                    Log.d(TAG, "🔒 Undo disabled - user accepted correction and continued typing")
                }
            }
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
    
    /**
     * Check if a primary code represents a separator (space, punctuation, or enter).
     * Uses actual character codes, not KeyEvent constants.
     */
    private fun isSeparator(code: Int): Boolean {
        // Space + common punctuation + ENTER/DONE
        return code == 32 || code == 46 || code == 44 || code == 33 || code == 63 || // space . , ! ?
               code == 58 || code == 59 || code == 39 || code == 10 ||               // : ; ' \n
               code == -4                                                             // DONE/ENTER (Keyboard.KEYCODE_DONE)
    }
    
    /**
     * Preserve the capitalization pattern of the original word in the suggestion
     */
    private fun preserveCase(suggestion: String, original: String): String {
        return when {
            original.isEmpty() -> suggestion
            original.all { it.isUpperCase() || !it.isLetter() } -> suggestion.uppercase()
            original.firstOrNull()?.isUpperCase() == true -> suggestion.replaceFirstChar { it.uppercase() }
            else -> suggestion.lowercase()
        }
    }
    
    /**
     * Try to auto-correct the word immediately before the cursor and then commit the separator.
     * Reads the last word from InputConnection instead of relying on currentWord.
     * @return true if we handled the key (committed text); false to let normal flow continue.
     */
    private fun applyAutocorrectOnSeparator(code: Int): Boolean {
        val autocorrectEnabled = isAutoCorrectEnabled()
        Log.d(TAG, "🔍 applyAutocorrectOnSeparator: autocorrect=$autocorrectEnabled, code=$code")
        
        if (!autocorrectEnabled) {
            Log.w(TAG, "⚠️ Autocorrect is DISABLED in settings")
            return false
        }
        
        val ic = currentInputConnection ?: run {
            Log.w(TAG, "⚠️ No InputConnection available")
            return false
        }
        
        // Look back to capture the last token (64 chars is plenty for a single word)
        val before = ic.getTextBeforeCursor(64, 0) ?: return false
        
        // Last run of letters/apostrophes (works well for English and Latin-script languages)
        val match = Regex("([\\p{L}']+)$").find(before)
        if (match == null) {
            Log.d(TAG, "🔍 No word found before cursor: '$before'")
            if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
            return true
        }
        
        val original = match.groupValues[1]
        Log.d(TAG, "🔍 Found word: '$original' (length=${original.length})")
        
        if (original.isEmpty() || original.length < 2) {
            // Too short to correct, just commit separator
            Log.d(TAG, "🔍 Word too short to correct")
            if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
            return true
        }
        
        // Check if autocorrect engine is ready
        if (!ensureEngineReady()) {
            Log.w(TAG, "⚠️ Autocorrect engine not ready")
            // Engine not ready, just commit separator
            if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
            return true
        }
        
        Log.d(TAG, "🔍 Getting best suggestion for: '$original'")
        
        try {
            val best = autocorrectEngine.getBestSuggestion(original, currentLanguage)
            Log.d(TAG, "🔍 Best suggestion: '$best' for '$original'")
            
            if (best == null) {
                Log.d(TAG, "🔍 No suggestion available")
                // No suggestion; just commit the separator
                if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
                return true
            }
            
            // Check if user previously rejected this correction
            if (correctionRejected && lastCorrection?.first.equals(original, ignoreCase = true)) {
                Log.d(TAG, "🚫 Skipping autocorrect — user rejected previous correction for '$original'")
                correctionRejected = false
                lastCorrection = null
                // Just commit the separator
                if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
                return true
            }
            
            val confidence = autocorrectEngine.getConfidence(original, best)
            val shouldReplace = confidence >= 0.7f && !best.equals(original, ignoreCase = true)
            Log.d(TAG, "🔍 Confidence: $confidence, shouldReplace: $shouldReplace (threshold: 0.7)")
            
            ic.beginBatchEdit()
            if (shouldReplace) {
                val replaced = preserveCase(best, original)
                ic.deleteSurroundingText(original.length, 0)
                ic.commitText(replaced, 1)
                
                // Enhanced single-line logging
                Log.d(TAG, "⚙️ Applying correction: '$original'→'$replaced' (conf=$confidence, lang=$currentLanguage)")
                
                // Store correction for undo functionality
                lastCorrection = Pair(original, replaced)
                correctionRejected = false
                undoAvailable = true
                Log.d(TAG, "💾 Stored last correction for undo: '$original' → '$replaced'")
                
                // 🔥 CRITICAL: Learn from this correction for adaptive improvement
                try {
                    autocorrectEngine.onCorrectionAccepted(original, best, currentLanguage)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error learning from correction", e)
                }
            }
            // Always commit the separator if it's a printable character
            if (code > 0) {
                ic.commitText(String(Character.toChars(code)), 1)
            }
            ic.endBatchEdit()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyAutocorrectOnSeparator", e)
            // Fallback: just commit separator
            if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
            return true
        }
    }
    
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
        // ✅ Enhanced autocorrect using UnifiedAutocorrectEngine
        val word = currentWord.trim()
        if (word.isEmpty() || word.length < 2) {
            currentWord = ""
            return
        }
        
        try {
            // Check if engine is ready
            if (!ensureEngineReady()) {
                Log.w(TAG, "Autocorrect engine not ready")
                currentWord = ""
                return
            }
            
            // Get suggestions from UnifiedAutocorrectEngine
            val suggestions = autocorrectEngine.getSuggestions(word, currentLanguage, limit = 3)
            
            if (suggestions.isNotEmpty()) {
                val best = suggestions.first()
                
                // Calculate confidence using engine's method
                val confidence = autocorrectEngine.getConfidence(word, best)
                
                // Auto-apply if confidence is high and word is different
                if (confidence > 0.7f && best.lowercase() != word.lowercase()) {
                    Log.d(TAG, "✨ AutoCorrect applied: $word → $best (confidence: $confidence)")
                    
                    // Delete the typed word
                    ic.deleteSurroundingText(word.length, 0)
                    
                    // Insert corrected word (preserve original capitalization pattern)
                    val corrected = if (word.firstOrNull()?.isUpperCase() == true) {
                        best.replaceFirstChar { it.uppercase() }
                    } else {
                        best
                    }
                    ic.commitText(corrected, 1)
                    
                    currentWord = ""
                    return
                } else {
                    Log.d(TAG, "AutoCorrect skipped: $word → $best (confidence: $confidence, threshold: 0.7)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in performAutoCorrection", e)
        }
        
        currentWord = ""
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
        
        // Handle Gboard-style undo autocorrect on first backspace
        if (undoAvailable && lastCorrection != null) {
            val (original, corrected) = lastCorrection!!
            
            // Check if the corrected word is still present (user hasn't typed anything else)
            val textBefore = ic.getTextBeforeCursor(corrected.length + 5, 0)?.toString() ?: ""
            if (textBefore.endsWith(corrected) || textBefore.endsWith("$corrected ")) {
                ic.beginBatchEdit()
                // Delete the corrected word (and trailing space if present)
                val deleteLength = if (textBefore.endsWith(" ")) corrected.length + 1 else corrected.length
                ic.deleteSurroundingText(deleteLength, 0)
                // Restore original word
                ic.commitText(original, 1)
                ic.endBatchEdit()
                
                Log.d(TAG, "↩️ Undo autocorrect: reverted '$corrected' → '$original'")
                
                // Mark as rejected and disable undo
                correctionRejected = true
                undoAvailable = false
                currentWord = original
                
                // Blacklist this correction permanently
                try {
                    if (::userDictionaryManager.isInitialized) {
                        userDictionaryManager.blacklistCorrection(original, corrected)
                    }
                    autocorrectEngine.learnFromUser(original, original, currentLanguage)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to blacklist rejected correction", e)
                }
                
                // Update suggestions
                if (aiSuggestionsEnabled) {
                    updateAISuggestions()
                }
                return
            }
        }
        
        // Handle slide-to-delete mode
        if (isSlideToDeleteModeActive) {
            deleteLastWord(ic)
            return
        }
        
        // Check for double-backspace revert within 1 second
        val lastWord = currentWord
        val revertCandidate = autocorrectEngine.getRevertCandidate(lastWord)
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
                    autocorrectEngine.learnFromUser(revertCandidate.toString(), revertCandidate.toString(), currentLanguage)
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
        
        // Check if user is manually editing a corrected word (rejection signal)
        if (lastCorrection != null && !correctionRejected) {
            val (original, corrected) = lastCorrection!!
            val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
            
            // If user is deleting characters from the corrected word, mark as rejected
            if (textBefore.contains(original.take(2)) || currentWord.startsWith(original.take(2))) {
                correctionRejected = true
                undoAvailable = false
                Log.d(TAG, "🚫 User manually rejected autocorrect '$original' → '$corrected'")
                
                // Blacklist this correction permanently
                try {
                    if (::userDictionaryManager.isInitialized) {
                        userDictionaryManager.blacklistCorrection(original, corrected)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to blacklist rejected correction", e)
                }
                
                lastCorrection = null
            }
        }
        
        // Update suggestions after backspace
        if (aiSuggestionsEnabled) {
            updateAISuggestions()
        }
    }
    
    /**
     * Delete the last word (used in slide-to-delete mode)
     */
    private fun deleteLastWord(ic: InputConnection) {
        try {
            val textBeforeCursor = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
            val wordBoundaryRegex = "\\S+\\s*$".toRegex()
            val lastWordMatch = wordBoundaryRegex.find(textBeforeCursor)
            
            if (lastWordMatch != null) {
                val deleteLength = lastWordMatch.value.length
                ic.deleteSurroundingText(deleteLength, 0)
                Log.d(TAG, "Slide-to-delete removed: '${lastWordMatch.value.trim()}'")
                
                // Clear current word
                currentWord = ""
                
                // Provide haptic feedback
                performAdvancedHapticFeedback(Keyboard.KEYCODE_DELETE)
                
                // Update suggestions
                if (aiSuggestionsEnabled) {
                    updateAISuggestions()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in slide-to-delete", e)
            // Fallback to normal backspace
            ic.deleteSurroundingText(1, 0)
        }
    }
    
    /**
     * Activate slide-to-delete mode
     */
    fun activateSlideToDelete() {
        isSlideToDeleteModeActive = true
        Log.d(TAG, "Slide-to-delete mode activated")
    }
    
    /**
     * Deactivate slide-to-delete mode
     */
    fun deactivateSlideToDelete() {
        isSlideToDeleteModeActive = false
        Log.d(TAG, "Slide-to-delete mode deactivated")
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
        var dictionaryExpanded = false
        
        // Check for dictionary expansion FIRST (before processing word)
        if (currentWord.isNotEmpty() && dictionaryEnabled) {
            val expansion = dictionaryManager.getExpansion(currentWord)
            if (expansion != null) {
                checkDictionaryExpansion(currentWord)
                dictionaryExpanded = true
            }
            // Clear current word after expansion attempt
            currentWord = ""
        } else {
            // Process current word normally if no expansion
            if (currentWord.isNotEmpty()) {
                finishCurrentWord()
            }
        }
        
        // Only add space if dictionary didn't expand (expansion includes space already)
        if (!dictionaryExpanded) {
            // Handle double space for period (like iOS/GBoard)
            val textBefore = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
            if (textBefore.endsWith("  ")) {
                // Replace double space with period + space
                ic.deleteSurroundingText(2, 0)
                ic.commitText(". ", 1)
            } else {
                ic.commitText(" ", 1)
            }
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
                
                val context = listOfNotNull(prev1, prev2).filter { it.isNotBlank() }
                autocorrectEngine.processBoundary(context, currentLanguage)
                
                // Get suggestions for the current word
                val suggestions = autocorrectEngine.getCorrections(currentWord, currentLanguage, context)
                val topSuggestion = suggestions.firstOrNull()
                
                if (topSuggestion != null && topSuggestion.isCorrection) {
                    // Auto-apply correction
                    val correction = topSuggestion.word
                    withContext(Dispatchers.Main) {
                        val ic = currentInputConnection
                        if (ic != null) {
                            // Replace the current word with correction
                            ic.deleteSurroundingText(currentWord.length, 0)
                            ic.commitText("$correction ", 1)
                            
                            // Save correction history for revert
                            autocorrectEngine.learnFromUser(currentWord, correction.toString(), currentLanguage)
                            
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
                    // Update suggestion strip with top suggestions
                    val autocorrectCandidates = suggestions.map { 
                        AutocorrectCandidate(
                            word = it.word,
                            score = it.score,
                            confidence = it.score,
                            type = if (it.isCorrection) CandidateType.CORRECTION else CandidateType.ORIGINAL,
                            editDistance = if (it.isCorrection) 1 else 0
                        )
                    }
                    updateSuggestionStrip(autocorrectCandidates)
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
            val revertCandidate = autocorrectEngine.getRevertCandidate("") // Placeholder for lastWord
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
    
    /**
     * Switch keyboard mode with CleverType-style cycling
     * Letters → Numbers → Symbols → Letters
     */
    private fun switchKeyboardMode(targetMode: KeyboardMode) {
        Log.d(TAG, "🔄 Switching from $currentKeyboardMode to $targetMode")
        
        // Save previous mode for emoji panel return
        if (currentKeyboardMode != KeyboardMode.EMOJI) {
            previousKeyboardMode = currentKeyboardMode
        }
        
        when (targetMode) {
            KeyboardMode.LETTERS -> {
                val lang = currentLanguage.uppercase()
                val keyboardResource = getKeyboardResourceForLanguage(lang, showNumberRow)
                keyboard = Keyboard(this, keyboardResource)
                currentKeyboard = KEYBOARD_LETTERS
                keyboardView?.keyboard = keyboard
                keyboardView?.showNormalLayout()
                rebindKeyboardListener()  // NOTE: Always rebind after layout change
                applyTheme()
            }
            
            KeyboardMode.NUMBERS -> {
                keyboard = Keyboard(this, R.xml.symbols)  // Use symbols.xml which has numbers row
                currentKeyboard = KEYBOARD_NUMBERS
                keyboardView?.keyboard = keyboard
                keyboardView?.showNormalLayout()
                rebindKeyboardListener()  // NOTE: Always rebind after layout change
                applyTheme()
            }
            
            KeyboardMode.SYMBOLS -> {
                keyboard = Keyboard(this, R.xml.symbols)
                currentKeyboard = KEYBOARD_SYMBOLS
                keyboardView?.keyboard = keyboard
                keyboardView?.showNormalLayout()
                rebindKeyboardListener()  // NOTE: Always rebind after layout change
                applyTheme()
            }
            
            KeyboardMode.EMOJI -> {
                handleEmojiToggle()
                return  // Don't update currentKeyboardMode yet, handleEmojiToggle will do it
            }
        }
        
        currentKeyboardMode = targetMode
        Log.d(TAG, "✅ Switched to $currentKeyboardMode")
    }
    
    /**
     * Cycle to next keyboard mode (CleverType behavior)
     * Letters → Numbers → Symbols → Letters
     */
    private fun cycleKeyboardMode() {
        val nextMode = when (currentKeyboardMode) {
            KeyboardMode.LETTERS -> KeyboardMode.NUMBERS
            KeyboardMode.NUMBERS -> KeyboardMode.SYMBOLS
            KeyboardMode.SYMBOLS -> KeyboardMode.LETTERS
            KeyboardMode.EMOJI -> previousKeyboardMode
        }
        Log.d(TAG, "⚡ Cycling keyboard: $currentKeyboardMode → $nextMode")
        switchKeyboardMode(nextMode)
    }
    
    /**
     * Return to letters mode (ABC button)
     */
    private fun returnToLetters() {
        Log.d(TAG, "🔤 Returning to letters mode")
        switchKeyboardMode(KeyboardMode.LETTERS)
    }
    
    // DEPRECATED - kept for backward compatibility
    @Deprecated("Use switchKeyboardMode() instead", ReplaceWith("switchKeyboardMode(KeyboardMode.SYMBOLS)"))
    private fun switchToSymbols() {
        switchKeyboardMode(KeyboardMode.SYMBOLS)
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
            "TE" -> if (withNumbers) R.xml.qwerty_te_with_numbers else R.xml.qwerty_te
            "TA" -> if (withNumbers) R.xml.qwerty_ta_with_numbers else R.xml.qwerty_ta
            else -> if (withNumbers) R.xml.qwerty_with_numbers else R.xml.qwerty // Default to English
        }
    }
    
    @Deprecated("Use switchKeyboardMode() instead", ReplaceWith("switchKeyboardMode(KeyboardMode.LETTERS)"))
    private fun switchToLetters() {
        switchKeyboardMode(KeyboardMode.LETTERS)
    }
    
    @Deprecated("Use switchKeyboardMode() instead", ReplaceWith("switchKeyboardMode(KeyboardMode.NUMBERS)"))
    private fun switchToNumbers() {
        switchKeyboardMode(KeyboardMode.NUMBERS)
    }
    
    private fun handleLanguageSwitch() {
        try {
            // Cycle to next language
            // Language cycling is now handled by cycleLanguage() method
            // This legacy method is kept for compatibility
            
            // Save current language index
            settings.edit().putInt("current_language_index", currentLanguageIndex).apply()
            
            // Reload keyboard layout to reflect language change
            switchToLetters()
            
            // switchToLetters() → switchKeyboardMode() already calls rebindKeyboardListener()
            
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
        // Guard: Check if aiBridge is initialized before use
        if (!::aiBridge.isInitialized) {
            Log.w(TAG, "⚠️ AI Bridge not initialized yet, skipping AI suggestions for '$currentWord'")
            return
        }
        
        // STEP 1: Add debounce guard to prevent duplicate calls
        if (!shouldUpdateAISuggestions()) return
        
        Log.d(TAG, "updateAISuggestions called - aiSuggestionsEnabled: $aiSuggestionsEnabled, isAIReady: $isAIReady, currentWord: '$currentWord'")
        
        // STEP 1: Prevent endless retries before keyboard is ready
        if (suggestionContainer == null || keyboardView == null) {
            // Check if keyboard is still attached before retrying
            if (keyboardView?.isAttachedToWindow == false) {
                Log.w(TAG, "⚠️ Keyboard not attached; skipping suggestion update")
                return
            }
            
            if (retryCount < 5) {
                retryCount++
                // Exponential backoff: 100ms, 400ms, 900ms, 1600ms, 2500ms
                val delay = 100L * retryCount * retryCount
                Log.w(TAG, "⚠️ Suggestion container not ready, retry $retryCount/5 (delay ${delay}ms)")
                mainHandler.postDelayed({ updateAISuggestions() }, delay)
            } else {
                Log.e(TAG, "❌ Suggestion container never initialized after 5 retries")
                retryCount = 0 // Reset for next input session
            }
            return
        }
        
        // Reset retry count on success
        retryCount = 0
        
        if (!aiSuggestionsEnabled) {
            Log.d(TAG, "AI suggestions disabled in settings")
            return
        }
        
        val word = currentWord.trim()
        if (word.isEmpty()) {
            updateSuggestionUI(emptyList())
            return
        }
        
        try {
            // ✅ ALWAYS try local autocorrect first for instant suggestions (like "teh → the")
            val localSuggestions = if (ensureEngineReady()) {
                autocorrectEngine.getCorrections(word, currentLanguage).map { it.word }
            } else {
                emptyList()
            }
            
            // ✅ Fallback when AI not ready - show local suggestions immediately
            if (!isAIReady) {
                Log.w(TAG, "⚠️ AI not ready yet, skipping remote suggestions for now")
                if (localSuggestions.isNotEmpty()) {
                    updateSuggestionUI(localSuggestions)
                    return
                } else {
                    // Last resort: generate basic suggestions
                    updateSuggestionUI(generateEnhancedBasicSuggestions(word))
                    return
                }
            }
            
            // ✅ AI is ready - try to get AI suggestions, but always have fallback
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Get AI-powered suggestions
                    val context = getRecentContext()
                    Log.d(TAG, "Getting AI suggestions for word: '$word', context: $context")
                    
                    // Also try enhanced suggestions even if AI bridge isn't fully ready
                    val fallbackSuggestions = if (localSuggestions.isNotEmpty()) {
                        localSuggestions
                    } else {
                        generateEnhancedBasicSuggestions(word)
                    }
                    
                    aiBridge.getSuggestions(word, context, object : AIServiceBridge.AICallback {
                        override fun onSuggestionsReady(suggestions: List<AIServiceBridge.AISuggestion>) {
                            // Convert AI suggestions to display format
                            val suggestionTexts = suggestions.map { suggestion ->
                                // Mark corrections with indicator
                                if (suggestion.isCorrection) "✓ ${suggestion.word}" else suggestion.word
                            }
                            
                            // Update UI on main thread
                            coroutineScope.launch(Dispatchers.Main) {
                                if (suggestionTexts.isNotEmpty()) {
                                    updateSuggestionUI(suggestionTexts)
                                } else {
                                    // AI returned empty, use fallback
                                    updateSuggestionUI(fallbackSuggestions)
                                }
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
                            Log.w(TAG, "AI suggestion error: $error — using fallback")
                            // Fallback to local suggestions
                            coroutineScope.launch(Dispatchers.Main) {
                                updateSuggestionUI(fallbackSuggestions)
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating AI suggestions", e)
                    // Final fallback: show local suggestions on error
                    coroutineScope.launch(Dispatchers.Main) {
                        updateSuggestionUI(localSuggestions)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in updateAISuggestions", e)
            // Emergency fallback
            updateSuggestionUI(generateEnhancedBasicSuggestions(word))
        }
    }
    
    private fun showAutoCorrection(original: String, corrected: String) {
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as TextView
                firstSuggestion.apply {
                    text = "✓ $corrected"
                    val palette = themeManager.getCurrentPalette()
                    setTextColor(palette.specialAccent) // Theme accent for corrections
                    setBackgroundColor(adjustColorBrightness(palette.specialAccent, 1.8f)) // Light accent background
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
     * Load dictionaries asynchronously to prevent UI jank
     */
    private fun loadDictionariesAsync() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔵 [Dictionary] Starting async dictionary load...")
                
                // Show loading indicator on main thread
                withContext(Dispatchers.Main) {
                    // On first launch, show async loading spinner until ready
                    Toast.makeText(this@AIKeyboardService, "📚 Loading dictionaries...", Toast.LENGTH_SHORT).show()
                }
                
                // Load dictionaries in background
                loadDictionaries()
                
                // Update UI on main thread when complete
                withContext(Dispatchers.Main) {
                    Log.i(TAG, "🟢 [Dictionary] Async dictionary load completed")
                    Toast.makeText(this@AIKeyboardService, "✅ Dictionaries loaded", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "🔴 [Dictionary] Error in async dictionary load", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AIKeyboardService, "⚠️ Dictionary load failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Get built-in correction for a word using loaded corrections dictionary
     */
    private fun getBuiltInCorrection(word: String): String? {
        return corrections[word.lowercase()]
    }
    
    /**
     * Generate enhanced basic suggestions with bigram context when AI engines aren't ready
     */
    private fun generateEnhancedBasicSuggestions(currentWord: String): List<String> {
        try {
            // Get surrounding text context for previous word extraction
            val surrounding = getInputTextAroundCursor(50)
            val prevToken = surrounding.trimEnd().split(Regex("\\s+")).dropLast(1).lastOrNull()
            
            // Get current language
            val lang = if (::languageManager.isInitialized) {
                languageManager.getCurrentLanguage().lowercase() // "en", "de", etc.
            } else {
                "en"
            }
            
            Log.d(TAG, "Enhanced suggestions: currentWord='$currentWord', prevToken='$prevToken', lang='$lang'")
            
            // Use NextWordPredictor for word candidates
            val wordCandidates = if (::nextWordPredictor.isInitialized) {
                nextWordPredictor.getPredictions(listOfNotNull(prevToken), lang, 8)
                    .mapIndexed { idx, word -> word to (0.8 - idx * 0.1) }
            } else {
                // Fallback to simple prefix search
                if (currentWord.isNotEmpty()) {
                    autocorrectEngine.getCandidates(currentWord, lang, 5)
                        .mapIndexed { idx, word -> word to (0.5 - idx * 0.1) }
                } else {
                    listOf("the", "of", "to", "and", "a").mapIndexed { idx, word -> word to (0.5 - idx * 0.1) }
                }
            }
            
            // Get emoji candidates (existing system)
            val emojiSuggestions = EmojiSuggestionEngine.getSuggestionsForTyping(currentWord, surrounding)
            val emojiCandidates = emojiSuggestions.mapIndexed { idx, emoji -> emoji to (0.3 - idx * 0.05) }
            
            // Use SuggestionRanker to merge with context-aware emoji gating
            val finalSuggestions = com.example.ai_keyboard.predict.SuggestionRanker.mergeForStrip(
                currentWord = currentWord,
                contextPrev = prevToken,
                wordCands = wordCandidates,
                emojiCands = emojiCandidates,
                max = 5
            )
            
            Log.d(TAG, "Enhanced prediction results: ${finalSuggestions.joinToString(", ")}")
            return finalSuggestions
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced prediction, falling back to basic", e)
            // Simple fallback
            return if (currentWord.isEmpty()) {
                listOf("the", "of", "to", "and", "I")
            } else {
                listOf(currentWord)
            }
        }
    }
    
    /**
     * Get input text around cursor for context analysis
     */
    private fun getInputTextAroundCursor(maxLength: Int = 50): String {
        return try {
            val ic = currentInputConnection ?: return ""
            val textBefore = ic.getTextBeforeCursor(maxLength, 0)?.toString() ?: ""
            val textAfter = ic.getTextAfterCursor(maxLength, 0)?.toString() ?: ""
            "$textBefore$textAfter"
        } catch (e: Exception) {
            Log.w(TAG, "Error getting text around cursor", e)
            ""
        }
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
        
        // Check for revert request (user tapping original word)
        val revertCandidate = autocorrectEngine.getRevertCandidate("") // Placeholder for lastWord
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
                    autocorrectEngine.learnFromUser(currentWord, cleanSuggestion, currentLanguage)
                    
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
            
            // Setup long-press detection for accent characters only
            // NOTE: Spacebar long-press removed - use globe button for language switching
            if (hasAccentVariants(primaryCode)) {
                currentLongPressKey = primaryCode
                longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT)
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
            longPressHandler.removeCallbacks(longPressRunnable)
            currentLongPressKey = -1
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
        
        text?.let {
            // **PHASE 2: Check reverse transliteration first (Native → Roman)**
            if (reverseTransliterationEnabled &&
                !transliterationEnabled && // Only one direction at a time
                currentLanguage in listOf("hi", "te", "ta") &&
                transliterationEngine != null) {
                
                val isIndicInput = it.all { char -> char.code > 127 }
                
                if (isIndicInput) {
                    // Buffer native characters
                    romanBuffer.append(it)
                    
                    val lastChar = it.last()
                    if (lastChar.code in listOf(32, 46, 44, 33, 63, 10, 59, 58)) { // Space, punctuation
                        val nativeText = romanBuffer.toString().dropLast(1).trim()
                        
                        if (nativeText.isNotEmpty()) {
                            try {
                                val romanText = transliterationEngine!!.reverseTransliterate(nativeText)
                                
                                // Delete buffered native text
                                ic.deleteSurroundingText(romanBuffer.length, 0)
                                
                                // Insert Roman text + punctuation
                                ic.commitText("$romanText${lastChar}", 1)
                                
                                Log.d(TAG, "🔄 Reverse transliterated: '$nativeText' → '$romanText'")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Reverse transliteration error", e)
                                ic.commitText(it, 1)
                            }
                        } else {
                            ic.commitText(it, 1)
                        }
                        
                        romanBuffer.clear()
                        return
                    } else {
                        ic.commitText(it, 1)
                        return
                    }
                } else {
                    romanBuffer.clear()
                }
            }
            
            // **PHASE 1+2: Forward transliteration (Roman → Native) with autocorrect**
            if (transliterationEnabled && 
                currentLanguage in listOf("hi", "te", "ta") &&
                transliterationEngine != null) {
                
                val isRomanInput = transliterationEngine!!.isRomanInput(it.toString())
                
                if (isRomanInput) {
                    // Buffer Roman characters
                    romanBuffer.append(it)
                    
                    // **PHASE 2: Show real-time transliteration suggestions**
                    // Temporarily disabled - pending full integration
                    // if (romanBuffer.length >= 2) {
                    //     try {
                    //         val suggestions = transliterationEngine!!.getSuggestions(romanBuffer.toString())
                    //         updateTransliterationSuggestions(suggestions)
                    //     } catch (e: Exception) {
                    //         Log.w(TAG, "Could not get real-time suggestions", e)
                    //     }
                    // }
                    
                    // Check if we hit a word boundary (space, punctuation, newline)
                    val lastChar = it.last()
                    if (lastChar in listOf(' ', '.', ',', '!', '?', '\n', ';', ':')) {
                        // Transliterate the buffered text
                        val romanText = romanBuffer.toString().dropLast(1).trim()
                        
                        if (romanText.isNotEmpty()) {
                            try {
                                // Transliterate using Phase 1 engine
                                // Phase 2 autocorrect integration pending
                                val nativeText = transliterationEngine!!.transliterate(romanText)
                                
                                // Delete the buffered Roman text
                                ic.deleteSurroundingText(romanBuffer.length, 0)
                                
                                // Insert transliterated text + punctuation
                                ic.commitText("$nativeText$lastChar", 1)
                                
                                Log.d(TAG, "🔤 Transliterated: '$romanText' → '$nativeText'")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Transliteration error", e)
                                // Fallback: just commit the original text
                                ic.commitText(it, 1)
                            }
                        } else {
                            // Just punctuation, commit as-is
                            ic.commitText(it, 1)
                        }
                        
                        romanBuffer.clear()
                        clearTransliterationSuggestions()
                    } else {
                        // Still buffering - commit character but keep buffering
                        ic.commitText(it, 1)
                    }
                    return
                } else {
                    // Native script input detected - clear buffer and pass through
                    romanBuffer.clear()
                    clearTransliterationSuggestions()
                }
            }
            
            // Default behavior: no transliteration
            romanBuffer.clear()
            ic.commitText(it, 1)
        }
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
        keyboardView?.background = themeManager.createKeyboardBackground()
        
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
            longPressHandler.removeCallbacks(longPressRunnable)
            currentLongPressKey = -1
        } catch (e: Exception) {
            // Ignore swipe start errors
        }
    }
    
    override fun onSwipeEnded() {
        try {
            // Reset visual feedback
            keyboardView?.background = themeManager.createKeyboardBackground()
            
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
                    val palette = themeManager.getCurrentPalette()
                    setTextColor(palette.specialAccent) // Theme accent for success
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
    
    private fun getSwipeActiveColor(): Int {
        return themeManager.getCurrentPalette().keyPressed
    }
    private fun getSwipeTextColor(): Int {
        return themeManager.getCurrentPalette().specialAccent
    }
    
    private fun handleClose() {
        requestHideSelf(0)
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
    
    /**
     * Data class for keyboard settings from Flutter
     */
    data class KeyboardSettings(
        // General Settings
        val numberRow: Boolean = false,
        val hintedNumberRow: Boolean = false,
        val hintedSymbols: Boolean = false,
        val showUtilityKey: Boolean = true,
        val displayLanguageOnSpace: Boolean = true,
        val portraitFontSize: Double = 100.0,
        val landscapeFontSize: Double = 100.0,
        
        // Layout Settings
        val borderlessKeys: Boolean = false,
        val oneHandedMode: Boolean = false,
        val oneHandedModeWidth: Double = 87.0,
        val landscapeFullScreenInput: Boolean = true,
        val keyboardWidth: Double = 100.0,
        val keyboardHeight: Double = 100.0,
        val verticalKeySpacing: Double = 5.0,
        val horizontalKeySpacing: Double = 2.0,
        val portraitBottomOffset: Double = 1.0,
        val landscapeBottomOffset: Double = 2.0,
        
        // Key Press Settings
        val popupVisibility: Boolean = true,
        val longPressDelay: Double = 200.0
    )
    
    /**
     * Load keyboard settings from SharedPreferences
     */
    private fun loadKeyboardSettings() {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
            
            keyboardSettings = KeyboardSettings(
                // General Settings
                numberRow = prefs.getBoolean("flutter.keyboard_settings.number_row", false),
                hintedNumberRow = prefs.getBoolean("flutter.keyboard_settings.hinted_number_row", false),
                hintedSymbols = prefs.getBoolean("flutter.keyboard_settings.hinted_symbols", false),
                showUtilityKey = prefs.getBoolean("flutter.keyboard_settings.show_utility_key", true),
                displayLanguageOnSpace = prefs.getBoolean("flutter.keyboard_settings.display_language_on_space", true),
                portraitFontSize = prefs.getFloat("flutter.keyboard_settings.portrait_font_size", 100.0f).toDouble(),
                landscapeFontSize = prefs.getFloat("flutter.keyboard_settings.landscape_font_size", 100.0f).toDouble(),
                
                // Layout Settings
                borderlessKeys = prefs.getBoolean("flutter.keyboard_settings.borderless_keys", false),
                oneHandedMode = prefs.getBoolean("flutter.keyboard_settings.one_handed_mode", false),
                oneHandedModeWidth = prefs.getFloat("flutter.keyboard_settings.one_handed_mode_width", 87.0f).toDouble(),
                landscapeFullScreenInput = prefs.getBoolean("flutter.keyboard_settings.landscape_full_screen_input", true),
                keyboardWidth = prefs.getFloat("flutter.keyboard_settings.keyboard_width", 100.0f).toDouble(),
                keyboardHeight = prefs.getFloat("flutter.keyboard_settings.keyboard_height", 100.0f).toDouble(),
                verticalKeySpacing = prefs.getFloat("flutter.keyboard_settings.vertical_key_spacing", 5.0f).toDouble(),
                horizontalKeySpacing = prefs.getFloat("flutter.keyboard_settings.horizontal_key_spacing", 2.0f).toDouble(),
                portraitBottomOffset = prefs.getFloat("flutter.keyboard_settings.portrait_bottom_offset", 1.0f).toDouble(),
                landscapeBottomOffset = prefs.getFloat("flutter.keyboard_settings.landscape_bottom_offset", 2.0f).toDouble(),
                
                // Key Press Settings
                popupVisibility = prefs.getBoolean("flutter.keyboard_settings.popup_visibility", true),
                longPressDelay = prefs.getFloat("flutter.keyboard_settings.long_press_delay", 200.0f).toDouble()
            )
            
            // Apply settings immediately
            applyKeyboardSettings()
            
            Log.d(TAG, "Keyboard settings loaded: numberRow=${keyboardSettings.numberRow}, " +
                    "borderlessKeys=${keyboardSettings.borderlessKeys}, " +
                    "oneHandedMode=${keyboardSettings.oneHandedMode}")
                    
        } catch (e: Exception) {
            Log.e(TAG, "Error loading keyboard settings", e)
        }
    }
    
    /**
     * Apply loaded keyboard settings to the keyboard view and layout
     */
    private fun applyKeyboardSettings() {
        try {
            // Apply number row setting
            showNumberRow = keyboardSettings.numberRow
            
            // Apply font size settings (multiplier as percentage)
            val fontMultiplier = (keyboardSettings.portraitFontSize / 100.0).toFloat()
            
            // Apply keyboard dimensions and spacing
            keyboardView?.let { view ->
                // Apply one-handed mode if enabled
                if (keyboardSettings.oneHandedMode) {
                    val layoutParams = view.layoutParams
                    if (layoutParams != null) {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val newWidth = (screenWidth * (keyboardSettings.oneHandedModeWidth / 100.0)).toInt()
                        layoutParams.width = newWidth
                        view.layoutParams = layoutParams
                    }
                }
                
                // Apply spacing settings via keyboard padding
                val horizontalPadding = (keyboardSettings.horizontalKeySpacing * resources.displayMetrics.density).toInt()
                val verticalPadding = (keyboardSettings.verticalKeySpacing * resources.displayMetrics.density).toInt()
                view.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                
                // Invalidate to redraw with new settings
                view.invalidate()
            }
            
            // Apply long press delay
            // Note: This would typically be applied to the KeyboardView's long press detection
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying keyboard settings", e)
        }
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        
        // Apply CleverType config on keyboard activation
        applyConfig()
        
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
        
        // Reset current word
        currentWord = ""
        
        // CRITICAL FIX: Ensure dictionaries are loaded before showing suggestions
        if (ensureEngineReady()) {
            val currentLang = currentLanguage
            if (!autocorrectEngine.isLanguageLoaded(currentLang)) {
                Log.w(TAG, "⚠️ Dictionary for $currentLang not loaded yet, deferring suggestions")
                coroutineScope.launch {
                    // Wait up to 1 second for dictionary to load
                    var retries = 0
                    while (!autocorrectEngine.isLanguageLoaded(currentLang) && retries < 10) {
                        delay(100)
                        retries++
                    }
                    withContext(Dispatchers.Main) {
                        if (autocorrectEngine.isLanguageLoaded(currentLang)) {
                            Log.d(TAG, "✅ Dictionary loaded for $currentLang, showing suggestions")
                            updateAISuggestions()
                        } else {
                            Log.e(TAG, "❌ Dictionary load timeout for $currentLang after ${retries * 100}ms")
                        }
                    }
                }
                return // Exit early, suggestions will appear when ready
            }
        }
        
        // Dictionary is ready, show suggestions immediately
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
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(settingsReceiver)
            Log.d(TAG, "Broadcast receiver unregistered")
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered or already unregistered
            Log.d(TAG, "Receiver already unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        
        // Flush user dictionary before closing
        if (::userDictionaryManager.isInitialized) {
            try {
                userDictionaryManager.flush()
                Log.d(TAG, "✅ User dictionary flushed on destroy")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error flushing user dictionary", e)
            }
        }
        
        // Clean up AI bridge
        aiBridge.destroy()
        
        coroutineScope.cancel()
        
        // Stop settings polling
        stopSettingsPolling()
        
        // Receiver already unregistered above - removed duplicate
        
        // Cleanup theme manager
        try {
            themeManager.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up theme manager", e)
        }
        
        // Cleanup clipboard history manager
        try {
            clipboardHistoryManager.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up clipboard history manager", e)
        }
        
        // Stop periodic user dictionary sync
        syncRunnable?.let { runnable ->
            syncHandler?.removeCallbacks(runnable)
        }
        syncHandler = null
        syncRunnable = null
        
        // Clean up advanced keyboard resources
        longPressHandler.removeCallbacks(longPressRunnable)
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
                Log.d(TAG, "Keyboard layout reloaded - NumberRow: $showNumberRow, Language: $currentLanguage")
            }
            
            // Recreate toolbar to reflect custom prompt changes
            recreateToolbar()
            
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
                        val palette = themeManager.getCurrentPalette()
                        setTextColor(palette.specialAccent)
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
    
    /**
     * Settings polling - DISABLED by default (BroadcastReceiver is authoritative)
     * Only runs in DEBUG builds as a safety net with 15s interval to reduce I/O cost
     */
    private fun startSettingsPolling() {
        // Only enable polling in debug builds as a fallback mechanism
        // BuildConfig.DEBUG check disabled - always disable polling (BroadcastReceiver is authoritative)
        Log.d(TAG, "Settings polling disabled (using BroadcastReceiver as authoritative source)")
        return
        
        /* Commented out DEBUG-only polling - uncomment if needed for debugging
        if (!BuildConfig.DEBUG) {
            Log.d(TAG, "Settings polling disabled in release build")
            return
        }*/
        
        if (settingsPoller != null) return
        
        settingsPoller = object : Runnable {
            override fun run() {
                try {
                    checkAndUpdateSettings()
                    // Poll every 15 seconds (was 2s - reduced to minimize I/O churn)
                    settingsPoller?.let { mainHandler.postDelayed(it, 15000) }
                } catch (e: Exception) {
                    // Ignore polling errors
                }
            }
        }
        
        // Start polling after 15 seconds (delayed first check)
        settingsPoller?.let { mainHandler.postDelayed(it, 15000) }
        Log.d(TAG, "⚠️ Settings polling enabled (DEBUG mode only, 15s interval)")
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
                        currentLongPressKey = -1
                        longPressHandler.removeCallbacks(longPressRunnable)
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
                    // Save current mode before showing emoji
                    previousKeyboardMode = currentKeyboardMode
                    currentKeyboardMode = KeyboardMode.EMOJI
                    
                    // Use new XML-based emoji panel with fixed toolbar
                    if (emojiPanelView == null && emojiPanelController != null) {
                        emojiPanelView = emojiPanelController!!.inflate(container)
                    }
                    
                    emojiPanelView?.let { panel ->
                        // Use the height already set in inflate() method
                        // (calculated to match letters keyboard height)
                        container.addView(panel)
                        
                        // Apply theme to emoji panel for consistent look
                        emojiPanelController?.applyTheme()
                        
                        Log.d(TAG, "😊 Showing emoji panel (saved previous mode: $previousKeyboardMode)")
                    }
                    
                    // Keep suggestion bar visible for emoji search
                    topContainer?.visibility = View.VISIBLE
                } else {
                    // Cleanup emoji panel popups before hiding
                    gboardEmojiPanel?.dismissAllPopups()
                    
                    // Return to previous keyboard mode
                    currentKeyboardMode = previousKeyboardMode
                    
                    // Show keyboard
                    keyboardView?.let { kv ->
                        container.addView(kv)
                        Log.d(TAG, "🔤 Returning to keyboard mode: $currentKeyboardMode")
                    }
                    
                    // Show suggestion bar when keyboard is visible
                    topContainer?.visibility = View.VISIBLE
                }
                
                // Update emoji key visual state
                keyboardView?.setEmojiKeyActive(isEmojiPanelVisible)
                
                // Request layout update
                container.requestLayout()
            }
            
            Log.d(TAG, "Emoji panel toggled: visible=$isEmojiPanelVisible, mode=$currentKeyboardMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling comprehensive emoji panel", e)
        }
    }
    
    /**
     * Enhanced context-aware enter key handler (Gboard-style)
     */
    private fun handleEnterKey(ic: InputConnection) {
        try {
            // Determine enter key behavior based on input context
            val inputType = currentInputEditorInfo?.inputType ?: 0
            val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
            
            // Check for special IME actions with enhanced feedback
            when (imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    // Search action with haptic feedback
                    ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                    performAdvancedHapticFeedback(Keyboard.KEYCODE_DONE)
                    Log.d(TAG, "Enter key: Search action performed")
                    return
                }
                EditorInfo.IME_ACTION_GO -> {
                    // Go action (URLs, forms)
                    ic.performEditorAction(EditorInfo.IME_ACTION_GO)
                    performAdvancedHapticFeedback(Keyboard.KEYCODE_DONE)
                    Log.d(TAG, "Enter key: Go action performed")
                    return
                }
                EditorInfo.IME_ACTION_SEND -> {
                    // Send action (messaging apps)
                    ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
                    performAdvancedHapticFeedback(Keyboard.KEYCODE_DONE)
                    Log.d(TAG, "Enter key: Send action performed")
                    return
                }
                EditorInfo.IME_ACTION_NEXT -> {
                    // Next field action
                    ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
                    performAdvancedHapticFeedback(Keyboard.KEYCODE_DONE)
                    Log.d(TAG, "Enter key: Next field action performed")
                    return
                }
                EditorInfo.IME_ACTION_DONE -> {
                    // Done action (close keyboard)
                    ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                    performAdvancedHapticFeedback(Keyboard.KEYCODE_DONE)
                    Log.d(TAG, "Enter key: Done action performed")
                    return
                }
            }
            
            // Check if multiline is supported
            val isMultiline = (inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            
            if (isMultiline) {
                // Insert newline for multiline text fields
                ic.commitText("\n", 1)
                Log.d(TAG, "Enter key: Newline inserted (multiline)")
            } else {
                // For single-line fields, try to perform default action
                try {
                    ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                    performAdvancedHapticFeedback(Keyboard.KEYCODE_DONE)
                    Log.d(TAG, "Enter key: Default done action")
                } catch (e: Exception) {
                    ic.commitText("\n", 1)
                    Log.d(TAG, "Enter key: Fallback newline")
                }
            }
            
            // Clear current word after enter
            currentWord = ""
            
            // Enhanced auto-capitalization after enter
            if (::capsShiftManager.isInitialized) {
                capsShiftManager.handleEnterPress(ic, inputType)
            }
            
            // Update suggestions
            if (aiSuggestionsEnabled) {
                updateAISuggestions()
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
     * Run AI feature using AdvancedAIService
     */
    private fun runAIFeature(feature: AdvancedAIService.ProcessingFeature) {
        try {
            // Get selected text or full text
            val text = getSelectedTextOrFull()
            if (text.isEmpty()) {
                Toast.makeText(this, "No text to process", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Running AI feature: ${feature.displayName} on text: ${text.take(50)}...")
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val result = advancedAIService.processText(text, feature)
                    withContext(Dispatchers.Main) {
                        if (result.success) {
                            replaceTextWithResult(text, result.text)
                            val cacheIndicator = if (result.fromCache) " (cached)" else ""
                            Toast.makeText(this@AIKeyboardService, 
                                "✓ ${feature.displayName}$cacheIndicator", 
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AIKeyboardService, 
                                "Error: ${result.error}", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AIKeyboardService, 
                            "Error: ${e.message}", 
                            Toast.LENGTH_LONG).show()
                    }
                    Log.e(TAG, "Error running AI feature", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing AI feature execution", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Run tone adjustment using AdvancedAIService
     */
    private fun runTone(tone: AdvancedAIService.ToneType) {
        try {
            // Get selected text or full text
            val text = getSelectedTextOrFull()
            if (text.isEmpty()) {
                Toast.makeText(this, "No text to adjust tone", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Running tone adjustment: ${tone.displayName} on text: ${text.take(50)}...")
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val result = advancedAIService.adjustTone(text, tone)
                    withContext(Dispatchers.Main) {
                        if (result.success) {
                            replaceTextWithResult(text, result.text)
                            val cacheIndicator = if (result.fromCache) " (cached)" else ""
                            Toast.makeText(this@AIKeyboardService, 
                                "✓ ${tone.displayName} tone$cacheIndicator", 
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AIKeyboardService, 
                                "Error: ${result.error}", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AIKeyboardService, 
                            "Error: ${e.message}", 
                            Toast.LENGTH_LONG).show()
                    }
                    Log.e(TAG, "Error running tone adjustment", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing tone adjustment", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Run custom prompt using AdvancedAIService
     */
    private fun runCustomPromptWithAdvancedAI(instruction: String, title: String) {
        try {
            // Get selected text or full text
            val text = getSelectedTextOrFull()
            if (text.isEmpty()) {
                Toast.makeText(this, "No text to process", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Running custom prompt: $title on text: ${text.take(50)}...")
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Use the new processWithCustomPrompt method
                    val result = advancedAIService.processWithCustomPrompt(text, instruction, title)
                    withContext(Dispatchers.Main) {
                        if (result.success) {
                            replaceTextWithResult(text, result.text)
                            val cacheIndicator = if (result.fromCache) " (cached)" else ""
                            Toast.makeText(this@AIKeyboardService, 
                                "✓ $title$cacheIndicator", 
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AIKeyboardService, 
                                "Error: ${result.error}", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AIKeyboardService, 
                            "Error: ${e.message}", 
                            Toast.LENGTH_LONG).show()
                    }
                    Log.e(TAG, "Error running custom prompt", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing custom prompt execution", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Get selected text or full text from input field
     */
    private fun getSelectedTextOrFull(): String {
        try {
            val ic = currentInputConnection ?: return ""
            
            // Try to get selected text first
            val selectedText = ic.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                return selectedText.toString()
            }
            
            // If no selection, get text before cursor (up to 1000 chars)
            val extractedText = ic.getExtractedText(ExtractedTextRequest(), 0)
            if (extractedText != null && !extractedText.text.isNullOrEmpty()) {
                return extractedText.text.toString()
            }
            
            // Fallback: get text before cursor
            val beforeCursor = ic.getTextBeforeCursor(1000, 0)
            return beforeCursor?.toString() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting text", e)
            return ""
        }
    }
    
    /**
     * Replace text with AI result
     */
    private fun replaceTextWithResult(originalText: String, newText: String) {
        try {
            val ic = currentInputConnection ?: return
            
            // Check if there was a selection
            val selectedText = ic.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                // Replace selected text
                ic.commitText(newText, 1)
                Log.d(TAG, "Replaced selected text with AI result")
            } else {
                // Replace all text before cursor
                val extractedText = ic.getExtractedText(ExtractedTextRequest(), 0)
                if (extractedText != null) {
                    // Delete old text and insert new text
                    ic.deleteSurroundingText(originalText.length, 0)
                    ic.commitText(newText, 1)
                    Log.d(TAG, "Replaced full text with AI result")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing text with result", e)
        }
    }
    
    /**
     * Open AI Panel (CleverType style)
     */
    private fun openAIPanel(type: AIPanelType) {
        try {
            Log.d(TAG, "Opening AI Panel - Type: $type")
            
            // Get current text
            currentAIOriginalText = getSelectedTextOrFull()
            if (currentAIOriginalText.isEmpty()) {
                Toast.makeText(this, "No text to process", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Hide keyboard and suggestion bar, show AI panel
            keyboardView?.visibility = View.GONE
            emojiPanelView?.visibility = View.GONE
            mediaPanelManager?.let { keyboardContainer?.removeView(it) }
            
            // Hide suggestion bar when AI panel is open
            suggestionContainer?.visibility = View.GONE
            
            // Add AI panel if not already in container
            if (aiPanel?.parent == null) {
                keyboardContainer?.addView(aiPanel)
            }
            
            aiPanel?.visibility = View.VISIBLE
            isAIPanelVisible = true
            
            // Populate chips based on type
            populateAIChips(type)
            
            // Reset result view
            aiResultView?.text = "Processing text:\n\n\"${currentAIOriginalText.take(100)}${if (currentAIOriginalText.length > 100) "..." else ""}\"\n\nSelect an AI feature above to start."
            aiReplaceButton?.isEnabled = false
            aiReplaceButton?.alpha = 0.5f
            
            Log.d(TAG, "✅ AI Panel opened successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening AI panel", e)
            Toast.makeText(this, "Error opening AI panel", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Close AI Panel and return to keyboard
     */
    private fun closeAIPanel() {
        try {
            Log.d(TAG, "Closing AI Panel")
            
            aiPanel?.visibility = View.GONE
            keyboardView?.visibility = View.VISIBLE
            isAIPanelVisible = false
            
            // Show suggestion bar again
            suggestionContainer?.visibility = View.VISIBLE
            
            // Clear state
            currentAIOriginalText = ""
            
            Log.d(TAG, "✅ AI Panel closed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error closing AI panel", e)
        }
    }
    
    /**
     * Populate chips based on AI panel type
     */
    private fun populateAIChips(type: AIPanelType) {
        try {
            aiChipContainer?.removeAllViews()
            
            val options = when (type) {
                AIPanelType.GRAMMAR -> listOf(
                    "Rephrase" to AdvancedAIService.ProcessingFeature.SIMPLIFY,
                    "Fix Grammar" to AdvancedAIService.ProcessingFeature.GRAMMAR_FIX,
                    "Expand" to AdvancedAIService.ProcessingFeature.EXPAND,
                    "Shorten" to AdvancedAIService.ProcessingFeature.SHORTEN,
                    "Bullet Points" to AdvancedAIService.ProcessingFeature.MAKE_BULLET_POINTS
                )
                AIPanelType.TONE -> listOf(
                    "Formal" to AdvancedAIService.ToneType.FORMAL,
                    "Casual" to AdvancedAIService.ToneType.CASUAL,
                    "Funny" to AdvancedAIService.ToneType.FUNNY,
                    "Confident" to AdvancedAIService.ToneType.CONFIDENT,
                    "Polite" to AdvancedAIService.ToneType.POLITE,
                    "Empathetic" to AdvancedAIService.ToneType.EMPATHETIC
                )
                AIPanelType.ASSISTANT -> {
                    val allCustomPrompts = customGrammarPrompts + customTonePrompts + customAssistantPrompts
                    allCustomPrompts.map { it.title to it }
                }
            }
            
            options.forEach { (label, data) ->
                val chip = createAIChip(label) {
                    runAIOption(type, label, data)
                }
                aiChipContainer?.addView(chip)
            }
            
            Log.d(TAG, "Populated ${options.size} chips for $type")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error populating chips", e)
        }
    }
    
    /**
     * Create a chip button for AI panel
     */
    private fun createAIChip(label: String, onClick: () -> Unit): android.widget.Button {
        val palette = themeManager.getCurrentPalette()
        
        return android.widget.Button(this).apply {
            text = label
            textSize = 14f
            setTextColor(palette.keyText) // Use themed text color
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(40)
            ).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)
            }
            
            // Rounded chip background with theme colors
            val cornerRadius = dpToPx(20).toFloat()
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(palette.keyBg) // Use themed key background
                setCornerRadius(cornerRadius)
                setStroke(dpToPx(2), palette.keyBorder) // Use themed border
            }
            
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            
            setOnClickListener { onClick() }
        }
    }
    
    /**
     * Run AI option based on chip selection
     */
    private fun runAIOption(type: AIPanelType, label: String, data: Any) {
        try {
            Log.d(TAG, "Running AI option: $label (Type: $type)")
            
            // Show loading state
            aiResultView?.text = "⏳ Processing with AI...\n\n$label is analyzing your text..."
            aiReplaceButton?.isEnabled = false
            aiReplaceButton?.alpha = 0.5f
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val result = when (type) {
                        AIPanelType.GRAMMAR -> {
                            val feature = data as AdvancedAIService.ProcessingFeature
                            advancedAIService.processText(currentAIOriginalText, feature)
                        }
                        AIPanelType.TONE -> {
                            val tone = data as AdvancedAIService.ToneType
                            advancedAIService.adjustTone(currentAIOriginalText, tone)
                        }
                        AIPanelType.ASSISTANT -> {
                            val prompt = data as CustomPrompt
                            advancedAIService.processWithCustomPrompt(
                                currentAIOriginalText,
                                prompt.instruction,
                                prompt.title
                            )
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (result.success) {
                            aiResultView?.text = result.text
                            aiReplaceButton?.isEnabled = true
                            aiReplaceButton?.alpha = 1.0f
                            
                            val cacheIndicator = if (result.fromCache) " (cached)" else ""
                            Toast.makeText(
                                this@AIKeyboardService,
                                "✓ $label$cacheIndicator - ${result.processingTimeMs}ms",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            Log.d(TAG, "✅ AI processing complete: $label (${result.processingTimeMs}ms, cached=${result.fromCache})")
                        } else {
                            aiResultView?.text = "❌ Error: ${result.error}\n\nPlease try again or select a different option."
                            Toast.makeText(
                                this@AIKeyboardService,
                                "Error: ${result.error}",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            Log.e(TAG, "AI processing failed: ${result.error}")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        aiResultView?.text = "❌ Unexpected error:\n\n${e.message}\n\nPlease try again."
                        Toast.makeText(
                            this@AIKeyboardService,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Log.e(TAG, "Error in AI processing", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running AI option", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Replace text from AI panel
     */
    private fun replaceTextFromAIPanel() {
        try {
            val newText = aiResultView?.text?.toString() ?: return
            
            if (newText.startsWith("⏳") || newText.startsWith("❌") || newText.startsWith("Select")) {
                Toast.makeText(this, "No valid AI result to replace", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Replacing text from AI panel")
            replaceTextWithResult(currentAIOriginalText, newText)
            
            Toast.makeText(this, "✓ Text replaced", Toast.LENGTH_SHORT).show()
            closeAIPanel()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing text from AI panel", e)
            Toast.makeText(this, "Error replacing text", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Recreate toolbar with updated custom prompts
     */
    private fun recreateToolbar() {
        try {
            // Get the main layout through the toolbar's parent
            val mainLayout = cleverTypeToolbar?.parent as? LinearLayout
            if (mainLayout == null) {
                Log.w(TAG, "Cannot recreate toolbar - main layout not found")
                return
            }
            
            // Remove old toolbar if exists
            cleverTypeToolbar?.let { oldToolbar ->
                mainLayout.removeView(oldToolbar)
            }
            
            // Create new toolbar with updated prompts
            cleverTypeToolbar = createCleverTypeToolbar()
            
            // Add new toolbar at the top (index 0)
            mainLayout.addView(cleverTypeToolbar, 0)
            
            Log.d(TAG, "Toolbar recreated with updated custom prompts")
        } catch (e: Exception) {
            Log.e(TAG, "Error recreating toolbar", e)
        }
    }
    
    /**
     * Create AI Features toolbar with Tone, Rewrite, Emoji, GIF, Clipboard, Settings buttons
     * Wrapped in HorizontalScrollView for overflow support
     */
    private fun createCleverTypeToolbar(): LinearLayout {
        // Create outer wrapper container
        val toolbarWrapper = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.toolbar_height)
            )
            val palette = themeManager.getCurrentPalette()
            setBackgroundColor(palette.toolbarBg)
        }
        
        // Create scrollable container for right-side buttons
        val scrollView = android.widget.HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            // Minimal padding for cleaner look (4dp horizontal, 6dp vertical)
            setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6))
            gravity = Gravity.CENTER_VERTICAL // Center icons vertically
        }
        
        // LEFT SIDE ICONS
        
        // Settings button (⚙️ settings)
        val settingsButton = createToolbarIconButton(
            icon = "⚙️",
            description = "Settings",
            onClick = { handleSettingsAccess() }
        )
        
        // Voice button (🎤 microphone)
        val voiceButton = createToolbarIconButton(
            icon = "🎤",
            description = "Voice",
            onClick = { handleVoiceInput() }
        )
        
        // Emoji button (😊 emoji_emotions)
        val emojiButton = createToolbarIconButton(
            icon = "😊",
            description = "Emoji",
            onClick = { toggleEmojiPanel() }
        )
        
        // SPACER - pushes right icons to the end
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f // Weight = 1 to take all available space
            )
        }
        
        // RIGHT SIDE ICONS
        
        // ChatGPT button
        val chatGPTButton = createToolbarIconButton(
            icon = "💬",
            description = "ChatGPT",
            onClick = { handleClipboardAccess() }
        )
        
        // Grammar/Rewrite button (✍️ edit_note)
        val grammarButton = createToolbarIconButton(
            icon = "✍️",
            description = "Grammar",
            onClick = { handleRewriteText() }
        )
        
        // AI Tone button (✨ auto_awesome)
        val aiToneButton = createToolbarIconButton(
            icon = "✨",
            description = "AI Tone",
            onClick = { handleToneAdjustment() }
        )
        
        // Add LEFT side buttons directly to wrapper (fixed position)
        toolbarWrapper.addView(settingsButton)
        toolbarWrapper.addView(voiceButton)
        toolbarWrapper.addView(emojiButton)
        
        // Add RIGHT side buttons to scrollable toolbar
        toolbar.addView(chatGPTButton)
        
        // Add built-in Grammar button if enabled
        if (builtInActionsEnabled["grammar"] == true) {
            toolbar.addView(grammarButton)
        }
        
        // Add built-in AI Tone button if enabled
        if (builtInActionsEnabled["formal"] == true || builtInActionsEnabled["concise"] == true || builtInActionsEnabled["expand"] == true) {
            toolbar.addView(aiToneButton)
        }
        
        // ═══════════════════════════════════════════════════════════
        // ✨ AI PANEL BUTTONS - CleverType Style
        // ═══════════════════════════════════════════════════════════
        
        // Grammar Panel Button - opens AI panel with grammar options
        val grammarPanelBtn = createAdvancedAIButton("✅", "Grammar") {
            openAIPanel(AIPanelType.GRAMMAR)
        }
        toolbar.addView(grammarPanelBtn)
        
        // Tone Panel Button - opens AI panel with tone options
        val tonePanelBtn = createAdvancedAIButton("🎨", "Tone") {
            openAIPanel(AIPanelType.TONE)
        }
        toolbar.addView(tonePanelBtn)
        
        // Assistant Panel Button - opens AI panel with custom prompts
        if (customGrammarPrompts.isNotEmpty() || customTonePrompts.isNotEmpty() || customAssistantPrompts.isNotEmpty()) {
            val assistantPanelBtn = createAdvancedAIButton("🤖", "Assistant") {
                openAIPanel(AIPanelType.ASSISTANT)
            }
            toolbar.addView(assistantPanelBtn)
        }
        
        // Add toolbar to scrollview, then scrollview to wrapper
        scrollView.addView(toolbar)
        toolbarWrapper.addView(scrollView)
        
        val allCustomPrompts = customGrammarPrompts + customTonePrompts + customAssistantPrompts
        val totalCustom = allCustomPrompts.size
        val hasAssistant = if (totalCustom > 0) 1 else 0
        val totalAIPanelButtons = 2 + hasAssistant // Grammar + Tone + (optional) Assistant
        
        Log.d(TAG, "✨ CleverType AI Toolbar: [Settings | Voice | Emoji] <scroll> [ChatGPT | Grammar | Tone] + $totalAIPanelButtons AI Panel buttons ($totalCustom custom prompts available)")
        return toolbarWrapper
    }
    
    /**
     * Create toolbar icon - SIMPLIFIED: Just PNG image, no button background
     */
    private fun createToolbarIconButton(
        icon: String,
        description: String,
        onClick: () -> Unit,
        useAccentColor: Boolean = false  // Ignored - PNGs have their own colors
    ): ImageView {
        // Optimal icon size for clean UI (24dp - compact but clear)
        val iconSize = dpToPx(24)
        
        // Map icon emoji to PNG filename (note: folder has trailing space)
        val iconFileName = when (icon) {
            "⚙️" -> "setting.png"         // Settings
            "🎤" -> "voice_input.png"     // Voice/Microphone
            "😊" -> "emoji.png"           // Emoji
            "💬" -> "chatGPT.png"         // ChatGPT
            "✍️" -> "Grammer_correct.png" // Grammar
            "✨" -> "AI_tone.png"          // AI Tone
            "📋" -> "clipboard.png"       // Clipboard (if needed)
            else -> "chatGPT.png"         // Default fallback
        }
        
        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                // Better spacing: horizontal 10dp, vertical 8dp
                setMargins(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = description
            tag = "toolbar_icon_$description"
            
            // NO BACKGROUND - completely transparent
            setBackgroundColor(Color.TRANSPARENT)
            elevation = 0f
            
            // Load PNG from assets (note: folder name has trailing space)
            try {
                val inputStream = assets.open("toolbar_icons /$iconFileName")
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                setImageBitmap(bitmap)
                inputStream.close()
                
                // NO COLOR FILTER - use PNG as-is with original colors
                clearColorFilter()
                imageTintList = null
                
                Log.d(TAG, "✓ Loaded toolbar icon: $iconFileName")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to load toolbar icon: $iconFileName", e)
                // Fallback: Create colored circle as placeholder
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor("#FF6B35"))
                    setSize(iconSize / 4, iconSize / 4)
                }
                setImageDrawable(drawable)
            }
            
            // Simple click with visual feedback (scale animation)
            isClickable = true
            isFocusable = true
            setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).start()
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        onClick()
                    }
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                true
            }
        }
        
        return iconView
    }
    
    /**
     * Create Advanced AI button with icon and label for AdvancedAIService features
     */
    private fun createAdvancedAIButton(
        icon: String,
        label: String,
        onClick: () -> Unit
    ): LinearLayout {
        val palette = themeManager.getCurrentPalette()
        
        // Container for icon + label
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dpToPx(6), 0, dpToPx(6), 0)
            }
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
        }
        
        // Icon (emoji)
        val iconView = TextView(this).apply {
            text = icon
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(2))
        }
        
        // Label text
        val labelView = TextView(this).apply {
            text = label
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(palette.suggestionText) // Use suggestion text color for consistency
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        
        container.addView(iconView)
        container.addView(labelView)
        
        // Touch feedback with scale animation
        container.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                }
                android.view.MotionEvent.ACTION_UP -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    onClick()
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
            }
            true
        }
        
        return container
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
     * Handle custom prompt execution
     */
    private fun handleCustomPrompt(prompt: CustomPrompt) {
        val ic = currentInputConnection ?: return
        val allText = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString() ?: ""
        
        if (allText.isEmpty()) {
            Toast.makeText(this, "📝 Type some text first for ${prompt.title}", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Custom prompt requested: ${prompt.title}")
        
        // Process the prompt by replacing {text} placeholder with actual text
        val instruction = prompt.instruction.replace("{text}", allText)
        
        // Trigger unified AI rewrite
        triggerAIRewrite(instruction, prompt.title, allText)
    }
    
    /**
     * Unified AI Rewrite Method - works for all prompts (built-in + custom)
     * Integrates with OpenAI or any AI backend
     */
    private fun triggerAIRewrite(instruction: String, actionName: String, originalText: String) {
        coroutineScope.launch {
            try {
                // Show loading indicator
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AIKeyboardService, "⏳ $actionName...", Toast.LENGTH_SHORT).show()
                }
                
                // Call AI service (OpenAI, Gemini, or custom backend)
                val result = withContext(Dispatchers.IO) {
                    callAIService(instruction)
                }
                
                // Insert rewritten text
                withContext(Dispatchers.Main) {
                    val ic = currentInputConnection
                    if (ic != null && result.isNotEmpty()) {
                        // Delete original text
                        ic.deleteSurroundingText(originalText.length, 0)
                        // Insert AI-generated text
                        ic.commitText(result.trim(), 1)
                        Toast.makeText(this@AIKeyboardService, "✅ $actionName complete", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "AI rewrite complete: $actionName")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in AI rewrite: $actionName", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AIKeyboardService, "❌ $actionName failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Call AI Service (OpenAI, Gemini, or custom backend)
     * Replace this with your actual AI integration
     */
    private suspend fun callAIService(prompt: String): String {
        // OPTION 1: OpenAI Integration
        return try {
            callOpenAI(prompt)
        } catch (e: Exception) {
            Log.w(TAG, "OpenAI failed, falling back to mock", e)
            // Fallback to mock response for testing
            getMockAIResponse(prompt)
        }
    }
    
    /**
     * OpenAI API Integration
     * Add your OpenAI API key in build.gradle or secrets
     */
    private suspend fun callOpenAI(prompt: String): String {
        // TODO: Add your OpenAI API key
        val apiKey = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            .getString("flutter.openai_api_key", "") ?: ""
        
        if (apiKey.isEmpty()) {
            Log.w(TAG, "OpenAI API key not set")
            throw Exception("OpenAI API key not configured")
        }
        
        // Build request body
        val requestBody = org.json.JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 500)
        }
        
        // Make HTTP request with modern OkHttp syntax
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBodyObj = requestBody.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyObj)
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        
        if (!response.isSuccessful) {
            throw Exception("OpenAI API error: ${response.code} - $responseBody")
        }
        
        // Parse response
        val jsonResponse = org.json.JSONObject(responseBody)
        val choices = jsonResponse.getJSONArray("choices")
        val message = choices.getJSONObject(0).getJSONObject("message")
        val content = message.getString("content")
        
        return content
    }
    
    /**
     * Mock AI response for testing (when OpenAI is not configured)
     */
    private fun getMockAIResponse(prompt: String): String {
        // Simple mock that just adds a prefix to show it's working
        return when {
            prompt.contains("grammar", ignoreCase = true) -> 
                "✓ Grammar checked (Mock mode - configure OpenAI for real results)"
            prompt.contains("formal", ignoreCase = true) || prompt.contains("professional", ignoreCase = true) -> 
                "Respectfully formatted text (Mock mode - configure OpenAI for real results)"
            prompt.contains("friendly", ignoreCase = true) -> 
                "Hey! This is in friendly tone 😊 (Mock mode - configure OpenAI for real results)"
            prompt.contains("concise", ignoreCase = true) || prompt.contains("summarize", ignoreCase = true) -> 
                "Brief summary here (Mock mode - configure OpenAI for real results)"
            prompt.contains("expand", ignoreCase = true) -> 
                "Expanded version with more details and context (Mock mode - configure OpenAI for real results)"
            else -> 
                "AI processed result (Mock mode - configure OpenAI for real results)"
        }
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
                
                // Restore a  ppropriate input mode
                currentInputMode = INPUT_MODE_NORMAL
                kv.showNormalLayout()
                
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
     * Show Tone Suggestions UI (similar to grammar correction)
     */
    private fun showToneSuggestionsUI(tone: CleverTypeAIService.ToneType, originalText: String) {
        keyboardContainer?.let { container ->
            container.removeAllViews()
            
            // Create tone suggestions UI
            val toneUI = LinearLayout(this).apply {
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
                text = "${tone.emoji} Choose ${tone.displayName} Variation"
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
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    hideReplacementUI()
                    restoreKeyboard()
                }
            }
            
            header.addView(title)
            header.addView(closeBtn)
            toneUI.addView(header)
            
            // Content
            val scrollView = android.widget.ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(280)
                )
            }
            
            val contentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(16))
            }
            
            // Progress text
            val progressText = TextView(this).apply {
                text = "✨ Generating ${tone.displayName} variations..."
                textSize = 16f
                setTextColor(Color.parseColor("#5f6368"))
                gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))
            }
            
            contentLayout.addView(progressText)
            scrollView.addView(contentLayout)
            toneUI.addView(scrollView)
            container.addView(toneUI)
            
            // Start tone adjustment
            performToneAdjustment(contentLayout, progressText, originalText, tone)
        }
    }
    
    /**
     * Perform tone adjustment and show results
     */
    private fun performToneAdjustment(contentLayout: LinearLayout, progressText: TextView, originalText: String, tone: CleverTypeAIService.ToneType) {
        if (!::cleverTypeService.isInitialized) {
            progressText.text = "❌ AI service not available"
            return
        }
        
        coroutineScope.launch {
            try {
                val result = cleverTypeService.adjustTone(originalText, tone)
                
                withContext(Dispatchers.Main) {
                    // Remove progress text
                    contentLayout.removeView(progressText)
                    
                    // Store variations for later use
                    currentToneVariations = result.variations
                    
                    // Show variations as buttons
                    showToneVariationButtons(contentLayout, result.variations, originalText, tone)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in tone adjustment", e)
                withContext(Dispatchers.Main) {
                    progressText.text = "❌ Error generating variations"
                    Handler(Looper.getMainLooper()).postDelayed({
                        hideReplacementUI()
                        restoreKeyboard()
                    }, 2000)
                }
            }
        }
    }
    
    /**
     * Show tone variation buttons
     */
    private fun showToneVariationButtons(contentLayout: LinearLayout, variations: List<String>, originalText: String, tone: CleverTypeAIService.ToneType) {
        if (variations.isEmpty()) {
            val noVariationsText = TextView(this).apply {
                text = "❌ No variations generated"
                textSize = 16f
                setTextColor(Color.parseColor("#d93025"))
                gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))
            }
            contentLayout.addView(noVariationsText)
            return
        }
        
        // Add description
        val descText = TextView(this).apply {
            text = "Tap a variation to use it:"
            textSize = 14f
            setTextColor(Color.parseColor("#5f6368"))
            setPadding(0, 0, 0, dpToPx(8))
        }
        contentLayout.addView(descText)
        
        // Show each variation as a button
        variations.forEachIndexed { index, variation ->
            // Create card for variation
            val variationCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dpToPx(8), 0, dpToPx(8))
                }
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                setBackgroundColor(Color.parseColor("#f8f9fa"))
                elevation = 2f
                isClickable = true
                isFocusable = true
                
                // Add click effect
                setOnClickListener {
                    replaceText(variation)
                    hideReplacementUI()
                    restoreKeyboard()
                    Toast.makeText(this@AIKeyboardService, "✨ Tone adjusted to ${tone.displayName}!", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Variation number and emoji
            val variationHeader = TextView(this).apply {
                text = "${tone.emoji} Variation ${index + 1}"
                textSize = 12f
                setTextColor(Color.parseColor("#1a73e8"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, dpToPx(4))
            }
            
            // Variation text
            val variationText = TextView(this).apply {
                text = variation
                textSize = 15f
                setTextColor(Color.parseColor("#202124"))
                setLineSpacing(dpToPx(2).toFloat(), 1f)
            }
            
            variationCard.addView(variationHeader)
            variationCard.addView(variationText)
            contentLayout.addView(variationCard)
        }
        
        // Add divider
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                setMargins(0, dpToPx(16), 0, dpToPx(8))
            }
            setBackgroundColor(Color.parseColor("#e0e0e0"))
        }
        contentLayout.addView(divider)
        
        // Keep original button
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
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
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
        
        // Store the original text
        currentToneReplacementText = allText
        
        // Show tone suggestions UI with loading state
        showToneSuggestionsUI(tone, allText)
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
        
        try {
            // Switch to clipboard input mode
            currentInputMode = INPUT_MODE_CLIPBOARD
            showClipboardKeyboard()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing clipboard keyboard", e)
            Toast.makeText(this, "Error accessing clipboard", Toast.LENGTH_SHORT).show()
        }
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
            "current_language" to currentLanguage
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
            if (autocorrectEngine.getCandidates(swipeLetters, currentLanguage, 1).isNotEmpty()) {
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
                    val freq = multilingualDictionary.getFrequency(currentLanguage, candidate.word)
                    val freqScore = kotlin.math.ln((freq + 1).toDouble())
                    
                    val bigramScore = if (prev1.isNotEmpty()) {
                        val bigramFreq = multilingualDictionary.getBigramFrequency(currentLanguage, prev1, candidate.word)
                        if (bigramFreq > 0) kotlin.math.ln(bigramFreq.toDouble() + 1) else -5.0
                    } else 0.0
                    
                    val trigramScore = if (prev2.isNotEmpty() && prev1.isNotEmpty()) {
                        val trigramFreq = 0 // Trigrams not implemented in unified engine yet
                        if (trigramFreq > 0) kotlin.math.ln(trigramFreq.toDouble() + 1) else -8.0
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
            val wordsToCheck = autocorrectEngine.getCandidates(swipeLetters.take(2), currentLanguage, 50)
            
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
            val autocorrectCandidates = autocorrectEngine.getCorrections(swipeLetters, currentLanguage, listOfNotNull(prev1, prev2).filter { it.isNotBlank() })
            
            // Convert UnifiedAutocorrectEngine suggestions to AutocorrectCandidate
            autocorrectCandidates.forEach { suggestion ->
                candidates.add(AutocorrectCandidate(
                    word = suggestion.word,
                    score = suggestion.score,
                    confidence = suggestion.score,
                    type = if (suggestion.isCorrection) CandidateType.CORRECTION else CandidateType.ORIGINAL,
                    editDistance = if (suggestion.isCorrection) 1 else 0
                ))
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
                    CandidateSource.UNIFIED_ENGINE -> "🔧"
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
    
    /**
     * Paste a clipboard item to the current input
     */
    private fun pasteClipboardItem(item: ClipboardItem) {
        try {
            val ic = currentInputConnection
            if (ic != null) {
                ic.commitText(item.text, 1)
                Log.d(TAG, "Pasted clipboard item: ${item.getPreview()}")
                
                // Show confirmation
                Toast.makeText(this, "Pasted: ${item.getPreview()}", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "No input connection available for paste")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pasting clipboard item", e)
        }
    }
    
    /**
     * Toggle pin status of a clipboard item
     */
    private fun toggleClipboardItemPin(item: ClipboardItem) {
        try {
            val wasPinned = clipboardHistoryManager.togglePin(item.id)
            val message = if (wasPinned) "Pinned" else "Unpinned"
            Toast.makeText(this, "$message: ${item.getPreview()}", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Toggled pin for clipboard item: ${item.getPreview()} -> $wasPinned")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling clipboard item pin", e)
        }
    }
    
    /**
     * Delete a clipboard item
     */
    private fun deleteClipboardItem(item: ClipboardItem) {
        try {
            val deleted = clipboardHistoryManager.deleteItem(item.id)
            if (deleted) {
                Toast.makeText(this, "Deleted: ${item.getPreview()}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Deleted clipboard item: ${item.getPreview()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting clipboard item", e)
        }
    }
    
    /**
     * Update suggestions to include clipboard items
     */
    private fun updateSuggestionsWithClipboard() {
        try {
            // Get OTP items first (highest priority)
            val otpItems = clipboardHistoryManager.getOTPItems()
            val recentItem = clipboardHistoryManager.getMostRecentItem()
            
            if (otpItems.isNotEmpty() || recentItem != null) {
                mainHandler.post {
                    updateSuggestionUIWithClipboard(otpItems, recentItem)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating suggestions with clipboard", e)
        }
    }
    
    /**
     * Update suggestion UI to include clipboard items
     */
    private fun updateSuggestionUIWithClipboard(otpItems: List<ClipboardItem>, recentItem: ClipboardItem?) {
        suggestionContainer?.let { container ->
            try {
                // First suggestion slot: OTP if available, otherwise recent item
                if (container.childCount > 0) {
                    val firstSuggestion = container.getChildAt(0) as? TextView
                    val clipboardItem = otpItems.firstOrNull() ?: recentItem
                    
                    if (clipboardItem != null && firstSuggestion != null) {
                        val prefix = if (clipboardItem.isOTP()) "OTP: " else "Paste: "
                        firstSuggestion.text = "$prefix${clipboardItem.getPreview(20)}"
                        firstSuggestion.visibility = View.VISIBLE
                        
                        // Set click listener to paste the item
                        firstSuggestion.setOnClickListener {
                            pasteClipboardItem(clipboardItem)
                        }
                        
                        Log.d(TAG, "Added clipboard suggestion: ${clipboardItem.getPreview()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating suggestion UI with clipboard", e)
            }
        }
    }
    
    /**
     * Reload clipboard settings from SharedPreferences
     */
    private fun reloadEmojiSettings() {
        try {
            // Reload emoji panel settings (legacy)
            gboardEmojiPanel?.reloadEmojiSettings()
            
            // Reload settings in new controller (includes theme)
            emojiPanelController?.reloadEmojiSettings()
            
            Log.d(TAG, "Emoji settings reloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading emoji settings", e)
        }
    }
    
    private fun reloadClipboardSettings() {
        try {
            val prefs = getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)
            
            // Load settings
            val enabled = prefs.getBoolean("clipboard_enabled", true)
            val maxHistorySize = prefs.getInt("max_history_size", 20)
            val autoExpiryEnabled = prefs.getBoolean("auto_expiry_enabled", true)
            val expiryDurationMinutes = prefs.getLong("expiry_duration_minutes", 60L)
            
            // Update clipboard suggestion setting
            clipboardSuggestionEnabled = enabled
            
            // Update clipboard history manager settings
            clipboardHistoryManager.updateSettings(
                maxHistorySize = maxHistorySize,
                autoExpiryEnabled = autoExpiryEnabled,
                expiryDurationMinutes = expiryDurationMinutes
            )
            
            // Load templates
            val templatesJson = prefs.getString("template_items", null)
            if (templatesJson != null) {
                try {
                    val jsonArray = org.json.JSONArray(templatesJson)
                    val templates = mutableListOf<ClipboardItem>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val template = ClipboardItem.fromJson(jsonArray.getJSONObject(i))
                        templates.add(template)
                    }
                    
                    clipboardHistoryManager.updateTemplates(templates)
                    Log.d(TAG, "Loaded ${templates.size} clipboard templates")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing templates JSON", e)
                }
            }
            
            Log.d(TAG, "Clipboard settings reloaded: enabled=$enabled, maxSize=$maxHistorySize, autoExpiry=$autoExpiryEnabled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading clipboard settings", e)
        }
    }
    
    /**
     * Show clipboard keyboard layout
     */
    private fun showClipboardKeyboard() {
        try {
            Log.d(TAG, "Showing clipboard keyboard")
            
            // Get clipboard items for UI
            val items = clipboardHistoryManager.getHistoryForUI(20)
            
            // Hide suggestion bar during clipboard mode
            topContainer?.visibility = View.GONE
            
            // Switch keyboard to clipboard layout
            keyboardView?.showClipboardLayout(items)
            
            Log.d(TAG, "Clipboard keyboard shown with ${items.size} items")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing clipboard keyboard", e)
            switchToNormalKeyboard()
        }
    }
    
    /**
     * Handle clipboard key tap
     */
    private fun handleClipboardKeyTap(item: ClipboardItem) {
        try {
            Log.d(TAG, "Clipboard key tapped: ${item.getPreview()}")
            
            // Commit text to input connection
            val ic = currentInputConnection
            if (ic != null) {
                ic.commitText(item.text, 1)
                Log.d(TAG, "Pasted clipboard item: ${item.getPreview()}")
            } else {
                Log.e(TAG, "No input connection available for paste")
            }
            
            // Switch back to normal keyboard
            switchToNormalKeyboard()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling clipboard key tap", e)
        }
    }
    
    /**
     * Handle clipboard back button tap
     */
    private fun handleClipboardBackTap() {
        Log.d(TAG, "Clipboard back button tapped")
        switchToNormalKeyboard()
    }
    
    /**
     * Switch to normal keyboard mode
     */
    private fun switchToNormalKeyboard() {
        try {
            Log.d(TAG, "Switching to normal keyboard")
            
            currentInputMode = INPUT_MODE_NORMAL
            
            // Show suggestion bar again
            topContainer?.visibility = View.VISIBLE
            
            // Restore normal keyboard layout
            keyboardView?.showNormalLayout()
            
            // Rebind listener to ensure long-press works
            rebindKeyboardListener()
            
            Log.d(TAG, "Normal keyboard restored with listener rebound")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error switching to normal keyboard", e)
        }
    }
    
    /**
     * Load enhanced keyboard settings
     */
    private fun loadEnhancedSettings() {
        try {
            val prefs = getSharedPreferences("keyboard_enhanced_settings", Context.MODE_PRIVATE)
            
            // Load bilingual settings
            bilingualModeEnabled = prefs.getBoolean("bilingual_enabled", false)
            if (bilingualModeEnabled) {
                val primary = prefs.getString("primary_language", "en") ?: "en"
                val secondary = prefs.getString("secondary_language", "es") ?: "es"
                keyboardLayoutManager?.enableBilingualMode(primary, secondary)
                Log.d(TAG, "Bilingual mode enabled: $primary + $secondary")
            }
            
            // Load floating keyboard settings
            floatingModeEnabled = prefs.getBoolean("floating_enabled", false)
            if (floatingModeEnabled) {
                keyboardView?.enableFloatingMode(true)
                Log.d(TAG, "Floating mode enabled")
            }
            
            // Load adaptive sizing settings
            adaptiveSizingEnabled = prefs.getBoolean("adaptive_sizing_enabled", true)
            Log.d(TAG, "Adaptive sizing enabled: $adaptiveSizingEnabled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading enhanced settings", e)
        }
    }
    
    /**
     * Handle enhanced language switching with bilingual support
     * NOTE: Deprecated - now using direct cycleLanguage() call from globe button
     */
    @Deprecated("Use cycleLanguage() directly instead")
    private fun handleEnhancedLanguageSwitch() {
        try {
            if (bilingualModeEnabled) {
                // Toggle between primary and secondary language contexts
                keyboardLayoutManager?.let { manager ->
                    val currentText = getCurrentInputTextForLanguageDetection()
                    val detectedLanguage = manager.detectLanguageContext(currentText)
                    Log.d(TAG, "Detected language context: $detectedLanguage")
                }
            } else {
                // Standard language switching
                cycleLanguage()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced language switch", e)
        }
    }
    
    /**
     * Get current input text for language detection
     */
    private fun getCurrentInputTextForLanguageDetection(): String {
        return try {
            val ic = currentInputConnection
            val beforeCursor = ic?.getTextBeforeCursor(100, 0)
            beforeCursor?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Enable/disable bilingual typing mode
     */
    fun setBilingualMode(enabled: Boolean, primary: String = "en", secondary: String = "es") {
        bilingualModeEnabled = enabled
        if (enabled) {
            keyboardLayoutManager?.enableBilingualMode(primary, secondary)
        } else {
            keyboardLayoutManager?.disableBilingualMode()
        }
        
        // Save settings
        val prefs = getSharedPreferences("keyboard_enhanced_settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("bilingual_enabled", enabled)
            putString("primary_language", primary)
            putString("secondary_language", secondary)
            apply()
        }
        
        Log.d(TAG, "Bilingual mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Enable/disable floating keyboard mode
     */
    fun setFloatingMode(enabled: Boolean) {
        floatingModeEnabled = enabled
        keyboardView?.enableFloatingMode(enabled)
        
        // Save settings
        val prefs = getSharedPreferences("keyboard_enhanced_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("floating_enabled", enabled).apply()
        
        Log.d(TAG, "Floating mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    // ====== CLIPBOARD & DICTIONARY ENHANCEMENTS ======
    
    /**
     * Update clipboard strip with recent/pinned items
     */
    private fun updateClipboardStrip() {
        try {
            mainHandler.post {
                val items = clipboardHistoryManager.getHistoryForUI(5)
                clipboardStripView?.updateItems(items)
                Log.d(TAG, "Updated clipboard strip with ${items.size} items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating clipboard strip", e)
        }
    }
    
    /**
     * Show options for clipboard item (pin/unpin, delete)
     */
    private fun showClipboardItemOptions(item: ClipboardItem) {
        try {
            val options = if (item.isPinned) {
                arrayOf("Paste", "Unpin", "Delete")
            } else {
                arrayOf("Paste", "Pin", "Delete")
            }
            
            // For now, just show a simple action based on first option
            // In a full implementation, you'd show a popup menu
            when {
                !item.isPinned -> {
                    clipboardHistoryManager.togglePin(item.id)
                    Toast.makeText(this, "Item pinned", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    clipboardHistoryManager.togglePin(item.id)
                    Toast.makeText(this, "Item unpinned", Toast.LENGTH_SHORT).show()
                }
            }
            
            updateClipboardStrip()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing clipboard options", e)
        }
    }
    
    /**
     * Get dictionary suggestions for current word
     * Integrates into the suggestion engine
     */
    private fun getDictionarySuggestions(word: String): List<String> {
        if (!dictionaryEnabled || word.isBlank()) return emptyList()
        
        return try {
            // Check for exact match first
            val expansion = dictionaryManager.getExpansion(word)
            if (expansion != null) {
                return listOf(expansion.expansion)
            }
            
            // Get matching shortcuts
            val matches = dictionaryManager.getMatchingShortcuts(word, 3)
            matches.map { it.expansion }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dictionary suggestions", e)
            emptyList()
        }
    }
    
    /**
     * Get previous words from input for contextual autocorrect (Gboard-quality enhancement)
     * Returns pair of (previousWord, wordBeforePrevious)
     */
    private fun getPreviousWordsFromInput(): Pair<String, String> {
        return try {
            val ic = currentInputConnection ?: return Pair("", "")
            val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
            
            // Split by word boundaries and get last completed words
            val words = textBefore.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            
            val prev1 = if (words.size >= 1) words.last() else ""
            val prev2 = if (words.size >= 2) words[words.size - 2] else ""
            
            Log.d(TAG, "🧠 Context words: prev1='$prev1', prev2='$prev2'")
            Pair(prev1, prev2)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting previous words", e)
            Pair("", "")
        }
    }
    
    /**
     * Check if current word should be auto-expanded from dictionary
     * Called on space/punctuation
     */
    private fun checkDictionaryExpansion(word: String) {
        if (!dictionaryEnabled || word.isBlank()) return
        
        try {
            val expansion = dictionaryManager.getExpansion(word)
            if (expansion != null) {
                val ic = currentInputConnection ?: return
                
                // Get text before cursor to verify the word is there
                val textBefore = ic.getTextBeforeCursor(word.length + 10, 0)?.toString() ?: ""
                Log.d(TAG, "Checking expansion for '$word', text before: '$textBefore'")
                
                // Delete the shortcut that was just typed
                // The word hasn't been committed yet at this point, so we delete from currentWord
                ic.deleteSurroundingText(word.length, 0)
                
                // Insert the expansion with a space
                ic.commitText("${expansion.expansion} ", 1)
                
                // Increment usage count
                dictionaryManager.incrementUsage(word)
                
                Log.d(TAG, "✅ Dictionary expansion: $word -> ${expansion.expansion}")
            } else {
                Log.d(TAG, "No expansion found for: $word")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking dictionary expansion", e)
        }
    }
    
    /**
     * Integrate dictionary and clipboard into suggestions
     * Override existing suggestion generation
     */
    private fun getEnhancedSuggestions(word: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        try {
            // 1. Dictionary suggestions (highest priority for exact shortcuts)
            val dictSuggestions = getDictionarySuggestions(word)
            suggestions.addAll(dictSuggestions)
            
            // 2. Regular AI/autocorrect suggestions
            // (existing suggestion logic would go here)
            
            // 3. User's learned words from database
            // (existing logic)
            
            return suggestions.take(5)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting enhanced suggestions", e)
            return emptyList()
        }
    }
    
    /**
     * Reload dictionary settings from Flutter
     */
    private fun reloadDictionarySettings() {
        try {
            val prefs = getSharedPreferences("dictionary_manager", Context.MODE_PRIVATE)
            dictionaryEnabled = prefs.getBoolean("dictionary_enabled", true)
            
            Log.d(TAG, "Dictionary settings reloaded: enabled=$dictionaryEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading dictionary settings", e)
        }
    }

    /**
     * Set up periodic cloud sync for user dictionary
     */
    private fun setupPeriodicSync() {
        syncHandler = Handler(Looper.getMainLooper())
        syncRunnable = object : Runnable {
            override fun run() {
                try {
                    if (::userDictionaryManager.isInitialized) {
                        userDictionaryManager.syncToCloud()
                        Log.d(TAG, "Periodic user dictionary sync completed")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error during periodic sync: ${e.message}")
                }
                
                // Schedule next sync
                syncHandler?.postDelayed(this, syncInterval)
            }
        }
        
        // Start initial sync after 2 minutes
        syncHandler?.postDelayed(syncRunnable!!, 2 * 60 * 1000L)
        Log.d(TAG, "Periodic sync scheduled every ${syncInterval / 1000 / 60} minutes")
    }
}
