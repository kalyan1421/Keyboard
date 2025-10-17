import UIKit
import Flutter
import Firebase

@main
@objc class AppDelegate: FlutterAppDelegate {
    
    private let CHANNEL = "ai_keyboard/config"
    
    override func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        
        print("🚀 App launching - AppDelegate didFinishLaunching")
        
        // ✅ Initialize Firebase FIRST, before accessing any Flutter components
        do {
            FirebaseApp.configure()
            print("✅ Firebase configured successfully")
        } catch {
            print("❌ Firebase configuration failed: \(error)")
        }
        
        // ✅ Call super first to ensure Flutter is properly initialized
        let result = super.application(application, didFinishLaunchingWithOptions: launchOptions)
        print("✅ Flutter super.application completed with result: \(result)")
        
        // ✅ Now safely access Flutter engine after initialization
        guard let window = self.window else {
            print("❌ Window is nil")
            return result
        }
        
        guard let controller = window.rootViewController as? FlutterViewController else {
            print("❌ Failed to get FlutterViewController - rootViewController type: \(type(of: window.rootViewController))")
            return result
        }
        
        print("✅ FlutterViewController obtained successfully")
        
        // ✅ Setup method channel with error handling
        setupMethodChannel(controller: controller)
        
        // ✅ Register Flutter plugins
        GeneratedPluginRegistrant.register(with: self)
        
        // Setup shortcuts for easier access (commented out until ShortcutsManager is added to Runner target)
        // if #available(iOS 12.0, *) {
        //     ShortcutsManager.shared.setupKeyboardShortcuts()
        // }
        
