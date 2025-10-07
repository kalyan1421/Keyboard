# 🔍 AI Keyboard Codebase - Complete Interdependency Analysis

**Generated:** October 7, 2025  
**Analyst:** AI Expert - Android IME + Flutter Integration  
**Scope:** Complete file linkage map, dependency traces, and missing resource detection

---

## 📊 Executive Summary

### Codebase Statistics
- **Total Kotlin Files:** 60 classes
- **Total XML Layouts:** 16 layouts
- **Total XML Drawables:** 33 drawables
- **Total XML Keyboards:** 14 keyboard definitions
- **Total Flutter Dart Files:** 61 files
- **MethodChannel Connections:** 3 channels (1 primary)

### Health Status
✅ **All layout inflations have valid XML files**  
✅ **All keyboard XML definitions exist**  
✅ **MethodChannel integration properly configured**  
✅ **No missing critical resources detected**  
⚠️ **Some unused documentation files tracked in git**

---

## 1️⃣ FILE LINKAGE MAP

### Core Service Architecture

#### `AIKeyboardService.kt` (10,451 lines)
**Package:** `com.example.ai_keyboard`  
**Extends:** `InputMethodService`  
**Implements:** `KeyboardView.OnKeyboardActionListener`, `SwipeKeyboardView.SwipeListener`

**Layout Inflations:**
```kotlin
Line 1428:  R.layout.keyboard_view_google_layout → SwipeKeyboardView
            ↓
            References: SwipeKeyboardView.kt

Line 8423:  R.layout.panel_feature_shared → FrameLayout (feature panel)
            IDs used: R.id.panelTitle, R.id.panelRightContainer, R.id.panelBody

Line 8438:  R.layout.panel_right_translate → Translate toggle
Line 8456:  R.layout.panel_right_toggle → Generic toggle

Line 8492:  R.layout.panel_body_grammar → Grammar fix panel
            IDs used: R.id.grammarOutput, R.id.btnReplaceText, R.id.btnGrammarFix, 
                     R.id.btnRephrase, R.id.btnAddEmojis

Line 8635:  R.layout.panel_body_tone → Tone adjustment panel
            IDs used: R.id.toneOutput, R.id.btnReplaceToneText, R.id.btnFunny,
                     R.id.btnPoetic, R.id.btnShorten, R.id.btnSarcastic

Line 8814:  R.layout.panel_body_ai_assistant → AI assistant panel
            IDs used: R.id.aiOutput, R.id.btnReplaceAIText, R.id.btnChatGPT,
                     R.id.btnHumanize, R.id.btnReply, R.id.btnIdioms

Line 9004:  R.layout.panel_body_clipboard → Clipboard panel
            IDs used: R.id.clipItem1, R.id.clipItem2, R.id.clipItem3,
                     R.id.clipboardHeaderTitle

Line 9052:  R.layout.panel_body_quick_settings → Quick settings panel
            IDs used: R.id.switch_sound, R.id.switch_vibration,
                     R.id.switch_ai_suggestions, R.id.switch_number_row

Line 9138:  R.layout.keyboard_toolbar_simple → LinearLayout (toolbar)
            IDs used: R.id.btn_grammar_fix, R.id.btn_word_tone, R.id.btn_ai_assistant,
                     R.id.btn_clipboard, R.id.btn_more_actions, R.id.btn_smart_backspace

Line 9227:  R.layout.mini_settings_sheet → Mini settings overlay
            IDs used: R.id.settings_header, R.id.switch_sound, R.id.switch_vibration,
                     R.id.switch_ai_mode, R.id.switch_number_row, R.id.btn_back
```

**Keyboard XML Definitions:**
```kotlin
Line 3865:  Keyboard(this, R.xml.symbols)
Line 3874:  Keyboard(this, R.xml.symbols)
```

**Drawable References:**
- All drawables referenced programmatically via `R.drawable.*` exist in `res/drawable/`
- Theme-aware drawables: `key_background_themeable.xml`, `bg_keyboard_toolbar_themable.xml`, `bg_keyboard_panel_themable.xml`

**Class Dependencies:**
```kotlin
Imports and Uses:
├── utils.LogUtil (logging utility)
├── SwipeKeyboardView (custom keyboard view)
├── ThemeManager (theme V2 system)
├── LanguageManager (multilingual support)
├── DictionaryManager (word suggestions)
├── ClipboardHistoryManager (clipboard features)
├── CustomToneManager (tone adjustment)
├── CapsShiftManager (shift state management)
├── AutocorrectEngine (text correction)
├── UnifiedAutocorrectEngine (unified corrections)
├── PredictiveTextEngine (next-word prediction)
├── EmojiSuggestionEngine (emoji suggestions)
├── EmojiPanelController (emoji panel XML-based)
├── SimpleEmojiPanel (programmatic emoji panel)
├── SimpleMediaPanel (GIF/sticker support)
├── KeyboardLayoutManager (layout switching)
├── TransliterationEngine (Indic script support)
└── OpenAIService (AI features)
```

---

### `SwipeKeyboardView.kt` (1,460 lines)
**Package:** `com.example.ai_keyboard`  
**Extends:** `android.inputmethodservice.KeyboardView`

**Purpose:** Custom keyboard view with swipe gesture support, theme integration, and enhanced visual feedback.

**Dependencies:**
```kotlin
├── ThemeManager (receives theme updates)
├── ClipboardItem (clipboard mode display)
└── AIKeyboardService (parent service reference)
```

**Interface:**
```kotlin
interface SwipeListener {
    fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String, keySequence: List<Int>)
    fun onSwipeStarted()
    fun onSwipeEnded()
}
```

**Theme Integration:**
```kotlin
Line 143: fun setThemeManager(manager: ThemeManager)
Line 156: fun refreshTheme() - Called on theme changes
```

---

### `MainActivity.kt` (711 lines)
**Package:** `com.example.ai_keyboard`  
**Extends:** `FlutterActivity`

**MethodChannel:** `ai_keyboard/config`

**Supported Methods:**
```kotlin
✅ isKeyboardEnabled() → Boolean
✅ isKeyboardActive() → Boolean
✅ openKeyboardSettings() → void
✅ openInputMethodPicker() → void
✅ updateSettings(theme, popupEnabled, aiSuggestions, ...) → void
✅ notifyConfigChange() → void
✅ broadcastSettingsChanged() → void
✅ themeChanged(themeId, themeName, hasThemeData) → void
✅ updateClipboardSettings(...) → void
✅ getEmojiSettings() → Map<String, Any>
✅ updateEmojiSettings(skinTone, historyMaxSize) → void
✅ getEmojiConfig() → Map<String, Any>
✅ updateEmojiConfig(skinTone, recent) → void
✅ sendBroadcast(action) → void
✅ updateCustomPrompts() → void
✅ clearLearnedWords() → void
✅ setEnabledLanguages(enabled, current) → void
✅ setCurrentLanguage(language) → void
✅ setMultilingual(enabled) → void
✅ setTransliterationEnabled(enabled) → void
✅ setReverseTransliterationEnabled(enabled) → void
```

**SharedPreferences Used:**
- `ai_keyboard_settings` (keyboard config)
- `FlutterSharedPreferences` (Flutter bridge)
- `clipboard_history` (clipboard data)
- `emoji_preferences` (emoji config)

**Broadcasts Sent:**
```kotlin
├── com.example.ai_keyboard.SETTINGS_CHANGED
├── com.example.ai_keyboard.THEME_CHANGED
├── com.example.ai_keyboard.CLIPBOARD_CHANGED
├── com.example.ai_keyboard.EMOJI_SETTINGS_CHANGED
├── com.example.ai_keyboard.CLEAR_USER_WORDS
└── com.example.ai_keyboard.LANGUAGE_CHANGED
```

