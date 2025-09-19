package com.example.ai_keyboard

/**
 * Language configuration data classes for multilingual keyboard support
 */
data class LanguageConfig(
    val code: String,
    val name: String,
    val nativeName: String,
    val layoutType: LayoutType,
    val script: Script,
    val direction: TextDirection,
    val hasAccents: Boolean,
    val dictionaryFile: String,
    val correctionRules: String,
    val flag: String
)

enum class LayoutType {
    QWERTY, AZERTY, QWERTZ, DEVANAGARI, CUSTOM
}

enum class Script {
    LATIN, DEVANAGARI, ARABIC, CYRILLIC
}

enum class TextDirection {
    LTR, RTL
}

data class Correction(
    val originalWord: String,
    val correctedWord: String,
    val confidence: Double,
    val language: String
)

/**
 * Predefined language configurations
 */
object LanguageConfigs {
    val SUPPORTED_LANGUAGES = mapOf(
        "en" to LanguageConfig(
            code = "en",
            name = "English",
            nativeName = "English",
            layoutType = LayoutType.QWERTY,
            script = Script.LATIN,
            direction = TextDirection.LTR,
            hasAccents = true,
            dictionaryFile = "en_words.txt",
            correctionRules = "en_corrections.txt",
            flag = "🇺🇸"
        ),
        "es" to LanguageConfig(
            code = "es",
            name = "Spanish",
            nativeName = "Español",
            layoutType = LayoutType.QWERTY,
            script = Script.LATIN,
            direction = TextDirection.LTR,
            hasAccents = true,
            dictionaryFile = "es_words.txt",
            correctionRules = "es_corrections.txt",
            flag = "🇪🇸"
        ),
        "fr" to LanguageConfig(
            code = "fr",
            name = "French",
            nativeName = "Français",
            layoutType = LayoutType.AZERTY,
            script = Script.LATIN,
            direction = TextDirection.LTR,
            hasAccents = true,
            dictionaryFile = "fr_words.txt",
            correctionRules = "fr_corrections.txt",
            flag = "🇫🇷"
        ),
        "de" to LanguageConfig(
            code = "de",
            name = "German",
            nativeName = "Deutsch",
            layoutType = LayoutType.QWERTZ,
            script = Script.LATIN,
            direction = TextDirection.LTR,
            hasAccents = true,
            dictionaryFile = "de_words.txt",
            correctionRules = "de_corrections.txt",
            flag = "🇩🇪"
        ),
        "hi" to LanguageConfig(
            code = "hi",
            name = "Hindi",
            nativeName = "हिन्दी",
            layoutType = LayoutType.DEVANAGARI,
            script = Script.DEVANAGARI,
            direction = TextDirection.LTR,
            hasAccents = false,
            dictionaryFile = "hi_words.txt",
            correctionRules = "hi_corrections.txt",
            flag = "🇮🇳"
        ),
        "ar" to LanguageConfig(
            code = "ar",
            name = "Arabic",
            nativeName = "العربية",
            layoutType = LayoutType.CUSTOM,
            script = Script.ARABIC,
            direction = TextDirection.RTL,
            hasAccents = false,
            dictionaryFile = "ar_dict.db",
            correctionRules = "ar_rules.json",
            flag = "🇸🇦"
        ),
        "ru" to LanguageConfig(
            code = "ru",
            name = "Russian",
            nativeName = "Русский",
            layoutType = LayoutType.CUSTOM,
            script = Script.CYRILLIC,
            direction = TextDirection.LTR,
            hasAccents = false,
            dictionaryFile = "ru_dict.db",
            correctionRules = "ru_rules.json",
            flag = "🇷🇺"
        ),
        "pt" to LanguageConfig(
            code = "pt",
            name = "Portuguese",
            nativeName = "Português",
            layoutType = LayoutType.QWERTY,
            script = Script.LATIN,
            direction = TextDirection.LTR,
            hasAccents = true,
            dictionaryFile = "pt_dict.db",
            correctionRules = "pt_rules.json",
            flag = "🇵🇹"
        ),
        "it" to LanguageConfig(
            code = "it",
            name = "Italian",
            nativeName = "Italiano",
            layoutType = LayoutType.QWERTY,
            script = Script.LATIN,
            direction = TextDirection.LTR,
            hasAccents = true,
            dictionaryFile = "it_dict.db",
            correctionRules = "it_rules.json",
            flag = "🇮🇹"
        ),
        "ja" to LanguageConfig(
            code = "ja",
            name = "Japanese",
            nativeName = "日本語",
            layoutType = LayoutType.CUSTOM,
            script = Script.LATIN, // For romaji input
            direction = TextDirection.LTR,
            hasAccents = false,
            dictionaryFile = "ja_dict.db",
            correctionRules = "ja_rules.json",
            flag = "🇯🇵"
        )
    )
    
    /**
     * Get language config by code
     */
    fun getLanguageConfig(code: String): LanguageConfig? {
        return SUPPORTED_LANGUAGES[code]
    }
    
    /**
     * Get all enabled language codes
     */
    fun getEnabledLanguages(enabledCodes: Set<String>): List<LanguageConfig> {
        return enabledCodes.mapNotNull { SUPPORTED_LANGUAGES[it] }
    }
    
    /**
     * Get languages by layout type
     */
    fun getLanguagesByLayout(layoutType: LayoutType): List<LanguageConfig> {
        return SUPPORTED_LANGUAGES.values.filter { it.layoutType == layoutType }
    }
    
    /**
     * Get languages by script
     */
    fun getLanguagesByScript(script: Script): List<LanguageConfig> {
        return SUPPORTED_LANGUAGES.values.filter { it.script == script }
    }
}
