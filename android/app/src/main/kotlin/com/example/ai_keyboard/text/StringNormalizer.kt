package com.example.ai_keyboard.text

object StringNormalizer {
    /** Lowercase, trim punctuation, collapse repeated letters (3+ → 2) */
    fun normalizeToken(raw: String): String {
        var t = raw.lowercase().trim()
        // keep apostrophes inside words (don't split we'll handle "don't")
        t = t.replace(Regex("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$"), "")
        // collapse runs like soooo -> soo (helps "sooo" match "so")
        t = t.replace(Regex("(\\p{L})\\1{2,}"), "$1$1")
        return t
    }

    /** True if token is purely alphabetic (latin/devanagari/etc.) */
    fun isAlphabetic(raw: String): Boolean =
        raw.isNotEmpty() && raw.all { Character.isLetter(it) }
}
