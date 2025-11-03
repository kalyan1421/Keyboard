import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:file_picker/file_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:convert';
import 'dart:io';
import 'theme_v2.dart';
import 'package:ai_keyboard/screens/main screens/mainscreen.dart';
import 'package:ai_keyboard/screens/main screens/choose_base_theme_screen.dart';
import 'package:ai_keyboard/screens/main screens/button_style_selector_screen.dart'
    as button_styles;
import 'package:ai_keyboard/widgets/font_picker.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/theme/Custom_theme.dart';

/// Theme Gallery Screen - CleverType style theme selection
class ThemeGalleryScreen extends StatefulWidget {
  const ThemeGalleryScreen({super.key});

  @override
  State<ThemeGalleryScreen> createState() => _ThemeGalleryScreenState();
}

class _ThemeGalleryScreenState extends State<ThemeGalleryScreen> {
  String _selectedCategory = 'Popular';
  final Map<String, List<KeyboardThemeV2>> _themesByCategory = KeyboardThemeV2.getThemesByCategory();
  
  // Sample background images (can be replaced with actual image URLs or assets)
  final List<BackgroundImage> _backgroundImages = [
    BackgroundImage(
      id: 'nature_1',
      category: 'Nature',
      imageUrl: 'https://picsum.photos/300/200?random=1',
      icon: Icons.eco,
    ),
    BackgroundImage(
      id: 'nature_2',
      category: 'Nature',
      imageUrl: 'https://picsum.photos/300/200?random=2',
      icon: Icons.eco,
    ),
    BackgroundImage(
      id: 'nature_3',
      category: 'Nature',
      imageUrl: 'https://picsum.photos/300/200?random=3',
      icon: Icons.eco,
    ),
    BackgroundImage(
      id: 'nature_4',
      category: 'Nature',
      imageUrl: 'https://picsum.photos/300/200?random=4',
      icon: Icons.eco,
    ),
    BackgroundImage(
      id: 'abstract_1',
      category: 'Abstract',
      imageUrl: 'https://picsum.photos/300/200?random=5',
      icon: Icons.auto_awesome,
    ),
    BackgroundImage(
      id: 'abstract_2',
      category: 'Abstract',
      imageUrl: 'https://picsum.photos/300/200?random=6',
      icon: Icons.auto_awesome,
    ),
    BackgroundImage(
      id: 'flowers_1',
      category: 'Flowers',
      imageUrl: 'https://picsum.photos/300/200?random=7',
      icon: Icons.local_florist,
    ),
    BackgroundImage(
      id: 'flowers_2',
      category: 'Flowers',
      imageUrl: 'https://picsum.photos/300/200?random=8',
      icon: Icons.local_florist,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: CustomScrollView(
        slivers: [
          // Image Background Selection Section
          SliverToBoxAdapter(
            child: Container(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Upload Photo Button
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: GestureDetector(
              onTap: (){
                Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const 
                  //CustomizeThemeScreen(),
                ThemeEditorScreenV2(isCreatingNew: true),
                ),
              );
              },
              child: Container(
                height: 120,width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.grey[200],
                  borderRadius: BorderRadius.circular(12),
                  
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.start,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                    
                      child: Image.asset('assets/keyboards/custom_left.png'),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            'Customize Theme',
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.bold,
                              fontSize: 24,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            'Create your own themes',
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: Colors.grey[600],
                              fontSize: 16,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      width:  60,
                      height: 60,
                     
                      child: Image.asset('assets/keyboards/custom_right.png'),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          
          
        ],
      ),
    )
          ),
          
          // Category Filters
          SliverToBoxAdapter(
            child: _buildCategoryFilters(),
          ),
          
          // Theme Grid
          SliverPadding(
            padding: const EdgeInsets.all(16),
            sliver: SliverGrid(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 16,
                mainAxisSpacing: 16,
                childAspectRatio: 1.2,
              ),
              delegate: SliverChildBuilderDelegate(
                (context, index) {
                  final theme = _themesByCategory[_selectedCategory]![index];
                  return _buildThemeCard(theme);
                },
                childCount: _themesByCategory[_selectedCategory]?.length ?? 0,
              ),
            ),
          ),
          
          
        ],
      ),
    );
  }
  
  
  Future<void> _uploadCustomImage() async {
    try {
      // Navigate to the new custom image theme flow
      final customTheme = await Navigator.push<KeyboardThemeV2>(
        context,
        MaterialPageRoute(
          builder: (context) => const ChooseBaseThemeScreen(),
        ),
      );
      
      // If theme was created and returned, it's already saved and applied
      if (customTheme != null && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('✅ Custom theme "${customTheme.name}" created successfully!'),
            backgroundColor: Colors.green,
            duration: const Duration(seconds: 2),
          ),
        );
        
        // Refresh the gallery if needed
        setState(() {});
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to create custom theme: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
  
  /// Save image to a location accessible by the keyboard service
  Future<String> _saveImageForKeyboard(File imageFile) async {
    try {
      // Get app's external files directory (accessible by keyboard service)
      final directory = await getExternalStorageDirectory();
      if (directory == null) {
        throw Exception('External storage not available');
      }
      
      // Create themes directory if it doesn't exist
      final themesDir = Directory('${directory.path}/keyboard_themes');
      if (!await themesDir.exists()) {
        await themesDir.create(recursive: true);
      }
      
      // Generate unique filename
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final extension = imageFile.path.split('.').last;
      final targetPath = '${themesDir.path}/bg_$timestamp.$extension';
      
      // Copy file
      await imageFile.copy(targetPath);
      
      return targetPath;
    } catch (e) {
      // Fallback: return original path
      return imageFile.path;
    }
  }
  
  Widget _buildImageCard(BackgroundImage image) {
    return GestureDetector(
      onTap: () => _applyImageBackground(image),
      child: Container(
        width: 160,
        margin: const EdgeInsets.only(right: 12),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.1),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: Stack(
            fit: StackFit.expand,
            children: [
              Image.network(
                image.imageUrl,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) {
                  return Container(
                    color: Colors.grey[300],
                    child: const Center(
                      child: Icon(Icons.image, size: 40, color: Colors.grey),
                    ),
                  );
                },
                loadingBuilder: (context, child, loadingProgress) {
                  if (loadingProgress == null) return child;
                  return Container(
                    color: Colors.grey[200],
                    child: const Center(
                      child: CircularProgressIndicator(),
                    ),
                  );
                },
              ),
              // Gradient overlay for better visibility
              Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      Colors.black.withOpacity(0.3),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
  
  Widget _buildCategoryFilters() {
    return Container(
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: _themesByCategory.keys.map((category) {
          final isSelected = category == _selectedCategory;
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: FilterChip(
              label: Text(category),
              selected: isSelected,
              selectedColor: Theme.of(context).primaryColor.withOpacity(0.2),
              onSelected: (selected) {
                if (selected) {
                  setState(() {
                    _selectedCategory = category;
                  });
                }
              },
            ),
          );
        }).toList(),
      ),
    );
  }
  
  Widget _buildTryThemeSection() {
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.grey[100],
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          Text(
            'Try your theme here',
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
              color: Colors.grey[600],
            ),
          ),
          const SizedBox(height: 12),
          // Keyboard preview placeholder
          Container(
            height: 200,
            decoration: BoxDecoration(
              color: Colors.grey[300],
              borderRadius: BorderRadius.circular(12),
            ),
            child: Center(
              child: Icon(
                Icons.keyboard,
                size: 60,
                color: Colors.grey[500],
              ),
            ),
          ),
        ],
      ),
    );
  }
  
  Future<void> _applyImageBackground(BackgroundImage image) async {
    try {
      // Create a theme with the selected image as background
      final imageTheme = KeyboardThemeV2.createPictureTheme().copyWith(
        id: 'custom_image_${image.id}',
        name: '${image.category} Theme',
        background: ThemeBackground(
          type: 'image',
          color: Colors.transparent,
          imagePath: image.imageUrl,
          imageOpacity: 0.85,
          gradient: null,
          overlayEffects: const [],
          adaptive: null,
        ),
      );
      
      await _applyTheme(imageTheme);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to apply image: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Widget _buildThemeCard(KeyboardThemeV2 theme) {
    return Card(
      elevation: 4,
      child: InkWell(
        onTap: () => _applyTheme(theme),
        child: Column(
          children: [
            // Theme Preview
            Expanded(
              child: Container(
                width: double.infinity,
                decoration: BoxDecoration(
                  color: theme.background.type == 'gradient' 
                    ? theme.background.gradient!.colors.first 
                    : theme.background.color,
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
                ),
                child: Stack(
                  children: [
                    // Background representation
                    if (theme.background.type == 'gradient')
                      Container(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: theme.background.gradient!.colors,
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                          ),
                          borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
                        ),
                      ),
                    // Key preview
                    Center(
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          _buildMiniKey(theme, false),
                          const SizedBox(width: 4),
                          _buildMiniKey(theme, false),
                          const SizedBox(width: 4),
                          _buildMiniKey(theme, true), // Accent key
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            // Theme Info
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                children: [
                  Text(
                    theme.name,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    theme.background.type.toUpperCase(),
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Colors.grey[600],
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMiniKey(KeyboardThemeV2 theme, bool isAccent) {
    return Container(
      width: 24,
      height: 24,
      decoration: BoxDecoration(
        color: isAccent ? theme.specialKeys.accent : theme.keys.bg,
        borderRadius: BorderRadius.circular(theme.keys.radius / 2),
        border: theme.keys.border.enabled 
          ? Border.all(color: Color(theme.keys.border.color.value), width: 0.5)
          : null,
      ),
    );
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
