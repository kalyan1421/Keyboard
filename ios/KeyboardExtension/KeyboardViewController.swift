//
//  KeyboardViewController.swift
//  KeyboardExtension
//
//  Enhanced iOS Keyboard Extension following Flutter AI Keyboard Development Roadmap
//  Implements programmatic UI, App Groups data sharing, and proper text insertion
//

import UIKit

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
    
    private var isShifted = false
    private var isCapsLock = false
    
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
        
        let character = isShifted || isCapsLock ? title.uppercased() : title.lowercased()
        textDocumentProxy.insertText(character)
        
        // Handle shift state
        if isShifted && !isCapsLock {
            isShifted = false
            updateShiftKey()
        }
        
        // Add haptic feedback if enabled
        if settingsManager.vibrationEnabled {
            let impactFeedback = UIImpactFeedbackGenerator(style: .light)
            impactFeedback.impactOccurred()
        }
    }
    
    @objc private func shiftPressed() {
        if isShifted {
            // Double tap for caps lock
            isCapsLock = !isCapsLock
            isShifted = isCapsLock
        } else {
            isShifted = true
        }
        updateShiftKey()
    }
    
    @objc private func deletePressed() {
        textDocumentProxy.deleteBackward()
        
        if settingsManager.vibrationEnabled {
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()
        }
    }
    
    @objc private func spacePressed() {
        textDocumentProxy.insertText(" ")
        
        // Auto-capitalize after sentence ending
        if let textBefore = textDocumentProxy.documentContextBeforeInput,
           textBefore.hasSuffix(". ") || textBefore.hasSuffix("! ") || textBefore.hasSuffix("? ") {
            isShifted = true
            updateShiftKey()
        }
    }
    
    @objc private func returnPressed() {
        textDocumentProxy.insertText("\n")
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
            button.backgroundColor = (isShifted || isCapsLock) ? UIColor.systemBlue : UIColor.systemGray4
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
}
