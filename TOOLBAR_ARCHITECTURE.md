# Toolbar Architecture & Panel Switching Documentation

## 📐 Overview

The AI Keyboard uses a **view container switching architecture** where toolbar buttons trigger different panels that **replace** the keyboard view in a shared container.

---

## 🏗️ View Hierarchy

```
mainKeyboardLayout (LinearLayout - root)
  ├─ cleverTypeToolbar (LinearLayout - simplified toolbar)
  │   ├─ btn_grammar_fix (TextView) → ✅
  │   ├─ btn_word_tone (TextView) → 🎨
  │   ├─ btn_ai_assistant (TextView) → 🤖
  │   ├─ btn_clipboard (TextView) → 📋
  │   ├─ btn_more_actions (TextView) → ⋮
  │   └─ btn_smart_backspace (TextView) → ↩
  │
  ├─ suggestionContainer (LinearLayout - suggestions bar)
  │   └─ [3 suggestion TextViews]
  │
  └─ keyboardContainer (LinearLayout - SWAPPABLE CONTENT)
      └─ ONE of these at a time:
          ├─ keyboardView (SwipeKeyboardView)
          ├─ aiPanel (LinearLayout)
          ├─ miniSettingsSheet (LinearLayout)
          ├─ clipboardPanel (View)
          └─ emojiPanel (View)
```

**Key Concept:** The `keyboardContainer` holds **only ONE view at a time**. When a toolbar button is tapped, the container's content is replaced.

---

## 🔄 View Switching Mechanism

### Core Container
```kotlin
private var keyboardContainer: LinearLayout? = null  // The swappable container
```

### State Flags
```kotlin
private var isMiniSettingsVisible = false
private var isAIPanelVisible = false
private var isEmojiPanelVisible = false
```

### Switching Pattern
```kotlin
// STEP 1: Remove all views from container
keyboardContainer?.removeAllViews()

// STEP 2: Add new view
keyboardContainer?.addView(newView)

// STEP 3: Update state flag
isMiniSettingsVisible = true  // or whichever panel
```

---

## 🎯 Toolbar Button Actions

### 1. ✅ Grammar Fix Button
**ID:** `btn_grammar_fix`

**Flow:**
```
User taps ✅
    ↓
onClick listener triggered
    ↓
openAIPanel(AIPanelType.GRAMMAR)
    ↓
Checks if text exists
    ↓
Closes mini settings if open
    ↓
keyboardContainer.removeAllViews()
    ↓
Adds AI panel to container
    ↓
Shows grammar options (Rephrase, Fix Grammar, Expand, etc.)
```

**Code:**
```kotlin
toolbar.findViewById<TextView>(R.id.btn_grammar_fix)?.setOnClickListener {
    Log.d(TAG, "Grammar Fix button tapped")
    openAIPanel(AIPanelType.GRAMMAR)
}
```

---

### 2. 🎨 Word Tone Button
**ID:** `btn_word_tone`

**Flow:**
```
User taps 🎨
    ↓
onClick listener triggered
    ↓
openAIPanel(AIPanelType.TONE)
    ↓
Checks if text exists
    ↓
Closes mini settings if open
    ↓
keyboardContainer.removeAllViews()
    ↓
Adds AI panel to container
    ↓
Shows tone options (Formal, Casual, Funny, etc.)
```

**Code:**
```kotlin
toolbar.findViewById<TextView>(R.id.btn_word_tone)?.setOnClickListener {
    Log.d(TAG, "Word Tone button tapped")
    openAIPanel(AIPanelType.TONE)
}
```

---

### 3. 🤖 AI Assistant Button
**ID:** `btn_ai_assistant`

**Flow:**
```
User taps 🤖
    ↓
onClick listener triggered
    ↓
openAIPanel(AIPanelType.ASSISTANT)
    ↓
Checks if text exists
    ↓
Closes mini settings if open
    ↓
keyboardContainer.removeAllViews()
    ↓
Adds AI panel to container
    ↓
Shows custom prompts (user-defined AI actions)
```

