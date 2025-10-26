# 🔧 Keyboard Blank Keys Fix

## ❌ Problem
Keyboard was displaying with blank keys - no labels or icons visible, even though the key structure and background colors were rendering correctly.

### Root Cause Analysis
The `UnifiedKeyboardView` was a `LinearLayout` that tried to draw keys directly on itself using `onDraw()`. This approach had two critical issues:

1. **Missing `setWillNotDraw(false)`**: `LinearLayout` by default has `willNotDraw = true`, which completely disables `onDraw()` calls
2. **Wrong canvas layer**: Even if `onDraw()` was called, keys drawn on the parent `LinearLayout` canvas would appear BEHIND its child views (`suggestionContainer`, `toolbarContainer`, `bodyContainer`)

## ✅ Solution
Created a dedicated `KeyboardGridView` inner class that:
- Extends `View` (not `ViewGroup`)
- Calls `setWillNotDraw(false)` in its `init` block ✅
- Gets added as a child to `bodyContainer` (not drawing on parent)
- Handles all key drawing and touch events independently

### Architecture Changes

**Before (Broken)**:
```
UnifiedKeyboardView (LinearLayout)
├─ suggestionContainer
├─ toolbarContainer
└─ bodyContainer (FrameLayout)
   └─ ❌ Keys drawn on parent canvas (invisible!)
```

**After (Fixed)**:
```
UnifiedKeyboardView (LinearLayout)
├─ suggestionContainer
├─ toolbarContainer
└─ bodyContainer (FrameLayout)
   └─ KeyboardGridView (View) ✅
      └─ Keys drawn here (visible!)
```

## 📝 Code Changes

### 1. Added `KeyboardGridView` Inner Class
**Location**: `UnifiedKeyboardView.kt` lines 734-1108

```kotlin
private class KeyboardGridView(
    context: Context,
    private val model: LanguageLayoutAdapter.LayoutModel,
    private val themeManager: ThemeManager,
    // ... other params
) : View(context) {
    
    init {
        setWillNotDraw(false) // ✅ CRITICAL FIX
        setBackgroundColor(Color.TRANSPARENT)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val palette = themeManager.getCurrentPalette()
        canvas.drawColor(palette.keyboardBg)
        dynamicKeys.forEach { key ->
            drawKey(canvas, key, palette)
        }
    }
    
    // ... drawing and touch handling methods
}
```

### 2. Modified `buildKeyboardGrid()` Method
**Location**: `UnifiedKeyboardView.kt` lines 333-363

Changed from building keys inline to creating and attaching `KeyboardGridView`:

```kotlin
private fun buildKeyboardGrid(model: LanguageLayoutAdapter.LayoutModel) {
    // Remove old grid view if any
    keyboardGridView?.let { bodyContainer.removeView(it) }

    // Create new keyboard grid view
    keyboardGridView = KeyboardGridView(
        context = context,
        model = model,
        themeManager = themeManager,
        // ...
    )

    // Add to body container
    bodyContainer.addView(keyboardGridView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    ))
}
```

### 3. Removed Direct Drawing from UnifiedKeyboardView
- Removed `onDraw()` override from `UnifiedKeyboardView`
- Removed `drawKeyboardGrid()` and related drawing methods from parent
- All drawing logic moved to `KeyboardGridView`

## 🎯 Key Features of KeyboardGridView

### Drawing Features
✅ Dynamic key layout calculation
✅ Theme-aware backgrounds (key drawable, special key drawable)
✅ Icon rendering (backspace, enter, shift, etc.) with proper tinting
✅ Text label rendering with proper font, size, and color
✅ Long-press hints (small accent character in corner)
✅ Language label on spacebar
✅ RTL layout support

### Touch Handling
✅ Key tap detection
✅ Long-press timer for accent characters
✅ Accent popup window for character variants
✅ Proper touch event forwarding to parent

### Layout Features
✅ Adaptive key sizing based on available height
✅ Dynamic width calculation (space = 5.5x, shift/backspace = 2x, etc.)
✅ Proper key spacing (horizontal and vertical)
✅ Borderless mode support

## 🔍 Why This Fix Works

1. **`View.onDraw()` is always called**: Unlike `LinearLayout`, a plain `View` with `setWillNotDraw(false)` guarantees `onDraw()` execution
2. **Correct canvas layer**: Keys are drawn on the `KeyboardGridView`'s own canvas, which is properly positioned inside `bodyContainer`
3. **Independent lifecycle**: `KeyboardGridView` handles its own layout, drawing, and touch events without interfering with parent
4. **Proper invalidation**: Calling `invalidate()` on `KeyboardGridView` triggers only its `onDraw()`, not the entire parent hierarchy

## 📊 Expected Result

**Before**: Blank white keys (structure visible, no labels)  
**After**: Fully labeled keyboard with:
- Letter labels (A, B, C, etc.)
- Icons (←, ⏎, ⇧, 🌐)
- Accent hints (é, ñ, etc. shown in corner)
- Language indicator on spacebar
- Proper color coding (white keys, orange special keys)

## 🧪 Testing Checklist

- [ ] Keys display with correct labels
- [ ] Icons render for special keys (backspace, enter, shift)
- [ ] Tap works (character input)
- [ ] Long-press shows accent variants
- [ ] Theme changes update key colors
- [ ] RTL layouts display correctly
- [ ] Number row toggles properly
- [ ] Symbol/emoji mode switches work

## 🐛 Potential Issues & Fixes

**If keys still don't show**:
1. Check `keyTextPaint` and `spaceLabelPaint` initialization
2. Verify `themeManager.createKeyTextPaint()` returns non-null
3. Check if `buildKeys()` is being called (look for log: "✅ Built X keys")
4. Verify `model.rows` is not empty

**If touch doesn't work**:
1. Ensure `KeyboardGridView` is clickable
2. Check `onKeyCallback` is properly passed and not null
3. Verify `findKeyAtPosition()` logic matches key coordinates

## 📚 Related Files
- `UnifiedKeyboardView.kt` - Main fix location
- `AIKeyboardService.kt` - Uses UnifiedKeyboardView
- `UnifiedLayoutController.kt` - Calls `showTypingLayout()`
- `ThemeManager.kt` - Provides paints and drawables

## ✅ Status
- ✅ Code refactored
- ✅ Compilation errors fixed
- ✅ Linter checks passed
- ⏳ Runtime testing (deploy to device)

---

**Last Updated**: 2025-10-22  
**Issue**: Blank keyboard keys  
**Solution**: Dedicated `KeyboardGridView` with proper `setWillNotDraw(false)`

