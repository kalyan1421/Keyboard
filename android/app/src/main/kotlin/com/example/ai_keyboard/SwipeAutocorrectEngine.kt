package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.*
import java.util.*

/**
 * Enhanced Swipe Auto-correction Engine
 * Provides Gboard/CleverType-level swipe typing with intelligent word prediction
 * 
 * Features:
 * - QWERTY keyboard proximity scoring
 * - Damerau-Levenshtein edit distance (≤2)
 * - Word frequency + context weighting
 * - User dictionary learning
 * - Sub-5ms candidate generation
 */
class SwipeAutocorrectEngine private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SwipeAutocorrectEngine"
        private const val MAX_EDIT_DISTANCE = 2
        private const val MAX_CANDIDATES = 20
        private const val MIN_WORD_LENGTH = 2
        
        @Volatile
        private var INSTANCE: SwipeAutocorrectEngine? = null
        
        fun getInstance(context: Context): SwipeAutocorrectEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SwipeAutocorrectEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Dictionary and frequency data
    private var mainDictionary = setOf<String>()
    private var wordFrequencies = mapOf<String, Int>()
    private var userDictionary = mutableMapOf<String, Int>() // word -> usage count
    private var bigramFrequencies = mapOf<Pair<String, String>, Int>()
    
    // STEP 2: Integration with UnifiedAutocorrectEngine
    private var unifiedEngine: UnifiedAutocorrectEngine? = null
    
    /**
     * Set the unified autocorrect engine for consistent suggestions
     */
    fun setUnifiedEngine(engine: UnifiedAutocorrectEngine) {
        this.unifiedEngine = engine
        Log.d(TAG, "✅ SwipeAutocorrectEngine integrated with UnifiedAutocorrectEngine")
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
     * Initialize the autocorrect engine
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Load main dictionary
            loadMainDictionary()
            
            // Load word frequencies  
            loadWordFrequencies()
            
            // Load bigram data for context
            loadBigramFrequencies()
            
            // Load user dictionary
            loadUserDictionary()
            
            val loadTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "SwipeAutocorrectEngine initialized in ${loadTime}ms")
            Log.d(TAG, "Loaded ${mainDictionary.size} words, ${userDictionary.size} user words, ${bigramFrequencies.size} bigrams")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SwipeAutocorrectEngine", e)
        }
    }
    
    /**
     * Generate auto-correction candidates for swipe input
     * Target: <5ms processing time
     */
    suspend fun getCandidates(
        swipeSequence: String,
        previousWord: String = "",
        previousWord2: String = ""
    ): SwipeResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        
        if (swipeSequence.length < MIN_WORD_LENGTH) {
            return@withContext SwipeResult(
                originalSequence = swipeSequence,
                candidates = emptyList(),
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
        
        try {
            val candidates = mutableListOf<SwipeCandidate>()
            val swipeLower = swipeSequence.lowercase()
            
            // Step 1: Exact dictionary matches (highest priority)
            findExactMatches(swipeLower, candidates)
            
            // Step 2: Edit distance matches with proximity scoring
            findProximityMatches(swipeLower, candidates)
            
            // Step 3: Phonetic and pattern matches
            findPatternMatches(swipeLower, candidates)
            
            // Step 4: User dictionary matches (boosted)
            findUserDictionaryMatches(swipeLower, candidates)
            
            // Step 5: Apply context scoring (bigrams)
            applyContextScoring(candidates, previousWord, previousWord2)
            
            // Step 6: Delegate to unified autocorrect engine for consistent scoring
            val lang = "en" // TODO: Get current language from context
            val unifiedCorrections = unifiedEngine?.suggestForSwipe(swipeSequence, lang) ?: emptyList()
            
            // Get ranked candidates first
            val rankedCandidates = rankCandidates(candidates).take(MAX_CANDIDATES)
            
            // Merge swipe predictions with unified corrections
            val mergedCandidates = mergePredictions(
                rankedCandidates.map { it.word }, 
                unifiedCorrections,
                swipeSequence
            )
            
            // Convert back to SwipeCandidate format
            val finalCandidates = mergedCandidates.mapIndexed { index, word ->
                val existingCandidate = rankedCandidates.find { it.word == word }
                existingCandidate ?: SwipeCandidate(
                    word = word,
                    editDistance = 1,
                    proximityScore = 0.8 - index * 0.1,
                    frequencyScore = 0.7 - index * 0.1,
                    contextScore = 0.0,
                    finalScore = 0.8 - index * 0.1,
                    source = CandidateSource.UNIFIED_ENGINE
                )
            }
            
            // rankedCandidates already computed above
            
            val processingTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Generated ${finalCandidates.size} unified candidates in ${processingTime}ms for '$swipeSequence'")
            
            return@withContext SwipeResult(
                originalSequence = swipeSequence,
                candidates = finalCandidates,
                processingTimeMs = processingTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating candidates", e)
            return@withContext SwipeResult(
                originalSequence = swipeSequence,
                candidates = emptyList(),
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Learn from user selection to improve future predictions
     */
    fun learnFromUserSelection(originalSequence: String, selectedWord: String) {
        try {
            // Boost user dictionary entry
            userDictionary[selectedWord] = (userDictionary[selectedWord] ?: 0) + 1
            
            // If user consistently selects a word, increase its base score
            if ((userDictionary[selectedWord] ?: 0) > 3) {
                Log.d(TAG, "Learning: '$originalSequence' → '$selectedWord' (usage: ${userDictionary[selectedWord]})")
            }
            
            // TODO: Persist user dictionary to storage
            
        } catch (e: Exception) {
            Log.e(TAG, "Error learning from user selection", e)
        }
    }
    
    /**
     * Record user rejection of auto-correction
     */
    fun recordRejection(originalSequence: String, rejectedWord: String) {
        try {
            // Reduce confidence in this correction pair
            val key = "${originalSequence}->${rejectedWord}"
            Log.d(TAG, "User rejected: $key")
            
            // TODO: Implement rejection learning
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recording rejection", e)
        }
    }
    
    // === PRIVATE IMPLEMENTATION ===
    
    private suspend fun loadMainDictionary() {
        try {
            val inputStream = context.assets.open("dictionaries/common_words.json")
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonText)
            
            val words = mutableSetOf<String>()
            
            // Load basic words
            if (jsonObject.has("basic_words")) {
                val basicArray = jsonObject.getJSONArray("basic_words")
                for (i in 0 until basicArray.length()) {
                    words.add(basicArray.getString(i).lowercase())
                }
            }
            
            // Load business words
            if (jsonObject.has("business_words")) {
                val businessArray = jsonObject.getJSONArray("business_words")
                for (i in 0 until businessArray.length()) {
                    words.add(businessArray.getString(i).lowercase())
                }
            }
            
            // Load from words array
            if (jsonObject.has("words")) {
                val wordsArray = jsonObject.getJSONArray("words")
                for (i in 0 until wordsArray.length()) {
                    words.add(wordsArray.getString(i).lowercase())
                }
            }
            
            mainDictionary = words
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dictionary", e)
            // Fallback basic dictionary
            mainDictionary = setOf(
                "the", "and", "to", "of", "in", "is", "it", "you", "that", "he", "was",
                "for", "on", "are", "as", "with", "his", "they", "at", "be", "this",
                "have", "from", "or", "one", "had", "by", "word", "but", "not", "what",
                "all", "were", "we", "when", "your", "can", "said", "there", "each",
                "which", "she", "do", "how", "their", "if", "will", "up", "other", "about"
            )
        }
    }
    
    private suspend fun loadWordFrequencies() {
        try {
            val inputStream = context.assets.open("dictionaries/common_words.json")
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonText)
            
            val frequencies = mutableMapOf<String, Int>()
            
            if (jsonObject.has("frequency")) {
                val freqObject = jsonObject.getJSONObject("frequency")
                val keys = freqObject.keys()
                while (keys.hasNext()) {
                    val word = keys.next()
                    frequencies[word.lowercase()] = freqObject.getInt(word)
                }
            }
            
            wordFrequencies = frequencies
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading word frequencies", e)
            wordFrequencies = emptyMap()
        }
    }
    
    private suspend fun loadBigramFrequencies() {
        try {
            // Generate simple bigram frequencies from common patterns
            val commonBigrams = mapOf(
                Pair("the", "quick") to 100,
                Pair("quick", "brown") to 90,
                Pair("brown", "fox") to 80,
                Pair("and", "the") to 95,
                Pair("of", "the") to 90,
                Pair("in", "the") to 85,
                Pair("to", "be") to 80,
                Pair("is", "a") to 75,
                Pair("it", "is") to 70,
                Pair("you", "are") to 65,
                Pair("that", "is") to 60
            )
            
            bigramFrequencies = commonBigrams
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bigram frequencies", e)
            bigramFrequencies = emptyMap()
        }
    }
    
    private suspend fun loadUserDictionary() {
        try {
            // TODO: Load from persistent storage
            userDictionary = mutableMapOf()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user dictionary", e)
        }
    }
    
    private fun findExactMatches(swipeSequence: String, candidates: MutableList<SwipeCandidate>) {
        if (mainDictionary.contains(swipeSequence)) {
            candidates.add(SwipeCandidate(
                word = swipeSequence,
                editDistance = 0,
                proximityScore = 1.0,
                frequencyScore = getFrequencyScore(swipeSequence),
                contextScore = 0.0,
                finalScore = 0.0, // Will be calculated later
                source = CandidateSource.EXACT_MATCH
            ))
        }
    }
    
    private fun findProximityMatches(swipeSequence: String, candidates: MutableList<SwipeCandidate>) {
        // Find dictionary words with edit distance ≤ 2 and good proximity scores
        for (word in mainDictionary) {
            if (abs(word.length - swipeSequence.length) > MAX_EDIT_DISTANCE) continue
            
            val editDistance = calculateEditDistance(swipeSequence, word)
            if (editDistance <= MAX_EDIT_DISTANCE && editDistance > 0) {
                val proximityScore = calculateProximityScore(swipeSequence, word)
                
                // Only include if proximity score is reasonable
                if (proximityScore > 0.3 || editDistance == 1) {
                    candidates.add(SwipeCandidate(
                        word = word,
                        editDistance = editDistance,
                        proximityScore = proximityScore,
                        frequencyScore = getFrequencyScore(word),
                        contextScore = 0.0,
                        finalScore = 0.0,
                        source = CandidateSource.EDIT_DISTANCE
                    ))
                }
            }
        }
    }
    
    private fun findPatternMatches(swipeSequence: String, candidates: MutableList<SwipeCandidate>) {
        // Find words with similar character patterns (for path-based swipe decoding)
        for (word in mainDictionary) {
            if (word.length < MIN_WORD_LENGTH || candidates.any { it.word == word }) continue
            
            val patternScore = calculatePatternScore(swipeSequence, word)
            if (patternScore > 0.6) {
                candidates.add(SwipeCandidate(
                    word = word,
                    editDistance = calculateEditDistance(swipeSequence, word),
                    proximityScore = patternScore,
                    frequencyScore = getFrequencyScore(word),
                    contextScore = 0.0,
                    finalScore = 0.0,
                    source = CandidateSource.PATTERN_MATCH
                ))
            }
        }
    }
    
    private fun findUserDictionaryMatches(swipeSequence: String, candidates: MutableList<SwipeCandidate>) {
        for ((userWord, usageCount) in userDictionary) {
            if (candidates.any { it.word == userWord }) continue
            
            val editDistance = calculateEditDistance(swipeSequence, userWord)
            if (editDistance <= MAX_EDIT_DISTANCE) {
                val proximityScore = calculateProximityScore(swipeSequence, userWord)
                val userBoost = min(usageCount * 0.1, 2.0) // Boost based on usage
                
                candidates.add(SwipeCandidate(
                    word = userWord,
                    editDistance = editDistance,
                    proximityScore = proximityScore,
                    frequencyScore = getFrequencyScore(userWord) + userBoost,
                    contextScore = 0.0,
                    finalScore = 0.0,
                    source = CandidateSource.USER_DICTIONARY
                ))
            }
        }
    }
    
    private fun applyContextScoring(candidates: MutableList<SwipeCandidate>, prev1: String, prev2: String) {
        for (i in candidates.indices) {
            var contextScore = 0.0
            
            // Check bigram with previous word
            if (prev1.isNotEmpty()) {
                val bigramKey = Pair(prev1.lowercase(), candidates[i].word)
                val bigramFreq = bigramFrequencies[bigramKey] ?: 0
                contextScore += bigramFreq * 0.01
            }
            
            // Check trigram context (simplified)
            if (prev2.isNotEmpty() && prev1.isNotEmpty()) {
                // Simple trigram scoring
                contextScore += 0.1
            }
            
            // Replace with updated candidate
            candidates[i] = candidates[i].copy(contextScore = contextScore)
        }
    }
    
    private fun rankCandidates(candidates: List<SwipeCandidate>): List<SwipeCandidate> {
        return candidates.map { candidate ->
            // Calculate final score with weights
            val editPenalty = candidate.editDistance * 0.3
            val proximityBoost = candidate.proximityScore * 0.4
            val frequencyBoost = candidate.frequencyScore * 0.2
            val contextBoost = candidate.contextScore * 0.1
            
            val finalScore = proximityBoost + frequencyBoost + contextBoost - editPenalty
            
            candidate.copy(finalScore = finalScore)
        }.sortedByDescending { it.finalScore }
        .distinctBy { it.word } // Remove duplicates
    }
    
    /**
     * Calculate Damerau-Levenshtein edit distance (insertions, deletions, substitutions, transpositions)
     */
    private fun calculateEditDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        if (len1 == 0) return len2
        if (len2 == 0) return len1
        
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }
        
        // Initialize base cases
        for (i in 0..len1) matrix[i][0] = i
        for (j in 0..len2) matrix[0][j] = j
        
        // Fill matrix
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
                
                // Transposition (Damerau addition)
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    matrix[i][j] = minOf(matrix[i][j], matrix[i - 2][j - 2] + cost)
                }
            }
        }
        
        return matrix[len1][len2]
    }
    
    /**
     * Calculate proximity score based on QWERTY keyboard layout
     */
    private fun calculateProximityScore(swipeSequence: String, word: String): Double {
        if (swipeSequence.length != word.length) {
            // For different lengths, use character-based proximity
            return calculateAlignedProximityScore(swipeSequence, word)
        }
        
        var totalDistance = 0.0
        var validPairs = 0
        
        for (i in swipeSequence.indices) {
            val swipeChar = swipeSequence[i]
            val wordChar = word[i]
            
            val swipePos = qwertyLayout[swipeChar]
            val wordPos = qwertyLayout[wordChar]
            
            if (swipePos != null && wordPos != null) {
                val distance = sqrt(
                    (swipePos.first - wordPos.first).toDouble().pow(2) +
                    (swipePos.second - wordPos.second).toDouble().pow(2)
                )
                totalDistance += distance
                validPairs++
            }
        }
        
        return if (validPairs > 0) {
            val avgDistance = totalDistance / validPairs
            max(0.0, 1.0 - (avgDistance / 3.0)) // Normalize to 0-1 range
        } else {
            0.0
        }
    }
    
    private fun calculateAlignedProximityScore(swipeSequence: String, word: String): Double {
        // Use longest common subsequence for different-length words
        val lcs = longestCommonSubsequence(swipeSequence, word)
        return lcs.toDouble() / max(swipeSequence.length, word.length)
    }
    
    private fun calculatePatternScore(swipeSequence: String, word: String): Double {
        // Calculate pattern similarity (character distribution, etc.)
        val swipeChars = swipeSequence.toCharArray().sorted()
        val wordChars = word.toCharArray().sorted()
        
        val intersection = swipeChars.intersect(wordChars.toSet()).size
        val union = swipeChars.union(wordChars.toSet()).size
        
        return if (union > 0) intersection.toDouble() / union else 0.0
    }
    
    private fun longestCommonSubsequence(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 1..m) {
            for (j in 1..n) {
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        
        return dp[m][n]
    }
    
    private fun getFrequencyScore(word: String): Double {
        val frequency = wordFrequencies[word] ?: 0
        return log10(max(frequency, 1).toDouble() + 1)
    }
    
    // ========== SWIPE PATH MODEL INTEGRATION ==========
    
    /**
     * Decode swipe path coordinates into word predictions
     * Placeholder for future TFLite model integration
     */
    fun decodeSwipePath(points: List<Pair<Float, Float>>): List<String> {
        if (points.size < 2) return emptyList()
        
        // Normalize gesture coordinates to 0-1 range
        val maxX = points.maxOf { it.first }
        val maxY = points.maxOf { it.second }
        val norm = points.map { (x, y) -> 
            Pair(x / maxOf(maxX, 1f), y / maxOf(maxY, 1f))
        }
        
        // Estimate starting letter based on first point
        val startLetter = estimateStartingLetter(norm.first())
        
        // Get candidates starting with estimated letter
        val candidates = mainDictionary.filter { 
            it.startsWith(startLetter) && it.length >= MIN_WORD_LENGTH 
        }.take(20)
        
        // Score candidates by path proximity (simplified scoring)
        val scored = candidates.map { word ->
            val pathScore = scoreWordByPath(word, norm)
            word to pathScore
        }.sortedByDescending { it.second }
        
        Log.d(TAG, "🔄 Swipe path decoded: ${scored.take(3).map { it.first }}")
        return scored.take(5).map { it.first }
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
     * Score word by how well it matches the swipe path
     * Simplified scoring - can be replaced by ML model
     */
    private fun scoreWordByPath(word: String, path: List<Pair<Float, Float>>): Double {
        if (word.isEmpty() || path.isEmpty()) return 0.0
        
        // Simple scoring based on word length and path length similarity
        val lengthRatio = minOf(word.length.toDouble() / path.size, 1.0)
        
        // Frequency boost
        val freqBoost = getFrequencyScore(word)
        
        return lengthRatio * 0.5 + freqBoost * 0.5
    }
    
    /**
     * Merge swipe predictions with text-based autocorrect predictions
     */
    fun mergePredictions(
        swipePreds: List<String>, 
        contextPreds: List<String>,
        currentWord: String = ""
    ): List<String> {
        val merged = mutableListOf<String>()
        
        // Prioritize swipe predictions if they're strong
        merged.addAll(swipePreds.take(2))
        
        // Add context predictions that aren't already included
        contextPreds.forEach { pred ->
            if (!merged.contains(pred) && merged.size < 5) {
                merged.add(pred)
            }
        }
        
        // If current word is partially typed, add prefix completions
        if (currentWord.length >= 2) {
            mainDictionary.filter { 
                it.startsWith(currentWord) && !merged.contains(it) 
            }.take(5 - merged.size).forEach { merged.add(it) }
        }
        
        Log.d(TAG, "🔀 Merged predictions: $merged")
        return merged.take(5).distinct()
    }
    
    /**
     * Integrate with AutocorrectEngine for unified predictions
     * This will be called from AIKeyboardService
     */
    suspend fun getUnifiedCandidates(
        swipePath: List<Pair<Float, Float>>?,
        typedSequence: String,
        previousWord: String = "",
        currentLanguage: String = "en"
    ): List<String> = withContext(Dispatchers.Default) {
        
        val predictions = mutableListOf<String>()
        
        // Get swipe-based predictions if path is available
        if (swipePath != null && swipePath.size >= 2) {
            val swipePreds = decodeSwipePath(swipePath)
            predictions.addAll(swipePreds)
        }
        
        // Get typed-text predictions
        if (typedSequence.isNotEmpty()) {
            val textResult = getCandidates(typedSequence, previousWord)
            predictions.addAll(textResult.candidates.take(3).map { it.word })
        }
        
        // STEP 4: Delegate to unified correction logic
        val decodedWord = predictions.firstOrNull() ?: typedSequence
        if (decodedWord.isNotEmpty() && unifiedEngine != null) {
            try {
                val unified = unifiedEngine!!.getCorrections(decodedWord, currentLanguage, emptyList())
                val unifiedWords = unified.map { it.word }
                predictions.addAll(unifiedWords)
                
                Log.d(TAG, "🔀 Merged predictions: ${predictions.distinct().take(5)} for '$decodedWord'")
            } catch (e: Exception) {
                Log.w(TAG, "Error getting unified predictions: ${e.message}")
            }
        }
        
        // Return merged and deduplicated results
        return@withContext predictions.distinct().take(5)
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
