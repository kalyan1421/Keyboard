package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ln

class PredictiveTextEngine private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "PredictiveTextEngine"
        private const val MAX_PREDICTIONS = 8
        private const val MIN_PREDICTION_SCORE = 0.1
        
        @Volatile
        private var INSTANCE: PredictiveTextEngine? = null
        
        fun getInstance(context: Context): PredictiveTextEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PredictiveTextEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val wordDatabase = WordDatabase.getInstance(context)
    
    /**
     * Get predictive text suggestions
     */
    suspend fun getPredictions(
        currentWord: String,
        previousWords: List<String>,
        includeCompletions: Boolean = true,
        includeNextWords: Boolean = true
    ): List<PredictiveSuggestion> = withContext(Dispatchers.Default) {
        
        val suggestions = mutableListOf<PredictiveSuggestion>()
        
        // Get different types of predictions
        if (includeCompletions && currentWord.isNotEmpty()) {
            suggestions.addAll(getWordCompletions(currentWord))
        }
        
        if (includeNextWords) {
            suggestions.addAll(getBigramPredictions(previousWords, currentWord))
            suggestions.addAll(getTrigramPredictions(previousWords, currentWord))
            suggestions.addAll(getUserBasedPredictions(currentWord, previousWords))
        }
        
        // Combine and rank suggestions
        val combinedSuggestions = combineAndRankSuggestions(suggestions)
        
        combinedSuggestions.take(MAX_PREDICTIONS)
    }
    
    /**
     * Get word completions based on current partial word
     */
    private suspend fun getWordCompletions(prefix: String): List<PredictiveSuggestion> = withContext(Dispatchers.Default) {
        if (prefix.isEmpty()) return@withContext emptyList()
        
        val completions = mutableListOf<PredictiveSuggestion>()
        
        // Get completions from main words database
        val wordSuggestions = wordDatabase.getWordsStartingWith(prefix, 15)
        
        for (suggestion in wordSuggestions) {
            val score = calculateCompletionScore(prefix, suggestion.word, suggestion.frequency, suggestion.isCommon)
            
            completions.add(PredictiveSuggestion(
                word = suggestion.word,
                score = score,
                type = PredictionType.COMPLETION,
                source = "dictionary"
            ))
        }
        
        // Get completions from user words
        val userSuggestions = wordDatabase.getUserWordSuggestions(prefix, 5)
        
        for (suggestion in userSuggestions) {
            val score = calculateUserCompletionScore(prefix, suggestion.word, suggestion.frequency)
            
            completions.add(PredictiveSuggestion(
                word = suggestion.word,
                score = score,
                type = PredictionType.USER_COMPLETION,
                source = "user"
            ))
        }
        
        completions
    }
    
    /**
     * Get predictions based on bigrams (previous word + current)
     */
    private suspend fun getBigramPredictions(
        previousWords: List<String>,
        currentWord: String
    ): List<PredictiveSuggestion> = withContext(Dispatchers.Default) {
        
        if (previousWords.isEmpty()) return@withContext emptyList()
        
        val predictions = mutableListOf<PredictiveSuggestion>()
        val lastWord = previousWords.last()
        
        val bigramSuggestions = wordDatabase.getBigramPredictions(lastWord, currentWord, 10)
        
        for (suggestion in bigramSuggestions) {
            val score = calculateBigramScore(suggestion.frequency, currentWord.length, suggestion.word.length)
            
            predictions.add(PredictiveSuggestion(
                word = suggestion.word,
                score = score,
                type = PredictionType.BIGRAM,
                source = "bigram:$lastWord"
            ))
        }
        
        predictions
    }
    
    /**
     * Get predictions based on trigrams (last two words + current)
     */
    private suspend fun getTrigramPredictions(
        previousWords: List<String>,
        currentWord: String
    ): List<PredictiveSuggestion> = withContext(Dispatchers.Default) {
        
        if (previousWords.size < 2) return@withContext emptyList()
        
        val predictions = mutableListOf<PredictiveSuggestion>()
        val word1 = previousWords[previousWords.size - 2]
        val word2 = previousWords.last()
        
        val trigramSuggestions = wordDatabase.getTrigramPredictions(word1, word2, currentWord, 8)
        
        for (suggestion in trigramSuggestions) {
            val score = calculateTrigramScore(suggestion.frequency, currentWord.length, suggestion.word.length)
            
            predictions.add(PredictiveSuggestion(
                word = suggestion.word,
                score = score,
                type = PredictionType.TRIGRAM,
                source = "trigram:$word1,$word2"
            ))
        }
        
        predictions
    }
    
    /**
     * Get user-based predictions from learning data
     */
    private suspend fun getUserBasedPredictions(
        currentWord: String,
        previousWords: List<String>
    ): List<PredictiveSuggestion> = withContext(Dispatchers.Default) {
        
        val predictions = mutableListOf<PredictiveSuggestion>()
        
        // Get user word suggestions
        val userSuggestions = wordDatabase.getUserWordSuggestions(currentWord, 8)
        
        for (suggestion in userSuggestions) {
            val score = calculateUserScore(suggestion.frequency, suggestion.isCommon, currentWord.length, suggestion.word.length)
            
            predictions.add(PredictiveSuggestion(
                word = suggestion.word,
                score = score,
                type = PredictionType.USER,
                source = "user_learned"
            ))
        }
        
        predictions
    }
    
    /**
     * Calculate completion score
     */
    private fun calculateCompletionScore(prefix: String, word: String, frequency: Int, isCommon: Boolean): Double {
        val lengthRatio = prefix.length.toDouble() / word.length
        val frequencyScore = min(1.0, frequency / 10000.0)
        val commonBonus = if (isCommon) 0.2 else 0.0
        
        return (lengthRatio * 0.3) + (frequencyScore * 0.5) + commonBonus + 0.2
    }
    
    /**
     * Calculate user completion score
     */
    private fun calculateUserCompletionScore(prefix: String, word: String, frequency: Int): Double {
        val lengthRatio = prefix.length.toDouble() / word.length
        val frequencyScore = min(1.0, frequency / 100.0)
        val userBonus = 0.3 // Bonus for user-learned words
        
        return (lengthRatio * 0.3) + (frequencyScore * 0.4) + userBonus + 0.3
    }
    
    /**
     * Calculate bigram score
     */
    private fun calculateBigramScore(frequency: Int, prefixLength: Int, wordLength: Int): Double {
        val frequencyScore = min(1.0, frequency / 1000.0)
        val lengthBonus = if (prefixLength > 0) min(0.3, prefixLength.toDouble() / wordLength) else 0.0
        
        return frequencyScore + lengthBonus + 0.4 // Base bigram bonus
    }
    
    /**
     * Calculate trigram score
     */
    private fun calculateTrigramScore(frequency: Int, prefixLength: Int, wordLength: Int): Double {
        val frequencyScore = min(1.0, frequency / 500.0)
        val lengthBonus = if (prefixLength > 0) min(0.3, prefixLength.toDouble() / wordLength) else 0.0
        
        return frequencyScore + lengthBonus + 0.6 // Higher base score for trigrams
    }
    
    /**
     * Calculate user-based score
     */
    private fun calculateUserScore(frequency: Int, userAdded: Boolean, prefixLength: Int, wordLength: Int): Double {
        val frequencyScore = min(1.0, frequency / 100.0)
        val userBonus = if (userAdded) 0.3 else 0.1
        val lengthBonus = if (prefixLength > 0) min(0.3, prefixLength.toDouble() / wordLength) else 0.0
        
        return frequencyScore + userBonus + lengthBonus + 0.3
    }
    
    /**
     * Combine and rank suggestions from different sources
     */
    private fun combineAndRankSuggestions(suggestions: List<PredictiveSuggestion>): List<PredictiveSuggestion> {
        // Group suggestions by word
        val groupedSuggestions = suggestions.groupBy { it.word }
        
        val combinedSuggestions = mutableListOf<PredictiveSuggestion>()
        
        for ((word, suggestionList) in groupedSuggestions) {
            // Combine scores from different sources
            var totalScore = 0.0
            var bestType = PredictionType.COMPLETION
            val sources = mutableListOf<String>()
            
            for (suggestion in suggestionList) {
                totalScore += suggestion.score
                sources.add(suggestion.source)
                
                // Prioritize certain types
                if (suggestion.type == PredictionType.TRIGRAM || 
                    (suggestion.type == PredictionType.BIGRAM && bestType == PredictionType.COMPLETION) ||
                    (suggestion.type == PredictionType.USER && bestType != PredictionType.TRIGRAM && bestType != PredictionType.BIGRAM)) {
                    bestType = suggestion.type
                }
            }
            
            // Apply diminishing returns to avoid over-boosting
            val finalScore = if (suggestionList.size > 1) {
                totalScore * (1.0 + ln(suggestionList.size.toDouble()) * 0.1)
            } else {
                totalScore
            }
            
            if (finalScore >= MIN_PREDICTION_SCORE) {
                combinedSuggestions.add(PredictiveSuggestion(
                    word = word,
                    score = finalScore,
                    type = bestType,
                    source = sources.joinToString(",")
                ))
            }
        }
        
        // Sort by score (descending)
        return combinedSuggestions.sortedByDescending { it.score }
    }
    
    /**
     * Get next word predictions (when current word is empty or complete)
     */
    suspend fun getNextWordPredictions(previousWords: List<String>, limit: Int = 5): List<PredictiveSuggestion> = withContext(Dispatchers.Default) {
        
        val predictions = mutableListOf<PredictiveSuggestion>()
        
        // Get bigram-based next word predictions
        if (previousWords.isNotEmpty()) {
            val bigramPredictions = getBigramPredictions(previousWords, "")
            predictions.addAll(bigramPredictions.take(limit))
        }
        
        // Get trigram-based next word predictions
        if (previousWords.size >= 2) {
            val trigramPredictions = getTrigramPredictions(previousWords, "")
            predictions.addAll(trigramPredictions.take(limit))
        }
        
        // Add common words if we don't have enough predictions
        if (predictions.size < limit) {
            val commonWords = listOf("the", "and", "to", "a", "of", "in", "is", "it", "you", "that")
            for (word in commonWords) {
                if (predictions.none { it.word == word } && predictions.size < limit) {
                    predictions.add(PredictiveSuggestion(
                        word = word,
                        score = 0.3,
                        type = PredictionType.COMMON,
                        source = "common_words"
                    ))
                }
            }
        }
        
        combineAndRankSuggestions(predictions).take(limit)
    }
    
    /**
     * Learn from user input (update n-grams)
     */
    suspend fun learnFromInput(words: List<String>) = withContext(Dispatchers.Default) {
        if (words.size < 2) return@withContext
        
        try {
            wordDatabase.learnFromSentence(words)
            Log.d(TAG, "Learned from input: ${words.joinToString(" ")}")
        } catch (e: Exception) {
            Log.e(TAG, "Error learning from input", e)
        }
    }
    
    /**
     * Get contextual predictions based on sentence context
     */
    suspend fun getContextualPredictions(
        sentence: String,
        cursorPosition: Int,
        limit: Int = 5
    ): List<PredictiveSuggestion> = withContext(Dispatchers.Default) {
        
        val context = parseTextContext(sentence, cursorPosition)
        
        return@withContext getPredictions(
            currentWord = context.currentWord,
            previousWords = context.previousWords,
            includeCompletions = context.currentWord.isNotEmpty(),
            includeNextWords = true
        ).take(limit)
    }
    
    /**
     * Parse text context around cursor position
     */
    private fun parseTextContext(text: String, cursorPosition: Int): TextContext {
        if (text.isEmpty()) {
            return TextContext(
                currentWord = "",
                previousWords = emptyList(),
                allWords = emptyList()
            )
        }
        
        // Find sentence boundaries
        val sentencePattern = Regex("[.!?]+\\s*")
        val sentenceStarts = mutableListOf(0)
        
        sentencePattern.findAll(text).forEach { match ->
            if (match.range.last < cursorPosition) {
                sentenceStarts.add(match.range.last + 1)
            }
        }
        
        val sentenceStart = sentenceStarts.maxOrNull() ?: 0
        val currentSentence = text.substring(sentenceStart, cursorPosition).trim()
        
        // Split into words
        val words = currentSentence.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map { cleanWord(it) }
            .filter { it.isNotEmpty() }
        
        // Find current word
        val beforeCursor = text.substring(0, cursorPosition)
        val lastSpaceIndex = beforeCursor.lastIndexOf(' ')
        val currentWord = if (lastSpaceIndex >= 0) {
            cleanWord(beforeCursor.substring(lastSpaceIndex + 1))
        } else {
            cleanWord(beforeCursor)
        }
        
        // Get previous words (excluding current incomplete word)
        val previousWords = if (words.isNotEmpty() && words.last() == currentWord) {
            words.dropLast(1)
        } else {
            words
        }
        
        return TextContext(
            currentWord = currentWord,
            previousWords = previousWords,
            allWords = words
        )
    }
    
    /**
     * Clean word of punctuation
     */
    private fun cleanWord(word: String): String {
        return word.replace(Regex("[^\\w]"), "").lowercase()
    }
    
    /**
     * Get smart predictions with context awareness
     */
    suspend fun getSmartPredictions(
        currentWord: String,
        previousWords: List<String>,
        typingSpeed: Long,
        timeOfDay: Int // 0-23 hour
    ): List<PredictiveSuggestion> = withContext(Dispatchers.Default) {
        
        val basePredictions = getPredictions(currentWord, previousWords)
        
        // Adjust scores based on typing patterns and context
        val adjustedPredictions = basePredictions.map { prediction ->
            var adjustedScore = prediction.score
            
            // Adjust based on typing speed
            when {
                typingSpeed < 100 -> adjustedScore *= 1.1 // Slow typing, boost accuracy
                typingSpeed > 300 -> adjustedScore *= 0.9 // Fast typing, slightly reduce to avoid interruption
            }
            
            // Time-based adjustments (could be enhanced with more sophisticated models)
            when (timeOfDay) {
                in 6..11 -> { // Morning
                    if (prediction.word in listOf("good", "morning", "hello", "hi")) {
                        adjustedScore *= 1.2
                    }
                }
                in 12..17 -> { // Afternoon
                    if (prediction.word in listOf("lunch", "meeting", "work")) {
                        adjustedScore *= 1.1
                    }
                }
                in 18..23 -> { // Evening
                    if (prediction.word in listOf("dinner", "evening", "night", "bye")) {
                        adjustedScore *= 1.2
                    }
                }
            }
            
            prediction.copy(score = adjustedScore)
        }
        
        adjustedPredictions.sortedByDescending { it.score }
    }
}

data class PredictiveSuggestion(
    val word: String,
    val score: Double,
    val type: PredictionType,
    val source: String
)

data class TextContext(
    val currentWord: String,
    val previousWords: List<String>,
    val allWords: List<String>
)

enum class PredictionType {
    COMPLETION,
    USER_COMPLETION,
    BIGRAM,
    TRIGRAM,
    USER,
    COMMON,
    CONTEXTUAL
}