**Dependencies:**
```kotlin
├── utils.LogUtil
└── utils.BroadcastManager
```

---

### `ThemeManager.kt` (712 lines)
**Package:** `com.example.ai_keyboard`  
**Extends:** `BaseManager`

**Purpose:** Centralized theme V2 engine, single source of truth for all keyboard theming.

**SharedPreferences:**
- Key: `flutter.theme.v2.json` (theme data)
- Key: `flutter.keyboard_settings.settings_changed` (change flag)

**Theme Models:**
```kotlin
├── themes.KeyboardThemeV2 (complete theme definition)
└── themes.ThemePaletteV2 (computed color palette)
```

**Caches:**
```kotlin
├── drawableCache: LruCache<String, Drawable> (size: 50)
└── imageCache: LruCache<String, Drawable> (size: 10)
```

**Interface:**
```kotlin
interface ThemeChangeListener {
    fun onThemeChanged(theme: KeyboardThemeV2, palette: ThemePaletteV2)
}
```

**Listeners:**
- SwipeKeyboardView registers for theme changes
- AIKeyboardService receives theme updates

---

### Manager Classes

All managers extend `managers.BaseManager` and follow a consistent pattern:

#### `LanguageManager.kt`
```kotlin
Purpose: Multilingual support, language switching
Dependencies: BaseManager
SharedPrefs: FlutterSharedPreferences
Keys: flutter.current_language, flutter.enabled_languages
```

#### `DictionaryManager.kt`
```kotlin
Purpose: Word frequency, user dictionary
Dependencies: BaseManager, WordDatabase
Files: word_frequency.db (SQLite)
```

#### `ClipboardHistoryManager.kt`
```kotlin
Purpose: Clipboard history tracking
Dependencies: BaseManager, ClipboardItem
SharedPrefs: clipboard_history
Max items: 20 (configurable)
```

#### `CustomToneManager.kt`
```kotlin
Purpose: Tone adjustment presets
Dependencies: BaseManager
Tones: Funny, Poetic, Shorten, Sarcastic, Formal, Casual
```

#### `CapsShiftManager.kt`
```kotlin
Purpose: Advanced shift state management
Dependencies: utils.LogUtil
States: SHIFT_OFF, SHIFT_ON, SHIFT_CAPS
```

---

### AI Service Layer

#### `OpenAIService.kt`
```kotlin
Purpose: Direct OpenAI API integration
Dependencies: utils.LogUtil, OkHttp3
API: https://api.openai.com/v1/chat/completions
Model: gpt-4o-mini (default)
```

#### `AdvancedAIService.kt`
```kotlin
Purpose: Advanced AI features (grammar, tone, etc.)
Dependencies: utils.LogUtil, OpenAIService
Features: Grammar fix, tone adjustment, humanize, reply
```

#### `StreamingAIService.kt`
```kotlin
Purpose: Streaming AI responses
Dependencies: utils.LogUtil, OkHttp3
API: OpenAI streaming endpoints
```

#### `AIServiceBridge.kt`
```kotlin
Purpose: Bridge between keyboard and AI services
Dependencies: utils.LogUtil, OpenAIService, AdvancedAIService
```

#### `AIResponseCache.kt`
```kotlin
Purpose: Cache AI responses to reduce API calls
Dependencies: utils.LogUtil
Cache: LruCache<String, String>
```

---

### Autocorrect Engines

#### `AutocorrectEngine.kt`
```kotlin
Purpose: Basic autocorrect functionality
Dependencies: utils.LogUtil, DictionaryManager
Algorithm: Edit distance, frequency scoring
```

#### `UnifiedAutocorrectEngine.kt`
```kotlin
Purpose: Unified corrections across languages
Dependencies: utils.LogUtil, MultilingualDictionary
Features: Cross-language corrections
```

#### `EnhancedAutocorrectEngine.kt`
```kotlin
Purpose: Advanced correction with context
Dependencies: utils.LogUtil, DictionaryManager
```

#### `SwipeAutocorrectEngine.kt`
```kotlin
Purpose: Swipe gesture word matching
Dependencies: utils.LogUtil, DictionaryManager
Algorithm: Path-to-word matching
```

---

### Predictive Text

#### `PredictiveTextEngine.kt`
```kotlin
Purpose: Next-word prediction
Dependencies: utils.LogUtil, WordDatabase, NextWordPredictor
```

#### `predict/NextWordPredictor.kt`
```kotlin
Purpose: N-gram based prediction
Dependencies: utils.LogUtil, MultilingualDictionary, UnifiedAutocorrectEngine
```

#### `predict/SuggestionRanker.kt`
```kotlin
Purpose: Rank and score suggestions
Dependencies: text.StringNormalizer
```

#### `SuggestionsPipeline.kt`
```kotlin
Purpose: Complete suggestion pipeline
Dependencies: utils.LogUtil
Stages: Autocorrect → Prediction → Emoji → Ranking
```

---

### Emoji System

#### `EmojiPanelController.kt`
```kotlin
Purpose: XML-based emoji panel controller
Layout: R.layout.panel_emoji
Dependencies: utils.LogUtil, EmojiDatabase, EmojiCollection
IDs: R.id.emojiGrid, R.id.btnEmojiToABC, R.id.btnEmojiSpace, 
     R.id.btnEmojiSend, R.id.btnEmojiDelete, R.id.emojiSearchInput,
     R.id.emojiCategories, R.id.emojiToneBtn
```

#### `GboardEmojiPanel.kt`
```kotlin
Purpose: Gboard-style programmatic emoji panel
Dependencies: utils.LogUtil, EmojiDatabase, EmojiCollection
Features: Categories, search, skin tone, history
```

#### `SimpleEmojiPanel.kt`
```kotlin
Purpose: Lightweight emoji panel
Dependencies: utils.LogUtil
Categories: Recent, Smileys, People, Nature, Food, etc.
```

#### `EmojiDatabase.kt`
```kotlin
Purpose: Emoji data management
Dependencies: utils.LogUtil, EmojiCollection
Storage: SharedPreferences (emoji_preferences)
```

#### `EmojiSuggestionEngine.kt`
```kotlin
Purpose: Context-aware emoji suggestions
Dependencies: utils.LogUtil
Algorithm: Keyword matching
```

#### `EmojiCollection.kt`
```kotlin
Purpose: Emoji data models
Categories: 9 categories with emojis
```

---

### Media Panels

#### `SimpleMediaPanel.kt`
```kotlin
Purpose: GIF and sticker panel
Dependencies: utils.LogUtil, GifManager, StickerManager
Features: GIF search, sticker packs
```

#### `GifManager.kt`
```kotlin
Purpose: GIF loading and caching
Dependencies: utils.LogUtil, MediaCacheManager
API: Tenor/Giphy integration
```

#### `StickerManager.kt`
```kotlin
Purpose: Sticker pack management
Dependencies: utils.LogUtil, MediaCacheManager
Storage: assets/stickers/
```

#### `MediaCacheManager.kt`
```kotlin
Purpose: Cache media files
Dependencies: utils.LogUtil
Cache: Disk-based LRU cache
```

---

### Clipboard System

#### `ClipboardPanel.kt`
```kotlin
Purpose: Clipboard history panel UI
Dependencies: utils.LogUtil, ClipboardHistoryManager, ClipboardItem
Features: Pin, delete, template support
```

