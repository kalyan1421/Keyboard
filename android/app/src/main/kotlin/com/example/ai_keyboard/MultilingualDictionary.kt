package com.example.ai_keyboard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Multilingual dictionary database for word suggestions and corrections
 */
class MultilingualDictionary(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val TAG = "MultilingualDictionary"
        private const val DATABASE_NAME = "multilingual_dictionaries.db"
        private const val DATABASE_VERSION = 1
        
        // Tables
        private const val TABLE_WORDS = "words"
        private const val TABLE_CORRECTIONS = "corrections"
        private const val TABLE_BIGRAMS = "bigrams"
        private const val TABLE_USER_WORDS = "user_words"
        
        // Words table columns
        private const val COL_WORD_ID = "id"
        private const val COL_WORD_LANGUAGE = "language"
        private const val COL_WORD_TEXT = "word"
        private const val COL_WORD_FREQUENCY = "frequency"
        private const val COL_WORD_CATEGORY = "category"
        private const val COL_WORD_LENGTH = "length"
        
        // Corrections table columns
        private const val COL_CORR_ID = "id"
        private const val COL_CORR_LANGUAGE = "language"
        private const val COL_CORR_ERROR = "error_word"
        private const val COL_CORR_CORRECT = "correct_word"
        private const val COL_CORR_CONFIDENCE = "confidence"
        
        // Bigrams table columns
        private const val COL_BIGRAM_ID = "id"
        private const val COL_BIGRAM_LANGUAGE = "language"
        private const val COL_BIGRAM_WORD1 = "word1"
        private const val COL_BIGRAM_WORD2 = "word2"
        private const val COL_BIGRAM_FREQUENCY = "frequency"
        
        // User words table columns
        private const val COL_USER_ID = "id"
        private const val COL_USER_LANGUAGE = "language"
        private const val COL_USER_WORD = "word"
        private const val COL_USER_FREQUENCY = "frequency"
        private const val COL_USER_ADDED_TIME = "added_time"
    }
    
    private val context: Context = context.applicationContext
    private val loadedLanguages = ConcurrentHashMap<String, Boolean>()
    private val dictionaryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate(db: SQLiteDatabase) {
        // Create words table
        db.execSQL("""
            CREATE TABLE $TABLE_WORDS (
                $COL_WORD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WORD_LANGUAGE TEXT NOT NULL,
                $COL_WORD_TEXT TEXT NOT NULL,
                $COL_WORD_FREQUENCY INTEGER DEFAULT 1,
                $COL_WORD_CATEGORY TEXT,
                $COL_WORD_LENGTH INTEGER,
                UNIQUE($COL_WORD_LANGUAGE, $COL_WORD_TEXT)
            )
        """)
        
        // Create corrections table
        db.execSQL("""
            CREATE TABLE $TABLE_CORRECTIONS (
                $COL_CORR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CORR_LANGUAGE TEXT NOT NULL,
                $COL_CORR_ERROR TEXT NOT NULL,
                $COL_CORR_CORRECT TEXT NOT NULL,
                $COL_CORR_CONFIDENCE REAL DEFAULT 1.0,
                UNIQUE($COL_CORR_LANGUAGE, $COL_CORR_ERROR, $COL_CORR_CORRECT)
            )
        """)
        
        // Create bigrams table
        db.execSQL("""
            CREATE TABLE $TABLE_BIGRAMS (
                $COL_BIGRAM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BIGRAM_LANGUAGE TEXT NOT NULL,
                $COL_BIGRAM_WORD1 TEXT NOT NULL,
                $COL_BIGRAM_WORD2 TEXT NOT NULL,
                $COL_BIGRAM_FREQUENCY INTEGER DEFAULT 1,
                UNIQUE($COL_BIGRAM_LANGUAGE, $COL_BIGRAM_WORD1, $COL_BIGRAM_WORD2)
            )
        """)
        
        // Create user words table
        db.execSQL("""
            CREATE TABLE $TABLE_USER_WORDS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_LANGUAGE TEXT NOT NULL,
                $COL_USER_WORD TEXT NOT NULL,
                $COL_USER_FREQUENCY INTEGER DEFAULT 1,
                $COL_USER_ADDED_TIME INTEGER DEFAULT 0,
                UNIQUE($COL_USER_LANGUAGE, $COL_USER_WORD)
            )
        """)
        
        // Create indexes for better performance
        createIndexes(db)
        
        Log.d(TAG, "Created multilingual dictionary database")
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop existing tables
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CORRECTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BIGRAMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_WORDS")
        
        // Recreate tables
        onCreate(db)
    }
    
    private fun createIndexes(db: SQLiteDatabase) {
        // Words table indexes
        db.execSQL("CREATE INDEX idx_words_language_word ON $TABLE_WORDS($COL_WORD_LANGUAGE, $COL_WORD_TEXT)")
        db.execSQL("CREATE INDEX idx_words_language_prefix ON $TABLE_WORDS($COL_WORD_LANGUAGE, $COL_WORD_TEXT)")
        db.execSQL("CREATE INDEX idx_words_frequency ON $TABLE_WORDS($COL_WORD_FREQUENCY DESC)")
        db.execSQL("CREATE INDEX idx_words_length ON $TABLE_WORDS($COL_WORD_LENGTH)")
        
        // Corrections table indexes
        db.execSQL("CREATE INDEX idx_corrections_language_error ON $TABLE_CORRECTIONS($COL_CORR_LANGUAGE, $COL_CORR_ERROR)")
        
        // Bigrams table indexes
        db.execSQL("CREATE INDEX idx_bigrams_language_word1 ON $TABLE_BIGRAMS($COL_BIGRAM_LANGUAGE, $COL_BIGRAM_WORD1)")
        db.execSQL("CREATE INDEX idx_bigrams_frequency ON $TABLE_BIGRAMS($COL_BIGRAM_FREQUENCY DESC)")
        
        // User words table indexes
        db.execSQL("CREATE INDEX idx_user_words_language ON $TABLE_USER_WORDS($COL_USER_LANGUAGE)")
        db.execSQL("CREATE INDEX idx_user_words_frequency ON $TABLE_USER_WORDS($COL_USER_FREQUENCY DESC)")
    }
    
    /**
     * Load dictionary for a specific language from assets
     */
    suspend fun loadLanguageDictionary(language: String): Boolean = withContext(Dispatchers.IO) {
        if (loadedLanguages[language] == true) {
            Log.d(TAG, "Dictionary for $language already loaded")
            return@withContext true
        }
        
        try {
            // Load words from assets
            loadWordsFromAssets(language)
            
            // Load corrections from assets
            loadCorrectionsFromAssets(language)
            
            // Load bigrams from assets
            loadBigramsFromAssets(language)
            
            loadedLanguages[language] = true
            Log.d(TAG, "Successfully loaded dictionary for language: $language")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dictionary for language: $language", e)
            return@withContext false
        }
    }
    
    private suspend fun loadWordsFromAssets(language: String) = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("dictionaries/${language}_words.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val db = writableDatabase
            
            db.beginTransaction()
            try {
                reader.useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.trim().split("\t")
                        if (parts.size >= 2) {
                            val word = parts[0].trim()
                            val frequency = parts[1].toIntOrNull() ?: 1
                            val category = if (parts.size > 2) parts[2] else null
                            
                            addWordToDatabase(db, language, word, frequency, category)
                        }
                    }
                }
                db.setTransactionSuccessful()
                Log.d(TAG, "Loaded words for language: $language")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load words for language: $language", e)
        }
    }
    
    private suspend fun loadCorrectionsFromAssets(language: String) = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("dictionaries/${language}_corrections.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val db = writableDatabase
            
            db.beginTransaction()
            try {
                reader.useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.trim().split("\t")
                        if (parts.size >= 2) {
                            val errorWord = parts[0].trim()
                            val correctWord = parts[1].trim()
                            val confidence = if (parts.size > 2) parts[2].toDoubleOrNull() ?: 1.0 else 1.0
                            
                            addCorrectionToDatabase(db, language, errorWord, correctWord, confidence)
                        }
                    }
                }
                db.setTransactionSuccessful()
                Log.d(TAG, "Loaded corrections for language: $language")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load corrections for language: $language", e)
        }
    }
    
    private suspend fun loadBigramsFromAssets(language: String) = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("dictionaries/${language}_bigrams.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val db = writableDatabase
            
            db.beginTransaction()
            try {
                reader.useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.trim().split("\t")
                        if (parts.size >= 3) {
                            val word1 = parts[0].trim()
                            val word2 = parts[1].trim()
                            val frequency = parts[2].toIntOrNull() ?: 1
                            
                            addBigramToDatabase(db, language, word1, word2, frequency)
                        }
                    }
                }
                db.setTransactionSuccessful()
                Log.d(TAG, "Loaded bigrams for language: $language")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load bigrams for language: $language", e)
        }
    }
    
    private fun addWordToDatabase(db: SQLiteDatabase, language: String, word: String, frequency: Int, category: String?) {
        val values = ContentValues().apply {
            put(COL_WORD_LANGUAGE, language)
            put(COL_WORD_TEXT, word)
            put(COL_WORD_FREQUENCY, frequency)
            put(COL_WORD_CATEGORY, category)
            put(COL_WORD_LENGTH, word.length)
        }
        
        db.insertWithOnConflict(TABLE_WORDS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }
    
    private fun addCorrectionToDatabase(db: SQLiteDatabase, language: String, errorWord: String, correctWord: String, confidence: Double) {
        val values = ContentValues().apply {
            put(COL_CORR_LANGUAGE, language)
            put(COL_CORR_ERROR, errorWord)
            put(COL_CORR_CORRECT, correctWord)
            put(COL_CORR_CONFIDENCE, confidence)
        }
        
        db.insertWithOnConflict(TABLE_CORRECTIONS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }
    
    private fun addBigramToDatabase(db: SQLiteDatabase, language: String, word1: String, word2: String, frequency: Int) {
        val values = ContentValues().apply {
            put(COL_BIGRAM_LANGUAGE, language)
            put(COL_BIGRAM_WORD1, word1)
            put(COL_BIGRAM_WORD2, word2)
            put(COL_BIGRAM_FREQUENCY, frequency)
        }
        
        db.insertWithOnConflict(TABLE_BIGRAMS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }
    
    /**
     * Get word suggestions for a given prefix
     */
    fun getWordSuggestions(language: String, prefix: String, limit: Int = 10): List<String> {
        if (prefix.isEmpty()) return emptyList()
        
        val cursor = readableDatabase.query(
            TABLE_WORDS,
            arrayOf(COL_WORD_TEXT),
            "$COL_WORD_LANGUAGE = ? AND $COL_WORD_TEXT LIKE ? COLLATE NOCASE",
            arrayOf(language, "$prefix%"),
            null, null,
            "$COL_WORD_FREQUENCY DESC, $COL_WORD_LENGTH ASC",
            limit.toString()
        )
        
        val suggestions = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                suggestions.add(it.getString(0))
            }
        }
        
        return suggestions
    }
    
    /**
     * Get corrections for a misspelled word
     */
    fun getCorrections(language: String, word: String): List<Correction> {
        val cursor = readableDatabase.query(
            TABLE_CORRECTIONS,
            arrayOf(COL_CORR_CORRECT, COL_CORR_CONFIDENCE),
            "$COL_CORR_LANGUAGE = ? AND $COL_CORR_ERROR = ? COLLATE NOCASE",
            arrayOf(language, word),
            null, null,
            "$COL_CORR_CONFIDENCE DESC"
        )
        
        val corrections = mutableListOf<Correction>()
        cursor.use {
            while (it.moveToNext()) {
                corrections.add(Correction(
                    originalWord = word,
                    correctedWord = it.getString(0),
                    confidence = it.getDouble(1),
                    language = language
                ))
            }
        }
        
        return corrections
    }
    
    /**
     * Get next word predictions based on previous word
     */
    fun getNextWordPredictions(language: String, previousWord: String, limit: Int = 5): List<String> {
        val cursor = readableDatabase.query(
            TABLE_BIGRAMS,
            arrayOf(COL_BIGRAM_WORD2),
            "$COL_BIGRAM_LANGUAGE = ? AND $COL_BIGRAM_WORD1 = ? COLLATE NOCASE",
            arrayOf(language, previousWord),
            null, null,
            "$COL_BIGRAM_FREQUENCY DESC",
            limit.toString()
        )
        
        val predictions = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                predictions.add(it.getString(0))
            }
        }
        
        return predictions
    }
    
    /**
     * Add user word to dictionary
     */
    fun addUserWord(language: String, word: String) {
        val currentTime = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_USER_LANGUAGE, language)
            put(COL_USER_WORD, word)
            put(COL_USER_FREQUENCY, 1)
            put(COL_USER_ADDED_TIME, currentTime)
        }
        
        val db = writableDatabase
        val rowsAffected = db.insertWithOnConflict(TABLE_USER_WORDS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        
        if (rowsAffected == -1L) {
            // Word already exists, increment frequency
            db.execSQL(
                "UPDATE $TABLE_USER_WORDS SET $COL_USER_FREQUENCY = $COL_USER_FREQUENCY + 1 WHERE $COL_USER_LANGUAGE = ? AND $COL_USER_WORD = ?",
                arrayOf(language, word)
            )
        }
        
        Log.d(TAG, "Added user word: $word for language: $language")
    }
    
    /**
     * Get user words for suggestions
     */
    fun getUserWordSuggestions(language: String, prefix: String, limit: Int = 5): List<String> {
        if (prefix.isEmpty()) return emptyList()
        
        val cursor = readableDatabase.query(
            TABLE_USER_WORDS,
            arrayOf(COL_USER_WORD),
            "$COL_USER_LANGUAGE = ? AND $COL_USER_WORD LIKE ? COLLATE NOCASE",
            arrayOf(language, "$prefix%"),
            null, null,
            "$COL_USER_FREQUENCY DESC, $COL_USER_ADDED_TIME DESC",
            limit.toString()
        )
        
        val suggestions = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                suggestions.add(it.getString(0))
            }
        }
        
        return suggestions
    }
    
    /**
     * Get combined suggestions (dictionary + user words)
     */
    fun getCombinedSuggestions(language: String, prefix: String, limit: Int = 10): List<String> {
        val dictionarySuggestions = getWordSuggestions(language, prefix, limit)
        val userSuggestions = getUserWordSuggestions(language, prefix, limit / 2)
        
        // Combine and deduplicate
        val combined = (userSuggestions + dictionarySuggestions).distinct()
        return combined.take(limit)
    }
    
    /**
     * Check if language dictionary is loaded
     */
    fun isLanguageLoaded(language: String): Boolean {
        return loadedLanguages[language] == true
    }
    
    /**
     * Preload multiple languages asynchronously
     */
    fun preloadLanguages(languages: Set<String>) {
        languages.forEach { language ->
            dictionaryScope.launch {
                loadLanguageDictionary(language)
            }
        }
    }
    
    /**
     * Clear all data for a specific language
     */
    fun clearLanguageData(language: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_WORDS, "$COL_WORD_LANGUAGE = ?", arrayOf(language))
            db.delete(TABLE_CORRECTIONS, "$COL_CORR_LANGUAGE = ?", arrayOf(language))
            db.delete(TABLE_BIGRAMS, "$COL_BIGRAM_LANGUAGE = ?", arrayOf(language))
            db.delete(TABLE_USER_WORDS, "$COL_USER_LANGUAGE = ?", arrayOf(language))
            db.setTransactionSuccessful()
            
            loadedLanguages.remove(language)
            Log.d(TAG, "Cleared data for language: $language")
        } finally {
            db.endTransaction()
        }
    }
    
    /**
     * Get database statistics
     */
    fun getDatabaseStats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_WORDS", null).use {
            if (it.moveToFirst()) stats["total_words"] = it.getInt(0)
        }
        
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_CORRECTIONS", null).use {
            if (it.moveToFirst()) stats["total_corrections"] = it.getInt(0)
        }
        
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_BIGRAMS", null).use {
            if (it.moveToFirst()) stats["total_bigrams"] = it.getInt(0)
        }
        
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_USER_WORDS", null).use {
            if (it.moveToFirst()) stats["total_user_words"] = it.getInt(0)
        }
        
        return stats
    }
}
