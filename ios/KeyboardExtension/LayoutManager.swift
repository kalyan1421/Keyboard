//
//  LayoutManager.swift
//  KeyboardExtension
//
//  Manages dynamic keyboard layout creation and orientation handling
//

import UIKit

class LayoutManager {
    
    // MARK: - Properties
    private weak var keyboardViewController: KeyboardViewController?
    private var currentOrientation: UIInterfaceOrientation = .portrait
    private let settingsManager = SettingsManager.shared
    
    // Layout configurations
    struct LayoutConfig {
        let keySpacing: CGFloat
        let rowSpacing: CGFloat
        let edgeInsets: UIEdgeInsets
        let keyHeight: CGFloat
        let keyboardHeight: CGFloat
    }
    
    // Keyboard layouts for different orientations
    private let portraitKeyRows = [
        ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
        ["a", "s", "d", "f", "g", "h", "j", "k", "l"],
        ["shift", "z", "x", "c", "v", "b", "n", "m", "delete"],
        ["123", "globe", "space", "return"]
    ]
    
    private let landscapeKeyRows = [
        ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
        ["a", "s", "d", "f", "g", "h", "j", "k", "l"],
        ["shift", "z", "x", "c", "v", "b", "n", "m", "delete"],
        ["123", "globe", "space", "return"]
    ]
    
    // MARK: - Initialization
    init(keyboardViewController: KeyboardViewController) {
        self.keyboardViewController = keyboardViewController
        self.currentOrientation = getCurrentOrientation()
    }
    
    // MARK: - Layout Creation
    func createKeyboardLayout(in containerView: UIView) -> UIView {
        let config = getLayoutConfig()
        let keyRows = getKeyRows()
        
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
            let rowStackView = createRowStackView(for: row, rowIndex: rowIndex, config: config)
            mainStackView.addArrangedSubview(rowStackView)
        }
        
