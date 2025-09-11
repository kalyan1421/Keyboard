package com.example.ai_keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class KeyboardSettingsActivity extends Activity {
    
    private SharedPreferences settings;
    private CheckBox vibrationCheckbox;
    private CheckBox swipeTypingCheckbox;
    private CheckBox keyPreviewCheckbox;
    private CheckBox aiSuggestionsCheckbox;
    private CheckBox voiceInputCheckbox;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE);
        
        // Create simple settings UI
        createSettingsUI();
        loadCurrentSettings();
        setupListeners();
    }
    
    private void createSettingsUI() {
        // For now, we'll use a simple programmatic UI
        // In a real app, you'd use a proper XML layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        // Vibration setting
        vibrationCheckbox = new CheckBox(this);
        vibrationCheckbox.setText("Enable Vibration");
        layout.addView(vibrationCheckbox);
        
        // Swipe typing setting
        swipeTypingCheckbox = new CheckBox(this);
        swipeTypingCheckbox.setText("Enable Swipe Typing");
        layout.addView(swipeTypingCheckbox);
        
        // Key preview setting
        keyPreviewCheckbox = new CheckBox(this);
        keyPreviewCheckbox.setText("Show Key Preview");
        layout.addView(keyPreviewCheckbox);
        
        // AI suggestions setting
        aiSuggestionsCheckbox = new CheckBox(this);
        aiSuggestionsCheckbox.setText("Enable AI Suggestions");
        layout.addView(aiSuggestionsCheckbox);
        
        // Voice input setting
        voiceInputCheckbox = new CheckBox(this);
        voiceInputCheckbox.setText("Enable Voice Input");
        layout.addView(voiceInputCheckbox);
        
        setContentView(layout);
    }
    
    private void loadCurrentSettings() {
        vibrationCheckbox.setChecked(settings.getBoolean("vibration_enabled", true));
        swipeTypingCheckbox.setChecked(settings.getBoolean("swipe_typing", true));
        keyPreviewCheckbox.setChecked(settings.getBoolean("key_preview_enabled", false));
        aiSuggestionsCheckbox.setChecked(settings.getBoolean("ai_suggestions", true));
        voiceInputCheckbox.setChecked(settings.getBoolean("voice_input", true));
    }
    
    private void setupListeners() {
        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveSettings();
            }
        };
        
        vibrationCheckbox.setOnCheckedChangeListener(listener);
        swipeTypingCheckbox.setOnCheckedChangeListener(listener);
        keyPreviewCheckbox.setOnCheckedChangeListener(listener);
        aiSuggestionsCheckbox.setOnCheckedChangeListener(listener);
        voiceInputCheckbox.setOnCheckedChangeListener(listener);
    }
    
    private void saveSettings() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("vibration_enabled", vibrationCheckbox.isChecked());
        editor.putBoolean("swipe_typing", swipeTypingCheckbox.isChecked());
        editor.putBoolean("key_preview_enabled", keyPreviewCheckbox.isChecked());
        editor.putBoolean("ai_suggestions", aiSuggestionsCheckbox.isChecked());
        editor.putBoolean("voice_input", voiceInputCheckbox.isChecked());
        editor.apply();
    }
}
