package com.example.ai_keyboard.predict

import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import com.example.ai_keyboard.MultilingualDictionary
import com.example.ai_keyboard.UnifiedAutocorrectEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * NextWordPredictor - Unified prediction system using UnifiedAutocorrectEngine
 * Replaces legacy prediction logic with modern unified approach
 */
class NextWordPredictor(
    private val autocorrectEngine: UnifiedAutocorrectEngine,
    private val dictionary: MultilingualDictionary? = null
) {
    companion object {
        private const val TAG = "NextWordPredictor"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * Get next word predictions based on context
     * 
     * @param context List of previous words for context
     * @param language Current language code
     * @param limit Maximum number of predictions to return
     * @return List of predicted words
     */
    fun getPredictions(context: List<String>, language: String = "en", limit: Int = 5): List<String> {
        if (context.isEmpty()) return emptyList()
        
        return try {
            val lastWord = context.last().lowercase()
            autocorrectEngine.getNextWordPredictions(lastWord, language, limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting predictions for context: $context", e)
            emptyList()
        }
    }

    /**
     * Get predictions for a single previous word (legacy compatibility)
     */
    fun getNextWords(previousWord: String, language: String = "en"): List<Pair<String, Double>> {
        return try {
            val predictions = autocorrectEngine.getNextWordPredictions(previousWord, language, 10)
            predictions.mapIndexed { index, word ->
                val score = 1.0 - (index * 0.1) // Simple scoring based on position
                word to score
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting next words for '$previousWord'", e)
            emptyList()
        }
    }

    /**
     * Get word candidates by prefix (for autocomplete)
     */
    fun getCandidates(prefix: String, language: String = "en", limit: Int = 10): List<String> {
        return try {
            autocorrectEngine.getCandidates(prefix, language, limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting candidates for prefix '$prefix'", e)
            emptyList()
        }
    }

    /**
     * Preload language models for better performance
     */
    fun preloadLanguage(language: String) {
        coroutineScope.launch {
            try {
                autocorrectEngine.preloadLanguages(listOf(language))
                Log.d(TAG, "Preloaded language: $language")
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading language: $language", e)
            }
        }
    }

    /**
     * Update context for better predictions (legacy compatibility)
     */
    fun updateContext(words: List<String>) {
        // Context is now handled directly in prediction calls
        Log.d(TAG, "Context updated: ${words.takeLast(3)}")
    }

    /**
     * Get user-specific top words (legacy compatibility)
     */
    fun getUserTopWords(language: String = "en", limit: Int = 10): List<Pair<String, Double>> {
        return try {
            // Get frequent words from dictionary as approximation
            val candidates = autocorrectEngine.getCandidates("", language, limit)
            candidates.mapIndexed { index, word ->
                val score = 1.0 - (index * 0.05)
                word to score
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user top words", e)
            emptyList()
        }
    }

    /**
     * Learn from user behavior (for adaptive predictions)
     */
    fun learnFromInput(context: List<String>, nextWord: String, language: String = "en") {
        try {
            // Use the autocorrect engine's learning capability
            if (context.isNotEmpty()) {
                autocorrectEngine.addUserWord(nextWord, language)
                Log.d(TAG, "Learned: ${context.last()} → $nextWord")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error learning from input", e)
        }
    }

    /**
     * Get prediction statistics (for debugging)
     */
    fun getStats(): Map<String, Any> {
        return try {
            autocorrectEngine.getStats()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stats", e)
            emptyMap()
        }
    }
}