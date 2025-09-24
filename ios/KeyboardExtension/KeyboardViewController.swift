//
//  KeyboardViewController.swift
//  KeyboardExtension
//
 //  Enhanced iOS Keyboard Extension following Flutter AI Keyboard Development Roadmap
//  Implements programmatic UI, App Groups data sharing, and proper text insertion
//

import UIKit
import AudioToolbox

class KeyboardViewController: UIInputViewController {

    // MARK: - Properties
    private var keyboardView: UIView!
    private var nextKeyboardButton: UIButton!
    private var keyboardHeight: CGFloat = 216
    private let settingsManager = SettingsManager()
    
    // Keyboard layout properties
    private let keyRows = [
        ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
        ["a", "s", "d", "f", "g", "h", "j", "k", "l"],
        ["shift", "z", "x", "c", "v", "b", "n", "m", "delete"],
        ["123", "globe", "space", "return"]
    ]
    
    // Enhanced Shift State Management (3-State FSM)
    private enum ShiftState {
        case normal      // 0 - Default lowercase
        case shift       // 1 - Single uppercase (next character only)
        case capsLock    // 2 - Continuous uppercase
    }
    
    private var shiftState: ShiftState = .normal
    private var lastShiftPressTime: TimeInterval = 0
    private var doubleTapTimeout: TimeInterval = 0.3
    
    // Backward compatibility
    private var isShifted: Bool {
        return shiftState != .normal
    }
    private var isCapsLock: Bool {
        return shiftState == .capsLock
    }
    
    // MARK: - Lifecycle Methods
    override func viewDidLoad() {
        super.viewDidLoad()
        setupKeyboard()
        loadSettings()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // Reload settings when keyboard appears
        loadSettings()
        updateKeyboardAppearance()
    }
    
    override func updateViewConstraints() {
        super.updateViewConstraints()
        
        // Set keyboard height constraint
        if let heightConstraint = view.constraints.first(where: { $0.firstAttribute == .height }) {
            heightConstraint.constant = keyboardHeight
        } else {
            view.heightAnchor.constraint(equalToConstant: keyboardHeight).isActive = true
        }
    }
    
    // MARK: - Keyboard Setup (Programmatic UI - No Storyboards)
    private func setupKeyboard() {
        // Create main keyboard container
        keyboardView = UIView()
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(keyboardView)
        
        // Setup constraints for keyboard view
        NSLayoutConstraint.activate([
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        createKeyboardLayout()
    }
    
    private func createKeyboardLayout() {
        let stackView = UIStackView()
        stackView.axis = .vertical
        stackView.distribution = .fillEqually
        stackView.spacing = 8
        stackView.translatesAutoresizingMaskIntoConstraints = false
        
        keyboardView.addSubview(stackView)
        
        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: keyboardView.topAnchor, constant: 8),
            stackView.leadingAnchor.constraint(equalTo: keyboardView.leadingAnchor, constant: 4),
            stackView.trailingAnchor.constraint(equalTo: keyboardView.trailingAnchor, constant: -4),
            stackView.bottomAnchor.constraint(equalTo: keyboardView.bottomAnchor, constant: -8)
        ])
        