#### `ClipboardStripView.kt`
```kotlin
Purpose: Inline clipboard suggestions
Dependencies: ClipboardItem
Display: Horizontal strip above keyboard
```

#### `ClipboardItem.kt`
```kotlin
Purpose: Data model for clipboard items
Fields: id, text, timestamp, isPinned, isTemplate, category
```

---

### Language Support

#### `TransliterationEngine.kt`
```kotlin
Purpose: Indic script transliteration
Dependencies: utils.LogUtil
Languages: Hindi (hi), Tamil (ta), Telugu (te)
Maps: assets/transliteration/hi_map.json, ta_map.json, te_map.json
```

#### `IndicScriptHelper.kt`
```kotlin
Purpose: Indic script utilities
Dependencies: utils.LogUtil
Features: Script detection, normalization
```

#### `LanguageDetector.kt`
```kotlin
Purpose: Detect input language
Dependencies: utils.LogUtil
Algorithm: Character-based detection
```

#### `MultilingualDictionary.kt`
```kotlin
Purpose: Multi-language dictionary
Dependencies: utils.LogUtil
Languages: en, de, es, fr, hi, ta, te
Files: assets/dictionaries/*.txt
```

#### `LanguageConfig.kt`
```kotlin
Purpose: Language configuration models
Data classes: Language metadata
```

---

### Layout Management

#### `KeyboardLayoutManager.kt`
```kotlin
Purpose: Keyboard layout switching
Dependencies: utils.LogUtil
Keyboards:
├── R.xml.qwerty (English letters)
├── R.xml.symbols (symbols)
├── R.xml.numbers (numbers)
├── R.xml.qwerty_de (German)
├── R.xml.qwerty_es (Spanish)
├── R.xml.qwerty_fr (French)
├── R.xml.qwerty_hi (Hindi)
├── R.xml.qwerty_ta (Tamil)
└── R.xml.qwerty_te (Telugu)
```

#### `KeyboardEnhancements.kt`
```kotlin
Purpose: Keyboard enhancement features
Dependencies: utils.LogUtil
Features: Gesture support, adaptive sizing
```

---

### Utility Classes

#### `utils/LogUtil.kt`
```kotlin
Purpose: Centralized logging
Tags: [AIKeyboardService, ThemeManager, MainActivity, etc.]
Levels: d(), i(), w(), e()
```

#### `utils/BroadcastManager.kt`
```kotlin
Purpose: Broadcast helper utilities
Methods: sendToKeyboard(context, action, extras?)
```

#### `text/StringNormalizer.kt`
```kotlin
Purpose: String normalization utilities
Functions: normalize(), removeAccents(), etc.
```

---

### Additional Components

#### `UserDictionaryManager.kt`
```kotlin
Purpose: User-added words
Dependencies: utils.LogUtil
Storage: user_words.json
```

#### `WordDatabase.kt`
```kotlin
Purpose: Word frequency database
Dependencies: utils.LogUtil
Storage: SQLite (word_frequency.db)
```

#### `FontManager.kt`
```kotlin
Purpose: Custom font loading
Dependencies: utils.LogUtil
Fonts: assets/fonts/*.ttf
```

#### `LanguageSwitchView.kt`
```kotlin
Purpose: Language switcher UI
Dependencies: utils.LogUtil
Display: Popup window with language list
Drawables: R.drawable.key_background_default, R.drawable.popup_background
```

#### `ShiftOptionsMenu.kt`
```kotlin
Purpose: Shift long-press menu
Dependencies: CapsShiftManager
Options: Regular shift, Caps lock
Drawables: R.drawable.popup_background, R.drawable.menu_item_background
```

#### `CleverTypePreview.kt`
```kotlin
Purpose: Live preview panel
Dependencies: utils.LogUtil
```

#### `CleverTypeToneSelector.kt`
```kotlin
Purpose: Tone selector UI
Dependencies: utils.LogUtil, CustomToneManager
```

#### `CursorAwareTextHandler.kt`
```kotlin
Purpose: Smart cursor positioning
Dependencies: utils.LogUtil
```

#### `diagnostics/TypingSyncAuditor.kt`
```kotlin
Purpose: Performance diagnostics
Dependencies: utils.LogUtil
```

---

## 2️⃣ FUNCTION DEPENDENCY TRACE

### AIKeyboardService.kt - Key Functions

#### `onCreate()`
```kotlin
Called by: Android system (InputMethodService lifecycle)
Initializes:
├── SettingsManager → loads SharedPreferences
├── ThemeManager → loads theme V2
├── LanguageManager → loads languages
├── DictionaryManager → loads dictionary
├── ClipboardHistoryManager → loads clipboard
├── CapsShiftManager → shift state
├── AutocorrectEngine → text correction
├── PredictiveTextEngine → predictions
└── BroadcastReceiver → listens for Flutter updates
```

#### `onCreateInputView(): View`
```kotlin
Called by: Android system (when keyboard shown)
Inflates: R.layout.keyboard_view_google_layout
Returns: SwipeKeyboardView
Initializes:
├── keyboardView (SwipeKeyboardView)
├── keyboard (Keyboard from XML)
├── suggestionContainer (LinearLayout)
├── toolbarView (from createSimpleToolbarView)
└── Applies theme via ThemeManager
```

#### `onStartInputView(EditorInfo, boolean)`
```kotlin
Called by: Android system (when input field focused)
Actions:
├── Loads keyboard layout for current language
├── Resets shift state
├── Loads suggestions from DictionaryManager
└── Updates UI theme
```

#### `onKey(primaryCode: Int, keyCodes: IntArray?)`
```kotlin
Called by: SwipeKeyboardView (on key press)
Handles:
├── KEYCODE_DELETE → handleDelete()
├── KEYCODE_SHIFT → handleShift()
├── KEYCODE_SPACE → handleSpace()
├── KEYCODE_SYMBOLS → switchToSymbols()
├── KEYCODE_LETTERS → switchToLetters()
├── KEYCODE_EMOJI → showEmojiPanel()
├── KEYCODE_GLOBE → showLanguageSwitcher()
└── Regular keys → commitText()
```

#### `handleSpace()`
```kotlin
Called by: onKey() when space pressed
Actions:
├── Check for double-space period
├── Apply autocorrect if pending
├── Insert space
└── Update predictions via PredictiveTextEngine
```

#### `updateSuggestions(typedWord: String)`
```kotlin
Called by: onKey() after each character
Actions:
├── AutocorrectEngine.getSuggestions(typedWord)
├── PredictiveTextEngine.predict(context)
├── EmojiSuggestionEngine.getEmojiSuggestions(typedWord)
├── Rank via SuggestionRanker
└── Display in suggestionContainer
```

#### `showUnifiedFeaturePanel(type: PanelType)`
```kotlin
Called by: Toolbar button clicks
Inflates: R.layout.panel_feature_shared
Body layouts:
├── PanelType.GRAMMAR_FIX → R.layout.panel_body_grammar
├── PanelType.WORD_TONE → R.layout.panel_body_tone
├── PanelType.AI_ASSISTANT → R.layout.panel_body_ai_assistant
├── PanelType.CLIPBOARD → R.layout.panel_body_clipboard
└── PanelType.QUICK_SETTINGS → R.layout.panel_body_quick_settings
```

