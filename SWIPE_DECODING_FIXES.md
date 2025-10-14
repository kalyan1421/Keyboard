# ✅ Swipe Decoding Fixes Applied

## Problem Identified ✅ FIXED

**Root cause**: SwipeKeyboardView collected finger path coordinates in `swipePoints` during move events, but the SwipeListener interface only reported strings (`swipedKeys`, `swipePattern`, `keySequence`) - **no coordinates**. The path never reached the decoding service.

## 3 Surgical Fixes Applied

### 1. ✅ Extended SwipeListener Interface (SwipeKeyboardView.kt)

**Before:**
```kotlin
interface SwipeListener {
    fun onSwipeDetected(swipedKeys: List<Int>, swipePattern: String, keySequence: List<Int> = swipedKeys)
}
```

**After:**
```kotlin  
interface SwipeListener {
    fun onSwipeDetected(
        swipedKeys: List<Int>, 
        swipePattern: String, 
        keySequence: List<Int> = swipedKeys,
        swipePath: List<Pair<Float, Float>> = emptyList() // NEW: actual finger coordinates
    )
}
```

**And updated the call site:**
```kotlin
// Convert swipePoints to normalized coordinates before passing to listener
val normalizedPath = swipePoints.map { point ->
    Pair(point[0] / width.toFloat(), point[1] / height.toFloat())
}

swipeListener?.onSwipeDetected(swipedKeys, swipePattern, keySequence, normalizedPath)
```

### 2. ✅ Updated AIKeyboardService to Use Path (AIKeyboardService.kt)

**Before:** String-based decoding with fallbacks
```kotlin
val swipeLetters = swipeSequence.toString()
val swipeResult = swipeAutocorrectEngine.getCandidates(swipeLetters, prev1, prev2)
```

**After:** Path-based decoding, no fallbacks
```kotlin
// Skip if path is too short (accidental taps)
if (swipePath.size < 2) return

// Use path-based swipe decoding (NOT string-based)
val candidates = swipeAutocorrectEngine.getUnifiedCandidates(
    swipePath = swipePath,
    typedSequence = swipePattern,  // Optional fallback pattern
    previousWord = prevWord,
    currentLanguage = currentLanguage
)

if (candidates.isNotEmpty()) {
    val bestCandidate = candidates.first()
    currentInputConnection?.commitText("$bestCandidate ", 1)
    updateSuggestionUI(candidates.take(3))
} else {
    // NO FALLBACK - path decoding failed, don't type anything
    updateSuggestionUI(emptyList())
}
```

### 3. ✅ Added Path-First API to SwipeAutocorrectEngine

**New Primary Method:**
```kotlin
suspend fun getUnifiedCandidates(
    swipePath: List<Pair<Float, Float>>,
    typedSequence: String = "",
    previousWord: String = "",
    currentLanguage: String = "en",
    maxSuggestions: Int = 5
): List<String> {
    val swipePathObj = SwipePath(swipePath)
    val swipeResult = unified.suggestForSwipe(swipePathObj, context)
    return swipeResult.take(maxSuggestions).map { it.text }
}
```

## ✅ Enhanced UnifiedAutocorrectEngine Decoding

**Implemented proper lattice-based decoding:**

1. **Points → Key Lattice**: `candidatesForEachPoint()` finds 1-2 nearest keys per touch point
2. **Beam Search**: `beamDecode()` generates fuzzy prefixes from lattice (e.g., "qyal", "qual", "quark")  
3. **Dictionary Query**: Finds all words starting with those prefixes (NOT just first letter)
4. **Metrics Calculation**: `SwipeMetrics` with pathScore, proximity, editDistance
5. **Unified Ranking**: `rankSwipeCandidates()` blends path metrics with LM/frequency

**Key scoring components:**
```kotlin
score = wFreq * ln(1.0 / (freq + 1)) +
        wLM * lm +
        wPath * pathScore +
        wProx * (1.0 / (1.0 + proximity)) +
        wEdit * (-editDistance) +
        wUser * userBoost +
        wCorr * correctionBoost
```

**Weights:** `wPath=1.6` (highest), `wLM=1.0`, `wEdit=0.8`, `wFreq=0.6`, `wProx=0.6`

## ✅ Removed Static Fallbacks

1. **No more "top N common words"** filtered by first letter
2. **No more common_words.json** as swipe fallback  
3. **No typing suggestions** for swipe mode
4. **No string pattern fallbacks** - coordinates required

## Expected Behavior Changes

### ✅ Debug Logs Will Show:
```
[Swipe] Decoding path with 37 points
[Swipe] lattice sizes: [2,2,2,1,1,...]  points=37
[Swipe] beam top prefixes: qyal, qval, quak, qual, qyalt ...
[Swipe] rawCandidates: 286 (first 10: quality, qualify, equality, quail, ...)
[Swipe] snapped key sequence: 'qyality'
[Unified] swipe-rank: word=quality path=1.42 prox=0.31 edit=1 lm=92 freq=503
✅ Swipe decoded: path(37 points) → 'quality' (286 alternatives, 125ms)
```

### ✅ No More:
```
❌ "cannot decode properly without real coordinates!"
❌ "Swipe fallback: '<raw>'"
❌ "Generated 0 unified candidates"
❌ Same Q-words for different paths
```

### ✅ User Experience:
- **Zig-zag across top row**: Should NOT return only Q-words anymore
- **Different paths**: Different candidates based on actual finger movement  
- **Failed swipes**: No junk text typed (clean fails)
- **Path geometry**: Actually influences ranking (better path fit = higher score)

## Files Modified ✅

1. **SwipeKeyboardView.kt** - Extended interface, pass coordinates
2. **AIKeyboardService.kt** - Use path-based API, remove fallbacks  
3. **SwipeAutocorrectEngine.kt** - Added path-first entry point
4. **UnifiedAutocorrectEngine.kt** - Proper lattice decoding + metrics

## Result

**The swipe path data flow is now complete:**
```
User finger → SwipeKeyboardView.swipePoints → SwipeListener.swipePath → 
AIKeyboardService.onSwipeDetected → SwipeAutocorrectEngine.getUnifiedCandidates →
UnifiedAutocorrectEngine.suggestForSwipe → Lattice decoding + unified ranking
```

**All surgical fixes applied. Coordinates now flow from UI to decoder as intended.**
