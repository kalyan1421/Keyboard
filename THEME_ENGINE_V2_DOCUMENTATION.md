# CleverType Theme Engine V2 - Complete Documentation

## 📋 Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Data Flow](#data-flow)
4. [File-by-File Breakdown](#file-by-file-breakdown)
5. [Core Components](#core-components)
6. [Theme Application Flow](#theme-application-flow)
7. [Key Methods & APIs](#key-methods--apis)
8. [Cross-Platform Communication](#cross-platform-communication)
9. [Usage Examples](#usage-examples)

---

## Overview

The Theme Engine V2 is a **JSON-based, cross-platform theming system** that allows users to customize every aspect of the AI Keyboard's appearance. It replaces the old hardcoded color system with a flexible, centralized theme manager.

### Key Features
- ✅ **Single Source of Truth**: All theme data stored in one JSON blob
- ✅ **Cross-Platform**: Shared between Flutter (UI) and Android (Keyboard IME)
- ✅ **Live Preview**: Real-time theme updates in editor
- ✅ **System-Wide**: Themes apply to the actual keyboard, not just app UI
- ✅ **CleverType-Style UX**: Toolbar & suggestions inherit from background, keys have independent styling
- ✅ **Import/Export**: Share themes as JSON files
- ✅ **No Hardcoded Colors**: All visual styling is programmatic
- ✅ **Zero Visual Gaps**: Seamless integration between toolbar, suggestions, and keys

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     FLUTTER LAYER (UI)                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────┐      ┌──────────────────────┐   │
│  │ ThemeEditorScreenV2  │──────│   KeyboardThemeV2    │   │
│  │  (User Interface)    │      │   (Dart Model)       │   │
│  └──────────────────────┘      └──────────────────────┘   │
│              │                           │                 │
│              └───────────┬───────────────┘                 │
│                          ▼                                 │
│              ┌──────────────────────┐                      │
│              │   ThemeManagerV2     │                      │
│              │  (Flutter Manager)   │                      │
│              └──────────────────────┘                      │
│                          │                                 │
└──────────────────────────┼─────────────────────────────────┘
                           │
                           │ SharedPreferences
                           │ Key: "flutter.theme.v2.json"
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    ANDROID LAYER (IME)                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────┐      ┌──────────────────────┐   │
│  │   ThemeManager.kt    │──────│  KeyboardThemeV2.kt  │   │
│  │  (Kotlin Manager)    │      │   (Kotlin Model)     │   │
│  └──────────────────────┘      └──────────────────────┘   │
│              │                           │                 │
│              └───────────┬───────────────┘                 │
│                          ▼                                 │
│              ┌──────────────────────┐                      │
│              │   ThemePaletteV2     │                      │
│              │  (Derived Colors)    │                      │
│              └──────────────────────┘                      │
│                          │                                 │
│              ┌───────────┴───────────┐                     │
│              ▼                       ▼                     │
│  ┌──────────────────────┐  ┌──────────────────────┐      │
│  │ SwipeKeyboardView.kt │  │ AIKeyboardService.kt │      │
│  │  (Draws Keyboard)    │  │  (IME Lifecycle)     │      │
│  └──────────────────────┘  └──────────────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### 1. **Theme Creation/Editing** (Flutter → SharedPreferences)

```
User edits theme in ThemeEditorScreenV2
         ↓
KeyboardThemeV2.toJson() converts to Map
         ↓
jsonEncode() converts to JSON string
         ↓
SharedPreferences.setString('theme.v2.json', jsonStr)
         ↓
Flutter plugin adds prefix → 'flutter.theme.v2.json'
         ↓
Data written to disk (FlutterSharedPreferences.xml)
```

### 2. **Cross-Platform Notification** (Flutter → Android)

```
ThemeManagerV2.saveThemeV2() called
         ↓
Saves to SharedPreferences
         ↓
MethodChannel('ai_keyboard/config').invokeMethod('themeChanged')
         ↓
MainActivity.kt receives call
         ↓
Broadcasts Intent: "com.example.ai_keyboard.THEME_CHANGED"
         ↓
AIKeyboardService.kt receives broadcast
```

### 3. **Theme Loading** (SharedPreferences → Android)

```
AIKeyboardService starts
         ↓
ThemeManager.kt initializes
         ↓
Reads SharedPreferences('flutter.theme.v2.json')
         ↓
KeyboardThemeV2.fromJson() parses JSON
         ↓
ThemePaletteV2 derived from theme (applies inheritance, contrast)
         ↓
Caches created (drawables, paints, images)
```

### 4. **Theme Application** (Android → UI)

```
ThemeManager.getCurrentTheme() returns KeyboardThemeV2
         ↓
ThemeManager.getPalette() returns ThemePaletteV2
         ↓
SwipeKeyboardView.onDraw() called
         ↓
Uses ThemeManager factory methods:
  - createKeyDrawable()
  - createKeyboardBackground()
  - createKeyTextPaint()
         ↓
Keyboard rendered with themed visuals
```

---

## File-by-File Breakdown

### **FLUTTER LAYER**

#### `lib/theme/theme_v2.dart` (876 lines)
**Purpose**: Dart models and Flutter-side theme management

**Key Components**:

1. **Data Classes** (Lines 1-715)
   - `KeyboardThemeV2`: Root theme object
   - `ThemeBackground`: Background configuration (solid/gradient/image)
   - `ThemeKeys`: Key styling (colors, borders, shadows, fonts)
   - `ThemeSpecialKeys`: Accent colors for special keys
   - `ThemeToolbar`: Toolbar configuration
   - `ThemeSuggestions`: Suggestion bar styling
   - `ThemeEffects`: Press animations
   - `ThemeSounds`: Sound pack configuration
   - `ThemeStickers`: Sticker layer settings
   - `ThemeAdvanced`: Advanced features

2. **Factory Methods**:
   ```dart
   // Line 136-228
   static KeyboardThemeV2 createDefault() {
     // Returns a default light theme
   }

   static KeyboardThemeV2 createDefaultDark() {
     // Returns a default dark theme
   }
   ```

3. **Serialization**:
   ```dart
   // Line 230-450
   factory KeyboardThemeV2.fromJson(Map<String, dynamic> json) {
     // Parses JSON with defaults for missing fields
   }

   Map<String, dynamic> toJson() {
     // Converts to JSON map
   }
   ```

**ThemeManagerV2 Class** (Lines 740-876):

1. **Storage Keys**:
   ```dart
   // Line 743-744
   static const String _themeKey = 'theme.v2.json';
   static const String _settingsChangedKey = 'keyboard_settings.settings_changed';
   // Flutter plugin adds "flutter." prefix → 'flutter.theme.v2.json'
   ```

2. **Save Method** (Lines 747-762):
   ```dart
   static Future<void> saveThemeV2(KeyboardThemeV2 theme) async {
     // 1. Convert theme to JSON
     final jsonStr = jsonEncode(theme.toJson());
     
     // 2. Save to SharedPreferences
     await prefs.setString(_themeKey, jsonStr);
     await prefs.setBool(_settingsChangedKey, true);
     
     // 3. Wait for native layer sync
     await Future.delayed(const Duration(milliseconds: 100));
     await prefs.reload();
     
     // 4. Send broadcast to Android
     await _sendThemeBroadcast(theme);
   }
   ```

3. **Broadcast Method** (Lines 765-778):
   ```dart
   static Future<void> _sendThemeBroadcast(KeyboardThemeV2 theme) async {
     const platform = MethodChannel('ai_keyboard/config');
     await platform.invokeMethod('themeChanged', {
       'themeId': theme.id,
       'themeName': theme.name,
       'hasThemeData': true,
     });
   }
   ```

4. **Load Method** (Lines 790-807):
   ```dart
   static Future<KeyboardThemeV2> loadThemeV2() async {
     final prefs = await SharedPreferences.getInstance();
     final jsonStr = prefs.getString(_themeKey);
     
     if (jsonStr != null) {
       final json = jsonDecode(jsonStr);
       return KeyboardThemeV2.fromJson(json);
     }
     return KeyboardThemeV2.createDefault();
   }
   ```

---

#### `lib/theme/theme_editor_v2.dart` (1,299 lines)
**Purpose**: UI for editing themes with live preview

**Key Components**:

1. **State Management** (Lines 27-65):
   ```dart
   class _ThemeEditorScreenV2State extends State<ThemeEditorScreenV2> {
     late TabController _tabController;
     late KeyboardThemeV2 _currentTheme;
     final _nameController = TextEditingController();
     
     @override
     void initState() {
       _currentTheme = widget.initialTheme ?? KeyboardThemeV2.createDefault();
       _nameController.text = _currentTheme.name;
     }
   }
   ```

2. **Save Theme** (Lines 66-89):
   ```dart
   Future<void> _saveTheme() async {
     final updatedTheme = _currentTheme.copyWith(
       name: _nameController.text.trim(),
       id: _currentTheme.id.isEmpty ? 'custom_${timestamp}' : _currentTheme.id,
     );
     
     await ThemeManagerV2.saveThemeV2(updatedTheme);
     
     ScaffoldMessenger.of(context).showSnackBar(
       const SnackBar(content: Text('Theme saved successfully!')),
     );
     Navigator.pop(context, updatedTheme);
   }
   ```

3. **Update Theme** (Lines 97-106):
   ```dart
   void _updateTheme(KeyboardThemeV2 newTheme) {
     setState(() {
       _currentTheme = newTheme;
     });
     
     // Apply immediately if live preview enabled
     if (_currentTheme.advanced.livePreview) {
       _applyThemeToKeyboard(newTheme);
     }
   }
   ```

4. **Apply to Keyboard** (Lines 108-115):
   ```dart
   Future<void> _applyThemeToKeyboard(KeyboardThemeV2 theme) async {
     await ThemeManagerV2.saveThemeV2(theme);
   }
   ```

5. **Import/Export** (Lines 117-165):
   ```dart
   Future<void> _exportTheme() async {
     final jsonStr = jsonEncode(_currentTheme.toJson());
     final fileName = '${_currentTheme.name.replaceAll(' ', '_')}_theme.json';
     // Save to file...
   }

   Future<void> _importTheme() async {
     final result = await FilePicker.platform.pickFiles(
       type: FileType.custom,
       allowedExtensions: ['json'],
     );
     // Load and parse...
   }
   ```

6. **UI Tabs** (Lines 186-199):
   - **Basic**: Name, mode (unified/split)
   - **Background**: Type, color, image, gradient, effects
   - **Keys**: Colors, borders, shadows, fonts, presets
   - **Toolbar**: Inheritance, colors, height, icon pack
   - **Suggestions**: Inheritance, chips, fonts
   - **Advanced**: Effects, sounds, stickers

---

#### `lib/main.dart` (Usage)
**Purpose**: App initialization and navigation

**Theme Initialization** (Line 50):
```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize V2 theme system
  await ThemeManagerV2.loadThemeV2();
  
  runApp(MyApp());
}
```

**Navigation to Editor**:
```dart
// Navigate to theme editor
Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => ThemeEditorScreenV2(
      initialTheme: currentTheme,
    ),
  ),
);
```

---

### **ANDROID LAYER**

#### `android/app/src/main/kotlin/com/example/ai_keyboard/themes/ThemeModels.kt` (739 lines)
**Purpose**: Kotlin data classes mirroring JSON schema

**Key Components**:

1. **Root Data Class** (Lines 10-30):
   ```kotlin
   data class KeyboardThemeV2(
       val id: String,
       val name: String,
       val mode: String,
       val background: Background,
       val keys: Keys,
       val specialKeys: SpecialKeys,
       val toolbar: Toolbar,
       val suggestions: Suggestions,
       val effects: Effects,
       val sounds: Sounds,
       val stickers: Stickers,
       val advanced: Advanced
   )
   ```

2. **Nested Classes** (Lines 32-200):
   - `Background`: `type`, `color`, `imagePath`, `imageOpacity`, `gradient`, `overlayEffects`
   - `Keys`: `preset`, `bg`, `text`, `pressed`, `border`, `radius`, `shadow`, `font`
   - `SpecialKeys`: `accent`, `useAccentForEnter`, `applyTo`, `spaceLabelColor`
   - And all other components...

3. **JSON Parsing** (Lines 202-450):
   ```kotlin
   companion object {
       fun fromJson(jsonString: String): KeyboardThemeV2 {
           val json = JSONObject(jsonString)
           
           // Parse background
           val bgObj = json.optJSONObject("background") ?: JSONObject()
           val background = Background(
               type = bgObj.optString("type", "solid"),
               color = parseColor(bgObj.optString("color", "#1B1B1F")),
               // ... parse all fields with defaults
           )
           
           // Parse keys, specialKeys, toolbar, etc.
           
           return KeyboardThemeV2(
               id = json.optString("id", "default_theme"),
               name = json.optString("name", "Default Theme"),
               // ... all fields
           )
       }
   }
   ```

4. **JSON Serialization** (Lines 452-739):
   ```kotlin
   fun toJson(): String {
       val obj = JSONObject()
       obj.put("id", id)
       obj.put("name", name)
       obj.put("mode", mode)
       
       // Serialize background
       val bgObj = JSONObject()
       bgObj.put("type", background.type)
       bgObj.put("color", colorToHex(background.color))
       // ... all fields
       obj.put("background", bgObj)
       
       // Serialize all other sections
       
       return obj.toString(2) // Pretty print with indent
   }
   ```

5. **Helper Methods**:
   ```kotlin
   // Line 600-620
   private fun parseColor(colorStr: String): Int {
       return try {
           Color.parseColor(colorStr)
       } catch (e: Exception) {
           Color.BLACK
       }
   }

   private fun colorToHex(color: Int): String {
       return String.format("#%06X", 0xFFFFFF and color)
   }
   ```

---

#### `android/app/src/main/kotlin/com/example/ai_keyboard/themes/ThemePaletteV2.kt`
**Purpose**: Derived theme properties with inheritance logic

**Key Components**:

```kotlin
class ThemePaletteV2(theme: KeyboardThemeV2) {
    // Keyboard background
    val keyboardBg: Int = theme.background.color ?: Color.parseColor("#1B1B1F")
    
    // Key colors
    val keyBg: Int = theme.keys.bg
    val keyText: Int = theme.keys.text
    val keyPressed: Int = theme.keys.pressed
    val keyBorder: Int = theme.keys.border.color
    
    // Special keys
    val accent: Int = theme.specialKeys.accent
    val spaceLabelColor: Int = theme.specialKeys.spaceLabelColor
    
    // Toolbar - with inheritance
    val toolbarBg: Int = if (theme.toolbar.inheritFromKeys) {
        theme.keys.bg
    } else {
        theme.toolbar.bg
    }
    
    val toolbarIcon: Int = if (theme.toolbar.inheritFromKeys) {
        theme.keys.text
    } else {
        theme.toolbar.icon
    }
    
    // Suggestions - with inheritance
    val suggestionBg: Int = if (theme.suggestions.inheritFromKeys) {
        theme.keys.bg
    } else {
        theme.suggestions.bg
    }
    
    val suggestionText: Int = if (theme.suggestions.inheritFromKeys) {
        theme.keys.text
    } else {
        theme.suggestions.text
    }
    
    // Chip colors
    val chipBg: Int = theme.suggestions.chip.bg
    val chipText: Int = theme.suggestions.chip.text
    val chipPressed: Int = theme.suggestions.chip.pressed
    
    // Dimensions
    val keyRadius: Float = theme.keys.radius
    val toolbarHeight: Float = theme.toolbar.heightDp
    val chipRadius: Float = theme.suggestions.chip.radius
    
    // Fonts
    val keyFontSize: Float = theme.keys.font.sizeSp
    val keyFontFamily: String = theme.keys.font.family
    val suggestionFontSize: Float = theme.suggestions.font.sizeSp
}
```

---

#### `android/app/src/main/kotlin/com/example/ai_keyboard/ThemeManager.kt` (614 lines)
**Purpose**: Central theme manager for Android - loads, caches, provides theme data

**Key Components**:

1. **Constants** (Lines 25-35):
   ```kotlin
   companion object {
       private const val TAG = "ThemeManagerV2"
       private const val PREFS_NAME = "FlutterSharedPreferences"
       // CRITICAL: Flutter plugin adds "flutter." prefix automatically!
       private const val THEME_V2_KEY = "flutter.theme.v2.json"
       private const val SETTINGS_CHANGED_KEY = "flutter.keyboard_settings.settings_changed"
       
       private const val DRAWABLE_CACHE_SIZE = 50
       private const val IMAGE_CACHE_SIZE = 10
   }
   ```

2. **Initialization** (Lines 63-66):
   ```kotlin
   init {
       prefs.registerOnSharedPreferenceChangeListener(prefsListener)
       loadThemeFromPrefs()
   }
   ```

3. **SharedPreferences Listener** (Lines 51-56):
   ```kotlin
   private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
       if (key == THEME_V2_KEY || key == SETTINGS_CHANGED_KEY) {
           loadThemeFromPrefs()
           notifyThemeChanged()
       }
   }
   ```

4. **Theme Loading** (Lines 88-109):
   ```kotlin
   private fun loadThemeFromPrefs() {
       val themeJson = prefs.getString(THEME_V2_KEY, null)
       
       if (themeJson != null) {
           val theme = KeyboardThemeV2.fromJson(themeJson)
           val newHash = themeJson.hashCode().toString()
           
           // Only update if theme actually changed
           if (newHash != themeHash) {
               currentTheme = theme
               currentPalette = ThemePaletteV2(theme)
               themeHash = newHash
               
               // Clear caches on theme change
               drawableCache.evictAll()
               imageCache.evictAll()
           }
       } else {
           migrateOldTheme()
       }
   }
   ```

5. **Drawable Factory Methods** (Lines 220-350):

   **Key Drawable**:
   ```kotlin
   fun createKeyDrawable(keyType: String, isPressed: Boolean): Drawable {
       val cacheKey = "key_${keyType}_${isPressed}_$themeHash"
       drawableCache.get(cacheKey)?.let { return it }
       
       val theme = getCurrentTheme()
       val color = when {
           isPressed -> theme.keys.pressed
           keyType in theme.specialKeys.applyTo -> theme.specialKeys.accent
           else -> theme.keys.bg
       }
       
       val drawable = GradientDrawable().apply {
           setColor(color)
           cornerRadius = theme.keys.radius * density
           
           if (theme.keys.border.enabled && !isPressed) {
               setStroke(
                   (theme.keys.border.widthDp * density).toInt(),
                   theme.keys.border.color
               )
           }
           
           if (theme.keys.shadow.enabled) {
               // Apply shadow/elevation
           }
       }
       
       drawableCache.put(cacheKey, drawable)
       return drawable
   }
   ```

   **Keyboard Background**:
   ```kotlin
   fun createKeyboardBackground(): Drawable {
       val cacheKey = "kb_bg_$themeHash"
       drawableCache.get(cacheKey)?.let { return it }
       
       val theme = getCurrentTheme()
       val drawable = when (theme.background.type) {
           "solid" -> buildSolidDrawable(theme.background.color)
           "gradient" -> buildGradientDrawable(theme.background.gradient)
           "image" -> buildImageDrawable(theme.background.imagePath, theme.background.imageOpacity)
           else -> buildSolidDrawable(Color.BLACK)
       }
       
       drawableCache.put(cacheKey, drawable)
       return drawable
   }
   ```

   **Toolbar Background**:
   ```kotlin
   fun createToolbarBackground(): Drawable {
       val cacheKey = "toolbar_bg_$themeHash"
       drawableCache.get(cacheKey)?.let { return it }
       
       val palette = getPalette()
       val drawable = GradientDrawable().apply {
           setColor(palette.toolbarBg)
           cornerRadius = 0f
       }
       
       drawableCache.put(cacheKey, drawable)
       return drawable
   }
   ```

   **Suggestion Chip**:
   ```kotlin
   fun createSuggestionChip(isPressed: Boolean): Drawable {
       val cacheKey = "chip_${isPressed}_$themeHash"
       drawableCache.get(cacheKey)?.let { return it }
       
       val theme = getCurrentTheme()
       val color = if (isPressed) theme.suggestions.chip.pressed else theme.suggestions.chip.bg
       
       val drawable = GradientDrawable().apply {
           setColor(color)
           cornerRadius = theme.suggestions.chip.radius * density
       }
       
       drawableCache.put(cacheKey, drawable)
       return drawable
   }
   ```

6. **Paint Factory Methods** (Lines 355-450):

   **Key Text Paint**:
   ```kotlin
   fun createKeyTextPaint(keyType: String): Paint {
       val theme = getCurrentTheme()
       return Paint().apply {
           color = theme.keys.text
           textSize = theme.keys.font.sizeSp * density
           textAlign = Paint.Align.CENTER
           typeface = if (theme.keys.font.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
           isAntiAlias = true
       }
   }
   ```

   **Suggestion Text Paint**:
   ```kotlin
   fun createSuggestionTextPaint(): Paint {
       val palette = getPalette()
       val theme = getCurrentTheme()
       return Paint().apply {
           color = palette.suggestionText
           textSize = theme.suggestions.font.sizeSp * density
           textAlign = Paint.Align.CENTER
           typeface = if (theme.suggestions.font.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
           isAntiAlias = true
       }
   }
   ```

7. **Theme Change Listeners** (Lines 68-82):
   ```kotlin
   interface ThemeChangeListener {
       fun onThemeChanged(theme: KeyboardThemeV2, palette: ThemePaletteV2)
   }
   
   fun addThemeChangeListener(listener: ThemeChangeListener) {
       listeners.add(listener)
   }
   
   private fun notifyThemeChanged() {
       val theme = currentTheme
       val palette = currentPalette
       if (theme != null && palette != null) {
           listeners.forEach { it.onThemeChanged(theme, palette) }
       }
   }
   ```

8. **Migration Support** (Lines 115-195):
   ```kotlin
   private fun migrateOldTheme() {
       val oldThemeData = prefs.getString("flutter.current_theme_data", null)
       
       if (oldThemeData != null) {
           try {
               val oldTheme = JSONObject(oldThemeData)
               val migratedTheme = createMigratedTheme(oldTheme)
               
               saveTheme(migratedTheme)
               prefs.edit()
                   .remove("flutter.current_theme_data")
                   .remove("flutter.current_theme_id")
                   .apply()
           } catch (e: Exception) {
               loadDefaultTheme()
           }
       } else {
           loadDefaultTheme()
       }
   }
   ```

---

#### `android/app/src/main/kotlin/com/example/ai_keyboard/AIKeyboardService.kt` (6,529 lines)
**Purpose**: Main IME service - handles keyboard lifecycle and theme application

**Key Components**:

1. **Initialization** (onCreate):
   ```kotlin
   override fun onCreate() {
       super.onCreate()
       
       // Initialize theme manager
       themeManager = ThemeManager(this)
       
       // Register as theme change listener
       themeManager.addThemeChangeListener(object : ThemeManager.ThemeChangeListener {
           override fun onThemeChanged(theme: KeyboardThemeV2, palette: ThemePaletteV2) {
               mainHandler.post {
                   applyThemeImmediately()
               }
           }
       })
   }
   ```

2. **View Creation** (onCreateInputView):
   ```kotlin
   override fun onCreateInputView(): View {
       val inputView = layoutInflater.inflate(R.layout.keyboard, null)
       
       keyboardView = inputView.findViewById(R.id.keyboard)
       keyboardView?.setService(this)
       
       // Apply theme to newly created view
       applyThemeImmediately()
       
       return inputView
   }
   ```

3. **Broadcast Receiver**:
   ```kotlin
   private val settingsReceiver = object : BroadcastReceiver() {
       override fun onReceive(context: Context?, intent: Intent?) {
           when (intent?.action) {
               "com.example.ai_keyboard.THEME_CHANGED" -> {
                   Thread.sleep(50) // Ensure SharedPreferences written
                   themeManager.reload()
                   mainHandler.post {
                       applyThemeImmediately()
                   }
               }
           }
       }
   }
   ```

4. **Apply Theme Method**:
   ```kotlin
   private fun applyThemeImmediately() {
       val theme = themeManager.getCurrentTheme()
       val palette = themeManager.getPalette()
       
       // Update keyboard view
       keyboardView?.refreshTheme()
       keyboardView?.invalidate()
       
       // Update suggestion bar
       updateSuggestionBarTheme()
       
       // Update toolbar
       updateToolbarTheme()
       
       // Update emoji/media panels
       applyThemeToEmojiPanel()
       applyThemeToMediaPanel()
   }
   ```

5. **Suggestion Bar Theming**:
   ```kotlin
   private fun updateSuggestionBarTheme() {
       val palette = themeManager.getPalette()
       val theme = themeManager.getCurrentTheme()
       
       suggestionBar?.apply {
           background = themeManager.createToolbarBackground()
           
           // Update each chip
           for (i in 0 until childCount) {
               val chip = getChildAt(i) as? TextView
               chip?.apply {
                   background = themeManager.createSuggestionChip(false)
                   setTextColor(palette.suggestionText)
                   textSize = theme.suggestions.font.sizeSp
               }
           }
       }
   }
   ```

6. **Toolbar Theming**:
   ```kotlin
   private fun updateToolbarTheme() {
       val palette = themeManager.getPalette()
       
       toolbar?.apply {
           background = themeManager.createToolbarBackground()
           
           // Tint all icons
           for (i in 0 until childCount) {
               val iconView = getChildAt(i) as? ImageView
               iconView?.setColorFilter(palette.toolbarIcon)
           }
           
           layoutParams.height = (palette.toolbarHeight * density).toInt()
       }
   }
   ```

---

#### `android/app/src/main/kotlin/com/example/ai_keyboard/SwipeKeyboardView.kt`
**Purpose**: Custom view that draws the keyboard keys

**Key Components**:

1. **Initialization**:
   ```kotlin
   private lateinit var themeManager: ThemeManager
   
   fun setService(service: AIKeyboardService) {
       this.service = service
       this.themeManager = service.themeManager
       initializeFromTheme()
   }
   ```

2. **Theme Initialization**:
   ```kotlin
   private fun initializeFromTheme() {
       val theme = themeManager.getCurrentTheme()
       val palette = themeManager.getPalette()
       
       // Set background
       background = themeManager.createKeyboardBackground()
       
       // Initialize paints
       keyTextPaint = themeManager.createKeyTextPaint("normal")
       suggestionTextPaint = themeManager.createSuggestionTextPaint()
       spaceLabelPaint = Paint().apply {
           color = palette.spaceLabelColor
           textSize = 14f * density
           textAlign = Paint.Align.CENTER
       }
   }
   ```

3. **Refresh Theme**:
   ```kotlin
   fun refreshTheme() {
       initializeFromTheme()
       invalidate()
   }
   ```

4. **Drawing Keys** (onDraw):
   ```kotlin
   override fun onDraw(canvas: Canvas) {
       super.onDraw(canvas)
       
       val keyboard = keyboard ?: return
       
       // Draw each key
       for (key in keyboard.keys) {
           drawThemedKey(canvas, key)
       }
   }
   
   private fun drawThemedKey(canvas: Canvas, key: Key) {
       val keyType = getKeyType(key)
       val isPressed = isKeyPressed(key)
       
       // Get themed drawable
       val drawable = themeManager.createKeyDrawable(keyType, isPressed)
       drawable.setBounds(key.x, key.y, key.x + key.width, key.y + key.height)
       drawable.draw(canvas)
       
       // Draw key icon or text
       if (key.icon != null) {
           drawKeyIcon(canvas, key, keyType)
       } else {
           drawKeyText(canvas, key, keyType)
       }
   }
   ```

5. **Key Type Detection**:
   ```kotlin
   private fun getKeyType(key: Key): String {
       return when (key.codes?.firstOrNull()) {
           Keyboard.KEYCODE_DONE -> "enter"
           Keyboard.KEYCODE_DELETE -> "backspace"
           Keyboard.KEYCODE_SHIFT -> "shift"
           Keyboard.KEYCODE_MODE_CHANGE -> "symbols"
           -5 -> "emoji"
           -6 -> "mic"
           -7 -> "globe"
           32 -> "space"
           else -> "normal"
       }
   }
   ```

6. **Special Key Handling**:
   ```kotlin
   private fun drawKeyText(canvas: Canvas, key: Key, keyType: String) {
       val paint = themeManager.createKeyTextPaint(keyType)
       
       // Apply accent color for special keys
       val theme = themeManager.getCurrentTheme()
       if (keyType in theme.specialKeys.applyTo) {
           paint.color = theme.specialKeys.accent
       }
       
       val text = key.label?.toString() ?: ""
       val x = key.x + key.width / 2f
       val y = key.y + key.height / 2f + paint.textSize / 3f
       
       canvas.drawText(text, x, y, paint)
   }
   ```

---

#### `android/app/src/main/kotlin/com/example/ai_keyboard/MainActivity.kt` (332 lines)
**Purpose**: Bridge between Flutter and Android keyboard service

**Key Components**:

1. **MethodChannel Setup**:
   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       
       flutterEngine?.dartExecutor?.binaryMessenger?.let { messenger ->
           MethodChannel(messenger, "ai_keyboard/config").setMethodCallHandler { call, result ->
               when (call.method) {
                   "themeChanged" -> {
                       val themeId = call.argument<String>("themeId")
                       val themeName = call.argument<String>("themeName")
                       val hasData = call.argument<Boolean>("hasThemeData") ?: false
                       
                       notifyKeyboardServiceThemeChangedV2(themeId, themeName, hasData)
                       result.success(true)
                   }
                   "settingsChanged" -> {
                       sendSettingsChangedBroadcast()
                       result.success(true)
                   }
               }
           }
       }
   }
   ```

2. **Send Broadcast**:
   ```kotlin
   private fun notifyKeyboardServiceThemeChangedV2(
       themeId: String?,
       themeName: String?,
       hasData: Boolean
   ) {
       val intent = Intent("com.example.ai_keyboard.THEME_CHANGED")
       intent.putExtra("themeId", themeId ?: "")
       intent.putExtra("themeName", themeName ?: "")
       intent.putExtra("isV2", true)
       intent.putExtra("hasThemeData", hasData)
       sendBroadcast(intent)
   }
   ```

---

## Core Components

### 1. **Theme Models**

**JSON Structure**:
```json
{
  "id": "custom_1234567890",
  "name": "My Custom Theme",
  "mode": "unified",
  "background": {
    "type": "solid",
    "color": "#1B1B1F",
    "imagePath": "",
    "imageOpacity": 0.85,
    "gradient": {
      "colors": ["#2B2B2B", "#1B1B1F"],
      "orientation": "TOP_BOTTOM"
    },
    "overlayEffects": []
  },
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
    "radius": 10.0,
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
    }
  },
  "specialKeys": {
    "accent": "#FF9F1A",
    "useAccentForEnter": true,
    "applyTo": ["enter", "globe", "emoji", "mic"],
    "spaceLabelColor": "#FFFFFF"
  },
  "toolbar": {
    "inheritFromKeys": true,
    "bg": "#3A3A3F",
    "icon": "#FFFFFF",
    "heightDp": 44.0,
    "activeAccent": "#FF9F1A",
    "iconPack": "default"
  },
  "suggestions": {
    "inheritFromKeys": true,
    "bg": "#3A3A3F",
    "text": "#FFFFFF",
    "chip": {
      "bg": "#4A4A50",
      "text": "#FFFFFF",
      "pressed": "#5A5A60",
      "radius": 14.0,
      "spacingDp": 6.0
    },
    "font": {
      "family": "Roboto",
      "sizeSp": 15.0,
      "bold": false
    }
  },
  "effects": {
    "pressAnimation": "ripple"
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
    "animated": false
  },
  "advanced": {
    "livePreview": true,
    "galleryEnabled": true,
    "shareEnabled": true,
    "dynamicTheme": "none",
    "materialYouExtract": false
  }
}
```

### 2. **CleverType-Style Color Hierarchy**

**Toolbar & Suggestions → Background Color**:
```kotlin
// Bars follow Background color (CleverType style)
val toolbarBg: Int = keyboardBg  // Uses background.color
val toolbarIcon: Int = keyText   // Icons use key text for contrast

val suggestionBg: Int = keyboardBg  // Uses background.color
val suggestionText: Int = keyText   // Text uses key text for contrast

// Chips use background with subtle variations
val chipBg: Int = lightenOrDarken(keyboardBg, 0.08f)      // +8% lighter
val chipPressed: Int = lightenOrDarken(keyboardBg, 0.15f) // +15% lighter
val chipBorderColor: Int = lightenOrDarken(keyboardBg, 0.12f) // Subtle border
```

**Keys → Independent Styling**:
```kotlin
// Keys have their own independent colors
val keyBg: Int = theme.keys.bg
val keyText: Int = theme.keys.text
val keyPressed: Int = theme.keys.pressed
val keyBorder: Int = theme.keys.border.color
```

**Benefits**:
- ✅ **Clean separation** between background and keys
- ✅ **Seamless visual flow** from toolbar → suggestions → keys
- ✅ **Zero visual gaps** with matching backgrounds
- ✅ **Automatic contrast** using lightenOrDarken helper
- ✅ **CleverType/Gboard UX** achieved

### 3. **Caching System**

**Purpose**: Avoid recreating drawables/images on every draw call

**Implementation**:
```kotlin
private val drawableCache = LruCache<String, Drawable>(50)
private val imageCache = LruCache<String, Drawable>(10)

fun createKeyDrawable(keyType: String, isPressed: Boolean): Drawable {
    val cacheKey = "key_${keyType}_${isPressed}_$themeHash"
    
    // Return cached if exists
    drawableCache.get(cacheKey)?.let { return it }
    
    // Create new drawable
    val drawable = GradientDrawable().apply {
        // ... configure
    }
    
    // Cache it
    drawableCache.put(cacheKey, drawable)
    return drawable
}
```

**Cache Invalidation**:
```kotlin
if (newThemeHash != currentThemeHash) {
    drawableCache.evictAll()
    imageCache.evictAll()
    currentThemeHash = newThemeHash
}
```

---

## Theme Application Flow

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. USER EDITS THEME IN FLUTTER                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  User changes accent color in ThemeEditorScreenV2              │
│            ↓                                                    │
│  _updateTheme(newTheme) called                                 │
│            ↓                                                    │
│  setState() updates local preview                              │
│            ↓                                                    │
│  If livePreview enabled → _applyThemeToKeyboard()              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. SAVE TO SHAREDPREFERENCES                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ThemeManagerV2.saveThemeV2(theme)                             │
│            ↓                                                    │
│  theme.toJson() → Map<String, dynamic>                         │
│            ↓                                                    │
│  jsonEncode() → String                                         │
│            ↓                                                    │
│  prefs.setString('theme.v2.json', jsonStr)                     │
│            ↓                                                    │
│  Plugin adds prefix → 'flutter.theme.v2.json'                  │
│            ↓                                                    │
│  Written to: FlutterSharedPreferences.xml                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. NOTIFY ANDROID VIA METHODCHANNEL                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  MethodChannel('ai_keyboard/config').invokeMethod(             │
│    'themeChanged',                                             │
│    { themeId, themeName, hasThemeData }                        │
│  )                                                             │
│            ↓                                                    │
│  MainActivity.kt receives call                                 │
│            ↓                                                    │
│  notifyKeyboardServiceThemeChangedV2()                         │
│            ↓                                                    │
│  sendBroadcast(Intent("THEME_CHANGED"))                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. ANDROID SERVICE RECEIVES BROADCAST                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  AIKeyboardService.settingsReceiver.onReceive()                │
│            ↓                                                    │
│  Thread.sleep(50) // Wait for prefs sync                       │
│            ↓                                                    │
│  themeManager.reload()                                         │
│            ↓                                                    │
│  Reads 'flutter.theme.v2.json' from SharedPreferences          │
│            ↓                                                    │
│  KeyboardThemeV2.fromJson(jsonStr)                             │
│            ↓                                                    │
│  ThemePaletteV2(theme) created with inheritance                │
│            ↓                                                    │
│  Cache cleared (drawableCache.evictAll())                      │
│            ↓                                                    │
│  notifyThemeChanged() → listeners triggered                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. APPLY THEME TO UI COMPONENTS                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  applyThemeImmediately()                                       │
│            ↓                                                    │
│  ┌────────────────────────────────────────────┐               │
│  │ UPDATE KEYBOARD VIEW                        │               │
│  │   keyboardView.refreshTheme()              │               │
│  │   - Gets theme from themeManager           │               │
│  │   - Recreates background drawable          │               │
│  │   - Recreates paints                       │               │
│  │   keyboardView.invalidate()                │               │
│  │   - Triggers onDraw()                      │               │
│  │   - Each key gets themed drawable          │               │
│  │   - Special keys get accent color          │               │
│  └────────────────────────────────────────────┘               │
│            ↓                                                    │
│  ┌────────────────────────────────────────────┐               │
│  │ UPDATE SUGGESTION BAR                       │               │
│  │   suggestionBar.background = themed         │               │
│  │   Each chip:                               │               │
│  │     - background = createSuggestionChip()  │               │
│  │     - textColor = palette.suggestionText   │               │
│  │     - textSize = theme.font.sizeSp         │               │
│  └────────────────────────────────────────────┘               │
│            ↓                                                    │
│  ┌────────────────────────────────────────────┐               │
│  │ UPDATE TOOLBAR                              │               │
│  │   toolbar.background = themed               │               │
│  │   toolbar.height = palette.toolbarHeight    │               │
│  │   Each icon:                               │               │
│  │     - colorFilter = palette.toolbarIcon    │               │
│  │     - activeColor = palette.activeAccent   │               │
│  └────────────────────────────────────────────┘               │
│            ↓                                                    │
│  ┌────────────────────────────────────────────┐               │
│  │ UPDATE EMOJI/MEDIA PANELS                   │               │
│  │   panel.background = palette.keyboardBg     │               │
│  │   Icons tinted with palette.toolbarIcon     │               │
│  └────────────────────────────────────────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. USER SEES UPDATED KEYBOARD                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✅ Keys have new accent color                                 │
│  ✅ Suggestion bar matches                                     │
│  ✅ Toolbar matches                                            │
│  ✅ All special keys themed                                    │
│  ✅ Background applied                                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Methods & APIs

### Flutter API

#### ThemeManagerV2 (Dart)

```dart
class ThemeManagerV2 {
  // Save theme (triggers Android update)
  static Future<void> saveThemeV2(KeyboardThemeV2 theme)
  
  // Load theme from storage
  static Future<KeyboardThemeV2> loadThemeV2()
  
  // Internal: Send broadcast to Android
  static Future<void> _sendThemeBroadcast(KeyboardThemeV2 theme)
  
  // Internal: Fallback broadcast
  static Future<void> _triggerSettingsBroadcast()
}
```

#### KeyboardThemeV2 (Dart)

```dart
class KeyboardThemeV2 {
  // Create default light theme
  static KeyboardThemeV2 createDefault()
  
  // Create default dark theme
  static KeyboardThemeV2 createDefaultDark()
  
  // Parse from JSON
  factory KeyboardThemeV2.fromJson(Map<String, dynamic> json)
  
  // Convert to JSON
  Map<String, dynamic> toJson()
  
  // Create copy with modifications
  KeyboardThemeV2 copyWith({...})
}
```

### Android API

#### ThemeManager (Kotlin)

```kotlin
class ThemeManager(context: Context) {
    // Get current theme (never null)
    fun getCurrentTheme(): KeyboardThemeV2
    
    // Get derived palette
    fun getPalette(): ThemePaletteV2
    
    // Reload from SharedPreferences
    fun reload()
    
    // Save theme
    fun saveTheme(theme: KeyboardThemeV2)
    
    // Drawable factories
    fun createKeyDrawable(keyType: String, isPressed: Boolean): Drawable
    fun createKeyboardBackground(): Drawable
    fun createToolbarBackground(): Drawable
    fun createSuggestionChip(isPressed: Boolean): Drawable
    
    // Paint factories
    fun createKeyTextPaint(keyType: String): Paint
    fun createSuggestionTextPaint(): Paint
    fun createSpaceLabelPaint(): Paint
    
    // Listeners
    fun addThemeChangeListener(listener: ThemeChangeListener)
    fun removeThemeChangeListener(listener: ThemeChangeListener)
    
    interface ThemeChangeListener {
        fun onThemeChanged(theme: KeyboardThemeV2, palette: ThemePaletteV2)
    }
}
```

#### KeyboardThemeV2 (Kotlin)

```kotlin
data class KeyboardThemeV2(...) {
    companion object {
        // Parse from JSON string
        fun fromJson(jsonString: String): KeyboardThemeV2
        
        // Create default theme
        fun createDefault(): KeyboardThemeV2
        
        // Create default dark theme
        fun createDefaultDark(): KeyboardThemeV2
    }
    
    // Convert to JSON string
    fun toJson(): String
    
    // Create copy
    fun copy(...): KeyboardThemeV2
}
```

#### AIKeyboardService (Kotlin)

```kotlin
class AIKeyboardService : InputMethodService() {
    lateinit var themeManager: ThemeManager
    
    // Apply theme to all components
    private fun applyThemeImmediately()
    
    // Update specific components
    private fun updateSuggestionBarTheme()
    private fun updateToolbarTheme()
    private fun applyThemeToEmojiPanel()
    private fun applyThemeToMediaPanel()
}
```

#### SwipeKeyboardView (Kotlin)

```kotlin
class SwipeKeyboardView : KeyboardView {
    private lateinit var themeManager: ThemeManager
    
    // Initialize theme
    fun setService(service: AIKeyboardService)
    
    // Refresh when theme changes
    fun refreshTheme()
    
    // Internal: Initialize paints and background
    private fun initializeFromTheme()
    
    // Internal: Draw themed key
    private fun drawThemedKey(canvas: Canvas, key: Key)
    
    // Internal: Determine key type
    private fun getKeyType(key: Key): String
}
```

---

## Cross-Platform Communication

### SharedPreferences Bridge

**Flutter Side** (`shared_preferences` plugin):
```dart
final prefs = await SharedPreferences.getInstance();

// Write
await prefs.setString('theme.v2.json', jsonStr);
// Actual key: 'flutter.theme.v2.json' (plugin adds prefix)
```

**Android Side** (Native):
```kotlin
val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)

// Read
val json = prefs.getString("flutter.theme.v2.json", null)
```

**Key Points**:
- Flutter plugin automatically adds `"flutter."` prefix
- Both sides must access `"FlutterSharedPreferences"` file
- Use `.commit()` for immediate write, `.apply()` for async

### MethodChannel Communication

**Flutter → Android**:
```dart
const platform = MethodChannel('ai_keyboard/config');
await platform.invokeMethod('themeChanged', {
  'themeId': theme.id,
  'themeName': theme.name,
});
```

**Android → Broadcast**:
```kotlin
MethodChannel(messenger, "ai_keyboard/config").setMethodCallHandler { call, result ->
    when (call.method) {
        "themeChanged" -> {
            val intent = Intent("com.example.ai_keyboard.THEME_CHANGED")
            intent.putExtra("themeId", call.argument<String>("themeId"))
            sendBroadcast(intent)
            result.success(true)
        }
    }
}
```

**Service Receives**:
```kotlin
private val settingsReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.example.ai_keyboard.THEME_CHANGED") {
            themeManager.reload()
            applyThemeImmediately()
        }
    }
}
```

---

## Usage Examples

### Example 1: Create and Apply Custom Theme

```dart
// In Flutter app
Future<void> createCustomTheme() async {
  // Create theme
  final customTheme = KeyboardThemeV2(
    id: 'my_blue_theme',
    name: 'Ocean Blue',
    mode: 'unified',
    background: ThemeBackground(
      type: 'solid',
      color: Color(0xFF1A237E), // Dark blue
    ),
    keys: ThemeKeys(
      bg: Color(0xFF283593),
      text: Color(0xFFFFFFFF),
      pressed: Color(0xFF3949AB),
      accent: Color(0xFF448AFF), // Light blue
      // ... other properties
    ),
    specialKeys: ThemeSpecialKeys(
      accent: Color(0xFF448AFF),
      useAccentForEnter: true,
      applyTo: ['enter', 'emoji', 'mic'],
    ),
    toolbar: ThemeToolbar(
      inheritFromKeys: true, // Match key colors
    ),
    suggestions: ThemeSuggestions(
      inheritFromKeys: true, // Match key colors
    ),
    // ... other sections
  );
  
  // Save and apply
  await ThemeManagerV2.saveThemeV2(customTheme);
  
  // Show confirmation
  print('Theme applied: ${customTheme.name}');
}
```

### Example 2: Load and Modify Existing Theme

```dart
Future<void> modifyTheme() async {
  // Load current theme
  final currentTheme = await ThemeManagerV2.loadThemeV2();
  
  // Modify it
  final modifiedTheme = currentTheme.copyWith(
    name: '${currentTheme.name} (Modified)',
    specialKeys: currentTheme.specialKeys.copyWith(
      accent: Color(0xFFFF5722), // Change accent to orange
    ),
  );
  
  // Save
  await ThemeManagerV2.saveThemeV2(modifiedTheme);
}
```

### Example 3: Export Theme to File

```dart
Future<void> exportTheme(KeyboardThemeV2 theme) async {
  // Convert to JSON
  final jsonMap = theme.toJson();
  final jsonStr = JsonEncoder.withIndent('  ').convert(jsonMap);
  
  // Save to file
  final directory = await getApplicationDocumentsDirectory();
  final file = File('${directory.path}/${theme.name}.json');
  await file.writeAsString(jsonStr);
  
  print('Theme exported to: ${file.path}');
}
```

### Example 4: Import Theme from File

```dart
Future<void> importTheme(String filePath) async {
  // Read file
  final file = File(filePath);
  final jsonStr = await file.readAsString();
  
  // Parse
  final jsonMap = jsonDecode(jsonStr) as Map<String, dynamic>;
  final theme = KeyboardThemeV2.fromJson(jsonMap);
  
  // Apply
  await ThemeManagerV2.saveThemeV2(theme);
  
  print('Theme imported: ${theme.name}');
}
```

### Example 5: React to Theme Changes (Android)

```kotlin
// In AIKeyboardService or custom component
class MyKeyboardComponent {
    private val themeManager: ThemeManager
    