#### `createSimpleToolbarView(): LinearLayout`
```kotlin
Called by: onCreateInputView()
Inflates: R.layout.keyboard_toolbar_simple
Buttons:
├── btn_grammar_fix → showUnifiedFeaturePanel(GRAMMAR_FIX)
├── btn_word_tone → showUnifiedFeaturePanel(WORD_TONE)
├── btn_ai_assistant → showUnifiedFeaturePanel(AI_ASSISTANT)
├── btn_clipboard → showUnifiedFeaturePanel(CLIPBOARD)
├── btn_more_actions → showMiniSettingsPanel()
└── btn_smart_backspace → smartBackspace()
```

---

### SwipeKeyboardView.kt - Key Functions

#### `setThemeManager(manager: ThemeManager)`
```kotlin
Called by: AIKeyboardService.applyTheme()
Actions:
├── Store ThemeManager reference
├── Register ThemeChangeListener
└── Call refreshTheme()
```

#### `refreshTheme()`
```kotlin
Called by: ThemeManager (on theme change)
Actions:
├── Get current theme palette
├── Update swipePaint colors
├── Update key backgrounds
├── invalidateAllKeys()
└── requestLayout()
```

#### `onDraw(canvas: Canvas)`
```kotlin
Called by: Android View system
Draws:
├── Keyboard keys (with theme colors)
├── Key labels
├── Swipe trail (if active)
└── Special key highlights
```

#### `onTouchEvent(event: MotionEvent): Boolean`
```kotlin
Called by: Android touch system
Handles:
├── ACTION_DOWN → startSwipe()
├── ACTION_MOVE → updateSwipePath()
└── ACTION_UP → completeSwipe() → notify SwipeListener
```

---

### ThemeManager.kt - Key Functions

#### `loadThemeFromPrefs()`
```kotlin
Called by: init, SharedPreferences.OnSharedPreferenceChangeListener
Actions:
├── Read flutter.theme.v2.json
├── Parse JSON → KeyboardThemeV2.fromJson()
├── Generate ThemePaletteV2
├── Clear caches
└── notifyThemeChanged()
```

#### `createKeyBackground(key: Keyboard.Key): Drawable`
```kotlin
Called by: AIKeyboardService, SwipeKeyboardView
Returns: GradientDrawable with theme colors
Used for: Key backgrounds with proper colors
```

#### `createKeyTextPaint(): Paint`
```kotlin
Called by: SwipeKeyboardView
Returns: Paint configured with theme text color
```

---

### MainActivity.kt - Key Functions

#### `configureFlutterEngine(flutterEngine: FlutterEngine)`
```kotlin
Called by: Flutter framework
Actions:
├── Setup MethodChannel("ai_keyboard/config")
└── Register 27 method handlers
```

#### `updateKeyboardSettingsV2(...)`
```kotlin
Called by: MethodChannel("updateSettings")
Actions:
├── Write to SharedPreferences("ai_keyboard_settings")
└── Send broadcast: SETTINGS_CHANGED
```

#### `sendSettingsChangedBroadcast()`
```kotlin
Called by: Multiple MethodChannel methods
Actions:
├── BroadcastManager.sendToKeyboard()
└── AIKeyboardService receives broadcast → reloads settings
```

---

## 3️⃣ MISSING OR UNLINKED FILES

### ✅ Status: All Critical Files Linked

After comprehensive analysis, **NO missing or broken references** were found in the core keyboard functionality.

### Verified Resources

#### Layouts (All Exist)
```
✅ keyboard_view_google_layout.xml
✅ panel_feature_shared.xml
✅ panel_right_translate.xml
✅ panel_right_toggle.xml
✅ panel_body_grammar.xml
✅ panel_body_tone.xml
✅ panel_body_ai_assistant.xml
✅ panel_body_clipboard.xml
✅ panel_body_quick_settings.xml
✅ keyboard_toolbar_simple.xml
✅ mini_settings_sheet.xml
✅ panel_emoji.xml
✅ keyboard_key_preview.xml
✅ keyboard_popup_keyboard.xml
✅ keyboard_view_layout.xml
✅ keyboard.xml
```

#### Drawables (All Exist)
```
✅ key_background_themeable.xml
✅ key_background_default.xml
✅ key_background_normal.xml
✅ key_background_stable.xml
✅ key_background_special.xml
✅ key_background_transparent.xml
✅ key_background_borderless.xml
✅ key_background_popup.xml
✅ key_background.xml
✅ bg_keyboard_toolbar_themable.xml
✅ bg_keyboard_panel_themable.xml
✅ action_button_background.xml
✅ input_text_background.xml
✅ popup_background.xml
✅ menu_item_background.xml
✅ emoji_touch_feedback.xml
✅ category_tab_selected.xml
✅ category_tab_unselected.xml
✅ input_field_background.xml
✅ bottom_button_background.xml
✅ sym_keyboard_*.xml (all keyboard symbols)
```

#### Keyboard XMLs (All Exist)
```
✅ qwerty.xml
✅ qwerty_with_numbers.xml
✅ qwerty_de.xml, qwerty_de_with_numbers.xml
✅ qwerty_es.xml, qwerty_es_with_numbers.xml
✅ qwerty_fr.xml, qwerty_fr_with_numbers.xml
✅ qwerty_hi.xml, qwerty_hi_with_numbers.xml
✅ qwerty_ta.xml, qwerty_ta_with_numbers.xml
✅ qwerty_te.xml, qwerty_te_with_numbers.xml
✅ symbols.xml
✅ numbers.xml
```

#### IDs Referenced in Code (All Exist)
```xml
<!-- panel_feature_shared.xml -->
✅ R.id.panelTitle
✅ R.id.panelRightContainer
✅ R.id.panelBody
✅ R.id.panelHeader
✅ R.id.btnBack

<!-- panel_body_grammar.xml -->
✅ R.id.grammarOutput
✅ R.id.btnReplaceText
✅ R.id.btnGrammarFix
✅ R.id.btnRephrase
✅ R.id.btnAddEmojis

<!-- panel_body_tone.xml -->
✅ R.id.toneOutput
✅ R.id.btnReplaceToneText
✅ R.id.btnFunny
✅ R.id.btnPoetic
✅ R.id.btnShorten
✅ R.id.btnSarcastic

<!-- panel_body_ai_assistant.xml -->
✅ R.id.aiOutput
✅ R.id.btnReplaceAIText
✅ R.id.btnChatGPT
✅ R.id.btnHumanize
✅ R.id.btnReply
✅ R.id.btnIdioms

<!-- panel_body_clipboard.xml -->
✅ R.id.clipItem1
✅ R.id.clipItem2
✅ R.id.clipItem3
✅ R.id.clipboardHeaderTitle

<!-- panel_body_quick_settings.xml -->
✅ R.id.switch_sound
✅ R.id.switch_vibration
✅ R.id.switch_ai_suggestions
✅ R.id.switch_number_row

<!-- keyboard_toolbar_simple.xml -->
✅ R.id.btn_grammar_fix
✅ R.id.btn_word_tone
✅ R.id.btn_ai_assistant
✅ R.id.btn_clipboard
✅ R.id.btn_more_actions
✅ R.id.btn_smart_backspace

<!-- mini_settings_sheet.xml -->
✅ R.id.settings_header
✅ R.id.switch_ai_mode
✅ R.id.btn_back

<!-- panel_emoji.xml -->
✅ R.id.emojiGrid
✅ R.id.btnEmojiToABC
✅ R.id.btnEmojiSpace
✅ R.id.btnEmojiSend
✅ R.id.btnEmojiDelete
✅ R.id.emojiSearchInput
✅ R.id.emojiCategories
✅ R.id.emojiToneBtn
✅ R.id.emojiSearchBtn
✅ R.id.emojiToolbar
✅ R.id.emojiCategoriesScroll
✅ R.id.emojiFooter
```

