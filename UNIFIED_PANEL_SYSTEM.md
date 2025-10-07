# Unified Feature Panel System 🎯

## Overview
The **Unified Feature Panel System** is a single, dynamic panel that handles all toolbar features (Grammar Fix, Word Tone, AI Assistant, and Clipboard). This replaces the previous fragmented approach with a clean, maintainable architecture.

---

## 🏗️ Architecture

### Core Concept
Instead of separate panels for each feature, we now have:
- **One shared container layout** (`panel_feature_shared.xml`)
- **Dynamic body content** loaded based on panel type
- **Single entry point** (`showFeaturePanel(type: PanelType)`)

### Flow Diagram
```
Toolbar Icon Tap
    ↓
showFeaturePanel(PanelType)
    ↓
1. Close any open panels
2. Inflate shared panel layout
3. Configure header (title + right widget)
4. Load body content for specific feature
5. Replace keyboard view
    ↓
User interacts with panel
    ↓
Tap Back Arrow (←)
    ↓
restoreKeyboardFromPanel()
    ↓
Normal keyboard restored
```

---

## 📂 File Structure

### Main Components

#### 1. **Shared Panel Layout**
**File:** `panel_feature_shared.xml`

```
┌─────────────────────────────────────┐
│ ← Back  |  Title  |  Right Widget  │  <- Header
├─────────────────────────────────────┤
│                                     │
│         Panel Body Content          │  <- Dynamic
│       (Grammar/Tone/AI/Clipboard)   │
│                                     │
└─────────────────────────────────────┘
```

**Structure:**
- `panelHeader` (LinearLayout) - Top bar
  - `btnBack` (TextView) - Back arrow button
  - `panelTitle` (TextView) - Panel title
  - `panelRightContainer` (FrameLayout) - Dynamic right widget
- `panelBody` (FrameLayout) - Main content area

#### 2. **Panel Body Templates**

| File | Purpose | Actions |
|------|---------|---------|
| `panel_body_grammar.xml` | Grammar corrections | Rephrase, Fix Grammar, Add Emojis |
| `panel_body_tone.xml` | Tone adjustments | Funny, Poetic, Shorten, Sarcastic |
| `panel_body_ai_assistant.xml` | AI writing help | ChatGPT, Humanize, Reply, Idioms |
| `panel_body_clipboard.xml` | Clipboard history | Recent clips (tap to paste) |

#### 3. **Header Right Widgets**

| File | Used By | Content |
|------|---------|---------|
| `panel_right_translate.xml` | Grammar, Tone, AI | 🌍 + Language dropdown |
| `panel_right_toggle.xml` | Clipboard | ON/OFF switch |

#### 4. **Drawable Resources**

| File | Purpose | Properties |
|------|---------|------------|
| `action_button_background.xml` | Button backgrounds | 8dp radius, themed accent |
| `input_text_background.xml` | Text output areas | 12dp radius, semi-transparent |

---

## 🔧 Implementation Details

### Enum: PanelType
```kotlin
enum class PanelType {
    GRAMMAR_FIX,
    WORD_TONE,
    AI_ASSISTANT,
    CLIPBOARD
}
```

### Main Function: `showFeaturePanel()`

**Location:** `AIKeyboardService.kt` (lines 8408-8484)

**Process:**
1. **Close other panels**
   ```kotlin
   if (isAIPanelVisible) {
       aiPanel?.visibility = View.GONE
       isAIPanelVisible = false
   }
   ```

2. **Inflate shared layout**
   ```kotlin
   val featurePanel = layoutInflater.inflate(R.layout.panel_feature_shared, null)
   ```

3. **Apply theme colors**
   ```kotlin
   val palette = themeManager.getCurrentPalette()
   featurePanel.setBackgroundColor(palette.keyboardBg)
   ```

4. **Configure based on type**
   ```kotlin
   when (type) {
       PanelType.GRAMMAR_FIX -> {
           title?.text = "Fix Grammar"
           // Add translate widget
           inflateGrammarBody(body)
       }
       // ... other cases
   }
   ```

5. **Replace keyboard view**
   ```kotlin
   keyboardContainer?.removeAllViews()
   keyboardContainer?.addView(featurePanel)
   ```

### Helper Functions

#### `inflateGrammarBody(container: FrameLayout?)`
- Inflates grammar panel body
- Wires up button listeners
- TODO: Connect to actual grammar API

#### `inflateToneBody(container: FrameLayout?)`
- Inflates tone adjustment body
- Sets up tone chip buttons
- TODO: Implement tone transformation

