# 🎯 Image Theme Implementation Guide

## Exact Configuration for Your Screenshots

Based on your reference images showing the keyboard with a custom photo background, here's the **exact configuration** to achieve that look:

---

## 📸 Reference Analysis

From your screenshots, I can see:

1. ✅ **Full custom image** covering the entire keyboard
2. ✅ **Transparent keys** - the photo is visible through them
3. ✅ **White key borders** - thin lines defining each key
4. ✅ **White key labels** - "Q", "W", "E", "R", etc. all white
5. ✅ **Number mode** showing "1", "2", "3", etc. with same styling
6. ✅ **Symbol mode** showing "@", "#", "$", etc. with same styling
7. ✅ **Language label** "English (United States)" on spacebar

---

## 🔧 Complete Theme JSON Configuration

### Option 1: Flutter Theme Object

```dart
// lib/theme/theme_manager.dart or lib/models/keyboard_theme_v2.dart

KeyboardThemeV2 createImageTheme(String imagePath) {
  return KeyboardThemeV2(
    id: 'custom_upload_${DateTime.now().millisecondsSinceEpoch}',
    name: 'Custom Image Theme',
    mode: 'unified',
    
    // ==========================================
    // 1. BACKGROUND - Your Custom Image
    // ==========================================
    background: KeyboardBackground(
      type: 'image',                    // ✅ Image mode
      color: '#1B1B1F',                 // Fallback if image fails
      imagePath: imagePath,             // ✅ Path to your selected photo
      imageOpacity: 0.85,               // 85% visible (slight darkening)
      gradient: null,
      overlayEffects: [],
      adaptive: null,
    ),
    
    // ==========================================
    // 2. KEYS - Transparent with White Borders
    // ==========================================
    keys: KeyboardKeys(
      preset: 'transparent',            // ✅ Transparent preset
      bg: '#00000000',                  // Fully transparent background
      text: '#FFFFFF',                  // ✅ White text
      pressed: '#33FFFFFF',             // 20% white when pressed
      rippleAlpha: 0.2,                 // Subtle ripple effect
      
      border: KeyBorder(
        enabled: true,                  // ✅ Show borders
        color: '#FFFFFF',               // ✅ White borders
        widthDp: 1.5,                   // Medium thickness
      ),
      
      radius: 8.0,                      // Rounded corners (8dp)
      
      shadow: KeyShadow(
        enabled: false,                 // No shadow (looks cleaner)
        elevationDp: 0,
        glow: false,
      ),
      
      font: KeyFont(
        family: 'Roboto',
        sizeSp: 20.0,                   // Slightly larger for visibility
        bold: false,
        italic: false,
      ),
    ),
    
    // ==========================================
    // 3. SPECIAL KEYS - Accent Color
    // ==========================================
    specialKeys: SpecialKeys(
      accent: '#FF9F1A',                // Orange accent for Enter, Emoji
      useAccentForEnter: true,
      applyTo: ['enter', 'emoji', 'globe', 'mic'],
      spaceLabelColor: '#FFFFFF',       // ✅ White language label
    ),
    
    // ==========================================
    // 4. EFFECTS
    // ==========================================
    effects: KeyboardEffects(
      pressAnimation: 'ripple',         // Ripple on key press
      globalEffects: [],                // No particles/snow
    ),
    
    // ==========================================
    // 5. SOUNDS
    // ==========================================
    sounds: KeyboardSounds(
      pack: 'soft',
      customUris: {},
      volume: 0.6,
    ),
    
    // ==========================================
    // 6. STICKERS & ADVANCED
    // ==========================================
    stickers: KeyboardStickers(
      enabled: false,
      pack: '',
      position: 'behind',
      opacity: 0.9,
      animated: false,
    ),
    
    advanced: AdvancedSettings(
      livePreview: true,
      galleryEnabled: true,
      shareEnabled: true,
      dynamicTheme: 'none',
      seasonalPack: 'none',
      materialYouExtract: false,
    ),
  );
}
```

### Option 2: Raw JSON (for API or SharedPreferences)

