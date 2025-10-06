package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * EnhancedAutocorrectEngine - Phase 2
 * 
 * Indic-aware autocorrect with:
 * - Transliteration-aware correction
 * - Grapheme-cluster edit distance
 * - Bigram context boosting
 * - Multi-script support
 */
class EnhancedAutocorrectEngine(
    private val context: Context,
    private val dictionary: MultilingualDictionary,
    private val transliterationEngine: TransliterationEngine? = null,
    private val indicHelper: IndicScriptHelper? = null
) {
    companion object {
        private const val TAG = "AutoIndic"
        
        // Scoring weights
        private const val WEIGHT_FREQUENCY = 1.0
        private const val WEIGHT_EDIT_DISTANCE = -1.4
        private const val WEIGHT_KEYBOARD_PENALTY = -0.2
        private const val WEIGHT_LENGTH_DIFF = -0.1
        private const val WEIGHT_BIGRAM = 0.9
        private const val WEIGHT_TRIGRAM = 0.4
        private const val WEIGHT_TRANSLIT_PROX = 0.5
        private const val WEIGHT_INDIC_BOOST = 0.3
        
        private const val MAX_EDIT_DISTANCE = 2
        private const val MAX_SUGGESTIONS = 5
    }
    
    data class Suggestion(
        val word: String,
        val score: Double,
        val source: String, // "dict", "translit", "bigram"
        val isCorrection: Boolean = false
    )
    
    /**
     * Main suggestion API
     * @param typed The word being typed
     * @param context Previous words for bigram context
     * @param language Target language code
     * @param limit Maximum suggestions to return
     */
    fun suggest(
        typed: String,
        context: List<String> = emptyList(),
        language: String,
        limit: Int = MAX_SUGGESTIONS
    ): List<Suggestion> {
        if (typed.isEmpty()) return emptyList()
        
        val startTime = System.currentTimeMillis()
        
        val suggestions = mutableListOf<Suggestion>()
        
        // Determine if this is Indic language + Roman input (transliteration path)
        val isIndicLanguage = language in listOf("hi", "te", "ta")
        val isRomanInput = typed.all { it.code in 32..126 }
        
        if (isIndicLanguage && isRomanInput && transliterationEngine != null) {
            // PATH A: Transliteration-aware correction
            suggestions.addAll(getTransliterationSuggestions(typed, context, language))
        } else {
            // PATH B: Standard dictionary correction
            suggestions.addAll(getDictionarySuggestions(typed, context, language))
        }
        
        // Sort by score and limit
        val finalSuggestions = suggestions
            .sortedByDescending { it.score }
            .take(limit)
        
        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "suggest lang=$language typed='$typed' → ${finalSuggestions.size} candidates (ms=$duration)")
        
        return finalSuggestions
    }
    
    /**
     * PATH A: Transliteration-aware suggestions (Roman → Native script)
     */
    private fun getTransliterationSuggestions(
        typed: String,
        context: List<String>,
        language: String
    ): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        try {
            // 1. Transliterate the Roman input
            val primaryNative = transliterationEngine?.transliterate(typed) ?: ""
            
            if (primaryNative.isNotEmpty()) {
                // Add primary transliteration with high score
                suggestions.add(
                    Suggestion(
                        word = primaryNative,
                        score = 100.0,
                        source = "translit",
                        isCorrection = false
                    )
                )
            }
            
            // 2. Find dictionary neighbors (edit distance <= 2)
            if (dictionary.isLoaded(language)) {
                val allWords = dictionary.getAllWords(language)
                
                for (candidate in allWords) {
                    val editDist = getGraphemeAwareEditDistance(primaryNative, candidate, language)
                    
                    if (editDist <= MAX_EDIT_DISTANCE) {
                        val score = scoreSuggestion(
                            candidate = candidate,
                            typed = primaryNative,
                            context = context,
                            language = language,
                            editDistance = editDist,
                            isTranslit = true
                        )
                        
                        suggestions.add(
                            Suggestion(
                                word = candidate,
                                score = score,
                                source = "dict",
                                isCorrection = editDist > 0
                            )
                        )
                    }
                    
                    // Limit candidates for performance
                    if (suggestions.size >= 50) break
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in transliteration suggestions", e)
        }
        
        return suggestions
    }
    
    /**
     * PATH B: Standard dictionary suggestions (native script or Latin)
     */
    private fun getDictionarySuggestions(
        typed: String,
        context: List<String>,
        language: String
    ): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        if (!dictionary.isLoaded(language)) {
            Log.w(TAG, "Dictionary not loaded for $language")
            return suggestions
        }
        
        try {
            // 1. Exact match
            if (dictionary.contains(language, typed)) {
                suggestions.add(
                    Suggestion(
                        word = typed,
                        score = 100.0,
                        source = "dict",
                        isCorrection = false
                    )
                )
            }
            
            // 2. Prefix matches
            val prefixMatches = dictionary.getCandidates(typed, language, 20)
            for (candidate in prefixMatches) {
                if (candidate != typed) {
                    val score = scoreSuggestion(
                        candidate = candidate,
                        typed = typed,
                        context = context,
                        language = language,
                        editDistance = 0,
                        isTranslit = false
                    )
                    
                    suggestions.add(
                        Suggestion(
                            word = candidate,
                            score = score,
                            source = "dict",
                            isCorrection = false
                        )
                    )
                }
            }
            
            // 3. Edit distance neighbors
            val allWords = dictionary.getAllWords(language).take(1000) // Limit for performance
            
            for (candidate in allWords) {
                if (suggestions.any { it.word == candidate }) continue
                
                val editDist = getGraphemeAwareEditDistance(typed, candidate, language)
                
                if (editDist in 1..MAX_EDIT_DISTANCE) {
                    val score = scoreSuggestion(
                        candidate = candidate,
                        typed = typed,
                        context = context,
                        language = language,
                        editDistance = editDist,
                        isTranslit = false
                    )
                    
                    suggestions.add(
                        Suggestion(
                            word = candidate,
                            score = score,
                            source = "dict",
                            isCorrection = true
                        )
                    )
                }
                
                if (suggestions.size >= 50) break
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in dictionary suggestions", e)
        }
        
        return suggestions
    }
    
    /**
     * Score a suggestion based on multiple factors
     */
    private fun scoreSuggestion(
        candidate: String,
        typed: String,
        context: List<String>,
        language: String,
        editDistance: Int,
        isTranslit: Boolean
    ): Double {
        var score = 0.0
        
        // 1. Frequency (lower rank = higher score)
        val freq = dictionary.getFrequency(language, candidate)
        val freqScore = if (freq < Int.MAX_VALUE) {
            // Normalize: rank 1 = 100, rank 1000+ = 0
            max(0.0, 100.0 - (freq / 10.0))
        } else {
            0.0
        }
        score += WEIGHT_FREQUENCY * freqScore
        
        // 2. Edit distance penalty
        score += WEIGHT_EDIT_DISTANCE * editDistance
        
        // 3. Length difference penalty
        val lenDiff = abs(candidate.length - typed.length)
        score += WEIGHT_LENGTH_DIFF * lenDiff
        
        // 4. Bigram boost
        if (context.isNotEmpty()) {
            val prevWord = context.last()
            val bigramFreq = dictionary.getBigramFrequency(language, prevWord, candidate)
            if (bigramFreq > 0) {
                score += WEIGHT_BIGRAM * min(bigramFreq.toDouble(), 50.0)
            }
        }
        
        // 5. Transliteration proximity boost
        if (isTranslit) {
            score += WEIGHT_TRANSLIT_PROX * 10.0
        }
        
        // 6. Indic script boost (prefer native over mixed)
        if (language in listOf("hi", "te", "ta")) {
            val isFullyIndic = candidate.all { it.code > 127 }
            if (isFullyIndic) {
                score += WEIGHT_INDIC_BOOST * 10.0
            }
        }
        
        return score
    }
    
    /**
     * Grapheme-aware edit distance for Indic scripts
     */
    private fun getGraphemeAwareEditDistance(s1: String, s2: String, language: String): Int {
        // Use grapheme clustering for Indic scripts
        // For now, use simple edit distance as IndicScriptHelper doesn't expose this method
        // TODO: Add graphemeAwareEditDistance to IndicScriptHelper if needed
        return simpleEditDistance(s1, s2)
    }
    
    /**
     * Simple Levenshtein distance
     */
    private fun simpleEditDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * Get keyboard proximity penalty (for future enhancement)
     */
    private fun getKeyboardProximity(c1: Char, c2: Char): Double {
        // TODO: Implement QWERTY keyboard distance matrix
        return if (c1 == c2) 0.0 else 1.0
    }
}

