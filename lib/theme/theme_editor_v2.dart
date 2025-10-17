import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:file_picker/file_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:convert';
import 'dart:io';
import 'theme_v2.dart';

import 'package:ai_keyboard/screens/main screens/mainscreen.dart';

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
      appBar: AppBar(
        title: const Text('Themes'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const ThemeEditorScreenV2(isCreatingNew: true),
                ),
              );
            },
            tooltip: 'Theme Settings',
          ),
        ],
      ),
      body: CustomScrollView(
        slivers: [
          // Image Background Selection Section
          SliverToBoxAdapter(
            child: _buildImageBackgroundSection(),
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
          
          // Try your theme here section (optional)
          SliverToBoxAdapter(
            child: _buildTryThemeSection(),
          ),
        ],
      ),
    );
  }
  
  Widget _buildImageBackgroundSection() {
    // Group images by category
    final Map<String, List<BackgroundImage>> imagesByCategory = {};
    for (var image in _backgroundImages) {
      imagesByCategory.putIfAbsent(image.category, () => []).add(image);
    }
    
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Upload Photo Button
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: GestureDetector(
              onTap: _uploadCustomImage,
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Theme.of(context).primaryColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: Theme.of(context).primaryColor,
                    width: 2,
                    style: BorderStyle.solid,
                  ),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: Theme.of(context).primaryColor,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(
                        Icons.add_photo_alternate,
                        color: Colors.white,
                        size: 24,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Upload Photo',
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            'Add your own image as keyboard background',
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: Colors.grey[600],
                            ),
                          ),
                        ],
                      ),
                    ),
                    Icon(
                      Icons.arrow_forward_ios,
                      color: Theme.of(context).primaryColor,
                      size: 16,
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          
          // Predefined images by category
          ...imagesByCategory.entries.map((entry) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Category Header
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: Row(
                    children: [
                      Icon(
                        entry.value.first.icon,
                        color: Colors.green,
                        size: 24,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        entry.key,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
                
                // Horizontal Image Grid
                SizedBox(
                  height: 120,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: entry.value.length,
                    itemBuilder: (context, index) {
                      final image = entry.value[index];
                      return _buildImageCard(image);
                    },
                  ),
                ),
                const SizedBox(height: 16),
              ],
            );
          }),
        ],
      ),
    );
  }
  
  Future<void> _uploadCustomImage() async {
    try {
      final ImagePicker picker = ImagePicker();
      final XFile? image = await picker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 1920,
        maxHeight: 1080,
        imageQuality: 90,
      );
      
      if (image != null) {
        // Copy image to app directory so keyboard service can access it
        final String savedPath = await _saveImageForKeyboard(File(image.path));
        
        // Create a theme with the selected image
        final customImageTheme = KeyboardThemeV2.createPictureTheme().copyWith(
          id: 'custom_upload_${DateTime.now().millisecondsSinceEpoch}',
          name: 'Custom Image Theme',
          background: ThemeBackground(
            type: 'image',
            color: const Color(0xFF000000),
            imagePath: savedPath,
            imageOpacity: 0.85,
            gradient: null,
            overlayEffects: const [],
            adaptive: null,
          ),
        );
        
        await _applyTheme(customImageTheme);
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Custom image applied successfully!'),
              backgroundColor: Colors.green,
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to upload image: $e'),
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
          color: const Color(0xFF000000),
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

/// CleverType Theme Editor V2
/// Complete theme editing experience with live preview and all V2 features
class ThemeEditorScreenV2 extends StatefulWidget {
  final KeyboardThemeV2? initialTheme;
  final bool isCreatingNew;

  const ThemeEditorScreenV2({
    super.key,
    this.initialTheme,
    this.isCreatingNew = false,
  });

  @override
  State<ThemeEditorScreenV2> createState() => _ThemeEditorScreenV2State();
}

class _ThemeEditorScreenV2State extends State<ThemeEditorScreenV2>
    with TickerProviderStateMixin {
  late TabController _tabController;
  late KeyboardThemeV2 _currentTheme;
  final _nameController = TextEditingController();
  
  // Animation controllers for live preview
  late AnimationController _previewController;
  late Animation<double> _previewAnimation;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    
    // Initialize with provided theme or create new one
    _currentTheme = widget.initialTheme ?? KeyboardThemeV2.createDefault();
    _nameController.text = _currentTheme.name;

    // Setup preview animation
    _previewController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _previewAnimation = CurvedAnimation(
      parent: _previewController,
      curve: Curves.easeInOut,
    );
    
    _previewController.forward();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _nameController.dispose();
    _previewController.dispose();
    super.dispose();
  }

  Future<void> _saveTheme() async {
    if (_nameController.text.trim().isEmpty) {
      _showError('Theme name cannot be empty');
      return;
    }

    // Force toolbar and suggestions to inherit from keys (CleverType style)
    final updatedTheme = _currentTheme.copyWith(
      name: _nameController.text.trim(),
      id: _currentTheme.id.isEmpty ? 'custom_${DateTime.now().millisecondsSinceEpoch}' : _currentTheme.id,
      toolbar: _currentTheme.toolbar.copyWith(inheritFromKeys: true),
      suggestions: _currentTheme.suggestions.copyWith(inheritFromKeys: true),
    );

    try {
      await ThemeManagerV2.saveThemeV2(updatedTheme);
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Theme saved successfully!')),
        );
        Navigator.of(context).pop(updatedTheme);
      }
    } catch (e) {
      _showError('Failed to save theme: $e');
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }

  void _updateTheme(KeyboardThemeV2 newTheme) {
    setState(() {
      _currentTheme = newTheme;
    });
    
    // Animate preview update
    _previewController.reset();
    _previewController.forward();
    
    // Apply theme immediately to system keyboard if live preview is enabled
    if (_currentTheme.advanced.livePreview) {
      _applyThemeToKeyboard(newTheme);
    }
  }
  
  /// Apply theme to system keyboard immediately
  Future<void> _applyThemeToKeyboard(KeyboardThemeV2 theme) async {
    try {
      // Force inheritance for seamless CleverType experience
      final seamlessTheme = theme.copyWith(
        toolbar: theme.toolbar.copyWith(inheritFromKeys: true),
        suggestions: theme.suggestions.copyWith(inheritFromKeys: true),
      );
      await ThemeManagerV2.saveThemeV2(seamlessTheme);
    } catch (e) {
      // Silently fail
    }
  }

  Future<void> _exportTheme() async {
    final jsonString = ThemeManagerV2.exportTheme(_currentTheme);
    await Clipboard.setData(ClipboardData(text: jsonString));
    
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Theme exported to clipboard!')),
    );
  }

  Future<void> _importTheme() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );
      
      if (result != null) {
        final file = File(result.files.single.path!);
        final jsonString = await file.readAsString();
        final theme = ThemeManagerV2.importTheme(jsonString);
        
        if (theme != null) {
          _updateTheme(theme);
          _nameController.text = theme.name;
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Theme imported successfully!')),
          );
        } else {
          _showError('Invalid theme file');
        }
      }
    } catch (e) {
      _showError('Failed to import theme: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Theme Editor'),
        backgroundColor: _currentTheme.background.color ?? Colors.grey[900],
        foregroundColor: _currentTheme.keys.text,
        actions: [
          IconButton(
            icon: const Icon(Icons.upload),
            onPressed: _exportTheme,
            tooltip: 'Export Theme',
          ),
          IconButton(
            icon: const Icon(Icons.download),
            onPressed: _importTheme,
            tooltip: 'Import Theme',
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => _applyThemeToKeyboard(_currentTheme),
            tooltip: 'Apply to Keyboard Now',
          ),
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _saveTheme,
            tooltip: 'Save Theme',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          isScrollable: true,
          indicatorColor: _currentTheme.specialKeys.accent,
          tabs: const [
            Tab(icon: Icon(Icons.palette), text: 'Background'),
            Tab(icon: Icon(Icons.keyboard), text: 'Button'),
            Tab(icon: Icon(Icons.auto_fix_high), text: 'Effects'),
            Tab(icon: Icon(Icons.font_download), text: 'Font'),
          ],
        ),
      ),
      body: Column(
        children: [
          // Live Preview
          if (_currentTheme.advanced.livePreview) _buildLivePreview(),
          
          // Tab Content
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildBackgroundTab(),
                _buildButtonTab(),
                _buildEffectsTab(),
                _buildFontTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLivePreview() {
    return AnimatedBuilder(
      animation: _previewAnimation,
      builder: (context, child) {
        return Transform.scale(
          scale: 0.7 + (0.3 * _previewAnimation.value),
          child: Container(
            margin: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _currentTheme.background.color ?? Colors.grey[900],
              borderRadius: BorderRadius.circular(12),
              boxShadow: const [
                BoxShadow(
                  color: Colors.black26,
                  blurRadius: 8,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _buildPreviewToolbar(),
                _buildPreviewSuggestions(),
                _buildPreviewKeyboard(),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildPreviewToolbar() {
    final toolbarBg = _currentTheme.toolbar.inheritFromKeys 
        ? _currentTheme.keys.bg 
        : _currentTheme.toolbar.bg;
    final toolbarIcon = _currentTheme.toolbar.inheritFromKeys 
        ? _currentTheme.keys.text 
        : _currentTheme.toolbar.icon;

    return Container(
      height: _currentTheme.toolbar.heightDp * 0.8,
      decoration: BoxDecoration(
        color: toolbarBg,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          Icon(Icons.mic, color: toolbarIcon, size: 16),
          Icon(Icons.emoji_emotions, color: _currentTheme.toolbar.activeAccent, size: 16),
          Icon(Icons.gif_box, color: toolbarIcon, size: 16),
          Icon(Icons.more_horiz, color: toolbarIcon, size: 16),
        ],
      ),
    );
  }

  Widget _buildPreviewSuggestions() {
    final suggestionBg = _currentTheme.suggestions.inheritFromKeys 
        ? _currentTheme.keys.bg 
        : _currentTheme.suggestions.bg;
    final suggestionText = _currentTheme.suggestions.inheritFromKeys 
        ? _currentTheme.keys.text 
        : _currentTheme.suggestions.text;

    return Container(
      height: 32,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      color: suggestionBg,
      child: Row(
        children: [
          Expanded(
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 2),
              decoration: BoxDecoration(
                color: _currentTheme.suggestions.chip.bg,
                borderRadius: BorderRadius.circular(_currentTheme.suggestions.chip.radius / 2),
              ),
              child: Center(
                child: Text(
                  'hello',
                  style: TextStyle(
                    color: _currentTheme.suggestions.chip.text,
                    fontSize: 10,
                    fontFamily: _currentTheme.suggestions.font.family,
                    fontWeight: _currentTheme.suggestions.font.bold ? FontWeight.bold : FontWeight.normal,
                  ),
                ),
              ),
            ),
          ),
          Expanded(
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 2),
              decoration: BoxDecoration(
                color: _currentTheme.suggestions.chip.bg,
                borderRadius: BorderRadius.circular(_currentTheme.suggestions.chip.radius / 2),
              ),
              child: Center(
                child: Text(
                  'world',
                  style: TextStyle(
                    color: _currentTheme.suggestions.chip.text,
                    fontSize: 10,
                    fontFamily: _currentTheme.suggestions.font.family,
                    fontWeight: _currentTheme.suggestions.font.bold ? FontWeight.bold : FontWeight.normal,
                  ),
                ),
              ),
            ),
          ),
          Expanded(
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 2),
              decoration: BoxDecoration(
                color: _currentTheme.suggestions.chip.bg,
                borderRadius: BorderRadius.circular(_currentTheme.suggestions.chip.radius / 2),
              ),
              child: Center(
                child: Text(
                  'test',
                  style: TextStyle(
                    color: _currentTheme.suggestions.chip.text,
                    fontSize: 10,
                    fontFamily: _currentTheme.suggestions.font.family,
                    fontWeight: _currentTheme.suggestions.font.bold ? FontWeight.bold : FontWeight.normal,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPreviewKeyboard() {
    return Padding(
      padding: const EdgeInsets.all(8),
      child: Column(
        children: [
          // First row
          Row(
            children: 'QWERTYUIOP'.split('').map((letter) => _buildPreviewKey(letter, false)).toList(),
          ),
          const SizedBox(height: 2),
          // Second row
          Row(
            children: [
              ...('ASDFGHJKL'.split('').map((letter) => _buildPreviewKey(letter, false))),
              _buildPreviewKey('⌫', true),
            ],
          ),
          const SizedBox(height: 2),
          // Third row
          Row(
            children: [
              _buildPreviewKey('⇧', true),
              ...('ZXCVBNM'.split('').map((letter) => _buildPreviewKey(letter, false))),
              _buildPreviewKey('⏎', _currentTheme.specialKeys.useAccentForEnter),
            ],
          ),
          const SizedBox(height: 2),
          // Space row
          Row(
            children: [
              _buildPreviewKey('123', false),
              Expanded(child: _buildPreviewKey('space', false, isWide: true)),
              _buildPreviewKey('🌐', _currentTheme.specialKeys.applyTo.contains('globe')),
              _buildPreviewKey('😀', _currentTheme.specialKeys.applyTo.contains('emoji')),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPreviewKey(String text, bool isSpecial, {bool isWide = false}) {
    final keyColor = isSpecial 
        ? _currentTheme.specialKeys.accent 
        : _currentTheme.keys.bg;
    final textColor = isSpecial 
        ? Colors.white 
        : _currentTheme.keys.text;

    return Expanded(
      flex: isWide ? 4 : 1,
      child: Container(
        height: 24,
        margin: const EdgeInsets.all(0.5),
        decoration: BoxDecoration(
          color: keyColor,
          borderRadius: BorderRadius.circular(_currentTheme.keys.radius / 3),
          border: _currentTheme.keys.border.enabled 
              ? Border.all(color: _currentTheme.keys.border.color, width: 0.5)
              : null,
        ),
        child: Center(
          child: Text(
            text,
            style: TextStyle(
              color: text == 'space' ? _currentTheme.specialKeys.spaceLabelColor : textColor,
              fontSize: 8,
              fontFamily: _currentTheme.keys.font.family,
              fontWeight: _currentTheme.keys.font.bold ? FontWeight.bold : FontWeight.normal,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBasicTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection(
            'Theme Information',
            [
              TextField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Theme Name',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: _currentTheme.mode,
                decoration: const InputDecoration(
                  labelText: 'Theme Mode',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'unified', child: Text('Unified (Same colors)')),
                  DropdownMenuItem(value: 'split', child: Text('Split (Custom colors)')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(mode: value));
                  }
                },
              ),
            ],
          ),
          const SizedBox(height: 24),
          _buildSection(
            'Quick Themes',
            [
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createDefault()),
                      child: const Text('Dark Theme'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(ThemeManagerV2.createLightTheme()),
                      child: const Text('Light Theme'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createBlueTheme()),
                      child: const Text('Blue'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createPinkTheme()),
                      child: const Text('Pink'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createGreenTheme()),
                      child: const Text('Green'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createLoveHeartsTheme()),
                      child: const Text('💕 Hearts'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createGoldStarTheme()),
                      child: const Text('⭐ Stars'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createNeonTheme()),
                      child: const Text('✨ Neon'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: _generateRandomTheme,
                      icon: const Icon(Icons.shuffle),
                      label: const Text('Random Theme'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.purple.shade100,
                        foregroundColor: Colors.purple.shade800,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createGalaxyTheme()),
                      icon: const Icon(Icons.auto_awesome),
                      label: const Text('🌌 Galaxy'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Generate a random theme with random colors and effects
  void _generateRandomTheme() {
    final random = DateTime.now().millisecondsSinceEpoch;
    final allColors = [
      Colors.red, Colors.pink, Colors.purple, Colors.blue,
      Colors.cyan, Colors.teal, Colors.green, Colors.yellow,
      Colors.orange, Colors.brown, Colors.grey,
      const Color(0xFF2196F3), const Color(0xFF4CAF50), const Color(0xFF9C27B0),
      const Color(0xFFFF9800), const Color(0xFFE91E63), const Color(0xFF00BCD4),
      const Color(0xFF03A9F4), const Color(0xFFCDDC39), const Color(0xFFFFC107),
    ];
    
    final bgColor = allColors[random % allColors.length];
    final keyColor = Color.lerp(bgColor, Colors.white, 0.3) ?? bgColor;
    final accentColor = Color.lerp(bgColor, Colors.black, 0.5) ?? bgColor;
    
    final presets = ['rounded', 'bordered', 'flat'];
    final animations = ['ripple', 'glow', 'bounce'];
    final effects = [<String>[], ['glow'], ['sparkles'], ['hearts'], ['sparkles', 'glow']];
    
    final randomTheme = _currentTheme.copyWith(
      id: 'random_theme_$random',
      name: 'Random ${random.toString().substring(random.toString().length - 4)}',
      background: _currentTheme.background.copyWith(color: bgColor),
      keys: _currentTheme.keys.copyWith(
        preset: presets[random % presets.length],
        bg: keyColor,
        radius: 4.0 + (random % 16).toDouble(),
      ),
      specialKeys: _currentTheme.specialKeys.copyWith(accent: accentColor),
      effects: _currentTheme.effects.copyWith(
        pressAnimation: animations[random % animations.length],
        globalEffects: effects[random % effects.length],
      ),
    );
    
    _updateTheme(randomTheme);
    _nameController.text = randomTheme.name;
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Text('Random theme generated! 🎲'),
        backgroundColor: accentColor,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  Widget _buildBackgroundTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection(
            'Background Type',
            [
              DropdownButtonFormField<String>(
                value: _currentTheme.background.type,
                decoration: const InputDecoration(
                  labelText: 'Background Type',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'solid', child: Text('Solid Color')),
                  DropdownMenuItem(value: 'gradient', child: Text('Gradient')),
                  DropdownMenuItem(value: 'image', child: Text('Image')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      background: _currentTheme.background.copyWith(type: value),
                    ));
                  }
                },
              ),
            ],
          ),
          const SizedBox(height: 24),
          if (_currentTheme.background.type == 'solid') _buildSolidBackgroundSection(),
          if (_currentTheme.background.type == 'gradient') _buildGradientBackgroundSection(),
          if (_currentTheme.background.type == 'image') _buildImageBackgroundSection(),
        ],
      ),
    );
  }

  Widget _buildSolidBackgroundSection() {
    return _buildSection(
      'Solid Color',
      [
        _buildColorPicker(
          'Background Color',
          _currentTheme.background.color ?? Colors.black,
          (color) {
            _updateTheme(_currentTheme.copyWith(
              background: _currentTheme.background.copyWith(color: color),
            ));
          },
        ),
      ],
    );
  }

  Widget _buildGradientBackgroundSection() {
    return _buildSection(
      'Gradient Settings',
      [
        DropdownButtonFormField<String>(
          value: _currentTheme.background.gradient?.orientation ?? 'TOP_BOTTOM',
          decoration: const InputDecoration(
            labelText: 'Gradient Direction',
            border: OutlineInputBorder(),
          ),
          items: const [
            DropdownMenuItem(value: 'TOP_BOTTOM', child: Text('Top to Bottom')),
            DropdownMenuItem(value: 'LEFT_RIGHT', child: Text('Left to Right')),
            DropdownMenuItem(value: 'TL_BR', child: Text('Top-Left to Bottom-Right')),
            DropdownMenuItem(value: 'TR_BL', child: Text('Top-Right to Bottom-Left')),
          ],
          onChanged: (value) {
            if (value != null) {
              final gradient = _currentTheme.background.gradient?.copyWith(orientation: value) ??
                  ThemeGradient(colors: [Colors.blue, Colors.purple], orientation: value);
              _updateTheme(_currentTheme.copyWith(
                background: _currentTheme.background.copyWith(gradient: gradient),
              ));
            }
          },
        ),
        const SizedBox(height: 16),
        _buildColorPicker(
          'Start Color',
          _currentTheme.background.gradient?.colors.first ?? Colors.blue,
          (color) {
            final colors = [...(_currentTheme.background.gradient?.colors ?? [Colors.blue, Colors.purple])];
            if (colors.isNotEmpty) colors[0] = color;
            final gradient = ThemeGradient(
              colors: colors,
              orientation: _currentTheme.background.gradient?.orientation ?? 'TOP_BOTTOM',
            );
            _updateTheme(_currentTheme.copyWith(
              background: _currentTheme.background.copyWith(gradient: gradient),
            ));
          },
        ),
        const SizedBox(height: 16),
        _buildColorPicker(
          'End Color',
          _currentTheme.background.gradient?.colors.last ?? Colors.purple,
          (color) {
            final colors = [...(_currentTheme.background.gradient?.colors ?? [Colors.blue, Colors.purple])];
            if (colors.length > 1) colors[1] = color;
            final gradient = ThemeGradient(
              colors: colors,
              orientation: _currentTheme.background.gradient?.orientation ?? 'TOP_BOTTOM',
            );
            _updateTheme(_currentTheme.copyWith(
              background: _currentTheme.background.copyWith(gradient: gradient),
            ));
          },
        ),
      ],
    );
  }

  Widget _buildImageBackgroundSection() {
    return _buildSection(
      'Image Settings',
      [
        TextFormField(
          initialValue: _currentTheme.background.imagePath ?? '',
          decoration: const InputDecoration(
            labelText: 'Image Path',
            border: OutlineInputBorder(),
            hintText: 'assets/images/background.png',
          ),
          onChanged: (value) {
            _updateTheme(_currentTheme.copyWith(
              background: _currentTheme.background.copyWith(imagePath: value),
            ));
          },
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            const Text('Opacity:'),
            const SizedBox(width: 16),
            Expanded(
              child: Slider(
                value: _currentTheme.background.imageOpacity,
                min: 0.0,
                max: 1.0,
                divisions: 20,
                label: '${(_currentTheme.background.imageOpacity * 100).round()}%',
                onChanged: (value) {
                  _updateTheme(_currentTheme.copyWith(
                    background: _currentTheme.background.copyWith(imageOpacity: value),
                  ));
                },
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildKeysTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection(
            'Key Appearance',
            [
              DropdownButtonFormField<String>(
                value: _currentTheme.keys.preset,
                decoration: const InputDecoration(
                  labelText: 'Key Style Preset',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'flat', child: Text('Flat')),
                  DropdownMenuItem(value: 'bordered', child: Text('Bordered')),
                  DropdownMenuItem(value: 'floating', child: Text('Floating')),
                  DropdownMenuItem(value: '3d', child: Text('3D')),
                  DropdownMenuItem(value: 'transparent', child: Text('Transparent')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      keys: _currentTheme.keys.copyWith(preset: value),
                    ));
                  }
                },
              ),
            ],
          ),
          const SizedBox(height: 24),
          _buildSection(
            'Key Colors',
            [
              _buildColorPicker('Key Background', _currentTheme.keys.bg, (color) {
                _updateTheme(_currentTheme.copyWith(
                  keys: _currentTheme.keys.copyWith(bg: color),
                ));
              }),
              const SizedBox(height: 16),
              _buildColorPicker('Key Text', _currentTheme.keys.text, (color) {
                _updateTheme(_currentTheme.copyWith(
                  keys: _currentTheme.keys.copyWith(text: color),
                ));
              }),
              const SizedBox(height: 16),
              _buildColorPicker('Key Pressed', _currentTheme.keys.pressed, (color) {
                _updateTheme(_currentTheme.copyWith(
                  keys: _currentTheme.keys.copyWith(pressed: color),
                ));
              }),
              const SizedBox(height: 16),
              _buildColorPicker('Accent Color', _currentTheme.specialKeys.accent, (color) {
                _updateTheme(_currentTheme.copyWith(
                  specialKeys: _currentTheme.specialKeys.copyWith(accent: color),
                ));
              }),
            ],
          ),
          const SizedBox(height: 24),
          _buildSection(
            'Key Shape',
            [
              Row(
                children: [
                  const Text('Corner Radius:'),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Slider(
                      value: _currentTheme.keys.radius,
                      min: 0.0,
                      max: 20.0,
                      divisions: 20,
                      label: '${_currentTheme.keys.radius.round()}dp',
                      onChanged: (value) {
                        _updateTheme(_currentTheme.copyWith(
                          keys: _currentTheme.keys.copyWith(radius: value),
                        ));
                      },
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildToolbarTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection(
            'Toolbar Settings',
            [
              SwitchListTile(
                title: const Text('Inherit from Keys'),
                subtitle: const Text('Use same colors as keyboard keys'),
                value: _currentTheme.toolbar.inheritFromKeys,
                onChanged: (value) {
                  _updateTheme(_currentTheme.copyWith(
                    toolbar: _currentTheme.toolbar.copyWith(inheritFromKeys: value),
                  ));
                },
              ),
            ],
          ),
          if (!_currentTheme.toolbar.inheritFromKeys) ...[
            const SizedBox(height: 24),
            _buildSection(
              'Custom Toolbar Colors',
              [
                _buildColorPicker('Background', _currentTheme.toolbar.bg, (color) {
                  _updateTheme(_currentTheme.copyWith(
                    toolbar: _currentTheme.toolbar.copyWith(bg: color),
                  ));
                }),
                const SizedBox(height: 16),
                _buildColorPicker('Icon Color', _currentTheme.toolbar.icon, (color) {
                  _updateTheme(_currentTheme.copyWith(
                    toolbar: _currentTheme.toolbar.copyWith(icon: color),
                  ));
                }),
              ],
            ),
          ],
          const SizedBox(height: 24),
          _buildSection(
            'Toolbar Layout',
            [
              Row(
                children: [
                  const Text('Height:'),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Slider(
                      value: _currentTheme.toolbar.heightDp,
                      min: 32.0,
                      max: 64.0,
                      divisions: 16,
                      label: '${_currentTheme.toolbar.heightDp.round()}dp',
                      onChanged: (value) {
                        _updateTheme(_currentTheme.copyWith(
                          toolbar: _currentTheme.toolbar.copyWith(heightDp: value),
                        ));
                      },
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSuggestionsTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection(
            'Suggestion Bar Settings',
            [
              SwitchListTile(
                title: const Text('Inherit from Keys'),
                subtitle: const Text('Use same colors as keyboard keys'),
                value: _currentTheme.suggestions.inheritFromKeys,
                onChanged: (value) {
                  _updateTheme(_currentTheme.copyWith(
                    suggestions: _currentTheme.suggestions.copyWith(inheritFromKeys: value),
                  ));
                },
              ),
            ],
          ),
          if (!_currentTheme.suggestions.inheritFromKeys) ...[
            const SizedBox(height: 24),
            _buildSection(
              'Custom Suggestion Colors',
              [
                _buildColorPicker('Background', _currentTheme.suggestions.bg, (color) {
                  _updateTheme(_currentTheme.copyWith(
                    suggestions: _currentTheme.suggestions.copyWith(bg: color),
                  ));
                }),
                const SizedBox(height: 16),
                _buildColorPicker('Text Color', _currentTheme.suggestions.text, (color) {
                  _updateTheme(_currentTheme.copyWith(
                    suggestions: _currentTheme.suggestions.copyWith(text: color),
                  ));
                }),
              ],
            ),
          ],
          const SizedBox(height: 24),
          _buildSection(
            'Suggestion Chips',
            [
              _buildColorPicker('Chip Background', _currentTheme.suggestions.chip.bg, (color) {
                _updateTheme(_currentTheme.copyWith(
                  suggestions: _currentTheme.suggestions.copyWith(
                    chip: _currentTheme.suggestions.chip.copyWith(bg: color),
                  ),
                ));
              }),
              const SizedBox(height: 16),
              Row(
                children: [
                  const Text('Chip Radius:'),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Slider(
                      value: _currentTheme.suggestions.chip.radius,
                      min: 0.0,
                      max: 20.0,
                      divisions: 20,
                      label: '${_currentTheme.suggestions.chip.radius.round()}dp',
                      onChanged: (value) {
                        _updateTheme(_currentTheme.copyWith(
                          suggestions: _currentTheme.suggestions.copyWith(
                            chip: _currentTheme.suggestions.chip.copyWith(radius: value),
                          ),
                        ));
                      },
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAdvancedTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection(
            'Preview & Gallery',
            [
              SwitchListTile(
                title: const Text('Live Preview'),
                subtitle: const Text('Show keyboard preview while editing'),
                value: _currentTheme.advanced.livePreview,
                onChanged: (value) {
                  _updateTheme(_currentTheme.copyWith(
                    advanced: _currentTheme.advanced.copyWith(livePreview: value),
                  ));
                },
              ),
              SwitchListTile(
                title: const Text('Gallery Enabled'),
                subtitle: const Text('Allow sharing to theme gallery'),
                value: _currentTheme.advanced.galleryEnabled,
                onChanged: (value) {
                  _updateTheme(_currentTheme.copyWith(
                    advanced: _currentTheme.advanced.copyWith(galleryEnabled: value),
                  ));
                },
              ),
            ],
          ),
          const SizedBox(height: 24),
          _buildSection(
            'Effects & Animation',
            [
              DropdownButtonFormField<String>(
                value: _currentTheme.effects.pressAnimation,
                decoration: const InputDecoration(
                  labelText: 'Press Animation',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'ripple', child: Text('Ripple Effect')),
                  DropdownMenuItem(value: 'bounce', child: Text('Bounce Effect')),
                  DropdownMenuItem(value: 'glow', child: Text('Glow Effect')),
                  DropdownMenuItem(value: 'none', child: Text('No Animation')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      effects: _currentTheme.effects.copyWith(pressAnimation: value),
                    ));
                  }
                },
              ),
            ],
          ),
          const SizedBox(height: 24),
          _buildSection(
            'Sound Pack',
            [
              DropdownButtonFormField<String>(
                value: _currentTheme.sounds.pack,
                decoration: const InputDecoration(
                  labelText: 'Sound Pack',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'soft', child: Text('Soft Clicks')),
                  DropdownMenuItem(value: 'mechanical', child: Text('Mechanical')),
                  DropdownMenuItem(value: 'clicky', child: Text('Clicky')),
                  DropdownMenuItem(value: 'classic', child: Text('Classic')),
                  DropdownMenuItem(value: 'typewriter', child: Text('Typewriter')),
                  DropdownMenuItem(value: 'piano', child: Text('Piano Keys')),
                  DropdownMenuItem(value: 'pop', child: Text('Pop Sound')),
                  DropdownMenuItem(value: 'silent', child: Text('Silent')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      sounds: _currentTheme.sounds.copyWith(pack: value),
                    ));
                  }
                },
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  const Text('Volume:'),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Slider(
                      value: _currentTheme.sounds.volume,
                      min: 0.0,
                      max: 1.0,
                      divisions: 10,
                      label: '${(_currentTheme.sounds.volume * 100).round()}%',
                      onChanged: (value) {
                        _updateTheme(_currentTheme.copyWith(
                          sounds: _currentTheme.sounds.copyWith(volume: value),
                        ));
                      },
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 24),
          _buildSection(
            'Seasonal & Dynamic',
            [
              DropdownButtonFormField<String>(
                value: _currentTheme.advanced.seasonalPack,
                decoration: const InputDecoration(
                  labelText: 'Seasonal Pack',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'none', child: Text('None')),
                  DropdownMenuItem(value: 'valentine', child: Text('Valentine\'s Day')),
                  DropdownMenuItem(value: 'halloween', child: Text('Halloween')),
                  DropdownMenuItem(value: 'christmas', child: Text('Christmas')),
                  DropdownMenuItem(value: 'spring', child: Text('Spring')),
                  DropdownMenuItem(value: 'summer', child: Text('Summer')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      advanced: _currentTheme.advanced.copyWith(seasonalPack: value),
                    ));
                  }
                },
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: _currentTheme.advanced.dynamicTheme,
                decoration: const InputDecoration(
                  labelText: 'Dynamic Theme',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'none', child: Text('None')),
                  DropdownMenuItem(value: 'time_of_day', child: Text('Time of Day')),
                  DropdownMenuItem(value: 'wallpaper', child: Text('Wallpaper')),
                  DropdownMenuItem(value: 'seasonal', child: Text('Seasonal')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      advanced: _currentTheme.advanced.copyWith(dynamicTheme: value),
                    ));
                  }
                },
              ),
            ],
          ),
        ],
      ),
    );
  }

  // ===== SIMPLIFIED TABS (CleverType Style) =====

  Widget _buildButtonTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection('Button Style', [
            DropdownButtonFormField<String>(
              value: _currentTheme.keys.preset,
              decoration: const InputDecoration(
                labelText: 'Style',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'rounded', child: Text('Rounded')),
                DropdownMenuItem(value: 'bordered', child: Text('Bordered')),
                DropdownMenuItem(value: 'flat', child: Text('Flat')),
                DropdownMenuItem(value: 'transparent', child: Text('Transparent')),
              ],
              onChanged: (value) {
                if (value != null) {
                  _updateTheme(_currentTheme.copyWith(
                    keys: _currentTheme.keys.copyWith(preset: value),
                  ));
                }
              },
            ),
          ]),
          const SizedBox(height: 16),
          _buildSection('Colors', [
            // Key Background Color
            ListTile(
              title: const Text('Key Background'),
              trailing: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: _currentTheme.keys.bg,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey),
                ),
              ),
              onTap: () => _showColorPicker(_currentTheme.keys.bg, (color) {
                _updateTheme(_currentTheme.copyWith(
                  keys: _currentTheme.keys.copyWith(bg: color),
                ));
              }),
            ),
            // Key Text Color
            ListTile(
              title: const Text('Key Text'),
              trailing: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: _currentTheme.keys.text,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey),
                ),
              ),
              onTap: () => _showColorPicker(_currentTheme.keys.text, (color) {
                _updateTheme(_currentTheme.copyWith(
                  keys: _currentTheme.keys.copyWith(text: color),
                ));
              }),
            ),
            // Pressed Color
            ListTile(
              title: const Text('Pressed Color'),
              trailing: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: _currentTheme.keys.pressed,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey),
                ),
              ),
              onTap: () => _showColorPicker(_currentTheme.keys.pressed, (color) {
                _updateTheme(_currentTheme.copyWith(
                  keys: _currentTheme.keys.copyWith(pressed: color),
                ));
              }),
            ),
            // Accent Color
            ListTile(
              title: const Text('Accent Color'),
              subtitle: const Text('Special keys (Enter, Globe, etc)'),
              trailing: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: _currentTheme.specialKeys.accent,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey),
                ),
              ),
              onTap: () => _showColorPicker(_currentTheme.specialKeys.accent, (color) {
                _updateTheme(_currentTheme.copyWith(
                  specialKeys: _currentTheme.specialKeys.copyWith(accent: color),
                ));
              }),
            ),
          ]),
          const SizedBox(height: 16),
          _buildSection('Shape', [
            Row(
              children: [
                const Text('Corner Radius:'),
                const SizedBox(width: 16),
                Expanded(
                  child: Slider(
                    value: _currentTheme.keys.radius,
                    min: 0.0,
                    max: 20.0,
                    divisions: 20,
                    label: '${_currentTheme.keys.radius.round()}px',
                    onChanged: (value) {
                      _updateTheme(_currentTheme.copyWith(
                        keys: _currentTheme.keys.copyWith(radius: value),
                      ));
                    },
                  ),
                ),
              ],
            ),
          ]),
        ],
      ),
    );
  }

  Widget _buildEffectsTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection('Press Effect', [
            DropdownButtonFormField<String>(
              value: _currentTheme.effects.pressAnimation,
              decoration: const InputDecoration(
                labelText: 'Effect Type',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'ripple', child: Text('Ripple')),
                DropdownMenuItem(value: 'bounce', child: Text('Bounce')),
                DropdownMenuItem(value: 'glow', child: Text('Glow')),
                DropdownMenuItem(value: 'none', child: Text('None')),
              ],
              onChanged: (value) {
                if (value != null) {
                  _updateTheme(_currentTheme.copyWith(
                    effects: _currentTheme.effects.copyWith(pressAnimation: value),
                  ));
                }
              },
            ),
          ]),
          const SizedBox(height: 16),
          _buildSection('Overlay Effects', [
            const Text('Add visual effects to your keyboard:'),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                'glow', 'sparkles', 'hearts', 'snow', 'particles', 'rain', 'leaves', 'stars', 'bubbles', 'flames'
              ].map((effect) {
                final isSelected = _currentTheme.effects.globalEffects.contains(effect);
                return FilterChip(
                  label: Text(effect.toUpperCase()),
                  selected: isSelected,
                  selectedColor: Colors.blue.shade100,
                  onSelected: (selected) {
                    final newEffects = List<String>.from(_currentTheme.effects.globalEffects);
                    if (selected) {
                      if (!newEffects.contains(effect)) {
                        newEffects.add(effect);
                      }
                    } else {
                      newEffects.remove(effect);
                    }
                    _updateTheme(_currentTheme.copyWith(
                      effects: _currentTheme.effects.copyWith(globalEffects: newEffects),
                    ));
                  },
                );
              }).toList(),
            ),
          ]),
          const SizedBox(height: 16),
          _buildSection('Sound', [
            DropdownButtonFormField<String>(
              value: _currentTheme.sounds.pack,
              decoration: const InputDecoration(
                labelText: 'Sound Pack',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'default', child: Text('Default')),
                DropdownMenuItem(value: 'soft', child: Text('Soft')),
                DropdownMenuItem(value: 'clicky', child: Text('Clicky')),
                DropdownMenuItem(value: 'mechanical', child: Text('Mechanical')),
                DropdownMenuItem(value: 'typewriter', child: Text('Typewriter')),
                DropdownMenuItem(value: 'piano', child: Text('Piano')),
                DropdownMenuItem(value: 'pop', child: Text('Pop')),
                DropdownMenuItem(value: 'silent', child: Text('Silent')),
              ],
              onChanged: (value) {
                if (value != null) {
                  _updateTheme(_currentTheme.copyWith(
                    sounds: _currentTheme.sounds.copyWith(pack: value),
                  ));
                }
              },
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Text('Volume:'),
                const SizedBox(width: 16),
                Expanded(
                  child: Slider(
                    value: _currentTheme.sounds.volume,
                    min: 0.0,
                    max: 1.0,
                    divisions: 10,
                    label: '${(_currentTheme.sounds.volume * 100).round()}%',
                    onChanged: (value) {
                      _updateTheme(_currentTheme.copyWith(
                        sounds: _currentTheme.sounds.copyWith(volume: value),
                      ));
                    },
                  ),
                ),
              ],
            ),
          ]),
        ],
      ),
    );
  }

  Widget _buildFontTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection('Font Settings', [
            DropdownButtonFormField<String>(
              value: _currentTheme.keys.font.family,
              decoration: const InputDecoration(
                labelText: 'Font Family',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'Roboto', child: Text('Roboto')),
                DropdownMenuItem(value: 'NotoSans', child: Text('Noto Sans')),
                DropdownMenuItem(value: 'Poppins', child: Text('Poppins')),
                DropdownMenuItem(value: 'monospace', child: Text('Monospace')),
              ],
              onChanged: (value) {
                if (value != null) {
                  _updateTheme(_currentTheme.copyWith(
                    keys: _currentTheme.keys.copyWith(
                      font: _currentTheme.keys.font.copyWith(family: value),
                    ),
                  ));
                }
              },
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Text('Font Size:'),
                const SizedBox(width: 16),
                Expanded(
                  child: Slider(
                    value: _currentTheme.keys.font.sizeSp,
                    min: 12.0,
                    max: 24.0,
                    divisions: 12,
                    label: '${_currentTheme.keys.font.sizeSp.round()}sp',
                    onChanged: (value) {
                      _updateTheme(_currentTheme.copyWith(
                        keys: _currentTheme.keys.copyWith(
                          font: _currentTheme.keys.font.copyWith(sizeSp: value),
                        ),
                      ));
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: CheckboxListTile(
                    title: const Text('Bold'),
                    value: _currentTheme.keys.font.bold,
                    onChanged: (value) {
                      if (value != null) {
                        _updateTheme(_currentTheme.copyWith(
                          keys: _currentTheme.keys.copyWith(
                            font: _currentTheme.keys.font.copyWith(bold: value),
                          ),
                        ));
                      }
                    },
                  ),
                ),
                Expanded(
                  child: CheckboxListTile(
                    title: const Text('Italic'),
                    value: _currentTheme.keys.font.italic,
                    onChanged: (value) {
                      if (value != null) {
                        _updateTheme(_currentTheme.copyWith(
                          keys: _currentTheme.keys.copyWith(
                            font: _currentTheme.keys.font.copyWith(italic: value),
                          ),
                        ));
                      }
                    },
                  ),
                ),
              ],
            ),
          ]),
        ],
      ),
    );
  }

  Widget _buildAdaptiveTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection('Adaptive Background', [
            SwitchListTile(
              title: const Text('Enable Adaptive Background'),
              subtitle: const Text('Automatically adapt colors to your wallpaper'),
              value: _currentTheme.background.type == 'adaptive',
              onChanged: (value) {
                if (value) {
                  _updateTheme(_currentTheme.copyWith(
                    background: _currentTheme.background.copyWith(
                      type: 'adaptive',
                      adaptive: ThemeAdaptive(
                        enabled: true,
                        source: 'wallpaper',
                        materialYou: false,
                      ),
                    ),
                  ));
                } else {
                  _updateTheme(_currentTheme.copyWith(
                    background: _currentTheme.background.copyWith(
                      type: 'solid',
                      adaptive: null,
                    ),
                  ));
                }
              },
            ),
            if (_currentTheme.background.type == 'adaptive' && _currentTheme.background.adaptive != null) ...[
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: _currentTheme.background.adaptive!.source,
                decoration: const InputDecoration(
                  labelText: 'Color Source',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'wallpaper', child: Text('Wallpaper')),
                  DropdownMenuItem(value: 'system', child: Text('System Theme')),
                  DropdownMenuItem(value: 'app', child: Text('App Theme')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      background: _currentTheme.background.copyWith(
                        adaptive: _currentTheme.background.adaptive!.copyWith(source: value),
                      ),
                    ));
                  }
                },
              ),
              const SizedBox(height: 16),
              SwitchListTile(
                title: const Text('Material You'),
                subtitle: const Text('Use Material You dynamic theming (Android 12+)'),
                value: _currentTheme.background.adaptive!.materialYou,
                onChanged: (value) {
                  _updateTheme(_currentTheme.copyWith(
                    background: _currentTheme.background.copyWith(
                      adaptive: _currentTheme.background.adaptive!.copyWith(materialYou: value),
                    ),
                  ));
                },
              ),
            ],
          ]),
        ],
      ),
    );
  }

  Widget _buildStickersTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection('Sticker Settings', [
            SwitchListTile(
              title: const Text('Enable Stickers'),
              subtitle: const Text('Add fun sticker overlays to your keyboard'),
              value: _currentTheme.stickers.enabled,
              onChanged: (value) {
                _updateTheme(_currentTheme.copyWith(
                  stickers: _currentTheme.stickers.copyWith(enabled: value),
                ));
              },
            ),
            if (_currentTheme.stickers.enabled) ...[
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: _currentTheme.stickers.pack.isEmpty ? 'cute_animals' : _currentTheme.stickers.pack,
                decoration: const InputDecoration(
                  labelText: 'Sticker Pack',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'cute_animals', child: Text('🐱 Cute Animals')),
                  DropdownMenuItem(value: 'valentine', child: Text('💕 Valentine\'s Day')),
                  DropdownMenuItem(value: 'halloween', child: Text('🎃 Halloween')),
                  DropdownMenuItem(value: 'christmas', child: Text('🎄 Christmas')),
                  DropdownMenuItem(value: 'nature', child: Text('🌿 Nature')),
                  DropdownMenuItem(value: 'space', child: Text('🚀 Space')),
                  DropdownMenuItem(value: 'celebration', child: Text('🎉 Celebration')),
                  DropdownMenuItem(value: 'flowers', child: Text('🌸 Flowers')),
                  DropdownMenuItem(value: 'food', child: Text('🍕 Food')),
                  DropdownMenuItem(value: 'sports', child: Text('⚽ Sports')),
                  DropdownMenuItem(value: 'music', child: Text('🎵 Music')),
                  DropdownMenuItem(value: 'travel', child: Text('✈️ Travel')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      stickers: _currentTheme.stickers.copyWith(pack: value),
                    ));
                  }
                },
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: _currentTheme.stickers.position,
                decoration: const InputDecoration(
                  labelText: 'Position',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'above', child: Text('Above Keyboard')),
                  DropdownMenuItem(value: 'below', child: Text('Below Keyboard')),
                  DropdownMenuItem(value: 'behind', child: Text('Behind Keys')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    _updateTheme(_currentTheme.copyWith(
                      stickers: _currentTheme.stickers.copyWith(position: value),
                    ));
                  }
                },
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  const Text('Opacity:'),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Slider(
                      value: _currentTheme.stickers.opacity,
                      min: 0.1,
                      max: 1.0,
                      divisions: 9,
                      label: '${(_currentTheme.stickers.opacity * 100).round()}%',
                      onChanged: (value) {
                        _updateTheme(_currentTheme.copyWith(
                          stickers: _currentTheme.stickers.copyWith(opacity: value),
                        ));
                      },
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              SwitchListTile(
                title: const Text('Animated'),
                subtitle: const Text('Enable sticker animations'),
                value: _currentTheme.stickers.animated,
                onChanged: (value) {
                  _updateTheme(_currentTheme.copyWith(
                    stickers: _currentTheme.stickers.copyWith(animated: value),
                  ));
                },
              ),
            ],
          ]),
        ],
      ),
    );
  }

  Widget _buildSection(String title, List<Widget> children) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildColorPicker(String label, Color color, ValueChanged<Color> onChanged) {
    return Row(
      children: [
        Expanded(
          child: Text(label),
        ),
        GestureDetector(
          onTap: () => _showColorPicker(color, onChanged),
          child: Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: color,
              border: Border.all(color: Colors.grey),
              borderRadius: BorderRadius.circular(8),
            ),
          ),
        ),
      ],
    );
  }

  void _showColorPicker(Color currentColor, ValueChanged<Color> onChanged) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Pick a Color'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Predefined colors (enhanced with theme-matching colors)
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  // Basic colors
                  Colors.red, Colors.pink, Colors.purple, Colors.blue,
                  Colors.cyan, Colors.teal, Colors.green, Colors.yellow,
                  Colors.orange, Colors.brown, Colors.grey, Colors.black,
                  Colors.white,
                  // Enhanced palette colors from themes
                  const Color(0xFF2196F3), const Color(0xFF4CAF50), const Color(0xFF9C27B0),
                  const Color(0xFFFF9800), const Color(0xFFE91E63), const Color(0xFF00BCD4),
                  const Color(0xFF03A9F4), const Color(0xFF1565C0), const Color(0xFFCDDC39),
                  const Color(0xFFFFC107), const Color(0xFF009688), const Color(0xFF3F51B5),
                  const Color(0xFF795548), const Color(0xFF673AB7), const Color(0xFF8BC34A),
                  const Color(0xFFFF5722), const Color(0xFFFFD700), const Color(0xFF0A0A0A),
                ].map((color) {
                  return GestureDetector(
                    onTap: () {
                      onChanged(color);
                      Navigator.of(context).pop();
                    },
                    child: Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: color,
                        border: Border.all(
                          color: color == currentColor ? Colors.blue : Colors.grey,
                          width: color == currentColor ? 3 : 1,
                        ),
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
        ],
      ),
    );
  }
}

