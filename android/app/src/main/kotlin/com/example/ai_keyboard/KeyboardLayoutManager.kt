package com.example.ai_keyboard

import android.content.Context
import android.inputmethodservice.Keyboard
import android.util.Log
import java.io.InputStream

/**
 * Manages different keyboard layouts for multilingual support
 */
class KeyboardLayoutManager(private val context: Context) {
    companion object {
        private const val TAG = "KeyboardLayoutManager"
        
        // Layout resource mappings
        private val LAYOUT_RESOURCES = mapOf(
            LayoutType.QWERTY to mapOf(
                "letters" to "qwerty_letters.xml",
                "symbols" to "qwerty_symbols.xml",
                "numbers" to "qwerty_numbers.xml"
            ),
            LayoutType.AZERTY to mapOf(
                "letters" to "azerty_letters.xml",
                "symbols" to "azerty_symbols.xml", 
                "numbers" to "azerty_numbers.xml"
            ),
            LayoutType.QWERTZ to mapOf(
                "letters" to "qwertz_letters.xml",
                "symbols" to "qwertz_symbols.xml",
                "numbers" to "qwertz_numbers.xml"
            ),
            LayoutType.DEVANAGARI to mapOf(
                "letters" to "devanagari_letters.xml",
                "symbols" to "devanagari_symbols.xml",
                "numbers" to "devanagari_numbers.xml"
            ),
            LayoutType.CUSTOM to mapOf(
                "letters" to "custom_letters.xml",
                "symbols" to "custom_symbols.xml",
                "numbers" to "custom_numbers.xml"
            )
        )
    }
    
    private val loadedLayouts = mutableMapOf<String, Keyboard>()
    private var currentLayoutType = LayoutType.QWERTY
    private var currentLanguage = "en"
    
    // Bilingual typing support
    private var bilingualModeEnabled = false
    private var primaryLanguage = "en"
    private var secondaryLanguage = "es"
    private var currentBilingualContext = "en"
    
    /**
     * Get keyboard for specific layout type and mode
     */
    fun getKeyboard(layoutType: LayoutType, mode: String, language: String): Keyboard? {
        val layoutKey = "${layoutType.name}_${mode}_$language"
        
        return loadedLayouts.getOrPut(layoutKey) {
            createKeyboard(layoutType, mode, language)
        }
    }
    
    /**
     * Get keyboard for current language
     */
    fun getCurrentKeyboard(mode: String): Keyboard? {
        val languageConfig = LanguageConfigs.getLanguageConfig(currentLanguage)
        val layoutType = languageConfig?.layoutType ?: LayoutType.QWERTY
        return getKeyboard(layoutType, mode, currentLanguage)
    }
    
    /**
     * Create keyboard for specific layout and language
     */
    private fun createKeyboard(layoutType: LayoutType, mode: String, language: String): Keyboard {
        val resourceName = LAYOUT_RESOURCES[layoutType]?.get(mode) ?: "qwerty_letters.xml"
        
        return try {
            // Load the layout directly based on layout type (no language prefix needed)
            loadKeyboardFromAssets(resourceName)
        } catch (e: Exception) {
            // Ultimate fallback to QWERTY
            Log.w(TAG, "Could not load layout $resourceName, using fallback", e)
            createFallbackKeyboard(mode)
        }
    }
    
