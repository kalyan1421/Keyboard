# Firebase Settings Sync - Testing Guide 🧪

## Quick Test Scenarios

### **Test 1: Default Settings for New User** ✅

**Steps:**
1. Create a new Firebase account (Sign up)
2. Complete keyboard setup
3. Open "Keyboard Settings"
4. Check the following:

**Expected Results:**
- ✅ Popup Visibility: **OFF** (toggle should be disabled)
- ✅ Dictionary: **ON** (enabled by default)
- Open "Typing & Suggestion":
  - ✅ Display Suggestions: **ON**
  - ✅ Display Mode: **3** suggestions
  - ✅ Clipboard History Size: **20** (minimum)
  - ✅ Internal Clipboard: **ON**
- Open "Sounds & Vibration":
  - ✅ Audio Feedback: **ON**
  - ✅ Haptic Feedback: **ON**

**Debug Console Output:**
```
🔵 [AuthWrapper] User logged in (newuser@example.com), starting cloud sync...
KeyboardCloudSync: Starting cloud sync for user xyz789...
KeyboardCloudSync: No remote settings found, creating defaults
✅ Default settings created in Firebase
KeyboardCloudSync: ✓ Settings applied and keyboard notified
✅ [AuthWrapper] Cloud sync started successfully
```

---

### **Test 2: Settings Sync to Firebase** ✅

**Steps:**
1. Login with existing account
2. Go to "Keyboard Settings"
3. Enable "Popup Visibility" (turn ON)
4. Change "Number Row" to ON
5. Go to "Typing & Suggestion"
6. Change "Display Mode" to 4 suggestions
7. Change "History Size" to 30

**Expected Results:**
- ✅ Success snackbar appears: "Settings saved! Keyboard updated immediately."
- ✅ Settings persist after closing and reopening the app
- ✅ Firebase console shows updated values in:
  ```
  users/{userId}/settings/keyboard
  ```

**Debug Console Output:**
```
✅ Keyboard settings saved
📤 Sending settings to Kotlin IME: {...}
✅ Settings synced to Firebase for cross-device sync
✅ Settings broadcast sent successfully
```

**Firebase Console Check:**
1. Open Firebase Console
2. Go to Firestore Database
3. Navigate to: `users/{userId}/settings/keyboard`
4. Verify:
   - `popupEnabled: true`
   - `clipboardSuggestions.historyItems: 30`

---

### **Test 3: Cross-Device Sync** ✅

**Prerequisites:**
- 2 devices (or 1 device + 1 emulator)
- Same Firebase account

**Steps:**

**Device A:**
1. Login with account (user@example.com)
2. Go to "Keyboard Settings"
3. Enable "Popup Visibility"
4. Enable "Number Row"
5. Go to "Sounds & Vibration"
6. Set "Sound Volume" to 80%

**Device B:**
1. **BEFORE** logging in, check current settings
2. Login with same account (user@example.com)
3. Wait 2-3 seconds
4. Go to "Keyboard Settings"

**Expected Results on Device B:**
- ✅ Popup Visibility: **ON** (same as Device A)
- ✅ Number Row: **ON** (same as Device A)
- ✅ Sound Volume: **80%** (same as Device A)
- ✅ All other settings match Device A

**Debug Console Output (Device B):**
```
🔵 [AuthWrapper] User logged in (user@example.com), starting cloud sync...
KeyboardCloudSync: Starting cloud sync for user abc123...
KeyboardCloudSync: Remote settings received, applying locally...
KeyboardCloudSync: ✓ Settings persisted to SharedPreferences
KeyboardCloudSync: ✓ Native keyboard notified via MethodChannel
KeyboardCloudSync: ✓ Settings applied and keyboard notified
✅ [AuthWrapper] Cloud sync started successfully
```

---

### **Test 4: Real-Time Sync Across Devices** ✅

**Prerequisites:**
- 2 devices logged in with same account
- Both devices on same WiFi (for faster sync)

**Steps:**

**Device A:**
1. Keep "Keyboard Settings" screen open
2. Toggle "Popup Visibility" OFF → ON → OFF
3. Watch Device B

**Device B:**
1. Keep "Keyboard Settings" screen open
2. Watch for changes

