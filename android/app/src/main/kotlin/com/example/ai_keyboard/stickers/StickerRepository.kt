package com.example.ai_keyboard.stickers

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import com.google.android.gms.tasks.Tasks

/**
 * StickerRepository — Firebase + JSON Cache replacement for StickerDatabase
 * 
 * This implementation provides:
 * - Firebase Firestore for cloud synchronization
 * - Local JSON cache for fast offline access
 * - Firebase Storage for sticker image files
 * - Real-time updates and multi-device sync
 */
class StickerRepository(private val context: Context) {

    companion object {
        private const val TAG = "StickerRepository"
        private const val CACHE_DIR_NAME = "stickers_cache"
        private const val PACKS_CACHE_FILE = "packs.json"
        private const val COLLECTION_STICKER_PACKS = "sticker_packs"
        private const val COLLECTION_STICKERS = "stickers"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val cacheDir = File(context.filesDir, CACHE_DIR_NAME)

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
            Log.d(TAG, "Created sticker cache directory")
        }
    }

    // -----------------------------------------------------
    // 1️⃣ Get all sticker packs (cached + Firebase sync)
    // -----------------------------------------------------
    suspend fun getStickerPacks(forceRefresh: Boolean = false): List<StickerPack> {
        val cacheFile = File(cacheDir, PACKS_CACHE_FILE)

        // Step 1: Load from cache first for fast initial load
        if (cacheFile.exists() && !forceRefresh) {
            try {
                val cachedJson = cacheFile.readText()
                val jsonObject = JSONObject(cachedJson)
                val packsArray = jsonObject.optJSONArray("packs") ?: JSONArray()
                val cachedPacks = mutableListOf<StickerPack>()
                
                for (i in 0 until packsArray.length()) {
                    try {
                        cachedPacks.add(StickerPack.fromJson(packsArray.getJSONObject(i)))
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing cached pack at index $i", e)
                    }
                }
                
                Log.d(TAG, "Loaded ${cachedPacks.size} packs from cache")
                
                // If we have cached data, return it immediately and sync in background
                if (cachedPacks.isNotEmpty() && !forceRefresh) {
                    // Trigger background sync without waiting
                    syncPacksInBackground()
                    return cachedPacks
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read from cache, falling back to Firebase", e)
            }
        }

        // Step 2: Fetch from Firebase
        return try {
            val snapshot = firestore.collection(COLLECTION_STICKER_PACKS).get().await()
            val packs = snapshot.documents.mapNotNull { doc ->
                try {
                    StickerPack.fromFirestore(doc)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing pack ${doc.id} from Firestore", e)
                    null
                }
            }

            // Step 3: Save to cache for future use
            savePacksToCache(packs)
            Log.d(TAG, "Loaded ${packs.size} packs from Firebase")
            packs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch packs from Firebase", e)
            // Try to return cached data as fallback
            loadPacksFromCache() ?: emptyList()
        }
    }

    // -----------------------------------------------------
    // 2️⃣ Get stickers for a specific pack
    // -----------------------------------------------------
    suspend fun getStickersForPack(packId: String, forceRefresh: Boolean = false): List<StickerData> {
        val cacheFile = File(cacheDir, "$packId.json")

        // Check cache first
        if (cacheFile.exists() && !forceRefresh) {
            try {
                val jsonArray = JSONArray(cacheFile.readText())
                val cachedStickers = mutableListOf<StickerData>()
                
                for (i in 0 until jsonArray.length()) {
                    try {
                        cachedStickers.add(StickerData.fromJson(jsonArray.getJSONObject(i)))
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing cached sticker at index $i for pack $packId", e)
                    }
                }
                
                Log.d(TAG, "Loaded ${cachedStickers.size} stickers from cache for pack $packId")
                return cachedStickers
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read stickers cache for pack $packId", e)
            }
        }

        // Fetch from Firebase subcollection
        return try {
            val snapshot = firestore.collection(COLLECTION_STICKER_PACKS)
                .document(packId)
                .collection(COLLECTION_STICKERS)
                .get()
                .await()

            val stickers = snapshot.documents.mapNotNull { doc ->
                try {
                    StickerData.fromFirestore(doc, packId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing sticker ${doc.id} from Firestore", e)
                    null
                }
            }

            // Save to cache
            saveStickersToCache(packId, stickers)
            Log.d(TAG, "Loaded ${stickers.size} stickers from Firebase for pack $packId")
            stickers
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stickers for pack $packId from Firebase", e)
            emptyList()
        }
    }

    // -----------------------------------------------------
    // 3️⃣ Download sticker image to local cache
    // -----------------------------------------------------
    suspend fun downloadSticker(sticker: StickerData): String? {
        val fileName = "${sticker.id}.${getFileExtension(sticker.imageUrl)}"
        val file = File(cacheDir, fileName)
        
        // Check if already downloaded
        if (file.exists()) {
            Log.d(TAG, "Sticker ${sticker.id} already cached")
            return file.absolutePath
        }

        return try {
            // Handle Firebase Storage URLs and regular URLs
            if (sticker.imageUrl.startsWith("gs://") || sticker.imageUrl.contains("firebasestorage")) {
                // Firebase Storage download
                val ref = storage.getReferenceFromUrl(sticker.imageUrl)
                ref.getFile(file).await()
            } else if (sticker.imageUrl.startsWith("http")) {
                // Regular HTTP download
                downloadFromUrl(sticker.imageUrl, file)
            } else {
                Log.e(TAG, "Unsupported URL format: ${sticker.imageUrl}")
                return null
            }

            Log.d(TAG, "Successfully downloaded sticker ${sticker.id}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download sticker ${sticker.id}: ${sticker.imageUrl}", e)
            null
        }
    }

    // -----------------------------------------------------
    // 4️⃣ Search stickers by tags or emojis
    // -----------------------------------------------------
    suspend fun searchStickers(query: String): List<StickerData> {
        val searchQuery = query.lowercase().trim()
        val results = mutableListOf<StickerData>()

        try {
            // Get all packs and search through their stickers
            val packs = getStickerPacks()
            for (pack in packs) {
                val stickers = getStickersForPack(pack.id)
                stickers.forEach { sticker ->
                    if (matchesSearchQuery(sticker, searchQuery)) {
                        results.add(sticker)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching stickers", e)
        }

        return results
    }

    // -----------------------------------------------------
    // 5️⃣ Update sticker usage statistics
    // -----------------------------------------------------
    suspend fun recordStickerUsage(stickerId: String) {
        try {
            // This would typically update Firestore analytics or local usage tracking
            // For now, just log the usage
            Log.d(TAG, "Recorded usage for sticker: $stickerId")
            
            // Could implement usage tracking in Firestore:
            // firestore.collection("sticker_usage")
            //     .document(stickerId)
            //     .update("usageCount", FieldValue.increment(1))
        } catch (e: Exception) {
            Log.e(TAG, "Error recording sticker usage", e)
        }
    }

    // -----------------------------------------------------
    // 6️⃣ Private helper methods
    // -----------------------------------------------------
    private fun syncPacksInBackground() {
        // Launch background sync - in a real implementation, 
        // you'd use a coroutine scope tied to application lifecycle
        Thread {
            try {
                val freshPacks = Tasks.await(firestore.collection(COLLECTION_STICKER_PACKS).get())
                val packs = freshPacks.documents.mapNotNull { doc ->
                    try {
                        StickerPack.fromFirestore(doc)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing pack in background sync", e)
                        null
                    }
                }
                savePacksToCache(packs)
                Log.d(TAG, "Background sync completed for ${packs.size} packs")
            } catch (e: Exception) {
                Log.e(TAG, "Background sync failed", e)
            }
        }.start()
    }

    private fun savePacksToCache(packs: List<StickerPack>) {
        try {
            val jsonArray = JSONArray()
            packs.forEach { pack ->
                jsonArray.put(pack.toJson())
            }
            
            val cacheObject = JSONObject().apply {
                put("packs", jsonArray)
                put("lastUpdated", System.currentTimeMillis())
                put("version", "2.0")
            }
            
            File(cacheDir, PACKS_CACHE_FILE).writeText(cacheObject.toString())
            Log.d(TAG, "Saved ${packs.size} packs to cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving packs to cache", e)
        }
    }

    private fun saveStickersToCache(packId: String, stickers: List<StickerData>) {
        try {
            val jsonArray = JSONArray()
            stickers.forEach { sticker ->
                jsonArray.put(sticker.toJson())
            }
            
            File(cacheDir, "$packId.json").writeText(jsonArray.toString())
            Log.d(TAG, "Saved ${stickers.size} stickers to cache for pack $packId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stickers to cache for pack $packId", e)
        }
    }

    private fun loadPacksFromCache(): List<StickerPack>? {
        return try {
            val cacheFile = File(cacheDir, PACKS_CACHE_FILE)
            if (!cacheFile.exists()) return null
            
            val jsonObject = JSONObject(cacheFile.readText())
            val packsArray = jsonObject.optJSONArray("packs") ?: return null
            val packs = mutableListOf<StickerPack>()
            
            for (i in 0 until packsArray.length()) {
                try {
                    packs.add(StickerPack.fromJson(packsArray.getJSONObject(i)))
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing cached pack at index $i", e)
                }
            }
            
            packs
        } catch (e: Exception) {
            Log.e(TAG, "Error loading packs from cache", e)
            null
        }
    }

    private fun matchesSearchQuery(sticker: StickerData, query: String): Boolean {
        // Search in tags
        if (sticker.tags.any { it.lowercase().contains(query) }) return true
        
        // Search in emojis
        if (sticker.emojis.any { it.contains(query) }) return true
        
        // Search in sticker ID
        if (sticker.id.lowercase().contains(query)) return true
        
        return false
    }

    private fun getFileExtension(url: String): String {
        return when {
            url.contains(".png", ignoreCase = true) -> "png"
            url.contains(".jpg", ignoreCase = true) || url.contains(".jpeg", ignoreCase = true) -> "jpg"
            url.contains(".gif", ignoreCase = true) -> "gif"
            url.contains(".webp", ignoreCase = true) -> "webp"
            else -> "png" // Default to PNG
        }
    }

    private suspend fun downloadFromUrl(url: String, file: File) {
        // Simple HTTP download implementation
        // In production, you might want to use a more robust HTTP client
        val connection = java.net.URL(url).openConnection()
        connection.doInput = true
        connection.connect()

        connection.getInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    // -----------------------------------------------------
    // 7️⃣ Cache management methods
    // -----------------------------------------------------
    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Log.d(TAG, "Cleared sticker cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }

    fun getCacheSize(): Long {
        return try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
            0L
        }
    }
}
