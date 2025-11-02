# Per-Key Customization Guide

## Overview

The AI Keyboard now supports **per-key customization**, allowing you to set custom fonts and button styles for individual keys. This feature enables you to create highly personalized keyboard themes where each key can have its own unique appearance.

## Features

### Custom Fonts for Individual Keys
- **Font Family**: Set a different font family for each key (e.g., Roboto, Arial, Comic Sans)
- **Font Size**: Customize the size of text for each key independently
- **Font Style**: Make individual keys bold or italic

### Custom Button Styles for Individual Keys
- **Background Color**: Set unique background colors for each key
- **Text Color**: Customize text color per key
- **Pressed Color**: Define what color the key shows when pressed
- **Border**: Customize border color, width, and enable/disable per key
- **Corner Radius**: Set different corner radius values for each key
- **Shadow**: Configure shadow elevation and glow effects per key

## Implementation Details

### 1. Theme Data Structure

The per-key customization is stored in the `perKeyCustomization` map within the theme JSON:

```json
{
  "keys": {
    "preset": "bordered",
    "bg": "#3A3A3F",
    "text": "#FFFFFF",
    "pressed": "#505056",
    "rippleAlpha": 0.12,
    "border": {
      "enabled": true,
      "color": "#636366",
      "widthDp": 1.0
    },
    "radius": 12.0,
    "shadow": {
      "enabled": true,
      "elevationDp": 2.0,
      "glow": false
    },
    "font": {
      "family": "Roboto",
      "sizeSp": 18.0,
      "bold": false,
      "italic": false
    },
    "perKeyCustomization": {
      "a": {
        "font": {
          "family": "Roboto",
          "sizeSp": 20.0,
          "bold": true,
          "italic": false
        },
        "bg": "#FF6B9D",
        "text": "#FFFFFF"
      },
      "enter": {
        "bg": "#00FF00",
        "text": "#000000",
        "radius": 8.0
      },
      "space": {
        "font": {
          "family": "Roboto",
          "sizeSp": 16.0,
          "bold": false,
          "italic": true
        },
        "border": {
          "enabled": true,
          "color": "#FFD700",
          "widthDp": 2.0
        }
      }
    }
  }
}
```

### 2. Key Identifiers

Each key is identified by a standardized identifier:

#### Special Keys
- `"space"` - Spacebar
- `"enter"` - Enter/Return key
- `"shift"` - Shift key
- `"backspace"` - Backspace/Delete key
- `"globe"` - Language switcher
- `"emoji"` - Emoji panel key
- `"mic"` - Voice input key
- `"symbols"` - Symbol mode switcher

#### Letter/Number Keys
- Single lowercase letters: `"a"`, `"b"`, `"c"`, etc.
- Single numbers: `"1"`, `"2"`, `"3"`, etc.
- Special characters use their character: `","`, `"."`, `"?"`, etc.

### 3. Using Per-Key Customization in Code

#### Creating a Theme with Per-Key Customization (Kotlin)

```kotlin
import android.graphics.Color
import com.example.ai_keyboard.themes.KeyboardThemeV2

// Create a theme with per-key customization
val theme = KeyboardThemeV2(
    id = "custom_theme",
    name = "Custom Theme with Per-Key Styles",
    mode = "unified",
    background = KeyboardThemeV2.Background(
        type = "solid",
        color = Color.parseColor("#1B1B1F"),
        imagePath = null,
        imageOpacity = 0.85f,
        gradient = null,
        overlayEffects = emptyList(),
        adaptive = null
    ),
    keys = KeyboardThemeV2.Keys(
        preset = "bordered",
        bg = Color.parseColor("#3A3A3F"),
        text = Color.parseColor("#FFFFFF"),
        pressed = Color.parseColor("#505056"),
        rippleAlpha = 0.12f,
        border = KeyboardThemeV2.Keys.Border(
            enabled = true,
            color = Color.parseColor("#636366"),
            widthDp = 1.0f
        ),
        radius = 12.0f,
        shadow = KeyboardThemeV2.Keys.Shadow(
            enabled = true,
            elevationDp = 2.0f,
            glow = false
        ),
        font = KeyboardThemeV2.Keys.Font(
            family = "Roboto",
            sizeSp = 18.0f,
            bold = false,
            italic = false
        ),
        perKeyCustomization = mapOf(
            // Customize the 'A' key
            "a" to KeyboardThemeV2.Keys.KeyCustomization(
                font = KeyboardThemeV2.Keys.Font(
                    family = "Roboto",
                    sizeSp = 22.0f,
                    bold = true,
                    italic = false
                ),
                bg = Color.parseColor("#FF6B9D"),
                text = Color.parseColor("#FFFFFF"),
                radius = 10.0f
            ),
            // Customize the Enter key
            "enter" to KeyboardThemeV2.Keys.KeyCustomization(
                bg = Color.parseColor("#00FF00"),
                text = Color.parseColor("#000000"),
                border = KeyboardThemeV2.Keys.Border(
                    enabled = true,
                    color = Color.parseColor("#00AA00"),
                    widthDp = 2.0f
                )
            ),
            // Customize the Spacebar
            "space" to KeyboardThemeV2.Keys.KeyCustomization(
                font = KeyboardThemeV2.Keys.Font(
                    family = "Roboto",
                    sizeSp = 16.0f,
                    bold = false,
                    italic = true
                ),
                bg = Color.parseColor("#4A4A4F"),
                border = KeyboardThemeV2.Keys.Border(
                    enabled = true,
                    color = Color.parseColor("#FFD700"),
                    widthDp = 3.0f
                )
            )
        )
    ),
    specialKeys = KeyboardThemeV2.SpecialKeys(
        accent = Color.parseColor("#FF9F1A"),
        useAccentForEnter = true,
        applyTo = listOf("enter", "globe", "emoji", "mic"),
        spaceLabelColor = Color.parseColor("#FFFFFF")
    ),
    effects = KeyboardThemeV2.Effects(
        pressAnimation = "ripple",
        globalEffects = emptyList()
    ),
    sounds = KeyboardThemeV2.Sounds(
        pack = "soft",
        customUris = emptyMap(),
        volume = 0.6f
    ),
    stickers = KeyboardThemeV2.Stickers(
        enabled = false,
        pack = "",
        position = "behind",
        opacity = 0.9f,
        animated = false
    ),
    advanced = KeyboardThemeV2.Advanced(
        livePreview = true,
        galleryEnabled = true,
        shareEnabled = true,
        dynamicTheme = "none",
        seasonalPack = "none",
        materialYouExtract = false
    )
)

// Save the theme
themeManager.saveTheme(theme)
```

