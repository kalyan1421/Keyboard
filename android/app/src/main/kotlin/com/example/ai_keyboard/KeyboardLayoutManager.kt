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
            "qwerty_letters.xml" -> R.xml.qwerty_google
            "qwerty_symbols.xml" -> R.xml.symbols_google
            "qwerty_numbers.xml" -> R.xml.numbers
            
            // AZERTY layouts  
            "azerty_letters.xml" -> R.xml.azerty_google
            "azerty_symbols.xml" -> R.xml.symbols_google
            "azerty_numbers.xml" -> R.xml.numbers
            
            // QWERTZ layouts
            "qwertz_letters.xml" -> R.xml.qwertz_google
            "qwertz_symbols.xml" -> R.xml.symbols_google
            "qwertz_numbers.xml" -> R.xml.numbers
            
            // Devanagari layouts
            "devanagari_letters.xml" -> R.xml.devanagari_google
            "devanagari_symbols.xml" -> R.xml.symbols_google
            "devanagari_numbers.xml" -> R.xml.numbers
            
            // Generic fallbacks
            "symbols.xml" -> R.xml.symbols_google
            "numbers.xml" -> R.xml.numbers
            
            else -> {
                Log.w(TAG, "Unknown resource name: $resourceName, using QWERTY fallback")
                R.xml.qwerty_google
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
        val keyboard = Keyboard(context, R.xml.qwerty_google)
        
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
        return Keyboard(context, R.xml.symbols_google)
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
     * Get accent mappings for a language
     */
    private fun getAccentMappings(languageCode: String): Map<String, List<String>> {
        return when (languageCode) {
            "es" -> mapOf(
                "a" to listOf("á", "à", "ä", "â"),
                "e" to listOf("é", "è", "ë", "ê"),
                "i" to listOf("í", "ì", "ï", "î"),
                "o" to listOf("ó", "ò", "ö", "ô"),
                "u" to listOf("ú", "ù", "ü", "û"),
                "n" to listOf("ñ"),
                "c" to listOf("ç")
            )
            "fr" -> mapOf(
                "a" to listOf("à", "â", "á", "ä"),
                "e" to listOf("é", "è", "ê", "ë"),
                "i" to listOf("î", "ï", "í", "ì"),
                "o" to listOf("ô", "ö", "ó", "ò"),
                "u" to listOf("ù", "û", "ü", "ú"),
                "c" to listOf("ç"),
                "y" to listOf("ÿ")
            )
            "de" -> mapOf(
                "a" to listOf("ä", "á", "à", "â"),
                "o" to listOf("ö", "ó", "ò", "ô"),
                "u" to listOf("ü", "ú", "ù", "û"),
                "s" to listOf("ß")
            )
            "pt" -> mapOf(
                "a" to listOf("á", "à", "â", "ã", "ä"),
                "e" to listOf("é", "è", "ê", "ë"),
                "i" to listOf("í", "ì", "î", "ï"),
                "o" to listOf("ó", "ò", "ô", "õ", "ö"),
                "u" to listOf("ú", "ù", "û", "ü"),
                "c" to listOf("ç")
            )
            "it" -> mapOf(
                "a" to listOf("à", "á", "â", "ä"),
                "e" to listOf("è", "é", "ê", "ë"),
                "i" to listOf("ì", "í", "î", "ï"),
                "o" to listOf("ò", "ó", "ô", "ö"),
                "u" to listOf("ù", "ú", "û", "ü")
            )
            else -> emptyMap()
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
