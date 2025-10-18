# 🔌 AI Keyboard - Complete Connection Flow Diagram

## Overview
This document illustrates all communication channels, data flows, and connections between Flutter (Dart) and Android (Kotlin) components in the AI Keyboard application.

---

## 📊 System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FLUTTER (Dart) LAYER                              │
│                                                                               │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │   Screens  │  │   Services   │  │    Widgets   │  │  Theme Manager  │  │
│  └─────┬──────┘  └──────┬───────┘  └──────┬───────┘  └────────┬────────┘  │
│        │                 │                  │                   │            │
│        └─────────────────┴──────────────────┴───────────────────┘            │
│                                    │                                         │
└────────────────────────────────────┼─────────────────────────────────────────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │   METHOD CHANNELS (6 total)    │
                    │  Binary Messenger Protocol     │
                    └────────────────┬────────────────┘
                                     │
┌────────────────────────────────────┼─────────────────────────────────────────┐
│                          ANDROID (Kotlin) LAYER                             │
│                                    │                                         │
│  ┌─────────────────────────────────▼──────────────────────────────────┐    │
│  │                          MainActivity                                │    │
│  │  • Method Channel Handlers (6 channels)                             │    │
│  │  • SharedPreferences Manager                                         │    │
│  │  • Broadcast Sender                                                  │    │
│  └────────┬────────────────────────────────────────────────────────────┘    │
│           │                                                                  │
│           │  Broadcasts                    SharedPreferences                │
│           │  & Intents                     (Data Persistence)                │
│           │                                                                  │
│  ┌────────▼─────────────────────────────────────────────────────────────┐  │
│  │                        AIKeyboardService                              │  │
│  │  • Broadcast Receivers (4 types)                                      │  │
│  │  • Settings Manager                                                   │  │
│  │  • Suggestion Controller                                              │  │
│  │  • Keyboard UI & Logic                                                │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Method Channels (Flutter → Android)

### 1. **Config Channel** (`ai_keyboard/config`)
**Purpose:** Main keyboard configuration and settings management

```
┌─────────────────┐                                    ┌──────────────────┐
│  Flutter App    │                                    │  MainActivity    │
│  (Settings UI)  │                                    │                  │
└────────┬────────┘                                    └────────▲─────────┘
         │                                                      │
         │  Method: "updateSettings"                           │
         │  ┌─────────────────────────────────────────────────┐│
         ├──┤ • theme                                          ││
         │  │ • popupEnabled                                   ││
         │  │ • aiSuggestions                                  ││
         │  │ • autoCorrect                                    ││
         │  │ • emojiSuggestions                              ││
         │  │ • nextWordPrediction                            ││
         │  │ • clipboardEnabled                              ││
         │  │ • clipboardWindowSec                            ││
         │  │ • clipboardHistoryItems                         ││
         │  │ • dictionaryEnabled                             ││
         │  │ • autoCapitalization                            ││
         │  │ • doubleSpacePeriod                             ││
         │  │ • soundEnabled                                  ││
         │  │ • soundVolume                                   ││
         │  │ • vibrationEnabled                              ││
         │  │ • vibrationMs                                   ││
         │  │ • swipeTyping                                   ││
         │  │ • voiceInput                                    ││
         │  │ • shiftFeedback                                 ││
         │  │ • showNumberRow                                 ││
         │  └─────────────────────────────────────────────────┘│
         │                                                      │
         │  Response: true/false                               │
         │◄─────────────────────────────────────────────────────┤
         │                                                      │

Other Methods:
├── isKeyboardEnabled() → Boolean
├── isKeyboardActive() → Boolean
├── openKeyboardSettings() → Boolean
├── openInputMethodPicker() → Boolean
├── notifyConfigChange() → Boolean
├── notifyThemeChange(themeId, themeName, hasData) → Boolean
├── getEmojiSettings() → Map<String, Any>
├── updateEmojiSettings(skinTone, maxSize) → Boolean
├── getEmojiConfig() → Map<String, Any>
├── updateEmojiConfig(skinTone, recent) → Boolean
└── clearUserLearnedWords() → Boolean
```

**Data Flow:**
```
Flutter UI → MethodChannel → MainActivity.updateSettings()
                                        ↓
                              SharedPreferences
                            "ai_keyboard_settings"
                                        ↓
                          Broadcast: SETTINGS_CHANGED
                                        ↓
                          AIKeyboardService.settingsReceiver
                                        ↓
                              Reload Settings & UI
```

