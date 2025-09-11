package com.example.ai_keyboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodInfo
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private companion object {
        const val CHANNEL = "ai_keyboard/config"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "isKeyboardEnabled" -> {
                        result.success(isKeyboardEnabled())
                    }
                    "isKeyboardActive" -> {
                        result.success(isKeyboardActive())
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
                        val theme = call.argument<String>("theme")
                        val aiSuggestions = call.argument<Boolean>("aiSuggestions")
                        val swipeTyping = call.argument<Boolean>("swipeTyping")
                        val voiceInput = call.argument<Boolean>("voiceInput")
                        val vibration = call.argument<Boolean>("vibration")
                        val keyPreview = call.argument<Boolean>("keyPreview")
                        updateKeyboardSettings(theme, aiSuggestions, swipeTyping, voiceInput, vibration, keyPreview)
                        result.success(true)
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
    }

    private fun isKeyboardEnabled(): Boolean {
        return try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.let {
                val packageName = packageName
                val enabledInputMethods = it.enabledInputMethodList
                enabledInputMethods.any { inputMethodInfo ->
                    packageName == inputMethodInfo.packageName
                }
            } ?: false
        } catch (e: Exception) {
            // Fallback to true to avoid blocking the UI
            true
        }
    }

    private fun isKeyboardActive(): Boolean {
        return try {
            val packageName = packageName
            val inputMethodId = "$packageName/$packageName.AIKeyboardService"
            
            val currentInputMethod = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            
            // Also check if our keyboard service is currently selected
            val isDefault = inputMethodId == currentInputMethod
            
            // Additional check: see if our keyboard is in the current input method
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.let {
                val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                currentIme?.contains(packageName) == true
            } ?: isDefault
        } catch (e: Exception) {
            // Fallback to false to encourage user to set it up
            false
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

    private fun updateKeyboardSettings(
        theme: String?, 
        aiSuggestions: Boolean?, 
        swipeTyping: Boolean?, 
        voiceInput: Boolean?, 
        vibration: Boolean?, 
        keyPreview: Boolean?
    ) {
        // Store settings in SharedPreferences for the keyboard service to read
        getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("keyboard_theme", theme)
            .putBoolean("ai_suggestions", aiSuggestions ?: true)
            .putBoolean("swipe_typing", swipeTyping ?: true)
            .putBoolean("voice_input", voiceInput ?: true)
            .putBoolean("vibration_enabled", vibration ?: true)
            .putBoolean("key_preview_enabled", keyPreview ?: false)
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
}
