package com.example.ai_keyboard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.Executors

/**
 * Sticker data classes
 */
data class StickerPack(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val thumbnailUrl: String,
    val category: String,
    val stickers: List<StickerData>,
    val isInstalled: Boolean = false,
    val installProgress: Int = 0
)

data class StickerData(
    val id: String,
    val packId: String,
    val imageUrl: String,
    val emojis: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val localPath: String? = null,
    val isDownloaded: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long = 0L
)

/**
 * Sticker database for managing sticker packs and individual stickers
 */
class StickerDatabase(private val context: Context) : SQLiteOpenHelper(context, "stickers.db", null, 1) {
    
    companion object {
        private const val TAG = "StickerDatabase"
        private const val TABLE_PACKS = "sticker_packs"
        private const val TABLE_STICKERS = "stickers"
        
        // Pack table columns
        private const val COL_PACK_ID = "pack_id"
        private const val COL_PACK_NAME = "name"
        private const val COL_PACK_AUTHOR = "author"
        private const val COL_PACK_VERSION = "version"
        private const val COL_PACK_THUMBNAIL = "thumbnail_url"
        private const val COL_PACK_CATEGORY = "category"
        private const val COL_PACK_INSTALLED = "is_installed"
        private const val COL_PACK_PROGRESS = "install_progress"
        
        // Sticker table columns
        private const val COL_STICKER_ID = "sticker_id"
        private const val COL_STICKER_PACK_ID = "pack_id"
        private const val COL_STICKER_URL = "image_url"
        private const val COL_STICKER_EMOJIS = "emojis"
        private const val COL_STICKER_TAGS = "tags"
        private const val COL_STICKER_LOCAL_PATH = "local_path"
        private const val COL_STICKER_DOWNLOADED = "is_downloaded"
        private const val COL_STICKER_USAGE = "usage_count"
        private const val COL_STICKER_LAST_USED = "last_used"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        // Create sticker packs table
        db.execSQL("""
            CREATE TABLE $TABLE_PACKS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PACK_ID TEXT UNIQUE NOT NULL,
                $COL_PACK_NAME TEXT NOT NULL,
                $COL_PACK_AUTHOR TEXT,
                $COL_PACK_VERSION TEXT,
                $COL_PACK_THUMBNAIL TEXT,
                $COL_PACK_CATEGORY TEXT DEFAULT 'general',
                $COL_PACK_INSTALLED INTEGER DEFAULT 0,
                $COL_PACK_PROGRESS INTEGER DEFAULT 0
            )
        """)
        
        // Create stickers table
        db.execSQL("""
            CREATE TABLE $TABLE_STICKERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_STICKER_ID TEXT UNIQUE NOT NULL,
                $COL_STICKER_PACK_ID TEXT NOT NULL,
                $COL_STICKER_URL TEXT NOT NULL,
                $COL_STICKER_EMOJIS TEXT,
                $COL_STICKER_TAGS TEXT,
                $COL_STICKER_LOCAL_PATH TEXT,
                $COL_STICKER_DOWNLOADED INTEGER DEFAULT 0,
                $COL_STICKER_USAGE INTEGER DEFAULT 0,
                $COL_STICKER_LAST_USED INTEGER DEFAULT 0,
                FOREIGN KEY($COL_STICKER_PACK_ID) REFERENCES $TABLE_PACKS($COL_PACK_ID)
            )
        """)
        
        // Create indexes
        db.execSQL("CREATE INDEX idx_pack_installed ON $TABLE_PACKS($COL_PACK_INSTALLED)")
        db.execSQL("CREATE INDEX idx_pack_category ON $TABLE_PACKS($COL_PACK_CATEGORY)")
        db.execSQL("CREATE INDEX idx_sticker_pack ON $TABLE_STICKERS($COL_STICKER_PACK_ID)")
        db.execSQL("CREATE INDEX idx_sticker_usage ON $TABLE_STICKERS($COL_STICKER_USAGE DESC)")
        db.execSQL("CREATE INDEX idx_sticker_last_used ON $TABLE_STICKERS($COL_STICKER_LAST_USED DESC)")
        
        // Populate with default sticker data
        populateDefaultStickers(db)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STICKERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PACKS")
        onCreate(db)
    }
    
