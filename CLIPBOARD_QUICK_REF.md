# 📋 Clipboard Feature - Quick Reference

## 🚀 Quick Start

### Copy Text in Any App
Text automatically captured when clipboard changes → Vibration feedback → Saved to history

### View Clipboard History
Open Kvīve App → Clipboard Screen → See all captured items

### Sync Actions
- **Sync from System**: Manual capture from Android clipboard
- **Sync to Cloud**: Upload history to Firestore
- **Sync from Cloud**: Download history from Firestore

---

## 🔍 Logcat Monitoring

### Filter by Tag
```bash
adb logcat -s "[Clipboard]"
```

### Key Log Messages
| Log | Meaning |
|-----|---------|
| `✅ Copied to Kvīve Clipboard` | Text successfully captured |
| `⚠️ Skipped duplicate` | Duplicate text ignored |
| `⚠️ Clipboard is disabled` | Clipboard history turned off |
| `📌 Toggled pin` | Item pinned/unpinned |
| `🗑️ Deleted item` | Item removed |
| `☁️ Synced X items to cloud` | Cloud upload complete |
| `☁️ Synced X items from cloud` | Cloud download complete |
| `🧹 Cleaned up X expired items` | Auto-expiry cleanup |

---

## 🛠️ Common Developer Tasks

### Task 1: Debug Why Text Not Captured
```bash
# Check if clipboard is enabled
adb logcat | grep "Clipboard is disabled"

# Check for duplicate detection
adb logcat | grep "Skipped duplicate"

# Verify listener is registered
adb logcat | grep "ClipboardHistoryManager initialized"
```

### Task 2: Manually Trigger Cloud Sync (Kotlin)
```kotlin
// In AIKeyboardService or anywhere with context
clipboardHistoryManager.syncToCloud()
clipboardHistoryManager.syncFromCloud()
```

### Task 3: Clear All Clipboard Data
```kotlin
// Delete all non-pinned items
val items = clipboardHistoryManager.getHistoryItems()
items.filter { !it.isPinned && !it.isTemplate }.forEach { item ->
    clipboardHistoryManager.deleteItem(item.id)
}
```

### Task 4: Add Template Item
```kotlin
clipboardHistoryManager.addTemplate(
    text = "Thanks for your message!",
    category = "Replies"
)
```

---

## 📡 Method Channel API

### Flutter → Kotlin

#### Get Clipboard History
```dart
final items = await ClipboardService.getHistory(maxItems: 50);
```

#### Toggle Pin
```dart
await ClipboardService.togglePin(itemId);
```

#### Delete Item
```dart
await ClipboardService.deleteItem(itemId);
```

#### Clear All
```dart
await ClipboardService.clearAll();
```

#### Update Settings
```dart
await ClipboardService.updateSettings({
  'enabled': true,
  'maxHistorySize': 20,
  'autoExpiryEnabled': true,
  'expiryDurationMinutes': 60,
});
```

#### Sync Operations
```dart
await ClipboardService.syncFromSystem();
await ClipboardService.syncToCloud();
await ClipboardService.syncFromCloud();
```

### Kotlin → Flutter (Callbacks)

#### On History Changed
```dart
ClipboardService.onHistoryChanged.listen((items) {
  print('History updated: ${items.length} items');
});
```

#### On New Item
```dart
ClipboardService.onNewItem.listen((item) {
  print('New clipboard item: ${item.text}');
});
```

---

## 🔑 Key Classes

### ClipboardHistoryManager (Kotlin)
```kotlin
// Located: android/app/src/main/kotlin/com/example/ai_keyboard/ClipboardManager.kt

class ClipboardHistoryManager(context: Context) : BaseManager(context) {
    // Core methods
    fun getAllItems(): List<ClipboardItem>
    fun togglePin(itemId: String): Boolean
    fun deleteItem(itemId: String): Boolean
    fun addTemplate(text: String, category: String?): ClipboardItem
    fun updateSettings(enabled, maxHistorySize, autoExpiryEnabled, expiryDurationMinutes)
    
    // Sync methods
    fun syncFromSystemClipboard()
    fun syncToCloud()
    fun syncFromCloud()
}
```

### ClipboardService (Dart)
```dart
// Located: lib/services/clipboard_service.dart

class ClipboardService {
    static Future<List<ClipboardItemData>> getHistory({int maxItems = 20})
    static Future<bool> togglePin(String itemId)
    static Future<bool> deleteItem(String itemId)
    static Future<bool> clearAll()
    static Future<bool> updateSettings(Map<String, dynamic> settings)
    static Future<bool> syncFromSystem()
    static Future<bool> syncToCloud()
    static Future<bool> syncFromCloud()
    
    // Streams
    static Stream<List<ClipboardItemData>> get onHistoryChanged
    static Stream<ClipboardItemData> get onNewItem
}
```

