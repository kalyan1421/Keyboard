package com.example.ai_keyboard

import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import kotlin.math.max

/**
 * Language detection system for automatic language switching
 */
class LanguageDetector {
    companion object {
        private const val TAG = "LanguageDetector"
        private const val MIN_CONFIDENCE_THRESHOLD = 0.6
        private const val MIN_TEXT_LENGTH = 3
    }
    
    // Character sets for different languages
    private val characterSets = mapOf(
        "en" to Regex("[a-zA-Z]"),
        "es" to Regex("[a-zA-ZñÑáéíóúüÁÉÍÓÚÜ¿¡]"),
        "fr" to Regex("[a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ]"),
        "de" to Regex("[a-zA-ZäöüßÄÖÜ]"),
        "pt" to Regex("[a-zA-ZãõáéíóúâêîôûàÃÕÁÉÍÓÚÂÊÎÔÛÀ]"),
        "it" to Regex("[a-zA-ZàèéìíîòóùúÀÈÉÌÍÎÒÓÙÚ]"),
        "hi" to Regex("[अ-ह़]"),
        "ar" to Regex("[ء-ي]"),
        "ru" to Regex("[а-яё]"),
        "ja" to Regex("[ひらがなカタカナ漢字]")
    )
    
    // Common words for each language (most frequent 20 words)
    private val commonWords = mapOf(
        "en" to setOf("the", "and", "to", "of", "a", "in", "is", "it", "you", "that", "he", "was", "for", "on", "are", "as", "with", "his", "they", "i"),
        "es" to setOf("el", "la", "de", "que", "y", "a", "en", "un", "es", "se", "no", "te", "lo", "le", "da", "su", "por", "son", "con", "para"),
        "fr" to setOf("le", "de", "et", "à", "un", "il", "être", "et", "en", "avoir", "que", "pour", "dans", "ce", "son", "une", "sur", "avec", "ne", "se"),
        "de" to setOf("der", "die", "und", "in", "den", "von", "zu", "das", "mit", "sich", "des", "auf", "für", "ist", "im", "dem", "nicht", "ein", "eine", "als"),
        "pt" to setOf("o", "a", "de", "e", "do", "da", "em", "um", "para", "é", "com", "não", "uma", "os", "no", "se", "na", "por", "mais", "as"),
        "it" to setOf("il", "di", "che", "e", "la", "un", "a", "per", "non", "in", "una", "si", "è", "da", "con", "i", "su", "le", "del", "lo"),
        "hi" to setOf("का", "में", "की", "और", "को", "है", "से", "पर", "एक", "यह", "वह", "कि", "जो", "तो", "ही", "भी", "या", "थे", "हैं", "था"),
        "ar" to setOf("في", "من", "إلى", "على", "أن", "هذا", "هذه", "التي", "الذي", "كان", "لم", "قد", "كل", "بعد", "عند", "أول", "غير", "بين", "حيث", "خلال"),
        "ru" to setOf("в", "и", "не", "на", "я", "быть", "с", "он", "а", "как", "по", "это", "она", "к", "но", "они", "мы", "что", "за", "из")
    )
    
    // Language-specific patterns
    private val languagePatterns = mapOf(
        "en" to listOf(
            Regex("\\b(the|and|that|with|have|this|will|you|from|they|know|want|been|good|much|some)\\b"),
            Regex("ing\\b"),
            Regex("tion\\b"),
            Regex("'s\\b"),
            Regex("n't\\b")
        ),
        "es" to listOf(
            Regex("\\b(que|con|por|para|una|del|los|las|sus|muy|más|también|donde|cuando)\\b"),
            Regex("ción\\b"),
            Regex("ando\\b"),
            Regex("endo\\b"),
            Regex("¿.*?\\?"),
            Regex("¡.*?!")
        ),
        "fr" to listOf(
            Regex("\\b(que|avec|pour|dans|sur|par|tout|bien|plus|sans|sous|entre|depuis)\\b"),
            Regex("tion\\b"),
            Regex("ment\\b"),
            Regex("eux\\b"),
            Regex("euse\\b")
        ),
        "de" to listOf(
            Regex("\\b(und|der|die|das|den|dem|des|ein|eine|eines|einem|einen|nicht|auch|nur|noch|schon)\\b"),
            Regex("ung\\b"),
            Regex("heit\\b"),
            Regex("keit\\b"),
            Regex("lich\\b")
        ),
        "pt" to listOf(
            Regex("\\b(que|com|por|para|uma|dos|das|seu|sua|muito|mais|também|onde|quando)\\b"),
            Regex("ção\\b"),
            Regex("ando\\b"),
            Regex("endo\\b"),
            Regex("mente\\b")
        ),
        "it" to listOf(
            Regex("\\b(che|con|per|una|del|dei|delle|suo|sua|molto|più|anche|dove|quando)\\b"),
            Regex("zione\\b"),
            Regex("ando\\b"),
            Regex("endo\\b"),
            Regex("mente\\b")
        ),
        "hi" to listOf(
            Regex("है\\b"),
            Regex("में\\b"),
            Regex("को\\b"),
            Regex("से\\b"),
            Regex("और\\b")
        ),
        "ar" to listOf(
            Regex("\\bال"),
            Regex("ين\\b"),
            Regex("ون\\b"),
            Regex("ها\\b"),
            Regex("هم\\b")
        ),
        "ru" to listOf(
            Regex("\\b(что|как|все|еще|уже|только|очень|здесь|там|где|когда)\\b"),
            Regex("ость\\b"),
            Regex("ение\\b"),
            Regex("ание\\b"),
            Regex("ный\\b")
        )
    )
    