// Extension methods for copyWith functionality
extension ThemeGradientCopyWith on ThemeGradient {
  ThemeGradient copyWith({
    List<Color>? colors,
    String? orientation,
  }) {
    return ThemeGradient(
      colors: colors ?? this.colors,
      orientation: orientation ?? this.orientation,
    );
  }
}

extension ThemeKeysCopyWith on ThemeKeys {
  ThemeKeys copyWith({
    String? preset,
    Color? bg,
    Color? text,
    Color? pressed,
    double? rippleAlpha,
    ThemeKeysBorder? border,
    double? radius,
    ThemeKeysShadow? shadow,
    ThemeKeysFont? font,
  }) {
    return ThemeKeys(
      preset: preset ?? this.preset,
      bg: bg ?? this.bg,
      text: text ?? this.text,
      pressed: pressed ?? this.pressed,
      rippleAlpha: rippleAlpha ?? this.rippleAlpha,
      border: border ?? this.border,
      radius: radius ?? this.radius,
      shadow: shadow ?? this.shadow,
      font: font ?? this.font,
    );
  }
}


extension ThemeToolbarCopyWith on ThemeToolbar {
  ThemeToolbar copyWith({
    bool? inheritFromKeys,
    Color? bg,
    Color? icon,
    double? heightDp,
    Color? activeAccent,
    String? iconPack,
  }) {
    return ThemeToolbar(
      inheritFromKeys: inheritFromKeys ?? this.inheritFromKeys,
      bg: bg ?? this.bg,
      icon: icon ?? this.icon,
      heightDp: heightDp ?? this.heightDp,
      activeAccent: activeAccent ?? this.activeAccent,
      iconPack: iconPack ?? this.iconPack,
    );
  }
}