---

### 2. **Language Channel** (`com.example.ai_keyboard/language`)
**Purpose:** Language data download and multilingual support

```
┌──────────────────┐                                ┌────────────────────┐
│  Language Screen │                                │  MainActivity      │
└────────┬─────────┘                                └─────────▲──────────┘
         │                                                    │
         │  Method: "downloadLanguageData"                   │
         │  Parameters: { lang: "es" }                       │
         ├────────────────────────────────────────────────────┤
         │                                                    │
         │  ← Progress Updates via callback                  │
         │◄───────────────────────────────────────────────────┤
         │  { lang: "es", progress: 50, status: "downloading" }
         │                                                    │
         │  Response: true/false                             │
         │◄───────────────────────────────────────────────────┤

Other Methods:
├── downloadLanguageData(lang) → Boolean + Progress Callbacks
├── deleteCachedLanguageData(lang) → Boolean
├── updateCachedLanguagesList(cachedLanguages) → Boolean
├── setEnabledLanguages(languages, current) → Boolean
├── setCurrentLanguage(language) → Boolean
└── setMultilingualMode(enabled) → Boolean
```

**Data Flow:**
```
Flutter → downloadLanguageData("es")
              ↓
    Firebase Storage Download
              ↓
    Local File Storage (/data/.../files/dictionaries/es/)
              ↓
    SharedPreferences Update
              ↓
    Broadcast: LANGUAGE_CHANGED
              ↓
    AIKeyboardService → Reload Language Resources
```

---

### 3. **AI Channel** (`ai_keyboard/unified_ai`)
**Purpose:** AI text processing (grammar, tone, features)

```
┌─────────────────┐                                 ┌────────────────────┐
│  Keyboard Panel │                                 │  UnifiedAIService  │
│  (AI Features)  │                                 │  (MainActivity)    │
└────────┬────────┘                                 └─────────▲──────────┘
         │                                                    │
         │  Method: "processAIText"                          │
         │  ┌───────────────────────────────────────────────┐│
         ├──┤ • text: "fix my grammer"                      ││
         │  │ • mode: "GRAMMAR" | "TONE" | "FEATURE"        ││
         │  │ • tone: "FUNNY" | "FORMAL" | "CASUAL"...      ││
         │  │ • feature: "TRANSLATE" | "SUMMARIZE"...       ││
         │  │ • stream: false                                ││
         │  └───────────────────────────────────────────────┘│
         │                                                    │
         │  Response: { result: "Fix my grammar" }           │
         │◄───────────────────────────────────────────────────┤

Other Methods:
├── processAIText(text, mode, tone, feature, stream) → String
├── getAIConfig() → Map<String, Any>
├── updateAIConfig(apiKey, model, temperature, maxTokens) → Boolean
└── testConnection() → Boolean
```

**AI Modes:**
- `GRAMMAR` - Fix grammar and spelling
- `TONE` - Change tone (Funny, Formal, Casual, Sarcastic, Poetic)
- `FEATURE` - Special features (Translate, Summarize, Expand, etc.)

---

### 4. **Prompts Channel** (`ai_keyboard/prompts`)
**Purpose:** Custom AI prompts management

```
┌──────────────────┐                               ┌──────────────────┐
│  Prompts Screen  │                               │  PromptManager   │
│                  │                               │  (MainActivity)  │
└────────┬─────────┘                               └─────────▲────────┘
         │                                                   │
         │  Method: "savePrompt"                            │
         │  ┌─────────────────────────────────────────────┐ │
         ├──┤ • category: "grammar" | "tone" | "assistant"││
         │  │ • title: "Make Professional"                 ││
         │  │ • prompt: "Rewrite this text..."             ││
         │  └─────────────────────────────────────────────┘ │
         │                                                   │
         │  Response: true/false                            │
         │◄──────────────────────────────────────────────────┤

Other Methods:
├── savePrompt(category, title, prompt) → Boolean
├── getPrompts(category) → List<Map<String, Any>>
├── deletePrompt(category, title) → Boolean
└── updatePromptOrder(category, orderedTitles) → Boolean
```

**Data Flow:**
```
Flutter → savePrompt()
              ↓
    PromptManager.savePrompt()
              ↓
    SharedPreferences
   "ai_keyboard_prompts_{category}"
              ↓
    Broadcast: PROMPTS_UPDATED
              ↓
    AIKeyboardService.promptReceiver
              ↓
    Reload Custom Prompts in Panels
```

