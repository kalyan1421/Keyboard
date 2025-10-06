package com.example.ai_keyboard.predict

import com.example.ai_keyboard.text.StringNormalizer

object SuggestionRanker {
    data class Bundle(
        val words: List<Pair<String, Double>>,
        val emojis: List<Pair<String, Double>>
    )

    /**
     * Rules:
     *  - If user is typing alphabetic currentWord → DO NOT show emoji.
     *  - If no currentWord and previous context is emotional ("love", "miss", "happy"),
     *    allow up to 1–2 emoji at the tail.
     */
    fun mergeForStrip(currentWord: String, contextPrev: String?, wordCands: List<Pair<String,Double>>, emojiCands: List<Pair<String,Double>>, max: Int = 5): List<String> {
        val alpha = StringNormalizer.isAlphabetic(currentWord)
        val list = mutableListOf<String>()

        if (alpha) {
            list += wordCands.take(max).map { it.first }
            return list.take(max)
        }

        // No current word: allow emojis at the end if context is emotional
        val emotionalPrev = when (contextPrev?.lowercase()) {
            "love", "miss", "thanks", "thank", "congrats", "happy", "sad", "excited" -> true
            else -> false
        }
        val allowedEmojis = if (emotionalPrev) emojiCands.take(2) else emptyList()

        val combined = (wordCands.take(max) + allowedEmojis).sortedByDescending { it.second }.map { it.first }
        return combined.distinct().take(max)
    }
}
