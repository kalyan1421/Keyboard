package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Enhanced bridge between Android keyboard service and AI engines
 * Provides advanced autocorrect and predictive text functionality
 */
class AIServiceBridge private constructor() {
    companion object {
        private const val TAG = "AIServiceBridge"
        private const val CHANNEL = "ai_keyboard/bridge"
        
        @Volatile
        private var INSTANCE: AIServiceBridge? = null
        private var context: Context? = null
        
        fun getInstance(): AIServiceBridge {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AIServiceBridge().also { INSTANCE = it }
            }
        }
        
        fun initialize(context: Context) {
            this.context = context.applicationContext
            getInstance().initializeEngines(context)
        }
    }
    
    // Enhanced AI engines
    private var wordDatabase: WordDatabase? = null
    private var autocorrectEngine: AutocorrectEngine? = null
    private var predictiveEngine: PredictiveTextEngine? = null
    
    // Callback interface for AI results
    interface AICallback {
        fun onSuggestionsReady(suggestions: List<AISuggestion>)
        fun onCorrectionReady(originalWord: String, correctedWord: String, confidence: Double)
        fun onError(error: String)
    }
    
    // AI Suggestion data class
    @Serializable
    data class AISuggestion(
        val word: String,
        val confidence: Double,
        val source: String,
        val isCorrection: Boolean
    )
    
    private var flutterEngine: FlutterEngine? = null
    private var methodChannel: MethodChannel? = null
    private var context: Context? = null
    private var isInitialized = false
    private var isEnhancedMode = true // Use enhanced engines by default
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initialize the enhanced AI engines
     */
    private fun initializeEngines(context: Context) {
        try {
            wordDatabase = WordDatabase.getInstance(context)
            autocorrectEngine = AutocorrectEngine.getInstance(context)
            predictiveEngine = PredictiveTextEngine.getInstance(context)
            
            // Initialize database with word data
            coroutineScope.launch {
                wordDatabase?.populateInitialData(context)
                Log.d(TAG, "Enhanced AI engines initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing enhanced AI engines", e)
            isEnhancedMode = false // Fall back to basic mode
        }
    }
    
    // HTTP client for AI API calls
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Initialize the AI service bridge - simplified version without Flutter engine
     * Uses built-in AI logic instead of Flutter services for system keyboard
     */
    fun initialize(context: Context) {
        this.context = context
        
        try {
            // For now, use built-in AI logic instead of Flutter engine
            // This avoids the complexity of running Flutter in keyboard service
            isInitialized = true
            Log.d(TAG, "AI Service Bridge initialized with built-in logic")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AI Service Bridge", e)
        }
    }
    
    /**
     * Get autocorrect and predictive text suggestions using built-in AI logic
     */
    fun getSuggestions(currentWord: String, context: List<String>, callback: AICallback) {
        if (!isInitialized) {
            callback.onError("AI services not initialized")
            return
        }
        
        Log.d(TAG, "Getting suggestions for: '$currentWord' with context: $context")
        
        coroutineScope.launch {
            try {
                val suggestions = if (isEnhancedMode && autocorrectEngine != null && predictiveEngine != null) {
                    generateEnhancedSuggestions(currentWord, context)
                } else {
                    generateBuiltInSuggestions(currentWord, context)
                }
                
                withContext(Dispatchers.Main) {
                    callback.onSuggestionsReady(suggestions)
                }
                
                // Handle autocorrect with new API
                if (isEnhancedMode && autocorrectEngine != null) {
                    val result = autocorrectEngine!!.processBoundary(currentWord)
                    if (result.shouldAutoCorrect && result.topCorrection != null) {
                        withContext(Dispatchers.Main) {
                            callback.onCorrectionReady(currentWord, result.topCorrection!!, 0.9)
                        }
                    }
                } else {
                    // Fallback to built-in correction
                    val correction = getBuiltInCorrection(currentWord)
                    if (correction != null && correction != currentWord) {
                        withContext(Dispatchers.Main) {
                            callback.onCorrectionReady(currentWord, correction, 0.9)
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating suggestions", e)
                withContext(Dispatchers.Main) {
                    callback.onError("Failed to generate suggestions: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Generate suggestions using built-in AI logic
     */
    private suspend fun generateBuiltInSuggestions(currentWord: String, context: List<String>): List<AISuggestion> = withContext(Dispatchers.Default) {
        val suggestions = mutableListOf<AISuggestion>()
        
        if (currentWord.isBlank()) {
            // Default suggestions when no current word
            return@withContext listOf(
                AISuggestion("the", 0.9, "builtin", false),
                AISuggestion("and", 0.8, "builtin", false),
                AISuggestion("to", 0.7, "builtin", false)
            )
        }
        
        val word = currentWord.lowercase().trim()
        
        // Check for common corrections first
        getBuiltInCorrection(word)?.let { correction ->
            if (correction != word) {
                suggestions.add(AISuggestion(correction, 0.95, "autocorrect", true))
            }
        }
        
        // Add predictive suggestions based on current word
        val predictions = getBuiltInPredictions(word, context)
        predictions.forEachIndexed { index, prediction ->
            if (suggestions.size >= 5) return@forEachIndexed
            if (prediction != word && !suggestions.any { it.word == prediction }) {
                val confidence = 0.8 - (index * 0.1) // Decreasing confidence
                suggestions.add(AISuggestion(prediction, confidence, "predictive", false))
            }
        }
        
        // Add prefix completions if we have partial word
        if (word.length >= 2) {
            val completions = getPrefixCompletions(word)
            completions.forEach { completion ->
                if (suggestions.size >= 5) return@forEach
                if (!suggestions.any { it.word == completion }) {
                    suggestions.add(AISuggestion(completion, 0.6, "completion", false))
                }
            }
        }
        
        suggestions
    }
    
    /**
     * Generate enhanced suggestions using the new AI engines
     */
    private suspend fun generateEnhancedSuggestions(currentWord: String, context: List<String>): List<AISuggestion> = withContext(Dispatchers.Default) {
        val suggestions = mutableListOf<AISuggestion>()
        
        try {
            // Get predictive suggestions
            val predictions = predictiveEngine?.getPredictions(
                currentWord = currentWord,
                previousWords = context,
                includeCompletions = currentWord.isNotEmpty(),
                includeNextWords = true
            ) ?: emptyList()
            
            // Convert to AISuggestion format
            predictions.take(5).forEach { prediction ->
                suggestions.add(AISuggestion(
                    word = prediction.word,
                    confidence = (prediction.score * 0.8).coerceIn(0.0, 1.0), // Scale confidence
                    source = "enhanced_${prediction.type.name.lowercase()}",
                    isCorrection = prediction.type == PredictionType.USER // Mark user-learned words as corrections
                ))
            }
            
            // Get autocorrect suggestions using new API
            if (currentWord.isNotEmpty() && autocorrectEngine != null) {
                val candidates = autocorrectEngine!!.getCandidates(currentWord)
                val corrections = candidates.drop(1).map { candidate ->
                    AutocorrectSuggestion(
                        word = candidate.word,
                        confidence = candidate.confidence,
                        editDistance = candidate.editDistance,
                        type = CorrectionType.EDIT_DISTANCE
                    )
                }
                
                corrections.take(2).forEach { correction ->
                    if (!suggestions.any { it.word == correction.word }) {
                        suggestions.add(AISuggestion(
                            word = correction.word,
                            confidence = correction.confidence,
                            source = "enhanced_autocorrect",
                            isCorrection = true
                        ))
                    }
                }
            }
            
            // Sort by confidence
            suggestions.sortByDescending { it.confidence }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced suggestions", e)
            // Fallback to built-in suggestions
            return@withContext generateBuiltInSuggestions(currentWord, context)
        }
        
        suggestions.take(5)
    }
    
    /**
     * Get autocorrect suggestion for a specific word using built-in logic
     */
    fun getCorrection(word: String, previousWord: String, callback: AICallback) {
        if (!isInitialized) {
            callback.onError("AI services not initialized")
            return
        }
        
        coroutineScope.launch {
            try {
                val correction = getBuiltInCorrection(word.lowercase())
                if (correction != null && correction != word.lowercase()) {
                    withContext(Dispatchers.Main) {
                        callback.onCorrectionReady(word, correction, 0.9)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Failed to get correction: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Learn from user input to improve future suggestions
     */
    fun learnFromInput(word: String, context: List<String>) {
        if (!isInitialized) return
        
        coroutineScope.launch {
            try {
                if (word.isNotBlank()) {
                    // Simple learning logic - track word frequency
                    Log.d(TAG, "Learning from input - word: $word, context: $context")
                    
                    // Could implement:
                    // - Update word frequency counters
                    // - Track bigram/trigram patterns
                    // - Store user preferences
                    // For now, just acknowledge the learning
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in learnFromInput", e)
            }
        }
    }
    
    /**
     * Learn from user corrections
     */
    fun learnCorrection(original: String, corrected: String, userConfirmed: Boolean) {
        if (!isInitialized) return
        
        coroutineScope.launch {
            try {
                if (original.isNotBlank() && corrected.isNotBlank() && original != corrected) {
                    Log.d(TAG, "Learning correction - original: $original, corrected: $corrected, userConfirmed: $userConfirmed")
                    
                    // Could implement:
                    // - Store user-specific corrections
                    // - Update correction confidence scores
                    // - Track correction patterns
                    // For now, just acknowledge the learning
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in learnCorrection", e)
            }
        }
    }
    
    /**
     * Get AI service statistics
     */
    fun getStats(callback: AICallback) {
        if (!isInitialized) {
            callback.onError("AI services not initialized")
            return
        }
        
        coroutineScope.launch {
            try {
                Log.d(TAG, "AI Stats: Built-in logic active, ready for suggestions")
                // Could implement actual stats tracking here
                // For now, just indicate that the service is working
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Stats error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Clear learning data
     */
    fun clearLearningData() {
        if (!isInitialized) return
        
        coroutineScope.launch {
            try {
                Log.d(TAG, "Clearing learning data (built-in logic)")
                // Could implement:
                // - Clear stored user patterns
                // - Reset word frequency counters
                // - Clear correction history
                // For now, just acknowledge the clear operation
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing learning data", e)
            }
        }
    }
    
    /**
     * Get built-in autocorrection for common typos
     */
    private fun getBuiltInCorrection(word: String): String? {
        return when (word.lowercase()) {
            "teh" -> "the"
            "adn" -> "and"
            "hte" -> "the"
            "nad" -> "and"
            "yuo" -> "you"
            "taht" -> "that"
            "woudl" -> "would"
            "coudl" -> "could"
            "shoudl" -> "should"
            "recieve" -> "receive"
            "seperate" -> "separate"
            "definately" -> "definitely"
            "occured" -> "occurred"
            "begining" -> "beginning"
            "wiht" -> "with"
            "thier" -> "their"
            "freind" -> "friend"
            "becuase" -> "because"
            "beleive" -> "believe"
            "acheive" -> "achieve"
            "neccessary" -> "necessary"
            "alot" -> "a lot"
            "dont" -> "don't"
            "cant" -> "can't"
            "wont" -> "won't"
            "shouldnt" -> "shouldn't"
            "couldnt" -> "couldn't"
            "wouldnt" -> "wouldn't"
            "isnt" -> "isn't"
            "arent" -> "aren't"
            "wasnt" -> "wasn't"
            "werent" -> "weren't"
            "havent" -> "haven't"
            "hasnt" -> "hasn't"
            "hadnt" -> "hadn't"
            "didnt" -> "didn't"
            "doesnt" -> "doesn't"
            else -> null
        }
    }
    
    /**
     * Get predictive suggestions based on current word and context
     */
    private fun getBuiltInPredictions(currentWord: String, context: List<String>): List<String> {
        val predictions = mutableListOf<String>()
        
        // Context-based predictions
        if (context.isNotEmpty()) {
            val lastWord = context.last().lowercase()
            
            val contextPredictions = when (lastWord) {
                "i" -> listOf("am", "will", "have", "think", "want")
                "you" -> listOf("are", "can", "will", "have", "should")
                "the" -> listOf("best", "most", "only", "same", "first")
                "and" -> listOf("the", "I", "then", "also", "now")
                "to" -> listOf("be", "do", "go", "get", "see")
                "how" -> listOf("are", "do", "to", "can", "about")
                "what" -> listOf("is", "are", "do", "about", "time")
                "good" -> listOf("morning", "evening", "night", "luck", "job")
                "thank" -> listOf("you", "goodness", "God")
                "hello" -> listOf("there", "everyone", "world")
                else -> emptyList()
            }
            predictions.addAll(contextPredictions)
        }
        
        // Prefix-based predictions
        if (currentWord.isNotEmpty()) {
            val prefix = currentWord.lowercase()
            
            val prefixPredictions = when {
                prefix.startsWith("h") -> listOf("hello", "have", "how", "here", "help", "home", "happy")
                prefix.startsWith("w") -> listOf("what", "when", "where", "why", "who", "will", "with", "work", "want")
                prefix.startsWith("t") -> listOf("the", "that", "this", "they", "then", "time", "think", "thank")
                prefix.startsWith("i") -> listOf("is", "it", "in", "if", "I'll", "into", "important")
                prefix.startsWith("a") -> listOf("and", "are", "all", "about", "after", "also", "any")
                prefix.startsWith("s") -> listOf("so", "some", "see", "should", "still", "said", "same")
                prefix.startsWith("c") -> listOf("can", "could", "come", "call", "change", "check")
                prefix.startsWith("m") -> listOf("make", "more", "may", "might", "most", "much", "my")
                prefix.startsWith("n") -> listOf("not", "now", "new", "need", "never", "next", "no")
                prefix.startsWith("g") -> listOf("get", "go", "good", "give", "great", "going")
                else -> emptyList()
            }
            predictions.addAll(prefixPredictions)
        }
        
        return predictions.distinct()
    }
    
    /**
     * Get word completions for partial input
     */
    private fun getPrefixCompletions(prefix: String): List<String> {
        val lower = prefix.lowercase()
        
        // Common word completions
        val commonWords = listOf(
            "about", "after", "again", "against", "all", "almost", "alone", "along", "already", "also",
            "although", "always", "among", "another", "answer", "any", "anyone", "anything", "appear", "are",
            "around", "ask", "back", "became", "because", "become", "been", "before", "began", "begin",
            "being", "believe", "between", "both", "bring", "build", "business", "call", "came", "can",
            "change", "come", "could", "country", "create", "day", "did", "different", "do", "does",
            "don't", "down", "during", "each", "early", "even", "ever", "every", "example", "experience",
            "fact", "family", "far", "feel", "few", "find", "first", "follow", "for", "found",
            "from", "get", "give", "go", "good", "government", "great", "group", "grow", "hand",
            "happen", "has", "have", "he", "help", "her", "here", "him", "his", "home",
            "house", "how", "however", "human", "important", "include", "increase", "information", "into", "issue",
            "it", "its", "job", "just", "keep", "kind", "know", "large", "last", "later",
            "learn", "leave", "left", "let", "life", "like", "line", "little", "live", "local",
            "long", "look", "lot", "love", "make", "man", "management", "many", "may", "me",
            "member", "might", "minute", "money", "month", "more", "most", "move", "much", "must",
            "my", "name", "national", "nature", "near", "need", "never", "new", "news", "next",
            "night", "no", "not", "nothing", "now", "number", "of", "off", "often", "oh",
            "old", "on", "once", "one", "only", "open", "or", "order", "organization", "other",
            "our", "out", "over", "own", "part", "particular", "party", "people", "person", "place",
            "plan", "play", "point", "political", "possible", "power", "president", "pressure", "pretty", "private",
            "probably", "problem", "program", "provide", "public", "put", "question", "quite", "rather", "really",
            "reason", "receive", "recognize", "record", "red", "reflect", "region", "relate", "relationship", "religious",
            "remain", "remember", "remove", "report", "represent", "republican", "require", "research", "resource", "respond",
            "result", "return", "reveal", "right", "rise", "risk", "road", "rock", "role", "room",
            "rule", "run", "safe", "same", "save", "say", "scene", "school", "science", "score",
            "sea", "season", "seat", "second", "section", "security", "see", "seek", "seem", "sell",
            "send", "senior", "sense", "series", "serious", "serve", "service", "set", "seven", "several",
            "sex", "sexual", "shake", "share", "she", "shoot", "short", "shot", "should", "show",
            "significant", "similar", "simple", "simply", "since", "sing", "single", "sister", "sit", "site",
            "situation", "six", "size", "skill", "skin", "small", "smile", "so", "social", "society",
            "soldier", "some", "somebody", "someone", "something", "sometimes", "son", "song", "soon", "sort",
            "sound", "source", "south", "southern", "space", "speak", "special", "specific", "spend", "spent",
            "split", "sport", "spring", "staff", "stage", "stand", "standard", "star", "start", "state",
            "statement", "station", "stay", "step", "still", "stock", "stop", "store", "story", "strategy",
            "street", "strong", "structure", "student", "study", "stuff", "style", "subject", "success", "successful",
            "such", "suddenly", "suffer", "suggest", "summer", "support", "sure", "surface", "system", "table",
            "take", "talk", "task", "tax", "teach", "teacher", "team", "technology", "television", "tell",
            "ten", "tend", "term", "test", "than", "thank", "that", "the", "their", "them",
            "themselves", "then", "theory", "there", "these", "they", "thing", "think", "third", "this",
            "those", "though", "thought", "thousand", "threat", "three", "through", "throughout", "throw", "thus",
            "time", "to", "today", "together", "tonight", "too", "top", "total", "tough", "toward",
            "town", "trade", "traditional", "training", "travel", "treat", "treatment", "tree", "trial", "trip",
            "trouble", "true", "truth", "try", "turn", "TV", "two", "type", "under", "understand",
            "unit", "until", "up", "upon", "us", "use", "used", "user", "usually", "value",
            "various", "very", "victim", "view", "violence", "visit", "voice", "vote", "wait", "walk",
            "wall", "want", "war", "watch", "water", "way", "we", "weapon", "wear", "week",
            "weight", "well", "west", "western", "what", "whatever", "when", "where", "whether", "which",
            "while", "white", "who", "whole", "whom", "whose", "why", "wide", "wife", "will",
            "win", "wind", "window", "wish", "with", "within", "without", "woman", "wonder", "word",
            "work", "worker", "world", "worry", "worse", "worst", "would", "write", "writer", "wrong",
            "yard", "yeah", "year", "yes", "yet", "you", "young", "your", "yourself"
        )
        
        return commonWords
            .filter { it.startsWith(lower) && it.length > lower.length }
            .take(10) // Limit completions
    }
    
    /**
     * Check if AI services are ready
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * Cleanup resources
     */
    fun destroy() {
        try {
            coroutineScope.cancel()
            flutterEngine?.destroy()
            flutterEngine = null
            methodChannel = null
            isInitialized = false
            Log.d(TAG, "AI Service Bridge destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying AI Service Bridge", e)
        }
    }
}