---

### 5. **Clipboard Channel** (`ai_keyboard/clipboard`)
**Purpose:** Clipboard history and template management

```
┌──────────────────┐                              ┌──────────────────┐
│ Clipboard Screen │                              │  MainActivity    │
│                  │                              │  ClipboardMgr    │
└────────┬─────────┘                              └─────────▲────────┘
         │                                                  │
         │  Method: "getHistory"                           │
         ├──────────────────────────────────────────────────┤
         │                                                  │
         │  Response: List<Map>                            │
         │◄─────────────────────────────────────────────────┤
         │  [{ id, text, timestamp, isPinned, isTemplate }] │
         │                                                  │
         │  Method: "togglePin"                            │
         │  Parameters: { id: "uuid" }                     │
         ├──────────────────────────────────────────────────┤
         │  Response: true                                 │
         │◄─────────────────────────────────────────────────┤

Other Methods:
├── getHistory() → List<Map<String, Any>>
├── togglePin(id) → Boolean
├── deleteItem(id) → Boolean
├── clearHistory() → Boolean
├── updateClipboardSettings(enabled, maxSize, expiry, templates) → Boolean
└── addTemplate(text, category) → Boolean
```

**Data Flow:**
```
User Copies Text
       ↓
ClipboardManager (AIKeyboardService)
       ↓
SharedPreferences "clipboard_history"
       ↓
Broadcast: CLIPBOARD_CHANGED
       ↓
Flutter Screen (via getHistory())
```

---

### 6. **Suggestions Channel** (`ai_keyboard/suggestions`)
**Purpose:** Real-time suggestion system configuration

```
┌──────────────────┐                            ┌─────────────────────────┐
│ Suggestion Screen│                            │ SuggestionBridge        │
│                  │                            │ UnifiedSuggestionCtrl   │
└────────┬─────────┘                            └──────────▲──────────────┘
         │                                                 │
         │  Method: "updateSettings"                      │
         │  ┌──────────────────────────────────────────┐  │
         ├──┤ • aiSuggestions: true                    │  │
         │  │ • emojiSuggestions: true                 │  │
         │  │ • clipboardSuggestions: false            │  │
         │  │ • nextWordPrediction: true               │  │
         │  │ • dictionaryEnabled: true                │  │
         │  └──────────────────────────────────────────┘  │
         │                                                 │
         │  Response: true                                │
         │◄────────────────────────────────────────────────┤

Other Methods:
├── updateSettings(ai, emoji, clipboard, nextWord, dict) → Boolean
├── getSettings() → Map<String, Any>
├── clearCache() → Boolean
└── getStats() → Map<String, Any>
```

**Note:** This channel connects to `UnifiedSuggestionController` which orchestrates all suggestion sources.

---

## 📡 Broadcast System (MainActivity → AIKeyboardService)

### Broadcast Flow Architecture

```
┌──────────────────┐                                  ┌─────────────────────┐
│  MainActivity    │                                  │  AIKeyboardService  │
│                  │                                  │                     │
│  [Method Handler]│                                  │  [Broadcast Rcvr]   │
└────────┬─────────┘                                  └──────────▲──────────┘
         │                                                       │
         │  1. Update SharedPreferences                         │
         │     (ai_keyboard_settings)                           │
         ▼                                                       │
    ┌────────────────┐                                          │
    │ SharedPrefs    │                                          │
    │ Persistence    │                                          │
    └────────────────┘                                          │
         │                                                       │
         │  2. Send Broadcast Intent                            │
         │     BroadcastManager.sendToKeyboard()                │
         ├───────────────────────────────────────────────────────┤
         │  Action: "com.example.ai_keyboard.SETTINGS_CHANGED"  │
         │  Extras: (optional metadata)                          │
         └───────────────────────────────────────────────────────┘
                                                                 │
                                                    3. Receive & Process
                                                                 │
                                                    4. Reload Settings
                                                                 │
                                                    5. Update UI
```

### Registered Broadcasts

