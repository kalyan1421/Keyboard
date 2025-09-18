package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.log
import kotlinx.coroutines.*

class AutocorrectEngine private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "AutocorrectEngine"
        private const val MAX_EDIT_DISTANCE = 2
        private const val MIN_CONFIDENCE = 0.3
        private const val MAX_SUGGESTIONS = 5
        
        @Volatile
        private var INSTANCE: AutocorrectEngine? = null
        
        fun getInstance(context: Context): AutocorrectEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutocorrectEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val wordDatabase = WordDatabase.getInstance(context)
    private val keyboardLayout = createKeyboardLayout()
    
    // QWERTY keyboard layout for proximity calculation
    private fun createKeyboardLayout(): Map<Char, List<Char>> {
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
    
    /**
     * Get autocorrect suggestions for a word
     */
    suspend fun getAutocorrectSuggestions(word: String): List<AutocorrectSuggestion> = withContext(Dispatchers.Default) {
        if (word.isEmpty() || word.length < 2) {
            return@withContext emptyList()
        }
        
        val lowerWord = word.lowercase()
        
        // Check if word exists in database
        if (wordDatabase.wordExists(lowerWord)) {
            return@withContext emptyList() // No correction needed
        }
        
        val suggestions = mutableListOf<AutocorrectSuggestion>()
        
        // Get candidate words with similar length (±2 characters)
        val minLength = max(1, word.length - 2)
        val maxLength = word.length + 2
        
        val candidates = mutableListOf<String>()
        for (length in minLength..maxLength) {
            candidates.addAll(wordDatabase.getWordsByLength(length, 200))
        }
        
        // Calculate edit distance and confidence for each candidate
        for (candidate in candidates) {
            val editDistance = damerauLevenshteinDistance(lowerWord, candidate)
            
            if (editDistance <= MAX_EDIT_DISTANCE) {
                val confidence = calculateConfidence(lowerWord, candidate, editDistance)
                
                if (confidence >= MIN_CONFIDENCE) {
                    suggestions.add(AutocorrectSuggestion(
                        word = candidate,
                        confidence = confidence,
                        editDistance = editDistance,
                        type = CorrectionType.EDIT_DISTANCE
                    ))
                }
            }
        }
        
        // Add phonetic suggestions
        suggestions.addAll(getPhoneticSuggestions(lowerWord))
        
        // Sort by confidence and return top suggestions
        suggestions.sortByDescending { it.confidence }
        suggestions.take(MAX_SUGGESTIONS)
    }
    
    /**
     * Calculate Damerau-Levenshtein distance (includes transposition)
     */
    private fun damerauLevenshteinDistance(word1: String, word2: String): Int {
        val len1 = word1.length
        val len2 = word2.length
        
        if (len1 == 0) return len2
        if (len2 == 0) return len1
        
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }
        
        // Initialize first row and column
        for (i in 0..len1) matrix[i][0] = i
        for (j in 0..len2) matrix[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (word1[i - 1] == word2[j - 1]) 0 else 1
                
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
                
                // Transposition
                if (i > 1 && j > 1 && 
                    word1[i - 1] == word2[j - 2] && 
                    word1[i - 2] == word2[j - 1]) {
                    matrix[i][j] = min(matrix[i][j], matrix[i - 2][j - 2] + cost)
                }
            }
        }
        
        return matrix[len1][len2]
    }
    
    /**
     * Calculate confidence score for a correction
     */
    private fun calculateConfidence(original: String, candidate: String, editDistance: Int): Double {
        // Base confidence inversely related to edit distance
        val maxLength = max(original.length, candidate.length)
        val distanceScore = 1.0 - (editDistance.toDouble() / maxLength)
        
        // Keyboard proximity score
        val proximityScore = calculateKeyboardProximity(original, candidate)
        
        // Length similarity score
        val lengthScore = 1.0 - (kotlin.math.abs(original.length - candidate.length).toDouble() / maxLength)
        
        // Common word bonus (would need frequency data)
        val frequencyScore = 0.5 // Placeholder - could be enhanced with actual frequency data
        
        // Weighted combination
        return (distanceScore * 0.4) + (proximityScore * 0.2) + (lengthScore * 0.2) + (frequencyScore * 0.2)
    }
    
    /**
     * Calculate keyboard proximity score
     */
    private fun calculateKeyboardProximity(word1: String, word2: String): Double {
        if (word1.length != word2.length) return 0.0
        
        var proximityScore = 0.0
        for (i in word1.indices) {
            val char1 = word1[i]
            val char2 = word2[i]
            
            when {
                char1 == char2 -> proximityScore += 2.0 // Exact match
                keyboardLayout[char1]?.contains(char2) == true -> proximityScore += 1.0 // Adjacent keys
            }
        }
        
        return proximityScore / (word1.length * 2.0)
    }
    
    /**
     * Get phonetic suggestions using Soundex algorithm
     */
    private fun getPhoneticSuggestions(word: String): List<AutocorrectSuggestion> {
        val soundexCode = soundex(word)
        val suggestions = mutableListOf<AutocorrectSuggestion>()
        
        // This would require a soundex index in the database
        // For now, return empty list as it's computationally expensive
        // to calculate soundex for all words in real-time
        
        return suggestions
    }
    
    /**
     * Soundex algorithm implementation
     */
    private fun soundex(word: String): String {
        if (word.isEmpty()) return ""
        
        val cleanWord = word.uppercase().replace(Regex("[^A-Z]"), "")
        if (cleanWord.isEmpty()) return ""
        
        var soundex = cleanWord[0].toString()
        
        val soundexMap = mapOf(
            'B' to '1', 'F' to '1', 'P' to '1', 'V' to '1',
            'C' to '2', 'G' to '2', 'J' to '2', 'K' to '2', 'Q' to '2', 'S' to '2', 'X' to '2', 'Z' to '2',
            'D' to '3', 'T' to '3',
            'L' to '4',
            'M' to '5', 'N' to '5',
            'R' to '6'
        )
        
        var prevCode = soundexMap[cleanWord[0]] ?: '0'
        
        for (i in 1 until cleanWord.length) {
            if (soundex.length >= 4) break
            
            val code = soundexMap[cleanWord[i]] ?: '0'
            if (code != '0' && code != prevCode) {
                soundex += code
            }
            prevCode = code
        }
        
        // Pad with zeros or truncate to 4 characters
        return soundex.padEnd(4, '0').take(4)
    }
    
    /**
     * Check if a word should be autocorrected
     */
    suspend fun shouldAutocorrect(word: String): Boolean = withContext(Dispatchers.Default) {
        if (word.length < 2) return@withContext false
        return@withContext !wordDatabase.wordExists(word.lowercase())
    }
    
    /**
     * Get the best autocorrect suggestion
     */
    suspend fun getBestAutocorrect(word: String, context: List<String> = emptyList()): String? = withContext(Dispatchers.Default) {
        val suggestions = getAutocorrectSuggestions(word)
        
        if (suggestions.isEmpty()) return@withContext null
        
        // If we have context, try to find contextually appropriate suggestion
        if (context.isNotEmpty()) {
            for (suggestion in suggestions.take(3)) {
                if (isContextuallyAppropriate(suggestion.word, context)) {
                    return@withContext suggestion.word
                }
            }
        }
        
        // Return the highest confidence suggestion
        suggestions.firstOrNull()?.word
    }
    
    /**
     * Check if a word is contextually appropriate
     */
    private suspend fun isContextuallyAppropriate(word: String, context: List<String>): Boolean = withContext(Dispatchers.Default) {
        if (context.isEmpty()) return@withContext true
        
        val lastWord = context.lastOrNull() ?: return@withContext true
        
        // Check bigram frequency
        val bigramSuggestions = wordDatabase.getBigramPredictions(lastWord, word, 1)
        return@withContext bigramSuggestions.isNotEmpty() && bigramSuggestions.first().frequency > 2
    }
    
    /**
     * Learn from user correction choice
     */
    suspend fun learnFromCorrection(original: String, chosen: String, rejected: List<String>) = withContext(Dispatchers.Default) {
        // Increase frequency of chosen word
        wordDatabase.updateWordFrequency(chosen)
        
        // This could be enhanced to store user preferences for specific corrections
        Log.d(TAG, "Learning from correction: $original -> $chosen")
    }
    
    /**
     * Get smart autocorrect considering typing patterns
     */
    suspend fun getSmartAutocorrect(word: String, typingSpeed: Long, context: List<String>): AutocorrectResult = withContext(Dispatchers.Default) {
        val suggestions = getAutocorrectSuggestions(word)
        
        if (suggestions.isEmpty()) {
            return@withContext AutocorrectResult(
                shouldCorrect = false,
                suggestion = null,
                confidence = 0.0,
                reason = "No suggestions found"
            )
        }
        
        val bestSuggestion = suggestions.first()
        
        // Adjust confidence based on typing speed
        val adjustedConfidence = when {
            typingSpeed < 100 -> bestSuggestion.confidence * 1.2 // Slow typing, more likely to be intentional error
            typingSpeed > 300 -> bestSuggestion.confidence * 0.8 // Fast typing, might be intentional abbreviation
            else -> bestSuggestion.confidence
        }
        
        val shouldCorrect = adjustedConfidence > 0.7 && bestSuggestion.editDistance <= 1
        
        AutocorrectResult(
            shouldCorrect = shouldCorrect,
            suggestion = if (shouldCorrect) bestSuggestion.word else null,
            confidence = adjustedConfidence,
            reason = if (shouldCorrect) "High confidence correction" else "Low confidence, showing as suggestion"
        )
    }
}

data class AutocorrectSuggestion(
    val word: String,
    val confidence: Double,
    val editDistance: Int,
    val type: CorrectionType
)

data class AutocorrectResult(
    val shouldCorrect: Boolean,
    val suggestion: String?,
    val confidence: Double,
    val reason: String
)

