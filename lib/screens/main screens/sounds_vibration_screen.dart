import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/custom_toggle_switch.dart';

class SoundsVibrationScreen extends StatefulWidget {
  const SoundsVibrationScreen({super.key});

  @override
  State<SoundsVibrationScreen> createState() => _SoundsVibrationScreenState();
}

class _SoundsVibrationScreenState extends State<SoundsVibrationScreen> {
  // Sounds Settings
  bool audioFeedback = true;
  double soundVolume = 50.0;
  bool keyPressSounds = true;
  bool longPressKeySounds = true;
  bool repeatedActionKeySounds = true;

  // Haptic feedback & Vibration Settings
  bool hapticFeedback = true;
  String vibrationMode = 'Select display mode';
  double vibrationDuration = 50.0;
  bool keyPressVibration = true;
  bool longPressKeyVibration = true;
  bool repeatedActionKeyVibration = true;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Sounds & Vibration',
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

            // Sounds Settings Section
            _buildSectionTitle('Sounds Settings'),
            const SizedBox(height: 16),

            // Audio feedback
            _buildToggleSetting(
              title: 'Audio feedback',
              description: 'Enabled',
              value: audioFeedback,
              onChanged: (value) => setState(() => audioFeedback = value),
            ),

            const SizedBox(height: 12),

            // Sound volume for input events
            _buildSliderSetting(
              title: 'Sound volume for input events',
              portraitValue: soundVolume,
              onPortraitChanged: (value) => setState(() => soundVolume = value),
              min: 0.0,
              max: 100.0,
              unit: '%',
              portraitLabel: 'Sounds',
              showLandscape: false,
            ),

            const SizedBox(height: 12),

            // Key press sounds
            _buildToggleSetting(
              title: 'Key press sounds',
              description: 'Enabled',
              value: keyPressSounds,
              onChanged: (value) => setState(() => keyPressSounds = value),
            ),

            const SizedBox(height: 12),

            // Long press key sounds
            _buildToggleSetting(
              title: 'Long press key sounds',
              description: 'Enabled',
              value: longPressKeySounds,
              onChanged: (value) => setState(() => longPressKeySounds = value),
            ),

            const SizedBox(height: 12),

            // Repeated action key sounds
            _buildToggleSetting(
              title: 'Repeated action key sounds',
              description: 'Enabled',
              value: repeatedActionKeySounds,
              onChanged: (value) =>
                  setState(() => repeatedActionKeySounds = value),
            ),

            const SizedBox(height: 32),

            // Haptic feedback & Vibration Section
            _buildSectionTitle('Haptic feedback & Vibration'),
            const SizedBox(height: 16),

            // Haptic feedback
            _buildToggleSetting(
              title: 'Haptic feedback',
              description: 'Enabled',
              value: hapticFeedback,
              onChanged: (value) => setState(() => hapticFeedback = value),
            ),

            const SizedBox(height: 12),

            // Vibration mode
            _buildVibrationModeCard(
              title: 'Vibration mode',
              subtitle: vibrationMode,
              onTap: () => _showVibrationModeDialog(),
            ),

            const SizedBox(height: 12),

            // Vibration duration
            _buildSliderSetting(
              title: 'Vibration duration',
              portraitValue: vibrationDuration,
              onPortraitChanged: (value) =>
                  setState(() => vibrationDuration = value),
              min: 10.0,
              max: 200.0,
              unit: 'ms',
              portraitLabel: 'Vibration',
              showLandscape: false,
            ),

            const SizedBox(height: 12),

            // Vibration mode (disabled)
            _buildDisabledVibrationModeCard(
              title: 'Vibration mode',
              description:
                  'Hardware is missing in your device, Need hardware for use features',
            ),

            const SizedBox(height: 12),

            // Key press vibration
            _buildToggleSetting(
              title: 'Key press vibration',
              description: 'Enabled',
              value: keyPressVibration,
              onChanged: (value) => setState(() => keyPressVibration = value),
            ),

            const SizedBox(height: 12),

            // Long press key vibration
            _buildToggleSetting(
              title: 'Long press key vibration',
              description: 'Enabled',
              value: longPressKeyVibration,
              onChanged: (value) =>
                  setState(() => longPressKeyVibration = value),
            ),

            const SizedBox(height: 12),

            // Repeated action key vibration
            _buildToggleSetting(
              title: 'Repeated action key vibration',
              description: 'Enabled',
              value: repeatedActionKeyVibration,
              onChanged: (value) =>
                  setState(() => repeatedActionKeyVibration = value),
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

  Widget _buildVibrationModeCard({
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
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
                    title,
                    style: AppTextStyle.titleLarge.copyWith(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
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

  Widget _buildDisabledVibrationModeCard({
    required String title,
    required String description,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey.withOpacity(0.5),
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
                    color: AppColors.grey,
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
          Icon(Icons.block, color: AppColors.grey, size: 16),
        ],
      ),
    );
  }

  void _showVibrationModeDialog() {
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
                  'Vibration Mode',
                  style: AppTextStyle.titleLarge.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Divider(color: AppColors.lightGrey, thickness: 1),

                // Vibration Mode Options
                _buildVibrationModeOption('Use vibrator directly'),
                const SizedBox(height: 12),
                _buildVibrationModeOption('Use haptic feedback interface'),

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
                    TextButton(
                      onPressed: () {},
                      child: Text(
                        'default',
                        style: AppTextStyle.buttonSecondary.copyWith(
                          fontWeight: FontWeight.w600,
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

  Widget _buildVibrationModeOption(String mode) {
    return GestureDetector(
      onTap: () {
        setState(() {
          vibrationMode = mode;
        });
        Navigator.of(context).pop();
      },
      child: Row(
        children: [
          // Radio Button
          Radio<String>(
            value: mode,
            groupValue: vibrationMode,
            onChanged: (String? value) {
              if (value != null) {
                setState(() {
                  vibrationMode = value;
                });
                Navigator.of(context).pop();
              }
            },
            activeColor: AppColors.secondary,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
          const SizedBox(width: 8),

          // Content
          Expanded(
            child: Text(
              mode,
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
