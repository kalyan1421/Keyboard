import Foundation
import Intents
import UIKit

// Shortcuts integration for easier keyboard access
@available(iOS 12.0, *)
class ShortcutsManager {
    static let shared = ShortcutsManager()
    
    private init() {}
    
    func setupKeyboardShortcuts() {
        // Create shortcut for opening keyboard settings
        let activity = NSUserActivity(activityType: "com.example.ai_keyboard.openSettings")
        activity.title = "Open AI Keyboard Settings"
        activity.userInfo = ["action": "openSettings"]
        activity.isEligibleForSearch = true
        activity.isEligibleForPrediction = true
        activity.persistentIdentifier = "ai_keyboard_settings"
        
        // Set up Siri shortcut phrase
        if #available(iOS 13.0, *) {
            activity.suggestedInvocationPhrase = "Open AI Keyboard Settings"
        }
        
        // Make it current so Siri can learn from it
        activity.becomeCurrent()
    }
    
    func handleShortcut(userActivity: NSUserActivity) -> Bool {
        guard let action = userActivity.userInfo?["action"] as? String else {
            return false
        }
        
        switch action {
        case "openSettings":
            openKeyboardSettings()
            return true
        default:
            return false
        }
    }
    
    private func openKeyboardSettings() {
        // Multiple attempts to open keyboard settings
        let keyboardUrls = [
            "App-prefs:General&path=Keyboard",
            "prefs:General&path=Keyboard",
            UIApplication.openSettingsURLString
        ]
        
        for urlString in keyboardUrls {
            if let url = URL(string: urlString),
               UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url, completionHandler: nil)
                break
            }
        }
    }
}

// Intent for Siri Shortcuts (iOS 13+)
@available(iOS 13.0, *)
class OpenKeyboardSettingsIntent: NSObject {
    static func createShortcut() -> INShortcut? {
        let activity = NSUserActivity(activityType: "com.example.ai_keyboard.openSettings")
        activity.title = "Open AI Keyboard Settings"
        activity.suggestedInvocationPhrase = "Open keyboard settings"
        
        return INShortcut(userActivity: activity)
    }
}
