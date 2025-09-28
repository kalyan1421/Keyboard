import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';

class AIWritingAssistanceScreen extends StatefulWidget {
  const AIWritingAssistanceScreen({Key? key}) : super(key: key);

  @override
  State<AIWritingAssistanceScreen> createState() =>
      _AIWritingAssistanceScreenState();
}

class _AIWritingAssistanceScreenState extends State<AIWritingAssistanceScreen> {
  // Track active features
  final Set<String> _activeFeatures = {'humanise', 'reply'};

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        backgroundColor: AppColors.primary,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Ai Writing Assistance',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),
        centerTitle: true,
        actions: [
          Stack(
            children: [
              IconButton(
                icon: const Icon(
                  Icons.notifications_outlined,
                  color: AppColors.white,
                ),
                onPressed: () {},
              ),
              Positioned(
                right: 8,
                top: 8,
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
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Active Section
            _buildSectionHeader('Active'),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'humanise',
              icon: AppIcons.humanise_icon,
              title: 'Humanise',
              description: 'Talk like human',
              isActive: true,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'reply',
              icon: AppIcons.spell_check_icon,
              title: 'Reply',
              description: 'How to use auto-correction?',
              isActive: true,
            ),

            const SizedBox(height: 32),

            // Deactivated Section
            _buildSectionHeader('Deactivated'),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'continue_writing',
              icon: AppIcons.humanise_icon,
              title: 'Continue Writing',
              description: 'Talk like human',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'facebook_post',
              icon: AppIcons.spell_check_icon,
              title: 'Facebook Post',
              description: 'How to use auto-correction?',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'instagram_caption',
              icon: AppIcons.report_icon,
              title: 'Instagram Caption',
              description: 'How to use Ai Rewriting',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'phrase_to_emoji',
              icon: AppIcons.report_icon,
              title: 'Phrase to Emoji',
              description: 'How to set up Keyboard?',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'summary',
              icon: AppIcons.report_icon,
              title: 'Summary',
              description: 'How to set up Keyboard?',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'essay',
              icon: AppIcons.report_icon,
              title: 'Essay',
              description: 'How to set up Keyboard?',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'text_to_tweet',
              icon: AppIcons.report_icon,
              title: 'Text to Tweet',
              description: 'How to set up Keyboard?',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'study_notes',
              icon: AppIcons.report_icon,
              title: 'Study Notes',
              description: 'How to set up Keyboard?',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'emojify',
              icon: AppIcons.report_icon,
              title: 'Emojify',
              description: 'How to set up Keyboard?',
              isActive: false,
            ),
            const SizedBox(height: 12),
            _buildFeatureCard(
              id: 'pickup_line',
              icon: AppIcons.report_icon,
              title: 'Pickup Line',
              description: 'How to set up Keyboard?',
              isActive: false,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Text(
      title,
      style: AppTextStyle.titleLarge.copyWith(
        color: AppColors.secondary,
        fontWeight: FontWeight.bold,
      ),
    );
  }

  Widget _buildFeatureCard({
    required String id,
    required String icon,
    required String title,
    required String description,
    required bool isActive,
  }) {
    final bool isCurrentlyActive = _activeFeatures.contains(id);

    return GestureDetector(
      onTap: () => _toggleFeature(id),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.lightGrey,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            // Icon
            Image.asset(icon, height: 24, width: 24),
            const SizedBox(width: 16),

            // Title and Description
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: AppTextStyle.titleMedium.copyWith(
                      fontWeight: FontWeight.bold,
                      color: AppColors.black,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    description,
                    style: AppTextStyle.bodyMedium.copyWith(
                      color: AppColors.grey,
                    ),
                  ),
                ],
              ),
            ),

            // Action Indicator
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                color: isCurrentlyActive
                    ? AppColors.secondary
                    : Colors.transparent,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Icon(
                isCurrentlyActive ? Icons.check : Icons.add,
                color: isCurrentlyActive
                    ? AppColors.white
                    : AppColors.secondary,
                size: isCurrentlyActive ? 20 : 32,
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _toggleFeature(String featureId) {
    setState(() {
      if (_activeFeatures.contains(featureId)) {
        _activeFeatures.remove(featureId);
      } else {
        _activeFeatures.add(featureId);
      }
    });
  }
}
