# How to Add Per-Key Customization UI from Flutter App to System Keyboard

## 📱 Overview

This guide shows you how to create a UI screen in your Flutter app where users can customize individual keys (fonts and button styles), and have those changes automatically apply to the system-wide Android keyboard.

## 🔄 Communication Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     FLUTTER APPLICATION                       │
│                                                               │
│  1. User Interface (ThemeEditorScreenV2)                     │
│     ↓                                                         │
│  2. Per-Key Customization Screen (NEW)                       │
│     ↓                                                         │
│  3. Update KeyboardThemeV2 with perKeyCustomization          │
│     ↓                                                         │
│  4. ThemeManagerV2.saveThemeV2(theme)                       │
│     ↓                                                         │
│  5. SharedPreferences.setString('theme.v2.json', json)       │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                          ↓
            [SharedPreferences Storage]
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                 ANDROID KEYBOARD SERVICE                      │
│                                                               │
│  1. SharedPreferences listener detects change                 │
│     ↓                                                         │
│  2. ThemeManager.loadThemeFromPrefs()                        │
│     ↓                                                         │
│  3. Parse perKeyCustomization map                            │
│     ↓                                                         │
│  4. KeyboardGridView renders with custom fonts/styles        │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Step 1: Add Per-Key Customization Data Structure to Flutter

First, update your Flutter theme model to support per-key customization:

**File: `lib/theme/theme_v2.dart`**

```dart
// Add these classes to your theme_v2.dart file

class KeyCustomization {
  final ThemeKeysFont? font;
  final Color? bg;
  final Color? text;
  final Color? pressed;
  final ThemeKeysBorder? border;
  final double? radius;
  final ThemeKeysShadow? shadow;

  const KeyCustomization({
    this.font,
    this.bg,
    this.text,
    this.pressed,
    this.border,
    this.radius,
    this.shadow,
  });

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{};
    if (font != null) map['font'] = font!.toJson();
    if (bg != null) map['bg'] = colorToHex(bg!);
    if (text != null) map['text'] = colorToHex(text!);
    if (pressed != null) map['pressed'] = colorToHex(pressed!);
    if (border != null) map['border'] = border!.toJson();
    if (radius != null) map['radius'] = radius;
    if (shadow != null) map['shadow'] = shadow!.toJson();
    return map;
  }

  static KeyCustomization fromJson(Map<String, dynamic> json) {
    return KeyCustomization(
      font: json['font'] != null 
          ? ThemeKeysFont.fromJson(json['font'] as Map<String, dynamic>) 
          : null,
      bg: json['bg'] != null ? hexToColor(json['bg'] as String) : null,
      text: json['text'] != null ? hexToColor(json['text'] as String) : null,
      pressed: json['pressed'] != null ? hexToColor(json['pressed'] as String) : null,
      border: json['border'] != null 
          ? ThemeKeysBorder.fromJson(json['border'] as Map<String, dynamic>) 
          : null,
      radius: json['radius'] as double?,
      shadow: json['shadow'] != null 
          ? ThemeKeysShadow.fromJson(json['shadow'] as Map<String, dynamic>) 
          : null,
    );
  }
}

// Update ThemeKeys class to include perKeyCustomization
class ThemeKeys {
  final String preset;
  final Color bg;
  final Color text;
  final Color pressed;
  final double rippleAlpha;
  final ThemeKeysBorder border;
  final double radius;
  final ThemeKeysShadow shadow;
  final ThemeKeysFont font;
  final Map<String, KeyCustomization> perKeyCustomization; // ← ADD THIS

  const ThemeKeys({
    required this.preset,
    required this.bg,
    required this.text,
    required this.pressed,
    required this.rippleAlpha,
    required this.border,
    required this.radius,
    required this.shadow,
    required this.font,
    this.perKeyCustomization = const {}, // ← ADD THIS
  });

  Map<String, dynamic> toJson() {
    final map = {
      'preset': preset,
      'bg': colorToHex(bg),
      'text': colorToHex(text),
      'pressed': colorToHex(pressed),
      'rippleAlpha': rippleAlpha,
      'border': border.toJson(),
      'radius': radius,
      'shadow': shadow.toJson(),
      'font': font.toJson(),
    };
    
    // ← ADD THIS
    if (perKeyCustomization.isNotEmpty) {
      map['perKeyCustomization'] = perKeyCustomization.map(
        (key, value) => MapEntry(key, value.toJson()),
      );
    }
    
    return map;
  }

  static ThemeKeys fromJson(Map<String, dynamic> json) {
    // Parse perKeyCustomization
    Map<String, KeyCustomization> perKey = {};
    if (json['perKeyCustomization'] != null) {
      final perKeyJson = json['perKeyCustomization'] as Map<String, dynamic>;
      perKey = perKeyJson.map(
        (key, value) => MapEntry(
          key,
          KeyCustomization.fromJson(value as Map<String, dynamic>),
        ),
      );
    }
    
    return ThemeKeys(
      preset: json['preset'] as String? ?? 'bordered',
      bg: hexToColor(json['bg'] as String? ?? '#3A3A3F'),
      text: hexToColor(json['text'] as String? ?? '#FFFFFF'),
      pressed: hexToColor(json['pressed'] as String? ?? '#505056'),
      rippleAlpha: (json['rippleAlpha'] as num?)?.toDouble() ?? 0.12,
      border: ThemeKeysBorder.fromJson(json['border'] as Map<String, dynamic>? ?? {}),
      radius: (json['radius'] as num?)?.toDouble() ?? 12.0,
      shadow: ThemeKeysShadow.fromJson(json['shadow'] as Map<String, dynamic>? ?? {}),
      font: ThemeKeysFont.fromJson(json['font'] as Map<String, dynamic>? ?? {}),
      perKeyCustomization: perKey, // ← ADD THIS
    );
  }
}
```

