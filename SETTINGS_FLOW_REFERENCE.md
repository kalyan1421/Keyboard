# Settings Flow Quick Reference

## 📍 Current Architecture (Post-Refactoring)

```
┌─────────────────────────────────────────────────────────────┐
│                    FLUTTER UI LAYER                         │
├─────────────────────────────────────────────────────────────┤
│  lib/screens/main screens/keyboard_settings_screen.dart    │
│                                                             │
│  User changes setting                                       │
│    ↓                                                        │
│  _saveSettings() → SharedPreferences (Flutter)             │
│    ↓                                                        │
│  _sendSettingsToKeyboard() → MethodChannel                │
│    ↓                                                        │
│  _notifyKeyboard() → "notifyConfigChange"                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   MAIN ACTIVITY LAYER                       │
├─────────────────────────────────────────────────────────────┤
│  android/.../MainActivity.kt                               │
│                                                             │
│  MethodChannel Handler:                                     │
│    "updateSettings" → writes to SharedPreferences          │
│    "notifyConfigChange" → sendBroadcast()                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    BROADCAST INTENT
         "com.example.ai_keyboard.SETTINGS_CHANGED"
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 KEYBOARD SERVICE LAYER                      │
├─────────────────────────────────────────────────────────────┤
│  android/.../AIKeyboardService.kt                          │
│                                                             │
│  BroadcastReceiver.onReceive()                             │
│    ↓                                                        │
│  Debounce check (250ms)                                     │
│    ↓                                                        │
│  applyLoadedSettings(settingsManager.loadAll())            │
│    ↓                                                        │
│  SettingsManager:                                           │
│    - Read from "ai_keyboard_settings"                      │
│    - Read from "FlutterSharedPreferences"                  │
│    - Merge and return UnifiedSettings                      │
│    ↓                                                        │
│  Apply to service fields:                                   │
│    - vibrationEnabled                                       │
│    - soundEnabled                                           │
│    - keyPreviewEnabled                                      │
│    - showNumberRow                                          │
│    - swipeTypingEnabled                                     │
│    - aiSuggestionsEnabled                                   │
│    - etc.                                                   │
│    ↓                                                        │
│  Update keyboard view:                                      │
│    - keyboardView?.isPreviewEnabled                        │
│    - keyboardView?.setSwipeEnabled()                       │
│    ↓                                                        │
│  Log: "Settings loaded" (if requested)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 Key Components

### 1. **SettingsManager (Internal Class)**
**Location:** AIKeyboardService.kt, lines 133-181

```kotlin
private class SettingsManager(private val context: Context) {
    private val flutterPrefs by lazy { 
        context.getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE) 
    }
    private val nativePrefs by lazy { 
        context.getSharedPreferences("ai_keyboard_settings", MODE_PRIVATE) 
    }
    
    fun loadAll(): UnifiedSettings { /* ... */ }
}
```

**Reads from:**
- `"ai_keyboard_settings"` - Native keyboard settings (MainActivity writes here)
- `"FlutterSharedPreferences"` - Flutter-managed settings (language, etc.)

---

### 2. **UnifiedSettings (Data Class)**
**Location:** AIKeyboardService.kt, lines 114-127

```kotlin
private data class UnifiedSettings(
    val vibrationEnabled: Boolean,
    val soundEnabled: Boolean,
    val keyPreviewEnabled: Boolean,
    val showNumberRow: Boolean,
    val swipeTypingEnabled: Boolean,
    val aiSuggestionsEnabled: Boolean,
    val currentLanguage: String,
    val enabledLanguages: List<String>,
    val autocorrectEnabled: Boolean,
    val autoCapitalization: Boolean,
    val doubleSpacePeriod: Boolean,
    val popupEnabled: Boolean
)
```

---

### 3. **applyLoadedSettings() Method**
**Location:** AIKeyboardService.kt, lines 2290-2326

```kotlin
private fun applyLoadedSettings(unified: UnifiedSettings, logSuccess: Boolean = false) {
    // Apply settings to service fields
    vibrationEnabled = unified.vibrationEnabled
    soundEnabled = unified.soundEnabled
    // ... etc
    
    // Hash-based change detection
    val newHash = listOf(...).hashCode()
    if (newHash != lastLoadedSettingsHash) {
        lastLoadedSettingsHash = newHash
        // Apply side effects
    }
    
    if (logSuccess) {
        Log.i(TAG, "Settings loaded")  // ← ONLY log site
    }
}
```

---

## 📝 Usage Patterns

### On Service Creation (onCreate)
```kotlin
// Line 642
settingsManager = SettingsManager(this)