#### Accessing Per-Key Customization (Kotlin)

```kotlin
// Get custom drawable for a specific key
val keyDrawable = themeManager.createKeyDrawable("a")

// Get custom text paint for a specific key
val keyTextPaint = themeManager.createKeyTextPaint("enter")

// Get custom text color for a specific key
val textColor = themeManager.getTextColor("space")
```

### 4. Using Per-Key Customization from Flutter/Dart

```dart
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

Future<void> saveThemeWithPerKeyCustomization() async {
  final prefs = await SharedPreferences.getInstance();
  
  final theme = {
    'id': 'custom_theme',
    'name': 'Custom Per-Key Theme',
    'mode': 'unified',
    'background': {
      'type': 'solid',
      'color': '#FF1B1B1F',
      'imagePath': null,
      'imageOpacity': 0.85,
      'gradient': null,
      'overlayEffects': [],
      'adaptive': null,
    },
    'keys': {
      'preset': 'bordered',
      'bg': '#FF3A3A3F',
      'text': '#FFFFFFFF',
      'pressed': '#FF505056',
      'rippleAlpha': 0.12,
      'border': {
        'enabled': true,
        'color': '#FF636366',
        'widthDp': 1.0,
      },
      'radius': 12.0,
      'shadow': {
        'enabled': true,
        'elevationDp': 2.0,
        'glow': false,
      },
      'font': {
        'family': 'Roboto',
        'sizeSp': 18.0,
        'bold': false,
        'italic': false,
      },
      'perKeyCustomization': {
        'a': {
          'font': {
            'family': 'Roboto',
            'sizeSp': 22.0,
            'bold': true,
            'italic': false,
          },
          'bg': '#FFFF6B9D',
          'text': '#FFFFFFFF',
          'radius': 10.0,
        },
        'enter': {
          'bg': '#FF00FF00',
          'text': '#FF000000',
          'border': {
            'enabled': true,
            'color': '#FF00AA00',
            'widthDp': 2.0,
          },
        },
        'space': {
          'font': {
            'family': 'Roboto',
            'sizeSp': 16.0,
            'bold': false,
            'italic': true,
          },
          'bg': '#FF4A4A4F',
          'border': {
            'enabled': true,
            'color': '#FFFFD700',
            'widthDp': 3.0,
          },
        },
      },
    },
    'specialKeys': {
      'accent': '#FFFF9F1A',
      'useAccentForEnter': true,
      'applyTo': ['enter', 'globe', 'emoji', 'mic'],
      'spaceLabelColor': '#FFFFFFFF',
    },
    'effects': {
      'pressAnimation': 'ripple',
      'globalEffects': [],
    },
    'sounds': {
      'pack': 'soft',
      'customUris': {},
      'volume': 0.6,
    },
    'stickers': {
      'enabled': false,
      'pack': '',
      'position': 'behind',
      'opacity': 0.9,
      'animated': false,
    },
    'advanced': {
      'livePreview': true,
      'galleryEnabled': true,
      'shareEnabled': true,
      'dynamicTheme': 'none',
      'seasonalPack': 'none',
      'materialYouExtract': false,
    },
  };
  
  await prefs.setString('theme.v2.json', jsonEncode(theme));
}
```

## Example Use Cases

### 1. Highlight Important Keys
Make the Enter and Space keys stand out with custom colors:

```json
{
  "perKeyCustomization": {
    "enter": {
      "bg": "#4CAF50",
      "text": "#FFFFFF",
      "radius": 8.0
    },
    "space": {
      "bg": "#2196F3",
      "text": "#FFFFFF"
    }
  }
}
```

