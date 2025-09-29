import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'emoji_skin_tone_screen.dart';

class EmojiSettingsScreen extends StatefulWidget {
  const EmojiSettingsScreen({super.key});

  @override
  State<EmojiSettingsScreen> createState() => _EmojiSettingsScreenState();
}

class _EmojiSettingsScreenState extends State<EmojiSettingsScreen> {
  double emojiHistoryMaxSize = 90.0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Emojis',
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
            // Section Header
            _buildSectionHeader('Emojis'),
            const SizedBox(height: 16),

            // Preferred Emoji Skin Tone Setting
            _buildTappableSetting(
              title: 'Preferred emoji skin tone',
              description: 'Choose emoji skin tone',
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const EmojiSkinToneScreen(),
                ),
              ),
            ),
            const SizedBox(height: 12),

            // Emoji History Max Size Setting
            _buildSliderSetting(
              title: 'Emoji history max size',
              value: emojiHistoryMaxSize,
              onChanged: (value) => setState(() => emojiHistoryMaxSize = value),
              min: 10.0,
              max: 100.0,
              unit: 'items',
            ),
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

  Widget _buildTappableSetting({
    required String title,
    required String description,
    required VoidCallback onTap,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: AppTextStyle.titleMedium.copyWith(
                color: AppColors.primary,
                fontWeight: FontWeight.w600,
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
    );
  }

  Widget _buildSliderSetting({
    required String title,
    required double value,
    required ValueChanged<double> onChanged,
    required double min,
    required double max,
    required String unit,
  }) {
    return Container(
      width: double.infinity,
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
            style: AppTextStyle.titleMedium.copyWith(
              color: AppColors.primary,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Text(
                unit,
                style: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Slider(
                  value: value,
                  min: min,
                  max: max,
                  onChanged: onChanged,
                  activeColor: AppColors.secondary,
                  inactiveColor: AppColors.white,
                  thumbColor: AppColors.white,
                ),
              ),
              const SizedBox(width: 16),
              Text(
                '${value.toInt()}',
                style: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