**Expected Results:**
- ✅ Device B updates within **1-3 seconds**
- ✅ Settings match exactly
- ✅ No manual refresh needed
- ✅ Visual feedback on Device B (settings change)

**Note:** If Device B doesn't auto-update the UI, navigate away and back to see changes (this is a UI limitation, the settings are still synced).

---

### **Test 5: Offline Behavior** ✅

**Steps:**
1. Login to app
2. Enable Airplane Mode (no internet)
3. Go to "Keyboard Settings"
4. Change "Popup Visibility" to ON
5. Change "Number Row" to ON
6. **Type in keyboard** - should still work!
7. Disable Airplane Mode (re-enable internet)
8. Wait 5 seconds

**Expected Results:**
- ✅ Settings save locally (SharedPreferences)
- ✅ Keyboard works with new settings (offline)
- ✅ No error messages
- ✅ When back online, settings sync to Firebase
- ✅ Other devices receive the updates

**Debug Console Output:**
```
✅ Keyboard settings saved (local only)
⚠ Failed to sync to Firebase: SocketException (expected offline)
[After coming online]
✅ Settings synced to Firebase for cross-device sync
```

---

### **Test 6: First Login with Existing Settings** ✅

**Scenario:** User had an account, used Device A, now logging in on Device B for the first time.

**Steps:**

**Device A (Already set up):**
1. Login with user@example.com
2. Set custom settings:
   - Popup: ON
   - Number Row: ON
   - Dictionary: OFF
   - Clipboard History: 50 items

**Device B (Fresh install):**
1. Install app
2. Login with user@example.com (same account)
3. Complete keyboard setup
4. Go to "Keyboard Settings"

**Expected Results on Device B:**
- ✅ Popup: **ON** (not default OFF)
- ✅ Number Row: **ON** (not default OFF)
- ✅ Dictionary: **OFF** (not default ON)
- ✅ Clipboard History: **50** items (not default 20)
- ✅ **Identical to Device A!**

---

### **Test 7: Multiple Users on Same Device** ✅

**Steps:**
1. Login as User A (usera@example.com)
2. Set Popup: ON, Number Row: ON
3. Logout
4. Login as User B (userb@example.com)
5. Check settings