    private fun populateDefaultStickers(db: SQLiteDatabase) {
        try {
            val inputStream = context.assets.open("stickers.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val stickerPacks = jsonObject.getJSONArray("sticker_packs")
            
            for (i in 0 until stickerPacks.length()) {
                val packObj = stickerPacks.getJSONObject(i)
                insertStickerPack(db, packObj)
            }
            
            Log.d(TAG, "Successfully loaded sticker database")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sticker data from assets, using fallback", e)
            loadFallbackStickers(db)
        }
    }
    
    private fun insertStickerPack(db: SQLiteDatabase, packObj: JSONObject) {
        try {
            val packId = packObj.getString("id")
            
            // Insert pack
            val packValues = ContentValues().apply {
                put(COL_PACK_ID, packId)
                put(COL_PACK_NAME, packObj.getString("name"))
                put(COL_PACK_AUTHOR, packObj.getString("author"))
                put(COL_PACK_VERSION, packObj.getString("version"))
                put(COL_PACK_THUMBNAIL, "file:///android_asset/stickers/${packObj.getString("thumbnail")}")
                put(COL_PACK_CATEGORY, packObj.getString("category"))
                put(COL_PACK_INSTALLED, 1) // Asset stickers are pre-installed
                put(COL_PACK_PROGRESS, 100)
            }
            
            db.insert(TABLE_PACKS, null, packValues)
            
            // Insert stickers
            val stickersArray = packObj.getJSONArray("stickers")
            for (j in 0 until stickersArray.length()) {
                val stickerObj = stickersArray.getJSONObject(j)
                
                val stickerValues = ContentValues().apply {
                    put(COL_STICKER_ID, stickerObj.getString("id"))
                    put(COL_STICKER_PACK_ID, packId)
                    put(COL_STICKER_URL, "file:///android_asset/stickers/${stickerObj.getString("file")}")
                    put(COL_STICKER_EMOJIS, stickerObj.getJSONArray("emojis").toString())
                    put(COL_STICKER_TAGS, stickerObj.getJSONArray("tags").toString())
                    put(COL_STICKER_LOCAL_PATH, "android_asset/stickers/${stickerObj.getString("file")}")
                    put(COL_STICKER_DOWNLOADED, 1)
                }
                
                db.insert(TABLE_STICKERS, null, stickerValues)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting sticker pack: ${packObj}", e)
        }
    }
    
    private fun loadFallbackStickers(db: SQLiteDatabase) {
        Log.d(TAG, "Loading fallback sticker data")
        
        // Create a basic emoji sticker pack
        val packValues = ContentValues().apply {
            put(COL_PACK_ID, "basic_emojis")
            put(COL_PACK_NAME, "Basic Emojis")
            put(COL_PACK_AUTHOR, "AI Keyboard")
            put(COL_PACK_VERSION, "1.0")
            put(COL_PACK_THUMBNAIL, "")
            put(COL_PACK_CATEGORY, "basic")
            put(COL_PACK_INSTALLED, 1)
            put(COL_PACK_PROGRESS, 100)
        }
        
        db.insert(TABLE_PACKS, null, packValues)
        
        // Add some basic emoji stickers
        val basicStickers = listOf(
            Triple("happy_face", "😊", listOf("happy", "smile")),
            Triple("thumbs_up", "👍", listOf("good", "approval")),
            Triple("heart", "❤️", listOf("love", "heart")),
            Triple("fire", "🔥", listOf("hot", "fire", "awesome")),
            Triple("star", "⭐", listOf("star", "favorite"))
        )
        
        basicStickers.forEach { (id, emoji, tags) ->
            val stickerValues = ContentValues().apply {
                put(COL_STICKER_ID, id)
                put(COL_STICKER_PACK_ID, "basic_emojis")
                put(COL_STICKER_URL, "emoji://$emoji")
                put(COL_STICKER_EMOJIS, "[]")
                put(COL_STICKER_TAGS, JSONArray(tags).toString())
                put(COL_STICKER_LOCAL_PATH, "emoji://$emoji")
                put(COL_STICKER_DOWNLOADED, 1)
            }
            db.insert(TABLE_STICKERS, null, stickerValues)
        }
    }
    
    fun getInstalledPacks(): List<StickerPack> {
        val cursor = readableDatabase.query(
            TABLE_PACKS,
            null,
            "$COL_PACK_INSTALLED = 1",
            null,
            null, null,
            "$COL_PACK_NAME ASC"
        )
        
        return cursorToStickerPackList(cursor)
    }
    
    fun getAvailablePacks(): List<StickerPack> {
        val cursor = readableDatabase.query(
            TABLE_PACKS,
            null,
            "$COL_PACK_INSTALLED = 0",
            null,
            null, null,
            "$COL_PACK_CATEGORY ASC, $COL_PACK_NAME ASC"
        )
        
        return cursorToStickerPackList(cursor)
    }
    
    fun getStickersFromPack(packId: String): List<StickerData> {
        val cursor = readableDatabase.query(
            TABLE_STICKERS,
            null,
            "$COL_STICKER_PACK_ID = ?",
            arrayOf(packId),
            null, null,
            "$COL_STICKER_USAGE DESC, id ASC"
        )
        
        return cursorToStickerList(cursor)
    }
    
    fun searchStickers(query: String): List<StickerData> {
        val cursor = readableDatabase.query(
            TABLE_STICKERS,
            null,
            "$COL_STICKER_TAGS LIKE ? AND $COL_STICKER_DOWNLOADED = 1",
            arrayOf("%$query%"),
            null, null,
            "$COL_STICKER_USAGE DESC",
            "20"
        )
        
        return cursorToStickerList(cursor)
    }
    
    fun getRecentStickers(limit: Int = 15): List<StickerData> {
        val cursor = readableDatabase.query(
            TABLE_STICKERS,
            null,
            "$COL_STICKER_LAST_USED > 0 AND $COL_STICKER_DOWNLOADED = 1",
            null,
            null, null,
            "$COL_STICKER_LAST_USED DESC",
            limit.toString()
        )
        
        return cursorToStickerList(cursor)
    }
    
    fun markPackAsInstalled(packId: String) {
        val values = ContentValues().apply {
            put(COL_PACK_INSTALLED, 1)
            put(COL_PACK_PROGRESS, 100)
        }
        
        writableDatabase.update(
            TABLE_PACKS,
            values,
            "$COL_PACK_ID = ?",
            arrayOf(packId)
        )
    }
    
    fun updatePackProgress(packId: String, progress: Int) {
        val values = ContentValues().apply {
            put(COL_PACK_PROGRESS, progress)
        }
        
        writableDatabase.update(
            TABLE_PACKS,
            values,
            "$COL_PACK_ID = ?",
            arrayOf(packId)
        )
    }
    
    fun markStickerAsDownloaded(stickerId: String, localPath: String) {
        val values = ContentValues().apply {
            put(COL_STICKER_DOWNLOADED, 1)
            put(COL_STICKER_LOCAL_PATH, localPath)
        }
        
        writableDatabase.update(
            TABLE_STICKERS,
            values,
            "$COL_STICKER_ID = ?",
            arrayOf(stickerId)
        )
    }
    
    fun recordStickerUsage(stickerId: String) {
        val currentTime = System.currentTimeMillis()
        writableDatabase.execSQL("""
            UPDATE $TABLE_STICKERS 
            SET $COL_STICKER_USAGE = $COL_STICKER_USAGE + 1, $COL_STICKER_LAST_USED = ?
            WHERE $COL_STICKER_ID = ?
        """, arrayOf(currentTime.toString(), stickerId))
        
        Log.d(TAG, "Recorded usage for sticker: $stickerId")
    }
    
    private fun cursorToStickerPackList(cursor: android.database.Cursor): List<StickerPack> {
        val results = mutableListOf<StickerPack>()
        
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val packId = it.getString(it.getColumnIndexOrThrow(COL_PACK_ID))
                    val stickers = getStickersFromPack(packId)
                    
                    results.add(StickerPack(
                        id = packId,
                        name = it.getString(it.getColumnIndexOrThrow(COL_PACK_NAME)),
                        author = it.getString(it.getColumnIndexOrThrow(COL_PACK_AUTHOR)),
                        version = it.getString(it.getColumnIndexOrThrow(COL_PACK_VERSION)),
                        thumbnailUrl = it.getString(it.getColumnIndexOrThrow(COL_PACK_THUMBNAIL)),
                        category = it.getString(it.getColumnIndexOrThrow(COL_PACK_CATEGORY)),
                        stickers = stickers,
                        isInstalled = it.getInt(it.getColumnIndexOrThrow(COL_PACK_INSTALLED)) == 1,
                        installProgress = it.getInt(it.getColumnIndexOrThrow(COL_PACK_PROGRESS))
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing sticker pack from cursor", e)
                }
            }
        }
        
        return results
    }
    
    private fun cursorToStickerList(cursor: android.database.Cursor): List<StickerData> {
        val results = mutableListOf<StickerData>()
        
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val emojisStr = it.getString(it.getColumnIndexOrThrow(COL_STICKER_EMOJIS))
                    val tagsStr = it.getString(it.getColumnIndexOrThrow(COL_STICKER_TAGS))
                    
                    val emojis = try {
                        val emojisArray = JSONArray(emojisStr)
                        (0 until emojisArray.length()).map { i -> emojisArray.getString(i) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    val tags = try {
                        val tagsArray = JSONArray(tagsStr)
                        (0 until tagsArray.length()).map { i -> tagsArray.getString(i) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    results.add(StickerData(
                        id = it.getString(it.getColumnIndexOrThrow(COL_STICKER_ID)),
                        packId = it.getString(it.getColumnIndexOrThrow(COL_STICKER_PACK_ID)),
                        imageUrl = it.getString(it.getColumnIndexOrThrow(COL_STICKER_URL)),
                        emojis = emojis,
                        tags = tags,
                        localPath = it.getString(it.getColumnIndexOrThrow(COL_STICKER_LOCAL_PATH)),
                        isDownloaded = it.getInt(it.getColumnIndexOrThrow(COL_STICKER_DOWNLOADED)) == 1,
                        usageCount = it.getInt(it.getColumnIndexOrThrow(COL_STICKER_USAGE)),
                        lastUsed = it.getLong(it.getColumnIndexOrThrow(COL_STICKER_LAST_USED))
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing sticker from cursor", e)
                }
            }
        }
        
        return results
    }
}

/**
 * Sticker cache for downloading and storing sticker images
 */
class StickerCache(private val context: Context) {
    
    companion object {
        private const val TAG = "StickerCache"
        private const val CACHE_DIR_NAME = "stickers"
    }
    
    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
    private val maxCacheSize = 100 * 1024 * 1024L // 100MB
    private val executor = Executors.newFixedThreadPool(3)
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    fun downloadSticker(stickerData: StickerData): Boolean {
        return try {
            val file = File(cacheDir, "${stickerData.id}.png")
            
            // Check if already cached
            if (file.exists()) return true
            
            // Handle special emoji:// URLs
            if (stickerData.imageUrl.startsWith("emoji://")) {
                // For emoji stickers, we don't need to download
                return true
            }
            
            // Download sticker image
            val url = URL(stickerData.imageUrl)
            val connection = url.openConnection()
            connection.doInput = true
            connection.connect()
            
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(file)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            // Manage cache size
            manageCacheSize()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error caching sticker: ${stickerData.id}", e)
            false
        }
    }
    
    private fun manageCacheSize() {
        val files = cacheDir.listFiles() ?: return
        val totalSize = files.sumOf { it.length() }
        
        if (totalSize > maxCacheSize) {
            // Remove oldest files (25% of total)
            files.sortedBy { it.lastModified() }
                .take((files.size * 0.25).toInt())
                .forEach { 
                    it.delete()
                    Log.d(TAG, "Deleted cached sticker: ${it.name}")
                }
        }
    }
    
    fun getLocalPath(stickerId: String): String? {
        val file = File(cacheDir, "$stickerId.png")
        return if (file.exists()) file.absolutePath else null
    }
    
    fun isCached(stickerId: String): Boolean {
        return File(cacheDir, "$stickerId.png").exists()
    }
    
    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        Log.d(TAG, "Cleared sticker cache")
    }
}

/**
 * Main sticker manager class
 */
class StickerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "StickerManager"
    }
    
    private val stickerDatabase = StickerDatabase(context)
    private val stickerCache = StickerCache(context)
    
    fun getInstalledPacks(): List<StickerPack> {
        return stickerDatabase.getInstalledPacks()
    }
    
    fun getAvailablePacks(): List<StickerPack> {
        return stickerDatabase.getAvailablePacks()
    }
    
    fun installStickerPack(pack: StickerPack, progressCallback: (Int) -> Unit) {
        Thread {
            try {
                var downloadedCount = 0
                val totalStickers = pack.stickers.size
                
                pack.stickers.forEach { sticker ->
                    val success = stickerCache.downloadSticker(sticker)
                    if (success) {
                        val localPath = stickerCache.getLocalPath(sticker.id)
                        localPath?.let { 
                            stickerDatabase.markStickerAsDownloaded(sticker.id, it)
                        }
                        downloadedCount++
                        val progress = (downloadedCount * 100) / totalStickers
                        progressCallback(progress)
                        stickerDatabase.updatePackProgress(pack.id, progress)
                    }
                }
                
                if (downloadedCount == totalStickers) {
                    stickerDatabase.markPackAsInstalled(pack.id)
                    Log.d(TAG, "Successfully installed sticker pack: ${pack.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error installing sticker pack: ${pack.name}", e)
            }
        }.start()
    }
    
    fun getStickersFromPack(packId: String): List<StickerData> {
        return stickerDatabase.getStickersFromPack(packId)
    }
    
    fun searchStickers(query: String): List<StickerData> {
        return stickerDatabase.searchStickers(query)
    }
    
    fun getRecentStickers(): List<StickerData> {
        return stickerDatabase.getRecentStickers()
    }
    
    fun recordStickerUsage(stickerId: String) {
        stickerDatabase.recordStickerUsage(stickerId)
    }
    
    fun getCacheSize(): Long {
        return stickerCache.getCacheSize()
    }
    
    fun clearCache() {
        stickerCache.clearCache()
    }
}
