import 'package:ai_keyboard/screens/main%20screens/ai_writing_assistance_screen.dart';
import 'package:ai_keyboard/screens/main%20screens/guidance_screen.dart';
import 'package:ai_keyboard/screens/main%20screens/language_screen.dart';
import 'package:ai_keyboard/screens/main%20screens/notification_screen.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/screens/main%20screens/upgrade_pro_screen.dart';
import 'package:flutter/material.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  void _showNotification() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const NotificationScreen()),
    );
  }

  void _showUpgradeProBottomSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        height: MediaQuery.of(context).size.height * 0.95,
        decoration: const BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.only(
            topLeft: Radius.circular(20),
            topRight: Radius.circular(20),
          ),
        ),
        child: const UpgradeProScreen(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final String userName = 'John Doe';
    final String wordcount = '21,485';
    bool hasNotification = true;
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        surfaceTintColor: AppColors.white,
        backgroundColor: AppColors.white,
        leading: Padding(
          padding: const EdgeInsets.only(left: 16),
          child: Image.asset(AppAssets.userIcon),
        ),
        title: Text(userName, style: AppTextStyle.headlineLarge),
        actions: [
          IconButton(
            onPressed: () {
              _showNotification();
            },
            icon: Stack(
              children: [
                const Icon(Icons.notifications_outlined, size: 32),
                if (hasNotification)
                  Positioned(
                    top: 0,
                    right: 0,
                    child: Container(
                      width: 16,
                      height: 16,
                      decoration: BoxDecoration(
                        border: Border.all(color: AppColors.white),
                        color: AppColors.secondary,
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildUpdatePROcARD(context, wordcount),
              SizedBox(height: 24),
              _buildThemes(context),
              SizedBox(height: 24),
              Column(
                spacing: 12,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Our Features', style: TextStyle(fontSize: 16)),
                  _buildtileOption(
                    title: 'AI Suggestions',
                    subtitle: 'Get smart text predictions and corrections',
                    icon: AppIcons.AI_writing_assistant,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => AIWritingAssistanceScreen(),
                        ),
                      );
                    },
                  ),
                  _buildtileOption(
                    title: 'Languages',
                    subtitle: 'Choose your preferred language',
                    icon: AppIcons.languages,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => LanguageScreen(),
                        ),
                      );
                    },
                  ),
                  _buildtileOption(
                    title: 'Guide',
                    subtitle: 'Get a guide to help you use the app',
                    icon: AppIcons.guide,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => GuidanceScreen(),
                        ),
                      );
                    },
                  ),
                  _buildtileOption(
                    title: 'Mail Us',
                    subtitle: 'Send us an email if you have any issues',
                    icon: AppIcons.mail_us_icon,
                    onTap: () {},
                  ),
                  _buildtileOption(
                    title: 'Privacy Policy',
                    subtitle: 'Read our privacy policy',
                    icon: AppIcons.privacy_policy,
                    onTap: () {},
                  ),
                  _buildtileOption(
                    title: 'Terms of Service',
                    subtitle: 'Read our terms of service',
                    icon: AppIcons.terms_of_service,
                    onTap: () {},
                  ),
                  Center(
                    child: Text(
                      'version 1.0.0',
                      style: TextStyle(fontSize: 16, color: AppColors.grey),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  SizedBox _buildThemes(BuildContext context) {
    return SizedBox(
      // height: MediaQuery.of(context).size.height * 0.23,
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Themes', style: TextStyle(fontSize: 16)),
              Text(
                'view All',
                style: TextStyle(color: AppColors.secondary, fontSize: 16),
              ),
            ],
          ),
          SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Expanded(
                child: Container(
                  padding: EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    color: AppColors.grey.withOpacity(0.2),
                  ),
                  height: MediaQuery.of(context).size.height * 0.18,
                  child: Column(
                    children: [
                      Image.asset(Appkeyboards.keyboard_white),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            'Light',
                            style: AppTextStyle.buttonSecondary.copyWith(
                              fontSize: 16,
                            ),
                          ),
                          SizedBox(height: 4),

                          Text(
                            'Free',
                            style: AppTextStyle.buttonSecondary.copyWith(
                              fontSize: 16,
                              color: AppColors.secondary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              SizedBox(width: 16),
              Expanded(
                child: Container(
                  padding: EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.grey,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  height: MediaQuery.of(context).size.height * 0.18,
                  child: Column(
                    children: [
                      Image.asset(Appkeyboards.keyboard_black),
                      SizedBox(height: 4),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            'Dark',
                            style: AppTextStyle.buttonPrimary.copyWith(
                              fontSize: 20,
                            ),
                          ),
                          Text(
                            'Free',
                            style: AppTextStyle.buttonSecondary.copyWith(
                              fontSize: 20,
                              color: AppColors.secondary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildUpdatePROcARD(BuildContext context, String wordcount) {
    return GestureDetector(
      onTap: _showUpgradeProBottomSheet,
      child: Container(
        width: double.infinity,
        height: MediaQuery.of(context).size.height * 0.2,
        decoration: BoxDecoration(
          color: AppColors.primary,
          borderRadius: BorderRadius.circular(10),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(
              child: Container(
                alignment: Alignment.center,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Text(
                      'Free Trial',
                      style: AppTextStyle.bodyLarge.copyWith(
                        color: AppColors.secondary,
                      ),
                    ),
                    Text(
                      '$wordcount Word Left',
                      style: AppTextStyle.headlineSmall.copyWith(
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Align(
              alignment: Alignment.bottomCenter,
              child: Container(
                width: double.infinity,
                height: MediaQuery.of(context).size.height * 0.06,
                decoration: BoxDecoration(
                  color: AppColors.secondary,
                  borderRadius: BorderRadius.only(
                    bottomLeft: Radius.circular(10),
                    bottomRight: Radius.circular(10),
                  ),
                ),
                child: Center(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Image.asset(AppIcons.crown, width: 20, height: 20),
                      SizedBox(width: 8),
                      Text(
                        'upgrade pro',
                        style: AppTextStyle.headlineSmall.copyWith(
                          color: AppColors.white,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _buildtileOption extends StatelessWidget {
  final String title;
  final String subtitle;
  final String icon;
  final VoidCallback onTap;
  const _buildtileOption({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      minTileHeight: 72,
      onTap: onTap,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      tileColor: AppColors.grey.withOpacity(0.2),

      leading: Image.asset(icon, width: 24, height: 28),
      title: Text(title, style: AppTextStyle.headlineSmall),
      subtitle: Text(subtitle),
    );
  }
}
