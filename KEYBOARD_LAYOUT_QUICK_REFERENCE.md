# Keyboard Layout System - Quick Reference Guide

## 🗂️ File Locations

### Core Kotlin Files

| File | Path | Purpose |
|------|------|---------|
| **AIKeyboardService.kt** | `/android/app/src/main/kotlin/com/example/ai_keyboard/` | Main keyboard orchestrator |
| **SwipeKeyboardView.kt** | `/android/app/src/main/kotlin/com/example/ai_keyboard/` | Keyboard view renderer |
| **LanguageLayoutAdapter.kt** | `/android/app/src/main/kotlin/com/example/ai_keyboard/` | JSON template processor |
| **KeyboardHeightManager.kt** | `/android/app/src/main/kotlin/com/example/ai_keyboard/` | Height calculator |
| **ThemeManager.kt** | `/android/app/src/main/kotlin/com/example/ai_keyboard/` | Theme and color management |
| **LanguageManager.kt** | `/android/app/src/main/kotlin/com/example/ai_keyboard/` | Multilingual support |

### JSON Template Files

| File | Path | Use Case |
|------|------|----------|
| **qwerty_template.json** | `/android/app/src/main/assets/layout_templates/` | Latin languages (EN, FR, ES, DE) |
| **inscript_template.json** | `/android/app/src/main/assets/layout_templates/` | Indic languages (HI, TE, TA, ML) |
| **arabic_template.json** | `/android/app/src/main/assets/layout_templates/` | Arabic script (AR, UR, FA) |
| **symbols_template.json** | `/android/app/src/main/assets/layout_templates/` | Symbol mode (all languages) |
| **extended_symbols_template.json** | `/android/app/src/main/assets/layout_templates/` | Extended symbols mode |
| **dialer_template.json** | `/android/app/src/main/assets/layout_templates/` | Number pad mode |

### Language Keymap Files

| File Pattern | Path | Content |
|--------------|------|---------|
| **{lang}_keymap.json** | `/android/app/src/main/assets/keymaps/` | Character mappings per language |

**Example**: `en_keymap.json`, `hi_keymap.json`, `es_keymap.json`

---

## 🔍 Key Methods & Line Numbers

### AIKeyboardService.kt

| Method | Line | Purpose |
|--------|------|---------|
| `loadDynamicLayout()` | 4228 | Load layout for any mode (symbols/dialer/letters) |
| `loadLanguageLayout()` | 4288 | Load letters mode layout (with auto-adjust) |
| `switchKeyboardMode()` | 4185 | Handle user mode switching |
| `onKey()` | 3127 | Handle all key press events |
| `handleEnterKey()` | 6680 | Context-aware RETURN key handler |
| `applyLoadedSettings()` | 600+ | Apply all user settings |

### SwipeKeyboardView.kt

| Method | Line | Purpose |
|--------|------|---------|
| `setKeyboardMode()` | 1602 | Initiate layout build for mode |
| `setDynamicLayout()` | 1355 | Convert LayoutModel to DynamicKeys |
| `getKeyWidthFactor()` | 1640 | Calculate relative key widths |
| `onDraw()` | ~1800 | Render keys on canvas |
| `onTouchEvent()` | ~900 | Handle touch and swipe events |
| `toggleNumberRow()` | 1463 | Show/hide number row |

### LanguageLayoutAdapter.kt

| Method | Line | Purpose |
|--------|------|---------|
| `buildLayoutFor()` | 95 | Main layout builder |
| `loadTemplate()` | 150+ | Load JSON template file |
| `loadKeymap()` | 200+ | Load language keymap |
| `applyKeymapToTemplate()` | 250+ | Merge template + keymap |
| `buildNumberRow()` | 71 | Create number row with native numerals |

### KeyboardHeightManager.kt

| Method | Line | Purpose |
|--------|------|---------|
| `calculateKeyboardHeight()` | 46 | Calculate total keyboard height |
| `getNavigationBarHeight()` | 83 | Detect navigation bar size |
| `applySystemInsets()` | 128 | Handle WindowInsets |

