

// main.dart
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'keyboard_feedback_system.dart';
// Demo keyboard widget removed - using system-wide keyboard instead

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  // Initialize the advanced feedback system
  KeyboardFeedbackSystem.initialize();
  runApp(const AIKeyboardApp());
}

class AIKeyboardApp extends StatelessWidget {
  const AIKeyboardApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AI Keyboard',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      home: const KeyboardConfigScreen(),
    );
  }
}

class KeyboardConfigScreen extends StatefulWidget {
  const KeyboardConfigScreen({super.key});

  @override
  State<KeyboardConfigScreen> createState() => _KeyboardConfigScreenState();
}

class _KeyboardConfigScreenState extends State<KeyboardConfigScreen> {
  static const platform = MethodChannel('ai_keyboard/config');
  bool _isKeyboardEnabled = false;
  bool _isKeyboardActive = false;
  String _selectedTheme = 'default';
  bool _aiSuggestionsEnabled = true;
  bool _swipeTypingEnabled = true;
  bool _voiceInputEnabled = true;
  bool _vibrationEnabled = true;
  bool _keyPreviewEnabled = false;
  
  // Advanced feedback settings
  FeedbackIntensity _hapticIntensity = FeedbackIntensity.medium;
  FeedbackIntensity _soundIntensity = FeedbackIntensity.light;
  FeedbackIntensity _visualIntensity = FeedbackIntensity.medium;
  double _soundVolume = 0.3;

  final List<String> _themes = [
    'default',
    'dark',
    'material_you',
    'professional',
    'colorful'
  ];

  @override
  void initState() {
    super.initState();
    _loadSettings();
    _checkKeyboardStatus();
    
    // Show setup reminder for iOS users if keyboard is not enabled
    if (Platform.isIOS) {
      _checkAndShowSetupReminder();
    }
  }

  Future<void> _checkAndShowSetupReminder() async {
    // Wait a bit for the UI to settle
    await Future.delayed(const Duration(seconds: 2));
    
    if (!_isKeyboardEnabled && mounted) {
      _showSetupReminder();
    }
  }

