# AI Panel Integration Guide 🚀

## Quick Start: Connecting AI Service to Keyboard Panels

This guide shows **exactly how to wire up** the AI service to your Grammar Fix, Word Tone, and AI Assistant panels.

---

## 📋 Prerequisites

### 1. Initialize AI Service in AIKeyboardService

```kotlin
class AIKeyboardService : InputMethodService() {
    
    // Add AI service instance
    private lateinit var advancedAIService: AdvancedAIService
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize AI service
        advancedAIService = AdvancedAIService(this)
        advancedAIService.preloadWarmup()  // Warm up for faster first use
    }
    
    override fun onDestroy() {
        super.onDestroy()
        advancedAIService.cleanup()  // Clean up resources
    }
}
```

---

## 1️⃣ Grammar Fix Panel Integration

### Update `inflateGrammarBody()` Function

```kotlin
private fun inflateGrammarBody(container: FrameLayout?) {
    val view = layoutInflater.inflate(R.layout.panel_body_grammar, container, false)
    container?.addView(view)
    
    val palette = themeManager.getCurrentPalette()
    view.setBackgroundColor(palette.keyboardBg)
    
    val grammarOutput = view.findViewById<TextView>(R.id.grammarOutput)
    val loadingIndicator = view.findViewById<ProgressBar>(R.id.loadingIndicator)  // Add to XML
    val replaceButton = view.findViewById<Button>(R.id.btnReplaceText)
    
    // Apply theme
    grammarOutput?.apply {
        setTextColor(palette.keyText)
        setHintTextColor(Color.argb(128, Color.red(palette.keyText), 
            Color.green(palette.keyText), Color.blue(palette.keyText)))
    }
    
    // REPHRASE BUTTON
    view.findViewById<Button>(R.id.btnRephrase)?.setOnClickListener {
        processGrammarFeature(
            feature = AdvancedAIService.ProcessingFeature.EXPAND,
            outputView = grammarOutput,
            loadingView = loadingIndicator
        )
    }
    
    // FIX GRAMMAR BUTTON ✅
    view.findViewById<Button>(R.id.btnGrammarFix)?.setOnClickListener {
        processGrammarFeature(
            feature = AdvancedAIService.ProcessingFeature.GRAMMAR_FIX,
            outputView = grammarOutput,
            loadingView = loadingIndicator
        )
    }
    
    // ADD EMOJIS BUTTON
    view.findViewById<Button>(R.id.btnAddEmojis)?.setOnClickListener {
        processCustomPrompt(
            prompt = "Add relevant emojis to this text to make it more expressive",
            outputView = grammarOutput,
            loadingView = loadingIndicator
        )
    }
    
    // REPLACE TEXT BUTTON
    replaceButton?.setOnClickListener {
        val correctedText = grammarOutput?.text?.toString() ?: return@setOnClickListener
        
        if (correctedText.isNotEmpty()) {
            // Delete current text
            val ic = currentInputConnection
            val textBefore = ic?.getTextBeforeCursor(10000, 0)?.toString() ?: ""
            ic?.deleteSurroundingText(textBefore.length, 0)
            
            // Insert corrected text
            ic?.commitText(correctedText, 1)
            
            // Show success feedback
            Toast.makeText(this, "✅ Text replaced", Toast.LENGTH_SHORT).show()
            
            // Return to keyboard
            restoreKeyboardFromPanel()
        }
    }
}

/**
 * Process text with grammar/text processing feature
 */
private fun processGrammarFeature(
    feature: AdvancedAIService.ProcessingFeature,
    outputView: TextView?,
    loadingView: ProgressBar?
) {
    // Get current input text
    val ic = currentInputConnection ?: return
    val selectedText = ic.getSelectedText(0)?.toString()
    val currentText = if (!selectedText.isNullOrEmpty()) {
        selectedText  // Use selected text if available
    } else {
        ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
    }
    
    if (currentText.isEmpty()) {
        Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Show loading
    loadingView?.visibility = View.VISIBLE
    outputView?.text = "Processing..."
    
    // Process with AI
    lifecycleScope.launch {
        val result = advancedAIService.processText(
            text = currentText,
            feature = feature
        )
        
        withContext(Dispatchers.Main) {
            loadingView?.visibility = View.GONE
            
            if (result.success) {
                outputView?.text = result.text
                
                // Show cache indicator
                if (result.fromCache) {
                    outputView?.append("\n💾 (from cache)")
                }
                
                Log.d(TAG, "✅ Grammar processed in ${result.processingTimeMs}ms")
            } else {
                outputView?.text = "❌ Error: ${result.error}"
                Toast.makeText(
                    this@AIKeyboardService, 
                    result.error ?: "Unknown error", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/**
 * Process text with custom prompt
 */
private fun processCustomPrompt(
    prompt: String,
    outputView: TextView?,
    loadingView: ProgressBar?
) {
    val ic = currentInputConnection ?: return
    val currentText = ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
    
    if (currentText.isEmpty()) {
        Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
        return
    }
    
    loadingView?.visibility = View.VISIBLE
    outputView?.text = "Processing..."
    
    lifecycleScope.launch {
        val result = advancedAIService.processWithCustomPrompt(
            text = currentText,
            systemPrompt = prompt,
            promptTitle = "custom"
        )
        
        withContext(Dispatchers.Main) {
            loadingView?.visibility = View.GONE
            
            if (result.success) {
                outputView?.text = result.text
            } else {
                outputView?.text = "❌ Error: ${result.error}"
            }
        }
    }
}
```

