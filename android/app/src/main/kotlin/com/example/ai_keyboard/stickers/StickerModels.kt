package com.example.ai_keyboard.stickers

import com.google.firebase.firestore.DocumentSnapshot
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sticker Pack data model - compatible with both JSON cache and Firestore
 */
data class StickerPack(
    val id: String,
    val name: String,
    val author: String,
    val thumbnailUrl: String,
    val category: String,
    val version: String = "1.0",
    val stickerCount: Int = 0,
    val isInstalled: Boolean = false,
    val installProgress: Int = 0,
    val description: String = "",
    val featured: Boolean = false,
    val tags: List<String> = emptyList()
) {
    
    /**
     * Convert to JSON for local caching
     */
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("author", author)
        put("thumbnailUrl", thumbnailUrl)
        put("category", category)
        put("version", version)
        put("stickerCount", stickerCount)
        put("isInstalled", isInstalled)
        put("installProgress", installProgress)
        put("description", description)
        put("featured", featured)
        put("tags", JSONArray(tags))
    }

    companion object {
        /**
         * Create from JSON cache
         */
        fun fromJson(obj: JSONObject): StickerPack {
            val tags = obj.optJSONArray("tags")?.let { arr -> 
                (0 until arr.length()).map { arr.getString(it) } 
            } ?: emptyList()
            
            return StickerPack(
                id = obj.getString("id"),
                name = obj.getString("name"),
                author = obj.optString("author", "Unknown"),
                thumbnailUrl = obj.getString("thumbnailUrl"),
                category = obj.optString("category", "general"),
                version = obj.optString("version", "1.0"),
                stickerCount = obj.optInt("stickerCount", 0),
                isInstalled = obj.optBoolean("isInstalled", false),
                installProgress = obj.optInt("installProgress", 0),
                description = obj.optString("description", ""),
                featured = obj.optBoolean("featured", false),
                tags = tags
            )
        }

        /**
         * Create from Firestore document
         */
        fun fromFirestore(doc: DocumentSnapshot): StickerPack {
            @Suppress("UNCHECKED_CAST")
            val tags = (doc.get("tags") as? List<String>) ?: emptyList()
            
            return StickerPack(
                id = doc.id,
                name = doc.getString("name") ?: "",
                author = doc.getString("author") ?: "Unknown",
                thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                category = doc.getString("category") ?: "general",
                version = doc.getString("version") ?: "1.0",
                stickerCount = (doc.get("stickerCount") as? Long)?.toInt() ?: 0,
                isInstalled = doc.getBoolean("isInstalled") ?: false,
                installProgress = (doc.get("installProgress") as? Long)?.toInt() ?: 0,
                description = doc.getString("description") ?: "",
                featured = doc.getBoolean("featured") ?: false,
                tags = tags
            )
        }

        /**
         * Convert from legacy assets format
         */
        fun fromLegacyJson(obj: JSONObject): StickerPack {
            val stickersArray = obj.optJSONArray("stickers")
            val stickerCount = stickersArray?.length() ?: 0
            
            return StickerPack(
                id = obj.getString("id"),
                name = obj.getString("name"),
                author = obj.optString("author", "AI Keyboard Team"),
                thumbnailUrl = obj.optString("thumbnail", obj.optString("thumbnailUrl", "")),
                category = obj.optString("category", "general"),
                version = obj.optString("version", "1.0"),
                stickerCount = stickerCount
            )
        }
    }
}

/**
 * Individual Sticker data model - compatible with both JSON cache and Firestore  
 */
