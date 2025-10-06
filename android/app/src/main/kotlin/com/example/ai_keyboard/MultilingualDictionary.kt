package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.*

/**
 * MultilingualDictionary - Lazy-loading dictionary manager for multiple languages
 * 
 * Features:
 * - Lazy load word lists and bigrams per language
 * - Frequency-based word ranking
 * - Bigram context prediction
 * - Memory-efficient (loads on demand)
 * 
 * Phase 2 Integration
 */
class MultilingualDictionary(private val context: Context) {
    
    companion object {
        private const val TAG = "MultilingualDict"
        private const val MAX_WORDS_PER_LANGUAGE = 50000
        private const val MAX_BIGRAMS_PER_LANGUAGE = 100000
    }
    
    // Language-specific word maps: word → frequency rank
    private val wordMaps = mutableMapOf<String, MutableMap<String, Int>>()
    
    // Language-specific bigram maps: "word1 word2" → frequency
    private val bigramMaps = mutableMapOf<String, MutableMap<String, Int>>()
    
    // Track which languages are loaded
    private val loadedLanguages = mutableSetOf<String>()
    
    // Loading jobs for async operations
    private val loadingJobs = mutableMapOf<String, Job>()
    
    /**
     * Check if a language is loaded
     */
    fun isLoaded(language: String): Boolean {
        return loadedLanguages.contains(language)
    }
    
    /**
     * Load dictionary and bigrams for a language (async)
     * Returns immediately; use isLoaded() to check completion
     */
    fun loadLanguage(language: String, scope: CoroutineScope) {
        if (isLoaded(language) || loadingJobs.containsKey(language)) {
            LogUtil.d(TAG, "Language $language already loaded or loading")
            return
        }
        
        LogUtil.d(TAG, "📚 Starting lazy load for language: $language")
        
        val job = scope.launch(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                // Load words
                val wordCount = loadWordsFromAsset(language)
                
                // Load bigrams
                val bigramCount = loadBigramsFromAsset(language)
                
                val duration = System.currentTimeMillis() - startTime
                
                withContext(Dispatchers.Main) {
                    loadedLanguages.add(language)
                    loadingJobs.remove(language)
                    LogUtil.d(TAG, "✅ Loaded $language: $wordCount words, $bigramCount bigrams (${duration}ms)")
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "❌ Error loading language $language", e)
                loadingJobs.remove(language)
            }
        }
        