### Update `panel_body_grammar.xml`

Add loading indicator:

```xml
<LinearLayout ...>
    
    <!-- Action Buttons -->
    <HorizontalScrollView>...</HorizontalScrollView>
    
    <!-- Loading Indicator -->
    <ProgressBar
        android:id="@+id/loadingIndicator"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:layout_marginVertical="8dp"
        android:visibility="gone"/>
    
    <!-- Result Output -->
    <TextView android:id="@+id/grammarOutput" .../>
    
    <!-- Replace Button -->
    <Button android:id="@+id/btnReplaceText" .../>
    
</LinearLayout>
```

---

## 2️⃣ Word Tone Panel Integration

### Update `inflateToneBody()` Function

```kotlin
private fun inflateToneBody(container: FrameLayout?) {
    val view = layoutInflater.inflate(R.layout.panel_body_tone, container, false)
    container?.addView(view)
    
    val palette = themeManager.getCurrentPalette()
    view.setBackgroundColor(palette.keyboardBg)
    
    val toneOutput = view.findViewById<TextView>(R.id.toneOutput)
    val loadingIndicator = view.findViewById<ProgressBar>(R.id.loadingIndicator)  // Add to XML
    val replaceButton = view.findViewById<Button>(R.id.btnReplaceToneText)
    
    // Apply theme
    toneOutput?.apply {
        setTextColor(palette.keyText)
        setHintTextColor(Color.argb(128, Color.red(palette.keyText), 
            Color.green(palette.keyText), Color.blue(palette.keyText)))
    }
    
    // FUNNY TONE
    view.findViewById<Button>(R.id.btnFunny)?.setOnClickListener {
        processToneAdjustment(
            tone = AdvancedAIService.ToneType.FUNNY,
            outputView = toneOutput,
            loadingView = loadingIndicator
        )
    }
    
    // POETIC TONE (map to FORMAL)
    view.findViewById<Button>(R.id.btnPoetic)?.setOnClickListener {
        processToneAdjustment(
            tone = AdvancedAIService.ToneType.FORMAL,
            outputView = toneOutput,
            loadingView = loadingIndicator
        )
    }
    
    // SHORTEN
    view.findViewById<Button>(R.id.btnShorten)?.setOnClickListener {
        processGrammarFeature(
            feature = AdvancedAIService.ProcessingFeature.SHORTEN,
            outputView = toneOutput,
            loadingView = loadingIndicator
        )
    }
    
    // SARCASTIC (map to CASUAL)
    view.findViewById<Button>(R.id.btnSarcastic)?.setOnClickListener {
        processToneAdjustment(
            tone = AdvancedAIService.ToneType.CASUAL,
            outputView = toneOutput,
            loadingView = loadingIndicator
        )
    }
    
    // REPLACE TEXT BUTTON
    replaceButton?.setOnClickListener {
        val adjustedText = toneOutput?.text?.toString() ?: return@setOnClickListener
        
        if (adjustedText.isNotEmpty() && !adjustedText.startsWith("❌")) {
            val ic = currentInputConnection
            val textBefore = ic?.getTextBeforeCursor(10000, 0)?.toString() ?: ""
            ic?.deleteSurroundingText(textBefore.length, 0)
            ic?.commitText(adjustedText, 1)
            
            Toast.makeText(this, "✅ Tone applied", Toast.LENGTH_SHORT).show()
            restoreKeyboardFromPanel()
        }
    }
}

/**
 * Process text with tone adjustment
 */
private fun processToneAdjustment(
    tone: AdvancedAIService.ToneType,
    outputView: TextView?,
    loadingView: ProgressBar?
) {
    val ic = currentInputConnection ?: return
    val selectedText = ic.getSelectedText(0)?.toString()
    val currentText = if (!selectedText.isNullOrEmpty()) {
        selectedText
    } else {
        ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
    }
    
    if (currentText.isEmpty()) {
        Toast.makeText(this, "⚠️ No text to adjust", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Show loading
    loadingView?.visibility = View.VISIBLE
    outputView?.text = "Adjusting tone..."
    
    // Process with AI
    lifecycleScope.launch {
        val result = advancedAIService.adjustTone(
            text = currentText,
            tone = tone
        )
        
        withContext(Dispatchers.Main) {
            loadingView?.visibility = View.GONE
            
            if (result.success) {
                outputView?.text = result.text
                
                // Show tone indicator
                outputView?.append("\n${tone.icon} ${tone.displayName}")
                
                if (result.fromCache) {
                    outputView?.append(" 💾")
                }
                
                Log.d(TAG, "✅ Tone adjusted in ${result.processingTimeMs}ms")
            } else {
                outputView?.text = "❌ Error: ${result.error}"
                Toast.makeText(
                    this@AIKeyboardService, 
                    result.error ?: "Unknown error", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
```