#### `inflateAIAssistantBody(container: FrameLayout?)`
- Inflates AI assistant body
- Configures AI action buttons
- TODO: Integrate with AI service

#### `inflateClipboardBody(container: FrameLayout?)`
- Inflates clipboard history body
- Sets up clip item listeners
- Currently shows placeholder clips

#### `restoreKeyboardFromPanel()`
- Removes panel from container
- Restores keyboard view
- Shows suggestion bar
- Called when Back (←) is tapped

---

## 🎨 Toolbar Integration

### Toolbar Button Configuration

**File:** `setupSimplifiedToolbarListeners()` (lines 8585-8639)

| Button | Icon | Action |
|--------|------|--------|
| Grammar Fix | ✅ | `showFeaturePanel(PanelType.GRAMMAR_FIX)` |
| Word Tone | 🎨 | `showFeaturePanel(PanelType.WORD_TONE)` |
| AI Assistant | 🤖 | `showFeaturePanel(PanelType.AI_ASSISTANT)` |
| Clipboard | 📋 | `showFeaturePanel(PanelType.CLIPBOARD)` |
| More Actions | ⋮ | `showMiniSettingsSheet()` (separate) |
| Smart Backspace | ↩ | `deleteFullWord()` |

**Updated listeners:**
```kotlin
toolbar.findViewById<TextView>(R.id.btn_grammar_fix)?.apply {
    setTextColor(palette.keyText)
    setOnClickListener {
        showFeaturePanel(PanelType.GRAMMAR_FIX)
    }
}
```

---

## 🎯 Panel-Specific Features

### 1. Grammar Fix Panel
**Header:** `← Back | Fix Grammar | 🌍 English ▼`

**Body Actions:**
- **Rephrase** - Rewrites selected text
- **Fix Grammar** - Corrects grammatical errors
- **Add Emojis** - Suggests contextual emojis

**Output:**
- Corrected text displayed in text area
- "Replace Text" button commits to input

### 2. Word Tone Panel
**Header:** `← Back | Word Tone | 🌍 English ▼`

**Tone Chips:**
- 😄 **Funny** - Add humor
- ✨ **Poetic** - Literary style
- 📝 **Shorten** - Condense text
- 😏 **Sarcastic** - Sarcastic tone

**Output:**
- Tone-adjusted text preview
- "Replace Text" to apply

### 3. AI Writing Assistant Panel
**Header:** `← Back | AI Writing Assistant | 🌍 English ▼`

**AI Actions:**
- 💬 **ChatGPT** - General AI assistance
- 👤 **Humanize** - Make text more natural
- ↩️ **Reply** - Smart reply suggestions
- 📚 **Idioms** - Add idiomatic expressions

**Output:**
- AI-generated text
- "Replace Text" to use

### 4. Clipboard Panel
**Header:** `← Back | Clipboard | 🔘 ON/OFF`

**Features:**
- **Recent Clips** - Last 3 clipboard items
- **Tap to paste** - Insert clip at cursor
- **Toggle** - Enable/disable clipboard tracking

**Current Clips (placeholder):**
- "I dont like agquuq"
- "ahajab"
- "I dint like"

---

## 🎨 Theming

### Theme Application

All panels use `ThemePaletteV2` for consistent theming:

```kotlin
val palette = themeManager.getCurrentPalette()

// Panel background
featurePanel.setBackgroundColor(palette.keyboardBg)

// Header background
panelHeader.setBackgroundColor(palette.toolbarBg)

// Text colors
title.setTextColor(palette.keyText)

// Accent for buttons
// Uses palette.specialAccent
```

### Color Properties Used
- `palette.keyboardBg` - Panel background
- `palette.toolbarBg` - Header background
- `palette.keyText` - Primary text
- `palette.specialAccent` - Button accents

---

## 🔄 State Management

### Panel State Variables

```kotlin
private var isAIPanelVisible = false         // Old AI panel
private var isMiniSettingsVisible = false    // Mini settings
```

**Mutual Exclusivity:**
- Only one panel can be visible at a time
- `showFeaturePanel()` closes other panels before opening
- `showMiniSettingsSheet()` closes AI panel if visible

### Container Management

**Keyboard Container:**
```kotlin
private var keyboardContainer: LinearLayout? = null
```

**View Switching:**
```kotlin
// Remove current view
keyboardContainer?.removeAllViews()

// Add new panel
keyboardContainer?.addView(featurePanel)
```

---

## 🚀 Usage Examples