        return mainStackView
    }
    
    func createMainStackView(config: LayoutConfig) -> UIStackView {
        let stackView = UIStackView()
        stackView.axis = .vertical
        stackView.distribution = .fillEqually
        stackView.spacing = config.rowSpacing
        stackView.translatesAutoresizingMaskIntoConstraints = false
        return stackView
    }
    
    private func createRowStackView(for keys: [String], rowIndex: Int, config: LayoutConfig) -> UIStackView {
        // Create container for staggered row
        let containerStackView = UIStackView()
        containerStackView.axis = .horizontal
        containerStackView.spacing = 0
        containerStackView.translatesAutoresizingMaskIntoConstraints = false
        
        // Add leading spacer for row stagger (QWERTY offset)
        let staggerOffset = getRowStaggerOffset(rowIndex: rowIndex, config: config)
        if staggerOffset > 0 {
            let leadingSpacer = UIView()
            leadingSpacer.translatesAutoresizingMaskIntoConstraints = false
            leadingSpacer.widthAnchor.constraint(equalToConstant: staggerOffset).isActive = true
            containerStackView.addArrangedSubview(leadingSpacer)
        }
        
        let rowStackView = UIStackView()
        rowStackView.axis = .horizontal
        rowStackView.spacing = config.keySpacing
        rowStackView.translatesAutoresizingMaskIntoConstraints = false
        
        // Adjust distribution based on row type
        if rowIndex == 3 { // Bottom row with space bar
            rowStackView.distribution = .fill
        } else if rowIndex == 2 { // Row with shift and delete
            rowStackView.distribution = .fill
        } else {
            rowStackView.distribution = .fillEqually
        }
        
        // Create buttons for this row
        for (keyIndex, key) in keys.enumerated() {
            let button = createKeyButton(for: key, rowIndex: rowIndex, keyIndex: keyIndex, config: config)
            rowStackView.addArrangedSubview(button)
            
            // Set specific widths for special keys
            setButtonConstraints(button: button, key: key, rowIndex: rowIndex, config: config)
        }
        
        // Add row to container (if staggering is used)
        if staggerOffset > 0 {
            containerStackView.addArrangedSubview(rowStackView)
            
            // Add trailing spacer to balance
            let trailingSpacer = UIView()
            trailingSpacer.translatesAutoresizingMaskIntoConstraints = false
            trailingSpacer.widthAnchor.constraint(equalToConstant: staggerOffset).isActive = true
            containerStackView.addArrangedSubview(trailingSpacer)
            
            return containerStackView
        } else {
            return rowStackView
        }
    }
    
    private func createKeyButton(for key: String, rowIndex: Int, keyIndex: Int, config: LayoutConfig) -> KeyButton {
        let keyType = getKeyType(for: key)
        let button = KeyButton(key, type: keyType)
        
        // Set minimum height
        button.heightAnchor.constraint(equalToConstant: config.keyHeight).isActive = true
        
        // Update appearance for current theme
        button.updateAppearance()
        
        return button
    }
    
    private func setButtonConstraints(button: KeyButton, key: String, rowIndex: Int, config: LayoutConfig) {
        switch key {
        case "space":
            // Space bar should be wider
            let spaceWidth = getSpaceBarWidth(config: config)
            button.widthAnchor.constraint(equalToConstant: spaceWidth).isActive = true
            
        case "shift", "delete":
            // Shift and delete should be slightly wider
            if isLandscape() {
                let specialKeyWidth = getSpecialKeyWidth(config: config)
                button.widthAnchor.constraint(equalToConstant: specialKeyWidth).isActive = true
            }
            
        case "123", "globe", "return":
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
    
    // MARK: - Layout Configuration
    func getLayoutConfig() -> LayoutConfig {
        let orientation = getCurrentOrientation()
        
        if isLandscape() {
            return LayoutConfig(
                keySpacing: 4,
                rowSpacing: 6,
                edgeInsets: UIEdgeInsets(top: 6, left: 8, bottom: 6, right: 8),
                keyHeight: 38,
                keyboardHeight: 180
            )
        } else {
            return LayoutConfig(
                keySpacing: 4,
                rowSpacing: 8,
                edgeInsets: UIEdgeInsets(top: 8, left: 4, bottom: 8, right: 4),
                keyHeight: 42,
                keyboardHeight: 216
            )
        }
    }
    
    func getKeyRows() -> [[String]] {
        return isLandscape() ? landscapeKeyRows : portraitKeyRows
    }
    
    // MARK: - Key Type Determination
    private func getKeyType(for key: String) -> KeyButton.KeyType {
        switch key {
        case "shift":
            return .shift
        case "delete":
            return .delete
        case "space":
            return .space
        case "return":
            return .returnKey
        case "123":
            return .number
        case "globe":
            return .globe
        default:
            return .character
        }
    }
    
    // MARK: - Width Calculations
    func getSpaceBarWidth(config: LayoutConfig) -> CGFloat {
        let screenWidth = UIScreen.main.bounds.width
        let totalHorizontalPadding = config.edgeInsets.left + config.edgeInsets.right
        let availableWidth = screenWidth - totalHorizontalPadding
        
        if isLandscape() {
            return availableWidth * 0.4 // 40% of available width in landscape
        } else {
            return availableWidth * 0.5 // 50% of available width in portrait
        }
    }
    
    func getSpecialKeyWidth(config: LayoutConfig) -> CGFloat {
        let screenWidth = UIScreen.main.bounds.width
        let totalHorizontalPadding = config.edgeInsets.left + config.edgeInsets.right
        let availableWidth = screenWidth - totalHorizontalPadding
        
        return availableWidth * 0.12 // 12% for special keys
    }
    
    func getBottomKeyWidth(config: LayoutConfig) -> CGFloat {
        let screenWidth = UIScreen.main.bounds.width
        let totalHorizontalPadding = config.edgeInsets.left + config.edgeInsets.right
        let availableWidth = screenWidth - totalHorizontalPadding
        
        return availableWidth * 0.15 // 15% for bottom row keys
    }
    
    private func getRowStaggerOffset(rowIndex: Int, config: LayoutConfig) -> CGFloat {
        // QWERTY keyboard row stagger offsets for more natural feel
        switch rowIndex {
        case 0: // Q row
            return 0
        case 1: // A row - slightly to the right
            return 18
        case 2: // Z row (with shift) - aligns with Q
            return 0
        case 3: // Bottom row
            return 0
        default:
            return 0
        }
    }
    
    // MARK: - Orientation Handling
    func handleOrientationChange(to newOrientation: UIInterfaceOrientation) {
        guard newOrientation != currentOrientation else { return }
        
        currentOrientation = newOrientation
        
        // Recreate layout for new orientation - safely
        guard let controller = keyboardViewController,
              let containerView = controller.view else {
            print("Cannot handle orientation change: controller or view is nil")
            return
        }
        
        _ = createKeyboardLayout(in: containerView)
        
        // Update keyboard height
        updateKeyboardHeight()
    }
    
    private func updateKeyboardHeight() {
        let config = getLayoutConfig()
        keyboardViewController?.updateKeyboardHeight(config.keyboardHeight)
    }
    
    private func getCurrentOrientation() -> UIInterfaceOrientation {
        // In keyboard extensions, we need to determine orientation differently
        let screenSize = UIScreen.main.bounds.size
        if screenSize.width > screenSize.height {
            return .landscapeLeft
        } else {
            return .portrait
        }
    }
    
    func isLandscape() -> Bool {
        return currentOrientation.isLandscape
    }
    
    // MARK: - Dynamic Layout Updates
    func updateLayoutForSettings() {
        // Recreate layout when settings change (theme, etc.)
        if let containerView = keyboardViewController?.view {
            _ = createKeyboardLayout(in: containerView)
        }
    }
    
    func updateButtonAppearances() {
        // Update all button appearances
        keyboardViewController?.view.subviews.forEach { view in
            updateButtonAppearances(in: view)
        }
    }
    
    private func updateButtonAppearances(in view: UIView) {
        if let keyButton = view as? KeyButton {
            keyButton.updateAppearance()
        } else if let stackView = view as? UIStackView {
            stackView.arrangedSubviews.forEach { subview in
                updateButtonAppearances(in: subview)
            }
        } else {
            view.subviews.forEach { subview in
                updateButtonAppearances(in: subview)
            }
        }
    }
    
    // MARK: - Accessibility Support
    func configureAccessibility() {
        // Configure accessibility for the entire keyboard layout
        keyboardViewController?.view.isAccessibilityElement = false
        keyboardViewController?.view.accessibilityLabel = "AI Keyboard"
        keyboardViewController?.view.accessibilityHint = "Custom keyboard with AI features"
    }
    
    // MARK: - Animation Support
    func animateLayoutTransition(duration: TimeInterval = 0.3) {
        UIView.animate(withDuration: duration, 
                      delay: 0,
                      usingSpringWithDamping: 0.8,
                      initialSpringVelocity: 0.5,
                      options: [.curveEaseInOut, .allowUserInteraction]) {
            self.keyboardViewController?.view.layoutIfNeeded()
        }
    }
}

// MARK: - KeyboardViewController Extension
extension KeyboardViewController {
    func updateKeyboardHeight(_ height: CGFloat) {
        // Update height constraint
        if let heightConstraint = view.constraints.first(where: { $0.firstAttribute == .height }) {
            heightConstraint.constant = height
        } else {
            view.heightAnchor.constraint(equalToConstant: height).isActive = true
        }
        
        // Animate the change
        UIView.animate(withDuration: 0.3) {
            self.view.layoutIfNeeded()
        }
    }
}

// MARK: - Layout Constants
extension LayoutManager {
    struct Constants {
        static let minKeyHeight: CGFloat = 32
        static let maxKeyHeight: CGFloat = 50
        static let minKeySpacing: CGFloat = 2
        static let maxKeySpacing: CGFloat = 8
        
        struct Portrait {
            static let keyboardHeight: CGFloat = 216
            static let keyHeight: CGFloat = 42
            static let rowSpacing: CGFloat = 8
            static let keySpacing: CGFloat = 4
        }
        
        struct Landscape {
            static let keyboardHeight: CGFloat = 180
            static let keyHeight: CGFloat = 38
            static let rowSpacing: CGFloat = 6
            static let keySpacing: CGFloat = 4
        }
    }
}
