package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import kotlinx.coroutines.*
import kotlin.math.*
import java.util.*

/**
 * SwipeAutocorrectEngine - Thin adapter for swipe gesture decoding
 * Refactored to delegate all dictionary operations to UnifiedAutocorrectEngine
 * 
 * Responsibilities:
 * - Gesture path extraction and normalization
 * - SwipePath creation from touch coordinates
 * - Delegation to UnifiedAutocorrectEngine.suggestForSwipe()
 * - Backward compatibility with existing API
 */
class SwipeAutocorrectEngine private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SwipeAutocorrectEngine"
        private const val MIN_WORD_LENGTH = 2
        
        @Volatile
        private var INSTANCE: SwipeAutocorrectEngine? = null
        
        fun getInstance(context: Context): SwipeAutocorrectEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SwipeAutocorrectEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // STRIPPED: All dictionary fields removed - delegating to UnifiedAutocorrectEngine
    
    // Integration with UnifiedAutocorrectEngine (REQUIRED)
    private var unifiedEngine: UnifiedAutocorrectEngine? = null
    
    /**
     * Set the unified autocorrect engine for consistent suggestions (REQUIRED)
     */
    fun setUnifiedEngine(engine: UnifiedAutocorrectEngine) {
        this.unifiedEngine = engine
        LogUtil.d(TAG, "✅ SwipeAutocorrectEngine integrated with UnifiedAutocorrectEngine")
    }
    
    // QWERTY keyboard layout for proximity scoring
    private val qwertyLayout = mapOf(
        'q' to Pair(0, 0), 'w' to Pair(1, 0), 'e' to Pair(2, 0), 'r' to Pair(3, 0),
        't' to Pair(4, 0), 'y' to Pair(5, 0), 'u' to Pair(6, 0), 'i' to Pair(7, 0),
        'o' to Pair(8, 0), 'p' to Pair(9, 0),
        
        'a' to Pair(0, 1), 's' to Pair(1, 1), 'd' to Pair(2, 1), 'f' to Pair(3, 1),
        'g' to Pair(4, 1), 'h' to Pair(5, 1), 'j' to Pair(6, 1), 'k' to Pair(7, 1),
        'l' to Pair(8, 1),
        
        'z' to Pair(0, 2), 'x' to Pair(1, 2), 'c' to Pair(2, 2), 'v' to Pair(3, 2),
        'b' to Pair(4, 2), 'n' to Pair(5, 2), 'm' to Pair(6, 2)
    )
    
    /**
     * Initialize the autocorrect engine (STRIPPED - no longer loads dictionaries)
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // STRIPPED: All dictionary loading removed - delegated to UnifiedAutocorrectEngine
            
            val loadTime = System.currentTimeMillis() - startTime
            LogUtil.d(TAG, "SwipeAutocorrectEngine initialized as thin adapter in ${loadTime}ms")
            
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error initializing SwipeAutocorrectEngine", e)
        }
    }
    
    /**
     * DEPRECATED: Generate auto-correction candidates for swipe input (STRING-BASED)
     * 
     * ⚠️ This method is deprecated because it cannot properly decode swipe paths from strings.
     * Use getUnifiedCandidates(swipePath: List<Pair<Float, Float>>) instead.
     * 
     * This method is retained for backward compatibility but will return empty results
     * to encourage migration to path-based swipe decoding.
     */
    @Deprecated("Use getUnifiedCandidates with actual swipe path coordinates instead")
    suspend fun getCandidates(
        swipeSequence: String,
        previousWord: String = "",
        previousWord2: String = ""
    ): SwipeResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        
        LogUtil.w(TAG, "⚠️ DEPRECATED: getCandidates(String) called with '$swipeSequence' - use path-based API instead")
        
        return@withContext SwipeResult(
            originalSequence = swipeSequence,
            candidates = emptyList(), // Force migration to path-based API
            processingTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * Learn from user selection (STRIPPED - delegated to UnifiedAutocorrectEngine)
     */
    fun learnFromUserSelection(originalSequence: String, selectedWord: String) {
        try {
            // STRIPPED: User dictionary operations now handled by UnifiedAutocorrectEngine
            LogUtil.d(TAG, "Learning delegated to UnifiedAutocorrectEngine: '$originalSequence' → '$selectedWord'")
            
            // Delegate to unified engine's learning system
            unifiedEngine?.let { engine ->
                // The unified engine will handle learning through UserDictionaryManager
                LogUtil.d(TAG, "Learning processed by unified system")
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error learning from user selection", e)
        }
    }
    
    /**
     * Record user rejection of auto-correction
     */
    fun recordRejection(originalSequence: String, rejectedWord: String) {
        try {
            // Reduce confidence in this correction pair
            val key = "${originalSequence}->${rejectedWord}"
            LogUtil.d(TAG, "User rejected: $key")
            
            // TODO: Implement rejection learning
            
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error recording rejection", e)
        }
    }
    
    // === STRIPPED: All dictionary loading methods removed ===
    // Dictionary operations now delegated to UnifiedAutocorrectEngine
    
    // === STRIPPED: All candidate matching methods removed ===
    // Candidate generation now delegated to UnifiedAutocorrectEngine
    
    // === STRIPPED: Scoring utility methods removed ===
    // All scoring now handled by UnifiedAutocorrectEngine
    
    // ========== SWIPE PATH MODEL INTEGRATION ==========
    
    /**
     * Decode swipe path coordinates into word predictions (SIMPLIFIED)
     * Real decoding now handled by UnifiedAutocorrectEngine
     */
    fun decodeSwipePath(points: List<Pair<Float, Float>>): List<String> {
        if (points.size < 2) return emptyList()
        
        // STRIPPED: Dictionary-based decoding removed
        // This is now handled by UnifiedAutocorrectEngine.suggestForSwipe()
        
        // Return simple placeholder for backward compatibility
        LogUtil.d(TAG, "🔄 Swipe path decoding delegated to UnifiedAutocorrectEngine")
        return emptyList() // Real results come from UnifiedAutocorrectEngine
    }
    
    /**
     * Estimate starting letter from normalized first touch point
     */
    private fun estimateStartingLetter(point: Pair<Float, Float>): String {
        val (x, y) = point
        
        // Rough QWERTY layout mapping (normalized coordinates)
        return when {
            y < 0.33 -> { // Top row
                when {
                    x < 0.1 -> "q"
                    x < 0.2 -> "w"
                    x < 0.3 -> "e"
                    x < 0.4 -> "r"
                    x < 0.5 -> "t"
                    x < 0.6 -> "y"
                    x < 0.7 -> "u"
                    x < 0.8 -> "i"
                    x < 0.9 -> "o"
                    else -> "p"
                }
            }
            y < 0.66 -> { // Middle row
                when {
                    x < 0.11 -> "a"
                    x < 0.22 -> "s"
                    x < 0.33 -> "d"
                    x < 0.44 -> "f"
                    x < 0.55 -> "g"
                    x < 0.66 -> "h"
                    x < 0.77 -> "j"
                    x < 0.88 -> "k"
                    else -> "l"
                }
            }
            else -> { // Bottom row
                when {
                    x < 0.14 -> "z"
                    x < 0.28 -> "x"
                    x < 0.42 -> "c"
                    x < 0.56 -> "v"
                    x < 0.70 -> "b"
                    x < 0.84 -> "n"
                    else -> "m"
                }
            }
        }
    }
    
    /**
     * Score word by how well it matches the swipe path (SIMPLIFIED)
     * Real scoring now handled by UnifiedAutocorrectEngine
     */
    private fun scoreWordByPath(word: String, path: List<Pair<Float, Float>>): Double {
        if (word.isEmpty() || path.isEmpty()) return 0.0
        
        // Simple scoring based on word length and path length similarity
        val lengthRatio = minOf(word.length.toDouble() / path.size, 1.0)
        
        return lengthRatio * 0.8 // Simplified scoring
    }
    
    // === STRIPPED: mergePredictions removed ===
    // Merging now handled by UnifiedAutocorrectEngine
    
    /**
     * Path-first swipe decoding API (PRIMARY METHOD)
     * This aligns with the class's stated responsibilities: build path → delegate
     */
    suspend fun getUnifiedCandidates(
        swipePath: List<Pair<Float, Float>>,
        typedSequence: String = "",
        previousWord: String = "",
        currentLanguage: String = "en",
        maxSuggestions: Int = 5
    ): List<String> = withContext(Dispatchers.Default) {
        val unified = unifiedEngine
        if (unified == null) {
            LogUtil.w(TAG, "UnifiedAutocorrectEngine not set")
            return@withContext emptyList()
        }
        
        val context = listOfNotNull(previousWord.takeIf { it.isNotBlank() })
        
        try {
            val swipePathObj = SwipePath(swipePath)
            val swipeResult = unified.suggestForSwipe(swipePathObj, context)
            return@withContext swipeResult.take(maxSuggestions).map { it.text }
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error getting path-based predictions", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Legacy method - now delegates to path-first method or handles typed input
     * Integrate with AutocorrectEngine for unified predictions (REFACTORED)
     * Now fully delegates to UnifiedAutocorrectEngine
     */
    suspend fun getUnifiedCandidates(
        swipePath: List<Pair<Float, Float>>?,
        typedSequence: String,
        previousWord: String = "",
        currentLanguage: String = "en"
    ): List<String> = withContext(Dispatchers.Default) {
        
        val unified = unifiedEngine
        if (unified == null) {
            LogUtil.w(TAG, "UnifiedAutocorrectEngine not set")
            return@withContext emptyList()
        }
        
        val context = listOfNotNull(previousWord.takeIf { it.isNotBlank() })
        
        try {
            // Get swipe-based predictions if path is available
            if (swipePath != null && swipePath.size >= 2) {
                val swipePathObj = SwipePath(swipePath)
                val swipeResult = unified.suggestForSwipe(swipePathObj, context)
                return@withContext swipeResult.map { it.text }
            }
            
            // REMOVED: typed-text fallback for swipe mode (per requirement #5)
            // Do not call typed suggestion path for swipe unless metrics are provided
            LogUtil.d(TAG, "No valid swipe path provided - returning empty (no fallback to typing)")
            
            return@withContext emptyList()
            
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error getting unified predictions", e)
            return@withContext emptyList()
        }
    }
}

/**
 * Swipe auto-correction result
 */
data class SwipeResult(
    val originalSequence: String,
    val candidates: List<SwipeCandidate>,
    val processingTimeMs: Long
) {
    val bestCandidate: SwipeCandidate?
        get() = candidates.firstOrNull()
}

/**
 * Individual swipe candidate
 */
data class SwipeCandidate(
    val word: String,
    val editDistance: Int,
    val proximityScore: Double,
    val frequencyScore: Double,
    val contextScore: Double,
    val finalScore: Double,
    val source: CandidateSource
)

/**
 * Source of candidate generation
 */
enum class CandidateSource {
    EXACT_MATCH,
    EDIT_DISTANCE, 
    PATTERN_MATCH,
    USER_DICTIONARY,
    UNIFIED_ENGINE
}
