package com.example.ai_keyboard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Custom Tone Manager for creating and managing user-defined tone templates
 */
class CustomToneManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CustomToneManager"
        private const val PREFS_NAME = "custom_tones"
        private const val KEY_CUSTOM_TONES = "custom_tones_json"
        private const val MAX_CUSTOM_TONES = 10
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Custom Tone data class
     */
    data class CustomTone(
        val id: String,
        val name: String,
        val icon: String,
        val description: String,
        val systemPrompt: String,
        val color: String,
        val createdAt: Long,
        val usageCount: Int = 0
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("icon", icon)
                put("description", description)
                put("systemPrompt", systemPrompt)
                put("color", color)
                put("createdAt", createdAt)
                put("usageCount", usageCount)
            }
        }
        
        companion object {
            fun fromJson(json: JSONObject): CustomTone {
                return CustomTone(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    icon = json.getString("icon"),
                    description = json.getString("description"),
                    systemPrompt = json.getString("systemPrompt"),
                    color = json.getString("color"),
                    createdAt = json.getLong("createdAt"),
                    usageCount = json.optInt("usageCount", 0)
                )
            }
        }
    }
    
    /**
     * Predefined tone templates for quick creation
     */
    enum class ToneTemplate(
        val displayName: String,
        val icon: String,
        val description: String,
        val promptTemplate: String,
        val color: String
    ) {
        MOTIVATIONAL(
            "Motivational",
            "💪",
            "Inspire and energize the reader",
            "Rewrite this text to be highly motivational and inspiring. Use energetic language that empowers and encourages action.",
            "#ff6d00"
        ),
        DIPLOMATIC(
            "Diplomatic",
            "🤝",
            "Tactful and politically correct",
            "Rewrite this text to be diplomatic and tactful. Use careful language that avoids offense while still conveying the message clearly.",
            "#4caf50"
        ),
        ACADEMIC(
            "Academic",
            "🎓",
            "Scholarly and research-oriented",
            "Rewrite this text in an academic style suitable for scholarly papers. Use formal language, precise terminology, and objective tone.",
            "#3f51b5"
        ),
        SALES(
            "Sales",
            "💼",
            "Persuasive and conversion-focused",
            "Rewrite this text to be persuasive and sales-oriented. Focus on benefits, create urgency, and encourage action.",
            "#f44336"
        ),
        STORYTELLING(
            "Storytelling",
            "📚",
            "Narrative and engaging",
            "Rewrite this text in a storytelling format. Make it engaging, narrative-driven, and emotionally compelling.",
            "#9c27b0"
        ),
        TECHNICAL(
            "Technical",
            "⚙️",
            "Precise and specification-focused",
            "Rewrite this text in a technical style. Use precise terminology, clear specifications, and structured information.",
            "#607d8b"
        ),
        FRIENDLY(
            "Friendly",
            "😊",
            "Warm and approachable",
            "Rewrite this text to be very friendly and warm. Use welcoming language that makes the reader feel comfortable and valued.",
            "#ff9800"
        ),
        URGENT(
            "Urgent",
            "⚡",
            "Time-sensitive and action-oriented",
            "Rewrite this text to convey urgency. Use language that emphasizes time sensitivity and the need for immediate action.",
            "#e91e63"
        )
    }
    
    /**
     * Get all custom tones
     */
    fun getCustomTones(): List<CustomTone> {
        return try {
            val jsonString = prefs.getString(KEY_CUSTOM_TONES, "[]")
            val jsonArray = JSONArray(jsonString!!)
            
            val customTones = mutableListOf<CustomTone>()
            for (i in 0 until jsonArray.length()) {
                val toneJson = jsonArray.getJSONObject(i)
                customTones.add(CustomTone.fromJson(toneJson))
            }
            
            // Sort by usage count (most used first), then by creation date
            customTones.sortedWith(compareByDescending<CustomTone> { it.usageCount }.thenByDescending { it.createdAt })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom tones", e)
            emptyList()
        }
    }
    
    /**
     * Save a custom tone
     */
    fun saveCustomTone(customTone: CustomTone): Boolean {
        return try {
            val currentTones = getCustomTones().toMutableList()
            
            // Check if we're at the limit
            if (currentTones.size >= MAX_CUSTOM_TONES) {
                // Remove the least used tone
                val leastUsed = currentTones.minByOrNull { it.usageCount }
                leastUsed?.let { currentTones.remove(it) }
            }
            
            // Add new tone
            currentTones.add(customTone)
            
            // Save to preferences
            val jsonArray = JSONArray()
            currentTones.forEach { tone ->
                jsonArray.put(tone.toJson())
            }
            
            prefs.edit()
                .putString(KEY_CUSTOM_TONES, jsonArray.toString())
                .apply()
            
            Log.d(TAG, "Custom tone saved: ${customTone.name}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom tone", e)
            false
        }
    }
    
    /**
     * Update custom tone usage count
     */
    fun incrementUsage(toneId: String) {
        try {
            val currentTones = getCustomTones().toMutableList()
            val toneIndex = currentTones.indexOfFirst { it.id == toneId }
            
            if (toneIndex >= 0) {
                val updatedTone = currentTones[toneIndex].copy(
                    usageCount = currentTones[toneIndex].usageCount + 1
                )
                currentTones[toneIndex] = updatedTone
                
                // Save updated list
                val jsonArray = JSONArray()
                currentTones.forEach { tone ->
                    jsonArray.put(tone.toJson())
                }
                
                prefs.edit()
                    .putString(KEY_CUSTOM_TONES, jsonArray.toString())
                    .apply()
                
                Log.d(TAG, "Incremented usage for tone: ${updatedTone.name}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing tone usage", e)
        }
    }
    
    /**
     * Delete a custom tone
     */
    fun deleteCustomTone(toneId: String): Boolean {
        return try {
            val currentTones = getCustomTones().toMutableList()
            val toneToRemove = currentTones.find { it.id == toneId }
            
            if (toneToRemove != null) {
                currentTones.remove(toneToRemove)
                
                // Save updated list
                val jsonArray = JSONArray()
                currentTones.forEach { tone ->
                    jsonArray.put(tone.toJson())
                }
                
                prefs.edit()
                    .putString(KEY_CUSTOM_TONES, jsonArray.toString())
                    .apply()
                
                Log.d(TAG, "Deleted custom tone: ${toneToRemove.name}")
                true
            } else {
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting custom tone", e)
            false
        }
    }
    
    /**
     * Create custom tone from template
     */
    fun createFromTemplate(
        template: ToneTemplate,
        customName: String? = null,
        customPrompt: String? = null
    ): CustomTone {
        val id = generateToneId()
        val name = customName ?: template.displayName
        val prompt = customPrompt ?: template.promptTemplate
        
        return CustomTone(
            id = id,
            name = name,
            icon = template.icon,
            description = template.description,
            systemPrompt = prompt,
            color = template.color,
            createdAt = System.currentTimeMillis(),
            usageCount = 0
        )
    }
    
    /**
     * Create completely custom tone
     */
    fun createCustomTone(
        name: String,
        icon: String,
        description: String,
        systemPrompt: String,
        color: String
    ): CustomTone {
        val id = generateToneId()
        
        return CustomTone(
            id = id,
            name = name,
            icon = icon,
            description = description,
            systemPrompt = systemPrompt,
            color = color,
            createdAt = System.currentTimeMillis(),
            usageCount = 0
        )
    }
    
    /**
     * Get available tone templates
     */
    fun getAvailableTemplates(): List<ToneTemplate> = ToneTemplate.values().toList()
    
    /**
     * Validate custom tone data
     */
    fun validateCustomTone(
        name: String,
        systemPrompt: String
    ): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Name cannot be empty")
            name.length > 20 -> ValidationResult(false, "Name must be 20 characters or less")
            systemPrompt.isBlank() -> ValidationResult(false, "System prompt cannot be empty")
            systemPrompt.length < 10 -> ValidationResult(false, "System prompt must be at least 10 characters")
            systemPrompt.length > 500 -> ValidationResult(false, "System prompt must be 500 characters or less")
            getCustomTones().any { it.name.equals(name, ignoreCase = true) } -> 
                ValidationResult(false, "A tone with this name already exists")
            else -> ValidationResult(true, "Valid")
        }
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
    
    /**
     * Generate unique tone ID
     */
    private fun generateToneId(): String {
        return "custom_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Get statistics about custom tones
     */
    fun getStatistics(): ToneStatistics {
        val customTones = getCustomTones()
        return ToneStatistics(
            totalCustomTones = customTones.size,
            totalUsage = customTones.sumOf { it.usageCount },
            mostUsedTone = customTones.maxByOrNull { it.usageCount },
            newestTone = customTones.maxByOrNull { it.createdAt },
            availableSlots = MAX_CUSTOM_TONES - customTones.size
        )
    }
    
    data class ToneStatistics(
        val totalCustomTones: Int,
        val totalUsage: Int,
        val mostUsedTone: CustomTone?,
        val newestTone: CustomTone?,
        val availableSlots: Int
    )
    
    /**
     * Export custom tones to JSON string
     */
    fun exportToJson(): String {
        val customTones = getCustomTones()
        val jsonArray = JSONArray()
        
        customTones.forEach { tone ->
            jsonArray.put(tone.toJson())
        }
        
        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("customTones", jsonArray)
        }.toString(2)
    }
    
    /**
     * Import custom tones from JSON string
     */
    fun importFromJson(jsonString: String): ImportResult {
        return try {
            val json = JSONObject(jsonString)
            val customTonesArray = json.getJSONArray("customTones")
            
            val importedTones = mutableListOf<CustomTone>()
            for (i in 0 until customTonesArray.length()) {
                val toneJson = customTonesArray.getJSONObject(i)
                importedTones.add(CustomTone.fromJson(toneJson))
            }
            
            // Save imported tones (this will replace existing ones)
            val jsonArray = JSONArray()
            importedTones.forEach { tone ->
                jsonArray.put(tone.toJson())
            }
            
            prefs.edit()
                .putString(KEY_CUSTOM_TONES, jsonArray.toString())
                .apply()
            
            ImportResult(true, "Successfully imported ${importedTones.size} custom tones")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing custom tones", e)
            ImportResult(false, "Import failed: ${e.message}")
        }
    }
    
    data class ImportResult(
        val success: Boolean,
        val message: String
    )
}
