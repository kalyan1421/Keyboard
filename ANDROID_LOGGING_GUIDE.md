# Android Logging Guide

## Normal Flutter/SurfaceView Logs

These logs are **100% normal** for any Flutter/SurfaceView-based activity and do NOT indicate any issues with your keyboard.

### Common Logs You'll See

#### BufferQueueProducer Logs
```
BufferQueueProducer
queueBuffer: slot=0/516 time=...
queueBuffer: slot=1/517 time=...
addAndGetFrameTimestamps
```

#### Why These Logs Keep Repeating

Your MainActivity (the Flutter side) uses:
- **SurfaceView** → Skia rendering pipeline → BufferQueueProducer

Flutter continuously draws frames (even when static) to keep UI smooth. So Android logs every buffer queued to the SurfaceFlinger + BLAST Consumer.

### Meaning of Each Log

| Log | Meaning |
|-----|---------|
| `BufferQueueProducer` | Flutter drawing a frame |
| `queueBuffer` | A new frame (texture) added to pipeline |
| `addAndGetFrameTimestamps` | Sync timing of the frame |
| `dataSpace=142671872` | Color space info (sRGB) |
| `scale=SCALE_TO_WINDOW` | Surface is scaled to window size |

**Nothing is wrong.**

### When Does This Happen?

These logs appear during:
- ✅ Running Flutter Activity behind your Android IME
- ✅ Using SurfaceView in the keyboard overlay
- ✅ Opening/closing panels (emoji, AI panel)
- ✅ Theme animations
- ✅ Keyboard height recalculations

### When to Worry?

Only if:
- ❌ Frame rate drops significantly
- ❌ Stutter in keyboard UI
- ❌ Logs spam at thousands/sec (yours is normal rate)

### Reducing Log Spam (Optional)

If you want to reduce log spam, you can hide SurfaceFlinger logs:

#### Temporarily Disable (Not Recommended)
```bash
adb shell setprop log.tag.BufferQueueProducer SILENT
```

#### Filter Your Logcat

Filter your Logcat to show only relevant tags:
- `AIKeyboardService`
- `UnifiedLayoutController`
- `EmojiPanelController`
- `SwipeKeyboardView`
- `MainActivity`
- `LogUtil`

Example Logcat filter:
```
package:com.example.ai_keyboard | tag:AIKeyboardService | tag:MainActivity | tag:LogUtil
```

### Final Answer

➡️ **These logs are harmless.**  
➡️ **They do NOT indicate any issue with your keyboard.**  
➡️ **They will always appear as long as Flutter UI is running.**

---

## Application Logging

For application-specific logging, see `LogUtil.kt` which provides centralized logging utilities:
- Debug logs are only enabled in DEBUG builds
- Error logs are always enabled
- Provides consistent logging across the application

### Using LogUtil

```kotlin
import com.example.ai_keyboard.utils.LogUtil

// Debug log (only in DEBUG builds)
LogUtil.d("MyClass", "Debug message")

// Error log (always enabled)
LogUtil.e("MyClass", "Error message", exception)

// Warning log (only in DEBUG builds)
LogUtil.w("MyClass", "Warning message")

// Info log (only in DEBUG builds)
LogUtil.i("MyClass", "Info message")
```

