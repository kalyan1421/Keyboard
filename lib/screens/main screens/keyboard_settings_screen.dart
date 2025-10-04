import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/custom_toggle_switch.dart';

class KeyboardSettingsScreen extends StatefulWidget {
  const KeyboardSettingsScreen({super.key});

  @override
  State<KeyboardSettingsScreen> createState() => _KeyboardSettingsScreenState();
}

class _KeyboardSettingsScreenState extends State<KeyboardSettingsScreen> {
  // General Settings
  bool numberRow = false;
  bool hintedNumberRow = false;
  bool hintedSymbols = false;
  bool showUtilityKey = true;
  bool displayLanguageOnSpace = true;
  double portraitFontSize = 100.0;
  double landscapeFontSize = 100.0;

  // Layout Settings
  bool borderlessKeys = false;
  bool oneHandedMode = false;
  double oneHandedModeWidth = 87.0;
  bool landscapeFullScreenInput = true;
  double keyboardWidth = 100.0;
  double keyboardHeight = 100.0;
  double verticalKeySpacing = 5.0;
  double horizontalKeySpacing = 2.0;
  double portraitBottomOffset = 1.0;
  double landscapeBottomOffset = 2.0;

  // Key Press Settings
  bool popupVisibility = true;
  double longPressDelay = 200.0;
  
  @override
  void initState() {
    super.initState();
    _loadSettings();
  }
  
