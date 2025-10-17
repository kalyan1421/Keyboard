import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/custom_toggle_switch.dart';
import 'package:ai_keyboard/keyboard_feedback_system.dart';
import 'package:ai_keyboard/services/keyboard_cloud_sync.dart';
import 'dart:async';

class KeyboardSettingsScreen extends StatefulWidget {
  const KeyboardSettingsScreen({super.key});

  @override
  State<KeyboardSettingsScreen> createState() => _KeyboardSettingsScreenState();
}

class _KeyboardSettingsScreenState extends State<KeyboardSettingsScreen> {
  // MethodChannel for keyboard communication
  static const _channel = MethodChannel('ai_keyboard/config');
  
  // Debounce timers
  Timer? _saveDebounceTimer;
  Timer? _notifyDebounceTimer;
  
  // General Settings
  bool numberRow = false;
  bool hintedNumberRow = false;
  bool hintedSymbols = true;
  String utilityKeyAction = 'emoji';
  bool displayLanguageOnSpace = true;
  double portraitFontSize = 100.0;
  double landscapeFontSize = 100.0;
   
  // Layout Settings
  bool borderlessKeys = false;
  bool oneHandedMode = false;
  String oneHandedSide = 'right';
  double oneHandedModeWidth = 87.0;
  bool landscapeFullScreenInput = true;
  double keyboardWidth = 100.0;
  double keyboardHeight = 100.0;
  double verticalKeySpacing = 5.0;
  double horizontalKeySpacing = 2.0;
  double portraitBottomOffset = 1.0;
  double landscapeBottomOffset = 2.0;

  // Key Press Settings
  bool popupVisibility = false; // ✅ Default OFF as requested
  double longPressDelay = 200.0;
  
  // Feature Settings
  bool _aiSuggestionsEnabled = true;
  bool _swipeTypingEnabled = true;
  bool _vibrationEnabled = true;
  bool _keyPreviewEnabled = false;
  bool _shiftFeedbackEnabled = false;
  bool _soundEnabled = true;
  bool _personalizedSuggestionsEnabled = true;
  bool _autoCorrectEnabled = true;  // ✅ NEW: Auto-Correct toggle
  
  // Advanced feedback settings
  FeedbackIntensity _hapticIntensity = FeedbackIntensity.medium;
  FeedbackIntensity _soundIntensity = FeedbackIntensity.light;
  FeedbackIntensity _visualIntensity = FeedbackIntensity.medium;
  double _soundVolume = 0.3;
  
  @override
  void initState() {
    super.initState();
    _loadSettings();
  }
  
  @override
  void dispose() {
    _saveDebounceTimer?.cancel();
    _notifyDebounceTimer?.cancel();
    super.dispose();
  }
  
