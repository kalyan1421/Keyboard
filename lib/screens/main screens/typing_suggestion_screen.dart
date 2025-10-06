import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/custom_toggle_switch.dart';
import 'package:ai_keyboard/services/keyboard_cloud_sync.dart';

class TypingSuggestionScreen extends StatefulWidget {
  const TypingSuggestionScreen({super.key});

  @override
  State<TypingSuggestionScreen> createState() => _TypingSuggestionScreenState();
}

class _TypingSuggestionScreenState extends State<TypingSuggestionScreen> {
  // MethodChannel for communication with native Kotlin keyboard
  static const _channel = MethodChannel('ai_keyboard/config');
  
  // Debounce timers
  Timer? _saveDebounceTimer;
  Timer? _notifyDebounceTimer;
  
  // Suggestion Settings
  bool displaySuggestions = true; // Default to true
  String displayMode = '3'; // Default to 3 columns
  double historySize = 20.0;
  bool clearPrimaryClipAffects = true;

  // Internal Settings
  bool internalClipboard = true;
  bool syncFromSystem = true;
  bool syncToFivive = true;

  @override
  void dispose() {
    _saveDebounceTimer?.cancel();
    _notifyDebounceTimer?.cancel();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  /// Load settings from SharedPreferences
  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      displaySuggestions = prefs.getBool('display_suggestions') ?? true; // Default true
      displayMode = prefs.getString('display_mode') ?? '3'; // Default 3 columns
      historySize = prefs.getDouble('clipboard_history_size') ?? 20.0;
      clearPrimaryClipAffects = prefs.getBool('clear_primary_clip_affects') ?? true;
      
      internalClipboard = prefs.getBool('internal_clipboard') ?? true;
      syncFromSystem = prefs.getBool('sync_from_system') ?? true;
      syncToFivive = prefs.getBool('sync_to_fivive') ?? true;
    });
  }

  /// Save settings with debouncing
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
    
    // Save all settings
    await prefs.setBool('display_suggestions', displaySuggestions);
    await prefs.setString('display_mode', displayMode);
    await prefs.setDouble('clipboard_history_size', historySize);
    await prefs.setBool('clear_primary_clip_affects', clearPrimaryClipAffects);
    
    await prefs.setBool('internal_clipboard', internalClipboard);
    await prefs.setBool('sync_from_system', syncFromSystem);
    await prefs.setBool('sync_to_fivive', syncToFivive);
    
    debugPrint('✅ Typing & Suggestion settings saved');
    
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
        'displaySuggestions': displaySuggestions,
        'displayMode': displayMode,
        'clipboardSuggestions': {
          'enabled': internalClipboard, // Use internal clipboard setting
          'windowSec': 60,
          'historyItems': historySize.toInt(),
        },
      });
      debugPrint('✅ Typing & Suggestion settings synced to Firebase');
    } catch (e) {
      debugPrint('⚠ Failed to sync to Firebase: $e');
      // Don't block user if Firebase fails
    }
  }
  
  /// Send settings to native keyboard
  Future<void> _sendSettingsToKeyboard() async {
    try {
      await _channel.invokeMethod('updateSettings', {
        'displaySuggestions': displaySuggestions,
        'displayMode': displayMode,
        'clipboardHistorySize': historySize.toInt(),
        'internalClipboard': internalClipboard,
        'syncFromSystem': syncFromSystem,
        'syncToFivive': syncToFivive,
        'clearPrimaryClipAffects': clearPrimaryClipAffects,
      });
      debugPrint('📤 Settings sent to native keyboard');
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
      await _channel.invokeMethod('broadcastSettingsChanged');
      debugPrint('✅ Keyboard notified - settings updated immediately');
      _showSuccessSnackBar();
    } catch (e) {
      debugPrint('⚠ Failed to notify: $e');
      _showErrorSnackBar(e.toString());
    }
  }
  
  /// Show success snackbar
  void _showSuccessSnackBar() {
    if (!mounted) return;
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Row(
          children: [
            Icon(Icons.check_circle, color: Colors.white, size: 20),
            SizedBox(width: 8),
            Expanded(child: Text('Settings saved! Keyboard updated immediately.')),
          ],
        ),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }
  
  /// Show error snackbar
  void _showErrorSnackBar(String error) {
    if (!mounted) return;
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Error updating keyboard: $error'),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 3),
        behavior: SnackBarBehavior.floating,
      ),
    );
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
          'Typing & Suggestion',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),
        centerTitle: false,
        backgroundColor: AppColors.primary,
        elevation: 0,
        actionsPadding: const EdgeInsets.only(right: 16),
        actions: [
          Stack(
            children: [
              Icon(Icons.notifications, color: AppColors.white, size: 24),
              Positioned(
                right: 0,
                top: 0,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
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
            const SizedBox(height: 8),

            // Suggestion Section
            _buildSectionTitle('Suggestion'),
            const SizedBox(height: 16),

            // Display suggestions
            _buildToggleSetting(
              title: 'Display suggestions',
              description: displaySuggestions ? 'Enabled' : 'Disabled',
              value: displaySuggestions,
              onChanged: (value) {
                setState(() => displaySuggestions = value);
                _saveSettings();
              },
            ),

            const SizedBox(height: 12),

            // Display mode
            _buildDisplayModeCard(),

            const SizedBox(height: 12),

            // History Size
            _buildSliderSetting(
              title: 'History Size',
              portraitValue: historySize,
              onPortraitChanged: (value) {
                setState(() => historySize = value);
                _saveSettings();
              },
              min: 5.0,
              max: 100.0,
              unit: '',
              portraitLabel: 'Items',
              showLandscape: false,
            ),

            const SizedBox(height: 12),

            // Clear primary clip affects
            _buildToggleSetting(
              title: 'Clear primary clip affects ...',
              description: clearPrimaryClipAffects ? 'Enabled' : 'Disabled',
              value: clearPrimaryClipAffects,
              onChanged: (value) {
                setState(() => clearPrimaryClipAffects = value);
                _saveSettings();
              },
            ),

            const SizedBox(height: 32),

            // Internal Settings Section
            _buildSectionTitle('Internal Settings'),
            const SizedBox(height: 16),

            // Internal Clipboard
            _buildToggleSetting(
              title: 'Internal Clipboard',
              description: internalClipboard ? 'Enabled' : 'Disabled',
              value: internalClipboard,
              onChanged: (value) {
                setState(() => internalClipboard = value);
                _saveSettings();
              },
            ),

            const SizedBox(height: 12),

            // Sync from system
            _buildToggleSetting(
              title: 'Sync from system',
              description: 'Sync from system clipboard',
              value: syncFromSystem,
              onChanged: (value) {
                setState(() => syncFromSystem = value);
                _saveSettings();
              },
            ),

            const SizedBox(height: 12),

            // Sync to fivive
            _buildToggleSetting(
              title: 'Sync to fivive',
              description: 'Sync to fivive clipboard',
              value: syncToFivive,
              onChanged: (value) {
                setState(() => syncToFivive = value);
                _saveSettings();
              },
            ),

            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
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
    required ValueChanged<bool> onChanged,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
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
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
                ),
              ],
            ),
          ),
          CustomToggleSwitch(
            value: value,
            onChanged: onChanged,
            width: 48.0,
            height: 16.0,
            knobSize: 24.0,
          ),
        ],
      ),
    );
  }

  Widget _buildDisplayModeCard() {
    // Display text based on mode value
    String modeText = displayMode == '3' ? '3 Suggestions' :
                      displayMode == '4' ? '4 Suggestions' :
                      displayMode == 'dynamic' ? 'Dynamic width' :
                      displayMode == 'scrollable' ? 'Dynamic width & scrollable' :
                      '3 Suggestions';
    
    return GestureDetector(
      onTap: () {
        _showDisplayModeDialog();
      },
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.lightGrey,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Display mode',
                    style: AppTextStyle.titleLarge.copyWith(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    modeText,
                    style: AppTextStyle.bodySmall.copyWith(
                      color: AppColors.grey,
                    ),
                  ),
                ],
              ),
            ),
            Icon(Icons.arrow_forward_ios, color: AppColors.grey, size: 16),
          ],
        ),
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
            style: AppTextStyle.titleLarge.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 12),
          // Portrait Slider
          Row(
            children: [
              SizedBox(
                width: 80,
                child: Text(
                  portraitLabel,
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.grey,
                  ),
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
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.w600,
                  ),
                  textAlign: TextAlign.right,
                ),
              ),
            ],
          ),
          // Landscape Slider (if enabled)
          if (showLandscape &&
              landscapeValue != null &&
              onLandscapeChanged != null) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                SizedBox(
                  width: 80,
                  child: Text(
                    landscapeLabel ?? 'Landscape',
                    style: AppTextStyle.bodyMedium.copyWith(
                      color: AppColors.grey,
                    ),
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

  void _showDisplayModeDialog() {
    String tempDisplayMode = displayMode; // Temporary selection
    
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return Dialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              child: Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Header
                    Text(
                      'Display Mode',
                      style: AppTextStyle.titleLarge.copyWith(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Select number of suggestions to display',
                      style: AppTextStyle.bodySmall.copyWith(
                        color: AppColors.grey,
                      ),
                    ),
                    Divider(color: AppColors.lightGrey, thickness: 1),
                    // Mode Options
                    _buildSimpleModeOptionInDialog('3', '3 Suggestions', tempDisplayMode, (value) {
                      setDialogState(() {
                        tempDisplayMode = value;
                      });
                    }),
                    const SizedBox(height: 12),
                    _buildSimpleModeOptionInDialog('4', '4 Suggestions', tempDisplayMode, (value) {
                      setDialogState(() {
                        tempDisplayMode = value;
                      });
                    }),
                    const SizedBox(height: 12),
                    _buildSimpleModeOptionInDialog('dynamic', 'Dynamic width', tempDisplayMode, (value) {
                      setDialogState(() {
                        tempDisplayMode = value;
                      });
                    }),
                    const SizedBox(height: 12),
                    _buildSimpleModeOptionInDialog('scrollable', 'Dynamic width & scrollable', tempDisplayMode, (value) {
                      setDialogState(() {
                        tempDisplayMode = value;
                      });
                    }),

                    const SizedBox(height: 20),

                    // Apply Button
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton(
                            onPressed: () => Navigator.of(context).pop(),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.lightGrey,
                              foregroundColor: AppColors.primary,
                              padding: const EdgeInsets.symmetric(vertical: 16),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(32),
                              ),
                              elevation: 0,
                            ),
                            child: Text(
                              'Cancel',
                              style: AppTextStyle.buttonSecondary.copyWith(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: () {
                              setState(() {
                                displayMode = tempDisplayMode;
                              });
                              _saveSettings();
                              Navigator.of(context).pop();
                            },
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.secondary,
                              foregroundColor: AppColors.white,
                              padding: const EdgeInsets.symmetric(vertical: 16),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(32),
                              ),
                              elevation: 0,
                            ),
                            child: Text(
                              'Apply',
                              style: AppTextStyle.buttonSecondary.copyWith(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
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
      },
    );
  }

  Widget _buildSimpleModeOptionInDialog(
    String value,
    String title,
    String currentValue,
    Function(String) onChanged,
  ) {
    bool isSelected = currentValue == value;

    return GestureDetector(
      onTap: () {
        onChanged(value);
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
        decoration: BoxDecoration(
          color: isSelected ? AppColors.secondary.withOpacity(0.1) : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isSelected ? AppColors.secondary : AppColors.lightGrey,
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            // Radio Button
            Radio<String>(
              value: value,
              groupValue: currentValue,
              onChanged: (String? newValue) {
                if (newValue != null) {
                  onChanged(newValue);
                }
              },
              activeColor: AppColors.secondary,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            const SizedBox(width: 8),

            // Content
            Expanded(
              child: Text(
                title,
                style: AppTextStyle.titleMedium.copyWith(
                  color: isSelected ? AppColors.secondary : AppColors.primary,
                  fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
