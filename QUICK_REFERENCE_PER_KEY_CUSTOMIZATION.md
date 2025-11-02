# Per-Key Customization Quick Reference

## 🚀 Quick Start

### JSON Example (Simplest)
```json
{
  "keys": {
    "perKeyCustomization": {
      "a": {
        "bg": "#FFFF6B9D",
        "text": "#FFFFFFFF"
      },
      "enter": {
        "bg": "#FF4CAF50"
      }
    }
  }
}
```

### Kotlin Example (Simplest)
```kotlin
val theme = currentTheme.copy(
    keys = currentTheme.keys.copy(
        perKeyCustomization = mapOf(
            "a" to KeyboardThemeV2.Keys.KeyCustomization(
                bg = Color.parseColor("#FF6B9D")
            )
        )
    )
)
themeManager.saveTheme(theme)
```

## 🔑 Key Identifiers

| Key | Identifier | Key | Identifier |
|-----|-----------|-----|-----------|
| A-Z | `"a"` - `"z"` | Space | `"space"` |
| 0-9 | `"0"` - `"9"` | Enter | `"enter"` |
| , . ? ! | `","` `"."` `"?"` `"!"` | Shift | `"shift"` |
| | | Backspace | `"backspace"` |
| | | Globe | `"globe"` |

## 🎨 Customization Options

### Font
```json
"font": {
  "family": "Roboto",
  "sizeSp": 20.0,
  "bold": true,
  "italic": false
}
```

### Colors
```json
"bg": "#FFFF6B9D",      // Background
"text": "#FFFFFFFF",    // Text
"pressed": "#FF505056"  // Pressed state
```

### Border
```json
"border": {
  "enabled": true,
  "color": "#FF636366",
  "widthDp": 2.0
}
```

### Other
```json
"radius": 10.0,         // Corner radius in DP
"shadow": {
  "enabled": true,
  "elevationDp": 4.0,
  "glow": false
}
```

## 📋 Common Patterns

### Highlight Enter & Space
```json
{
  "enter": { "bg": "#4CAF50" },
  "space": { "bg": "#2196F3" }
}
```

### Bold Numbers
```json
{
  "1": { "font": { "sizeSp": 20.0, "bold": true } },
  "2": { "font": { "sizeSp": 20.0, "bold": true } }
}
```

### High Contrast Vowels
```json
{
  "a": { "bg": "#FFFF00", "text": "#000000" },
  "e": { "bg": "#FFFF00", "text": "#000000" }
}
```

## 🛠️ API Methods

### ThemeManager
```kotlin
// Get custom drawable for key
themeManager.createKeyDrawable("a")

// Get custom text paint for key
themeManager.createKeyTextPaint("enter")

// Get custom text color for key
themeManager.getTextColor("space")
```

## 📂 Files

- **Full Guide**: `PER_KEY_CUSTOMIZATION_GUIDE.md`
- **Implementation**: `PER_KEY_CUSTOMIZATION_IMPLEMENTATION_SUMMARY.md`
- **Example Theme**: `example_per_key_theme.json`

## ⚠️ Important Notes

1. **Fallback**: If no customization specified, uses global settings
2. **Special Keys**: Accent-colored keys override customization
3. **Performance**: All customizations are cached
4. **Backwards Compatible**: Existing themes work unchanged

## 🎯 Color Format

Colors must be in 32-bit ARGB hex format:
- Format: `#AARRGGBB`
- Example: `#FFFF6B9D` (opaque pink)
- Example: `#80FF0000` (semi-transparent red)

## 📝 Example: Customize A, B, C Keys

### JSON
```json
{
  "keys": {
    "perKeyCustomization": {
      "a": {
        "font": { "sizeSp": 22.0, "bold": true },
        "bg": "#FFFF6B9D",
        "text": "#FFFFFFFF"
      },
      "b": {
        "bg": "#FF6B9DFF",
        "border": { "enabled": true, "color": "#FF0000FF", "widthDp": 2.0 }
      },
      "c": {
        "radius": 20.0,
        "shadow": { "enabled": true, "elevationDp": 6.0 }
      }
    }
  }
}
```

### Kotlin
```kotlin
mapOf(
    "a" to KeyCustomization(
        font = Font("Roboto", 22f, true, false),
        bg = Color.parseColor("#FF6B9D"),
        text = Color.WHITE
    ),
    "b" to KeyCustomization(
        bg = Color.parseColor("#6B9DFF"),
        border = Border(true, Color.BLUE, 2f)
    ),
    "c" to KeyCustomization(
        radius = 20f,
        shadow = Shadow(true, 6f, false)
    )
)
```

## ✅ Testing

1. Load theme with `example_per_key_theme.json`
2. Verify customized keys render correctly
3. Check fallback for non-customized keys
4. Test theme switching

## 🆘 Troubleshooting

### Key not customizing?
- Check key identifier spelling
- Verify color format (`#AARRGGBB`)
- Check if key is in special keys list

### Custom font not showing?
- Verify font family name
- Ensure font is installed on device
- Check font size is reasonable (12-30 SP)

### Performance issues?
- Limit customizations to essential keys
- Avoid excessive shadow/effects
- Use caching (automatic)

