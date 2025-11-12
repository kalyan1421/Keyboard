import 'package:flutter/material.dart';
import 'theme_v2.dart';

import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/screens/main screens/mainscreen.dart';
import 'package:ai_keyboard/theme/Custom_theme.dart';
import 'package:ai_keyboard/theme/view_all_themes_screen.dart';

/// Theme Gallery Screen - CleverType style theme selection
class ThemeGalleryScreen extends StatefulWidget {
  const ThemeGalleryScreen({super.key});

  @override
  State<ThemeGalleryScreen> createState() => _ThemeGalleryScreenState();
}

class _ThemeGalleryScreenState extends State<ThemeGalleryScreen> {
  final Map<String, List<KeyboardThemeV2>> _themesByCategory = KeyboardThemeV2.getThemesByCategory();
  final Set<String> _ownedThemes = {'theme_white', 'theme_dark'}; // User's owned themes

  @override
  void initState() {
    super.initState();
    _loadOwnedThemes();
  }

  Future<void> _loadOwnedThemes() async {
    // TODO: Load owned themes from SharedPreferences
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            
            
            // Customize Theme Card
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: GestureDetector(
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => const ThemeEditorScreenV2(isCreatingNew: true),
                      ),
                    );
                  },
                  child: Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF0F0F0),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(Icons.palette, color: Colors.orange, size: 28),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Customize Theme',
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.black87,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                'Create your own themes',
                                style: TextStyle(
                                  fontSize: 14,
                                  color: Colors.grey[600],
                                ),
                              ),
                            ],
                          ),
                        ),
                        Image.asset('assets/keyboards/custom_right.png', width: 50, height: 50),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            
            // Popular Section
            _buildCategorySection('Popular', ['theme_white', 'theme_dark']),
            
            // Colour Section
            _buildCategorySection('Colour', ['theme_yellow', 'theme_red']),
            
            // // Gradients Section
            // _buildCategorySection('Gradients', ['theme_gradient', 'theme_galaxy']),
            
            // // Picture Section
            // _buildCategorySection('Picture', ['theme_picture', 'theme_picture']),
            
            const SliverToBoxAdapter(child: SizedBox(height: 32)),
          ],
        ),
      ),
    );
  }
  
  Widget _buildCategorySection(String categoryName, List<String> themeIds) {
    final themes = themeIds.map((id) {
      return KeyboardThemeV2.getPresetThemes().firstWhere(
        (theme) => theme.id == id,
        orElse: () => KeyboardThemeV2.createDefault(),
      );
    }).toList();

    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.only(left: 16, right: 16, top: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Section header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  categoryName,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                TextButton(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => ViewAllThemesScreen(category: categoryName),
                      ),
                    );
                    // TODO: Navigate to category page
                  },
                  child: const Text(
                    'See All',
                    style: TextStyle(
                      color: Colors.orange,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            // Theme cards in row
            Row(
              children: themes.take(2).map((theme) {
                return Expanded(
                  child: Padding(
                    padding: EdgeInsets.only(
                      right: theme == themes.first ? 8 : 0,
                      left: theme == themes.last && themes.length > 1 ? 8 : 0,
                    ),
                    child: _buildThemeCard(theme),
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildThemeCard(KeyboardThemeV2 theme) {
    final isOwned = _ownedThemes.contains(theme.id);
    
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => _applyTheme(theme),
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: [
              theme.background.color ?? Colors.grey[300]!,
              (theme.background.color ?? Colors.grey[300]!).withOpacity(0.8),
            ],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.1),
              blurRadius: 8,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
        //    mainAxisSize: MainAxisSize.min,
            children: [
              // Keyboard preview using static images
              Container(
                height: 100,
                decoration: BoxDecoration(borderRadius: BorderRadius.circular(6)),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(6),
                  child: _getKeyboardPreview(theme.id),
                ),
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      theme.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                    decoration: BoxDecoration(
                      color: isOwned ? Colors.grey[600] : Colors.green,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                       'Free',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
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

  // Helper function to get keyboard preview based on theme ID
  Widget _getKeyboardPreview(String themeId) {
    switch (themeId) {
      case 'theme_white':
        return Image.asset(
          Appkeyboards.keyboard_white,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        );
      case 'theme_dark':
        return Image.asset(
          Appkeyboards.keyboard_black,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        );
      case 'theme_yellow':
        return Image.asset(
          Appkeyboards.keyboard_yellow,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        );
      case 'theme_red':
        return Image.asset(
          Appkeyboards.keyboard_red,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        );
      case 'theme_blue':
        return Image.asset(
          Appkeyboards.keyboard_blue,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        );
      default:
        // Default fallback for themes without specific images
        return Image.asset(
          Appkeyboards.keyboard_white,
          fit: BoxFit.contain,
          width: double.infinity,
          height: double.infinity,
        );
    }
  }

  Future<void> _applyTheme(KeyboardThemeV2 theme) async {
    try {
      await ThemeManagerV2.saveThemeV2(theme);
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Applied theme: ${theme.name}'),
            backgroundColor: theme.specialKeys.accent,
            duration: const Duration(seconds: 2),
          ),
        );
        // Navigate back to home screen instead of just popping
        // This prevents black screen issues
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(builder: (context) => const mainscreen()),
          (route) => false,
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to apply theme: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }
}