| Broadcast Action | Purpose | Trigger | Extras |
|-----------------|---------|---------|--------|
| `SETTINGS_CHANGED` | General settings update | updateSettings() | - |
| `THEME_CHANGED` | Theme/appearance update | notifyThemeChange() | theme_id, theme_name, has_theme_data, is_v2_theme |
| `CLIPBOARD_CHANGED` | Clipboard settings update | updateClipboardSettings() | - |
| `EMOJI_SETTINGS_CHANGED` | Emoji preferences update | updateEmojiSettings() | - |
| `LANGUAGE_CHANGED` | Language switch | setCurrentLanguage() | language, multilingual_enabled |
| `PROMPTS_UPDATED` | Custom prompts modified | savePrompt() / deletePrompt() | - |
| `CLEAR_USER_WORDS` | Dictionary reset | clearUserLearnedWords() | - |

### Broadcast Receivers in AIKeyboardService

```kotlin
// 1. Settings Receiver (Primary)
private val settingsReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "SETTINGS_CHANGED" → reloadAllSettings()
            "THEME_CHANGED" → reloadTheme()
            "CLIPBOARD_CHANGED" → reloadClipboardSettings()
            "EMOJI_SETTINGS_CHANGED" → reloadEmojiSettings()
            "LANGUAGE_CHANGED" → reloadLanguageSettings()
            "CLEAR_USER_WORDS" → clearUserDictionary()
        }
    }
}

// 2. Prompts Receiver
private val promptReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "PROMPTS_UPDATED") {
            reloadAIPrompts() // Refresh custom prompt buttons
        }
    }
}
```

---

## 💾 SharedPreferences (Data Persistence Layer)

### Preference Files

```
┌────────────────────────────────────────────────────────────────┐
│                     SharedPreferences Files                    │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  1. FlutterSharedPreferences                                   │
│     ├── flutter.current_theme_data                            │
│     ├── flutter.current_theme_id                              │
│     ├── flutter.theme.v2.json                                 │
│     ├── flutter.enabled_languages                             │
│     ├── flutter.current_language                              │
│     └── flutter.multilingual_enabled                          │
│                                                                │
│  2. ai_keyboard_settings (Native)                             │
│     ├── keyboard_theme                                        │
│     ├── popup_enabled                                         │
│     ├── ai_suggestions                                        │
│     ├── auto_correct                                          │
│     ├── emoji_suggestions                                     │
│     ├── next_word_prediction                                  │
│     ├── clipboard_suggestions_enabled                         │
│     ├── clipboard_window_sec                                  │
│     ├── clipboard_history_items                               │
│     ├── dictionary_enabled                                    │
│     ├── auto_capitalization                                   │
│     ├── double_space_period                                   │
│     ├── sound_enabled                                         │
│     ├── sound_volume                                          │
│     ├── vibration_enabled                                     │
│     ├── vibration_ms                                          │
│     ├── swipe_typing                                          │
│     ├── voice_input                                           │
│     ├── show_shift_feedback                                   │
│     └── show_number_row                                       │
│                                                                │
│  3. clipboard_history                                         │
│     ├── clipboard_enabled                                     │
│     ├── max_history_size                                      │
│     ├── auto_expiry_enabled                                   │
│     ├── expiry_duration_minutes                               │
│     ├── history_items (JSON)                                  │
│     └── template_items (JSON)                                 │
│                                                                │
│  4. emoji_preferences                                         │
│     ├── preferred_skin_tone                                   │
│     ├── emoji_history_max_size                                │
│     └── emoji_history (JSON array)                            │
│                                                                │
│  5. ai_keyboard_prompts_grammar                               │
│  6. ai_keyboard_prompts_tone                                  │
│  7. ai_keyboard_prompts_assistant                             │
│     └── Custom prompt JSON arrays                             │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Settings Manager (AIKeyboardService)

```
┌─────────────────────────────────────────────────────────────┐
│           SettingsManager (Unified Settings Reader)         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  class SettingsManager(context: Context) {                 │
│      private val flutterPrefs                               │
│      private val nativePrefs                                │
│                                                             │
│      fun loadSettings(): UnifiedSettings {                  │
│          // Read from BOTH sources                          │
│          // Native prefs take precedence                    │
│          // Eliminates redundant I/O                        │
│      }                                                      │
│  }                                                          │
│                                                             │
│  Benefits:                                                  │
│  ✓ Single source of truth                                  │
│  ✓ No duplicate reads                                      │
│  ✓ Consistent behavior                                     │
│  ✓ Performance optimization                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎨 Theme System Flow

