# 🧠 Unified Suggestion Architecture - Integration Guide

## Overview

This guide explains how to integrate the new **UnifiedSuggestionController** into your AI keyboard system, replacing the fragmented suggestion logic scattered across `AIKeyboardService`.

## ✅ Components Created

1. **`UnifiedSuggestionController.kt`** - Central suggestion coordinator
2. **`SuggestionBarRenderer.kt`** - Unified UI rendering
3. **`SuggestionMethodChannelBridge.kt`** - Flutter↔Kotlin settings sync

## 📋 Integration Steps

### Step 1: Initialize UnifiedSuggestionController in AIKeyboardService

Add to AIKeyboardService class properties (~line 472):

```kotlin
// ✅ NEW: Unified Suggestion System
private lateinit var unifiedSuggestionController: UnifiedSuggestionController
private lateinit var suggestionBarRenderer: SuggestionBarRenderer
```

### Step 2: Initialize in `initializeCoreComponents()` (after line 1100)

```kotlin
// Initialize UnifiedSuggestionController
unifiedSuggestionController = UnifiedSuggestionController(
    context = this,
    suggestionsPipeline = suggestionsPipeline,
    unifiedAutocorrectEngine = autocorrectEngine,
    clipboardHistoryManager = clipboardHistoryManager
)

// Initialize SuggestionBarRenderer
suggestionBarRenderer = SuggestionBarRenderer(this)
suggestionBarRenderer.setOnSuggestionClickListener { suggestion ->
    handleSuggestionClick(suggestion)
}

Log.d(TAG, "✅ UnifiedSuggestionController initialized")
```

### Step 3: Replace Suggestion Update Logic

Find all calls to `updateSuggestions()` or similar and replace with:

```kotlin
/**
 * 🎯 NEW: Unified suggestion update using UnifiedSuggestionController
 */
private fun updateUnifiedSuggestions() {
    coroutineScope.launch {
        try {
            val prefix = getCurrentWord()
            val context = getPreviousWords()
            
            val suggestions = unifiedSuggestionController.getUnifiedSuggestions(
                prefix = prefix,
                context = context,
                includeEmoji = true,
                includeClipboard = prefix.isEmpty()
            )
            
            // Render suggestions in UI
            suggestionContainer?.let { container ->
                suggestionBarRenderer.renderSuggestions(container, suggestions)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating unified suggestions", e)
        }
    }
}
```

### Step 4: Handle Suggestion Clicks

Add this method to handle all suggestion types:

```kotlin
/**
 * Handle unified suggestion click
 */
private fun handleSuggestionClick(suggestion: UnifiedSuggestionController.UnifiedSuggestion) {
    when (suggestion.type) {
        UnifiedSuggestionController.SuggestionType.TYPING,
        UnifiedSuggestionController.SuggestionType.NEXT_WORD,
        UnifiedSuggestionController.SuggestionType.AUTOCORRECT -> {
            commitText(suggestion.text + " ")
        }
        
        UnifiedSuggestionController.SuggestionType.EMOJI -> {
            commitText(suggestion.text)
        }
        
        UnifiedSuggestionController.SuggestionType.CLIPBOARD -> {
            val fullText = suggestion.metadata["fullText"] as? String ?: suggestion.text
            commitText(fullText)
        }
        
        UnifiedSuggestionController.SuggestionType.AI_REWRITE -> {
            // Handle AI rewrite (future enhancement)
            commitText(suggestion.text)
        }
        
        else -> {
            commitText(suggestion.text)
        }
    }
}
```

### Step 5: Update Key Press Handlers

In `onKey()` method, replace existing suggestion logic with:

```kotlin
override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
    // ... existing code ...
    
    // Update suggestions after key press
    updateUnifiedSuggestions()
}
```

### Step 6: Update Swipe Handler

In swipe completion handler, use unified swipe suggestions:

```kotlin
override fun onSwipeComplete(path: List<Pair<Float, Float>>) {
    coroutineScope.launch {
        try {
            val swipePath = SwipePath(path)
            val context = getPreviousWords()
            
            val suggestions = unifiedSuggestionController.getSwipeSuggestions(swipePath, context)
            
            suggestionContainer?.let { container ->
                suggestionBarRenderer.renderSuggestions(container, suggestions)
            }
            
            // Auto-commit first suggestion if high confidence
            if (suggestions.isNotEmpty() && suggestions.first().score > 2.0) {
                commitText(suggestions.first().text + " ")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling swipe", e)
        }
    }
}
```

### Step 7: Cleanup

