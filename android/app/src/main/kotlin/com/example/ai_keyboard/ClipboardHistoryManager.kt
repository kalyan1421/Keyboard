package com.example.ai_keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages clipboard history with automatic cleanup, persistence, and template support
 */
class ClipboardHistoryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ClipboardHistoryManager"
        private const val PREFS_NAME = "clipboard_history"
        private const val KEY_HISTORY = "history_items"
        private const val KEY_TEMPLATES = "template_items"
        private const val KEY_MAX_HISTORY_SIZE = "max_history_size"
        private const val KEY_AUTO_EXPIRY_ENABLED = "auto_expiry_enabled"
        private const val KEY_EXPIRY_DURATION_MINUTES = "expiry_duration_minutes"
        
        private const val DEFAULT_MAX_HISTORY_SIZE = 20
        private const val DEFAULT_EXPIRY_DURATION_MINUTES = 60L
    }
    
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Thread-safe lists for concurrent access
    private val historyItems = CopyOnWriteArrayList<ClipboardItem>()
    private val templateItems = CopyOnWriteArrayList<ClipboardItem>()
    
    // Settings
    private var maxHistorySize = DEFAULT_MAX_HISTORY_SIZE
    private var autoExpiryEnabled = true
    private var expiryDurationMinutes = DEFAULT_EXPIRY_DURATION_MINUTES
    
    // Listeners for history changes
    private val listeners = mutableListOf<ClipboardHistoryListener>()
    
    interface ClipboardHistoryListener {
        fun onHistoryUpdated(items: List<ClipboardItem>)
        fun onNewClipboardItem(item: ClipboardItem)
    }
    
    // Clipboard change listener
    private val clipboardChangeListener = ClipboardManager.OnPrimaryClipChangedListener {
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank()) {
                    addClipboardItem(text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling clipboard change", e)
        }
    }
    
    /**
     * Initialize the clipboard manager
     */
    fun initialize() {
        Log.d(TAG, "Initializing ClipboardHistoryManager")
        
        // Load settings
        loadSettings()
        
        // Load persisted data
        loadHistoryFromPrefs()
        loadTemplatesFromPrefs()
        
        // Register clipboard listener
        clipboardManager.addPrimaryClipChangedListener(clipboardChangeListener)
        
        // Perform initial cleanup
        cleanupExpiredItems()
        
        Log.d(TAG, "ClipboardHistoryManager initialized with ${historyItems.size} history items and ${templateItems.size} templates")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            clipboardManager.removePrimaryClipChangedListener(clipboardChangeListener)
            listeners.clear()
            Log.d(TAG, "ClipboardHistoryManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * Add a new clipboard item
     */
    private fun addClipboardItem(text: String) {
        try {
            val trimmedText = text.trim()
            if (trimmedText.isEmpty()) return
            
            // Don't add if it's the same as the most recent item
            if (historyItems.isNotEmpty() && historyItems[0].text == trimmedText) {
                Log.d(TAG, "Skipping duplicate clipboard item")
                return
            }
            
            val newItem = ClipboardItem(text = trimmedText)
            
            // Add to beginning of history
            historyItems.add(0, newItem)
            
            // Enforce max size
            while (historyItems.size > maxHistorySize) {
                val removed = historyItems.removeAt(historyItems.size - 1)
                Log.d(TAG, "Removed old clipboard item: ${removed.getPreview()}")
            }
            
            // Save to preferences
            saveHistoryToPrefs()
            
            // Notify listeners
            notifyHistoryUpdated()
            notifyNewItem(newItem)
            
            Log.d(TAG, "Added clipboard item: ${newItem.getPreview()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding clipboard item", e)
        }
    }
    
    /**
     * Get all clipboard items (history + templates)
     */
    fun getAllItems(): List<ClipboardItem> {
        cleanupExpiredItems()
        return (templateItems + historyItems).distinctBy { it.text }
    }
    
    /**
     * Get only history items
     */
    fun getHistoryItems(): List<ClipboardItem> {
        cleanupExpiredItems()
        return historyItems.toList()
    }
    
    /**
     * Get only template items
     */
    fun getTemplateItems(): List<ClipboardItem> {
        return templateItems.toList()
    }
    
    /**
     * Get history items for UI display (templates + recent history)
     */
    fun getHistoryForUI(maxItems: Int): List<ClipboardItem> {
        cleanupExpiredItems()
        
        // Combine templates (always first) with recent history
        val allItems = mutableListOf<ClipboardItem>()
        
        // Add templates first (they're always pinned)
        allItems.addAll(templateItems)
        
        // Add history items, excluding duplicates
        val templateTexts = templateItems.map { it.text }.toSet()
        val uniqueHistoryItems = historyItems.filter { it.text !in templateTexts }
        
        allItems.addAll(uniqueHistoryItems)
        
        // Return up to maxItems
        return allItems.take(maxItems)
    }
    
    /**
     * Get the most recent clipboard item for suggestion bar
     */
    fun getMostRecentItem(): ClipboardItem? {
        cleanupExpiredItems()
        return historyItems.firstOrNull()
    }
    
    /**
     * Get OTP items (prioritized for suggestions)
     */
    fun getOTPItems(): List<ClipboardItem> {
        cleanupExpiredItems()
        return historyItems.filter { it.isOTP() }
    }
    
    /**
     * Pin/unpin an item
     */
    fun togglePin(itemId: String): Boolean {
        val item = historyItems.find { it.id == itemId }
        if (item != null) {
            val updatedItem = item.copy(isPinned = !item.isPinned)
            val index = historyItems.indexOf(item)
            historyItems[index] = updatedItem
            saveHistoryToPrefs()
            notifyHistoryUpdated()
            Log.d(TAG, "Toggled pin for item: ${updatedItem.getPreview()} -> pinned: ${updatedItem.isPinned}")
            return updatedItem.isPinned
        }
        return false
    }
    
    /**
     * Delete an item
     */
    fun deleteItem(itemId: String): Boolean {
        val historyRemoved = historyItems.removeIf { it.id == itemId }
        val templateRemoved = templateItems.removeIf { it.id == itemId }
        
        if (historyRemoved || templateRemoved) {
            if (historyRemoved) saveHistoryToPrefs()
            if (templateRemoved) saveTemplatesToPrefs()
            notifyHistoryUpdated()
            Log.d(TAG, "Deleted clipboard item: $itemId")
            return true
        }
        return false
    }
    
    /**
     * Add a template item
     */
    fun addTemplate(text: String, category: String? = null): ClipboardItem {
        val template = ClipboardItem.createTemplate(text, category)
        templateItems.add(template)
        saveTemplatesToPrefs()
        notifyHistoryUpdated()
        Log.d(TAG, "Added template: ${template.getPreview()}")
        return template
    }
    
    /**
     * Update templates from Flutter app
     */
    fun updateTemplates(templates: List<ClipboardItem>) {
        templateItems.clear()
        templateItems.addAll(templates.filter { it.isTemplate })
        saveTemplatesToPrefs()
        notifyHistoryUpdated()
        Log.d(TAG, "Updated templates: ${templateItems.size} items")
    }
    
    /**
     * Update settings
     */
    fun updateSettings(
        maxHistorySize: Int = this.maxHistorySize,
        autoExpiryEnabled: Boolean = this.autoExpiryEnabled,
        expiryDurationMinutes: Long = this.expiryDurationMinutes
    ) {
        this.maxHistorySize = maxHistorySize
        this.autoExpiryEnabled = autoExpiryEnabled
        this.expiryDurationMinutes = expiryDurationMinutes
        
        saveSettings()
        
        // Reload items from Flutter prefs in case they were changed there
        loadFromFlutterPrefs()
        
        // Cleanup with new settings
        cleanupExpiredItems()
        
        // Enforce new max size
        while (historyItems.size > maxHistorySize) {
            historyItems.removeAt(historyItems.size - 1)
        }
        
        if (historyItems.size != getHistoryItems().size) {
            saveHistoryToPrefs()
            notifyHistoryUpdated()
        }
        
        Log.d(TAG, "Updated settings: maxSize=$maxHistorySize, autoExpiry=$autoExpiryEnabled, expiryMinutes=$expiryDurationMinutes")
    }
    
    /**
     * Clean up expired items
     */
    private fun cleanupExpiredItems() {
        if (!autoExpiryEnabled) return
        
        val expiryDurationMs = expiryDurationMinutes * 60 * 1000
        val initialSize = historyItems.size
        
        historyItems.removeIf { it.isExpired(expiryDurationMs) }
        
        if (historyItems.size != initialSize) {
            saveHistoryToPrefs()
            notifyHistoryUpdated()
            Log.d(TAG, "Cleaned up ${initialSize - historyItems.size} expired items")
        }
    }
    
    /**
     * Add listener for history changes
     */
    fun addListener(listener: ClipboardHistoryListener) {
        listeners.add(listener)
    }
    
    /**
     * Remove listener
     */
    fun removeListener(listener: ClipboardHistoryListener) {
        listeners.remove(listener)
    }
    
    private fun notifyHistoryUpdated() {
        listeners.forEach { it.onHistoryUpdated(getAllItems()) }
    }
    
    private fun notifyNewItem(item: ClipboardItem) {
        listeners.forEach { it.onNewClipboardItem(item) }
    }
    
    private fun loadSettings() {
        maxHistorySize = prefs.getInt(KEY_MAX_HISTORY_SIZE, DEFAULT_MAX_HISTORY_SIZE)
        autoExpiryEnabled = prefs.getBoolean(KEY_AUTO_EXPIRY_ENABLED, true)
        expiryDurationMinutes = prefs.getLong(KEY_EXPIRY_DURATION_MINUTES, DEFAULT_EXPIRY_DURATION_MINUTES)
    }
    
    private fun saveSettings() {
        prefs.edit()
            .putInt(KEY_MAX_HISTORY_SIZE, maxHistorySize)
            .putBoolean(KEY_AUTO_EXPIRY_ENABLED, autoExpiryEnabled)
            .putLong(KEY_EXPIRY_DURATION_MINUTES, expiryDurationMinutes)
            .commit() // Use commit() for immediate persistence
    }
    
    private fun loadHistoryFromPrefs() {
        try {
            val historyJson = prefs.getString(KEY_HISTORY, null)
            if (historyJson != null) {
                val jsonArray = JSONArray(historyJson)
                historyItems.clear()
                for (i in 0 until jsonArray.length()) {
                    val item = ClipboardItem.fromJson(jsonArray.getJSONObject(i))
                    historyItems.add(item)
                }
                Log.d(TAG, "Loaded ${historyItems.size} history items from preferences")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading history from preferences", e)
        }
    }
    
    private fun saveHistoryToPrefs() {
        try {
            val jsonArray = JSONArray()
            historyItems.forEach { item ->
                jsonArray.put(item.toJson())
            }
            prefs.edit()
                .putString(KEY_HISTORY, jsonArray.toString())
                .commit() // Use commit() for immediate persistence
            
            // Also save to Flutter SharedPreferences for UI display
            syncToFlutterPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving history to preferences", e)
        }
    }
    
    /**
     * Sync clipboard items to Flutter SharedPreferences
     * So the Flutter UI can display them
     */
    private fun syncToFlutterPrefs() {
        try {
            val flutterPrefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            historyItems.forEach { item ->
                jsonArray.put(item.toJson())
            }
            flutterPrefs.edit()
                .putString("flutter.clipboard_items", jsonArray.toString())
                .commit()
            Log.d(TAG, "Synced ${historyItems.size} items to Flutter SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Flutter SharedPreferences", e)
        }
    }
    
    /**
     * Load clipboard items from Flutter SharedPreferences
     * This allows Flutter UI changes to be reflected in the keyboard
     */
    private fun loadFromFlutterPrefs() {
        try {
            val flutterPrefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val itemsJson = flutterPrefs.getString("flutter.clipboard_items", null)
            if (itemsJson.isNullOrEmpty()) {
                Log.d(TAG, "No clipboard items in Flutter prefs")
                return
            }
            
            val jsonArray = JSONArray(itemsJson)
            // Merge with existing items, preserving items not in Flutter prefs
            val flutterItems = mutableListOf<ClipboardItem>()
            var skippedCount = 0
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val item = ClipboardItem.fromJson(jsonObj)
                    
                    // Skip items with empty text
                    if (item.text.isNotEmpty()) {
                        flutterItems.add(item)
                    } else {
                        skippedCount++
                        Log.w(TAG, "Skipped clipboard item with empty text at index $i")
                    }
                } catch (e: Exception) {
                    skippedCount++
                    Log.w(TAG, "Failed to parse clipboard item at index $i: ${e.message}")
                }
            }
            
            // Update existing items or add new ones
            flutterItems.forEach { flutterItem ->
                val existingIndex = historyItems.indexOfFirst { it.id == flutterItem.id }
                if (existingIndex >= 0) {
                    historyItems[existingIndex] = flutterItem
                } else {
                    historyItems.add(flutterItem)
                }
            }
            
            Log.d(TAG, "Loaded ${flutterItems.size} items from Flutter SharedPreferences (skipped: $skippedCount)")
            
            // Save cleaned list back to prefs
            if (flutterItems.isNotEmpty()) {
                saveHistoryToPrefs()
            }
            
            notifyHistoryUpdated()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from Flutter SharedPreferences", e)
        }
    }
    
    private fun loadTemplatesFromPrefs() {
        try {
            val templatesJson = prefs.getString(KEY_TEMPLATES, null)
            if (templatesJson != null) {
                val jsonArray = JSONArray(templatesJson)
                templateItems.clear()
                for (i in 0 until jsonArray.length()) {
                    val item = ClipboardItem.fromJson(jsonArray.getJSONObject(i))
                    templateItems.add(item)
                }
                Log.d(TAG, "Loaded ${templateItems.size} template items from preferences")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading templates from preferences", e)
        }
    }
    
    private fun saveTemplatesToPrefs() {
        try {
            val jsonArray = JSONArray()
            templateItems.forEach { item ->
                jsonArray.put(item.toJson())
            }
            prefs.edit()
                .putString(KEY_TEMPLATES, jsonArray.toString())
                .commit() // Use commit() for immediate persistence
        } catch (e: Exception) {
            Log.e(TAG, "Error saving templates to preferences", e)
        }
    }
}