// Line 662
applyLoadedSettings(settingsManager.loadAll(), logSuccess = true)
// Logs: "Settings loaded" (once)
```

### On Settings Changed (BroadcastReceiver)
```kotlin
// Line 485
applyLoadedSettings(settingsManager.loadAll(), logSuccess = false)
// No log (silent update)
```

### Change Detection
```kotlin
// Hash-based comparison prevents unnecessary updates
val newHash = listOf(settings...).hashCode()
if (newHash != lastLoadedSettingsHash) {
    // Only apply if actually changed
    lastLoadedSettingsHash = newHash
    keyboardView?.isPreviewEnabled = keyPreviewEnabled
}
```

---

## 🚦 Timing & Debouncing

### BroadcastReceiver Debouncing
```kotlin
// Line 473-476
if (!settingsDebouncer.shouldApply()) {
    Log.d(TAG, "⏳ Settings change debounced (${settingsDebouncer.timeUntilNextMs()}ms remaining)")
    return
}
```

**Configuration:**
- Minimum interval: **250ms**
- Prevents spam from rapid Flutter updates
- Defined at line 126: `SettingsDebouncer(minIntervalMs = 250)`

### Polling Fallback (DEBUG only)
```kotlin
// Lines 5166-5187
if (!BuildConfig.DEBUG) {
    Log.d(TAG, "Settings polling disabled in release build")
    return
}
// Poll every 15 seconds in DEBUG builds only
```

---

## 🔍 Log Messages

### Successful Settings Load
```
I/AIKeyboardService: Settings loaded
```
**Location:** applyLoadedSettings() at line 2323  
**Frequency:** Once during onCreate(), silent during broadcasts  

### Broadcast Received
```
D/AIKeyboardService: SETTINGS_CHANGED broadcast received!
D/AIKeyboardService: 📥 Loading settings from broadcast...
D/AIKeyboardService: ✅ Settings applied successfully
```

### Debouncing Active
```
D/AIKeyboardService: ⏳ Settings change debounced (150ms remaining)
```

### Polling Status (DEBUG builds)
```
D/AIKeyboardService: ⚠️ Settings polling enabled (DEBUG mode only, 15s interval)
```

### Cleanup
```
D/AIKeyboardService: Broadcast receiver unregistered
```

---

## ⚙️ Configuration

### Debounce Interval
```kotlin
// Line 126
private val settingsDebouncer = SettingsDebouncer(minIntervalMs = 250)
```
**Modify:** Change `250` to desired milliseconds

### Polling Interval (DEBUG)
```kotlin
// Line 5178
settingsPoller?.let { mainHandler.postDelayed(it, 15000) }
```
**Modify:** Change `15000` to desired milliseconds  
**Disable:** Remove or comment out `startSettingsPolling()` call in onCreate()

### SharedPreferences Names
```kotlin
// SettingsManager class
"ai_keyboard_settings"        // Native settings
"FlutterSharedPreferences"    // Flutter settings
```
**Critical:** Do NOT change these without updating MainActivity.kt

---

## 🐛 Debugging

### Check if Settings Loaded
```bash
adb logcat | grep "Settings loaded"
# Should appear ONCE during onCreate()
```

### Check Broadcast Reception
```bash
adb logcat | grep "SETTINGS_CHANGED"
# Should appear when Flutter sends update
```

### Check Debouncing
```bash
adb logcat | grep "debounced"
# Should appear if updates come faster than 250ms
```

### Verify Polling Disabled (Release)
```bash
adb logcat | grep "Settings polling disabled"
# Should appear in release builds
```

### Check Cleanup
```bash
adb logcat | grep "Broadcast receiver unregistered"
# Should appear when keyboard service destroyed
```

---

## 🚨 Common Issues

### Settings Not Updating
1. Check Flutter MethodChannel is calling `notifyConfigChange`
2. Verify MainActivity sends broadcast with correct action
3. Check BroadcastReceiver is registered (onCreate line 730-746)
4. Look for debouncing messages (might be too fast)

### Multiple "Settings loaded" Logs
❌ **Should never happen** - indicates regression  
✅ Only one occurrence at line 2323 (applyLoadedSettings)

### Battery Drain from Polling
1. Check BuildConfig.DEBUG is false in release
2. Verify "Settings polling disabled" log appears
3. Use battery historian to confirm no 2s intervals

### Memory Leaks
1. Verify onDestroy() calls unregisterReceiver() (line 5060)
2. Check LeakCanary for receiver leaks
3. Enable/disable keyboard 20+ times to test

---

## 📚 Related Files

| File | Purpose | Key Lines |
|------|---------|-----------|
| `AIKeyboardService.kt` | Main service, settings mgmt | 133-181 (SettingsManager), 2290-2326 (apply) |
| `MainActivity.kt` | MethodChannel handler | 49-86 (updateSettings), 87-93 (notifyConfigChange) |
| `keyboard_settings_screen.dart` | Flutter UI | 224-238 (_sendSettingsToKeyboard) |

---

## ✅ Verification Checklist

- [ ] SettingsManager exists at lines 133-181
- [ ] applyLoadedSettings() called twice: onCreate (line 662) + BroadcastReceiver (line 485)
- [ ] Single "Settings loaded" log at line 2323
- [ ] Polling disabled in release builds (line 5166)
- [ ] Receiver unregistered in onDestroy() (line 5060)
- [ ] No linter errors in AIKeyboardService.kt
- [ ] Debouncing set to 250ms (line 126)
- [ ] Hash-based change detection active (line 2311)

---

**Last Updated:** October 5, 2025  
**Status:** ✅ Production Ready
