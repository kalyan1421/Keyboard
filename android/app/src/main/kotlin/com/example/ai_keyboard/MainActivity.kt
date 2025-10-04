package com.example.ai_keyboard

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodInfo
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*


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
                                val theme = call.argument<String>("theme") ?: "default"
                                val aiSuggestions = call.argument<Boolean>("aiSuggestions") ?: true
                                val swipeTyping = call.argument<Boolean>("swipeTyping") ?: true
                                val voiceInput = call.argument<Boolean>("voiceInput") ?: true
                                val vibration = call.argument<Boolean>("vibration") ?: true
                                val keyPreview = call.argument<Boolean>("keyPreview") ?: false
                                val shiftFeedback = call.argument<Boolean>("shiftFeedback") ?: false
                                val showNumberRow = call.argument<Boolean>("showNumberRow") ?: false
                                val soundEnabled = call.argument<Boolean>("soundEnabled") ?: true
                                
                                withContext(Dispatchers.IO) {
                                    updateKeyboardSettings(theme, aiSuggestions, swipeTyping, voiceInput, vibration, keyPreview, shiftFeedback, showNumberRow, soundEnabled)
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
                                
                                Log.d("MainActivity", "🎨 Theme V2 changed: $themeName ($themeId)")
                                withContext(Dispatchers.IO) {
                                    notifyKeyboardServiceThemeChangedV2(themeId, themeName, hasThemeData)
                                }
                                result.success(true)
                            }
                            "settingsChanged" -> {
                                Log.d("MainActivity", "Settings changed broadcast requested")
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
    
    private fun notifyKeyboardServiceSettingsChanged() {
        try {
            // Send broadcast to keyboard service to reload settings
            val intent = Intent("com.example.ai_keyboard.SETTINGS_CHANGED").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
            android.util.Log.d("MainActivity", "Broadcast sent: SETTINGS_CHANGED")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error sending broadcast", e)
        }
    }
    
    private fun notifyKeyboardServiceThemeChanged() {
        try {
            android.util.Log.d("MainActivity", "Starting theme change notification process")
            
            // Log SharedPreferences state for debugging
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val themeData = prefs.getString("flutter.current_theme_data", null)
            val themeId = prefs.getString("flutter.current_theme_id", null)
            android.util.Log.d("MainActivity", "Theme data - ID: $themeId, Data length: ${themeData?.length ?: 0}")
            
            // Ensure SharedPreferences are flushed before sending broadcast
            // Note: Using apply() to ensure async write is committed
            
            // Add small delay to ensure SharedPreferences are written to disk
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent("com.example.ai_keyboard.THEME_CHANGED").apply {
                    setPackage(packageName)
                    // Add theme info to intent for debugging
                    putExtra("theme_id", themeId)
                    putExtra("has_theme_data", themeData != null)
                }
                sendBroadcast(intent)
                android.util.Log.d("MainActivity", "Theme broadcast sent successfully with delay")
            }, 50) // 50ms delay should be sufficient
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to send theme change broadcast", e)
        }
    }
    
    private fun notifyKeyboardServiceThemeChangedV2(themeId: String, themeName: String, hasThemeData: Boolean) {
        try {
            android.util.Log.d("MainActivity", "Starting Theme V2 change notification: $themeName")
            
            // Log V2 SharedPreferences state for debugging
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val themeV2Data = prefs.getString("flutter.theme.v2.json", null)
            val settingsChanged = prefs.getBoolean("flutter.keyboard_settings.settings_changed", false)
            android.util.Log.d("MainActivity", "Theme V2 data - ID: $themeId, Data length: ${themeV2Data?.length ?: 0}, Settings changed: $settingsChanged")
            
            // Send immediate broadcast for theme change
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent("com.example.ai_keyboard.THEME_CHANGED").apply {
                    setPackage(packageName)
                    putExtra("theme_id", themeId)
                    putExtra("theme_name", themeName)
                    putExtra("has_theme_data", hasThemeData)
                    putExtra("is_v2_theme", true)
                }
                sendBroadcast(intent)
                android.util.Log.d("MainActivity", "🎨 Theme V2 broadcast sent: $themeName")
            }, 10) // Minimal delay for V2
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to send Theme V2 broadcast", e)
        }
    }
    
    private fun sendSettingsChangedBroadcast() {
        try {
            val intent = Intent("com.example.ai_keyboard.SETTINGS_CHANGED").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
            android.util.Log.d("MainActivity", "Settings changed broadcast sent")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to send settings broadcast", e)
        }
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
            android.util.Log.e("MainActivity", "Error saving clipboard templates", e)
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
            android.util.Log.d("MainActivity", "Clipboard settings broadcast sent")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to send clipboard settings broadcast", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
