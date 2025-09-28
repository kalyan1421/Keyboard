import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/custom_toggle_switch.dart';

class ClipboardScreen extends StatefulWidget {
  const ClipboardScreen({super.key});

  @override
  State<ClipboardScreen> createState() => _ClipboardScreenState();
}

class _ClipboardScreenState extends State<ClipboardScreen> {
  // History Settings
  bool clipboardHistory = true;
  double cleanOldHistoryMinutes = 0.0;
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
          'Clipboard',
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

            // History Settings Section
            _buildSectionTitle('History Settings'),
            const SizedBox(height: 16),

            // Clipboard History
            _buildToggleSetting(
              title: 'Clipboard History',
              description: 'Enabled',
              value: clipboardHistory,
              onChanged: (value) => setState(() => clipboardHistory = value),
            ),

            const SizedBox(height: 12),

            // Clean Old History Items
            _buildSliderSetting(
              title: 'Clean Old History Items',
              portraitValue: cleanOldHistoryMinutes,
              onPortraitChanged: (value) =>
                  setState(() => cleanOldHistoryMinutes = value),
              min: 0.0,
              max: 60.0,
              unit: ' min',
              portraitLabel: 'Minutes',
              showLandscape: false,
            ),

            const SizedBox(height: 12),

            // History Size
            _buildSliderSetting(
              title: 'History Size',
              portraitValue: historySize,
              onPortraitChanged: (value) => setState(() => historySize = value),
              min: 5.0,
              max: 100.0,
              unit: ' ',
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
}
