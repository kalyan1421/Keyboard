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
import java.util.concurrent.Future

/**
 * GIF data classes
 */
data class GifData(
    val id: String,
    val url: String,
    val thumbnailUrl: String,
    val title: String,
    val tags: List<String>,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val category: String = "general",
    val isDownloaded: Boolean = false,
    val localPath: String? = null,
    val thumbnailPath: String? = null
)

/**
 * GIF database for managing GIF metadata
 */
class GifDatabase(private val context: Context) : SQLiteOpenHelper(context, "gifs.db", null, 1) {
    
    companion object {
        private const val TAG = "GifDatabase"
        private const val TABLE_GIFS = "gifs"
        
        // GIF table columns
        private const val COL_ID = "id"
        private const val COL_GIF_ID = "gif_id"
        private const val COL_URL = "url"
        private const val COL_THUMBNAIL_URL = "thumbnail_url"
        private const val COL_TITLE = "title"
        private const val COL_TAGS = "tags"
        private const val COL_WIDTH = "width"
        private const val COL_HEIGHT = "height"
        private const val COL_FILE_SIZE = "file_size"
        private const val COL_CATEGORY = "category"
        private const val COL_IS_DOWNLOADED = "is_downloaded"
        private const val COL_LOCAL_PATH = "local_path"
        private const val COL_THUMBNAIL_PATH = "thumbnail_path"
        private const val COL_USAGE_COUNT = "usage_count"
        private const val COL_LAST_USED = "last_used"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_GIFS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_GIF_ID TEXT UNIQUE NOT NULL,
                $COL_URL TEXT NOT NULL,
                $COL_THUMBNAIL_URL TEXT,
                $COL_TITLE TEXT NOT NULL,
                $COL_TAGS TEXT,
                $COL_WIDTH INTEGER DEFAULT 0,
                $COL_HEIGHT INTEGER DEFAULT 0,
                $COL_FILE_SIZE INTEGER DEFAULT 0,
                $COL_CATEGORY TEXT DEFAULT 'general',
                $COL_IS_DOWNLOADED INTEGER DEFAULT 0,
                $COL_LOCAL_PATH TEXT,
                $COL_THUMBNAIL_PATH TEXT,
                $COL_USAGE_COUNT INTEGER DEFAULT 0,
                $COL_LAST_USED INTEGER DEFAULT 0
            )
        """)
        
        // Create indexes
        db.execSQL("CREATE INDEX idx_gif_category ON $TABLE_GIFS($COL_CATEGORY)")
        db.execSQL("CREATE INDEX idx_gif_downloaded ON $TABLE_GIFS($COL_IS_DOWNLOADED)")
        db.execSQL("CREATE INDEX idx_gif_usage ON $TABLE_GIFS($COL_USAGE_COUNT DESC)")
        
        // Populate with default GIF data
        populateDefaultGifs(db)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GIFS")
        onCreate(db)
    }
    
    private fun populateDefaultGifs(db: SQLiteDatabase) {
        try {
            val inputStream = context.assets.open("gifs.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val collections = jsonObject.getJSONObject("gif_collections")
            
            collections.keys().forEach { category ->
                val gifsArray = collections.getJSONArray(category)
                for (i in 0 until gifsArray.length()) {
                    val gifObj = gifsArray.getJSONObject(i)
                    insertGif(db, gifObj, category)
                }
            }
            
            Log.d(TAG, "Successfully loaded GIF database")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading GIF data from assets, using fallback", e)
            loadFallbackGifs(db)
        }
    }
    
    private fun insertGif(db: SQLiteDatabase, gifObj: JSONObject, category: String) {
        try {
            val values = ContentValues().apply {
                put(COL_GIF_ID, gifObj.getString("id"))
                put(COL_URL, "file:///android_asset/gifs/${gifObj.getString("file")}")
                put(COL_THUMBNAIL_URL, "file:///android_asset/gifs/${gifObj.getString("thumbnail")}")
                put(COL_TITLE, gifObj.getString("title"))
                put(COL_TAGS, gifObj.getJSONArray("tags").toString())
                put(COL_WIDTH, gifObj.optInt("width", 320))
                put(COL_HEIGHT, gifObj.optInt("height", 240))
                put(COL_FILE_SIZE, gifObj.optLong("file_size", 0))
                put(COL_CATEGORY, category)
                put(COL_IS_DOWNLOADED, 1) // Asset GIFs are always "downloaded"
                put(COL_LOCAL_PATH, "android_asset/gifs/${gifObj.getString("file")}")
                put(COL_THUMBNAIL_PATH, "android_asset/gifs/${gifObj.getString("thumbnail")}")
            }
            
            db.insert(TABLE_GIFS, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting GIF: ${gifObj}", e)
        }
    }
    
    private fun loadFallbackGifs(db: SQLiteDatabase) {
        Log.d(TAG, "Loading fallback GIF data")
        
        val fallbackGifs = listOf(
            Triple("thumbs_up", "Thumbs Up", "reactions"),
            Triple("applause", "Applause", "reactions"),
            Triple("celebration", "Celebration", "reactions"),
            Triple("dancing", "Dancing", "activities"),
            Triple("laughing", "Laughing", "reactions"),
            Triple("crying", "Crying", "reactions"),
            Triple("shocked", "Shocked", "reactions"),
            Triple("thinking", "Thinking", "reactions"),
            Triple("sleeping", "Sleeping", "activities"),
            Triple("working", "Working", "activities")
        )
        
        fallbackGifs.forEach { (id, title, category) ->
            val values = ContentValues().apply {
                put(COL_GIF_ID, id)
                put(COL_URL, "placeholder_url")
                put(COL_THUMBNAIL_URL, "placeholder_thumbnail")
                put(COL_TITLE, title)
                put(COL_TAGS, "[]")
                put(COL_WIDTH, 320)
                put(COL_HEIGHT, 240)
                put(COL_FILE_SIZE, 50000)
                put(COL_CATEGORY, category)
                put(COL_IS_DOWNLOADED, 0)
            }
            db.insert(TABLE_GIFS, null, values)
        }
    }
    
    fun getGifsByCategory(category: String): List<GifData> {
        val cursor = readableDatabase.query(
            TABLE_GIFS,
            null,
            "$COL_CATEGORY = ?",
            arrayOf(category),
            null, null,
            "$COL_USAGE_COUNT DESC, $COL_TITLE ASC"
        )
        
        return cursorToGifList(cursor)
    }
    
    fun searchGifs(query: String): List<GifData> {
        val cursor = readableDatabase.query(
            TABLE_GIFS,
            null,
            "$COL_TITLE LIKE ? OR $COL_TAGS LIKE ?",
            arrayOf("%$query%", "%$query%"),
            null, null,
            "$COL_USAGE_COUNT DESC, $COL_TITLE ASC",
            "50"
        )
        
        return cursorToGifList(cursor)
    }
    
    fun getDownloadedGifs(): List<GifData> {
        val cursor = readableDatabase.query(
            TABLE_GIFS,
            null,
            "$COL_IS_DOWNLOADED = 1",
            null,
            null, null,
            "$COL_USAGE_COUNT DESC, $COL_LAST_USED DESC"
        )
        
        return cursorToGifList(cursor)
    }
    
    fun getRecentGifs(limit: Int = 20): List<GifData> {
        val cursor = readableDatabase.query(
            TABLE_GIFS,
            null,
            "$COL_LAST_USED > 0",
            null,
            null, null,
            "$COL_LAST_USED DESC",
            limit.toString()
        )
        
        return cursorToGifList(cursor)
    }
    
    fun markAsDownloaded(gifId: String, localPath: String) {
        val values = ContentValues().apply {
            put(COL_IS_DOWNLOADED, 1)
            put(COL_LOCAL_PATH, localPath)
        }
        
        writableDatabase.update(
            TABLE_GIFS,
            values,
            "$COL_GIF_ID = ?",
            arrayOf(gifId)
        )
    }
    
    fun recordGifUsage(gifId: String) {
        val currentTime = System.currentTimeMillis()
        writableDatabase.execSQL("""
            UPDATE $TABLE_GIFS 
            SET $COL_USAGE_COUNT = $COL_USAGE_COUNT + 1, $COL_LAST_USED = ?
            WHERE $COL_GIF_ID = ?
        """, arrayOf(currentTime.toString(), gifId))
        
        Log.d(TAG, "Recorded usage for GIF: $gifId")
    }
    
    fun getCategories(): List<String> {
        val cursor = readableDatabase.rawQuery(
            "SELECT DISTINCT $COL_CATEGORY FROM $TABLE_GIFS ORDER BY $COL_CATEGORY",
            null
        )
        
        val categories = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                categories.add(it.getString(0))
            }
        }
        
        return categories
    }
    
    private fun cursorToGifList(cursor: android.database.Cursor): List<GifData> {
        val results = mutableListOf<GifData>()
        
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val gifId = it.getString(it.getColumnIndexOrThrow(COL_GIF_ID))
                    val url = it.getString(it.getColumnIndexOrThrow(COL_URL))
                    val thumbnailUrl = it.getString(it.getColumnIndexOrThrow(COL_THUMBNAIL_URL))
                    val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                    val tagsStr = it.getString(it.getColumnIndexOrThrow(COL_TAGS))
                    val width = it.getInt(it.getColumnIndexOrThrow(COL_WIDTH))
                    val height = it.getInt(it.getColumnIndexOrThrow(COL_HEIGHT))
                    val fileSize = it.getLong(it.getColumnIndexOrThrow(COL_FILE_SIZE))
                    val category = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY))
                    val isDownloaded = it.getInt(it.getColumnIndexOrThrow(COL_IS_DOWNLOADED)) == 1
                    val localPath = it.getString(it.getColumnIndexOrThrow(COL_LOCAL_PATH))
                    val thumbnailPath = it.getString(it.getColumnIndexOrThrow(COL_THUMBNAIL_PATH))
                    
                    val tags = try {
                        val tagsArray = JSONArray(tagsStr)
                        (0 until tagsArray.length()).map { i -> tagsArray.getString(i) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    results.add(GifData(
                        id = gifId,
                        url = url,
                        thumbnailUrl = thumbnailUrl ?: url,
                        title = title,
                        tags = tags,
                        width = width,
                        height = height,
                        fileSize = fileSize,
                        category = category,
                        isDownloaded = isDownloaded,
                        localPath = localPath,
                        thumbnailPath = thumbnailPath
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing GIF from cursor", e)
                }
            }
        }
        
        return results
    }
}

/**
 * GIF cache manager for downloading and storing GIFs
 */
class GifCache(private val context: Context) {
    
    companion object {
        private const val TAG = "GifCache"
        private const val CACHE_DIR_NAME = "gifs"
        private const val THUMBNAIL_DIR_NAME = "gif_thumbnails"
    }
    
    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
    private val thumbnailDir = File(context.cacheDir, THUMBNAIL_DIR_NAME)
    private val maxCacheSize = 50 * 1024 * 1024L // 50MB
    private val executor = Executors.newFixedThreadPool(2)
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdirs()
        }
    }
    
    fun downloadAndCache(gifData: GifData): Boolean {
        return try {
            val gifFile = File(cacheDir, "${gifData.id}.gif")
            val thumbnailFile = File(thumbnailDir, "${gifData.id}.png")
            
            // Check if already cached
            if (gifFile.exists() && thumbnailFile.exists()) return true
            
            // Download GIF
            if (!gifFile.exists()) {
                downloadFile(gifData.url, gifFile)
            }
            
            // Download thumbnail
            if (!thumbnailFile.exists() && gifData.thumbnailUrl.isNotEmpty()) {
                downloadFile(gifData.thumbnailUrl, thumbnailFile)
            }
            
            // Manage cache size
            manageCacheSize()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error caching GIF: ${gifData.id}", e)
            false
        }
    }
    
    private fun downloadFile(url: String, file: File): Boolean {
        return try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(file)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file: $url", e)
            false
        }
    }
    
    private fun manageCacheSize() {
        val gifFiles = cacheDir.listFiles() ?: return
        val thumbnailFiles = thumbnailDir.listFiles() ?: return
        val allFiles = gifFiles + thumbnailFiles
        
        val totalSize = allFiles.sumOf { it.length() }
        
        if (totalSize > maxCacheSize) {
            // Remove oldest files (30% of total)
            allFiles.sortedBy { it.lastModified() }
                .take((allFiles.size * 0.3).toInt())
                .forEach { 
                    it.delete()
                    Log.d(TAG, "Deleted cached file: ${it.name}")
                }
        }
    }
    
    fun getLocalPath(gifId: String): String? {
        val file = File(cacheDir, "$gifId.gif")
        return if (file.exists()) file.absolutePath else null
    }
    
    fun getThumbnailPath(gifId: String): String? {
        val file = File(thumbnailDir, "$gifId.png")
        return if (file.exists()) file.absolutePath else null
    }
    
    fun isCached(gifId: String): Boolean {
        return File(cacheDir, "$gifId.gif").exists()
    }
    
    fun getCacheSize(): Long {
        val gifFiles = cacheDir.listFiles() ?: return 0L
        val thumbnailFiles = thumbnailDir.listFiles() ?: return 0L
        return (gifFiles + thumbnailFiles).sumOf { it.length() }
    }
    
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        thumbnailDir.listFiles()?.forEach { it.delete() }
        Log.d(TAG, "Cleared GIF cache")
    }
}

/**
 * Main GIF manager class
 */
class GifManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GifManager"
    }
    
    private val gifDatabase = GifDatabase(context)
    private val gifCache = GifCache(context)
    
    fun getTrendingGifs(): List<GifData> {
        return gifDatabase.getGifsByCategory("trending")
    }
    
    fun getReactionGifs(): List<GifData> {
        return gifDatabase.getGifsByCategory("reactions")
    }
    
    fun getCelebrationGifs(): List<GifData> {
        return gifDatabase.getGifsByCategory("celebrations")
    }
    
    fun getActivityGifs(): List<GifData> {
        return gifDatabase.getGifsByCategory("activities")
    }
    
    fun searchGifs(query: String): List<GifData> {
        return gifDatabase.searchGifs(query)
    }
    
    fun getDownloadedGifs(): List<GifData> {
        return gifDatabase.getDownloadedGifs()
    }
    
    fun getRecentGifs(): List<GifData> {
        return gifDatabase.getRecentGifs()
    }
    
    fun downloadGif(gifData: GifData, callback: (Boolean) -> Unit) {
        Thread {
            try {
                val success = gifCache.downloadAndCache(gifData)
                if (success) {
                    val localPath = gifCache.getLocalPath(gifData.id)
                    localPath?.let { 
                        gifDatabase.markAsDownloaded(gifData.id, it)
                    }
                }
                callback(success)
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading GIF: ${gifData.id}", e)
                callback(false)
            }
        }.start()
    }
    
    fun recordGifUsage(gifId: String) {
        gifDatabase.recordGifUsage(gifId)
    }
    
    fun getCategories(): List<String> {
        return gifDatabase.getCategories()
    }
    
    fun getCacheSize(): Long {
        return gifCache.getCacheSize()
    }
    
    fun clearCache() {
        gifCache.clearCache()
    }
}
