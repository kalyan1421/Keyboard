# Per-Key Customization UI - Quick Start Guide

## 🎯 What You Get

A complete UI screen where users can:
- **Select keys** from a visual keyboard
- **Customize fonts** (family, size, bold, italic)
- **Change colors** (background, text, pressed)
- **Adjust shapes** (corner radius, borders)
- **Apply instantly** to the system keyboard

## 🚀 Quick Implementation (3 Steps)

### Step 1: Add to Theme Model (5 minutes)

**File:** `lib/theme/theme_v2.dart`

```dart
// Add to ThemeKeys class:
final Map<String, KeyCustomization> perKeyCustomization;

// Add to toJson():
if (perKeyCustomization.isNotEmpty) {
  map['perKeyCustomization'] = perKeyCustomization.map(
    (key, value) => MapEntry(key, value.toJson()),
  );
}
```

### Step 2: Create UI Screen (Copy provided code)

**File:** `lib/screens/main screens/per_key_customization_screen.dart`

✅ Complete code provided in `HOW_TO_ADD_PER_KEY_CUSTOMIZATION_UI.md`

### Step 3: Add Button to Theme Editor

**File:** `lib/theme/theme_editor_v2.dart`

```dart
// Add to AppBar actions:
IconButton(
  icon: const Icon(Icons.keyboard),
  onPressed: () async {
    final updatedTheme = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => PerKeyCustomizationScreen(
          theme: _currentTheme,
          onThemeUpdated: (theme) => _updateTheme(theme),
        ),
      ),
    );
  },
  tooltip: 'Customize Keys',
),
```

## 📱 User Flow

```
1. User opens app
   ↓
2. Goes to Theme Editor
   ↓
3. Clicks "Keyboard" icon
   ↓
4. Selects a key from visual keyboard
   ↓
5. Changes font/color/style
   ↓
6. Clicks "Save"
   ↓
7. Theme saved to SharedPreferences
   ↓
8. Android keyboard automatically updates!
```

## 🔄 How It Connects to System Keyboard

```
┌─────────────────────┐
│   Flutter App UI    │
│  Per-Key Screen     │
└──────────┬──────────┘
           │ Save theme
           ↓
┌─────────────────────┐
│  SharedPreferences  │
│ "theme.v2.json"     │
└──────────┬──────────┘
           │ Auto-detects change
           ↓
┌─────────────────────┐
│  Android Keyboard   │
│  Service Applies    │
└─────────────────────┘
```

## 🎨 Example: Customize "A" Key

```dart
// User selects "A" key and changes:
{
  "perKeyCustomization": {
    "a": {
      "font": {
        "family": "Roboto",
        "sizeSp": 24.0,
        "bold": true
      },
      "bg": "#FFFF6B9D",  // Pink background
      "text": "#FFFFFFFF"  // White text
    }
  }
}
```

## ✅ What Happens

1. **In App**: User sees changes immediately in preview
2. **Save**: Theme JSON stored in SharedPreferences
3. **Keyboard Service**: Reads updated theme automatically
4. **Keyboard UI**: "A" key now shows:
   - Pink background
   - White text
   - Bold font at 24sp
   - Roboto font family

## 🎯 Key Identifiers

| Key Type | Identifier |
|----------|-----------|
| Letters | `"a"` to `"z"` (lowercase) |
| Numbers | `"1"` to `"9"`, `"0"` |
| Space | `"space"` |
| Enter | `"enter"` |
| Shift | `"shift"` |
| Backspace | `"backspace"` |
| Symbols | `"symbols"` |

## 🛠️ Already Implemented on Android Side

✅ ThemeModels.kt - Supports perKeyCustomization map
✅ ThemeManager.kt - Methods to get per-key fonts/drawables
✅ UnifiedKeyboardView.kt - Renders with custom styles

**You just need to add the Flutter UI!**

## 📋 Files to Update

1. ✅ `lib/theme/theme_v2.dart` - Add perKeyCustomization to model
2. ✅ `lib/screens/main screens/per_key_customization_screen.dart` - Create new file
3. ✅ `lib/theme/theme_editor_v2.dart` - Add navigation button

## 🎉 Result

Users can now:
- Customize any key's font and appearance
- See changes in real-time preview
- Apply to system keyboard instantly
- Share themes with per-key customizations

**Total implementation time: ~30 minutes!**

---

## 📚 Full Documentation

See `HOW_TO_ADD_PER_KEY_CUSTOMIZATION_UI.md` for:
- Complete code
- Detailed explanations
- Troubleshooting
- Advanced features

