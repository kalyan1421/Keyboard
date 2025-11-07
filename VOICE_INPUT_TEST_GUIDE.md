# Voice Input Testing Guide

## Quick Test (Recommended)

### 1. Build and Install
```bash
cd /Users/kalyan/AI-keyboard
flutter build apk --debug
# Install will happen automatically, or manually:
adb install -r build/app/outputs/flutter-apk/app-debug.apk
```

### 2. Test Voice Input
1. Open any app with text input (Notes, Messages, WhatsApp, etc.)
2. Tap into a text field to show your keyboard
3. Tap the **microphone button** 🎤 on your keyboard
4. Speak clearly: "Hello world this is a test"
5. **Watch for**: Text should appear in the field after you finish speaking

### 3. Monitor Logs (Optional)
In a second terminal, run:
```bash
adb logcat | grep -E "AIKeyboardService|VoiceInputActivity|VoiceInputManager" | grep -v "^D/ViewRootImpl"
```

## What Should Happen

### ✅ SUCCESS - Persistent Voice Panel
1. **First Input**: Speak "Hello world" → Text appears immediately
2. **Panel Stays Open**: Shows "✓ Text added. Speak again or tap ← to close"
3. **Auto-Restarts**: After 800ms, automatically starts listening again
4. **Second Input**: Speak "This is amazing" → Text is added without reopening panel
5. **Continue**: Keep speaking as many times as you want
6. **Close**: Tap the ← back button when done

**This is the KEY improvement**: Panel works like emoji panel - stays open for multiple inputs!

**Successful Log Pattern:**
```
D/VoiceInputManager: Vosk partial result: hello
D/VoiceInputManager: Vosk partial result: hello world
D/AIKeyboardService: 📝 Received voice input result: 'hello world this is a test' (26 chars)
D/AIKeyboardService: Input connection available: true
D/AIKeyboardService: InputConnection validated, committing text...
D/AIKeyboardService: ✅ Voice input committed: hello world this is a test (commitText returned=true)
D/AIKeyboardService: After commit: cursor position=27, recent text='hello world this is a test '
D/AIKeyboardService: ✅ Verified: Text successfully inserted into editor
D/AIKeyboardService: ✅ Voice result committed successfully
```

### ❌ FAILURE - Text Doesn't Appear

**If you see:**
```
D/AIKeyboardService: ⚠️ Warning: Text may not have been inserted (verification failed)
```
Or:
```
D/AIKeyboardService: Cursor position after voice commit: 0
```

**Try:**
1. Increase the delay in `VoiceInputActivity.kt` line 219 from `200` to `300` or `400`
2. Make sure your keyboard is actually the active input method
3. Check that the text field has focus (cursor should be blinking)

## Test Different Scenarios

### Test 1: Short Phrase
- **Say**: "Hello"
- **Expected**: "Hello " (with trailing space)

### Test 2: Long Sentence
- **Say**: "This is a much longer sentence with many words to test the voice input system"
- **Expected**: Full sentence appears with trailing space

### Test 3: Multiple Voice Inputs (NEW FEATURE!)
- Tap voice button once
- Speak: "First sentence"
- **Wait** - panel shows "✓ Text added. Speak again or tap ← to close"
- Speak: "Second sentence" (without closing/reopening!)
- Speak: "Third sentence"
- **Expected**: All three sentences appear: "First sentence Second sentence Third sentence "
- **This is the killer feature!** No need to reopen for each input!

### Test 4: Different Apps
Test in multiple apps to ensure compatibility:
- ✓ Google Keep / Notes
- ✓ WhatsApp / Telegram
- ✓ Gmail / Email
- ✓ Chrome / Browser search bar
- ✓ Messages / SMS

## Common Issues and Solutions

### Issue: "No voice input button"
**Solution**: Check keyboard settings, voice input might be disabled
```bash
# Check settings
adb shell content query --uri content://settings/secure | grep ai_keyboard
```

### Issue: "Microphone permission denied"
**Solution**: Grant permission manually
1. Go to Settings → Apps → AI Keyboard
2. Permissions → Microphone → Allow

### Issue: "Voice input activity crashes"
**Solution**: Check for missing Vosk model or permission issues
```bash
adb logcat | grep -E "VoiceInputManager|Vosk"
```

### Issue: "Panel closes after each input"
**Solution**: This was fixed! Panel should now stay open
**If still closing**: Check that `FLAG_ALT_FOCUSABLE_IM` is set in `VoiceInputActivity.onCreate()`

### Issue: "Recognition doesn't auto-restart"
**Behavior**: Should auto-restart after 800ms
**Solution**: Check logs for any errors in `deliverResult()` method

## Performance Notes

- **Fast devices** (Pixel 6+, Snapdragon 8 Gen 2+): 200ms delay works perfectly
- **Mid-range devices**: May need 250-300ms
- **Older/slower devices**: May need 300-400ms

Adjust the delay in `VoiceInputActivity.kt` line 219 if needed.

## Verification Checklist

- [ ] Voice input button appears on keyboard
- [ ] Microphone permission granted
- [ ] Voice recognition works (partial results show in logs)
- [ ] Text appears in target app's text field
- [ ] **Panel stays open after first input** ⭐ NEW
- [ ] **Shows "✓ Text added" message** ⭐ NEW
- [ ] **Auto-restarts recognition after 800ms** ⭐ NEW
- [ ] **Can speak multiple times without reopening** ⭐ NEW
- [ ] **Only closes when back button tapped** ⭐ NEW
- [ ] Cursor moves to end of inserted text
- [ ] Trailing space is added
- [ ] Works in multiple apps
- [ ] No "Dropping event" errors in logs
- [ ] No crashes during voice input

## Quick Verification Command

Run this one-liner to check if voice input is working:
```bash
adb logcat -c && echo "Cleared logs. Now use voice input..." && adb logcat | grep -E "Voice input committed|Verified: Text successfully inserted"
```

You should see both messages after using voice input successfully.

---

**Need help?** Check `VOICE_INPUT_FIX_SUMMARY.md` for technical details.

