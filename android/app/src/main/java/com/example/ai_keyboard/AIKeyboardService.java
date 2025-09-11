package com.example.ai_keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    
    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private boolean caps = false;
    private long lastShiftTime;
    private boolean isShifted = false;
    private boolean swipeMode = false;
    private StringBuilder swipeBuffer = new StringBuilder();
    private List<Integer> swipePath = new ArrayList<>();
    private long swipeStartTime = 0;
    private boolean isCurrentlySwiping = false;
    
    // AI and suggestion components
    private TextView suggestionView;
    private LinearLayout suggestionContainer;
    private List<String> currentSuggestions = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Settings
    private SharedPreferences settings;
    private String currentTheme = "default";
    private boolean aiSuggestionsEnabled = true;
    private boolean swipeTypingEnabled = true;
    private boolean voiceInputEnabled = true;
    private boolean vibrationEnabled = true;
    private boolean keyPreviewEnabled = false; // Disabled by default
    
    // Keyboard layouts
    private static final int KEYBOARD_LETTERS = 1;
    private static final int KEYBOARD_SYMBOLS = 2;
    private static final int KEYBOARD_NUMBERS = 3;
    private int currentKeyboard = KEYBOARD_LETTERS;
    
    @Override
    public void onCreate() {
        super.onCreate();
        settings = getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE);
        loadSettings();
    }
    
    @Override
    public View onCreateInputView() {
        // Create the main keyboard layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(getThemeBackgroundColor());
        
        // Create suggestion bar
        createSuggestionBar(mainLayout);
        
        // Create keyboard view
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboard = new Keyboard(this, R.xml.qwerty);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        
        // Configure key preview (disable popup)
        keyboardView.setPreviewEnabled(keyPreviewEnabled);
        
        // Apply theme
        applyTheme();
        
        mainLayout.addView(keyboardView);
        return mainLayout;
    }
    
    private void createSuggestionBar(LinearLayout parent) {
        suggestionContainer = new LinearLayout(this);
        suggestionContainer.setOrientation(LinearLayout.HORIZONTAL);
        suggestionContainer.setBackgroundColor(getThemeKeyColor());
        suggestionContainer.setPadding(16, 8, 16, 8);
        
        // Add three suggestion text views
        for (int i = 0; i < 3; i++) {
            TextView suggestion = new TextView(this);
            suggestion.setTextColor(getThemeTextColor());
            suggestion.setTextSize(16);
            suggestion.setPadding(16, 8, 16, 8);
            suggestion.setBackgroundResource(R.drawable.suggestion_background);
            suggestion.setClickable(true);
            
            final int index = i;
            suggestion.setOnClickListener(v -> {
                if (index < currentSuggestions.size()) {
                    applySuggestion(currentSuggestions.get(index));
                }
            });
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(4, 0, 4, 0);
            suggestion.setLayoutParams(params);
            
            suggestionContainer.addView(suggestion);
        }
        
        parent.addView(suggestionContainer);
    }
    
    private void loadSettings() {
        currentTheme = settings.getString("keyboard_theme", "default");
        aiSuggestionsEnabled = settings.getBoolean("ai_suggestions", true);
        swipeTypingEnabled = settings.getBoolean("swipe_typing", true);
        voiceInputEnabled = settings.getBoolean("voice_input", true);
        vibrationEnabled = settings.getBoolean("vibration_enabled", true);
        keyPreviewEnabled = settings.getBoolean("key_preview_enabled", false);
    }
    
    private void applyTheme() {
        if (keyboardView != null) {
            keyboardView.setBackgroundColor(getThemeBackgroundColor());
        }
    }
    
    private int getThemeBackgroundColor() {
        switch (currentTheme) {
            case "dark": return Color.parseColor("#1E1E1E");
            case "material_you": return Color.parseColor("#6750A4");
            case "professional": return Color.parseColor("#37474F");
            case "colorful": return Color.parseColor("#E1F5FE");
            default: return Color.parseColor("#F5F5F5");
        }
    }
    
    private int getThemeKeyColor() {
        switch (currentTheme) {
            case "dark": return Color.parseColor("#2D2D2D");
            case "material_you": return Color.parseColor("#7C4DFF");
            case "professional": return Color.parseColor("#455A64");
            case "colorful": return Color.parseColor("#81D4FA");
            default: return Color.WHITE;
        }
    }
    
    private int getThemeTextColor() {
        switch (currentTheme) {
            case "dark":
            case "material_you":
            case "professional":
                return Color.WHITE;
            case "colorful": return Color.parseColor("#0D47A1");
            default: return Color.parseColor("#212121");
        }
    }
    
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        playClick(primaryCode);
        
        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                handleBackspace(ic);
                break;
            case Keyboard.KEYCODE_SHIFT:
                handleShift();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            case KEYCODE_SPACE:
                handleSpace(ic);
                break;
            case KEYCODE_SYMBOLS:
                switchToSymbols();
                break;
            case KEYCODE_LETTERS:
                switchToLetters();
                break;
            case KEYCODE_NUMBERS:
                switchToNumbers();
                break;
            case KEYCODE_VOICE:
                if (voiceInputEnabled) {
                    startVoiceInput();
                }
                break;
            default:
                handleCharacter(primaryCode, ic);
                break;
        }
        
        // Update AI suggestions after key press
        if (aiSuggestionsEnabled && primaryCode != Keyboard.KEYCODE_DELETE) {
            updateAISuggestions();
        }
    }
    
    private void handleCharacter(int primaryCode, InputConnection ic) {
        char code = (char) primaryCode;
        
        // Enhanced character handling with better case management
        if (Character.isLetter(code)) {
            if (caps) {
                code = Character.toUpperCase(code);
            } else {
                code = Character.toLowerCase(code);
            }
        }
        
        // Insert character with enhanced text processing
        insertCharacterWithProcessing(ic, code);
        
        // Enhanced auto-shift logic
        updateCapsState(code);
        
        // Update keyboard visual state
        updateKeyboardState();
    }
    
    private void insertCharacterWithProcessing(InputConnection ic, char code) {
        // Get current text context for smart processing
        CharSequence textBefore = ic.getTextBeforeCursor(10, 0);
        String context = textBefore != null ? textBefore.toString() : "";
        
        // Handle smart punctuation
        if (isSmartPunctuationEnabled() && isPunctuation(code)) {
            handleSmartPunctuation(ic, code, context);
        } else {
            // Standard character insertion
            ic.commitText(String.valueOf(code), 1);
        }
        
        // Handle auto-correction if enabled
        if (isAutoCorrectEnabled() && (code == ' ' || isPunctuation(code))) {
            performAutoCorrection(ic);
        }
    }
    
    private void updateCapsState(char code) {
        boolean wasCapitalized = caps;
        
        // Auto-shift after sentence end
        if (code == '.' || code == '!' || code == '?') {
            caps = true;
        } else if (caps && Character.isLetter(code) && !isCapslockEnabled()) {
            // Turn off caps after typing a letter (unless caps lock is on)
            caps = false;
        }
        
        // Update visual state if changed
        if (wasCapitalized != caps && keyboardView != null) {
            keyboardView.setShifted(caps);
        }
    }
    
    private void updateKeyboardState() {
        if (keyboardView != null) {
            keyboardView.invalidateAllKeys();
        }
    }
    
    private boolean isSmartPunctuationEnabled() {
        return settings.getBoolean("smart_punctuation", true);
    }
    
    private boolean isAutoCorrectEnabled() {
        return settings.getBoolean("auto_correct", true);
    }
    
    private boolean isCapslockEnabled() {
        return settings.getBoolean("caps_lock_active", false);
    }
    
    private boolean isPunctuation(char c) {
        return ".,!?;:".indexOf(c) != -1;
    }
    
    private void handleSmartPunctuation(InputConnection ic, char code, String context) {
        // Smart spacing around punctuation
        if (code == '.' || code == '!' || code == '?') {
            // Remove extra spaces before sentence-ending punctuation
            if (context.endsWith(" ")) {
                ic.deleteSurroundingText(1, 0);
            }
            ic.commitText(String.valueOf(code), 1);
        } else if (code == ',') {
            // Handle comma spacing
            ic.commitText(String.valueOf(code), 1);
        } else {
            ic.commitText(String.valueOf(code), 1);
        }
    }
    
    private void performAutoCorrection(InputConnection ic) {
        // Get the last word typed
        CharSequence textBefore = ic.getTextBeforeCursor(50, 0);
        if (textBefore == null) return;
        
        String text = textBefore.toString();
        String[] words = text.split("\\s+");
        if (words.length == 0) return;
        
        String lastWord = words[words.length - 1];
        if (lastWord.length() < 2) return;
        
        // Simple auto-correction dictionary
        String corrected = getAutoCorrection(lastWord);
        if (corrected != null && !corrected.equals(lastWord)) {
            // Replace the word with correction
            ic.deleteSurroundingText(lastWord.length(), 0);
            ic.commitText(corrected, 1);
        }
    }
    
    private String getAutoCorrection(String word) {
        // Simple auto-correction dictionary
        switch (word.toLowerCase()) {
            case "teh": return "the";
            case "adn": return "and";
            case "hte": return "the";
            case "taht": return "that";
            case "thier": return "their";
            case "recieve": return "receive";
            case "seperate": return "separate";
            case "definately": return "definitely";
            case "occured": return "occurred";
            case "begining": return "beginning";
            default: return null;
        }
    }
    
    private void handleBackspace(InputConnection ic) {
        CharSequence selectedText = ic.getSelectedText(0);
        if (TextUtils.isEmpty(selectedText)) {
            ic.deleteSurroundingText(1, 0);
        } else {
            ic.commitText("", 1);
        }
    }
    
    private void handleSpace(InputConnection ic) {
        ic.commitText(" ", 1);
        updateAISuggestions();
    }
    
    private void handleShift() {
        long now = System.currentTimeMillis();
        if (lastShiftTime + 800 > now) {
            // Double tap for caps lock
            caps = !caps;
            lastShiftTime = 0;
        } else {
            // Single tap for shift
            caps = !caps;
            lastShiftTime = now;
        }
        
        if (keyboardView != null) {
            keyboardView.setShifted(caps);
            keyboardView.invalidateAllKeys();
        }
    }
    
    private void switchToSymbols() {
        if (currentKeyboard != KEYBOARD_SYMBOLS) {
            keyboard = new Keyboard(this, R.xml.symbols);
            currentKeyboard = KEYBOARD_SYMBOLS;
            keyboardView.setKeyboard(keyboard);
        }
    }
    
    private void switchToLetters() {
        if (currentKeyboard != KEYBOARD_LETTERS) {
            keyboard = new Keyboard(this, R.xml.qwerty);
            currentKeyboard = KEYBOARD_LETTERS;
            keyboardView.setKeyboard(keyboard);
        }
    }
    
    private void switchToNumbers() {
        if (currentKeyboard != KEYBOARD_NUMBERS) {
            keyboard = new Keyboard(this, R.xml.numbers);
            currentKeyboard = KEYBOARD_NUMBERS;
            keyboardView.setKeyboard(keyboard);
        }
    }
    
    private void startVoiceInput() {
        // Placeholder for voice input implementation
        Toast.makeText(this, "Voice input feature coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void updateAISuggestions() {
        if (!aiSuggestionsEnabled) return;
        
        executorService.execute(() -> {
            try {
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) return;
                
                // Get current text context
                CharSequence textBefore = ic.getTextBeforeCursor(50, 0);
                String currentText = textBefore != null ? textBefore.toString() : "";
                
                // Generate AI suggestions (simplified version)
                List<String> suggestions = generateAISuggestions(currentText);
                
                // Update UI on main thread
                mainHandler.post(() -> updateSuggestionUI(suggestions));
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private List<String> generateAISuggestions(String currentText) {
        // Simplified AI suggestion logic
        List<String> suggestions = new ArrayList<>();
        
        if (currentText.isEmpty()) {
            suggestions.addAll(Arrays.asList("Hello", "How are", "Good"));
        } else {
            String[] words = currentText.toLowerCase().split("\\s+");
            String lastWord = words.length > 0 ? words[words.length - 1] : "";
            
            switch (lastWord) {
                case "hello":
                    suggestions.addAll(Arrays.asList("there", "everyone", "friend"));
                    break;
                case "good":
                    suggestions.addAll(Arrays.asList("morning", "evening", "night"));
                    break;
                case "how":
                    suggestions.addAll(Arrays.asList("are you", "is it", "about"));
                    break;
                case "thank":
                    suggestions.addAll(Arrays.asList("you", "you so much", "goodness"));
                    break;
                case "i":
                    suggestions.addAll(Arrays.asList("am", "will", "think"));
                    break;
                default:
                    suggestions.addAll(Arrays.asList("and", "the", "to"));
                    break;
            }
        }
        
        currentSuggestions = suggestions;
        return suggestions;
    }
    
    private void updateSuggestionUI(List<String> suggestions) {
        if (suggestionContainer == null) return;
        
        for (int i = 0; i < suggestionContainer.getChildCount() && i < 3; i++) {
            TextView suggestionView = (TextView) suggestionContainer.getChildAt(i);
            if (i < suggestions.size()) {
                suggestionView.setText(suggestions.get(i));
                suggestionView.setVisibility(View.VISIBLE);
            } else {
                suggestionView.setVisibility(View.INVISIBLE);
            }
        }
    }
    
    private void applySuggestion(String suggestion) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        // Get text before cursor to determine what to replace
        CharSequence textBefore = ic.getTextBeforeCursor(50, 0);
        if (textBefore != null) {
            String[] words = textBefore.toString().split("\\s+");
            if (words.length > 0) {
                String lastWord = words[words.length - 1];
                ic.deleteSurroundingText(lastWord.length(), 0);
            }
        }
        
        ic.commitText(suggestion + " ", 1);
        updateAISuggestions();
    }
    
    private void playClick(int keyCode) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) {
            switch (keyCode) {
                case 32: // Space
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                    break;
                case Keyboard.KEYCODE_DONE:
                case 10: // Enter
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                    break;
                case Keyboard.KEYCODE_DELETE:
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                    break;
                default:
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
            }
        }
    }
    
    @Override
    public void onPress(int primaryCode) {
        // Handle key press visual feedback
        if (keyboardView != null) {
            // Add haptic feedback only if enabled
            if (vibrationEnabled) {
                keyboardView.performHapticFeedback(
                    android.view.HapticFeedbackConstants.KEYBOARD_TAP,
                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                );
            }
            
            // Check if this could be the start of swipe typing
            if (swipeTypingEnabled && Character.isLetter(primaryCode) && !isCurrentlySwiping) {
                // Potential start of swipe typing - wait for movement or release
                swipeStartTime = System.currentTimeMillis();
            }
            
            // Visual press feedback is handled by the key background drawable
            // Additional custom animations can be added here
            animateKeyPress(primaryCode);
        }
    }
    
    @Override
    public void onRelease(int primaryCode) {
        // Handle key release visual feedback
        if (keyboardView != null) {
            animateKeyRelease(primaryCode);
        }
    }
    
    private void animateKeyPress(int primaryCode) {
        // Custom key press animation
        if (keyboardView != null) {
            // Scale effect for pressed key
            keyboardView.setScaleX(0.95f);
            keyboardView.setScaleY(0.95f);
            
            // Reset scale after short delay
            keyboardView.postDelayed(() -> {
                keyboardView.setScaleX(1.0f);
                keyboardView.setScaleY(1.0f);
            }, 100);
        }
    }
    
    private void animateKeyRelease(int primaryCode) {
        // Ensure scale is reset on release
        if (keyboardView != null) {
            keyboardView.setScaleX(1.0f);
            keyboardView.setScaleY(1.0f);
        }
    }
    
    @Override
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.commitText(text, 1);
    }
    
    @Override
    public void swipeDown() {
        if (swipeTypingEnabled && isCurrentlySwiping) {
            finishSwipeTyping();
        } else {
            handleClose();
        }
    }
    
    @Override
    public void swipeLeft() {
        if (swipeTypingEnabled && isCurrentlySwiping) {
            // Continue swipe typing
            processSwipeMovement(-1); // Left direction
        } else {
            handleBackspace(getCurrentInputConnection());
        }
    }
    
    @Override
    public void swipeRight() {
        if (swipeTypingEnabled && isCurrentlySwiping) {
            // Continue swipe typing
            processSwipeMovement(1); // Right direction
        } else {
            // Swipe right for space
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(" ", 1);
            }
        }
    }
    
    @Override
    public void swipeUp() {
        if (swipeTypingEnabled && isCurrentlySwiping) {
            // Continue swipe typing
            processSwipeMovement(0); // Up direction
        } else {
            // Swipe up for shift
            handleShift();
        }
    }
    
    // Enhanced swipe typing methods
    private void startSwipeTyping(int primaryCode) {
        if (!swipeTypingEnabled) return;
        
        isCurrentlySwiping = true;
        swipeStartTime = System.currentTimeMillis();
        swipePath.clear();
        swipeBuffer.setLength(0);
        swipePath.add(primaryCode);
        
        // Visual feedback for swipe start
        if (keyboardView != null) {
            keyboardView.setBackgroundColor(getSwipeActiveColor());
        }
    }
    
    private void processSwipeMovement(int direction) {
        if (!isCurrentlySwiping || !swipeTypingEnabled) return;
        
        // Add direction to swipe path for processing
        swipePath.add(direction);
        
        // Process swipe path to predict words
        String predictedWord = processSwipePath(swipePath);
        if (!predictedWord.isEmpty()) {
            // Update swipe buffer
            swipeBuffer.setLength(0);
            swipeBuffer.append(predictedWord);
            
            // Show prediction in suggestion bar
            showSwipePrediction(predictedWord);
        }
    }
    
    private void finishSwipeTyping() {
        if (!isCurrentlySwiping || !swipeTypingEnabled) return;
        
        isCurrentlySwiping = false;
        
        // Process final swipe path
        String finalWord = processSwipePath(swipePath);
        
        if (!finalWord.isEmpty()) {
            // Commit the swiped word
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(finalWord + " ", 1);
                updateAISuggestions();
            }
        }
        
        // Reset visual feedback
        if (keyboardView != null) {
            keyboardView.setBackgroundColor(getThemeBackgroundColor());
        }
        
        // Clear swipe data
        swipePath.clear();
        swipeBuffer.setLength(0);
    }
    
    private String processSwipePath(List<Integer> path) {
        if (path.isEmpty()) return "";
        
        // Simple swipe-to-word mapping (basic implementation)
        // In a real implementation, this would use advanced algorithms
        StringBuilder word = new StringBuilder();
        
        for (int code : path) {
            if (code > 0 && code < 256) {
                char c = (char) code;
                if (Character.isLetter(c)) {
                    word.append(c);
                }
            }
        }
        
        // Apply basic swipe word corrections
        String result = word.toString().toLowerCase();
        return applySwipeCorrections(result);
    }
    
    private String applySwipeCorrections(String swipeWord) {
        // Basic swipe pattern to word mapping
        switch (swipeWord) {
            case "qwerty": return "hello";
            case "asdf": return "and";
            case "zxcv": return "the";
            case "qwe": return "you";
            case "asd": return "are";
            case "zxc": return "to";
            default: 
                return swipeWord.length() > 1 ? swipeWord : "";
        }
    }
    
    private void showSwipePrediction(String prediction) {
        if (suggestionContainer != null && suggestionContainer.getChildCount() > 0) {
            TextView firstSuggestion = (TextView) suggestionContainer.getChildAt(0);
            firstSuggestion.setText(prediction);
            firstSuggestion.setTextColor(getSwipeTextColor());
            firstSuggestion.setVisibility(View.VISIBLE);
        }
    }
    
    private int getSwipeActiveColor() {
        return Color.parseColor("#E3F2FD"); // Light blue for swipe mode
    }
    
    private int getSwipeTextColor() {
        return Color.parseColor("#1976D2"); // Blue for swipe predictions
    }
    
    private void handleClose() {
        requestHideSelf(0);
    }
    
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset keyboard state
        caps = false;
        if (keyboardView != null) {
            keyboardView.setShifted(caps);
        }
        
        // Auto-capitalize for sentence start
        if (attribute.inputType != 0) {
            int inputType = attribute.inputType & EditorInfo.TYPE_MASK_CLASS;
            if (inputType == EditorInfo.TYPE_CLASS_TEXT) {
                int variation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_NORMAL ||
                    variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT) {
                    caps = true;
                    if (keyboardView != null) {
                        keyboardView.setShifted(caps);
                    }
                }
            }
        }
        
        // Load fresh settings
        loadSettings();
        applyTheme();
        
        // Clear suggestions
        clearSuggestions();
    }
    
    @Override
    public void onFinishInput() {
        super.onFinishInput();
        clearSuggestions();
    }
    
    private void clearSuggestions() {
        currentSuggestions.clear();
        if (suggestionContainer != null) {
            mainHandler.post(() -> {
                for (int i = 0; i < suggestionContainer.getChildCount(); i++) {
                    suggestionContainer.getChildAt(i).setVisibility(View.INVISIBLE);
                }
            });
        }
    }
    
    // Custom key codes
    private static final int KEYCODE_SPACE = 32;
    private static final int KEYCODE_SYMBOLS = -10;
    private static final int KEYCODE_LETTERS = -11;
    private static final int KEYCODE_NUMBERS = -12;
    private static final int KEYCODE_VOICE = -13;
    
    // Method to update settings from Flutter
    public void updateSettings(String theme, boolean aiSuggestions, boolean swipeTyping, boolean voiceInput, boolean vibration, boolean keyPreview) {
        currentTheme = theme;
        aiSuggestionsEnabled = aiSuggestions;
        swipeTypingEnabled = swipeTyping;
        voiceInputEnabled = voiceInput;
        vibrationEnabled = vibration;
        keyPreviewEnabled = keyPreview;
        
        // Save to preferences
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("keyboard_theme", theme);
        editor.putBoolean("ai_suggestions", aiSuggestions);
        editor.putBoolean("swipe_typing", swipeTyping);
        editor.putBoolean("voice_input", voiceInput);
        editor.putBoolean("vibration_enabled", vibration);
        editor.putBoolean("key_preview_enabled", keyPreview);
        editor.apply();
        
        // Apply settings immediately
        applyTheme();
        if (keyboardView != null) {
            keyboardView.setPreviewEnabled(keyPreviewEnabled);
        }
    }
    
    // Overloaded method for backward compatibility
    public void updateSettings(String theme, boolean aiSuggestions, boolean swipeTyping, boolean voiceInput) {
        updateSettings(theme, aiSuggestions, swipeTyping, voiceInput, true, false);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
