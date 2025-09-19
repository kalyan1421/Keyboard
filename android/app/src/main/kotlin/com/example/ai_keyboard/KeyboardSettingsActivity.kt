package com.example.ai_keyboard

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout

class KeyboardSettingsActivity : Activity() {
    
    private lateinit var settings: SharedPreferences
    private lateinit var vibrationCheckbox: CheckBox
    private lateinit var swipeTypingCheckbox: CheckBox
    private lateinit var keyPreviewCheckbox: CheckBox
    private lateinit var aiSuggestionsCheckbox: CheckBox
    private lateinit var voiceInputCheckbox: CheckBox
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        settings = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
        
        // Create simple settings UI
        createSettingsUI()
        loadCurrentSettings()
        setupListeners()
    }
    
    private fun createSettingsUI() {
        // Create a simple programmatic UI
        // In a real app, you'd use a proper XML layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Vibration setting
        vibrationCheckbox = CheckBox(this).apply {
            text = "Enable Vibration"
        }
        layout.addView(vibrationCheckbox)
        
        // Swipe typing setting
        swipeTypingCheckbox = CheckBox(this).apply {
            text = "Enable Swipe Typing"
        }
        layout.addView(swipeTypingCheckbox)
        
        // Key preview setting
        keyPreviewCheckbox = CheckBox(this).apply {
            text = "Show Key Preview"
        }
        layout.addView(keyPreviewCheckbox)
        
        // AI suggestions setting
        aiSuggestionsCheckbox = CheckBox(this).apply {
            text = "Enable AI Suggestions"
        }
        layout.addView(aiSuggestionsCheckbox)
        
        // Voice input setting
        voiceInputCheckbox = CheckBox(this).apply {
            text = "Enable Voice Input"
        }
        layout.addView(voiceInputCheckbox)
        
        setContentView(layout)
    }
    
    private fun loadCurrentSettings() {
        vibrationCheckbox.isChecked = settings.getBoolean("vibration_enabled", true)
        swipeTypingCheckbox.isChecked = settings.getBoolean("swipe_typing", true)
        keyPreviewCheckbox.isChecked = settings.getBoolean("key_preview_enabled", false)
        aiSuggestionsCheckbox.isChecked = settings.getBoolean("ai_suggestions", true)
        voiceInputCheckbox.isChecked = settings.getBoolean("voice_input", true)
    }
    
    private fun setupListeners() {
        val listener = CompoundButton.OnCheckedChangeListener { _, _ ->
            saveSettings()
        }
        
        vibrationCheckbox.setOnCheckedChangeListener(listener)
        swipeTypingCheckbox.setOnCheckedChangeListener(listener)
        keyPreviewCheckbox.setOnCheckedChangeListener(listener)
        aiSuggestionsCheckbox.setOnCheckedChangeListener(listener)
        voiceInputCheckbox.setOnCheckedChangeListener(listener)
    }
    
    private fun saveSettings() {
        settings.edit().apply {
            putBoolean("vibration_enabled", vibrationCheckbox.isChecked)
            putBoolean("swipe_typing", swipeTypingCheckbox.isChecked)
            putBoolean("key_preview_enabled", keyPreviewCheckbox.isChecked)
            putBoolean("ai_suggestions", aiSuggestionsCheckbox.isChecked)
            putBoolean("voice_input", voiceInputCheckbox.isChecked)
            apply()
        }
    }
}
