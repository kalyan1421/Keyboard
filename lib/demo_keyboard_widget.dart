// demo_keyboard_widget.dart
// Interactive Demo Keyboard Widget for Testing Advanced Feedback
// Allows users to test feedback settings directly within the Flutter app

import 'package:flutter/material.dart';
import 'keyboard_feedback_system.dart';

class DemoKeyboardWidget extends StatefulWidget {
  const DemoKeyboardWidget({super.key});

  @override
  State<DemoKeyboardWidget> createState() => _DemoKeyboardWidgetState();
}

class _DemoKeyboardWidgetState extends State<DemoKeyboardWidget>
    with TickerProviderStateMixin {
  
  final List<List<String>> keyRows = [
    ['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'],
    ['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'],
    ['shift', 'z', 'x', 'c', 'v', 'b', 'n', 'm', 'delete'],
    ['123', 'space', 'return']
  ];

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 8,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Colors.grey[100]!,
              Colors.grey[200]!,
            ],
          ),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header
            const Row(
              children: [
                Icon(Icons.keyboard, color: Colors.blue, size: 24),
                SizedBox(width: 8),
                Text(
                  'Interactive Demo Keyboard',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.blue,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              'Test your feedback settings! Tap keys to experience haptic, sound, and visual effects.',
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[600],
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            
            // Demo keyboard
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey[300],
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey[400]!),
              ),
              child: Column(
                children: keyRows.asMap().entries.map((entry) {
                  int rowIndex = entry.key;
                  List<String> row = entry.value;
                  
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: _buildKeyRow(row, rowIndex),
                  );
                }).toList(),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Effects overlay
            Container(
              height: 100,
              width: double.infinity,
              decoration: BoxDecoration(
                color: Colors.black.withOpacity(0.05),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey[300]!),
              ),
              child: Stack(
                children: [
                  // Particles and ripples will be rendered here
                  Positioned.fill(
                    child: CustomPaint(
                      painter: FeedbackEffectsPainter(
                        particles: KeyboardFeedbackSystem.particles,
                        ripples: KeyboardFeedbackSystem.ripples,
                      ),
                    ),
                  ),
                  const Center(
                    child: Text(
                      'Visual Effects Area\n(Particles & Ripples)',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: Colors.grey,
                        fontSize: 12,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildKeyRow(List<String> keys, int rowIndex) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: keys.map((key) => _buildDemoKey(key, rowIndex)).toList(),
    );
  }

  Widget _buildDemoKey(String key, int rowIndex) {
    final isSpecialKey = ['shift', 'delete', '123', 'space', 'return'].contains(key);
    final isSpaceBar = key == 'space';
    final isEnterKey = key == 'return';
    
    double keyWidth = isSpaceBar ? 120 : (isSpecialKey ? 50 : 35);
    double keyHeight = 40;
    
    String displayText = key;
    IconData? icon;
    
    switch (key) {
      case 'shift':
        icon = Icons.keyboard_arrow_up;
        displayText = '';
        break;
      case 'delete':
        icon = Icons.backspace_outlined;
        displayText = '';
        break;
      case 'space':
        displayText = '';
        break;
      case 'return':
        icon = Icons.keyboard_return;
        displayText = '';
        break;
      case '123':
        displayText = '123';
        break;
    }

    return AnimatedKeyWidget(
      keyId: 'demo_$key',
      isSpecialKey: isSpecialKey,
      isSpaceBar: isSpaceBar,
      isEnterKey: isEnterKey,
      onPressed: () => _handleDemoKeyPress(key),
      child: Container(
        width: keyWidth,
        height: keyHeight,
        margin: const EdgeInsets.all(2),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: isSpecialKey 
              ? [Colors.grey[400]!, Colors.grey[500]!]
              : [Colors.white, Colors.grey[100]!],
          ),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: Colors.grey[400]!),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.1),
              offset: const Offset(0, 1),
              blurRadius: 2,
            ),
          ],
        ),
        child: Center(
          child: icon != null
            ? Icon(
                icon,
                size: isSpaceBar ? 16 : 18,
                color: Colors.grey[700],
              )
            : Text(
                displayText,
                style: TextStyle(
                  fontSize: isSpaceBar ? 12 : 16,
                  fontWeight: FontWeight.w500,
                  color: Colors.grey[700],
                ),
              ),
        ),
      ),
    );
  }

  void _handleDemoKeyPress(String key) {
    // Show feedback in snackbar
    String message;
    switch (key) {
      case 'space':
        message = '🚀 Space bar with bounce animation!';
        break;
      case 'return':
        message = '⚡ Enter key with enhanced feedback!';
        break;
      case 'shift':
      case 'delete':
        message = '✨ Special key with particle effects!';
        break;
      default:
        message = '⌨️ Regular key: "$key"';
        break;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(milliseconds: 1500),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(10),
        ),
      ),
    );
  }

  @override
  void dispose() {
    // Animation controllers are managed by KeyboardFeedbackSystem
    super.dispose();
  }
}