  /// Load settings from SharedPreferences
  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    
    setState(() {
      // General Settings
      numberRow = prefs.getBool('flutter.keyboard_settings.number_row') ?? false;
      hintedNumberRow = prefs.getBool('flutter.keyboard_settings.hinted_number_row') ?? false;
      hintedSymbols = prefs.getBool('flutter.keyboard_settings.hinted_symbols') ?? false;
      showUtilityKey = prefs.getBool('flutter.keyboard_settings.show_utility_key') ?? true;
      displayLanguageOnSpace = prefs.getBool('flutter.keyboard_settings.display_language_on_space') ?? true;
      portraitFontSize = prefs.getDouble('flutter.keyboard_settings.portrait_font_size') ?? 100.0;
      landscapeFontSize = prefs.getDouble('flutter.keyboard_settings.landscape_font_size') ?? 100.0;
      
      // Layout Settings
      borderlessKeys = prefs.getBool('flutter.keyboard_settings.borderless_keys') ?? false;
      oneHandedMode = prefs.getBool('flutter.keyboard_settings.one_handed_mode') ?? false;
      oneHandedModeWidth = prefs.getDouble('flutter.keyboard_settings.one_handed_mode_width') ?? 87.0;
      landscapeFullScreenInput = prefs.getBool('flutter.keyboard_settings.landscape_full_screen_input') ?? true;
      keyboardWidth = prefs.getDouble('flutter.keyboard_settings.keyboard_width') ?? 100.0;
      keyboardHeight = prefs.getDouble('flutter.keyboard_settings.keyboard_height') ?? 100.0;
      verticalKeySpacing = prefs.getDouble('flutter.keyboard_settings.vertical_key_spacing') ?? 5.0;
      horizontalKeySpacing = prefs.getDouble('flutter.keyboard_settings.horizontal_key_spacing') ?? 2.0;
      portraitBottomOffset = prefs.getDouble('flutter.keyboard_settings.portrait_bottom_offset') ?? 1.0;
      landscapeBottomOffset = prefs.getDouble('flutter.keyboard_settings.landscape_bottom_offset') ?? 2.0;
      
      // Key Press Settings
      popupVisibility = prefs.getBool('flutter.keyboard_settings.popup_visibility') ?? true;
      longPressDelay = prefs.getDouble('flutter.keyboard_settings.long_press_delay') ?? 200.0;
    });
  }
  
  /// Save settings to SharedPreferences
  Future<void> _saveSettings() async {
    final prefs = await SharedPreferences.getInstance();
    
    // General Settings
    await prefs.setBool('flutter.keyboard_settings.number_row', numberRow);
    await prefs.setBool('flutter.keyboard_settings.hinted_number_row', hintedNumberRow);
    await prefs.setBool('flutter.keyboard_settings.hinted_symbols', hintedSymbols);
    await prefs.setBool('flutter.keyboard_settings.show_utility_key', showUtilityKey);
    await prefs.setBool('flutter.keyboard_settings.display_language_on_space', displayLanguageOnSpace);
    await prefs.setDouble('flutter.keyboard_settings.portrait_font_size', portraitFontSize);
    await prefs.setDouble('flutter.keyboard_settings.landscape_font_size', landscapeFontSize);
    
    // Layout Settings
    await prefs.setBool('flutter.keyboard_settings.borderless_keys', borderlessKeys);
    await prefs.setBool('flutter.keyboard_settings.one_handed_mode', oneHandedMode);
    await prefs.setDouble('flutter.keyboard_settings.one_handed_mode_width', oneHandedModeWidth);
    await prefs.setBool('flutter.keyboard_settings.landscape_full_screen_input', landscapeFullScreenInput);
    await prefs.setDouble('flutter.keyboard_settings.keyboard_width', keyboardWidth);
    await prefs.setDouble('flutter.keyboard_settings.keyboard_height', keyboardHeight);
    await prefs.setDouble('flutter.keyboard_settings.vertical_key_spacing', verticalKeySpacing);
    await prefs.setDouble('flutter.keyboard_settings.horizontal_key_spacing', horizontalKeySpacing);
    await prefs.setDouble('flutter.keyboard_settings.portrait_bottom_offset', portraitBottomOffset);
    await prefs.setDouble('flutter.keyboard_settings.landscape_bottom_offset', landscapeBottomOffset);
    
    // Key Press Settings
    await prefs.setBool('flutter.keyboard_settings.popup_visibility', popupVisibility);
    await prefs.setDouble('flutter.keyboard_settings.long_press_delay', longPressDelay);
    
    // Notify Android side of settings change
    await prefs.setBool('flutter.keyboard_settings.settings_changed', true);
    
    debugPrint('Keyboard settings saved successfully');
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

      body: Column(
        children: [
          // Header

          // Content
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // General Section
                  _buildSectionHeader('General'),
                  const SizedBox(height: 12),
                  _buildToggleSetting(
                    title: 'Number row',
                    description: 'Show a number row above the character layout',
                    value: numberRow,
                    onChanged: (value) => setState(() => numberRow = value),
                  ),
                  const SizedBox(height: 8),
                  _buildToggleSetting(
                    title: 'Hinted number row',
                    description: 'Disabled',
                    value: hintedNumberRow,
                    onChanged: (value) {
                      setState(() => hintedNumberRow = value);
                      _saveSettings();
                    },
                  ),
                  const SizedBox(height: 8),
                  _buildToggleSetting(
                    title: 'Hinted symbols',
                    description: 'Disabled',
                    value: hintedSymbols,
                    onChanged: (value) => setState(() => hintedSymbols = value),
                  ),
                  const SizedBox(height: 8),
                  _buildToggleSetting(
                    title: 'Show utility key',
                    description:
                        'Shows a configurable utility key next to space bar',
                    value: showUtilityKey,
                    onChanged: (value) {
                      setState(() => showUtilityKey = value);
                      _saveSettings();
                    },
                  ),
                  const SizedBox(height: 8),
                  _buildToggleSetting(
                    title: 'Display language on spac...',
                    description:
                        'Display the current active language name on the sp...',
                    value: displayLanguageOnSpace,
                    onChanged: (value) {
                      setState(() => displayLanguageOnSpace = value);
                      _saveSettings();
                    },
                  ),
                  const SizedBox(height: 8),
                  _buildSliderSetting(
                    title: 'Font Size multiplier',
                    portraitValue: portraitFontSize,
                    landscapeValue: landscapeFontSize,
                    onPortraitChanged: (value) {
                      setState(() => portraitFontSize = value);
                      _saveSettings();
                    },
                    onLandscapeChanged: (value) {
                      setState(() => landscapeFontSize = value);
                      _saveSettings();
                    },
                    min: 50.0,
                    max: 150.0,
                    unit: '%',
                  ),
                  const SizedBox(height: 24),

                  // Layout Section
                  _buildSectionHeader('Layout'),
                  const SizedBox(height: 12),
                  _buildToggleSetting(
                    title: 'Borderless keys',
                    description: 'Disabled',
                    value: borderlessKeys,
                    onChanged: (value) {
                      setState(() => borderlessKeys = value);
                      _saveSettings();
                    },
                  ),
                  const SizedBox(height: 8),
                  _buildToggleSetting(
                    title: 'One-handed mode',
                    description: 'Off',
                    value: oneHandedMode,
                    onChanged: (value) => setState(() => oneHandedMode = value),
                  ),
                  const SizedBox(height: 8),
                  _buildValueDisplay(
                    title: 'One-handed mode keyboard widh',
                    value: '${oneHandedModeWidth.toInt()}%',
                  ),
                  const SizedBox(height: 8),
                  _buildToggleSetting(
                    title: 'Landscape full screen input',
                    description: 'Dynamically show',
                    value: landscapeFullScreenInput,
                    onChanged: (value) {
                      setState(() => landscapeFullScreenInput = value);
                      _saveSettings();
                    },
                  ),
                  const SizedBox(height: 8),
                  _buildSliderSetting(
                    title: 'Keyboard Height',
                    portraitValue: keyboardWidth,
                    landscapeValue: keyboardHeight,
                    onPortraitChanged: (value) =>
                        setState(() => keyboardWidth = value),
                    onLandscapeChanged: (value) =>
                        setState(() => keyboardHeight = value),
                    min: 50.0,
                    max: 150.0,
                    unit: '%',
                    portraitLabel: 'Width',
                    landscapeLabel: 'Hight',
                  ),
                  const SizedBox(height: 8),
                  _buildSliderSetting(
                    title: 'Key spacing',
                    portraitValue: verticalKeySpacing,
                    landscapeValue: horizontalKeySpacing,
                    onPortraitChanged: (value) =>
                        setState(() => verticalKeySpacing = value),
                    onLandscapeChanged: (value) =>
                        setState(() => horizontalKeySpacing = value),
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
                    onPortraitChanged: (value) =>
                        setState(() => portraitBottomOffset = value),
                    onLandscapeChanged: (value) =>
                        setState(() => landscapeBottomOffset = value),
                    min: 0.0,
                    max: 10.0,
                    unit: ' dp',
                  ),
                  const SizedBox(height: 24),

                  // Key Press Section
                  _buildSectionHeader('Key Press'),
                  const SizedBox(height: 12),
                  _buildToggleSetting(
                    title: 'Popup Visibility',
                    description: 'Show popup when you press a key',
                    value: popupVisibility,
                    onChanged: (value) {
                      setState(() => popupVisibility = value);
                      _saveSettings();
                    },
                  ),
                  const SizedBox(height: 8),
                  _buildSliderSetting(
                    title: 'Long Press Delay',
                    portraitValue: longPressDelay,
                    onPortraitChanged: (value) =>
                        setState(() => longPressDelay = value),
                    min: 100.0,
                    max: 1000.0,
                    unit: 'ms',
                    portraitLabel: 'Delay',
                    showLandscape: false,
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),
        ],
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
          // Portrait Slider
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
                    style: AppTextStyle.bodySmall.copyWith(
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
}