extension ThemeSuggestionsCopyWith on ThemeSuggestions {
  ThemeSuggestions copyWith({
    bool? inheritFromKeys,
    Color? bg,
    Color? text,
    ThemeChip? chip,
    ThemeSuggestionsFont? font,
  }) {
    return ThemeSuggestions(
      inheritFromKeys: inheritFromKeys ?? this.inheritFromKeys,
      bg: bg ?? this.bg,
      text: text ?? this.text,
      chip: chip ?? this.chip,
      font: font ?? this.font,
    );
  }
}

extension ThemeChipCopyWith on ThemeChip {
  ThemeChip copyWith({
    Color? bg,
    Color? text,
    Color? pressed,
    double? radius,
    double? spacingDp,
  }) {
    return ThemeChip(
      bg: bg ?? this.bg,
      text: text ?? this.text,
      pressed: pressed ?? this.pressed,
      radius: radius ?? this.radius,
      spacingDp: spacingDp ?? this.spacingDp,
    );
  }
}

extension ThemeEffectsCopyWith on ThemeEffects {
  ThemeEffects copyWith({
    String? pressAnimation,
    List<String>? globalEffects,
  }) {
    return ThemeEffects(
      pressAnimation: pressAnimation ?? this.pressAnimation,
      globalEffects: globalEffects ?? this.globalEffects,
    );
  }
}

