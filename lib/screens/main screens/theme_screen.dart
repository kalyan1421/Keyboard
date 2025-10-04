import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/screens/main screens/view_all_themes_screen.dart';
import 'package:ai_keyboard/screens/main screens/customize_theme_screen.dart';

class ThemeScreen extends StatefulWidget {
  const ThemeScreen({super.key});

  @override
  State<ThemeScreen> createState() => _ThemeScreenState();
}

class _ThemeScreenState extends State<ThemeScreen> {
  String selectedTheme = 'White';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      body: Column(
        children: [
          // Main content
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Customize Theme Card
                  _buildCustomizeThemeCard(),

                  const SizedBox(height: 24),

                  // Popular Section
                  _buildSectionHeader('Popular', 'See All'),
                  const SizedBox(height: 12),
                  _buildPopularSection(),

                  const SizedBox(height: 24),

                  // Colour Section
                  _buildSectionHeader('Colour', 'See All'),
                  const SizedBox(height: 12),
                  _buildColourSection(),

                  const SizedBox(height: 24),

                  // Gradients Section
                  _buildSectionHeader('Gradients', 'See All'),
                  const SizedBox(height: 12),
                  _buildGradientsSection(),

                  const SizedBox(height: 24),

                  // Picture Section
                  _buildSectionHeader('Picture', 'See All'),
                  const SizedBox(height: 12),
                  _buildPictureSection(),

                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCustomizeThemeCard() {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => const CustomizeThemeScreen()),
        );
      },
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.lightGrey,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            // Customization Icon
            Container(
              width: 24,
              height: 24,
              decoration: BoxDecoration(
                color: AppColors.white,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Image.asset(
                'assets/icons/custom_theme_icon.png',
                width: 20,
                height: 20,
              ),
            ),
            const SizedBox(width: 16),

            // Text Content
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Customize Theme',
                    style: AppTextStyle.titleLarge.copyWith(
                      color: AppColors.black,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Create your own themes',
                    style: AppTextStyle.bodySmall.copyWith(
                      color: AppColors.grey,
                    ),
                  ),
                ],
              ),
            ),

            // Theme Creation Illustration
            Container(
              width: 60,
              height: 40,
              decoration: BoxDecoration(
                // color: AppColors.black,
                borderRadius: BorderRadius.circular(6),
              ),
              child: Image.asset(
                'assets/icons/custom_theme_illustration.png',
                width: 20,
                height: 20,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title, String seeAll) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          title,
          style: AppTextStyle.titleMedium.copyWith(
            color: AppColors.secondary,
            fontWeight: FontWeight.w600,
          ),
        ),
        GestureDetector(
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => ViewAllThemesScreen(category: title),
              ),
            );
          },
          child: Text(
            seeAll,
            style: AppTextStyle.bodyMedium.copyWith(
              color: AppColors.secondary,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPopularSection() {
    return Row(
      children: [
        Expanded(
          child: _buildThemeCard(
            'White',
            'Owned',
            _buildWhiteKeyboard(),
            isSelected: selectedTheme == 'White',
            onTap: () => setState(() => selectedTheme = 'White'),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildThemeCard(
            'Dark',
            'Owned',
            _buildDarkKeyboard(),
            isSelected: selectedTheme == 'Dark',
            onTap: () => setState(() => selectedTheme = 'Dark'),
          ),
        ),
      ],
    );
  }

  Widget _buildColourSection() {
    return Row(
      children: [
        Expanded(
          child: _buildThemeCard(
            'Yellow',
            'Free',
            _buildYellowKeyboard(),
            isSelected: selectedTheme == 'Yellow',
            onTap: () => setState(() => selectedTheme = 'Yellow'),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildThemeCard(
            'Red',
            'Free',
            _buildRedKeyboard(),
            isSelected: selectedTheme == 'Red',
            onTap: () => setState(() => selectedTheme = 'Red'),
          ),
        ),
      ],
    );
  }

  Widget _buildGradientsSection() {
    return Row(
      children: [
        Expanded(
          child: _buildThemeCard(
            'White',
            'Free',
            _buildPurpleOrangeGradient(),
            isSelected: selectedTheme == 'PurpleOrange',
            onTap: () => setState(() => selectedTheme = 'PurpleOrange'),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildThemeCard(
            'White',
            'Free',
            _buildBlueGradient(),
            isSelected: selectedTheme == 'BlueGradient',
            onTap: () => setState(() => selectedTheme = 'BlueGradient'),
          ),
        ),
      ],
    );
  }

  Widget _buildPictureSection() {
    return Row(
      children: [
        Expanded(
          child: _buildThemeCard(
            'White',
            'Free',
            _buildHeartsTheme(),
            isSelected: selectedTheme == 'Hearts',
            onTap: () => setState(() => selectedTheme = 'Hearts'),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildThemeCard(
            'White',
            'Free',
            _buildMossyTheme(),
            isSelected: selectedTheme == 'Mossy',
            onTap: () => setState(() => selectedTheme = 'Mossy'),
          ),
        ),
      ],
    );
  }

  Widget _buildThemeCard(
    String title,
    String status,
    Widget preview, {
    bool isSelected = false,
    VoidCallback? onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 180,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: AppColors.lightGrey,
          borderRadius: BorderRadius.circular(12),
          border: isSelected
              ? Border.all(color: AppColors.secondary, width: 2)
              : null,
        ),
        child: Column(
          children: [
            // Preview
            SizedBox(height: 120, child: preview),
            const SizedBox(height: 8),

            // Title and Status
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  title,
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                Text(
                  status,
                  style: AppTextStyle.bodySmall.copyWith(
                    color: AppColors.secondary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildWhiteKeyboard() {
    return Container(
      height: 100,
      decoration: BoxDecoration(borderRadius: BorderRadius.circular(6)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(6),
        child: Image.asset(
          Appkeyboards.keyboard_white,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        ),
      ),
    );
  }

  Widget _buildDarkKeyboard() {
    return Container(
      decoration: BoxDecoration(borderRadius: BorderRadius.circular(6)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(6),
        child: Image.asset(
          Appkeyboards.keyboard_black,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        ),
      ),
    );
  }

  Widget _buildYellowKeyboard() {
    return Container(
      decoration: BoxDecoration(borderRadius: BorderRadius.circular(6)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(6),
        child: Image.asset(
          Appkeyboards.keyboard_yellow,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        ),
      ),
    );
  }

  Widget _buildRedKeyboard() {
    return Container(
      decoration: BoxDecoration(borderRadius: BorderRadius.circular(6)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(6),
        child: Image.asset(
          Appkeyboards.keyboard_red,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        ),
      ),
    );
  }

  Widget _buildPurpleOrangeGradient() {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.purple[800]!, Colors.orange[400]!],
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
        ),
        borderRadius: BorderRadius.circular(6),
      ),
    );
  }

  Widget _buildBlueGradient() {
    return Stack(
      children: [
        Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [Colors.blue[200]!, Colors.blue[800]!],
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
            ),
            borderRadius: BorderRadius.circular(6),
          ),
        ),
        Positioned(
          top: 8,
          right: 8,
          child: Icon(Icons.keyboard, color: AppColors.secondary, size: 16),
        ),
      ],
    );
  }

  Widget _buildHeartsTheme() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.pink[100],
        borderRadius: BorderRadius.circular(6),
      ),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.favorite, color: Colors.pink[300], size: 20),
            Icon(Icons.park, color: Colors.green[400], size: 16),
          ],
        ),
      ),
    );
  }

  Widget _buildMossyTheme() {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            Colors.green[200]!,
            Colors.green[600]!,
            Colors.red[300]!,
            Colors.orange[300]!,
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(6),
      ),
    );
  }
}