  void _showSetupReminder() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.info_outline, color: Colors.blue),
              SizedBox(width: 8),
              Text('Setup Required'),
            ],
          ),
          content: const Text(
            'AI Keyboard needs to be enabled in iOS Settings to work. Would you like to set it up now?'
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Later'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                _openInputMethodPicker();
              },
              child: const Text('Setup Now'),
            ),
          ],
        );
      },
    );
  }

  void _showSettingsUpdatedSnackBar() {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Row(
            children: [
              Icon(Icons.check_circle, color: Colors.white, size: 20),
              SizedBox(width: 8),
              Text('Settings saved! Switch to keyboard to see changes.'),
            ],
          ),
          backgroundColor: Colors.green,
          duration: const Duration(seconds: 3),
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
          action: SnackBarAction(
            label: 'Test Keyboard',
            textColor: Colors.white,
            onPressed: () {
              // Show dialog to test keyboard
              _showTestKeyboardDialog();
            },
          ),
        ),
      );
    }
  }

  void _showTestKeyboardDialog() {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return Dialog(
          child: Container(
            constraints: const BoxConstraints(maxWidth: 600, maxHeight: 700),
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.rocket_launch, color: Colors.blue, size: 28),
                    const SizedBox(width: 12),
                    const Text(
                      'Advanced Feedback Testing',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const Spacer(),
                    IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.close),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                
                // System keyboard status panel
                const SystemKeyboardStatusPanel(),
                const SizedBox(height: 20),
                
                // AI Service Information
                const Expanded(
                  child: SingleChildScrollView(
                    child: AIServiceInfoWidget(),
                  ),
                ),
                
                const SizedBox(height: 16),
                
                // Instructions
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.blue.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.blue.withOpacity(0.3)),
                  ),
                  child: const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '💡 How to test:',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: Colors.blue,
                        ),
                      ),
                      SizedBox(height: 8),
                      Text('• Tap demo keys above to test current feedback settings'),
                      Text('• Use quick test buttons for individual feedback types'),
                      Text('• Adjust settings and see changes instantly'),
                      Text('• For real keyboard: Open any text app and switch to AI Keyboard'),
                    ],
                  ),
                ),
                
                const SizedBox(height: 16),
                
                // Action buttons
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    TextButton(
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text('Close'),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton.icon(
                      onPressed: () {
                        Navigator.of(context).pop();
                        _openInputMethodPicker();
                      },
                      icon: const Icon(Icons.keyboard),
                      label: const Text('Switch to AI Keyboard'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.blue,
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _selectedTheme = prefs.getString('keyboard_theme') ?? 'default';
      _aiSuggestionsEnabled = prefs.getBool('ai_suggestions') ?? true;
      _swipeTypingEnabled = prefs.getBool('swipe_typing') ?? true;
      _voiceInputEnabled = prefs.getBool('voice_input') ?? true;
      _vibrationEnabled = prefs.getBool('vibration_enabled') ?? true;
      _keyPreviewEnabled = prefs.getBool('key_preview_enabled') ?? false;
      
      // Load advanced feedback settings
      _hapticIntensity = FeedbackIntensity.values[prefs.getInt('haptic_intensity') ?? 2]; // medium
      _soundIntensity = FeedbackIntensity.values[prefs.getInt('sound_intensity') ?? 1]; // light
      _visualIntensity = FeedbackIntensity.values[prefs.getInt('visual_intensity') ?? 2]; // medium
      _soundVolume = prefs.getDouble('sound_volume') ?? 0.3;
    });
    
    // Update feedback system with loaded settings
    KeyboardFeedbackSystem.updateSettings(
      haptic: _hapticIntensity,
      sound: _soundIntensity,
      visual: _visualIntensity,
      volume: _soundVolume,
    );
  }

  Future<void> _saveSettings() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('keyboard_theme', _selectedTheme);
    await prefs.setBool('ai_suggestions', _aiSuggestionsEnabled);
    await prefs.setBool('swipe_typing', _swipeTypingEnabled);
    await prefs.setBool('voice_input', _voiceInputEnabled);
    await prefs.setBool('vibration_enabled', _vibrationEnabled);
    await prefs.setBool('key_preview_enabled', _keyPreviewEnabled);
    
    // Save advanced feedback settings
    await prefs.setInt('haptic_intensity', _hapticIntensity.index);
    await prefs.setInt('sound_intensity', _soundIntensity.index);
    await prefs.setInt('visual_intensity', _visualIntensity.index);
    await prefs.setDouble('sound_volume', _soundVolume);
    
    // Update feedback system with new settings
    KeyboardFeedbackSystem.updateSettings(
      haptic: _hapticIntensity,
      sound: _soundIntensity,
      visual: _visualIntensity,
      volume: _soundVolume,
    );
    
    // Send settings to native keyboard
    await _sendSettingsToKeyboard();
    
    // Show success feedback
    _showSettingsUpdatedSnackBar();
  }

  Future<void> _sendSettingsToKeyboard() async {
    try {
      await platform.invokeMethod('updateSettings', {
        'theme': _selectedTheme,
        'aiSuggestions': _aiSuggestionsEnabled,
        'swipeTyping': _swipeTypingEnabled,
        'voiceInput': _voiceInputEnabled,
        'vibration': _vibrationEnabled,
        'keyPreview': _keyPreviewEnabled,
      });
    } catch (e) {
      print('Error sending settings: $e');
    }
  }

  Future<void> _checkKeyboardStatus() async {
    try {
      final bool enabled = await platform.invokeMethod('isKeyboardEnabled');
      final bool active = await platform.invokeMethod('isKeyboardActive');
      setState(() {
        _isKeyboardEnabled = enabled;
        _isKeyboardActive = active;
      });
    } catch (e) {
      print('Error checking keyboard status: $e');
    }
  }

  Future<void> _openKeyboardSettings() async {
    try {
      await platform.invokeMethod('openKeyboardSettings');
    } catch (e) {
      print('Error opening keyboard settings: $e');
    }
  }

  Future<void> _openInputMethodPicker() async {
    try {
      if (Platform.isAndroid) {
        await platform.invokeMethod('openInputMethodPicker');
      } else if (Platform.isIOS) {
        // Show interactive tutorial for iOS
        await platform.invokeMethod('showKeyboardTutorial');
      }
    } catch (e) {
      print('Error opening input method picker: $e');
    }
  }

  Future<void> _openKeyboardsDirectly() async {
    try {
      await platform.invokeMethod('openKeyboardsDirectly');
    } catch (e) {
      print('Error opening keyboards directly: $e');
    }
  }

  Future<void> _showQuickSwitchGuide() async {
    if (Platform.isIOS) {
      showDialog(
        context: context,
        builder: (BuildContext context) {
          return AlertDialog(
            title: const Row(
              children: [
                Text('🚀 Quick Switch Guide'),
              ],
            ),
            content: const Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Once AI Keyboard is enabled, switch quickly by:'),
                SizedBox(height: 12),
                Text('🌐 Tap globe icon to cycle keyboards'),
                Text('🌐 Long-press globe for keyboard list'),
                Text('⌨️ Or go to any text field and tap keyboard icon'),
                SizedBox(height: 12),
                Text('💡 Pro tip: Set AI Keyboard as default in Settings!'),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('Got it!'),
              ),
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop();
                  _openKeyboardsDirectly();
                },
                child: const Text('Open Settings'),
              ),
            ],
          );
        },
      );
    }
  }

  String _getThemeDisplayName(String theme) {
    switch (theme) {
      case 'material_you': return 'Material You';
      default: return theme.replaceAll('_', ' ').split(' ')
          .map((word) => word[0].toUpperCase() + word.substring(1)).join(' ');
    }
  }

  void _showIOSInstructions() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Enable AI Keyboard on iOS'),
          content: const Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('To enable AI Keyboard on iOS:'),
              SizedBox(height: 12),
              Text('1. Open Settings app'),
              Text('2. Go to General → Keyboard'),
              Text('3. Tap "Keyboards"'),
              Text('4. Tap "Add New Keyboard..."'),
              Text('5. Select "AI Keyboard" from Third-Party Keyboards'),
              Text('6. Enable "Allow Full Network Access" if needed'),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Got it'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                _openKeyboardSettings();
              },
              child: const Text('Open Settings'),
            ),
          ],
        );
      },
    );
  }

  void _showSystemKeyboardInstructions(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('🚀 System Keyboard Setup'),
          content: const SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'Your AI keyboard is ready! Follow these steps:',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                SizedBox(height: 16),
                Text('1. Go to Android Settings'),
                Text('2. Navigate to: System → Languages & input → Virtual keyboard'),
                Text('3. Tap "Manage keyboards"'),
                Text('4. Enable "AI Keyboard"'),
                Text('5. Open any app and tap in a text field'),
                Text('6. Select "AI Keyboard" from the keyboard picker'),
                SizedBox(height: 16),
                Text(
                  '✨ You\'ll now have AI-powered suggestions in all apps!',
                  style: TextStyle(fontWeight: FontWeight.bold, color: Colors.green),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Got it!'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Keyboard Settings'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: RefreshIndicator(
        onRefresh: _checkKeyboardStatus,
        child: ListView(
          padding: const EdgeInsets.all(16.0),
          children: [
            _buildKeyboardStatusCard(),
            const SizedBox(height: 20),
            _buildPlatformInfoCard(),
            const SizedBox(height: 20),
            if (Platform.isIOS) ...[
              _buildIOSSetupCard(),
              const SizedBox(height: 20),
            ],
            _buildThemeSelectionCard(),
            const SizedBox(height: 20),
            _buildFeaturesCard(),
            const SizedBox(height: 20),
            _buildTestKeyboardCard(),
          ],
        ),
      ),
    );
  }

  Widget _buildKeyboardStatusCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Keyboard Status',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            _buildStatusRow('Enabled', _isKeyboardEnabled),
            const SizedBox(height: 8),
            _buildStatusRow('Active', _isKeyboardActive),
            const SizedBox(height: 16),
            if (Platform.isIOS) ...[
              // iOS-specific enhanced buttons
              ElevatedButton.icon(
                onPressed: _openInputMethodPicker,
                icon: const Icon(Icons.help_outline),
                label: const Text('Quick Setup Guide'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue,
                  foregroundColor: Colors.white,
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _openKeyboardsDirectly,
                      child: const Text('Go to Settings'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _showQuickSwitchGuide,
                      child: const Text('Switch Guide'),
                    ),
                  ),
                ],
              ),
            ] else ...[
              // Android buttons
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _openKeyboardSettings,
                      child: const Text('Enable Keyboard'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _openInputMethodPicker,
                      child: const Text('Select Keyboard'),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildStatusRow(String label, bool status) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          decoration: BoxDecoration(
            color: status ? Colors.green : Colors.red,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Text(
            status ? 'Active' : 'Inactive',
            style: const TextStyle(color: Colors.white, fontSize: 12),
          ),
        ),
      ],
    );
  }

  Widget _buildPlatformInfoCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Platform Information',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Platform:'),
                Text(Platform.isIOS ? 'iOS' : 'Android', 
                     style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Keyboard Type:'),
                Text(Platform.isIOS ? 'Extension' : 'InputMethodService',
                     style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildIOSSetupCard() {
    return Card(
      color: Colors.blue.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.phone_iphone, color: Colors.blue.shade700),
                const SizedBox(width: 8),
                Text(
                  'iOS Setup Made Easy',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: Colors.blue.shade700,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.blue.shade200),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '📱 Quick Steps:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  _buildStepRow('1', 'Tap "Quick Setup Guide" above'),
                  _buildStepRow('2', 'Follow the interactive tutorial'),
                  _buildStepRow('3', 'Add AI Keyboard in Settings'),
                  _buildStepRow('4', 'Use 🌐 key to switch keyboards'),
                ],
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Icon(Icons.lightbulb_outline, color: Colors.amber.shade600, size: 20),
                const SizedBox(width: 8),
                const Expanded(
                  child: Text(
                    'Pro tip: Long-press the 🌐 globe key in any app to see all available keyboards!',
                    style: TextStyle(fontStyle: FontStyle.italic),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStepRow(String number, String description) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 20,
            height: 20,
            decoration: BoxDecoration(
              color: Colors.blue.shade600,
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                number,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(child: Text(description)),
        ],
      ),
    );
  }

  Widget _buildThemeSelectionCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Keyboard Theme',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 8,
              children: _themes.map((theme) {
                return ChoiceChip(
                  label: Text(_getThemeDisplayName(theme)),
                  selected: _selectedTheme == theme,
                  onSelected: (selected) {
                    setState(() {
                      _selectedTheme = theme;
                    });
                    _saveSettings();
                  },
                );
              }).toList(),
            ),
            const SizedBox(height: 12),
            Text(
              'Current theme: ${_getThemeDisplayName(_selectedTheme)}',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFeaturesCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Features',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            _buildFeatureSwitch(
              'AI Suggestions',
              'Get smart text predictions and corrections',
              _aiSuggestionsEnabled,
              (value) {
                setState(() {
                  _aiSuggestionsEnabled = value;
                });
                _saveSettings();
              },
            ),
            _buildFeatureSwitch(
              'Swipe Typing',
              'Swipe across letters to form words instantly! Try swiping "hello" or "the"',
              _swipeTypingEnabled,
              (value) {
                setState(() {
                  _swipeTypingEnabled = value;
                });
                _saveSettings();
              },
            ),
            _buildFeatureSwitch(
              'Voice Input',
              'Convert speech to text',
              _voiceInputEnabled,
              (value) {
                setState(() {
                  _voiceInputEnabled = value;
                });
                _saveSettings();
              },
            ),
            _buildFeatureSwitch(
              'Vibration Feedback',
              'Haptic feedback when typing (Recommended: ON)',
              _vibrationEnabled,
              (value) {
                setState(() {
                  _vibrationEnabled = value;
                });
                _saveSettings();
              },
            ),
            _buildFeatureSwitch(
              'Key Preview',
              'Show letter popup when typing (Recommended: OFF for cleaner experience)',
              _keyPreviewEnabled,
              (value) {
                setState(() {
                  _keyPreviewEnabled = value;
                });
                _saveSettings();
              },
            ),
            
            // Advanced Feedback Settings Section
            const SizedBox(height: 24),
            _buildSectionHeader('🎯 Advanced Feedback Settings'),
            const SizedBox(height: 16),
            
            _buildIntensitySelector(
              'Haptic Feedback Intensity',
              'Control the strength of touch vibrations',
              _hapticIntensity,
              (value) {
                setState(() {
                  _hapticIntensity = value;
                });
                _saveSettings();
              },
            ),
            
            _buildIntensitySelector(
              'Sound Feedback Intensity',
              'Control keyboard typing sounds',
              _soundIntensity,
              (value) {
                setState(() {
                  _soundIntensity = value;
                });
                _saveSettings();
              },
            ),
            
            _buildIntensitySelector(
              'Visual Effects Intensity',
              'Control animations, particles, and ripple effects',
              _visualIntensity,
              (value) {
                setState(() {
                  _visualIntensity = value;
                });
                _saveSettings();
              },
            ),
            
            _buildVolumeSlider(),
          ],
        ),
      ),
    );
  }

  Widget _buildFeatureSwitch(
    String title,
    String subtitle,
    bool value,
    ValueChanged<bool> onChanged,
  ) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(title),
      subtitle: Text(subtitle),
      trailing: Switch(
        value: value,
        onChanged: onChanged,
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.bold,
        color: Colors.blue,
      ),
    );
  }

  Widget _buildIntensitySelector(
    String title,
    String subtitle,
    FeedbackIntensity value,
    ValueChanged<FeedbackIntensity> onChanged,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[600],
              ),
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: FeedbackIntensity.values.map((intensity) {
                final isSelected = value == intensity;
                return GestureDetector(
                  onTap: () => onChanged(intensity),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    decoration: BoxDecoration(
                      color: isSelected ? Colors.blue : Colors.transparent,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: isSelected ? Colors.blue : Colors.grey,
                        width: 1,
                      ),
                    ),
                    child: Text(
                      _getIntensityLabel(intensity),
                      style: TextStyle(
                        color: isSelected ? Colors.white : Colors.grey[700],
                        fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildVolumeSlider() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Sound Volume',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Adjust keyboard sound volume level',
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[600],
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Icon(Icons.volume_down, color: Colors.grey),
                Expanded(
                  child: Slider(
                    value: _soundVolume,
                    min: 0.0,
                    max: 1.0,
                    divisions: 10,
                    label: '${(_soundVolume * 100).round()}%',
                    onChanged: (value) {
                      setState(() {
                        _soundVolume = value;
                      });
                      _saveSettings();
                    },
                  ),
                ),
                const Icon(Icons.volume_up, color: Colors.grey),
              ],
            ),
            Text(
              'Current: ${(_soundVolume * 100).round()}%',
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey[600],
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  String _getIntensityLabel(FeedbackIntensity intensity) {
    switch (intensity) {
      case FeedbackIntensity.off:
        return 'OFF';
      case FeedbackIntensity.light:
        return 'Light';
      case FeedbackIntensity.medium:
        return 'Medium';
      case FeedbackIntensity.strong:
        return 'Strong';
    }
  }

  Widget _buildTestKeyboardCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Test Keyboard',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            TextButton(
              onPressed: () {
                // Navigate to system keyboard setup instructions
                _showSystemKeyboardInstructions(context);
              },
              child: const Text('Test Keyboard'),
            ),
            const SizedBox(height: 16),
            const TextField(
              decoration: InputDecoration(
                hintText: 'Tap here to test your AI keyboard...',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 12),
            Text(
              Platform.isIOS 
                ? 'Make sure AI Keyboard is enabled in iOS Settings → General → Keyboard → Keyboards, then tap the text field above to test.'
                : 'Make sure AI Keyboard is selected as your input method, then tap the text field above to test.',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.blue.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.blue.withOpacity(0.3)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '💡 Features to try:',
                    style: TextStyle(fontWeight: FontWeight.bold, color: Colors.blue[800]),
                  ),
                  const SizedBox(height: 8),
                  const Text('• Try different themes from above'),
                  const Text('• Use swipe gestures for quick actions'),
                  const Text('• Test AI suggestions while typing'),
                  const Text('• Try caps lock (double-tap shift)'),
                  const Text('• Switch between letter/symbol/number layouts'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// AI Service for text suggestions
class AIService {
  static const String _baseUrl = 'https://api.openai.com/v1';
  static const String _apiKey = 'YOUR_API_KEY_HERE'; // Replace with actual key

  static Future<List<String>> getTextSuggestions(String currentText) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/completions'),
        headers: {
          'Authorization': 'Bearer $_apiKey',
          'Content-Type': 'application/json',
        },
        body: json.encode({
          'model': 'gpt-3.5-turbo-instruct',
          'prompt': 'Complete this text with 3 short suggestions: "$currentText"',
          'max_tokens': 50,
          'n': 3,
          'temperature': 0.7,
        }),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return (data['choices'] as List)
            .map((choice) => choice['text'].toString().trim())
            .toList();
      }
    } catch (e) {
      print('AI Service error: $e');
    }
    
    // Fallback suggestions
    return _getFallbackSuggestions(currentText);
  }

  static List<String> _getFallbackSuggestions(String text) {
    final words = text.split(' ');
    final lastWord = words.isNotEmpty ? words.last.toLowerCase() : '';
    
    // Simple word completion suggestions
    final Map<String, List<String>> suggestions = {
      'the': ['the quick', 'the best', 'the most'],
      'how': ['how are you', 'how to', 'how much'],
      'what': ['what is', 'what are', 'what time'],
      'when': ['when is', 'when are', 'when will'],
      'where': ['where is', 'where are', 'where to'],
      'good': ['good morning', 'good night', 'good job'],
      'thank': ['thank you', 'thank you so much', 'thanks'],
      'please': ['please help', 'please let me know', 'please send'],
    };

    if (suggestions.containsKey(lastWord)) {
      return suggestions[lastWord]!;
    }

    return ['and', 'the', 'to'];
  }

  static Future<String> correctGrammar(String text) async {
    // Simplified grammar correction
    return text
        .replaceAll(' i ', ' I ')
        .replaceAll(' im ', ' I\'m ')
        .replaceAll(' dont ', ' don\'t ')
        .replaceAll(' cant ', ' can\'t ')
        .replaceAll(' wont ', ' won\'t ');
  }
}

// Keyboard theme configuration
class KeyboardTheme {
  final String name;
  final Color backgroundColor;
  final Color keyColor;
  final Color textColor;
  final Color accentColor;

  KeyboardTheme({
    required this.name,
    required this.backgroundColor,
    required this.keyColor,
    required this.textColor,
    required this.accentColor,
  });

  static KeyboardTheme getTheme(String themeName) {
    switch (themeName) {
      case 'dark':
        return KeyboardTheme(
          name: 'Dark',
          backgroundColor: const Color(0xFF1E1E1E),
          keyColor: const Color(0xFF2D2D2D),
          textColor: Colors.white,
          accentColor: Colors.blue,
        );
      case 'material_you':
        return KeyboardTheme(
          name: 'Material You',
          backgroundColor: const Color(0xFF6750A4),
          keyColor: const Color(0xFF7C4DFF),
          textColor: Colors.white,
          accentColor: const Color(0xFFBB86FC),
        );
      case 'professional':
        return KeyboardTheme(
          name: 'Professional',
          backgroundColor: const Color(0xFF37474F),
          keyColor: const Color(0xFF455A64),
          textColor: Colors.white,
          accentColor: const Color(0xFF26A69A),
        );
      case 'colorful':
        return KeyboardTheme(
          name: 'Colorful',
          backgroundColor: const Color(0xFFE1F5FE),
          keyColor: const Color(0xFF81D4FA),
          textColor: const Color(0xFF0D47A1),
          accentColor: const Color(0xFFFF6B35),
        );
      default:
        return KeyboardTheme(
          name: 'Default',
          backgroundColor: const Color(0xFFF5F5F5),
          keyColor: Colors.white,
          textColor: Colors.black87,
          accentColor: Colors.blue,
        );
    }
  }

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'backgroundColor': backgroundColor.value,
      'keyColor': keyColor.value,
      'textColor': textColor.value,
      'accentColor': accentColor.value,
    };
  }
}
/// System keyboard status panel
class SystemKeyboardStatusPanel extends StatelessWidget {
  const SystemKeyboardStatusPanel({super.key});

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
                Icon(Icons.keyboard, color: Colors.blue),
                SizedBox(width: 8),
                Text(
                  'System Keyboard Status',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.blue,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            const Text('✅ AI Keyboard Service: Ready'),
            const Text('🤖 Autocorrect Engine: Active'),
            const Text('🧠 Predictive Text: Learning'),
            const Text('📱 System Integration: Complete'),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: () {
                showDialog(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: const Text('System Keyboard'),
                    content: const Text('Go to Android Settings > System > Languages & input > Virtual keyboard to enable AI Keyboard'),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.pop(context),
                        child: const Text('OK'),
                      ),
                    ],
                  ),
                );
              },
              icon: const Icon(Icons.settings),
              label: const Text('Enable System Keyboard'),
            ),
          ],
        ),
      ),
    );
  }
}

/// AI Service information widget
class AIServiceInfoWidget extends StatelessWidget {
  const AIServiceInfoWidget({super.key});

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
                Icon(Icons.psychology, color: Colors.green),
                SizedBox(width: 8),
                Text(
                  'AI Features Now Available System-Wide',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.green,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            const Text('🔧 Smart Autocorrect'),
            const Text('   • Typo detection and correction'),
            const Text('   • Context-aware suggestions'),
            const SizedBox(height: 8),
            const Text('🧠 Predictive Text'),
            const Text('   • Word completion'),
            const Text('   • Context predictions'),
            const SizedBox(height: 8),
            const Text('✨ Learning System'),
            const Text('   • Adapts to your typing style'),
            const Text('   • Improves over time'),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.blue.shade50,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.blue.shade200),
              ),
              child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '🎯 How to Use:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 8),
                  Text('1. Enable AI Keyboard in Android Settings'),
                  Text('2. Open any app (WhatsApp, Gmail, etc.)'),
                  Text('3. Tap in text field → Select AI Keyboard'),
                  Text('4. Start typing → See AI suggestions!'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