**Code:**
```kotlin
toolbar.findViewById<TextView>(R.id.btn_ai_assistant)?.setOnClickListener {
    Log.d(TAG, "AI Assistant button tapped")
    openAIPanel(AIPanelType.ASSISTANT)
}
```

---

### 4. 📋 Clipboard Button
**ID:** `btn_clipboard`

**Flow:**
```
User taps 📋
    ↓
onClick listener triggered
    ↓
handleClipboardAccess()
    ↓
Sets currentInputMode = INPUT_MODE_CLIPBOARD
    ↓
showClipboardKeyboard()
    ↓
keyboardContainer.removeAllViews()
    ↓
Adds clipboard panel
    ↓
Shows recent clipboard items
```

**Code:**
```kotlin
toolbar.findViewById<TextView>(R.id.btn_clipboard)?.setOnClickListener {
    Log.d(TAG, "Clipboard button tapped")
    handleClipboardAccess()
}
```

---

### 5. ⋮ More Actions Button
**ID:** `btn_more_actions`

**Flow:**
```
User taps ⋮
    ↓
onClick listener triggered
    ↓
showMiniSettingsSheet()
    ↓
Closes AI panel if open
    ↓
keyboardContainer.removeAllViews()
    ↓
Inflates mini_settings_sheet.xml
    ↓
Adds settings sheet to container
    ↓
Shows quick settings (Sound, Vibration, AI, Number Row)
```

**Code:**
```kotlin
toolbar.findViewById<TextView>(R.id.btn_more_actions)?.setOnClickListener {
    Log.d(TAG, "More Actions button tapped")
    showMiniSettingsSheet()
}
```

---

### 6. ↩ Smart Backspace Button
**ID:** `btn_smart_backspace`

**Flow:**
```
User taps ↩
    ↓
onClick listener triggered
    ↓
deleteFullWord()
    ↓
Gets text before cursor (up to 100 chars)
    ↓
Finds last word (non-whitespace sequence)
    ↓
ic.deleteSurroundingText(wordLength, 0)
    ↓
Provides haptic feedback
    ↓
Word deleted
```

**Code:**
```kotlin
toolbar.findViewById<TextView>(R.id.btn_smart_backspace)?.setOnClickListener {
    Log.d(TAG, "Smart Backspace button tapped")
    deleteFullWord()
}
```

---

## 📝 Key Methods

### 1. `openAIPanel(type: AIPanelType)`
**Purpose:** Opens AI panel with grammar/tone/assistant options

**Logic:**
```kotlin
private fun openAIPanel(type: AIPanelType) {
    // 1. Get current text
    currentAIOriginalText = getSelectedTextOrFull()
    if (currentAIOriginalText.isEmpty()) {
        Toast.makeText(this, "No text to process", Toast.LENGTH_SHORT).show()
        return
    }
    
    // 2. Close mini settings if open
    if (isMiniSettingsVisible) {
        isMiniSettingsVisible = false
    }
    
    // 3. Remove ALL views from container
    keyboardContainer?.removeAllViews()
    
    // 4. Hide keyboard and emoji views
    keyboardView?.visibility = View.GONE
    emojiPanelView?.visibility = View.GONE
    
    // 5. Hide suggestion bar
    suggestionContainer?.visibility = View.GONE
    
    // 6. Add AI panel if not already added
    if (aiPanel?.parent == null) {
        keyboardContainer?.addView(aiPanel)
    }
    
    // 7. Show AI panel
    aiPanel?.visibility = View.VISIBLE
    isAIPanelVisible = true
    
    // 8. Populate chips based on type
    populateAIChips(type)
}
```

**What it does:**
- ✅ Validates text exists
- ✅ Closes conflicting panels (mini settings)
- ✅ Clears container completely
- ✅ Adds and shows AI panel
- ✅ Updates state flags

---

### 2. `closeAIPanel()`
**Purpose:** Closes AI panel and restores keyboard