  /// Load settings from SharedPreferences
  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    
    setState(() {
      // General Settings
      numberRow = prefs.getBool('keyboard.numberRow') ?? false;
      hintedNumberRow = prefs.getBool('keyboard.hintedNumberRow') ?? false;
      hintedSymbols = prefs.getBool('keyboard.hintedSymbols') ?? true;
      utilityKeyAction = prefs.getString('keyboard.utilityKeyAction') ?? 'emoji';
      displayLanguageOnSpace = prefs.getBool('keyboard.showLanguageOnSpace') ?? true;
      portraitFontSize = (prefs.getDouble('keyboard.fontScalePortrait') ?? 1.0) * 100.0;
      landscapeFontSize = (prefs.getDouble('keyboard.fontScaleLandscape') ?? 1.0) * 100.0;
      
      // Layout Settings
      borderlessKeys = prefs.getBool('keyboard.borderlessKeys') ?? false;
      oneHandedMode = prefs.getBool('keyboard.oneHanded.enabled') ?? false;
      oneHandedSide = prefs.getString('keyboard.oneHanded.side') ?? 'right';
      oneHandedModeWidth = (prefs.getDouble('keyboard.oneHanded.widthPct') ?? 0.87) * 100.0;
      landscapeFullScreenInput = prefs.getBool('keyboard.landscapeFullscreen') ?? true;
      keyboardWidth = (prefs.getDouble('keyboard.scaleX') ?? 1.0) * 100.0;
      keyboardHeight = (prefs.getDouble('keyboard.scaleY') ?? 1.0) * 100.0;
      verticalKeySpacing = prefs.getInt('keyboard.keySpacingVdp')?.toDouble() ?? 5.0;
      horizontalKeySpacing = prefs.getInt('keyboard.keySpacingHdp')?.toDouble() ?? 2.0;
      portraitBottomOffset = prefs.getInt('keyboard.bottomOffsetPortraitDp')?.toDouble() ?? 1.0;
      landscapeBottomOffset = prefs.getInt('keyboard.bottomOffsetLandscapeDp')?.toDouble() ?? 2.0;
      
      // Key Press Settings
      popupVisibility = prefs.getBool('keyboard.popupPreview') ?? false; // ✅ Default OFF
      longPressDelay = prefs.getInt('keyboard.longPressDelayMs')?.toDouble() ?? 200.0;
      
      // Feature Settings
      _aiSuggestionsEnabled = prefs.getBool('ai_suggestions') ?? true;
      _swipeTypingEnabled = prefs.getBool('swipe_typing') ?? true;
      _vibrationEnabled = prefs.getBool('vibration_enabled') ?? true;
      _keyPreviewEnabled = prefs.getBool('key_preview_enabled') ?? false;
      _shiftFeedbackEnabled = prefs.getBool('show_shift_feedback') ?? false;
      _soundEnabled = prefs.getBool('sound_enabled') ?? true;
      _personalizedSuggestionsEnabled = prefs.getBool('personalized_enabled') ?? true;
      _autoCorrectEnabled = prefs.getBool('auto_correct') ?? true;  // ✅ Load auto-correct setting
      
      // Advanced feedback settings
      _hapticIntensity = FeedbackIntensity.values[prefs.getInt('haptic_intensity') ?? 2];
      _soundIntensity = FeedbackIntensity.values[prefs.getInt('sound_intensity') ?? 1];
      _visualIntensity = FeedbackIntensity.values[prefs.getInt('visual_intensity') ?? 2];
      _soundVolume = prefs.getDouble('sound_volume') ?? 0.3;
    });
    