```json
{
  "id": "custom_upload_1730000000000",
  "name": "Custom Image Theme",
  "mode": "unified",
  
  "background": {
    "type": "image",
    "color": "#FF1B1B1F",
    "imagePath": "/storage/emulated/0/Android/data/com.example.ai_keyboard/files/themes/image_123456.jpg",
    "imageOpacity": 0.85,
    "gradient": null,
    "overlayEffects": [],
    "adaptive": null
  },
  
  "keys": {
    "preset": "transparent",
    "bg": "#00000000",
    "text": "#FFFFFFFF",
    "pressed": "#33FFFFFF",
    "rippleAlpha": 0.2,
    
    "border": {
      "enabled": true,
      "color": "#FFFFFFFF",
      "widthDp": 1.5
    },
    
    "radius": 8.0,
    
    "shadow": {
      "enabled": false,
      "elevationDp": 0,
      "glow": false
    },
    
    "font": {
      "family": "Roboto",
      "sizeSp": 20.0,
      "bold": false,
      "italic": false
    }
  },
  
  "specialKeys": {
    "accent": "#FFFF9F1A",
    "useAccentForEnter": true,
    "applyTo": ["enter", "emoji", "globe", "mic"],
    "spaceLabelColor": "#FFFFFFFF"
  },
  
  "effects": {
    "pressAnimation": "ripple",
    "globalEffects": []
  },
  
  "sounds": {
    "pack": "soft",
    "customUris": {},
    "volume": 0.6
  },
  
  "stickers": {
    "enabled": false,
    "pack": "",
    "position": "behind",
    "opacity": 0.9,
    "animated": false
  },
  
  "advanced": {
    "livePreview": true,
    "galleryEnabled": true,
    "shareEnabled": true,
    "dynamicTheme": "none",
    "seasonalPack": "none",
    "materialYouExtract": false
  }
}
```

---

## 🖼️ Image Selection & Storage Flow

### Complete Implementation in Flutter:

```dart
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

class CustomImageThemeCreator {
  
  /// Step 1: Let user pick image from gallery
  Future<String?> pickAndSaveImage() async {
    try {
      // Pick image
      final ImagePicker picker = ImagePicker();
      final XFile? image = await picker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 1920,  // Limit resolution for performance
        maxHeight: 1080,
        imageQuality: 85, // Slight compression
      );
      
      if (image == null) return null;
      
      // Get app's private storage
      final appDir = await getApplicationDocumentsDirectory();
      final themesDir = Directory('${appDir.path}/themes');
      if (!await themesDir.exists()) {
        await themesDir.create(recursive: true);
      }
      
      // Save with timestamp
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final filename = 'custom_image_$timestamp.jpg';
      final savedPath = '${themesDir.path}/$filename';
      
      // Copy image to app storage
      await File(image.path).copy(savedPath);
      
      print('✅ Image saved to: $savedPath');
      return savedPath;
      
    } catch (e) {
      print('❌ Error picking image: $e');
      return null;
    }
  }
  
  /// Step 2: Create theme with the selected image
  Future<bool> createAndApplyImageTheme(String imagePath) async {
    try {
      final themeJson = _buildImageThemeJson(imagePath);
      
      // Save to SharedPreferences (Android keyboard reads from here)
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('flutter.theme.v2.json', jsonEncode(themeJson));
      await prefs.setBool('flutter.keyboard_settings.settings_changed', true);
      
      print('✅ Theme applied with image: $imagePath');
      return true;
      
    } catch (e) {
      print('❌ Error applying theme: $e');
      return false;
    }
  }
  
  /// Build complete theme JSON
  Map<String, dynamic> _buildImageThemeJson(String imagePath) {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    
    return {
      'id': 'custom_upload_$timestamp',
      'name': 'Custom Image Theme',
      'mode': 'unified',
      
      'background': {
        'type': 'image',              // ✅ KEY: Enable image mode
        'color': '#FF1B1B1F',
        'imagePath': imagePath,       // ✅ KEY: Your image path
        'imageOpacity': 0.85,
        'gradient': null,
        'overlayEffects': [],
        'adaptive': null,
      },
      
      'keys': {
        'preset': 'transparent',      // ✅ KEY: Transparent keys
        'bg': '#00000000',            // ✅ KEY: No background
        'text': '#FFFFFFFF',          // ✅ KEY: White text
        'pressed': '#33FFFFFF',
        'rippleAlpha': 0.2,
        
        'border': {
          'enabled': true,            // ✅ KEY: Show borders
          'color': '#FFFFFFFF',       // ✅ KEY: White borders
          'widthDp': 1.5,
        },
        
        'radius': 8.0,
        
        'shadow': {
          'enabled': false,
          'elevationDp': 0,
          'glow': false,
        },
        
        'font': {
          'family': 'Roboto',
          'sizeSp': 20.0,
          'bold': false,
          'italic': false,
        },
      },
      
      'specialKeys': {
        'accent': '#FFFF9F1A',
        'useAccentForEnter': true,
        'applyTo': ['enter', 'emoji', 'globe', 'mic'],
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
  }
  
  /// Complete workflow: Pick image and apply theme
  Future<void> createCustomImageTheme(BuildContext context) async {
    // Show loading
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => const Center(child: CircularProgressIndicator()),
    );
    
    try {
      // Step 1: Pick and save image
      final imagePath = await pickAndSaveImage();
      
      if (imagePath == null) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('No image selected')),
        );
        return;
      }
      
      // Step 2: Create and apply theme
      final success = await createAndApplyImageTheme(imagePath);
      
      Navigator.pop(context);
      
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('✅ Custom image theme applied!'),
            backgroundColor: Colors.green,
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('❌ Failed to apply theme'),
            backgroundColor: Colors.red,
          ),
        );
      }
      
    } catch (e) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: $e')),
      );
    }
  }
}
```

