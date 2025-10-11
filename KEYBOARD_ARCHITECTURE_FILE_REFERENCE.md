# AI Keyboard - Complete File Architecture Reference

This document provides a comprehensive listing of all files used in the AI Keyboard project, organized by functionality and platform.

## Table of Contents
1. [Android Platform Files](#android-platform-files)
2. [iOS Platform Files](#ios-platform-files)
3. [Flutter/Dart Files](#flutterdart-files)
4. [Configuration Files](#configuration-files)
5. [Asset Files](#asset-files)

---

## Android Platform Files

### 📁 Core Keyboard Service
Located in: `android/app/src/main/kotlin/com/example/ai_keyboard/`

#### Main Keyboard Service
- **`AIKeyboardService.kt`** (10,853 lines)
  - Main keyboard input method service
  - Handles keyboard lifecycle, input/output, and text management
  - Integrates all keyboard components (view, suggestions, panels)
  - Manages keyboard modes (letters, numbers, symbols)
  - Handles shift states and caps lock
  - Coordinates with all other services

### 📁 Keyboard Layout & View
- **`SwipeKeyboardView.kt`** (1,473 lines)
  - Custom keyboard view extending KeyboardView
  - Handles swipe gesture detection and rendering
  - Manages visual appearance of keys
  - Theme integration for dynamic styling
  - Touch event handling
  - Special key rendering (emoji, voice, symbols)
  - Adaptive sizing for different screen sizes
  - One-handed mode support
  - Floating mode support
  - Spacebar gestures (cursor control)
  
- **`KeyboardLayoutManager.kt`**
  - Manages different keyboard layout configurations
  - Handles layout switching (QWERTY, AZERTY, etc.)
  - Language-specific layout adaptations

- **`CapsShiftManager.kt`**
  - Manages shift key states (normal, shifted, caps lock)
  - Handles automatic capitalization logic
  - Shift state transitions

### 📁 Suggestion & Prediction System
- **`SuggestionsPipeline.kt`**
  - Central suggestion processing pipeline
  - Coordinates multiple suggestion sources
  - Prioritizes and ranks suggestions
  
- **`PredictiveTextEngine.kt`**
  - Next word prediction logic
  - Context-aware predictions
  - Learning from user patterns

- **`predict/NextWordPredictor.kt`**
  - Advanced next word prediction algorithm
  - Uses n-gram models
  - Contextual prediction

- **`predict/SuggestionRanker.kt`**
  - Ranks suggestions by relevance and confidence
  - Combines multiple scoring factors

- **`WordDatabase.kt`**
  - Word frequency database
  - Common words and phrases storage
  - Database queries for suggestions

- **`word_trie.dart`** (Dart implementation)
  - Trie data structure for efficient word lookups
  - Prefix-based word suggestions

### 📁 Autocorrect & Correction
- **`UnifiedAutocorrectEngine.kt`**
  - Unified autocorrect system
  - Handles multiple languages
  - Typo detection and correction
  - Learning user corrections

- **`AutocorrectEngine.kt`**
  - Core autocorrect algorithms
  - Edit distance calculations
  - Correction confidence scoring

- **`EnhancedAutocorrectEngine.kt`**
  - Enhanced autocorrect with ML features
  - Context-aware corrections
  - User preference learning

- **`SwipeAutocorrectEngine.kt`**
  - Specialized autocorrect for swipe typing
  - Path-to-word conversion
  - Swipe gesture interpretation

### 📁 Swipe Typing System
- **`SwipeKeyboardView.kt`** (also listed above)
  - Primary swipe gesture detection
  - Touch tracking and path rendering
  - Swipe-to-type implementation
  - Key: `onSwipeDetected()`, `onSwipeStarted()`, `onSwipeEnded()`

- **`SwipeAutocorrectEngine.kt`**
  - Converts swipe paths to words
  - Handles swipe-specific corrections

### 📁 Dictionary & Language
- **`DictionaryManager.kt`**
  - Manages dictionary loading and access
  - Dictionary updates and syncing
  - Custom word additions

- **`MultilingualDictionary.kt`**
  - Multi-language dictionary support
  - Language-specific word lists
  - Cross-language suggestions

- **`UserDictionaryManager.kt`**
  - User-added words and phrases
  - Personal dictionary management
  - Cloud synchronization of user words

- **`LanguageManager.kt`**
  - Language switching logic
  - Available languages management
  - Language preference storage

- **`LanguageConfig.kt`**
  - Language configuration data
  - Language-specific settings
  - Layout mappings

- **`LanguageDetector.kt`**
  - Automatic language detection
  - Context-based language switching

- **`LanguageSwitchView.kt`**
  - UI for language switching
  - Language selector display

### 📁 Transliteration & Indic Support
- **`TransliterationEngine.kt`**
  - Transliteration between scripts
  - Phonetic input support
  - Indic language transliteration

- **`IndicScriptHelper.kt`**
  - Helper functions for Indic scripts
  - Devanagari, Tamil, Telugu support
  - Script-specific processing

### 📁 AI Features & Intelligence
- **`AIFeaturesPanel.kt`** (944 lines)
  - AI-powered writing assistance panel
  - Grammar correction interface
  - Tone adjustment UI
  - Smart reply generation
  - Text transformation features

- **`AdvancedAIService.kt`**
  - Advanced AI text processing
  - Grammar and style checking
  - Tone analysis and adjustment
  - Text enhancement suggestions

- **`StreamingAIService.kt`**
  - Streaming AI responses
  - Real-time text generation
  - Progressive suggestion display

- **`CleverTypeAIService.kt`**
  - CleverType-style AI features
  - Smart text completion
  - Context-aware suggestions

- **`AIServiceBridge.kt`**
  - Bridge between keyboard and AI services
  - Manages AI service lifecycle
  - Suggestion callback handling

- **`AIResponseCache.kt`**
  - Caches AI responses
  - Improves response time
  - Reduces API calls

- **`OpenAIService.kt`**
  - OpenAI API integration
  - GPT-based text generation
  - API request handling

- **`OpenAIConfig.kt`**
  - OpenAI configuration
  - API key management
  - Model selection

- **`CustomToneManager.kt`**
  - Manages custom writing tones
  - User-defined tone styles
  - Tone preference storage

### 📁 Emoji & Media
- **`EmojiPanelController.kt`**
  - Controls emoji panel display
  - Emoji category management
  - Recent emoji tracking

- **`GboardEmojiPanel.kt`**
  - Gboard-style emoji panel
  - Category tabs
  - Emoji search

- **`SimpleEmojiPanel.kt`**
  - Simplified emoji picker
  - Basic emoji grid
  - Quick emoji access

- **`EmojiDatabase.kt`**
  - Emoji data storage
  - Emoji metadata
  - Search indexing

- **`EmojiCollection.kt`**
  - Emoji categorization
  - Emoji collections management
  - Category definitions

- **`EmojiSuggestionEngine.kt`**
  - Context-based emoji suggestions
  - Emoji prediction
  - Relevance scoring

- **`SimpleMediaPanel.kt`**
  - Media (GIF/Sticker) panel
  - Media category display
  - Media insertion

- **`GifManager.kt`**
  - GIF loading and caching
  - GIF search integration
  - GIF display

- **`StickerManager.kt`**
  - Sticker pack management
  - Sticker display
  - Custom sticker support

- **`MediaCacheManager.kt`**
  - Caches media files
  - Memory management
  - Cache cleanup

### 📁 Clipboard
- **`ClipboardPanel.kt`**
  - Clipboard history panel
  - Clipboard item display
  - Quick paste functionality

- **`ClipboardHistoryManager.kt`**
  - Manages clipboard history
  - Persistent clipboard storage
  - Clipboard synchronization

- **`ClipboardStripView.kt`**
  - Compact clipboard strip UI
  - Quick access to recent clips
  - Inline clipboard preview

- **`ClipboardItem.kt`**
  - Data model for clipboard items
  - Timestamp and metadata
  - Item serialization

### 📁 Toolbar Components
The toolbar in Android is implemented as part of `AIKeyboardService.kt` with the following components:

- **Suggestion Container** (in `AIKeyboardService.kt`)
  - Displays word suggestions
  - Autocorrect indicator
  - Clickable suggestion chips
  - Functions: `updateSuggestions()`, `createSuggestionContainer()`

- **Language Switcher** (via `LanguageSwitchView.kt`)
  - Quick language switch button
  - Current language indicator
  - Multi-language support

- **AI Features Button** (in toolbar)
  - Opens AI features panel
  - Grammar/tone access
  - CleverType integration

- **Settings Button** (in toolbar)
  - Quick settings access
  - Keyboard preferences
  - Mini-settings panel

### 📁 Theme & Styling
- **`ThemeManager.kt`**
  - Central theme management
  - Theme switching
  - Dynamic color application
  - Theme persistence

- **`themes/ThemeModels.kt`**
  - Theme data models
  - Theme configuration structures
  - Color palette definitions

- **`FontManager.kt`**
  - Font loading and management
  - Custom font support
  - Font scaling

### 📁 UI Panels
- **`CleverTypePreview.kt`**
  - Preview panel for CleverType suggestions
  - Before/after text display
  - Accept/reject interface

- **`CleverTypeToneSelector.kt`**
  - Tone selection UI
  - Tone preview
  - Quick tone switching

- **`ShiftOptionsMenu.kt`**
  - Long-press shift menu
  - Special character access
  - Alternative key options

### 📁 Text Processing
- **`CursorAwareTextHandler.kt`**
  - Cursor position tracking
  - Selection handling
  - Smart text insertion

- **`text/StringNormalizer.kt`**
  - Text normalization
  - Unicode handling
  - Case normalization

### 📁 Utilities & Diagnostics
- **`utils/LogUtil.kt`**
  - Logging utility
  - Debug log management
  - Error reporting

- **`utils/BroadcastManager.kt`**
  - Broadcast message handling
  - Inter-component communication
  - Settings synchronization

- **`managers/BaseManager.kt`**
  - Base class for managers
  - Common manager functionality
  - Lifecycle management

- **`diagnostics/TypingSyncAuditor.kt`**
  - Typing performance monitoring
  - Latency tracking
  - Performance diagnostics

### 📁 Additional Components
- **`KeyboardEnhancements.kt`**
  - Enhancement features
  - Advanced typing features
  - Feature toggles

- **`KeyboardSettingsActivity.kt`**
  - Settings screen activity
  - Preference management UI
  - Settings persistence

- **`MainActivity.kt`**
  - Main app activity
  - App initialization
  - Permission handling

---

## iOS Platform Files

### 📁 iOS Keyboard Extension
Located in: `ios/KeyboardExtension/`

- **`KeyboardViewController.swift`** (504 lines)
  - Main iOS keyboard controller
  - Keyboard lifecycle management
  - Text input and output
  - Shift state management (3-state FSM)
  - Auto-capitalization
  - Settings synchronization via App Groups
  - Darwin notification handling

- **`LayoutManager.swift`**
  - Manages keyboard layouts (QWERTY, numbers, symbols)
  - Dynamic key layout generation
  - Programmatic UI creation
  - Layout switching logic
  - Key button creation and configuration

- **`NumberLayoutManager.swift`**
  - Number pad layout
  - Symbol layout
  - Special character layout
  - Layout-specific logic

- **`KeyButton.swift`**
  - Individual key button component
  - Key appearance and styling
  - Touch handling
  - Key press feedback

- **`SettingsManager.swift`**
  - Settings persistence using App Groups
  - Settings synchronization with main app
  - Preference management
  - Default settings

### 📁 iOS Main App
Located in: `ios/Runner/`

- **`AppDelegate.swift`**
  - App initialization
  - Flutter engine setup
  - Push notification handling
  - App lifecycle management

### 📁 iOS Entitlements
- **`KeyboardExtensionDebug.entitlements`**
- **`KeyboardExtensionProfile.entitlements`**
- **`KeyboardExtensionRelease.entitlements`**
- **`RunnerProfile.entitlements`**
  - App Groups configuration
  - Keyboard extension capabilities
  - Security entitlements

---

## Flutter/Dart Files

### 📁 Main Application
Located in: `lib/`

- **`main.dart`**
  - App entry point
  - Firebase initialization
  - Theme configuration
  - Navigation setup

- **`firebase_options.dart`**
  - Firebase configuration
  - Platform-specific Firebase options

### 📁 Keyboard UI Components
- **`widgets/compose_keyboard.dart`**
  - Compose keyboard widget
  - Preview keyboard interface
  - Flutter keyboard UI

- **`keyboard_feedback_system.dart`**
  - Haptic feedback system
  - Sound feedback
  - Vibration patterns

### 📁 Theme Management
- **`theme_manager.dart`**
  - Flutter theme management
  - Theme synchronization with native
  - Theme persistence

- **`theme/theme_v2.dart`**
  - Theme data structures
  - Color schemes
  - Typography definitions

- **`theme/theme_editor_v2.dart`**
  - Theme customization UI
  - Color picker
  - Theme preview

- **`theme_editor_screen.dart`**
  - Theme editor screen
  - Theme creation interface
  - Theme export/import

### 📁 Settings Screens
Located in: `lib/screens/main screens/`

- **`keyboard_settings_screen.dart`**
  - Keyboard preferences
  - Typing settings
  - Layout options

- **`typing_suggestion_screen.dart`**
  - Suggestion settings
  - Autocorrect preferences
  - Prediction settings

- **`sounds_vibration_screen.dart`**
  - Audio feedback settings
  - Vibration intensity
  - Feedback customization

- **`gestures_glide_screen.dart`**
  - Swipe typing settings
  - Gesture configuration
  - Glide typing options

- **`language_screen.dart`**
  - Language selection
  - Keyboard layout per language
  - Language preferences

- **`theme_screen.dart`**
  - Theme selection
  - Theme preview
  - Theme management

- **`customize_theme_screen.dart`**
  - Custom theme creation
  - Color customization
  - Theme components

- **`view_all_themes_screen.dart`**
  - Theme gallery
  - Theme browsing
  - Theme installation

- **`emoji_settings_screen.dart`**
  - Emoji preferences
  - Recent emoji management
  - Emoji suggestions toggle

- **`emoji_skin_tone_screen.dart`**
  - Skin tone selection
  - Default skin tone setting
  - Emoji modifiers

- **`dictionary_screen.dart`**
  - User dictionary management
  - Word additions/deletions
  - Dictionary synchronization

- **`clipboard_screen.dart`**
  - Clipboard history settings
  - Clipboard size limit
  - Auto-clear options

### 📁 AI Features Screens
- **`ai_writing_assistance_screen.dart`**
  - AI writing features settings
  - Feature toggles
  - AI preferences

- **`ai_rewriting_screen.dart`**
  - AI rewriting interface
  - Text transformation
  - Style adjustment

- **`ai_rewriting_guidance_screen.dart`**
  - AI rewriting tutorial
  - Feature explanation
  - Usage guide

- **`chatgpt_guidance_screen.dart`**
  - ChatGPT integration guide
  - API key setup
  - Usage instructions

- **`autocorrect_guidance_screen.dart`**
  - Autocorrect tutorial
  - Feature explanation
  - Tips and tricks

### 📁 Other Screens
- **`home_screen.dart`**
  - Main app home screen
  - Feature overview
  - Quick access

- **`mainscreen.dart`**
  - Main navigation screen
  - Tab management
  - Screen coordination

- **`setting_screen.dart`**
  - Main settings screen
  - Settings categories
  - Navigation to sub-settings

- **`profile_screen.dart`**
  - User profile
  - Account management
  - Subscription status

- **`chat_screen.dart`**
  - AI chat interface
  - Conversation management
  - Message history

- **`notification_screen.dart`**
  - Notification preferences
  - Push notification settings
  - Notification history

- **`upgrade_pro_screen.dart`**
  - Premium features
  - Subscription options
  - Payment interface

- **`info_app_screen.dart`**
  - App information
  - Version details
  - About screen

- **`guidance_screen.dart`**
  - General guidance
  - Feature tutorials
  - Help documentation

- **`set_keyboard_guidance_screen.dart`**
  - Keyboard setup guide
  - Installation instructions
  - Troubleshooting

### 📁 Authentication
Located in: `lib/screens/login/`

- **`login_screen.dart`**
- **`signup_screen.dart`**
- **`mobile_login_screen.dart`**
- **`otp_verification_screen.dart`**
- **`user_information_screen.dart`**
- **`login_illustraion_screen.dart`**
- **`success_screen.dart`**

### 📁 Onboarding
Located in: `lib/screens/onboarding/`

- **`animated_onboarding_screen.dart`**
- **`on_boarding_screen_1.dart`**

### 📁 Services
Located in: `lib/services/`

- **`firebase_auth_service.dart`**
  - Firebase authentication
  - User management
  - Token handling

- **`keyboard_cloud_sync.dart`**
  - Keyboard settings cloud sync
  - Multi-device synchronization
  - Conflict resolution

- **`dictionary_cloud_sync.dart`**
  - User dictionary cloud sync
  - Word list synchronization
  - Backup and restore

### 📁 Utilities
Located in: `lib/utils/`

- **`appassets.dart`**
  - Asset path constants
  - Resource management
  - Asset loading helpers

- **`apptextstyle.dart`**
  - Text style definitions
  - Typography constants
  - Style helpers

### 📁 Widgets
Located in: `lib/widgets/`

- **`custom_toggle_switch.dart`**
  - Custom toggle switch component
  - Theme-aware styling
  - Animation

- **`orange_button.dart`**
  - Primary action button
  - Branded button style
  - Loading state

- **`back_button.dart`**
  - Custom back button
  - Navigation helper
  - Themed appearance

- **`phone_number_input.dart`**
  - Phone number input field
  - Country code picker
  - Validation

- **`otp_input.dart`**
  - OTP input fields
  - Auto-focus management
  - Paste handling

- **`rate_app_modal.dart`**
  - App rating dialog
  - Store redirect
  - Feedback collection

- **`account_section.dart`**
  - Account info section
  - Profile display
  - Account actions

---

## Configuration Files

### 📁 Project Configuration
- **`pubspec.yaml`**
  - Flutter dependencies
  - Asset declarations
  - Platform configurations
  - Version information

- **`pubspec.lock`**
  - Locked dependency versions
  - Dependency tree

- **`analysis_options.yaml`**
  - Dart analyzer rules
  - Linting configuration
  - Code quality settings

### 📁 Android Configuration
Located in: `android/`

- **`build.gradle.kts`**
  - Android project configuration
  - Plugin dependencies
  - Build settings

- **`settings.gradle.kts`**
  - Gradle project settings
  - Module declarations
  - Plugin repositories

- **`gradle.properties`**
  - Gradle properties
  - Build options
  - Performance settings

- **`local.properties`**
  - Local SDK paths
  - Machine-specific settings

- **`app/build.gradle.kts`**
  - App module configuration
  - Dependencies
  - Build variants
  - Signing configs

- **`app/src/main/AndroidManifest.xml`**
  - App permissions
  - Activities and services
  - Intent filters
  - Keyboard service declaration

### 📁 iOS Configuration
Located in: `ios/`

- **`Podfile`**
  - CocoaPods dependencies
  - Pod configurations
  - Platform requirements

- **`Runner.xcodeproj/project.pbxproj`**
  - Xcode project file
  - Build settings
  - Target configurations
  - File references

- **`Runner/Info.plist`**
  - iOS app configuration
  - Permissions
  - App metadata

- **`KeyboardExtension/Info.plist`**
  - Keyboard extension configuration
  - Extension properties
  - Capabilities

### 📁 Firebase Configuration
- **`firebase.json`**
  - Firebase project settings
  - Hosting configuration
  - Functions configuration

- **`firestore.rules`**
  - Firestore security rules
  - Access control
  - Data validation

- **`firestore.indexes.json`**
  - Firestore database indexes
  - Query optimization

### 📁 Data Connect
Located in: `dataconnect/`

- **`dataconnect.yaml`**
  - Data Connect configuration

- **`schema/*.gql`**
  - GraphQL schemas

- **`example/*.gql`**
  - Example queries

---

## Asset Files

### 📁 Dictionaries
Located in: `assets/dictionaries/`

- **Word frequency files** (`.txt`)
  - English word lists
  - Language-specific dictionaries
  - Common phrases

- **Dictionary metadata** (`.json`)
  - Dictionary configurations
  - Word metadata
  - Language mappings

### 📁 Transliteration
Located in: `assets/transliteration/`

- **Transliteration mapping files** (`.json`)
  - Script conversion rules
  - Phonetic mappings
  - Language-specific transliteration

### 📁 Fonts
Located in: `assets/fonts/`

- **Font files** (`.ttf`)
  - Custom keyboard fonts
  - Language-specific fonts
  - Icon fonts

### 📁 Icons & Images
Located in: `assets/icons/` and `assets/images/`

- **Icon files** (`.png`, `.svg`)
  - App icons
  - Feature icons
  - UI elements

- **Image files** (`.png`, `.gif`, `.svg`)
  - Onboarding images
  - Tutorial graphics
  - Background images

### 📁 Sounds
Located in: `assets/sounds/`

- **Sound effect files** (`.wav`)
  - Key press sounds
  - Feedback sounds
  - Notification sounds

### 📁 Keyboards
Located in: `assets/keyboards/`

- **Keyboard preview images** (`.png`)
  - Layout previews
  - Theme previews

---

## Functional Component Mapping

### 🔤 **Keyboard Core Functionality**

#### Text Input/Output
- **Android:** `AIKeyboardService.kt`
- **iOS:** `KeyboardViewController.swift`
- **Functions:** Text insertion, deletion, cursor management, selection handling

#### Keyboard Layout
- **Android:** `SwipeKeyboardView.kt`, `KeyboardLayoutManager.kt`
- **iOS:** `LayoutManager.swift`, `NumberLayoutManager.swift`, `KeyButton.swift`
- **Functions:** Key rendering, layout switching, touch detection

#### Shift/Caps Management
- **Android:** `CapsShiftManager.kt` (integrated in AIKeyboardService)
- **iOS:** `KeyboardViewController.swift` (3-state FSM)
- **Functions:** Shift state tracking, caps lock, auto-capitalization

---

### 🎯 **Toolbar Components**

#### Suggestion Display
- **Android:** Suggestion container in `AIKeyboardService.kt`
- **UI Components:** LinearLayout with suggestion chips
- **Functions:** `updateSuggestions()`, `createSuggestionContainer()`, `refreshSuggestionRow()`

#### Suggestion Generation
- **Sources:**
  - `PredictiveTextEngine.kt` - Next word prediction
  - `predict/NextWordPredictor.kt` - Advanced prediction
  - `SuggestionsPipeline.kt` - Suggestion coordination
  - `predict/SuggestionRanker.kt` - Suggestion ranking
  - `UnifiedAutocorrectEngine.kt` - Correction suggestions
  - `EmojiSuggestionEngine.kt` - Emoji suggestions

#### Toolbar Buttons
- **Language Switcher:** `LanguageSwitchView.kt`
- **AI Features:** Opens `AIFeaturesPanel.kt`
- **Settings:** Mini-settings panel in `AIKeyboardService.kt`

---

### 🎨 **Layout & Theming**

#### Theme Management
- **Android:** `ThemeManager.kt`, `themes/ThemeModels.kt`
- **iOS:** Theme colors in `LayoutManager.swift`
- **Flutter:** `theme_manager.dart`, `theme/theme_v2.dart`

#### Visual Styling
- **Keys:** `SwipeKeyboardView.kt` - Key rendering with theme
- **Toolbar:** Dynamic styling in `AIKeyboardService.kt`
- **Panels:** Theme-aware panels in `AIFeaturesPanel.kt`

#### Font Management
- **Android:** `FontManager.kt`
- **Assets:** `assets/fonts/*.ttf`

---

### 📝 **Suggestion System**

#### Word Prediction
1. **`PredictiveTextEngine.kt`** - Main prediction engine
2. **`predict/NextWordPredictor.kt`** - N-gram based prediction
3. **`WordDatabase.kt`** - Word frequency data
4. **`word_trie.dart`** - Trie-based word lookup

#### Autocorrect
1. **`UnifiedAutocorrectEngine.kt`** - Unified correction system
2. **`AutocorrectEngine.kt`** - Core algorithms
3. **`EnhancedAutocorrectEngine.kt`** - ML-enhanced corrections
4. **`SwipeAutocorrectEngine.kt`** - Swipe-specific corrections

#### Suggestion Flow
```
User types → AIKeyboardService.kt
            ↓
SuggestionsPipeline.kt (coordinator)
            ↓
├─→ PredictiveTextEngine.kt (predictions)
├─→ UnifiedAutocorrectEngine.kt (corrections)
├─→ EmojiSuggestionEngine.kt (emojis)
└─→ NextWordPredictor.kt (next word)
            ↓
SuggestionRanker.kt (ranking)
            ↓
Display in toolbar (updateSuggestions)
```

---

### 👆 **Swipe Functions**

#### Swipe Detection
- **Primary:** `SwipeKeyboardView.kt`
  - Touch tracking: `onTouchEvent()`
  - Path rendering: `onDraw()`
  - Gesture recognition
  - Swipe path calculation

#### Swipe Processing
- **Engine:** `SwipeAutocorrectEngine.kt`
  - Path-to-word conversion
  - Swipe pattern matching
  - Word disambiguation

#### Swipe Callbacks
- **Interface:** `SwipeListener` in `SwipeKeyboardView.kt`
  - `onSwipeDetected(swipedKeys, swipePattern, keySequence)`
  - `onSwipeStarted()`
  - `onSwipeEnded()`

#### Swipe Settings
- **Constants in SwipeKeyboardView:**
  - `MIN_SWIPE_TIME = 300L` ms
  - `MIN_SWIPE_DISTANCE = 100f` pixels
  - `SWIPE_START_THRESHOLD = 50f` pixels

---

### 🤖 **AI Features**

#### AI Panel
- **Main:** `AIFeaturesPanel.kt`
- **Functions:** Grammar check, tone adjustment, smart replies, rewriting

#### AI Services
- **`AdvancedAIService.kt`** - Advanced AI processing
- **`StreamingAIService.kt`** - Streaming responses
- **`CleverTypeAIService.kt`** - CleverType features
- **`OpenAIService.kt`** - OpenAI integration
- **`AIServiceBridge.kt`** - Service coordination

#### AI Components
- **`CleverTypePreview.kt`** - Preview AI suggestions
- **`CleverTypeToneSelector.kt`** - Tone selection UI
- **`CustomToneManager.kt`** - Custom tone management

---

### 📋 **Clipboard**

#### Clipboard System
- **Panel:** `ClipboardPanel.kt`
- **Manager:** `ClipboardHistoryManager.kt`
- **Strip:** `ClipboardStripView.kt`
- **Model:** `ClipboardItem.kt`

#### Functions
- Copy/paste tracking
- History persistence
- Quick paste
- Cloud sync ready

---

### 😊 **Emoji System**

#### Emoji Panels
- **`GboardEmojiPanel.kt`** - Full-featured emoji panel
- **`SimpleEmojiPanel.kt`** - Simplified emoji picker
- **`EmojiPanelController.kt`** - Panel control logic

#### Emoji Data
- **`EmojiDatabase.kt`** - Emoji storage
- **`EmojiCollection.kt`** - Emoji categories
- **`EmojiSuggestionEngine.kt`** - Context-based suggestions

---

### 🌍 **Multi-Language Support**

#### Language System
- **`LanguageManager.kt`** - Language management
- **`LanguageConfig.kt`** - Language configurations
- **`LanguageDetector.kt`** - Auto-detection
- **`LanguageSwitchView.kt`** - Switcher UI

#### Dictionaries
- **`MultilingualDictionary.kt`** - Multi-language dictionary
- **`DictionaryManager.kt`** - Dictionary loading
- **`UserDictionaryManager.kt`** - User words

#### Transliteration
- **`TransliterationEngine.kt`** - Script conversion
- **`IndicScriptHelper.kt`** - Indic script support
- **Assets:** `assets/transliteration/*.json`

---

### 🎭 **Media (GIF/Stickers)**

#### Media System
- **`SimpleMediaPanel.kt`** - Media panel
- **`GifManager.kt`** - GIF management
- **`StickerManager.kt`** - Sticker management
- **`MediaCacheManager.kt`** - Media caching

---

### 🔊 **Feedback System**

#### Haptic & Audio
- **Flutter:** `keyboard_feedback_system.dart`
- **Android:** Vibration in `AIKeyboardService.kt`
- **iOS:** AudioToolbox in `KeyboardViewController.swift`

---

### ⚙️ **Settings & Configuration**

#### Settings Storage
- **Android:** SharedPreferences in `AIKeyboardService.kt`
- **iOS:** App Groups in `SettingsManager.swift`
- **Flutter:** Various settings screens

#### Settings Screens
- All located in `lib/screens/main screens/`
- 20+ settings screens for different features

---

### 🔄 **Synchronization**

#### Cloud Sync
- **`keyboard_cloud_sync.dart`** - Keyboard settings sync
- **`dictionary_cloud_sync.dart`** - Dictionary sync
- **Firebase integration** - Backend sync

#### Broadcast Communication
- **`utils/BroadcastManager.kt`** - Inter-component messaging
- Intent filters in `AIKeyboardService.kt`

---

### 📊 **Diagnostics & Logging**

#### Monitoring
- **`diagnostics/TypingSyncAuditor.kt`** - Performance tracking
- **`utils/LogUtil.kt`** - Logging utility

---

## Key Interaction Flows

### 1. **Normal Typing Flow**
```
User presses key
    ↓
SwipeKeyboardView.kt (touch detection)
    ↓
AIKeyboardService.kt (onKey callback)
    ↓
Text processing (shift, autocorrect)
    ↓
Insert text via InputConnection
    ↓
Update suggestions
```

### 2. **Swipe Typing Flow**
```
User swipes across keys
    ↓
SwipeKeyboardView.kt (path tracking)
    ↓
onSwipeDetected(keys, pattern)
    ↓
SwipeAutocorrectEngine.kt (path-to-word)
    ↓
AIKeyboardService.kt (word insertion)
    ↓
Update suggestions
```

### 3. **Suggestion Selection Flow**
```
User taps suggestion
    ↓
Suggestion chip onClick
    ↓
AIKeyboardService.kt (applySuggestion)
    ↓
Replace current word
    ↓
Update cursor position
    ↓
Request new suggestions
```

### 4. **AI Features Flow**
```
User opens AI panel
    ↓
AIFeaturesPanel.kt (display)
    ↓
User selects feature (grammar/tone)
    ↓
AdvancedAIService.kt (process text)
    ↓
Display results in panel
    ↓
User accepts → Insert into text field
```

---

## File Size Reference

### Large Files (>5,000 lines)
- `AIKeyboardService.kt` - 10,853 lines (main service)

### Medium Files (1,000-5,000 lines)
- `SwipeKeyboardView.kt` - 1,473 lines
- `AIFeaturesPanel.kt` - 944 lines
- `KeyboardViewController.swift` - 504 lines

### Typical Files (<1,000 lines)
- Most other files are under 1,000 lines
- Average size: 200-500 lines

---

## Platform Distribution

### Android Native (Kotlin)
- **60 files** in `android/app/src/main/kotlin/`
- **~30,000+ lines** of Kotlin code
- Primary keyboard implementation

### iOS Native (Swift)
- **5 files** in `ios/KeyboardExtension/`
- **~1,500 lines** of Swift code
- Native iOS keyboard

### Flutter (Dart)
- **61 files** in `lib/`
- **~15,000 lines** of Dart code
- Settings UI and app interface

---

## Critical Files for Each Function

### Must-Have Files for Basic Keyboard
1. `AIKeyboardService.kt` - Core service
2. `SwipeKeyboardView.kt` - View/rendering
3. `KeyboardLayoutManager.kt` - Layout management

### Must-Have Files for Suggestions
1. `SuggestionsPipeline.kt` - Coordination
2. `PredictiveTextEngine.kt` - Predictions
3. `UnifiedAutocorrectEngine.kt` - Corrections
4. `WordDatabase.kt` - Word data

### Must-Have Files for Swipe
1. `SwipeKeyboardView.kt` - Detection
2. `SwipeAutocorrectEngine.kt` - Processing

### Must-Have Files for Toolbar
- Toolbar is built into `AIKeyboardService.kt`
- Uses `LanguageSwitchView.kt` for language button

### Must-Have Files for AI
1. `AIFeaturesPanel.kt` - UI
2. `AdvancedAIService.kt` - Processing
3. `OpenAIService.kt` - API integration

---

## Documentation Files

This project includes extensive documentation:
- 50+ `.md` files documenting various aspects
- Architecture guides
- Fix reports
- Implementation summaries
- Testing guides

---

## Summary Statistics

- **Total Android Kotlin Files:** 60
- **Total iOS Swift Files:** 5
- **Total Flutter Dart Files:** 61
- **Total Configuration Files:** 20+
- **Total Asset Files:** 100+
- **Total Lines of Code:** ~50,000+
- **Primary Language:** Kotlin (Android implementation)
- **Secondary Language:** Dart (Flutter UI)
- **Tertiary Language:** Swift (iOS implementation)

---

## Version Control

This is a Git repository with active development:
- Main branch: `main`
- Multiple feature branches documented in reports
- Extensive commit history
- Regular updates and fixes

---

*Document Version: 1.0*
*Last Updated: October 10, 2025*
*Project: AI Keyboard (Flutter + Native Android/iOS)*


