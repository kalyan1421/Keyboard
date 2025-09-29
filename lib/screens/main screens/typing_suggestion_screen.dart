import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/custom_toggle_switch.dart';

class TypingSuggestionScreen extends StatefulWidget {
  const TypingSuggestionScreen({super.key});

  @override
  State<TypingSuggestionScreen> createState() => _TypingSuggestionScreenState();
}

class _TypingSuggestionScreenState extends State<TypingSuggestionScreen> {
  // Suggestion Settings
  bool displaySuggestions = true;
  String displayMode = 'Select display mode';
  double historySize = 20.0;
  bool clearPrimaryClipAffects = true;

  // Internal Settings
  bool internalClipboard = true;
  bool syncFromSystem = true;
  bool syncToFivive = true;

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
              description: 'Enabled',
              value: displaySuggestions,
              onChanged: (value) => setState(() => displaySuggestions = value),
            ),

            const SizedBox(height: 12),

            // Display mode
            _buildDisplayModeCard(),

            const SizedBox(height: 12),

            // History Size
            _buildSliderSetting(
              title: 'History Size',
              portraitValue: historySize,
              onPortraitChanged: (value) => setState(() => historySize = value),
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
              description: 'Enabled',
              value: clearPrimaryClipAffects,
              onChanged: (value) =>
                  setState(() => clearPrimaryClipAffects = value),
            ),

            const SizedBox(height: 32),

            // Internal Settings Section
            _buildSectionTitle('Internal Settings'),
            const SizedBox(height: 16),

            // Internal Clipboard
            _buildToggleSetting(
              title: 'Internal Clipboard',
              description: 'Enabled',
              value: internalClipboard,
              onChanged: (value) => setState(() => internalClipboard = value),
            ),

            const SizedBox(height: 12),

            // Sync from system
            _buildToggleSetting(
              title: 'Sync from system',
              description: 'Sync from system clipboard',
              value: syncFromSystem,
              onChanged: (value) => setState(() => syncFromSystem = value),
            ),

            const SizedBox(height: 12),

            // Sync to fivive
            _buildToggleSetting(
              title: 'Sync to fivive',
              description: 'Sync to fivive clipboard',
              value: syncToFivive,
              onChanged: (value) => setState(() => syncToFivive = value),
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
    return GestureDetector(
      onTap: () {
        // TODO: Navigate to display mode selection screen
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
                    displayMode,
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
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
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
                Divider(color: AppColors.lightGrey, thickness: 1),
                // Mode Options
                _buildSimpleModeOption('Classic (3 columns)'),
                const SizedBox(height: 12),
                _buildSimpleModeOption('4 Columns'),
                const SizedBox(height: 12),
                _buildSimpleModeOption('Dynamic width'),
                const SizedBox(height: 12),
                _buildSimpleModeOption('Dynamic width & scrollable'),

                const SizedBox(height: 20),

                // Apply Button
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.of(context).pop(),
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
                          'Cancel',
                          style: AppTextStyle.buttonSecondary.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    Expanded(
                      child: TextButton(
                        onPressed: () {
                          setState(() {
                            displayMode = 'Classic (3 columns)';
                          });
                          Navigator.of(context).pop();
                        },
                        child: Text(
                          'default',
                          style: AppTextStyle.buttonSecondary.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.of(context).pop(),
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
                          'OK',
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
  }

  Widget _buildSimpleModeOption(String title) {
    bool isSelected = displayMode == title;

    return GestureDetector(
      onTap: () {
        setState(() {
          displayMode = title;
        });
      },
      child: Row(
        children: [
          // Radio Button
          Radio<String>(
            value: title,
            groupValue: displayMode,
            onChanged: (String? value) {
              if (value != null) {
                setState(() {
                  displayMode = value;
                });
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
                color: AppColors.primary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
