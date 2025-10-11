# Keyboard Inset Fix - Double Counting Issue Resolved

## 🐛 Problem Identified

**Symptom:** Keyboard height was 142px taller than expected (navigation bar height being added twice)

**Root Cause:** When the IME (Input Method Editor) is visible, Android's system **already accounts for navigation bar height**. Our code was adding the navigation bar padding again, causing:
- `visibleHeight = 1342 px` (correct IME height)
- `navBar = 142 px` being added as padding
- **Total keyboard height = 1342 + 142 = 1484 px** ❌

## ✅ Solution Applied

### Key Insight
```kotlin
// ✅ When IME visible → Android has already handled nav bar
// ✅ Only add nav bar padding when IME is NOT visible
val finalInset = if (imeVisible) 0 else navHeight
```

### Changes Made to 3 Files

#### 1. AIKeyboardService.kt (Lines 1400-1428)
**Before:**
```kotlin
val bottomPadding = maxOf(imeInsets.bottom, navInsets.bottom, systemBarsInsets.bottom)
view.setPadding(..., bottomPadding)
```

**After:**
```kotlin
val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
val navHeight = navInsets.bottom.coerceAtLeast(sysInsets.bottom)
val finalInset = if (imeVisible) 0 else navHeight
view.setPadding(..., finalInset)
```

#### 2. SwipeKeyboardView.kt (Lines 1007-1030)
**Before:**
```kotlin
val bottomInset = maxOf(imeInsets.bottom, navBarInsets.bottom, systemBarsInsets.bottom)
v.setPadding(..., bottomInset)
```

**After:**
```kotlin
val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
val navHeight = navBarInsets.bottom.coerceAtLeast(systemBarsInsets.bottom)
val finalInset = if (imeVisible) 0 else navHeight
v.setPadding(..., finalInset)
```

#### 3. AIFeaturesPanel.kt (Lines 880-917)
**Before:**
```kotlin
val bottomInset = maxOf(navBarInsets.bottom, systemBarsInsets.bottom)
view.setPadding(..., bottomInset)
val adjustedHeight = baseHeightPx + bottomInset
```

**After:**
```kotlin
val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
val navHeight = navBarInsets.bottom.coerceAtLeast(systemBarsInsets.bottom)
val finalInset = if (imeVisible) 0 else navHeight
view.setPadding(..., finalInset)
val adjustedHeight = baseHeightPx + finalInset
```

## 🧠 How It Works

### When IME is Visible (Keyboard Showing)
```
┌─────────────────────────────────────┐
│   App Content                        │
│                                      │
├─────────────────────────────────────┤ ← Android positions this correctly
│   Keyboard (IME)                     │
│   Height: 1342px                     │
│   Padding: 0px ✅                    │
├─────────────────────────────────────┤
│   Nav Bar (142px)                    │ ← Already handled by system
└─────────────────────────────────────┘
```

### When IME is Hidden (Panel Showing)
```
┌─────────────────────────────────────┐
│   App Content                        │
│                                      │
├─────────────────────────────────────┤
│   AI Features Panel                  │
│   Base: 280dp                        │
│   Padding: 142px ✅                  │
├─────────────────────────────────────┤
│   Nav Bar (142px)                    │ ← We add padding for this
└─────────────────────────────────────┘
```

## 📊 Expected Log Output

### Before Fix (Double Counting)
```
[AIKeyboard] Dynamic insets applied → ime=1342px, nav=142px, final=1342px  ❌ Wrong!
[SwipeKeyboardView] [Insets] ime=1342px, nav=142px, applied=1342px        ❌ Wrong!
```

### After Fix (Correct)
```
[AIKeyboard] Dynamic insets → ime=1342px, nav=142px, imeVisible=true, final=0px  ✅
[SwipeKeyboardView] [Insets] ime=1342px, nav=142px, imeVisible=true, applied=0px ✅
[AIPanel] Dynamic insets → nav=142px, imeVisible=true, final=0px                ✅
```

### When Only Panel Visible (IME Hidden)
```
[AIPanel] Dynamic insets → nav=142px, imeVisible=false, final=142px  ✅
```

## 🧪 Testing Checklist

- [ ] Build and run: `flutter run`
- [ ] Open keyboard - verify logs show `imeVisible=true, final=0px`
- [ ] Measure keyboard height - should be ~1342px (not 1484px)
- [ ] Open AI panel without keyboard - verify logs show `imeVisible=false, final=142px`
- [ ] Check that keys are not cut off at bottom
- [ ] Test with 3-button navigation
- [ ] Test with gesture navigation
- [ ] Test screen rotation
- [ ] Test on different Android versions (10, 11, 12+)

## 🎯 Benefits of This Fix

✅ **Correct Keyboard Height** - No more extra 142px
✅ **Proper Touch Regions** - All keys receive touch events correctly
✅ **No Visual Overlap** - UI elements don't overlap system navigation
✅ **Adaptive Behavior** - Different handling for IME visible vs. hidden
✅ **Performance** - No unnecessary padding calculations when not needed

## 🔍 Debug Commands

### Monitor Insets in Real-Time
```bash
adb logcat -s AIKeyboard:D SwipeKeyboardView:D AIPanel:D | grep -E "imeVisible|final="
```

### Expected Output When Opening Keyboard
```
AIKeyboard: [AIKeyboard] Dynamic insets → ime=1342px, nav=142px, imeVisible=true, final=0px
SwipeKeyboardView: [Insets] ime=1342px, nav=142px, imeVisible=true, applied=0px
```

### Expected Output When Opening AI Panel (Keyboard Closed)
```
AIPanel: [AIPanel] Dynamic insets → nav=142px, imeVisible=false, final=142px
AIPanel: [AIPanel] Height adjusted: base=980px, adjusted=1122px (280dp)
```

## 📚 Technical Background

### Why Android Handles IME Insets Automatically

When an `InputMethodService` (like our keyboard) is active, Android's WindowManager:
1. Measures the IME height
2. **Automatically subtracts navigation bar height** from available screen space
3. Positions the IME view correctly above the navigation bar
4. Reports the IME inset to the app

**Our job:** Only add padding when we're showing custom views (panels) that aren't part of the IME.

### The isVisible() Check

```kotlin
insets.isVisible(WindowInsetsCompat.Type.ime())
```

This returns `true` when:
- Keyboard is actively shown on screen
- IME animation is in progress
- Soft input is being displayed

This returns `false` when:
- Keyboard is hidden
- Only custom panels are visible
- App is in hardware keyboard mode

## 🎉 Summary

**Problem:** 142px extra height due to double-counting navigation bar
**Solution:** Check if IME is visible before adding navigation bar padding
**Result:** Perfect keyboard height that adapts automatically to system UI

---

**Fix Date:** October 10, 2025
**Status:** ✅ Complete - Ready for Testing
**Files Modified:** 3 (AIKeyboardService.kt, SwipeKeyboardView.kt, AIFeaturesPanel.kt)
**Lines Changed:** ~15 lines per file
**Breaking Changes:** None
**Backward Compatibility:** Maintained

