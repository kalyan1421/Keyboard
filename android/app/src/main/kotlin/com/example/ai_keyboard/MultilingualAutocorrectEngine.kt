package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Enhanced autocorrect engine with multilingual support
 */
class MultilingualAutocorrectEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "MultilingualAutocorrect"
        private const val MAX_EDIT_DISTANCE = 2
        private const val MIN_CONFIDENCE = 0.3
        private const val MAX_SUGGESTIONS = 5
        private const val CONTEXT_WEIGHT = 0.3
        private const val FREQUENCY_WEIGHT = 0.4
        private const val EDIT_DISTANCE_WEIGHT = 0.3
    }
    
    private val multilingualDictionary = MultilingualDictionary(context)
    private val languageDetector = LanguageDetector()
    private var currentLanguage = "en"
    private var enabledLanguages = setOf("en")
    
    // Language-specific keyboard layouts
    private val keyboardLayouts = mapOf(
        LayoutType.QWERTY to createQwertyLayout(),
        LayoutType.AZERTY to createAzertyLayout(),
        LayoutType.QWERTZ to createQwertzLayout(),
        LayoutType.DEVANAGARI to createDevanagariLayout()
    )
    
    /**
     * Initialize the autocorrect engine
     */
    suspend fun initialize(languages: Set<String>) = withContext(Dispatchers.IO) {
        enabledLanguages = languages
        
        // Preload dictionaries for enabled languages
        multilingualDictionary.preloadLanguages(languages)
        
        Log.d(TAG, "Initialized multilingual autocorrect for languages: $languages")
    }
    
    /**
     * Get autocorrect suggestions for a word
     */
    suspend fun getCorrections(
        word: String, 
        context: String = "", 
        targetLanguage: String = currentLanguage
    ): List<CorrectionSuggestion> = withContext(Dispatchers.IO) {
        
        if (word.length < 2) return@withContext emptyList()
        
        val suggestions = mutableListOf<CorrectionSuggestion>()
        
        // 1. Check direct corrections from dictionary
        val directCorrections = multilingualDictionary.getCorrections(targetLanguage, word)
        directCorrections.forEach { correction ->
            suggestions.add(CorrectionSuggestion(
                original = word,
                suggestion = correction.correctedWord,
                confidence = correction.confidence,
                type = MultilingualCorrectionType.DIRECT,
                language = targetLanguage
            ))
        }
        
        // 2. Generate edit distance based corrections
        val editDistanceCorrections = generateEditDistanceCorrections(word, targetLanguage)
        suggestions.addAll(editDistanceCorrections)
        
        // 3. Check phonetic similarities
        val phoneticCorrections = generatePhoneticCorrections(word, targetLanguage)
        suggestions.addAll(phoneticCorrections)
        
        // 4. If no good suggestions in target language, try language detection
        if (suggestions.isEmpty() || suggestions.maxByOrNull { it.confidence }?.confidence ?: 0.0 < MIN_CONFIDENCE) {
            val detectedLanguage = languageDetector.detectWordLanguage(word)
            if (detectedLanguage != null && detectedLanguage != targetLanguage && enabledLanguages.contains(detectedLanguage)) {
                val crossLanguageCorrections = getCorrections(word, context, detectedLanguage)
                suggestions.addAll(crossLanguageCorrections.map { it.copy(type = MultilingualCorrectionType.CROSS_LANGUAGE) })
            }
        }
        
        // 5. Apply context-based scoring
        if (context.isNotEmpty()) {
            applyContextScoring(suggestions, context, targetLanguage)
        }
        
        // Sort by confidence and return top suggestions
        return@withContext suggestions
            .sortedByDescending { it.confidence }
            .take(MAX_SUGGESTIONS)
    }
    
    /**
     * Generate corrections based on edit distance
     */
    private suspend fun generateEditDistanceCorrections(
        word: String, 
        language: String
    ): List<CorrectionSuggestion> = withContext(Dispatchers.IO) {
        
        val suggestions = mutableListOf<CorrectionSuggestion>()
        val languageConfig = LanguageConfigs.getLanguageConfig(language)
        val keyboardLayout = keyboardLayouts[languageConfig?.layoutType ?: LayoutType.QWERTY]
        
        // Get candidate words from dictionary
        val candidates = multilingualDictionary.getWordSuggestions(language, word.take(2), 100)
        
        candidates.forEach { candidate ->
            if (candidate.length >= word.length - 2 && candidate.length <= word.length + 2) {
                val editDistance = calculateEditDistance(word, candidate)
                if (editDistance <= MAX_EDIT_DISTANCE) {
                    val proximityScore = calculateKeyboardProximityScore(word, candidate, keyboardLayout)
                    val confidence = calculateConfidence(editDistance, proximityScore, candidate.length)
                    
                    if (confidence >= MIN_CONFIDENCE) {
                        suggestions.add(CorrectionSuggestion(
                            original = word,
                            suggestion = candidate,
                            confidence = confidence,
                            type = MultilingualCorrectionType.EDIT_DISTANCE,
                            language = language
                        ))
                    }
                }
            }
        }
        
        return@withContext suggestions
    }
    
    /**
     * Generate phonetic-based corrections
     */
    private suspend fun generatePhoneticCorrections(
        word: String, 
        language: String
    ): List<CorrectionSuggestion> = withContext(Dispatchers.IO) {
        
        val suggestions = mutableListOf<CorrectionSuggestion>()
        
        // Language-specific phonetic rules
        val phoneticVariants = generatePhoneticVariants(word, language)
        
        phoneticVariants.forEach { variant ->
            val candidates = multilingualDictionary.getWordSuggestions(language, variant, 10)
            candidates.forEach { candidate ->
                if (candidate != word) {
                    val confidence = calculatePhoneticConfidence(word, candidate, language)
                    if (confidence >= MIN_CONFIDENCE) {
                        suggestions.add(CorrectionSuggestion(
                            original = word,
                            suggestion = candidate,
                            confidence = confidence,
                            type = MultilingualCorrectionType.PHONETIC,
                            language = language
                        ))
                    }
                }
            }
        }
        
        return@withContext suggestions
    }
    
    /**
     * Generate phonetic variants for a word
     */
    private fun generatePhoneticVariants(word: String, language: String): List<String> {
        val variants = mutableSetOf<String>()
        val lowerWord = word.lowercase()
        
        // Language-specific phonetic rules
        when (language) {
            "en" -> {
                variants.addAll(listOf(
                    lowerWord.replace("ph", "f"),
                    lowerWord.replace("f", "ph"),
                    lowerWord.replace("c", "k"),
                    lowerWord.replace("k", "c"),
                    lowerWord.replace("s", "z"),
                    lowerWord.replace("z", "s")
                ))
            }
            "es" -> {
                variants.addAll(listOf(
                    lowerWord.replace("b", "v"),
                    lowerWord.replace("v", "b"),
                    lowerWord.replace("s", "z"),
                    lowerWord.replace("z", "s")
                ))
            }
            "fr" -> {
                variants.addAll(listOf(
                    lowerWord.replace("é", "e"),
                    lowerWord.replace("è", "e"),
                    lowerWord.replace("ç", "c")
                ))
            }
            "de" -> {
                variants.addAll(listOf(
                    lowerWord.replace("ß", "ss"),
                    lowerWord.replace("ss", "ß"),
                    lowerWord.replace("ä", "ae"),
                    lowerWord.replace("ö", "oe"),
                    lowerWord.replace("ü", "ue")
                ))
            }
        }
        
        return variants.filter { it != lowerWord }.toList()
    }
    
    /**
     * Apply context-based scoring to suggestions
     */
    private suspend fun applyContextScoring(
        suggestions: MutableList<CorrectionSuggestion>,
        context: String,
        language: String
    ) = withContext(Dispatchers.IO) {
        
        val contextWords = context.split("\\s+".toRegex()).takeLast(3).filter { it.isNotEmpty() }
        if (contextWords.isEmpty()) return@withContext
        
        suggestions.forEach { suggestion ->
            val contextScore = calculateContextScore(suggestion.suggestion, contextWords, language)
            suggestion.confidence = (suggestion.confidence * (1 - CONTEXT_WEIGHT)) + (contextScore * CONTEXT_WEIGHT)
        }
    }
    
    /**
     * Calculate context score based on bigrams
     */
    private suspend fun calculateContextScore(
        word: String,
        contextWords: List<String>,
        language: String
    ): Double = withContext(Dispatchers.IO) {
        
        if (contextWords.isEmpty()) return@withContext 0.0
        
        var totalScore = 0.0
        var count = 0
        
        contextWords.forEach { contextWord ->
            val predictions = multilingualDictionary.getNextWordPredictions(language, contextWord, 20)
            val index = predictions.indexOf(word.lowercase())
            if (index != -1) {
                totalScore += (predictions.size - index).toDouble() / predictions.size
                count++
            }
        }
        
        return@withContext if (count > 0) totalScore / count else 0.0
    }
    
    /**
     * Calculate edit distance between two strings
     */
    private fun calculateEditDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = 1 + minOf(
                        dp[i - 1][j],    // deletion
                        dp[i][j - 1],    // insertion
                        dp[i - 1][j - 1] // substitution
                    )
                }
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * Calculate keyboard proximity score
     */
    private fun calculateKeyboardProximityScore(
        word1: String,
        word2: String,
        keyboardLayout: Map<Char, List<Char>>?
    ): Double {
        if (keyboardLayout == null || word1.length != word2.length) return 0.0
        
        var proximityScore = 0.0
        
        for (i in word1.indices) {
            val char1 = word1[i].lowercaseChar()
            val char2 = word2[i].lowercaseChar()
            
            when {
                char1 == char2 -> proximityScore += 1.0
                keyboardLayout[char1]?.contains(char2) == true -> proximityScore += 0.7
                else -> proximityScore += 0.0
            }
        }
        
        return proximityScore / word1.length
    }
    
    /**
     * Calculate confidence score
     */
    private fun calculateConfidence(editDistance: Int, proximityScore: Double, wordLength: Int): Double {
        val editDistanceScore = 1.0 - (editDistance.toDouble() / maxOf(wordLength, 1))
        return (editDistanceScore * EDIT_DISTANCE_WEIGHT) + (proximityScore * (1 - EDIT_DISTANCE_WEIGHT))
    }
    
    /**
     * Calculate phonetic confidence
     */
    private fun calculatePhoneticConfidence(word1: String, word2: String, language: String): Double {
        // Simple phonetic similarity based on common patterns
        val soundex1 = calculateSoundex(word1)
        val soundex2 = calculateSoundex(word2)
        
        return if (soundex1 == soundex2) 0.8 else 0.0
    }
    
    /**
     * Simple Soundex algorithm for phonetic matching
     */
    private fun calculateSoundex(word: String): String {
        if (word.isEmpty()) return ""
        
        val soundex = StringBuilder()
        val upperWord = word.uppercase()
        
        soundex.append(upperWord[0])
        
        val mapping = mapOf(
            'B' to '1', 'F' to '1', 'P' to '1', 'V' to '1',
            'C' to '2', 'G' to '2', 'J' to '2', 'K' to '2', 'Q' to '2', 'S' to '2', 'X' to '2', 'Z' to '2',
            'D' to '3', 'T' to '3',
            'L' to '4',
            'M' to '5', 'N' to '5',
            'R' to '6'
        )
        
        for (i in 1 until upperWord.length) {
            val code = mapping[upperWord[i]]
            if (code != null && (soundex.isEmpty() || soundex.last() != code)) {
                soundex.append(code)
            }
        }
        
        return soundex.toString().padEnd(4, '0').take(4)
    }
    
    /**
     * Update current language
     */
    fun updateCurrentLanguage(language: String) {
        currentLanguage = language
        Log.d(TAG, "Updated current language to: $language")
    }
    
    /**
     * Update enabled languages
     */
    suspend fun updateEnabledLanguages(languages: Set<String>) {
        enabledLanguages = languages
        multilingualDictionary.preloadLanguages(languages)
        Log.d(TAG, "Updated enabled languages: $languages")
    }
    
    /**
     * Learn from user corrections
     */
    suspend fun learnCorrection(original: String, correction: String, language: String) = withContext(Dispatchers.IO) {
        // Add user correction to dictionary
        multilingualDictionary.addUserWord(language, correction)
        Log.d(TAG, "Learned correction: $original -> $correction in $language")
    }
    
    /**
     * Create keyboard layouts for different layout types
     */
    private fun createQwertyLayout(): Map<Char, List<Char>> {
        return mapOf(
            'q' to listOf('w', 'a'),
            'w' to listOf('q', 'e', 's'),
            'e' to listOf('w', 'r', 'd'),
            'r' to listOf('e', 't', 'f'),
            't' to listOf('r', 'y', 'g'),
            'y' to listOf('t', 'u', 'h'),
            'u' to listOf('y', 'i', 'j'),
            'i' to listOf('u', 'o', 'k'),
            'o' to listOf('i', 'p', 'l'),
            'p' to listOf('o', 'l'),
            'a' to listOf('q', 's', 'z'),
            's' to listOf('a', 'w', 'd', 'x'),
            'd' to listOf('s', 'e', 'f', 'c'),
            'f' to listOf('d', 'r', 'g', 'v'),
            'g' to listOf('f', 't', 'h', 'b'),
            'h' to listOf('g', 'y', 'j', 'n'),
            'j' to listOf('h', 'u', 'k', 'm'),
            'k' to listOf('j', 'i', 'l'),
            'l' to listOf('k', 'o', 'p'),
            'z' to listOf('a', 's', 'x'),
            'x' to listOf('z', 's', 'd', 'c'),
            'c' to listOf('x', 'd', 'f', 'v'),
            'v' to listOf('c', 'f', 'g', 'b'),
            'b' to listOf('v', 'g', 'h', 'n'),
            'n' to listOf('b', 'h', 'j', 'm'),
            'm' to listOf('n', 'j', 'k')
        )
    }
    
    private fun createAzertyLayout(): Map<Char, List<Char>> {
        return mapOf(
            'a' to listOf('z', 'q'),
            'z' to listOf('a', 'e', 's'),
            'e' to listOf('z', 'r', 'd'),
            'r' to listOf('e', 't', 'f'),
            't' to listOf('r', 'y', 'g'),
            'y' to listOf('t', 'u', 'h'),
            'u' to listOf('y', 'i', 'j'),
            'i' to listOf('u', 'o', 'k'),
            'o' to listOf('i', 'p', 'l'),
            'p' to listOf('o', 'l'),
            'q' to listOf('a', 's', 'w'),
            's' to listOf('q', 'z', 'd', 'x'),
            'd' to listOf('s', 'e', 'f', 'c'),
            'f' to listOf('d', 'r', 'g', 'v'),
            'g' to listOf('f', 't', 'h', 'b'),
            'h' to listOf('g', 'y', 'j', 'n'),
            'j' to listOf('h', 'u', 'k'),
            'k' to listOf('j', 'i', 'l'),
            'l' to listOf('k', 'o', 'p', 'm'),
            'm' to listOf('l', 'p'),
            'w' to listOf('q', 's', 'x'),
            'x' to listOf('w', 's', 'd', 'c'),
            'c' to listOf('x', 'd', 'f', 'v'),
            'v' to listOf('c', 'f', 'g', 'b'),
            'b' to listOf('v', 'g', 'h', 'n'),
            'n' to listOf('b', 'h', 'j')
        )
    }
    
    private fun createQwertzLayout(): Map<Char, List<Char>> {
        return mapOf(
            'q' to listOf('w', 'a'),
            'w' to listOf('q', 'e', 's'),
            'e' to listOf('w', 'r', 'd'),
            'r' to listOf('e', 't', 'f'),
            't' to listOf('r', 'z', 'g'),
            'z' to listOf('t', 'u', 'h'),
            'u' to listOf('z', 'i', 'j'),
            'i' to listOf('u', 'o', 'k'),
            'o' to listOf('i', 'p', 'l'),
            'p' to listOf('o', 'l'),
            'a' to listOf('q', 's', 'y'),
            's' to listOf('a', 'w', 'd', 'x'),
            'd' to listOf('s', 'e', 'f', 'c'),
            'f' to listOf('d', 'r', 'g', 'v'),
            'g' to listOf('f', 't', 'h', 'b'),
            'h' to listOf('g', 'z', 'j', 'n'),
            'j' to listOf('h', 'u', 'k', 'm'),
            'k' to listOf('j', 'i', 'l'),
            'l' to listOf('k', 'o', 'p'),
            'y' to listOf('a', 's', 'x'),
            'x' to listOf('y', 's', 'd', 'c'),
            'c' to listOf('x', 'd', 'f', 'v'),
            'v' to listOf('c', 'f', 'g', 'b'),
            'b' to listOf('v', 'g', 'h', 'n'),
            'n' to listOf('b', 'h', 'j', 'm'),
            'm' to listOf('n', 'j', 'k')
        )
    }
    
    private fun createDevanagariLayout(): Map<Char, List<Char>> {
        // Simplified Devanagari layout - would need full implementation
        return emptyMap()
    }
}

/**
 * Data class for correction suggestions
 */
data class CorrectionSuggestion(
    val original: String,
    val suggestion: String,
    var confidence: Double,
    val type: MultilingualCorrectionType,
    val language: String
)

/**
 * Types of corrections for multilingual system
 */
enum class MultilingualCorrectionType {
    DIRECT,           // Direct dictionary correction
    EDIT_DISTANCE,    // Based on edit distance
    PHONETIC,         // Based on phonetic similarity
    CONTEXT,          // Based on context
    CROSS_LANGUAGE    // From another language
}