    /**
     * Detect the most likely language for the given text
     */
    fun detectLanguage(text: String): String {
        if (text.length < MIN_TEXT_LENGTH) {
            return "en" // Default to English for short text
        }
        
        val confidences = getLanguageConfidences(text)
        val bestMatch = confidences.maxByOrNull { it.value }
        
        return if (bestMatch != null && bestMatch.value >= MIN_CONFIDENCE_THRESHOLD) {
            LogUtil.d(TAG, "Detected language: ${bestMatch.key} (confidence: ${bestMatch.value})")
            bestMatch.key
        } else {
            LogUtil.d(TAG, "No confident language detection, defaulting to English")
            "en"
        }
    }
    
    /**
     * Get confidence scores for all languages
     */
    fun getLanguageConfidences(text: String): Map<String, Double> {
        val cleanText = text.lowercase().trim()
        val confidence = mutableMapOf<String, Double>()
        
        if (cleanText.isEmpty()) {
            return mapOf("en" to 1.0)
        }
        
        // Calculate confidence for each language
        characterSets.keys.forEach { language ->
            val score = calculateLanguageScore(cleanText, language)
            confidence[language] = score
        }
        
        // Normalize scores
        val maxScore = confidence.values.maxOrNull() ?: 0.0
        if (maxScore > 0) {
            confidence.forEach { (lang, score) ->
                confidence[lang] = score / maxScore
            }
        }
        
        return confidence
    }
    
    /**
     * Calculate language score based on character sets, common words, and patterns
     */
    private fun calculateLanguageScore(text: String, language: String): Double {
        var score = 0.0
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val totalWords = max(words.size, 1)
        val totalChars = max(text.length, 1)
        
        // Character set matching (30% weight)
        val charset = characterSets[language]
        if (charset != null) {
            val matchingChars = charset.findAll(text).count()
            score += (matchingChars.toDouble() / totalChars) * 0.3
        }
        
        // Common words matching (40% weight)
        val commonWordsSet = commonWords[language] ?: emptySet()
        val matchingCommonWords = words.count { word -> 
            commonWordsSet.contains(word.lowercase())
        }
        score += (matchingCommonWords.toDouble() / totalWords) * 0.4
        
        // Language-specific patterns (30% weight)
        val patterns = languagePatterns[language] ?: emptyList()
        var patternMatches = 0
        patterns.forEach { pattern ->
            patternMatches += pattern.findAll(text).count()
        }
        score += (patternMatches.toDouble() / totalWords) * 0.3
        
        return score
    }
    
    /**
     * Detect language from recent typing context
     */
    fun detectLanguageFromContext(recentText: String, currentLanguage: String): String? {
        if (recentText.length < MIN_TEXT_LENGTH * 3) {
            return null // Not enough context
        }
        
        val detectedLanguage = detectLanguage(recentText)
        val confidence = getLanguageConfidences(recentText)[detectedLanguage] ?: 0.0
        
        return if (detectedLanguage != currentLanguage && confidence > MIN_CONFIDENCE_THRESHOLD * 1.2) {
            LogUtil.d(TAG, "Context suggests language switch from $currentLanguage to $detectedLanguage")
            detectedLanguage
        } else {
            null
        }
    }
    
    /**
     * Check if text is likely in a specific language
     */
    fun isTextInLanguage(text: String, language: String, minConfidence: Double = MIN_CONFIDENCE_THRESHOLD): Boolean {
        val confidences = getLanguageConfidences(text)
        val confidence = confidences[language] ?: 0.0
        return confidence >= minConfidence
    }
    
    /**
     * Get the most likely languages for the text (sorted by confidence)
     */
    fun getTopLanguages(text: String, maxResults: Int = 3): List<Pair<String, Double>> {
        val confidences = getLanguageConfidences(text)
        return confidences.toList()
            .sortedByDescending { it.second }
            .take(maxResults)
    }
    
    /**
     * Detect language for a single word
     */
    fun detectWordLanguage(word: String): String? {
        if (word.length < 2) return null
        
        val scores = mutableMapOf<String, Int>()
        
        // Check character sets
        characterSets.forEach { (language, regex) ->
            val matches = regex.findAll(word).count()
            if (matches > 0) {
                scores[language] = matches
            }
        }
        
        // Check common words
        commonWords.forEach { (language, words) ->
            if (words.contains(word.lowercase())) {
                scores[language] = (scores[language] ?: 0) + word.length * 2
            }
        }
        
        return scores.maxByOrNull { it.value }?.key
    }
    
    /**
     * Check if automatic language switching should be suggested
     */
    fun shouldSuggestLanguageSwitch(
        recentText: String, 
        currentLanguage: String, 
        enabledLanguages: Set<String>
    ): String? {
        val suggestedLanguage = detectLanguageFromContext(recentText, currentLanguage)
        
        return if (suggestedLanguage != null && 
                   enabledLanguages.contains(suggestedLanguage) &&
                   suggestedLanguage != currentLanguage) {
            suggestedLanguage
        } else {
            null
        }
    }
}