**Logic:**
```kotlin
private fun closeAIPanel() {
    // 1. Remove AI panel from container
    keyboardContainer?.removeView(aiPanel)
    aiPanel?.visibility = View.GONE
    
    // 2. Restore keyboard view
    keyboardView?.visibility = View.VISIBLE
    keyboardView?.let { keyboardContainer?.addView(it) }
    
    // 3. Update state
    isAIPanelVisible = false
    
    // 4. Show suggestion bar
    suggestionContainer?.visibility = View.VISIBLE
    
    // 5. Clear state
    currentAIOriginalText = ""
}
```

**What it does:**
- ✅ Removes AI panel from container
- ✅ Re-adds keyboard view to container
- ✅ Restores suggestion bar
- ✅ Clears temporary state

---

### 3. `showMiniSettingsSheet()`
**Purpose:** Shows quick settings panel

**Logic:**
```kotlin
private fun showMiniSettingsSheet() {
    val container = keyboardContainer ?: return
    
    // 1. Close AI panel if open
    if (isAIPanelVisible) {
        closeAIPanel()
    }
    
    // 2. Remove all views from container
    container.removeAllViews()
    
    // 3. Inflate settings sheet
    val settingsSheet = layoutInflater.inflate(
        R.layout.mini_settings_sheet, 
        container, 
        false
    )
    
    // 4. Apply theme colors
    val palette = themeManager.getCurrentPalette()
    settingsSheet.setBackgroundColor(palette.keyboardBg)
    
    // 5. Load current settings
    val prefs = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
    // ... setup switches
    
    // 6. Add to container
    container.addView(settingsSheet)
    isMiniSettingsVisible = true
}
```

**What it does:**
- ✅ Closes conflicting panels (AI panel)
- ✅ Clears container
- ✅ Inflates settings layout
- ✅ Applies theme
- ✅ Loads current settings into switches
- ✅ Updates state flag

---

### 4. `restoreKeyboardFromSettings()`
**Purpose:** Returns from settings sheet to keyboard

**Logic:**
```kotlin
private fun restoreKeyboardFromSettings() {
    val container = keyboardContainer ?: return
    
    // 1. Ensure AI panel is closed
    if (isAIPanelVisible) {
        aiPanel?.visibility = View.GONE
        isAIPanelVisible = false
    }
    
    // 2. Remove settings sheet
    container.removeAllViews()
    
    // 3. Check if number row changed
    val prefs = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
    val newNumberRow = prefs.getBoolean("show_number_row", false)
    
    // 4. Reload keyboard if layout changed, otherwise restore
    if (newNumberRow != showNumberRow) {
        showNumberRow = newNumberRow
        reloadKeyboard()
    } else {
        keyboardView?.let { container.addView(it) }
    }
    
    // 5. Update state
    isMiniSettingsVisible = false
}
```

**What it does:**
- ✅ Ensures no other panels are open
- ✅ Removes settings sheet
- ✅ Checks if keyboard layout needs reload (number row toggle)
- ✅ Either reloads or restores keyboard
- ✅ Updates state flag

---

### 5. `deleteFullWord()`
**Purpose:** Smart backspace - deletes entire last word

**Logic:**
```kotlin
private fun deleteFullWord() {
    val ic = currentInputConnection ?: return
    
    // 1. Get text before cursor
    val textBeforeCursor = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
    
    if (textBeforeCursor.isEmpty()) {
        Log.d(TAG, "No text before cursor to delete")
        return
    }
    
    // 2. Find the last word
    val trimmed = textBeforeCursor.trimEnd()
    val lastSpaceIndex = trimmed.lastIndexOf(' ')
    val lastWord = if (lastSpaceIndex >= 0) {
        trimmed.substring(lastSpaceIndex + 1)
    } else {
        trimmed
    }
    
    // 3. Delete the word
    if (lastWord.isNotEmpty()) {
        ic.deleteSurroundingText(lastWord.length, 0)
        
        // 4. Haptic feedback
        performAdvancedHapticFeedback(Keyboard.KEYCODE_DELETE)
    }
}
```

**What it does:**
- ✅ Gets text before cursor
- ✅ Finds last complete word (space-delimited)
- ✅ Deletes entire word at once
- ✅ Provides haptic feedback

