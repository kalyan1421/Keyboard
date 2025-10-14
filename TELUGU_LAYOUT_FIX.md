# Telugu Layout Fix - INSCRIPT Template Issue

## Problem

When adding Telugu language, the keyboard was displaying **Hindi characters** instead of **Telugu characters**, even though the Telugu keymap file (`te.json`) existed and was being loaded correctly.

### Root Cause

The `inscript_template.json` file had **hardcoded Hindi characters** instead of Latin placeholder keys:

**BEFORE (INCORRECT):**
```json
{
  "rows": [
    ["ौ", "ै", "ा", "ी", "ू", "ब", "ह", "ग", "द", "ज"],
    ["ो", "े", "्", "ि", "ु", "प", "र", "क", "त", "च"],
    ...
  ]
}
```

### How the Mapping System Works

1. **Template** defines the layout structure with placeholder keys
2. **Keymap** (`hi.json`, `te.json`, etc.) maps placeholders → language-specific characters
3. **Code** looks up each template key in the keymap:
   ```kotlin
   val mappedChar = baseMap.optString(baseKey, baseKey)
   ```

### The Bug

- Template had: `["ौ", "ै", "ा", ...]` (Hindi characters)
- Telugu keymap had: `{"q": "ౌ", "w": "ై", ...}` (Latin → Telugu)
- Code looked up "ौ" in Telugu keymap → **NOT FOUND** → used "ौ" as-is
- Result: Telugu keyboard showed Hindi characters!

## Solution

### 1. Fixed INSCRIPT Template

Changed from hardcoded Hindi to Latin placeholders:

**AFTER (CORRECT):**
```json
{
  "name": "INSCRIPT",
  "description": "Standard INSCRIPT layout for Indic languages",
  "rows": [
    ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
    ["a", "s", "d", "f", "g", "h", "j", "k", "l", ";"],
    ["z", "x", "c", "v", "b", "n", "m", ",", ".", "/"],
    ["SHIFT", "[", "]", "\\", "1", "2", "3", "4", "DELETE"],
    ["?123", ",", "GLOBE", "SPACE", ".", "RETURN"]
  ]
}
```

### 2. Updated Hindi Keymap

Changed from self-referential mapping to proper Latin → Hindi mapping:

**BEFORE:**
```json
"base": {
  "ौ": "ौ", "ै": "ै", "ा": "ा", ...
}
```

**AFTER:**
```json
"base": {
  "q": "ौ", "w": "ै", "e": "ा", "r": "ी", "t": "ू",
  "y": "ब", "u": "ह", "i": "ग", "o": "द", "p": "ज",
  ...
}
```

### 3. Completed Telugu & Tamil Keymaps

Added missing keys to ensure all template positions are mapped:

```json
{
  "base": {
    ...
    ";": "చ",  // Added semicolon key
    ",": "య", ".": "శ", "/": "ష",  // Added punctuation keys
    "[": "క్ష", "]": "త్ర", "\\": "జ్ఞ",  // Added bracket keys
    "1": "అ", "2": "ఆ", "3": "ఇ", "4": "ఈ"  // Added number keys
  }
}
```

## Files Modified

### INSCRIPT Layout (Indic Languages)
1. `/android/app/src/main/assets/layout_templates/inscript_template.json`
   - Changed from Hindi characters to Latin placeholders

2. `/android/app/src/main/assets/keymaps/hi.json`
   - Updated `base` and `long_press` mappings to use Latin keys

3. `/android/app/src/main/assets/keymaps/te.json`
   - Added missing keys: `;`, `,`, `.`, `/`, `[`, `]`, `\\`, `1-4`
   - Updated long-press mappings

4. `/android/app/src/main/assets/keymaps/ta.json`
   - Added missing keys for consistency

### Arabic Layout (RTL Languages)
5. `/android/app/src/main/assets/layout_templates/arabic_template.json`
   - Changed from Arabic characters to Latin placeholders
   - Simplified to standard 4-row layout

6. `/android/app/src/main/assets/keymaps/ar.json`
   - Updated to map from Latin keys to Arabic characters
   - Added missing keys: `;`, `,`, `.`, `/`

7. `/android/app/src/main/assets/keymaps/ur.json` (**NEW FILE**)
   - Created Urdu keymap with proper Urdu characters
   - Maps Latin keys to Urdu script
   - Includes Urdu-specific characters (ٹ, پ, ڈ, چ, ں, etc.)

### Code Changes
8. `/android/app/src/main/kotlin/com/example/ai_keyboard/LanguageLayoutAdapter.kt`
   - Updated `createFallbackKeymap()` to use correct template based on language family
   - Now Arabic/Urdu/Farsi default to `arabic_template.json` instead of `qwerty_template.json`

## Testing

After rebuilding the app:

### LTR Languages (QWERTY)
1. **English** should display: q, w, e, r, t, y, u, i, o, p...

### LTR Languages (INSCRIPT)
2. **Hindi keyboard** should display: ौ, ै, ा, ी, ू, ब, ह, ग, द, ज...
3. **Telugu keyboard** should display: ౌ, ై, ా, ీ, ూ, బ, హ, గ, ద, జ...
4. **Tamil keyboard** should display: ௌ, ை, ா, ீ, ூ, ப, ஹ, க, த, ஜ...

### RTL Languages (ARABIC)
5. **Arabic keyboard** should display: ض, ص, ث, ق, ف, غ, ع, ه, خ, ح...
6. **Urdu keyboard** should display: ط, ص, ھ, د, ٹ, پ, ت, ب, ج, ح...

## Key Learnings

1. **Templates should use neutral placeholders** (like Latin letters), not language-specific characters
2. **Keymaps translate placeholders** to actual characters for each language
3. **All template positions must be mapped** in each language's keymap file
4. The INSCRIPT layout is shared across multiple Indic languages (Hindi, Telugu, Tamil, etc.)

## Impact

This fix ensures that **all languages using template-based layouts** will now display correctly:

### INSCRIPT Layout (Indic Languages)
- Hindi (हिन्दी)
- Telugu (తెలుగు)
- Tamil (தமிழ்)
- Malayalam, Gujarati, Bengali, Kannada, Odia, Punjabi (when added)

### Arabic Layout (RTL Languages)
- Arabic (العربية)
- Urdu (اردو)
- Farsi/Persian (when added)
- Pashto (when added)

### Additional Fixes
- Long-press popup positioning fix (shows above pressed key)
- Fallback keymap now uses correct template based on language family

