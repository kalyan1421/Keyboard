package com.example.ai_keyboard.stickers

import android.content.Context
import android.util.Log
import com.example.ai_keyboard.StickerManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Migration helper to transition from SQLite to Firebase + JSON cache
 */
class StickerMigrationHelper(private val context: Context) {

    companion object {
        private const val TAG = "StickerMigrationHelper"
        private const val MIGRATION_KEY = "sticker_migration_completed"
        private const val MIGRATION_VERSION = 1
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val settings = context.getSharedPreferences("sticker_migration", Context.MODE_PRIVATE)

    /**
     * Check if migration is needed and perform it
     */
    suspend fun performMigrationIfNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val migrationCompleted = settings.getInt(MIGRATION_KEY, 0) >= MIGRATION_VERSION
                
                if (migrationCompleted) {
                    Log.d(TAG, "Migration already completed")
                    return@withContext true
                }

                Log.d(TAG, "Starting sticker migration from SQLite to Firebase+JSON...")
                
                // Step 1: Export from legacy SQLite system
                val legacyData = exportFromLegacySystem()
                
                // Step 2: Import to Firebase (if needed - might be done by admin)
                if (legacyData.isNotEmpty()) {
                    Log.d(TAG, "Found ${legacyData.size} legacy sticker packs")
                    // Note: In production, you might upload this to Firebase via admin script
                    // For now, we'll save as JSON cache to help with transition
                    saveLegacyDataAsCache(legacyData)
                }
                
                // Step 3: Initialize new system with legacy assets
                initializeFromAssets()
                
                // Mark migration as completed
                settings.edit()
                    .putInt(MIGRATION_KEY, MIGRATION_VERSION)
                    .putLong("migration_timestamp", System.currentTimeMillis())
                    .apply()
                
                Log.d(TAG, "✅ Sticker migration completed successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Migration failed", e)
                false
            }
        }
    }

    /**
     * Export data from legacy SQLite system
     */
    private fun exportFromLegacySystem(): List<StickerPack> {
        return try {
            val legacyStickerManager = StickerManager(context)
            val availablePacks = legacyStickerManager.getAvailablePacks()
            val installedPacks = legacyStickerManager.getInstalledPacks()
            
            Log.d(TAG, "Legacy system has ${availablePacks.size} available packs, ${installedPacks.size} installed")
            
            // Convert legacy packs to new format
            availablePacks.map { legacyPack ->
                StickerPack(
                    id = legacyPack.id,
                    name = legacyPack.name,
                    author = legacyPack.author,
                    thumbnailUrl = legacyPack.thumbnailUrl,
                    category = legacyPack.category,
                    version = legacyPack.version,
                    stickerCount = legacyPack.stickers.size,
                    isInstalled = installedPacks.any { it.id == legacyPack.id }
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not access legacy system", e)
            emptyList()
        }
    }

    /**
     * Save legacy data as JSON cache for the new system
     */
    private fun saveLegacyDataAsCache(packs: List<StickerPack>) {
        try {
            val stickerRepository = StickerRepository(context)
            val cacheDir = File(context.filesDir, "stickers_cache")
            
            // Save packs list
            val packsArray = JSONArray()
            packs.forEach { pack ->
                packsArray.put(pack.toJson())
            }
            
            val cacheObject = JSONObject().apply {
                put("packs", packsArray)
                put("lastUpdated", System.currentTimeMillis())
                put("version", "2.0")
                put("source", "legacy_migration")
            }
            
            File(cacheDir, "packs.json").writeText(cacheObject.toString())
            Log.d(TAG, "Saved legacy packs to JSON cache")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving legacy data to cache", e)
        }
    }

    /**
     * Initialize new system from assets/stickers.json
     */
    private fun initializeFromAssets() {
        try {
            val inputStream = context.assets.open("stickers.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val stickerPacksArray = jsonObject.getJSONArray("sticker_packs")
            
            val packs = mutableListOf<StickerPack>()
            val allStickers = mutableMapOf<String, List<StickerData>>()
            
            for (i in 0 until stickerPacksArray.length()) {
                val packObj = stickerPacksArray.getJSONObject(i)
                
                // Convert legacy pack format
                val pack = StickerPack.fromLegacyJson(packObj)
                packs.add(pack)
                
                // Convert legacy stickers
                val stickersArray = packObj.optJSONArray("stickers")
                if (stickersArray != null) {
                    val stickers = mutableListOf<StickerData>()
                    for (j in 0 until stickersArray.length()) {
                        val stickerObj = stickersArray.getJSONObject(j)
                        val sticker = StickerData.fromLegacyJson(stickerObj, pack.id)
                        stickers.add(sticker)
                    }
                    allStickers[pack.id] = stickers
                }
            }
            
            // Save to new JSON cache format
            val cacheDir = File(context.filesDir, "stickers_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            // Save packs
            val packsArray = JSONArray()
            packs.forEach { pack ->
                packsArray.put(pack.toJson())
            }
            
            val cacheObject = JSONObject().apply {
                put("packs", packsArray)
                put("lastUpdated", System.currentTimeMillis())
                put("version", "2.0")
                put("source", "assets_initialization")
            }
            
            File(cacheDir, "packs.json").writeText(cacheObject.toString())
            
            // Save individual pack stickers
            allStickers.forEach { (packId, stickers) ->
                val stickersArray = JSONArray()
                stickers.forEach { sticker ->
                    stickersArray.put(sticker.toJson())
                }
                File(cacheDir, "$packId.json").writeText(stickersArray.toString())
            }
            
            Log.d(TAG, "Initialized ${packs.size} packs from assets")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing from assets", e)
        }
    }

    /**
     * Clean up old SQLite files (call this after successful migration)
     */
    fun cleanupLegacyFiles() {
        try {
            val dbFile = File(context.getDatabasePath("stickers.db").absolutePath)
            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                Log.d(TAG, "Legacy database cleanup: ${if (deleted) "success" else "failed"}")
            }
            
            // Clean up old cache directories if they exist
            val legacyCache = File(context.cacheDir, "stickers")
            if (legacyCache.exists() && legacyCache.isDirectory) {
                legacyCache.deleteRecursively()
                Log.d(TAG, "Cleaned up legacy sticker cache")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during legacy cleanup", e)
        }
    }

    /**
     * Force re-migration (useful for testing)
     */
    fun resetMigration() {
        settings.edit().remove(MIGRATION_KEY).apply()
        Log.d(TAG, "Migration reset - will run again next time")
    }

    /**
     * Get migration status
     */
    fun getMigrationStatus(): MigrationStatus {
        val completed = settings.getInt(MIGRATION_KEY, 0) >= MIGRATION_VERSION
        val timestamp = settings.getLong("migration_timestamp", 0)
        
        return MigrationStatus(
            completed = completed,
            version = settings.getInt(MIGRATION_KEY, 0),
            timestamp = timestamp
        )
    }
}

/**
 * Migration status data class
 */
data class MigrationStatus(
    val completed: Boolean,
    val version: Int,
    val timestamp: Long
)

