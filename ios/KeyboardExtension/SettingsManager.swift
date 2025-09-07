import Foundation

/// Manages settings synchronization between the main app and keyboard extension
class SettingsManager {
    static let shared = SettingsManager()
    
    private let appGroupIdentifier = "group.com.example.aiKeyboard"
    
    private var userDefaults: UserDefaults? {
        return UserDefaults(suiteName: appGroupIdentifier)
    }
    
    private init() {
        // Simple initialization without complex notifications for now
    }
    
    // MARK: - Settings Properties
    
    var keyboardTheme: String {
        get { userDefaults?.string(forKey: "keyboard_theme") ?? "default" }
        set { 
            userDefaults?.set(newValue, forKey: "keyboard_theme")
            userDefaults?.synchronize()
        }
    }
    
    var aiSuggestionsEnabled: Bool {
        get { userDefaults?.bool(forKey: "ai_suggestions") ?? true }
        set { 
            userDefaults?.set(newValue, forKey: "ai_suggestions")
            userDefaults?.synchronize()
        }
    }
    
    var swipeTypingEnabled: Bool {
        get { userDefaults?.bool(forKey: "swipe_typing") ?? true }
        set { 
            userDefaults?.set(newValue, forKey: "swipe_typing")
            userDefaults?.synchronize()
        }
    }
    
    var voiceInputEnabled: Bool {
        get { userDefaults?.bool(forKey: "voice_input") ?? true }
        set { 
            userDefaults?.set(newValue, forKey: "voice_input")
            userDefaults?.synchronize()
        }
    }
    
    // MARK: - Bulk Operations
    
    func loadAllSettings() -> KeyboardSettings {
        return KeyboardSettings(
            theme: keyboardTheme,
            aiSuggestions: aiSuggestionsEnabled,
            swipeTyping: swipeTypingEnabled,
            voiceInput: voiceInputEnabled
        )
    }
    
    func saveAllSettings(_ settings: KeyboardSettings) {
        keyboardTheme = settings.theme
        aiSuggestionsEnabled = settings.aiSuggestions
        swipeTypingEnabled = settings.swipeTyping
        voiceInputEnabled = settings.voiceInput
    }
}

// MARK: - Settings Model

struct KeyboardSettings {
    let theme: String
    let aiSuggestions: Bool
    let swipeTyping: Bool
    let voiceInput: Bool
}

// MARK: - Notification Extension

extension Notification.Name {
    static let settingsDidChange = Notification.Name("settingsDidChange")
}