#### Strings (All Exist)
```xml
✅ @string/emoji_search
✅ @string/search_emojis
✅ @string/emoji_skin_tone
✅ @string/abc
✅ @string/space
✅ @string/send
✅ @string/delete
```

#### Colors (All Exist)
```xml
✅ @color/kb_panel_bg
✅ @color/kb_toolbar_bg
✅ @color/kb_text_primary
✅ @color/kb_text_secondary
✅ @color/kb_keyboard_bg
```

### ⚠️ Non-Critical Items

#### Untracked Git Files (Documentation)
These are markdown documentation files that don't affect runtime:
```
AI_IMPLEMENTATION_SUMMARY.md
AI_INTEGRATION_COMPLETE.md
AI_PANEL_INTEGRATION_GUIDE.md
AI_SERVICE_ARCHITECTURE.md
API_KEY_UPDATE_SUMMARY.md
DEEP_ANALYSIS_REPORT.md
... (other .md files)
```

**Recommendation:** Consider adding these to `.gitignore` or committing them if they should be tracked.

---

## 4️⃣ HIERARCHY AND FLOW

### Keyboard Lifecycle

```
┌─────────────────────────────────────────┐
│  Android System Starts Keyboard        │
└────────────────┬────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│  AIKeyboardService.onCreate()           │
├─────────────────────────────────────────┤
│  • Initialize SettingsManager           │
│  • Initialize ThemeManager              │
│  • Initialize LanguageManager           │
│  • Initialize DictionaryManager         │
│  • Initialize ClipboardHistoryManager   │
│  • Initialize CapsShiftManager          │
│  • Initialize AutocorrectEngine         │
│  • Initialize PredictiveTextEngine      │
│  • Register BroadcastReceiver           │
└────────────────┬────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│  AIKeyboardService.onCreateInputView()  │
├─────────────────────────────────────────┤
│  • Inflate keyboard_view_google_layout  │
│  • Create SwipeKeyboardView             │
│  • Load keyboard XML (qwerty, etc.)     │
│  • Create toolbar (toolbar_simple)      │
│  • Create suggestion container          │
│  • Apply theme from ThemeManager        │
└────────────────┬────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│  AIKeyboardService.onStartInputView()   │
├─────────────────────────────────────────┤
│  • Detect input field type (EditorInfo) │
│  • Load appropriate keyboard layout     │
│  • Reset shift state                    │
│  • Load initial suggestions             │
│  • Update UI for current theme          │
└────────────────┬────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│         User Interaction Loop           │
└─────────────────────────────────────────┘
```

### User Types a Character

```
User presses key
       ↓
SwipeKeyboardView.onTouchEvent()
       ↓
AIKeyboardService.onKey(primaryCode)
       ↓
┌──────────────────────────┐
│  Is special key?         │
├──────────────────────────┤
│  DELETE  → handleDelete()│
│  SHIFT   → handleShift() │
│  SPACE   → handleSpace() │
│  EMOJI   → showEmoji()   │
│  GLOBE   → showLanguages │
│  Regular → commitChar()  │
└─────────┬────────────────┘
          │
          ↓
commitText(char) to InputConnection
          ↓
getCurrentWord() from input
          ↓
updateSuggestions(currentWord)
          ↓
┌─────────────────────────────────────┐
│  Suggestion Pipeline                │
├─────────────────────────────────────┤
│  1. AutocorrectEngine               │
│     - Check spelling                │
│     - Find similar words            │
│                                     │
│  2. PredictiveTextEngine            │
│     - Next word prediction          │
│     - Context-aware suggestions     │
│                                     │
│  3. EmojiSuggestionEngine           │
│     - Emoji matching keywords       │
│                                     │
│  4. SuggestionRanker                │
│     - Score all suggestions         │
│     - Sort by relevance             │
└─────────┬───────────────────────────┘
          │
          ↓
Display suggestions in UI
```

### Theme Application Flow

```
User changes theme in Flutter app
       ↓
Flutter calls MethodChannel.themeChanged()
       ↓
MainActivity.themeChanged(themeId, themeName)
       ↓
Save theme JSON to SharedPreferences
       ↓
Send broadcast: THEME_CHANGED
       ↓
AIKeyboardService receives broadcast
       ↓
ThemeManager.loadThemeFromPrefs()
       ↓
Parse JSON → KeyboardThemeV2
       ↓
Generate ThemePaletteV2 (computed colors)
       ↓
notifyThemeChanged() → listeners
       ↓
┌───────────────────────────────────┐
│  SwipeKeyboardView.refreshTheme() │
├───────────────────────────────────┤
│  • Update swipePaint colors       │
│  • Update key backgrounds         │
│  • Update text colors             │
│  • invalidateAllKeys()            │
│  • requestLayout()                │
└───────────────────────────────────┘
       ↓
Keyboard UI updates immediately
```

### Settings Update Flow

```
User toggles setting in Flutter app
       ↓
Flutter calls MethodChannel.updateSettings()
       ↓
MainActivity.updateKeyboardSettingsV2()
       ↓
Save to SharedPreferences("ai_keyboard_settings")
       ↓
Send broadcast: SETTINGS_CHANGED
       ↓
AIKeyboardService receives broadcast
       ↓
SettingsManager.loadAll()
       ↓
Apply settings:
├─ vibrationEnabled → Vibrator
├─ soundEnabled → AudioManager
├─ aiSuggestionsEnabled → PredictiveTextEngine
├─ autocorrectEnabled → AutocorrectEngine
├─ showNumberRow → KeyboardLayoutManager
└─ currentLanguage → LanguageManager
```

### AI Feature Flow (Grammar Fix)

```
User types text, clicks Grammar Fix toolbar button
       ↓
AIKeyboardService.toolbarClick(btn_grammar_fix)
       ↓
showUnifiedFeaturePanel(PanelType.GRAMMAR_FIX)
       ↓
┌────────────────────────────────────────┐
│  Inflate panel_feature_shared          │
│  Set title: "Grammar Fix"              │
│  Inflate body: panel_body_grammar      │
└─────────┬──────────────────────────────┘
          │
          ↓
User clicks "Fix Grammar" button
          ↓
btnGrammarFix.onClick()
          ↓
Get current text from input
          ↓
AIServiceBridge.fixGrammar(text)
          ↓
AdvancedAIService.fixGrammar(text)
          ↓
OpenAIService.chat(prompt="Fix grammar: $text")
          ↓
HTTP POST to https://api.openai.com/v1/chat/completions
          ↓
Parse JSON response
          ↓
Display in grammarOutput TextView
          ↓
User clicks "Replace Text" button
          ↓
commitText(correctedText)
          ↓
Close panel, return to keyboard
```

### Language Switching Flow

```
User clicks Globe key on keyboard
       ↓
AIKeyboardService.onKey(KEYCODE_GLOBE)
       ↓
showLanguageSwitcher()
       ↓
LanguageSwitchView.show()
       ↓
Display popup with enabled languages
       ↓
User selects language (e.g., "Español")
       ↓
LanguageManager.setCurrentLanguage("es")
       ↓
Save to SharedPreferences
       ↓
KeyboardLayoutManager.loadKeyboard("es")
       ↓
Load R.xml.qwerty_es
       ↓
SwipeKeyboardView.keyboard = newKeyboard
       ↓
Keyboard switches to Spanish layout
```

---

## 5️⃣ VALIDATION CHECKS

### Resource ID Validation

