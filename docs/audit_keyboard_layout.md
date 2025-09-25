# 📝 Audit: Keyboard Layout & Behavior Responsibilities

This document maps the existing AI Keyboard codebase to the responsibilities related to layout calculation, key rendering, hit testing, suggestion strip, theme integration, and behavior implementation on both Android and iOS.

---

## 1. Android IME (Kotlin)

### 1.1 Layout Calculation
- **SwipeKeyboardView.calculateKeyboardLayout** (implicit in `KeyboardView`): grid positioning based on `Keyboard` XML and view dimensions.
- **SwipeKeyboardView.calculateClipboardKeyLayout**: custom grid for clipboard mode using screen width/height, padding, keyMargin.
- **AIKeyboardService.getKeyboardResourceForLanguage**: selects XML layout resource (`res/xml/qwerty.xml`, `qwerty_with_numbers.xml`, etc.) based on language/number row.
- **res/xml/**
  - `keyboard_view_google_layout.xml` (base layout for `SwipeKeyboardView`).
  - **`res/xml/qwerty*.xml`**: key coordinates and spans for QWERTY, numbers, and symbols layers.

### 1.2 Key Drawing & Rendering
- **SwipeKeyboardView.onDraw(canvas: Canvas)**: overrides default draw; dispatches to `drawClipboardLayout` or themed drawing.
- **SwipeKeyboardView.drawThemedKey(canvas, key)**: draws individual key background, border, and label with theme paints.
- **SwipeKeyboardView.drawClipboardLayout**, **drawReplacementUI / showGrammarCorrectionUI** in service use `LinearLayout` + custom draw for replacement panels.

### 1.3 Hit Testing & Touch Handling
- **SwipeKeyboardView.findKeyAtPoint(x, y)**: maps touch coordinates to key index.
- **SwipeKeyboardView.onTouchEvent(MotionEvent)**: unified handler for normal, swipe, shift, and clipboard modes.
- **SwipeKeyboardView.handleSwipeTouch**, **handleShiftKeyTouch**, **handleClipboardTouch**: route input to appropriate handlers.

### 1.4 Suggestion Strip & Toolbar
- **AIKeyboardService.createSuggestionBarContainer**, **createSuggestionBar**: builds suggestion `TextView`s.
- **AIKeyboardService.updateSuggestionUI(suggestions: List<String>)**: populates suggestion strip.
- **AIKeyboardService.updateAISuggestions()**: coroutine-based AI suggestion fetch.
- **AIKeyboardService.createCleverTypeToolbar**: builds top toolbar with AI actions, emoji, GIF, clipboard, settings buttons.

### 1.5 Key Size & Spacing Constants
- **Dimensions in `SwipeKeyboardView`**:
  - `padding = 16f`, `keyMargin = 8f`, `backButtonHeight = 80f` (pixels, converted from dp).
  - Key span weights in replacement: 1.0f (normal), 1.25f (Shift, Backspace), 2.0f (Space).
- **`res/values/dimens.xml`**:
  - `toolbar_button_padding`, `toolbar_icon_size`, `toolbar_min_touch_target`, `toolbar_height`.
  - Key preview sizing in `keyboard_key_preview.xml`.

### 1.6 Behaviors Implementation
- **Shift & Caps Lock**: `handleShiftKeyTouch`, double-tap detection, `isShiftHighlighted`.
- **Backspace Auto-repeat**: long-press detection with `Handler` in `InputMethodService`.
- **Swipe Typing**: `handleSwipeTouch`, `processSwipe`, `calculateSwipePattern`, `sampleSwipePath`.
- **Space Cursor Swipe**: (to implement in `Gestures.kt` or within `SwipeKeyboardView`).
- **Backspace Slide-Delete**: gesture detection on key; (to migrate to `Gestures.kt`).
- **Long-press Diacritics**: default `KeyboardView` popup; triggered by `KeyboardView` long-press.

### 1.7 Theme Bridging & Live Updates
- **ThemeManager.kt (Android)**:
  - `reloadTheme()`: reads JSON from `SharedPreferences` and applies via `notifyThemeChanged()`.
  - Provides colors/fonts through `getCurrentTheme()` and drawable creation methods.
- **MainActivity.kt**:
  - `MethodChannel('ai_keyboard/config')` handler for `updateClipboardSettings`, `notifyThemeChange`.
  - Broadcasts to IME: `THEME_CHANGED`, `CLIPBOARD_CHANGED`, `SETTINGS_CHANGED`.

---

## 2. iOS Keyboard Extension (Swift)

### 2.1 Layout & View Controllers
- **ios/KeyboardExtension/KeyboardViewController.swift**: subclass of `UIInputViewController` managing key rows.
- **ios/KeyboardExtension/KeyboardLayout.swift** (if present) or inline `UIStackView` layout code.
- **ios/KeyboardExtension/SettingsManager.swift**: reads theme/size from App Group defaults.

### 2.2 Key Rendering & Hit Testing
- Custom `KeyView.swift`, `KeyPreviewView.swift` (if available) for drawing keys and popups.
- Gesture recognizers for touches: `touchesBegan`, mapping to `keyForPoint(_:)`.

### 2.3 Behavior Implementation
- **Shift & Caps**: state machine in `KeyboardViewController`, double-tap detection.
- **Backspace Auto-repeat**: `Timer` with acceleration.
- **Swipe Cursor**: `UIPanGestureRecognizer` on space bar.
- **Slide-Delete Word**: long-press + horizontal drag.
- **Long-press Diacritics**: `UILongPressGestureRecognizer` on key views.

### 2.4 Theme & Settings Bridge
- **App Group UserDefaults**: `UserDefaults(suiteName: 
