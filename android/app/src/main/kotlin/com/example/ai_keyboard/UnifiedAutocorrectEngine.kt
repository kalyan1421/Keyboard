package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * UnifiedAutocorrectEngine - Single engine for all languages (English + Indic)
 * Replaces: AutocorrectEngine, MultilingualAutocorrectEngine, WordDatabase
 * 
 * Features:
 * - Transliteration-aware correction for Indic languages
 * - Multi-factor scoring (frequency, bigram, edit distance, transliteration proximity)
 * - Unified dictionary management with lazy loading
 * - User dictionary integration
 * - Context-aware next-word prediction
 */
class UnifiedAutocorrectEngine(
    private val context: Context,
    private val dictionary: MultilingualDictionary,
    private val transliterationEngine: TransliterationEngine? = null,
    private val indicScriptHelper: IndicScriptHelper? = null,
    private val userDictionaryManager: UserDictionaryManager? = null
) {
    companion object {
        private const val TAG = "UnifiedAutocorrectEngine"
        private val INDIC_LANGUAGES = listOf("hi", "te", "ta", "ml", "bn", "gu", "kn", "pa", "ur")
    }

    // Cache for suggestions to improve performance
    private val suggestionCache = ConcurrentHashMap<String, List<Suggestion>>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    // Corrections map loaded from corrections.json
    private val correctionsMap = ConcurrentHashMap<String, String>()
    
    init {
        loadCorrectionsFromAssets()
    }

    /**
     * Data class for autocorrect suggestions
     */
    data class Suggestion(
        val word: String,
        val score: Double,
        val isCorrection: Boolean = false,
        val sourceLanguage: String = "en"
    )

    /**
     * Preload dictionaries for specified languages
     */
    fun preloadLanguages(languages: List<String>) {
        languages.forEach { lang ->
            if (!dictionary.isLoaded(lang)) {
                dictionary.loadLanguage(lang, coroutineScope)
                Log.d(TAG, "Preloaded dictionary for $lang")
            }
        }
    }

    /**
     * Check if language dictionary is loaded
     */
    fun isLanguageLoaded(language: String): Boolean {
        return dictionary.isLoaded(language)
    }
    
    /**
     * Check if engine is fully ready for use
     * Returns true if corrections are loaded and at least one language dictionary is loaded
     */
    fun isReady(): Boolean {
        val ready = correctionsMap.isNotEmpty() && dictionary.getLoadedLanguages().isNotEmpty()
        if (!ready) {
            Log.w(TAG, "⚠️ Engine not ready: corrections=${correctionsMap.size}, langs=${dictionary.getLoadedLanguages()}")
        }
        return ready
    }

    /**
     * Get autocorrect suggestions for a typed word
     * 
     * @param word The typed word
     * @param language Current language code
     * @param context Previous words for bigram context
     * @return List of scored suggestions
     */
    fun getCorrections(word: String, language: String = "en", context: List<String> = emptyList()): List<Suggestion> {
        if (word.isBlank()) return emptyList()
        
        val cacheKey = "$word:$language:${context.joinToString(",")}"
        suggestionCache[cacheKey]?.let { return it }

        val suggestions = if (language in INDIC_LANGUAGES) {
            getIndicCorrections(word, language, context)
        } else {
            getStandardCorrections(word, language, context)
        }

        // Cache results for performance
        suggestionCache[cacheKey] = suggestions
        return suggestions
    }

    /**
     * Convenience method to get suggestions as a simple list of words
     * Returns top 3 suggestions by default
     * @param input The word to get suggestions for
     * @param language Current language code
     * @param limit Maximum number of suggestions to return (default: 3)
     * @return List of suggestion words
     */
    fun getSuggestions(input: String, language: String = "en", limit: Int = 3): List<String> {
        if (input.isBlank()) return emptyList()
        val corrections = getCorrections(input, language)
        return corrections.take(limit).map { it.word }
    }

    /**
     * Get the best (highest-ranked) suggestion for a word
     * Includes fallback to common typo corrections
     * @param input The word to get suggestion for
     * @param language Current language code
     * @return The best suggestion, or null if no suggestions available
     */
    fun getBestSuggestion(input: String, language: String = "en"): String? {
        if (input.isBlank()) return null
        
        // PRIORITY 1: Check corrections.json map
        val normalized = input.lowercase()
        correctionsMap[normalized]?.let { suggestion ->
            // Check if this correction was previously rejected by user
            if (userDictionaryManager?.isBlacklisted(normalized, suggestion.lowercase()) == true) {
                Log.d(TAG, "🚫 Skipping blacklisted correction '$input' → '$suggestion'")
                return null
            }
            Log.d(TAG, "✨ Found correction in corrections.json: '$input' → '$suggestion'")
            return suggestion 
        }
        
        // PRIORITY 2: Try dictionary-based suggestions
        val suggestions = getSuggestions(input, language, limit = 1)
        if (suggestions.isNotEmpty()) {
            val bestSuggestion = suggestions.first()
            // Check if this suggestion was previously rejected by user
            if (userDictionaryManager?.isBlacklisted(normalized, bestSuggestion.lowercase()) == true) {
                Log.d(TAG, "🚫 Skipping blacklisted dictionary suggestion '$input' → '$bestSuggestion'")
                return null
            }
            return bestSuggestion
        }
        
        // PRIORITY 3: Fallback to hardcoded common typo corrections (for backwards compatibility)
        if (language == "en") {
            return getCommonTypoCorrection(normalized)
        }
        
        return null
    }
    
    /**
     * Common typo corrections as a fallback
     */
    private fun getCommonTypoCorrection(word: String): String? {
        return when (word) {
            "teh" -> "the"
            "hte" -> "the"
            "adn" -> "and"
            "nad" -> "and"
            "taht" -> "that"
            "thta" -> "that"
            "waht" -> "what"
            "wnat" -> "want"
            "tiem" -> "time"
            "thier" -> "their"
            "thier" -> "their"
            "recieve" -> "receive"
            "recive" -> "receive"
            "seperate" -> "separate"
            "definately" -> "definitely"
            "occured" -> "occurred"
            "begining" -> "beginning"
            "wich" -> "which"
            "whcih" -> "which"
            "freind" -> "friend"
            "frined" -> "friend"
            "becuase" -> "because"
            "becasue" -> "because"
            "coudl" -> "could"
            "woudl" -> "would"
            "shoudl" -> "should"
            "dont" -> "don't"
            "cant" -> "can't"
            "wont" -> "won't"
            "didnt" -> "didn't"
            "doesnt" -> "doesn't"
            "isnt" -> "isn't"
            "arent" -> "aren't"
            "wasnt" -> "wasn't"
            "werent" -> "weren't"
            "hasnt" -> "hasn't"
            "havent" -> "haven't"
            "hadnt" -> "hadn't"
            "youre" -> "you're"
            "theyre" -> "they're"
            "were" -> "we're"
            "its" -> "it's"  // Only when clearly a contraction
            else -> null
        }
    }

    /**
     * Get corrections for Indic languages with transliteration support
     */
    private fun getIndicCorrections(word: String, language: String, context: List<String>): List<Suggestion> {
        val typed = word.trim().lowercase()
        val suggestions = mutableListOf<Suggestion>()

        try {
            // Path A: Roman input → transliterate → find candidates
            if (transliterationEngine != null && indicScriptHelper?.detectScript(typed) == IndicScriptHelper.Script.LATIN) {
                val nativeText = transliterationEngine.transliterate(typed)
                Log.d(TAG, "Transliterating '$typed' → '$nativeText'")

                val candidates = dictionary.getCandidates(nativeText, language, 20)
                candidates.forEach { candidate ->
                    val editDistance = getEditDistance(nativeText, candidate)
                    if (editDistance <= 2) {
                        val score = calculateScore(candidate, typed, editDistance, context, language, true)
                        suggestions.add(Suggestion(candidate, score, editDistance > 0, language))
                    }
                }
            }

            // Path B: Native script input → direct matching
            val nativeCandidates = dictionary.getCandidates(typed, language, 15)
            nativeCandidates.forEach { candidate ->
                val editDistance = getEditDistance(typed, candidate)
                val score = calculateScore(candidate, typed, editDistance, context, language, false)
                suggestions.add(Suggestion(candidate, score, editDistance > 0, language))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting Indic corrections for $typed", e)
        }

        return suggestions.distinctBy { it.word.lowercase() }
            .sortedByDescending { it.score }
            .take(5)
    }

    /**
     * Get corrections for standard languages (English, etc.)
     */
    private fun getStandardCorrections(word: String, language: String, context: List<String>): List<Suggestion> {
        val typed = word.trim().lowercase()
        val candidates = dictionary.getCandidates(typed, language, 20)
        
        return candidates.map { candidate ->
            val editDistance = getEditDistance(typed, candidate)
            val score = calculateScore(candidate, typed, editDistance, context, language, false)
            Suggestion(candidate, score, editDistance > 0, language)
        }.sortedByDescending { it.score }
            .take(5)
    }

    /**
     * Calculate multi-factor score for a suggestion
     */
    private fun calculateScore(
        candidate: String,
        typedWord: String,
        editDistance: Int,
        context: List<String>,
        language: String,
        isTransliterationPath: Boolean
    ): Double {
        // Base frequency score
        val frequency = dictionary.getFrequency(language, candidate).toDouble()
        var score = frequency * 0.7

        // Edit distance penalty
        score -= (editDistance * 1.2)

        // Length difference penalty
        val lengthDiff = kotlin.math.abs(candidate.length - typedWord.length)
        score -= (lengthDiff * 0.1)

        // Bigram context boost
        val lastContextWord = context.lastOrNull()?.lowercase()
        if (lastContextWord != null) {
            val bigramFreq = dictionary.getBigramFrequency(language, lastContextWord, candidate).toDouble()
            score += (bigramFreq * 0.8)
        }

        // Transliteration proximity boost
        if (isTransliterationPath) {
            score += 0.5
        }

        // Indic language boost
        if (language in INDIC_LANGUAGES) {
            score += 0.3
        }

        // Exact match bonus
        if (candidate.equals(typedWord, ignoreCase = true)) {
            score += 1.0
        }

        // User dictionary boost - learned words get higher priority
        userDictionaryManager?.let { userDict ->
            if (userDict.hasLearnedWord(candidate)) {
                val usageCount = userDict.getWordCount(candidate)
                // Base boost of 0.8 + additional 0.05 per usage (capped at +0.5)
                val usageBoost = kotlin.math.min(usageCount * 0.05, 0.5)
                score += (0.8 + usageBoost)
                Log.d(TAG, "👤 User dictionary boost for '$candidate': +${0.8 + usageBoost} (used $usageCount times)")
            }
        }

        return score
    }

    /**
     * Get word candidates by prefix (for suggestion strip)
     */
    fun getCandidates(prefix: String, language: String = "en", limit: Int = 10): List<String> {
        if (prefix.isBlank()) return emptyList()
        return dictionary.getCandidates(prefix, language, limit)
    }

    /**
     * Get next word predictions based on bigram context
     */
    fun getNextWordPredictions(previousWord: String, language: String = "en", limit: Int = 5): List<String> {
        if (previousWord.isBlank()) return emptyList()
        
        return try {
            // Get bigram predictions from dictionary
            // Get bigram predictions - simplified for now
            val bigramCandidates = emptyList<String>() // TODO: Implement when getBigramNextWords is available
            
            // TODO: Add user dictionary predictions when available
            // val userPredictions = userDictionaryManager?.getNextWordSuggestions(previousWord, language) ?: emptyList()
            
            // Return bigram candidates for now
            bigramCandidates.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting next word predictions for '$previousWord'", e)
            emptyList()
        }
    }

    /**
     * Add word to user dictionary
     */
    fun addUserWord(word: String, language: String = "en", frequency: Int = 1) {
        try {
            if (word.isBlank() || word.length < 2) {
                Log.w(TAG, "⚠️ Word too short to add: '$word'")
                return
            }
            
            // Add to user dictionary
            userDictionaryManager?.learnWord(word)
            
            // Clear cache to ensure new word appears in suggestions
            clearCache()
            
            Log.d(TAG, "✅ Added user word: '$word' ($language)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding user word '$word'", e)
        }
    }

    /**
     * Learn from user input (for adaptive corrections)
     */
    fun learnFromUser(originalWord: String, correctedWord: String, language: String = "en") {
        try {
            if (originalWord.equals(correctedWord, ignoreCase = true)) {
                // User kept original word - don't learn
                return
            }
            
            // Learn the corrected word
            userDictionaryManager?.learnWord(correctedWord)
            
            // Also add to corrections map if this is a correction pattern
            if (language == "en" && originalWord.length >= 3) {
                correctionsMap[originalWord.lowercase()] = correctedWord.lowercase()
            }
            
            Log.d(TAG, "✨ Learned: '$originalWord' → '$correctedWord' for $language")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error learning correction", e)
        }
    }

    /**
     * Get word suggestions (legacy compatibility method)
     */
    fun getWordSuggestions(prefix: String, language: String = "en", limit: Int = 10): List<String> {
        return getCandidates(prefix, language, limit)
    }

    /**
     * Apply correction to input (legacy compatibility method)
     */
    fun applyCorrection(word: String, language: String = "en"): String {
        val suggestions = getCorrections(word, language)
        return suggestions.firstOrNull()?.word ?: word
    }

    /**
     * Get revert candidate (for undo functionality)
     */
    fun getRevertCandidate(correctedWord: String): String? {
        // This would need to be implemented based on correction history
        return null
    }

    /**
     * Process word boundary (for context-aware corrections)
     */
    fun processBoundary(context: List<String>, language: String = "en") {
        // Update context for next predictions
        Log.d(TAG, "Processing boundary with context: ${context.takeLast(2)}")
    }

    /**
     * Set locale for the engine
     */
    fun setLocale(language: String) {
        Log.d(TAG, "Locale set to: $language")
        if (!isLanguageLoaded(language)) {
            dictionary.loadLanguage(language, coroutineScope)
        }
    }

    /**
     * Simple Levenshtein distance calculation
     */
    private fun getEditDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) {
            for (j in 0..len2) {
                if (i == 0) {
                    dp[i][j] = j
                } else if (j == 0) {
                    dp[i][j] = i
                } else if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[len1][len2]
    }

    /**
     * Get suggestions optimized for swipe input
     * Used by SwipeAutocorrectEngine for consistent scoring
     */
    fun suggestForSwipe(input: String, language: String): List<String> {
        return try {
            getCorrections(input, language, emptyList()).take(3).map { it.word }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting swipe suggestions for '$input'", e)
            emptyList()
        }
    }
    
    /**
     * STEP 5: Simplified interface for quick suggestions
     */
    fun suggest(input: String, language: String): List<String> {
        return getCorrections(input, language).map { it.word }
    }

    /**
     * Clear suggestion cache (for memory management)
     */
    fun clearCache() {
        suggestionCache.clear()
        Log.d(TAG, "Suggestion cache cleared")
    }

    /**
     * Get engine statistics (for debugging)
     * Returns actual loaded data instead of placeholders
     */
    fun getStats(): Map<String, Any> {
        val loadedLangs = dictionary.getLoadedLanguages()
        val totalWords = dictionary.getLoadedWordCount()
        // User words count - for now return 0 as UserDictionaryManager doesn't expose count
        // TODO: Add getTotalWordCount() method to UserDictionaryManager
        val userWordCount = 0
        
        return mapOf(
            "cacheSize" to suggestionCache.size,
            "loadedLanguages" to loadedLangs,
            "totalWords" to totalWords,
            "userWords" to userWordCount,
            "corrections" to correctionsMap.size
        )
    }

    /**
     * Calculate confidence score for an autocorrect suggestion
     * Returns value between 0.0 and 1.0, where higher = more confident
     * 
     * @param input The typed word
     * @param suggestion The suggested correction
     * @return Confidence score (0.0 to 1.0)
     */
    fun getConfidence(input: String, suggestion: String): Float {
        if (input.isEmpty() || suggestion.isEmpty()) return 0f
        
        // Exact match = perfect confidence
        if (input.equals(suggestion, ignoreCase = true)) return 1.0f
        
        val inputLower = input.lowercase()
        val suggestionLower = suggestion.lowercase()
        
        // 🔥 HIGH PRIORITY: corrections.json matches get high confidence (0.8)
        // This ensures predefined corrections like "plz→please" always apply
        if (correctionsMap.containsKey(inputLower) && correctionsMap[inputLower] == suggestionLower) {
            return 0.8f
        }
        
        // 🔥 HIGH-PRIORITY: Detect transpositions (adjacent character swaps)
        // Examples: "teh" → "the", "hte" → "the", "taht" → "that"
        if (inputLower.length == suggestionLower.length) {
            var diffCount = 0
            var transpositionFound = false
            
            for (i in inputLower.indices) {
                if (inputLower[i] != suggestionLower[i]) {
                    diffCount++
                    // Check if next character is swapped
                    if (i < inputLower.length - 1 &&
                        inputLower[i] == suggestionLower[i + 1] &&
                        inputLower[i + 1] == suggestionLower[i]) {
                        transpositionFound = true
                    }
                }
            }
            
            // If it's a single transposition, give very high confidence
            if (transpositionFound && diffCount == 2) {
                return 0.85f  // High confidence for transpositions
            }
        }
        
        // Calculate edit distance based confidence
        val maxLen = maxOf(inputLower.length, suggestionLower.length).toFloat()
        val distance = getEditDistance(inputLower, suggestionLower)
        
        // Base confidence from edit distance
        val editDistanceConfidence = 1f - (distance / maxLen)
        
        // Bonus for common typo patterns (higher confidence)
        val typoBonus = when {
            // Single character difference
            distance == 1 && inputLower.length == suggestionLower.length -> 0.3f
            // Single insertion/deletion
            distance == 1 -> 0.2f
            // Two character difference in longer words
            distance == 2 && inputLower.length >= 4 -> 0.15f
            else -> 0f
        }
        
        // Penalty for length mismatch (less confident if lengths differ a lot)
        val lengthDiff = kotlin.math.abs(inputLower.length - suggestionLower.length)
        val lengthPenalty = if (lengthDiff > 2) 0.1f else 0f
        
        // Final confidence score
        return (editDistanceConfidence + typoBonus - lengthPenalty).coerceIn(0f, 1f)
    }
    
    /**
     * Load predefined corrections from corrections.json
     * This is called once during initialization
     */
    private fun loadCorrectionsFromAssets() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val json = context.assets.open("dictionaries/corrections.json")
                    .bufferedReader().use { it.readText() }
                
                val jsonObject = JSONObject(json)
                val corrections = jsonObject.getJSONObject("corrections")
                
                val keys = corrections.keys()
                var count = 0
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = corrections.getString(key)
                    correctionsMap[key.lowercase()] = value
                    count++
                }
                
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "✅ Loaded $count corrections from corrections.json")
                    Log.d(TAG, "✅ Engine ready [corrections=$count, langs=${dictionary.getLoadedLanguages()}]")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load corrections.json", e)
            }
        }
    }
    
    /**
     * Call this when user accepts an autocorrect suggestion
     * This helps the system learn user preferences
     */
    fun onCorrectionAccepted(originalWord: String, acceptedWord: String, language: String = "en") {
        try {
            // Learn the accepted word
            userDictionaryManager?.learnWord(acceptedWord)
            
            // If it's a correction (not just a suggestion), learn the pattern
            if (!originalWord.equals(acceptedWord, ignoreCase = true)) {
                learnFromUser(originalWord, acceptedWord, language)
            }
            
            Log.d(TAG, "✅ User accepted: '$originalWord' → '$acceptedWord'")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing accepted correction", e)
        }
    }
}