## 🎯 Step 2: Create Per-Key Customization UI Screen

Create a new file for the per-key customization screen:

**File: `lib/screens/main screens/per_key_customization_screen.dart`**

```dart
import 'package:flutter/material.dart';
import 'package:ai_keyboard/theme/theme_v2.dart';

class PerKeyCustomizationScreen extends StatefulWidget {
  final KeyboardThemeV2 theme;
  final Function(KeyboardThemeV2) onThemeUpdated;

  const PerKeyCustomizationScreen({
    super.key,
    required this.theme,
    required this.onThemeUpdated,
  });

  @override
  State<PerKeyCustomizationScreen> createState() => _PerKeyCustomizationScreenState();
}

class _PerKeyCustomizationScreenState extends State<PerKeyCustomizationScreen> {
  late KeyboardThemeV2 _currentTheme;
  String? _selectedKey;
  
  // Common keys that users might want to customize
  final List<KeyData> _commonKeys = [
    KeyData('a', 'A'),
    KeyData('s', 'S'),
    KeyData('d', 'D'),
    KeyData('f', 'F'),
    KeyData('enter', '⏎ Enter'),
    KeyData('space', 'Space'),
    KeyData('shift', '⇧ Shift'),
    KeyData('backspace', '⌫ Backspace'),
    KeyData('1', '1'),
    KeyData('2', '2'),
    KeyData('3', '3'),
  ];

  @override
  void initState() {
    super.initState();
    _currentTheme = widget.theme;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Customize Individual Keys'),
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _saveAndApplyTheme,
            tooltip: 'Save & Apply',
          ),
        ],
      ),
      body: Row(
        children: [
          // Left side: Key selector
          Expanded(
            flex: 2,
            child: _buildKeySelector(),
          ),
          const VerticalDivider(width: 1),
          // Right side: Customization options
          Expanded(
            flex: 3,
            child: _selectedKey != null
                ? _buildCustomizationPanel()
                : const Center(
                    child: Text('Select a key to customize'),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildKeySelector() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          'Select a Key',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 16),
        
        // Visual keyboard layout
        _buildVisualKeyboard(),
        
        const SizedBox(height: 24),
        const Divider(),
        const SizedBox(height: 16),
        
        // List of common keys
        const Text(
          'Quick Select',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        ..._commonKeys.map((keyData) => _buildKeyListItem(keyData)),
      ],
    );
  }

  Widget _buildVisualKeyboard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            // Row 1: QWERTYUIOP
            _buildKeyboardRow(['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p']),
            const SizedBox(height: 4),
            // Row 2: ASDFGHJKL
            _buildKeyboardRow(['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l']),
            const SizedBox(height: 4),
            // Row 3: ZXCVBNM
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _buildMiniKey('shift', '⇧'),
                const SizedBox(width: 2),
                ..._buildKeyboardRow(['z', 'x', 'c', 'v', 'b', 'n', 'm']).children,
                const SizedBox(width: 2),
                _buildMiniKey('backspace', '⌫'),
              ],
            ),
            const SizedBox(height: 4),
            // Row 4: Space row
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _buildMiniKey('symbols', '123'),
                const SizedBox(width: 2),
                Expanded(child: _buildMiniKey('space', 'Space')),
                const SizedBox(width: 2),
                _buildMiniKey('enter', '⏎'),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildKeyboardRow(List<String> keys) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: keys.map((key) {
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 2),
          child: _buildMiniKey(key, key.toUpperCase()),
        );
      }).toList(),
    );
  }

  Widget _buildMiniKey(String keyId, String label) {
    final isCustomized = _currentTheme.keys.perKeyCustomization.containsKey(keyId);
    final isSelected = _selectedKey == keyId;
    
    return InkWell(
      onTap: () => setState(() => _selectedKey = keyId),
      child: Container(
        width: label == 'Space' ? 120 : 32,
        height: 32,
        decoration: BoxDecoration(
          color: isCustomized
              ? Colors.blue.shade100
              : (isSelected ? Colors.orange.shade100 : Colors.grey.shade200),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(
            color: isSelected ? Colors.orange : (isCustomized ? Colors.blue : Colors.grey.shade400),
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Center(
          child: Text(
            label,
            style: TextStyle(
              fontSize: 10,
              fontWeight: isCustomized ? FontWeight.bold : FontWeight.normal,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildKeyListItem(KeyData keyData) {
    final isCustomized = _currentTheme.keys.perKeyCustomization.containsKey(keyData.id);
    final isSelected = _selectedKey == keyData.id;
    
    return Card(
      color: isSelected ? Colors.orange.shade50 : null,
      child: ListTile(
        leading: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: isCustomized ? Colors.blue.shade100 : Colors.grey.shade200,
            borderRadius: BorderRadius.circular(8),
          ),
          child: Center(
            child: Text(
              keyData.display,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
        ),
        title: Text(keyData.display),
        subtitle: isCustomized 
            ? const Text('Customized', style: TextStyle(color: Colors.blue))
            : const Text('Default'),
        trailing: isCustomized
            ? IconButton(
                icon: const Icon(Icons.clear, color: Colors.red),
                onPressed: () => _removeCustomization(keyData.id),
                tooltip: 'Remove customization',
              )
            : null,
        onTap: () => setState(() => _selectedKey = keyData.id),
      ),
    );
  }

  Widget _buildCustomizationPanel() {
    if (_selectedKey == null) return const SizedBox();
    
    final customization = _currentTheme.keys.perKeyCustomization[_selectedKey!];
    
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Customize "$_selectedKey" Key',
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 24),
        
        _buildSection(
          'Font',
          [
            _buildFontFamilyDropdown(customization),
            const SizedBox(height: 16),
            _buildFontSizeSlider(customization),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(child: _buildBoldCheckbox(customization)),
                Expanded(child: _buildItalicCheckbox(customization)),
              ],
            ),
          ],
        ),
        
        const SizedBox(height: 24),
        
        _buildSection(
          'Colors',
          [
            _buildColorPicker('Background', customization?.bg, (color) {
              _updateCustomization(bg: color);
            }),
            const SizedBox(height: 16),
            _buildColorPicker('Text', customization?.text, (color) {
              _updateCustomization(text: color);
            }),
            const SizedBox(height: 16),
            _buildColorPicker('Pressed', customization?.pressed, (color) {
              _updateCustomization(pressed: color);
            }),
          ],
        ),
        
        const SizedBox(height: 24),
        
        _buildSection(
          'Shape',
          [
            _buildRadiusSlider(customization),
          ],
        ),
        
        const SizedBox(height: 32),
        
        ElevatedButton.icon(
          onPressed: () => _removeCustomization(_selectedKey!),
          icon: const Icon(Icons.restore),
          label: const Text('Reset to Default'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.grey.shade300,
            foregroundColor: Colors.black87,
          ),
        ),
      ],
    );
  }

  Widget _buildSection(String title, List<Widget> children) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildFontFamilyDropdown(KeyCustomization? customization) {
    final currentFont = customization?.font?.family ?? _currentTheme.keys.font.family;
    
    return DropdownButtonFormField<String>(
      value: currentFont,
      decoration: const InputDecoration(
        labelText: 'Font Family',
        border: OutlineInputBorder(),
      ),
      items: const [
        DropdownMenuItem(value: 'Roboto', child: Text('Roboto')),
        DropdownMenuItem(value: 'NotoSans', child: Text('Noto Sans')),
        DropdownMenuItem(value: 'Poppins', child: Text('Poppins')),
        DropdownMenuItem(value: 'monospace', child: Text('Monospace')),
      ],
      onChanged: (value) {
        if (value != null) {
          _updateCustomization(fontFamily: value);
        }
      },
    );
  }

  Widget _buildFontSizeSlider(KeyCustomization? customization) {
    final currentSize = customization?.font?.sizeSp ?? _currentTheme.keys.font.sizeSp;
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Font Size: ${currentSize.round()}sp'),
        Slider(
          value: currentSize,
          min: 12,
          max: 28,
          divisions: 16,
          label: '${currentSize.round()}sp',
          onChanged: (value) {
            _updateCustomization(fontSize: value);
          },
        ),
      ],
    );
  }

  Widget _buildBoldCheckbox(KeyCustomization? customization) {
    final isBold = customization?.font?.bold ?? _currentTheme.keys.font.bold;
    
    return CheckboxListTile(
      title: const Text('Bold'),
      value: isBold,
      onChanged: (value) {
        if (value != null) {
          _updateCustomization(fontBold: value);
        }
      },
    );
  }

  Widget _buildItalicCheckbox(KeyCustomization? customization) {
    final isItalic = customization?.font?.italic ?? _currentTheme.keys.font.italic;
    
    return CheckboxListTile(
      title: const Text('Italic'),
      value: isItalic,
      onChanged: (value) {
        if (value != null) {
          _updateCustomization(fontItalic: value);
        }
      },
    );
  }

  Widget _buildRadiusSlider(KeyCustomization? customization) {
    final currentRadius = customization?.radius ?? _currentTheme.keys.radius;
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Corner Radius: ${currentRadius.round()}dp'),
        Slider(
          value: currentRadius,
          min: 0,
          max: 20,
          divisions: 20,
          label: '${currentRadius.round()}dp',
          onChanged: (value) {
            _updateCustomization(radius: value);
          },
        ),
      ],
    );
  }

  Widget _buildColorPicker(String label, Color? currentColor, ValueChanged<Color> onChanged) {
    final displayColor = currentColor ?? Colors.transparent;
    
    return ListTile(
      title: Text(label),
      trailing: GestureDetector(
        onTap: () => _showColorPicker(displayColor, onChanged),
        child: Container(
          width: 50,
          height: 50,
          decoration: BoxDecoration(
            color: currentColor ?? Colors.grey.shade300,
            border: Border.all(color: Colors.grey),
            borderRadius: BorderRadius.circular(8),
          ),
          child: currentColor == null
              ? const Center(child: Text('Default', style: TextStyle(fontSize: 10)))
              : null,
        ),
      ),
    );
  }

  void _showColorPicker(Color currentColor, ValueChanged<Color> onChanged) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Pick a Color'),
        content: Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            Colors.red, Colors.pink, Colors.purple, Colors.blue,
            Colors.cyan, Colors.teal, Colors.green, Colors.yellow,
            Colors.orange, Colors.brown, Colors.grey, Colors.black,
            Colors.white,
          ].map((color) {
            return GestureDetector(
              onTap: () {
                onChanged(color);
                Navigator.of(context).pop();
              },
              child: Container(
                width: 50,
                height: 50,
                decoration: BoxDecoration(
                  color: color,
                  border: Border.all(color: Colors.grey),
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }

  void _updateCustomization({
    String? fontFamily,
    double? fontSize,
    bool? fontBold,
    bool? fontItalic,
    Color? bg,
    Color? text,
    Color? pressed,
    double? radius,
  }) {
    if (_selectedKey == null) return;
    
    final currentCustomization = _currentTheme.keys.perKeyCustomization[_selectedKey!];
    final currentFont = currentCustomization?.font ?? _currentTheme.keys.font;
    
    final newFont = ThemeKeysFont(
      family: fontFamily ?? currentFont.family,
      sizeSp: fontSize ?? currentFont.sizeSp,
      bold: fontBold ?? currentFont.bold,
      italic: fontItalic ?? currentFont.italic,
    );
    
    final newCustomization = KeyCustomization(
      font: fontFamily != null || fontSize != null || fontBold != null || fontItalic != null
          ? newFont
          : currentCustomization?.font,
      bg: bg ?? currentCustomization?.bg,
      text: text ?? currentCustomization?.text,
      pressed: pressed ?? currentCustomization?.pressed,
      radius: radius ?? currentCustomization?.radius,
      border: currentCustomization?.border,
      shadow: currentCustomization?.shadow,
    );
    
    final newPerKeyCustomization = Map<String, KeyCustomization>.from(
      _currentTheme.keys.perKeyCustomization
    );
    newPerKeyCustomization[_selectedKey!] = newCustomization;
    
    setState(() {
      _currentTheme = _currentTheme.copyWith(
        keys: _currentTheme.keys.copyWith(
          perKeyCustomization: newPerKeyCustomization,
        ),
      );
    });
  }

  void _removeCustomization(String keyId) {
    final newPerKeyCustomization = Map<String, KeyCustomization>.from(
      _currentTheme.keys.perKeyCustomization
    );
    newPerKeyCustomization.remove(keyId);
    
    setState(() {
      _currentTheme = _currentTheme.copyWith(
        keys: _currentTheme.keys.copyWith(
          perKeyCustomization: newPerKeyCustomization,
        ),
      );
      if (_selectedKey == keyId) {
        _selectedKey = null;
      }
    });
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Removed customization for "$keyId" key')),
    );
  }

  void _saveAndApplyTheme() async {
    try {
      // Save theme using ThemeManagerV2
      await ThemeManagerV2.saveThemeV2(_currentTheme);
      
      // Notify parent widget
      widget.onThemeUpdated(_currentTheme);
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('✅ Theme saved and applied to keyboard!'),
            backgroundColor: Colors.green,
            duration: Duration(seconds: 2),
          ),
        );
        
        Navigator.of(context).pop(_currentTheme);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to save theme: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
}

class KeyData {
  final String id;
  final String display;
  
  KeyData(this.id, this.display);
}
```

