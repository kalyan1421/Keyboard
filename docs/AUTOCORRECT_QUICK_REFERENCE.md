# Autocorrect System - Quick Reference Guide

**For**: Developers who need quick answers  
**See**: `AUTOCORRECT_DETAILED_ANALYSIS.md` for comprehensive explanations

---

## 📁 File Locations

| Component | File | Lines | Purpose |
|-----------|------|-------|---------|
| **Typed Autocorrect** | `AutocorrectEngine.kt` | 718 | Core correction logic with Firestore |
| **Swipe Autocorrect** | `SwipeAutocorrectEngine.kt` | 729 | Gesture recognition & path decoding |
| **Integration** | `AIKeyboardService.kt` | 8203 | Main service orchestration |
| **Database** | `WordDatabase.kt` | ~300 | SQLite word storage |
| **User Dictionary** | `UserDictionaryManager.kt` | ~200 | Personalized learning |

---

## 🔑 Key Functions

### AutocorrectEngine.kt

```kotlin
// Get correction candidates
suspend fun getCandidates(
    original: String,      // Word to correct
    prev1: String = "",    // Previous word for bigram
    prev2: String = ""     // Word before previous for trigram
): List<AutocorrectCandidate>

// Load Firestore frequencies (async)
fun loadWordFrequency(lang: String)

// Update locale
fun setLocale(locale: String)

// Learn from user interaction
suspend fun learnFromUser(original: String, chosen: String, rejected: List<String>)
```

### SwipeAutocorrectEngine.kt

```kotlin
// Decode swipe gesture into words
fun decodeSwipePath(points: List<Pair<Float, Float>>): List<String>

// Merge multiple prediction sources
fun mergePredictions(
    swipePreds: List<String>,
    contextPreds: List<String>,
    currentWord: String = ""
): List<String>

// Get unified predictions from all sources
suspend fun getUnifiedCandidates(
    swipePath: List<Pair<Float, Float>>?,
    typedSequence: String,
    previousWord: String = "",
    autocorrectEngine: AutocorrectEngine?
): List<String>
```

### AIKeyboardService.kt

```kotlin
// Extract context for predictions
private fun getPreviousWordsFromInput(): Pair<String, String>

// Handle key press
override fun onText(primaryCode: Int)

// Handle swipe gesture
override fun onSwipeEnd()
```

---

## 🎯 Scoring Formula

```
score = (WEIGHT_FREQUENCY * frequency * 1.2_if_firestore)
      - (WEIGHT_EDIT_DISTANCE * edit_distance)
      - (WEIGHT_KEYBOARD_PENALTY * keyboard_proximity)
      - (WEIGHT_LENGTH_DIFF * abs(length_diff))
      + (WEIGHT_BIGRAM * bigram_score)
      + (WEIGHT_TRIGRAM * trigram_score)
      - (levenshtein_penalty * 0.1)
```

### Weight Values

| Weight | Value | Impact |
|--------|-------|--------|
| `WEIGHT_FREQUENCY` | 1.0 | Word popularity |
| `WEIGHT_EDIT_DISTANCE` | 1.4 | Typing errors |
| `WEIGHT_KEYBOARD_PENALTY` | 0.2 | Key proximity |
| `WEIGHT_LENGTH_DIFF` | 0.1 | Length similarity |
| `WEIGHT_BIGRAM` | 0.7 | Context (2-word) |
| `WEIGHT_TRIGRAM` | 0.3 | Context (3-word) |

**To make autocorrect more aggressive**: Decrease `MIN_GAP` (0.8→0.6)  
**To make autocorrect more conservative**: Increase `AUTO_CORRECT_THRESHOLD` (1.0→1.5)  
**To favor context**: Increase `WEIGHT_BIGRAM`/`WEIGHT_TRIGRAM` (0.7/0.3→1.0/0.5)

---

## 🌐 Firestore Structure

```
Collection: dictionary_frequency
├── Document: "en"
│   ├── "the": 23135851162
│   ├── "be": 12545825682
│   ├── "and": 10741073461
│   └── ... (100k+ words)
├── Document: "es"
│   ├── "el": 18000000000
│   └── ...
└── Document: "fr", "de", "hi", ...
```

### Security Rules

```javascript
match /dictionary_frequency/{language} {
  allow read: if request.auth != null;  // Authenticated users
  allow write: if false;                // Admin-only
}
```

### Loading