        // Create rows
        for (rowIndex, row) in keyRows.enumerated() {
            let rowStackView = createRowStackView(for: row, rowIndex: rowIndex)
            stackView.addArrangedSubview(rowStackView)
        }
    }
    
    private func createRowStackView(for keys: [String], rowIndex: Int) -> UIStackView {
        let rowStackView = UIStackView()
        rowStackView.axis = .horizontal
        rowStackView.distribution = .fillEqually
        rowStackView.spacing = 4
        
        for key in keys {
            let button = createKeyButton(for: key, rowIndex: rowIndex)
            rowStackView.addArrangedSubview(button)
        }
        
        return rowStackView
    }
    
    private func createKeyButton(for key: String, rowIndex: Int) -> UIButton {
        let button = UIButton(type: .system)
        button.translatesAutoresizingMaskIntoConstraints = false
        
        // Configure button appearance
        button.layer.cornerRadius = 6
        button.layer.borderWidth = 1
        button.titleLabel?.font = UIFont.systemFont(ofSize: 18, weight: .medium)
        
        // Set button title and action based on key type
        switch key {
        case "shift":
            button.setTitle("⇧", for: .normal)
            button.addTarget(self, action: #selector(shiftPressed), for: .touchUpInside)
        case "delete":
            button.setTitle("⌫", for: .normal)
            button.addTarget(self, action: #selector(deletePressed), for: .touchUpInside)
        case "123":
            button.setTitle("123", for: .normal)
            button.addTarget(self, action: #selector(numbersPressed), for: .touchUpInside)
        case "globe":
            button.setTitle("🌐", for: .normal)
            button.addTarget(self, action: #selector(globePressed), for: .touchUpInside)
        case "space":
            button.setTitle("space", for: .normal)
            button.addTarget(self, action: #selector(spacePressed), for: .touchUpInside)
        case "return":
            button.setTitle("return", for: .normal)
            button.addTarget(self, action: #selector(returnPressed), for: .touchUpInside)
        default:
            button.setTitle(key, for: .normal)
            button.addTarget(self, action: #selector(keyPressed(_:)), for: .touchUpInside)
        }
        
        return button
    }
    
    // MARK: - Settings Management (App Groups Implementation)
    private func loadSettings() {
        // Load settings from shared UserDefaults (App Groups)
        settingsManager.loadSettings()
        updateKeyboardAppearance()
    }
    
    private func updateKeyboardAppearance() {
        let isDarkMode = textDocumentProxy.keyboardAppearance == .dark || settingsManager.currentTheme == "dark"
        
        // Update keyboard background
        keyboardView.backgroundColor = isDarkMode ? UIColor.systemGray6 : UIColor.systemGray5
        
        // Update all key buttons
        for case let stackView as UIStackView in keyboardView.subviews {
            updateButtonsAppearance(in: stackView, isDarkMode: isDarkMode)
        }
    }
    
    private func updateButtonsAppearance(in view: UIView, isDarkMode: Bool) {
        if let stackView = view as? UIStackView {
            for subview in stackView.arrangedSubviews {
                updateButtonsAppearance(in: subview, isDarkMode: isDarkMode)
            }
        } else if let button = view as? UIButton {
            button.backgroundColor = isDarkMode ? UIColor.systemGray4 : UIColor.white
            button.setTitleColor(isDarkMode ? UIColor.white : UIColor.black, for: .normal)
            button.layer.borderColor = isDarkMode ? UIColor.systemGray3.cgColor : UIColor.systemGray4.cgColor
        }
    }
    
    // MARK: - Key Actions (Text Insertion via textDocumentProxy)
    @objc private func keyPressed(_ sender: UIButton) {
        guard let title = sender.currentTitle else { return }
        
        // Add advanced haptic feedback based on intensity
        if settingsManager.vibrationEnabled && settingsManager.hapticIntensity > 0 {
            let feedbackStyle: UIImpactFeedbackGenerator.FeedbackStyle
            
            switch settingsManager.hapticIntensity {
            case 1: // Light
                feedbackStyle = .light
            case 2: // Medium
                feedbackStyle = .medium
            case 3: // Strong
                feedbackStyle = .heavy
            default:
                feedbackStyle = .medium
            }
            
            let impactFeedback = UIImpactFeedbackGenerator(style: feedbackStyle)
            impactFeedback.impactOccurred()
        }
        
        // Add sound feedback if enabled
        if settingsManager.soundIntensity > 0 {
            playKeySound(for: title)
        }
        
        // Add visual feedback animation
        animateKeyPress(sender)
        
        // Enhanced character handling with new shift state system
        let character: String
        switch shiftState {
        case .normal:
            character = title.lowercased()
        case .shift:
            character = title.uppercased()
            // Auto-reset to normal after single character (except for caps lock)
            shiftState = .normal
            updateShiftKey()
        case .capsLock:
            character = title.uppercased()
        }
        
        textDocumentProxy.insertText(character)
    }
    
    @objc private func shiftPressed() {
        let now = Date().timeIntervalSince1970
        
        // Enhanced 3-State Shift Management: normal -> shift -> capsLock -> normal
        switch shiftState {
        case .normal:
            // Single tap: Activate shift for next character only
            shiftState = .shift
            lastShiftPressTime = now
            showShiftFeedback("Shift ON")
            
        case .shift:
            if now - lastShiftPressTime < doubleTapTimeout {
                // Double tap detected within timeout - activate caps lock
                shiftState = .capsLock
                showShiftFeedback("CAPS LOCK")
            } else {
                // Single tap after timeout - turn off shift
                shiftState = .normal
                lastShiftPressTime = now
                showShiftFeedback("Shift OFF")
            }
            
        case .capsLock:
            // Any tap from caps lock - turn off completely
            shiftState = .normal
            showShiftFeedback("CAPS LOCK OFF")
        }
        
        updateShiftKey()
        provideShiftHapticFeedback()
    }
    
    private func showShiftFeedback(_ message: String) {
        // Show feedback if enabled in settings
        if settingsManager.shiftFeedbackEnabled {
            print("Shift State: \(message)")
            // In a real implementation, you might show a brief toast or visual indicator
        }
    }
    
    private func provideShiftHapticFeedback() {
        if settingsManager.vibrationEnabled {
            let intensity: UIImpactFeedbackGenerator.FeedbackStyle
            
            switch shiftState {
            case .normal:
                intensity = .light      // Light vibration for turning off
            case .shift:
                intensity = .medium     // Medium vibration for shift on
            case .capsLock:
                intensity = .heavy      // Strong vibration for caps lock
            }
            
            let impactFeedback = UIImpactFeedbackGenerator(style: intensity)
            impactFeedback.impactOccurred()
        }
    }
    
    @objc private func deletePressed() {
        textDocumentProxy.deleteBackward()
        
        if settingsManager.vibrationEnabled {
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()
        }
    }
    
    @objc private func spacePressed() {
        // Add enhanced haptic feedback for space bar
        if settingsManager.vibrationEnabled && settingsManager.hapticIntensity > 0 {
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()
        }
        
        // Add sound feedback
        if settingsManager.soundIntensity > 0 {
            playKeySound(for: " ")
        }
        
        textDocumentProxy.insertText(" ")
        
        // Enhanced auto-capitalize after sentence ending (if enabled)
        if settingsManager.autoCapitalizationEnabled,
           let textBefore = textDocumentProxy.documentContextBeforeInput,
           textBefore.hasSuffix(". ") || textBefore.hasSuffix("! ") || textBefore.hasSuffix("? ") {
            if shiftState == .normal {
                shiftState = .shift
                updateShiftKey()
            }
        }
    }
    
    @objc private func returnPressed() {
        textDocumentProxy.insertText("\n")
        
        // Auto-capitalize after new line (if enabled)
        if settingsManager.autoCapitalizationEnabled && shiftState == .normal {
            shiftState = .shift
            updateShiftKey()
        }
    }
    
    @objc private func numbersPressed() {
        // TODO: Implement number keyboard layout
        print("Numbers keyboard not yet implemented")
    }
    
    @objc private func globePressed() {
        advanceToNextInputMode()
    }
    
    private func updateShiftKey() {
        // Find and update shift key appearance
        // This is a simplified version - in production, you'd store button references
        for case let stackView as UIStackView in keyboardView.subviews {
            updateShiftButton(in: stackView)
        }
    }
    
    private func updateShiftButton(in view: UIView) {
        if let stackView = view as? UIStackView {
            for subview in stackView.arrangedSubviews {
                updateShiftButton(in: subview)
            }
        } else if let button = view as? UIButton, button.currentTitle == "⇧" {
            // Enhanced visual feedback based on shift state
            switch shiftState {
            case .normal:
                // Normal state - dim/unhighlighted
                button.backgroundColor = UIColor.systemGray4
                button.tintColor = UIColor.label
                button.alpha = 0.7
                
            case .shift:
                // Shift state - highlighted once
                button.backgroundColor = UIColor.systemBlue
                button.tintColor = UIColor.white
                button.alpha = 1.0
                
            case .capsLock:
                // Caps lock - strongly highlighted with underline effect
                button.backgroundColor = UIColor.systemOrange
                button.tintColor = UIColor.white
                button.alpha = 1.0
                
                // Add visual indicator for caps lock (could be an underline or border)
                button.layer.borderWidth = 2.0
                button.layer.borderColor = UIColor.systemOrange.cgColor
            }
            
            // Remove border for non-caps lock states
            if shiftState != .capsLock {
                button.layer.borderWidth = 0
                button.layer.borderColor = UIColor.clear.cgColor
            }
        }
    }
    
    // MARK: - Text Context Handling
    override func textWillChange(_ textInput: UITextInput?) {
        super.textWillChange(textInput)
    }
    
    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        updateKeyboardAppearance()
    }
    
    // MARK: - Advanced Feedback Methods
    
    private func playKeySound(for key: String) {
        // Play system sound based on key type and intensity
        let soundID: SystemSoundID
        
        switch key.lowercased() {
        case " ": // Space
            soundID = 1104 // Spacebar sound
        case "delete", "⌫":
            soundID = 1155 // Delete sound
        case "return", "↵":
            soundID = 1156 // Return sound
        default:
            soundID = 1103 // Standard key sound
        }
        
        // Adjust for sound intensity (iOS doesn't support volume directly for system sounds)
        if settingsManager.soundIntensity > 0 {
            AudioServicesPlaySystemSound(soundID)
        }
    }
    
    private func animateKeyPress(_ button: UIButton) {
        guard settingsManager.visualIntensity > 0 else { return }
        
        let animationIntensity = Double(settingsManager.visualIntensity) / 3.0
        let scaleDown = 0.95 - (0.05 * (1.0 - animationIntensity))
        let duration = 0.1 + (0.05 * animationIntensity)
        
        // Scale down animation
        UIView.animate(withDuration: duration, animations: {
            button.transform = CGAffineTransform(scaleX: scaleDown, y: scaleDown)
        }) { _ in
            // Spring back animation
            UIView.animate(withDuration: duration * 1.5, 
                          delay: 0,
                          usingSpringWithDamping: 0.6,
                          initialSpringVelocity: 0.8,
                          options: .curveEaseOut) {
                button.transform = CGAffineTransform.identity
            }
        }
        
        // Add brightness effect for higher intensities
        if settingsManager.visualIntensity >= 2 {
            UIView.animate(withDuration: duration) {
                button.alpha = 1.2
            } completion: { _ in
                UIView.animate(withDuration: duration) {
                    button.alpha = 1.0
                }
            }
        }
    }
    
    private func animateSpaceBarPress(_ button: UIButton) {
        guard settingsManager.visualIntensity > 0 else { return }
        
        // Enhanced bounce animation for space bar
        let scaleDown: CGFloat = 0.90
        let duration = 0.15
        
        UIView.animate(withDuration: duration, animations: {
            button.transform = CGAffineTransform(scaleX: scaleDown, y: scaleDown)
        }) { _ in
            // Triple bounce effect for space bar
            UIView.animate(withDuration: duration * 2, 
                          delay: 0,
                          usingSpringWithDamping: 0.3,
                          initialSpringVelocity: 1.2,
                          options: .curveEaseOut) {
                button.transform = CGAffineTransform.identity
            }
        }
    }
}