    /**
     * Load keyboard from XML resources
     */
    private fun loadKeyboardFromAssets(resourceName: String): Keyboard {
        // Map resource names to actual XML resource IDs
        val resourceId = when (resourceName) {
            // QWERTY layouts
            "qwerty_letters.xml" -> R.xml.qwerty
            "qwerty_with_numbers.xml" -> R.xml.qwerty_with_numbers
            "qwerty_symbols.xml" -> R.xml.symbols
            "qwerty_numbers.xml" -> R.xml.numbers
            
            // AZERTY layouts  
            "azerty_letters.xml" -> R.xml.qwerty
            "azerty_symbols.xml" -> R.xml.symbols
            "azerty_numbers.xml" -> R.xml.numbers
            
            // QWERTZ layouts
            "qwertz_letters.xml" -> R.xml.qwerty
            "qwertz_symbols.xml" -> R.xml.symbols
            "qwertz_numbers.xml" -> R.xml.numbers
            
            // Devanagari layouts
            "devanagari_letters.xml" -> R.xml.qwerty
            "devanagari_symbols.xml" -> R.xml.symbols
            "devanagari_numbers.xml" -> R.xml.numbers
            
            // Generic fallbacks
            "symbols.xml" -> R.xml.symbols
            "numbers.xml" -> R.xml.numbers
            
            else -> {
                Log.w(TAG, "Unknown resource name: $resourceName, using QWERTY fallback")
                R.xml.qwerty
            }
        }
        
        return try {
            Keyboard(context, resourceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading keyboard resource $resourceName", e)
            createFallbackKeyboard("letters")
        }
    }
    
    /**
     * Create fallback keyboard programmatically
     */
    private fun createFallbackKeyboard(mode: String): Keyboard {
        return when (mode) {
            "letters" -> createQwertyLettersKeyboard()
            "symbols" -> createSymbolsKeyboard()
            "numbers" -> createNumbersKeyboard()
            else -> createQwertyLettersKeyboard()
        }
    }
    
    /**
     * Create QWERTY letters keyboard programmatically
     */
    private fun createQwertyLettersKeyboard(): Keyboard {
        val keyboard = Keyboard(context, R.xml.qwerty)
        
        // Customize keys based on current language if needed
        val languageConfig = LanguageConfigs.getLanguageConfig(currentLanguage)
        if (languageConfig?.hasAccents == true) {
            addAccentSupport(keyboard, languageConfig)
        }
        
        return keyboard
    }
    
    /**
     * Create symbols keyboard programmatically
     */
    private fun createSymbolsKeyboard(): Keyboard {
        return Keyboard(context, R.xml.symbols)
    }
    
    /**
     * Create numbers keyboard programmatically
     */
    private fun createNumbersKeyboard(): Keyboard {
        return Keyboard(context, R.xml.numbers)
    }
    
    /**
     * Add accent support to keyboard keys
     */
    private fun addAccentSupport(keyboard: Keyboard, languageConfig: LanguageConfig) {
        val accentMappings = getAccentMappings(languageConfig.code)
        
        keyboard.keys?.forEach { key ->
            val baseChar = key.label?.toString()?.lowercase()
            if (baseChar != null && accentMappings.containsKey(baseChar)) {
                // Store accent options in key's popupCharacters
                val accents = accentMappings[baseChar]
                if (accents != null) {
                    key.popupCharacters = accents.joinToString("")
                }
            }
        }
    }
    
    /**
     * Get enhanced accent mappings for a language with symbols and currency
     */
    private fun getAccentMappings(languageCode: String): Map<String, List<String>> {
        val baseMapping = when (languageCode) {
            "es" -> mapOf(
                "a" to listOf("á", "à", "ä", "â", "ã", "å", "ā", "ă", "ą", "α", "@"),
                "e" to listOf("é", "è", "ë", "ê", "ē", "ė", "ę", "ε", "€"),
                "i" to listOf("í", "ì", "ï", "î", "ī", "į", "ι"),
                "o" to listOf("ó", "ò", "ö", "ô", "õ", "ō", "ő", "œ", "ø", "ω"),
                "u" to listOf("ú", "ù", "ü", "û", "ū", "ů", "ű", "μ"),
                "n" to listOf("ñ", "ń", "ņ", "ň", "ŋ", "η"),
                "c" to listOf("ç", "ć", "č", "ĉ", "©"),
                "s" to listOf("ß", "ś", "š", "ş", "σ", "$"),
                "?" to listOf("¿", "‽"),
                "!" to listOf("¡")
            )
            "fr" -> mapOf(
                "a" to listOf("à", "â", "á", "ä", "ã", "å", "ā", "ă", "ą", "α", "@"),
                "e" to listOf("é", "è", "ê", "ë", "ē", "ė", "ę", "ε", "€"),
                "i" to listOf("î", "ï", "í", "ì", "ī", "į", "ι"),
                "o" to listOf("ô", "ö", "ó", "ò", "õ", "ō", "ő", "œ", "ø", "ω"),
                "u" to listOf("ù", "û", "ü", "ú", "ū", "ů", "ű", "μ"),
                "c" to listOf("ç", "ć", "č", "ĉ", "©"),
                "y" to listOf("ÿ", "ý", "ŷ", "¥"),
                "s" to listOf("ś", "š", "ş", "σ", "$")
            )
            "de" -> mapOf(
                "a" to listOf("ä", "á", "à", "â", "ã", "å", "ā", "ă", "ą", "α", "@"),
                "o" to listOf("ö", "ó", "ò", "ô", "õ", "ō", "ő", "œ", "ø", "ω"),
                "u" to listOf("ü", "ú", "ù", "û", "ū", "ů", "ű", "μ"),
                "s" to listOf("ß", "ś", "š", "ş", "σ", "$"),
                "e" to listOf("é", "è", "ê", "ë", "ē", "ė", "ę", "ε", "€")
            )
            "hi" -> mapOf(
                "a" to listOf("आ", "अ", "ा", "ं"),
                "e" to listOf("ए", "ै", "े"),
                "i" to listOf("इ", "ी", "ि"),
                "o" to listOf("ओ", "ौ", "ो"),
                "u" to listOf("उ", "ू", "ु"),
                "n" to listOf("न", "ण", "ञ", "ङ", "ं")
            )
            "pt" -> mapOf(
                "a" to listOf("á", "à", "â", "ã", "ä", "å", "ā", "ă", "ą", "α", "@"),
                "e" to listOf("é", "è", "ê", "ë", "ē", "ė", "ę", "ε", "€"),
                "i" to listOf("í", "ì", "î", "ï", "ī", "į", "ι"),
                "o" to listOf("ó", "ò", "ô", "õ", "ö", "ō", "ő", "œ", "ø", "ω"),
                "u" to listOf("ú", "ù", "û", "ü", "ū", "ů", "ű", "μ"),
                "c" to listOf("ç", "ć", "č", "ĉ", "©")
            )
            "it" -> mapOf(
                "a" to listOf("à", "á", "â", "ä", "ã", "å", "ā", "ă", "ą", "α", "@"),
                "e" to listOf("è", "é", "ê", "ë", "ē", "ė", "ę", "ε", "€"),
                "i" to listOf("ì", "í", "î", "ï", "ī", "į", "ι"),
                "o" to listOf("ò", "ó", "ô", "ö", "õ", "ō", "ő", "œ", "ø", "ω"),
                "u" to listOf("ù", "ú", "û", "ü", "ū", "ů", "ű", "μ")
            )
            else -> emptyMap()
        }
        
        // Add bilingual mappings if enabled
        return if (bilingualModeEnabled && languageCode == primaryLanguage) {
            val secondaryMappings = getAccentMappings(secondaryLanguage)
            baseMapping.toMutableMap().apply {
                secondaryMappings.forEach { (key, values) ->
                    this[key] = (this[key] ?: emptyList()) + values
                }
            }
        } else {
            baseMapping
        }
    }
    
    /**
     * Enable bilingual typing mode
     */
    fun enableBilingualMode(primary: String, secondary: String) {
        bilingualModeEnabled = true
        primaryLanguage = primary
        secondaryLanguage = secondary
        currentBilingualContext = primary
        
        // Clear cache to reload with bilingual mappings
        clearCache()
        Log.d(TAG, "Enabled bilingual mode: $primary + $secondary")
    }
    
    /**
     * Disable bilingual typing mode
     */
    fun disableBilingualMode() {
        bilingualModeEnabled = false
        clearCache()
        Log.d(TAG, "Disabled bilingual mode")
    }
    
    /**
     * Smart language detection for bilingual typing
     */
    fun detectLanguageContext(inputText: String): String {
        if (!bilingualModeEnabled) return currentLanguage
        
        val primaryPatterns = getLanguagePatterns(primaryLanguage)
        val secondaryPatterns = getLanguagePatterns(secondaryLanguage)
        
        val primaryScore = primaryPatterns.count { inputText.contains(it, true) }
        val secondaryScore = secondaryPatterns.count { inputText.contains(it, true) }
        
        currentBilingualContext = if (secondaryScore > primaryScore) secondaryLanguage else primaryLanguage
        return currentBilingualContext
    }
    
    /**
     * Get language-specific patterns for detection
     */
    private fun getLanguagePatterns(languageCode: String): List<String> {
        return when (languageCode) {
            "es" -> listOf("ñ", "¿", "¡", "rr", "ll", "ch", "qu")
            "fr" -> listOf("ç", "œ", "tion", "ment", "eau", "eux")
            "de" -> listOf("ß", "sch", "ung", "ich", "ein", "der")
            "hi" -> listOf("क", "ख", "ग", "च", "ज", "त", "प", "म")
            "en" -> listOf("th", "ing", "tion", "qu", "ph", "gh")
            else -> emptyList()
        }
    }
    
    /**
     * Switch to a different layout type
     */
    fun switchLayoutType(layoutType: LayoutType, language: String) {
        currentLayoutType = layoutType
        currentLanguage = language
        Log.d(TAG, "Switched to layout type: $layoutType for language: $language")
    }
    
    /**
     * Update current language
     */
    fun updateCurrentLanguage(language: String) {
        val languageConfig = LanguageConfigs.getLanguageConfig(language)
        if (languageConfig != null) {
            currentLanguage = language
            currentLayoutType = languageConfig.layoutType
            Log.d(TAG, "Updated language to: $language with layout: $currentLayoutType")
        }
    }
    
    /**
     * Get available modes for current layout
     */
    fun getAvailableModes(): List<String> {
        return listOf("letters", "symbols", "numbers")
    }
    
    /**
     * Check if layout supports specific mode
     */
    fun supportsMode(layoutType: LayoutType, mode: String): Boolean {
        return LAYOUT_RESOURCES[layoutType]?.containsKey(mode) == true
    }
    
    /**
     * Get layout type for language
     */
    fun getLayoutTypeForLanguage(language: String): LayoutType {
        val languageConfig = LanguageConfigs.getLanguageConfig(language)
        return languageConfig?.layoutType ?: LayoutType.QWERTY
    }
    
    /**
     * Clear layout cache
     */
    fun clearCache() {
        loadedLayouts.clear()
        Log.d(TAG, "Cleared keyboard layout cache")
    }
    
    /**
     * Preload layouts for enabled languages
     */
    fun preloadLayouts(enabledLanguages: Set<String>) {
        enabledLanguages.forEach { language ->
            val languageConfig = LanguageConfigs.getLanguageConfig(language)
            if (languageConfig != null) {
                // Preload all modes for this language
                getAvailableModes().forEach { mode ->
                    try {
                        getKeyboard(languageConfig.layoutType, mode, language)
                        Log.d(TAG, "Preloaded $mode layout for $language")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not preload $mode layout for $language", e)
                    }
                }
            }
        }
    }
    
    /**
     * Get keyboard dimensions and layout info
     */
    fun getLayoutInfo(layoutType: LayoutType): Map<String, Any> {
        return mapOf(
            "type" to layoutType.name,
            "rows" to getRowCount(layoutType),
            "hasAccents" to hasAccentSupport(layoutType),
            "direction" to getTextDirection(layoutType)
        )
    }
    
    private fun getRowCount(layoutType: LayoutType): Int {
        return when (layoutType) {
            LayoutType.QWERTY, LayoutType.AZERTY, LayoutType.QWERTZ -> 4
            LayoutType.DEVANAGARI -> 5
            LayoutType.CUSTOM -> 4
        }
    }
    
    private fun hasAccentSupport(layoutType: LayoutType): Boolean {
        return when (layoutType) {
            LayoutType.QWERTY, LayoutType.AZERTY, LayoutType.QWERTZ -> true
            LayoutType.DEVANAGARI, LayoutType.CUSTOM -> false
        }
    }
    
    private fun getTextDirection(layoutType: LayoutType): String {
        return when (layoutType) {
            LayoutType.QWERTY, LayoutType.AZERTY, LayoutType.QWERTZ, 
            LayoutType.DEVANAGARI, LayoutType.CUSTOM -> "LTR"
        }
    }
}
