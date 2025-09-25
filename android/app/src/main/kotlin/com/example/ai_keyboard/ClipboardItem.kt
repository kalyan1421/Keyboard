package com.example.ai_keyboard

import org.json.JSONObject
import java.util.UUID

/**
 * Represents a clipboard history item with metadata
 */
data class ClipboardItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isTemplate: Boolean = false, // For user-defined templates
    val category: String? = null // Optional category for templates
) {
    
    /**
     * Check if this item is an OTP (4-8 digit numeric code)
     */
    fun isOTP(): Boolean {
        return text.matches(Regex("\\b\\d{4,8}\\b"))
    }
    
    /**
     * Check if this item has expired based on given expiry duration
     */
    fun isExpired(expiryDurationMs: Long): Boolean {
        if (isPinned || isTemplate) return false // Pinned items and templates never expire
        return System.currentTimeMillis() - timestamp > expiryDurationMs
    }
    
    /**
     * Get a shortened preview of the text for display
     */
    fun getPreview(maxLength: Int = 50): String {
        return if (text.length <= maxLength) {
            text
        } else {
            "${text.take(maxLength - 3)}..."
        }
    }
    
    /**
     * Get formatted timestamp for display
     */
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now" // Less than 1 minute
            diff < 3600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
            diff < 86400_000 -> "${diff / 3600_000}h ago" // Less than 1 day
            else -> "${diff / 86400_000}d ago" // Days ago
        }
    }
    
    /**
     * Convert to JSON for storage
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("timestamp", timestamp)
            put("isPinned", isPinned)
            put("isTemplate", isTemplate)
            put("category", category)
        }
    }
    
    companion object {
        /**
         * Create from JSON
         */
        fun fromJson(json: JSONObject): ClipboardItem {
            return ClipboardItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                text = json.getString("text"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                isPinned = json.optBoolean("isPinned", false),
                isTemplate = json.optBoolean("isTemplate", false),
                category = json.optString("category", null).takeIf { it.isNotEmpty() }
            )
        }
        
        /**
         * Create a template item
         */
        fun createTemplate(text: String, category: String? = null): ClipboardItem {
            return ClipboardItem(
                text = text,
                isPinned = true,
                isTemplate = true,
                category = category
            )
        }
    }
}
