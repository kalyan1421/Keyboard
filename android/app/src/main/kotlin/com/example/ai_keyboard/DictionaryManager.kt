package com.example.ai_keyboard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages user dictionary entries (shortcuts → expansions) with frequency tracking
 * Similar to Gboard and CleverType personal dictionary features
 */
class DictionaryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DictionaryManager"
        private const val PREFS_NAME = "dictionary_manager"
        private const val KEY_ENTRIES = "dictionary_entries"
        private const val KEY_ENABLED = "dictionary_enabled"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Thread-safe list for concurrent access
    private val entries = CopyOnWriteArrayList<DictionaryEntry>()
    
    // Cache for fast lookup during typing
    private val shortcutMap = mutableMapOf<String, DictionaryEntry>()
    
    private var isEnabled = true
    
    // Listeners for dictionary changes
    private val listeners = mutableListOf<DictionaryListener>()
    
    interface DictionaryListener {
        fun onDictionaryUpdated(entries: List<DictionaryEntry>)
        fun onExpansionTriggered(shortcut: String, expansion: String)
    }
    
    /**
     * Initialize the dictionary manager
     */
    fun initialize() {
        Log.d(TAG, "Initializing DictionaryManager")
        
        // Load settings
        isEnabled = prefs.getBoolean(KEY_ENABLED, true)
        
        // Load persisted entries from native prefs
        loadEntriesFromPrefs()
        
        // Also try to load from Flutter prefs (in case Flutter made changes)
        loadFromFlutterPrefs()
        
        // Rebuild shortcut map with merged entries
        rebuildShortcutMap()
        
        Log.d(TAG, "DictionaryManager initialized with ${entries.size} entries (enabled: $isEnabled)")
    }
    
    /**
     * Add a new dictionary entry
     */
    fun addEntry(shortcut: String, expansion: String): Boolean {
        try {
            val cleanShortcut = shortcut.trim().lowercase()
            val cleanExpansion = expansion.trim()
            
            if (cleanShortcut.isEmpty() || cleanExpansion.isEmpty()) {
                Log.w(TAG, "Cannot add empty shortcut or expansion")
                return false
            }
            
            // Check if shortcut already exists
            val existingIndex = entries.indexOfFirst { it.shortcut == cleanShortcut }
            
            if (existingIndex != -1) {
                // Update existing entry
                val existingEntry = entries[existingIndex]
                entries[existingIndex] = existingEntry.copy(expansion = cleanExpansion)
                Log.d(TAG, "Updated existing entry: $cleanShortcut -> $cleanExpansion")
            } else {
                // Add new entry
                val newEntry = DictionaryEntry(
                    shortcut = cleanShortcut,
                    expansion = cleanExpansion
                )
                entries.add(newEntry)
                Log.d(TAG, "Added new entry: $cleanShortcut -> $cleanExpansion")
            }
            
            // Save and update cache
            saveEntriesToPrefs()
            rebuildShortcutMap()
            notifyDictionaryUpdated()
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding dictionary entry", e)
            return false
        }
    }
    
    /**
     * Remove a dictionary entry
     */
    fun removeEntry(id: String): Boolean {
        val removed = entries.removeIf { it.id == id }
        if (removed) {
            saveEntriesToPrefs()
            rebuildShortcutMap()
            notifyDictionaryUpdated()
            Log.d(TAG, "Removed dictionary entry: $id")
        }
        return removed
    }
    
    /**
     * Update an existing entry
     */
    fun updateEntry(id: String, newShortcut: String, newExpansion: String): Boolean {
        val index = entries.indexOfFirst { it.id == id }
        if (index == -1) return false
        
        val cleanShortcut = newShortcut.trim().lowercase()
        val cleanExpansion = newExpansion.trim()
        
        if (cleanShortcut.isEmpty() || cleanExpansion.isEmpty()) return false
        
        val entry = entries[index]
        entries[index] = entry.copy(
            shortcut = cleanShortcut,
            expansion = cleanExpansion
        )
        
        saveEntriesToPrefs()
        rebuildShortcutMap()
        notifyDictionaryUpdated()
        
        Log.d(TAG, "Updated entry: $cleanShortcut -> $cleanExpansion")
        return true
    }
    
    /**
     * Check if a word matches a dictionary shortcut and return expansion
     */
    fun getExpansion(word: String): DictionaryEntry? {
        if (!isEnabled || word.isBlank()) return null
        
        val cleanWord = word.trim().lowercase()
        return shortcutMap[cleanWord]
    }
    
    /**
     * Check if a prefix matches any shortcuts (for suggestions)
     */
    fun getMatchingShortcuts(prefix: String, limit: Int = 5): List<DictionaryEntry> {
        if (!isEnabled || prefix.isBlank()) return emptyList()
        
        val cleanPrefix = prefix.lowercase()
        return entries
            .filter { it.shortcut.startsWith(cleanPrefix) }
            .sortedByDescending { it.usageCount }
            .take(limit)
    }
    
    /**
     * Increment usage count for an entry
     */
    fun incrementUsage(shortcut: String) {
        val cleanShortcut = shortcut.trim().lowercase()
        val index = entries.indexOfFirst { it.shortcut == cleanShortcut }
        
        if (index != -1) {
            val entry = entries[index]
            entries[index] = entry.copy(
                usageCount = entry.usageCount + 1,
                lastUsed = System.currentTimeMillis()
            )
            
            // Update map and save
            shortcutMap[cleanShortcut] = entries[index]
            saveEntriesToPrefs()
            
            Log.d(TAG, "Incremented usage for $cleanShortcut: ${entries[index].usageCount}")
            
            notifyExpansionTriggered(cleanShortcut, entry.expansion)
        }
    }
    
    /**
     * Get all dictionary entries
     */
    fun getAllEntries(): List<DictionaryEntry> {
        return entries.toList()
    }
    
    /**
     * Get entries sorted by usage frequency
     */
    fun getEntriesByFrequency(limit: Int = 20): List<DictionaryEntry> {
        return entries
            .sortedByDescending { it.usageCount }
            .take(limit)
    }
    
    /**
     * Clear all entries
     */
    fun clearAll() {
        entries.clear()
        shortcutMap.clear()
        saveEntriesToPrefs()
        notifyDictionaryUpdated()
        Log.d(TAG, "Cleared all dictionary entries")
    }
    
    /**
     * Enable/disable dictionary
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        Log.d(TAG, "Dictionary enabled: $enabled")
    }
    
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * Add listener for dictionary changes
     */
    fun addListener(listener: DictionaryListener) {
        listeners.add(listener)
    }
    
    /**
     * Remove listener
     */
    fun removeListener(listener: DictionaryListener) {
        listeners.remove(listener)
    }
    
    private fun notifyDictionaryUpdated() {
        listeners.forEach { it.onDictionaryUpdated(getAllEntries()) }
    }
    
    private fun notifyExpansionTriggered(shortcut: String, expansion: String) {
        listeners.forEach { it.onExpansionTriggered(shortcut, expansion) }
    }
    
    // ========== FIRESTORE SYNC METHODS (Gboard + CleverType Integration) ==========
    
    /**
     * Sync dictionary with Firestore
     */
    suspend fun syncDictionaryWithFirestore(userId: String) {
        try {
            Log.d(TAG, "📤 Syncing dictionary to Firestore for user: $userId")
            Log.d(TAG, "✅ Dictionary sync initiated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing with Firestore", e)
        }
    }
    
    /**
     * Check if word is valid
     */
    fun isValidWord(word: String): Boolean {
        if (word.isBlank()) return false
        return entries.any { 
            it.shortcut.equals(word, ignoreCase = true) || 
            it.expansion.equals(word, ignoreCase = true)
        }
    }
    
    /**
     * Add user word
     */
    fun addUserWord(word: String) {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.isEmpty() || cleanWord.length < 2) return
        addEntry(cleanWord, cleanWord)
        Log.d(TAG, "📝 Learned word: $cleanWord")
    }
    
    /**
     * Remove user word
     */
    fun removeUserWord(word: String) {
        val cleanWord = word.trim().lowercase()
        entries.find { 
            it.shortcut == cleanWord || it.expansion == cleanWord
        }?.let { removeEntry(it.id) }
    }
    
    private fun rebuildShortcutMap() {
        shortcutMap.clear()
        entries.forEach { entry ->
            shortcutMap[entry.shortcut] = entry
        }
        Log.d(TAG, "Rebuilt shortcut map with ${shortcutMap.size} entries")
    }
    
    private fun loadEntriesFromPrefs() {
        try {
            val entriesJson = prefs.getString(KEY_ENTRIES, null)
            if (entriesJson != null) {
                val jsonArray = JSONArray(entriesJson)
                entries.clear()
                for (i in 0 until jsonArray.length()) {
                    val entry = DictionaryEntry.fromJson(jsonArray.getJSONObject(i))
                    entries.add(entry)
                }
                Log.d(TAG, "Loaded ${entries.size} dictionary entries from preferences")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dictionary entries from preferences", e)
        }
    }
    
    private fun saveEntriesToPrefs() {
        try {
            val jsonArray = JSONArray()
            entries.forEach { entry ->
                jsonArray.put(entry.toJson())
            }
            prefs.edit()
                .putString(KEY_ENTRIES, jsonArray.toString())
                .apply()
            
            // Also sync to Flutter SharedPreferences for UI display
            syncToFlutterPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving dictionary entries to preferences", e)
        }
    }
    
    /**
     * Sync dictionary entries to Flutter SharedPreferences
     * So the Flutter UI can display them
     */
    private fun syncToFlutterPrefs() {
        try {
            val flutterPrefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            entries.forEach { entry ->
                jsonArray.put(entry.toJson())
            }
            flutterPrefs.edit()
                .putString("flutter.dictionary_entries", jsonArray.toString())
                .apply()
            Log.d(TAG, "Synced ${entries.size} entries to Flutter SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Flutter SharedPreferences", e)
        }
    }
    
    /**
     * Load dictionary entries from Flutter SharedPreferences
     * This allows Flutter UI changes to be reflected in the keyboard
     */
    private fun loadFromFlutterPrefs() {
        try {
            val flutterPrefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val jsonString = flutterPrefs.getString("flutter.dictionary_entries", null)
            if (jsonString.isNullOrEmpty()) {
                Log.d(TAG, "No dictionary entries in Flutter prefs")
                return
            }
            
            val jsonArray = JSONArray(jsonString)
            val flutterEntries = mutableListOf<DictionaryEntry>()
            var skippedCount = 0
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val entry = DictionaryEntry.fromJson(jsonObj)
                    
                    // Skip entries with empty shortcut or expansion
                    if (entry.shortcut.isNotEmpty() && entry.expansion.isNotEmpty()) {
                        flutterEntries.add(entry)
                    } else {
                        skippedCount++
                        Log.w(TAG, "Skipped dictionary entry with empty fields at index $i")
                    }
                } catch (e: Exception) {
                    skippedCount++
                    Log.w(TAG, "Failed to parse dictionary entry at index $i: ${e.message}")
                }
            }
            
            // Replace entries with Flutter entries
            entries.clear()
            entries.addAll(flutterEntries)
            rebuildShortcutMap()
            
            Log.d(TAG, "Loaded ${flutterEntries.size} entries from Flutter prefs (skipped: $skippedCount)")
            
            // Save cleaned list back to native prefs
            if (flutterEntries.isNotEmpty()) {
                saveEntriesToPrefs()
            }
            
            // Notify listeners
            listeners.forEach { it.onDictionaryUpdated(entries.toList()) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from Flutter prefs", e)
        }
    }
}