Add to `onDestroy()`:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    
    // Cleanup unified controller
    if (::unifiedSuggestionController.isInitialized) {
        unifiedSuggestionController.cleanup()
    }
    
    // ... existing cleanup ...
}
```

## 🔄 Method Channel Integration (Flutter Settings)

The `SuggestionMethodChannelBridge` automatically syncs settings from Flutter.

### Option 1: Initialize from MainActivity (Recommended)

Add to `MainActivity.configureFlutterEngine()`:

```kotlin
override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    
    // ... existing channel setup ...
    
    // Initialize Suggestion Settings Bridge
    try {
        // Get or create UnifiedSuggestionController from AIKeyboardService
        val keyboardService = AIKeyboardService.getInstance()
        if (keyboardService != null) {
            val suggestionBridge = SuggestionMethodChannelBridge(
                context = this,
                suggestionController = keyboardService.getUnifiedSuggestionController()
            )
            suggestionBridge.initialize(flutterEngine)
            Log.d(TAG, "✅ Suggestion settings bridge initialized")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error initializing suggestion bridge", e)
    }
}
```

### Option 2: Lazy Initialization from AIKeyboardService

If the bridge needs to be initialized after FlutterEngine is available:

```kotlin
// In AIKeyboardService
private var suggestionBridge: SuggestionMethodChannelBridge? = null

// Call this when Flutter engine becomes available
fun initializeSuggestionBridge(flutterEngine: FlutterEngine) {
    if (suggestionBridge == null) {
        suggestionBridge = SuggestionMethodChannelBridge(
            context = this,
            suggestionController = unifiedSuggestionController
        )
        suggestionBridge?.initialize(flutterEngine)
    }
}
```

## 🎨 Theme Integration

Apply theme colors to suggestion bar:

```kotlin
private fun applyThemeToSuggestions() {
    val theme = themeManager.getCurrentTheme()
    val palette = themeManager.getCurrentPalette()
    
    val colors = SuggestionBarRenderer.ThemeColors(
        backgroundDefault = palette.suggestionBackground ?: 0xFF3C3C3C.toInt(),
        backgroundAutocorrect = palette.accentColor ?: 0xFF4A90E2.toInt(),
        backgroundEmoji = palette.emojiBackground ?: 0xFF5C5C5C.toInt(),
        backgroundClipboard = palette.clipboardBackground ?: 0xFF6B4A9E.toInt(),
        textDefault = palette.suggestionTextColor ?: 0xFFFFFFFF.toInt(),
        textAutocorrect = palette.textColor ?: 0xFFFFFFFF.toInt()
    )
    
    suggestionBarRenderer.setThemeColors(colors)
}
```

## 🚀 Benefits

✅ **Centralized Logic** - All suggestion types in one place  
✅ **Unified Ranking** - Consistent scoring across all sources  
✅ **Settings Integration** - Flutter settings sync automatically  
✅ **Type-Aware UI** - Different styling for each suggestion type  
✅ **Performance** - LRU caching reduces redundant computations  
✅ **Maintainability** - Clear separation of concerns  

## 🧪 Testing

1. **Basic Typing**: Type letters and verify suggestions appear
2. **Next-Word**: Type a word + space, verify next-word predictions
3. **Emoji**: Type words like "love", "happy" - verify emoji suggestions
4. **Clipboard**: Copy text, wait < 60s, verify clipboard suggestion when empty input
5. **Swipe**: Perform swipe gesture, verify swipe suggestions
6. **Settings**: Toggle settings in Flutter, verify changes reflect in suggestions
7. **Theme**: Change theme, verify suggestion bar colors update

## 📊 Monitoring

Check logs for:
- `UnifiedSuggestionCtrl` - Core suggestion logic
- `SuggestionBarRenderer` - UI rendering
- `SuggestionMethodChannelBridge` - Settings sync

## 🐛 Troubleshooting

**No suggestions appearing:**
- Check `unifiedSuggestionController.getStats()` to verify initialization
- Verify `autocorrectEngine.isReady()` returns true
- Check Firebase data is loaded

**Settings not syncing:**
- Verify MethodChannel name matches Flutter side: `"ai_keyboard/suggestions"`
- Check SharedPreferences for `"unified_suggestions"` namespace

**UI not updating:**
- Verify `suggestionContainer` is not null
- Check theme colors are applied
- Ensure main thread context for UI updates

## 🔮 Future Enhancements

1. **AI Rewrites in Suggestion Bar** - Show tone/grammar suggestions inline
2. **Contextual Emoji** - Sentiment-based emoji recommendations
3. **Personalized Ranking** - ML-based suggestion ordering
4. **Multi-line Suggestions** - Support for phrase predictions
5. **Voice Input Integration** - Unified suggestions for voice typing