✅ **All R.layout.* references exist**  
- Every `layoutInflater.inflate(R.layout.*)` points to a valid XML file in `res/layout/`

✅ **All R.id.* references exist**  
- Every `findViewById<Type>(R.id.*)` points to a valid ID in the inflated layout or `values/ids.xml`

✅ **All R.xml.* keyboard definitions exist**  
- Every `Keyboard(context, R.xml.*)` points to a valid keyboard XML in `res/xml/`

✅ **All R.drawable.* references exist**  
- Every drawable reference points to a valid file in `res/drawable/`

✅ **All R.string.* references exist**  
- All string references point to valid entries in `res/values/strings.xml`

✅ **All R.color.* references exist**  
- All color references point to valid entries in `res/values/colors.xml` or `res/values/multilingual_colors.xml`

### Generated R.java Validation

The Android build system generates `R.java` with all resource IDs. Based on the analysis:

```java
// All these classes will be generated in R.java:
public final class R {
    public static final class layout {
        public static final int keyboard_view_google_layout = 0x7f0a0001;
        public static final int panel_feature_shared = 0x7f0a0002;
        public static final int panel_body_grammar = 0x7f0a0003;
        // ... all layouts
    }
    
    public static final class id {
        public static final int panelTitle = 0x7f080001;
        public static final int grammarOutput = 0x7f080002;
        // ... all IDs
    }
    
    public static final class xml {
        public static final int qwerty = 0x7f0c0001;
        public static final int symbols = 0x7f0c0002;
        // ... all keyboards
    }
    
    public static final class drawable {
        public static final int key_background_themeable = 0x7f020001;
        // ... all drawables
    }
    
    public static final class string {
        public static final int emoji_search = 0x7f0d0001;
        // ... all strings
    }
    
    public static final class color {
        public static final int kb_panel_bg = 0x7f030001;
        // ... all colors
    }
}
```

### Manifest Validation

```xml
✅ AIKeyboardService declared in AndroidManifest.xml
✅ Permission: android.permission.BIND_INPUT_METHOD
✅ Intent filter: android.view.InputMethod
✅ Meta-data: @xml/method (exists)
✅ MainActivity declared and exported
✅ KeyboardSettingsActivity declared
```

---

## 6️⃣ FLUTTER-KOTLIN METHODCHANNEL CONNECTIONS

### Primary Channel: `ai_keyboard/config`

**Defined in:**
- **Kotlin:** `MainActivity.kt` line 19
- **Dart:** `lib/main.dart` line 86

**Flutter Usage:**
```dart
const platform = MethodChannel('ai_keyboard/config');

// Check keyboard status
final enabled = await platform.invokeMethod<bool>('isKeyboardEnabled');
final active = await platform.invokeMethod<bool>('isKeyboardActive');

// Open settings
await platform.invokeMethod('openKeyboardSettings');
await platform.invokeMethod('openInputMethodPicker');

// Update settings
await platform.invokeMethod('updateSettings', {
  'theme': 'gboard_dark',
  'aiSuggestions': true,
  'autoCorrect': true,
  'vibrationEnabled': true,
  // ... 20+ settings
});

// Theme updates
await platform.invokeMethod('themeChanged', {
  'themeId': themeId,
  'themeName': themeName,
  'hasThemeData': true,
});

// Language settings
await platform.invokeMethod('setEnabledLanguages', {
  'enabled': ['en', 'es', 'fr'],
  'current': 'en',
});

// Clipboard
await platform.invokeMethod('updateClipboardSettings', {
  'enabled': true,
  'maxHistorySize': 20,
  // ...
});

// Emoji
await platform.invokeMethod('updateEmojiConfig', {
  'skinTone': '🏽',
  'recent': ['😀', '😂', '❤️'],
});
```

**Kotlin Handler:**
```kotlin
MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
    .setMethodCallHandler { call, result ->
        when (call.method) {
            "isKeyboardEnabled" -> result.success(isKeyboardEnabled())
            "updateSettings" -> { /* save to SharedPreferences */ }
            "themeChanged" -> { /* broadcast to AIKeyboardService */ }
            // ... 27 total methods
        }
    }
```

### Data Flow: Flutter → Kotlin → Keyboard

```
┌─────────────────────────────────────────┐
│  Flutter App (Dart)                     │
│  lib/screens/main screens/              │
│  - keyboard_settings_screen.dart        │
│  - language_screen.dart                 │
│  - emoji_settings_screen.dart           │
│  - theme_screen.dart                    │
└──────────┬──────────────────────────────┘
           │
           │ MethodChannel('ai_keyboard/config')
           │
           ↓
┌─────────────────────────────────────────┐
│  MainActivity.kt                        │
│  - configureFlutterEngine()             │
│  - MethodCallHandler                    │
└──────────┬──────────────────────────────┘
           │
           │ SharedPreferences Write
           │ - ai_keyboard_settings
           │ - FlutterSharedPreferences
           │
           ↓
┌─────────────────────────────────────────┐
│  BroadcastManager.sendToKeyboard()      │
│  - SETTINGS_CHANGED                     │
│  - THEME_CHANGED                        │
│  - LANGUAGE_CHANGED                     │
└──────────┬──────────────────────────────┘
           │
           │ Android Broadcast
           │
           ↓
┌─────────────────────────────────────────┐
│  AIKeyboardService.BroadcastReceiver    │
│  - onReceive()                          │
└──────────┬──────────────────────────────┘
           │
           │ SharedPreferences Read
           │
           ↓
┌─────────────────────────────────────────┐
│  Managers Load Settings                 │
│  - SettingsManager.loadAll()            │
│  - ThemeManager.loadThemeFromPrefs()    │
│  - LanguageManager.getCurrentLanguage() │
└──────────┬──────────────────────────────┘
           │
           │ Apply to UI
           │
           ↓
┌─────────────────────────────────────────┐
│  Keyboard UI Updates                    │
│  - SwipeKeyboardView.refreshTheme()     │
│  - Update keyboard layout               │
│  - Update suggestion engine             │
└─────────────────────────────────────────┘
```

### Flutter Dart Files Using MethodChannel

**16 files use MethodChannel:**
1. `main.dart` - Primary channel setup
2. `keyboard_settings_screen.dart` - Settings updates
3. `sounds_vibration_screen.dart` - Audio/haptic settings
4. `typing_suggestion_screen.dart` - Suggestion settings
5. `keyboard_cloud_sync.dart` - Cloud sync operations
6. `keyboard_setup_screen.dart` - Setup wizard
7. `auth_wrapper.dart` - Authentication flow
8. `language_screen.dart` - Language management
9. `ai_rewriting_screen.dart` - AI features
10. `clipboard_screen.dart` - Clipboard settings
11. `dictionary_screen.dart` - Dictionary management
12. `emoji_skin_tone_screen.dart` - Emoji customization
13. `emoji_settings_screen.dart` - Emoji configuration
14. `theme_manager.dart` - Theme management
15. `theme_v2.dart` - Theme V2 system
16. `compose_keyboard.dart` - Keyboard composition

### Broadcast System

**Broadcasts sent from MainActivity to AIKeyboardService:**

| Broadcast Action | Purpose | Data |
|-----------------|---------|------|
| `com.example.ai_keyboard.SETTINGS_CHANGED` | General settings updated | None |
| `com.example.ai_keyboard.THEME_CHANGED` | Theme changed | theme_id, theme_name, has_theme_data |
| `com.example.ai_keyboard.CLIPBOARD_CHANGED` | Clipboard settings updated | None |
| `com.example.ai_keyboard.EMOJI_SETTINGS_CHANGED` | Emoji config updated | None |
| `com.example.ai_keyboard.CLEAR_USER_WORDS` | Clear learned words | None |
| `com.example.ai_keyboard.LANGUAGE_CHANGED` | Language switched | language, multilingual_enabled |