### 2. Custom Font for Number Row
Use a different font for number keys:

```json
{
  "perKeyCustomization": {
    "1": { "font": { "family": "Roboto", "sizeSp": 20.0, "bold": true } },
    "2": { "font": { "family": "Roboto", "sizeSp": 20.0, "bold": true } },
    "3": { "font": { "family": "Roboto", "sizeSp": 20.0, "bold": true } },
    "4": { "font": { "family": "Roboto", "sizeSp": 20.0, "bold": true } },
    "5": { "font": { "family": "Roboto", "sizeSp": 20.0, "bold": true } }
  }
}
```

### 3. Branded Keys
Create branded keys with company colors:

```json
{
  "perKeyCustomization": {
    "a": {
      "bg": "#FF0000",
      "text": "#FFFFFF",
      "font": { "family": "Roboto", "sizeSp": 18.0, "bold": true }
    },
    "b": {
      "bg": "#00FF00",
      "text": "#000000",
      "font": { "family": "Roboto", "sizeSp": 18.0, "bold": true }
    }
  }
}
```

### 4. Accessibility - High Contrast Keys
Make certain keys more visible for accessibility:

```json
{
  "perKeyCustomization": {
    "a": { "bg": "#FFFF00", "text": "#000000" },
    "e": { "bg": "#FFFF00", "text": "#000000" },
    "i": { "bg": "#FFFF00", "text": "#000000" },
    "o": { "bg": "#FFFF00", "text": "#000000" },
    "u": { "bg": "#FFFF00", "text": "#000000" }
  }
}
```

## Architecture

### Component Flow

```
KeyboardThemeV2.Keys
    ↓ (contains)
perKeyCustomization: Map<String, KeyCustomization>
    ↓ (used by)
ThemeManager
    ↓ (provides)
createKeyDrawable(keyIdentifier)
createKeyTextPaint(keyIdentifier)
getTextColor(keyIdentifier)
    ↓ (consumed by)
UnifiedKeyboardView.KeyboardGridView
    ↓ (renders)
Individual Keys with Custom Styles
```

### Files Modified

1. **ThemeModels.kt**
   - Added `KeyCustomization` data class
   - Added `perKeyCustomization` map to `Keys` data class
   - Added parsing and serialization support

2. **ThemeManager.kt**
   - Added `createKeyDrawable(keyIdentifier: String)`
   - Added `createKeyTextPaint(keyIdentifier: String)`
   - Added `getTextColor(keyIdentifier: String)`
   - Added `buildKeyDrawable(customization: KeyCustomization)`

3. **UnifiedKeyboardView.kt**
   - Added `getKeyIdentifier(key: DynamicKey)`
   - Modified `drawKey()` to use per-key drawables
   - Modified `drawKeyText()` to use per-key fonts and colors

## Best Practices

1. **Performance**: Per-key customizations are cached by ThemeManager, so there's minimal performance impact

2. **Fallback**: If a key doesn't have customization, it automatically falls back to the global key settings

3. **Consistency**: Keep most keys using the global style and only customize keys that need special attention

4. **Testing**: Test themes on different screen sizes to ensure custom fonts remain readable

5. **Accessibility**: Maintain sufficient contrast ratios when customizing colors (aim for WCAG AA: 4.5:1 minimum)

## Limitations

1. **Icon Keys**: Keys that use icons (shift, backspace, enter with icons) won't show text color customization

2. **Special Key Override**: If a key is in the `specialKeys.applyTo` list, the accent color will override custom colors

3. **Space Key Language Label**: The space key's language label uses a separate paint and may not fully respect font customization

## Migration Guide

### Existing Themes

Existing themes will continue to work without modification. The `perKeyCustomization` map is optional and defaults to an empty map.

### Adding Per-Key Customization to Existing Theme

1. Load your existing theme
2. Add the `perKeyCustomization` map to the `keys` object
3. Define customizations for specific keys
4. Save the updated theme

Example:
```kotlin
val currentTheme = themeManager.getCurrentTheme()
val updatedTheme = currentTheme.copy(
    keys = currentTheme.keys.copy(
        perKeyCustomization = mapOf(
            "a" to KeyboardThemeV2.Keys.KeyCustomization(
                bg = Color.parseColor("#FF6B9D")
            )
        )
    )
)
themeManager.saveTheme(updatedTheme)
```

## Future Enhancements

Potential future enhancements to per-key customization:

1. **Per-Key Animations**: Custom press animations for specific keys
2. **Per-Key Sounds**: Different sound effects for different keys
3. **Per-Key Icons**: Custom icons for letter keys
4. **Gradient Backgrounds**: Per-key gradient support
5. **Per-Key Effects**: Custom glow, shadow, or particle effects

## Support

For questions or issues with per-key customization:

1. Check this guide for examples
2. Verify JSON syntax in theme files
3. Ensure key identifiers match the documented format
4. Check ThemeManager logs for debugging information

## Changelog

### Version 1.0 (Initial Release)
- Added per-key customization support
- Implemented font customization per key
- Implemented button style customization per key
- Added caching for per-key drawables and paints
- Full backwards compatibility with existing themes

