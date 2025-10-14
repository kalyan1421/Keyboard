package com.example.ai_keyboard

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import com.google.firebase.storage.FirebaseStorage
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Data structures for unified language resources
 */
typealias Lexicon = Map<String, Int>  // word → frequency
typealias NGram2 = Map<Pair<String, String>, Int>  // bigrams
typealias NGram3 = Map<Triple<String, String, String>, Int>  // trigrams
typealias NGram4 = Map<List<String>, Int>  // quadgrams (optional)

/**
 * LanguageResources DTO - Single container for all language data
 * Used by UnifiedAutocorrectEngine as single source of truth
 */
data class LanguageResources(
    val lang: String,
    val words: Lexicon,                 // Trie/DAWG set + freq
    val bigrams: NGram2,                // Map<Pair<String,String>, Int>
    val trigrams: NGram3,               // Map<Triple<String,String,String>, Int>
    val quadgrams: NGram4?,             // Map<List<String>, Int> (optional)
    val corrections: Map<String, String>,
    val userWords: Set<String>,
    val shortcuts: Map<String, String>
)

/**
 * Interface for MultilingualDictionary as specified
 */
interface MultilingualDictionary {
    suspend fun preload(lang: String)
    fun get(lang: String): LanguageResources?
    fun isLoaded(lang: String): Boolean
}

/**
 * MultilingualDictionary - Single source of truth for language data
 * Refactored to use LanguageResources DTO and eliminate duplicate loading
 * 
 * Features:
 * - LanguageResources DTO with all n-gram data
 * - Thread-safe resource management
 * - User dictionary and corrections integration
 * - Lazy loading with atomic updates
 */
class MultilingualDictionaryImpl(private val context: Context) : MultilingualDictionary {
    
    companion object {
        private const val TAG = "MultilingualDict"
        private const val MAX_WORDS_PER_LANGUAGE = 50000
        private const val MAX_BIGRAMS_PER_LANGUAGE = 100000
        private const val MAX_TRIGRAMS_PER_LANGUAGE = 50000
    }
    
    // Thread-safe resource storage
    private val languageResources = ConcurrentHashMap<String, LanguageResources>()
    
    // Track which languages are loaded
    private val loadedLanguages = ConcurrentHashMap.newKeySet<String>()
    
    // Loading jobs for async operations
    private val loadingJobs = ConcurrentHashMap<String, Job>()
    
    // Helper to check asset existence
    private fun assetExists(path: String): Boolean {
        return try {
            context.assets.open(path).close()
            true
        } catch (_: Exception) {
            false
        }
    }
    
    // User dictionary and corrections integration
    private var userDictionaryManager: UserDictionaryManager? = null
    private var dictionaryManager: DictionaryManager? = null
    
    /**
     * Set user dictionary manager for integration
     */
    fun setUserDictionaryManager(manager: UserDictionaryManager) {
        this.userDictionaryManager = manager
    }
    
    /**
     * Set dictionary manager for integration
     */
    fun setDictionaryManager(manager: DictionaryManager) {
        this.dictionaryManager = manager
    }

    /**
     * Check if a language is loaded (interface method)
     */
    override fun isLoaded(lang: String): Boolean {
        return loadedLanguages.contains(lang)
    }
    
    /**
     * Get LanguageResources for a language (interface method)
     * Returns immutable snapshot of language data
     */
    override fun get(lang: String): LanguageResources? {
        return languageResources[lang]
    }
    
    /**
     * Preload a language dictionary (interface method)
     * This is the single entry point for loading language data
     */
    override suspend fun preload(lang: String) {
        if (isLoaded(lang) || loadingJobs.containsKey(lang)) {
            LogUtil.d(TAG, "Language $lang already loaded or loading")
            return
        }
        
        LogUtil.d(TAG, "📚 Starting preload for language: $lang")
        
        val job = GlobalScope.launch(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                // Load base dictionary data
                val words = loadWordsFromAsset(lang)
                val bigrams = loadBigramsFromAsset(lang)
                val trigrams = loadTrigramsFromAsset(lang)
                val quadgrams = loadQuadgramsFromAsset(lang) // Optional
                
                // Load corrections
                val corrections = loadCorrections()
                
                // Get user data
                val userWords = getUserWords(lang)
                val shortcuts = getShortcuts(lang)
                
                // Create immutable LanguageResources
                val resources = LanguageResources(
                    lang = lang,
                    words = words,
                    bigrams = bigrams,
                    trigrams = trigrams,
                    quadgrams = quadgrams,
                    corrections = corrections,
                    userWords = userWords,
                    shortcuts = shortcuts
                )
                
                // Atomically update resources
                languageResources[lang] = resources
                loadedLanguages.add(lang)
                loadingJobs.remove(lang)
                
                val duration = System.currentTimeMillis() - startTime
                LogUtil.d(TAG, "✅ MultilingualDictionary: Loaded $lang: [words, bi, tri, quad] (${duration}ms)")
                
            } catch (e: Exception) {
                LogUtil.e(TAG, "❌ Error preloading language $lang", e)
                loadingJobs.remove(lang)
            }
        }
        