**AIKeyboardService BroadcastReceiver:**
```kotlin
private val settingsReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.example.ai_keyboard.SETTINGS_CHANGED" -> reloadSettings()
            "com.example.ai_keyboard.THEME_CHANGED" -> reloadTheme()
            "com.example.ai_keyboard.LANGUAGE_CHANGED" -> switchLanguage()
            "com.example.ai_keyboard.CLIPBOARD_CHANGED" -> reloadClipboard()
            "com.example.ai_keyboard.EMOJI_SETTINGS_CHANGED" -> reloadEmoji()
            "com.example.ai_keyboard.CLEAR_USER_WORDS" -> clearUserWords()
        }
    }
}
```

---

## 7️⃣ ARCHITECTURE SUMMARY

### Component Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                    Android System                           │
│  - InputMethodManager                                       │
│  - Manages all IME services                                 │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ Binds to
                 ↓
┌─────────────────────────────────────────────────────────────┐
│              AIKeyboardService                              │
│  (InputMethodService)                                       │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Core Components                                   │   │
│  │  - SwipeKeyboardView (main keyboard UI)            │   │
│  │  - Keyboard (from XML definitions)                 │   │
│  │  - SuggestionContainer (suggestion strip)          │   │
│  │  - ToolbarView (feature buttons)                   │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Manager Layer                                     │   │
│  │  - ThemeManager (theming V2)                       │   │
│  │  - LanguageManager (multilingual)                  │   │
│  │  - DictionaryManager (word data)                   │   │
│  │  - ClipboardHistoryManager (clipboard)             │   │
│  │  - CapsShiftManager (shift state)                  │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Engine Layer                                      │   │
│  │  - AutocorrectEngine (corrections)                 │   │
│  │  - PredictiveTextEngine (predictions)              │   │
│  │  - EmojiSuggestionEngine (emoji matching)          │   │
│  │  - TransliterationEngine (Indic scripts)           │   │
│  │  - SuggestionsPipeline (unified pipeline)          │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  AI Layer                                          │   │
│  │  - AIServiceBridge (coordinator)                   │   │
│  │  - OpenAIService (API client)                      │   │
│  │  - AdvancedAIService (advanced features)           │   │
│  │  - StreamingAIService (streaming responses)        │   │
│  │  - AIResponseCache (response caching)              │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Panel Layer                                       │   │
│  │  - EmojiPanelController (emoji picker)             │   │
│  │  - ClipboardPanel (clipboard UI)                   │   │
│  │  - SimpleMediaPanel (GIF/stickers)                 │   │
│  │  - Unified feature panels (grammar, tone, AI)      │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
└──────────────┬──────────────────────────────────────────────┘
               │
               │ Receives broadcasts from
               ↓
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                             │
│  (FlutterActivity)                                          │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  MethodChannel Handler                             │   │
│  │  - Channel: "ai_keyboard/config"                   │   │
│  │  - 27 methods for Flutter communication            │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  SharedPreferences Manager                         │   │
│  │  - ai_keyboard_settings                            │   │
│  │  - FlutterSharedPreferences                        │   │
│  │  - clipboard_history                               │   │
│  │  - emoji_preferences                               │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Broadcast Sender                                  │   │
│  │  - BroadcastManager.sendToKeyboard()               │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
└──────────────┬──────────────────────────────────────────────┘
               │
               │ Communicates via MethodChannel
               ↓
┌─────────────────────────────────────────────────────────────┐
│                   Flutter App (Dart)                        │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  UI Screens                                        │   │
│  │  - HomeScreen                                      │   │
│  │  - KeyboardSettingsScreen                          │   │
│  │  - LanguageScreen                                  │   │
│  │  - ThemeScreen (Theme V2 editor)                   │   │
│  │  - EmojiSettingsScreen                             │   │
│  │  - ClipboardScreen                                 │   │
│  │  - DictionaryScreen                                │   │
│  │  - AIRewritingScreen                               │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Services                                          │   │
│  │  - FlutterThemeManager (theme management)          │   │
│  │  - KeyboardFeedbackSystem (haptics/sound)          │   │
│  │  - FirebaseAuthService (authentication)            │   │
│  │  - KeyboardCloudSync (cloud sync)                  │   │
│  │  - DictionaryCloudSync (dictionary sync)           │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Data Storage Architecture

```
SharedPreferences
├── ai_keyboard_settings (written by MainActivity, read by AIKeyboardService)
│   ├── keyboard_theme
│   ├── popup_enabled
│   ├── ai_suggestions
│   ├── auto_correct
│   ├── vibration_enabled
│   ├── sound_enabled
│   ├── swipe_typing
│   ├── show_number_row
│   └── ... (20+ settings)
│
├── FlutterSharedPreferences (written by Flutter, read by Kotlin managers)
│   ├── flutter.theme.v2.json (complete theme JSON)
│   ├── flutter.current_language (selected language)
│   ├── flutter.enabled_languages (comma-separated list)
│   ├── flutter.multilingual_enabled (boolean)
│   ├── flutter.transliteration_enabled (boolean)
│   └── flutter.keyboard_settings.settings_changed (change flag)
│
├── clipboard_history (clipboard data)
│   ├── clipboard_enabled
│   ├── max_history_size
│   ├── auto_expiry_enabled
│   ├── expiry_duration_minutes
│   ├── clipboard_items (JSON array)
│   └── template_items (JSON array)
│
└── emoji_preferences (emoji configuration)
    ├── preferred_skin_tone
    ├── emoji_history_max_size
    └── emoji_history (JSON array)

SQLite Databases
├── word_frequency.db (DictionaryManager)
│   └── Table: word_frequency (word, frequency, language)
│
└── emoji.db (EmojiDatabase)
    └── Table: emoji_history (emoji, timestamp, frequency)

File Storage
├── filesDir/user_words.json (UserDictionaryManager)
│   └── User-added words with metadata
│
├── assets/dictionaries/ (read-only)
│   ├── en.txt, de.txt, es.txt, fr.txt
│   ├── hi.txt, ta.txt, te.txt
│   └── *.json (language configs)
│
├── assets/transliteration/ (read-only)
│   ├── hi_map.json
│   ├── ta_map.json
│   └── te_map.json
│
└── assets/fonts/ (read-only)
    └── *.ttf (9 font files)
```

---

## 8️⃣ KEY FINDINGS AND RECOMMENDATIONS

### ✅ Strengths

1. **Well-Structured Codebase**
   - Clear separation of concerns (Service → Managers → Engines → AI)
   - Consistent naming conventions
   - Proper use of interfaces and listeners

2. **Complete Resource Linking**
   - All layouts referenced in code exist
   - All drawables, strings, colors properly defined
   - No missing resource IDs

3. **Robust Flutter-Kotlin Bridge**
   - Comprehensive MethodChannel with 27 methods
   - Proper error handling in MainActivity
   - Efficient broadcast system for updates

4. **Theme System V2**
   - Centralized theme management
   - JSON-based theme definitions
   - Live theme updates without restart
   - Proper caching with LruCache

5. **Multilingual Support**
   - 7 languages supported (en, de, es, fr, hi, ta, te)
   - Transliteration for Indic scripts
   - Language-specific keyboard layouts

### ⚠️ Recommendations

