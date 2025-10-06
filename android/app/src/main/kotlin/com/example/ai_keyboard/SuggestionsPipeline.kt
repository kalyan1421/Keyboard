package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * SuggestionsPipeline: Unified suggestion generation combining:
 * - Dictionary/N-gram (fast, offline)
 * - AI predictions (when available)
 * - Emoji mapper
 * - Clipboard suggestions (time-gated)
 * 
 * Implements Gboard + CleverType suggestion features
 */
class SuggestionsPipeline(
    private val context: Context,
    private val dictionaryManager: DictionaryManager,
    private val clipboardManager: ClipboardHistoryManager
) {
    companion object {
        private const val TAG = "SuggestionsPipeline"
        private const val MAX_SUGGESTIONS = 3
    }
    
    // Settings (updated via updateSettings)
    private var aiSuggestionsEnabled = true
    private var emojiSuggestionsEnabled = true
    private var clipboardSuggestionsEnabled = true
    private var nextWordPredictionEnabled = true
    private var clipboardWindowSec = 60
    
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // AI service (optional, may be null if not available)
    private var advancedAIService: AdvancedAIService? = null
    
    // Predictive text engine for next-word prediction
    private var predictiveEngine: PredictiveTextEngine? = null
    
    init {
        try {
            advancedAIService = AdvancedAIService(context)
        } catch (e: Exception) {
            Log.w(TAG, "AI service not available, falling back to dictionary-only suggestions")
        }
        
        try {
            predictiveEngine = PredictiveTextEngine.getInstance(context)
        } catch (e: Exception) {
            Log.w(TAG, "Predictive engine not available")
        }
    }
    
    /**
     * Suggestion type for UI rendering
     */
    enum class SuggestionType {
        WORD,       // Regular word suggestion
        EMOJI,      // Emoji suggestion
        CLIPBOARD,  // Clipboard quick paste
        ACTION      // Special action (e.g., AI rewrite)
    }
    
    /**
     * Unified suggestion data class
     */
    data class Suggestion(
        val text: String,
        val type: SuggestionType,
        val score: Float = 0f,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    /**
     * Build suggestions for current typing context
     * 
     * @param prefix Current word being typed
     * @param previousWords Context words before current word
     * @return List of suggestions, sorted by relevance
     */
    suspend fun buildSuggestions(
        prefix: String,
        previousWords: List<String> = emptyList()
    ): List<Suggestion> = withContext(Dispatchers.Default) {
        val results = mutableListOf<Suggestion>()
        
        try {
            // 1. Dictionary / n-gram suggestions (always fast, offline)
            val dictSuggestions = getDictionarySuggestions(prefix, previousWords)
            results.addAll(dictSuggestions)
            
            // 2. AI predictions (if available and enabled)
            if (aiSuggestionsEnabled && aiAvailable()) {
                val aiSuggestions = getAISuggestions(prefix, previousWords)
                results.addAll(aiSuggestions)
            }
            
            // 3. Emoji suggestions (if enabled and relevant)
            if (emojiSuggestionsEnabled && prefix.length >= 2) {
                val emojiSuggestions = getEmojiSuggestions(prefix)
                results.addAll(emojiSuggestions)
            }
            
            // 4. Clipboard suggestions (if enabled and within time window)
            if (clipboardSuggestionsEnabled && prefix.isEmpty()) {
                val clipboardSuggestion = getClipboardSuggestion()
                clipboardSuggestion?.let { results.add(it) }
            }
            
            // De-duplicate and rank
            results
                .distinctBy { it.text.lowercase() }
                .sortedByDescending { it.score }
                .take(MAX_SUGGESTIONS)
                
        } catch (e: Exception) {
            Log.e(TAG, "Error building suggestions", e)
            emptyList()
        }
    }
    
    /**
     * Get dictionary-based suggestions
     */
    private suspend fun getDictionarySuggestions(
        prefix: String,
        previousWords: List<String>
    ): List<Suggestion> {
        if (prefix.isEmpty() && !nextWordPredictionEnabled) {
            return emptyList()
        }
        
        val suggestions = mutableListOf<Suggestion>()
        
        try {
            // Prefix matching from dictionary shortcuts
            if (prefix.isNotEmpty()) {
                val matches = dictionaryManager.getMatchingShortcuts(prefix, 5)
                matches.forEach { entry ->
                    suggestions.add(
                        Suggestion(
                            text = entry.expansion,
                            type = SuggestionType.WORD,
                            score = calculateDictionaryScore(entry.expansion, prefix)
                        )
                    )
                }
            }
            
            // Next-word prediction using predictive engine (if enabled)
            if (nextWordPredictionEnabled && previousWords.isNotEmpty() && predictiveEngine != null) {
                try {
                    val predictions = predictiveEngine!!.getNextWordPredictions(previousWords, 3)
                    predictions.forEach { pred ->
                        // Only add if not already present
                        if (suggestions.none { it.text.equals(pred.word, ignoreCase = true) }) {
                            suggestions.add(
                                Suggestion(
                                    text = pred.word,
                                    type = SuggestionType.WORD,
                                    score = pred.score.toFloat()
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting next-word predictions: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dictionary suggestions", e)
        }
        
        return suggestions
    }
    
    /**
     * Get AI-powered suggestions
     */
    private suspend fun getAISuggestions(
        prefix: String,
        previousWords: List<String>
    ): List<Suggestion> {
        val service = advancedAIService ?: return emptyList()
        
        // For now, return empty - full AI integration can be added later
        // This is a placeholder for future AI completions
        return emptyList()
    }
    
    /**
     * Get emoji suggestions based on word/prefix
     */
    private fun getEmojiSuggestions(prefix: String): List<Suggestion> {
        val emojiMap = mapOf(
            "love" to "❤️",
            "heart" to "❤️",
            "like" to "👍",
            "ok" to "👌",
            "thanks" to "🙏",
            "thank" to "🙏",
            "smile" to "😊",
            "happy" to "😊",
            "sad" to "😢",
            "laugh" to "😂",
            "lol" to "😂",
            "fire" to "🔥",
            "cool" to "😎",
            "think" to "🤔",
            "party" to "🎉",
            "celebrate" to "🎉",
            "food" to "🍔",
            "eat" to "🍕",
            "drink" to "☕",
            "coffee" to "☕",
            "beer" to "🍺",
            "sun" to "☀️",
            "moon" to "🌙",
            "star" to "⭐",
            "check" to "✅",
            "yes" to "✅",
            "no" to "❌",
            "stop" to "🛑",
            "wait" to "⏳"
        )
        
        val results = mutableListOf<Suggestion>()
        val lowerPrefix = prefix.lowercase()
        
        emojiMap.forEach { (word, emoji) ->
            if (word.startsWith(lowerPrefix) || lowerPrefix.startsWith(word)) {
                results.add(
                    Suggestion(
                        text = emoji,
                        type = SuggestionType.EMOJI,
                        score = 0.6f,
                        metadata = mapOf("word" to word)
                    )
                )
            }
        }
        
        return results.take(1) // Only show 1 emoji suggestion
    }
    
    /**
     * Get clipboard suggestion (if recent copy within time window)
     */
    private fun getClipboardSuggestion(): Suggestion? {
        try {
            val recentItems = clipboardManager.getHistoryItems()
            if (recentItems.isEmpty()) return null
            
            val item = recentItems.firstOrNull() ?: return null
            val ageSeconds = (System.currentTimeMillis() - item.timestamp) / 1000
            
            // Only suggest if within time window
            if (ageSeconds > clipboardWindowSec) {
                return null
            }
            
            // Truncate long text for suggestion display
            val displayText = if (item.text.length > 30) {
                item.text.take(27) + "..."
            } else {
                item.text
            }
            
            return Suggestion(
                text = displayText,
                type = SuggestionType.CLIPBOARD,
                score = 0.75f,
                metadata = mapOf(
                    "fullText" to item.text,
                    "ageSeconds" to ageSeconds
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting clipboard suggestion", e)
            return null
        }
    }
    
    /**
     * Calculate dictionary score based on prefix match quality
     */
    private fun calculateDictionaryScore(word: String, prefix: String): Float {
        if (prefix.isEmpty()) return 0.5f
        
        val lowerWord = word.lowercase()
        val lowerPrefix = prefix.lowercase()
        
        return when {
            lowerWord == lowerPrefix -> 1.0f  // Exact match
            lowerWord.startsWith(lowerPrefix) -> 0.9f - (word.length - prefix.length) * 0.05f
            else -> 0.5f
        }.coerceIn(0.5f, 1.0f)
    }
    
    /**
     * Check if AI is available
     */
    private fun aiAvailable(): Boolean {
        // Check network connectivity and AI service status
        return advancedAIService != null
    }
    
    /**
     * Update settings from config change
     */
    fun updateSettings(
        aiSuggestions: Boolean = true,
        emojiSuggestions: Boolean = true,
        clipboardSuggestions: Boolean = true,
        nextWordPrediction: Boolean = true,
        clipboardWindowSeconds: Int = 60
    ) {
        this.aiSuggestionsEnabled = aiSuggestions
        this.emojiSuggestionsEnabled = emojiSuggestions
        this.clipboardSuggestionsEnabled = clipboardSuggestions
        this.nextWordPredictionEnabled = nextWordPrediction
        this.clipboardWindowSec = clipboardWindowSeconds
        
        Log.d(TAG, "Settings updated: AI=$aiSuggestions, Emoji=$emojiSuggestions, Clipboard=$clipboardSuggestions, NextWord=$nextWordPrediction")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        coroutineScope.cancel()
        advancedAIService?.cleanup()
    }
}

