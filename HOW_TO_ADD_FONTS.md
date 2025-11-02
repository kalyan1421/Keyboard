# How to Add Custom Fonts to Your Keyboard

This guide explains how to add custom fonts to your AI Keyboard app, making them available for use on the system-wide keyboard.

## Quick Start

### 1. Add Font Files

Place your `.ttf` or `.otf` font files in the `assets/fonts/` directory:

```
assets/
  └── fonts/
      ├── YourFont-Regular.ttf
      ├── YourFont-Bold.ttf
      ├── YourFont-Italic.ttf
      └── YourFont-BoldItalic.ttf
```

### 2. The System Will Automatically Detect Them!

✅ **That's it!** The keyboard will automatically:
- Detect the new font files
- Make them available in the font picker
- Load them when selected by the user

No code changes or configuration needed!

## Font File Naming Conventions

For best results, name your font files following these patterns:

### Standard Naming
```
FontName-Regular.ttf      → "FontName" (Normal)
FontName-Bold.ttf         → "FontName" (Bold)
FontName-Italic.ttf       → "FontName" (Italic)
FontName-BoldItalic.ttf   → "FontName" (Bold+Italic)
```

### Alternative Naming
```
FontNameRegular.ttf       → "FontName" (Normal)
FontName.ttf              → "FontName" (Normal)
```

The system will automatically strip `-Regular`, `Regular`, and file extensions to create clean font names.

## Recommended Fonts to Add

Here are some popular free fonts you can add:

### Modern & Clean
- **Inter** - Modern sans-serif, great for UI
- **Poppins** - Geometric sans-serif
- **Montserrat** - Urban, modern headings
- **Open Sans** - Clean, readable
- **Lato** - Professional sans-serif

### Coding & Monospace
- **Fira Code** - Coding font with ligatures
- **JetBrains Mono** - Developer-friendly
- **Source Code Pro** - Adobe's monospace
- **Cascadia Code** - Microsoft's coding font

### Stylish & Display
- **Pacifico** - Handwritten script
- **Dancing Script** - Elegant cursive
- **Permanent Marker** - Casual marker style
- **Bebas Neue** - Bold condensed display

### Multilingual Support
- **Noto Sans** (already included) - 800+ languages
- **Noto Serif** - Serif version
- **Noto Sans Arabic** - Arabic script
- **Noto Sans Thai** - Thai script

## Where to Download Free Fonts

### Google Fonts (Recommended)
- Website: https://fonts.google.com
- All fonts are free and open source
- Click "Download family" to get all weights

### Font Squirrel
- Website: https://www.fontsquirrel.com
- Filter by "100% Free" for commercial use

### DaFont
- Website: https://www.dafont.com
- Check license before using commercially

## Step-by-Step: Adding a Font

### Example: Adding "Poppins" Font

1. **Download the font from Google Fonts**
   - Go to https://fonts.google.com/specimen/Poppins
   - Click "Download family"
   - Extract the ZIP file

2. **Copy font files to your project**
   ```bash
   cp Poppins-Regular.ttf /Users/kalyan/AI-keyboard/assets/fonts/
   cp Poppins-Bold.ttf /Users/kalyan/AI-keyboard/assets/fonts/
   cp Poppins-Italic.ttf /Users/kalyan/AI-keyboard/assets/fonts/
   ```

3. **Run the app**
   ```bash
   flutter run
   ```

4. **Select the font**
   - Open the app
   - Go to Theme Editor → Button tab
   - Click the Font dropdown
   - Select "Poppins"
   - The keyboard will now use Poppins font!

## Using Google Fonts Package (Alternative Method)

The app already includes `google_fonts` package. You can also use Google Fonts programmatically:

### In Flutter Code:
```dart
import 'package:google_fonts/google_fonts.dart';

// Use in a widget
Text(
  'Hello',
  style: GoogleFonts.poppins(fontSize: 18),
)
```

### Available via Google Fonts (No Download Required):
- 1000+ font families
- Automatically cached
- Usage: Just reference the font name

## Technical Details

### How Font Loading Works

1. **Flutter App (Font Selection)**
   - User selects font from dropdown/picker
   - Font name saved to `KeyboardThemeV2` as `keys.font.family`
   - Theme synchronized to SharedPreferences

2. **Android Keyboard Service (Font Rendering)**
   - Loads theme from SharedPreferences
   - `ThemeManager.createTypeface()` resolves the font:
     - Checks if it's a file (`.ttf`/`.otf`) → loads from assets
     - Checks if it's a system font → uses Android Typeface
     - Tries common naming patterns
     - Falls back to default font if not found
   - Caches loaded typefaces for performance

### Font Cache

Fonts are cached after first load for better performance:
```kotlin
// In ThemeManager.kt
private val fontCache = mutableMapOf<String, Typeface>()
```

Clear cache if needed by restarting the keyboard service.

## Troubleshooting

### Font Not Showing Up

**Problem**: Added font to `assets/fonts/` but don't see it in the dropdown.

**Solutions**:
1. Make sure file extension is `.ttf` or `.otf`
2. Check file name doesn't have spaces (use hyphens or underscores)
3. Hot restart the app: `r` in terminal, or stop and rerun

### Font Not Rendering on Keyboard

**Problem**: Selected font but keyboard still shows default font.

**Solutions**:
1. Go to Settings → System → Languages & input → Virtual keyboard → AI Keyboard → Stop service
2. Reopen an app with text input to restart keyboard
3. Check Android logs for font loading errors:
   ```bash
   adb logcat | grep ThemeManager
   ```

### Wrong Font Style (Bold/Italic)

**Problem**: Font appears normal even with Bold/Italic checked.

**Possible Causes**:
- Font doesn't include bold/italic variants
- Need to add `FontName-Bold.ttf` and `FontName-Italic.ttf`
- System will try to fake the style (less ideal)

**Solution**: Download all font weights from source.

## Best Practices

1. **Include Multiple Weights**
   - At minimum: Regular and Bold
   - Ideal: Regular, Bold, Italic, BoldItalic

2. **File Size Considerations**
   - Variable fonts are larger (~200KB each)
   - Static fonts are smaller (~50KB each)
   - Only include weights you'll actually use

3. **License Compliance**
   - Check font license before distributing
   - Google Fonts are all SIL Open Font License (commercial use OK)
   - Include LICENSE file if required by font

4. **Testing**
   - Test font on keyboard before finalizing
   - Check readability at small sizes
   - Verify all characters render (especially @, #, etc.)

## Advanced: Per-Key Fonts

You can set different fonts for individual keys using `perKeyCustomization`:

```json
{
  "keys": {
    "font": {
      "family": "Roboto",
      "sizeSp": 18
    },
    "perKeyCustomization": {
      "emoji": {
        "font": {
          "family": "NotoColorEmoji",
          "sizeSp": 20
        }
      },
      "space": {
        "font": {
          "family": "Courier",
          "sizeSp": 14
        }
      }
    }
  }
}
```

## Font Resources

- [Google Fonts](https://fonts.google.com) - 1000+ free fonts
- [Font Squirrel](https://www.fontsquirrel.com) - Commercial-free fonts
- [DaFont](https://www.dafont.com) - Free fonts (check license)
- [1001 Fonts](https://www.1001fonts.com) - Free font collection
- [Fontshare](https://www.fontshare.com) - Professional free fonts

## Support

For issues with font loading or rendering, check the logs:
```bash
# Android logs
adb logcat | grep -E "ThemeManager|Font"

# Flutter logs
flutter logs
```

Report font-related bugs with:
- Font name and source
- File size
- Error messages from logs
- Screenshots of the issue

