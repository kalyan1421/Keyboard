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
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import java.util.*
import java.util.concurrent.Executors

class AIKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener, SwipeKeyboardView.SwipeListener {
    
    private var keyboardView: SwipeKeyboardView? = null
    private var keyboard: Keyboard? = null
    private var caps = false
    private var lastShiftTime: Long = 0
    private var isShifted = false
    private var swipeMode = false
    private val swipeBuffer = StringBuilder()
    private val swipePath = mutableListOf<Int>()
    private var swipeStartTime: Long = 0
    private var isCurrentlySwiping = false
    
    // Settings polling
    private var settingsPoller: Runnable? = null
    private var lastSettingsCheck: Long = 0
    
    // AI and suggestion components
    private var suggestionView: TextView? = null
    private var suggestionContainer: LinearLayout? = null
    private val currentSuggestions = mutableListOf<String>()
    private val executorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Settings
    private lateinit var settings: SharedPreferences
    private var currentTheme = "default"
    private var aiSuggestionsEnabled = true
    private var swipeTypingEnabled = true
    private var voiceInputEnabled = true
    private var vibrationEnabled = true
    private var keyPreviewEnabled = false // Disabled by default
    
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
    
    // Keyboard layouts
    companion object {
        private const val KEYBOARD_LETTERS = 1
        private const val KEYBOARD_SYMBOLS = 2
        private const val KEYBOARD_NUMBERS = 3
        private const val KEYCODE_SPACE = 32
        private const val KEYCODE_SYMBOLS = -10
        private const val KEYCODE_LETTERS = -11
        private const val KEYCODE_NUMBERS = -12
        private const val KEYCODE_VOICE = -13
    }
    
    private var currentKeyboard = KEYBOARD_LETTERS
    
    override fun onCreate() {
        super.onCreate()
        settings = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
        loadSettings()
        
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
    
    override fun onCreateInputView(): View {
        // Create the main keyboard layout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getThemeBackgroundColor())
        }
        
        // Create suggestion bar
        createSuggestionBar(mainLayout)
        
        // Create keyboard view with swipe support
        keyboardView = SwipeKeyboardView(this, null).apply {
            keyboard = Keyboard(this@AIKeyboardService, R.xml.qwerty)
            setKeyboard(keyboard)
            setOnKeyboardActionListener(this@AIKeyboardService)
            setSwipeListener(this@AIKeyboardService)
            setSwipeEnabled(swipeTypingEnabled)
            
            // Configure key preview (disable popup)
            setPreviewEnabled(keyPreviewEnabled)
        }
        
        // Apply theme
        applyTheme()
        
        mainLayout.addView(keyboardView)
        return mainLayout
    }
    
    private fun createSuggestionBar(parent: LinearLayout) {
        suggestionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(getThemeKeyColor())
            setPadding(16, 8, 16, 8)
        }
        
        // Add three suggestion text views
        repeat(3) { i ->
            val suggestion = TextView(this).apply {
                setTextColor(getThemeTextColor())
                textSize = 16f
                setPadding(16, 8, 16, 8)
                setBackgroundResource(R.drawable.suggestion_background)
                isClickable = true
                
                setOnClickListener {
                    if (i < currentSuggestions.size) {
                        applySuggestion(currentSuggestions[i])
                    }
                }
            }
            
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(4, 0, 4, 0)
            }
            suggestion.layoutParams = params
            
