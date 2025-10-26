# 🎨 Keyboard Image Theme - Complete Analysis

## Current Implementation Overview

Your keyboard **already implements** full image coverage with visible key labels, exactly like the reference screenshots! Here's how it works:

---

## ✅ How Image Themes Currently Work

### 1. **Full Image Background Coverage**
**Location**: `ThemeManager.kt` lines 478-521

```kotlin
private fun buildImageBackground(): Drawable {
    val theme = getCurrentTheme()
    val imagePath = theme.background.imagePath
    
    // Load user's custom image
    val bitmap = loadImageBitmap(imagePath)
    val bitmapDrawable = BitmapDrawable(context.resources, bitmap).apply {
        alpha = (theme.background.imageOpacity * 255).toInt()
        isFilterBitmap = true
        gravity = Gravity.FILL  // ✅ Fills entire keyboard
        tileModeX = Shader.TileMode.CLAMP
        tileModeY = Shader.TileMode.CLAMP
    }
    
    // Creates LayerDrawable with:
    // 1. Base color layer
    // 2. User's image (covers full keyboard)
    // 3. Subtle gradient overlay for depth
}
```

**Result**: Image covers the **entire keyboard area** from top to bottom, including under all keys.

---

### 2. **Transparent Keys with Borders**
**Location**: `ThemeManager.kt` lines 361-391

```kotlin
private fun buildKeyDrawable(): Drawable {
    val palette = getCurrentPalette()
    val isTransparentStyle = palette.usesImageBackground || palette.isTransparentPreset
    
    val drawable = GradientDrawable()
    drawable.shape = GradientDrawable.RECTANGLE
    drawable.setColor(
        if (isTransparentStyle) Color.TRANSPARENT  // ✅ Transparent background
        else palette.keyBg
    )
    drawable.cornerRadius = radiusPx
    
    // ✅ Add white/visible border for transparent keys
    if (isTransparentStyle) {
        val strokeColor = if (palette.keyBorderEnabled) {
            palette.keyBorderColor
        } else {
            ColorUtils.setAlphaComponent(palette.keyText, 170)  // Semi-transparent white
        }
        drawable.setStroke(strokeWidth, strokeColor)
    }
}
```

**Result**: Keys are **transparent** with **white borders**, allowing the image to show through while keeping keys visible.

---

### 3. **Visible White Key Labels**
**Location**: `UnifiedKeyboardView.kt` (KeyboardGridView) lines 1471-1487

```kotlin
private fun drawKeyText(canvas: Canvas, key: DynamicKey, keyRect: RectF, palette: ThemePaletteV2) {
    val textPaint = Paint(parentView.keyTextPaint)
    textPaint.textSize = parentView.keyTextPaint.textSize * labelScaleMultiplier
    
    // ✅ White text for visibility on images
    textPaint.color = when {
        themeManager.shouldUseAccentForKey(key.keyType) -> Color.WHITE
        else -> palette.keyText  // White for image themes
    }
    
    canvas.drawText(text, centerX, centerY + textOffset, textPaint)
}
```

**Result**: Key labels are **white** (or theme text color) for maximum visibility on dark/colorful images.

---

## 📋 Theme Configuration for Image Keyboards

### JSON Theme Structure
**Location**: `themes/ThemeModels.kt` lines 18-557

To create an image theme like your screenshots:

```json
{
  "id": "custom_image_theme",
  "name": "My Photo Theme",
  "mode": "unified",
  "background": {
    "type": "image",                    // ✅ Enable image mode
    "color": "#1B1B1F",                 // Fallback color if image fails
    "imagePath": "/path/to/image.jpg",  // ✅ Your custom image
    "imageOpacity": 0.85,               // 0.0 - 1.0 (0.85 = 85% visible)
    "gradient": null,
    "overlayEffects": [],
    "adaptive": null
  },
  "keys": {
    "preset": "transparent",            // ✅ Makes keys transparent
    "bg": "#00000000",                  // Fully transparent
    "text": "#FFFFFF",                  // ✅ White labels
    "pressed": "#66FFFFFF",             // Semi-transparent white when pressed
    "rippleAlpha": 0.2,
    "border": {
      "enabled": true,                  // ✅ Show borders
      "color": "#FFFFFF",               // ✅ White borders
      "widthDp": 1.5                    // Border thickness
    },
    "radius": 8.0,                      // Rounded corners
    "shadow": {
      "enabled": false,
      "elevationDp": 0,
      "glow": false
    },
    "font": {
      "family": "Roboto",
      "sizeSp": 18.0,
      "bold": false,
      "italic": false
    }
  },
  "specialKeys": {
    "accent": "#FF9F1A",                // Enter, emoji, etc.
    "useAccentForEnter": true,
    "applyTo": ["enter", "emoji", "globe"],
    "spaceLabelColor": "#FFFFFF"        // ✅ White language label on space
  },
  "effects": {
    "pressAnimation": "ripple",
    "globalEffects": []
  }
}
```

