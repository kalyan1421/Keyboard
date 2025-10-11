//
//  NumberLayoutManager.swift
//  KeyboardExtension
//
//  Extension to LayoutManager for numeric and symbols keyboard layouts
//

import UIKit

// MARK: - Keyboard Layout Types
extension LayoutManager {
    enum KeyboardLayoutType {
        case alphabetic
        case numeric
        case symbols
    }
}

// MARK: - Number and Symbol Layout Definitions
extension LayoutManager {
    
    // Numeric layout (numbers and common symbols)
    var numericKeyRows: [[String]] {
        return [
            ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"],
            ["-", "/", ":", ";", "(", ")", "$", "&", "@", "\""],
            ["#+=", ".", ",", "?", "!", "'", "delete"],
            ["ABC", "globe", "space", "return"]
        ]
    }
    
    // Symbols layout (additional symbols)
    var symbolsKeyRows: [[String]] {
        return [
            ["[", "]", "{", "}", "#", "%", "^", "*", "+", "="],
            ["_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•"],
            ["123", ".", ",", "?", "!", "'", "delete"],
            ["ABC", "globe", "space", "return"]
        ]
    }
    
    // MARK: - Layout Creation Methods
    
    func createNumericLayout(in containerView: UIView) -> UIView {
        return createLayout(in: containerView, keyRows: numericKeyRows, layoutType: .numeric)
    }
    
    func createSymbolsLayout(in containerView: UIView) -> UIView {
        return createLayout(in: containerView, keyRows: symbolsKeyRows, layoutType: .symbols)
    }
    
    private func createLayout(in containerView: UIView, keyRows: [[String]], layoutType: KeyboardLayoutType) -> UIView {
        let config = getLayoutConfig()
        
        // Clear existing layout
        containerView.subviews.forEach { $0.removeFromSuperview() }
        
        // Create main stack view
        let mainStackView = createMainStackView(config: config)
        containerView.addSubview(mainStackView)
        
        // Setup constraints
        NSLayoutConstraint.activate([
            mainStackView.topAnchor.constraint(equalTo: containerView.topAnchor, constant: config.edgeInsets.top),
            mainStackView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: config.edgeInsets.left),
            mainStackView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -config.edgeInsets.right),
            mainStackView.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -config.edgeInsets.bottom)
        ])
        
        // Create rows
        for (rowIndex, row) in keyRows.enumerated() {
            let rowStackView = createNumberRowStackView(for: row, rowIndex: rowIndex, config: config, layoutType: layoutType)
            mainStackView.addArrangedSubview(rowStackView)
        }
        
        return mainStackView
    }
    
    private func createNumberRowStackView(for keys: [String], rowIndex: Int, config: LayoutConfig, layoutType: KeyboardLayoutType) -> UIStackView {
        let rowStackView = UIStackView()
        rowStackView.axis = .horizontal
        rowStackView.spacing = config.keySpacing
        rowStackView.translatesAutoresizingMaskIntoConstraints = false
        
        // Adjust distribution based on row type
        if rowIndex == 3 { // Bottom row with space bar
            rowStackView.distribution = .fill
        } else if rowIndex == 2 { // Row with special keys
            rowStackView.distribution = .fill
        } else {
            rowStackView.distribution = .fillEqually
        }
        
        // Create buttons for this row
        for (keyIndex, key) in keys.enumerated() {
            let button = createNumberKeyButton(for: key, rowIndex: rowIndex, keyIndex: keyIndex, config: config, layoutType: layoutType)
            rowStackView.addArrangedSubview(button)
            
            // Set specific widths for special keys
            setNumberButtonConstraints(button: button, key: key, rowIndex: rowIndex, config: config)
        }
        
        return rowStackView
    }
    
    private func createNumberKeyButton(for key: String, rowIndex: Int, keyIndex: Int, config: LayoutConfig, layoutType: KeyboardLayoutType) -> KeyButton {
        let keyType = getNumberKeyType(for: key, layoutType: layoutType)
        let button = KeyButton(key, type: keyType)
        
        // Set minimum height
        button.heightAnchor.constraint(equalToConstant: config.keyHeight).isActive = true
        
        // Update appearance for current theme
        button.updateAppearance()
        
        return button
    }
    
    private func setNumberButtonConstraints(button: KeyButton, key: String, rowIndex: Int, config: LayoutConfig) {
        switch key {
        case "space":
            // Space bar should be wider
            let spaceWidth = getSpaceBarWidth(config: config)
            button.widthAnchor.constraint(equalToConstant: spaceWidth).isActive = true
            
        case "delete":
            // Delete should be slightly wider in number layout
            if isLandscape() {
                let specialKeyWidth = getSpecialKeyWidth(config: config)
                button.widthAnchor.constraint(equalToConstant: specialKeyWidth).isActive = true
            }
            
        case "#+=", "123", "ABC":
            // Layout switcher keys
            if isLandscape() {
                let bottomKeyWidth = getBottomKeyWidth(config: config) * 1.2
                button.widthAnchor.constraint(equalToConstant: bottomKeyWidth).isActive = true
            }
            
        case "globe", "return":
            // Bottom row special keys
            if isLandscape() {
                let bottomKeyWidth = getBottomKeyWidth(config: config)
                button.widthAnchor.constraint(equalToConstant: bottomKeyWidth).isActive = true
            }
            
        default:
            // Regular keys use equal distribution
            break
        }
    }
    
    // MARK: - Key Type Determination for Numbers/Symbols
    
    private func getNumberKeyType(for key: String, layoutType: KeyboardLayoutType) -> KeyButton.KeyType {
        switch key {
        case "delete":
            return .delete
        case "space":
            return .space
        case "return":
            return .returnKey
        case "ABC", "123", "#+=":
            return .special(key)
        case "globe":
            return .globe
        default:
            return .character
        }
    }
}

// MARK: - KeyboardViewController Extension for Layout Switching

extension KeyboardViewController {
    
    // Track current layout
    private static var currentLayoutType: LayoutManager.KeyboardLayoutType = .alphabetic
    
    var currentLayoutType: LayoutManager.KeyboardLayoutType {
        get {
            return KeyboardViewController.currentLayoutType
        }
        set {
            KeyboardViewController.currentLayoutType = newValue
        }
    }
    
    // Switch between layouts
    func switchToLayout(_ layoutType: LayoutManager.KeyboardLayoutType) {
        guard let layoutMgr = layoutManager, let keyboardContainer = keyboardView else {
            print("Cannot switch layout: layoutManager or keyboardView is nil")
            return
        }
        
        currentLayoutType = layoutType
        
        switch layoutType {
        case .alphabetic:
            _ = layoutMgr.createKeyboardLayout(in: keyboardContainer)
        case .numeric:
            _ = layoutMgr.createNumericLayout(in: keyboardContainer)
        case .symbols:
            _ = layoutMgr.createSymbolsLayout(in: keyboardContainer)
        }
        
        // Animate the transition
        UIView.transition(with: keyboardContainer, 
                         duration: 0.2, 
                         options: .transitionCrossDissolve,
                         animations: nil,
                         completion: nil)
    }
}