            suggestionContainer?.addView(suggestion)
        }
        
        parent.addView(suggestionContainer)
    }
    
    private fun loadSettings() {
        currentTheme = settings.getString("keyboard_theme", "default") ?: "default"
        aiSuggestionsEnabled = settings.getBoolean("ai_suggestions", true)
        swipeTypingEnabled = settings.getBoolean("swipe_typing", true)
        voiceInputEnabled = settings.getBoolean("voice_input", true)
        vibrationEnabled = settings.getBoolean("vibration_enabled", true)
        keyPreviewEnabled = settings.getBoolean("key_preview_enabled", false)
    }
    
    private fun applyTheme() {
        keyboardView?.setBackgroundColor(getThemeBackgroundColor())
    }
    
    private fun getThemeBackgroundColor(): Int {
        return when (currentTheme) {
            "dark" -> Color.parseColor("#1E1E1E")
            "material_you" -> Color.parseColor("#6750A4")
            "professional" -> Color.parseColor("#37474F")
            "colorful" -> Color.parseColor("#E1F5FE")
            else -> Color.parseColor("#F5F5F5")
        }
    }
    
    private fun getThemeKeyColor(): Int {
        return when (currentTheme) {
            "dark" -> Color.parseColor("#2D2D2D")
            "material_you" -> Color.parseColor("#7C4DFF")
            "professional" -> Color.parseColor("#455A64")
            "colorful" -> Color.parseColor("#81D4FA")
            else -> Color.WHITE
        }
    }
    
    private fun getThemeTextColor(): Int {
        return when (currentTheme) {
            "dark", "material_you", "professional" -> Color.WHITE
            "colorful" -> Color.parseColor("#0D47A1")
            else -> Color.parseColor("#212121")
        }
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
            KEYCODE_VOICE -> if (voiceInputEnabled) startVoiceInput()
            else -> handleCharacter(primaryCode, ic)
        }
        
        // Update AI suggestions after key press
        if (aiSuggestionsEnabled && primaryCode != Keyboard.KEYCODE_DELETE) {
            updateAISuggestions()
        }
    }
    
    private fun handleCharacter(primaryCode: Int, ic: InputConnection) {
        var code = primaryCode.toChar()
        
        // Enhanced character handling with better case management
        if (Character.isLetter(code)) {
            code = if (caps) Character.toUpperCase(code) else Character.toLowerCase(code)
        }
        
        // Insert character with enhanced text processing
        insertCharacterWithProcessing(ic, code)
        
        // Enhanced auto-shift logic
        updateCapsState(code)
        
        // Update keyboard visual state
        updateKeyboardState()
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
            else -> if (caps && Character.isLetter(code) && !isCapslockEnabled()) {
                // Turn off caps after typing a letter (unless caps lock is on)
                caps = false
            }
        }
        
        // Update visual state if changed
        if (wasCapitalized != caps) {
            keyboardView?.setShifted(caps)
        }
    }
    
    private fun updateKeyboardState() {
        keyboardView?.invalidateAllKeys()
    }
    
    private fun isSmartPunctuationEnabled(): Boolean = settings.getBoolean("smart_punctuation", true)
    private fun isAutoCorrectEnabled(): Boolean = settings.getBoolean("auto_correct", true)
    private fun isCapslockEnabled(): Boolean = settings.getBoolean("caps_lock_active", false)
    private fun isPunctuation(c: Char): Boolean = ".,!?;:".contains(c)
    
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
    
    private fun getAutoCorrection(word: String): String? {
        return when (word.lowercase()) {
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
    }
    
    private fun handleBackspace(ic: InputConnection) {
        val selectedText = ic.getSelectedText(0)
        if (TextUtils.isEmpty(selectedText)) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
    }
    
    private fun handleSpace(ic: InputConnection) {
        ic.commitText(" ", 1)
        updateAISuggestions()
    }
    
    private fun handleShift() {
        val now = System.currentTimeMillis()
        if (lastShiftTime + 800 > now) {
            // Double tap for caps lock
            caps = !caps
            lastShiftTime = 0
        } else {
            // Single tap for shift
            caps = !caps
            lastShiftTime = now
        }
        
        keyboardView?.let {
            it.setShifted(caps)
            it.invalidateAllKeys()
        }
    }
    
    private fun switchToSymbols() {
        if (currentKeyboard != KEYBOARD_SYMBOLS) {
            keyboard = Keyboard(this, R.xml.symbols)
            currentKeyboard = KEYBOARD_SYMBOLS
            keyboardView?.keyboard = keyboard
        }
    }
    
    private fun switchToLetters() {
        if (currentKeyboard != KEYBOARD_LETTERS) {
            keyboard = Keyboard(this, R.xml.qwerty)
            currentKeyboard = KEYBOARD_LETTERS
            keyboardView?.keyboard = keyboard
        }
    }
    
    private fun switchToNumbers() {
        if (currentKeyboard != KEYBOARD_NUMBERS) {
            keyboard = Keyboard(this, R.xml.numbers)
            currentKeyboard = KEYBOARD_NUMBERS
            keyboardView?.keyboard = keyboard
        }
    }
    
    private fun startVoiceInput() {
        // Placeholder for voice input implementation
        // Toast.makeText(this, "Voice input feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateAISuggestions() {
        if (!aiSuggestionsEnabled) return
        
        executorService.execute {
            try {
                val ic = currentInputConnection ?: return@execute
                
                // Get current text context
                val textBefore = ic.getTextBeforeCursor(50, 0)
                val currentText = textBefore?.toString() ?: ""
                
                // Generate AI suggestions (simplified version)
                val suggestions = generateAISuggestions(currentText)
                
                // Update UI on main thread
                mainHandler.post { updateSuggestionUI(suggestions) }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun generateAISuggestions(currentText: String): List<String> {
        // Simplified AI suggestion logic
        val suggestions = mutableListOf<String>()
        
        if (currentText.isEmpty()) {
            suggestions.addAll(listOf("Hello", "How are", "Good"))
        } else {
            val words = currentText.lowercase().split("\\s+".toRegex())
            val lastWord = if (words.isNotEmpty()) words.last() else ""
            
            when (lastWord) {
                "hello" -> suggestions.addAll(listOf("there", "everyone", "friend"))
                "good" -> suggestions.addAll(listOf("morning", "evening", "night"))
                "how" -> suggestions.addAll(listOf("are you", "is it", "about"))
                "thank" -> suggestions.addAll(listOf("you", "you so much", "goodness"))
                "i" -> suggestions.addAll(listOf("am", "will", "think"))
                else -> suggestions.addAll(listOf("and", "the", "to"))
            }
        }
        
        return suggestions.also { currentSuggestions.clear(); currentSuggestions.addAll(it) }
    }
    
    private fun updateSuggestionUI(suggestions: List<String>) {
        suggestionContainer?.let { container ->
            for (i in 0 until minOf(container.childCount, 3)) {
                val suggestionView = container.getChildAt(i) as TextView
                if (i < suggestions.size) {
                    suggestionView.text = suggestions[i]
                    suggestionView.visibility = View.VISIBLE
                } else {
                    suggestionView.visibility = View.INVISIBLE
                }
            }
        }
    }
    
    private fun applySuggestion(suggestion: String) {
        val ic = currentInputConnection ?: return
        
        // Get text before cursor to determine what to replace
        val textBefore = ic.getTextBeforeCursor(50, 0)
        textBefore?.let {
            val words = it.toString().split("\\s+".toRegex())
            if (words.isNotEmpty()) {
                val lastWord = words.last()
                ic.deleteSurroundingText(lastWord.length, 0)
            }
        }
        
        ic.commitText("$suggestion ", 1)
        updateAISuggestions()
    }
    
    private fun playClick(keyCode: Int) {
        val am = getSystemService(AUDIO_SERVICE) as? AudioManager
        am?.let {
            when (keyCode) {
                32 -> it.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR) // Space
                Keyboard.KEYCODE_DONE, 10 -> it.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN) // Enter
                Keyboard.KEYCODE_DELETE -> it.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE)
                else -> it.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
            }
        }
    }
    
    override fun onPress(primaryCode: Int) {
        // Handle key press visual feedback
        keyboardView?.let {
            // Add haptic feedback only if enabled
            if (vibrationEnabled) {
                it.performHapticFeedback(
                    android.view.HapticFeedbackConstants.KEYBOARD_TAP,
                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
            
            // Check if this could be the start of swipe typing
            if (swipeTypingEnabled && Character.isLetter(primaryCode) && !isCurrentlySwiping) {
                // Potential start of swipe typing - wait for movement or release
                swipeStartTime = System.currentTimeMillis()
            }
            
            // Visual press feedback is handled by the key background drawable
            animateKeyPress(primaryCode)
        }
    }
    
    override fun onRelease(primaryCode: Int) {
        // Handle key release visual feedback
        keyboardView?.let {
            animateKeyRelease(primaryCode)
        }
    }
    
    private fun animateKeyPress(primaryCode: Int) {
        // Custom key press animation
        keyboardView?.let {
            // Scale effect for pressed key
            it.scaleX = 0.95f
            it.scaleY = 0.95f
            
            // Reset scale after short delay
            it.postDelayed({
                it.scaleX = 1.0f
                it.scaleY = 1.0f
            }, 100)
        }
    }
    
    private fun animateKeyRelease(primaryCode: Int) {
        // Ensure scale is reset on release
        keyboardView?.let {
            it.scaleX = 1.0f
            it.scaleY = 1.0f
        }
    }
    
    override fun onText(text: CharSequence?) {
        val ic = currentInputConnection ?: return
        text?.let { ic.commitText(it, 1) }
    }
    
    // Swipe gesture handlers
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
            handleBackspace(currentInputConnection ?: return)
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
    
    // SwipeListener implementation
    override fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String) {
        if (swipedKeys.isEmpty()) return
        
        // Convert key codes to characters
        val swipeWord = StringBuilder()
        for (keyCode in swipedKeys) {
            if (keyCode in 1..255) {
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
                currentInputConnection?.let {
                    it.commitText("$finalWord ", 1)
                    updateAISuggestions()
                }
                
                // Show success feedback
                showSwipeSuccess(finalWord)
            }
        }
    }
    
    override fun onSwipeStarted() {
        // Visual feedback for swipe start
        keyboardView?.setBackgroundColor(getSwipeActiveColor())
        
        // Show swipe mode indicator
        showSwipeIndicator(true)
    }
    
    override fun onSwipeEnded() {
        // Reset visual feedback
        keyboardView?.setBackgroundColor(getThemeBackgroundColor())
        
        // Hide swipe mode indicator
        showSwipeIndicator(false)
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
            else -> if (swipeWord.length > 1) swipeWord else ""
        }
    }
    
    private fun showSwipeSuccess(word: String) {
        // Show the swiped word in the first suggestion slot briefly
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as TextView
                firstSuggestion.text = "✓ $word"
                firstSuggestion.setTextColor(Color.parseColor("#4CAF50")) // Green for success
                firstSuggestion.visibility = View.VISIBLE
                
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
                    indicator.text = "🔄 Swipe"
                    indicator.setTextColor(getSwipeTextColor())
                    indicator.visibility = View.VISIBLE
                } else {
                    indicator.visibility = View.INVISIBLE
                }
            }
        }
    }
    
    private fun getSwipeActiveColor(): Int = Color.parseColor("#E3F2FD") // Light blue for swipe mode
    private fun getSwipeTextColor(): Int = Color.parseColor("#1976D2") // Blue for swipe predictions
    
    // Enhanced swipe typing methods (simplified for Kotlin)
    private fun processSwipeMovement(direction: Int) {
        if (!isCurrentlySwiping || !swipeTypingEnabled) return
        
        // Add direction to swipe path for processing
        swipePath.add(direction)
        
        // Process swipe path to predict words
        val predictedWord = processSwipePath(swipePath)
        if (predictedWord.isNotEmpty()) {
            // Update swipe buffer
            swipeBuffer.clear()
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
            currentInputConnection?.let {
                it.commitText("$finalWord ", 1)
                updateAISuggestions()
            }
        }
        
        // Reset visual feedback
        keyboardView?.setBackgroundColor(getThemeBackgroundColor())
        
        // Clear swipe data
        swipePath.clear()
        swipeBuffer.clear()
    }
    
    private fun processSwipePath(path: List<Int>): String {
        if (path.isEmpty()) return ""
        
        // Simple swipe-to-word mapping (basic implementation)
        val word = StringBuilder()
        
        for (code in path) {
            if (code in 1..255) {
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
    
    private fun showSwipePrediction(prediction: String) {
        suggestionContainer?.let { container ->
            if (container.childCount > 0) {
                val firstSuggestion = container.getChildAt(0) as TextView
                firstSuggestion.text = prediction
                firstSuggestion.setTextColor(getSwipeTextColor())
                firstSuggestion.visibility = View.VISIBLE
            }
        }
    }
    
    private fun handleClose() {
        requestHideSelf(0)
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        
        // Reset keyboard state
        caps = false
        keyboardView?.setShifted(caps)
        
        // Auto-capitalize for sentence start
        attribute?.let { attr ->
            if (attr.inputType != 0) {
                val inputType = attr.inputType and EditorInfo.TYPE_MASK_CLASS
                if (inputType == EditorInfo.TYPE_CLASS_TEXT) {
                    val variation = attr.inputType and EditorInfo.TYPE_MASK_VARIATION
                    if (variation == EditorInfo.TYPE_TEXT_VARIATION_NORMAL ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT) {
                        caps = true
                        keyboardView?.setShifted(caps)
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
    
    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
        
        // Stop settings polling
        stopSettingsPolling()
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(settingsReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
        }
    }
    
    private fun applySettingsImmediately() {
        try {
            // Apply theme changes
            applyTheme()
            
            // Update keyboard view settings
            keyboardView?.let {
                it.setPreviewEnabled(keyPreviewEnabled)
                it.setSwipeEnabled(swipeTypingEnabled)
                
                // Force refresh of the keyboard view
                it.invalidateAllKeys()
            }
            
            // Show feedback to user that settings were applied
            suggestionContainer?.let { container ->
                if (container.childCount > 0) {
                    val firstSuggestion = container.getChildAt(0) as TextView
                    firstSuggestion.text = "⚙️ Settings Updated"
                    firstSuggestion.setTextColor(Color.parseColor("#4CAF50"))
                    firstSuggestion.visibility = View.VISIBLE
                    
                    // Reset after 2 seconds
                    mainHandler.postDelayed({
                        try {
                            if (container.childCount > 0) {
                                val suggestion = container.getChildAt(0) as TextView
                                suggestion.visibility = View.INVISIBLE
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
        settingsPoller?.let {
            mainHandler.removeCallbacks(it)
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
                val oldVoiceInput = voiceInputEnabled
                
                // Reload settings
                loadSettings()
                
                // Check if any settings changed
                val settingsChanged = oldTheme != currentTheme ||
                    oldVibration != vibrationEnabled ||
                    oldSwipeTyping != swipeTypingEnabled ||
                    oldKeyPreview != keyPreviewEnabled ||
                    oldAISuggestions != aiSuggestionsEnabled ||
                    oldVoiceInput != voiceInputEnabled
                
                if (settingsChanged) {
                    applySettingsImmediately()
                }
            }
        } catch (e: Exception) {
            // Ignore polling errors
        }
    }
}
