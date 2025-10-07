package com.example.ai_keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import com.example.ai_keyboard.utils.LogUtil
import com.example.ai_keyboard.utils.BroadcastManager


class MainActivity : FlutterActivity() {
    companion object {
        private const val CHANNEL = "ai_keyboard/config"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                coroutineScope.launch {
                    try {
                        when (call.method) {
                            "isKeyboardEnabled" -> {
                                val isEnabled = withContext(Dispatchers.IO) { isKeyboardEnabled() }
                                result.success(isEnabled)
                            }
                            "isKeyboardActive" -> {
                                val isActive = withContext(Dispatchers.IO) { isKeyboardActive() }
                                result.success(isActive)
                            }
                            "openKeyboardSettings" -> {
                                openKeyboardSettings()
                                result.success(true)
                            }
                            "openInputMethodPicker" -> {
                                openInputMethodPicker()
                                result.success(true)
                            }
                            "updateSettings" -> {
                                // Enhanced settings with Gboard + CleverType features
                                val theme = call.argument<String>("theme") ?: "default"
                                val popupEnabled = call.argument<Boolean>("popupEnabled") ?: false
                                val aiSuggestions = call.argument<Boolean>("aiSuggestions") ?: true
                                val autocorrect = call.argument<Boolean>("autoCorrect") ?: true  // ✅ Fixed: Match Flutter camelCase
                                val emojiSuggestions = call.argument<Boolean>("emojiSuggestions") ?: true
                                val nextWordPrediction = call.argument<Boolean>("nextWordPrediction") ?: true
                                val clipboardEnabled = call.argument<Boolean>("clipboardEnabled") ?: true
                                val clipboardWindowSec = call.argument<Int>("clipboardWindowSec") ?: 60
                                val clipboardHistoryItems = call.argument<Int>("clipboardHistoryItems") ?: 20
                                val dictionaryEnabled = call.argument<Boolean>("dictionaryEnabled") ?: true
                                val autoCapitalization = call.argument<Boolean>("autoCapitalization") ?: true
                                val doubleSpacePeriod = call.argument<Boolean>("doubleSpacePeriod") ?: true
                                val soundEnabled = call.argument<Boolean>("soundEnabled") ?: true
                                val soundVolume = call.argument<Double>("soundVolume") ?: 0.5
                                val vibrationEnabled = call.argument<Boolean>("vibrationEnabled") ?: true
                                val vibrationMs = call.argument<Int>("vibrationMs") ?: 50
                                
                                // Legacy settings (backwards compat)
                                val swipeTyping = call.argument<Boolean>("swipeTyping") ?: true
                                val voiceInput = call.argument<Boolean>("voiceInput") ?: true
                                val shiftFeedback = call.argument<Boolean>("shiftFeedback") ?: false
                                val showNumberRow = call.argument<Boolean>("showNumberRow") ?: false
                                
                                withContext(Dispatchers.IO) {
                                    updateKeyboardSettingsV2(
                                        theme, popupEnabled, aiSuggestions, autocorrect, 
                                        emojiSuggestions, nextWordPrediction, clipboardEnabled,
                                        clipboardWindowSec, clipboardHistoryItems, dictionaryEnabled,
                                        autoCapitalization, doubleSpacePeriod, soundEnabled,
                                        soundVolume, vibrationEnabled, vibrationMs,
                                        swipeTyping, voiceInput, shiftFeedback, showNumberRow
                                    )
                                }
                                LogUtil.d("MainActivity", "✓ Settings updated via MethodChannel")
                                result.success(true)
                            }
                            "notifyConfigChange" -> {
                                // Unified method for all config changes (settings + themes)
                                LogUtil.d("MainActivity", "✓ notifyConfigChange received")
                                withContext(Dispatchers.IO) {
                                    sendSettingsChangedBroadcast()
                                }
                                result.success(true)
                            }
                            "broadcastSettingsChanged" -> {
                                // Force immediate broadcast to keyboard service
                                LogUtil.d("MainActivity", "✓ broadcastSettingsChanged received - forcing immediate update")
                                withContext(Dispatchers.IO) {
                                    sendSettingsChangedBroadcast()
                                }
                                result.success(true)
                            }
                            "notifyThemeChange" -> {
                                withContext(Dispatchers.IO) {
                                    notifyKeyboardServiceThemeChanged()
                                }
                                result.success(true)
                            }
                            "themeChanged" -> {
                                val themeId = call.argument<String>("themeId") ?: "default_theme"
                                val themeName = call.argument<String>("themeName") ?: "Unknown Theme"
                                val hasThemeData = call.argument<Boolean>("hasThemeData") ?: false
                                
                                LogUtil.d("MainActivity", "🎨 Theme V2 changed: $themeName ($themeId)")
                                withContext(Dispatchers.IO) {
                                    notifyKeyboardServiceThemeChangedV2(themeId, themeName, hasThemeData)
                                }
                                result.success(true)
                            }
                            "updateTheme" -> {
                                // Unified theme update from Flutter with specific color values
                                val keyboardBg = call.argument<String>("keyboard_theme_bg")
                                val keyColor = call.argument<String>("keyboard_key_color")
                                
                                LogUtil.d("MainActivity", "🎨 updateTheme called: bg=$keyboardBg, key=$keyColor")
                                
                                withContext(Dispatchers.IO) {
                                    val prefs = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
                                    prefs.edit().apply {
                                        keyboardBg?.let { putString("keyboard_theme_bg", it) }
                                        keyColor?.let { putString("keyboard_key_color", it) }
                                        apply()
                                    }
                                    
                                    // Notify keyboard service to apply theme to panels
                                    notifyKeyboardServiceThemeChanged()
                                }
                                result.success(true)
                            }
                            "settingsChanged" -> {
                                LogUtil.d("MainActivity", "Settings changed broadcast requested")
                                withContext(Dispatchers.IO) {
                                    sendSettingsChangedBroadcast()
                                }
                                result.success(true)
                            }
                            "updateClipboardSettings" -> {
                                val enabled = call.argument<Boolean>("enabled") ?: true
                                val maxHistorySize = call.argument<Int>("maxHistorySize") ?: 20
                                val autoExpiryEnabled = call.argument<Boolean>("autoExpiryEnabled") ?: true
                                val expiryDurationMinutes = call.argument<Long>("expiryDurationMinutes") ?: 60L
                                val templates = call.argument<List<Map<String, Any>>>("templates") ?: emptyList()
                                
                                withContext(Dispatchers.IO) {
                                    updateClipboardSettings(enabled, maxHistorySize, autoExpiryEnabled, expiryDurationMinutes, templates)
                                }
                                result.success(true)
                            }
                            "getEmojiSettings" -> {
                                val settings = withContext(Dispatchers.IO) { getEmojiSettings() }
                                result.success(settings)
                            }
                            "updateEmojiSettings" -> {
                                val skinTone = call.argument<String>("skinTone") ?: ""
                                val historyMaxSize = call.argument<Int>("historyMaxSize") ?: 90
                                
                                withContext(Dispatchers.IO) {
                                    updateEmojiSettings(skinTone, historyMaxSize)
                                }
                                result.success(true)
                            }
                            "getEmojiConfig" -> {
                                val config = withContext(Dispatchers.IO) { getEmojiConfig() }
                                result.success(config)
                            }
                            "updateEmojiConfig" -> {
                                val skinTone = call.argument<String>("skinTone") ?: ""
                                val recent = call.argument<List<String>>("recent") ?: emptyList()
                                
                                withContext(Dispatchers.IO) {
                                    updateEmojiConfig(skinTone, recent)
                                }
                                result.success(true)
                            }
                            "sendBroadcast" -> {
                                val action = call.argument<String>("action") ?: ""
                                LogUtil.d("MainActivity", "sendBroadcast called with action: $action")
                                withContext(Dispatchers.IO) {
                                    sendBroadcast(action)
                                }
                                result.success(true)
                            }
                            "updateCustomPrompts" -> {
                                LogUtil.d("MainActivity", "Custom prompts updated, notifying keyboard service")
                                withContext(Dispatchers.IO) {
                                    sendSettingsChangedBroadcast()
                                }
                                result.success(true)
                            }
                            "clearLearnedWords" -> {
                                LogUtil.d("MainActivity", "Clearing learned words")
                                withContext(Dispatchers.IO) {
                                    clearUserLearnedWords()
                                }
                                result.success(true)
                            }
                            "setEnabledLanguages" -> {
                                val enabled = call.argument<List<String>>("enabled") ?: emptyList()
                                val current = call.argument<String>("current") ?: "en"
                                LogUtil.d("MainActivity", "Setting enabled languages: $enabled, current: $current")
                                withContext(Dispatchers.IO) {
                                    setEnabledLanguages(enabled, current)
                                }
                                result.success(true)
                            }
                            "setCurrentLanguage" -> {
                                val language = call.argument<String>("language") ?: "en"
                                LogUtil.d("MainActivity", "Setting current language: $language")
                                withContext(Dispatchers.IO) {
                                    setCurrentLanguage(language)
                                }
                                result.success(true)
                            }
                            "setMultilingual" -> {
                                val enabled = call.argument<Boolean>("enabled") ?: false
                                LogUtil.d("MainActivity", "Setting multilingual mode: $enabled")
                                withContext(Dispatchers.IO) {
                                    setMultilingualMode(enabled)
                                }
                                result.success(true)
                            }
                            "setTransliterationEnabled" -> {
                                val enabled = call.argument<Boolean>("enabled") ?: true
                                LogUtil.d("MainActivity", "Setting transliteration: $enabled")
                                withContext(Dispatchers.IO) {
                                    setTransliterationEnabled(enabled)
                                }
                                result.success(true)
                            }
                            "setReverseTransliterationEnabled" -> {
                                val enabled = call.argument<Boolean>("enabled") ?: false
                                LogUtil.d("MainActivity", "Setting reverse transliteration: $enabled")
                                withContext(Dispatchers.IO) {
                                    setReverseTransliterationEnabled(enabled)
                                }
                                result.success(true)
                            }
                            else -> result.notImplemented()
                        }
                    } catch (e: Exception) {
                        result.error("ERROR", "Failed to execute method: ${call.method}", e.message)
                    }
                }
            }
    }

    private suspend fun isKeyboardEnabled(): Boolean = withContext(Dispatchers.IO) {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.let { inputMethodManager ->
                val packageName = packageName
                val enabledInputMethods = inputMethodManager.enabledInputMethodList
                enabledInputMethods.any { it.packageName == packageName }
            } ?: true // Fallback to true to avoid blocking the UI
        } catch (e: Exception) {
            true // Fallback to true to avoid blocking the UI
        }
    }

    private suspend fun isKeyboardActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            val packageName = packageName
            val inputMethodId = "$packageName/$packageName.AIKeyboardService"
            
            val currentInputMethod = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            
            // Check if our keyboard service is currently selected
            val isDefault = inputMethodId == currentInputMethod
            
            // Additional check: see if our keyboard is in the current input method
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            val containsPackage = currentIme?.contains(packageName) == true
            
            isDefault || containsPackage
        } catch (e: Exception) {
            false // Fallback to false to encourage user to set it up
        }
    }

    private fun openKeyboardSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun openInputMethodPicker() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }

    private suspend fun updateKeyboardSettings(
        theme: String,
        aiSuggestions: Boolean,
        swipeTyping: Boolean,
        voiceInput: Boolean,
        vibration: Boolean,
        keyPreview: Boolean,
        shiftFeedback: Boolean,
        showNumberRow: Boolean,
        soundEnabled: Boolean
    ) = withContext(Dispatchers.IO) {
        // Store settings in SharedPreferences for the keyboard service to read
        getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("keyboard_theme", theme)
            .putBoolean("ai_suggestions", aiSuggestions)
            .putBoolean("swipe_typing", swipeTyping)
            .putBoolean("voice_input", voiceInput)
            .putBoolean("vibration_enabled", vibration)
            .putBoolean("key_preview_enabled", keyPreview)
            .putBoolean("show_shift_feedback", shiftFeedback)
            .putBoolean("show_number_row", showNumberRow)
            .putBoolean("sound_enabled", soundEnabled)
            .apply()
            
        // Notify keyboard service to reload settings immediately
        notifyKeyboardServiceSettingsChanged()
    }
    
    private suspend fun updateKeyboardSettingsV2(
        theme: String,
        popupEnabled: Boolean,
        aiSuggestions: Boolean,
        autocorrect: Boolean,
        emojiSuggestions: Boolean,
        nextWordPrediction: Boolean,
        clipboardEnabled: Boolean,
        clipboardWindowSec: Int,
        clipboardHistoryItems: Int,
        dictionaryEnabled: Boolean,
        autoCapitalization: Boolean,
        doubleSpacePeriod: Boolean,
        soundEnabled: Boolean,
        soundVolume: Double,
        vibrationEnabled: Boolean,
        vibrationMs: Int,
        swipeTyping: Boolean,
        voiceInput: Boolean,
        shiftFeedback: Boolean,
        showNumberRow: Boolean
    ) = withContext(Dispatchers.IO) {
        // Store enhanced settings in SharedPreferences
        val prefs = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("keyboard_theme", theme)
            .putBoolean("popup_enabled", popupEnabled)
            .putBoolean("ai_suggestions", aiSuggestions)
            .putBoolean("auto_correct", autocorrect)  // ✅ Fixed: Match AIKeyboardService key
            .putBoolean("emoji_suggestions", emojiSuggestions)
            .putBoolean("next_word_prediction", nextWordPrediction)
            .putBoolean("clipboard_suggestions_enabled", clipboardEnabled)
            .putInt("clipboard_window_sec", clipboardWindowSec)
            .putInt("clipboard_history_items", clipboardHistoryItems)
            .putBoolean("dictionary_enabled", dictionaryEnabled)
            .putBoolean("auto_capitalization", autoCapitalization)
            .putBoolean("double_space_period", doubleSpacePeriod)
            .putBoolean("sound_enabled", soundEnabled)
            .putFloat("sound_volume", soundVolume.toFloat())
            .putBoolean("vibration_enabled", vibrationEnabled)
            .putInt("vibration_ms", vibrationMs)
            // Legacy settings
            .putBoolean("swipe_typing", swipeTyping)
            .putBoolean("voice_input", voiceInput)
            .putBoolean("show_shift_feedback", shiftFeedback)
            .putBoolean("show_number_row", showNumberRow)
            .apply()
            
        LogUtil.d("MainActivity", "✓ Settings V2 persisted to SharedPreferences")
        LogUtil.d("TypingSync", "Settings applied: popup=$popupEnabled, autocorrect=$autocorrect, emoji=$emojiSuggestions, nextWord=$nextWordPrediction, clipboard=$clipboardEnabled")
        
        // Notify keyboard service to reload settings immediately
        notifyKeyboardServiceSettingsChanged()
    }
    
    private fun notifyKeyboardServiceSettingsChanged() {
        BroadcastManager.sendToKeyboard(this, "com.example.ai_keyboard.SETTINGS_CHANGED")
    }
    
    private fun notifyKeyboardServiceThemeChanged() {
        try {
            LogUtil.d("MainActivity", "Starting theme change notification process")
            
            // Log SharedPreferences state for debugging
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val themeData = prefs.getString("flutter.current_theme_data", null)
            val themeId = prefs.getString("flutter.current_theme_id", null)
            LogUtil.d("MainActivity", "Theme data - ID: $themeId, Data length: ${themeData?.length ?: 0}")
            
            // Send broadcast with theme extras
            val extras = Bundle().apply {
                putString("theme_id", themeId)
                putBoolean("has_theme_data", themeData != null)
            }
            BroadcastManager.sendToKeyboard(this, "com.example.ai_keyboard.THEME_CHANGED", extras)
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "Failed to send theme change broadcast", e)
        }
    }
    
    private fun notifyKeyboardServiceThemeChangedV2(themeId: String, themeName: String, hasThemeData: Boolean) {
        try {
            LogUtil.d("MainActivity", "Starting Theme V2 change notification: $themeName")
            
            // Log V2 SharedPreferences state for debugging
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val themeV2Data = prefs.getString("flutter.theme.v2.json", null)
            val settingsChanged = prefs.getBoolean("flutter.keyboard_settings.settings_changed", false)
            LogUtil.d("MainActivity", "Theme V2 data - ID: $themeId, Data length: ${themeV2Data?.length ?: 0}, Settings changed: $settingsChanged")
            
            // Send broadcast with theme V2 extras
            val extras = Bundle().apply {
                putString("theme_id", themeId)
                putString("theme_name", themeName)
                putBoolean("has_theme_data", hasThemeData)
                putBoolean("is_v2_theme", true)
            }
            BroadcastManager.sendToKeyboard(this, "com.example.ai_keyboard.THEME_CHANGED", extras)
            LogUtil.d("MainActivity", "🎨 Theme V2 broadcast sent: $themeName")
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "Failed to send Theme V2 broadcast", e)
        }
    }
    
    private fun sendSettingsChangedBroadcast() {
        BroadcastManager.sendToKeyboard(this, "com.example.ai_keyboard.SETTINGS_CHANGED")
    }
    
    private fun sendBroadcast(action: String) {
        BroadcastManager.sendToKeyboard(this, action)
    }
    
    private suspend fun updateClipboardSettings(
        enabled: Boolean,
        maxHistorySize: Int,
        autoExpiryEnabled: Boolean,
        expiryDurationMinutes: Long,
        templates: List<Map<String, Any>>
    ) = withContext(Dispatchers.IO) {
        // Store clipboard settings in SharedPreferences
        getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("clipboard_enabled", enabled)
            .putInt("max_history_size", maxHistorySize)
            .putBoolean("auto_expiry_enabled", autoExpiryEnabled)
            .putLong("expiry_duration_minutes", expiryDurationMinutes)
            .commit() // Use commit() for immediate persistence
            
        // Store templates
        try {
            val templatesJson = org.json.JSONArray()
            templates.forEach { templateMap ->
                val templateJson = org.json.JSONObject().apply {
                    put("text", templateMap["text"] ?: "")
                    put("category", templateMap["category"] ?: "")
                    put("isTemplate", true)
                    put("isPinned", true)
                    put("id", templateMap["id"] ?: java.util.UUID.randomUUID().toString())
                    put("timestamp", System.currentTimeMillis())
                }
                templatesJson.put(templateJson)
            }
            
            getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)
                .edit()
                .putString("template_items", templatesJson.toString())
                .commit()
                
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "Error saving clipboard templates", e)
        }
        
        // Notify keyboard service to reload clipboard settings
        notifyKeyboardServiceClipboardChanged()
    }
    
    private fun notifyKeyboardServiceClipboardChanged() {
        try {
            val intent = Intent("com.example.ai_keyboard.CLIPBOARD_CHANGED").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
            LogUtil.d("MainActivity", "Clipboard settings broadcast sent")
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "Failed to send clipboard settings broadcast", e)
        }
    }
    
    private fun getEmojiSettings(): Map<String, Any> {
        val prefs = getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val skinTone = prefs.getString("preferred_skin_tone", "🏽") ?: "🏽"
        val historyMaxSize = prefs.getInt("emoji_history_max_size", 90)
        val historyJson = prefs.getString("emoji_history", "[]") ?: "[]"
        
        // Parse history list
        val history = mutableListOf<String>()
        try {
            val cleanJson = historyJson.trim('[', ']')
            if (cleanJson.isNotEmpty()) {
                cleanJson.split(",").forEach { emoji ->
                    val cleanEmoji = emoji.trim().trim('"')
                    if (cleanEmoji.isNotEmpty()) {
                        history.add(cleanEmoji)
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "Error parsing emoji history", e)
        }
        
        return mapOf(
            "skinTone" to skinTone,
            "historyMaxSize" to historyMaxSize,
            "history" to history
        )
    }
    
    private fun updateEmojiSettings(skinTone: String, historyMaxSize: Int) {
        val prefs = getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("preferred_skin_tone", skinTone)
            .putInt("emoji_history_max_size", historyMaxSize)
            .apply()
        
        LogUtil.d("MainActivity", "Emoji settings updated: skinTone=$skinTone, historyMaxSize=$historyMaxSize")
        
        // Notify keyboard service to reload emoji settings
        notifyKeyboardServiceEmojiChanged()
    }
    
    private fun notifyKeyboardServiceEmojiChanged() {
        try {
            val intent = Intent("com.example.ai_keyboard.EMOJI_SETTINGS_CHANGED").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
            LogUtil.d("MainActivity", "Emoji settings broadcast sent")
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "Failed to send emoji settings broadcast", e)
        }
    }
    
    private fun getEmojiConfig(): Map<String, Any> {
        val prefs = getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val skinTone = prefs.getString("preferred_skin_tone", "🏽") ?: "🏽"
        val historyJson = prefs.getString("emoji_history", "[]") ?: "[]"
        
        val recent = mutableListOf<String>()
        try {
            val cleanJson = historyJson.trim('[', ']')
            if (cleanJson.isNotEmpty()) {
                cleanJson.split(",").take(40).forEach { emoji ->
                    val cleanEmoji = emoji.trim().trim('"')
                    if (cleanEmoji.isNotEmpty()) {
                        recent.add(cleanEmoji)
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "Error parsing emoji history", e)
        }
        
        return mapOf(
            "skinTone" to skinTone,
            "recent" to recent
        )
    }
    
    private fun updateEmojiConfig(skinTone: String, recent: List<String>) {
        val prefs = getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val historyJson = recent.joinToString(",") { "\"$it\"" }
        
        prefs.edit()
            .putString("preferred_skin_tone", skinTone)
            .putString("emoji_history", "[$historyJson]")
            .apply()
        
        LogUtil.d("MainActivity", "Emoji config updated: skinTone=$skinTone, recent=${recent.size}")
        notifyKeyboardServiceEmojiChanged()
    }

    private fun clearUserLearnedWords() {
        try {
            // Clear the local file
            val userWordsFile = java.io.File(filesDir, "user_words.json")
            if (userWordsFile.exists()) {
                userWordsFile.delete()
                LogUtil.d("MainActivity", "Local user words file deleted")
            }
            
            // Send broadcast to keyboard service to clear words
            val intent = Intent("com.example.ai_keyboard.CLEAR_USER_WORDS").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
            
            LogUtil.d("MainActivity", "✅ Clear learned words request sent to keyboard service")
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "❌ Error clearing learned words", e)
            throw e
        }
    }

    private fun setEnabledLanguages(languages: List<String>, current: String) {
        try {
            // Store in FlutterSharedPreferences format
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            
            // Store enabled languages as comma-separated string
            prefs.edit()
                .putString("flutter.enabled_languages", languages.joinToString(","))
                .putString("flutter.current_language", current)
                .apply()
            
            LogUtil.d("MainActivity", "✅ Enabled languages saved: $languages")
            
            // Send broadcast to keyboard service
            sendLanguageChangeBroadcast(current)
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "❌ Error setting enabled languages", e)
        }
    }
    
    private fun setCurrentLanguage(language: String) {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("flutter.current_language", language)
                .apply()
            
            LogUtil.d("MainActivity", "✅ Current language set to: $language")
            
            // Send broadcast to keyboard service
            sendLanguageChangeBroadcast(language)
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "❌ Error setting current language", e)
        }
    }
    
    private fun setMultilingualMode(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("flutter.multilingual_enabled", enabled)
                .apply()
            
            LogUtil.d("MainActivity", "✅ Multilingual mode set to: $enabled")
            
            // Send broadcast to keyboard service
            val intent = Intent("com.example.ai_keyboard.LANGUAGE_CHANGED").apply {
                setPackage(packageName)
                putExtra("multilingual_enabled", enabled)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "❌ Error setting multilingual mode", e)
        }
    }
    
    private fun sendLanguageChangeBroadcast(language: String) {
        try {
            val intent = Intent("com.example.ai_keyboard.LANGUAGE_CHANGED").apply {
                setPackage(packageName)
                putExtra("language", language)
            }
            sendBroadcast(intent)
            LogUtil.d("MainActivity", "✅ Language change broadcast sent: $language")
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "❌ Failed to send language change broadcast", e)
        }
    }
    
    // Phase 2: Transliteration toggles
    private fun setTransliterationEnabled(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("flutter.transliteration_enabled", enabled)
                .apply()
            
            LogUtil.d("MainActivity", "✅ Transliteration enabled set to: $enabled")
            
            // Send broadcast to keyboard service
            val intent = Intent("com.example.ai_keyboard.LANGUAGE_CHANGED").apply {
                setPackage(packageName)
                putExtra("transliteration_enabled", enabled)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "❌ Error setting transliteration enabled", e)
        }
    }
    
    private fun setReverseTransliterationEnabled(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("flutter.reverse_transliteration_enabled", enabled)
                .apply()
            
            LogUtil.d("MainActivity", "✅ Reverse transliteration enabled set to: $enabled")
            
            // Send broadcast to keyboard service
            val intent = Intent("com.example.ai_keyboard.LANGUAGE_CHANGED").apply {
                setPackage(packageName)
                putExtra("reverse_transliteration_enabled", enabled)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            LogUtil.e("MainActivity", "❌ Error setting reverse transliteration enabled", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
