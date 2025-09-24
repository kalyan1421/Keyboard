# Cursor Behavior Improvements

## Overview
This document outlines the cursor behavior improvements implemented to match Google Keyboard (Gboard) reference standards for proper cursor positioning during text input, emoji insertion, and rich content handling.

## Key Improvements Implemented

### 1. Enhanced Emoji Insertion (`insertEmojiWithCursor`)
- **Problem**: Basic `commitText(emoji, 1)` didn't handle complex emoji sequences properly
- **Solution**: 
  - Proper Unicode cluster handling for multi-part emojis
  - Cursor automatically moves after emoji (1 emoji unit forward)
  - Supports complex sequences like 👨‍👩‍👧‍👦, ❤️‍🔥, skin tone modifiers

### 2. Advanced Backspace Handling (`deleteCharacterOrCluster`)
- **Problem**: Simple `deleteSurroundingText(1, 0)` broke emoji clusters
- **Solution**: 
  - Detects and deletes entire emoji clusters atomically
  - Handles surrogate pairs correctly
  - Supports family emojis, ZWJ sequences, skin tone modifiers
  - Prevents splitting multi-byte Unicode sequences

### 3. Cursor Stability During State Changes (`ensureCursorStability`)
- **Problem**: Cursor position could change during Normal → Shift → Caps Lock transitions
- **Solution**:
  - Preserves cursor position during keyboard state changes
  - Only visual keyboard state updates, cursor remains stable
  - Enhanced logging for cursor position tracking

### 4. Rich Content Support (`insertRichContent`)
- **Problem**: No proper GIF/sticker insertion via Android's `commitContent()` API
- **Solution**:
  - Implements `InputContentInfo` with proper MIME types
  - Falls back to text links if rich content not supported
  - Cursor moves after content placeholder for continued typing
  - Supports both local files and network URLs

### 5. Cursor-Aware Text Handler Utility (`CursorAwareTextHandler`)
- **New utility class** providing consistent cursor behavior across all operations:
  - `insertText()`: Standard text insertion with cursor control
  - `insertEmoji()`: Emoji-specific insertion with Unicode handling
  - `performBackspace()`: Advanced backspace with cluster detection
  - `preserveCursorPosition()`: Wrapper for cursor-stable operations
  - `getCursorInfo()`: Debugging utility for cursor position

## Emoji Pattern Recognition

The system now recognizes and properly handles:

### Basic Patterns
- **Surrogate pairs**: Standard emoji (2 bytes)
- **Variation selectors**: ❤️ (emoji + FE0F)
- **Skin tone modifiers**: 👍🏽 (emoji + skin tone)

### Complex Patterns  
- **ZWJ sequences**: 👨‍👩‍👧‍👦 (Zero Width Joiner sequences)
- **Family emojis**: Various family combinations
- **Compound emojis**: ❤️‍🔥 (heart on fire)
- **Flag emojis**: Regional indicator sequences

### Pattern Detection Order
1. Most complex patterns first (family emojis, compound sequences)
2. ZWJ sequences with multiple components
3. Skin tone modified emojis
4. Basic surrogate pairs
5. Single character fallback

## API Usage

### Text Insertion
```kotlin
// Enhanced emoji insertion
insertEmojiWithCursor("👨‍👩‍👧‍👦")

// Rich content insertion
insertRichContent("https://example.com/gif.gif", "image/gif", "Funny GIF")

// Cursor-aware text insertion
CursorAwareTextHandler.insertText(ic, "Hello", moveCursor = true)
```

### Backspace Handling
```kotlin
// Enhanced backspace (handles emoji clusters)
val deletedLength = CursorAwareTextHandler.performBackspace(ic)

// Old method (replaced)
// ic.deleteSurroundingText(1, 0) // ❌ Breaks emojis
```

### Cursor Stability
```kotlin
// Preserve cursor during operations
CursorAwareTextHandler.preserveCursorPosition(ic) {
    // Perform keyboard state changes
    updateShiftState()
    updateCapsState()
}
```

## Testing Scenarios

### Emoji Behavior
1. **Insert emoji** → Cursor moves after emoji
2. **Insert complex emoji** (👨‍👩‍👧‍👦) → Cursor moves after entire sequence
3. **Backspace after emoji** → Deletes entire emoji cluster
4. **Move cursor inside text, insert emoji** → Emoji appears at cursor, not at end

### State Transitions
1. **Type text** → **Shift** → **Caps Lock** → Cursor stays in same position
2. **Move cursor manually** → **Change keyboard state** → Cursor remains at manual position

### Rich Content
1. **Insert GIF** in supported app → Shows as rich content, cursor after placeholder
2. **Insert GIF** in unsupported app → Shows as link, cursor after link
3. **Backspace after GIF** → Removes entire content block

## Implementation Details

### Key Files Modified
- `AIKeyboardService.kt`: Main keyboard service with enhanced cursor methods
- `CursorAwareTextHandler.kt`: New utility class for consistent cursor behavior

### New Methods Added
- `insertEmojiWithCursor()`: Enhanced emoji insertion
- `deleteCharacterOrCluster()`: Smart backspace handling
- `insertRichContent()`: Rich content API integration
- `ensureCursorStability()`: Cursor position preservation

### Android API Integration
- Uses `InputConnection.commitText()` with proper cursor positioning
- Implements `InputConnection.commitContent()` for rich media
- Leverages `InputContentInfo` for proper MIME type handling

## Compatibility

- **Minimum Android Version**: API 25+ (Android 7.1) for rich content support
- **Fallback Support**: Text-based insertion for older versions
- **App Compatibility**: Automatic detection of rich content support per app

## Future Enhancements

1. **Undo Support**: Implement undo after autocorrect (backspace immediately after)
2. **Advanced Emoji Picker**: Visual emoji selection with cursor awareness  
3. **Custom Sticker Support**: Extended rich content for custom sticker formats
4. **Voice Input Integration**: Cursor-aware voice-to-text insertion
5. **Multi-language Cursor**: Language-specific cursor behavior patterns

## Debugging

Enable cursor behavior logging:
```kotlin
Log.d(TAG, CursorAwareTextHandler.getCursorInfo(ic))
```

This provides detailed cursor position information for troubleshooting cursor-related issues.