## 🎯 Step 3: Add Button to Theme Editor

Update your **`theme_editor_v2.dart`** to add a button that opens the per-key customization screen:

```dart
// In ThemeEditorScreenV2 widget, add this to the AppBar actions:

actions: [
  IconButton(
    icon: const Icon(Icons.keyboard), // ← ADD THIS
    onPressed: () async {
      final updatedTheme = await Navigator.push<KeyboardThemeV2>(
        context,
        MaterialPageRoute(
          builder: (context) => PerKeyCustomizationScreen(
            theme: _currentTheme,
            onThemeUpdated: (theme) {
              setState(() {
                _currentTheme = theme;
              });
            },
          ),
        ),
      );
      
      if (updatedTheme != null) {
        _updateTheme(updatedTheme);
      }
    },
    tooltip: 'Customize Individual Keys',
  ),
  IconButton(
    icon: const Icon(Icons.upload),
    onPressed: _exportTheme,
    tooltip: 'Export Theme',
  ),
  // ... rest of your actions
],
```

## 🎯 Step 4: How It Works

### When User Customizes a Key:

1. **User selects a key** (e.g., "A" key) in the PerKeyCustomizationScreen
2. **User changes font/color/style** using the UI controls
3. **State updates** in Flutter with new `KeyCustomization` object
4. **User clicks "Save"**
5. **`ThemeManagerV2.saveThemeV2(theme)`** is called
6. **SharedPreferences** stores the theme JSON with `perKeyCustomization` map
7. **Android keyboard service** detects the change (via SharedPreferences listener)
8. **ThemeManager.loadThemeFromPrefs()** reads the new theme
9. **Keyboard re-renders** with custom fonts/styles for the specified key

