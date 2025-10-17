# AI Keyboard - Layout System Architecture Analysis

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [JSON Template Architecture](#json-template-architecture)
3. [Layout Formation Flow](#layout-formation-flow)
4. [Auto-Adjust Mechanism](#auto-adjust-mechanism)
5. [Key Components](#key-components)
6. [Complete Processing Pipeline](#complete-processing-pipeline)

---

## 1. System Overview

The AI Keyboard uses a **modern dynamic multilingual layout system** that separates keyboard structure from character mappings. This architecture enables:

- ✅ **Reusable Templates**: Base keyboard layouts (QWERTY, INSCRIPT, Arabic)
- ✅ **Language-Specific Keymaps**: Character mappings per language
- ✅ **Dynamic Mode Switching**: Letters, Symbols, Extended Symbols, Dialer
- ✅ **Auto-Adjust Height**: Automatic keyboard height calculation
- ✅ **RTL Support**: Right-to-left language layouts
- ✅ **Number Row**: Optional number row with native numerals

---

## 2. JSON Template Architecture

### 2.1 Template Structure

**Location**: `/android/app/src/main/assets/layout_templates/`

**Example - qwerty_template.json**:
```json
{
  "name": "QWERTY",
  "description": "Standard QWERTY layout for Latin-based languages",
  "rows": [
    ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
    ["a", "s", "d", "f", "g", "h", "j", "k", "l"],
    ["SHIFT", "z", "x", "c", "v", "b", "n", "m", "DELETE"],
    ["?123", ",", "GLOBE", "SPACE", ".", "RETURN"]
  ]
}
```

### 2.2 Available Templates

| Template | Use Case | Languages |
|----------|----------|-----------|
| `qwerty_template.json` | Latin-based | English, French, Spanish, German |
| `inscript_template.json` | Indic scripts | Hindi, Telugu, Tamil, Malayalam |
| `arabic_template.json` | Arabic script | Arabic, Urdu, Farsi |
| `symbols_template.json` | Symbol mode | All languages |
| `extended_symbols_template.json` | Extended symbols | All languages |
| `dialer_template.json` | Number pad | All languages |

### 2.3 Language-Specific Keymaps

**Location**: `/android/app/src/main/assets/keymaps/`

Keymaps define character mappings for each language:
- `base`: Primary characters (lowercase)
- `shift`: Shifted characters (uppercase/alternates)
- `alt`: Number row characters (native numerals)
- `longPress`: Long-press accent variants

**Example - keymap structure**:
```json
{
  "language": "hindi",
  "template": "inscript_template.json",
  "base": {"q": "ौ", "w": "ै", ...},
  "shift": {"q": "औ", "w": "ऐ", ...},
  "alt": {"1": "१", "2": "२", ...},
  "longPress": {"a": ["à", "á", "â", "ä"]}
}
```

---

## 3. Layout Formation Flow

### 3.1 Component Hierarchy

```
AIKeyboardService (Orchestrator)
    ↓
LanguageLayoutAdapter (JSON Processor)
    ↓
SwipeKeyboardView (Renderer)
    ↓
DynamicKey Objects (Individual Keys)
```

### 3.2 Processing Steps

#### **Step 1: Request Layout**
```kotlin
// AIKeyboardService.kt - Line 4228
private fun loadDynamicLayout(
    languageCode: String, 
    mode: KeyboardMode
) {
    keyboardView?.setKeyboardMode(mode, languageLayoutAdapter, showNumberRow)
}
```

#### **Step 2: Build Layout Model**
```kotlin
// LanguageLayoutAdapter.kt - Line 95
suspend fun buildLayoutFor(
    languageCode: String, 
    mode: KeyboardMode, 
    numberRowEnabled: Boolean
): LayoutModel {
    // 1. Determine template based on mode and language
    val templateName = when (mode) {
        LETTERS -> keymap.optString("template", "qwerty_template.json")
        SYMBOLS -> "symbols_template.json"
        EXTENDED_SYMBOLS -> "extended_symbols_template.json"
        DIALER -> "dialer_template.json"
    }
    
    // 2. Load base template
    val baseLayout = loadTemplate(templateName)
    
    // 3. Apply language-specific mappings
    val keymap = loadKeymap(languageCode)
    val rows = applyKeymapToTemplate(baseLayout, keymap)
    
    // 4. Inject number row if enabled
    if (numberRowEnabled && mode == LETTERS) {
        rows = listOf(buildNumberRow(languageCode)) + rows
    }
    
    return LayoutModel(rows, languageCode, layoutType, direction)
}
```

#### **Step 3: Convert to DynamicKey Objects**
```kotlin
// SwipeKeyboardView.kt - Line 1355
fun setDynamicLayout(layout: LayoutModel, showNumberRow: Boolean) {
    dynamicKeys.clear()
    
    // Calculate dimensions
    val screenWidth = width
    val screenHeight = height
    val numRows = layout.rows.size
    val keyHeight = screenHeight / numRows
    
    // Build keys for each row
    layout.rows.forEach { row ->
        val totalWidthUnits = row.sumOf { getKeyWidthFactor(it.label) }
        val unitWidth = screenWidth / totalWidthUnits
        
        row.forEach { keyModel ->
            val keyWidth = unitWidth * getKeyWidthFactor(keyModel.label)
            
            dynamicKeys.add(DynamicKey(
                x = currentX,
                y = currentY,
                width = keyWidth,
                height = keyHeight,
                label = keyModel.label,
                code = keyModel.code,
                longPressOptions = keyModel.longPress
            ))
        }
    }
}
```

#### **Step 4: Render Keys**
```kotlin
// SwipeKeyboardView.kt - onDraw() method
override fun onDraw(canvas: Canvas) {
    if (isDynamicLayoutMode) {
        dynamicKeys.forEach { key ->
            drawThemedKey(canvas, key, themeManager)
        }
    }
}
```

---

## 4. Auto-Adjust Mechanism

### 4.1 Height Calculation System

**File**: `KeyboardHeightManager.kt`

```kotlin
fun calculateKeyboardHeight(
    includeToolbar: Boolean = true,
    includeSuggestions: Boolean = true
): Int {
    val screenHeight = getScreenHeight()
    val isLandscape = orientation == LANDSCAPE
    
    // Calculate base height as percentage of screen
    val basePercent = if (isLandscape) 0.5f else 0.24f
    val baseHeight = (screenHeight * basePercent).toInt()
    
    // Clamp between min/max
    val keyboardHeight = baseHeight.coerceIn(
        MIN_HEIGHT_DP.toPx(), 
        MAX_HEIGHT_DP.toPx()
    )
    
    // Add toolbar and suggestions
    var totalHeight = keyboardHeight
    if (includeToolbar) totalHeight += 72.dp
    if (includeSuggestions) totalHeight += 44.dp
    
    return totalHeight
}
```

### 4.2 Constants

| Constant | Portrait | Landscape |
|----------|----------|-----------|
| **Keyboard Height** | 24% of screen | 50% of screen |
| **Min Height** | 260dp | - |
| **Max Height** | 310dp | - |
| **Toolbar Height** | 72dp | 72dp |
| **Suggestion Bar** | 44dp | 44dp |

### 4.3 Auto-Adjust Triggers

The keyboard auto-adjusts in these scenarios:

#### **Scenario 1: Initial Keyboard Open**
```kotlin
// AIKeyboardService.kt - Line 4234
mainHandler.post {
    keyboardView?.setKeyboardMode(mode, adapter, showNumberRow)
    
    // 🎯 AUTO-ADJUST: Force recalculation
    keyboardContainer?.requestLayout()
    mainKeyboardLayout?.requestLayout()
    updateInputViewShown()
}
```

#### **Scenario 2: Mode Switch (Symbols/Dialer)**
```kotlin
// Same auto-adjust calls after loading new mode
loadDynamicLayout(currentLanguage, mode)
```

#### **Scenario 3: Language Change**
```kotlin
// Triggered when user switches language
loadLanguageLayout(newLanguage)
// Includes same auto-adjust calls
```

#### **Scenario 4: Number Row Toggle**
```kotlin
// When user enables/disables number row
keyboardView?.toggleNumberRow(enabled)
keyboardContainer?.requestLayout()
```

### 4.4 Why Auto-Adjust Was Failing

**Problem**: On first open, `loadDynamicLayout()` was missing these critical calls:

```kotlin
// MISSING (before fix):
keyboardContainer?.requestLayout()
mainKeyboardLayout?.requestLayout()
updateInputViewShown()
```

**Result**: Keyboard wouldn't auto-adjust until mode switch triggered Android's layout system.

**Fix Applied**: Added auto-adjust calls to both:
- `loadDynamicLayout()` - All modes (letters, symbols, dialer)
- `loadLanguageLayout()` - Letters mode only (already had it)

---

## 5. Key Components

### 5.1 LanguageLayoutAdapter

**Purpose**: Converts JSON templates + keymaps into LayoutModel

**Key Methods**:
- `buildLayoutFor()` - Main layout builder
- `loadTemplate()` - Loads JSON template
- `loadKeymap()` - Loads language keymap
- `applyKeymapToTemplate()` - Merges template with keymap
- `buildNumberRow()` - Creates number row with native numerals

**Data Models**:
```kotlin
data class KeyModel(
    val label: String,              // Display text
    val code: Int,                  // Character code
    val altLabel: String? = null,   // Secondary label
    val longPress: List<String>?    // Long-press variants
)

data class LayoutModel(
    val rows: List<List<KeyModel>>,
    val languageCode: String,
    val layoutType: String,
    val direction: String = "LTR",
    val numberRow: List<KeyModel> = emptyList()
)
```

### 5.2 SwipeKeyboardView

**Purpose**: Renders keyboard and handles touch events

**Key Features**:
- **Dynamic Layout Mode**: Programmatic key rendering
- **Swipe Typing**: Gesture-based input
- **Theme Integration**: Color and style application
- **RTL Support**: Right-to-left languages
- **Touch Target Expansion**: Larger hit areas
- **Long-Press Accents**: Accent character popups

**Key Methods**:
- `setKeyboardMode()` - Initiates layout build
- `setDynamicLayout()` - Converts LayoutModel to DynamicKeys
- `getKeyWidthFactor()` - Calculates relative key widths
- `drawThemedKey()` - Renders individual key
- `onTouchEvent()` - Handles user input

**Special Key Width Factors**:
| Key Type | Width Factor | Example |
|----------|--------------|---------|
| Space Bar | 5.5x | SPACE |
| Return/Enter | 2.0x | ⏎ |
| Shift/Delete | 2.0x | ⇧ ⌫ |
| Standard Keys | 1.0x | a, b, c |
| Mode Switches | 1.0x | ?123, ABC |
| Punctuation | 1.0x | , . |

### 5.3 AIKeyboardService

**Purpose**: Orchestrates entire keyboard system

**Responsibilities**:
- **Layout Loading**: Calls LanguageLayoutAdapter
- **Mode Management**: Tracks current mode (letters/symbols/etc)
- **Language Switching**: Handles multilingual support
- **Settings Sync**: Applies user preferences
- **Auto-Adjust**: Triggers layout recalculation
- **Theme Application**: Applies visual theme

**Key Methods**:
- `loadDynamicLayout()` - All modes except letters
- `loadLanguageLayout()` - Letters mode specifically
- `switchKeyboardMode()` - User-initiated mode change
- `applyLoadedSettings()` - Apply all user settings
- `updateInputViewShown()` - Recalculate IME insets

### 5.4 KeyboardHeightManager

**Purpose**: Calculates optimal keyboard height

**Features**:
- **Responsive Sizing**: Adapts to screen size
- **Orientation Handling**: Portrait vs landscape
- **Navigation Bar Detection**: Accounts for system UI
- **Inset Management**: Handles Android 11+ WindowInsets
- **Toolbar/Suggestion Sizing**: Consistent panel heights

---

## 6. Complete Processing Pipeline

### 6.1 Initialization Sequence

```
1. onCreate()
   ├─ Initialize LanguageLayoutAdapter
   ├─ Initialize KeyboardHeightManager
   ├─ Load user preferences
   └─ Create SwipeKeyboardView

2. onCreateInputView()
   ├─ Inflate keyboard layout XML
   ├─ Initialize theme manager
   ├─ Set up toolbar and suggestion bar
   └─ Calculate initial keyboard height

3. onStartInput()
   ├─ Detect input field type
   ├─ Load appropriate keyboard mode
   └─ Trigger layout build
```

### 6.2 Layout Build Pipeline

```
User Opens Keyboard
    ↓
AIKeyboardService.onStartInput()
    ↓
loadDynamicLayout(lang="en", mode=LETTERS)
    ↓
SwipeKeyboardView.setKeyboardMode(LETTERS)
    ↓
LanguageLayoutAdapter.buildLayoutFor("en", LETTERS, numberRow=true)
    ↓
[ASYNC OPERATION]
├─ Load qwerty_template.json
├─ Load en_keymap.json
├─ Apply character mappings
├─ Inject number row (if enabled)
└─ Return LayoutModel
    ↓
SwipeKeyboardView.setDynamicLayout(layoutModel)
    ↓
[MEASURE & LAYOUT]
├─ Calculate screen dimensions
├─ Calculate key height = screenHeight / numRows
├─ Calculate key width = screenWidth / totalWidthUnits
├─ Create DynamicKey objects with positions
└─ Store in dynamicKeys list
    ↓
[AUTO-ADJUST] 🎯
├─ keyboardContainer?.requestLayout()
├─ mainKeyboardLayout?.requestLayout()
└─ updateInputViewShown()
    ↓
SwipeKeyboardView.invalidate()
    ↓
SwipeKeyboardView.onDraw()
├─ Iterate dynamicKeys
├─ Draw each key with theme
└─ Render to screen
    ↓
Keyboard Visible to User ✅
```

### 6.3 Mode Switch Pipeline

```
User Taps "?123" Key
    ↓
onKey(primaryCode = -10)
    ↓
switchKeyboardMode(KeyboardMode.SYMBOLS)
    ↓
loadDynamicLayout(lang="en", mode=SYMBOLS)
    ↓
LanguageLayoutAdapter.buildLayoutFor("en", SYMBOLS, numberRow=false)
    ↓
[Load symbols_template.json]
└─ No keymap needed (symbols are universal)
    ↓
SwipeKeyboardView.setDynamicLayout(symbolsLayout)
    ↓
[Re-measure keys for symbols layout]
    ↓
[AUTO-ADJUST] 🎯
    ↓
Redraw with new layout ✅
```

### 6.4 Language Switch Pipeline

```
User Selects Hindi
    ↓
LanguageManager.setCurrentLanguage("hi")
    ↓
onLanguageChanged(old="en", new="hi")
    ↓
loadLanguageLayout("hi")
    ↓
LanguageLayoutAdapter.buildLayoutFor("hi", LETTERS, numberRow=true)
    ↓
[ASYNC OPERATION]
├─ Load inscript_template.json (Indic layout)
├─ Load hi_keymap.json
├─ Apply Hindi character mappings
├─ Inject Devanagari numerals (१ २ ३...)
└─ Set direction = "LTR"
    ↓
SwipeKeyboardView.setDynamicLayout(hindiLayout)
    ↓
[Rebuild all keys with Hindi characters]
    ↓
[AUTO-ADJUST] 🎯
    ↓
Hindi Keyboard Ready ✅
```

---

## 7. Key Insights

### 7.1 Design Advantages

✅ **Separation of Concerns**
- Templates define structure
- Keymaps define content
- View handles rendering

✅ **Language Scalability**
- Add new language = add one keymap JSON
- No code changes needed
- Firebase fallback for missing keymaps

✅ **Mode Flexibility**
- Same template can support multiple languages
- Easy to add new keyboard modes
- Consistent behavior across modes

✅ **Auto-Adjust Reliability**
- Centralized height calculation
- Automatic inset handling
- Consistent across all modes (after fix)

### 7.2 Critical Fix Summary

**Issue**: First keyboard open didn't auto-adjust height

**Root Cause**: `loadDynamicLayout()` missing layout recalculation calls

**Solution**: Added three critical calls:
```kotlin
keyboardContainer?.requestLayout()      // Force container remeasure
mainKeyboardLayout?.requestLayout()     // Force layout remeasure
updateInputViewShown()                  // Update IME window insets
```

**Result**: Auto-adjust now works consistently on:
- ✅ First keyboard open
- ✅ Mode switches (symbols/dialer)
- ✅ Language switches
- ✅ Number row toggle

### 7.3 Performance Optimizations

🚀 **Async Layout Building**
- Templates and keymaps loaded in coroutines
- UI remains responsive during layout build
- Main thread only handles rendering

🚀 **Layout Caching**
- Current layout stored in `currentLayoutModel`
- Avoids redundant JSON parsing
- Quick toggle for number row

🚀 **Minimal Invalidation**
- Only invalidates changed regions
- Efficient redraw on theme changes
- requestLayout() only when dimensions change

---

## 8. Future Enhancements

### Potential Improvements:
1. **Layout Preloading**: Cache frequently used layouts
2. **Animation Transitions**: Smooth mode switching
3. **Gesture Customization**: User-defined swipe patterns
4. **Cloud Sync**: Download new language packs on-demand
5. **A/B Layout Testing**: Firebase Remote Config for layouts

---

## 📊 Summary

The AI Keyboard's layout system is a **well-architected, scalable solution** that:
- Separates templates from content for maximum flexibility
- Supports 20+ languages with minimal code
- Auto-adjusts to any screen size and orientation
- Handles RTL languages seamlessly
- Provides smooth mode switching (letters/symbols/dialer)
- Uses modern Android best practices (coroutines, WindowInsets, etc.)

The recent auto-adjust fix ensures consistent behavior across all keyboard states, making the user experience smooth and professional.

---

**Generated**: $(date)
**Version**: 1.0
**Status**: Production-Ready ✅