    init {
        // Listen for theme changes
        themeManager.addThemeChangeListener(object : ThemeManager.ThemeChangeListener {
            override fun onThemeChanged(theme: KeyboardThemeV2, palette: ThemePaletteV2) {
                // Update UI
                updateColors(palette)
                redraw()
            }
        })
    }
    
    private fun updateColors(palette: ThemePaletteV2) {
        myView.setBackgroundColor(palette.keyboardBg)
        myTextView.setTextColor(palette.keyText)
        myButton.setBackgroundColor(palette.accent)
    }
}
```

### Example 6: Create Themed Drawable in Android

```kotlin
fun createCustomButton() {
    val button = Button(context)
    
    // Get theme data
    val theme = themeManager.getCurrentTheme()
    val palette = themeManager.getPalette()
    
    // Create themed background
    val background = GradientDrawable().apply {
        setColor(palette.accent)
        cornerRadius = theme.keys.radius * density
        
        if (theme.keys.shadow.enabled) {
            elevation = theme.keys.shadow.elevationDp * density
        }
    }
    
    button.background = background
    button.setTextColor(Color.WHITE)
    button.textSize = theme.keys.font.sizeSp
}
```

---

## Best Practices

### 1. **Always Use ThemeManager Methods**
❌ **Don't**:
```kotlin
// Hardcoded color
val paint = Paint().apply {
    color = Color.parseColor("#FF5722")
}
```

✅ **Do**:
```kotlin
// Use ThemeManager
val paint = themeManager.createKeyTextPaint("normal")
```

### 2. **Leverage Inheritance**
```dart
// Toolbar and suggestions automatically match keys
ThemeToolbar(
  inheritFromKeys: true, // ✅ Automatic color coordination
)

