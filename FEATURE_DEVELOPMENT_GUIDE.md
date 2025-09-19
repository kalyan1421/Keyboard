# AI Keyboard - Step-by-Step Feature Development Guide

## Table of Contents
1. [System-Wide Keyboard Development Steps](#system-wide-keyboard-development-steps)
2. [In-App Keyboard Development Steps](#in-app-keyboard-development-steps)
3. [Feature Implementation Workflows](#feature-implementation-workflows)
4. [Testing and Validation Procedures](#testing-and-validation-procedures)
5. [Troubleshooting Guide](#troubleshooting-guide)

---

## System-Wide Keyboard Development Steps

### Step 1: Project Setup and Migration

#### 1.1 Java to Kotlin Migration
**Objective**: Convert existing Java codebase to modern Kotlin

**Steps**:
1. **Update build.gradle.kts**:
   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       kotlin("plugin.serialization") version "1.9.10"
   }
   
   dependencies {
       implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
       implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
       implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
   }
   ```

2. **Convert MainActivity.java to MainActivity.kt**:
   ```kotlin
   class MainActivity : FlutterActivity() {
       private val CHANNEL = "com.example.ai_keyboard/keyboard"
       
       override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
           super.configureFlutterEngine(flutterEngine)
           MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
               .setMethodCallHandler { call, result ->
                   when (call.method) {
                       "checkKeyboardEnabled" -> {
                           result.success(isKeyboardEnabled())
                       }
                       "updateSettings" -> {
                           updateKeyboardSettings(call.arguments as Map<String, Any>)
                           result.success(true)
                       }
                       else -> result.notImplemented()
                   }
               }
       }
   }
   ```

3. **Replace Java Collections with Kotlin Collections**:
   ```kotlin
   // Before (Java)
   ArrayList<String> suggestions = new ArrayList<>();
   HashMap<String, Integer> wordCount = new HashMap<>();
   
   // After (Kotlin)
   val suggestions = mutableListOf<String>()
   val wordCount = mutableMapOf<String, Int>()
   ```

4. **Convert AsyncTask to Kotlin Coroutines**:
   ```kotlin
   // Before (Java)
   private class SuggestionTask extends AsyncTask<String, Void, List<String>> {
       @Override
       protected List<String> doInBackground(String... words) {
           return getSuggestions(words[0]);
       }
   }
   
   // After (Kotlin)
   private fun loadSuggestions(word: String) {
       CoroutineScope(Dispatchers.IO).launch {
           val suggestions = getSuggestions(word)
           withContext(Dispatchers.Main) {
               updateSuggestionUI(suggestions)
           }
       }
   }
   ```

#### 1.2 InputMethodService Setup
**Objective**: Create the foundation for system-wide keyboard

**Steps**:
1. **Create AIKeyboardService.kt**:
   ```kotlin
   class AIKeyboardService : InputMethodService(), 
       KeyboardView.OnKeyboardActionListener,
       SwipeKeyboardView.SwipeListener {
       
       companion object {
           private const val TAG = "AIKeyboardService"
       }
       
       private var keyboardView: SwipeKeyboardView? = null
       private var keyboard: Keyboard? = null
       private lateinit var settings: SharedPreferences
   }
   ```

2. **Register Service in AndroidManifest.xml**:
   ```xml
   <service
       android:name=".AIKeyboardService"
       android:exported="true"
       android:permission="android.permission.BIND_INPUT_METHOD">
       <intent-filter>
           <action android:name="android.view.InputMethod" />
       </intent-filter>
       <meta-data
           android:name="android.view.im"
           android:resource="@xml/method" />
   </service>
   ```

3. **Create method.xml Configuration**:
   ```xml
   <input-method xmlns:android="http://schemas.android.com/apk/res/android"
       android:settingsActivity="com.example.ai_keyboard.MainActivity"
       android:supportsSwitchingToNextInputMethod="true">
   </input-method>
   ```

### Step 2: Core Keyboard Implementation

#### 2.1 Keyboard Layout Definition
**Objective**: Create XML-based keyboard layouts

**Steps**:
1. **Create qwerty.xml Layout**:
   ```xml
   <Keyboard xmlns:android="http://schemas.android.com/apk/res/android"
       android:keyWidth="10%p"
       android:horizontalGap="0px"
       android:verticalGap="0px"
       android:keyHeight="@dimen/key_height">
       
       <Row>
           <Key android:codes="113" android:keyLabel="q" android:keyWidth="9%p"/>
           <Key android:codes="119" android:keyLabel="w" android:keyWidth="9%p"/>
           <Key android:codes="101" android:keyLabel="e" android:keyWidth="9%p"/>
           <!-- ... continue for all keys -->
       </Row>
       
       <Row>
           <Key android:codes="97" android:keyLabel="a" android:keyWidth="9%p"/>
           <!-- ... -->
       </Row>
       
       <Row>
           <Key android:codes="-1" android:keyIcon="@drawable/sym_keyboard_shift" android:keyWidth="15%p"/>
           <Key android:codes="122" android:keyLabel="z" android:keyWidth="9%p"/>
           <!-- ... -->
           <Key android:codes="-5" android:keyIcon="@drawable/sym_keyboard_delete" android:keyWidth="15%p"/>
       </Row>
   </Keyboard>
   ```

2. **Create numbers.xml and symbols.xml**:
   ```xml
   <!-- numbers.xml -->
   <Keyboard xmlns:android="http://schemas.android.com/apk/res/android"
       android:keyWidth="10%p"
       android:keyHeight="@dimen/key_height">
       <Row>
           <Key android:codes="49" android:keyLabel="1"/>
           <Key android:codes="50" android:keyLabel="2"/>
           <!-- ... -->
       </Row>
   </Keyboard>
   ```

3. **Implement Layout Switching Logic**:
   ```kotlin
   private fun switchToSymbols() {
       if (currentKeyboard != KEYBOARD_SYMBOLS) {
           keyboard = Keyboard(this, R.xml.symbols)
           currentKeyboard = KEYBOARD_SYMBOLS
           keyboardView?.keyboard = keyboard
           keyboardView?.invalidateAllKeys()
       }
   }
   
   private fun switchToLetters() {
       if (currentKeyboard != KEYBOARD_LETTERS) {
           keyboard = Keyboard(this, R.xml.qwerty)
           currentKeyboard = KEYBOARD_LETTERS
           keyboardView?.keyboard = keyboard
           keyboardView?.invalidateAllKeys()
       }
   }
   ```

#### 2.2 Key Event Handling
**Objective**: Process user key presses and convert to text input

**Steps**:
1. **Implement onKey Method**:
   ```kotlin
   override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
       val ic = currentInputConnection ?: return
       
       when (primaryCode) {
           Keyboard.KEYCODE_DELETE -> handleBackspace(ic)
           Keyboard.KEYCODE_SHIFT -> handleShift()
           Keyboard.KEYCODE_DONE -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
           KEYCODE_SPACE -> handleSpace(ic)
           KEYCODE_SYMBOLS -> switchToSymbols()
           KEYCODE_LETTERS -> switchToLetters()
           KEYCODE_NUMBERS -> switchToNumbers()
           else -> handleCharacter(primaryCode, ic)
       }
       
       if (aiSuggestionsEnabled && primaryCode != Keyboard.KEYCODE_DELETE) {
           updateAISuggestions()
       }
   }
   ```

2. **Implement Character Handling**:
   ```kotlin
   private fun handleCharacter(primaryCode: Int, ic: InputConnection) {
       var code = primaryCode.toChar()
       
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
       }
       
       if (Character.isLetter(code)) {
           currentWord += Character.toLowerCase(code)
       }
       
       ic.commitText(code.toString(), 1)
   }
   ```

### Step 3: Advanced Shift Management Implementation

#### 3.1 Three-State Shift System
**Objective**: Implement lowercase → uppercase → caps lock → lowercase cycle

**Steps**:
1. **Define Shift States**:
   ```kotlin
   companion object {
       private const val SHIFT_OFF = 0
       private const val SHIFT_ON = 1 
       private const val SHIFT_CAPS = 2
       private const val DOUBLE_TAP_TIMEOUT = 300L
   }
   
   private var shiftState = SHIFT_OFF
   private var lastShiftPressTime = 0L
   ```

2. **Implement Enhanced Shift Handler**:
   ```kotlin
   private fun handleShift() {
       val now = System.currentTimeMillis()
       
       when (shiftState) {
           SHIFT_OFF -> {
               shiftState = SHIFT_ON
               lastShiftPressTime = now
           }
           SHIFT_ON -> {
               if (now - lastShiftPressTime < DOUBLE_TAP_TIMEOUT) {
                   shiftState = SHIFT_CAPS // Double tap = caps lock
               } else {
                   shiftState = SHIFT_OFF // Single tap after timeout
               }
           }
           SHIFT_CAPS -> {
               shiftState = SHIFT_OFF // Turn off caps lock
           }
       }
       
       keyboardView?.let {
           it.isShifted = (shiftState != SHIFT_OFF)
           it.invalidateAllKeys()
       }
   }
   ```

3. **Visual Feedback for Shift States**:
   ```kotlin
   private fun updateShiftKeyAppearance() {
       val shiftKey = findShiftKey()
       shiftKey?.let { key ->
           when (shiftState) {
               SHIFT_OFF -> {
                   key.icon = getDrawable(R.drawable.sym_keyboard_shift)
                   key.label = null
               }
               SHIFT_ON -> {
                   key.icon = getDrawable(R.drawable.sym_keyboard_shift_locked)
                   key.label = null
               }
               SHIFT_CAPS -> {
                   key.icon = getDrawable(R.drawable.sym_keyboard_caps_lock)
                   key.label = "CAPS"
               }
           }
       }
   }
   ```

### Step 4: Long-Press Accent System

#### 4.1 Accent Mapping Definition
**Objective**: Create comprehensive accent character mappings

**Steps**:
1. **Define Accent Mappings**:
   ```kotlin
   private val accentMap = mapOf(
       // Vowels with comprehensive accent support
       'a'.code to listOf("á", "à", "â", "ä", "ã", "å", "ā", "ă", "ą", "ǎ", "ȧ", "ạ"),
       'e'.code to listOf("é", "è", "ê", "ë", "ē", "ĕ", "ė", "ę", "ě", "ȩ", "ẹ", "ế"),
       'i'.code to listOf("í", "ì", "î", "ï", "ī", "ĭ", "į", "ı", "ǐ", "ȋ", "ị", "ί"),
       'o'.code to listOf("ó", "ò", "ô", "ö", "õ", "ō", "ŏ", "ő", "ø", "ǒ", "ȯ", "ọ"),
       'u'.code to listOf("ú", "ù", "û", "ü", "ū", "ŭ", "ů", "ű", "ų", "ǔ", "ȕ", "ụ"),
       
       // Consonants with accent variants
       'c'.code to listOf("ç", "ć", "ĉ", "ċ", "č"),
       'n'.code to listOf("ñ", "ń", "ņ", "ň", "ŉ", "ŋ"),
       's'.code to listOf("ś", "ŝ", "ş", "š", "ș"),
       'z'.code to listOf("ź", "ż", "ž", "ẑ"),
       
       // Numbers with superscripts/subscripts
       '0'.code to listOf("°", "₀", "⁰"),
       '1'.code to listOf("¹", "₁", "½", "⅓", "¼", "⅛"),
       '2'.code to listOf("²", "₂", "⅔", "⅖"),
       
       // Currency symbols
       '$'.code to listOf("¢", "£", "€", "¥", "₹", "₽", "₩", "₪", "₦", "₡")
   )
   ```

2. **Implement Long-Press Detection**:
   ```kotlin
   override fun onPress(primaryCode: Int) {
       keyboardView?.let { view ->
           showKeyPreview(primaryCode)
           performAdvancedHapticFeedback(primaryCode)
           
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
       }
   }
   
   private fun hasAccentVariants(primaryCode: Int): Boolean {
       return accentMap.containsKey(primaryCode)
   }
   ```

3. **Create Accent Options Popup**:
   ```kotlin
   private fun showAccentOptions(primaryCode: Int) {
       val accents = accentMap[primaryCode] ?: return
       if (accents.isEmpty()) return
       
       hideAccentOptions() // Clean up any existing popup
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
       
       // Add accent variants
       accents.forEach { accent ->
           val adjustedAccent = if (shiftState != SHIFT_OFF && accent.length == 1) {
               accent.uppercase()
           } else {
               accent
           }
           addAccentOption(container, adjustedAccent)
       }
       
       accentPopup = PopupWindow(container, WRAP_CONTENT, WRAP_CONTENT).apply {
           setBackgroundDrawable(ColorDrawable(Color.WHITE))
           isFocusable = false // Critical: Prevent focus stealing
           isOutsideTouchable = true
           isTouchable = true
           inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
           elevation = 8f
       }
       
       keyboardView?.let { view ->
           accentPopup?.showAsDropDown(view, 0, -view.height - 150, Gravity.CENTER_HORIZONTAL)
       }
   }
   ```

4. **Handle Accent Selection**:
   ```kotlin
   private fun addAccentOption(container: LinearLayout, accent: String) {
       val textView = TextView(this).apply {
           text = accent
           textSize = 20f
           setTextColor(Color.BLACK)
           setPadding(16, 12, 16, 12)
           setBackgroundColor(Color.LTGRAY)
           
           setOnTouchListener { _, event ->
               when (event.action) {
                   MotionEvent.ACTION_DOWN -> {
                       setBackgroundColor(Color.GRAY)
                       performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                       true
                   }
                   MotionEvent.ACTION_UP -> {
                       setBackgroundColor(Color.LTGRAY)
                       
                       // Insert the selected accent
                       currentInputConnection?.commitText(accent, 1)
                       
                       // Handle shift state after character input
                       if (shiftState == SHIFT_ON) {
                           shiftState = SHIFT_OFF
                           keyboardView?.let {
                               it.isShifted = false
                               it.invalidateAllKeys()
                           }
                       }
                       
                       hideAccentOptions()
                       
                       if (aiSuggestionsEnabled) {
                           updateAISuggestions()
                       }
                       true
                   }
                   MotionEvent.ACTION_CANCEL -> {
                       setBackgroundColor(Color.LTGRAY)
                       true
                   }
                   else -> false
               }
           }
       }
       
       container.addView(textView, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
           setMargins(4, 0, 4, 0)
       })
   }
   ```

### Step 5: Enhanced Haptic Feedback

#### 5.1 Context-Aware Vibration System
**Objective**: Implement different vibration patterns for different key types

**Steps**:
1. **Initialize Vibration System**:
   ```kotlin
   override fun onCreate() {
       super.onCreate()
       // ... other initialization
       vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
       longPressHandler = Handler(Looper.getMainLooper())
   }
   ```

2. **Implement Advanced Haptic Feedback**:
   ```kotlin
   private fun performAdvancedHapticFeedback(primaryCode: Int) {
       if (!vibrationEnabled || vibrator == null) return
       
       try {
           val intensity = when (primaryCode) {
               Keyboard.KEYCODE_DELETE, KEYCODE_SHIFT -> 
                   VibrationEffect.DEFAULT_AMPLITUDE * 1.2f
               KEYCODE_SPACE, Keyboard.KEYCODE_DONE -> 
                   VibrationEffect.DEFAULT_AMPLITUDE * 0.8f
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
               HapticFeedbackConstants.KEYBOARD_TAP,
               HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
           )
       }
   }
   ```

3. **Special Haptic Patterns**:
   ```kotlin
   private fun performLongPressHaptic() {
       try {
           if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
               vibrator?.vibrate(VibrationEffect.createOneShot(50L, 200))
           } else {
               @Suppress("DEPRECATION")
               vibrator?.vibrate(50L)
           }
       } catch (e: Exception) {
           keyboardView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
       }
   }
   ```

### Step 6: AI Suggestion Engine

#### 6.1 Built-in Suggestion System
**Objective**: Create intelligent text prediction without external APIs

**Steps**:
1. **Create AIServiceBridge.kt**:
   ```kotlin
   class AIServiceBridge {
       private val commonWords = mutableSetOf<String>()
       private val userDictionary = mutableMapOf<String, Int>()
       private val contextHistory = mutableListOf<String>()
       private val wordPairs = mutableMapOf<String, Int>()
       
       data class Suggestion(
           val word: String,
           val confidence: Float,
           val isCorrection: Boolean = false
       )
       
       fun initialize() {
           loadCommonWords()
           loadUserDictionary()
       }
   }
   ```

2. **Implement Suggestion Algorithm**:
   ```kotlin
   fun getSuggestions(currentWord: String, context: List<String>): List<Suggestion> {
       val suggestions = mutableListOf<Suggestion>()
       
       // Context-aware suggestions
       if (context.isNotEmpty()) {
           val lastWord = context.last().lowercase()
           val contextualSuggestions = getContextualSuggestions(lastWord, currentWord)
           suggestions.addAll(contextualSuggestions)
       }
       
       // Frequency-based suggestions
       val frequencySuggestions = getFrequencyBasedSuggestions(currentWord)
       suggestions.addAll(frequencySuggestions)
       
       // Auto-corrections
       val corrections = getAutoCorrections(currentWord)
       suggestions.addAll(corrections)
       
       // Remove duplicates and sort by confidence
       return suggestions.distinctBy { it.word }
           .sortedByDescending { it.confidence }
           .take(3)
   }
   
   private fun getContextualSuggestions(lastWord: String, currentWord: String): List<Suggestion> {
       val suggestions = mutableListOf<Suggestion>()
       
       // Common word pairs
       val commonPairs = mapOf(
           "how" to listOf("are", "do", "can", "will", "much"),
           "what" to listOf("is", "are", "do", "time", "about"),
           "where" to listOf("is", "are", "do", "can", "will"),
           "when" to listOf("is", "are", "do", "will", "did"),
           "why" to listOf("is", "are", "do", "did", "would")
       )
       
       commonPairs[lastWord]?.forEach { suggestion ->
           if (suggestion.startsWith(currentWord, ignoreCase = true)) {
               suggestions.add(Suggestion(suggestion, 0.9f, false))
           }
       }
       
       // User-learned pairs
       wordPairs.forEach { (pair, frequency) ->
           val parts = pair.split(" ")
           if (parts.size == 2 && parts[0] == lastWord) {
               val suggestion = parts[1]
               if (suggestion.startsWith(currentWord, ignoreCase = true)) {
                   val confidence = minOf(0.8f, frequency / 10.0f)
                   suggestions.add(Suggestion(suggestion, confidence, false))
               }
           }
       }
       
       return suggestions
   }
   ```

3. **Implement Learning System**:
   ```kotlin
   fun learnFromInput(word: String, context: List<String>) {
       // Update word frequency
       userDictionary[word] = userDictionary.getOrDefault(word, 0) + 1
       
       // Learn word pairs for contextual suggestions
       if (context.isNotEmpty()) {
           val lastWord = context.last()
           val pair = "$lastWord $word"
           wordPairs[pair] = wordPairs.getOrDefault(pair, 0) + 1
       }
       
       // Maintain context history
       contextHistory.add(word)
       if (contextHistory.size > 10) {
           contextHistory.removeAt(0)
       }
   }
   
   fun adaptToUser(selectedWord: String, alternatives: List<String>) {
       // Increase confidence for selected word
       userDictionary[selectedWord] = userDictionary.getOrDefault(selectedWord, 0) + 2
       
       // Slightly decrease confidence for alternatives
       alternatives.forEach { alt ->
           if (alt != selectedWord) {
               val current = userDictionary.getOrDefault(alt, 0)
               if (current > 0) {
                   userDictionary[alt] = current - 1
               }
           }
       }
   }
   ```

### Step 7: Swipe Typing Implementation

#### 7.1 Custom SwipeKeyboardView
**Objective**: Create gesture-based text input system

**Steps**:
1. **Create SwipeKeyboardView.kt**:
   ```kotlin
   class SwipeKeyboardView @JvmOverloads constructor(
       context: Context,
       attrs: AttributeSet? = null,
       defStyleAttr: Int = 0
   ) : KeyboardView(context, attrs, defStyleAttr) {
       
       interface SwipeListener {
           fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String)
           fun onSwipeStarted()
           fun onSwipeEnded()
       }
       
       private var swipeListener: SwipeListener? = null
       private val swipePath = mutableListOf<PointF>()
       private var isSwipeMode = false
       private var swipeStartTime = 0L
   }
   ```

2. **Implement Touch Event Handling**:
   ```kotlin
   override fun onTouchEvent(event: MotionEvent): Boolean {
       when (event.action) {
           MotionEvent.ACTION_DOWN -> {
               swipePath.clear()
               swipePath.add(PointF(event.x, event.y))
               swipeStartTime = System.currentTimeMillis()
               isSwipeMode = false
           }
           
           MotionEvent.ACTION_MOVE -> {
               swipePath.add(PointF(event.x, event.y))
               
               // Detect if this is a swipe (movement over threshold)
               if (!isSwipeMode && swipePath.size > 1) {
                   val distance = calculatePathDistance()
                   if (distance > SWIPE_START_THRESHOLD) {
                       isSwipeMode = true
                       swipeListener?.onSwipeStarted()
                   }
               }
           }
           
           MotionEvent.ACTION_UP -> {
               if (isSwipeMode) {
                   processSwipeGesture()
                   swipeListener?.onSwipeEnded()
               } else {
                   // Regular key press
                   return super.onTouchEvent(event)
               }
               isSwipeMode = false
           }
       }
       
       return if (isSwipeMode) true else super.onTouchEvent(event)
   }
   ```

3. **Process Swipe Gesture**:
   ```kotlin
   private fun processSwipeGesture() {
       if (swipePath.size < 2) return
       
       val swipeTime = System.currentTimeMillis() - swipeStartTime
       if (swipeTime < MIN_SWIPE_TIME) return
       
       val distance = calculatePathDistance()
       if (distance < MIN_SWIPE_DISTANCE) return
       
       val swipedKeys = detectSwipedKeys()
       val swipePattern = generateSwipePattern()
       
       swipeListener?.onSwipeDetected(swipedKeys, swipePattern)
   }
   
   private fun detectSwipedKeys(): List<Int> {
       val keys = mutableListOf<Int>()
       val keyboard = keyboard ?: return keys
       
       swipePath.forEach { point ->
           val keyIndex = getKeyIndices(point.x.toInt(), point.y.toInt())
           if (keyIndex.isNotEmpty()) {
               val key = keyboard.keys[keyIndex[0]]
               if (key.codes.isNotEmpty() && !keys.contains(key.codes[0])) {
                   keys.add(key.codes[0])
               }
           }
       }
       
       return keys
   }
   ```

---

## In-App Keyboard Development Steps

### Step 1: Flutter Project Structure

#### 1.1 Create Widget Architecture
**Objective**: Build modular, testable Flutter keyboard components

**Steps**:
1. **Create lib/models/keyboard_state.dart**:
   ```dart
   enum ShiftState {
     lowercase,
     uppercase,
     capsLock
   }
   
   class KeyboardState extends ChangeNotifier {
     ShiftState _shiftState = ShiftState.lowercase;
     bool _isShiftEngaged = false;
     
     ShiftState get shiftState => _shiftState;
     bool get isShiftEngaged => _isShiftEngaged;
     
     void handleShiftPress() {
       switch (_shiftState) {
         case ShiftState.lowercase:
           _shiftState = ShiftState.uppercase;
           _isShiftEngaged = true;
           break;
         case ShiftState.uppercase:
           _shiftState = ShiftState.capsLock;
           _isShiftEngaged = true;
           break;
         case ShiftState.capsLock:
           _shiftState = ShiftState.lowercase;
           _isShiftEngaged = false;
           break;
       }
       notifyListeners();
     }
     
     void handleCharacterInput() {
       if (_shiftState == ShiftState.uppercase) {
         _shiftState = ShiftState.lowercase;
         _isShiftEngaged = false;
         notifyListeners();
       }
     }
   }
   ```

2. **Create lib/models/keyboard_layout.dart**:
   ```dart
   enum KeyboardLayout {
     qwerty,
     numbers,
     symbols,
   }
   
   class LayoutManager {
     static const List<List<String>> qwertyLayout = [
       ['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'],
       ['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'],
       ['⇧', 'z', 'x', 'c', 'v', 'b', 'n', 'm', '⌫'],
       ['🌐', '123', 'space', '⏎'],
     ];
     
     static List<List<String>> getLayout(KeyboardLayout layout) {
       switch (layout) {
         case KeyboardLayout.qwerty:
           return qwertyLayout;
         case KeyboardLayout.numbers:
           return numberLayout;
         case KeyboardLayout.symbols:
           return symbolLayout;
       }
     }
   }
   ```

### Step 2: Individual Key Component

#### 2.1 Create KeyboardKey Widget
**Objective**: Build reusable key component with gesture support

**Steps**:
1. **Create lib/widgets/keyboard_key.dart**:
   ```dart
   class KeyboardKey extends StatefulWidget {
     final String keyValue;
     final Function(String) onKeyPressed;
     final Function(String)? onKeyLongPressed;
     final double? width;
     final double? height;
     final bool isShiftKey;
     final bool isBackspaceKey;
     
     const KeyboardKey({
       Key? key,
       required this.keyValue,
       required this.onKeyPressed,
       this.onKeyLongPressed,
       this.width,
       this.height,
       this.isShiftKey = false,
       this.isBackspaceKey = false,
     }) : super(key: key);
     
     @override
     _KeyboardKeyState createState() => _KeyboardKeyState();
   }
   ```

2. **Implement Gesture Handling**:
   ```dart
   class _KeyboardKeyState extends State<KeyboardKey> 
       with SingleTickerProviderStateMixin {
     
     late AnimationController _animationController;
     late Animation<double> _scaleAnimation;
     GlobalKey _key = GlobalKey();
     
     @override
     void initState() {
       super.initState();
       _animationController = AnimationController(
         vsync: this,
         duration: const Duration(milliseconds: 100),
       );
       _scaleAnimation = Tween<double>(begin: 1.0, end: 0.95)
           .animate(CurvedAnimation(
         parent: _animationController,
         curve: Curves.easeInOut,
       ));
     }
     
     void _onTapDown(TapDownDetails details) {
       _animationController.forward();
       _showKeyPreview(details.globalPosition);
       HapticService.keyPress();
     }
     
     void _onLongPressStart(LongPressStartDetails details) {
       _animationController.forward();
       HapticService.longPress();
       _showAccentOptions(details.globalPosition);
     }
   }
   ```

### Step 3: Haptic Service Implementation

#### 3.1 Cross-Platform Haptic Feedback
**Objective**: Provide consistent haptic feedback across platforms

**Steps**:
1. **Create lib/services/haptic_service.dart**:
   ```dart
   import 'package:flutter/services.dart';
   
   enum HapticFeedbackIntensity {
     light,
     medium,
     heavy,
   }
   
   class HapticService {
     static bool _isEnabled = true;
     static HapticFeedbackIntensity _intensity = HapticFeedbackIntensity.medium;
     
     static Future<void> keyPress() async {
       if (!_isEnabled) return;
       try {
         switch (_intensity) {
           case HapticFeedbackIntensity.light:
             await HapticFeedback.lightImpact();
             break;
           case HapticFeedbackIntensity.medium:
             await HapticFeedback.mediumImpact();
             break;
           case HapticFeedbackIntensity.heavy:
             await HapticFeedback.heavyImpact();
             break;
         }
       } catch (e) {
         HapticFeedback.selectionClick();
       }
     }
     
     static Future<void> longPress() async {
       if (!_isEnabled) return;
       try {
         await HapticFeedback.heavyImpact();
       } catch (e) {
         HapticFeedback.vibrate();
       }
     }
   }
   ```

### Step 4: Key Preview System

#### 4.1 Overlay-Based Previews
**Objective**: Show character previews and accent options using Flutter overlays

**Steps**:
1. **Create lib/widgets/key_preview_popup.dart**:
   ```dart
   class KeyPreviewPopup extends StatelessWidget {
     final String character;
     final Offset position;
     final bool visible;
     final double keyWidth;
     final double keyHeight;
     
     const KeyPreviewPopup({
       Key? key,
       required this.character,
       required this.position,
       required this.visible,
       this.keyWidth = 40.0,
       this.keyHeight = 50.0,
     }) : super(key: key);
     
     @override
     Widget build(BuildContext context) {
       if (!visible) return const SizedBox.shrink();
       
       const popupWidth = 60.0;
       const popupHeight = 80.0;
       const popupOffset = 20.0;
       
       final left = position.dx + (keyWidth / 2) - (popupWidth / 2);
       final top = position.dy - popupHeight - popupOffset;
       
       final screenWidth = MediaQuery.of(context).size.width;
       final adjustedLeft = left.clamp(8.0, screenWidth - popupWidth - 8.0);
       final adjustedTop = top.clamp(8.0, MediaQuery.of(context).size.height - popupHeight - 8.0);
       
       return Positioned(
         left: adjustedLeft,
         top: adjustedTop,
         child: Material(
           color: Colors.transparent,
           child: Container(
             width: popupWidth,
             height: popupHeight,
             decoration: BoxDecoration(
               color: Colors.white,
               borderRadius: BorderRadius.circular(8),
               boxShadow: [
                 BoxShadow(
                   color: Colors.black.withOpacity(0.3),
                   blurRadius: 8,
                   offset: const Offset(0, 4),
                 ),
               ],
             ),
             child: Center(
               child: Text(
                 character,
                 style: const TextStyle(
                   color: Colors.black,
                   fontSize: 24,
                   fontWeight: FontWeight.bold,
                 ),
               ),
             ),
           ),
         ),
       );
     }
   }
   ```

2. **Implement Preview Manager**:
   ```dart
   class KeyPreviewManager {
     static OverlayEntry? _currentPreview;
     static OverlayEntry? _currentAccentOptions;
     
     static void showKeyPreview({
       required BuildContext context,
       required String character,
       required Offset position,
       double keyWidth = 40.0,
       double keyHeight = 50.0,
     }) {
       hideKeyPreview();
       _currentPreview = OverlayEntry(
         builder: (context) => KeyPreviewPopup(
           character: character,
           position: position,
           visible: true,
           keyWidth: keyWidth,
           keyHeight: keyHeight,
         ),
       );
       Overlay.of(context).insert(_currentPreview!);
     }
     
     static void hideKeyPreview() {
       _currentPreview?.remove();
       _currentPreview = null;
     }
   }
   ```

### Step 5: Main Keyboard Widget

#### 5.1 Advanced Keyboard Integration
**Objective**: Combine all components into a complete keyboard widget

**Steps**:
1. **Create lib/widgets/advanced_keyboard.dart**:
   ```dart
   class AdvancedKeyboard extends StatefulWidget {
     final Function(String) onTextInput;
     final VoidCallback onBackspace;
     final VoidCallback onEnter;
     
     const AdvancedKeyboard({
       Key? key,
       required this.onTextInput,
       required this.onBackspace,
       required this.onEnter,
     }) : super(key: key);
     
     @override
     _AdvancedKeyboardState createState() => _AdvancedKeyboardState();
   }
   
   class _AdvancedKeyboardState extends State<AdvancedKeyboard> {
     KeyboardLayout _currentLayout = KeyboardLayout.qwerty;
     
     @override
     void initState() {
       super.initState();
       HapticService.initialize();
     }
     
     void _onKeyPressed(String keyValue, KeyboardState keyboardState) {
       if (LayoutManager.isShiftKey(keyValue)) {
         keyboardState.handleShiftPress();
         HapticService.specialKey();
       } else if (LayoutManager.isBackspaceKey(keyValue)) {
         widget.onBackspace();
         HapticService.specialKey();
       } else if (LayoutManager.isLayoutSwitchKey(keyValue)) {
         _handleLayoutSwitch(keyValue, keyboardState);
       } else {
         _handleCharacterInput(keyValue, keyboardState);
       }
     }
     
     @override
     Widget build(BuildContext context) {
       final keyboardState = Provider.of<KeyboardState>(context);
       final layout = LayoutManager.getLayout(_currentLayout);
       
       return Container(
         color: Colors.grey[200],
         padding: const EdgeInsets.all(8.0),
         child: Column(
           children: layout.map((row) {
             return Row(
               mainAxisAlignment: MainAxisAlignment.spaceEvenly,
               children: row.map((key) {
                 return KeyboardKey(
                   keyValue: key,
                   onKeyPressed: (char) => _onKeyPressed(char, keyboardState),
                   isShiftKey: LayoutManager.isShiftKey(key),
                   isBackspaceKey: LayoutManager.isBackspaceKey(key),
                 );
               }).toList(),
             );
           }).toList(),
         ),
       );
     }
   }
   ```

### Step 6: Test Application

#### 6.1 Comprehensive Testing Environment
**Objective**: Create testing interface for keyboard functionality

**Steps**:
1. **Create Test Screen**:
   ```dart
   class KeyboardTestScreen extends StatefulWidget {
     const KeyboardTestScreen({Key? key}) : super(key: key);
     
     @override
     _KeyboardTestScreenState createState() => _KeyboardTestScreenState();
   }
   
   class _KeyboardTestScreenState extends State<KeyboardTestScreen> {
     final TextEditingController _textController = TextEditingController();
     final FocusNode _focusNode = FocusNode();
     
     @override
     Widget build(BuildContext context) {
       return Scaffold(
         appBar: AppBar(title: const Text('Advanced Keyboard Test')),
         body: Column(
           children: [
             Expanded(
               child: Padding(
                 padding: const EdgeInsets.all(16.0),
                 child: TextField(
                   controller: _textController,
                   focusNode: _focusNode,
                   maxLines: null,
                   expands: true,
                   decoration: const InputDecoration(
                     hintText: 'Start typing to test the keyboard...',
                     border: OutlineInputBorder(),
                   ),
                   readOnly: true,
                 ),
               ),
             ),
             AdvancedKeyboard(
               onTextInput: (text) => _handleTextInput(text),
               onBackspace: () => _handleBackspace(),
               onEnter: () => _handleEnter(),
             ),
           ],
         ),
       );
     }
     
     void _handleTextInput(String text) {
       final currentText = _textController.text;
       final selection = _textController.selection;
       
       final startOffset = selection.start.clamp(0, currentText.length);
       final endOffset = selection.end.clamp(0, currentText.length);
       
       final newText = currentText.replaceRange(startOffset, endOffset, text);
       final newOffset = (startOffset + text.length).clamp(0, newText.length);
       
       _textController.value = TextEditingValue(
         text: newText,
         selection: TextSelection.collapsed(offset: newOffset),
       );
     }
   }
   ```

---

## Feature Implementation Workflows

### Workflow 1: Adding a New Keyboard Layout

**Steps**:
1. **Create XML Layout File** (Android):
   ```xml
   <!-- res/xml/new_layout.xml -->
   <Keyboard xmlns:android="http://schemas.android.com/apk/res/android">
       <Row>
           <Key android:codes="..." android:keyLabel="..." />
       </Row>
   </Keyboard>
   ```

2. **Add Layout Constant**:
   ```kotlin
   companion object {
       private const val KEYBOARD_NEW_LAYOUT = 4
   }
   ```

3. **Update Layout Manager** (Flutter):
   ```dart
   enum KeyboardLayout {
     qwerty,
     numbers,
     symbols,
     newLayout, // Add new layout
   }
   
   static const List<List<String>> newLayoutDefinition = [
     // Define layout structure
   ];
   ```

4. **Add Switch Logic**:
   ```kotlin
   private fun switchToNewLayout() {
       if (currentKeyboard != KEYBOARD_NEW_LAYOUT) {
           keyboard = Keyboard(this, R.xml.new_layout)
           currentKeyboard = KEYBOARD_NEW_LAYOUT
           keyboardView?.keyboard = keyboard
           keyboardView?.invalidateAllKeys()
       }
   }
   ```

### Workflow 2: Adding New Accent Characters

**Steps**:
1. **Update Accent Map**:
   ```kotlin
   private val accentMap = mapOf(
       // Existing mappings...
       'ñ'.code to listOf("ň", "ņ", "ń", "ŋ"), // Add new character
   )
   ```

2. **Test Accent Display**:
   ```kotlin
   // Test in onPress method
   if (hasAccentVariants('ñ'.code)) {
       showAccentOptions('ñ'.code)
   }
   ```

3. **Update Flutter Accent Mappings**:
   ```dart
   // lib/utils/accent_mappings.dart
   static const Map<String, List<String>> accents = {
     'ñ': ['ň', 'ņ', 'ń', 'ŋ'], // Add corresponding Flutter mapping
   };
   ```

### Workflow 3: Adding New AI Suggestion Logic

**Steps**:
1. **Extend Suggestion Algorithm**:
   ```kotlin
   private fun getCustomSuggestions(word: String): List<Suggestion> {
       val suggestions = mutableListOf<Suggestion>()
       
       // Add custom logic here
       if (word.startsWith("th")) {
           suggestions.add(Suggestion("the", 0.8f, false))
           suggestions.add(Suggestion("that", 0.7f, false))
           suggestions.add(Suggestion("this", 0.6f, false))
       }
       
       return suggestions
   }
   ```

2. **Integrate with Main Algorithm**:
   ```kotlin
   fun getSuggestions(currentWord: String, context: List<String>): List<Suggestion> {
       val suggestions = mutableListOf<Suggestion>()
       
       // Existing suggestions...
       suggestions.addAll(getContextualSuggestions(lastWord, currentWord))
       suggestions.addAll(getFrequencyBasedSuggestions(currentWord))
       suggestions.addAll(getCustomSuggestions(currentWord)) // Add custom
       
       return suggestions.distinctBy { it.word }
           .sortedByDescending { it.confidence }
           .take(3)
   }
   ```

---

## Testing and Validation Procedures

### Test 1: System-Wide Keyboard Integration

**Procedure**:
1. **Install APK**: `flutter build apk && adb install build/app/outputs/flutter-apk/app-debug.apk`
2. **Enable Keyboard**: Settings → System → Languages & Input → Virtual Keyboard → Manage keyboards → AI Keyboard
3. **Set as Default**: Settings → System → Languages & Input → Virtual Keyboard → Default keyboard → AI Keyboard
4. **Test in Multiple Apps**: 
   - SMS app
   - Email app
   - Browser search
   - Social media apps

**Validation Criteria**:
- ✅ Keyboard appears in all tested apps
- ✅ All key presses register correctly
- ✅ Shift states work properly
- ✅ Long-press accents appear
- ✅ Swipe typing functions
- ✅ AI suggestions display

### Test 2: Long-Press Accent Functionality

**Procedure**:
1. **Test Each Vowel**:
   ```
   Press and hold 'a' → Should show: á, à, â, ä, ã, å, ā, ă, ą
   Press and hold 'e' → Should show: é, è, ê, ë, ē, ĕ, ė, ę, ě
   Press and hold 'i' → Should show: í, ì, î, ï, ī, ĭ, į, ı
   Press and hold 'o' → Should show: ó, ò, ô, ö, õ, ō, ŏ, ő, ø
   Press and hold 'u' → Should show: ú, ù, û, ü, ū, ŭ, ů, ű, ų
   ```

2. **Test Special Characters**:
   ```
   Press and hold '$' → Should show: ¢, £, €, ¥, ₹, ₽, ₩
   Press and hold '1' → Should show: ¹, ₁, ½, ⅓, ¼
   ```

3. **Test Shift State Integration**:
   ```
   Enable shift → Press and hold 'a' → Should show uppercase accents
   Enable caps lock → Press and hold 'a' → Should show uppercase accents
   ```

**Validation Criteria**:
- ✅ All accent options appear within 500ms
- ✅ Popup positioned correctly above key
- ✅ Tapping accent inserts character
- ✅ Tapping outside dismisses popup
- ✅ Keyboard remains stable throughout

### Test 3: Advanced Shift Management

**Procedure**:
1. **Test Single Tap**:
   ```
   Tap shift → Type 'a' → Should produce 'A', then return to lowercase
   ```

2. **Test Double Tap**:
   ```
   Double tap shift (within 300ms) → Type 'abc' → Should produce 'ABC'
   ```

3. **Test Triple Tap**:
   ```
   Triple tap shift → Should return to lowercase mode
   ```

**Validation Criteria**:
- ✅ Single tap capitalizes next character only
- ✅ Double tap enables caps lock
- ✅ Triple tap returns to lowercase
- ✅ Visual indicator shows current shift state
- ✅ Shift state persists across layout switches

### Test 4: AI Suggestion Accuracy

**Procedure**:
1. **Test Contextual Suggestions**:
   ```
   Type "how" → Should suggest: are, do, can, will, much
   Type "what" → Should suggest: is, are, do, time, about
   ```

2. **Test Learning**:
   ```
   Type "my name is John" multiple times
   Type "my" → Should eventually suggest "name"
   Type "my name" → Should eventually suggest "is"
   ```

3. **Test Auto-Correction**:
   ```
   Type "teh" → Should suggest "the" as correction
   Type "recieve" → Should suggest "receive" as correction
   ```

**Validation Criteria**:
- ✅ Contextual suggestions appear within 200ms
- ✅ Learning improves suggestions over time
- ✅ Auto-corrections marked with ✓ indicator
- ✅ Tapping suggestion inserts word correctly
- ✅ Suggestions update as user types

---

## Troubleshooting Guide

### Issue 1: Keyboard Not Appearing in System

**Symptoms**:
- Keyboard doesn't show up in system settings
- Can't select as default keyboard

**Solutions**:
1. **Check AndroidManifest.xml**:
   ```xml
   <service
       android:name=".AIKeyboardService"
       android:exported="true"
       android:permission="android.permission.BIND_INPUT_METHOD">
   ```

2. **Verify method.xml**:
   ```xml
   <input-method xmlns:android="http://schemas.android.com/apk/res/android"
       android:settingsActivity="com.example.ai_keyboard.MainActivity">
   ```

3. **Check Service Implementation**:
   ```kotlin
   class AIKeyboardService : InputMethodService() {
       // Must extend InputMethodService
   }
   ```

### Issue 2: Long-Press Causing Keyboard to Close

**Symptoms**:
- Accent popup appears briefly then keyboard closes
- "no window focus" errors in logs

**Solutions**:
1. **Fix PopupWindow Focus**:
   ```kotlin
   accentPopup = PopupWindow(container, WRAP_CONTENT, WRAP_CONTENT).apply {
       isFocusable = false // Critical: Don't steal focus
       isOutsideTouchable = true
       isTouchable = true
       inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
   }
   ```

2. **Clean Up on Swipe Start**:
   ```kotlin
   override fun onSwipeStarted() {
       hideAccentOptions() // Prevent interference
       hideKeyPreview()
       longPressHandler?.removeCallbacksAndMessages(null)
   }
   ```

### Issue 3: Haptic Feedback Not Working

**Symptoms**:
- No vibration on key press
- Crashes when accessing vibrator

**Solutions**:
1. **Add Vibration Permission**:
   ```xml
   <uses-permission android:name="android.permission.VIBRATE" />
   ```

2. **Safe Vibrator Access**:
   ```kotlin
   vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
   
   if (vibrator?.hasVibrator() == true) {
       // Safe to use vibrator
   }
   ```

3. **Fallback Implementation**:
   ```kotlin
   try {
       vibrator?.vibrate(VibrationEffect.createOneShot(duration, intensity))
   } catch (e: Exception) {
       keyboardView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
   }
   ```

### Issue 4: AI Suggestions Not Appearing

**Symptoms**:
- Suggestion bar empty
- No contextual suggestions

**Solutions**:
1. **Check Initialization**:
   ```kotlin
   override fun onCreate() {
       super.onCreate()
       initializeAIBridge() // Must call this
   }
   
   private fun initializeAIBridge() {
       aiBridge.initialize()
   }
   ```

2. **Verify Update Trigger**:
   ```kotlin
   override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
       // ... handle key
       
       if (aiSuggestionsEnabled && primaryCode != Keyboard.KEYCODE_DELETE) {
           updateAISuggestions() // Must call after each key
       }
   }
   ```

3. **Check Word Building**:
   ```kotlin
   private fun handleCharacter(primaryCode: Int, ic: InputConnection) {
       // ...
       if (Character.isLetter(code)) {
           currentWord += Character.toLowerCase(code) // Build current word
       }
   }
   ```

### Issue 5: Flutter In-App Keyboard Layout Issues

**Symptoms**:
- Keys overlapping or misaligned
- Keyboard too wide/narrow for screen

**Solutions**:
1. **Dynamic Key Sizing**:
   ```dart
   double _getDefaultKeyWidth() {
     final screenWidth = MediaQuery.of(context).size.width;
     return (screenWidth - 20) / 10; // Account for padding
   }
   ```

2. **Responsive Layout**:
   ```dart
   Widget build(BuildContext context) {
     return LayoutBuilder(
       builder: (context, constraints) {
         final keyWidth = constraints.maxWidth / 10;
         return Container(
           width: constraints.maxWidth,
           child: buildKeyboard(keyWidth),
         );
       },
     );
   }
   ```

3. **Safe Area Handling**:
   ```dart
   @override
   Widget build(BuildContext context) {
     return SafeArea(
       child: Container(
         padding: EdgeInsets.only(
           bottom: MediaQuery.of(context).viewInsets.bottom,
         ),
         child: AdvancedKeyboard(...),
       ),
     );
   }
   ```

---

This comprehensive guide provides step-by-step instructions for implementing every feature in the AI Keyboard application, from basic setup to advanced functionality. Each section includes detailed code examples, testing procedures, and troubleshooting solutions to ensure successful implementation.