data class StickerData(
    val id: String,
    val packId: String,
    val imageUrl: String,
    val tags: List<String> = emptyList(),
    val emojis: List<String> = emptyList(),
    val localPath: String? = null,
    val isDownloaded: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long = 0L,
    val fileSize: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
) {

    /**
     * Convert to JSON for local caching
     */
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("packId", packId)
        put("imageUrl", imageUrl)
        put("tags", JSONArray(tags))
        put("emojis", JSONArray(emojis))
        put("localPath", localPath ?: "")
        put("isDownloaded", isDownloaded)
        put("usageCount", usageCount)
        put("lastUsed", lastUsed)
        put("fileSize", fileSize)
        put("width", width)
        put("height", height)
    }

    companion object {
        /**
         * Create from JSON cache
         */
        fun fromJson(obj: JSONObject): StickerData {
            val tags = obj.optJSONArray("tags")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            
            val emojis = obj.optJSONArray("emojis")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()

            return StickerData(
                id = obj.getString("id"),
                packId = obj.getString("packId"),
                imageUrl = obj.getString("imageUrl"),
                tags = tags,
                emojis = emojis,
                localPath = obj.optString("localPath").takeIf { it.isNotEmpty() },
                isDownloaded = obj.optBoolean("isDownloaded", false),
                usageCount = obj.optInt("usageCount", 0),
                lastUsed = obj.optLong("lastUsed", 0L),
                fileSize = obj.optLong("fileSize", 0L),
                width = obj.optInt("width", 0),
                height = obj.optInt("height", 0)
            )
        }

        /**
         * Create from Firestore document
         */
        fun fromFirestore(doc: DocumentSnapshot, packId: String): StickerData {
            @Suppress("UNCHECKED_CAST")
            val tags = (doc.get("tags") as? List<String>) ?: emptyList()
            @Suppress("UNCHECKED_CAST") 
            val emojis = (doc.get("emojis") as? List<String>) ?: emptyList()

            return StickerData(
                id = doc.id,
                packId = packId,
                imageUrl = doc.getString("imageUrl") ?: "",
                tags = tags,
                emojis = emojis,
                localPath = doc.getString("localPath"),
                isDownloaded = doc.getBoolean("isDownloaded") ?: false,
                usageCount = (doc.get("usageCount") as? Long)?.toInt() ?: 0,
                lastUsed = doc.get("lastUsed") as? Long ?: 0L,
                fileSize = doc.get("fileSize") as? Long ?: 0L,
                width = (doc.get("width") as? Long)?.toInt() ?: 0,
                height = (doc.get("height") as? Long)?.toInt() ?: 0
            )
        }

        /**
         * Convert from legacy assets format
         */
        fun fromLegacyJson(obj: JSONObject, packId: String): StickerData {
            val tags = obj.optJSONArray("tags")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            
            val emojis = obj.optJSONArray("emojis")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()

            // Handle legacy 'file' field vs 'imageUrl'
            val imageUrl = obj.optString("imageUrl").takeIf { it.isNotEmpty() }
                ?: obj.optString("file", "")

            return StickerData(
                id = obj.getString("id"),
                packId = packId,
                imageUrl = imageUrl,
                tags = tags,
                emojis = emojis
            )
        }
    }
}

/**
 * Sticker search result with relevance scoring
 */
data class StickerSearchResult(
    val sticker: StickerData,
    val relevanceScore: Float,
    val matchType: MatchType
)

enum class MatchType {
    EXACT_TAG,
    PARTIAL_TAG,
    EMOJI,
    ID_MATCH
}

/**
 * Sticker pack category for organization
 */
enum class StickerCategory(val displayName: String) {
    ANIMALS("Animals"),
    EMOTIONS("Emotions"), 
    BUSINESS("Business"),
    FOOD("Food"),
    SPORTS("Sports"),
    TRAVEL("Travel"),
    TECHNOLOGY("Technology"),
    GENERAL("General"),
    FEATURED("Featured"),
    RECENT("Recently Used");
    
    companion object {
        fun fromString(category: String): StickerCategory {
            return values().find { 
                it.name.equals(category, ignoreCase = true) || 
                it.displayName.equals(category, ignoreCase = true)
            } ?: GENERAL
        }
    }
}