```
┌─────────────────┐
│  Theme Gallery  │
│  (Flutter)      │
└────────┬────────┘
         │
         │ 1. User selects theme
         │
         ▼
┌──────────────────────────────┐
│ ThemeManager.applyTheme()    │
│ (Flutter)                    │
└────────┬─────────────────────┘
         │
         │ 2. Save to SharedPreferences
         │    "flutter.current_theme_data"
         │    "flutter.theme.v2.json"
         │
         ▼
┌──────────────────────────────┐
│ MethodChannel                │
│ "notifyThemeChange"          │
└────────┬─────────────────────┘
         │
         │ 3. MainActivity receives
         │
         ▼
┌──────────────────────────────┐
│ BroadcastManager             │
│ Action: THEME_CHANGED        │
│ Extras: theme_id, name       │
└────────┬─────────────────────┘
         │
         │ 4. AIKeyboardService receives
         │
         ▼
┌──────────────────────────────┐
│ ThemeManager.reloadTheme()   │
│ (Kotlin)                     │
└────────┬─────────────────────┘
         │
         │ 5. Read SharedPreferences
         │    Parse JSON theme data
         │
         ▼
┌──────────────────────────────┐
│ Update Keyboard UI           │
│ • Key colors                 │
│ • Background                 │
│ • Text colors                │
│ • Suggestion bar             │
│ • Panels                     │
└──────────────────────────────┘
```

---

## 📝 Suggestion System Architecture

```
┌───────────────────────────────────────────────────────────────────────┐
│                    UNIFIED SUGGESTION SYSTEM                          │
└───────────────────────────────────────────────────────────────────────┘

┌─────────────────┐
│   User Types    │
│   "helo wor"    │
└────────┬────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│  AIKeyboardService.onText()                      │
│  • Captures typed text                           │
│  • Triggers suggestion pipeline                  │
└────────┬─────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│  UnifiedSuggestionController                     │
│  • Orchestrates all suggestion sources           │
│  • Prioritizes results                           │
│  • Filters & deduplicates                        │
└────────┬─────────────────────────────────────────┘
         │
         ├────────────┬──────────────┬──────────────┬──────────────┐
         │            │              │              │              │
         ▼            ▼              ▼              ▼              ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
│ Autocorrect  │ │  Emoji   │ │Clipboard │ │Next-Word │ │   AI/ML      │
│   Engine     │ │Suggestions│ │Suggestions│ │Prediction│ │ Suggestions  │
└──────┬───────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘
       │              │            │            │              │
       │ "hello"      │ "👋"       │ (recent)   │ "world"      │ "hello"
       │ "help"       │ "😊"       │            │ "work"       │
       │ "held"       │            │            │              │
       │              │            │            │              │
       └──────────────┴────────────┴────────────┴──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │  Priority & Merge           │
                    │  • Autocorrect first        │
                    │  • Then predictions         │
                    │  • Mix emoji if enabled     │
                    │  • Deduplicate              │
                    └───────────┬─────────────────┘
                                │
                                ▼
                    ┌─────────────────────────────┐
                    │  Update Suggestion UI       │
                    │  [hello] [help] [world] 👋  │
                    └─────────────────────────────┘
```

### Suggestion Sources Configuration

| Source | Setting Key | SharedPrefs | Controller |
|--------|------------|-------------|------------|
| Autocorrect | `auto_correct` | `ai_keyboard_settings` | `UnifiedAutocorrectEngine` |
| Emoji | `emoji_suggestions` | `ai_keyboard_settings` | `EmojiSuggestionManager` |
| Clipboard | `clipboard_suggestions_enabled` | `ai_keyboard_settings` | `ClipboardManager` |
| Next-Word | `next_word_prediction` | `ai_keyboard_settings` | `LanguageModel` |
| Dictionary | `dictionary_enabled` | `ai_keyboard_settings` | `MultilingualDictionary` |

---

## 🌐 Language System Flow

