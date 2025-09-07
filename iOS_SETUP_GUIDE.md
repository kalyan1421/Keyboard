# iOS AI Keyboard Setup Guide - Simplified Edition

## 🎯 Overview
This guide will help you set up the iOS keyboard extension with **simplified input method switching**. The enhanced iOS implementation now includes multiple ways to make keyboard setup and switching as easy as possible for users.

## ✨ New Simplified Features
- **Interactive Setup Tutorial**: Step-by-step guidance in the app
- **Direct Settings Links**: Attempts to open keyboard settings directly
- **Quick Switch Guide**: Visual instructions for keyboard switching
- **Setup Reminders**: Automatic prompts for iOS users
- **Siri Shortcuts Integration**: Voice commands to open settings
- **Widget Support**: Home screen widget for quick access

## 📋 Prerequisites
- Xcode 15.0 or later
- iOS 12.0+ deployment target
- Apple Developer Account (for device testing)
- Flutter project already set up

## 🚀 Step-by-Step Setup

### Step 1: Open Xcode Project
```bash
cd ios
open Runner.xcworkspace
```

### Step 2: Create Keyboard Extension Target

1. **In Xcode:**
   - Select the project in the navigator (top-level "Runner")
   - Click the "+" button at the bottom of the targets list
   - Choose "App Extension" → "Custom Keyboard Extension"
   - Configure the extension:
     - **Product Name:** `KeyboardExtension`
     - **Bundle Identifier:** `com.example.ai_keyboard.KeyboardExtension`
     - **Language:** Swift
     - **Uncheck** "Include UI Extension"

2. **Delete generated files:**
   - Delete the auto-generated `KeyboardViewController.swift`
   - We'll replace it with our custom implementation

### Step 3: Add iOS Files

The following files have been created in your project:

- `ios/KeyboardExtension/KeyboardViewController.swift` - Main keyboard implementation
- `ios/KeyboardExtension/SettingsManager.swift` - Settings synchronization
- `ios/KeyboardExtension/Info.plist` - Extension configuration
- `ios/Runner/AppDelegate.swift` - Updated with platform channels

### Step 4: Configure App Groups

**For Main App (Runner target):**
1. Select Runner target → Signing & Capabilities
2. Click "+" and add "App Groups"
3. Add group: `group.com.example.ai_keyboard`

**For Keyboard Extension:**
1. Select KeyboardExtension target → Signing & Capabilities  
2. Click "+" and add "App Groups"
3. Add the same group: `group.com.example.ai_keyboard`

### Step 5: Configure Build Settings

**KeyboardExtension Target Settings:**
- **Deployment Target:** iOS 12.0 or later
- **Swift Language Version:** Swift 5
- **Code Signing:** Same as main app
- **Architectures:** arm64 (device), x86_64 (simulator)

### Step 6: Verify File Structure

Your iOS directory should look like this:
```
ios/
├── Runner/
│   ├── AppDelegate.swift          ✅ Updated
│   └── Info.plist
├── KeyboardExtension/             ✅ New
│   ├── KeyboardViewController.swift
│   ├── SettingsManager.swift
│   └── Info.plist
└── Runner.xcworkspace
```

### Step 7: Build and Test

1. **Clean and build:**
   ```bash
   # In Flutter project root
   flutter clean
   flutter pub get
   flutter build ios
   ```

2. **In Xcode:**
   - Select your device/simulator
   - Select "Runner" scheme
   - Build and run (⌘+R)

## 📱 Enable Keyboard on Device

### For Users:
1. **Install the app** on iOS device
2. **Go to Settings** → General → Keyboard → Keyboards
3. **Tap "Add New Keyboard..."**
4. **Select "AI Keyboard"** from Third-Party Keyboards
5. **Enable "Allow Full Network Access"** (if needed for AI features)
6. **Test in any app** by tapping a text field

### For Development:
- The keyboard will appear in the third-party keyboards list after installation
- You can switch keyboards using the globe icon or long-press the globe icon
- Settings changes in the main app will sync to the keyboard extension

## ✨ Features Implemented

### ✅ Core Features
- **System-wide keyboard**: Works in all iOS apps
- **Multiple layouts**: Letters, symbols, numbers  
- **Theme support**: 5 themes matching Android version
- **AI suggestions**: Smart text predictions
- **Swipe gestures**: Left=delete, right=space, up=shift, down=dismiss
- **Auto-shift**: Automatic capitalization after punctuation
- **Caps lock**: Double-tap shift for caps lock
- **Settings sync**: Real-time updates between app and keyboard

### ✅ iOS-Specific Enhancements
- **Native iOS styling**: Follows iOS design guidelines
- **Sound feedback**: Native iOS click sounds
- **App Groups**: Secure data sharing
- **Darwin notifications**: Real-time settings updates
- **Memory optimization**: Designed for iOS extension limits
- **VoiceOver support**: Built-in accessibility