        return result
    }
    
    // MARK: - Method Channel Setup
    private func setupMethodChannel(controller: FlutterViewController) {
        do {
            let keyboardChannel = FlutterMethodChannel(name: CHANNEL, binaryMessenger: controller.binaryMessenger)
            print("✅ Method channel created successfully")
            
            keyboardChannel.setMethodCallHandler({ [weak self] (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
                guard let self = self else {
                    result(FlutterError(code: "NO_SELF", message: "AppDelegate instance is nil", details: nil))
                    return
                }
                
                print("📞 Method channel call received: \(call.method)")
                
                switch call.method {
                case "isKeyboardEnabled":
                    result(self.isKeyboardEnabled())
                case "isKeyboardActive":
                    result(self.isKeyboardActive())
                case "openKeyboardSettings":
                    self.openKeyboardSettings()
                    result(true)
                case "openInputMethodPicker":
                    // iOS doesn't have an input method picker like Android
                    result(false)
                case "updateSettings":
                    if let args = call.arguments as? [String: Any] {
                        self.updateKeyboardSettings(args)
                    }
                    result(true)
                case "showKeyboardTutorial":
                    self.showKeyboardTutorial()
                    result(true)
                case "openKeyboardsDirectly":
                    self.openKeyboardsDirectly()
                    result(true)
                case "checkKeyboardPermissions":
                    result(self.checkKeyboardPermissions())
                default:
                    print("⚠️ Unknown method: \(call.method)")
                    result(FlutterMethodNotImplemented)
                }
            })
            
            print("✅ Method channel handler set successfully")
        } catch {
            print("❌ Failed to setup method channel: \(error)")
        }
    }
    
    private func isKeyboardEnabled() -> Bool {
        // Check if the keyboard extension is enabled
        // This is a simplified check - in production, you might want more sophisticated detection
        guard let bundleIdentifier = Bundle.main.bundleIdentifier else { return false }
        let keyboardBundleId = "\(bundleIdentifier).KeyboardExtension"
        
        // Check if keyboard is in the list of enabled keyboards
        if let keyboards = UserDefaults.standard.object(forKey: "AppleKeyboards") as? [String] {
            return keyboards.contains(keyboardBundleId)
        }
        
        return false
    }
    
    private func isKeyboardActive() -> Bool {
        // iOS doesn't provide easy way to check if keyboard is currently active
        // Return true if enabled for simplicity
        return isKeyboardEnabled()
    }
    
    private func openKeyboardSettings() {
        // Try to open directly to Keyboard settings with deep link
        let keyboardSettingsUrl = "App-prefs:General&path=Keyboard"
        
        if let url = URL(string: keyboardSettingsUrl),
           UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url, completionHandler: nil)
        } else {
            // Fallback to general settings
            if let settingsUrl = URL(string: UIApplication.openSettingsURLString) {
                if UIApplication.shared.canOpenURL(settingsUrl) {
                    UIApplication.shared.open(settingsUrl, completionHandler: nil)
                }
            }
        }
    }
    
    private func updateKeyboardSettings(_ settings: [String: Any]) {
        // Share settings with keyboard extension using App Groups
        if let userDefaults = UserDefaults(suiteName: "group.com.example.aiKeyboard.shared") {
            userDefaults.set(settings["theme"] as? String ?? "default", forKey: "keyboard_theme")
            userDefaults.set(settings["aiSuggestions"] as? Bool ?? true, forKey: "ai_suggestions")
            userDefaults.set(settings["swipeTyping"] as? Bool ?? true, forKey: "swipe_typing")
            userDefaults.set(settings["voiceInput"] as? Bool ?? true, forKey: "voice_input")
            userDefaults.synchronize()
            
            // Notify keyboard extension of settings change
            let notificationCenter = CFNotificationCenterGetDarwinNotifyCenter()
            let notificationName = "com.example.aiKeyboard.settingsChanged" as CFString
            CFNotificationCenterPostNotification(notificationCenter, CFNotificationName(notificationName), nil, nil, true)
        }
    }
    
    private func showKeyboardTutorial() {
        // Show interactive tutorial for keyboard setup
        guard let rootViewController = window?.rootViewController else { return }
        
        let alert = UIAlertController(
            title: "🎯 Quick Setup Guide",
            message: "Follow these steps to enable AI Keyboard:",
            preferredStyle: .alert
        )
        
        let tutorialMessage = """
        Step 1: Tap 'Go to Settings' below
        Step 2: Scroll to 'Keyboards' section
        Step 3: Tap 'Add New Keyboard...'
        Step 4: Find 'AI Keyboard' in list
        Step 5: Tap to enable it
        Step 6: Return to any app and test!
        
        💡 Tip: Long-press the 🌐 key to switch keyboards quickly!
        """
        
        alert.message = tutorialMessage
        
        alert.addAction(UIAlertAction(title: "Go to Settings", style: .default) { _ in
            self.openKeyboardsDirectly()
        })
        
        alert.addAction(UIAlertAction(title: "Later", style: .cancel))
        
        rootViewController.present(alert, animated: true)
    }
    
    private func openKeyboardsDirectly() {
        // Multiple attempts to open keyboard settings directly
        let keyboardUrls = [
            "App-prefs:General&path=Keyboard",
            "prefs:General&path=Keyboard",
            "App-prefs:General&path=KEYBOARD",
            UIApplication.openSettingsURLString
        ]
        
        for urlString in keyboardUrls {
            if let url = URL(string: urlString),
               UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url, completionHandler: nil)
                return
            }
        }
    }
    
    private func checkKeyboardPermissions() -> Bool {
        // Enhanced keyboard permission checking
        guard let bundleIdentifier = Bundle.main.bundleIdentifier else { return false }
        let keyboardBundleId = "\(bundleIdentifier).KeyboardExtension"
        
        // Check multiple sources for keyboard status
        let sources = [
            UserDefaults.standard.object(forKey: "AppleKeyboards") as? [String],
            UserDefaults.standard.object(forKey: "AddedKeyboards") as? [String]
        ]
        
        for keyboards in sources {
            if let keyboards = keyboards,
               keyboards.contains(where: { $0.contains(keyboardBundleId) }) {
                return true
            }
        }
        
        return false
    }
}

// MARK: - Keyboard Extension Manager
class KeyboardExtensionManager {
    static let shared = KeyboardExtensionManager()
    
    private init() {}
    
    func sendSettingsUpdate(theme: String, aiSuggestions: Bool, swipeTyping: Bool, voiceInput: Bool) {
        guard let userDefaults = UserDefaults(suiteName: "group.com.example.aiKeyboard.shared") else { return }
        
        userDefaults.set(theme, forKey: "keyboard_theme")
        userDefaults.set(aiSuggestions, forKey: "ai_suggestions")
        userDefaults.set(swipeTyping, forKey: "swipe_typing")
        userDefaults.set(voiceInput, forKey: "voice_input")
        userDefaults.synchronize()
        
        // Post notification to keyboard extension
        let notification = CFNotificationCenterGetDarwinNotifyCenter()
        let name = "com.example.aiKeyboard.settingsChanged" as CFString
        CFNotificationCenterPostNotification(notification, CFNotificationName(name), nil, nil, true)
    }
    
    // Handle Siri shortcuts and user activities (TODO: Implement after adding ShortcutsManager)
    // func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
    //     if #available(iOS 12.0, *) {
    //         if ShortcutsManager.shared.handleShortcut(userActivity: userActivity) {
    //             return true
    //         }
    //     }
    //     return false
    // }
}