        loadingJobs[language] = job
    }
    
    /**
     * Load words from assets/dictionaries/{lang}_words.txt
     * Format: word frequency_rank
     * Example: नमस्ते 42
     */
    private fun loadWordsFromAsset(language: String): Int {
        val wordFile = "dictionaries/${language}_words.txt"
        val wordMap = mutableMapOf<String, Int>()
        var count = 0
        
        try {
            context.assets.open(wordFile).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        if (count >= MAX_WORDS_PER_LANGUAGE) return@forEach
                        
                        val parts = line.trim().split(Regex("\\s+"), 2)
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
                }
            }
            
            wordMaps[language] = wordMap
            LogUtil.d(TAG, "📖 Loaded $count words for $language")
        } catch (e: Exception) {
            LogUtil.w(TAG, "⚠️ Could not load words for $language: ${e.message}")
        }
        
        return count
    }
    
    /**
     * Load bigrams from assets/dictionaries/{lang}_bigrams.txt
     * Format: word1 word2 frequency
     * Example: नमस्ते आप 150
     */
    private fun loadBigramsFromAsset(language: String): Int {
        // Try both native and regular bigram files
        val bigramFiles = listOf(
            "dictionaries/${language}_bigrams_native.txt",
            "dictionaries/${language}_bigrams.txt"
        )
        
        val bigramMap = mutableMapOf<String, Int>()
        var count = 0
        
        for (bigramFile in bigramFiles) {
            try {
                context.assets.open(bigramFile).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        reader.lineSequence().forEach { line ->
                            if (count >= MAX_BIGRAMS_PER_LANGUAGE) return@forEach
                            
                            val parts = line.trim().split(Regex("\\s+"))
                            if (parts.size >= 2) {
                                val w1 = parts[0]
                                val w2 = parts[1]
                                val freq = if (parts.size > 2) {
                                    parts[2].toIntOrNull() ?: 10
                                } else {
                                    10
                                }
                                val key = "$w1 $w2"
                                bigramMap[key] = freq
                                count++
                            }
                        }
                    }
                }
                
                // If we successfully loaded from this file, break
                break
            } catch (e: Exception) {
                // Try next file
                continue
            }
        }
        
        if (count > 0) {
            bigramMaps[language] = bigramMap
            LogUtil.d(TAG, "📊 Loaded $count bigrams for $language")
        } else {
            LogUtil.w(TAG, "⚠️ Could not load bigrams for $language")
        }
        
        return count
    }
    
    /**
     * Get word candidates that start with prefix
     * Returns words sorted by frequency (most frequent first)
     */
    fun getCandidates(prefix: String, language: String, limit: Int = 64): List<String> {
        if (!isLoaded(language)) {
            LogUtil.w(TAG, "Language $language not loaded yet")
            return emptyList()
        }
        
        val wordMap = wordMaps[language] ?: return emptyList()
        val prefixLower = prefix.lowercase()
        
        return wordMap.entries
            .filter { it.key.lowercase().startsWith(prefixLower) }
            .sortedBy { it.value } // Lower frequency rank = more common
            .take(limit)
            .map { it.key }
    }
    
    /**
     * Get all words for a language (for edit distance matching)
     */
    fun getAllWords(language: String): List<String> {
        if (!isLoaded(language)) return emptyList()
        return wordMaps[language]?.keys?.toList() ?: emptyList()
    }
    
    /**
     * Get frequency rank for a word (lower = more common)
     * Returns Int.MAX_VALUE if word not found
     */
    fun getFrequency(language: String, word: String): Int {
        if (!isLoaded(language)) return Int.MAX_VALUE
        return wordMaps[language]?.get(word) ?: Int.MAX_VALUE
    }
    
    /**
     * Get bigram frequency
     * Returns 0 if bigram not found
     */
    fun getBigramFrequency(language: String, w1: String, w2: String): Int {
        if (!isLoaded(language)) return 0
        val key = "$w1 $w2"
        return bigramMaps[language]?.get(key) ?: 0
    }
    
    /**
     * Check if a word exists in the dictionary
     */
    fun contains(language: String, word: String): Boolean {
        if (!isLoaded(language)) return false
        return wordMaps[language]?.containsKey(word) ?: false
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
        return wordMaps.values.sumOf { it.size }
    }
    
    /**
     * Get statistics for loaded languages
     */
    fun getStats(): Map<String, Map<String, Int>> {
        val stats = mutableMapOf<String, Map<String, Int>>()
        
        loadedLanguages.forEach { lang ->
            stats[lang] = mapOf(
                "words" to (wordMaps[lang]?.size ?: 0),
                "bigrams" to (bigramMaps[lang]?.size ?: 0)
            )
        }
        
        return stats
    }
    
    /**
     * Unload a language to free memory
     */
    fun unloadLanguage(language: String) {
        wordMaps.remove(language)
        bigramMaps.remove(language)
            loadedLanguages.remove(language)
        loadingJobs[language]?.cancel()
        loadingJobs.remove(language)
        LogUtil.d(TAG, "🗑️ Unloaded language: $language")
    }
    
    /**
     * Clear all dictionaries
     */
    fun clear() {
        wordMaps.clear()
        bigramMaps.clear()
        loadedLanguages.clear()
        loadingJobs.values.forEach { it.cancel() }
        loadingJobs.clear()
        LogUtil.d(TAG, "🗑️ Cleared all dictionaries")
    }
}