---

## 🎨 UI Integration Example

### Add Button to Theme Screen:

```dart
// In your theme selection screen

ElevatedButton.icon(
  onPressed: () async {
    final creator = CustomImageThemeCreator();
    await creator.createCustomImageTheme(context);
  },
  icon: const Icon(Icons.add_photo_alternate),
  label: const Text('Create Custom Image Theme'),
  style: ElevatedButton.styleFrom(
    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
    backgroundColor: Colors.blue,
    foregroundColor: Colors.white,
  ),
)
```

---

## 🔍 Verification & Testing

### Check Theme Applied Correctly:

```dart
Future<void> verifyThemeApplied() async {
  final prefs = await SharedPreferences.getInstance();
  final themeJson = prefs.getString('flutter.theme.v2.json');
  
  if (themeJson != null) {
    final theme = jsonDecode(themeJson);
    
    print('✅ Theme loaded:');
    print('  - Name: ${theme['name']}');
    print('  - Background type: ${theme['background']['type']}');
    print('  - Image path: ${theme['background']['imagePath']}');
    print('  - Keys preset: ${theme['keys']['preset']}');
    print('  - Keys bg: ${theme['keys']['bg']}');
    print('  - Keys text: ${theme['keys']['text']}');
    print('  - Border enabled: ${theme['keys']['border']['enabled']}');
    print('  - Border color: ${theme['keys']['border']['color']}');
    
    // Verify image file exists
    final imagePath = theme['background']['imagePath'];
    final imageFile = File(imagePath);
    if (await imageFile.exists()) {
      print('✅ Image file exists: $imagePath');
    } else {
      print('❌ Image file NOT found: $imagePath');
    }
  } else {
    print('❌ No theme found in SharedPreferences');
  }
}
```

---

## 📊 Customization Options

### Adjust for Different Looks:

| Want to... | Change | Value |
|------------|--------|-------|
| **Brighter image** | `imageOpacity` | `0.9 - 1.0` |
| **Darker image** | `imageOpacity` | `0.6 - 0.8` |
| **Thicker borders** | `border.widthDp` | `2.0 - 3.0` |
| **No borders** | `border.enabled` | `false` |
| **Colored borders** | `border.color` | `#FF00FFFF` (cyan) |
| **Larger text** | `font.sizeSp` | `22.0 - 24.0` |
| **Bold text** | `font.bold` | `true` |
| **More rounded keys** | `radius` | `12.0 - 16.0` |
| **Square keys** | `radius` | `0.0` |

### Example Variations:

#### Variation 1: High Contrast (Very Visible)
```dart
'imageOpacity': 0.7,        // Darker image
'border.widthDp': 2.0,      // Thicker borders
'font.sizeSp': 22.0,        // Larger text
'font.bold': true,          // Bold text
```

#### Variation 2: Minimal/Subtle
```dart
'imageOpacity': 1.0,        // Full image brightness
'border.widthDp': 1.0,      // Thin borders
'border.color': '#99FFFFFF',// Semi-transparent borders
'font.sizeSp': 18.0,        // Normal text
```

#### Variation 3: Colorful Borders
```dart
'border.color': '#FF00FFFF', // Cyan borders
'specialKeys.accent': '#FFFF00FF', // Magenta accent
```

---

## 🐛 Troubleshooting

### Problem: Image Not Showing

