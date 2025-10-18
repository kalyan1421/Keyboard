# Clipboard Integration Changes for AIKeyboardService.kt

## Summary
This document outlines the specific changes needed to integrate UnifiedClipboardManager into AIKeyboardService.kt and remove duplicate code.

## Changes Required

### 1. Replace Clipboard Component Declarations (Lines 520-524)

**OLD CODE:**
```kotlin
// Clipboard history management
private lateinit var clipboardHistoryManager: ClipboardHistoryManager
private var clipboardPanel: ClipboardPanel? = null
private var clipboardSuggestionEnabled = true
private var clipboardStripView: ClipboardStripView? = null
```

**NEW CODE:**
```kotlin
// Unified clipboard management
private lateinit var unifiedClipboardManager: UnifiedClipboardManager
```

### 2. Remove clipboardHistoryListener (Lines 552-569)

**DELETE THIS ENTIRE BLOCK** - UnifiedClipboardManager handles this internally

### 3. Update broadcast receiver for CLIPBOARD_CHANGED (Lines 676-687)

**OLD CODE:**
```kotlin
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
```

**NEW CODE:**
```kotlin
"com.example.ai_keyboard.CLIPBOARD_CHANGED" -> {
    Log.d(TAG, "CLIPBOARD_CHANGED broadcast received!")
    mainHandler.post {
        try {
            Log.d(TAG, "Reloading clipboard settings from broadcast...")
            if (::unifiedClipboardManager.isInitialized) {
                unifiedClipboardManager.reloadSettings()
            }
            Log.d(TAG, "Clipboard settings reloaded successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading clipboard settings from broadcast", e)
        }
    }
}
```

### 4. Update initializeCoreComponents() (Around line 1086)

**OLD CODE:**
```kotlin
// Initialize clipboard history manager (needed for SuggestionsPipeline)
clipboardHistoryManager = ClipboardHistoryManager(this)
Log.d(TAG, "✅ ClipboardHistoryManager initialized")
```

**NEW CODE:**
```kotlin
// Initialize unified clipboard manager (consolidates history, panel, strip)
unifiedClipboardManager = UnifiedClipboardManager(this, themeManager)
Log.d(TAG, "✅ UnifiedClipboardManager created")
```

### 5. Update onCreate() clipboard initialization (Lines 895-900)

**OLD CODE:**
```kotlin
// ClipboardHistoryManager already initialized in initializeCoreComponents()
// Just complete setup with listener
if (::clipboardHistoryManager.isInitialized) {
    clipboardHistoryManager.initialize()
    clipboardHistoryManager.addListener(clipboardHistoryListener)
}
```

**NEW CODE:**
```kotlin
// Initialize unified clipboard manager
if (::unifiedClipboardManager.isInitialized) {
    unifiedClipboardManager.initialize()
    
    // Set up callbacks
    unifiedClipboardManager.setOnHistoryUpdatedCallback { items ->
        mainHandler.post {
            if (unifiedClipboardManager.isSuggestionEnabled()) {
                updateSuggestionsWithClipboard()
            }
        }
    }
    
    unifiedClipboardManager.setOnNewItemCallback { item ->
        mainHandler.post {
            if (unifiedClipboardManager.isSuggestionEnabled()) {
                updateSuggestionsWithClipboard()
            }
        }
    }
    
    unifiedClipboardManager.setOnItemSelectedCallback { item ->
        // Paste clipboard item to input
        pasteClipboardItem(item)
    }
}
```

### 6. Update SuggestionsPipeline initialization (Around line 1190)

**OLD CODE:**
```kotlin
suggestionsPipeline = SuggestionsPipeline(
    context = this,
    clipboardManager = clipboardHistoryManager,
    unifiedAutocorrectEngine = autocorrectEngine,
    multilingualDictionary = multilingualDictionary,
    languageManager = languageManager
)
```

**NEW CODE:**
```kotlin
// Get historyManager from unified clipboard manager
val clipboardHistoryManager = unifiedClipboardManager.historyManager

suggestionsPipeline = SuggestionsPipeline(
    context = this,
    clipboardManager = clipboardHistoryManager,
    unifiedAutocorrectEngine = autocorrectEngine,
    multilingualDictionary = multilingualDictionary,
    languageManager = languageManager
)
```

**NOTE:** Need to expose historyManager in UnifiedClipboardManager as a public property.

### 7. Update onDestroy() cleanup (Around line 6000)

**OLD CODE:**
```kotlin
// Cleanup clipboard history manager
try {
    clipboardHistoryManager.cleanup()
} catch (e: Exception) {
    Log.e(TAG, "Error cleaning up clipboard history manager", e)
}
```

**NEW CODE:**
```kotlin
// Cleanup unified clipboard manager
try {
    if (::unifiedClipboardManager.isInitialized) {
        unifiedClipboardManager.cleanup()
    }
} catch (e: Exception) {
    Log.e(TAG, "Error cleaning up unified clipboard manager", e)
}
```

### 8. Replace showFeaturePanel(PanelType.CLIPBOARD) (Around line 7386-7391)

**OLD CODE:**
```kotlin
PanelType.CLIPBOARD -> {
    title?.text = "Clipboard"
    val toggle = layoutInflater.inflate(R.layout.panel_right_toggle, rightContainer, false)
    rightContainer?.addView(toggle)
    inflateClipboardBody(body)
}
```