ThemeSuggestions(
  inheritFromKeys: true, // ✅ Consistent styling
)
```

### 3. **Use Factory Methods for Consistency**
```kotlin
// All key drawables created through one method
val drawable = themeManager.createKeyDrawable(keyType, isPressed)

// Not scattered Paint() objects
```

### 4. **Invalidate Views After Theme Change**
```kotlin
themeManager.addThemeChangeListener { theme, palette ->
    keyboardView?.invalidate()
    suggestionBar?.invalidate()
    toolbar?.requestLayout()
}
```

### 5. **Handle Missing Data Gracefully**
```kotlin
// fromJson provides defaults
val theme = KeyboardThemeV2.fromJson(jsonString)
// Never crashes on missing fields
```

### 6. **Cache Expensive Operations**
```kotlin
// LRU cache prevents recreation
val drawable = drawableCache.get(cacheKey) ?: createAndCache()
```

---

## Troubleshooting

### Theme Not Applying

**Check**:
1. SharedPreferences key matches: `flutter.theme.v2.json`
2. MethodChannel broadcast sent after save
3. AIKeyboardService registered broadcast receiver
4. `applyThemeImmediately()` called in main thread

### Colors Look Wrong

**Check**:
1. Color format: `#RRGGBB` (6 hex digits)
2. Inheritance flags set correctly
3. Special key `applyTo` array includes key type
4. Contrast sufficient (use ThemePaletteV2 contrast methods)

