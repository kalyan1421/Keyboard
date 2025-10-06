package com.example.ai_keyboard

import android.util.Log
import kotlin.math.min

/**
 * Helper utilities for Indic script operations
 * 
 * Provides script-specific operations for Devanagari (Hindi), Telugu, Tamil, and other Indic scripts
 * Key features:
 * - Grapheme cluster-aware text processing (base character + combining marks)
 * - Script detection
 * - Grapheme-aware edit distance calculation
 * - Unicode normalization for Indic characters
 */
class IndicScriptHelper {
    
    companion object {
        private const val TAG = "IndicScriptHelper"
        
        // Unicode ranges for Indic scripts
        private const val DEVANAGARI_START = 0x0900
        private const val DEVANAGARI_END = 0x097F
        
        private const val BENGALI_START = 0x0980
        private const val BENGALI_END = 0x09FF
        
        private const val GUJARATI_START = 0x0A80
        private const val GUJARATI_END = 0x0AFF
        
        private const val TAMIL_START = 0x0B80
        private const val TAMIL_END = 0x0BFF
        
        private const val TELUGU_START = 0x0C00
        private const val TELUGU_END = 0x0C7F
        
        private const val KANNADA_START = 0x0C80
        private const val KANNADA_END = 0x0CFF
        
        private const val MALAYALAM_START = 0x0D00
        private const val MALAYALAM_END = 0x0D7F
        
        // Devanagari combining marks ranges
        private val DEVANAGARI_COMBINING = listOf(
            0x0901..0x0903,  // Combining marks
            0x093C..0x094F,  // Vowel signs (matras)
            0x0951..0x0954,  // Stress marks
            0x0962..0x0963   // Vocalic marks
        )
        
        // Telugu combining marks
        private val TELUGU_COMBINING = listOf(
            0x0C00..0x0C04,  // Combining marks
            0x0C3E..0x0C4C,  // Vowel signs
            0x0C55..0x0C56,  // Length marks
            0x0C62..0x0C63   // Vocalic marks
        )
        
        // Tamil combining marks
        private val TAMIL_COMBINING = listOf(
            0x0B82..0x0B83,  // Combining marks
            0x0BBE..0x0BC2,  // Vowel signs
            0x0BC6..0x0BCD   // Length marks and virama
        )
    }
    
    /**
     * Enum representing different Indic scripts
     */
    enum class Script {
        LATIN,
        DEVANAGARI,
        BENGALI,
        GUJARATI,
        TAMIL,
        TELUGU,
        KANNADA,
        MALAYALAM,
        UNKNOWN
    }
    
    /**
     * Detect the primary script of a text string
     */
    fun detectScript(text: String): Script {
        if (text.isEmpty()) return Script.UNKNOWN
        
        // Find first non-whitespace, non-punctuation character
        val firstChar = text.firstOrNull { !it.isWhitespace() && it.code > 127 }
            ?: return Script.LATIN
        
        return when (firstChar.code) {
            in DEVANAGARI_START..DEVANAGARI_END -> Script.DEVANAGARI
            in BENGALI_START..BENGALI_END -> Script.BENGALI
            in GUJARATI_START..GUJARATI_END -> Script.GUJARATI
            in TAMIL_START..TAMIL_END -> Script.TAMIL
            in TELUGU_START..TELUGU_END -> Script.TELUGU
            in KANNADA_START..KANNADA_END -> Script.KANNADA
            in MALAYALAM_START..MALAYALAM_END -> Script.MALAYALAM
            in 32..127 -> Script.LATIN
            else -> Script.UNKNOWN
        }
    }
    
    /**
     * Check if a character is an Indic combining mark (matra, virama, etc.)
     * These marks combine with the preceding base character to form a grapheme cluster
     */
    fun isIndicCombining(char: Char): Boolean {
        val code = char.code
        
        // Check Devanagari combining marks
        if (DEVANAGARI_COMBINING.any { code in it }) return true
        
        // Check Telugu combining marks
        if (TELUGU_COMBINING.any { code in it }) return true
        
        // Check Tamil combining marks
        if (TAMIL_COMBINING.any { code in it }) return true
        
        // Check other common combining marks
        return when (code) {
            // Bengali
            in 0x0981..0x0983, in 0x09BC..0x09CD, in 0x09D7..0x09D7, in 0x09E2..0x09E3 -> true
            // Gujarati
            in 0x0A81..0x0A83, in 0x0ABC..0x0ACD, in 0x0AE2..0x0AE3 -> true
            // Kannada
            in 0x0C81..0x0C83, in 0x0CBC..0x0CCD, in 0x0CE2..0x0CE3 -> true
            // Malayalam
            in 0x0D01..0x0D03, in 0x0D3E..0x0D4D, in 0x0D57..0x0D57, in 0x0D62..0x0D63 -> true
            else -> false
        }
    }
    