---

## 🔧 Key Data Structures

### KeyModel
```kotlin
// LanguageLayoutAdapter.kt - Line 50
data class KeyModel(
    val label: String,              // "a", "⇧", "SPACE"
    val code: Int,                  // Character code or special code
    val altLabel: String? = null,   // Secondary label
    val longPress: List<String>?    // ["à", "á", "â", "ä"]
)
```

### LayoutModel
```kotlin
// LanguageLayoutAdapter.kt - Line 60
data class LayoutModel(
    val rows: List<List<KeyModel>>,
    val languageCode: String,       // "en", "hi", "es"
    val layoutType: String,         // "QWERTY", "INSCRIPT"
    val direction: String = "LTR",  // "LTR" or "RTL"
    val numberRow: List<KeyModel> = emptyList()
)
```

### DynamicKey
```kotlin
// SwipeKeyboardView.kt - Line 159
data class DynamicKey(
    val x: Int,                     // X position in pixels
    val y: Int,                     // Y position in pixels
    val width: Int,                 // Key width in pixels
    val height: Int,                // Key height in pixels
    val label: String,              // Display text
    val code: Int,                  // Character/action code
    val longPressOptions: List<String>? = null
)
```

---

## 🎯 Special Key Codes

| Code | Constant | Purpose | Visual |
|------|----------|---------|--------|
| `-1` | `Keyboard.KEYCODE_SHIFT` | Shift/Caps | ⇧ |
| `-2` | `Keyboard.KEYCODE_MODE_CHANGE` | Mode switch (legacy) | ?123 |
| `-3` | (custom) | Language/Globe | 🌐 |
| `-4` | (custom) | Return/Enter | ⏎ |
| `-5` | `Keyboard.KEYCODE_DELETE` | Backspace | ⌫ |
| `-10` | `KEYCODE_SYMBOLS` | Switch to symbols | ?123 |
| `-11` | `KEYCODE_LETTERS` | Switch to letters | ABC |
| `-20` | (custom) | Extended symbols | =< |
| `-21` | (custom) | Dialer mode | 1234 |
| `32` | (standard) | Space bar | SPACE |
| `10` | (standard) | Newline | \n |

---

## 📐 Layout Constants

### Height Constants (KeyboardHeightManager.kt)

| Constant | Value | Notes |
|----------|-------|-------|
| `MIN_KEYBOARD_HEIGHT_DP` | 260dp | Minimum keyboard height |
| `MAX_KEYBOARD_HEIGHT_DP` | 310dp | Maximum keyboard height |
| `DEFAULT_KEYBOARD_HEIGHT_DP` | 285dp | Default/fallback height |
| `KEYBOARD_HEIGHT_PERCENTAGE` | 0.24f | 24% of screen (portrait) |
| `TOOLBAR_HEIGHT_DP` | 72dp | AI toolbar height |
| `SUGGESTION_BAR_HEIGHT_DP` | 44dp | Suggestion bar height |

### Width Factors (SwipeKeyboardView.kt - Line 1640)

| Key Type | Factor | Example Keys |
|----------|--------|--------------|
| Space Bar | 5.5x | SPACE |
| Return/Enter | 2.0x | ⏎, RETURN |
| Shift/Delete | 2.0x | ⇧, ⌫ |
| Standard Keys | 1.0x | a, b, c, 1, 2, 3 |
| Mode Switches | 1.0x | ?123, ABC, =< |
| Punctuation | 1.0x | , . ? ! |

---

## 🔄 Auto-Adjust Sequence

```kotlin
// Required calls for auto-adjust (AIKeyboardService.kt)

// 1. Force container remeasure
keyboardContainer?.requestLayout()

// 2. Force layout remeasure  
mainKeyboardLayout?.requestLayout()

// 3. Update IME window insets
updateInputViewShown()
```

