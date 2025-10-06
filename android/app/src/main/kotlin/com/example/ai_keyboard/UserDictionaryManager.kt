package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class UserDictionaryManager(private val context: Context) {
    private val TAG = "UserDictionaryManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid ?: "anonymous"

    private val localFile = File(context.filesDir, "user_words.json")
    private val localMap = mutableMapOf<String, Int>() // word → usageCount
    
    // Rejection blacklist for autocorrect
    private val rejectionBlacklist = mutableSetOf<Pair<String, String>>()
    
    // Debounced save mechanism
    private var saveJob: Job? = null
    private val saveScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadLocalCache()
        loadBlacklist()
    }

    /** Load user words from local JSON file */
    private fun loadLocalCache() {
        if (!localFile.exists()) return
        try {
            val json = JSONObject(localFile.readText())
            json.keys().forEach { key ->
                localMap[key] = json.getInt(key)
            }
            LogUtil.i(TAG, "✅ Loaded ${localMap.size} learned words from local cache.")
        } catch (e: Exception) {
            LogUtil.w(TAG, "⚠️ Failed to load local cache: ${e.message}")
        }
    }

    /** Save local cache to file */
    private fun saveLocalCache() {
        try {
            val json = JSONObject(localMap as Map<*, *>)
            localFile.writeText(json.toString())
            // Enhanced single-line logging
            LogUtil.d(TAG, "💾 Saved user dictionary (${localMap.size} entries)")
        } catch (e: Exception) {
            LogUtil.e(TAG, "❌ Failed to save cache: ${e.message}")
        }
    }

    /** Learn a new word (called from TypingSyncAudit or Autocorrect acceptance) */
    fun learnWord(word: String) {
        if (word.length < 2 || word.any { it.isDigit() }) return
        val count = localMap.getOrDefault(word, 0) + 1
        localMap[word] = count
        LogUtil.d(TAG, "✨ Learned '$word' (count=$count)")
        
        // Debounced save: only save once after 2 seconds of inactivity
        saveJob?.cancel()
        saveJob = saveScope.launch {
            delay(2000)
            saveLocalCache()
        }
    }
    
    /** Force immediate save (call on keyboard close) */
    fun flush() {
        saveJob?.cancel()
        saveLocalCache()
        LogUtil.d(TAG, "🔄 User dictionary flushed to disk")
    }

    /** Push local dictionary to Firestore */
    fun syncToCloud() {
        val data = localMap.entries.map { mapOf("word" to it.key, "count" to it.value) }
        firestore.collection("users")
            .document(userId)
            .collection("user_dictionary")
            .document("words")
            .set(mapOf("entries" to data))
            .addOnSuccessListener {
                LogUtil.i(TAG, "☁️ Synced ${localMap.size} user words to Firestore.")
            }
            .addOnFailureListener {
                LogUtil.w(TAG, "⚠️ Firestore sync failed: ${it.message}")
            }
    }

    /** Pull from Firestore and merge */
    fun syncFromCloud() {
        firestore.collection("users")
            .document(userId)
            .collection("user_dictionary")
            .document("words")
            .get()
            .addOnSuccessListener { doc ->
                val entries = (doc.get("entries") as? List<Map<String, Any>>) ?: return@addOnSuccessListener
                for (entry in entries) {
                    val w = entry["word"] as? String ?: continue
                    val c = (entry["count"] as? Long)?.toInt() ?: 1
                    localMap[w] = (localMap[w] ?: 0) + c
                }
                saveLocalCache()
                LogUtil.i(TAG, "🔄 Merged ${entries.size} cloud words into local cache.")
            }
            .addOnFailureListener {
                LogUtil.w(TAG, "⚠️ Firestore download failed: ${it.message}")
            }
    }

    /** Get top learned words for suggestion ranking */
    fun getTopWords(limit: Int = 50): List<String> =
        localMap.entries.sortedByDescending { it.value }.take(limit).map { it.key }

    /** Check if word is in user dictionary */
    fun hasLearnedWord(word: String): Boolean = localMap.containsKey(word)

    /** Get word usage count */
    fun getWordCount(word: String): Int = localMap[word] ?: 0

    /** Clear all learned words locally and in cloud */
    fun clearAllWords() {
        localMap.clear()
        saveLocalCache()
        
        // Also clear from Firestore
        firestore.collection("users")
            .document(userId)
            .collection("user_dictionary")
            .document("words")
            .delete()
            .addOnSuccessListener {
                LogUtil.i(TAG, "🗑️ Cleared all user words from cloud")
            }
            .addOnFailureListener {
                LogUtil.w(TAG, "⚠️ Failed to clear cloud words: ${it.message}")
            }
    }

    /** Get statistics */
    fun getStats(): Map<String, Int> = mapOf(
        "total_words" to localMap.size,
        "top_usage" to (localMap.values.maxOrNull() ?: 0),
        "avg_usage" to if (localMap.isNotEmpty()) localMap.values.average().toInt() else 0
    )
    
    // ==================== Autocorrect Rejection Blacklist ====================
    
    /**
     * Blacklist a correction that the user rejected
     */
    fun blacklistCorrection(original: String, corrected: String) {
        val pair = Pair(original.lowercase(), corrected.lowercase())
        rejectionBlacklist.add(pair)
        LogUtil.d(TAG, "🚫 Blacklisted correction '$original' → '$corrected'")
        saveBlacklist()
    }
    
    /**
     * Check if a correction is blacklisted
     */
    fun isBlacklisted(original: String, corrected: String): Boolean {
        return rejectionBlacklist.contains(Pair(original.lowercase(), corrected.lowercase()))
    }
    
    /**
     * Save blacklist to SharedPreferences
     */
    private fun saveBlacklist() {
        try {
            val prefs = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
            val json = JSONArray()
            for ((o, c) in rejectionBlacklist) {
                val obj = JSONObject()
                obj.put("o", o)
                obj.put("c", c)
                json.put(obj)
            }
            prefs.edit().putString("rejection_blacklist", json.toString()).apply()
            LogUtil.d(TAG, "💾 Saved ${rejectionBlacklist.size} rejected corrections to prefs")
        } catch (e: Exception) {
            LogUtil.e(TAG, "❌ Failed to save blacklist: ${e.message}")
        }
    }
    
    /**
     * Load blacklist from SharedPreferences
     */
    fun loadBlacklist() {
        try {
            val prefs = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
            val data = prefs.getString("rejection_blacklist", null) ?: return
            val arr = JSONArray(data)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rejectionBlacklist.add(Pair(obj.getString("o"), obj.getString("c")))
            }
            LogUtil.d(TAG, "🧠 Loaded ${rejectionBlacklist.size} rejected corrections from prefs")
        } catch (e: Exception) {
            LogUtil.e(TAG, "⚠️ Error loading blacklist: ${e.message}")
        }
    }
    
    /**
     * Clear all blacklisted corrections
     */
    fun clearBlacklist() {
        rejectionBlacklist.clear()
        val prefs = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("rejection_blacklist").apply()
        LogUtil.d(TAG, "🗑️ Cleared all rejected corrections")
    }
    
    /**
     * Get blacklist size for debugging
     */
    fun getBlacklistSize(): Int = rejectionBlacklist.size
}