```
┌─────────────────────────────────────────────────────────────┐
│                 MULTILINGUAL LANGUAGE SYSTEM                │
└─────────────────────────────────────────────────────────────┘

1. LANGUAGE DOWNLOAD (One-time)
┌────────────────┐
│ Language Screen│
└───────┬────────┘
        │ Select Spanish
        ▼
┌──────────────────────────┐
│ MethodChannel            │
│ "downloadLanguageData"   │
│ { lang: "es" }           │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ Firebase Storage         │
│ gs://.../dictionaries/es/│
│ • dict_es.txt            │
│ • bigrams_es.txt         │
│ • trigrams_es.txt        │
│ • translit_es.json       │
└───────┬──────────────────┘
        │ Download
        ▼
┌──────────────────────────┐
│ Local File System        │
│ /data/.../files/         │
│ dictionaries/es/         │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ Update Cached List       │
│ SharedPreferences        │
└──────────────────────────┘

2. LANGUAGE ACTIVATION
┌────────────────┐
│ User Selects   │
│ Spanish        │
└───────┬────────┘
        │
        ▼
┌──────────────────────────┐
│ setCurrentLanguage("es") │
│ MethodChannel            │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ SharedPreferences        │
│ "flutter.current_language"│
│ = "es"                   │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ Broadcast:               │
│ LANGUAGE_CHANGED         │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ AIKeyboardService        │
│ • Load Spanish dict      │
│ • Init transliteration   │
│ • Update autocorrect     │
│ • Refresh layout         │
└──────────────────────────┘

3. LANGUAGE CYCLING (Multilingual Mode)
┌────────────────┐
│ User Taps 🌐   │
│ Globe Key      │
└───────┬────────┘
        │
        ▼
┌──────────────────────────────────┐
│ cycleLanguage()                  │
│ AIKeyboardService                │
└───────┬──────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Load Enabled Languages           │
│ [en, es, fr, de]                 │
│ Current: en → Next: es           │
└───────┬──────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ activateLanguage("es")           │
│ • Swap dictionaries              │
│ • Update autocorrect             │
│ • Update UI labels               │
│ • Show toast "Español"           │
└──────────────────────────────────┘
```

---

## 📋 Clipboard System Flow

```
┌───────────────────────────────────────────────────────────┐
│                  CLIPBOARD INTEGRATION                    │
└───────────────────────────────────────────────────────────┘

1. CLIPBOARD CAPTURE
┌────────────────┐
│ User Copies    │
│ External App   │
└───────┬────────┘
        │
        ▼
┌──────────────────────────┐
│ Android Clipboard API    │
│ ClipboardManager         │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ AIKeyboardService        │
│ ClipboardManager         │
│ • Detect clipboard change│
│ • Store to history       │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ SharedPreferences        │
│ "clipboard_history"      │
│ {                        │
│   id, text, timestamp,   │
│   isPinned, isTemplate   │
│ }                        │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ Broadcast:               │
│ CLIPBOARD_CHANGED        │
└──────────────────────────┘

2. CLIPBOARD SUGGESTIONS (Real-time)
┌────────────────┐
│ User Types     │
└───────┬────────┘
        │
        ▼
┌──────────────────────────────┐
│ UnifiedSuggestionController  │
│ Check: clipboardEnabled?     │
└───────┬──────────────────────┘
        │ YES
        ▼
┌──────────────────────────────┐
│ ClipboardManager             │
│ getRecentItems(3)            │
└───────┬──────────────────────┘
        │
        │ ["Meeting at 2pm",
        │  "example@email.com",
        │  "555-1234"]
        ▼
┌──────────────────────────────┐
│ Suggestion Bar               │
│ [recent1] [recent2] [recent3]│
└──────────────────────────────┘

3. CLIPBOARD PANEL (Full Access)
┌────────────────┐
│ User Taps 📋   │
│ Clipboard Key  │
└───────┬────────┘
        │
        ▼
┌──────────────────────────────┐
│ Show Clipboard Panel         │
│ • History (20 items)         │
│ • Pinned items               │
│ • Templates                  │
│ • Search                     │
└──────────────────────────────┘
        │
        │ User selects item
        ▼
┌──────────────────────────────┐
│ Insert to input field        │
└──────────────────────────────┘
```

---

## 🎮 Emoji System Flow