### Opening a Panel from Code
```kotlin
// Grammar Fix
showFeaturePanel(PanelType.GRAMMAR_FIX)

// Word Tone
showFeaturePanel(PanelType.WORD_TONE)

// AI Assistant
showFeaturePanel(PanelType.AI_ASSISTANT)

// Clipboard
showFeaturePanel(PanelType.CLIPBOARD)
```

### Restoring Keyboard
```kotlin
// Called automatically by Back (←) button
restoreKeyboardFromPanel()

// Or call manually
restoreKeyboardFromPanel()
```

### Adding New Panel Type

1. **Add to enum:**
```kotlin
enum class PanelType {
    GRAMMAR_FIX,
    WORD_TONE,
    AI_ASSISTANT,
    CLIPBOARD,
    NEW_FEATURE  // ← Add here
}
```

2. **Create body layout:**
```xml
<!-- panel_body_new_feature.xml -->
<LinearLayout ...>
    <!-- Your UI -->
</LinearLayout>
```

3. **Add inflate function:**
```kotlin
private fun inflateNewFeatureBody(container: FrameLayout?) {
    val view = layoutInflater.inflate(
        R.layout.panel_body_new_feature, 
        container, 
        false
    )
    container?.addView(view)
    
    // Wire up listeners
}
```

4. **Add to when clause:**
```kotlin
when (type) {
    // ... existing cases
    PanelType.NEW_FEATURE -> {
        title?.text = "New Feature"
        // Configure header widget
        inflateNewFeatureBody(body)
    }
}
```

---

## 📊 Benefits Over Previous System

### Before (Fragmented Panels)
❌ Separate layout files for each panel  
❌ Duplicate header code  
❌ Inconsistent theming  
❌ Multiple show/hide functions  
❌ Hard to maintain  

### After (Unified System)
✅ Single shared layout  
✅ Reusable header components  
✅ Consistent theme application  
✅ One entry point function  
✅ Easy to extend  
✅ Reduced code duplication  

---

## 🐛 Debugging

### Common Issues

#### Panel not showing?
**Check:**
1. Is `keyboardContainer` initialized?
2. Are layout files present in `res/layout/`?
3. Check Logcat for errors: `TAG: "AIKeyboard"`

#### Theme not applying?
**Check:**
1. `themeManager.getCurrentPalette()` returns valid colors
2. Color resources exist in `values/colors.xml`

#### Back button not working?
**Check:**
1. Back button listener is set: `btnBack.setOnClickListener { ... }`
2. `restoreKeyboardFromPanel()` is called
3. `keyboardView` is not null

### Debug Logs

**Panel opening:**
```
D/AIKeyboard: Opening feature panel: GRAMMAR_FIX
D/AIKeyboard: ✅ Feature panel displayed: GRAMMAR_FIX
```

**Panel closing:**
```
D/AIKeyboard: Back button tapped, restoring keyboard
D/AIKeyboard: ✅ Keyboard restored from panel
```

---

## 🔮 Future Enhancements

### Planned Features
- [ ] Dynamic clipboard history from `ClipboardHistoryManager`
- [ ] Grammar API integration
- [ ] Tone transformation backend
- [ ] AI service integration (ChatGPT, etc.)
- [ ] Language switcher functionality
- [ ] Panel animations (slide in/out)
- [ ] Persistent panel state
- [ ] Multi-language support for panels

### API Integration Points
```kotlin
// Grammar API
private fun applyGrammarFix(text: String): String {
    // TODO: Call grammar API
    return correctedText
}

// Tone API
private fun applyTone(text: String, tone: String): String {
    // TODO: Call tone transformation API
    return tonedText
}

// AI API
private fun callAIAssistant(prompt: String): String {
    // TODO: Call AI service
    return aiResponse
}
```

---

## 📝 Summary

The **Unified Feature Panel System** provides:
- **Consistency** - All panels share the same structure
- **Maintainability** - Single source of truth
- **Extensibility** - Easy to add new panel types
- **Theming** - Automatic theme application
- **User Experience** - Smooth panel switching

**Key Files:**
- `AIKeyboardService.kt` - Main logic
- `panel_feature_shared.xml` - Shared layout
- `panel_body_*.xml` - Feature-specific content
- `panel_right_*.xml` - Header widgets

**Entry Point:**
```kotlin
showFeaturePanel(PanelType.GRAMMAR_FIX)
```

**Exit Point:**
```kotlin
restoreKeyboardFromPanel()
```

---

*Last Updated: 2025-10-06*  
*Author: AI Keyboard Development Team*