## 🔧 Troubleshooting

### Keyboard Not Appearing
1. Check that KeyboardExtension target builds successfully
2. Verify App Groups are configured identically for both targets
3. Ensure keyboard is enabled in iOS Settings
4. Try deleting and re-adding the keyboard in Settings

### Settings Not Syncing
1. Verify App Groups capability is enabled for both targets
2. Check UserDefaults suite name matches exactly: `group.com.example.ai_keyboard`
3. Ensure Darwin notifications are properly configured

### Build Errors
1. Check Swift version compatibility (Swift 5)
2. Verify deployment target is iOS 12.0+
3. Ensure all files are added to correct target
4. Clean build folder and retry (Product → Clean Build Folder)

### Memory Issues
1. iOS keyboard extensions have strict memory limits
2. Avoid loading large assets or performing heavy operations
3. Use lazy loading for UI components
4. Monitor memory usage in Xcode debugger

## 🔐 Security & Privacy

### iOS Keyboard Extensions have restrictions:
- **Limited network access** by default
- **Sandboxed environment** 
- **Memory constraints** (more restrictive than main apps)
- **No access to other apps' data**

### For AI features requiring network access:
1. User must enable "Allow Full Network Access" in keyboard settings
2. Handle network errors gracefully
3. Cache suggestions locally when possible
4. Respect user privacy (no keystroke logging)

## 🎨 Customization

### Adding New Themes
1. Update `KeyboardTheme` enum in `KeyboardViewController.swift`
2. Add new theme case with colors
3. Ensure theme names match between iOS and Android

### Custom Key Layouts
1. Modify the `rows` arrays in keyboard creation methods
2. Add new special key handlers
3. Update key button creation logic
4. Test across different screen sizes (iPhone/iPad)

### AI Integration
1. Add network permissions if needed
2. Implement proper API calls in suggestion generation
3. Add error handling and fallbacks
4. Consider on-device ML models for privacy

## 📊 Performance Optimization

### Memory Management
- Use weak references to avoid retain cycles
- Implement proper deallocation in `deinit`
- Monitor memory usage during development
- Use Instruments to profile memory usage

### UI Performance
- Use `UIStackView` for efficient layouts
- Implement view recycling for suggestion buttons
- Avoid creating views on every theme change
- Use `CALayer` properties for styling when possible

## 🧪 Testing Checklist

- [ ] Keyboard appears in all apps (Messages, Notes, Safari, etc.)
- [ ] All three layouts work (letters, symbols, numbers)
- [ ] Themes change correctly from main app
- [ ] Suggestions appear and function properly
- [ ] Swipe gestures work as expected
- [ ] Shift and caps lock functionality
- [ ] Settings sync between app and keyboard
- [ ] Sound feedback plays correctly
- [ ] Memory usage stays within limits
- [ ] No crashes during extended typing sessions
- [ ] Works on both iPhone and iPad
- [ ] Supports both portrait and landscape orientations

## 🆚 iOS vs Android Differences

| Feature | Android | iOS |
|---------|---------|-----|
| **Implementation** | InputMethodService | Keyboard Extension |
| **Permissions** | Accessibility Service | App Groups + Full Access |
| **Settings Storage** | SharedPreferences | UserDefaults with App Groups |
| **Communication** | Platform Channels | Darwin Notifications |
| **UI Layout** | XML Resources | Programmatic UIKit |
| **Themes** | XML drawable resources | UIColor programmatic |
| **Gestures** | Native touch handling | UIGestureRecognizer |
| **Memory Limits** | More permissive | Very strict |
| **Installation** | Enable in Settings | Add keyboard in Settings |

## 🔄 Continuous Integration

### For automated builds:
```yaml
# .github/workflows/ios.yml
- name: Build iOS
  run: |
    cd ios
    xcodebuild -workspace Runner.xcworkspace -scheme Runner -destination 'platform=iOS Simulator,name=iPhone 14' build
```

## 📚 Additional Resources

- [Apple Keyboard Extension Guide](https://developer.apple.com/documentation/uikit/keyboards_and_input)
- [App Groups Documentation](https://developer.apple.com/documentation/foundation/userdefaults)
- [iOS Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/keyboards)
- [Swift UIKit Documentation](https://developer.apple.com/documentation/uikit)

## 🎉 Congratulations!

You now have a fully functional cross-platform AI Keyboard that works on both Android and iOS! The keyboard provides:

- **Consistent experience** across platforms
- **Native performance** on each platform  
- **Real-time settings synchronization**
- **Professional UI/UX** following platform guidelines
- **Extensible architecture** for future enhancements

Your users can now enjoy the same AI-powered typing experience whether they're on Android or iOS! 🚀