**Called in:**
- ✅ `loadDynamicLayout()` - Line 4256-4260
- ✅ `loadLanguageLayout()` - Line 4303-4308
- ✅ Fallback XML layout - Line 4278-4280

---

## 🌍 Language Support

### Template Mapping

| Language Code | Template | Script |
|---------------|----------|--------|
| `en`, `fr`, `es`, `de`, `pt`, `it` | qwerty_template.json | Latin |
| `hi`, `te`, `ta`, `ml`, `gu`, `bn`, `kn`, `or`, `pa` | inscript_template.json | Indic |
| `ar`, `ur`, `fa` | arabic_template.json | Arabic |

### Native Numeral Support

| Language | Numerals |
|----------|----------|
| English | 1 2 3 4 5 6 7 8 9 0 |
| Hindi | १ २ ३ ४ ५ ६ ७ ८ ९ ० |
| Tamil | ௧ ௨ ௩ ௪ ௫ ௬ ௭ ௮ ௯ ௦ |
| Telugu | ౧ ౨ ౩ ౪ ౫ ౬ ౭ ౮ ౯ ౦ |

---

## 🔨 Common Modifications

### Add a New Language

1. **Create keymap file**: `/assets/keymaps/{lang}_keymap.json`
2. **Define mappings**: base, shift, alt, longPress
3. **Add to available languages**: `assets/dictionaries/available_languages.json`
4. **No code changes needed** ✅

### Add a New Keyboard Mode

1. **Add enum**: `LanguageLayoutAdapter.KeyboardMode`
2. **Create template**: `/assets/layout_templates/{mode}_template.json`
3. **Add switch case**: `AIKeyboardService.switchKeyboardMode()`
4. **Define special key code**: For mode switch button

### Modify Key Widths

1. **Edit method**: `SwipeKeyboardView.getKeyWidthFactor()` (Line 1640)
2. **Update logic**: Add new key label pattern
3. **Adjust factor**: Change multiplier (1.0x to 10.0x)

### Change Keyboard Height

1. **Edit constants**: `KeyboardHeightManager.kt` (Line 27-30)
2. **Adjust percentage**: `KEYBOARD_HEIGHT_PERCENTAGE`
3. **Adjust min/max**: `MIN_KEYBOARD_HEIGHT_DP`, `MAX_KEYBOARD_HEIGHT_DP`

---

## 🐛 Debug Logs

### Enable Verbose Logging

Look for these log tags:
- `AIKeyboardService` - General keyboard events
- `SwipeKeyboardView` - Layout and rendering
- `LanguageLayoutAdapter` - Template loading
- `KeyboardHeightManager` - Height calculations
- `KeyAudit` - Key press diagnostics

### Key Log Messages

```kotlin
// Layout build
Log.d(TAG, "🔧 Building layout for: $languageCode, mode: $mode")
Log.d(TAG, "✅ Dynamic layout set: ${dynamicKeys.size} keys")

// Auto-adjust
Log.d(TAG, "🎯 AUTO-ADJUST: Force recalculation")

// Key press
Log.d("KeyAudit", "🔍 Key pressed: $keyLabel | Code: $primaryCode")

// Mode switch
Log.d(TAG, "✅ setKeyboardMode: $mode for language: $currentLangCode")
```

---

## ⚡ Performance Tips

1. **Layout Caching**: Current layout stored in `currentLayoutModel`
2. **Async Loading**: Templates loaded in coroutines
3. **Minimal Invalidation**: Only redraw changed regions
4. **Width Factor Optimization**: Pre-calculate widths once per layout

---

## 📚 Related Documentation

- **Full Analysis**: `KEYBOARD_LAYOUT_SYSTEM_ANALYSIS.md`
- **Flow Diagrams**: `KEYBOARD_LAYOUT_FLOW_DIAGRAM.md`
- **Theme System**: `UNIFIED_THEMING_ARCHITECTURE.md`
- **Features**: `AI_KEYBOARD_FEATURES_DOCUMENTATION.md`

---

**Last Updated**: 2025
**Version**: 1.0
**Status**: Production ✅