---

## 🔁 Panel Conflict Resolution

### Problem: Multiple Panels Open Simultaneously
**Solution:** Each panel opening method closes conflicting panels first

### Conflict Matrix

| Opening Panel | Closes These Panels | Method Called |
|---------------|---------------------|---------------|
| AI Panel | Mini Settings | `isMiniSettingsVisible = false` |
| Mini Settings | AI Panel | `closeAIPanel()` |
| Keyboard | All panels | `container.removeAllViews()` |

### Implementation Pattern

**When opening AI panel:**
```kotlin
// Close mini settings if open
if (isMiniSettingsVisible) {
    isMiniSettingsVisible = false
}
keyboardContainer?.removeAllViews()  // Nuclear option
```

**When opening mini settings:**
```kotlin
// Close AI panel if open
if (isAIPanelVisible) {
    closeAIPanel()
}
container.removeAllViews()  // Nuclear option
```

**Result:** Only ONE panel is ever visible at a time ✅

---

## 🎨 Theme Integration

All panels use the same theme colors for consistency:

```kotlin
val palette = themeManager.getCurrentPalette()

// Apply to toolbar
toolbarView.setBackgroundColor(palette.toolbarBg)
buttonView.setTextColor(palette.keyText)

// Apply to settings sheet
settingsSheet.setBackgroundColor(palette.keyboardBg)
textView.setTextColor(palette.keyText)
button.setTint(palette.specialAccent)

// Apply to AI panel
aiPanel.background = themeManager.createKeyboardBackground()
```

**Theme Properties Used:**
- `palette.toolbarBg` - Toolbar background color
- `palette.keyboardBg` - Panel background color
- `palette.keyText` - Text color (auto-contrast)
- `palette.specialAccent` - Accent color for buttons

---

## 📊 Sequence Diagrams

### Opening AI Panel from Keyboard

```
User                Toolbar              AIKeyboardService           Container
 |                    |                         |                         |
 |-- tap ✅ --------->|                         |                         |
 |                    |-- openAIPanel(GRAMMAR)->|                         |
 |                    |                         |                         |
 |                    |                         |-- getSelectedText() --->|
 |                    |                         |<-- text ----------------|
 |                    |                         |                         |
 |                    |                         |-- removeAllViews() ---->|
 |                    |                         |                         |
 |                    |                         |-- addView(aiPanel) ---->|
 |                    |                         |                         |
 |                    |                         |-- populateChips() ----->|
 |                    |                         |                         |
 |<-- AI Panel shown ----------------------------|------------------------|
```

### Opening Mini Settings from AI Panel

```
User                Toolbar              AIKeyboardService           Container
 |                    |                         |                         |
 |-- tap ⋮ ---------->|                         |                         |
 |                    |-- showMiniSettings() -->|                         |
 |                    |                         |                         |
 |                    |                         |-- closeAIPanel() ------>|
 |                    |                         |<-- AI panel removed ----|
 |                    |                         |                         |
 |                    |                         |-- removeAllViews() ---->|
 |                    |                         |                         |
 |                    |                         |-- inflate(settings) --->|
 |                    |                         |                         |
 |                    |                         |-- addView(settings) --->|
 |                    |                         |                         |
 |<-- Settings shown -----------------------------|------------------------|
```

### Returning to Keyboard

```
User                Button               AIKeyboardService           Container
 |                    |                         |                         |
 |-- tap "Back" ----->|                         |                         |
 |                    |-- restoreKeyboard() --->|                         |
 |                    |                         |                         |
 |                    |                         |-- removeAllViews() ---->|
 |                    |                         |                         |
 |                    |                         |-- addView(keyboard) --->|
 |                    |                         |                         |
 |                    |                         |-- show suggestions ---->|
 |                    |                         |                         |
 |<-- Keyboard shown -----------------------------|------------------------|
```

---

## 🧪 Testing Scenarios

### Scenario 1: Panel Switching
```
✅ Tap Grammar → AI panel opens (keyboard hidden)
✅ Tap More → Settings opens (AI panel closes)
✅ Tap Back → Keyboard restored (settings closes)
```