---

## 🎯 Key Theme Properties for Image Keyboards

### Critical Settings:

| Property | Value | Purpose |
|----------|-------|---------|
| `background.type` | `"image"` | Enable image background mode |
| `background.imagePath` | File path or URI | Path to your custom image |
| `background.imageOpacity` | `0.7 - 1.0` | Image visibility (0.85 recommended) |
| `keys.preset` | `"transparent"` | Makes keys transparent |
| `keys.bg` | `"#00000000"` | Fully transparent key background |
| `keys.text` | `"#FFFFFF"` | White text for visibility |
| `keys.border.enabled` | `true` | Show key borders |
| `keys.border.color` | `"#FFFFFF"` | White borders |
| `keys.border.widthDp` | `1.0 - 2.0` | Border thickness |

---

## 🖼️ Image Processing Flow

### 1. Image Loading
**Location**: `ThemeManager.kt` lines 523-546

Supports multiple image sources:
- **Local files**: `/storage/emulated/0/Pictures/image.jpg`
- **Content URIs**: `content://media/external/images/media/123`
- **File URIs**: `file:///data/user/0/com.example.ai_keyboard/files/theme_image.jpg`
- **Assets**: `keyboards/keyboard_blue.png`
- **Network URLs**: `https://example.com/image.jpg` (cached)

### 2. Image Scaling
The image is automatically scaled to fit the keyboard using `Gravity.FILL`:
```kotlin
gravity = Gravity.FILL  // Scales image to fill entire keyboard area
tileModeX = Shader.TileMode.CLAMP  // No repeating
tileModeY = Shader.TileMode.CLAMP
```

### 3. Overlay Layers
From bottom to top:
1. **Base color** (fallback if image fails)
2. **User's image** (scaled to fit)
3. **Gradient overlay** (subtle darkening at bottom for better contrast)

---

## 🎨 Rendering Pipeline

### Full Stack Visualization:

```
┌─────────────────────────────────────────────────┐
│  UnifiedKeyboardView (Main Container)           │
│  ├─ background: Image Drawable (full coverage)  │ ← Your photo here
│  │                                               │
│  ├─ toolbarContainer (transparent)              │
│  │   └─ AI, Emoji, Settings buttons             │
│  │                                               │
│  ├─ suggestionContainer (transparent)           │
│  │   └─ Suggestion text (white)                 │
│  │                                               │
│  └─ bodyContainer (keyboard grid)               │
│      └─ KeyboardGridView                        │
│          ├─ Key 1: Transparent bg + white border + "Q" label
│          ├─ Key 2: Transparent bg + white border + "W" label
│          ├─ Key 3: Transparent bg + white border + "E" label
│          └─ ... (all keys transparent)          │
└─────────────────────────────────────────────────┘
```

### Key Rendering Details:
**Location**: `UnifiedKeyboardView.kt` lines 1408-1437

```kotlin
// 1. Draw transparent key background with border
val keyDrawable = themeManager.createKeyDrawable()  // Transparent + border
keyDrawable.setBounds(keyRect)
keyDrawable.draw(canvas)

// 2. Draw white text/icon on top
drawKeyText(canvas, key, keyRect, palette)  // White labels
```

**Result**: Image shows through transparent keys, labels are clearly visible.

---

## 🔧 Implementation Checklist

### ✅ Already Implemented:
- [x] Full image background coverage (entire keyboard)
- [x] Transparent key backgrounds
- [x] White/visible key borders
- [x] White text labels for all keys
- [x] White language label on spacebar
- [x] Accent-colored special keys (Enter, Emoji)
- [x] Image opacity control (0-100%)
- [x] Gradient overlay for better contrast
- [x] Support for local files, URIs, and network images
- [x] Image caching for performance
- [x] Transparent toolbar and suggestion bar
- [x] RTL language support

---

## 📱 Flutter Side Integration

### Theme Creation in Flutter
**Location**: `lib/screens/*_screen.dart` (theme editor screens)

When user uploads an image:

```dart
// 1. User selects image from gallery
final ImagePicker picker = ImagePicker();
final XFile? image = await picker.pickImage(source: ImageSource.gallery);

// 2. Save image to app storage
final appDir = await getApplicationDocumentsDirectory();
final savedImage = await File(image.path).copy('${appDir.path}/theme_image_$timestamp.jpg');

// 3. Create theme JSON with image path
final themeJson = {
  'id': 'custom_upload_$timestamp',
  'name': 'My Photo Theme',
  'background': {
    'type': 'image',
    'imagePath': savedImage.path,  // ✅ Absolute path to saved image
    'imageOpacity': 0.85,
  },
  'keys': {
    'preset': 'transparent',
    'bg': '#00000000',
    'text': '#FFFFFF',
    'border': {
      'enabled': true,
      'color': '#FFFFFF',
      'widthDp': 1.5,
    },
  },
};

// 4. Save to SharedPreferences
await prefs.setString('flutter.theme.v2.json', jsonEncode(themeJson));
```

### Theme Change Notification
**Location**: `android/app/.../AIKeyboardService.kt` lines 818-820

```kotlin
// Keyboard service listens for theme changes
D/AIKeyboardService: 🎨 THEME_CHANGED broadcast received! 
D/AIKeyboardService: Theme: Custom Image Theme (custom_upload_1761504649095)
D/AIKeyboardService: Loaded theme after reload: Custom Image Theme
```

---

## 🐛 Common Issues & Solutions

### Issue 1: Image Not Showing
**Symptoms**: Keyboard shows solid color instead of image

**Solutions**:
1. Check `background.type` is `"image"` (not `"solid"` or `"gradient"`)
2. Verify `imagePath` is a valid absolute path
3. Check file permissions (read access)
4. Ensure image file exists at specified path
5. Check logs for `Failed to load background image` errors

### Issue 2: Keys Not Transparent
**Symptoms**: Keys have colored backgrounds covering image

**Solutions**:
1. Set `keys.preset` to `"transparent"`
2. Set `keys.bg` to `"#00000000"` (fully transparent)
3. Check `ThemePaletteV2.usesImageBackground` returns `true`

### Issue 3: Labels Not Visible
**Symptoms**: Can't see key text on image

**Solutions**:
1. Set `keys.text` to `"#FFFFFF"` (white)
2. Enable borders: `keys.border.enabled = true`
3. Use darker images or increase `imageOpacity` to < 1.0
4. Add subtle gradient overlay (already implemented)

### Issue 4: Image Too Bright/Dark
**Symptoms**: Hard to see keys or text

**Solutions**:
1. Adjust `background.imageOpacity`: 
   - Too bright? → Decrease to 0.7
   - Too dark? → Increase to 1.0
2. Adjust `keys.border.color` contrast
3. Use higher contrast border: `widthDp: 2.0`

---

## 📊 Performance Considerations

### Image Caching
**Location**: `ThemeManager.kt` lines 48, 487-520

```kotlin
// LRU cache prevents repeated image loading
private val imageCache = LruCache<String, Drawable>(IMAGE_CACHE_SIZE)

// Images are cached by path
val cacheKey = "bg_image_layer_$imagePath"
return imageCache.get(cacheKey) ?: run {
    val bitmap = loadImageBitmap(imagePath)
    // ... create drawable ...
    imageCache.put(cacheKey, layered)
    layered
}
```

**Benefits**:
- Images loaded once, reused across theme changes
- Fast theme switching (cached drawables)
- Memory-efficient LRU eviction

### Image Optimization Recommendations:
- **Resolution**: 1080×800px or screen width × keyboard height
- **Format**: JPEG (smaller) or PNG (better quality)
- **File size**: < 500KB for fast loading
- **Aspect ratio**: Match device screen ratio

---

## 🎉 Summary

Your keyboard implementation **already supports full image themes** exactly like your reference screenshots:

✅ **Full image coverage** - Image fills entire keyboard background
✅ **Transparent keys** - Keys have no background, image shows through
✅ **White borders** - Keys have visible borders for definition
✅ **White labels** - All text is white for maximum visibility
✅ **Proper layering** - Image → Keys → Labels rendered correctly

### To create an image theme:
1. Set `background.type = "image"`
2. Set `background.imagePath` to your image file
3. Set `keys.preset = "transparent"`
4. Set `keys.text = "#FFFFFF"` (white)
5. Enable `keys.border` with white color

**The system is working correctly!** 🎯

