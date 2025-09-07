# 🎯 iOS Keyboard Extension Setup - Quick Guide

## The Problem
Your AI Keyboard is not appearing in iOS Settings because the **Keyboard Extension Target** hasn't been created in Xcode yet.

## The Solution (5 minutes)

### Step 1: Open Xcode
```bash
cd ios
open Runner.xcworkspace
```

### Step 2: Create Keyboard Extension Target

1. **In Xcode Project Navigator:**
   - Click on "Runner" (the top-level project)
   - You'll see the targets list in the main area

2. **Add New Target:**
   - Click the "+" button at the bottom of the targets list
   - Choose: **iOS → App Extension → Custom Keyboard Extension**

3. **Configure Extension:**
   - **Product Name:** `KeyboardExtension`
   - **Bundle Identifier:** `com.example.aiKeyboard.KeyboardExtension`
   - **Language:** Swift
   - **Team:** (Select your development team)
   - **UNCHECK** "Include UI Extension"
   - Click "Finish"

4. **When Xcode asks "Activate KeyboardExtension scheme?"**
   - Click "**Cancel**" (we want to keep using Runner scheme)

### Step 3: Replace Generated Files

Xcode just created a basic template. Now we need to use our custom implementation:

1. **Delete the generated file:**
   - In Xcode, find the auto-generated `KeyboardViewController.swift` in the KeyboardExtension group
   - Right-click → Delete → Move to Trash

2. **Add our custom files:**
   - Drag these files from Finder into the KeyboardExtension group in Xcode:
     - `ios/KeyboardExtension/KeyboardViewController.swift`
     - `ios/KeyboardExtension/SettingsManager.swift`
   - When prompted, make sure "Add to target" has **KeyboardExtension** checked

3. **Replace Info.plist:**
   - Delete the generated Info.plist in KeyboardExtension group
   - Drag `ios/KeyboardExtension/Info.plist` from Finder into the group

### Step 4: Configure App Groups

**For Runner target:**
1. Select "Runner" target in the project settings
2. Go to "Signing & Capabilities" tab
3. Click "+" and add "App Groups"
4. Add group: `group.com.example.aiKeyboard`

**For KeyboardExtension target:**
1. Select "KeyboardExtension" target
2. Go to "Signing & Capabilities" tab  
3. Click "+" and add "App Groups"
4. Add the SAME group: `group.com.example.aiKeyboard`

### Step 5: Build and Test

1. Select "Runner" scheme (not KeyboardExtension)
2. Select your device or simulator
3. Press ⌘+R to build and run
4. Install on your device
5. Go to Settings → General → Keyboard → Keyboards → Add New Keyboard
6. Look for "AI Keyboard" in the Third-Party Keyboards section!

## ✅ Success Indicators

- Xcode builds without errors
- App installs successfully on device
- "AI Keyboard" appears in iOS Settings
- Keyboard can be enabled and used in any app

## 🔧 Troubleshooting

### "AI Keyboard" not in Settings
- Rebuild the project (Product → Clean Build Folder, then ⌘+R)
- Check that both targets have the same App Group
- Verify KeyboardExtension target builds successfully

### Build Errors
- Make sure all files are added to KeyboardExtension target
- Check bundle identifiers match the pattern: `com.example.aiKeyboard.KeyboardExtension`
- Ensure deployment target is iOS 12.0+

### Files not found
- The KeyboardExtension files should already exist in your project
- If missing, they may have been created with different bundle identifiers
- Check the ios/KeyboardExtension/ directory

## 🎉 What's Next?

Once the keyboard appears in Settings:
1. Enable "AI Keyboard" in iOS Settings
2. Grant "Allow Full Network Access" if you want AI features
3. Test typing in any app (Messages, Notes, etc.)
4. Long-press the 🌐 globe key to switch between keyboards
5. Customize themes and settings in the main AI Keyboard app

The keyboard and main app will sync settings automatically using App Groups!
