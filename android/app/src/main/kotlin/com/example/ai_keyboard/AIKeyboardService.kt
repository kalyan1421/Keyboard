package com.example.ai_keyboard
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
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
import android.view.ViewGroup
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
import android.widget.FrameLayout
import android.widget.Button
import android.widget.ScrollView
import android.view.Gravity
import android.view.LayoutInflater
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ai_keyboard.utils.*
import android.inputmethodservice.InputMethodService.Insets
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
        
        // Singleton instance for external access
        private var instance: AIKeyboardService? = null
        
        fun getInstance(): AIKeyboardService? = instance
        
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
     * Keyboard mode enum for multi-mode layout system
     * Letters → Symbols → Extended Symbols → Dialer
     */
    enum class KeyboardMode {
        LETTERS,
        NUMBERS,
        SYMBOLS,
        EXTENDED_SYMBOLS,
        DIALER,
        EMOJI
    }
    
    /**
     * Unified feature panel types
     * Single dynamic panel for all toolbar features
     */
    enum class PanelType {
        GRAMMAR_FIX,
        WORD_TONE,
        AI_ASSISTANT,
        CLIPBOARD,
        QUICK_SETTINGS,
        EMOJI
    }
    
    /**
     * AI Panel Type enum
     */
    
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
    private lateinit var keyboardHeightManager: KeyboardHeightManager
    private var suggestionContainer: LinearLayout? = null
    private var topContainer: LinearLayout? = null // Container for suggestions + language switch
    private var mediaPanelManager: SimpleMediaPanel? = null
    // gboardEmojiPanel removed - using EmojiPanelController only
    private var emojiPanelController: EmojiPanelController? = null // New XML-based controller
    private var emojiPanelView: View? = null // Inflated emoji panel
    internal var keyboardContainer: LinearLayout? = null
    internal var mainKeyboardLayout: LinearLayout? = null // Main layout containing toolbar + keyboard
    private var isMediaPanelVisible = false
    private var isEmojiPanelVisible = false
    private var isMiniSettingsVisible = false
    
    // Enhancements: Simple tracking (KeyboardEnhancements.kt removed as unused)
    private var lastSettingsApply = 0L
    
    // AI Panel (CleverType-style)
    private var aiPanel: LinearLayout? = null
    private var aiChipContainer: LinearLayout? = null
    private var aiResultView: TextView? = null
    private var aiReplaceButton: android.widget.Button? = null
    private var aiLanguageSpinner: android.widget.Spinner? = null
    private var isAIPanelVisible = false
    private var currentAIOriginalText = ""
    
    // Track panel views for theme updates
    private var currentGrammarPanelView: View? = null
    private var currentTonePanelView: View? = null
    private var currentAIAssistantPanelView: View? = null
    
    // Prompt management broadcast receiver
    private val promptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ai_keyboard.PROMPTS_UPDATED") {
                Log.d(TAG, "🔄 Received prompts update broadcast")
                reloadAIPrompts()
            }
        }
    }
    
    /**
     * Reload AI prompts after update broadcast
     */
    private fun reloadAIPrompts() {
        try {
            val grammarPrompts = PromptManager.getPrompts("grammar")
            val tonePrompts = PromptManager.getPrompts("tone")
            val assistantPrompts = PromptManager.getPrompts("assistant")
            
            Log.d(TAG, "🔄 Reloaded prompts: grammar=${grammarPrompts.size}, tone=${tonePrompts.size}, assistant=${assistantPrompts.size}")
            
            // Update visible panels if they're currently displayed
            // Grammar panel update
            currentGrammarPanelView?.let { view ->
                // Clear existing custom buttons first (buttons beyond the static 3)
                val buttonContainer = view.findViewById<LinearLayout>(R.id.grammarButtonContainer)
                val staticButtonCount = 3 // Rephrase, Fix Grammar, Add Emojis
                if (buttonContainer != null && buttonContainer.childCount > staticButtonCount) {
                    buttonContainer.removeViews(staticButtonCount, buttonContainer.childCount - staticButtonCount)
                }
                val grammarOutput = view.findViewById<TextView>(R.id.grammarOutput)
                loadCustomGrammarPrompts(view, grammarOutput)
                Log.d(TAG, "🔄 Updated grammar panel with ${grammarPrompts.size} new prompts")
            }
            
            // Tone panel update  
            currentTonePanelView?.let { view ->
                // Clear existing custom buttons first (buttons beyond the static 4)
                val buttonContainer = view.findViewById<LinearLayout>(R.id.toneButtonContainer)
                val staticButtonCount = 4 // Funny, Poetic, Shorten, Sarcastic
                if (buttonContainer != null && buttonContainer.childCount > staticButtonCount) {
                    buttonContainer.removeViews(staticButtonCount, buttonContainer.childCount - staticButtonCount)
                }
                val outputs = listOf(
                    view.findViewById<TextView>(R.id.toneOutput1),
                    view.findViewById<TextView>(R.id.toneOutput2),
                    view.findViewById<TextView>(R.id.toneOutput3)
                )
                loadCustomTonePrompts(view, outputs)
                Log.d(TAG, "🔄 Updated tone panel with ${tonePrompts.size} new prompts")
            }
            
            // AI Assistant panel update
            currentAIAssistantPanelView?.let { view ->
                // Clear existing custom buttons first (buttons beyond the static 4)
                val buttonContainer = view.findViewById<LinearLayout>(R.id.aiAssistantButtonContainer)
                val staticButtonCount = 4 // ChatGPT, Humanize, Reply, Idioms
                if (buttonContainer != null && buttonContainer.childCount > staticButtonCount) {
                    buttonContainer.removeViews(staticButtonCount, buttonContainer.childCount - staticButtonCount)
                }
                val aiOutput = view.findViewById<TextView>(R.id.aiOutput)
                loadCustomAIAssistantPrompts(view, aiOutput)
                Log.d(TAG, "🔄 Updated AI assistant panel with ${assistantPrompts.size} new prompts")
            }
            
            // Refresh any visible AI panels
            if (isAIPanelVisible) {
                // Could trigger panel refresh here if needed
                Log.d(TAG, "🔄 AI panel is visible, prompts updated")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reloading AI prompts", e)
        }
    }
    
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
    private var currentLongPressKeyObject: Keyboard.Key? = null
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
    
    // Unified layout orchestrator (centralizes all layout loading)
    private lateinit var unifiedController: UnifiedLayoutController
    
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
    // indicScriptHelper removed - was never used (dead code cleanup)  
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
    
    // PERFORMANCE: Debounced suggestion updates with caching
    private var suggestionUpdateJob: Job? = null
    private val suggestionDebounceMs = 100L
    private val suggestionCache = mutableMapOf<String, List<String>>()
    
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
    // languageDetector removed - was never used (dead code cleanup)
    
    // Unified suggestion system
    private lateinit var suggestionsPipeline: SuggestionsPipeline
    
    // Settings and Theme
    private lateinit var settings: SharedPreferences
    lateinit var themeManager: ThemeManager
    private lateinit var settingsManager: SettingsManager
    private var lastLoadedSettingsHash: Int = 0
    
    // Method channels removed for compatibility - using SharedPreferences only for theme updates
    
    private lateinit var languageLayoutAdapter: LanguageLayoutAdapter
    private lateinit var multilingualDictionary: MultilingualDictionaryImpl
    private lateinit var autocorrectEngine: UnifiedAutocorrectEngine
    private var languageSwitchView: LanguageSwitchView? = null
    
    // Enhanced prediction system
    private lateinit var nextWordPredictor: com.example.ai_keyboard.predict.NextWordPredictor
    
    
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
    
    // AI Services
    private lateinit var advancedAIService: AdvancedAIService
    private lateinit var unifiedAIService: UnifiedAIService
    
    
    
    // Main handler for UI operations
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
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
    
    // Broadcast receiver for settings changes and AI prompt updates
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.action) {
                    "com.example.ai_keyboard.SETTINGS_CHANGED" -> {
                        Log.d(TAG, "SETTINGS_CHANGED broadcast received!")
                        
                        // Simple debouncing to avoid spam (replaces SettingsDebouncer)
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastSettingsApply < 250) {
                            Log.d(TAG, "⏳ Settings change debounced")
                            return
                        }
                        
                        // Reload settings immediately on main thread
                        mainHandler.post {
                            try {
                                Log.d(TAG, "📥 Loading settings from broadcast...")
                                lastSettingsApply = currentTime
                                
                                // UNIFIED SETTINGS LOAD - single read from all prefs
                                applyLoadedSettings(settingsManager.loadAll(), logSuccess = false)
                                
                                // Apply CleverType config
                                applyConfig()
                                
                                // Reload theme from Flutter SharedPreferences
                                themeManager.reload()
                                applyTheme()
                                
                                // Check if number row setting changed and reload layout
                                val numberRowEnabled = getNumberRowEnabled()
                                if (currentKeyboardMode == KeyboardMode.LETTERS) {
                                    coroutineScope.launch {
                                        loadLanguageLayout(currentLanguage)
                                        Log.d(TAG, "✅ Layout reloaded with numberRow=$numberRowEnabled")
                                    }
                                }
                                
                                // ✅ Update AI prompt displays in panels (if custom prompt changed)
                                updateAIPanelsWithNewPrompt()
                                
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
                        
                        // ✅ FIX: Replace Thread.sleep with proper async delay
                        mainHandler.postDelayed({
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
                        }, 50) // Delay 50ms to ensure SharedPreferences are written
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
                                
                                // ✅ Reload dictionary entries and refresh LanguageResources
                                if (::dictionaryManager.isInitialized) {
                                    dictionaryManager.reloadFromFlutterPrefs()
                                    Log.d(TAG, "✅ Dictionary entries reloaded from Flutter!")
                                    
                                    // Refresh LanguageResources with updated user data (NEW UNIFIED APPROACH)
                                    refreshLanguageResourcesAsync()
                                } else {
                                    Log.w(TAG, "⚠️ DictionaryManager not initialized yet")
                                }
                                
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
                    "AI_PROMPT_UPDATED" -> {
                        Log.d(TAG, "AI_PROMPT_UPDATED broadcast received!")
                        mainHandler.post {
                            try {
                                Log.d(TAG, "🧠 Updating AI panels with new custom prompt...")
                                updateAIPanelsWithNewPrompt()
                                Log.d(TAG, "✅ AI panels updated successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error updating AI panels from broadcast", e)
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
        
        // Initialize keyboard height manager
        keyboardHeightManager = KeyboardHeightManager(this)
        
        // Initialize PromptManager and register prompt receiver
        try {
            PromptManager.init(this)
            val intentFilter = IntentFilter("com.example.ai_keyboard.PROMPTS_UPDATED")
            registerReceiver(promptReceiver, intentFilter)
            Log.d(TAG, "✅ PromptManager initialized and receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing PromptManager", e)
        }
        
        // Initialize OpenAI configuration first (critical for AI features)
        try {
            OpenAIConfig.getInstance(this)
            Log.d(TAG, "OpenAI configuration initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OpenAI configuration", e)
        }
        
        
        // Initialize Advanced AI Service for toolbar panels
        try {
            advancedAIService = AdvancedAIService(this)
            advancedAIService.preloadWarmup()
            Log.d(TAG, "✅ AdvancedAIService initialized and warmed up")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AdvancedAIService", e)
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
        
        // Keyboard layouts now handled by LanguageLayoutAdapter (JSON-based)
        
        // CRITICAL: Initialize all core components FIRST before any async operations
        initializeCoreComponents()
        
        // UNIFIED SETTINGS LOAD - replaces loadSettings(), loadEnhancedSettings(), loadKeyboardSettings()
        applyLoadedSettings(settingsManager.loadAll(), logSuccess = true)
        // loadDictionariesAsync() // DISABLED: Using new unified MultilingualDictionary system instead
        
        // Initialize multilingual components (now safe)
        initializeMultilingualComponents()
        
        // Default English activation using unified function
        coroutineScope.launch {
            try {
                Log.d(TAG, "🌍 Starting default English activation...")
                activateLanguage("en")
                Log.d(TAG, "🌍 Default English activation complete (Firebase cache)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed default English activation", e)
            }
        }

        // Initialize Enhanced AI Service Bridge
        initializeAIBridge()
        
        // Initialize Unified AI Service for enhanced features
        try {
            unifiedAIService = UnifiedAIService(this)
            Log.d(TAG, "🧠 UnifiedAI Service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize UnifiedAI Service", e)
        }
        
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
        
        
        
        // Initialize Theme MethodChannel
        initializeThemeChannel()
        
        // Initialize advanced keyboard features
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        // longPressHandler is now a val initialized at declaration
        
        // Caps/Shift Manager now initialized by UnifiedLayoutController
        // initializeCapsShiftManager() - MOVED to UnifiedLayoutController
        
        // ClipboardHistoryManager already initialized in initializeCoreComponents()
        // Just complete setup with listener
        if (::clipboardHistoryManager.isInitialized) {
            clipboardHistoryManager.initialize()
            clipboardHistoryManager.addListener(clipboardHistoryListener)
        }
        
        // DictionaryManager already initialized in initializeCoreComponents()
        // Just complete setup with listener
        if (::dictionaryManager.isInitialized) {
            dictionaryManager.initialize()
            dictionaryManager.addListener(dictionaryListener)
        }
        
        // User dictionary manager already initialized in initializeCoreComponents()
        // Phase 3: Multi-language sync integration
        if (::userDictionaryManager.isInitialized) {
            val currentLang = dictionaryManager.getCurrentLanguage()
            
            // Sync learned words from cloud
            userDictionaryManager.syncFromCloud(currentLang)
            Log.d(TAG, "✅ User dictionary sync initiated for $currentLang")
            
            // Connect dictionary manager to cloud sync (per-language)
            dictionaryManager.setCloudSyncCallback { shortcuts ->
                // Sync shortcuts to Firebase via UserDictionaryManager with language context
                userDictionaryManager.syncShortcutsToCloud(shortcuts, currentLang)
                Log.d(TAG, "☁️ Synced ${shortcuts.size} shortcuts for $currentLang")
            }
            
            // Load shortcuts from cloud and import
            userDictionaryManager.loadShortcutsFromCloud(currentLang) { cloudShortcuts ->
                dictionaryManager.importFromCloud(cloudShortcuts)
                Log.d(TAG, "✅ Imported ${cloudShortcuts.size} shortcuts from cloud for $currentLang")
            }
            
            Log.d(TAG, "✅ Custom shortcuts cloud sync enabled for $currentLang")
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
                addAction("AI_PROMPT_UPDATED")
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
        
            // onCreate complete
    }
    
    /**
     * Override to explicitly control keyboard height including number row
     * Ensures keyboard grows upward when number row is active, regardless of navigation bar
     */
    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        
        outInsets?.let { insets ->
            // Get the main input view
            val inputView = mainKeyboardLayout ?: return
            
            // Read the current number row setting to ensure consistency
            val numberRowEnabled = getNumberRowEnabled()
            
            // Calculate the desired keyboard height including number row if active
            val desiredHeight = keyboardHeightManager.calculateKeyboardHeight(
                includeToolbar = true,
                includeSuggestions = true
            )
            
            // ✅ FIX: Always set all insets to 0 to prevent white space gaps
            // The keyboard view uses weight layout param to fill available space
            insets.contentTopInsets = 0
            insets.visibleTopInsets = 0
            
            // ✅ FIX: Remove bottom gap by ensuring touch region extends to bottom edge
            // Set input view height to exactly fill the available space 
            val windowHeight = resources.displayMetrics.heightPixels
            val statusBarHeight = getStatusBarHeight()
            val navBarHeight = getNavigationBarHeight()
            val availableHeight = windowHeight - statusBarHeight
            
            // Make sure keyboard view fills entire bottom area (no gaps)
            inputView.layoutParams = inputView.layoutParams.apply {
                height = desiredHeight
            }
            
            // Ensure touch region covers the entire keyboard
            insets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
            insets.touchableRegion.setEmpty()
            insets.touchableRegion.op(
                0, 
                0,  // Start from top
                inputView.width, 
                inputView.height,
                android.graphics.Region.Op.UNION
            )
            
        }
    }
    
    /**
     * Initialize core components FIRST to prevent UninitializedPropertyAccessException
     * This must be called before any async operations that might access these components
     */
    private fun initializeCoreComponents() {
        try {
            Log.d(TAG, "🔧 Initializing core components...")
            
            // Initialize language manager FIRST (needed for language preferences)
            languageManager = LanguageManager(this)
            Log.d(TAG, "✅ LanguageManager initialized")
            
            // 🔍 AUDIT: Add language change listener to sync layout/dictionary
            languageManager.addLanguageChangeListener(object : LanguageManager.LanguageChangeListener {
                override fun onLanguageChanged(oldLanguage: String, newLanguage: String) {
                    Log.d("LangSwitch", "🌐 Switching from $oldLanguage → $newLanguage")
                    
                    // Update current language tracking
                    currentLanguage = newLanguage
                    
                            // Use unified activation function for language switching
                            coroutineScope.launch {
                                try {
                                    // Switch user managers first
                                    if (::dictionaryManager.isInitialized) {
                                        dictionaryManager.switchLanguage(newLanguage)
                                    }
                                    
                                    if (::userDictionaryManager.isInitialized) {
                                        userDictionaryManager.switchLanguage(newLanguage)
                                    }
                                    
                                    // Use unified activation function
                                    activateLanguage(newLanguage)
                                    
                                } catch (e: Exception) {
                                    Log.e("LangSwitch", "❌ Error in unified language switching", e)
                                }
                            }
                    
                    Log.d("LangSwitch", "✅ Language switch initiated: $oldLanguage → $newLanguage")
                }
                
                override fun onEnabledLanguagesChanged(enabledLanguages: Set<String>) {
                    Log.d("LangSwitch", "🌐 Enabled languages updated: $enabledLanguages")
                }
            })
            
            // Initialize clipboard history manager (needed for SuggestionsPipeline)
            clipboardHistoryManager = ClipboardHistoryManager(this)
            Log.d(TAG, "✅ ClipboardHistoryManager initialized")
            
            // Initialize dictionary manager (for shortcuts/custom mappings)
            dictionaryManager = DictionaryManager(this)
            Log.d(TAG, "✅ DictionaryManager initialized")
            
            // Initialize user dictionary manager
            userDictionaryManager = UserDictionaryManager(this)
            Log.d(TAG, "✅ UserDictionaryManager initialized")
            
            // Initialize multilingual dictionary (NEW UNIFIED ARCHITECTURE)
            multilingualDictionary = MultilingualDictionaryImpl(this)
            Log.d(TAG, "✅ MultilingualDictionary initialized")
            
            // 2️⃣ Set up enhanced autocorrect reload hook
            multilingualDictionary.setOnLanguageReadyListener { lang ->
                Log.d("AIKeyboardService", "🔁 Autocorrect engine reloaded for $lang")
                try {
                    val resources = multilingualDictionary.get(lang)
                    if (resources != null && ::autocorrectEngine.isInitialized) {
                        autocorrectEngine.setLanguage(lang, resources)
                        Log.d(TAG, "✅ UnifiedAutocorrectEngine reloaded for language: $lang [words=${resources.words.size}, bigrams=${resources.bigrams.size}, trigrams=${resources.trigrams.size}]")
                        Log.d(TAG, "🔥 AUTOCORRECT ENGINE NOW READY - Next-word prediction should work!")
                        
                        // Verify the engine is actually ready
                        val isReady = autocorrectEngine.hasLanguage(lang)
                        Log.d(TAG, "🔍 Post-reload verification - Engine ready: $isReady")
                        
                    } else {
                        Log.e(TAG, "❌ CRITICAL: Resources null or engine not initialized - lang=$lang, resources=${resources != null}, engineReady=${::autocorrectEngine.isInitialized}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to reload engine for language: $lang", e)
                }
            }
            Log.d(TAG, "✅ Enhanced language readiness callback registered")
            
            // Wire managers to feed into MultilingualDictionary
            multilingualDictionary.setUserDictionaryManager(userDictionaryManager)
            multilingualDictionary.setDictionaryManager(dictionaryManager)
            Log.d(TAG, "✅ Dictionary managers wired to MultilingualDictionary")
            
            // Wire observers to refresh LanguageResources when user data changes
            userDictionaryManager.setChangeObserver {
                refreshLanguageResourcesAsync()
            }
            dictionaryManager.setChangeObserver {
                refreshLanguageResourcesAsync()
            }
            Log.d(TAG, "✅ Change observers wired for LanguageResources refresh")
            
            // Initialize transliteration engine for default language
            transliterationEngine = TransliterationEngine(this, "en")
            Log.d(TAG, "✅ TransliterationEngine initialized")
            
            // IndicScriptHelper removed - was never used (dead code cleanup)
            
            // Initialize unified autocorrect engine (NEW UNIFIED API)
            autocorrectEngine = UnifiedAutocorrectEngine(
                context = this,
                multilingualDictionary = multilingualDictionary,
                transliterationEngine = transliterationEngine,
                userDictionaryManager = userDictionaryManager
            )
            Log.d(TAG, "✅ UnifiedAutocorrectEngine initialized")
            
            // Attach suggestion callback for real-time updates
            autocorrectEngine.attachSuggestionCallback { suggestions ->
                updateSuggestionUI(suggestions)
            }
            Log.d(TAG, "✅ Suggestion callback attached to autocorrect engine")
            
            // Initialize current language using unified activation function
            coroutineScope.launch {
                try {
                    Log.d(TAG, "🌐 Activating current language: $currentLanguage")
                    activateLanguage(currentLanguage)
                    
                    // Background preload additional enabled languages
                    val enabledLanguages = languageManager.getEnabledLanguages()
                    val otherLanguages = enabledLanguages.filter { it != currentLanguage }
                    
                    if (otherLanguages.isNotEmpty()) {
                        Log.d(TAG, "🌍 Background preloading additional languages: $otherLanguages")
                        
                        launch(Dispatchers.Default) {
                            otherLanguages.forEach { lang ->
                                try {
                                    activateLanguage(lang)
                                    Log.d(TAG, "✅ Background activated: $lang")
                                } catch (e: Exception) {
                                    Log.w(TAG, "⚠️ Failed to activate $lang in background: ${e.message}")
                                }
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in language activation", e)
                }
            }
            
            // Initialize SuggestionsPipeline with unified engine
            suggestionsPipeline = SuggestionsPipeline(
                context = this,
                clipboardManager = clipboardHistoryManager,
                unifiedAutocorrectEngine = autocorrectEngine,
                multilingualDictionary = multilingualDictionary,
                languageManager = languageManager
            )
            Log.d(TAG, "✅ SuggestionsPipeline initialized with unified engine")
            
            // Set up SuggestionsPipeline dependencies
            suggestionsPipeline.setMultilingualDictionary(multilingualDictionary)
            suggestionsPipeline.setLanguageManager(languageManager)
            Log.d(TAG, "✅ SuggestionsPipeline dependencies configured")
            
            // Wire language activation callback for automatic engine setup after download
            multilingualDictionary.setOnLanguageActivatedListener { lang ->
                Log.d(TAG, "🎯 Language activation callback received for $lang")
                coroutineScope.launch {
                    try {
                        onLanguageFullyActivated(lang)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error in language activation callback for $lang", e)
                    }
                }
            }
            
            // Verify loading status asynchronously (don't block onCreate)
            coroutineScope.launch {
                delay(1000) // Wait for async loads to complete
                
                if (autocorrectEngine.hasLanguage(currentLanguage)) {
                    Log.i(TAG, "✅ UnifiedAutocorrectEngine ready for language: $currentLanguage")
                } else {
                    Log.w(TAG, "⚠️ UnifiedAutocorrectEngine not ready for language: $currentLanguage")
                }
            }
            
            
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
        
        // Check if unified engine has current language loaded (NEW UNIFIED READINESS)
        if (!autocorrectEngine.hasLanguage(currentLanguage)) {
            // Don't log warning here as it's normal during async load
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
        if (::unifiedAIService.isInitialized && unifiedAIService.isReady()) {
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
                                // UnifiedAI Service already initialized
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
            val hasAutocorrectEngine = true // UnifiedAutocorrectEngine with built-in swipe support
            
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
            // AIServiceBridge removed - UnifiedAI Service handles this
            Log.d(TAG, "AI Bridge initialized successfully")
            
            // Check AI readiness periodically - retry up to 5 times only if dictionaries are not ready
            coroutineScope.launch {
                delay(1000)
                var retryCount = 0
                val currentLang = currentLanguage
                
                while (!isAIReady && retryCount < 5) {
                    // Check if dictionaries are ready first
                    val dictionariesReady = autocorrectEngine.hasLanguage(currentLang)
                    
                    if (!dictionariesReady) {
                        Log.d(TAG, "🔵 [AI] Waiting for dictionaries to load for $currentLang, retry $retryCount/5")
                    } else {
                        isAIReady = unifiedAIService.isReady()
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
                Log.d(TAG, "✅ Number row changed to $numberRow, keyboard height updated")
            }
            
            // Determine font scale and bottom offset based on orientation
            val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val fontScale = if (isLandscape) fontScaleL else fontScaleP
            // ✅ FIX: NO bottom padding - keys now auto-fill available space
            val hasNavBar = keyboardHeightManager.hasNavigationBar()
            val navBarHeight = if (hasNavBar) keyboardHeightManager.getNavigationBarHeight() else 0
            val bottom = 0  // Always 0 - keys will auto-size to fill space
            Log.d(TAG, "⚙️ Bottom padding: hasNavBar=$hasNavBar, navBarHeight=$navBarHeight, bottomPadding=$bottom (keys auto-fill)")
            
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
        Log.d(TAG, "🚀 initializeMultilingualComponents() called")
        try {
            // LanguageManager already initialized in initializeCoreComponents()
            // No need to reinitialize
            
            // LanguageDetector removed - was never used (dead code cleanup)
            
            // Initialize language layout adapter (JSON-based dynamic layouts)
            languageLayoutAdapter = LanguageLayoutAdapter(this)
            Log.d(TAG, "✓ LanguageLayoutAdapter initialized")
            
            // 🔍 AUDIT: Verify all key mappings at startup (after languageLayoutAdapter is ready)
            Log.d(TAG, "🔍 Running key mapping verification audit...")
            try {
                languageLayoutAdapter.verifyAllMappings()
                
                // 🔍 AUDIT: Compare all template mappings
                listOf("qwerty_template.json", "symbols_template.json", "extended_symbols_template.json", "dialer_template.json")
                    .forEach { templateName ->
                        languageLayoutAdapter.compareKeyMappings(templateName)
                    }
                Log.d(TAG, "✅ Key mapping audit complete")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Key mapping audit failed (non-fatal)", e)
            }
            
            // ✅ Swipe functionality is built into UnifiedAutocorrectEngine (no separate initialization needed)
            Log.d(TAG, "🔧 Swipe functionality available in UnifiedAutocorrectEngine")
            
            // Core components already initialized in initializeCoreComponents()
            // Language loading happens asynchronously in background
            
            // Connect user dictionary manager to autocorrect engine (if initialized)
            if (::userDictionaryManager.isInitialized) {
                // User dictionary integrated in UnifiedAutocorrectEngine
                Log.d(TAG, "Connected user dictionary manager to autocorrect engine")
            }
            
            // Initialize enhanced prediction system (AFTER language loading)
            nextWordPredictor = com.example.ai_keyboard.predict.NextWordPredictor(autocorrectEngine, multilingualDictionary)
            Log.d(TAG, "✅ NextWordPredictor initialized")
            
            // Phase 2: User dictionary integration handled by UnifiedAutocorrectEngine
            Log.d(TAG, "User dictionary integration complete")
            
            Log.d(TAG, "Enhanced prediction system initialized (swipe built-in)")
            
            // Run validation tests for enhanced autocorrect
            coroutineScope.launch {
                delay(2000) // Wait for dictionary to load
                val testResults = autocorrectEngine.getStats()
                testResults.forEach { result ->
                    Log.d(TAG, "Autocorrect Test: $result")
                }
                
                // Dictionary loading handled by MultilingualDictionary system
                // loadDictionariesAsync() // DISABLED: Unified system handles this
                Log.d(TAG, "Dictionary initialization completed via unified system")
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
            logEngineStatus()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing multilingual components", e)
        }
    }
    
    /**
     * Handle language change
     */
    // STREAMLINED: Language change handling with proper logging
    private fun handleLanguageChange(oldLanguage: String, newLanguage: String) {
        try {
            Log.i(TAG, "[AIKeyboard] Language: $oldLanguage → $newLanguage")
            
            // Update unified autocorrect engine locale
            autocorrectEngine.setLocale(newLanguage)
            
            // Update keyboard view with new layout
            // Reload dynamic JSON-based layout for new language
            if (currentKeyboard == KEYBOARD_LETTERS) {
                // ✅ Use unified controller for consistent layout loading
                unifiedController.buildAndRender(newLanguage, LanguageLayoutAdapter.KeyboardMode.LETTERS, showNumberRow)
            }
            
            // Update language switch view
            languageSwitchView?.refreshDisplay()
            
            // Clear current word and update suggestions
            currentWord = ""
            updateAISuggestions()
            
            // Show confirmation toast
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
            
            // Preload keyboard layouts via LanguageLayoutAdapter
            coroutineScope.launch {
                enabledLanguages.forEach { language ->
                    try {
                        languageLayoutAdapter.preloadKeymap(language)
                        Log.d(TAG, "Preloaded keymap for $language")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not preload keymap for $language", e)
                    }
                }
            }
            
            // Update language switch view visibility
            languageSwitchView?.refreshDisplay()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling enabled languages change", e)
        }
    }
    
    override fun onCreateInputView(): View {
        // ✅ UNIFIED CONTAINER: Single background for toolbar + keyboard
        // This ensures perfect theme matching and supports background images
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeManager.createKeyboardBackground()  // Single unified background
            fitsSystemWindows = false
            clipToPadding = false
            clipToOutline = false
            setPadding(0, 0, 0, 0)  // Explicitly remove all padding
            minimumHeight = 0  // No minimum height constraint
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT  // Fill entire available space
            )
        }
        
        mainKeyboardLayout = mainLayout
        
        
        
        // Create suggestion bar
        createUnifiedSuggestionBar(mainLayout)
        
        // Legacy container wrapper for compatibility - NO padding, margins, or gaps
        // ✅ TRANSPARENT: Let parent's unified background show through
        val keyboardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,  // Fill remaining space
                1.0f  // Weight to fill remaining space after toolbar/suggestions
            )
            setPadding(0, 0, 0, 0)  // Explicitly remove all padding
            minimumHeight = 0  // No minimum height constraint
            setBackgroundColor(Color.TRANSPARENT)  // Transparent to show unified background
        }
        
        // Initialize keyboard view without additional WindowInsets handling
        keyboardView = try {
            layoutInflater.inflate(R.layout.keyboard_view_google_layout, null) as SwipeKeyboardView
        } catch (e: Exception) {
            SwipeKeyboardView(this, null, 0)
        }
        
        keyboardView?.apply {
            // ✅ HYBRID APPROACH: Start with XML for immediate display, then switch to dynamic layout
            val keyboardResource = getKeyboardResourceForLanguage(currentLanguage.uppercase(), showNumberRow)
            keyboard = Keyboard(this@AIKeyboardService, keyboardResource)
            setKeyboard(keyboard)
            setOnKeyboardActionListener(this@AIKeyboardService)
            setSwipeListener(this@AIKeyboardService)
            setSwipeEnabled(swipeTypingEnabled)
            isPreviewEnabled = keyPreviewEnabled
            setKeyboardService(this@AIKeyboardService)
            
            // ✅ Let keyboard view fill container - NO margins or spacing
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT  // Fill entire container
            ).apply {
                setMargins(0, 0, 0, 0)  // Remove any margins
            }
            setPadding(0, 0, 0, 0)  // Remove any internal padding
            setBackgroundColor(Color.TRANSPARENT)  // Transparent to show unified background
            
            // ✅ UNIFIED: Load dynamic layout for ALL languages on first open
            // Uses unified controller for consistent behavior
            if (this is SwipeKeyboardView) {
                currentLangCode = currentLanguage
                post {
                    // Use unified controller (will be initialized after this view setup)
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (::unifiedController.isInitialized) {
                            unifiedController.buildAndRender(
                                currentLanguage,
                                LanguageLayoutAdapter.KeyboardMode.LETTERS,
                                showNumberRow
                            )
                        }
                    }, 100) // Small delay to ensure controller is initialized
                }
            }
            
            Log.d(TAG, "[AIKeyboard] Initialized: lang=$currentLanguage, numberRow=$showNumberRow")
        }
        
        // Initialize unified layout controller (centralized layout orchestrator)
        unifiedController = UnifiedLayoutController(
            context = this,
            service = this,
            adapter = languageLayoutAdapter,
            keyboardView = keyboardView!!,
            heightManager = keyboardHeightManager
        )
        
        // Initialize caps manager first, then integrate with unified controller
        initializeCapsShiftManagerDirect()
        
        // Initialize unified controller with all components
        unifiedController.initialize(languageManager, capsShiftManager, languageSwitchView)
        
        Log.d(TAG, "✅ Unified layout controller initialized with all components")
        
        // Create other panels
        createMediaPanel()
        createEmojiPanel()  
        createAIPanel()
        
        // Add keyboard view to container
        keyboardView?.let { keyboardContainer.addView(it) }
        mainLayout.addView(keyboardContainer)
        this.keyboardContainer = keyboardContainer
        
        // Apply theme and handle pending updates
        applyTheme()
        if (pendingThemeUpdate) {
            mainLayout.postDelayed({
                applyThemeFromBroadcast()
                pendingThemeUpdate = false
            }, 100)
        }
        
        return mainLayout
    }
    
    /**
     * Get navigation bar height from system resources
     * Note: This is now only used for reference/debugging, not for height calculations
     */
    private fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
    
    /**
     * Get status bar height from system resources
     */
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
    
    // MERGED: Unified suggestion bar creation (replaces both createSuggestionBarContainer + createSuggestionBar)
    private fun createUnifiedSuggestionBar(parent: LinearLayout) {
        val palette = themeManager.getCurrentPalette()
        
        // ✅ TRANSPARENT CONTAINER: Let parent's unified background show through
        topContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 0)
            setBackgroundColor(Color.TRANSPARENT)  // Transparent for unified theming
        }
        
        // Add toolbar
        val toolbar = createSimplifiedToolbar()
        topContainer?.addView(toolbar)
        
        // ✅ TRANSPARENT SUGGESTIONS: Let unified background show through
        suggestionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.TRANSPARENT)  // Transparent for unified theming
            setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
            visibility = View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            elevation = 0f
        }
        
        // Create 3 equal-weight text suggestions (CleverType style)
        repeat(3) { index ->
            val suggestion = TextView(this).apply {
                setTextColor(palette.keyText) // Use keyText for better visibility
                textSize = 14f
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                gravity = Gravity.CENTER
                setBackgroundColor(Color.TRANSPARENT) // Intentionally transparent for text-only suggestions
                isClickable = true
                isFocusable = true
                ellipsize = TextUtils.TruncateAt.END // Handle long text
                maxLines = 1
                
                setOnClickListener { view ->
                    val suggestionText = (view as TextView).text.toString()
                    if (suggestionText.isNotEmpty()) {
                        applySuggestion(suggestionText)
                    }
                }
                
                // Equal weight distribution
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                layoutParams = params
            }
            suggestionContainer?.addView(suggestion)
        }
        
        // Add suggestions to topContainer
        topContainer?.addView(suggestionContainer)
        
        // Add topContainer to parent
        parent.addView(topContainer)
        
        // Initialize with default suggestions
        mainHandler.postDelayed({
            updateSuggestionUI(listOf("I", "The", "And"))
        }, 300)
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
                setTextColor(themeManager.getTextColor())
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
                // Return to normal keyboard (same as other panels)
                restoreKeyboardFromPanel()
            },
            inputConnectionProvider = { currentInputConnection }
        )
        
        // Using EmojiPanelController only - GboardEmojiPanel removed for cleaner architecture
        
        Log.d(TAG, "Comprehensive emoji panel created (not added to layout yet)")
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
                coroutineScope.launch {
                    try {
                        val stickerService = com.example.ai_keyboard.stickers.StickerServiceAdapter(this@AIKeyboardService)
                        stickerService.recordStickerUsage(sticker.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording sticker usage", e)
                    }
                }
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
                // IndicScriptHelper removed - functionality integrated elsewhere
                
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
                romanBuffer.clear()
                // Font will be handled by system default
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing transliteration for $currentLanguage", e)
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
        val oldLanguage = currentLanguage
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
        
        // CRITICAL: Activate the new language in UnifiedAutocorrectEngine
        coroutineScope.launch {
            try {
                Log.d(TAG, "🔄 Activating switched language: $oldLanguage → $currentLanguage")
                activateLanguage(currentLanguage)
                Log.d(TAG, "✅ Language switch activation complete for $currentLanguage")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error activating switched language $currentLanguage", e)
            }
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
        
        // Phase 3: Sync dictionary managers to new language
        try {
            if (::dictionaryManager.isInitialized) {
                dictionaryManager.switchLanguage(currentLanguage)
                Log.d(TAG, "✅ Dictionary manager switched to $currentLanguage")
            }
            
            if (::userDictionaryManager.isInitialized) {
                userDictionaryManager.switchLanguage(currentLanguage)
                
                // Sync from cloud for new language
                userDictionaryManager.syncFromCloud(currentLanguage)
                
                // Load shortcuts for new language
                userDictionaryManager.loadShortcutsFromCloud(currentLanguage) { cloudShortcuts ->
                    dictionaryManager.importFromCloud(cloudShortcuts)
                    Log.d(TAG, "✅ Loaded ${cloudShortcuts.size} shortcuts for $currentLanguage")
                }
                
                Log.d(TAG, "✅ User dictionary manager switched to $currentLanguage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing managers on language switch", e)
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
    // REMOVED: Deprecated loadSettings() method - now using unified settings loader
    
    
    internal fun applyTheme() {
        keyboardView?.let { view ->
            // Apply theme using comprehensive ThemeManager
            val theme = themeManager.getCurrentTheme()
            val palette = themeManager.getCurrentPalette()
            
            // Set keyboard background
            val backgroundDrawable = themeManager.createKeyboardBackground()
            view.background = backgroundDrawable
            
            // The SwipeKeyboardView will request paint objects from ThemeManager
            if (view is SwipeKeyboardView) {
                view.setThemeManager(themeManager)
                view.refreshTheme()
                // Explicitly set background color to ensure consistency
                view.setBackgroundColor(palette.keyboardBg)
                Log.d(TAG, "[AIKeyboard] Theme applied to keyboard and swipe view - Keyboard BG: ${Integer.toHexString(palette.keyboardBg)}")
            }
            
            // Force redraw with new theme
            view.invalidateAllKeys()
            view.invalidate()
            view.requestLayout()
            
            // Streamlined theme application log
            Log.d(TAG, "Applied theme: ${theme.name} (${theme.id})")
        } ?: run {
            Log.w(TAG, "[AIKeyboard] keyboardView is null, cannot apply theme")
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
                    // Explicitly set background color to ensure consistency
                    view.setBackgroundColor(palette.keyboardBg)
                    
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
                
                Log.d(TAG, "✅ Keyboard view themed with V2 system - Keyboard BG: ${Integer.toHexString(palette.keyboardBg)}")
            }
            
            // 2. ✅ UNIFIED THEMING: Update main layout background (covers toolbar + keyboard)
            mainKeyboardLayout?.let { layout ->
                layout.background = themeManager.createKeyboardBackground()
                Log.d(TAG, "✅ Main layout themed with unified background")
            }
            
            // 3. Update suggestion bar text colors (keep transparent background)
            suggestionContainer?.let { container ->
                // Keep transparent background for unified theming
                container.setBackgroundColor(Color.TRANSPARENT)
                container.elevation = 0f
                
                // Update each suggestion - text-only, NO chip backgrounds
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i)
                    if (child is TextView) {
                        // Use keyText color for better visibility
                        child.setTextColor(palette.keyText)
                        child.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                Log.d(TAG, "✅ Suggestion bar themed - text colors updated")
            }
            
            // 4. ✅ Keep topContainer transparent for unified background
            topContainer?.let { container ->
                container.setBackgroundColor(Color.TRANSPARENT)
                Log.d(TAG, "✅ Top container transparent")
            }
            
            // 5. ✅ Keep keyboardContainer transparent for unified background
            keyboardContainer?.setBackgroundColor(Color.TRANSPARENT)
            Log.d(TAG, "✅ Keyboard container transparent")
            
            // 6. Update emoji panel theming (using EmojiPanelController only)
            emojiPanelController?.applyTheme()
            Log.d(TAG, "✅ Emoji panel (new) themed")
            
            // 7. Update media panel theming
            mediaPanelManager?.let { panel ->
                applyThemeToMediaPanel(panel, palette)
                // Also call the panel's own theme method
                (panel as? SimpleMediaPanel)?.applyTheme()
                Log.d(TAG, "✅ Media panel themed")
            }
            
            // 8. Update toolbar icons with new theme colors
            topContainer?.findViewById<LinearLayout>(R.id.keyboard_toolbar_simple)?.let { toolbar ->
                loadToolbarIcons(toolbar, palette)
                Log.d(TAG, "✅ Toolbar icons re-themed")
            }
            
            // 8. Update AI feature panels to match keyboard theme
            applyThemeToPanels()
            
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
     * Apply unified theme colors to all AI feature panels
     * Called when theme changes to ensure panels match keyboard background
     */
    private fun applyThemeToPanels() {
        try {
            // Get unified colors from ThemeManager
            val bgColor = themeManager.getKeyboardBackgroundColor()
            val textColor = themeManager.getTextColor()
            val keyColor = themeManager.getKeyColor()
            val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            
            // Update Grammar Panel if visible
            currentGrammarPanelView?.let { view ->
                view.setBackgroundColor(bgColor)
                
                // Style single output TextView
                view.findViewById<TextView>(R.id.grammarOutput)?.apply {
                    setTextColor(textColor)
                    setHintTextColor(Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))
                    background = createRoundedTextAreaDrawable(keyColor, strokeColor)
                }
                
                // Style all buttons with rounded corners
                listOf(
                    R.id.btnRephrase,
                    R.id.btnGrammarFix,
                    R.id.btnAddEmojis
                ).forEach { buttonId ->
                    view.findViewById<Button>(buttonId)?.apply {
                        setTextColor(textColor)
                        background = createRoundedButtonDrawable(keyColor, strokeColor)
                    }
                }
                Log.d(TAG, "✅ Grammar panel themed with unified colors")
            }
            
            // Update Tone Panel if visible
            currentTonePanelView?.let { view ->
                view.setBackgroundColor(bgColor)
                
                // Style all 3 output TextViews
                listOf(R.id.toneOutput1, R.id.toneOutput2, R.id.toneOutput3).forEach { outputId ->
                    view.findViewById<TextView>(outputId)?.apply {
                    setTextColor(textColor)
                    setHintTextColor(Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))
                    background = createRoundedTextAreaDrawable(keyColor, strokeColor)
                    }
                }
                
                // Style all buttons with rounded corners
                listOf(
                    R.id.btnFunny,
                    R.id.btnPoetic,
                    R.id.btnShorten,
                    R.id.btnSarcastic
                ).forEach { buttonId ->
                    view.findViewById<Button>(buttonId)?.apply {
                        setTextColor(textColor)
                        background = createRoundedButtonDrawable(keyColor, strokeColor)
                    }
                }
                Log.d(TAG, "✅ Tone panel themed with unified colors")
            }
            
            // Update AI Assistant Panel if visible
            currentAIAssistantPanelView?.let { view ->
                view.setBackgroundColor(bgColor)
                view.findViewById<TextView>(R.id.aiOutput)?.apply {
                    setTextColor(textColor)
                    setHintTextColor(Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))
                    background = createRoundedTextAreaDrawable(keyColor, strokeColor)
                }
                
                // Style all buttons with rounded corners
                listOf(
                    R.id.btnChatGPT,
                    R.id.btnHumanize,
                    R.id.btnReply,
                    R.id.btnIdioms
                ).forEach { buttonId ->
                    view.findViewById<Button>(buttonId)?.apply {
                        setTextColor(textColor)
                        background = createRoundedButtonDrawable(keyColor, strokeColor)
                    }
                }
                Log.d(TAG, "✅ AI Assistant panel themed with unified colors")
            }
            
            // Update ClipboardPanel if it exists
            clipboardPanel?.let {
                // ClipboardPanel will fetch theme colors from ThemeManager when shown
                Log.d(TAG, "✅ Clipboard panel will use unified theme on next show")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme to panels", e)
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

    // Update suggestion bar theme (SIMPLIFIED: text-only, no chips)

    // Show theme update confirmation
    
    /**
     * 🔍 DEEP DIAGNOSTIC: Log every key press with full context for audit
     */
    private fun logKeyDiagnostics(primaryCode: Int, keyCodes: IntArray?) {
        val keyLabel = when (primaryCode) {
            Keyboard.KEYCODE_SHIFT, -1 -> "SHIFT"
            Keyboard.KEYCODE_DELETE, -5 -> "DELETE"
            Keyboard.KEYCODE_DONE, -4 -> "RETURN"
            32 -> "SPACE"
            -14 -> "GLOBE"
            -10 -> "?123"
            -11 -> "ABC"
            -20 -> "=<"
            -21 -> "1234"
            else -> if (primaryCode > 0 && primaryCode < 128) primaryCode.toChar().toString() else "CODE_$primaryCode"
        }
        
        Log.d("KeyAudit", "🔍 Key pressed: $keyLabel | Code: $primaryCode | Mode: $currentKeyboardMode | Lang: $currentLanguage")
    }
    
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        // 🔍 DIAGNOSTIC: Log every key press with full context
        logKeyDiagnostics(primaryCode, keyCodes)
        
        // ✅ Intercept separators for autocorrect BEFORE normal processing
        // BUT: Don't early-return for ENTER key - it needs special handling
        val isEnterKey = (primaryCode == Keyboard.KEYCODE_DONE || primaryCode == -4)
        
        if (isSeparator(primaryCode) && !isEnterKey) {
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
            Keyboard.KEYCODE_DONE, -4 -> {
                // Apply autocorrect before enter (if there's a word to correct)
                if (currentWord.isNotEmpty()) {
                    applyAutocorrectOnSeparator(primaryCode)
                }
                
                // Context-aware enter key behavior (Gboard-style)
                // -4 is sym_keyboard_return from dynamic layouts
                handleEnterKey(ic)
                
                // CRITICAL FIX: Clear suggestions after sentence completion
                currentWord = ""
                updateAISuggestions() // This will clear suggestions for empty word
            }
            KEYCODE_SPACE -> handleSpace(ic)
            KEYCODE_SYMBOLS -> switchKeyboardMode(KeyboardMode.SYMBOLS)  // ?123 key
            KEYCODE_LETTERS -> returnToLetters()    // ABC key returns to letters
            KEYCODE_NUMBERS -> cycleKeyboardMode()  // Also cycle
            -20 -> switchKeyboardMode(KeyboardMode.EXTENDED_SYMBOLS)  // =< key
            -21 -> switchKeyboardMode(KeyboardMode.DIALER)  // 1234 key
            // REMOVED: XML legacy codes (-3, -2) - all XML files now use consistent codes
            KEYCODE_VOICE -> {
                // Enhanced voice input with visual feedback
                try {
                    showVoiceInputFeedback(true)
                    Toast.makeText(this, "🎤 Voice input activated", Toast.LENGTH_SHORT).show()
                    // Reset visual feedback after a delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        showVoiceInputFeedback(false)
                    }, 2000)
                    Log.d(TAG, "Voice input activated")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting voice input", e)
                    Toast.makeText(this, "Voice input not available", Toast.LENGTH_SHORT).show()
                }
                ensureCursorStability()
            }
            KEYCODE_GLOBE -> {
                // Language switching - cycle through enabled languages
                cycleLanguage()
                
                // CRITICAL FIX: Notify AI services about language change
                if (::unifiedAIService.isInitialized) {
                    try {
                        // Reset suggestion context for new language
                        currentWord = ""
                        updateAISuggestions()
                        Log.d(TAG, "✅ AI services notified of language change to: $currentLanguage")
                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ Error notifying AI services of language change", e)
                    }
                }
                
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
        
        // Enhanced character handling with CapsShiftManager (via unified controller)
        if (::unifiedController.isInitialized) {
            // Note: character processing handled internally by caps manager
            // No direct access needed here
        } else if (::capsShiftManager.isInitialized) {
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
            // ✅ FIX: Use safe call instead of !!
            lastCorrection?.let { (_, corrected) ->
                if (undoAvailable && !corrected.startsWith(currentWord, ignoreCase = true)) {
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
        val ic = currentInputConnection ?: run {
            Log.w(TAG, "⚠️ No InputConnection available")
            return false
        }
        
        // Look back to capture the last token (64 chars is plenty for a single word)
        val before = ic.getTextBeforeCursor(64, 0) ?: run {
            Log.w(TAG, "⚠️ Could not get text before cursor")
            // Still commit the separator
            if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
            return true
        }
        
        // Last run of letters/apostrophes (works well for English and Latin-script languages)
        val match = Regex("([\\p{L}']+)$").find(before)
        if (match == null) {
            Log.d(TAG, "🔍 No word found before cursor: '$before'")
            if (code > 0) {
                val charStr = String(Character.toChars(code))
                Log.d(TAG, "💾 Committing separator: '$charStr' (code=$code)")
                ic.commitText(charStr, 1)
            }
            return true
        }
        
        val original = match.groupValues[1]
        Log.d(TAG, "🔍 Found word: '$original' (length=${original.length})")
        
        // ✅ CHECK FOR CUSTOM DICTIONARY EXPANSION FIRST (before autocorrect!)
        if (dictionaryEnabled) {
            val expansion = dictionaryManager.getExpansion(original)
            if (expansion != null) {
                val expandedText = expansion.expansion
                val separator = if (code > 0) String(Character.toChars(code)) else " "
                
                ic.beginBatchEdit()
                ic.deleteSurroundingText(original.length, 0)
                ic.commitText("$expandedText$separator", 1)
                ic.endBatchEdit()
                
                dictionaryManager.incrementUsage(original)
                
                // ✅ Add expanded text to word history for next-word prediction
                wordHistory.add(expandedText)
                if (wordHistory.size > 20) {
                    wordHistory.removeAt(0)
                }
                
                Log.d(TAG, "⚙️ Shortcut expanded: '$original' → '$expandedText'")
                Log.d(TAG, "📚 Added '$expandedText' to word history (size=${wordHistory.size})")
                return true  // Expansion done, don't run autocorrect
            }
        }
        
        if (original.isEmpty() || original.length < 2) {
            // Too short to correct, just commit separator
            Log.d(TAG, "🔍 Word too short to correct")
            if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
            return true
        }
        
        val autocorrectEnabled = isAutoCorrectEnabled()
        Log.d(TAG, "🔍 applyAutocorrectOnSeparator: autocorrect=$autocorrectEnabled, code=$code")
        
        if (!autocorrectEnabled) {
            Log.w(TAG, "⚠️ Autocorrect is DISABLED in settings")
            // Still commit the separator character
            if (code > 0) {
                val charStr = String(Character.toChars(code))
                Log.d(TAG, "💾 Committing separator (autocorrect OFF): '$charStr' (code=$code)")
                ic.commitText(charStr, 1)
            }
            return true
        }
        
        // Check if unified autocorrect engine is ready (Firebase data loaded)
        if (!autocorrectEngine.hasLanguage(currentLanguage)) {
            Log.w(TAG, "⚠️ UnifiedAutocorrectEngine not ready for $currentLanguage - Firebase data not loaded")
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
                
                // ✅ Add original word to history for next-word prediction
                wordHistory.add(original)
                if (wordHistory.size > 20) {
                    wordHistory.removeAt(0)
                }
                Log.d(TAG, "📚 Added '$original' to word history (size=${wordHistory.size})")
                
                // No suggestion; just commit the separator
                if (code > 0) ic.commitText(String(Character.toChars(code)), 1)
                return true
            }
            
            // Check if user previously rejected this correction
            if (correctionRejected && lastCorrection?.first.equals(original, ignoreCase = true)) {
                Log.d(TAG, "🚫 Skipping autocorrect — user rejected previous correction for '$original'")
                correctionRejected = false
                lastCorrection = null
                
                // ✅ Add original word to history for next-word prediction
                wordHistory.add(original)
                if (wordHistory.size > 20) {
                    wordHistory.removeAt(0)
                }
                Log.d(TAG, "📚 Added '$original' to word history (size=${wordHistory.size})")
                
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
                
                // ✅ Add corrected word to history for next-word prediction
                wordHistory.add(replaced)
                if (wordHistory.size > 20) {
                    wordHistory.removeAt(0)
                }
                Log.d(TAG, "📚 Added '$replaced' to word history (size=${wordHistory.size})")
                
                // 🔥 CRITICAL: Learn from this correction for adaptive improvement
                try {
                    autocorrectEngine.onCorrectionAccepted(original, best, currentLanguage)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error learning from correction", e)
                }
            } else {
                // ✅ No replacement, but still add original word to history
                wordHistory.add(original)
                if (wordHistory.size > 20) {
                    wordHistory.removeAt(0)
                }
                Log.d(TAG, "📚 Added '$original' to word history (size=${wordHistory.size})")
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
    
    
    // For double-backspace revert functionality
    private var lastBackspaceTime = 0L
    
    private fun handleBackspace(ic: InputConnection) {
        val currentTime = System.currentTimeMillis()
        
        // Handle Gboard-style undo autocorrect on first backspace
        // ✅ FIX: Use safe call instead of !!
        if (undoAvailable) {
            lastCorrection?.let { (original, corrected) ->
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
                    
                    // Phase 5: Learn from rejection
                    onCorrectionRejected(original, corrected)
                    
                    // Update suggestions
                    if (aiSuggestionsEnabled) {
                        updateAISuggestions()
                    }
                    return
                }
            }
        }
        
        // Handle slide-to-delete mode
        if (isSlideToDeleteModeActive) {
            deleteLastWord(ic)
            return
        }
        
        lastBackspaceTime = System.currentTimeMillis()
        
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
        // ✅ FIX: Use safe call instead of !!
        if (!correctionRejected) {
            lastCorrection?.let { (original, corrected) ->
                val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
                
                // If user is deleting characters from the corrected word, mark as rejected
                if (textBefore.contains(original.take(2)) || currentWord.startsWith(original.take(2))) {
                    correctionRejected = true
                    undoAvailable = false
                    Log.d(TAG, "🚫 User manually rejected autocorrect '$original' → '$corrected'")
                    
                    // Phase 5: Learn from rejection
                    onCorrectionRejected(original, corrected)
                    
                    lastCorrection = null
                }
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
        
        // Enhanced auto-capitalization via unified controller
        if (::unifiedController.isInitialized) {
            unifiedController.handleSpacePress()
        } else if (::capsShiftManager.isInitialized) {
            val inputType = currentInputEditorInfo?.inputType ?: 0
            capsShiftManager.handleSpacePress(ic, inputType)
        } else {
            // Fallback to old implementation
            caps = shouldCapitalizeAfterSpace(ic)
            keyboardView?.isShifted = caps
        }
        
        updateAISuggestions()
    }
    
    // ==================== PHASE 3: WORD LEARNING INTEGRATION ====================
    
    /**
     * Called when a word is committed (typed and accepted)
     * Phase 3: Automatic word learning for personalization
     */
    private fun onWordCommitted(word: String) {
        if (word.isBlank() || word.length < 2) return
        
        try {
            if (::userDictionaryManager.isInitialized) {
                userDictionaryManager.learnWord(word)
                Log.d(TAG, "✨ Learned word: '$word'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to learn word", e)
        }
    }
    
    /**
     * Phase 5: Called when user accepts an autocorrect suggestion
     * Positive learning for personalization
     */
    private fun onCorrectionAccepted(original: String, corrected: String) {
        try {
            if (::userDictionaryManager.isInitialized) {
                // Learn the corrected word
                userDictionaryManager.learnWord(corrected)
                
                // Also tell autocorrect engine
                if (::autocorrectEngine.isInitialized) {
                    autocorrectEngine.learnFromUser(original, corrected, currentLanguage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process accepted correction", e)
        }
    }
    
    /**
     * Phase 5: Called when user rejects an autocorrect suggestion
     * Negative learning - blacklists the correction
     */
    private fun onCorrectionRejected(original: String, corrected: String) {
        try {
            if (::userDictionaryManager.isInitialized) {
                userDictionaryManager.blacklistCorrection(original, corrected)
                
                // Learn the original word as valid
                userDictionaryManager.learnWord(original)
                
                // Tell autocorrect engine
                if (::autocorrectEngine.isInitialized) {
                    autocorrectEngine.learnFromUser(original, original, currentLanguage)
                }
                
                Log.d(TAG, "🚫 Rejected correction: '$original' ≠ '$corrected'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process rejected correction", e)
        }
    }
    
    // ==================== END PHASE 3 & 5 ====================
    
    private fun finishCurrentWord() {
        if (currentWord.isEmpty()) return
        
        // Process word with enhanced autocorrect pipeline
        coroutineScope.launch {
            try {
                val prev1 = if (wordHistory.isNotEmpty()) wordHistory.last() else ""
                val prev2 = if (wordHistory.size >= 2) wordHistory[wordHistory.size - 2] else ""
                
                val context = listOfNotNull(prev1, prev2).filter { it.isNotBlank() }
                
                // Get suggestions for the current word
                val allSuggestions = autocorrectEngine.getCorrections(currentWord, currentLanguage, context)
                
                // Phase 5: Filter out blacklisted corrections
                val suggestions = if (::userDictionaryManager.isInitialized) {
                    allSuggestions.filter { suggestion ->
                        !userDictionaryManager.isBlacklisted(currentWord, suggestion.text)
                    }.also { filtered ->
                        val blacklistedCount = allSuggestions.size - filtered.size
                        if (blacklistedCount > 0) {
                            Log.d(TAG, "🚫 Filtered $blacklistedCount blacklisted corrections for '$currentWord'")
                        }
                    }
                } else {
                    allSuggestions
                }
                
                val topSuggestion = suggestions.firstOrNull()
                
                if (topSuggestion != null && topSuggestion.source == SuggestionSource.CORRECTION) {
                    // Auto-apply correction
                    val correction = topSuggestion.text
                    withContext(Dispatchers.Main) {
                        val ic = currentInputConnection
                        if (ic != null) {
                            // Replace the current word with correction
                            ic.deleteSurroundingText(currentWord.length, 0)
                            ic.commitText("$correction ", 1)
                            
                            // Show brief underline/highlight feedback
                            showAutocorrectFeedback(correction)
                            
                            // Phase 3 & 5: Learn from accepted correction
                            onCorrectionAccepted(currentWord, correction)
                            
                            Log.d(TAG, "Auto-corrected '$currentWord' → '$correction'")
                        }
                    }
                    
                    // Add corrected word to history
                    wordHistory.add(correction)
                } else {
                    // No auto-correction, add original word
                    wordHistory.add(currentWord)
                    
                    // Phase 3: Learn original word
                    onWordCommitted(currentWord)
                }
                
                // Update suggestion strip with candidates
                withContext(Dispatchers.Main) {
                    // Update suggestion strip with top suggestions
                    val suggestionTexts = suggestions.map { suggestion ->
                        if (suggestion.source == SuggestionSource.CORRECTION) {
                            "✓ ${suggestion.text}" // Mark corrections
                        } else {
                            suggestion.text
                        }
                    }
                    updateSuggestionUI(suggestionTexts)
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
            // Learning moved to UnifiedAI internal processing
            Log.d(TAG, "AI context learning: $currentWord with context: $context")
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
        val context = wordHistory.subList(start, wordHistory.size)
        Log.d(TAG, "🔍 getRecentContext: wordHistory.size=${wordHistory.size}, context=$context")
        return context
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
    
    
    private fun handleShift() {
        // Delegate to unified controller for shift handling
        if (::unifiedController.isInitialized) {
            unifiedController.handleShiftPress()
        } else if (::capsShiftManager.isInitialized) {
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
    }
    
    private fun updateShiftVisualState() {
        keyboardView?.let { view ->
            // Update the shift key visual state
            view.isShifted = (shiftState != SHIFT_OFF)
            
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
                // ✅ Use unified controller for consistent layout loading
                unifiedController.buildAndRender(currentLanguage, LanguageLayoutAdapter.KeyboardMode.LETTERS, showNumberRow)
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
                // ✅ Use unified controller for consistent layout loading
                unifiedController.buildAndRender(currentLanguage, LanguageLayoutAdapter.KeyboardMode.SYMBOLS, false)
            }
            
            KeyboardMode.EXTENDED_SYMBOLS -> {
                // ✅ Use unified controller for consistent layout loading
                unifiedController.buildAndRender(currentLanguage, LanguageLayoutAdapter.KeyboardMode.EXTENDED_SYMBOLS, false)
            }
            
            KeyboardMode.DIALER -> {
                // ✅ Use unified controller for consistent layout loading
                unifiedController.buildAndRender(currentLanguage, LanguageLayoutAdapter.KeyboardMode.DIALER, false)
            }
            
            KeyboardMode.EMOJI -> {
                handleEmojiToggle()
                return  // Don't update currentKeyboardMode yet, handleEmojiToggle will do it
            }
        }
        
        currentKeyboardMode = targetMode
        rebindKeyboardListener()  // Ensure listener is bound after layout change
    }
    
    /**
     * ✅ FIXED: Load dynamic JSON-based layout for a language with proper mode synchronization
     * This ensures key codes are rebuilt correctly when switching modes
     */
    private fun loadDynamicLayout(languageCode: String, mode: LanguageLayoutAdapter.KeyboardMode = LanguageLayoutAdapter.KeyboardMode.LETTERS) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "📱 Loading dynamic layout for: $languageCode, mode: $mode")
                val showNumberRow = getNumberRowEnabled()
                
                mainHandler.post {
                    // ✅ CRITICAL FIX: Use setKeyboardMode instead of setDynamicLayout
                    // This rebuilds the layout with correct key codes for the current mode
                    keyboardView?.let { view ->
                        if (view is SwipeKeyboardView) {
                            view.currentLangCode = languageCode
                            view.setKeyboardMode(mode, languageLayoutAdapter, showNumberRow)
                            view.setCurrentLanguage(languageManager.getLanguageDisplayName(languageCode))
                        }
                    }
                    
                    currentKeyboard = when (mode) {
                        LanguageLayoutAdapter.KeyboardMode.LETTERS -> KEYBOARD_LETTERS
                        LanguageLayoutAdapter.KeyboardMode.SYMBOLS -> KEYBOARD_SYMBOLS
                        LanguageLayoutAdapter.KeyboardMode.EXTENDED_SYMBOLS -> KEYBOARD_SYMBOLS
                        LanguageLayoutAdapter.KeyboardMode.DIALER -> KEYBOARD_NUMBERS
                    }
                    
                    // 🔧 FIX: Rebind swipe listener after layout rebuild
                    rebindKeyboardListener()
                    
                    // Note: Auto-adjust now happens in SwipeKeyboardView.setKeyboardMode() 
                    // after the layout is fully built (avoids race condition)
                    
                    applyTheme()
                    Log.d(TAG, "✅ Dynamic layout loaded for $languageCode (mode: $mode)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load dynamic layout for $languageCode", e)
                // Fallback to legacy XML layout
                mainHandler.post {
                    val lang = languageCode.uppercase()
                    val keyboardResource = getKeyboardResourceForLanguage(lang, showNumberRow)
                    keyboard = Keyboard(this@AIKeyboardService, keyboardResource)
                    currentKeyboard = KEYBOARD_LETTERS
                    keyboardView?.keyboard = keyboard
                    keyboardView?.useLegacyKeyboardMode()
                    rebindKeyboardListener()
                    
                    // 🎯 AUTO-ADJUST: Legacy layout needs manual trigger
                    keyboardView?.post {
                        keyboardContainer?.requestLayout()
                        mainKeyboardLayout?.requestLayout()
                        updateInputViewShown()
                    }
                    
                    applyTheme()
                    Log.w(TAG, "⚠️ Fell back to legacy XML layout for $languageCode")
                }
            }
        }
    }
    
    /**
     * ⚡ PERFORMANCE: Use cached value instead of reading SharedPreferences every time
     * The cache is updated in applyLoadedSettings() when settings change
     */
    private fun getNumberRowEnabled(): Boolean {
        return showNumberRow
    }
    
    /**
     * ✅ FIXED: Load language layout with number row support
     * Enhanced version that respects user settings and properly synchronizes mode
     */
    private suspend fun loadLanguageLayout(langCode: String) {
        val showNumberRow = getNumberRowEnabled()
        
        // ✅ CRITICAL FIX: Use setKeyboardMode to ensure proper key code mapping
        withContext(Dispatchers.Main) {
            keyboardView?.let { view ->
                if (view is SwipeKeyboardView) {
                    view.currentLangCode = langCode
                    view.setKeyboardMode(LanguageLayoutAdapter.KeyboardMode.LETTERS, languageLayoutAdapter, showNumberRow)
                    view.setCurrentLanguage(languageManager.getLanguageDisplayName(langCode))
                    view.refreshTheme()
                    
                    // 🔧 FIX: Rebind swipe listener after layout rebuild
                    rebindKeyboardListener()
                    
                    // Force the parent container to remeasure and relayout
                    keyboardContainer?.requestLayout()
                    mainKeyboardLayout?.requestLayout()
                    
                    // Update the IME window to recalculate insets with new height
                    updateInputViewShown()
                    
                    Log.d(TAG, "✅ Layout loaded for $langCode with numberRow=$showNumberRow")
                }
            }
        }

        autocorrectEngine.preloadLanguages(listOf(langCode))
        Log.d(TAG, "✅ Layout loaded for $langCode with numberRow=$showNumberRow")
    }
    
    /**
     * Cycle to next keyboard mode (CleverType behavior)
     * Letters → Symbols → Extended Symbols → Dialer → Letters
     */
    private fun cycleKeyboardMode() {
        val nextMode = when (currentKeyboardMode) {
            KeyboardMode.LETTERS -> KeyboardMode.SYMBOLS
            KeyboardMode.NUMBERS -> KeyboardMode.SYMBOLS
            KeyboardMode.SYMBOLS -> KeyboardMode.EXTENDED_SYMBOLS
            KeyboardMode.EXTENDED_SYMBOLS -> KeyboardMode.DIALER
            KeyboardMode.DIALER -> KeyboardMode.LETTERS
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
    
    // REMOVED: Deprecated switchToSymbols() - use switchKeyboardMode(KeyboardMode.SYMBOLS) instead
    
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
    
    // REMOVED: Deprecated switchToLetters() and switchToNumbers() - use switchKeyboardMode() instead
    
    private fun handleLanguageSwitch() {
        try {
            // Cycle to next language
            // Language cycling is now handled by cycleLanguage() method
            // This legacy method is kept for compatibility
            
            // Save current language index
            settings.edit().putInt("current_language_index", currentLanguageIndex).apply()
            
            // Reload keyboard layout to reflect language change
            switchKeyboardMode(KeyboardMode.LETTERS)
            
            // switchKeyboardMode() already calls rebindKeyboardListener()
            
            // Show language change feedback
            Toast.makeText(this, "🌐 $currentLanguage", Toast.LENGTH_SHORT).show()
            
            Log.d(TAG, "Language cycled to: $currentLanguage (index: $currentLanguageIndex)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cycling language", e)
            Toast.makeText(this, "🌐 Language cycle failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    
    
    // PERFORMANCE: Debounced wrapper for suggestion updates
    private fun updateAISuggestions() {
        // Guard: Check if suggestion container is ready
        if (suggestionContainer == null) {
            Log.w(TAG, "Suggestion container not ready, skipping update")
            return
        }
        
        if (!shouldUpdateAISuggestions()) return
        
        // Cancel previous job to debounce
        suggestionUpdateJob?.cancel()
        suggestionUpdateJob = coroutineScope.launch {
            delay(suggestionDebounceMs)
            if (isActive) {
                updateAISuggestionsImmediate()
            }
        }
    }
    
    private fun updateAISuggestionsImmediate() {
        // Guard: Check if aiBridge is initialized before use
        if (!::unifiedAIService.isInitialized) {
            Log.w(TAG, "⚠️ AI Bridge not initialized yet, skipping AI suggestions for '$currentWord'")
            return
        }
        
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
        
        val word = currentWord.trim()
        
        // ✅ NEXT-WORD PREDICTION: Check BEFORE AI suggestions (works even if AI disabled)
        if (word.isEmpty()) {
            Log.d(TAG, "🔍 Empty word detected - checking for next-word predictions")
            
            if (::nextWordPredictor.isInitialized && ::autocorrectEngine.isInitialized) {
                try {
                    val context = getRecentContext()
                    Log.d(TAG, "🔍 Context for predictions: $context")
                    
                    // Check if unified autocorrect engine is ready (Firebase data loaded)
                    if (!autocorrectEngine.hasLanguage(currentLanguage)) {
                        Log.w(TAG, "⚠️ UnifiedAutocorrectEngine not ready for $currentLanguage - Firebase data may not be loaded")
                        clearSuggestions()
                        return
                    }
                    
                    if (context.isNotEmpty()) {
                        Log.d(TAG, "🔮 Getting next-word predictions for context: $context")
                        val predictions = nextWordPredictor.getPredictions(context, currentLanguage, 3)
                        
                        if (predictions.isNotEmpty()) {
                            Log.d(TAG, "📊 Next-word predictions: $predictions")
                            updateSuggestionUI(predictions)
                            return
                        } else {
                            Log.d(TAG, "⚠️ No next-word predictions available for context: $context")
                        }
                    } else {
                        Log.d(TAG, "⚠️ Context is empty, no previous words to predict from")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error getting next-word predictions", e)
                    e.printStackTrace()
                }
            } else {
                Log.w(TAG, "⚠️ NextWordPredictor or AutocorrectEngine not initialized")
            }
            
            clearSuggestions()
            return
        }
        
        // Now check AI suggestions setting (only affects AI, not next-word)
        if (!aiSuggestionsEnabled) {
            Log.d(TAG, "AI suggestions disabled in settings (next-word still works)")
            clearSuggestions()
            return
        }
        
        // Check cache first for instant performance
        val cachedSuggestions = suggestionCache[word]
        if (cachedSuggestions != null) {
            updateSuggestionUI(cachedSuggestions)
            return
        }
        
        try {
            // ✅ ALWAYS try local autocorrect first for instant suggestions (like "teh → the")
            val localSuggestions = if (ensureEngineReady()) {
                autocorrectEngine.getCorrections(word, currentLanguage).map { it.text }
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
                    
                    // Use UnifiedAI service for enhanced suggestions (simplified approach)
                    coroutineScope.launch(Dispatchers.Main) {
                        try {
                            // For now, just show the fallback suggestions and log that UnifiedAI is available
                            if (::unifiedAIService.isInitialized && unifiedAIService.isReady()) {
                                Log.d(TAG, "🧠 UnifiedAI Service ready for suggestions: $word")
                                // Could add AI-enhanced suggestions here in future versions
                            }
                            
                            // Cache with size limit to prevent memory bloat
                            if (suggestionCache.size > 50) suggestionCache.clear()
                            suggestionCache[word] = fallbackSuggestions
                            
                            updateSuggestionUI(fallbackSuggestions)
                        } catch (e: Exception) {
                            Log.w(TAG, "Suggestion error: ${e.message}")
                            updateSuggestionUI(fallbackSuggestions)
                        }
                    }
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
     * Generate suggestions using UNIFIED FIREBASE ENGINE ONLY
     * No more fallback to hardcoded words - Firebase-only approach
     */
    private fun generateEnhancedBasicSuggestions(currentWord: String): List<String> {
        try {
            // Get surrounding text context for previous word extraction
            val surrounding = getInputTextAroundCursor(50)
            val prevToken = surrounding.trimEnd().split(Regex("\\s+")).dropLast(1).lastOrNull()
            
            // Get current language - use AIKeyboardService's currentLanguage for consistency
            val lang = currentLanguage.lowercase()
            
            Log.d(TAG, "Firebase suggestions: currentWord='$currentWord', prevToken='$prevToken', lang='$lang' (from currentLanguage field)")
            
            // Build word candidates ONLY from UnifiedAutocorrectEngine (Firebase data)
            val wordCandidates = mutableListOf<Pair<String, Double>>()
            
            // Check if unified engine is ready
            if (!autocorrectEngine.hasLanguage(lang)) {
                Log.w(TAG, "⚠️ UnifiedAutocorrectEngine not ready for $lang - no Firebase data available")
                return emptyList() // Firebase-only approach - no fallback
            }
            
            // 1. For current word typing - use UnifiedAutocorrectEngine (Firebase only)
            if (currentWord.isNotEmpty()) {
                try {
                    val typingSuggestions = autocorrectEngine.getSuggestionsFor(currentWord)
                    wordCandidates.addAll(typingSuggestions.mapIndexed { idx, word -> word to (0.9 - idx * 0.1) })
                    Log.d(TAG, "Firebase typing suggestions: ${typingSuggestions.size} for '$currentWord'")
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase typing suggestions failed: ${e.message}")
                }
            }
            
            // 2. For context predictions - use UnifiedAutocorrectEngine (Firebase only)
            if (currentWord.isEmpty() && prevToken != null) {
                try {
                    val contextPredictions = autocorrectEngine.getNextWordPredictions(listOf(prevToken))
                    wordCandidates.addAll(contextPredictions.mapIndexed { idx, word -> word to (0.8 - idx * 0.1) })
                    Log.d(TAG, "Firebase context predictions: ${contextPredictions.size} for '$prevToken'")
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase context predictions failed: ${e.message}")
                }
            }
            
            // NO FALLBACK TO HARDCODED WORDS - Firebase-only approach
            if (wordCandidates.isEmpty()) {
                Log.d(TAG, "No Firebase suggestions available for '$currentWord' in language '$lang'")
                return emptyList()
            }
            
            Log.d(TAG, "Firebase suggestion candidates: ${wordCandidates.size}")
            
            // Get emoji candidates (existing system)
            val emojiCandidates = try {
                val emojiSuggestions = EmojiSuggestionEngine.getSuggestionsForTyping(currentWord, surrounding)
                emojiSuggestions.mapIndexed { idx, emoji -> emoji to (0.3 - idx * 0.05) }
            } catch (e: Exception) {
                Log.w(TAG, "Emoji suggestions failed: ${e.message}")
                emptyList()
            }
            
            // Use SuggestionRanker to merge with context-aware emoji gating
            val finalSuggestions = try {
                com.example.ai_keyboard.predict.SuggestionRanker.mergeForStrip(
                    currentWord = currentWord,
                    contextPrev = prevToken,
                    wordCands = wordCandidates,
                    emojiCands = emojiCandidates,
                    max = 5
                )
            } catch (e: Exception) {
                Log.w(TAG, "SuggestionRanker failed: ${e.message}")
                // Manually merge - take top 3 word candidates
                wordCandidates.sortedByDescending { it.second }.take(3).map { it.first }
            }
            
            // Firebase-only approach - return exactly what Firebase provides
            val result = finalSuggestions
            
            Log.d(TAG, "Firebase prediction results: ${result.joinToString(", ")}")
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in Firebase prediction engine", e)
            // Firebase-only approach - no hardcoded fallbacks
            return emptyList()
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
    
    // OPTIMIZED: Fast suggestion UI update (main-thread safe)
    private fun updateSuggestionUI(suggestions: List<String>) {
        try {
            // Ensure UI updates happen on main thread to avoid "Posting sync barrier on non-owner thread" warnings
            mainHandler.post {
                suggestionContainer?.let { container ->
                    val childCount = minOf(container.childCount, 3)
                    
                    for (i in 0 until childCount) {
                        val suggestionView = container.getChildAt(i) as? TextView
                        suggestionView?.apply {
                            text = suggestions.getOrNull(i) ?: ""
                            visibility = if (text.isNotEmpty()) View.VISIBLE else View.INVISIBLE
                            // Ensure consistent text color
                            val palette = themeManager.getCurrentPalette()
                            setTextColor(palette.keyText)
                        }
                    }
                    
                    Log.d(TAG, "Updated suggestion UI: ${suggestions.take(3)}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to update suggestion UI", e)
        }
    }
    
    private fun clearSuggestions() {
        safeMain {
            suggestionContainer?.let { container ->
                for (i in 0 until container.childCount) {
                    (container.getChildAt(i) as? TextView)?.text = ""
                }
            }
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
                        // Learning moved to UnifiedAI internal processing
                        Log.d(TAG, "AI correction learning: $currentWord -> $cleanSuggestion")
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
                // Find and store the key object for positioning the popup
                currentLongPressKeyObject = view.keyboard?.keys?.find { key ->
                    key.codes.firstOrNull() == primaryCode
                }
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
            currentLongPressKeyObject = null
        }
        
        // Hide key preview but keep accent options if they're showing
        if (accentPopup?.isShowing != true) {
            hideKeyPreview()
        }
        
        // Don't hide accent popup here - let user tap to select or tap outside to dismiss
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
                                // ✅ FIX: Use safe call instead of !!
                                val romanText = transliterationEngine?.reverseTransliterate(nativeText)
                                if (romanText != null) {
                                    // Delete buffered native text
                                    ic.deleteSurroundingText(romanBuffer.length, 0)
                                    
                                    // Insert Roman text + punctuation
                                    ic.commitText("$romanText${lastChar}", 1)
                                    
                                    Log.d(TAG, "🔄 Reverse transliterated: '$nativeText' → '$romanText'")
                                } else {
                                    ic.commitText(it, 1)
                                }
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
            // ✅ FIX: Use safe call instead of !!
            if (transliterationEnabled && 
                currentLanguage in listOf("hi", "te", "ta") &&
                transliterationEngine != null) {
                
                val isRomanInput = transliterationEngine?.isRomanInput(it.toString()) ?: false
                
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
                                // ✅ FIX: Use safe call instead of !!
                                val nativeText = transliterationEngine?.transliterate(romanText)
                                
                                if (nativeText != null) {
                                    // Delete the buffered Roman text
                                    ic.deleteSurroundingText(romanBuffer.length, 0)
                                    
                                    // Insert transliterated text + punctuation
                                    ic.commitText("$nativeText$lastChar", 1)
                                    
                                    Log.d(TAG, "🔤 Transliterated: '$romanText' → '$nativeText'")
                                }
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
            // ✅ FIX: Use safe call instead of !!
            currentInputConnection?.let { handleBackspace(it) }
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
        
        // ✅ UNIFIED THEMING: Don't change background during swipe
        // Visual feedback handled by swipe trail, not background color
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
    
    /**
     * Generate a simple fallback when swipe decoder returns nothing
     */
    private fun generateSwipeFallback(swipePath: List<Pair<Float, Float>>): String {
        if (swipePath.isEmpty()) return "swipe"
        
        // Simple heuristic: map to rough QWERTY regions
        val letters = swipePath.mapNotNull { (x, y) ->
            when {
                y < 0.33f -> { // Top row
                    when {
                        x < 0.2f -> 'q'
                        x < 0.4f -> 'w' 
                        x < 0.6f -> 'e'
                        x < 0.8f -> 'r'
                        else -> 't'
                    }
                }
                y < 0.66f -> { // Middle row
                    when {
                        x < 0.25f -> 'a'
                        x < 0.5f -> 's'
                        x < 0.75f -> 'd'
                        else -> 'f'
                    }
                }
                else -> { // Bottom row
                    when {
                        x < 0.33f -> 'z'
                        x < 0.66f -> 'x'
                        else -> 'c'
                    }
                }
            }
        }
        
        // Collapse consecutive same letters
        val collapsed = StringBuilder()
        var prev: Char? = null
        for (c in letters) {
            if (c != prev) {
                collapsed.append(c)
                prev = c
            }
        }
        
        return if (collapsed.length >= 2) collapsed.toString() else "swipe"
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
    
    // Swipe state tracking (using UnifiedAutocorrectEngine directly)
    private var lastCommittedSwipeWord = ""
    
    // Implement SwipeListener interface methods with enhanced autocorrection
    override fun onSwipeDetected(
        swipedKeys: List<Int>, 
        swipePattern: String, 
        keySequence: List<Int>,
        swipePath: List<Pair<Float, Float>>
    ) {
        if (keySequence.isEmpty()) return
        
        // ✅ Safety check: Ensure unified engine is initialized
        if (!::autocorrectEngine.isInitialized) {
            Log.w(TAG, "⚠️ UnifiedAutocorrectEngine not initialized yet - ignoring swipe gesture")
            return
        }
        
        // Skip if path is too short (accidental taps)
        if (swipePath.size < 2) {
            Log.d(TAG, "Swipe path too short (${swipePath.size} points), treating as tap")
            return
        }
        
        Log.d(TAG, "🔍 Processing swipe with ${swipePath.size} coordinate points")
        Log.d("AIKeyboardService", "🧭 onSwipeDetected: pts=${swipePath.size} first=${swipePath.firstOrNull()} last=${swipePath.lastOrNull()}")
        
        coroutineScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                // Get context word for better prediction
                val context = if (wordHistory.isNotEmpty()) listOf(wordHistory.last()) else emptyList()
                
                // Use UnifiedAutocorrectEngine for swipe decoding directly
                val swipePathObj = SwipePath(swipePath)
                val swipeSuggestions = autocorrectEngine.suggestForSwipe(swipePathObj, context)
                val candidates = swipeSuggestions.map { it.text }
                    
                val processingTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "🔍 Swipe decoding took ${processingTime}ms, got ${candidates.size} candidates")
                
                withContext(Dispatchers.Main) {
                    // GUARANTEE: Never leave user with empty results
                    val finalCandidates = if (candidates.isNotEmpty()) {
                        candidates
                    } else {
                        // Last resort: create a simple fallback based on swipe path
                        val fallback = generateSwipeFallback(swipePath)
                        Log.w("AIKeyboardService", "⚠️ Swipe decoder empty — emitting fallback='$fallback'")
                        listOf(fallback)
                    }
                    
                    val bestCandidate = finalCandidates.first()
                    
                    // Auto-commit the best candidate  
                    currentInputConnection?.let { ic ->
                        ic.commitText("$bestCandidate ", 1)
                        lastCommittedSwipeWord = bestCandidate
                    }
                    
                    // Update word history for context
                    wordHistory.add(bestCandidate)
                    if (wordHistory.size > 20) {
                        wordHistory.removeAt(0)
                    }
                        
                    // Show alternatives in suggestion strip
                    updateSuggestionUI(finalCandidates.take(3))
                        
                    Log.d(TAG, "✅ Swipe decoded: path(${swipePath.size} points) → '$bestCandidate' (${finalCandidates.size} alternatives, ${processingTime}ms)")
                    
                    // Update AI suggestions for next word prediction
                    updateAISuggestions()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in swipe path processing", e)
                // NO FALLBACK - let user try again or tap individual keys
                withContext(Dispatchers.Main) {
                    updateSuggestionUI(emptyList())
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
                
                // Learn from user selection (using UserDictionaryManager)
                if (::userDictionaryManager.isInitialized) {
                    userDictionaryManager.learnWord(selectedWord)
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
            if (lastCommittedSwipeWord.isNotEmpty()) {
                // Record that user rejected the correction (using UserDictionaryManager if needed)
                // For now, just clear the last committed word
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
            
            // ✅ UNIFIED THEMING: Don't change background during swipe
            // Visual feedback handled by swipe trail, not background color
            
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
            // ✅ UNIFIED THEMING: Restore transparent background
            keyboardView?.setBackgroundColor(Color.TRANSPARENT)
            
            // Hide swipe mode indicator
            showSwipeIndicator(false)
        } catch (e: Exception) {
            // Ignore swipe end errors
        }
    }
    
    private fun showSwipeSuccess(word: String, confidence: Double = 0.0) {
        // Show the swiped word in the first suggestion slot briefly (no icons)
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as TextView
                firstSuggestion.apply {
                    text = word // Remove checkmark icon
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
        // Don't show swipe indicator icons, but keep middle suggestion visible
        // This function no longer sets any swipe indicators to keep suggestions clean
        // Middle suggestion will be handled by normal suggestion update process
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
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        
        // Use KeyboardHeightManager to ensure consistent height
        val keyboardHeight = keyboardHeightManager.calculateKeyboardHeight(
            includeToolbar = true,
            includeSuggestions = true
        )
        
        // Apply consistent height to main layout
        mainKeyboardLayout?.post {
            mainKeyboardLayout?.layoutParams?.height = keyboardHeight
            mainKeyboardLayout?.requestLayout()
            Log.d(TAG, "[KeyboardHeightManager] Applied keyboard height: ${keyboardHeight}px")
        }
        
        // Height is now managed by keyboard container - no manual adjustment needed
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        
        // Apply CleverType config on keyboard activation
        applyConfig()
        
        // 🔁 Trigger Unified AI Suggestion update on typing start
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val beforeCursor = currentInputConnection?.getTextBeforeCursor(50, 0)?.toString() ?: ""
                val words = beforeCursor.split(" ").filter { it.isNotBlank() }
                val currentWord = words.lastOrNull() ?: ""
                val contextWords = if (words.size > 1) words.dropLast(1) else emptyList()
                updateAISuggestions(currentWord, contextWords)
            } catch (e: Exception) {
                Log.e("AIKeyboardService", "⚠️ Failed to trigger updateAISuggestions", e)
            }
        }
        
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
            if (!autocorrectEngine.hasLanguage(currentLang)) {
                Log.w(TAG, "⚠️ Dictionary for $currentLang not loaded yet, deferring suggestions")
                coroutineScope.launch {
                    // Wait up to 1 second for dictionary to load
                    var retries = 0
                    while (!autocorrectEngine.hasLanguage(currentLang) && retries < 10) {
                        delay(100)
                        retries++
                    }
                    withContext(Dispatchers.Main) {
                        if (autocorrectEngine.hasLanguage(currentLang)) {
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
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Recalculate keyboard height for new configuration
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        val newHeight = keyboardHeightManager.calculateKeyboardHeight()
        
        // Update keyboard height
        mainKeyboardLayout?.layoutParams?.height = newHeight
        mainKeyboardLayout?.requestLayout()
        
        // Height is now managed by keyboard container - no manual adjustment needed
        
        Log.d(TAG, "[KeyboardHeightManager] Configuration changed - Landscape: $isLandscape, Height: $newHeight")
        
        // Reinitialize keyboard for new configuration
        keyboardView?.let { view ->
            val keyboardResource = getKeyboardResourceForLanguage(currentLanguage.uppercase(), showNumberRow)
            val newKeyboard = Keyboard(this, keyboardResource)
            view.keyboard = newKeyboard
            view.setKeyboard(newKeyboard)
            keyboard = newKeyboard
        }
    }
    
    // REMOVED: Duplicate clearSuggestions() - using optimized version from line 4458
    
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
        
        // Clear singleton instance
        instance = null
        
        // Cleanup AI service
        try {
            if (::advancedAIService.isInitialized) {
                advancedAIService.cleanup()
                Log.d(TAG, "AI service cleaned up")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AI service", e)
        }
        
        // Cancel coroutine scope
        coroutineScope.cancel()
        
        // Cleanup unified layout controller
        try {
            if (::unifiedController.isInitialized) {
                unifiedController.clear()
                Log.d(TAG, "Unified layout controller cleaned up")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up unified layout controller", e)
        }
        
        // Unregister broadcast receivers
        try {
            unregisterReceiver(promptReceiver)
            Log.d(TAG, "Prompt receiver unregistered")
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Prompt receiver was not registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering prompt receiver", e)
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
        // Clean up UnifiedAI service
        if (::unifiedAIService.isInitialized) {
            unifiedAIService.cleanup()
        }
        
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
                switchKeyboardMode(KeyboardMode.LETTERS)  // This will apply number row and language changes
                Log.d(TAG, "Keyboard layout reloaded - NumberRow: $showNumberRow, Language: $currentLanguage")
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
                
                // Reload settings using unified settings loader
                applyLoadedSettings(settingsManager.loadAll())
                
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
                setTextColor(themeManager.getTextColor())
                setBackgroundColor(themeManager.getKeyColor()) // Use theme key background
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
                setBackgroundColor(themeManager.getKeyboardBackgroundColor()) // Use theme background
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
                setBackgroundDrawable(ColorDrawable(themeManager.getKeyboardBackgroundColor())) // Use theme background
                
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
                        currentLongPressKeyObject = null
                        longPressHandler.removeCallbacks(longPressRunnable)
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }
            }
            
            // Measure the popup to get its dimensions
            container.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = container.measuredWidth
            val popupHeight = container.measuredHeight
            
            // Show popup above the pressed key if we have its position
            keyboardView?.let { view ->
                try {
                    val pressedKey = currentLongPressKeyObject
                    if (pressedKey != null) {
                        // Calculate position: center horizontally above the pressed key
                        val location = IntArray(2)
                        view.getLocationInWindow(location)
                        
                        val popupX = location[0] + pressedKey.x + (pressedKey.width / 2) - (popupWidth / 2)
                        val popupY = location[1] + pressedKey.y - popupHeight - 10 // 10px above the key
                        
                        accentPopup?.showAtLocation(view, Gravity.NO_GRAVITY, popupX, popupY)
                    } else {
                        // Fallback: show at top center if key position unknown
                        accentPopup?.showAsDropDown(view, 0, -view.height - 150, Gravity.CENTER_HORIZONTAL)
                    }
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
            setTextColor(themeManager.getTextColor())
            setPadding(16, 12, 16, 12)
            setBackgroundColor(themeManager.getKeyColor()) // Use theme key color for buttons
            
            // Use touch listener instead of click listener for better control
            setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        setBackgroundColor(themeManager.getAccentColor()) // Visual feedback with accent
                        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        setBackgroundColor(themeManager.getKeyColor()) // Reset to theme key color
                        
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
    
    /**
     * Initialize Enhanced Caps/Shift Manager (direct method for compatibility)
     */
    private fun initializeCapsShiftManagerDirect() {
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
            
            Log.d(TAG, "Enhanced Caps/Shift Manager initialized successfully (direct)")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Caps/Shift Manager", e)
        }
    }
    
    /**
     * Initialize Enhanced Caps/Shift Manager (legacy method - kept for compatibility)
     */
    private fun initializeCapsShiftManager() {
        initializeCapsShiftManagerDirect()
    }
    
    /**
     * Update visual state based on new caps/shift state
     */
    private fun updateShiftVisualState(newState: Int) {
        keyboardView?.let { view ->
            // Update the shift key visual state
            view.isShifted = (newState != CapsShiftManager.STATE_NORMAL)
            
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
                    KEYBOARD_LETTERS -> switchKeyboardMode(KeyboardMode.SYMBOLS)
                    KEYBOARD_SYMBOLS -> switchKeyboardMode(KeyboardMode.NUMBERS)
                    KEYBOARD_NUMBERS -> switchKeyboardMode(KeyboardMode.LETTERS)
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
            isEmojiPanelVisible = !isEmojiPanelVisible
            keyboardContainer?.let { container ->
                container.removeAllViews()
                
                if (isEmojiPanelVisible) {
                    // Save current mode before showing emoji
                    previousKeyboardMode = currentKeyboardMode
                    currentKeyboardMode = KeyboardMode.EMOJI
                    
                    // Use new XML-based emoji panel with fixed toolbar
                    // ✅ FIX: Use safe call instead of !!
                    if (emojiPanelView == null) {
                        emojiPanelView = emojiPanelController?.inflate(container)
                    }
                    
                    emojiPanelView?.let { panel ->
                        // Use the height already set in inflate() method
                        // (calculated to match letters keyboard height)
                        container.addView(panel)
                        
                        // Apply theme to emoji panel for consistent look
                        emojiPanelController?.applyTheme()
                        
                        // Force theme application after brief delay to override XML colors
                        mainHandler.postDelayed({
                            emojiPanelController?.applyTheme()
                            Log.d(TAG, "🎨 Emoji panel theme refreshed")
                        }, 150)
                        
                        Log.d(TAG, "😊 Showing emoji panel (saved previous mode: $previousKeyboardMode)")
                    }
                    
                    // Hide toolbar when emoji panel is active
                    suggestionContainer?.visibility = View.GONE
                    topContainer?.findViewById<LinearLayout>(R.id.keyboard_toolbar_simple)?.visibility = View.GONE
                } else {
                    // Cleanup emoji panel popups before hiding
                    // gboardEmojiPanel removed - using EmojiPanelController only
                    
                    // Return to previous keyboard mode
                    currentKeyboardMode = previousKeyboardMode
                    
                    // Show keyboard
                    keyboardView?.let { kv ->
                        container.addView(kv)
                        Log.d(TAG, "🔤 Returning to keyboard mode: $currentKeyboardMode")
                    }
                    
                    // Show suggestions and toolbar when returning to keyboard
                    suggestionContainer?.visibility = View.VISIBLE
                    topContainer?.findViewById<LinearLayout>(R.id.keyboard_toolbar_simple)?.visibility = View.VISIBLE
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
            
            // Enhanced auto-capitalization after enter via unified controller
            if (::unifiedController.isInitialized) {
                unifiedController.handleEnterPress()
            } else if (::capsShiftManager.isInitialized) {
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
     * Close AI Panel and return to keyboard
     */
    private fun closeAIPanel() {
        try {
            Log.d(TAG, "Closing AI Panel")
            
            // Remove AI panel from container
            keyboardContainer?.removeView(aiPanel)
            aiPanel?.visibility = View.GONE
            
            // Restore keyboard view
            keyboardView?.visibility = View.VISIBLE
            keyboardView?.let { keyboardContainer?.addView(it) }
            
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
     * Update AI panels with new custom prompt information
     * Called when AI_PROMPT_UPDATED or SETTINGS_CHANGED broadcast is received
     */
    private fun updateAIPanelsWithNewPrompt() {
        // Custom prompt functionality removed
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
    
    // ========================================
    // AI HELPER FUNCTIONS
    // ========================================
    
    /**
     * Replace current text with AI-generated text
     */
    private fun replaceWithAIText(newText: String) {
        val ic = currentInputConnection ?: return
        try {
            // Get text length to delete
            val textBefore = ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
            
            // Delete current text
            if (textBefore.isNotEmpty()) {
                ic.deleteSurroundingText(textBefore.length, 0)
            }
            
            // Insert new text
            ic.commitText(newText, 1)
            
            Log.d(TAG, "✅ Text replaced successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing text", e)
            Toast.makeText(this, "Error replacing text", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show AI processing error
     */
    private fun showAIError(result: AdvancedAIService.AIResult, outputView: TextView?) {
        when {
            result.error?.contains("network", ignoreCase = true) == true -> {
                outputView?.text = "❌ No internet connection"
                Toast.makeText(this, "Please check your internet connection", Toast.LENGTH_LONG).show()
            }
            result.error?.contains("rate limit", ignoreCase = true) == true -> {
                outputView?.text = "❌ Rate limit reached"
                Toast.makeText(this, "Too many requests. Please wait a moment.", Toast.LENGTH_LONG).show()
            }
            result.error?.contains("API key", ignoreCase = true) == true -> {
                outputView?.text = "❌ API key not configured"
                Toast.makeText(this, "Please configure your OpenAI API key in settings", Toast.LENGTH_LONG).show()
            }
            else -> {
                outputView?.text = "❌ ${result.error}"
                Toast.makeText(this, result.error ?: "Unknown error", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // ========================================
    // UNIFIED FEATURE PANEL SYSTEM
    // ========================================
    
    /**
     * Show unified feature panel (replaces keyboard with dynamic panel)
     * This is the single entry point for Grammar, Tone, AI Assistant, and Clipboard
     */
    private fun showFeaturePanel(type: PanelType) {
        try {
            Log.d(TAG, "Opening feature panel: $type")
            
            // Close any other open panels
            if (isAIPanelVisible) {
                aiPanel?.visibility = View.GONE
                isAIPanelVisible = false
            }
            if (isMiniSettingsVisible) {
                isMiniSettingsVisible = false
            }
            
            // Inflate shared panel layout
            val featurePanel = layoutInflater.inflate(R.layout.panel_feature_shared, null)
            val title = featurePanel.findViewById<TextView>(R.id.panelTitle)
            val rightContainer = featurePanel.findViewById<FrameLayout>(R.id.panelRightContainer)
            val body = featurePanel.findViewById<FrameLayout>(R.id.panelBody)
            val backButton = featurePanel.findViewById<ImageView>(R.id.btnBack)
            
            // Apply theme colors
            val palette = themeManager.getCurrentPalette()
            featurePanel.setBackgroundColor(palette.keyboardBg)
            featurePanel.findViewById<LinearLayout>(R.id.panelHeader)?.setBackgroundColor(palette.toolbarBg)
            title?.setTextColor(palette.keyText)
            
            // Apply arrow drawable with dynamic theming to back button
            backButton?.let {
                val arrowDrawable = ContextCompat.getDrawable(this, R.drawable.arrow)
                arrowDrawable?.let { d ->
                    DrawableCompat.setTint(d, palette.keyText)
                }
                it.setImageDrawable(arrowDrawable)
            }
            // body?.setBackgroundColor(palette.keyboardBg) // Removed to eliminate white bar
            
            // Configure panel based on type
            when (type) {
                PanelType.GRAMMAR_FIX -> {
                    title?.text = "Fix Grammar"
                    val translate = layoutInflater.inflate(R.layout.panel_right_translate, rightContainer, false)
                    rightContainer?.addView(translate)
                    inflateGrammarBody(body)
                }
                PanelType.WORD_TONE -> {
                    title?.text = "Word  Tone"
                    val translate = layoutInflater.inflate(R.layout.panel_right_translate, rightContainer, false)
                    rightContainer?.addView(translate)
                    inflateToneBody(body)
                }
                PanelType.AI_ASSISTANT -> {
                    title?.text = "AI Writing Assistant"
                    val translate = layoutInflater.inflate(R.layout.panel_right_translate, rightContainer, false)
                    rightContainer?.addView(translate)
                    inflateAIAssistantBody(body)
                }
                
                PanelType.CLIPBOARD -> {
                    title?.text = "Clipboard"
                    val toggle = layoutInflater.inflate(R.layout.panel_right_toggle, rightContainer, false)
                    rightContainer?.addView(toggle)
                    inflateClipboardBody(body)
                }
                PanelType.QUICK_SETTINGS -> {
                    title?.text = "Quick Settings"
                    // No right widget for settings
                    inflateQuickSettingsBody(body)
                }
                PanelType.EMOJI -> {
                    title?.text = "Emoji"
                    // No right widget for emoji
                    
                    // Hide the header for emoji panel (remove arrow button and title)
                    featurePanel.findViewById<LinearLayout>(R.id.panelHeader)?.visibility = View.GONE
                    
                    // CRITICAL FIX: Completely disable panel-level scrolling for emoji
                    // Replace the ScrollView wrapper with a LinearLayout to prevent any panel scrolling
                    val scrollParent = featurePanel.findViewById<FrameLayout>(R.id.panelBody)?.parent as? ViewGroup
                    val panelBodyFrame = featurePanel.findViewById<FrameLayout>(R.id.panelBody)
                    
                    if (scrollParent is ScrollView && panelBodyFrame != null) {
                        // Remove panelBody from ScrollView
                        scrollParent.removeView(panelBodyFrame)
                        
                        // Find the parent of ScrollView (should be the main LinearLayout)
                        val mainLayout = scrollParent.parent as? ViewGroup
                        val scrollIndex = mainLayout?.indexOfChild(scrollParent) ?: -1
                        
                        if (mainLayout != null && scrollIndex >= 0) {
                            // Remove ScrollView completely
                            mainLayout.removeView(scrollParent)
                            
                            // Add panelBody directly to main layout with proper weight
                            panelBodyFrame.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                0,
                                1f // Take remaining space
                            )
                            mainLayout.addView(panelBodyFrame, scrollIndex)
                            
                            Log.d(TAG, "🔒 Removed ScrollView wrapper - emoji panel is now FIXED")
                        }
                    }
                    
                    inflateEmojiBody(body)
                }
            }
            
            // Back button handler
            featurePanel.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
                Log.d(TAG, "Back button tapped, restoring keyboard")
                restoreKeyboardFromPanel()
            }
            
            // Switch to panel in container
            // ✅ UNIFIED THEMING: Keep keyboardContainer transparent when showing panels
            keyboardContainer?.setBackgroundColor(Color.TRANSPARENT)
            keyboardContainer?.removeAllViews()
            keyboardContainer?.addView(featurePanel)
            
            // Hide suggestions and toolbar when panel is active
            suggestionContainer?.visibility = View.GONE
            topContainer?.findViewById<LinearLayout>(R.id.keyboard_toolbar_simple)?.visibility = View.GONE
            
            
            Log.d(TAG, "✅ Feature panel displayed: $type (toolbar hidden)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing feature panel", e)
            Toast.makeText(this, "Error opening panel", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Create rounded button drawable with theme colors
     */
    private fun createRoundedButtonDrawable(fillColor: Int, strokeColor: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = 60f * resources.displayMetrics.density // 60dp radius for pill shape
            setStroke(
                (1 * resources.displayMetrics.density).toInt(),
                strokeColor
            )
        }
    }
    
    /**
     * Create rounded text area drawable with theme colors
     */
    private fun createRoundedTextAreaDrawable(fillColor: Int, strokeColor: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = 8f * resources.displayMetrics.density // 8dp radius for text areas
            setStroke(
                (1 * resources.displayMetrics.density).toInt(),
                strokeColor
            )
        }
    }
    
    /**
     * Inflate grammar panel body with AI integration
     */
    private fun inflateGrammarBody(container: FrameLayout?) {
        val view = layoutInflater.inflate(R.layout.panel_body_grammar, container, false)
        container?.addView(view)
        
        // Store view reference for theme updates
        currentGrammarPanelView = view
        
        // Apply unified theme colors from ThemeManager
        val bgColor = themeManager.getKeyboardBackgroundColor()
        val textColor = themeManager.getTextColor()
        val keyColor = themeManager.getKeyColor()
        
        // ✅ UNIFIED THEMING: Keep transparent to show main background
        view.setBackgroundColor(Color.TRANSPARENT)
        
        val grammarOutput = view.findViewById<TextView>(R.id.grammarOutput)
        
        // Style output text area with rounded corners
        val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
        grammarOutput?.apply {
            setTextColor(textColor)
            setHintTextColor(Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))
            background = createRoundedTextAreaDrawable(keyColor, strokeColor)
        }
        
        // Style all action buttons with rounded corners (pill shape)
        listOf(
            R.id.btnRephrase,
            R.id.btnGrammarFix,
            R.id.btnAddEmojis
        ).forEach { buttonId ->
            view.findViewById<Button>(buttonId)?.apply {
                setTextColor(textColor)
                background = createRoundedButtonDrawable(keyColor, strokeColor)
            }
        }
        
        // Connect existing XML buttons to UnifiedAI service
        connectGrammarButtons(view, grammarOutput)
        
        // Load custom grammar prompts
        loadCustomGrammarPrompts(view, grammarOutput)
    }
    
    /**
     * Connect existing grammar panel XML buttons to UnifiedAI service and add custom buttons
     */
    private fun connectGrammarButtons(view: View, grammarOutput: TextView?) {
        // Get theme colors for custom buttons
        val textColor = themeManager.getTextColor()
        val keyColor = themeManager.getKeyColor()
        val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
        
        // Find the existing horizontal scroll view and button container from XML
        val scrollView = view.findViewById<android.widget.HorizontalScrollView>(R.id.grammarScrollView)
        val buttonContainer = view.findViewById<LinearLayout>(R.id.grammarButtonContainer)
        
        // 🔁 REPHRASE BUTTON (from XML)
        view.findViewById<Button>(R.id.btnRephrase)?.setOnClickListener {
            processGrammarText(getCurrentInputText(), "Rephrase and improve this text", grammarOutput, "Rephrase")
        }
        
        // ✅ GRAMMAR FIX BUTTON (from XML)
        view.findViewById<Button>(R.id.btnGrammarFix)?.setOnClickListener {
            processGrammarText(getCurrentInputText(), "Fix grammar and spelling errors", grammarOutput, "Grammar Fix")
        }
        
        // 😀 ADD EMOJIS BUTTON (from XML)
        view.findViewById<Button>(R.id.btnAddEmojis)?.setOnClickListener {
            processGrammarText(getCurrentInputText(), "Add relevant emojis to this text", grammarOutput, "Add Emojis")
        }
        
        Log.d(TAG, "✅ Grammar panel XML buttons connected to UnifiedAI")
    }
    
    /**
     * Load custom grammar prompts into the grammar panel
     */
    private fun loadCustomGrammarPrompts(view: View, grammarOutput: TextView?) {
        try {
            val customPrompts = PromptManager.getPrompts("grammar")
            // ✅ Append to main button container (inline with static buttons)
            val buttonContainer = view.findViewById<LinearLayout>(R.id.grammarButtonContainer)
            
            if (customPrompts.isNotEmpty() && buttonContainer != null) {
                // Add custom prompt buttons inline with static buttons
                customPrompts.forEach { prompt ->
                    val button = Button(this).apply {
                        text = prompt.title
                        textSize = 14f
                        val textColor = themeManager.getTextColor()
                        val keyColor = themeManager.getKeyColor()
                        val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
                        
                        setTextColor(textColor)
                        background = createRoundedButtonDrawable(keyColor, strokeColor)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (36 * resources.displayMetrics.density).toInt()
                        ).apply {
                            setMargins(0, 0, (8 * resources.displayMetrics.density).toInt(), 0)
                        }
                        setPadding((16 * resources.displayMetrics.density).toInt(), 0, (16 * resources.displayMetrics.density).toInt(), 0)
                        
                        setOnClickListener {
                            processCustomPromptText(
                                getCurrentInputText(), 
                                prompt.prompt,
                                grammarOutput, 
                                prompt.title
                            )
                        }
                    }
                    buttonContainer.addView(button)
                }
                
                Log.d(TAG, "✅ Loaded ${customPrompts.size} custom grammar prompts inline")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading custom grammar prompts", e)
        }
    }

    /**
     * Inflate tone panel body with custom prompts
     */
    private fun inflateToneBody(container: FrameLayout?) {
        val view = layoutInflater.inflate(R.layout.panel_body_tone, container, false)
        container?.addView(view)
        
        // Store view reference for theme updates
        currentTonePanelView = view
        
        // Apply unified theme colors from ThemeManager
        val textColor = themeManager.getTextColor()
        val keyColor = themeManager.getKeyColor()
        
        // ✅ UNIFIED THEMING: Keep transparent to show main background
        view.setBackgroundColor(Color.TRANSPARENT)
        
        val toneOutput1 = view.findViewById<TextView>(R.id.toneOutput1)
        val toneOutput2 = view.findViewById<TextView>(R.id.toneOutput2)
        val toneOutput3 = view.findViewById<TextView>(R.id.toneOutput3)
        val outputs = listOf(toneOutput1, toneOutput2, toneOutput3)
        
        // Style output text areas with rounded corners
        val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
        outputs.forEach { output ->
            output?.apply {
                setTextColor(textColor)
                setHintTextColor(Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))
                background = createRoundedTextAreaDrawable(keyColor, strokeColor)
            }
        }
        
        // Style all action buttons
        listOf(
            R.id.btnFunny,
            R.id.btnPoetic,
            R.id.btnShorten,
            R.id.btnSarcastic
        ).forEach { buttonId ->
            view.findViewById<Button>(buttonId)?.apply {
                setTextColor(textColor)
                background = createRoundedButtonDrawable(keyColor, strokeColor)
            }
        }
        
        // Connect existing XML buttons to UnifiedAI service
        connectToneButtons(view, outputs)
        
        // Load custom tone prompts
        loadCustomTonePrompts(view, outputs)
    }

    /**
     * Connect existing tone panel XML buttons to UnifiedAI service
     */
    private fun connectToneButtons(view: View, outputs: List<TextView?>) {
        // 😄 FUNNY BUTTON (from XML)
        view.findViewById<Button>(R.id.btnFunny)?.setOnClickListener {
            processToneText(getCurrentInputText(), "Make this text funny and humorous", outputs, "Funny")
        }
        
        // ✨ POETIC BUTTON (from XML)
        view.findViewById<Button>(R.id.btnPoetic)?.setOnClickListener {
            processToneText(getCurrentInputText(), "Make this text poetic and elegant", outputs, "Poetic")
        }
        
        // 📝 SHORTEN BUTTON (from XML)
        view.findViewById<Button>(R.id.btnShorten)?.setOnClickListener {
            processToneText(getCurrentInputText(), "Make this text shorter and more concise", outputs, "Shorten")
        }
        
        // 😏 SARCASTIC BUTTON (from XML)
        view.findViewById<Button>(R.id.btnSarcastic)?.setOnClickListener {
            processToneText(getCurrentInputText(), "Make this text sarcastic and witty", outputs, "Sarcastic")
        }
        
        Log.d(TAG, "✅ Tone panel XML buttons connected to UnifiedAI")
    }
    
    /**
     * Load custom tone prompts into the tone panel
     */
    private fun loadCustomTonePrompts(view: View, outputs: List<TextView?>) {
        try {
            val customPrompts = PromptManager.getPrompts("tone")
            // ✅ Append to main button container (inline with static buttons)
            val buttonContainer = view.findViewById<LinearLayout>(R.id.toneButtonContainer)
            
            if (customPrompts.isNotEmpty() && buttonContainer != null) {
                // Add custom prompt buttons inline with static buttons
                customPrompts.forEach { prompt ->
                    val button = Button(this).apply {
                        text = prompt.title
                        textSize = 14f
                        val textColor = themeManager.getTextColor()
                        val keyColor = themeManager.getKeyColor()
                        val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
                        
                        setTextColor(textColor)
                        background = createRoundedButtonDrawable(keyColor, strokeColor)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (36 * resources.displayMetrics.density).toInt()
                        ).apply {
                            setMargins(0, 0, (8 * resources.displayMetrics.density).toInt(), 0)
                        }
                        setPadding((16 * resources.displayMetrics.density).toInt(), 0, (16 * resources.displayMetrics.density).toInt(), 0)
                        
                        setOnClickListener {
                            processToneText(
                                getCurrentInputText(), 
                                prompt.prompt,
                                outputs, 
                                prompt.title
                            )
                        }
                    }
                    buttonContainer.addView(button)
                }
                
                Log.d(TAG, "✅ Loaded ${customPrompts.size} custom tone prompts inline")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading custom tone prompts", e)
        }
    }

    /**
     * Inflate AI assistant panel body with custom prompts
     */
    private fun inflateAIAssistantBody(container: FrameLayout?) {
        val view = layoutInflater.inflate(R.layout.panel_body_ai_assistant, container, false)
        container?.addView(view)
        
        // Store view reference for theme updates
        currentAIAssistantPanelView = view
        
        // Apply unified theme colors from ThemeManager
        val textColor = themeManager.getTextColor()
        val keyColor = themeManager.getKeyColor()
        
        // ✅ UNIFIED THEMING: Keep transparent to show main background
        view.setBackgroundColor(Color.TRANSPARENT)
        
        val aiOutput = view.findViewById<TextView>(R.id.aiOutput)
        
        // Style output text area with rounded corners
        val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
        aiOutput?.apply {
            setTextColor(textColor)
            setHintTextColor(Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))
            background = createRoundedTextAreaDrawable(keyColor, strokeColor)
        }
        
        // Style all action buttons
        listOf(
            R.id.btnChatGPT,
            R.id.btnHumanize,
            R.id.btnReply,
            R.id.btnIdioms
        ).forEach { buttonId ->
            view.findViewById<Button>(buttonId)?.apply {
                setTextColor(textColor)
                background = createRoundedButtonDrawable(keyColor, strokeColor)
            }
        }
        
        // Connect existing XML buttons to UnifiedAI service
        connectAIAssistantButtons(view, aiOutput)
        
        // Load custom AI assistant prompts
        loadCustomAIAssistantPrompts(view, aiOutput)
    }

    /**
     * Connect existing AI assistant panel XML buttons to UnifiedAI service
     */
    private fun connectAIAssistantButtons(view: View, aiOutput: TextView?) {
        // 💬 CHATGPT BUTTON (from XML)
        view.findViewById<Button>(R.id.btnChatGPT)?.setOnClickListener {
            processAIAssistantText(getCurrentInputText(), "Process with ChatGPT and improve this text", aiOutput, "ChatGPT")
        }
        
        // 👤 HUMANIZE BUTTON (from XML)
        view.findViewById<Button>(R.id.btnHumanize)?.setOnClickListener {
            processAIAssistantText(getCurrentInputText(), "Make this text sound more human and natural", aiOutput, "Humanize")
        }
        
        // ↩️ REPLY BUTTON (from XML)
        view.findViewById<Button>(R.id.btnReply)?.setOnClickListener {
            processAIAssistantText(getCurrentInputText(), "Generate a smart reply to this message", aiOutput, "Reply")
        }
        
        // 📚 IDIOMS BUTTON (from XML)
        view.findViewById<Button>(R.id.btnIdioms)?.setOnClickListener {
            processAIAssistantText(getCurrentInputText(), "Add idioms and expressions to this text", aiOutput, "Add Idioms")
        }
        
        Log.d(TAG, "✅ AI assistant panel XML buttons connected to UnifiedAI")
    }
    
    /**
     * Load custom AI assistant prompts into the AI assistant panel
     */
    private fun loadCustomAIAssistantPrompts(view: View, aiOutput: TextView?) {
        try {
            val customPrompts = PromptManager.getPrompts("assistant")
            // ✅ Append to main button container (inline with static buttons)
            val buttonContainer = view.findViewById<LinearLayout>(R.id.aiAssistantButtonContainer)
            
            if (customPrompts.isNotEmpty() && buttonContainer != null) {
                // Add custom prompt buttons inline with static buttons
                customPrompts.forEach { prompt ->
                    val button = Button(this).apply {
                        text = prompt.title
                        textSize = 14f
                        val textColor = themeManager.getTextColor()
                        val keyColor = themeManager.getKeyColor()
                        val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
                        
                        setTextColor(textColor)
                        background = createRoundedButtonDrawable(keyColor, strokeColor)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (36 * resources.displayMetrics.density).toInt()
                        ).apply {
                            setMargins(0, 0, (8 * resources.displayMetrics.density).toInt(), 0)
                        }
                        setPadding((16 * resources.displayMetrics.density).toInt(), 0, (16 * resources.displayMetrics.density).toInt(), 0)
                        
                        setOnClickListener {
                            processAIAssistantText(
                                getCurrentInputText(), 
                                prompt.prompt,
                                aiOutput, 
                                prompt.title
                            )
                        }
                    }
                    buttonContainer.addView(button)
                }
                
                Log.d(TAG, "✅ Loaded ${customPrompts.size} custom AI assistant prompts inline")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading custom AI assistant prompts", e)
        }
    }
    
    /**
     * Process text with custom prompt for grammar panel
     */
    private fun processCustomPromptText(inputText: String, prompt: String, outputView: TextView?, actionName: String) {
        if (inputText.isEmpty()) {
            Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
            return
        }

        outputView?.text = "🤖 Processing with custom prompt..."
        
        // Store original text for replacement
        val originalText = inputText

        coroutineScope.launch {
            try {
                val fullPrompt = "$prompt\n\nText to process: $inputText"
                
                val result = UnifiedAIService(this@AIKeyboardService).processText(
                    text = fullPrompt,
                    mode = UnifiedAIService.Mode.CUSTOM,
                    stream = false
                ).first()

                mainHandler.post {
                    if (result.success) {
                        // Strip any numbering patterns from the result
                        val cleanedText = stripNumberingFromText(result.text)
                        outputView?.text = cleanedText
                        // Make output clickable to replace with proper tracking
                        outputView?.setOnClickListener {
                            replaceTextWithResult(originalText, cleanedText)
                            Toast.makeText(this@AIKeyboardService, "✅ Text replaced", Toast.LENGTH_SHORT).show()
                            restoreKeyboardFromPanel()
                        }
                        Log.d(TAG, "✅ $actionName completed successfully")
                    } else {
                        outputView?.text = "❌ ${result.error}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in $actionName", e)
                mainHandler.post {
                    outputView?.text = "❌ Processing failed"
                }
            }
        }
    }

    /**
     * Helper function to strip numbering patterns from AI responses
     */
    private fun stripNumberingFromText(text: String): String {
        return text
            .replaceFirst(Regex("^\\d+[.):]\\s*"), "") // Remove "1. ", "1) ", "1: " at start
            .replaceFirst(Regex("^[•\\-*]\\s+"), "") // Remove "• ", "- ", "* " at start
            .trim()
    }

    /**
     * Process text for grammar panel
     */
    private fun processGrammarText(inputText: String, prompt: String, outputView: TextView?, actionName: String) {
        if (inputText.isEmpty()) {
            Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
            return
        }

        outputView?.text = "🤖 Processing..."
        
        // Store original text for replacement
        val originalText = inputText

        coroutineScope.launch {
            try {
                val result = UnifiedAIService(this@AIKeyboardService).processText(
                    text = inputText,
                    mode = UnifiedAIService.Mode.GRAMMAR,
                    stream = false
                ).first()

                mainHandler.post {
                    if (result.success) {
                        // Strip any numbering patterns from the result
                        val cleanedText = stripNumberingFromText(result.text)
                        outputView?.text = cleanedText
                        // Make output clickable to replace with proper tracking
                        outputView?.setOnClickListener {
                            replaceTextWithResult(originalText, cleanedText)
                            Toast.makeText(this@AIKeyboardService, "✅ Text replaced", Toast.LENGTH_SHORT).show()
                            restoreKeyboardFromPanel()
                        }
                        Log.d(TAG, "✅ $actionName completed successfully")
                    } else {
                        outputView?.text = "❌ ${result.error}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in $actionName", e)
                mainHandler.post {
                    outputView?.text = "❌ Processing failed"
                }
            }
        }
    }

    /**
     * Process text for tone panel with multiple outputs
     */
    private fun processToneText(inputText: String, prompt: String, outputs: List<TextView?>, actionName: String) {
        if (inputText.isEmpty()) {
            Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
            return
        }

        outputs.forEach { output ->
            output?.text = "🤖 Processing..."
        }
        
        // Store original text for replacement
        val originalText = inputText

        coroutineScope.launch {
            try {
                val fullPrompt = "$prompt\n\nText to process: $inputText\n\nProvide exactly 3 different variations, each on a new line:"
                
                val result = UnifiedAIService(this@AIKeyboardService).processText(
                    text = fullPrompt,
                    mode = UnifiedAIService.Mode.CUSTOM,
                    stream = false
                ).first()

                mainHandler.post {
                    if (result.success) {
                        // Split by newlines and filter out empty lines
                        val allLines = result.text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                        
                        // Extract variations by removing numbering patterns (1., 2., 3., 1), 2), 3), etc.)
                        val variations = allLines.mapNotNull { line ->
                            // Remove common numbering patterns at the start of the line
                            val cleaned = line
                                .replaceFirst(Regex("^\\d+[.):]\\s*"), "") // Remove "1. ", "1) ", "1: "
                                .replaceFirst(Regex("^[•\\-*]\\s+"), "") // Remove "• ", "- ", "* "
                                .trim()
                            
                            if (cleaned.isNotEmpty()) cleaned else null
                        }.take(3) // Take only first 3 variations
                        
                        Log.d(TAG, "📝 Parsed ${variations.size} variations from AI response")
                        
                        // Display each variation in its own box
                        outputs.forEachIndexed { index, output ->
                            val variation = variations.getOrNull(index) ?: "Variation ${index + 1} not available"
                            output?.text = variation
                            
                            // Make each output clickable to replace with proper tracking
                            output?.setOnClickListener {
                                replaceTextWithResult(originalText, variation)
                                Toast.makeText(this@AIKeyboardService, "✅ Text replaced", Toast.LENGTH_SHORT).show()
                                restoreKeyboardFromPanel()
                            }
                        }
                        Log.d(TAG, "✅ $actionName completed successfully")
                    } else {
                        outputs.forEach { output ->
                            output?.text = "❌ ${result.error}"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in $actionName", e)
                mainHandler.post {
                    outputs.forEach { output ->
                        output?.text = "❌ Processing failed"
                    }
                }
            }
        }
    }

    /**
     * Process text for AI assistant panel
     */
    private fun processAIAssistantText(inputText: String, prompt: String, outputView: TextView?, actionName: String) {
        if (inputText.isEmpty()) {
            Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
            return
        }

        outputView?.text = "🤖 Processing with AI assistant..."
        
        // Store original text for replacement
        val originalText = inputText

        coroutineScope.launch {
            try {
                val fullPrompt = "$prompt\n\nText to process: $inputText"
                
                val result = UnifiedAIService(this@AIKeyboardService).processText(
                    text = fullPrompt,
                    mode = UnifiedAIService.Mode.CUSTOM,
                    stream = false
                ).first()

                mainHandler.post {
                    if (result.success) {
                        // Strip any numbering patterns from the result
                        val cleanedText = stripNumberingFromText(result.text)
                        outputView?.text = cleanedText
                        // Make output clickable to replace with proper tracking
                        outputView?.setOnClickListener {
                            replaceTextWithResult(originalText, cleanedText)
                            Toast.makeText(this@AIKeyboardService, "✅ Text replaced", Toast.LENGTH_SHORT).show()
                            restoreKeyboardFromPanel()
                        }
                        Log.d(TAG, "✅ $actionName completed successfully")
                    } else {
                        outputView?.text = "❌ ${result.error}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in $actionName", e)
                mainHandler.post {
                    outputView?.text = "❌ Processing failed"
                }
            }
        }
    }
    
    
    /**
     * Add a dynamic button to a container
     */
    private fun addDynamicButton(
        container: LinearLayout,
        text: String,
        textColor: Int,
        keyColor: Int,
        strokeColor: Int,
        onClick: () -> Unit
    ) {
        val button = Button(this).apply {
            this.text = text
            setTextColor(textColor)
            background = createRoundedButtonDrawable(keyColor, strokeColor)
            setOnClickListener { onClick() }
            
            // Style the button
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 4, 8, 4)
            layoutParams = params
            
            setPadding(24, 12, 24, 12)
            textSize = 14f
        }
        
        container.addView(button)
    }
    
    /**
     * Process text with UnifiedAI service (original method for grammar/tone panels)
     */
    private fun processTextWithUnifiedAI(
        inputText: String,
        mode: UnifiedAIService.Mode,
        outputView: TextView?,
        actionName: String,
        tone: AdvancedAIService.ToneType? = null,
        feature: AdvancedAIService.ProcessingFeature? = null
    ) {
        if (inputText.isEmpty()) {
            Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
            return
        }
        
        outputView?.text = "🧠 Processing $actionName..."
        Log.d(TAG, "🧠 Processing with UnifiedAI: $actionName")
        
        coroutineScope.launch {
            try {
                if (::unifiedAIService.isInitialized && unifiedAIService.isReady()) {
                    unifiedAIService.processText(
                        text = inputText,
                        mode = mode,
                        tone = tone,
                        feature = feature,
                        stream = false
                    ).collect { result ->
                        withContext(Dispatchers.Main) {
                            if (result.success) {
                                outputView?.text = result.text
                                if (result.fromCache) {
                                    outputView?.append("\n⚡ (cached)")
                                }
                                Log.d(TAG, "✅ $actionName completed via UnifiedAI in ${result.time}ms")
                                Toast.makeText(this@AIKeyboardService, "✅ $actionName completed!", Toast.LENGTH_SHORT).show()
                            } else {
                                outputView?.text = "❌ Error: ${result.error}"
                                Log.e(TAG, "UnifiedAI $actionName error: ${result.error}")
                                Toast.makeText(this@AIKeyboardService, "❌ $actionName failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ UnifiedAI not ready for $actionName")
                    outputView?.text = "❌ AI service not ready"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    outputView?.text = "❌ Error: ${e.message}"
                    Log.e(TAG, "$actionName error", e)
                }
            }
        }
    }
    
    /**
     * Process text with UnifiedAI service for SINGLE output (Grammar, AI Assistant)
     */
    private fun processTextWithSingleOutput(
        inputText: String,
        mode: UnifiedAIService.Mode,
        outputView: TextView?,
        actionName: String,
        tone: AdvancedAIService.ToneType? = null,
        feature: AdvancedAIService.ProcessingFeature? = null
    ) {
        if (inputText.isEmpty()) {
            Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show shimmer loading effect
        startShimmerEffect(outputView)
        Log.d(TAG, "🧠 Processing with UnifiedAI: $actionName")
        
        coroutineScope.launch {
            try {
                if (::unifiedAIService.isInitialized && unifiedAIService.isReady()) {
                    unifiedAIService.processText(
                        text = inputText,
                        mode = mode,
                        tone = tone,
                        feature = feature,
                        stream = false
                    ).collect { result ->
                        withContext(Dispatchers.Main) {
                            stopShimmerEffect(outputView)
                            if (result.success) {
                                outputView?.text = result.text
                                Log.d(TAG, "✅ $actionName completed in ${result.time}ms")
                                Toast.makeText(this@AIKeyboardService, "✅ $actionName completed!", Toast.LENGTH_SHORT).show()
                            } else {
                                outputView?.text = "❌ Error: ${result.error}"
                                Log.e(TAG, "UnifiedAI $actionName error: ${result.error}")
                                Toast.makeText(this@AIKeyboardService, "❌ $actionName failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ UnifiedAI not ready for $actionName")
                    withContext(Dispatchers.Main) {
                        stopShimmerEffect(outputView)
                        outputView?.text = "❌ AI service not ready"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    stopShimmerEffect(outputView)
                    outputView?.text = "❌ Error: ${e.message}"
                    Log.e(TAG, "$actionName error", e)
                }
            }
        }
    }
    
    /**
     * Process text with UnifiedAI service and generate 3 variations in a SINGLE API call
     */
    private fun processTextWithMultipleOutputs(
        inputText: String,
        mode: UnifiedAIService.Mode,
        outputs: List<TextView?>,
        actionName: String,
        tone: AdvancedAIService.ToneType? = null,
        feature: AdvancedAIService.ProcessingFeature? = null
    ) {
        if (inputText.isEmpty()) {
            Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show shimmer loading effect on all outputs
        outputs.forEach { startShimmerEffect(it) }
        Log.d(TAG, "🧠 Processing with UnifiedAI: $actionName (requesting 3 variations in single call)")
        
        coroutineScope.launch {
            try {
                if (::unifiedAIService.isInitialized && unifiedAIService.isReady()) {
                    // Make SINGLE API call requesting 3 variations
                    val enhancedPrompt = buildPromptFor3Variations(inputText, actionName, mode, tone, feature)
                    
                    unifiedAIService.processText(
                        text = enhancedPrompt,
                        mode = UnifiedAIService.Mode.CUSTOM,
                        stream = false
                    ).collect { result ->
                        withContext(Dispatchers.Main) {
                            // Stop shimmer on all outputs
                            outputs.forEach { stopShimmerEffect(it) }
                            
                            if (result.success) {
                                // Parse the response to extract 3 variations
                                val variations = parseVariations(result.text)
                                
                                variations.forEachIndexed { index, variation ->
                                    if (index < outputs.size && variation.isNotBlank()) {
                                        outputs[index]?.text = variation.trim()
                                    }
                                }
                                
                                // Fill remaining outputs if less than 3 variations were returned
                                for (i in variations.size until outputs.size) {
                                    if (variations.isNotEmpty()) {
                                        outputs[i]?.text = variations[i % variations.size]  // Repeat variations
                                    }
                                }
                                
                                Log.d(TAG, "✅ $actionName completed with ${variations.size} variations in ${result.time}ms")
                                Toast.makeText(this@AIKeyboardService, "✅ $actionName completed!", Toast.LENGTH_SHORT).show()
                            } else {
                                outputs.forEach { it?.text = "❌ Error: ${result.error}" }
                                Log.e(TAG, "UnifiedAI $actionName error: ${result.error}")
                                Toast.makeText(this@AIKeyboardService, "❌ $actionName failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ UnifiedAI not ready for $actionName")
                    withContext(Dispatchers.Main) {
                        outputs.forEach { 
                            stopShimmerEffect(it)
                            it?.text = "❌ AI service not ready" 
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    outputs.forEach { 
                        stopShimmerEffect(it)
                        it?.text = "❌ Error: ${e.message}" 
                    }
                    Log.e(TAG, "$actionName error", e)
                }
            }
        }
    }
    
    /**
     * Start shimmer loading effect on TextView with theme colors
     */
    private fun startShimmerEffect(textView: TextView?) {
        textView?.let {
            val textColor = themeManager.getTextColor()
            val keyColor = themeManager.getKeyColor()
            
            // Create shimmer gradient colors from theme
            val shimmerColor1 = Color.argb(30, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            val shimmerColor2 = Color.argb(80, Color.red(keyColor), Color.green(keyColor), Color.blue(keyColor))
            val shimmerColor3 = Color.argb(30, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            
            val shimmerDrawable = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(shimmerColor1, shimmerColor2, shimmerColor3)
            )
            shimmerDrawable.cornerRadius = 12f
            
            it.background = shimmerDrawable
            it.text = ""
            
            // Animate the shimmer
            val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 1500
            animator.repeatCount = android.animation.ValueAnimator.INFINITE
            animator.repeatMode = android.animation.ValueAnimator.RESTART
            animator.addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                shimmerDrawable.setGradientCenter(fraction, 0.5f)
                it.invalidate()
            }
            it.tag = animator  // Store animator in tag to stop later
            animator.start()
        }
    }
    
    /**
     * Stop shimmer loading effect and restore normal background
     */
    private fun stopShimmerEffect(textView: TextView?) {
        textView?.let {
            // Stop and remove animator
            val animator = it.tag as? android.animation.ValueAnimator
            animator?.cancel()
            it.tag = null
            
            // Restore normal background
            val textColor = themeManager.getTextColor()
            val keyColor = themeManager.getKeyColor()
            val strokeColor = Color.argb(50, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            it.background = createRoundedTextAreaDrawable(keyColor, strokeColor)
        }
    }
    
    /**
     * Build a prompt that requests 3 different variations
     */
    private fun buildPromptFor3Variations(
        inputText: String,
        actionName: String,
        mode: UnifiedAIService.Mode,
        tone: AdvancedAIService.ToneType?,
        feature: AdvancedAIService.ProcessingFeature?
    ): String {
        val taskDescription = when {
            actionName.contains("Grammar", ignoreCase = true) -> "fix the grammar of"
            actionName.contains("Rephrase", ignoreCase = true) -> "rephrase"
            actionName.contains("Emoji", ignoreCase = true) -> "add relevant emojis to"
            actionName.contains("Funny", ignoreCase = true) -> "rewrite in a funny tone"
            actionName.contains("Poetic", ignoreCase = true) -> "rewrite in a poetic/formal tone"
            actionName.contains("Shorten", ignoreCase = true) -> "shorten and make more concise"
            actionName.contains("Sarcastic", ignoreCase = true) -> "rewrite in a casual/sarcastic tone"
            tone != null -> "adjust the tone of"
            feature != null -> "improve"
            else -> "enhance"
        }
        
        return """Generate exactly 3 different variations to $taskDescription the following text. 
Each variation should be unique and creative.
Respond ONLY with the 3 variations, separated by the delimiter '|||'.
Do not include numbering, labels, or any other text.

Text: "$inputText"

Format: variation1|||variation2|||variation3"""
    }
    
    /**
     * Parse AI response to extract 3 variations
     */
    private fun parseVariations(response: String): List<String> {
        // Try splitting by ||| first (our requested delimiter)
        val variations = response.split("|||").map { it.trim() }.filter { it.isNotBlank() }
        
        if (variations.size >= 2) {
            return variations.take(3)
        }
        
        // Fallback: Try splitting by newlines or numbered patterns
        val lines = response.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val cleanedLines = lines.map { line ->
            // Remove common prefixes like "1.", "2.", "•", "-", etc.
            line.replace(Regex("^[\\d]+[.)\\s]+"), "")
                .replace(Regex("^[•\\-*]\\s+"), "")
                .replace(Regex("^Variation\\s+\\d+[:\\s]*", RegexOption.IGNORE_CASE), "")
                .trim()
        }.filter { it.isNotBlank() }
        
        if (cleanedLines.size >= 2) {
            return cleanedLines.take(3)
        }
        
        // Last resort: split by sentence endings if we have a long response
        if (response.length > 100) {
            val sentences = response.split(Regex("[.!?]+\\s+")).map { it.trim() }.filter { it.isNotBlank() }
            if (sentences.size >= 3) {
                return sentences.take(3)
            }
        }
        
        // If all else fails, return the whole response in all 3 slots
        return if (response.isNotBlank()) listOf(response, response, response) else emptyList()
    }
    
    
    private fun inflateClipboardBody(container: FrameLayout?) {
        val view = layoutInflater.inflate(R.layout.panel_body_clipboard, container, false)
        container?.addView(view)
        
        // Apply theme colors
        val palette = themeManager.getCurrentPalette()
        // ✅ UNIFIED THEMING: Keep transparent to show main background
        view.setBackgroundColor(Color.TRANSPARENT)
        
        // Style header title
       // view.findViewById<TextView>(R.id.clipboardHeaderTitle)?.apply {
         //   setTextColor(palette.keyText)
       // }
        
        // Create stroke color for clipboard items
        val strokeColor = Color.argb(50, Color.red(palette.keyText), Color.green(palette.keyText), Color.blue(palette.keyText))
        
        // Get actual clipboard items from ClipboardHistoryManager
        val clipboardItems = if (::clipboardHistoryManager.isInitialized) {
            clipboardHistoryManager.getHistoryForUI(5) // Get up to 5 items
        } else {
            Log.w(TAG, "ClipboardHistoryManager not initialized")
            emptyList()
        }
        
        // Get the container for dynamic items
        val itemsContainer = view.findViewById<LinearLayout>(R.id.clipboardItemsContainer)
        itemsContainer?.removeAllViews() // Clear existing views
        
        // Get header and Clear All button
        val header = view.findViewById<LinearLayout>(R.id.clipboardHeader)
        val clearAllButton = view.findViewById<Button>(R.id.btnClearAll)
        
        if (clipboardItems.isEmpty()) {
            // Hide header when empty
            header?.visibility = View.GONE
            
            // Show empty state
            val emptyText = TextView(this).apply {
                text = "No clipboard history yet.\nCopy some text to get started!"
                textSize = 14f
                setTextColor(palette.keyText)
                gravity = android.view.Gravity.CENTER
                setPadding(32, 64, 32, 64)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            itemsContainer?.addView(emptyText)
        } else {
            // Show header when there are items
            header?.visibility = View.VISIBLE
            
            // Style Clear All button
            clearAllButton?.apply {
                setTextColor(Color.RED)
                val keyColor = palette.keyBg
                val strokeColor = Color.RED
                background = createRoundedButtonDrawable(keyColor, strokeColor)
                
                setOnClickListener {
                    // Clear clipboard history (except pinned/template items)
                    if (::clipboardHistoryManager.isInitialized) {
                        clipboardItems.forEach { item ->
                            if (!item.isPinned && !item.isTemplate) {
                                clipboardHistoryManager.deleteItem(item.id)
                            }
                        }
                        Toast.makeText(this@AIKeyboardService, "✅ Clipboard cleared", Toast.LENGTH_SHORT).show()
                        // Refresh the panel
                        restoreKeyboardFromPanel()
                        showFeaturePanel(PanelType.CLIPBOARD)
                    }
                }
            }
            
            // Dynamically add clipboard items (max 5)
            clipboardItems.take(5).forEachIndexed { index, item ->
                val itemView = TextView(this).apply {
                    // Show preview with OTP indicator if applicable
                    val prefix = if (item.isOTP()) "🔢 " else ""
                    val pinIndicator = if (item.isPinned) " 📌" else ""
                    text = "$prefix${item.getPreview(60)}$pinIndicator"
                    
                    textSize = 14f
                    setTextColor(palette.keyText)
                    setPadding(12, 12, 12, 12)
                    background = createRoundedTextAreaDrawable(palette.keyBg, strokeColor)
                    
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    if (index > 0) {
                        params.topMargin = 8
                    }
                    layoutParams = params
                    
                    // Make clickable
                    isClickable = true
                    isFocusable = true
                    
                    setOnClickListener {
                        // Insert clipboard item
                        currentInputConnection?.commitText(item.text, 1)
                        Toast.makeText(this@AIKeyboardService, "✅ Inserted: ${item.getPreview(30)}", Toast.LENGTH_SHORT).show()
                        restoreKeyboardFromPanel()
                    }
                    
                    // Long press to pin/delete
                    setOnLongClickListener {
                        showClipboardItemActions(item)
                        true
                    }
                }
                itemsContainer?.addView(itemView)
            }
        }
    }
    
    /**
     * Show actions for a clipboard item (pin/delete)
     */
    private fun showClipboardItemActions(item: ClipboardItem) {
        try {
            val items = mutableListOf<String>()
            
            if (!item.isTemplate) {
                items.add(if (item.isPinned) "📍 Unpin" else "📌 Pin")
                items.add("🗑️ Delete")
            }
            items.add("📋 Copy Again")
            items.add("❌ Cancel")
            
            val builder = android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            builder.setTitle(item.getPreview(40))
            builder.setItems(items.toTypedArray()) { dialog, which ->
                when (items[which]) {
                    "📌 Pin", "📍 Unpin" -> {
                        if (::clipboardHistoryManager.isInitialized) {
                            val wasPinned = clipboardHistoryManager.togglePin(item.id)
                            val message = if (wasPinned) "Pinned" else "Unpinned"
                            Toast.makeText(this, "$message: ${item.getPreview(30)}", Toast.LENGTH_SHORT).show()
                            // Refresh panel
                            restoreKeyboardFromPanel()
                            showFeaturePanel(PanelType.CLIPBOARD)
                        }
                    }
                    "🗑️ Delete" -> {
                        if (::clipboardHistoryManager.isInitialized) {
                            clipboardHistoryManager.deleteItem(item.id)
                            Toast.makeText(this, "Deleted: ${item.getPreview(30)}", Toast.LENGTH_SHORT).show()
                            // Refresh panel
                            restoreKeyboardFromPanel()
                            showFeaturePanel(PanelType.CLIPBOARD)
                        }
                    }
                    "📋 Copy Again" -> {
                        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("text", item.text)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            builder.create().show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing clipboard actions", e)
        }
    }
    
    /**
     * Inflate quick settings panel body
     */
    private fun inflateQuickSettingsBody(container: FrameLayout?) {
        val view = layoutInflater.inflate(R.layout.panel_body_quick_settings, container, false)
        container?.addView(view)
        
        // Apply theme colors
        val palette = themeManager.getCurrentPalette()
        view.setBackgroundColor(palette.keyboardBg)
        
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
        
        // Sound Switch
        view.findViewById<android.widget.Switch>(R.id.switch_sound)?.apply {
            isChecked = prefs.getBoolean("key_sound", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("key_sound", isChecked).apply()
                soundEnabled = isChecked
                sendSettingToFlutter("key_sound", isChecked)
                Log.d(TAG, "Key Sound: $isChecked")
            }
        }
        
        // Vibration Switch
        view.findViewById<android.widget.Switch>(R.id.switch_vibration)?.apply {
            isChecked = prefs.getBoolean("vibration", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("vibration", isChecked).apply()
                vibrationEnabled = isChecked
                sendSettingToFlutter("vibration", isChecked)
                Log.d(TAG, "Vibration: $isChecked")
            }
        }
        
        // AI Suggestions Switch
        view.findViewById<android.widget.Switch>(R.id.switch_ai_suggestions)?.apply {
            isChecked = prefs.getBoolean("ai_suggestions", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("ai_suggestions", isChecked).apply()
                sendSettingToFlutter("ai_suggestions", isChecked)
                Log.d(TAG, "AI Suggestions: $isChecked")
            }
        }
        
        // Number Row Switch
        view.findViewById<android.widget.Switch>(R.id.switch_number_row)?.apply {
            isChecked = prefs.getBoolean("number_row", false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("number_row", isChecked).apply()
                showNumberRow = isChecked
                sendSettingToFlutter("number_row", isChecked)
                Log.d(TAG, "Number Row: $isChecked")
                
                // Reload keyboard with new setting
                restoreKeyboardFromPanel()
                reloadKeyboard()
            }
        }
    }
    
    /**
     * Inflate emoji panel body
     */
    private fun inflateEmojiBody(container: FrameLayout?) {
        try {
            // Use the existing EmojiPanelController to inflate the emoji panel
            val emojiView = emojiPanelController?.inflate(container ?: return)
            if (emojiView != null) {
                // Remove the view from its current parent if it has one
                val currentParent = emojiView.parent as? ViewGroup
                currentParent?.removeView(emojiView)
                
                // Add the emoji view to the container (ensure no scrolling wrapper)
                container.removeAllViews()
                
                // CRITICAL: Ensure container is completely fixed - no scrolling at all
                container.isScrollContainer = false
                container.overScrollMode = View.OVER_SCROLL_NEVER
                
                // Add view with proper layout params for fixed positioning  
                emojiView.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                container.addView(emojiView)
                
                Log.d(TAG, "✅ Emoji panel added - ONLY emoji grid scrolls, panel is FIXED")
                
                // Track emoji panel state
                emojiPanelView = emojiView
                isEmojiPanelVisible = true
                
                // Force apply theme after inflation (overrides XML hardcoded colors)
                emojiPanelController?.applyTheme()
                
                // Force another theme application after a brief delay to ensure it sticks
                mainHandler.postDelayed({
                    emojiPanelController?.applyTheme()
                    Log.d(TAG, "🎨 Emoji panel theme force-applied")
                }, 100)
                
                Log.d(TAG, "✅ Emoji panel body inflated and themed")
            } else {
                Log.e(TAG, "Failed to inflate emoji panel body")
                Toast.makeText(this, "Error loading emoji panel", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inflating emoji body", e)
            Toast.makeText(this, "Error opening emoji panel", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Restore keyboard from feature panel
     */
    private fun restoreKeyboardFromPanel() {
        try {
            // Hide emoji panel if visible
            if (isEmojiPanelVisible) {
                isEmojiPanelVisible = false
                emojiPanelView = null
            }
            
            // Restore keyboard in container
            // ✅ UNIFIED THEMING: Ensure transparency is maintained
            keyboardContainer?.setBackgroundColor(Color.TRANSPARENT)
            keyboardView?.let { 
                it.visibility = View.VISIBLE
                it.setBackgroundColor(Color.TRANSPARENT)
                keyboardContainer?.removeAllViews()
                keyboardContainer?.addView(it)
            }
            
            // Show suggestions and toolbar when returning from panel
            suggestionContainer?.visibility = View.VISIBLE
            topContainer?.findViewById<LinearLayout>(R.id.keyboard_toolbar_simple)?.visibility = View.VISIBLE
            
            
            Log.d(TAG, "✅ Keyboard restored from panel (toolbar shown)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring keyboard from panel", e)
        }
    }
    
    /**
     * Create simplified 6-button toolbar
     * ✅ TRANSPARENT TOOLBAR: Unified background for perfect theme matching
     */
    private fun createSimplifiedToolbar(): LinearLayout {
        return try {
            val toolbarView = layoutInflater.inflate(R.layout.keyboard_toolbar_simple, null) as LinearLayout
            
            // Apply theme colors
            val palette = themeManager.getCurrentPalette()
            toolbarView.setBackgroundColor(Color.TRANSPARENT)  // Transparent for unified theming
            
            // CRITICAL: Load icons IMMEDIATELY before showing toolbar
            loadToolbarIcons(toolbarView, palette)
            
            // Setup button listeners (after icons are loaded)
            setupSimplifiedToolbarListeners(toolbarView, palette)
            
            Log.d(TAG, "✅ Simplified toolbar created with icons preloaded")
            toolbarView
        } catch (e: Exception) {
            Log.w(TAG, "Toolbar inflate failed, creating fallback toolbar", e)
            createFallbackToolbar()
        }
    }
    
    /**
     * Create a box-style drawable with background color and icon
     */
    private fun createBoxIconDrawable(iconResId: Int, boxColor: Int, iconColor: Int): android.graphics.drawable.LayerDrawable {
        // Create rounded rectangle background
        val backgroundShape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 8f * resources.displayMetrics.density // 8dp corner radius for 40dp icons
            setColor(boxColor)
        }
        
        // Load and tint the icon
        val iconDrawable = ContextCompat.getDrawable(this, iconResId)?.mutate()
        iconDrawable?.let {
            DrawableCompat.setTint(it, iconColor)
        }
        
        // Create layer drawable
        val layers = arrayOf(backgroundShape, iconDrawable)
        val layerDrawable = android.graphics.drawable.LayerDrawable(layers)
        
        // Add padding to the icon layer so it sits inside the box
        val padding = (4 * resources.displayMetrics.density).toInt() // 4dp padding for 40dp icons
        layerDrawable.setLayerInset(1, padding, padding, padding, padding)
        
        return layerDrawable
    }
    
    /**
     * Load all toolbar icons immediately (called during toolbar creation)
     */
    private fun loadToolbarIcons(toolbar: LinearLayout, palette: com.example.ai_keyboard.themes.ThemePaletteV2) {
        try {
            // Load box-style icons for ImageView buttons - Left Side
            toolbar.findViewById<ImageView>(R.id.btn_ai_assistant)?.let {
                it.setImageDrawable(createBoxIconDrawable(R.drawable.chatgpt_icon, palette.specialAccent, palette.keyText))
            }
            
            toolbar.findViewById<ImageView>(R.id.btn_grammar_fix)?.let {
                it.setImageDrawable(createBoxIconDrawable(R.drawable.grammar_icon, palette.specialAccent, palette.keyText))
            }
            
            toolbar.findViewById<ImageView>(R.id.btn_word_tone)?.let {
                it.setImageDrawable(createBoxIconDrawable(R.drawable.tone_icon, palette.specialAccent, palette.keyText))
            }
            
            // Load PNG icons for ImageView buttons - Right Side
            toolbar.findViewById<ImageView>(R.id.btn_clipboard)?.let {
                // Use vector drawable for dynamic theming
                val drawable = ContextCompat.getDrawable(this, R.drawable.clipboard)
                drawable?.let { d ->
                    DrawableCompat.setTint(d, palette.keyText)
                }
                it.setImageDrawable(drawable)
            }
            
            toolbar.findViewById<ImageView>(R.id.btn_emoji)?.let {
                // Use vector drawable for dynamic theming
                val drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.emoji_icon)
                drawable?.let { d ->
                    androidx.core.graphics.drawable.DrawableCompat.setTint(d, palette.keyText)
                }
                it.setImageDrawable(drawable)
            }
            
            toolbar.findViewById<ImageView>(R.id.btn_voice)?.let {
                // Use vector drawable for dynamic theming
                val drawable = ContextCompat.getDrawable(this, R.drawable.voice)
                drawable?.let { d ->
                    DrawableCompat.setTint(d, palette.keyText)
                }
                it.setImageDrawable(drawable)
            }
            
            Log.d(TAG, "✅ All toolbar icons preloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error preloading toolbar icons", e)
        }
    }
    
    /**
     * Create fallback toolbar if main toolbar fails to inflate
     */
    private fun createFallbackToolbar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            )
            setPadding(16, 8, 16, 8)
            
            // Add minimal text button as fallback
            addView(TextView(this@AIKeyboardService).apply {
                text = "Keyboard Ready"
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.0f
                )
            })
        }
    }
    
    /**
     * Load icon from assets without tinting (preserves original colors)
     * Icons are scaled to consistent size for uniform appearance
     */
    private fun loadIconFromAssets(assetPath: String, imageView: ImageView, tintColor: Int) {
        try {
            val inputStream = assets.open(assetPath)
            
            // Decode with inJustDecodeBounds to get dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Fixed target size for consistent icon appearance (32dp = good balance)
            val targetSize = (32 * resources.displayMetrics.density).toInt()
            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
            
            // Decode bitmap with sample size
            val inputStream2 = assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
            inputStream2.close()
            
            if (bitmap != null) {
                // Scale bitmap to exact target size for consistency
                val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                    bitmap, 
                    targetSize, 
                    targetSize, 
                    true
                )
                
                imageView.setImageBitmap(scaledBitmap)
                
                // Clear any existing tint/filter to show original icon colors
                imageView.clearColorFilter()
                imageView.imageTintList = null
                imageView.setBackgroundColor(Color.TRANSPARENT)
                
                // Recycle original if different from scaled
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                }
                
                Log.d(TAG, "✅ Loaded toolbar icon: $assetPath (scaled to ${targetSize}x${targetSize}px) - original colors preserved")
            } else {
                Log.e(TAG, "❌ Failed to decode bitmap from: $assetPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading icon from assets: $assetPath", e)
        }
    }
    
    /**
     * Calculate inSampleSize for bitmap loading to reduce memory usage
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Setup listeners for simplified toolbar buttons
     * Note: Icons are already loaded in loadToolbarIcons()
     */
    private fun setupSimplifiedToolbarListeners(toolbar: LinearLayout, palette: com.example.ai_keyboard.themes.ThemePaletteV2) {
        // Grammar Fix Button
        toolbar.findViewById<ImageView>(R.id.btn_grammar_fix)?.setOnClickListener {
            Log.d(TAG, "Grammar Fix button tapped")
            showFeaturePanel(PanelType.GRAMMAR_FIX)
        }
        
        // Word Tone Button
        toolbar.findViewById<ImageView>(R.id.btn_word_tone)?.setOnClickListener {
            Log.d(TAG, "Word Tone button tapped")
            showFeaturePanel(PanelType.WORD_TONE)
        }
        
        // AI Assistant Button
        toolbar.findViewById<ImageView>(R.id.btn_ai_assistant)?.setOnClickListener {
            Log.d(TAG, "AI Assistant button tapped")
            showFeaturePanel(PanelType.AI_ASSISTANT)
        }
        
        // Clipboard Button
        toolbar.findViewById<ImageView>(R.id.btn_clipboard)?.setOnClickListener {
            Log.d(TAG, "Clipboard button tapped")
            showFeaturePanel(PanelType.CLIPBOARD)
        }
        
        // Emoji Button
        toolbar.findViewById<ImageView>(R.id.btn_emoji)?.setOnClickListener {
            Log.d(TAG, "Emoji button tapped")
            showFeaturePanel(PanelType.EMOJI)
        }
        
        // Voice Input Button
        toolbar.findViewById<ImageView>(R.id.btn_voice)?.setOnClickListener {
            Log.d(TAG, "Voice input button tapped")
            try {
                showVoiceInputFeedback(true)
                Toast.makeText(this, "🎤 Voice input activated", Toast.LENGTH_SHORT).show()
                // Reset visual feedback after a delay
                Handler(Looper.getMainLooper()).postDelayed({
                    showVoiceInputFeedback(false)
                }, 2000)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting voice input", e)
                Toast.makeText(this, "Voice input not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Show mini settings sheet in place of keyboard
     */
    private fun showMiniSettingsSheet() {
        try {
            val mainLayout = mainKeyboardLayout ?: return
            val container = keyboardContainer ?: return
            
            // CRITICAL: Close AI panel if open
            if (isAIPanelVisible) {
                closeAIPanel()
            }
            
            // Hide current keyboard view (removes ALL views including AI panel)
            container.removeAllViews()
            
            // Hide toolbar and suggestions
            
            suggestionContainer?.visibility = View.GONE
            
            // Inflate mini settings sheet
            val settingsSheet = layoutInflater.inflate(R.layout.mini_settings_sheet, container, false)
            
            // Apply theme colors to the sheet
            val palette = themeManager.getCurrentPalette()
            settingsSheet.setBackgroundColor(palette.keyboardBg)
            
            // Get current settings
            val prefs = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
            val soundEnabled = prefs.getBoolean("sound_enabled", true)
            val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
            val aiEnabled = prefs.getBoolean("ai_suggestions", true)
            val numberRow = prefs.getBoolean("show_number_row", false)
            
            // Setup header theming
            settingsSheet.findViewById<TextView>(R.id.settings_header)?.apply {
                setTextColor(palette.keyText)
            }
            
            // Setup switches with current values
            val switchSound = settingsSheet.findViewById<android.widget.Switch>(R.id.switch_sound)
            val switchVibration = settingsSheet.findViewById<android.widget.Switch>(R.id.switch_vibration)
            val switchAI = settingsSheet.findViewById<android.widget.Switch>(R.id.switch_ai_mode)
            val switchNumberRow = settingsSheet.findViewById<android.widget.Switch>(R.id.switch_number_row)
            
            switchSound?.isChecked = soundEnabled
            switchVibration?.isChecked = vibrationEnabled
            switchAI?.isChecked = aiEnabled
            switchNumberRow?.isChecked = numberRow
            
            // Setup switch listeners
            switchSound?.setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("sound_enabled", checked).apply()
                this.soundEnabled = checked
                sendSettingToFlutter("sound_enabled", checked)
                Log.d(TAG, "Sound ${if (checked) "enabled" else "disabled"}")
            }
            
            switchVibration?.setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("vibration_enabled", checked).apply()
                this.vibrationEnabled = checked
                sendSettingToFlutter("vibration_enabled", checked)
                Log.d(TAG, "Vibration ${if (checked) "enabled" else "disabled"}")
            }
            
            switchAI?.setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("ai_suggestions", checked).apply()
                this.aiSuggestionsEnabled = checked
                sendSettingToFlutter("ai_suggestions", checked)
                Log.d(TAG, "AI Suggestions ${if (checked) "enabled" else "disabled"}")
            }
            
            switchNumberRow?.setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("show_number_row", checked).apply()
                this.showNumberRow = checked
                sendSettingToFlutter("show_number_row", checked)
                Log.d(TAG, "Number row ${if (checked) "enabled" else "disabled"}")
                // Keyboard will reload when returning to keyboard view
            }
            
            // Setup back button
            settingsSheet.findViewById<android.widget.Button>(R.id.btn_back)?.apply {
                // Apply theme color to button
                val bgDrawable = background?.mutate()
                bgDrawable?.setTint(palette.specialAccent)
                background = bgDrawable
                
                setOnClickListener {
                    restoreKeyboardFromSettings()
                }
            }
            
            // Apply theme colors to all text labels
            val labels = listOf(
                R.id.settings_header
            )
            labels.forEach { id ->
                settingsSheet.findViewById<TextView>(id)?.setTextColor(palette.keyText)
            }
            
            // Add settings sheet to container
            container.addView(settingsSheet)
            isMiniSettingsVisible = true
            
            Log.d(TAG, "✅ Mini settings sheet displayed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing mini settings sheet", e)
            Toast.makeText(this, "Error opening settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Restore keyboard view from mini settings sheet
     */
    private fun restoreKeyboardFromSettings() {
        try {
            val container = keyboardContainer ?: return
            
            // CRITICAL: Ensure AI panel is closed
            if (isAIPanelVisible) {
                aiPanel?.visibility = View.GONE
                isAIPanelVisible = false
            }
            
            // Remove settings sheet
            container.removeAllViews()
            
            // Check if number row setting changed - need to reload keyboard layout
            if (isMiniSettingsVisible) {
                val prefs = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
                val newNumberRow = prefs.getBoolean("show_number_row", false)
                
                // If number row changed, recreate keyboard with new layout
                if (newNumberRow != showNumberRow) {
                    showNumberRow = newNumberRow
                    reloadKeyboard()
                } else {
                    // Just restore existing keyboard view
                    keyboardView?.let { container.addView(it) }
                }
            } else {
                // Just restore existing keyboard view
                keyboardView?.let { container.addView(it) }
            }
            
            // Show toolbar and suggestions again
            
            suggestionContainer?.visibility = View.VISIBLE
            
            isMiniSettingsVisible = false
            Log.d(TAG, "✅ Keyboard restored from settings sheet")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring keyboard", e)
            Toast.makeText(this, "Error restoring keyboard", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Send setting change to Flutter via SharedPreferences
     * Flutter's MethodChannel monitors SharedPreferences changes
     */
    private fun sendSettingToFlutter(key: String, value: Any) {
        try {
            // Settings are already saved to SharedPreferences
            // Flutter side will sync via the existing MethodChannel when needed
            Log.d(TAG, "Setting synced: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing setting to Flutter", e)
        }
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
            switchKeyboardMode(KeyboardMode.LETTERS)
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
     * DEPRECATED: Legacy function - now handled by UnifiedAutocorrectEngine.suggestForSwipe()
     */
    @Deprecated("Use UnifiedAutocorrectEngine.suggestForSwipe() instead")
    private suspend fun generateSwipeCandidates(
        swipeLetters: String, 
        prev1: String, 
        prev2: String
    ): List<String> = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        val candidates = mutableListOf<String>()
        
        try {
            // Use UnifiedAutocorrectEngine for swipe candidates
            val suggestions = autocorrectEngine.getCorrections(
                swipeLetters, 
                currentLanguage, 
                listOfNotNull(prev1, prev2).filter { it.isNotBlank() }
            )
            
            candidates.addAll(suggestions.map { it.text })
                
            val processingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Swipe candidates generated in ${processingTime}ms for '$swipeLetters' -> ${candidates.size} candidates")
            
            return@withContext candidates.take(10)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating swipe candidates", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Generate candidates using Damerau-Levenshtein edit distance
     * DEPRECATED: Legacy function from old AutocorrectEngine - now handled by UnifiedAutocorrectEngine
     */
    @Deprecated("Use UnifiedAutocorrectEngine.getCorrections() instead")
    private suspend fun generateEditDistanceCandidates(
        swipeLetters: String, 
        prev1: String, 
        prev2: String
    ): List<String> = withContext(Dispatchers.Default) {
        try {
            // Use enhanced autocorrect engine for edit distance candidates
            val autocorrectCandidates = autocorrectEngine.getCorrections(swipeLetters, currentLanguage, listOfNotNull(prev1, prev2).filter { it.isNotBlank() })
            return@withContext autocorrectCandidates.map { it.text }
        } catch (e: Exception) {
            Log.e(TAG, "Error in edit distance candidates", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Update suggestion strip with enhanced swipe candidates (clean, no icons)
     * DEPRECATED: This method is no longer used - swipe suggestions now handled directly
     */
    @Deprecated("Swipe suggestions now handled in onSwipeDetected()")
    private fun updateEnhancedSwipeSuggestions(candidates: List<String>, originalSwipe: String) {
        try {
            if (candidates.isEmpty()) {
                updateSuggestionUI(listOf(originalSwipe))
                return
            }
            
            val suggestionTexts = mutableListOf<String>()
            
            // Always show original swipe as first option (for reversion)
            suggestionTexts.add(originalSwipe)
            
            // Add top candidates without any icons or indicators
            candidates.take(2).forEach { candidate ->
                suggestionTexts.add(candidate) // Clean text only, no icons
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
    /**
     * Update swipe suggestion strip
     * DEPRECATED: Legacy function - now using unified suggestion system
     */
    @Deprecated("Use updateSuggestionUI() directly")
    private fun updateSwipeSuggestionStrip(candidates: List<String>) {
        try {
            updateSuggestionUI(candidates.take(3))
            Log.d(TAG, "Swipe suggestion strip updated with ${candidates.size} options: $candidates")
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
                // gboardEmojiPanel removed - using EmojiPanelController only
            
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
                Log.d(TAG, "🔍 Checking expansion for '$word', text before: '$textBefore'")
                
                // Verify the word actually exists at the end of the text
                val wordInText = textBefore.takeLast(word.length).lowercase()
                if (wordInText == word.lowercase()) {
                    // Delete the shortcut that was just typed
                    ic.deleteSurroundingText(word.length, 0)
                    
                    // Insert the expansion with a space
                    ic.commitText("${expansion.expansion} ", 1)
                    
                    // Increment usage count
                    dictionaryManager.incrementUsage(word)
                    
                    Log.d(TAG, "✅ Dictionary expansion: $word -> ${expansion.expansion}")
                } else {
                    Log.w(TAG, "⚠️ Word mismatch: expected '$word' but found '$wordInText' in text field")
                    // Try alternative: finish composing and then replace
                    ic.finishComposingText()
                    ic.deleteSurroundingText(word.length, 0)
                    ic.commitText("${expansion.expansion} ", 1)
                    dictionaryManager.incrementUsage(word)
                    Log.d(TAG, "✅ Dictionary expansion (alternative method): $word -> ${expansion.expansion}")
                }
            } else {
                Log.d(TAG, "No expansion found for: $word")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking dictionary expansion", e)
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
        // ✅ FIX: Use safe call instead of !!
        syncRunnable?.let { runnable ->
            syncHandler?.postDelayed(runnable, 2 * 60 * 1000L)
            Log.d(TAG, "Periodic sync scheduled every ${syncInterval / 1000 / 60} minutes")
        }
    }
    
    /**
     * Suspend helper to wait until a specific language dictionary is ready
     */
    private suspend fun UnifiedAutocorrectEngine.waitUntilReady(lang: String) {
        repeat(10) {
            if (hasLanguage(lang)) {
                return
            }
            delay(300)
        }
        Log.w("UnifiedAutocorrectEngine", "⚠️ Timeout waiting for $lang dictionary")
    }
    
    /**
     * 🔄 Unified AI suggestion updater
     * Combines offline dictionary + emoji + AI pipeline
     */
    private suspend fun updateAISuggestions(currentWord: String, contextWords: List<String>) {
        try {
            if (!::autocorrectEngine.isInitialized) return
            
            // Get suggestions from unified autocorrect engine
            // Note: contextWords not directly used by current API, but kept for future enhancement
            val suggestions = autocorrectEngine.getSuggestions(currentWord, currentLanguage, 5)
            
            // updateSuggestionUI() now handles main thread posting internally
            updateSuggestionUI(suggestions)
        } catch (e: Exception) {
            Log.e("AIKeyboardService", "Error updating AI suggestions", e)
        }
    }
    
    /**
     * 🧩 Log engine readiness status for debugging
     */
    private fun logEngineStatus() {
        val langReady = if (::autocorrectEngine.isInitialized) autocorrectEngine.hasLanguage("en") else false
        Log.d("AIKeyboardService", "🧩 Engine readiness check — Unified=$langReady (swipe built-in)")
    }
    
    /**
     * Thread-safe helper for posting UI updates on the main thread.
     */
    private inline fun safeMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post { block() }
        }
    }
    
    /**
     * UNIFIED LANGUAGE ACTIVATION FUNCTION
     * Single entry point for activating any language with Firebase-only data
     * 
     * This function:
     * 1. Downloads language data from Firebase if needed
     * 2. Preloads the language into MultilingualDictionary
     * 3. Sets the language in UnifiedAutocorrectEngine
     * 4. Updates all dependent components
     * 
     * @param lang Language code to activate (e.g., "en", "hi")
     */
    // Thread-safe activation tracking to prevent duplicate calls
    private val activatingLanguages = mutableSetOf<String>()
    
    suspend fun activateLanguage(lang: String) {
        // Prevent concurrent activation of same language
        synchronized(activatingLanguages) {
            if (activatingLanguages.contains(lang)) {
                Log.d(TAG, "⏳ Language $lang already being activated, waiting...")
                return
            }
            activatingLanguages.add(lang)
        }
        
        try {
            Log.d(TAG, "🌐 Activating Firebase language: $lang")
            
            // Check if already activated to prevent duplicate work
            if (autocorrectEngine.hasLanguage(lang)) {
                Log.d(TAG, "✅ Language $lang already activated, skipping")
                return
            }
            
            // Step 1: Ensure Firebase data is available (download if needed)
            multilingualDictionary.ensureLanguageAvailable(lang)
            
            // Step 2: Preload language into memory
            multilingualDictionary.preload(lang)
            
            // Step 3: Wait for resources with retry logic
            var resources: LanguageResources? = null
            var attempts = 0
            val maxAttempts = 10
            
            while (resources == null && attempts < maxAttempts) {
                resources = multilingualDictionary.get(lang)
                if (resources == null) {
                    attempts++
                    Log.w(TAG, "⏳ Waiting for Firebase resources to load... attempt $attempts/$maxAttempts")
                    delay(300) // Wait 300ms between attempts
                }
            }
            
            if (resources != null) {
                // Step 4: Set language in unified engine
                autocorrectEngine.setLanguage(lang, resources)
                
                // Step 5: Double-verify engine readiness with retry
                var isReady = false
                var readinessAttempts = 0
                val maxReadinessAttempts = 3
                
                while (!isReady && readinessAttempts < maxReadinessAttempts) {
                    isReady = autocorrectEngine.hasLanguage(lang)
                    if (!isReady) {
                        readinessAttempts++
                        Log.w(TAG, "⏳ Engine readiness check failed, retrying... $readinessAttempts/$maxReadinessAttempts")
                        delay(200)
                    }
                }
                
                if (isReady) {
                    Log.d(TAG, "✅ Activated Firebase language: $lang words=${resources.words.size}")
                    
                    // Step 6: Update current language tracking
                    currentLanguage = lang
                    
                    // Step 7: Update dependent components on main thread
                    withContext(Dispatchers.Main) {
                        // Update keyboard layout if in letters mode
                        if (currentKeyboardMode == KeyboardMode.LETTERS) {
                            loadLanguageLayout(lang)
                        }
                        
                        // Update language switch view
                        languageSwitchView?.refreshDisplay()
                        
                        // Clear current input and refresh suggestions
                        currentWord = ""
                        updateAISuggestions()
                    }
                    
                    Log.d(TAG, "🔁 Language switched to $lang (Firebase data)")
                } else {
                    Log.e(TAG, "❌ Engine readiness check failed for $lang after $maxReadinessAttempts attempts")
                    throw IllegalStateException("Engine not ready after activation for $lang")
                }
            } else {
                Log.e(TAG, "❌ Failed to get Firebase resources for language: $lang after $maxAttempts attempts")
                throw IllegalStateException("Failed to load Firebase resources for $lang")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error activating Firebase language $lang", e)
            throw e
        } finally {
            // Always remove from activating set
            synchronized(activatingLanguages) {
                activatingLanguages.remove(lang)
            }
        }
    }
    
    /**
     * PUBLIC API: Handle language download completion from MainActivity
     * This method should be called after MainActivity successfully downloads language files
     * 
     * @param lang Language code that was downloaded (e.g., "hi", "te", "ta")
     */
    fun onLanguageDownloaded(lang: String) {
        Log.d(TAG, "🎯 onLanguageDownloaded() called for $lang from MainActivity")
        
        coroutineScope.launch {
            try {
                // Activate the downloaded language in unified engine
                activateLanguage(lang)
                
                // If this is the current language, ensure UI is updated
                if (lang == currentLanguage) {
                    withContext(Dispatchers.Main) {
                        if (currentKeyboardMode == KeyboardMode.LETTERS) {
                            loadLanguageLayout(lang)
                        }
                        updateAISuggestions()
                    }
                }
                
                Log.d(TAG, "✅ Language $lang fully activated after download")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error activating downloaded language $lang", e)
            }
        }
    }
    
    /**
     * Handle language fully activated callback (download + preload + engine setup)
     * This is called automatically when a language completes its full initialization
     */
    private suspend fun onLanguageFullyActivated(lang: String) {
        try {
            LogUtil.d(TAG, "🎯 onLanguageFullyActivated() called for $lang")
            
            // Get the loaded resources from MultilingualDictionary
            val resources = multilingualDictionary.get(lang)
            if (resources != null) {
                // Set language in unified engine
                autocorrectEngine.setLanguage(lang, resources)
                LogUtil.d(TAG, "✅ UnifiedAutocorrectEngine activated for $lang: words=${resources.words.size}")
                
                // If this is the current language, update UI
                if (lang == currentLanguage) {
                    withContext(Dispatchers.Main) {
                        // Update keyboard layout if in letters mode
                        if (currentKeyboardMode == KeyboardMode.LETTERS) {
                            loadLanguageLayout(lang)
                        }
                        
                        // Update language switch view
                        languageSwitchView?.refreshDisplay()
                        
                        // Refresh suggestions
                        updateAISuggestions()
                        
                        LogUtil.d(TAG, "✅ UI updated for activated language: $lang")
                    }
                }
                
                LogUtil.d(TAG, "✅ Language $lang fully activated and ready for suggestions")
                
            } else {
                LogUtil.e(TAG, "❌ No resources found for activated language: $lang")
            }
            
        } catch (e: Exception) {
            LogUtil.e(TAG, "❌ Error in onLanguageFullyActivated for $lang", e)
        }
    }
    
    
    
    /**
     * Refresh LanguageResources after user data changes (NEW UNIFIED APPROACH)
     * Called when user dictionary or shortcuts are updated
     */
    private fun refreshLanguageResourcesAsync() {
        coroutineScope.launch {
            try {
                Log.d(TAG, "🔄 Refreshing Firebase resources for $currentLanguage with updated user data")
                
                // Only refresh if the language is already loaded to prevent excessive activations
                if (autocorrectEngine.hasLanguage(currentLanguage)) {
                    // Just reload the current language to pick up user data changes
                    multilingualDictionary.preload(currentLanguage)
                    val resources = multilingualDictionary.get(currentLanguage)
                    if (resources != null) {
                        autocorrectEngine.setLanguage(currentLanguage, resources)
                        Log.d(TAG, "✅ Firebase resources refreshed: ${resources.userWords.size} user words, ${resources.shortcuts.size} shortcuts")
                    }
                } else {
                    Log.w(TAG, "⚠️ Language $currentLanguage not loaded, skipping refresh")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error refreshing Firebase resources", e)
            }
        }
    }
}