### Example Theme JSON Saved to SharedPreferences:

```json
{
  "id": "custom_theme_123",
  "name": "My Custom Theme",
  "keys": {
    "preset": "bordered",
    "bg": "#FF3A3A3F",
    "text": "#FFFFFFFF",
    "perKeyCustomization": {
      "a": {
        "font": {
          "family": "Roboto",
          "sizeSp": 24.0,
          "bold": true,
          "italic": false
        },
        "bg": "#FFFF6B9D",
        "text": "#FFFFFFFF"
      },
      "enter": {
        "bg": "#FF4CAF50",
        "radius": 8.0
      }
    }
  }
}
```

## 🎯 Step 5: Test the Integration

### Testing Steps:

1. **Open your Flutter app**
2. **Navigate to Theme Editor** (ThemeGalleryScreen → ThemeEditorScreenV2)
3. **Click the Keyboard icon** in the AppBar (new button)
4. **Select a key** from the visual keyboard or list
5. **Customize the key** (change font, colors, etc.)
6. **Click Save**
7. **Switch to any app** with a text input
8. **Open the keyboard** - the customized key should show your changes!

## 🔧 Troubleshooting

### Theme not applying?
- Check if SharedPreferences key is correct: `"theme.v2.json"`
- Verify the keyboard service is reading from SharedPreferences
- Check Android logs: `adb logcat | grep ThemeManager`

### Customization not showing?
- Ensure key identifiers match (lowercase for letters: "a", not "A")
- Verify JSON is properly formatted
- Check if special keys are being overridden by accent color

### Performance issues?
- Limit customizations to essential keys only
- The Android keyboard caches drawables automatically

## 📚 Additional Features You Can Add

1. **Copy Key Customization**: Copy settings from one key to another
2. **Preset Templates**: Create templates like "High Contrast Vowels"
3. **Import/Export**: Share key customizations separately from themes
4. **Preview**: Real-time preview of customized keys
5. **Batch Edit**: Customize multiple keys at once

## ✅ Summary

You now have:
- ✅ Per-key customization UI in Flutter app
- ✅ Automatic sync to system-wide keyboard
- ✅ Visual key selector with mini keyboard
- ✅ Individual font and style controls per key
- ✅ Real-time theme saving to SharedPreferences
- ✅ Android keyboard automatically picks up changes

The connection is seamless - users customize keys in your app, and the system keyboard updates automatically! 🎉

