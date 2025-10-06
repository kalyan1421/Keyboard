# Noto Fonts for Indic Scripts

## Required Fonts for Phase 1 Implementation

To display Indic scripts correctly, download the following fonts from Google Fonts and place them in this directory:

### 1. Hindi (Devanagari)
**Font Name:** Noto Sans Devanagari
**Download from:** https://fonts.google.com/noto/specimen/Noto+Sans+Devanagari

**Files needed:**
- `NotoSansDevanagari-Regular.ttf`
- `NotoSansDevanagari-Bold.ttf` (optional, for emphasis)

### 2. Telugu
**Font Name:** Noto Sans Telugu  
**Download from:** https://fonts.google.com/noto/specimen/Noto+Sans+Telugu

**Files needed:**
- `NotoSansTelugu-Regular.ttf`
- `NotoSansTelugu-Bold.ttf` (optional)

### 3. Tamil
**Font Name:** Noto Sans Tamil
**Download from:** https://fonts.google.com/noto/specimen/Noto+Sans+Tamil

**Files needed:**
- `NotoSansTamil-Regular.ttf`
- `NotoSansTamil-Bold.ttf` (optional)

## Download Instructions

### Option 1: Google Fonts Website (Recommended)
1. Visit the links above
2. Click "Download family" button
3. Extract the .zip file
4. Copy the **static** TTF files (not variable fonts) to this directory
5. Rename if necessary to match the exact names above

### Option 2: Direct Download via GitHub
```bash
# Hindi (Devanagari)
wget https://github.com/notofonts/devanagari/raw/main/fonts/NotoSansDevanagari/hinted/ttf/NotoSansDevanagari-Regular.ttf

# Telugu
wget https://github.com/notofonts/telugu/raw/main/fonts/NotoSansTelugu/hinted/ttf/NotoSansTelugu-Regular.ttf

# Tamil
wget https://github.com/notofonts/tamil/raw/main/fonts/NotoSansTamil/hinted/ttf/NotoSansTamil-Regular.ttf
```

### Option 3: Package Manager (macOS/Linux)
```bash
# macOS (Homebrew)
brew tap homebrew/cask-fonts
brew install font-noto-sans-devanagari
brew install font-noto-sans-telugu
brew install font-noto-sans-tamil

# Then copy from system fonts:
# macOS: /Library/Fonts/Noto*.ttf
# Linux: /usr/share/fonts/noto/
```

## File Verification

After downloading, your directory should look like this:

```
assets/fonts/
├── README_FONTS.md (this file)
├── NotoSansDevanagari-Regular.ttf  ✅ Required
├── NotoSansTelugu-Regular.ttf       ✅ Required
└── NotoSansTamil-Regular.ttf        ✅ Required
```

## License

All Noto fonts are licensed under the **Open Font License (OFL)**:
https://scripts.sil.org/OFL

You are free to:
- Use commercially
- Modify and distribute
- Bundle with applications

## Testing

After placing the fonts, run the app and:
1. Switch to Hindi/Telugu/Tamil keyboard
2. Type some text
3. Verify characters display correctly (not as boxes/tofu)
4. Check logs for: "Applied [language] font: fonts/Noto*.ttf"

## Troubleshooting

**Q: Characters show as boxes (□)**
A: Fonts not loaded. Check:
- Files are in correct location
- Filenames match exactly (case-sensitive)
- Files aren't corrupted (re-download if needed)

**Q: Build error: Font not found**
A: Ensure assets directory is included in build.gradle:
```gradle
android {
    sourceSets {
        main {
            assets.srcDirs = ['assets']
        }
    }
}
```

**Q: Font file is too large**
A: Use the "hinted" TTF versions (not variable fonts)
- Hinted Regular: ~200-400 KB each
- Variable fonts: ~1-2 MB each (not recommended for mobile)

## Alternative Fonts (if Noto unavailable)

If you cannot use Noto fonts, fallback options:

**Hindi:**
- Lohit Devanagari
- Gargi (lightweight)
- Saral (supports conjuncts well)

**Telugu:**
- Lohit Telugu
- Potti Sreeramulu

**Tamil:**
- Lohit Tamil
- TSCu_Comic (casual style)

Note: Noto fonts are preferred for consistent quality and complete character coverage.

## Size Impact on APK

Adding these three fonts will increase APK size by approximately:
- NotoSansDevanagari-Regular.ttf: ~350 KB
- NotoSansTelugu-Regular.ttf: ~250 KB
- NotoSansTamil-Regular.ttf: ~200 KB
- **Total: ~800 KB**

This is acceptable for the quality improvement in Indic script rendering.

## Next Steps

Once fonts are downloaded:
1. Run `flutter clean && flutter build apk`
2. Install on device
3. Test transliteration: "namaste" → "नमस्ते"
4. Verify font rendering quality
5. Check spacebar shows language name in native script

---

**Last Updated:** October 5, 2025  
**Status:** ⏳ FONTS NEEDED - Download before building