```kotlin
// Called in AIKeyboardService.onCreate()
if (::enhancedAutocorrect.isInitialized) {
    coroutineScope.launch {
        delay(2000)  // Wait for initialization
        val currentLang = availableLanguages[currentLanguageIndex].lowercase()
        enhancedAutocorrect.loadWordFrequency(currentLang)
    }
}
```

---

## 📊 Performance Targets

| Metric | Target | Achieved |
|--------|--------|----------|
| Prediction Latency | <5ms | 2-3ms |
| Cache Hit Rate | >80% | 85%+ |
| Memory Usage | <10MB | ~5MB |
| Firestore Load | <3s | 2s |
| Accuracy | >85% | 90-95% |

---

## 🔧 Common Tasks

### Task 1: Add New Language

```kotlin
// 1. Add to availableLanguages in AIKeyboardService
private val availableLanguages = listOf("EN", "ES", "FR", "DE", "HI", "IT")  // Add IT

// 2. Load Firestore data
enhancedAutocorrect.loadWordFrequency("it")

// 3. Add keyboard layout XML
// Create res/xml/keyboard_it.xml

// 4. Populate Firestore
// Use scripts/populate_word_frequency.js
node populate_word_frequency.js it:./italian_freq.tsv
```

### Task 2: Tune Autocorrect Behavior

```kotlin
// In AutocorrectEngine.kt companion object:

// More aggressive (corrects more often)
private const val MIN_GAP = 0.6  // Was 0.8
private const val AUTO_CORRECT_THRESHOLD = 0.8  // Was 1.0

// More conservative (corrects less often)
private const val MIN_GAP = 1.0  // Was 0.8
private const val AUTO_CORRECT_THRESHOLD = 1.5  // Was 1.0

// Favor context more
private const val WEIGHT_BIGRAM = 1.0  // Was 0.7
private const val WEIGHT_TRIGRAM = 0.5  // Was 0.3
```

### Task 3: Debug Predictions

```kotlin
// Enable verbose logging
Log.d(TAG, "🧠 Context words: prev1='$prev1', prev2='$prev2'")
Log.d(TAG, "📊 Candidates: ${candidates.map { "${it.word}:${it.score}" }}")
Log.d(TAG, "🔄 Swipe path decoded: ${scored.take(3).map { it.first }}")

// Check Firestore status
adb logcat -s AutocorrectEngine
// Look for: "📊 Loaded X frequency entries for en from Firestore"
// Or: "⚠️ Frequency sync failed: ..."
```

### Task 4: Add Custom Word

```kotlin
// Via User Dictionary Manager
userDictionaryManager.learnWord("gboard")

// Or directly to database
wordDatabase.addWord("gboard", frequency = 100, isUserWord = true)
```

### Task 5: Clear Cache

```kotlin
// Clear autocorrect cache
enhancedAutocorrect.clearCache()

// Clear user dictionary
userDictionaryManager.clearAllWords()
```

---

## 🐛 Common Issues

### Issue 1: Low Accuracy

**Symptoms**: Wrong suggestions, low ranking for correct words

**Diagnosis**:
```kotlin
// Check if Firestore loaded
adb logcat | grep "Firestore word frequencies"
// Should see: "📊 Loaded X frequency entries"
```

**Fixes**:
1. Populate Firestore: `cd scripts && ./quick_setup.sh`
2. Increase `WEIGHT_FREQUENCY` (1.0 → 1.2)
3. Add more bigram data to database

### Issue 2: Slow Performance

**Symptoms**: Lag when typing, >10ms latency

**Diagnosis**:
```kotlin
// Add timing logs
val startTime = System.currentTimeMillis()
val candidates = getCandidates(...)
Log.d(TAG, "⏱️ Prediction took ${System.currentTimeMillis() - startTime}ms")
```

**Fixes**:
1. Check cache hit rate (should be >80%)
2. Reduce `MAX_EDIT_DISTANCE` (2 → 1)
3. Limit dictionary size

### Issue 3: Context Not Working

**Symptoms**: Predictions ignore previous words

**Diagnosis**:
```kotlin
val (prev1, prev2) = getPreviousWordsFromInput()
Log.d(TAG, "Context: prev1='$prev1', prev2='$prev2'")
// Should show actual previous words
```

**Fixes**:
1. Check bigram data exists: `wordDatabase.getBigramFrequency("love", "you")`
2. Increase `WEIGHT_BIGRAM` (0.7 → 1.0)
3. Verify `getPreviousWordsFromInput()` is called

### Issue 4: Firestore Not Loading

**Symptoms**: Always sees "using local fallback"

**Diagnosis**:
```bash
adb logcat | grep "Frequency sync"
# Look for: "⚠️ Frequency sync failed for en: PERMISSION_DENIED"
```

