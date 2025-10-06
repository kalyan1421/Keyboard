package com.example.ai_keyboard.diagnostics

import android.util.Log

/**
 * TypingSyncAuditor: One-time analysis tool to audit current typing/suggestion features
 * Prints a structured JSON summary to logcat for gap analysis
 */
object TypingSyncAuditor {
    private const val TAG = "TypingSyncAudit"
    
    /**
     * Report current feature status
     * Call this from AIKeyboardService.onCreate() after initialization
     */
    fun report(
        hasUpdateAISuggestions: Boolean,
        hasSuggestionContainerInflation: Boolean,
        hasEmojiPipeline: Boolean,
        hasNextWordModel: Boolean,
        hasClipboardSuggester: Boolean,
        hasAutocap: Boolean,
        hasDoubleSpacePeriod: Boolean,
        hasPopupPreviewSetting: Boolean,
        hasDictionaryManager: Boolean = false,
        hasLanguageManager: Boolean = false,
        hasAutocorrectEngine: Boolean = false
    ) {
        val json = """
            {
              "analysis": "TypingSyncAudit - Feature Status Report",
              "timestamp": ${System.currentTimeMillis()},
              "features": {
                "suggestions": {
                  "updateAISuggestions": $hasUpdateAISuggestions,
                  "suggestionContainerInflation": $hasSuggestionContainerInflation,
                  "nextWordModel": $hasNextWordModel
                },
                "content": {
                  "emojiPipeline": $hasEmojiPipeline,
                  "clipboardSuggester": $hasClipboardSuggester
                },
                "typing": {
                  "autocap": $hasAutocap,
                  "doubleSpacePeriod": $hasDoubleSpacePeriod,
                  "autocorrectEngine": $hasAutocorrectEngine
                },
                "ui": {
                  "popupPreviewSetting": $hasPopupPreviewSetting
                },
                "dictionary": {
                  "dictionaryManager": $hasDictionaryManager,
                  "languageManager": $hasLanguageManager
                }
              },
              "summary": {
                "totalFeatures": 11,
                "implementedFeatures": ${countImplemented(hasUpdateAISuggestions, hasSuggestionContainerInflation, hasEmojiPipeline, hasNextWordModel, hasClipboardSuggester, hasAutocap, hasDoubleSpacePeriod, hasPopupPreviewSetting, hasDictionaryManager, hasLanguageManager, hasAutocorrectEngine)},
                "implementationRate": "${calculateRate(hasUpdateAISuggestions, hasSuggestionContainerInflation, hasEmojiPipeline, hasNextWordModel, hasClipboardSuggester, hasAutocap, hasDoubleSpacePeriod, hasPopupPreviewSetting, hasDictionaryManager, hasLanguageManager, hasAutocorrectEngine)}%"
              }
            }
        """.trimIndent()
        
        Log.d(TAG, "═══════════════════════════════════════════════════")
        Log.d(TAG, json)
        Log.d(TAG, "═══════════════════════════════════════════════════")
    }
    
    private fun countImplemented(vararg features: Boolean): Int {
        return features.count { it }
    }
    
    private fun calculateRate(vararg features: Boolean): String {
        val implemented = features.count { it }
        val total = features.size
        val rate = (implemented.toDouble() / total * 100).toInt()
        return rate.toString()
    }
    
    /**
     * Report gaps and missing features
     */
    fun reportGaps(gaps: List<String>) {
        if (gaps.isEmpty()) {
            Log.d(TAG, "✓ No feature gaps detected - all features present")
            return
        }
        
        Log.w(TAG, "⚠ Feature gaps detected:")
        gaps.forEachIndexed { index, gap ->
            Log.w(TAG, "  ${index + 1}. $gap")
        }
    }
    
    /**
     * Report Flutter → Android setting mapping status
     */
    fun reportSettingsSyncStatus(
        settingsInFlutter: Map<String, Boolean>,
        settingsInAndroid: Map<String, Boolean>,
        settingsInFirestore: Map<String, Boolean>
    ) {
        Log.d(TAG, "═══ Settings Sync Status ═══")
        Log.d(TAG, "Flutter settings: ${settingsInFlutter.size} keys")
        Log.d(TAG, "Android settings: ${settingsInAndroid.size} keys")
        Log.d(TAG, "Firestore settings: ${settingsInFirestore.size} keys")
        
        // Find missing mappings
        val flutterOnly = settingsInFlutter.keys - settingsInAndroid.keys
        val androidOnly = settingsInAndroid.keys - settingsInFlutter.keys
        
        if (flutterOnly.isNotEmpty()) {
            Log.w(TAG, "⚠ Flutter settings not mirrored to Android: $flutterOnly")
        }
        
        if (androidOnly.isNotEmpty()) {
            Log.w(TAG, "⚠ Android settings not exposed to Flutter: $androidOnly")
        }
        
        val notInFirestore = settingsInFlutter.keys - settingsInFirestore.keys
        if (notInFirestore.isNotEmpty()) {
            Log.w(TAG, "⚠ Settings not synced to Firestore: $notInFirestore")
        }
    }
}

