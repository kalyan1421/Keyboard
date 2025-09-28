import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';

class LanguageScreen extends StatefulWidget {
  const LanguageScreen({Key? key}) : super(key: key);

  @override
  State<LanguageScreen> createState() => _LanguageScreenState();
}

class _LanguageScreenState extends State<LanguageScreen> {
  // Track selected languages
  Set<String> selectedLanguages = {'English', 'Hindi', 'Hinglish'};

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        toolbarHeight: 70,
        backgroundColor: AppColors.primary,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Language',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),

        actions: [
          IconButton(
            icon: const Icon(
              Icons.notifications_outlined,
              color: AppColors.white,
            ),
            onPressed: () {},
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Language Information Card
            _buildLanguageInfoCard(),

            const SizedBox(height: 24),

            // Selected Languages Section
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Text(
                'Selected Language',
                style: AppTextStyle.headlineSmall,
              ),
            ),
            const SizedBox(height: 16),

            // Selected Languages List
            ...selectedLanguages.map(
              (language) => _buildSelectedLanguageTile(language),
            ),

            const SizedBox(height: 24),

            // Available Languages Section
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Text(
                'Available Languages',
                style: AppTextStyle.headlineSmall,
              ),
            ),
            const SizedBox(height: 16),

            // Available Languages List
            ..._getAvailableLanguages().map(
              (language) => _buildAvailableLanguageTile(language),
            ),

            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildLanguageInfoCard() {
    return Container(
      margin: const EdgeInsets.all(24),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          // Left language icon
          Image.asset(AppIcons.languages, width: 24, height: 24),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Language', style: AppTextStyle.headlineSmall),
                const SizedBox(height: 4),
                Text(
                  'Select multiple languages',
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.grey,
                  ),
                ),
              ],
            ),
          ),
          // Right language icon
          Image.asset(AppAssets.languages_image, width: 40, height: 40),
        ],
      ),
    );
  }

  Widget _buildSelectedLanguageTile(String language) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      child: _buildTileOption(
        title: language,
        subtitle: _getLanguageSubtitle(language),
        icon: Icons.check_box,
        isSelected: true,
        onTap: () {
          setState(() {
            selectedLanguages.remove(language);
          });
        },
      ),
    );
  }

  Widget _buildAvailableLanguageTile(Map<String, dynamic> language) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      child: _buildTileOption(
        title: language['name'],
        subtitle: language['subtitle'],
        icon: Icons.add_circle_outline,
        isDownloadable: true,
        onTap: () {
          setState(() {
            selectedLanguages.add(language['name']);
          });
        },
      ),
    );
  }

  String _getLanguageSubtitle(String language) {
    switch (language) {
      case 'English':
        return 'QWERTY';
      case 'Hindi':
        return 'Hindi';
      case 'Hinglish':
        return 'QWERTY';
      default:
        return 'QWERTY';
    }
  }

  List<Map<String, dynamic>> _getAvailableLanguages() {
    return [
      {'name': 'Gujarati', 'subtitle': 'Gujarati'},
      {'name': 'Gujarati/ગુજરાતી', 'subtitle': "A'n આ"},
      {'name': 'English (India)', 'subtitle': 'QWERTY'},
      {'name': 'Bengali', 'subtitle': 'QWERTY'},
      {'name': 'Telugu', 'subtitle': 'QWERTY'},
      {'name': 'Marathi', 'subtitle': 'QWERTY'},
      {'name': 'Tamil', 'subtitle': 'QWERTY'},
      {'name': 'Urdu', 'subtitle': 'QWERTY'},
      {'name': 'Kannada', 'subtitle': 'QWERTY'},
      {'name': 'Punjabi', 'subtitle': 'QWERTY'},
      {'name': 'Nepali', 'subtitle': 'QWERTY'},
    ];
  }
}

class _buildTileOption extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final VoidCallback onTap;
  final bool isSelected;
  final bool isDownloadable;

  const _buildTileOption({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.onTap,
    this.isSelected = false,
    this.isDownloadable = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
        minTileHeight: 72,
        onTap: onTap,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        leading: _buildLeadingIcon(),
        title: Text(title, style: AppTextStyle.headlineSmall),
        subtitle: Text(
          subtitle,
          style: AppTextStyle.bodyMedium.copyWith(color: AppColors.grey),
        ),
        trailing: isDownloadable
            ? Image.asset(AppIcons.download_button, width: 24, height: 24)
            : null,
      ),
    );
  }

  Widget _buildLeadingIcon() {
    if (isSelected) {
      return Container(
        width: 24,
        height: 24,
        decoration: BoxDecoration(
          color: AppColors.secondary,
          borderRadius: BorderRadius.circular(4),
        ),
        child: const Icon(Icons.check, color: AppColors.white, size: 16),
      );
    } else if (isDownloadable) {
      return Container(
        width: 24,
        height: 24,
        decoration: BoxDecoration(
          color: AppColors.secondary,
          borderRadius: BorderRadius.circular(12),
        ),
        child: const Icon(Icons.add, color: AppColors.white, size: 16),
      );
    } else {
      return Icon(icon, color: AppColors.grey, size: 24);
    }
  }
}