**NEW CODE:**
```kotlin
PanelType.CLIPBOARD -> {
    // Close the panel immediately and show popup instead
    dismissFeaturePanel()
    
    // Show clipboard panel from unified manager
    if (::unifiedClipboardManager.isInitialized && ::keyboardView.isInitialized) {
        unifiedClipboardManager.showPanel(keyboardView)
    }
    return // Exit early since we're showing popup instead
}
```

### 9. DELETE inflateClipboardBody() method (Lines 8452-8700+)

**DELETE THE ENTIRE METHOD** - This is duplicate functionality now in ClipboardPanel

### 10. Update updateSuggestionsWithClipboard() (Around line 9524)

**OLD CODE:**
```kotlin
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
```

**NEW CODE:**
```kotlin
private fun updateSuggestionsWithClipboard() {
    try {
        if (!::unifiedClipboardManager.isInitialized) return
        
        // Get OTP items first (highest priority)
        val otpItems = unifiedClipboardManager.getOTPItems()
        val recentItem = unifiedClipboardManager.getMostRecentItem()
        
        if (otpItems.isNotEmpty() || recentItem != null) {
            mainHandler.post {
                updateSuggestionUIWithClipboard(otpItems, recentItem)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error updating suggestions with clipboard", e)
    }
}
```

### 11. Update reloadClipboardSettings() (Around line 9587)

**REPLACE ENTIRE METHOD:**
```kotlin
private fun reloadClipboardSettings() {
    try {
        if (::unifiedClipboardManager.isInitialized) {
            unifiedClipboardManager.reloadSettings()
            Log.d(TAG, "✅ Clipboard settings reloaded via UnifiedClipboardManager")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error reloading clipboard settings", e)
    }
}
```

### 12. Update updateClipboardStrip() (Around line 9644)

**OLD CODE:**
```kotlin
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
```

**NEW CODE:**
```kotlin
private fun updateClipboardStrip() {
    try {
        if (!::unifiedClipboardManager.isInitialized) return
        
        mainHandler.post {
            // Strip view is managed internally by UnifiedClipboardManager
            Log.d(TAG, "Clipboard strip updated via UnifiedClipboardManager")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error updating clipboard strip", e)
    }
}
```

### 13. Add new helper method for pasting clipboard items

**ADD NEW METHOD:**
```kotlin
/**
 * Paste a clipboard item to the current input field
 */
private fun pasteClipboardItem(item: ClipboardItem) {
    try {
        val ic = currentInputConnection ?: return
        ic.commitText(item.text, 1)
        Log.d(TAG, "📋 Pasted clipboard item: ${item.getPreview()}")
    } catch (e: Exception) {
        Log.e(TAG, "Error pasting clipboard item", e)
    }
}
```

### 14. Initialize MethodChannel for Clipboard (In onCreate or initializeMethodChannels)

**ADD THIS CODE** (after theme channel initialization):
```kotlin
// Initialize Clipboard MethodChannel
try {
    val clipboardChannel = MethodChannel(getBinaryMessenger(), UnifiedClipboardManager.CHANNEL_NAME)
    if (::unifiedClipboardManager.isInitialized) {
        unifiedClipboardManager.setupMethodChannel(clipboardChannel)
        Log.d(TAG, "✅ Clipboard MethodChannel initialized")
    }
} catch (e: Exception) {
    Log.e(TAG, "❌ Error initializing clipboard MethodChannel", e)
}
```

**NOTE:** Need to add `getBinaryMessenger()` method or use FlutterEngine to get messenger.

## Additional Changes

### Update UnifiedClipboardManager.kt

Add public accessor for historyManager:

```kotlin
// Add this property to UnifiedClipboardManager
val historyManager: ClipboardHistoryManager
    get() = this.historyManager
```

Wait, that would be circular. Instead, keep it as:

```kotlin
// In UnifiedClipboardManager, expose getter
fun getHistoryManager(): ClipboardHistoryManager = historyManager
```

Or better yet, expose the needed methods directly:

```kotlin
// Already done - we have getAllItems(), getHistoryForUI(), etc.
```

## Testing Checklist

After implementing these changes:

- [ ] Keyboard starts without crashes
- [ ] Clipboard items are captured when copied
- [ ] Clipboard panel opens from keyboard
- [ ] Clipboard items can be selected and pasted
- [ ] Pin/unpin works in panel
- [ ] Delete works in panel
- [ ] Settings from Flutter app sync to keyboard
- [ ] Clipboard suggestions appear (if enabled)
- [ ] OTP items are highlighted
- [ ] Clear all works for non-pinned items
- [ ] Flutter app shows clipboard history
- [ ] Real-time sync between app and keyboard

## Migration Benefits

1. **Single Source of Truth**: All clipboard logic in UnifiedClipboardManager
2. **MethodChannel Communication**: Direct Flutter ↔ Kotlin communication
3. **Reduced Code**: Removed ~250 lines of duplicate clipboard UI code
4. **Better Architecture**: Clear separation of concerns
5. **Easier Maintenance**: Changes in one place, not scattered
6. **Feature Parity**: All Flutter features now in keyboard