        loadingJobs[lang] = job
        job.join() // Wait for completion
    }
    
    /**
     * Load words from assets/dictionaries/{lang}_words.txt or common sources
     * Returns immutable Lexicon map
     */
    private suspend fun loadWordsFromAsset(language: String): Lexicon = withContext(Dispatchers.IO) {
        return@withContext loadWords(language)
    }
    
    private fun loadWords(language: String): Lexicon {
        val wordMap = mutableMapOf<String, Int>()
        var count = 0
        
        val paths = listOf(
            "dictionaries/${language}_words.txt",      // exact per-lang word list (e.g., en_words.txt)
            "dictionaries/common_words.json"           // fallback JSON (array or {word:freq})
        )

        for (path in paths) {
            if (!assetExists(path)) {
                LogUtil.w(TAG, "Dictionary asset not found: $path")
                continue
            }
            
            context.assets.open(path).bufferedReader().use { br ->
                if (path.endsWith(".txt")) {
                    br.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .forEachIndexed { index, line ->
                            if (count >= MAX_WORDS_PER_LANGUAGE) return@forEachIndexed
                            val parts = line.split(Regex("\\s+"), 2)
                            if (parts.isNotEmpty()) {
                                val word = parts[0]
                                val freq = if (parts.size > 1) {
                                    parts[1].toIntOrNull() ?: (1000 + count)
                                } else {
                                    1000 + count
                                }
                                wordMap[word] = freq
                                count++
                            }
                        }
                } else { // JSON fallback
                    val raw = br.readText()
                    // accept either ["word", ...] or {"word": freq, ...} or {"words": {"word": freq}}
                    val cleaned = raw.trim()
                    val words = if (cleaned.startsWith("[")) {
                        // Array format: ["word1", "word2", ...]
                        org.json.JSONArray(cleaned).let { arr ->
                            (0 until arr.length()).map { i -> 
                                val word = arr.getString(i)
                                wordMap[word] = 1000 + i
                                word
                            }
                        }
                    } else {
                        val jsonObject = org.json.JSONObject(cleaned)
                        when {
                            jsonObject.has("words") -> {
                                // Handle {"words": {"word": freq}} format
                                val wordsObj = jsonObject.getJSONObject("words")
                                wordsObj.keys().asSequence().map { word ->
                                    val freq = wordsObj.getInt(word)
                                    wordMap[word] = freq
                                    word
                                }.toList()
                            }
                            else -> {
                                // Handle direct {"word": freq, ...} format (root level keys)
                                jsonObject.keys().asSequence().map { word ->
                                    val freq = jsonObject.getInt(word)
                                    wordMap[word] = freq
                                    word
                                }.toList()
                            }
                        }
                    }
                    count += words.size
                }
            }
            break // Successfully loaded from this path
        }
        
        LogUtil.d(TAG, "📖 Loaded $count words for $language")
        return wordMap.toMap()
    }
    
    
    
    /**
     * Load bigrams from assets/dictionaries/{lang}_bigrams.txt
     * Returns immutable NGram2 map
     */
    private suspend fun loadBigramsFromAsset(language: String): NGram2 = withContext(Dispatchers.IO) {
        return@withContext loadBigrams(language)
    }
    
    private fun loadBigrams(language: String): NGram2 {
        val bigramMap = mutableMapOf<Pair<String, String>, Int>()
        var count = 0
        
        // special Hindi native bigrams file is present in your assets
        val langBigram = when {
            language == "hi" && assetExists("dictionaries/hi_bigrams_native.txt") ->
                "dictionaries/hi_bigrams_native.txt"
            assetExists("dictionaries/${language}_bigrams.txt") ->
                "dictionaries/${language}_bigrams.txt"
            else -> null
        }

        val candidates = listOfNotNull(
            langBigram,                                 // preferred per-lang txt
            "dictionaries/common_bigrams.json"          // fallback JSON
        )

        for (path in candidates) {
            if (!assetExists(path)) {
                LogUtil.w(TAG, "Dictionary asset not found: $path")
                continue
            }
            
            context.assets.open(path).bufferedReader().use { br ->
                if (path.endsWith(".txt")) {
                    // Accept lines like "word1 word2" or "word1,word2[,weight]"
                    br.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .forEach { line ->
                            if (count >= MAX_BIGRAMS_PER_LANGUAGE) return@forEach
                            val parts = when {
                                "," in line -> line.split(",")
                                else -> line.split(Regex("\\s+"))
                            }
                            if (parts.size >= 2) {
                                val freq = if (parts.size > 2) {
                                    parts[2].toIntOrNull() ?: 10
                                } else {
                                    10
                                }
                                bigramMap[Pair(parts[0], parts[1])] = freq
                                count++
                            }
                        }
                } else {
                    val jsonString = br.readText()
                    val obj = org.json.JSONObject(jsonString)
                    val bigrams = obj.optJSONObject("bigrams")
                    bigrams?.keys()?.forEach { key ->
                        val w = key.trim()
                        val sp = when {
                            "," in w -> w.split(",")
                            else -> w.split(Regex("\\s+"))
                        }
                        if (sp.size >= 2) {
                            val freq = bigrams.getInt(key)
                            bigramMap[Pair(sp[0], sp[1])] = freq
                            count++
                        }
                    }
                }
            }
            break // Successfully loaded from this path
        }
        
        LogUtil.d(TAG, "📊 Loaded $count bigrams for $language")
        return bigramMap.toMap()
    }
    
    
    
    // === LEGACY COMPATIBILITY METHODS ===
    // These methods provide backward compatibility for existing code
    
    /**
     * Get word candidates that start with prefix (legacy compatibility)
     * Delegates to LanguageResources data
     */
    fun getCandidates(prefix: String, language: String, limit: Int = 64): List<String> {
        val resources = get(language) ?: return emptyList()
        val prefixLower = prefix.lowercase()
        
        return resources.words.entries
            .filter { it.key.lowercase().startsWith(prefixLower) }
            .sortedBy { it.value } // Lower frequency rank = more common
            .take(limit)
            .map { it.key }
    }
    
    /**
     * Get all words for a language (legacy compatibility)
     */
    fun getAllWords(language: String): List<String> {
        val resources = get(language) ?: return emptyList()
        return resources.words.keys.toList()
    }
    
    /**
     * Get frequency rank for a word (legacy compatibility)
     */
    fun getFrequency(language: String, word: String): Int {
        val resources = get(language) ?: return Int.MAX_VALUE
        return resources.words[word] ?: Int.MAX_VALUE
    }
    
    /**
     * Get bigram frequency (legacy compatibility)
     */
    fun getBigramFrequency(language: String, w1: String, w2: String): Int {
        val resources = get(language) ?: return 0
        return resources.bigrams[Pair(w1, w2)] ?: 0
    }
    
    /**
     * Get next word predictions based on bigram frequency (legacy compatibility)
     */
    fun getBigramNextWords(language: String, previousWord: String, limit: Int = 5): List<String> {
        val resources = get(language) ?: return emptyList()
        val normalizedPrev = previousWord.lowercase().trim()
        
        return try {
            resources.bigrams
                .filterKeys { it.first == normalizedPrev }
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key.second }
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error getting bigram next words for '$previousWord'", e)
            emptyList()
        }
    }
    
    /**
     * Get trigram frequency (legacy compatibility)
     */
    fun getTrigramFrequency(language: String, w1: String, w2: String, w3: String): Int {
        val resources = get(language) ?: return 0
        return resources.trigrams[Triple(w1, w2, w3)] ?: 0
    }
    
    /**
     * Load trigrams from assets/dictionaries/{lang}_trigrams.txt
     * Returns immutable NGram3 map
     */
    private suspend fun loadTrigramsFromAsset(language: String): NGram3 = withContext(Dispatchers.IO) {
        return@withContext loadTrigrams(language)
    }
    
    private fun loadTrigrams(language: String): NGram3 {
        val trigramMap = mutableMapOf<Triple<String, String, String>, Int>()
        var count = 0
        
        val paths = listOf(
            "dictionaries/${language}_trigrams.txt",    // e.g. en_trigrams.txt
            "dictionaries/en_trigrams.txt"              // fallback to English
        )

        for (path in paths) {
            if (!assetExists(path)) {
                LogUtil.w(TAG, "Dictionary asset not found: $path")
                continue
            }
            
            context.assets.open(path).bufferedReader().use { br ->
                br.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .forEach { line ->
                        if (count >= MAX_TRIGRAMS_PER_LANGUAGE) return@forEach
                        val parts = when {
                            "," in line -> line.split(",")
                            else -> line.split(Regex("\\s+"))
                        }
                        if (parts.size >= 3) {
                            val freq = parts.getOrNull(3)?.toIntOrNull() ?: 1
                            trigramMap[Triple(parts[0], parts[1], parts[2])] = freq
                            count++
                        }
                    }
            }
            break // Successfully loaded from this path
        }
        
        LogUtil.d(TAG, "📊 Loaded $count trigrams for $language")
        return trigramMap.toMap()
    }
    
    
    
    /**
     * Load quadgrams - gracefully returns empty since we don't have quadgram files
     */
    private suspend fun loadQuadgramsFromAsset(language: String): NGram4? = withContext(Dispatchers.IO) {
        return@withContext loadQuadgrams(language)
    }
    
    private fun loadQuadgrams(language: String): NGram4? {
        // You don't have any *_quadgrams.txt. Make this a no-op that gracefully returns empty
        LogUtil.d(TAG, "📊 Quadgrams not available for $language (no quadgram files in assets)")
        return null
    }
    
    /**
     * Load corrections from corrections.json
     */
    private suspend fun loadCorrections(): Map<String, String> = withContext(Dispatchers.IO) {
        val correctionsMap = mutableMapOf<String, String>()
        val path = "dictionaries/corrections.json"
        
        if (!assetExists(path)) {
            LogUtil.w(TAG, "Dictionary asset not found: $path")
            return@withContext emptyMap()
        }
        
        try {
            context.assets.open(path).bufferedReader().use { br ->
                val jsonString = br.readText()
                val jsonObject = org.json.JSONObject(jsonString)
                val corrections = jsonObject.getJSONObject("corrections")
                
                corrections.keys().forEach { key ->
                    val value = corrections.getString(key)
                    correctionsMap[key.lowercase()] = value
                }
            }
            LogUtil.d(TAG, "📝 Loaded ${correctionsMap.size} corrections")
        } catch (e: Exception) {
            LogUtil.e(TAG, "❌ Error loading corrections: ${e.message}")
        }
        
        return@withContext correctionsMap.toMap()
    }
    
    /**
     * Get user words from UserDictionaryManager
     */
    private suspend fun getUserWords(language: String): Set<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            userDictionaryManager?.getTopWords(1000)?.toSet() ?: emptySet()
        } catch (e: Exception) {
            LogUtil.w(TAG, "Error loading user words: ${e.message}")
            emptySet()
        }
    }
    
    /**
     * Get shortcuts from DictionaryManager
     */
    private suspend fun getShortcuts(language: String): Map<String, String> = withContext(Dispatchers.IO) {
        return@withContext try {
            dictionaryManager?.getAllEntriesAsMap() ?: emptyMap()
        } catch (e: Exception) {
            LogUtil.w(TAG, "Error loading shortcuts: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Get next word predictions based on trigram context (legacy compatibility)
     */
    fun getTrigramNextWords(language: String, previousWord1: String, previousWord2: String, limit: Int = 5): List<String> {
        val resources = get(language) ?: return emptyList()
        val normalized1 = previousWord1.lowercase().trim()
        val normalized2 = previousWord2.lowercase().trim()
        
        return try {
            resources.trigrams
                .filterKeys { it.first == normalized1 && it.second == normalized2 }
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key.third }
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error getting trigram next words for '$normalized1 $normalized2'", e)
            emptyList()
        }
    }
    
    /**
     * Check if a word exists in the dictionary (legacy compatibility)
     */
    fun contains(language: String, word: String): Boolean {
        val resources = get(language) ?: return false
        return resources.words.containsKey(word)
    }
    
    /**
     * Get list of currently loaded languages
     */
    fun getLoadedLanguages(): List<String> {
        return loadedLanguages.toList()
    }
    
    /**
     * Get total word count across all loaded languages
     */
    fun getLoadedWordCount(): Int {
        return languageResources.values.sumOf { it.words.size }
    }
    
    /**
     * Get statistics for loaded languages
     */
    fun getStats(): Map<String, Map<String, Int>> {
        val stats = mutableMapOf<String, Map<String, Int>>()
        
        languageResources.forEach { (lang, resources) ->
            stats[lang] = mapOf(
                "words" to resources.words.size,
                "bigrams" to resources.bigrams.size,
                "trigrams" to resources.trigrams.size,
                "corrections" to resources.corrections.size,
                "userWords" to resources.userWords.size,
                "shortcuts" to resources.shortcuts.size
            )
        }
        
        return stats
    }
    
    /**
     * Unload a language to free memory
     */
    fun unloadLanguage(language: String) {
        languageResources.remove(language)
        loadedLanguages.remove(language)
        loadingJobs[language]?.cancel()
        loadingJobs.remove(language)
        LogUtil.d(TAG, "🗑️ Unloaded language: $language")
    }
    
    /**
     * Clear all dictionaries
     */
    fun clear() {
        languageResources.clear()
        loadedLanguages.clear()
        loadingJobs.values.forEach { it.cancel() }
        loadingJobs.clear()
        LogUtil.d(TAG, "🗑️ Cleared all dictionaries")
    }
    
    /**
     * Update resources for a language (for dynamic user word updates)
     */
    fun updateLanguageResources(lang: String, newResources: LanguageResources) {
        languageResources[lang] = newResources
        LogUtil.d(TAG, "🔄 Updated resources for $lang")
    }
    
    /**
     * Legacy compatibility method for loadLanguage
     * Delegates to preload method
     */
    fun loadLanguage(language: String, scope: CoroutineScope) {
        scope.launch {
            try {
                preload(language)
            } catch (e: Exception) {
                LogUtil.e(TAG, "Error in legacy loadLanguage for $language", e)
            }
        }
    }
    
    /**
     * Ensure dictionary file exists, downloading from Firebase if needed
     * This enables hybrid offline-first with CDN fallback approach
     * 
     * @param lang Language code (e.g., "en", "hi")
     * @param type Dictionary type (e.g., "words", "bigrams", "trigrams")
     * @return File if available, null if not found anywhere
     */
    private fun ensureDictionaryFile(lang: String, type: String): File? {
        // First, check if file exists in local filesDir (cached from previous download)
        val cachedFile = File(context.filesDir, "dictionaries/${lang}_${type}.txt")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            LogUtil.d(TAG, "✅ Using cached dictionary: ${cachedFile.name}")
            return cachedFile
        }
        
        // Check if it exists in assets (bundled)
        try {
            context.assets.open("dictionaries/${lang}_${type}.txt").use {
                LogUtil.d(TAG, "✅ Using bundled dictionary: ${lang}_${type}.txt")
                return null // Return null to signal: use assets
            }
        } catch (e: Exception) {
            // Not in assets, try Firebase
        }
        
        // Attempt to download from Firebase Storage
        try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference.child("dictionaries/${lang}_${type}.txt")
            
            // Create cache directory if needed
            val cacheDir = File(context.filesDir, "dictionaries")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // Download synchronously (we're already in a coroutine)
            val tempFile = File.createTempFile("${lang}_${type}", ".txt", context.cacheDir)
            
            // Note: This will block, but we're already in IO dispatcher
            val task = storageRef.getFile(tempFile)
            task.addOnSuccessListener {
                tempFile.copyTo(cachedFile, overwrite = true)
                tempFile.delete()
                LogUtil.d(TAG, "✅ Downloaded and cached $type dictionary for $lang")
            }.addOnFailureListener { exception: Exception ->
                LogUtil.w(TAG, "⚠️ Failed to download $type dictionary for $lang: ${exception.message ?: "Unknown error"}")
                tempFile.delete()
            }
            
            // Wait for completion
            while (!task.isComplete) {
                Thread.sleep(100)
            }
            
            return if (cachedFile.exists() && cachedFile.length() > 0) cachedFile else null
        } catch (e: Exception) {
            LogUtil.e(TAG, "❌ Error downloading dictionary for $lang/$type", e)
            return null
        }
    }
}