---

## 3️⃣ AI Assistant Panel Integration

### Update `inflateAIAssistantBody()` Function

```kotlin
private fun inflateAIAssistantBody(container: FrameLayout?) {
    val view = layoutInflater.inflate(R.layout.panel_body_ai_assistant, container, false)
    container?.addView(view)
    
    val palette = themeManager.getCurrentPalette()
    view.setBackgroundColor(palette.keyboardBg)
    
    val aiOutput = view.findViewById<TextView>(R.id.aiOutput)
    val loadingIndicator = view.findViewById<ProgressBar>(R.id.loadingIndicator)  // Add to XML
    val replaceButton = view.findViewById<Button>(R.id.btnReplaceAIText)
    
    // Apply theme
    aiOutput?.apply {
        setTextColor(palette.keyText)
        setHintTextColor(Color.argb(128, Color.red(palette.keyText), 
            Color.green(palette.keyText), Color.blue(palette.keyText)))
    }
    
    // CHATGPT - General AI assistance
    view.findViewById<Button>(R.id.btnChatGPT)?.setOnClickListener {
        processAIAssistance(
            prompt = "Improve this text and make it more professional and clear",
            outputView = aiOutput,
            loadingView = loadingIndicator,
            title = "ChatGPT"
        )
    }
    
    // HUMANIZE - Make text natural
    view.findViewById<Button>(R.id.btnHumanize)?.setOnClickListener {
        processAIAssistance(
            prompt = "Rewrite this text to sound more natural, human, and conversational",
            outputView = aiOutput,
            loadingView = loadingIndicator,
            title = "Humanize"
        )
    }
    
    // REPLY - Generate smart replies
    view.findViewById<Button>(R.id.btnReply)?.setOnClickListener {
        generateSmartReplies(
            outputView = aiOutput,
            loadingView = loadingIndicator
        )
    }
    
    // IDIOMS - Add idiomatic expressions
    view.findViewById<Button>(R.id.btnIdioms)?.setOnClickListener {
        processAIAssistance(
            prompt = "Rewrite this text using appropriate idioms and expressions to make it more engaging",
            outputView = aiOutput,
            loadingView = loadingIndicator,
            title = "Idioms"
        )
    }
    
    // REPLACE TEXT BUTTON
    replaceButton?.setOnClickListener {
        val aiText = aiOutput?.text?.toString() ?: return@setOnClickListener
        
        if (aiText.isNotEmpty() && !aiText.startsWith("❌")) {
            val ic = currentInputConnection
            val textBefore = ic?.getTextBeforeCursor(10000, 0)?.toString() ?: ""
            ic?.deleteSurroundingText(textBefore.length, 0)
            ic?.commitText(aiText, 1)
            
            Toast.makeText(this, "✅ AI text inserted", Toast.LENGTH_SHORT).show()
            restoreKeyboardFromPanel()
        }
    }
}

/**
 * Process with AI assistance
 */
private fun processAIAssistance(
    prompt: String,
    outputView: TextView?,
    loadingView: ProgressBar?,
    title: String
) {
    val ic = currentInputConnection ?: return
    val currentText = ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
    
    if (currentText.isEmpty()) {
        Toast.makeText(this, "⚠️ No text to process", Toast.LENGTH_SHORT).show()
        return
    }
    
    loadingView?.visibility = View.VISIBLE
    outputView?.text = "$title processing..."
    
    lifecycleScope.launch {
        val result = advancedAIService.processWithCustomPrompt(
            text = currentText,
            systemPrompt = prompt,
            promptTitle = title.lowercase()
        )
        
        withContext(Dispatchers.Main) {
            loadingView?.visibility = View.GONE
            
            if (result.success) {
                outputView?.text = result.text
                
                if (result.fromCache) {
                    outputView?.append("\n💾 (cached)")
                }
            } else {
                outputView?.text = "❌ Error: ${result.error}"
                Toast.makeText(
                    this@AIKeyboardService, 
                    result.error ?: "Unknown error", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/**
 * Generate smart replies
 */
private fun generateSmartReplies(
    outputView: TextView?,
    loadingView: ProgressBar?
) {
    val ic = currentInputConnection ?: return
    val receivedMessage = ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
    
    if (receivedMessage.isEmpty()) {
        Toast.makeText(this, "⚠️ No message to reply to", Toast.LENGTH_SHORT).show()
        return
    }
    
    loadingView?.visibility = View.VISIBLE
    outputView?.text = "Generating replies..."
    
    lifecycleScope.launch {
        val result = advancedAIService.generateSmartReplies(
            message = receivedMessage,
            context = "general",
            count = 3
        )
        
        withContext(Dispatchers.Main) {
            loadingView?.visibility = View.GONE
            
            if (result.success) {
                outputView?.text = result.text
            } else {
                outputView?.text = "❌ Error: ${result.error}"
            }
        }
    }
}
```

