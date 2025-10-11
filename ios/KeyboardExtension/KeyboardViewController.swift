//
//  KeyboardViewController.swift
//  KeyboardExtension
//
 //  Enhanced iOS Keyboard Extension following Flutter AI Keyboard Development Roadmap
//  Implements programmatic UI, App Groups data sharing, and proper text insertion
//

import UIKit
import AudioToolbox

@objc(KeyboardViewController)
class KeyboardViewController: UIInputViewController {

    // MARK: - Properties
    var keyboardView: UIView!
    private var keyboardHeight: CGFloat = 216
    private let settingsManager = SettingsManager.shared
    var layoutManager: LayoutManager!
    
    // Enhanced Shift State Management (3-State FSM)
    var shiftState: ShiftState = .normal
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
        
        // Initialize layout manager
        layoutManager = LayoutManager(keyboardViewController: self)
        
        // Setup keyboard with fallback
        if layoutManager != nil {
            setupKeyboard()
        } else {
            print("Failed to create LayoutManager, using basic setup")
            setupBasicKeyboard()
        }
        
        // Load settings
        loadSettings()
        
        // Auto-capitalize at document start if enabled
        if settingsManager.autoCapitalizationEnabled {
            let textBefore = textDocumentProxy.documentContextBeforeInput ?? ""
            if textBefore.isEmpty {
                shiftState = .shift
                updateShiftKey()
            }
        }
        
        // Observe settings changes from main app via Darwin notifications
        let notificationCenter = CFNotificationCenterGetDarwinNotifyCenter()
        let notificationName = "com.example.aiKeyboard.settingsChanged" as CFString
        let observer = UnsafeRawPointer(Unmanaged.passUnretained(self).toOpaque())
        
        CFNotificationCenterAddObserver(
            notificationCenter,
            observer,
            { _, observer, name, _, _ in
                guard let observer = observer else { return }
                let viewController = Unmanaged<KeyboardViewController>.fromOpaque(observer).takeUnretainedValue()
                DispatchQueue.main.async {
                    viewController.loadSettings()
                    viewController.updateKeyboardAppearance()
                }
            },
            notificationName,
            nil,
            .deliverImmediately
        )
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
    
    // MARK: - Keyboard Setup (Using LayoutManager)
    private func setupKeyboard() {
        guard layoutManager != nil else {
            print("LayoutManager is nil, falling back to basic setup")
            setupBasicKeyboard()
            return
        }
        
        // Create main keyboard container
        keyboardView = UIView()
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        keyboardView.backgroundColor = UIColor.systemGray6
        view.addSubview(keyboardView)
        
        // Setup constraints for keyboard view
        NSLayoutConstraint.activate([
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        // Use LayoutManager to create keyboard layout
        _ = layoutManager.createKeyboardLayout(in: keyboardView)
        
        // Configure accessibility
        layoutManager.configureAccessibility()
    }
    
    // MARK: - Fallback Basic Keyboard Setup
    private func setupBasicKeyboard() {
        // Create a simple fallback keyboard if LayoutManager fails
        keyboardView = UIView()
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        keyboardView.backgroundColor = UIColor.systemGray6
        view.addSubview(keyboardView)
        
        // Setup constraints
        NSLayoutConstraint.activate([
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        // Create a simple "Hello" button for testing
        let testButton = UIButton(type: .system)
        testButton.setTitle("Hello", for: .normal)
        testButton.backgroundColor = UIColor.systemBlue
        testButton.setTitleColor(.white, for: .normal)
        testButton.layer.cornerRadius = 8
        testButton.translatesAutoresizingMaskIntoConstraints = false
        
        testButton.addTarget(self, action: #selector(testButtonPressed), for: .touchUpInside)
        
        keyboardView.addSubview(testButton)
        
        NSLayoutConstraint.activate([
            testButton.centerXAnchor.constraint(equalTo: keyboardView.centerXAnchor),
            testButton.centerYAnchor.constraint(equalTo: keyboardView.centerYAnchor),
            testButton.widthAnchor.constraint(equalToConstant: 100),
            testButton.heightAnchor.constraint(equalToConstant: 44)
        ])
    }
    
    @objc private func testButtonPressed() {
        textDocumentProxy.insertText("Hello ")
    }
    
    // MARK: - Settings Management (App Groups Implementation)
    private func loadSettings() {
        // Load settings from shared UserDefaults (App Groups)
        settingsManager.loadSettings()
        updateKeyboardAppearance()
    }
    
    private func updateKeyboardAppearance() {
        guard keyboardView != nil else { return }
        
        let isDarkMode = textDocumentProxy.keyboardAppearance == .dark || settingsManager.currentTheme == "dark"
        
        // Update keyboard background
        keyboardView.backgroundColor = isDarkMode ? UIColor.systemGray6 : UIColor.systemGray5
        
        // Use LayoutManager to update button appearances if available
        if layoutManager != nil {
            layoutManager.updateButtonAppearances()
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
    
    @objc func shiftPressed() {
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
    
    @objc func numbersPressed() {
        // Toggle between alphabetic and numeric layouts
        if currentLayoutType == .alphabetic {
            switchToLayout(.numeric)
        } else {
            switchToLayout(.alphabetic)
        }
    }
    
    @objc private func globePressed() {
        advanceToNextInputMode()
    }
    
    private func updateShiftKey() {
        // Find and update shift key appearance using KeyButton
        updateShiftButton(in: keyboardView)
    }
    
    private func updateShiftButton(in view: UIView) {
        if let stackView = view as? UIStackView {
            for subview in stackView.arrangedSubviews {
                updateShiftButton(in: subview)
            }
        } else if let keyButton = view as? KeyButton, keyButton.keyType == .shift {
            keyButton.updateForShiftState(shiftState)
        } else {
            view.subviews.forEach { subview in
                updateShiftButton(in: subview)
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
        
        // Enhanced auto-capitalization logic
        if settingsManager.autoCapitalizationEnabled && shiftState == .normal {
            let textBefore = textDocumentProxy.documentContextBeforeInput ?? ""
            
            // Capitalize at start of document
            if textBefore.isEmpty {
                shiftState = .shift
                updateShiftKey()
                return
            }
            
            // Capitalize after double newline (new paragraph)
            if textBefore.hasSuffix("\n\n") {
                shiftState = .shift
                updateShiftKey()
                return
            }
            
            // Capitalize after sentence ending punctuation followed by space
            let sentenceEnders = [". ", "! ", "? ", ".\n", "!\n", "?\n"]
            for ender in sentenceEnders {
                if textBefore.hasSuffix(ender) {
                    shiftState = .shift
                    updateShiftKey()
                    return
                }
            }
        }
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        
        coordinator.animate(alongsideTransition: { _ in
            // Handle orientation change - determine orientation from size
            let newOrientation: UIInterfaceOrientation = size.width > size.height ? .landscapeLeft : .portrait
            
            // Only handle orientation change if layoutManager exists
            if self.layoutManager != nil {
                self.layoutManager.handleOrientationChange(to: newOrientation)
            }
        })
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
