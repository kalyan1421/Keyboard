package com.example.ai_keyboard

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Emoji data classes
 */
data class EmojiData(
    val unicode: String,
    val description: String,
    val category: EmojiCategory,
    val keywords: List<String>,
    val skinToneVariants: List<String> = emptyList(),
    val frequency: Int = 0,
    val lastUsed: Long = 0L
)

enum class EmojiCategory {
    SMILEYS_EMOTION,
    PEOPLE_BODY,
    ANIMALS_NATURE,
    FOOD_DRINK,
    ACTIVITIES,
    TRAVEL_PLACES,
    OBJECTS,
    SYMBOLS,
    FLAGS,
    RECENTLY_USED,
    FREQUENTLY_USED
}

/**
 * SQLite database for emoji management
 */
class EmojiDatabase(private val context: Context) : SQLiteOpenHelper(context, "emojis.db", null, 1) {
    
    companion object {
        private const val TAG = "EmojiDatabase"
        private const val TABLE_EMOJIS = "emojis"
        private const val TABLE_EMOJI_SEARCH = "emoji_search"
        
        // Emoji table columns
        private const val COL_ID = "id"
        private const val COL_UNICODE = "unicode"
        private const val COL_DESCRIPTION = "description"
        private const val COL_CATEGORY = "category"
        private const val COL_KEYWORDS = "keywords"
        private const val COL_SKIN_VARIANTS = "skin_variants"
        private const val COL_USAGE_COUNT = "usage_count"
        private const val COL_LAST_USED = "last_used"
        
        // Search table columns
        private const val COL_EMOJI_ID = "emoji_id"
        private const val COL_KEYWORD = "keyword"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        // Create emojis table
        db.execSQL("""
            CREATE TABLE $TABLE_EMOJIS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_UNICODE TEXT UNIQUE NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_KEYWORDS TEXT NOT NULL,
                $COL_SKIN_VARIANTS TEXT,
                $COL_USAGE_COUNT INTEGER DEFAULT 0,
                $COL_LAST_USED INTEGER DEFAULT 0
            )
        """)
        
        // Create search index table
        db.execSQL("""
            CREATE TABLE $TABLE_EMOJI_SEARCH (
                $COL_EMOJI_ID INTEGER,
                $COL_KEYWORD TEXT,
                FOREIGN KEY($COL_EMOJI_ID) REFERENCES $TABLE_EMOJIS($COL_ID)
            )
        """)
        
        // Create indexes for performance
        db.execSQL("CREATE INDEX idx_emoji_category ON $TABLE_EMOJIS($COL_CATEGORY)")
        db.execSQL("CREATE INDEX idx_emoji_search ON $TABLE_EMOJI_SEARCH($COL_KEYWORD)")
        db.execSQL("CREATE INDEX idx_emoji_usage ON $TABLE_EMOJIS($COL_USAGE_COUNT DESC)")
        db.execSQL("CREATE INDEX idx_emoji_last_used ON $TABLE_EMOJIS($COL_LAST_USED DESC)")
        
        // Populate with default emoji data
        populateEmojiDatabase(db)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EMOJI_SEARCH")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EMOJIS")
        onCreate(db)
    }
    
    private fun populateEmojiDatabase(db: SQLiteDatabase) {
        try {
            val inputStream = context.assets.open("emojis.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val emojiArray = jsonObject.getJSONArray("emojis")
            
            Log.d(TAG, "Loading ${emojiArray.length()} emojis from assets")
            
            for (i in 0 until emojiArray.length()) {
                val emojiObj = emojiArray.getJSONObject(i)
                insertEmoji(db, emojiObj)
            }
            
            Log.d(TAG, "Successfully loaded emoji database")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading emoji data from assets", e)
            // Load fallback emoji data
            loadFallbackEmojis(db)
        }
    }
    
    private fun insertEmoji(db: SQLiteDatabase, emojiObj: JSONObject) {
        try {
            val unicode = emojiObj.getString("unicode")
            val description = emojiObj.getString("description")
            val category = emojiObj.getString("category")
            val keywords = emojiObj.getJSONArray("keywords")
            val skinVariants = emojiObj.optJSONArray("skin_variants")
            
            // Insert emoji
            val values = ContentValues().apply {
                put(COL_UNICODE, unicode)
                put(COL_DESCRIPTION, description)
                put(COL_CATEGORY, category)
                put(COL_KEYWORDS, keywords.toString())
                put(COL_SKIN_VARIANTS, skinVariants?.toString() ?: "")
                put(COL_USAGE_COUNT, 0)
                put(COL_LAST_USED, 0L)
            }
            
            val emojiId = db.insert(TABLE_EMOJIS, null, values)
            
            // Insert search keywords
            for (j in 0 until keywords.length()) {
                val keyword = keywords.getString(j)
                val searchValues = ContentValues().apply {
                    put(COL_EMOJI_ID, emojiId)
                    put(COL_KEYWORD, keyword.lowercase())
                }
                db.insert(TABLE_EMOJI_SEARCH, null, searchValues)
            }
            
            // Also add description as searchable
            val descValues = ContentValues().apply {
                put(COL_EMOJI_ID, emojiId)
                put(COL_KEYWORD, description.lowercase())
            }
            db.insert(TABLE_EMOJI_SEARCH, null, descValues)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting emoji: ${emojiObj}", e)
        }
    }
    
    private fun loadFallbackEmojis(db: SQLiteDatabase) {
        Log.d(TAG, "Loading fallback emoji data")
        
        val fallbackEmojis = listOf(
            Triple("😀", "grinning face", "SMILEYS_EMOTION"),
            Triple("😃", "grinning face with big eyes", "SMILEYS_EMOTION"),
            Triple("😄", "grinning face with smiling eyes", "SMILEYS_EMOTION"),
            Triple("😁", "beaming face with smiling eyes", "SMILEYS_EMOTION"),
            Triple("😆", "grinning squinting face", "SMILEYS_EMOTION"),
            Triple("😅", "grinning face with sweat", "SMILEYS_EMOTION"),
            Triple("🤣", "rolling on the floor laughing", "SMILEYS_EMOTION"),
            Triple("😂", "face with tears of joy", "SMILEYS_EMOTION"),
            Triple("🙂", "slightly smiling face", "SMILEYS_EMOTION"),
            Triple("🙃", "upside-down face", "SMILEYS_EMOTION"),
            Triple("😉", "winking face", "SMILEYS_EMOTION"),
            Triple("😊", "smiling face with smiling eyes", "SMILEYS_EMOTION"),
            Triple("😇", "smiling face with halo", "SMILEYS_EMOTION"),
            Triple("🥰", "smiling face with hearts", "SMILEYS_EMOTION"),
            Triple("😍", "smiling face with heart-eyes", "SMILEYS_EMOTION"),
            Triple("🤩", "star-struck", "SMILEYS_EMOTION"),
            Triple("😘", "face blowing a kiss", "SMILEYS_EMOTION"),
            Triple("😗", "kissing face", "SMILEYS_EMOTION"),
            Triple("☺️", "smiling face", "SMILEYS_EMOTION"),
            Triple("😚", "kissing face with closed eyes", "SMILEYS_EMOTION"),
            Triple("😙", "kissing face with smiling eyes", "SMILEYS_EMOTION"),
            Triple("👋", "waving hand", "PEOPLE_BODY"),
            Triple("🤚", "raised back of hand", "PEOPLE_BODY"),
            Triple("🖐️", "hand with fingers splayed", "PEOPLE_BODY"),
            Triple("✋", "raised hand", "PEOPLE_BODY"),
            Triple("🖖", "vulcan salute", "PEOPLE_BODY"),
            Triple("👌", "OK hand", "PEOPLE_BODY"),
            Triple("🤌", "pinched fingers", "PEOPLE_BODY"),
            Triple("🤏", "pinching hand", "PEOPLE_BODY"),
            Triple("✌️", "victory hand", "PEOPLE_BODY"),
            Triple("🤞", "crossed fingers", "PEOPLE_BODY"),
            Triple("🤟", "love-you gesture", "PEOPLE_BODY"),
            Triple("🤘", "sign of the horns", "PEOPLE_BODY"),
            Triple("🤙", "call me hand", "PEOPLE_BODY"),
            Triple("👈", "backhand index pointing left", "PEOPLE_BODY"),
            Triple("👉", "backhand index pointing right", "PEOPLE_BODY"),
            Triple("👆", "backhand index pointing up", "PEOPLE_BODY"),
            Triple("🖕", "middle finger", "PEOPLE_BODY"),
            Triple("👇", "backhand index pointing down", "PEOPLE_BODY"),
            Triple("☝️", "index pointing up", "PEOPLE_BODY"),
            Triple("👍", "thumbs up", "PEOPLE_BODY"),
            Triple("👎", "thumbs down", "PEOPLE_BODY"),
            Triple("✊", "raised fist", "PEOPLE_BODY"),
            Triple("👊", "oncoming fist", "PEOPLE_BODY"),
            Triple("🤛", "left-facing fist", "PEOPLE_BODY"),
            Triple("🤜", "right-facing fist", "PEOPLE_BODY"),
            Triple("👏", "clapping hands", "PEOPLE_BODY"),
            Triple("🙌", "raising hands", "PEOPLE_BODY"),
            Triple("👐", "open hands", "PEOPLE_BODY"),
            Triple("🤲", "palms up together", "PEOPLE_BODY"),
            Triple("🤝", "handshake", "PEOPLE_BODY"),
            Triple("🙏", "folded hands", "PEOPLE_BODY"),
            Triple("❤️", "red heart", "SYMBOLS"),
            Triple("🧡", "orange heart", "SYMBOLS"),
            Triple("💛", "yellow heart", "SYMBOLS"),
            Triple("💚", "green heart", "SYMBOLS"),
            Triple("💙", "blue heart", "SYMBOLS"),
            Triple("💜", "purple heart", "SYMBOLS"),
            Triple("🖤", "black heart", "SYMBOLS"),
            Triple("🤍", "white heart", "SYMBOLS"),
            Triple("🤎", "brown heart", "SYMBOLS"),
            Triple("💔", "broken heart", "SYMBOLS"),
            Triple("❣️", "heart exclamation", "SYMBOLS"),
            Triple("💕", "two hearts", "SYMBOLS"),
            Triple("💞", "revolving hearts", "SYMBOLS"),
            Triple("💓", "beating heart", "SYMBOLS"),
            Triple("💗", "growing heart", "SYMBOLS"),
            Triple("💖", "sparkling heart", "SYMBOLS"),
            Triple("💘", "heart with arrow", "SYMBOLS"),
            Triple("💝", "heart with ribbon", "SYMBOLS"),
            Triple("💟", "heart decoration", "SYMBOLS"),
            Triple("☮️", "peace symbol", "SYMBOLS"),
            Triple("✝️", "latin cross", "SYMBOLS"),
            Triple("☪️", "star and crescent", "SYMBOLS"),
            Triple("🕉️", "om", "SYMBOLS"),
            Triple("☸️", "wheel of dharma", "SYMBOLS"),
            Triple("✡️", "star of David", "SYMBOLS"),
            Triple("🔯", "dotted six-pointed star", "SYMBOLS"),
            Triple("🕎", "menorah", "SYMBOLS"),
            Triple("☯️", "yin yang", "SYMBOLS"),
            Triple("☦️", "orthodox cross", "SYMBOLS"),
            Triple("🛐", "place of worship", "SYMBOLS"),
            Triple("⛎", "Ophiuchus", "SYMBOLS"),
            Triple("♈", "Aries", "SYMBOLS"),
            Triple("♉", "Taurus", "SYMBOLS"),
            Triple("♊", "Gemini", "SYMBOLS"),
            Triple("♋", "Cancer", "SYMBOLS"),
            Triple("♌", "Leo", "SYMBOLS"),
            Triple("♍", "Virgo", "SYMBOLS"),
            Triple("♎", "Libra", "SYMBOLS"),
            Triple("♏", "Scorpio", "SYMBOLS"),
            Triple("♐", "Sagittarius", "SYMBOLS"),
            Triple("♑", "Capricorn", "SYMBOLS"),
            Triple("♒", "Aquarius", "SYMBOLS"),
            Triple("♓", "Pisces", "SYMBOLS")
        )
        
        fallbackEmojis.forEach { (unicode, description, category) ->
            val values = ContentValues().apply {
                put(COL_UNICODE, unicode)
                put(COL_DESCRIPTION, description)
                put(COL_CATEGORY, category)
                put(COL_KEYWORDS, "[]")
                put(COL_SKIN_VARIANTS, "")
                put(COL_USAGE_COUNT, 0)
                put(COL_LAST_USED, 0L)
            }
            db.insert(TABLE_EMOJIS, null, values)
        }
    }
    
    /**
     * Search emojis by keyword or description
     */
    fun searchEmojis(query: String, limit: Int = 50): List<EmojiData> {
        val cursor = readableDatabase.rawQuery("""
            SELECT DISTINCT e.$COL_UNICODE, e.$COL_DESCRIPTION, e.$COL_CATEGORY, 
                   e.$COL_KEYWORDS, e.$COL_SKIN_VARIANTS, e.$COL_USAGE_COUNT, e.$COL_LAST_USED
            FROM $TABLE_EMOJIS e
            LEFT JOIN $TABLE_EMOJI_SEARCH es ON e.$COL_ID = es.$COL_EMOJI_ID
            WHERE e.$COL_DESCRIPTION LIKE ? OR es.$COL_KEYWORD LIKE ?
            ORDER BY e.$COL_USAGE_COUNT DESC, e.$COL_LAST_USED DESC
            LIMIT ?
        """, arrayOf("%${query.lowercase()}%", "%${query.lowercase()}%", limit.toString()))
        
        return cursorToEmojiList(cursor)
    }
    
    /**
     * Get emojis by category
     */
    fun getEmojisByCategory(category: EmojiCategory, limit: Int = 100): List<EmojiData> {
        val cursor = readableDatabase.query(
            TABLE_EMOJIS,
            null,
            "$COL_CATEGORY = ?",
            arrayOf(category.name),
            null, null,
            "$COL_USAGE_COUNT DESC, $COL_LAST_USED DESC",
            limit.toString()
        )
        
        return cursorToEmojiList(cursor)
    }
    
    /**
     * Get recently used emojis
     */
    fun getRecentlyUsedEmojis(limit: Int = 30): List<EmojiData> {
        val cursor = readableDatabase.query(
            TABLE_EMOJIS,
            null,
            "$COL_LAST_USED > 0",
            null,
            null, null,
            "$COL_LAST_USED DESC",
            limit.toString()
        )
        
        return cursorToEmojiList(cursor)
    }
    
    /**
     * Get frequently used emojis
     */
    fun getFrequentlyUsedEmojis(limit: Int = 30): List<EmojiData> {
        val cursor = readableDatabase.query(
            TABLE_EMOJIS,
            null,
            "$COL_USAGE_COUNT > 0",
            null,
            null, null,
            "$COL_USAGE_COUNT DESC",
            limit.toString()
        )
        
        return cursorToEmojiList(cursor)
    }
    
    /**
     * Record emoji usage
     */
    fun recordEmojiUsage(unicode: String) {
        val currentTime = System.currentTimeMillis()
        writableDatabase.execSQL("""
            UPDATE $TABLE_EMOJIS 
            SET $COL_USAGE_COUNT = $COL_USAGE_COUNT + 1, $COL_LAST_USED = ?
            WHERE $COL_UNICODE = ?
        """, arrayOf(currentTime.toString(), unicode))
        
        Log.d(TAG, "Recorded usage for emoji: $unicode")
    }
    
    /**
     * Clean up old usage data to maintain performance
     */
    fun cleanupOldUsageData(maxRecords: Int) {
        writableDatabase.execSQL("""
            UPDATE $TABLE_EMOJIS 
            SET $COL_USAGE_COUNT = 0, $COL_LAST_USED = 0
            WHERE $COL_ID NOT IN (
                SELECT $COL_ID FROM $TABLE_EMOJIS 
                ORDER BY $COL_LAST_USED DESC 
                LIMIT ?
            )
        """, arrayOf(maxRecords.toString()))
    }
    
    private fun cursorToEmojiList(cursor: Cursor): List<EmojiData> {
        val results = mutableListOf<EmojiData>()
        
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val unicode = it.getString(it.getColumnIndexOrThrow(COL_UNICODE))
                    val description = it.getString(it.getColumnIndexOrThrow(COL_DESCRIPTION))
                    val categoryStr = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY))
                    val keywordsStr = it.getString(it.getColumnIndexOrThrow(COL_KEYWORDS))
                    val skinVariantsStr = it.getString(it.getColumnIndexOrThrow(COL_SKIN_VARIANTS))
                    val usageCount = it.getInt(it.getColumnIndexOrThrow(COL_USAGE_COUNT))
                    val lastUsed = it.getLong(it.getColumnIndexOrThrow(COL_LAST_USED))
                    
                    val category = try {
                        EmojiCategory.valueOf(categoryStr)
                    } catch (e: IllegalArgumentException) {
                        EmojiCategory.SYMBOLS
                    }
                    
                    val keywords = try {
                        val keywordsArray = JSONArray(keywordsStr)
                        (0 until keywordsArray.length()).map { i -> keywordsArray.getString(i) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    val skinVariants = try {
                        if (skinVariantsStr.isNotEmpty()) {
                            val variantsArray = JSONArray(skinVariantsStr)
                            (0 until variantsArray.length()).map { i -> variantsArray.getString(i) }
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    results.add(EmojiData(
                        unicode = unicode,
                        description = description,
                        category = category,
                        keywords = keywords,
                        skinToneVariants = skinVariants,
                        frequency = usageCount,
                        lastUsed = lastUsed
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing emoji from cursor", e)
                }
            }
        }
        
        return results
    }
    
    /**
     * Get category display information
     */
    fun getCategoryInfo(): Map<EmojiCategory, Pair<String, String>> {
        return mapOf(
            EmojiCategory.SMILEYS_EMOTION to Pair("😊", "Smileys & Emotion"),
            EmojiCategory.PEOPLE_BODY to Pair("👤", "People & Body"),
            EmojiCategory.ANIMALS_NATURE to Pair("🐶", "Animals & Nature"),
            EmojiCategory.FOOD_DRINK to Pair("🍔", "Food & Drink"),
            EmojiCategory.ACTIVITIES to Pair("⚽", "Activities"),
            EmojiCategory.TRAVEL_PLACES to Pair("🚗", "Travel & Places"),
            EmojiCategory.OBJECTS to Pair("💡", "Objects"),
            EmojiCategory.SYMBOLS to Pair("❤️", "Symbols"),
            EmojiCategory.FLAGS to Pair("🏁", "Flags"),
            EmojiCategory.RECENTLY_USED to Pair("⏰", "Recent"),
            EmojiCategory.FREQUENTLY_USED to Pair("⭐", "Frequent")
        )
    }
}