### Themes Not Persisting

**Check**:
1. Using `.commit()` not `.apply()` for immediate write
2. `prefs.reload()` called after save
3. Delay added before reading in Android
4. File permissions correct

### Performance Issues

**Check**:
1. Drawables cached (check cache hits)
2. Not creating Paint() objects in draw loops
3. Cache size appropriate (`DRAWABLE_CACHE_SIZE`)
4. Images scaled appropriately before caching

---

## Summary

The Theme Engine V2 provides a **comprehensive, cross-platform theming system** that:

✅ **Centralizes** all theme data in a single JSON source  
✅ **Synchronizes** between Flutter UI and Android keyboard  
✅ **Caches** expensive operations for performance  
✅ **Supports** inheritance for automatic color coordination  
✅ **Validates** data with defaults for safety  
✅ **Enables** live preview and import/export  
✅ **Eliminates** hardcoded colors entirely  

**Key Files**:
- Flutter Models: `lib/theme/theme_v2.dart`
- Flutter Editor: `lib/theme/theme_editor_v2.dart`
- Android Models: `android/.../themes/ThemeModels.kt`
- Android Manager: `android/.../ThemeManager.kt`
- Android Service: `android/.../AIKeyboardService.kt`
- Android View: `android/.../SwipeKeyboardView.kt`
- Bridge: `android/.../MainActivity.kt`

**Data Flow**:  
Flutter Editor → SharedPreferences → MethodChannel → Broadcast → ThemeManager → UI Components

This architecture ensures **consistent, performant, and flexible theming** across the entire application.