**Check 1**: Verify theme type
```dart
// Should be "image", not "solid" or "gradient"
final bgType = theme['background']['type'];
print('Background type: $bgType');  // Should print: "image"
```

**Check 2**: Verify image path exists
```dart
final imagePath = theme['background']['imagePath'];
final file = File(imagePath);
print('File exists: ${await file.exists()}');  // Should be: true
```

**Check 3**: Check Android logs
```bash
adb logcat | grep "ThemeManager\|AIKeyboardService"
# Look for: "Loaded theme after reload: Custom Image Theme"
```

### Problem: Keys Not Transparent

**Check**: Verify keys configuration
```dart
print('Keys preset: ${theme['keys']['preset']}');    // Should be: "transparent"
print('Keys bg: ${theme['keys']['bg']}');            // Should be: "#00000000"
```

### Problem: Can't See Text

**Check**: Verify text color
```dart
print('Keys text: ${theme['keys']['text']}');        // Should be: "#FFFFFFFF" (white)
print('Border enabled: ${theme['keys']['border']['enabled']}');  // Should be: true
```

---

## 📝 Complete Example App

```dart
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';

class CustomImageThemeScreen extends StatefulWidget {
  @override
  State<CustomImageThemeScreen> createState() => _CustomImageThemeScreenState();
}

class _CustomImageThemeScreenState extends State<CustomImageThemeScreen> {
  String? _selectedImagePath;
  bool _isLoading = false;
  
  final _creator = CustomImageThemeCreator();
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Custom Image Theme'),
        backgroundColor: Colors.deepPurple,
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Preview selected image
            if (_selectedImagePath != null)
              Container(
                width: 300,
                height: 200,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  image: DecorationImage(
                    image: FileImage(File(_selectedImagePath!)),
                    fit: BoxFit.cover,
                  ),
                ),
              )
            else
              Container(
                width: 300,
                height: 200,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  color: Colors.grey[300],
                ),
                child: const Center(
                  child: Text('No image selected'),
                ),
              ),
            
            const SizedBox(height: 32),
            
            // Pick image button
            ElevatedButton.icon(
              onPressed: _isLoading ? null : () async {
                setState(() => _isLoading = true);
                final path = await _creator.pickAndSaveImage();
                setState(() {
                  _selectedImagePath = path;
                  _isLoading = false;
                });
              },
              icon: const Icon(Icons.add_photo_alternate),
              label: const Text('Select Image'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Apply theme button
            ElevatedButton.icon(
              onPressed: (_isLoading || _selectedImagePath == null) 
                  ? null 
                  : () async {
                      setState(() => _isLoading = true);
                      final success = await _creator.createAndApplyImageTheme(_selectedImagePath!);
                      setState(() => _isLoading = false);
                      
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(success 
                              ? '✅ Theme applied! Open keyboard to see it.' 
                              : '❌ Failed to apply theme'),
                          backgroundColor: success ? Colors.green : Colors.red,
                        ),
                      );
                    },
              icon: const Icon(Icons.check),
              label: const Text('Apply Theme'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
                backgroundColor: Colors.green,
              ),
            ),
            
            if (_isLoading)
              const Padding(
                padding: EdgeInsets.all(16.0),
                child: CircularProgressIndicator(),
              ),
          ],
        ),
      ),
    );
  }
}
```

---

## ✅ Final Checklist

Before deploying:

- [ ] Image picker works on Android 13+ (check permissions)
- [ ] Images saved to app private storage (not gallery)
- [ ] Theme JSON saved to `flutter.theme.v2.json` in SharedPreferences
- [ ] `settings_changed` flag set to `true` to trigger reload
- [ ] Keyboard service receives theme change broadcast
- [ ] Image file path is absolute (not relative)
- [ ] Image resolution is reasonable (< 1920×1080)
- [ ] Image file size is manageable (< 1MB)
- [ ] Theme preview shown before applying
- [ ] Error handling for image picker failures
- [ ] Error handling for file I/O failures
- [ ] User feedback (loading indicators, success/error messages)

---

## 🎉 You're Done!

Your keyboard now supports custom image themes exactly like your reference screenshots! The implementation is already complete in your codebase - you just need to configure themes with the settings shown above.

**Key Points to Remember:**
1. Set `background.type = "image"`
2. Provide valid `imagePath`
3. Use `keys.preset = "transparent"`
4. Enable white borders and white text
5. Save theme to SharedPreferences

Happy theming! 🎨✨