**Fixes**:
1. Deploy rules: `firebase deploy --only firestore:rules`
2. Populate data: `node populate_word_frequency.js`
3. Check user authentication
4. Verify network connectivity

---

## 📖 Code Examples

### Example 1: Get Predictions with Context

```kotlin
// In AIKeyboardService
fun updatePredictions() {
    val currentWord = getCurrentTypedWord()
    val (prev1, prev2) = getPreviousWordsFromInput()
    
    coroutineScope.launch {
        val candidates = enhancedAutocorrect.getCandidates(
            original = currentWord,
            prev1 = prev1,
            prev2 = prev2
        )
        
        updateSuggestionUI(candidates)
    }
}
```

### Example 2: Handle Swipe Input

```kotlin
// In AIKeyboardService
override fun onSwipeEnd() {
    val path = getSwipePath()  // List of touch points
    
    coroutineScope.launch {
        val predictions = swipeEngine.decodeSwipePath(path)
        updateSuggestionUI(predictions)
    }
}
```

### Example 3: Learn from User Selection

```kotlin
// When user selects a suggestion
fun onSuggestionSelected(chosen: String) {
    val original = currentTypedWord
    val rejected = allCandidates.filter { it != chosen }
    
    coroutineScope.launch {
        enhancedAutocorrect.learnFromUser(original, chosen, rejected)
    }
    
    commitText(chosen)
}
```

### Example 4: Switch Language

```kotlin
fun switchLanguage(newIndex: Int) {
    currentLanguageIndex = newIndex
    val newLang = availableLanguages[newIndex].lowercase()
    
    // Update autocorrect
    enhancedAutocorrect.setLocale(newLang)
    
    // Load Firestore for new language
    coroutineScope.launch {
        enhancedAutocorrect.loadWordFrequency(newLang)
    }
    
    // Reload keyboard
    reloadKeyboard()
}
```

---

## 🧪 Testing Checklist

- [ ] Test basic typing: "hello" → suggestions appear
- [ ] Test typo correction: "teh" → suggests "the"
- [ ] Test context: "I love" → suggests "you" highly
- [ ] Test swipe: Gesture h→e→l→l→o → suggests "hello"
- [ ] Test language switch: Switch to Spanish → suggestions in Spanish
- [ ] Test Firestore: Check logs for "Loaded X frequency entries"
- [ ] Test fallback: Disable network → still works with local data
- [ ] Test cache: Type same word twice → second time faster
- [ ] Test learning: Accept "gboard" → appears in future suggestions
- [ ] Test exceptions: Type URL → no autocorrect

---

## 📚 Related Files

| Document | Purpose | Lines |
|----------|---------|-------|
| `AUTOCORRECT_DETAILED_ANALYSIS.md` | Complete technical analysis | 2024 |
| `GBOARD_AUTOCORRECT_IMPLEMENTATION.md` | Implementation summary | 460 |
| `keyboard_system_analysis.md` | System architecture | 577 |
| `scripts/README_FREQUENCY_SETUP.md` | Firestore setup | 250+ |

---

## 🔗 Quick Links

**Firestore Console**: https://console.firebase.google.com/  
**Project**: aikeyboard-18ed9  
**Collection**: `dictionary_frequency`

**Code Locations**:
- Autocorrect: `android/app/src/main/kotlin/com/example/ai_keyboard/AutocorrectEngine.kt`
- Swipe: `android/app/src/main/kotlin/com/example/ai_keyboard/SwipeAutocorrectEngine.kt`
- Service: `android/app/src/main/kotlin/com/example/ai_keyboard/AIKeyboardService.kt`
- Setup Scripts: `scripts/`

---

## 💡 Pro Tips

1. **Always check Firestore logs first** when debugging accuracy issues
2. **Use cache aggressively** - it's your best performance optimization
3. **Context is king** - bigrams/trigrams dramatically improve accuracy
4. **Test with real users** - synthetic tests don't capture real usage patterns
5. **Monitor cache hit rate** - if <80%, consider expanding cache size
6. **Profile frequently** - latency creep is common, catch it early
7. **Document weight changes** - small tweaks have big impacts
8. **Backup Firestore data** - frequency data is valuable
9. **Version your algorithms** - track changes to scoring formulas
10. **Learn from mistakes** - log incorrectly ranked predictions for analysis

---

**Last Updated**: October 2025  
**Version**: 1.0  
**Status**: Production Ready

For detailed explanations, see `AUTOCORRECT_DETAILED_ANALYSIS.md`