---

## 🎨 UI Components

### Clipboard Screen
```
lib/screens/main screens/clipboard_screen.dart

Features:
- History Settings (enable/disable, size, expiry)
- Internal Settings (sync options)
- Sync Action Buttons (system, cloud upload/download)
- Clipboard History List (with pin/delete actions)
```

---

## 📦 SharedPreferences Keys

### Native (clipboard_history)
```kotlin
prefs.getBoolean("clipboard_enabled", true)
prefs.getInt("max_history_size", 20)
prefs.getBoolean("auto_expiry_enabled", true)
prefs.getLong("expiry_duration_minutes", 60L)
prefs.getString("history_items", null) // JSON array
prefs.getString("template_items", null) // JSON array
```

### Flutter (FlutterSharedPreferences)
```dart
prefs.getBool('flutter.clipboard_history') ?? true
prefs.getDouble('flutter.history_size') ?? 20.0
prefs.getDouble('flutter.clean_old_history_minutes') ?? 0.0
prefs.getString('flutter.clipboard_items') // JSON array
```

---

## 🔥 Firestore Structure

```
users/
  {userId}/
    clipboard/
      {itemId}/
        id: String
        text: String
        timestamp: Long (milliseconds since epoch)
        isPinned: Boolean
```

---

## ⚙️ Configuration Options

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `clipboard_enabled` | Boolean | true | Enable/disable clipboard capture |
| `max_history_size` | Int | 20 | Max number of items to keep |
| `auto_expiry_enabled` | Boolean | true | Enable auto-deletion of old items |
| `expiry_duration_minutes` | Long | 60 | Minutes before item expires |
| `clipboard_suggestion_enabled` | Boolean | true | Show clipboard in suggestions |
| `internal_clipboard` | Boolean | true | Use internal clipboard |
| `sync_from_system` | Boolean | true | Auto-sync from system clipboard |

---

## 🐛 Troubleshooting

### Problem: "No vibration on copy"
**Solution:** Check `VIBRATE` permission in AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.VIBRATE"/>
```

### Problem: "Cloud sync fails"
**Checklist:**
- [ ] Firebase initialized in app
- [ ] User authenticated (not anonymous)
- [ ] Firestore rules allow access
- [ ] Internet connectivity available
- [ ] Check logcat for Firebase errors

### Problem: "Items not persisting"
**Solution:** Verify SharedPreferences write succeeds
```kotlin
// Use commit() instead of apply() for immediate write
prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).commit()
```

### Problem: "Memory leak warning"
**Solution:** Always remove listener on cleanup
```kotlin
override fun onDestroy() {
    clipboardHistoryManager.cleanup() // Removes clipboard listener
    super.onDestroy()
}
```

---

## 📊 Performance Tips

1. **Limit History Size**: Default 20 items, increase if needed but be mindful of memory
2. **Enable Auto-Expiry**: Automatically cleans up old items
3. **Batch Cloud Sync**: Only syncs when user explicitly requests (not automatic)
4. **Efficient Storage**: Uses JSON for compact storage
5. **Thread-Safe**: Uses `CopyOnWriteArrayList` for concurrent access

---

## 🎯 Testing Commands

```bash
# Monitor clipboard logs
adb logcat | grep "\[Clipboard\]"

# Copy text via ADB (for testing)
adb shell input text "Test clipboard text"

# Check SharedPreferences
adb shell run-as com.example.ai_keyboard cat /data/data/com.example.ai_keyboard/shared_prefs/clipboard_history.xml

# Force stop and restart keyboard
adb shell am force-stop com.example.ai_keyboard
adb shell am start -n com.example.ai_keyboard/.MainActivity

# Clear app data (test fresh install)
adb shell pm clear com.example.ai_keyboard
```

---

## 📚 Additional Resources

- Full Documentation: `CLIPBOARD_FIX_SUMMARY.md`
- Source Code: `android/app/src/main/kotlin/com/example/ai_keyboard/ClipboardManager.kt`
- Flutter Service: `lib/services/clipboard_service.dart`
- UI Screen: `lib/screens/main screens/clipboard_screen.dart`

---

**Need help? Check the comprehensive testing guide in `CLIPBOARD_FIX_SUMMARY.md`** 🚀