**Expected Results:**
- ✅ User B has their own settings (not User A's)
- ✅ If User B is new: Gets defaults (Popup: OFF, Dictionary: ON)
- ✅ If User B is existing: Gets their saved settings
- ✅ Settings are isolated per user

**Security Check:**
- User A cannot see User B's settings
- User B cannot see User A's settings
- Firestore rules enforce isolation

---

### **Test 8: Stress Test - Rapid Changes** ✅

**Steps:**
1. Login to app
2. Go to "Keyboard Settings"
3. Rapidly toggle "Popup Visibility" 20 times (ON/OFF/ON/OFF...)
4. Rapidly change "Number Row" 20 times
5. Wait 5 seconds
6. Check Firebase Console

**Expected Results:**
- ✅ No crashes
- ✅ UI responsive
- ✅ Debouncing works (only final state synced)
- ✅ Firebase shows final state (not 20 updates)
- ✅ Network efficient (batched updates)

**Debug Console Output:**
```
✅ Keyboard settings saved (debounced)
✅ Settings synced to Firebase for cross-device sync
[Only 1-2 Firebase writes, not 20]
```

---

## 🔍 Manual Verification

### **Check Firebase Console**

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Firestore Database**
4. Navigate to: `users → {userId} → settings → keyboard`

**Expected Document Structure:**
```json
{
  "version": 1,
  "popupEnabled": false,
  "aiSuggestions": true,
  "autocorrect": true,
  "dictionaryEnabled": true,
  "clipboardSuggestions": {
    "enabled": true,
    "windowSec": 60,
    "historyItems": 20
  },
  "soundEnabled": true,
  "soundVolume": 0.5,
  "vibrationEnabled": true,
  "vibrationMs": 50,
  "updatedAt": "Timestamp(...)"
}
```

### **Check SharedPreferences (Android)**

**Using Android Studio:**
1. Open Device File Explorer
2. Navigate to: `/data/data/com.example.ai_keyboard/shared_prefs/`
3. Open `FlutterSharedPreferences.xml`
4. Verify settings keys:
   - `flutter.keyboard.popupPreview: false`
   - `flutter.clipboard_history_items: 20`
   - etc.

**Using ADB:**
```bash
adb shell run-as com.example.ai_keyboard cat shared_prefs/FlutterSharedPreferences.xml
```

### **Check Kotlin Keyboard**

1. Open any text input (e.g., ChatScreen)
2. Type some text
3. Verify keyboard behavior:
   - Popup preview OFF → No popup when pressing keys
   - Dictionary ON → Suggestions appear
   - Clipboard ON → Recent text available

---

## 📊 Performance Benchmarks

**Expected Timings:**

| Operation | Expected Time |
|-----------|---------------|
| Local save (SharedPreferences) | < 50ms |
| Native keyboard update | < 150ms |
| Firebase upload | 200-500ms (background) |
| Cross-device sync | 1-3 seconds |
| Offline save | < 50ms (local only) |

**Network Usage:**

| Operation | Data Size |
|-----------|-----------|
| Initial settings load | ~2-5 KB |
| Settings update | ~500 bytes - 2 KB |
| Real-time listener | ~100 bytes per update |

---

## 🐛 Common Issues & Solutions

### **Issue 1: Settings not syncing across devices**

**Symptoms:**
- Device B doesn't get Device A's settings
- Manual refresh needed

**Solutions:**
1. Check Firebase Authentication: Both devices logged in?
2. Check internet connection
3. Check Firestore rules:
   ```javascript
   allow read, write: if request.auth != null 
                      && request.auth.uid == userId;
   ```
4. Check console logs for errors
5. Try logout + login on Device B

### **Issue 2: Default settings not applied**

**Symptoms:**
- New user gets random settings
- Popup is ON instead of OFF

**Solutions:**
1. Check `KeyboardCloudSync._getDefaultSettings()`
2. Check `KeyboardSettingsScreen` initial values
3. Clear app data and reinstall
4. Check Firebase Console for corrupted document

### **Issue 3: Keyboard not updating immediately**

**Symptoms:**
- Settings saved but keyboard doesn't change
- Need to restart keyboard

**Solutions:**
1. Check `broadcastSettingsChanged()` is called
2. Check Kotlin `BroadcastReceiver` is registered
3. Check `MainActivity.kt` handles `broadcastSettingsChanged`
4. Try switching to another keyboard and back

---

## ✅ Testing Checklist

Before deploying to production, verify:

- [ ] New user gets default settings (Popup OFF, Dictionary ON, etc.)
- [ ] Settings save to SharedPreferences
- [ ] Settings save to Firebase
- [ ] Settings send to Kotlin keyboard
- [ ] Keyboard updates immediately (< 150ms)
- [ ] Cross-device sync works (1-3 seconds)
- [ ] Offline mode works (saves locally)
- [ ] Multiple users isolated (security)
- [ ] Rapid changes don't crash (debouncing)
- [ ] Performance acceptable (see benchmarks)
- [ ] Firebase Console shows correct data
- [ ] Firestore rules enforce security
- [ ] No linter errors
- [ ] No console errors

---

## 📝 Test Report Template

```
Date: _______________
Tester: _______________
Build: _______________

Test 1: Default Settings
- [ ] PASS  [ ] FAIL
Notes: _______________________________

Test 2: Settings Sync to Firebase
- [ ] PASS  [ ] FAIL
Notes: _______________________________

Test 3: Cross-Device Sync
- [ ] PASS  [ ] FAIL
Notes: _______________________________

Test 4: Real-Time Sync
- [ ] PASS  [ ] FAIL
Notes: _______________________________

Test 5: Offline Behavior
- [ ] PASS  [ ] FAIL
Notes: _______________________________

Test 6: First Login with Existing Settings
- [ ] PASS  [ ] FAIL
Notes: _______________________________

Test 7: Multiple Users
- [ ] PASS  [ ] FAIL
Notes: _______________________________

Test 8: Stress Test
- [ ] PASS  [ ] FAIL
Notes: _______________________________

Overall Result: [ ] PASS  [ ] FAIL
Production Ready: [ ] YES  [ ] NO
```

---

**Last Updated:** October 6, 2025  
**Status:** Ready for Testing  
**Next Steps:** Run all test scenarios and fill out report