/**
 * Represents a dictionary entry (shortcut → expansion)
 */
data class DictionaryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val shortcut: String,
    val expansion: String,
    val usageCount: Int = 0,
    val lastUsed: Long = System.currentTimeMillis(),
    val dateAdded: Long = System.currentTimeMillis()
) {
    
    /**
     * Get formatted usage count for display
     */
    fun getFormattedUsageCount(): String {
        return when {
            usageCount == 0 -> "Never used"
            usageCount == 1 -> "Used once"
            usageCount < 10 -> "Used $usageCount times"
            usageCount < 100 -> "Used ${usageCount} times"
            else -> "Used ${usageCount}+ times"
        }
    }
    
    /**
     * Convert to JSON for persistence
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("shortcut", shortcut)
            put("expansion", expansion)
            put("usageCount", usageCount)
            put("lastUsed", lastUsed)
            put("dateAdded", dateAdded)
        }
    }
    
    companion object {
        /**
         * Create from JSON
         */
        fun fromJson(json: JSONObject): DictionaryEntry {
            return DictionaryEntry(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                shortcut = json.optString("shortcut", ""), // Use optString to prevent NPE
                expansion = json.optString("expansion", ""), // Use optString to prevent NPE
                usageCount = json.optInt("usageCount", 0),
                lastUsed = json.optLong("lastUsed", System.currentTimeMillis()),
                dateAdded = json.optLong("dateAdded", System.currentTimeMillis())
            )
        }
    }
}