---

## 4️⃣ Add Required Imports

Add to top of `AIKeyboardService.kt`:

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.ProgressBar
```

---

## 5️⃣ Add LifecycleOwner Support

Since `InputMethodService` doesn't implement `LifecycleOwner` by default, add:

```kotlin
class AIKeyboardService : InputMethodService(), LifecycleOwner {
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    
    override fun getLifecycle(): Lifecycle = lifecycleRegistry
    
    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        // ... rest of onCreate
    }
    
    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
```

Or use alternative coroutine scope:

```kotlin
// Add to AIKeyboardService
private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

// Replace lifecycleScope.launch with:
serviceScope.launch { ... }

// Clean up in onDestroy:
override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
}
```

---

## 6️⃣ Error Handling Best Practices

### Loading States

```kotlin
private fun showLoading(view: ProgressBar?, outputView: TextView?, message: String = "Processing...") {
    view?.visibility = View.VISIBLE
    outputView?.text = message
}

private fun hideLoading(view: ProgressBar?) {
    view?.visibility = View.GONE
}
```

### Network Error Handling

```kotlin
private fun handleAIError(result: AdvancedAIService.AIResult, outputView: TextView?) {
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
            Toast.makeText(this, "Please configure your OpenAI API key", Toast.LENGTH_LONG).show()
        }
        else -> {
            outputView?.text = "❌ ${result.error}"
            Toast.makeText(this, result.error ?: "Unknown error", Toast.LENGTH_SHORT).show()
        }
    }
}
```

---

## 7️⃣ Testing Checklist

### Grammar Fix Panel
- [ ] "Fix Grammar" button processes text
- [ ] Loading indicator shows during processing
- [ ] Corrected text displays in output area
- [ ] "Replace Text" inserts corrected text
- [ ] Error messages show for failures
- [ ] Cache indicator shows for cached responses

### Word Tone Panel
- [ ] All tone buttons work (Funny, Poetic, Shorten, Sarcastic)
- [ ] Loading indicator shows
- [ ] Tone-adjusted text displays correctly
- [ ] Tone emoji/label shows in output
- [ ] "Replace Text" works

### AI Assistant Panel
- [ ] ChatGPT button improves text
- [ ] Humanize makes text natural
- [ ] Reply generates smart replies
- [ ] Idioms adds expressions
- [ ] All show loading states
- [ ] Replace text works

### Error Scenarios
- [ ] No internet - shows error
- [ ] No API key - shows error
- [ ] Rate limit - shows wait time
- [ ] Empty text - shows warning
- [ ] API failure - graceful error

---

## 8️⃣ Performance Tips

### 1. Debounce Rapid Clicks
```kotlin
private var lastClickTime = 0L
private val CLICK_DEBOUNCE_MS = 500L