    /**
     * Split a string into grapheme clusters
     * A grapheme cluster = base character + all following combining marks
     * Example: "कि" = "क" + "ि" (2 code points, 1 grapheme)
     */
    fun toGraphemeList(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        
        val graphemes = mutableListOf<String>()
        var i = 0
        
        while (i < text.length) {
            val char = text[i]
            val cluster = StringBuilder().append(char)
            i++
            
            // Collect all following combining marks
            while (i < text.length && isIndicCombining(text[i])) {
                cluster.append(text[i])
                i++
            }
            
            graphemes.add(cluster.toString())
        }
        
        return graphemes
    }
    
    /**
     * Calculate edit distance at grapheme cluster level
     * This is more accurate for Indic scripts than character-level distance
     * 
     * Example: "कि" vs "की" is 1 edit (different matra), not 2
     */
    fun calculateGraphemeDistance(s1: String, s2: String): Int {
        val graphemes1 = toGraphemeList(s1)
        val graphemes2 = toGraphemeList(s2)
        
        return levenshteinDistance(graphemes1, graphemes2)
    }
    
    /**
     * Standard Levenshtein distance algorithm on grapheme clusters
     */
    private fun levenshteinDistance(s1: List<String>, s2: List<String>): Int {
        val m = s1.size
        val n = s2.size
        
        // Create distance matrix
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        // Initialize base cases
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        // Fill the matrix
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // Deletion
                    dp[i][j - 1] + 1,      // Insertion
                    dp[i - 1][j - 1] + cost // Substitution
                )
            }
        }
        
        return dp[m][n]
    }
    
    /**
     * Get grapheme count (user-perceived character count)
     * More accurate than String.length for Indic text
     */
    fun getGraphemeCount(text: String): Int {
        return toGraphemeList(text).size
    }
    
    /**
     * Normalize Indic text (basic normalization)
     * Handles common variations and canonical representations
     */
    fun normalize(text: String): String {
        // For now, just trim and basic cleanup
        // Can be extended with NFC/NFD normalization if needed
        return text.trim()
    }
    
    /**
     * Check if text contains any Indic script characters
     */
    fun containsIndicScript(text: String): Boolean {
        return text.any { char ->
            val code = char.code
            code in DEVANAGARI_START..DEVANAGARI_END ||
            code in BENGALI_START..BENGALI_END ||
            code in GUJARATI_START..GUJARATI_END ||
            code in TAMIL_START..TAMIL_END ||
            code in TELUGU_START..TELUGU_END ||
            code in KANNADA_START..KANNADA_END ||
            code in MALAYALAM_START..MALAYALAM_END
        }
    }
    
    /**
     * Get the base character (consonant/vowel) from a grapheme cluster
     * Example: "कि" → "क", "की" → "क"
     */
    fun getBaseCharacter(grapheme: String): Char? {
        return grapheme.firstOrNull()
    }
    
    /**
     * Get combining marks from a grapheme cluster
     * Example: "कि" → listOf('ि'), "की" → listOf('ी')
     */
    fun getCombiningMarks(grapheme: String): List<Char> {
        if (grapheme.length <= 1) return emptyList()
        return grapheme.drop(1).toList()
    }
    
    /**
     * Check if a character is a virama/halant (vowel killer)
     * Used to form conjuncts in Indic scripts
     */
    fun isVirama(char: Char): Boolean {
        return when (char.code) {
            0x094D, // Devanagari virama
            0x09CD, // Bengali virama
            0x0ACD, // Gujarati virama
            0x0B4D, // Tamil virama
            0x0BCD, // Tamil virama (alternate)
            0x0C4D, // Telugu virama
            0x0CCD, // Kannada virama
            0x0D4D  // Malayalam virama
            -> true
            else -> false
        }
    }
    
    /**
     * Calculate similarity percentage between two Indic texts (grapheme-aware)
     * Returns 0.0 to 1.0
     */
    fun calculateSimilarity(text1: String, text2: String): Double {
        if (text1 == text2) return 1.0
        if (text1.isEmpty() || text2.isEmpty()) return 0.0
        
        val distance = calculateGraphemeDistance(text1, text2)
        val maxLen = maxOf(getGraphemeCount(text1), getGraphemeCount(text2))
        
        return if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)
    }
    
    /**
     * Get language code from detected script
     */
    fun getLanguageFromScript(script: Script): String {
        return when (script) {
            Script.DEVANAGARI -> "hi"
            Script.TELUGU -> "te"
            Script.TAMIL -> "ta"
            Script.BENGALI -> "bn"
            Script.GUJARATI -> "gu"
            Script.KANNADA -> "kn"
            Script.MALAYALAM -> "ml"
            Script.LATIN -> "en"
            Script.UNKNOWN -> "en"
        }
    }
    
    /**
     * Log grapheme breakdown for debugging
     */
    fun logGraphemeBreakdown(text: String) {
        val graphemes = toGraphemeList(text)
        Log.d(TAG, "Text: '$text' (${text.length} chars)")
        Log.d(TAG, "Graphemes: ${graphemes.size}")
        graphemes.forEachIndexed { index, grapheme ->
            val base = getBaseCharacter(grapheme)
            val marks = getCombiningMarks(grapheme)
            val codes = grapheme.map { "U+${it.code.toString(16).uppercase()}" }.joinToString(" ")
            Log.d(TAG, "  [$index] '$grapheme' = base:'$base' + marks:$marks ($codes)")
        }
    }
}

