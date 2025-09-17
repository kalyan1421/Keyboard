package com.example.ai_keyboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
                                
                                withContext(Dispatchers.IO) {
                                    updateKeyboardSettings(theme, aiSuggestions, swipeTyping, voiceInput, vibration, keyPreview)
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
        keyPreview: Boolean
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
        } catch (e: Exception) {
            // Ignore broadcast errors to prevent crashes
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