fun onButtonClick(action: () -> Unit) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastClickTime > CLICK_DEBOUNCE_MS) {
        lastClickTime = currentTime
        action()
    }
}
```

### 2. Cancel Previous Requests
```kotlin
private var currentJob: Job? = null

fun processWithCancellation(action: suspend () -> Unit) {
    currentJob?.cancel()
    currentJob = serviceScope.launch {
        action()
    }
}
```

### 3. Optimize Text Extraction
```kotlin
// Don't get full text if not needed
val currentText = if (needFullText) {
    ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
} else {
    ic.getSelectedText(0)?.toString() ?: 
    ic.getTextBeforeCursor(500, 0)?.toString() ?: ""
}
```

---

## 🎉 Complete!

You now have fully integrated AI features in your keyboard:

✅ **Grammar Fix** - Corrects errors instantly  
✅ **Word Tone** - Adjusts tone (8 options)  
✅ **AI Assistant** - Smart replies & improvements  
✅ **Caching** - Fast repeated requests  
✅ **Rate Limiting** - Prevents API overuse  
✅ **Error Handling** - Graceful failures  

### Next: Configure API Key

Users need to add their OpenAI API key in settings:
1. Open keyboard settings
2. Navigate to AI Features
3. Enter OpenAI API key
4. Save and test

---

*Integration guide last updated: 2025-10-06*

