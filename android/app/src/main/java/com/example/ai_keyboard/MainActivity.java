package com.example.ai_keyboard;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodInfo;
import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodCall;
import java.util.List;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "ai_keyboard/config";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
            .setMethodCallHandler((call, result) -> {
                switch (call.method) {
                    case "isKeyboardEnabled":
                        result.success(isKeyboardEnabled());
                        break;
                    case "isKeyboardActive":
                        result.success(isKeyboardActive());
                        break;
                    case "openKeyboardSettings":
                        openKeyboardSettings();
                        result.success(true);
                        break;
                    case "openInputMethodPicker":
                        openInputMethodPicker();
                        result.success(true);
                        break;
                    case "updateSettings":
                        String theme = call.argument("theme");
                        Boolean aiSuggestions = call.argument("aiSuggestions");
                        Boolean swipeTyping = call.argument("swipeTyping");
                        Boolean voiceInput = call.argument("voiceInput");
                        updateKeyboardSettings(theme, aiSuggestions, swipeTyping, voiceInput);
                        result.success(true);
                        break;
                    default:
                        result.notImplemented();
                        break;
                }
            });
    }

    private boolean isKeyboardEnabled() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                String packageName = getPackageName();
                List<InputMethodInfo> enabledInputMethods = imm.getEnabledInputMethodList();
                for (InputMethodInfo inputMethodInfo : enabledInputMethods) {
                    if (packageName.equals(inputMethodInfo.getPackageName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to true to avoid blocking the UI
            return true;
        }
        return false;
    }

    private boolean isKeyboardActive() {
        try {
            String packageName = getPackageName();
            String inputMethodId = packageName + "/" + packageName + ".AIKeyboardService";
            
            String currentInputMethod = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD
            );
            
            return inputMethodId.equals(currentInputMethod);
        } catch (Exception e) {
            // Fallback to false to encourage user to set it up
            return false;
        }
    }

    private void openKeyboardSettings() {
        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openInputMethodPicker() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showInputMethodPicker();
        }
    }

    private void updateKeyboardSettings(String theme, Boolean aiSuggestions, Boolean swipeTyping, Boolean voiceInput) {
        // Store settings in SharedPreferences for the keyboard service to read
        getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("keyboard_theme", theme)
            .putBoolean("ai_suggestions", aiSuggestions != null ? aiSuggestions : true)
            .putBoolean("swipe_typing", swipeTyping != null ? swipeTyping : true)
            .putBoolean("voice_input", voiceInput != null ? voiceInput : true)
            .apply();
    }
}