extension ThemeSoundsCopyWith on ThemeSounds {
  ThemeSounds copyWith({
    String? pack,
    Map<String, String>? customUris,
    double? volume,
  }) {
    return ThemeSounds(
      pack: pack ?? this.pack,
      customUris: customUris ?? this.customUris,
      volume: volume ?? this.volume,
    );
  }
}

extension ThemeStickersCopyWith on ThemeStickers {
  ThemeStickers copyWith({
    bool? enabled,
    String? pack,
    String? position,
    double? opacity,
    bool? animated,
  }) {
    return ThemeStickers(
      enabled: enabled ?? this.enabled,
      pack: pack ?? this.pack,
      position: position ?? this.position,
      opacity: opacity ?? this.opacity,
      animated: animated ?? this.animated,
    );
  }
}

extension ThemeAdvancedCopyWith on ThemeAdvanced {
  ThemeAdvanced copyWith({
    bool? livePreview,
    bool? galleryEnabled,
    bool? shareEnabled,
    String? dynamicTheme,
    String? seasonalPack,
    bool? materialYouExtract,
  }) {
    return ThemeAdvanced(
      livePreview: livePreview ?? this.livePreview,
      galleryEnabled: galleryEnabled ?? this.galleryEnabled,
      shareEnabled: shareEnabled ?? this.shareEnabled,
      dynamicTheme: dynamicTheme ?? this.dynamicTheme,
      seasonalPack: seasonalPack ?? this.seasonalPack,
      materialYouExtract: materialYouExtract ?? this.materialYouExtract,
    );
  }
}

/// Background Image model for theme gallery
class BackgroundImage {
  final String id;
  final String category;
  final String imageUrl;
  final IconData icon;

  BackgroundImage({
    required this.id,
    required this.category,
    required this.imageUrl,
    required this.icon,
  });
}
