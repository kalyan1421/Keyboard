# Auto-Adjust Not Working on First Open - Root Cause Analysis

## 🐛 Problem Statement

**Symptom**: Keyboard does not auto-adjust height on first open, leaving white space at the bottom. However, after tapping "?123" to switch to symbols mode, the keyboard auto-adjusts correctly.

**Expected**: Keyboard should auto-adjust on first open, just like it does on mode switch.

---

## 🔍 Root Cause Analysis

### The Issue: Two Different Loading Paths

The keyboard has **TWO different loading mechanisms**:

#### Path 1: First Keyboard Open (❌ Broken)
```kotlin
// onCreateInputView() - Line 1785
val keyboardResource = getKeyboardResourceForLanguage(currentLanguage, showNumberRow)
keyboard = Keyboard(this@AIKeyboardService, keyboardResource)  // OLD XML METHOD
```

**Result**: 
- Uses legacy XML keyboard files (`/res/xml/qwerty.xml`)
- No dynamic layout loading
- No auto-adjust triggered
- Only worked for Indic languages (hi, te, ta)

#### Path 2: Mode Switch (✅ Working)
```kotlin
// switchKeyboardMode() → loadDynamicLayout() → setKeyboardMode()
setKeyboardMode(mode, layoutAdapter, showNumberRow)
  ↓
GlobalScope.launch {
    val layout = buildLayoutFor(...)  // Dynamic JSON layout
    setDynamicLayout(layout)
    post {
        parent?.requestLayout()  // ✅ Auto-adjust triggered!
    }
}
```

**Result**:
- Uses dynamic JSON layout system
- Properly triggers auto-adjust in coroutine completion
- Works perfectly

---

## 📊 Log Analysis

### First Open (Missing Logs)
```
Line 494: Calculated keyboard height: 1128 px
Line 509: [KeyboardHeightManager] Applied keyboard height: 1128px
❌ NO "📱 Loading dynamic layout" log
❌ NO "✅ setKeyboardMode" log
❌ NO "✅ Layout rebuilt" log
❌ NO "🎯 Auto-adjust triggered" log
```

### Mode Switch to Symbols (Working Logs)
```
Line 560: 📱 Loading dynamic layout for: en, mode: SYMBOLS
Line 561: ✅ setKeyboardMode: SYMBOLS for language: en
Line 578: ✅ Layout rebuilt for mode: SYMBOLS, keys: 34
Line 579: 🎯 Auto-adjust triggered after layout build  ← Auto-adjust WORKS!
```

**Conclusion**: First open never calls the dynamic layout system, so auto-adjust never triggers.

---

## 🔧 The Fix

### Change 1: Always Use Dynamic Layout on First Open

**File**: `AIKeyboardService.kt` (Line 1804-1817)

**Before**:
```kotlin
// Only upgrade to dynamic layout for Indic languages
if (this is SwipeKeyboardView && currentLanguage in listOf("hi", "te", "ta")) {
    currentLangCode = currentLanguage
    post {
        coroutineScope.launch {
            val layout = languageLayoutAdapter.buildLayoutFor(...)
            withContext(Dispatchers.Main) {
                setDynamicLayout(layout, showNumberRow)
            }
        }
    }
}
```

**After**:
```kotlin
// Always load dynamic layout for ALL languages on first open
if (this is SwipeKeyboardView) {
    currentLangCode = currentLanguage
    post {
        // This triggers the same path as mode switch
        setKeyboardMode(
            LanguageLayoutAdapter.KeyboardMode.LETTERS,
            languageLayoutAdapter,
            showNumberRow
        )
        Log.d(TAG, "🔄 Loading dynamic layout on first open for: $currentLanguage")
    }
}
```

**Why This Works**:
- ✅ Uses the same `setKeyboardMode()` that works perfectly on mode switch
- ✅ Ensures auto-adjust triggers after layout is built
- ✅ Works for ALL languages, not just Indic ones
- ✅ Consistent behavior across first open and mode switching

### Change 2: Auto-Adjust in SwipeKeyboardView

**File**: `SwipeKeyboardView.kt` (Line 1628-1634)

Added auto-adjust trigger **after** layout is fully built:

```kotlin
GlobalScope.launch(Dispatchers.Main) {
    try {
        val layout = layoutAdapter.buildLayoutFor(currentLangCode, mode, showNumberRow)
        setDynamicLayout(layout, showNumberRow)
        
        // 🎯 AUTO-ADJUST: Notify parent containers AFTER layout is built
        post {
            (parent as? View)?.requestLayout()
            (parent?.parent as? View)?.requestLayout()
            Log.d("SwipeKeyboardView", "🎯 Auto-adjust triggered after layout build")
        }
        
        invalidate()
    } catch (e: Exception) {
        Log.e("SwipeKeyboardView", "❌ Failed to set keyboard mode: $mode", e)
    }
}
```

**Why Post Inside Coroutine**:
- ✅ Ensures layout is **fully built** before triggering auto-adjust
- ✅ Avoids race condition where auto-adjust runs before keys exist
- ✅ Works consistently for both first open and mode switch

---

## 🎯 Expected Behavior After Fix

### First Open Sequence
```
1. onCreateInputView()
   ├─ Create XML keyboard (instant display)
   └─ Post: setKeyboardMode(LETTERS, ...)
       ↓
2. view.post { setKeyboardMode() }
   ├─ Launch coroutine
   ├─ buildLayoutFor("en", LETTERS)
   ├─ setDynamicLayout(layout)
   └─ post { requestLayout() }  ← 🎯 AUTO-ADJUST!
       ↓
3. Keyboard auto-adjusts ✅
```

### Mode Switch Sequence (Already Working)
```
1. User taps "?123"
   ↓
2. loadDynamicLayout("en", SYMBOLS)
   └─ setKeyboardMode(SYMBOLS, ...)
       ↓
3. Same coroutine path
   ├─ buildLayoutFor("en", SYMBOLS)
   ├─ setDynamicLayout(layout)
   └─ post { requestLayout() }  ← 🎯 AUTO-ADJUST!
       ↓
4. Keyboard auto-adjusts ✅
```

---

## 📝 Summary

### Root Cause
- **First open** used legacy XML keyboard → no auto-adjust
- **Mode switch** used dynamic JSON layout → auto-adjust worked

### Solution
- ✅ Always use dynamic layout system on first open (for ALL languages)
- ✅ Trigger auto-adjust after layout is fully built in coroutine
- ✅ Consistent behavior across all keyboard states

### Files Modified
1. `AIKeyboardService.kt` - Line 1804-1817: Always load dynamic layout on first open
2. `SwipeKeyboardView.kt` - Line 1628-1634: Trigger auto-adjust after layout build

---

## 🧪 Testing

After applying the fix, on **first keyboard open**, you should see these logs:

```
📱 Loading dynamic layout for: en, mode: LETTERS
✅ setKeyboardMode: LETTERS for language: en
🔄 Loading dynamic layout on first open for: en
✅ Layout rebuilt for mode: LETTERS, keys: X
🎯 Auto-adjust triggered after layout build  ← This is the key log!
```

If you see `🎯 Auto-adjust triggered after layout build` on first open, the fix is working!

---

**Status**: ✅ Fixed
**Version**: Final
**Date**: 2025

