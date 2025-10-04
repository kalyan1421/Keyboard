# CleverType Keyboard Cycling Implementation Guide
## System-Wide Keyboard Mode Switching & Emoji Theming

**Date:** October 2025  
**Project:** AI Keyboard - CleverType Layout Cycling  
**Status:** Implementation Ready

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Target CleverType Behavior](#target-clevertype-behavior)
3. [Implementation Plan](#implementation-plan)
4. [Code Changes](#code-changes)
5. [Testing Checklist](#testing-checklist)

---

## Current State Analysis

### Existing Keyboard Constants

```kotlin
// In AIKeyboardService.kt (lines 53-55)
private const val KEYBOARD_LETTERS = 1
private const val KEYBOARD_SYMBOLS = 2
private const val KEYBOARD_NUMBERS = 3

// Key codes (lines 64-71)
private const val KEYCODE_SYMBOLS = -10
private const val KEYCODE_LETTERS = -11
private const val KEYCODE_NUMBERS = -12
private const val KEYCODE_EMOJI = -15
```

### Current Switching Methods

```kotlin
// switchToSymbols() - line 2280
private fun switchToSymbols() {
    if (currentKeyboard != KEYBOARD_SYMBOLS) {
        keyboard = Keyboard(this, R.xml.symbols)
        currentKeyboard = KEYBOARD_SYMBOLS
        keyboardView?.keyboard = keyboard
        applyTheme()
    }
}

// switchToLetters() - line 2303
private fun switchToLetters() {
    val lang = availableLanguages[currentLanguageIndex]
    val keyboardResource = getKeyboardResourceForLanguage(lang, showNumberRow)
    keyboard = Keyboard(this, keyboardResource)
    currentKeyboard = KEYBOARD_LETTERS
    keyboardView?.keyboard = keyboard
    applyTheme()
}

// switchToNumbers() - line 2354
private fun switchToNumbers() {
    if (currentKeyboard != KEYBOARD_NUMBERS) {
        keyboard = Keyboard(this, R.xml.numbers)
        currentKeyboard = KEYBOARD_NUMBERS
        keyboardView?.keyboard = keyboard
        applyTheme()
    }
}
```

### Current onKey Handling

```kotlin
// Lines 1577-1593
KEYCODE_SYMBOLS -> switchToSymbols()
KEYCODE_LETTERS -> switchToLetters()
KEYCODE_NUMBERS -> switchToNumbers()
KEYCODE_EMOJI -> {
    handleEmojiToggle()
    ensureCursorStability()
}
```

### Current XML Layouts

**qwerty.xml (line 84):**
```xml
<Key android:codes="-3" android:keyLabel="\?123" android:keyWidth="15%p" android:keyEdgeFlags="left"/>
```

**symbols.xml (line 67):**
```xml
<Key android:codes="-2" android:keyLabel="ABC" android:keyWidth="15%p" android:keyEdgeFlags="left"/>
```

### Emoji Panel Theming

✅ **Already Implemented!** (lines 1279-1336)

```kotlin
// applyThemeImmediately() - line 1279
gboardEmojiPanel?.let { panel ->
    applyThemeToEmojiPanel(panel, palette)
    Log.d(TAG, "✅ Emoji panel themed")
}

// applyThemeToEmojiPanel() - line 1309
private fun applyThemeToEmojiPanel(panel: View, palette: ThemePaletteV2) {
    if (panel is ViewGroup) {
        panel.setBackgroundColor(palette.keyboardBg)
        
        for (i in 0 until panel.childCount) {
            val child = panel.getChildAt(i)
            when (child) {
                is LinearLayout -> {
                    if (child.tag == "emoji_categories" || child.tag == "emoji_header") {
                        child.setBackgroundColor(palette.toolbarBg)
                    }
                    applyThemeToEmojiPanel(child, palette)
                }
                is TextView -> {
                    if (child.tag?.toString()?.startsWith("category_") == true) {
                        child.setTextColor(palette.keyText)
                    }
                }
            }
        }
    }
}
```

---

## Target CleverType Behavior

### Keyboard Mode Cycling

```
User presses ?123:
    Letters → Numbers
    
User presses ?123 again:
    Numbers → Symbols
    
User presses ABC:
    Symbols → Letters
    Numbers → Letters
    
User presses Emoji:
    Any mode → Emoji Panel
    
User presses ABC from Emoji:
    Emoji Panel → Letters (returns to previous mode)
```

### Visual Flow

```
┌─────────────┐
│   LETTERS   │
│  (QWERTY)   │
│             │
│  ?123 key   │ ──┐
└─────────────┘   │
                  ↓
┌─────────────┐
│   NUMBERS   │
│  (1-9-0)    │
│             │
│  ?123 key   │ ──┐
└─────────────┘   │
                  ↓
┌─────────────┐
│   SYMBOLS   │
│  (!@#$%^&*) │
│             │
│   ABC key   │ ──┐
└─────────────┘   │
                  │
                  └─→ Back to LETTERS

┌─────────────┐
│  EMOJI 😊   │
│ (Any mode)  │
│             │
│  ABC key    │ ──→ Back to previous keyboard
└─────────────┘
```

---

## Implementation Plan

### Step 1: Add KeyboardMode Enum

Add after companion object (around line 86):

```kotlin
/**
 * Keyboard mode enum for CleverType-style cycling
 */
enum class KeyboardMode {
    LETTERS,
    NUMBERS,
    SYMBOLS,
    EMOJI
}
```

### Step 2: Add Mode State Variable

Add to state variables (around line 103):

```kotlin
private var currentKeyboardMode = KeyboardMode.LETTERS
private var previousKeyboardMode = KeyboardMode.LETTERS  // For emoji return
```

### Step 3: Implement Cycling Logic

Replace the three separate switch methods with unified cycling:

```kotlin
/**
 * Switch keyboard mode with CleverType-style cycling
 * Letters → Numbers → Symbols → Letters
 */
private fun switchKeyboardMode(targetMode: KeyboardMode) {
    Log.d(TAG, "Switching from $currentKeyboardMode to $targetMode")
    
    // Save previous mode for emoji panel return
    if (currentKeyboardMode != KeyboardMode.EMOJI) {
        previousKeyboardMode = currentKeyboardMode
    }
    
    when (targetMode) {
        KeyboardMode.LETTERS -> {
            val lang = availableLanguages[currentLanguageIndex]
            val keyboardResource = getKeyboardResourceForLanguage(lang, showNumberRow)
            keyboard = Keyboard(this, keyboardResource)
            currentKeyboard = KEYBOARD_LETTERS
            keyboardView?.keyboard = keyboard
            keyboardView?.showNormalLayout()
            applyTheme()
        }
        
        KeyboardMode.NUMBERS -> {
            keyboard = Keyboard(this, R.xml.symbols)  // Use symbols.xml for numbers row
            currentKeyboard = KEYBOARD_NUMBERS
            keyboardView?.keyboard = keyboard
            keyboardView?.showNormalLayout()
            applyTheme()
        }
        
        KeyboardMode.SYMBOLS -> {
            keyboard = Keyboard(this, R.xml.symbols)
            currentKeyboard = KEYBOARD_SYMBOLS
            keyboardView?.keyboard = keyboard
            keyboardView?.showNormalLayout()
            applyTheme()
        }
        
        KeyboardMode.EMOJI -> {
            toggleEmojiPanel()
            return  // Don't update currentKeyboardMode yet
        }
    }
    
    currentKeyboardMode = targetMode
    Log.d(TAG, "Switched to $currentKeyboardMode")
}

/**
 * Cycle to next keyboard mode (CleverType behavior)
 */
private fun cycleKeyboardMode() {
    val nextMode = when (currentKeyboardMode) {
        KeyboardMode.LETTERS -> KeyboardMode.NUMBERS
        KeyboardMode.NUMBERS -> KeyboardMode.SYMBOLS
        KeyboardMode.SYMBOLS -> KeyboardMode.LETTERS
        KeyboardMode.EMOJI -> previousKeyboardMode
    }
    switchKeyboardMode(nextMode)
}

/**
 * Return to letters mode (ABC button)
 */
private fun returnToLetters() {
    switchKeyboardMode(KeyboardMode.LETTERS)
}
```

### Step 4: Update onKey Handler

Replace existing switch calls (around lines 1577-1593):

```kotlin
KEYCODE_SYMBOLS -> cycleKeyboardMode()  // ?123 key cycles forward
KEYCODE_LETTERS -> returnToLetters()     // ABC key returns to letters
KEYCODE_NUMBERS -> cycleKeyboardMode()   // Also cycle
KEYCODE_EMOJI -> switchKeyboardMode(KeyboardMode.EMOJI)
```

### Step 5: Update toggleEmojiPanel()

Modify the emoji toggle to respect mode state (around line 4327):

```kotlin
private fun toggleEmojiPanel() {
    try {
        isEmojiPanelVisible = !isEmojiPanelVisible
        keyboardContainer?.let { container ->
            container.removeAllViews()
            
            if (isEmojiPanelVisible) {
                // Save current mode before showing emoji
                previousKeyboardMode = currentKeyboardMode
                currentKeyboardMode = KeyboardMode.EMOJI
                
                gboardEmojiPanel?.let { panel ->
                    container.addView(panel)
                    // Apply theme to emoji panel
                    applyThemeToEmojiPanel(panel, themeManager.getCurrentPalette())
                }
                
                topContainer?.visibility = View.VISIBLE
                Log.d(TAG, "Emoji panel shown")
            } else {
                // Return to previous mode
                keyboardView?.let { container.addView(it) }
                currentKeyboardMode = previousKeyboardMode
                switchKeyboardMode(currentKeyboardMode)
                Log.d(TAG, "Emoji panel hidden, returned to $currentKeyboardMode")
            }
        }
        
        keyboardView?.setEmojiKeyActive(isEmojiPanelVisible)
    } catch (e: Exception) {
        Log.e(TAG, "Error toggling emoji panel", e)
    }
}
```

### Step 6: Update XML Layouts (Optional - Current Codes Work)

The current XML uses `-3` and `-2`, but our code uses `-10`, `-11`, `-12`. We need to handle both OR update XML:

**Option A: Update onKey to map XML codes**

```kotlin
-3 -> cycleKeyboardMode()  // ?123 in qwerty.xml
-2 -> returnToLetters()    // ABC in symbols.xml
```

**Option B: Update XML files** (recommended for consistency)

Update `qwerty.xml` line 84:
```xml
<Key android:codes="-10" android:keyLabel="\?123" android:keyWidth="15%p" android:keyEdgeFlags="left"/>
```

Update `symbols.xml` line 67:
```xml
<Key android:codes="-11" android:keyLabel="ABC" android:keyWidth="15%p" android:keyEdgeFlags="left"/>
```

### Step 7: Enhance Emoji Panel Theme Integration

The emoji panel theming is ALREADY implemented! Just ensure it's called consistently. The existing code at line 1279-1336 already:
- ✅ Applies keyboard background
- ✅ Applies toolbar background to emoji categories
- ✅ Applies text colors to category labels

No changes needed here!

---

## Code Changes

### File: `AIKeyboardService.kt`

#### Change 1: Add enum (after line 86)

```kotlin
/**
 * Keyboard mode enum for CleverType-style cycling
 */
enum class KeyboardMode {
    LETTERS,
    NUMBERS,
    SYMBOLS,
    EMOJI
}
```

#### Change 2: Add mode state (after line 103)

```kotlin
private var currentKeyboardMode = KeyboardMode.LETTERS
private var previousKeyboardMode = KeyboardMode.LETTERS
```

#### Change 3: Replace switch methods (lines 2280-2361)

```kotlin
/**
 * Switch keyboard mode with CleverType-style cycling
 */
private fun switchKeyboardMode(targetMode: KeyboardMode) {
    Log.d(TAG, "Switching from $currentKeyboardMode to $targetMode")
    
    if (currentKeyboardMode != KeyboardMode.EMOJI) {
        previousKeyboardMode = currentKeyboardMode
    }
    
    when (targetMode) {
        KeyboardMode.LETTERS -> {
            val lang = availableLanguages[currentLanguageIndex]
            val keyboardResource = getKeyboardResourceForLanguage(lang, showNumberRow)
            keyboard = Keyboard(this, keyboardResource)
            currentKeyboard = KEYBOARD_LETTERS
            keyboardView?.keyboard = keyboard
            keyboardView?.showNormalLayout()
            applyTheme()
        }
        
        KeyboardMode.NUMBERS -> {
            keyboard = Keyboard(this, R.xml.symbols)
            currentKeyboard = KEYBOARD_NUMBERS
            keyboardView?.keyboard = keyboard
            keyboardView?.showNormalLayout()
            applyTheme()
        }
        
        KeyboardMode.SYMBOLS -> {
            keyboard = Keyboard(this, R.xml.symbols)
            currentKeyboard = KEYBOARD_SYMBOLS
            keyboardView?.keyboard = keyboard
            keyboardView?.showNormalLayout()
            applyTheme()
        }
        
        KeyboardMode.EMOJI -> {
            toggleEmojiPanel()
            return
        }
    }
    
    currentKeyboardMode = targetMode
    Log.d(TAG, "Switched to $currentKeyboardMode")
}

/**
 * Cycle to next keyboard mode (CleverType behavior)
 */
private fun cycleKeyboardMode() {
    val nextMode = when (currentKeyboardMode) {
        KeyboardMode.LETTERS -> KeyboardMode.NUMBERS
        KeyboardMode.NUMBERS -> KeyboardMode.SYMBOLS
        KeyboardMode.SYMBOLS -> KeyboardMode.LETTERS
        KeyboardMode.EMOJI -> previousKeyboardMode
    }
    switchKeyboardMode(nextMode)
}

/**
 * Return to letters mode (ABC button)
 */
private fun returnToLetters() {
    switchKeyboardMode(KeyboardMode.LETTERS)
}

// DEPRECATED - kept for compatibility
@Deprecated("Use switchKeyboardMode() instead", ReplaceWith("switchKeyboardMode(KeyboardMode.SYMBOLS)"))
private fun switchToSymbols() {
    switchKeyboardMode(KeyboardMode.SYMBOLS)
}

@Deprecated("Use switchKeyboardMode() instead", ReplaceWith("switchKeyboardMode(KeyboardMode.LETTERS)"))
private fun switchToLetters() {
    switchKeyboardMode(KeyboardMode.LETTERS)
}

@Deprecated("Use switchKeyboardMode() instead", ReplaceWith("switchKeyboardMode(KeyboardMode.NUMBERS)"))
private fun switchToNumbers() {
    switchKeyboardMode(KeyboardMode.NUMBERS)
}
```

#### Change 4: Update onKey handler (lines 1577-1593)

```kotlin
KEYCODE_SYMBOLS -> cycleKeyboardMode()  // ?123 cycles forward
KEYCODE_LETTERS -> returnToLetters()    // ABC returns to letters
KEYCODE_NUMBERS -> cycleKeyboardMode()  // Also cycle
-3 -> cycleKeyboardMode()  // Handle XML ?123 code
-2 -> returnToLetters()    // Handle XML ABC code
KEYCODE_EMOJI -> switchKeyboardMode(KeyboardMode.EMOJI)
```

#### Change 5: Update toggleEmojiPanel() (line 4327)

```kotlin
private fun toggleEmojiPanel() {
    try {
        isEmojiPanelVisible = !isEmojiPanelVisible
        keyboardContainer?.let { container ->
            container.removeAllViews()
            
            if (isEmojiPanelVisible) {
                previousKeyboardMode = currentKeyboardMode
                currentKeyboardMode = KeyboardMode.EMOJI
                
                gboardEmojiPanel?.let { panel ->
                    container.addView(panel)
                    applyThemeToEmojiPanel(panel, themeManager.getCurrentPalette())
                }
                
                topContainer?.visibility = View.VISIBLE
                Log.d(TAG, "Emoji panel shown, saved previous mode: $previousKeyboardMode")
            } else {
                keyboardView?.let { container.addView(it) }
                currentKeyboardMode = previousKeyboardMode
                switchKeyboardMode(currentKeyboardMode)
                Log.d(TAG, "Emoji panel hidden, returned to $currentKeyboardMode")
            }
        }
        
        keyboardView?.setEmojiKeyActive(isEmojiPanelVisible)
    } catch (e: Exception) {
        Log.e(TAG, "Error toggling emoji panel", e)
    }
}
```

---

## Testing Checklist

### Basic Cycling

- [ ] Press ?123 from Letters → Numbers layout shown
- [ ] Press ?123 from Numbers → Symbols layout shown
- [ ] Press ABC from Symbols → Letters layout shown
- [ ] Press ABC from Numbers → Letters layout shown

### Emoji Integration

- [ ] Press Emoji from Letters → Emoji panel shown
- [ ] Press ABC from Emoji → Returns to Letters
- [ ] Press Emoji from Numbers → Emoji panel shown
- [ ] Press ABC from Emoji → Returns to Numbers
- [ ] Press Emoji from Symbols → Emoji panel shown
- [ ] Press ABC from Emoji → Returns to Symbols

### Theme Application

- [ ] Letters keyboard has correct theme
- [ ] Numbers keyboard has correct theme
- [ ] Symbols keyboard has correct theme
- [ ] Emoji panel background = keyboard background
- [ ] Emoji category tabs = toolbar background
- [ ] Emoji category text = key text color
- [ ] Theme persists across mode switches
- [ ] No visual gaps between panels

### Edge Cases

- [ ] Rapid switching works correctly
- [ ] Language switch preserves mode
- [ ] Theme change updates all modes
- [ ] Emoji panel closes properly
- [ ] Mode state survives keyboard hide/show
- [ ] Numbers row setting doesn't break cycling

---

## Summary

### What Changed

✅ **Added CleverType cycling**: Letters → Numbers → Symbols → Letters  
✅ **Unified mode switching**: Single `switchKeyboardMode()` method  
✅ **Emoji panel integration**: Returns to previous mode  
✅ **Backward compatible**: Deprecated old methods still work  
✅ **Emoji theming**: Already working, no changes needed!  

### What Stayed the Same

✅ **Emoji panel theming**: Already perfect (lines 1279-1336)  
✅ **Theme application**: Uses existing `applyTheme()`  
✅ **Layout resources**: XML files can stay as-is (with code mapping)  
✅ **Visual design**: No UI changes, just cycling behavior  

### Benefits

- **Simpler**: One cycling method instead of three switch methods
- **Consistent**: Matches CleverType/Gboard behavior exactly
- **Maintainable**: Enum-based state management
- **Themed**: Emoji panel matches keyboard (already implemented!)
- **Seamless**: No visual gaps, unified background

---

**Status:** ✅ Ready for Implementation  
**Estimated Time:** 30 minutes  
**Risk Level:** Low (backward compatible with deprecation)


