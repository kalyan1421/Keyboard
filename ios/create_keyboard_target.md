# Quick Xcode Setup for AI Keyboard

## Problem
The AI Keyboard is not appearing in iOS Settings → Keyboards → Add New Keyboard because the Keyboard Extension target hasn't been created in Xcode.

## Solution: 5-Minute Setup

### Step 1: Open Xcode
```bash
cd ios
open Runner.xcworkspace
```

### Step 2: Create Keyboard Extension
1. Click "Runner" project (top-level in navigator)
2. Click "+" at bottom of targets list
3. Choose: **App Extension → Custom Keyboard Extension**
4. Configure:
   - Product Name: `KeyboardExtension`
   - Bundle Identifier: `com.example.ai_keyboard.KeyboardExtension`
   - Language: Swift
   - **UNCHECK** "Include UI Extension"
5. Click "Finish"
6. When asked "Activate scheme?", click "Cancel"

### Step 3: Replace Generated Files
1. Delete auto-generated `KeyboardViewController.swift`
2. Drag our files from Finder into the KeyboardExtension group:
   - `ios/KeyboardExtension/KeyboardViewController.swift`
   - `ios/KeyboardExtension/SettingsManager.swift`
   - `ios/KeyboardExtension/Info.plist`

### Step 4: Configure App Groups
**For Runner target:**
1. Select "Runner" target
2. Signing & Capabilities → Add "App Groups"
3. Add: `group.com.example.ai_keyboard`

**For KeyboardExtension target:**
1. Select "KeyboardExtension" target  
2. Signing & Capabilities → Add "App Groups"
3. Add: `group.com.example.ai_keyboard`

### Step 5: Build & Test
1. Select "Runner" scheme
2. Press ⌘+B to build
3. Run on device: ⌘+R
4. Check Settings → Keyboards → Add New Keyboard
5. "AI Keyboard" should now appear!

## If Still Not Working

Try this minimal approach:

1. **Just create the target** (Steps 1-2 above)
2. **Don't add custom files yet** - use the generated template
3. **Build and test** - generic keyboard should appear
4. **Then replace files** with our custom implementation

## Expected Result
After completing Step 2, you should see "AI Keyboard" in:
Settings → General → Keyboard → Keyboards → Add New Keyboard → Third-Party Keyboards

The key is creating the proper **Keyboard Extension target** in Xcode - the files we created are ready, we just need Xcode to package them as a proper keyboard extension.