/// Enhanced demo keyboard with live effects rendering
class LiveEffectsDemoKeyboard extends StatefulWidget {
  const LiveEffectsDemoKeyboard({super.key});

  @override
  State<LiveEffectsDemoKeyboard> createState() => _LiveEffectsDemoKeyboardState();
}

class _LiveEffectsDemoKeyboardState extends State<LiveEffectsDemoKeyboard>
    with TickerProviderStateMixin {
  
  late AnimationController _effectsController;

  @override
  void initState() {
    super.initState();
    _effectsController = AnimationController(
      duration: const Duration(milliseconds: 16), // 60 FPS
      vsync: this,
    );
    
    // Start animation loop for live effects
    _effectsController.repeat();
    _effectsController.addListener(() {
      if (mounted) {
        setState(() {
          // Trigger rebuild to show updated particles and ripples
        });
      }
    });
  }

  @override
  void dispose() {
    _effectsController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        const DemoKeyboardWidget(),
        
        // Live effects overlay
        Positioned.fill(
          child: IgnorePointer(
            child: CustomPaint(
              painter: FeedbackEffectsPainter(
                particles: KeyboardFeedbackSystem.particles,
                ripples: KeyboardFeedbackSystem.ripples,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

/// Settings test panel for quick feedback testing
class FeedbackTestPanel extends StatelessWidget {
  const FeedbackTestPanel({super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.tune, color: Colors.orange),
                SizedBox(width: 8),
                Text(
                  'Quick Feedback Tests',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.orange,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () => _testHapticFeedback(),
                    icon: const Icon(Icons.vibration),
                    label: const Text('Test Haptic'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () => _testSoundFeedback(),
                    icon: const Icon(Icons.volume_up),
                    label: const Text('Test Sound'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.green,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
            
            const SizedBox(height: 8),
            
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () => _testVisualEffects(context),
                    icon: const Icon(Icons.auto_awesome),
                    label: const Text('Test Visual'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.purple,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () => _testAllEffects(context),
                    icon: const Icon(Icons.rocket_launch),
                    label: const Text('Test All'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.orange,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _testHapticFeedback() {
    KeyboardFeedbackSystem.onKeyPress(
      keyId: 'test_haptic',
      touchPoint: const Offset(100, 100),
      isSpecialKey: true,
    );
  }

  void _testSoundFeedback() {
    KeyboardFeedbackSystem.onKeyPress(
      keyId: 'test_sound',
      touchPoint: const Offset(150, 100),
      isSpaceBar: true,
    );
  }

  void _testVisualEffects(BuildContext context) {
    final center = Offset(
      MediaQuery.of(context).size.width / 2,
      200,
    );
    
    KeyboardFeedbackSystem.onKeyPress(
      keyId: 'test_visual',
      touchPoint: center,
      isSpecialKey: true,
    );
  }

  void _testAllEffects(BuildContext context) {
    final center = Offset(
      MediaQuery.of(context).size.width / 2,
      200,
    );
    
    // Trigger multiple effects
    KeyboardFeedbackSystem.onKeyPress(
      keyId: 'test_all_1',
      touchPoint: center + const Offset(-50, 0),
      isSpecialKey: true,
    );
    
    Future.delayed(const Duration(milliseconds: 100), () {
      KeyboardFeedbackSystem.onKeyPress(
        keyId: 'test_all_2',
        touchPoint: center,
        isEnterKey: true,
      );
    });
    
    Future.delayed(const Duration(milliseconds: 200), () {
      KeyboardFeedbackSystem.onKeyPress(
        keyId: 'test_all_3',
        touchPoint: center + const Offset(50, 0),
        isSpaceBar: true,
      );
    });
  }
}