    // Update feedback system
    KeyboardFeedbackSystem.updateSettings(
      haptic: _hapticIntensity,
      sound: _soundIntensity,
      visual: _visualIntensity,
      volume: _soundVolume,
    );
  }
  
  /// Save settings with proper debouncing
  Future<void> _saveSettings({bool immediate = false}) async {
    // Cancel existing timer
    _saveDebounceTimer?.cancel();
    
    if (immediate) {
      await _performSave();
    } else {
      // Debounce for 500ms
      _saveDebounceTimer = Timer(const Duration(milliseconds: 500), () {
        _performSave();
      });
    }
  }
  
  /// Actually perform the save operation
  Future<void> _performSave() async {
    final prefs = await SharedPreferences.getInstance();
    
    // Enforce mutual exclusivity
    if (numberRow && hintedNumberRow) {
      hintedNumberRow = false;
    }
    
    // Clamp ranges
    portraitFontSize = portraitFontSize.clamp(80.0, 130.0);
    landscapeFontSize = landscapeFontSize.clamp(80.0, 130.0);
    keyboardWidth = keyboardWidth.clamp(85.0, 115.0);
    keyboardHeight = keyboardHeight.clamp(85.0, 115.0);
    verticalKeySpacing = verticalKeySpacing.clamp(0.0, 8.0);
    horizontalKeySpacing = horizontalKeySpacing.clamp(0.0, 8.0);
    longPressDelay = longPressDelay.clamp(150.0, 600.0);
    oneHandedModeWidth = oneHandedModeWidth.clamp(70.0, 100.0);
    
    // Save all settings
    await prefs.setBool('keyboard.numberRow', numberRow);
    await prefs.setBool('keyboard.hintedNumberRow', hintedNumberRow);
    await prefs.setBool('keyboard.hintedSymbols', hintedSymbols);
    await prefs.setString('keyboard.utilityKeyAction', utilityKeyAction);
    await prefs.setBool('keyboard.showLanguageOnSpace', displayLanguageOnSpace);
    await prefs.setDouble('keyboard.fontScalePortrait', portraitFontSize / 100.0);
    await prefs.setDouble('keyboard.fontScaleLandscape', landscapeFontSize / 100.0);
    
    await prefs.setBool('keyboard.borderlessKeys', borderlessKeys);
    await prefs.setBool('keyboard.oneHanded.enabled', oneHandedMode);
    await prefs.setString('keyboard.oneHanded.side', oneHandedSide);
    await prefs.setDouble('keyboard.oneHanded.widthPct', oneHandedModeWidth / 100.0);
    await prefs.setBool('keyboard.landscapeFullscreen', landscapeFullScreenInput);
    await prefs.setDouble('keyboard.scaleX', keyboardWidth / 100.0);
    await prefs.setDouble('keyboard.scaleY', keyboardHeight / 100.0);
    await prefs.setInt('keyboard.keySpacingVdp', verticalKeySpacing.round());
    await prefs.setInt('keyboard.keySpacingHdp', horizontalKeySpacing.round());
    await prefs.setInt('keyboard.bottomOffsetPortraitDp', portraitBottomOffset.round());
    await prefs.setInt('keyboard.bottomOffsetLandscapeDp', landscapeBottomOffset.round());
    
    await prefs.setBool('keyboard.popupPreview', popupVisibility);
    await prefs.setInt('keyboard.longPressDelayMs', longPressDelay.round());
    
    await prefs.setBool('ai_suggestions', _aiSuggestionsEnabled);
    await prefs.setBool('swipe_typing', _swipeTypingEnabled);
    await prefs.setBool('vibration_enabled', _vibrationEnabled);
    await prefs.setBool('key_preview_enabled', _keyPreviewEnabled);
    await prefs.setBool('show_shift_feedback', _shiftFeedbackEnabled);
    await prefs.setBool('sound_enabled', _soundEnabled);
    await prefs.setBool('personalized_enabled', _personalizedSuggestionsEnabled);
    await prefs.setBool('auto_correct', _autoCorrectEnabled);  // ✅ Save auto-correct setting
    
    await prefs.setInt('haptic_intensity', _hapticIntensity.index);
    await prefs.setInt('sound_intensity', _soundIntensity.index);
    await prefs.setInt('visual_intensity', _visualIntensity.index);
    await prefs.setDouble('sound_volume', _soundVolume);
    
    debugPrint('✅ Keyboard settings saved');
    
    // Update feedback system
    KeyboardFeedbackSystem.updateSettings(
      haptic: _hapticIntensity,
      sound: _soundIntensity,
      visual: _visualIntensity,
      volume: _soundVolume,
    );
    
    // Send to native keyboard
    await _sendSettingsToKeyboard();
    
    // Sync to Firebase for cross-device sync
    await _syncToFirebase();
    
    // Notify keyboard (debounced)
    _debouncedNotifyKeyboard();
  }
  
  /// Sync settings to Firebase for cross-device sync
  Future<void> _syncToFirebase() async {
    try {
      await KeyboardCloudSync.upsert({
        'popupEnabled': popupVisibility,
        'aiSuggestions': _aiSuggestionsEnabled,
        'autocorrect': _autoCorrectEnabled,
        'emojiSuggestions': true,
        'nextWordPrediction': true,
        'clipboardSuggestions': {
          'enabled': true, // ✅ Clipboard ON
          'windowSec': 60,
          'historyItems': 20, // ✅ Min 20 items
        },
        'dictionaryEnabled': true, // ✅ Dictionary ON
        'autoCapitalization': true,
        'doubleSpacePeriod': true,
        'soundEnabled': _soundEnabled,
        'soundVolume': _soundVolume,
        'vibrationEnabled': _vibrationEnabled,
        'vibrationMs': 50,
        // Advanced settings
        'numberRow': numberRow,
        'hintedSymbols': hintedSymbols,
        'swipeTyping': _swipeTypingEnabled,
        'keyPreview': _keyPreviewEnabled,
        'personalizedSuggestions': _personalizedSuggestionsEnabled,
      });
      debugPrint('✅ Settings synced to Firebase for cross-device sync');
    } catch (e) {
      debugPrint('⚠ Failed to sync to Firebase: $e');
      // Don't block user if Firebase fails
    }
  }
  
  /// Send settings to native keyboard
  Future<void> _sendSettingsToKeyboard() async {
    try {
      await _channel.invokeMethod('updateSettings', {
        'theme': 'default',
        'aiSuggestions': _aiSuggestionsEnabled,
        'autoCorrect': _autoCorrectEnabled,  // ✅ Send auto-correct setting
        'swipeTyping': _swipeTypingEnabled,
        'vibration': _vibrationEnabled,
        'keyPreview': _keyPreviewEnabled,
        'shiftFeedback': _shiftFeedbackEnabled,
        'showNumberRow': numberRow,
        'soundEnabled': _soundEnabled,
      });
    } catch (e) {
      debugPrint('⚠ Error sending settings: $e');
    }
  }
  
  /// Notify keyboard with debounce
  void _debouncedNotifyKeyboard() {
    _notifyDebounceTimer?.cancel();
    _notifyDebounceTimer = Timer(const Duration(milliseconds: 300), () {
      _notifyKeyboard();
    });
  }
  
  /// Notify keyboard via MethodChannel
  Future<void> _notifyKeyboard() async {
    try {
      await _channel.invokeMethod('notifyConfigChange');
      debugPrint('✓ Notified keyboard');
      _showSuccessSnackBar();
    } catch (e) {
      debugPrint('⚠ Failed to notify: $e');
    }
  }
  
  /// Show success snackbar
  void _showSuccessSnackBar() {
    if (!mounted) return;
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: const [
            Icon(Icons.check_circle, color: Colors.white, size: 20),
            SizedBox(width: 8),
            Expanded(child: Text('Settings saved successfully!')),
          ],
        ),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }

  /// Show dialog to confirm clearing learned words
  void _showClearWordsDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: Row(
            children: [
              const Icon(Icons.warning_amber, color: Colors.orange, size: 24),
              const SizedBox(width: 8),
              Text(
                'Clear Learned Words',
                style: AppTextStyle.titleMedium.copyWith(
                  color: AppColors.black,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          content: Text(
            'This will permanently delete all words the keyboard has learned from your typing patterns. This action cannot be undone.\n\nAre you sure you want to continue?',
            style: AppTextStyle.bodyMedium.copyWith(color: AppColors.grey),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: Text(
                'Cancel',
                style: AppTextStyle.labelLarge.copyWith(
                  color: AppColors.grey,
                ),
              ),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
                _clearLearnedWords();
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: Text(
                'Clear All',
                style: AppTextStyle.labelLarge.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  /// Clear all learned words via MethodChannel
  Future<void> _clearLearnedWords() async {
    try {
      await _channel.invokeMethod('clearLearnedWords');
      
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: const [
              Icon(Icons.check_circle, color: Colors.white, size: 20),
              SizedBox(width: 8),
              Expanded(child: Text('Learned words cleared successfully!')),
            ],
          ),
          backgroundColor: Colors.green,
          duration: const Duration(seconds: 2),
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
      );
    } catch (e) {
      debugPrint('⚠ Error clearing learned words: $e');
      
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: const [
              Icon(Icons.error, color: Colors.white, size: 20),
              SizedBox(width: 8),
              Expanded(child: Text('Failed to clear learned words')),
            ],
          ),
          backgroundColor: Colors.red,
          duration: const Duration(seconds: 3),
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Keyboard Settings',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),
        centerTitle: false,
        backgroundColor: AppColors.primary,
        elevation: 0,
        actionsPadding: const EdgeInsets.only(right: 16),
        actions: [
          Stack(
            children: [
              const Icon(Icons.notifications, color: AppColors.white, size: 24),
              Positioned(
                right: 0,
                top: 0,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
      backgroundColor: AppColors.white,

      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildToggleSetting(
              title: 'Auto-Correct',
              description: 'Automatically fix typos as you type (e.g., "teh" → "the")',
              value: _autoCorrectEnabled,
              onChanged: (value) {
                setState(() => _autoCorrectEnabled = value);
                _saveSettings(immediate: true);
              },
            ),
            // General Section
            _buildSectionHeader('General'),
            const SizedBox(height: 12),
            _buildToggleSetting(
              title: 'Number row',
              description: 'Show a number row above the character layout',
              value: numberRow,
              onChanged: (value) {
                setState(() {
                  numberRow = value;
                  if (value) hintedNumberRow = false;
                });
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'Hinted number row',
              description: numberRow ? 'Disabled (conflicts with number row)' : 'Show number hints on letter keys',
              value: hintedNumberRow,
              onChanged: numberRow ? null : (value) {
                setState(() => hintedNumberRow = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'Hinted symbols',
              description: 'Show symbol hints on letter keys',
              value: hintedSymbols,
              onChanged: (value) {
                setState(() => hintedSymbols = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'Display language on spacebar',
              description: 'Show current language name on spacebar',
              value: displayLanguageOnSpace,
              onChanged: (value) {
                setState(() => displayLanguageOnSpace = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildSliderSetting(
              title: 'Font Size multiplier',
              portraitValue: portraitFontSize,
              landscapeValue: landscapeFontSize,
              onPortraitChanged: (value) {
                setState(() => portraitFontSize = value);
                _saveSettings(); // Debounced
              },
              onLandscapeChanged: (value) {
                setState(() => landscapeFontSize = value);
                _saveSettings(); // Debounced
              },
              min: 80.0,
              max: 130.0,
              unit: '%',
            ),
            const SizedBox(height: 24),

            // Layout Section
            _buildSectionHeader('Layout'),
            const SizedBox(height: 12),
            _buildToggleSetting(
              title: 'Borderless keys',
              description: 'Remove key borders for cleaner look',
              value: borderlessKeys,
              onChanged: (value) {
                setState(() => borderlessKeys = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'One-handed mode',
              description: 'Dock keyboard to left or right side',
              value: oneHandedMode,
              onChanged: (value) {
                setState(() => oneHandedMode = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            if (oneHandedMode) ...[
              _buildSideSelector(),
              const SizedBox(height: 8),
            ],
            _buildValueDisplay(
              title: 'One-handed mode keyboard width',
              value: '${oneHandedModeWidth.toInt()}%',
            ),
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'Landscape full screen input',
              description: 'Expand keyboard to full height in landscape',
              value: landscapeFullScreenInput,
              onChanged: (value) {
                setState(() => landscapeFullScreenInput = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildSliderSetting(
              title: 'Keyboard Size',
              portraitValue: keyboardWidth,
              landscapeValue: keyboardHeight,
              onPortraitChanged: (value) {
                setState(() => keyboardWidth = value);
                _saveSettings();
              },
              onLandscapeChanged: (value) {
                setState(() => keyboardHeight = value);
                _saveSettings();
              },
              min: 50.0,
              max: 150.0,
              unit: '%',
              portraitLabel: 'Width',
              landscapeLabel: 'Height',
            ),
            const SizedBox(height: 8),
            _buildSliderSetting(
              title: 'Key spacing',
              portraitValue: verticalKeySpacing,
              landscapeValue: horizontalKeySpacing,
              onPortraitChanged: (value) {
                setState(() => verticalKeySpacing = value);
                _saveSettings();
              },
              onLandscapeChanged: (value) {
                setState(() => horizontalKeySpacing = value);
                _saveSettings();
              },
              min: 0.0,
              max: 10.0,
              unit: ' dp',
              portraitLabel: 'Vertical',
              landscapeLabel: 'Horizontal',
            ),
            const SizedBox(height: 8),
            _buildSliderSetting(
              title: 'Bottom offset',
              portraitValue: portraitBottomOffset,
              landscapeValue: landscapeBottomOffset,
              onPortraitChanged: (value) {
                setState(() => portraitBottomOffset = value);
                _saveSettings();
              },
              onLandscapeChanged: (value) {
                setState(() => landscapeBottomOffset = value);
                _saveSettings();
              },
              min: 0.0,
              max: 10.0,
              unit: ' dp',
            ),
            const SizedBox(height: 24),

            // Key Press Section
            _buildSectionHeader('Key Press'),
            const SizedBox(height: 12),
            // Popup Visibility feature disabled - commented out to prevent users from enabling it
            // _buildToggleSetting(
            //   title: 'Popup Visibility',
            //   description: 'Show popup preview when pressing keys',
            //   value: popupVisibility,
            //   onChanged: (value) {
            //     setState(() => popupVisibility = value);
            //     _saveSettings(immediate: true);
            //   },
            // ),
            // const SizedBox(height: 8),
            _buildSliderSetting(
              title: 'Long Press Delay',
              portraitValue: longPressDelay,
              onPortraitChanged: (value) {
                setState(() => longPressDelay = value);
                _saveSettings();
              },
              min: 100.0,
              max: 1000.0,
              unit: 'ms',
              portraitLabel: 'Delay',
              showLandscape: false,
            ),
            const SizedBox(height: 24),
            
            // Features Section
            _buildSectionHeader('Features'),
            const SizedBox(height: 12),
            _buildToggleSetting(
              title: 'AI Suggestions',
              description: 'Get smart text predictions and corrections',
              value: _aiSuggestionsEnabled,
              onChanged: (value) {
                setState(() => _aiSuggestionsEnabled = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'Swipe Typing',
              description: 'Swipe across letters to form words',
              value: _swipeTypingEnabled,
              onChanged: (value) {
                setState(() => _swipeTypingEnabled = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'Vibration Feedback',
              description: 'Haptic feedback when typing',
              value: _vibrationEnabled,
              onChanged: (value) {
                setState(() => _vibrationEnabled = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'Sound Feedback',
              description: 'Play typing sounds when pressing keys',
              value: _soundEnabled,
              onChanged: (value) {
                setState(() => _soundEnabled = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildToggleSetting(
              title: 'Personalized Suggestions',
              description: 'Learn from your typing to provide better suggestions',
              value: _personalizedSuggestionsEnabled,
              onChanged: (value) {
                setState(() => _personalizedSuggestionsEnabled = value);
                _saveSettings(immediate: true);
              },
            ),
            const SizedBox(height: 8),
            _buildClearWordsButton(),
            const SizedBox(height: 24),
            
            
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Text(
      title,
      style: AppTextStyle.titleMedium.copyWith(
        color: AppColors.secondary,
        fontWeight: FontWeight.w600,
      ),
    );
  }

  Widget _buildToggleSetting({
    required String title,
    required String description,
    required bool value,
    required ValueChanged<bool>? onChanged,
  }) {
    final isEnabled = onChanged != null;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isEnabled ? AppColors.lightGrey : AppColors.lightGrey.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppTextStyle.titleLarge.copyWith(
                    color: isEnabled ? AppColors.primary : AppColors.grey,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: AppTextStyle.bodySmall.copyWith(
                    color: isEnabled ? AppColors.grey : AppColors.grey.withValues(alpha: 0.6),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8), // Fix overflow issue
          CustomToggleSwitch(
            value: value,
            onChanged: isEnabled ? onChanged : (_) {},
            width: 52.0,
            height: 20.0,
            knobSize: 24.0,
          ),
        ],
      ),
    );
  }

  Widget _buildSliderSetting({
    required String title,
    required double portraitValue,
    double? landscapeValue,
    required ValueChanged<double> onPortraitChanged,
    ValueChanged<double>? onLandscapeChanged,
    required double min,
    required double max,
    required String unit,
    String portraitLabel = 'Portrait',
    String? landscapeLabel,
    bool showLandscape = true,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: AppTextStyle.titleSmall.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              SizedBox(
                width: 80,
                child: Text(
                  portraitLabel,
                  style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
                ),
              ),
              Expanded(
                child: Slider(
                  thumbColor: AppColors.white,
                  value: portraitValue,
                  min: min,
                  max: max,
                  onChanged: onPortraitChanged,
                  activeColor: AppColors.secondary,
                  inactiveColor: AppColors.white,
                ),
              ),
              SizedBox(
                width: 50,
                child: Text(
                  '${portraitValue.toInt()}$unit',
                  style: AppTextStyle.bodySmall.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.w600,
                  ),
                  textAlign: TextAlign.right,
                ),
              ),
            ],
          ),
          if (showLandscape && landscapeValue != null && onLandscapeChanged != null) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                SizedBox(
                  width: 80,
                  child: Text(
                    landscapeLabel ?? 'Landscape',
                    style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
                  ),
                ),
                Expanded(
                  child: Slider(
                    thumbColor: AppColors.white,
                    value: landscapeValue,
                    min: min,
                    max: max,
                    onChanged: onLandscapeChanged,
                    activeColor: AppColors.secondary,
                    inactiveColor: AppColors.white,
                  ),
                ),
                SizedBox(
                  width: 50,
                  child: Text(
                    '${landscapeValue.toInt()}$unit',
                    style: AppTextStyle.bodySmall.copyWith(
                      color: AppColors.black,
                      fontWeight: FontWeight.w600,
                    ),
                    textAlign: TextAlign.right,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildValueDisplay({required String title, required String value}) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              title,
              style: AppTextStyle.titleSmall.copyWith(
                color: AppColors.black,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          Text(
            value,
            style: AppTextStyle.bodySmall.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildIntensitySelector(
    String title,
    String subtitle,
    FeedbackIntensity value,
    ValueChanged<FeedbackIntensity> onChanged,
  ) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: AppTextStyle.titleSmall.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: FeedbackIntensity.values.map((intensity) {
              final isSelected = value == intensity;
              return GestureDetector(
                onTap: () => onChanged(intensity),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: isSelected ? AppColors.secondary : Colors.transparent,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: isSelected ? AppColors.secondary : AppColors.grey,
                      width: 1,
                    ),
                  ),
                  child: Text(
                    _getIntensityLabel(intensity),
                    style: AppTextStyle.bodySmall.copyWith(
                      color: isSelected ? AppColors.white : AppColors.grey,
                      fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildVolumeSlider() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Sound Volume',
            style: AppTextStyle.titleSmall.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Adjust keyboard sound volume level',
            style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              const Icon(Icons.volume_down, color: AppColors.grey, size: 20),
              Expanded(
                child: Slider(
                  value: _soundVolume,
                  min: 0.0,
                  max: 1.0,
                  divisions: 10,
                  onChanged: (value) {
                    setState(() => _soundVolume = value);
                    _saveSettings();
                  },
                  activeColor: AppColors.secondary,
                  inactiveColor: AppColors.white,
                  thumbColor: AppColors.white,
                ),
              ),
              const Icon(Icons.volume_up, color: AppColors.grey, size: 20),
            ],
          ),
          Center(
            child: Text(
              '${(_soundVolume * 100).round()}%',
              style: AppTextStyle.bodySmall.copyWith(
                color: AppColors.black,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
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
  
  Widget _buildSideSelector() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Keyboard Side',
            style: AppTextStyle.titleSmall.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Choose which side to dock the keyboard',
            style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: GestureDetector(
                  onTap: () {
                    setState(() => oneHandedSide = 'left');
                    _saveSettings(immediate: true);
                  },
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    decoration: BoxDecoration(
                      color: oneHandedSide == 'left' ? AppColors.secondary : Colors.transparent,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: oneHandedSide == 'left' ? AppColors.secondary : AppColors.grey,
                        width: 2,
                      ),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.keyboard_arrow_left,
                          color: oneHandedSide == 'left' ? AppColors.white : AppColors.grey,
                        ),
                        Text(
                          'Left',
                          style: AppTextStyle.bodyMedium.copyWith(
                            color: oneHandedSide == 'left' ? AppColors.white : AppColors.grey,
                            fontWeight: oneHandedSide == 'left' ? FontWeight.w600 : FontWeight.normal,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: GestureDetector(
                  onTap: () {
                    setState(() => oneHandedSide = 'right');
                    _saveSettings(immediate: true);
                  },
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    decoration: BoxDecoration(
                      color: oneHandedSide == 'right' ? AppColors.secondary : Colors.transparent,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: oneHandedSide == 'right' ? AppColors.secondary : AppColors.grey,
                        width: 2,
                      ),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Right',
                          style: AppTextStyle.bodyMedium.copyWith(
                            color: oneHandedSide == 'right' ? AppColors.white : AppColors.grey,
                            fontWeight: oneHandedSide == 'right' ? FontWeight.w600 : FontWeight.normal,
                          ),
                        ),
                        Icon(
                          Icons.keyboard_arrow_right,
                          color: oneHandedSide == 'right' ? AppColors.white : AppColors.grey,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildClearWordsButton() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Learned Words',
            style: AppTextStyle.titleSmall.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Clear all words learned from your typing patterns',
            style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: _showClearWordsDialog,
              icon: const Icon(Icons.delete_outline, color: Colors.red, size: 20),
              label: Text(
                'Clear Learned Words',
                style: AppTextStyle.bodyMedium.copyWith(
                  color: Colors.red,
                  fontWeight: FontWeight.w600,
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red.withOpacity(0.1),
                foregroundColor: Colors.red,
                elevation: 0,
                side: BorderSide(color: Colors.red.withOpacity(0.3), width: 1),
                padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}