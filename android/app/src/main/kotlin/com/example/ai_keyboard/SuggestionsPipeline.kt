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
    private val unifiedAutocorrectEngine: UnifiedAutocorrectEngine? = null,
    private var multilingualDictionary: MultilingualDictionary? = null,
    private var languageManager: LanguageManager? = null
) {
    
    // Queue for pending requests when dictionary is loading
    private val pendingRequests = mutableMapOf<String, MutableList<PendingSuggestionRequest>>()
    
    // Timeout for dictionary loading (5 seconds)
    private val dictionaryTimeoutMs = 5000L
    
    data class PendingSuggestionRequest(
        val prefix: String,
        val previousWords: List<String>,
        val callback: (List<Suggestion>) -> Unit,
        val timestamp: Long = System.currentTimeMillis()
    )
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
        
        // Set up language ready listener for MultilingualDictionary
        setupLanguageReadyListener()
    }
    
    /**
     * Set up listener for when languages become ready
     */
    private fun setupLanguageReadyListener() {
        // This will be called when MultilingualDictionary finishes loading a language
        // We can use this to process any pending requests
    }
    
    /**
     * Set MultilingualDictionary instance for language ready callbacks
     */
    fun setMultilingualDictionary(dictionary: MultilingualDictionary) {
        this.multilingualDictionary = dictionary
        if (dictionary is MultilingualDictionaryImpl) {
            dictionary.onLanguageReady = { lang ->
                Log.d(TAG, "✅ SuggestionsPipeline: Dictionary ready for $lang")
                reloadSuggestionsForLanguage(lang)
            }
        }
    }
    
    /**
     * Set LanguageManager instance for current language detection
     */
    fun setLanguageManager(manager: LanguageManager) {
        this.languageManager = manager
    }
    
    /**
     * Process pending suggestions when a language becomes ready
     */
    private fun reloadSuggestionsForLanguage(language: String) {
        val pending = pendingRequests.remove(language) ?: return
        
        Log.d(TAG, "Processing ${pending.size} pending suggestion requests for $language")
        
        pending.forEach { request ->
            // Check if request hasn't timed out
            if (System.currentTimeMillis() - request.timestamp < dictionaryTimeoutMs) {
                coroutineScope.launch {
                    try {
                        val suggestions = buildSuggestions(request.prefix, request.previousWords)
                        request.callback(suggestions)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing pending suggestion request", e)
                        request.callback(emptyList())
                    }
                }
            } else {
                Log.w(TAG, "Pending suggestion request timed out for $language")
                request.callback(emptyList())
            }
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
     * Now routes everything through UnifiedAutocorrectEngine with async dictionary support
     * 
     * @param prefix Current word being typed
     * @param previousWords Context words before current word
     * @return List of suggestions, sorted by relevance
     */
    suspend fun buildSuggestions(
        prefix: String,
        previousWords: List<String> = emptyList()
    ): List<Suggestion> = withContext(Dispatchers.Default) {
        return@withContext buildSuggestionsWithCallback(prefix, previousWords) { suggestions ->
            // Synchronous version - we already have the result
        }
    }
    
    /**
     * Build suggestions with callback support for async dictionary loading
     * 
     * @param prefix Current word being typed
     * @param previousWords Context words before current word
     * @param callback Callback for when suggestions are ready (may be immediate or async)
     * @return List of suggestions if immediately available, empty list if queued for async
     */
    suspend fun buildSuggestionsWithCallback(
        prefix: String,
        previousWords: List<String> = emptyList(),
        callback: (List<Suggestion>) -> Unit
    ): List<Suggestion> = withContext(Dispatchers.Default) {
        val unified = unifiedAutocorrectEngine
        if (unified == null) {
            Log.w(TAG, "UnifiedAutocorrectEngine not available")
            val fallbackSuggestions = getOnlyClipboardSuggestions(prefix)
            callback(fallbackSuggestions)
            return@withContext fallbackSuggestions
        }
        
        // Check if dictionary is still loading
        val currentLanguage = getCurrentLanguage()
        if (currentLanguage != null && !isDictionaryReady(currentLanguage)) {
            Log.d(TAG, "📋 Dictionary for $currentLanguage still loading, queuing request")
            
            // Queue the request for when dictionary is ready
            val request = PendingSuggestionRequest(prefix, previousWords, callback)
            pendingRequests.getOrPut(currentLanguage) { mutableListOf() }.add(request)
            
            // Return clipboard suggestions immediately if available
            val immediateResults = getOnlyClipboardSuggestions(prefix)
            callback(immediateResults)
            return@withContext immediateResults
        }
        
        val results = mutableListOf<Suggestion>()
        
        try {
            // 4️⃣ ENHANCED SOURCE MERGING - Priority order: AI > Autocorrect > NextWord > Dictionary
            Log.d(TAG, "🔄 Building suggestions - prefix='$prefix', context=$previousWords")
            
            if (prefix.isEmpty()) {
                // Empty current token → nextWord predictions
                if (nextWordPredictionEnabled && previousWords.isNotEmpty()) {
                    Log.d(TAG, "🔮 Getting next-word suggestions from unified engine")
                    val nextWordSuggestions = unified.nextWord(previousWords, MAX_SUGGESTIONS)
                    nextWordSuggestions.forEach { unifiedSuggestion ->
                        results.add(convertFromUnified(unifiedSuggestion, SuggestionType.WORD))
                    }
                    
                    // No dictionary fallback needed - unified engine handles all data sources
                }
            } else {
                // Partial token → suggestForTyping(prefix, context)
                Log.d(TAG, "✍️ Getting typing suggestions for prefix '$prefix'")
                val typingSuggestions = unified.suggestForTyping(prefix, previousWords)
                typingSuggestions.forEach { unifiedSuggestion ->
                    results.add(convertFromUnified(unifiedSuggestion, SuggestionType.WORD))
                }
                Log.d(TAG, "✍️ Unified engine provided ${typingSuggestions.size} typing suggestions")
            }
            
            // Add emoji suggestions (if enabled and relevant)
            if (emojiSuggestionsEnabled && prefix.length >= 2) {
                val emojiSuggestions = getEmojiSuggestions(prefix)
                results.addAll(emojiSuggestions)
                Log.d(TAG, "😊 Added ${emojiSuggestions.size} emoji suggestions")
            }
            
            // Add clipboard suggestions (if enabled and within time window)
            if (clipboardSuggestionsEnabled && prefix.isEmpty()) {
                val clipboardSuggestion = getClipboardSuggestion()
                clipboardSuggestion?.let { 
                    results.add(it)
                    Log.d(TAG, "📋 Added clipboard suggestion: ${it.text}")
                }
            }
            
            // De-duplicate and rank by priority
            val finalResults = results
                .distinctBy { it.text.lowercase() }
                .sortedByDescending { it.score }
                .take(MAX_SUGGESTIONS)
                
            Log.d(TAG, "✅ SuggestionsPipeline: Using unified Firebase data for ${getCurrentLanguage() ?: "unknown"}: AI=${finalResults.count { it.type == SuggestionType.ACTION }}, Word=${finalResults.count { it.type == SuggestionType.WORD }}, Emoji=${finalResults.count { it.type == SuggestionType.EMOJI }}, Clipboard=${finalResults.count { it.type == SuggestionType.CLIPBOARD }}")
            
            callback(finalResults)
            return@withContext finalResults
                
        } catch (e: Exception) {
            Log.e(TAG, "Error building suggestions", e)
            val fallbackResults = getOnlyClipboardSuggestions(prefix)
            callback(fallbackResults)
            return@withContext fallbackResults
        }
    }
    
    /**
     * Get current language from LanguageManager
     */
    private fun getCurrentLanguage(): String? {
        return languageManager?.getCurrentLanguage()
    }
    
    /**
     * Check if unified engine is ready for a language (Firebase data loaded)
     */
    private fun isDictionaryReady(language: String): Boolean {
        val dictReady = multilingualDictionary?.isLoaded(language) ?: false
        val engineReady = unifiedAutocorrectEngine?.isLanguageLoaded(language) ?: false
        Log.d(TAG, "📋 Firebase data status for $language: dict=$dictReady, engine=$engineReady")
        return dictReady && engineReady
    }
    
    /**
     * Clean up pending requests that have timed out
     */
    private fun cleanupTimedOutRequests() {
        val currentTime = System.currentTimeMillis()
        
        pendingRequests.entries.removeAll { (language, requests) ->
            val validRequests = requests.filter { request ->
                currentTime - request.timestamp < dictionaryTimeoutMs
            }
            
            if (validRequests.size != requests.size) {
                Log.w(TAG, "Cleaned up ${requests.size - validRequests.size} timed out requests for $language")
            }
            
            requests.clear()
            requests.addAll(validRequests)
            
            requests.isEmpty() // Remove empty entries
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
        // Clear pending requests
        pendingRequests.clear()
        
        // Clean up periodic timeout cleanup
        cleanupTimedOutRequests()
        
        coroutineScope.cancel()
        advancedAIService?.cleanup()
    }
}

