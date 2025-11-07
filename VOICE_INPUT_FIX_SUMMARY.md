# Voice Input Persistent Panel Fix

## Problem
1. Voice input text was not appearing in the target application's text field
2. Voice input panel was closing after each input (bad UX)
3. The `InputConnection` was being blocked by the `VoiceInputActivity`

## Root Cause
The `VoiceInputActivity` was:
1. Blocking the keyboard's `InputConnection` to the text field
2. Closing immediately after each voice input (required reopening for next input)
3. Not configured to allow the IME to maintain its connection while visible

## Solution

### 1. VoiceInputActivity.kt - Persistent Panel with Auto-Restart
**File**: `android/app/src/main/kotlin/com/example/ai_keyboard/VoiceInputActivity.kt`

**CRITICAL CHANGES**:

#### A. Window Configuration to Allow IME Connection
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Configure window to not interfere with keyboard's InputConnection
    window?.apply {
        // Allow the keyboard service to maintain its InputConnection
        addFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        // Don't dim the background
        clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    
    setContentView(R.layout.activity_voice_input)
    applyPanelHeight()
    // ... rest of setup
}
```

**Why this works:**
- `FLAG_ALT_FOCUSABLE_IM` allows the IME (keyboard) to keep its InputConnection active
- This is the KEY fix - the keyboard can now commit text while the voice panel is open

#### B. Stay Open After Voice Input (Like Emoji Panel)
```kotlin
private fun deliverResult(text: String) {
    recognitionInProgress = false
    statusFromError = false
    val service = AIKeyboardService.getInstance()
    if (service != null) {
        // Commit text directly without closing activity
        service.handleVoiceInputResult(text)
        
        // Reset UI and automatically start listening again for next input
        uiHandler.postDelayed({
            retryCount = 0
            statusFromError = false
            
            // Show brief success feedback
            statusText.text = "✓ Text added. Speak again or tap ← to close"
            
            // Auto-restart recognition after brief pause
            uiHandler.postDelayed({
                if (!isFinishing && !recognitionInProgress) {
                    startRecognition()
                }
            }, 800) // 800ms pause before auto-restart
        }, 100)
    } else {
        Toast.makeText(this, getString(R.string.voice_input_error), Toast.LENGTH_SHORT).show()
        finish()
        overridePendingTransition(0, 0)
    }
}
```

**Benefits:**
- Panel stays open for multiple voice inputs (like emoji panel)
- Automatically restarts recognition after 800ms
- User only closes it when done (tap back button)
- Much better UX - no need to reopen for each input

### 2. AIKeyboardService.kt - InputConnection Validation and Verification
**File**: `android/app/src/main/kotlin/com/example/ai_keyboard/AIKeyboardService.kt`

Enhanced `commitVoiceResultInternal()` with:
- InputConnection validity checking
- Text insertion verification
- Batch editing for atomic operations
- Comprehensive error handling and diagnostics

```kotlin
private fun commitVoiceResultInternal(text: String): Boolean {
    // Get a fresh InputConnection reference
    val ic = currentInputConnection
    if (ic == null) {
        Log.w(TAG, "❌ Cannot commit voice input: no input connection available")
        return false
    }
    
    try {
        // Verify the InputConnection is still valid by testing it
        val testText = ic.getTextBeforeCursor(1, 0)
        if (testText == null) {
            Log.w(TAG, "❌ InputConnection appears to be stale or invalid")
            return false
        }
        
        Log.d(TAG, "InputConnection validated, committing text...")
        
        // Use batch edit for more reliable text insertion
        ic.beginBatchEdit()
        try {
            val textToCommit = "$text "
            
            // Alternative approach: Use finishComposingText + commitText
            ic.finishComposingText()
            val committed = ic.commitText(textToCommit, 1)
            
            Log.d(TAG, "✅ Voice input committed: $text (commitText returned=$committed)")
            
            // Verify the text was actually inserted
            val afterText = ic.getTextBeforeCursor(textToCommit.length + 10, 0)?.toString() ?: ""
            val cursorPos = afterText.length
            Log.d(TAG, "After commit: cursor position=$cursorPos, recent text='${afterText.takeLast(50)}'")
            
            // Check if our text is in the editor
            if (afterText.contains(text)) {
                Log.d(TAG, "✅ Verified: Text successfully inserted into editor")
                return true
            } else {
                Log.w(TAG, "⚠️ Warning: Text may not have been inserted (verification failed)")
                return false
            }
        } finally {
            ic.endBatchEdit()
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error committing voice input: ${e.message}", e)
        return false
    } finally {
        // Update UI state after commit attempt
        try {
            mainHandler.post {
                ensureCursorStability()
                updateAISuggestions()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating UI after voice commit", e)
        }
    }
}
```

### 3. Enhanced Voice Input Handling
Added better logging and diagnostics in `handleVoiceInputResult()`:

```kotlin
fun handleVoiceInputResult(spokenText: String) {
    val text = spokenText.trim()
    if (text.isEmpty()) {
        Log.w(TAG, "❌ Voice input result is empty, skipping")
        return
    }
    
    Log.d(TAG, "📝 Received voice input result: '$text' (${text.length} chars)")
    
    mainHandler.post {
        val hasConnection = currentInputConnection != null
        Log.d(TAG, "Input connection available: $hasConnection")
        
        val committed = commitVoiceResultInternal(text)
        if (!committed) {
            pendingVoiceResult = text
            Log.w(TAG, "⏳ Voice result queued (no active input connection) - will retry")
            schedulePendingVoiceFlush()
        } else {
            pendingVoiceResult = null
            Log.d(TAG, "✅ Voice result committed successfully")
        }
    }
}
```

### 4. Additional Safety Net
Added pending voice result flush in `onStartInputView()` to catch any edge cases:

```kotlin
override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
    super.onStartInputView(info, restarting)

    // Flush any pending voice input when keyboard becomes visible
    flushPendingVoiceResult()
    
    // ... rest of the method
}
```

## Benefits
1. **Persistent Panel** - Voice input stays open like emoji panel (better UX)
2. **Correct InputConnection** - `FLAG_ALT_FOCUSABLE_IM` allows keyboard to maintain connection
3. **Auto-restart** - Automatically listens again after each input
4. **Multiple inputs** - Speak multiple times without reopening
5. **Verification system** - Text insertion is verified by reading back from editor
6. **Atomic operations** - Batch editing provides atomic text insertion
7. **Better error handling** - Catches and logs exceptions during commit
8. **Fallback mechanism** - Pending voice results are queued and retried if initial commit fails
9. **Improved diagnostics** - Enhanced logging helps identify future issues

## Key Insights
1. **`FLAG_ALT_FOCUSABLE_IM` is the critical fix** - Allows IME to maintain InputConnection while activity is open
2. **Persistent panel UX** - Works like emoji panel, stays open until explicitly closed
3. **Auto-restart recognition** - User can speak multiple times in succession

## Testing
To verify the fix:
1. Open any text input field (Notes, Messages, Browser, etc.)
2. Tap the voice input button on the keyboard
3. **Speak first phrase**: "Hello world"
4. **Verify**: Text appears immediately with trailing space
5. **Wait 800ms**: Panel shows "✓ Text added. Speak again or tap ← to close"
6. **Automatically starts listening again**
7. **Speak second phrase**: "This is amazing"
8. **Verify**: Second text is added without having to reopen panel
9. **Tap back button** to close the voice panel when done
10. Check logcat for success messages

## Expected Behavior
- Voice input panel stays open after each input (persistent mode)
- Text appears immediately in the target application's text field
- After 800ms, recognition automatically restarts
- You can speak multiple times without reopening the panel
- Panel shows "✓ Text added. Speak again or tap ← to close"
- No "Dropping event" warnings should appear in logcat
- Cursor should be positioned after the inserted text with a trailing space
- Only closes when you tap the back button

## Expected Log Output (Success)
When voice input works correctly, you should see:
```
D/AIKeyboardService: 📝 Received voice input result: '[your text]' (XX chars)
D/AIKeyboardService: Input connection available: true
D/AIKeyboardService: InputConnection validated, committing text...
D/AIKeyboardService: ✅ Voice input committed: [your text] (commitText returned=true)
D/AIKeyboardService: After commit: cursor position=XX, recent text='[your text] '
D/AIKeyboardService: ✅ Verified: Text successfully inserted into editor
D/AIKeyboardService: ✅ Voice result committed successfully
```

## Troubleshooting
If text still doesn't appear, check logs for:
- `❌ InputConnection appears to be stale or invalid` → InputConnection issue
- `⚠️ Warning: Text may not have been inserted (verification failed)` → Commit failed
- `⏳ Voice result queued (no active input connection)` → Will retry automatically

If you see these, the 200ms delay might need to be increased to 300-400ms depending on device performance.

## Technical Notes
- **`FLAG_ALT_FOCUSABLE_IM`** is essential - allows IME to keep InputConnection active while activity is visible
- The 800ms delay before auto-restart gives users time to see the feedback message
- Batch editing (`beginBatchEdit`/`endBatchEdit`) is the recommended Android approach for IME text insertion
- Text insertion verification ensures the commit was actually successful
- The pending voice result mechanism provides resilience against timing issues
- Activity behaves like emoji panel - persistent until explicitly closed
- Multiple flush points ensure voice input is committed even if the keyboard state changes

---

**Date**: November 7, 2025  
**Status**: ✅ Fixed and Tested

