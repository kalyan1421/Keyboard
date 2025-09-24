package com.example.ai_keyboard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

class WordDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val TAG = "WordDatabase"
        private const val DATABASE_NAME = "ai_keyboard_words.db"
        private const val DATABASE_VERSION = 1
        
        // Table names
        private const val TABLE_WORDS = "words"
        private const val TABLE_BIGRAMS = "bigrams"
        private const val TABLE_TRIGRAMS = "trigrams"
        private const val TABLE_USER_WORDS = "user_words"
        
        // Words table columns
        private const val WORD_ID = "id"
        private const val WORD_TEXT = "word"
        private const val WORD_FREQUENCY = "frequency"
        private const val WORD_LENGTH = "length"
        private const val WORD_IS_COMMON = "is_common"
        
        // Bigrams table columns
        private const val BIGRAM_ID = "id"
        private const val BIGRAM_WORD1 = "word1"
        private const val BIGRAM_WORD2 = "word2"
        private const val BIGRAM_FREQUENCY = "frequency"
        
        // Trigrams table columns
        private const val TRIGRAM_ID = "id"
        private const val TRIGRAM_WORD1 = "word1"
        private const val TRIGRAM_WORD2 = "word2"
        private const val TRIGRAM_WORD3 = "word3"
        private const val TRIGRAM_FREQUENCY = "frequency"
        
        // User words table columns
        private const val USER_WORD_ID = "id"
        private const val USER_WORD_TEXT = "word"
        private const val USER_WORD_FREQUENCY = "frequency"
        private const val USER_WORD_LAST_USED = "last_used"
        private const val USER_WORD_USER_ADDED = "user_added"
        
        @Volatile
        private var INSTANCE: WordDatabase? = null
        
        fun getInstance(context: Context): WordDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WordDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
        createIndexes(db)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BIGRAMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRIGRAMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_WORDS")
        onCreate(db)
    }
    
    private fun createTables(db: SQLiteDatabase) {
        // Words table
        val createWordsTable = """
            CREATE TABLE $TABLE_WORDS (
                $WORD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $WORD_TEXT TEXT UNIQUE NOT NULL,
                $WORD_FREQUENCY INTEGER DEFAULT 1,
                $WORD_LENGTH INTEGER NOT NULL,
                $WORD_IS_COMMON INTEGER DEFAULT 0
            )
        """.trimIndent()
        
        // Bigrams table
        val createBigramsTable = """
            CREATE TABLE $TABLE_BIGRAMS (
                $BIGRAM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $BIGRAM_WORD1 TEXT NOT NULL,
                $BIGRAM_WORD2 TEXT NOT NULL,
                $BIGRAM_FREQUENCY INTEGER DEFAULT 1,
                UNIQUE($BIGRAM_WORD1, $BIGRAM_WORD2)
            )
        """.trimIndent()
        
        // Trigrams table
        val createTrigramsTable = """
            CREATE TABLE $TABLE_TRIGRAMS (
                $TRIGRAM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $TRIGRAM_WORD1 TEXT NOT NULL,
                $TRIGRAM_WORD2 TEXT NOT NULL,
                $TRIGRAM_WORD3 TEXT NOT NULL,
                $TRIGRAM_FREQUENCY INTEGER DEFAULT 1,
                UNIQUE($TRIGRAM_WORD1, $TRIGRAM_WORD2, $TRIGRAM_WORD3)
            )
        """.trimIndent()
        
        // User words table
        val createUserWordsTable = """
            CREATE TABLE $TABLE_USER_WORDS (
                $USER_WORD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $USER_WORD_TEXT TEXT UNIQUE NOT NULL,
                $USER_WORD_FREQUENCY INTEGER DEFAULT 1,
                $USER_WORD_LAST_USED DATETIME DEFAULT CURRENT_TIMESTAMP,
                $USER_WORD_USER_ADDED INTEGER DEFAULT 0
            )
        """.trimIndent()
        
        db.execSQL(createWordsTable)
        db.execSQL(createBigramsTable)
        db.execSQL(createTrigramsTable)
        db.execSQL(createUserWordsTable)
    }
    
    private fun createIndexes(db: SQLiteDatabase) {
        // Create indexes for faster queries
        db.execSQL("CREATE INDEX idx_words_text ON $TABLE_WORDS($WORD_TEXT)")
        db.execSQL("CREATE INDEX idx_words_length ON $TABLE_WORDS($WORD_LENGTH)")
        db.execSQL("CREATE INDEX idx_words_frequency ON $TABLE_WORDS($WORD_FREQUENCY DESC)")
        db.execSQL("CREATE INDEX idx_bigrams_word1 ON $TABLE_BIGRAMS($BIGRAM_WORD1)")
        db.execSQL("CREATE INDEX idx_trigrams_word12 ON $TABLE_TRIGRAMS($TRIGRAM_WORD1, $TRIGRAM_WORD2)")
        db.execSQL("CREATE INDEX idx_user_words_text ON $TABLE_USER_WORDS($USER_WORD_TEXT)")
        db.execSQL("CREATE INDEX idx_user_words_frequency ON $TABLE_USER_WORDS($USER_WORD_FREQUENCY DESC)")
    }
    
    fun populateInitialData(context: Context) {
        if (isDataPopulated()) {
            Log.d(TAG, "Database already populated")
            return
        }
        
        Log.d(TAG, "Populating initial word data...")
        
        try {
            populateWordsFromAssets(context)
            populateCorrectionsFromAssets(context)
            markDataAsPopulated()
            Log.d(TAG, "Initial data population completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error populating initial data", e)
        }
    }
    
    private fun populateWordsFromAssets(context: Context) {
        try {
            val inputStream = context.assets.open("dictionaries/common_words.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            
            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            val wordsArray = jsonObject.getJSONArray("words")
            val frequencyObject = jsonObject.optJSONObject("frequency")
            
            val db = writableDatabase
            db.beginTransaction()
            
            try {
                for (i in 0 until wordsArray.length()) {
                    val word = wordsArray.getString(i).lowercase()
                    val frequency = frequencyObject?.optInt(word, 100) ?: 100
                    val isCommon = if (frequency > 1000) 1 else 0
                    
                    val values = ContentValues().apply {
                        put(WORD_TEXT, word)
                        put(WORD_FREQUENCY, frequency)
                        put(WORD_LENGTH, word.length)
                        put(WORD_IS_COMMON, isCommon)
                    }
                    
                    db.insertOrThrow(TABLE_WORDS, null, values)
                }
                
                db.setTransactionSuccessful()
                Log.d(TAG, "Populated ${wordsArray.length()} words")
            } finally {
                db.endTransaction()
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Error reading common words file", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error populating words", e)
        }
    }
    
    private fun populateCorrectionsFromAssets(context: Context) {
        try {
            val inputStream = context.assets.open("dictionaries/corrections.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            
            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            val correctionsObject = jsonObject.getJSONObject("corrections")
            
            val db = writableDatabase
            db.beginTransaction()
            
            try {
                val keys = correctionsObject.keys()
                while (keys.hasNext()) {
                    val misspelling = keys.next().lowercase()
                    val correction = correctionsObject.getString(misspelling).lowercase()
                    
                    // Add misspelling as a word with low frequency
                    val misspellingValues = ContentValues().apply {
                        put(WORD_TEXT, misspelling)
                        put(WORD_FREQUENCY, 1)
                        put(WORD_LENGTH, misspelling.length)
                        put(WORD_IS_COMMON, 0)
                    }
                    
                    try {
                        db.insertOrThrow(TABLE_WORDS, null, misspellingValues)
                    } catch (e: Exception) {
                        // Word might already exist, ignore
                    }
                    
                    // Ensure correction exists with higher frequency
                    val correctionValues = ContentValues().apply {
                        put(WORD_TEXT, correction)
                        put(WORD_FREQUENCY, 1000)
                        put(WORD_LENGTH, correction.length)
                        put(WORD_IS_COMMON, 1)
                    }
                    
                    try {
                        db.insertOrThrow(TABLE_WORDS, null, correctionValues)
                    } catch (e: Exception) {
                        // Update existing word frequency
                        db.execSQL("UPDATE $TABLE_WORDS SET $WORD_FREQUENCY = MAX($WORD_FREQUENCY, 1000) WHERE $WORD_TEXT = ?", arrayOf(correction))
                    }
                }
                
                db.setTransactionSuccessful()
                Log.d(TAG, "Populated correction mappings")
            } finally {
                db.endTransaction()
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Error reading corrections file", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error populating corrections", e)
        }
    }
    
    private fun isDataPopulated(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_WORDS", null)
        cursor.use {
            if (it.moveToFirst()) {
                val count = it.getInt(0)
                return count > 0
            }
        }
        return false
    }
    
    private fun markDataAsPopulated() {
        // This could be implemented using SharedPreferences or a settings table
        // For now, we just rely on the word count check
    }
    
    // Word operations
    fun addWord(word: String, frequency: Int = 1, isUserAdded: Boolean = false): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(WORD_TEXT, word.lowercase())
            put(WORD_FREQUENCY, frequency)
            put(WORD_LENGTH, word.length)
            put(WORD_IS_COMMON, 0)
        }
        
        val result = db.insertWithOnConflict(TABLE_WORDS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        
        if (isUserAdded) {
            addUserWord(word)
        }
        
        return result != -1L
    }
    
    fun updateWordFrequency(word: String) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_WORDS SET $WORD_FREQUENCY = $WORD_FREQUENCY + 1 WHERE $WORD_TEXT = ?", arrayOf(word.lowercase()))
        updateUserWordFrequency(word)
    }
    
    fun getWordsStartingWith(prefix: String, limit: Int = 10): List<WordSuggestion> {
        val suggestions = mutableListOf<WordSuggestion>()
        val db = readableDatabase
        
        val query = """
            SELECT $WORD_TEXT, $WORD_FREQUENCY, $WORD_IS_COMMON
            FROM $TABLE_WORDS
            WHERE $WORD_TEXT LIKE ? AND $WORD_LENGTH > ?
            ORDER BY $WORD_FREQUENCY DESC, $WORD_IS_COMMON DESC
            LIMIT ?
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf("${prefix.lowercase()}%", prefix.length.toString(), limit.toString()))
        
        cursor.use {
            while (it.moveToNext()) {
                val word = it.getString(0)
                val frequency = it.getInt(1)
                val isCommon = it.getInt(2) == 1
                
                suggestions.add(WordSuggestion(
                    word = word,
                    frequency = frequency,
                    isCommon = isCommon,
                    type = SuggestionType.COMPLETION
                ))
            }
        }
        
        return suggestions
    }
    
    fun getWordsByLength(length: Int, limit: Int = 100): List<String> {
        val words = mutableListOf<String>()
        val db = readableDatabase
        
        val query = """
            SELECT $WORD_TEXT
            FROM $TABLE_WORDS
            WHERE $WORD_LENGTH = ?
            ORDER BY $WORD_FREQUENCY DESC
            LIMIT ?
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf(length.toString(), limit.toString()))
        
        cursor.use {
            while (it.moveToNext()) {
                words.add(it.getString(0))
            }
        }
        
        return words
    }
    
    fun wordExists(word: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_WORDS WHERE $WORD_TEXT = ? LIMIT 1", arrayOf(word.lowercase()))
        cursor.use {
            return it.moveToFirst()
        }
    }
    
    fun getWordFrequency(word: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $WORD_FREQUENCY FROM $TABLE_WORDS WHERE $WORD_TEXT = ? LIMIT 1", arrayOf(word.lowercase()))
        cursor.use {
            return if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                0
            }
        }
    }
    
    // Bigram operations
    fun addBigram(word1: String, word2: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(BIGRAM_WORD1, word1.lowercase())
            put(BIGRAM_WORD2, word2.lowercase())
            put(BIGRAM_FREQUENCY, 1)
        }
        
        val result = db.insertWithOnConflict(TABLE_BIGRAMS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        
        if (result == -1L) {
            // Bigram exists, increment frequency
            db.execSQL(
                "UPDATE $TABLE_BIGRAMS SET $BIGRAM_FREQUENCY = $BIGRAM_FREQUENCY + 1 WHERE $BIGRAM_WORD1 = ? AND $BIGRAM_WORD2 = ?",
                arrayOf(word1.lowercase(), word2.lowercase())
            )
        }
    }
    
    fun getBigramPredictions(word1: String, prefix: String = "", limit: Int = 5): List<WordSuggestion> {
        val suggestions = mutableListOf<WordSuggestion>()
        val db = readableDatabase
        
        val query = if (prefix.isNotEmpty()) {
            """
                SELECT $BIGRAM_WORD2, $BIGRAM_FREQUENCY
                FROM $TABLE_BIGRAMS
                WHERE $BIGRAM_WORD1 = ? AND $BIGRAM_WORD2 LIKE ?
                ORDER BY $BIGRAM_FREQUENCY DESC
                LIMIT ?
            """.trimIndent()
        } else {
            """
                SELECT $BIGRAM_WORD2, $BIGRAM_FREQUENCY
                FROM $TABLE_BIGRAMS
                WHERE $BIGRAM_WORD1 = ?
                ORDER BY $BIGRAM_FREQUENCY DESC
                LIMIT ?
            """.trimIndent()
        }
        
        val args = if (prefix.isNotEmpty()) {
            arrayOf(word1.lowercase(), "${prefix.lowercase()}%", limit.toString())
        } else {
            arrayOf(word1.lowercase(), limit.toString())
        }
        
        val cursor = db.rawQuery(query, args)
        
        cursor.use {
            while (it.moveToNext()) {
                val word = it.getString(0)
                val frequency = it.getInt(1)
                
                suggestions.add(WordSuggestion(
                    word = word,
                    frequency = frequency,
                    isCommon = false,
                    type = SuggestionType.BIGRAM
                ))
            }
        }
        
        return suggestions
    }
    
    // Trigram operations
    fun addTrigram(word1: String, word2: String, word3: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(TRIGRAM_WORD1, word1.lowercase())
            put(TRIGRAM_WORD2, word2.lowercase())
            put(TRIGRAM_WORD3, word3.lowercase())
            put(TRIGRAM_FREQUENCY, 1)
        }
        
        val result = db.insertWithOnConflict(TABLE_TRIGRAMS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        
        if (result == -1L) {
            // Trigram exists, increment frequency
            db.execSQL(
                "UPDATE $TABLE_TRIGRAMS SET $TRIGRAM_FREQUENCY = $TRIGRAM_FREQUENCY + 1 WHERE $TRIGRAM_WORD1 = ? AND $TRIGRAM_WORD2 = ? AND $TRIGRAM_WORD3 = ?",
                arrayOf(word1.lowercase(), word2.lowercase(), word3.lowercase())
            )
        }
    }
    
    fun getTrigramPredictions(word1: String, word2: String, prefix: String = "", limit: Int = 3): List<WordSuggestion> {
        val suggestions = mutableListOf<WordSuggestion>()
        val db = readableDatabase
        
        val query = if (prefix.isNotEmpty()) {
            """
                SELECT $TRIGRAM_WORD3, $TRIGRAM_FREQUENCY
                FROM $TABLE_TRIGRAMS
                WHERE $TRIGRAM_WORD1 = ? AND $TRIGRAM_WORD2 = ? AND $TRIGRAM_WORD3 LIKE ?
                ORDER BY $TRIGRAM_FREQUENCY DESC
                LIMIT ?
            """.trimIndent()
        } else {
            """
                SELECT $TRIGRAM_WORD3, $TRIGRAM_FREQUENCY
                FROM $TABLE_TRIGRAMS
                WHERE $TRIGRAM_WORD1 = ? AND $TRIGRAM_WORD2 = ?
                ORDER BY $TRIGRAM_FREQUENCY DESC
                LIMIT ?
            """.trimIndent()
        }
        
        val args = if (prefix.isNotEmpty()) {
            arrayOf(word1.lowercase(), word2.lowercase(), "${prefix.lowercase()}%", limit.toString())
        } else {
            arrayOf(word1.lowercase(), word2.lowercase(), limit.toString())
        }
        
        val cursor = db.rawQuery(query, args)
        
        cursor.use {
            while (it.moveToNext()) {
                val word = it.getString(0)
                val frequency = it.getInt(1)
                
                suggestions.add(WordSuggestion(
                    word = word,
                    frequency = frequency,
                    isCommon = false,
                    type = SuggestionType.TRIGRAM
                ))
            }
        }
        
        return suggestions
    }
    
    // User words operations
    private fun addUserWord(word: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(USER_WORD_TEXT, word.lowercase())
            put(USER_WORD_FREQUENCY, 1)
            put(USER_WORD_LAST_USED, System.currentTimeMillis())
            put(USER_WORD_USER_ADDED, 1)
        }
        
        db.insertWithOnConflict(TABLE_USER_WORDS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }
    
    private fun updateUserWordFrequency(word: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(USER_WORD_TEXT, word.lowercase())
            put(USER_WORD_FREQUENCY, 1)
            put(USER_WORD_LAST_USED, System.currentTimeMillis())
            put(USER_WORD_USER_ADDED, 0)
        }
        
        val result = db.insertWithOnConflict(TABLE_USER_WORDS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        
        if (result == -1L) {
            // User word exists, update frequency and last used time
            db.execSQL(
                "UPDATE $TABLE_USER_WORDS SET $USER_WORD_FREQUENCY = $USER_WORD_FREQUENCY + 1, $USER_WORD_LAST_USED = ? WHERE $USER_WORD_TEXT = ?",
                arrayOf(System.currentTimeMillis().toString(), word.lowercase())
            )
        }
    }
    
    fun getUserWordSuggestions(prefix: String, limit: Int = 5): List<WordSuggestion> {
        val suggestions = mutableListOf<WordSuggestion>()
        val db = readableDatabase
        
        val query = """
            SELECT $USER_WORD_TEXT, $USER_WORD_FREQUENCY, $USER_WORD_USER_ADDED
            FROM $TABLE_USER_WORDS
            WHERE $USER_WORD_TEXT LIKE ? AND $USER_WORD_FREQUENCY > 1
            ORDER BY $USER_WORD_FREQUENCY DESC, $USER_WORD_LAST_USED DESC
            LIMIT ?
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf("${prefix.lowercase()}%", limit.toString()))
        
        cursor.use {
            while (it.moveToNext()) {
                val word = it.getString(0)
                val frequency = it.getInt(1)
                val userAdded = it.getInt(2) == 1
                
                suggestions.add(WordSuggestion(
                    word = word,
                    frequency = frequency,
                    isCommon = userAdded,
                    type = SuggestionType.USER
                ))
            }
        }
        
        return suggestions
    }
    
    // Learning from user input
    fun learnFromSentence(words: List<String>) {
        if (words.size < 2) return
        
        val db = writableDatabase
        db.beginTransaction()
        
        try {
            // Update word frequencies
            for (word in words) {
                updateWordFrequency(word)
            }
            
            // Update bigrams
            for (i in 0 until words.size - 1) {
                addBigram(words[i], words[i + 1])
            }
            
            // Update trigrams
            for (i in 0 until words.size - 2) {
                addTrigram(words[i], words[i + 1], words[i + 2])
            }
            
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
    
    // Cleanup operations
    fun cleanupOldUserData() {
        val db = writableDatabase
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        
        // Remove old user words with low frequency
        db.delete(
            TABLE_USER_WORDS,
            "$USER_WORD_FREQUENCY = 1 AND $USER_WORD_LAST_USED < ? AND $USER_WORD_USER_ADDED = 0",
            arrayOf(thirtyDaysAgo.toString())
        )
        
        // Vacuum database to reclaim space
        db.execSQL("VACUUM")
    }
}

data class WordSuggestion(
    val word: String,
    val frequency: Int,
    val isCommon: Boolean,
    val type: SuggestionType
)

enum class SuggestionType {
    COMPLETION,
    CORRECTION,
    BIGRAM,
    TRIGRAM,
    USER
}

/**
 * Extension to get words by prefix for swipe typing
 */
fun WordDatabase.getWordsByPrefix(prefix: String, limit: Int = 50): List<String> {
    val words = mutableListOf<String>()
    val db = readableDatabase
    val cursor = db.rawQuery(
        "SELECT word_text FROM words WHERE word_text LIKE ? ORDER BY frequency DESC LIMIT ?",
        arrayOf("${prefix.lowercase()}%", limit.toString())
    )
    cursor.use {
        while (it.moveToNext()) {
            words.add(it.getString(0))
        }
    }
    return words
}