```
┌────────────────────────────────────────────────────────┐
│                   EMOJI SYSTEM                         │
└────────────────────────────────────────────────────────┘

1. EMOJI SETTINGS
┌────────────────┐
│ Emoji Settings │
│ Screen         │
└───────┬────────┘
        │
        ▼
┌──────────────────────────┐
│ updateEmojiSettings()    │
│ { skinTone: "🏽",        │
│   historyMaxSize: 90 }   │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ SharedPreferences        │
│ "emoji_preferences"      │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ Broadcast:               │
│ EMOJI_SETTINGS_CHANGED   │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ AIKeyboardService        │
│ Reload emoji config      │
└──────────────────────────┘

2. EMOJI PANEL
┌────────────────┐
│ User Taps 😊   │
│ Emoji Key      │
└───────┬────────┘
        │
        ▼
┌──────────────────────────┐
│ Show Emoji Panel         │
│ ├── Recents              │
│ ├── Smileys              │
│ ├── Animals              │
│ ├── Food                 │
│ ├── Activities           │
│ ├── Travel               │
│ ├── Objects              │
│ └── Symbols              │
└───────┬──────────────────┘
        │
        │ User selects 👍
        ▼
┌──────────────────────────┐
│ Insert emoji             │
│ Update history           │
└──────────────────────────┘

3. EMOJI SUGGESTIONS (Inline)
┌────────────────┐
│ User Types     │
│ "good job"     │
└───────┬────────┘
        │
        ▼
┌──────────────────────────┐
│ UnifiedSuggestionCtrl    │
│ emojiSuggestions = true? │
└───────┬──────────────────┘
        │ YES
        ▼
┌──────────────────────────┐
│ EmojiMatcher             │
│ Analyze: "good job"      │
│ → [👍, 👏, 💪]           │
└───────┬──────────────────┘
        │
        ▼
┌──────────────────────────┐
│ Suggestion Bar           │
│ [good] [job] 👍 👏 💪    │
└──────────────────────────┘
```

---

## 🔧 Complete Data Flow Summary

### Settings Update Flow
```
Flutter UI
   ↓ (Method Channel)
MainActivity.updateSettings()
   ↓ (Save)
SharedPreferences
   ↓ (Broadcast)
AIKeyboardService.settingsReceiver
   ↓ (Reload)
SettingsManager.loadSettings()
   ↓ (Apply)
Keyboard UI Update
```

### AI Processing Flow
```
User Selects Text in Keyboard
   ↓
Show AI Panel (Grammar/Tone/Assistant)
   ↓
User Taps AI Feature Button
   ↓ (Method Channel)
MainActivity → UnifiedAIService
   ↓ (API Call)
OpenAI / Gemini API
   ↓ (Response)
MainActivity
   ↓ (Result)
Flutter/Native Callback
   ↓
Display Result in Panel
   ↓
User Taps "Apply"
   ↓
Replace Text in Input Field
```

### Suggestion Generation Flow
```
User Types Character
   ↓
AIKeyboardService.onText()
   ↓
UnifiedSuggestionController.getSuggestions()
   ↓
   ├─→ AutocorrectEngine (dictionary-based)
   ├─→ EmojiSuggestionManager (emoji matching)
   ├─→ ClipboardManager (recent clips)
   ├─→ LanguageModel (next-word prediction)
   └─→ AI Suggestions (if enabled)
   ↓
Merge, Prioritize, Deduplicate
   ↓
Update Suggestion Bar UI
```

### Language Switch Flow
```
User Taps Globe Key 🌐
   ↓
AIKeyboardService.cycleLanguage()
   ↓
Load Next Enabled Language
   ↓
activateLanguage(lang)
   ↓
   ├─→ Load Dictionary
   ├─→ Init Transliteration
   ├─→ Update Autocorrect
   ├─→ Refresh Layout
   └─→ Show Toast Notification
   ↓
Update SharedPreferences
   ↓
Keyboard Ready in New Language
```

---

## 📞 Complete Method Channel Reference

| Channel Name | Purpose | Key Methods | Used By |
|-------------|---------|-------------|---------|
| `ai_keyboard/config` | Keyboard settings, emoji, dictionary | updateSettings, getEmojiConfig, clearUserWords | Settings screens |
| `com.example.ai_keyboard/language` | Language management | downloadLanguageData, setCurrentLanguage | Language screen |
| `ai_keyboard/unified_ai` | AI text processing | processAIText | AI panels |
| `ai_keyboard/prompts` | Custom prompts | savePrompt, getPrompts | Prompts screen |
| `ai_keyboard/clipboard` | Clipboard operations | getHistory, togglePin | Clipboard screen |
| `ai_keyboard/suggestions` | Suggestion config | updateSettings | Suggestion settings |

---

## 🔄 Broadcast Reference