### Scenario 2: Multiple Panel Attempts
```
✅ Tap Grammar → AI panel opens
✅ Tap Tone → AI panel updates (same panel, different options)
✅ Tap More → Settings opens (AI panel closes)
✅ Tap Grammar → AI panel opens (settings closes)
```

### Scenario 3: Smart Backspace
```
✅ Type "Hello world amazing"
✅ Tap ↩ → Deletes "amazing"
✅ Result: "Hello world "
✅ Tap ↩ → Deletes "world"
✅ Result: "Hello "
```

---

## 🔍 Debugging Tips

### Check Current Panel State
```kotlin
Log.d(TAG, "Panel states:")
Log.d(TAG, "- AI Panel: $isAIPanelVisible")
Log.d(TAG, "- Settings: $isMiniSettingsVisible")
Log.d(TAG, "- Emoji: $isEmojiPanelVisible")
Log.d(TAG, "- Container children: ${keyboardContainer?.childCount}")
```

### Verify Container Contents
```kotlin
keyboardContainer?.let { container ->
    Log.d(TAG, "Container has ${container.childCount} children:")
    for (i in 0 until container.childCount) {
        val child = container.getChildAt(i)
        Log.d(TAG, "  Child $i: ${child::class.simpleName}")
    }
}
```

### Common Issues

**Problem:** Multiple panels showing
- **Cause:** Panels adding without removing previous ones
- **Fix:** Always call `removeAllViews()` before adding new panel

**Problem:** Blank screen after panel close
- **Cause:** Keyboard not re-added to container
- **Fix:** Ensure `keyboardView?.let { container.addView(it) }`

**Problem:** Settings not persisting
- **Cause:** SharedPreferences not applying changes
- **Fix:** Call `.apply()` or `.commit()` after edit

---

## 📚 Summary

### Key Principles

1. **Single Container Pattern**
   - One `keyboardContainer` holds all swappable views
   - Only ONE view visible at a time

2. **Explicit Removal**
   - Always `removeAllViews()` before adding new panel
   - Prevents multiple panels stacking

3. **State Tracking**
   - Boolean flags track which panel is open
   - Used for conflict resolution

4. **Theme Consistency**
   - All panels use same `ThemePaletteV2` colors
   - Seamless visual transitions

5. **Graceful Degradation**
   - Null checks on all container operations
   - Toast messages for user feedback on errors

### Flow Summary

```
Toolbar Button Tap
    ↓
Check for conflicting panels → Close them
    ↓
Remove all views from container
    ↓
Inflate/create new panel view
    ↓
Apply theme colors
    ↓
Add panel to container
    ↓
Update state flags
    ↓
Panel displayed ✅
```

---

## 🎯 Quick Reference

| Action | Method | State Flag | Container Action |
|--------|--------|------------|------------------|
| Open Grammar | `openAIPanel(GRAMMAR)` | `isAIPanelVisible = true` | `removeAllViews()` + `addView(aiPanel)` |
| Open Tone | `openAIPanel(TONE)` | `isAIPanelVisible = true` | `removeAllViews()` + `addView(aiPanel)` |
| Open Assistant | `openAIPanel(ASSISTANT)` | `isAIPanelVisible = true` | `removeAllViews()` + `addView(aiPanel)` |
| Open Clipboard | `handleClipboardAccess()` | *(internal)* | `removeAllViews()` + `addView(clipboardPanel)` |
| Open Settings | `showMiniSettingsSheet()` | `isMiniSettingsVisible = true` | `removeAllViews()` + `addView(settingsSheet)` |
| Close AI Panel | `closeAIPanel()` | `isAIPanelVisible = false` | `removeView(aiPanel)` + `addView(keyboard)` |
| Close Settings | `restoreKeyboardFromSettings()` | `isMiniSettingsVisible = false` | `removeAllViews()` + `addView(keyboard)` |
| Delete Word | `deleteFullWord()` | *(no change)* | *(no change)* |

---

**Implementation Status: ✅ COMPLETE AND DOCUMENTED**

