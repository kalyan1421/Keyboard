package com.example.ai_keyboard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages language switching, preferences, and app-specific language settings
 */
class LanguageManager(private val context: Context) {
    companion object {
        private const val TAG = "LanguageManager"
        private const val PREFS_NAME = "keyboard_language_prefs"
        private const val KEY_CURRENT_LANGUAGE = "current_language"
        private const val KEY_ENABLED_LANGUAGES = "enabled_languages"
        private const val KEY_AUTO_SWITCH = "auto_switch"
        private const val KEY_TAP_BEHAVIOR = "tap_behavior"
        private const val KEY_APP_PREFIX = "app_"
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var currentLanguage = "en"
    private var enabledLanguages = mutableSetOf("en")
    private val languageChangeListeners = mutableListOf<LanguageChangeListener>()
    
    interface LanguageChangeListener {
        fun onLanguageChanged(oldLanguage: String, newLanguage: String)
        fun onEnabledLanguagesChanged(enabledLanguages: Set<String>)
    }
    
    enum class TapBehavior {
        CYCLE,      // Tap to cycle through languages (default)
        POPUP       // Tap to show language selection popup
    }
    
    init {
        loadPreferences()
        // Enable multiple languages by default for testing
        if (enabledLanguages.size == 1 && enabledLanguages.contains("en")) {
            enabledLanguages.addAll(setOf("es", "fr", "de", "hi"))
            saveEnabledLanguages()
            Log.d(TAG, "Enabled multiple languages by default: $enabledLanguages")
        }
    }
    
    /**
     * Load language preferences from SharedPreferences
     */
    private fun loadPreferences() {
        currentLanguage = preferences.getString(KEY_CURRENT_LANGUAGE, "en") ?: "en"
        enabledLanguages = preferences.getStringSet(KEY_ENABLED_LANGUAGES, setOf("en"))?.toMutableSet() ?: mutableSetOf("en")
        
        // Ensure current language is in enabled languages
        if (!enabledLanguages.contains(currentLanguage)) {
            enabledLanguages.add(currentLanguage)
            saveEnabledLanguages()
        }
        
        Log.d(TAG, "Loaded preferences - Current: $currentLanguage, Enabled: $enabledLanguages")
    }
    
    /**
     * Switch to the next enabled language
     */
    fun switchToNextLanguage() {
        val enabledList = enabledLanguages.toList().sorted()
        if (enabledList.size <= 1) {
            Log.d(TAG, "Only one language enabled, no switching needed")
            return
        }
        
        val currentIndex = enabledList.indexOf(currentLanguage)
        val nextIndex = (currentIndex + 1) % enabledList.size
        val nextLanguage = enabledList[nextIndex]
        
        switchToLanguage(nextLanguage)
    }
    
    /**
     * Switch to a specific language
     */
    fun switchToLanguage(languageCode: String) {
        if (!isLanguageSupported(languageCode)) {
            Log.w(TAG, "Language $languageCode is not supported")
            return
        }
        
        if (!enabledLanguages.contains(languageCode)) {
            Log.w(TAG, "Language $languageCode is not enabled")
            return
        }
        
        if (currentLanguage == languageCode) {
            Log.d(TAG, "Already using language $languageCode")
            return
        }
        
        val oldLanguage = currentLanguage
        currentLanguage = languageCode
        saveCurrentLanguage()
        
        Log.d(TAG, "Switched language from $oldLanguage to $currentLanguage")
        
        // Notify listeners
        languageChangeListeners.forEach { listener ->
            try {
                listener.onLanguageChanged(oldLanguage, currentLanguage)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying language change listener", e)
            }
        }
    }
    
    /**
     * Get current language code
     */
    fun getCurrentLanguage(): String = currentLanguage
    
    /**
     * Get current language configuration
     */
    fun getCurrentLanguageConfig(): LanguageConfig? {
        return LanguageConfigs.getLanguageConfig(currentLanguage)
    }
    
    /**
     * Get all enabled languages
     */
    fun getEnabledLanguages(): Set<String> = enabledLanguages.toSet()
    
    /**
     * Get enabled language configurations
     */
    fun getEnabledLanguageConfigs(): List<LanguageConfig> {
        return LanguageConfigs.getEnabledLanguages(enabledLanguages)
    }
    
    /**
     * Set enabled languages
     */
    fun setEnabledLanguages(languages: Set<String>) {
        val validLanguages = languages.filter { isLanguageSupported(it) }.toMutableSet()
        
        // Ensure at least English is enabled
        if (validLanguages.isEmpty()) {
            validLanguages.add("en")
        }
        
        enabledLanguages = validLanguages
        saveEnabledLanguages()
        
        // Switch to a valid language if current is no longer enabled
        if (!enabledLanguages.contains(currentLanguage)) {
            switchToLanguage(enabledLanguages.first())
        }
        
        Log.d(TAG, "Updated enabled languages: $enabledLanguages")
        
        // Notify listeners
        languageChangeListeners.forEach { listener ->
            try {
                listener.onEnabledLanguagesChanged(enabledLanguages)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying enabled languages change listener", e)
            }
        }
    }
    
    /**
     * Add a language to enabled languages
     */
    fun enableLanguage(languageCode: String) {
        if (isLanguageSupported(languageCode) && !enabledLanguages.contains(languageCode)) {
            enabledLanguages.add(languageCode)
            saveEnabledLanguages()
            Log.d(TAG, "Enabled language: $languageCode")
        }
    }
    
    /**
     * Remove a language from enabled languages
     */
    fun disableLanguage(languageCode: String) {
        if (enabledLanguages.size > 1 && enabledLanguages.contains(languageCode)) {
            enabledLanguages.remove(languageCode)
            saveEnabledLanguages()
            
            // Switch to another language if current was disabled
            if (currentLanguage == languageCode) {
                switchToLanguage(enabledLanguages.first())
            }
            
            Log.d(TAG, "Disabled language: $languageCode")
        }
    }
    
    /**
     * Check if a language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return LanguageConfigs.SUPPORTED_LANGUAGES.containsKey(languageCode)
    }
    
    /**
     * Check if a language is enabled
     */
    fun isLanguageEnabled(languageCode: String): Boolean {
        return enabledLanguages.contains(languageCode)
    }
    
    /**
     * Set app-specific language preference
     */
    fun setAppLanguage(packageName: String, languageCode: String) {
        if (isLanguageSupported(languageCode) && isLanguageEnabled(languageCode)) {
            preferences.edit()
                .putString(KEY_APP_PREFIX + packageName, languageCode)
                .apply()
            Log.d(TAG, "Set app language for $packageName: $languageCode")
        }
    }
    
    /**
     * Get app-specific language preference
     */
    fun getAppLanguage(packageName: String): String? {
        return preferences.getString(KEY_APP_PREFIX + packageName, null)
    }
    
    /**
     * Remove app-specific language preference
     */
    fun removeAppLanguage(packageName: String) {
        preferences.edit()
            .remove(KEY_APP_PREFIX + packageName)
            .apply()
        Log.d(TAG, "Removed app language for $packageName")
    }
    
    /**
     * Set auto-switch enabled
     */
    fun setAutoSwitchEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_AUTO_SWITCH, enabled)
            .apply()
        Log.d(TAG, "Auto-switch enabled: $enabled")
    }
    
    /**
     * Check if auto-switch is enabled
     */
    fun isAutoSwitchEnabled(): Boolean {
        return preferences.getBoolean(KEY_AUTO_SWITCH, true)
    }
    
    /**
     * Set tap behavior for language switch button
     */
    fun setTapBehavior(behavior: TapBehavior) {
        preferences.edit()
            .putString(KEY_TAP_BEHAVIOR, behavior.name)
            .apply()
        Log.d(TAG, "Tap behavior set to: $behavior")
    }
    
    /**
     * Get current tap behavior
     */
    fun getTapBehavior(): TapBehavior {
        val behaviorName = preferences.getString(KEY_TAP_BEHAVIOR, TapBehavior.CYCLE.name)
        return try {
            TapBehavior.valueOf(behaviorName ?: TapBehavior.CYCLE.name)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid tap behavior: $behaviorName, using default")
            TapBehavior.CYCLE
        }
    }
    
    /**
     * Toggle between CYCLE and POPUP tap behaviors
     */
    fun toggleTapBehavior() {
        val currentBehavior = getTapBehavior()
        val newBehavior = when (currentBehavior) {
            TapBehavior.CYCLE -> TapBehavior.POPUP
            TapBehavior.POPUP -> TapBehavior.CYCLE
        }
        setTapBehavior(newBehavior)
        Log.d(TAG, "Toggled tap behavior from $currentBehavior to $newBehavior")
    }
    
    /**
     * Get language display name
     */
    fun getLanguageDisplayName(languageCode: String): String {
        val config = LanguageConfigs.getLanguageConfig(languageCode)
        return if (config != null) {
            "${config.flag} ${config.nativeName}"
        } else {
            languageCode.uppercase()
        }
    }
    
    /**
     * Get available layout types for enabled languages
     */
    fun getAvailableLayoutTypes(): Set<LayoutType> {
        return getEnabledLanguageConfigs().map { it.layoutType }.toSet()
    }
    
    /**
     * Add language change listener
     */
    fun addLanguageChangeListener(listener: LanguageChangeListener) {
        if (!languageChangeListeners.contains(listener)) {
            languageChangeListeners.add(listener)
            Log.d(TAG, "Added language change listener")
        }
    }
    
    /**
     * Remove language change listener
     */
    fun removeLanguageChangeListener(listener: LanguageChangeListener) {
        languageChangeListeners.remove(listener)
        Log.d(TAG, "Removed language change listener")
    }
    
    /**
     * Save current language to preferences
     */
    private fun saveCurrentLanguage() {
        preferences.edit()
            .putString(KEY_CURRENT_LANGUAGE, currentLanguage)
            .apply()
    }
    
    /**
     * Save enabled languages to preferences
     */
    private fun saveEnabledLanguages() {
        preferences.edit()
            .putStringSet(KEY_ENABLED_LANGUAGES, enabledLanguages)
            .apply()
    }
    
    /**
     * Get languages that can be suggested based on recent usage
     */
    fun getSuggestedLanguages(): List<String> {
        // This could be enhanced with usage analytics
        val allSupported = LanguageConfigs.SUPPORTED_LANGUAGES.keys
        val notEnabled = allSupported.filter { !enabledLanguages.contains(it) }
        return notEnabled.take(3) // Suggest top 3 non-enabled languages
    }
    
    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        currentLanguage = "en"
        enabledLanguages = mutableSetOf("en")
        preferences.edit().clear().apply()
        saveCurrentLanguage()
        saveEnabledLanguages()
        Log.d(TAG, "Reset to default language settings")
    }
}
