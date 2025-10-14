package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import kotlinx.coroutines.*

/**
 * SuggestionsPipeline: Unified suggestion facade around UnifiedAutocorrectEngine
 * Refactored to be a facade that routes all requests through UnifiedAutocorrectEngine
 * 
 * Features:
 * - Routes empty current token → nextWord(context)
 * - Routes partial token → suggestForTyping(prefix, context)
 * - Routes swipe → Unified via Swipe adapter
 * - Never touches files or other managers directly
 * - Maintains backward compatibility
 */
class SuggestionsPipeline(
    private val context: Context,
    private val clipboardManager: ClipboardHistoryManager,
    private val unifiedAutocorrectEngine: UnifiedAutocorrectEngine? = null
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
    
    // STRIPPED: All direct dictionary dependencies removed
    
    // Optional AI service (kept for additional features if needed)
    private var advancedAIService: AdvancedAIService? = null
    
    init {
        try {
            advancedAIService = AdvancedAIService(context)
        } catch (e: Exception) {
            Log.w(TAG, "AI service not available, using unified engine only")
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
     * Build suggestions for current typing context (REFACTORED)
     * Now routes everything through UnifiedAutocorrectEngine
     * 
     * @param prefix Current word being typed
     * @param previousWords Context words before current word
     * @return List of suggestions, sorted by relevance
     */
    suspend fun buildSuggestions(
        prefix: String,
        previousWords: List<String> = emptyList()
    ): List<Suggestion> = withContext(Dispatchers.Default) {
        val unified = unifiedAutocorrectEngine
        if (unified == null) {
            Log.w(TAG, "UnifiedAutocorrectEngine not available")
            return@withContext getOnlyClipboardSuggestions(prefix)
        }
        
        val results = mutableListOf<Suggestion>()
        
        try {
            // Route requests through UnifiedAutocorrectEngine
            if (prefix.isEmpty()) {
                // Empty current token → nextWord(context)
                if (nextWordPredictionEnabled && previousWords.isNotEmpty()) {
                    val nextWordSuggestions = unified.nextWord(previousWords, MAX_SUGGESTIONS)
                    nextWordSuggestions.forEach { unifiedSuggestion ->
                        results.add(convertFromUnified(unifiedSuggestion, SuggestionType.WORD))
                    }
                }
            } else {
                // Partial token → suggestForTyping(prefix, context)
                val typingSuggestions = unified.suggestForTyping(prefix, previousWords)
                typingSuggestions.forEach { unifiedSuggestion ->
                    results.add(convertFromUnified(unifiedSuggestion, SuggestionType.WORD))
                }
            }
            
            // Add emoji suggestions (if enabled and relevant)
            if (emojiSuggestionsEnabled && prefix.length >= 2) {
                val emojiSuggestions = getEmojiSuggestions(prefix)
                results.addAll(emojiSuggestions)
            }
            
            // Add clipboard suggestions (if enabled and within time window)
            if (clipboardSuggestionsEnabled && prefix.isEmpty()) {
                val clipboardSuggestion = getClipboardSuggestion()
                clipboardSuggestion?.let { results.add(it) }
            }
            
            // De-duplicate and rank
            return@withContext results
                .distinctBy { it.text.lowercase() }
                .sortedByDescending { it.score }
                .take(MAX_SUGGESTIONS)
                
        } catch (e: Exception) {
            Log.e(TAG, "Error building suggestions", e)
            return@withContext getOnlyClipboardSuggestions(prefix)
        }
    }
    
    /**
     * Convert UnifiedAutocorrectEngine.Suggestion to SuggestionsPipeline.Suggestion
     */
    private fun convertFromUnified(
        unifiedSuggestion: com.example.ai_keyboard.Suggestion,
        type: SuggestionType
    ): Suggestion {
        return Suggestion(
            text = unifiedSuggestion.text,
            type = type,
            score = unifiedSuggestion.score.toFloat(),
            metadata = mapOf("source" to unifiedSuggestion.source.name)
        )
    }
    
    /**
     * Fallback when UnifiedAutocorrectEngine is not available
     */
    private suspend fun getOnlyClipboardSuggestions(prefix: String): List<Suggestion> {
        val results = mutableListOf<Suggestion>()
        
        if (clipboardSuggestionsEnabled && prefix.isEmpty()) {
            val clipboardSuggestion = getClipboardSuggestion()
            clipboardSuggestion?.let { results.add(it) }
        }
        
        return results
    }
    
    // === STRIPPED: getDictionarySuggestions removed ===
    // Dictionary operations now routed through UnifiedAutocorrectEngine
    
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
    
    // === STRIPPED: calculateDictionaryScore removed ===
    // Scoring now handled by UnifiedAutocorrectEngine
    
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