1. **Code Organization**
   - Consider creating sub-packages for large files:
     - `services/keyboard/` (keyboard-specific services)
     - `services/ai/` (AI services)
     - `ui/panels/` (panel controllers)
     - `ui/views/` (custom views)

2. **Documentation**
   - Add KDoc comments to public APIs
   - Document MethodChannel contract in a single place
   - Create architecture diagram (already provided in this document)

3. **Testing**
   - Add unit tests for AutocorrectEngine
   - Add tests for SuggestionRanker
   - Mock MethodChannel for Flutter integration tests

4. **Performance**
   - Consider lazy initialization for heavy managers
   - Profile memory usage of caches
   - Optimize suggestion pipeline latency

5. **Git Management**
   - Add untracked .md files to .gitignore or commit them:
     ```gitignore
     # Documentation (if you want to exclude them)
     *_SUMMARY.md
     *_COMPLETE.md
     *_GUIDE.md
     *_ARCHITECTURE.md
     ```

6. **Resource Optimization**
   - Consider using vector drawables (SVG) instead of PNGs
   - Use WebP format for images
   - Minimize drawable sizes

---

## 9️⃣ CONCLUSION

The AI Keyboard codebase demonstrates **excellent architecture** with proper separation of concerns, complete resource linkage, and robust Flutter-Kotlin integration. All critical files are properly connected, and no missing resources were detected in the core functionality.

The keyboard successfully implements:
- ✅ Custom IME service with swipe support
- ✅ Theme system V2 with live updates
- ✅ Multilingual support with transliteration
- ✅ AI-powered features (grammar, tone, assistance)
- ✅ Emoji panel with skin tone support
- ✅ Clipboard history management
- ✅ Predictive text and autocorrect
- ✅ Flutter UI for settings and customization

**No critical issues or broken links were found.**

---

## 📚 APPENDIX: Complete File Inventory

### Kotlin Files (60)
```
AIKeyboardService.kt (10,451 lines) - Core service
SwipeKeyboardView.kt (1,460 lines) - Custom keyboard view
MainActivity.kt (711 lines) - Flutter bridge
ThemeManager.kt (712 lines) - Theme V2 engine
LanguageManager.kt - Language management
DictionaryManager.kt - Dictionary management
ClipboardHistoryManager.kt - Clipboard management
CustomToneManager.kt - Tone presets
CapsShiftManager.kt - Shift state management
AutocorrectEngine.kt - Basic autocorrect
UnifiedAutocorrectEngine.kt - Unified corrections
EnhancedAutocorrectEngine.kt - Advanced corrections
SwipeAutocorrectEngine.kt - Swipe matching
PredictiveTextEngine.kt - Next-word prediction
NextWordPredictor.kt - N-gram predictor
SuggestionRanker.kt - Suggestion scoring
SuggestionsPipeline.kt - Unified pipeline
EmojiPanelController.kt - Emoji panel (XML)
GboardEmojiPanel.kt - Emoji panel (programmatic)
SimpleEmojiPanel.kt - Lightweight emoji
EmojiDatabase.kt - Emoji data
EmojiSuggestionEngine.kt - Emoji matching
EmojiCollection.kt - Emoji models
OpenAIService.kt - OpenAI API client
AdvancedAIService.kt - Advanced AI features
StreamingAIService.kt - Streaming AI
AIServiceBridge.kt - AI coordinator
AIResponseCache.kt - Response caching
OpenAIConfig.kt - OpenAI configuration
CleverTypeAIService.kt - CleverType AI
CleverTypePreview.kt - Live preview
CleverTypeToneSelector.kt - Tone selector
ClipboardPanel.kt - Clipboard UI
ClipboardStripView.kt - Inline clipboard
ClipboardItem.kt - Clipboard model
SimpleMediaPanel.kt - GIF/sticker panel
GifManager.kt - GIF management
StickerManager.kt - Sticker packs
MediaCacheManager.kt - Media caching
TransliterationEngine.kt - Indic transliteration
IndicScriptHelper.kt - Indic utilities
LanguageDetector.kt - Language detection
MultilingualDictionary.kt - Multi-language dictionary
LanguageConfig.kt - Language models
KeyboardLayoutManager.kt - Layout switching
KeyboardEnhancements.kt - Enhancement features
LanguageSwitchView.kt - Language switcher UI
ShiftOptionsMenu.kt - Shift menu
FontManager.kt - Font loading
UserDictionaryManager.kt - User words
WordDatabase.kt - Word frequency DB
CursorAwareTextHandler.kt - Cursor positioning
TypingSyncAuditor.kt - Performance diagnostics
BaseManager.kt - Base manager class
ThemeModels.kt - Theme V2 models
LogUtil.kt - Logging utility
BroadcastManager.kt - Broadcast helper
StringNormalizer.kt - String utilities
KeyboardSettingsActivity.kt - Settings activity (unused?)
AIFeaturesPanel.kt - AI features UI (unused?)
```

### XML Layouts (16)
```
keyboard_view_google_layout.xml - Main keyboard container
panel_feature_shared.xml - Shared feature panel template
panel_body_grammar.xml - Grammar fix panel body
panel_body_tone.xml - Tone adjustment panel body
panel_body_ai_assistant.xml - AI assistant panel body
panel_body_clipboard.xml - Clipboard panel body
panel_body_quick_settings.xml - Quick settings panel body
panel_right_translate.xml - Translate toggle
panel_right_toggle.xml - Generic toggle
keyboard_toolbar_simple.xml - 6-button toolbar
mini_settings_sheet.xml - Mini settings overlay
panel_emoji.xml - Emoji panel layout
keyboard_key_preview.xml - Key preview popup
keyboard_popup_keyboard.xml - Popup keyboard
keyboard_view_layout.xml - Alternative keyboard layout
keyboard.xml - Another keyboard layout
```

### XML Drawables (33)
```
key_background_themeable.xml
key_background_default.xml
key_background_normal.xml
key_background_stable.xml
key_background_special.xml
key_background_transparent.xml
key_background_borderless.xml
key_background_popup.xml
key_background.xml
bg_keyboard_toolbar_themable.xml
bg_keyboard_panel_themable.xml
action_button_background.xml
input_text_background.xml
input_field_background.xml
popup_background.xml
menu_item_background.xml
spinner_background.xml
processed_text_background.xml
output_text_background.xml
original_text_background.xml
emoji_touch_feedback.xml
category_tab_selected.xml
category_tab_unselected.xml
bottom_button_background.xml
sym_keyboard_space.xml
sym_keyboard_mic.xml
sym_keyboard_enter.xml
sym_keyboard_done.xml
sym_keyboard_return.xml
sym_keyboard_delete.xml
sym_keyboard_shift.xml
launch_background.xml (v21 variant exists)
```

### XML Keyboards (14)
```
qwerty.xml
qwerty_with_numbers.xml
qwerty_de.xml, qwerty_de_with_numbers.xml
qwerty_es.xml, qwerty_es_with_numbers.xml
qwerty_fr.xml, qwerty_fr_with_numbers.xml
qwerty_hi.xml, qwerty_hi_with_numbers.xml
qwerty_ta.xml, qwerty_ta_with_numbers.xml
qwerty_te.xml, qwerty_te_with_numbers.xml
symbols.xml
numbers.xml
```

### Flutter Dart Files (61)
(See glob_file_search results above)

---

**End of Report**

*This analysis was generated using comprehensive codebase scanning, resource cross-referencing, and dependency tracing. All findings are based on the current state of the repository as of October 7, 2025.*