| Broadcast Action | Sent By | Received By | Trigger | Purpose |
|-----------------|---------|-------------|---------|---------|
| `SETTINGS_CHANGED` | MainActivity | AIKeyboardService | updateSettings() | Reload all settings |
| `THEME_CHANGED` | MainActivity | AIKeyboardService | notifyThemeChange() | Update theme |
| `CLIPBOARD_CHANGED` | MainActivity | AIKeyboardService | updateClipboardSettings() | Reload clipboard config |
| `EMOJI_SETTINGS_CHANGED` | MainActivity | AIKeyboardService | updateEmojiSettings() | Reload emoji config |
| `LANGUAGE_CHANGED` | MainActivity | AIKeyboardService | setCurrentLanguage() | Switch language |
| `PROMPTS_UPDATED` | MainActivity | AIKeyboardService | savePrompt() | Refresh custom prompts |
| `CLEAR_USER_WORDS` | MainActivity | AIKeyboardService | clearUserLearnedWords() | Reset dictionary |

---

## 🎯 Key Architectural Patterns

### 1. **Method Channels** (Flutter ↔ Android)
- **Purpose:** Bidirectional async communication
- **Pattern:** Request-Response
- **Use Cases:** Settings updates, AI processing, data queries

### 2. **Broadcasts** (MainActivity → AIKeyboardService)
- **Purpose:** Unidirectional notifications
- **Pattern:** Fire-and-forget (with receiver)
- **Use Cases:** Settings changed, theme updated, language switched

### 3. **SharedPreferences** (Persistent Storage)
- **Purpose:** Configuration persistence across app restarts
- **Pattern:** Key-value storage
- **Use Cases:** All user settings, preferences, history

### 4. **Singleton Pattern** (Service Access)
- **Purpose:** Global access to keyboard service
- **Pattern:** `AIKeyboardService.getInstance()`
- **Use Cases:** External components accessing keyboard state

### 5. **Observer Pattern** (Change Notifications)
- **Purpose:** React to data changes
- **Pattern:** Callbacks, listeners
- **Use Cases:** Dictionary updates, suggestion changes

---

## 🚀 Performance Optimizations

1. **Settings Debouncing**
   - Prevents rapid consecutive settings updates
   - 250ms cooldown between updates

2. **Lazy Loading**
   - SharedPreferences loaded on-demand
   - Language resources loaded asynchronously

3. **Caching**
   - Suggestion results cached
   - Language data cached locally
   - Theme data cached in memory

4. **Broadcast Efficiency**
   - Minimal extras in broadcasts
   - RECEIVER_NOT_EXPORTED for security
   - Unregistered when not needed

5. **Coroutine-based I/O**
   - All file/network operations off main thread
   - Structured concurrency with proper scoping

---

## 📝 Integration Checklist

When adding a new feature that requires Flutter ↔ Android communication:

- [ ] Define method channel constant in `MainActivity.companion object`
- [ ] Implement method handler in `MainActivity.configureFlutterEngine()`
- [ ] Create corresponding Dart service in `lib/services/`
- [ ] Define broadcast action if needed (in BroadcastManager)
- [ ] Register broadcast receiver in `AIKeyboardService.onCreate()`
- [ ] Add SharedPreferences keys for persistence
- [ ] Update SettingsManager if settings-related
- [ ] Document in this flow diagram
- [ ] Add error handling for all async operations
- [ ] Test bidirectional communication

---

## 🔍 Debugging Tips

### Method Channel Issues
```bash
# Check logs for method channel calls
adb logcat | grep "MainActivity"

# Check for method not implemented errors
adb logcat | grep "notImplemented"
```

### Broadcast Issues
```bash
# Check if broadcasts are being sent
adb logcat | grep "BroadcastManager"

# Check if receiver is registered
adb logcat | grep "registerReceiver"
```

### SharedPreferences Issues
```bash
# Inspect SharedPreferences files
adb shell run-as com.example.ai_keyboard cat /data/data/com.example.ai_keyboard/shared_prefs/ai_keyboard_settings.xml
```

---

## 📚 Related Documentation

- `UNIFIED_SUGGESTION_ARCHITECTURE_INTEGRATION.md` - Suggestion system details
- `UNIFIED_LANGUAGE_SYSTEM_COMPLETE.md` - Language system architecture
- `CLIPBOARD_COMPLETE_SOLUTION.md` - Clipboard implementation
- `DYNAMIC_PROMPT_SYSTEM_COMPLETE.md` - Custom prompts feature
- `ENHANCED_THEME_SYSTEM_COMPLETE.md` - Theme system details

---

**Document Version:** 1.0  
**Last Updated:** October 18, 2025  
**Maintained By:** AI Keyboard Development Team

