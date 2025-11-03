
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
import 'package:google_fonts/google_fonts.dart';

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
  late KeyboardThemeV2 _currentTheme;
  final _nameController = TextEditingController();
  final FocusNode _keyboardFocusNode = FocusNode();
  late final List<_EditorTab> _tabs;
  static const List<_EffectOption> _effectOptions = [
    _EffectOption(
      id: 'none',
      label: 'None',
      icon: Icons.close,
      gradientColors: [
        Color(0xFFE6EBF1),
        Color(0xFFD5DAE2),
      ],
      iconColor: Color(0xFF5F6A7A),
    ),
    _EffectOption(
      id: 'sparkles',
      label: 'Sparkle',
      icon: Icons.auto_awesome,
      gradientColors: [
        Color(0xFF7F7FD5),
        Color(0xFF86A8E7),
      ],
    ),
    _EffectOption(
      id: 'stars',
      label: 'Stars',
      icon: Icons.star,
      gradientColors: [
        Color(0xFFFFC371),
        Color(0xFFFF5F6D),
      ],
    ),
    _EffectOption(
      id: 'hearts',
      label: 'Hearts',
      icon: Icons.favorite,
      gradientColors: [
        Color(0xFFFF758C),
        Color(0xFFFF7EB3),
      ],
    ),
    _EffectOption(
      id: 'bubbles',
      label: 'Bubbles',
      icon: Icons.bubble_chart,
      gradientColors: [
        Color(0xFF56E4FF),
        Color(0xFF5B9DF9),
      ],
    ),
    _EffectOption(
      id: 'leaves',
      label: 'Leaves',
      icon: Icons.eco,
      gradientColors: [
        Color(0xFF7BC74D),
        Color(0xFF28A745),
      ],
    ),
    _EffectOption(
      id: 'snow',
      label: 'Snow',
      icon: Icons.ac_unit,
      gradientColors: [
        Color(0xFF8EC5FC),
        Color(0xFFE0C3FC),
      ],
    ),
    _EffectOption(
      id: 'lightning',
      label: 'Bolt',
      icon: Icons.bolt,
      gradientColors: [
        Color(0xFFFFF000),
        Color(0xFFFFA500),
      ],
    ),
    _EffectOption(
      id: 'confetti',
      label: 'Confetti',
      icon: Icons.celebration,
      gradientColors: [
        Color(0xFFFF6CAB),
        Color(0xFF7366FF),
      ],
    ),
    _EffectOption(
      id: 'butterflies',
      label: 'Butterfly',
      icon: Icons.flutter_dash,
      gradientColors: [
        Color(0xFFFFB6C1),
        Color(0xFFF8BBD0),
      ],
    ),
    _EffectOption(
      id: 'rainbow',
      label: 'Rainbow',
      icon: Icons.wb_sunny,
      gradientColors: [
        Color(0xFFFF9A9E),
        Color(0xFFFAD0C4),
      ],
    ),
  ];

  static final List<_FontOption> _fontOptions = [
    _FontOption(
      id: 'font_default',
      displayName: 'Default',
      previewText: 'F',
      themeFamily: 'Roboto',
      previewStyleBuilder: (selected) => GoogleFonts.roboto(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w600,
      ),
    ),
    _FontOption(
      id: 'font_clean',
      displayName: 'Clean',
      previewText: 'Aa',
      themeFamily: 'SansSerif',
      previewStyleBuilder: (selected) => GoogleFonts.nunito(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w700,
      ),
    ),
    _FontOption(
      id: 'font_serif',
      displayName: 'Serif',
      previewText: 'Aa',
      themeFamily: 'Serif',
      previewStyleBuilder: (selected) => GoogleFonts.playfairDisplay(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w600,
      ),
    ),
    _FontOption(
      id: 'font_script',
      displayName: 'Script',
      previewText: 'Aa',
      themeFamily: 'Cursive',
      previewStyleBuilder: (selected) => GoogleFonts.dancingScript(
        fontSize: selected ? 26 : 24,
        fontWeight: FontWeight.w600,
      ),
      italic: true,
    ),
    _FontOption(
      id: 'font_modern',
      displayName: 'Modern',
      previewText: 'Aa',
      themeFamily: 'Roboto',
      previewStyleBuilder: (selected) => GoogleFonts.robotoCondensed(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w700,
      ),
      bold: true,
    ),
    _FontOption(
      id: 'font_mono',
      displayName: 'Mono',
      previewText: 'Aa',
      themeFamily: 'RobotoMono',
      previewStyleBuilder: (selected) => GoogleFonts.robotoMono(
        fontSize: selected ? 22 : 20,
        fontWeight: FontWeight.w600,
      ),
    ),
    _FontOption(
      id: 'font_casual',
      displayName: 'Casual',
      previewText: 'Aa',
      themeFamily: 'Casual',
      previewStyleBuilder: (selected) => GoogleFonts.comfortaa(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w600,
      ),
    ),
    _FontOption(
      id: 'font_round',
      displayName: 'Rounded',
      previewText: 'Aa',
      themeFamily: 'SansSerif',
      previewStyleBuilder: (selected) => GoogleFonts.quicksand(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w600,
      ),
    ),
    _FontOption(
      id: 'font_italic',
      displayName: 'Italic',
      previewText: 'Aa',
      themeFamily: 'Roboto',
      previewStyleBuilder: (selected) => GoogleFonts.roboto(
        fontSize: selected ? 24 : 22,
        fontStyle: FontStyle.italic,
        fontWeight: FontWeight.w500,
      ),
      italic: true,
    ),
    _FontOption(
      id: 'font_noto',
      displayName: 'Noto',
      previewText: 'Aa',
      themeFamily: 'NotoSans-VariableFont_wdth,wght.ttf',
      previewStyleBuilder: (selected) => GoogleFonts.notoSans(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w700,
      ),
    ),
    _FontOption(
      id: 'font_devanagari',
      displayName: 'Hindi',
      previewText: 'अआ',
      themeFamily: 'NotoSansDevanagari-Regular.ttf',
      previewStyleBuilder: (selected) => GoogleFonts.notoSansDevanagari(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w600,
      ),
    ),
    _FontOption(
      id: 'font_tamil',
      displayName: 'Tamil',
      previewText: 'அஆ',
      themeFamily: 'NotoSansTamil-Regular.ttf',
      previewStyleBuilder: (selected) => GoogleFonts.notoSansTamil(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w600,
      ),
    ),
    _FontOption(
      id: 'font_telugu',
      displayName: 'Telugu',
      previewText: 'అఆ',
      themeFamily: 'NotoSansTelugu-Regular.ttf',
      previewStyleBuilder: (selected) => GoogleFonts.notoSansTelugu(
        fontSize: selected ? 24 : 22,
        fontWeight: FontWeight.w600,
      ),
    ),
  ];

  static const String _customFontId = 'font_custom';
  
  late String _selectedFontId;
  
  // Animation controllers for live preview
  late AnimationController _previewController;
  late Animation<double> _previewAnimation;

  int _currentTabIndex = 0;

  @override
  void initState() {
    super.initState();
    // Initialize with provided theme or create new one
    _currentTheme = widget.initialTheme ?? KeyboardThemeV2.createDefault();
    _nameController.text = _currentTheme.name;
    _selectedFontId = _resolveFontOptionId(
      _currentTheme.keys.font.family,
      _currentTheme.keys.font.bold,
      _currentTheme.keys.font.italic,
    );
    _tabs = [
      _EditorTab(
        icon: Icons.camera_alt_outlined,
        label: 'Image',
        builder: _buildImageTab,
      ),
      _EditorTab(
        icon: Icons.format_color_text_rounded,
        label: 'Button',
        builder: _buildButtonTab,
      ),
      _EditorTab(
        icon: Icons.auto_awesome,
        label: 'Effect',
        builder: _buildEffectsTab,
      ),
      _EditorTab(
        icon: Icons.font_download,
        label: 'Font',
        builder: _buildFontTab,
      ),
      _EditorTab(
        icon: Icons.music_note_rounded,
        label: 'Sound',
        builder: _buildSoundTab,
      ),
      _EditorTab(
        icon: Icons.emoji_emotions_outlined,
        label: 'Stickers',
        builder: _buildStickersTab,
      ),
    ];

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
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _keyboardFocusNode.requestFocus();
      }
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _previewController.dispose();
    _keyboardFocusNode.dispose();
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
      _selectedFontId = _resolveFontOptionId(
        newTheme.keys.font.family,
        newTheme.keys.font.bold,
        newTheme.keys.font.italic,
      );
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
    final Widget tabContent = _tabs[_currentTabIndex].builder();
    return Scaffold(
      backgroundColor: AppColors.lightGrey,
      appBar: AppBar(
        toolbarHeight: 100,
        backgroundColor: AppColors.primary,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.of(context).pop(),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: ElevatedButton(
              onPressed: _saveTheme,
              style: _noShadowButtonStyle(
                backgroundColor: AppColors.white,
                foregroundColor: AppColors.black,
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              ),
              child: Text(
                'Save',
                style: AppTextStyle.bodyLarge.copyWith(
                  color: AppColors.black,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
        ],
        title: Text(
          'Customize Theme',
          style: AppTextStyle.titleLarge.copyWith(color: AppColors.white),
        ),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(72),
          child: _buildTabNavigation(),
        ),
      ),
      body: Column(
        children: [
          SizedBox(
            height: 1,
            child: TextField(
              focusNode: _keyboardFocusNode,
              autofocus: true,
              readOnly: true,
              decoration: const InputDecoration(
                border: InputBorder.none,
                contentPadding: EdgeInsets.zero,
              ),
              style: const TextStyle(color: Colors.transparent, fontSize: 1),
            ),
          ),
          Expanded(
            child: DecoratedBox(
              decoration: const BoxDecoration(color: AppColors.lightGrey),
              child: tabContent,
            ),
          ),
          // _buildPreviewSection(),
        ],
      ),
    );
  }

  Widget _buildTabNavigation() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      color: AppColors.primary,
      child: Row(
        children: List.generate(_tabs.length, (index) {
          final tab = _tabs[index];
          final isSelected = _currentTabIndex == index;
          return Expanded(
            child: GestureDetector(
              onTap: () {
                setState(() {
                  _currentTabIndex = index;
                });
              },
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 50,
                    height: 50,
                    decoration: BoxDecoration(
                      color: isSelected ? AppColors.secondary : const Color(0xFF1C3453),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Icon(
                      tab.icon,
                      color: isSelected ? AppColors.primary : AppColors.white,
                      size: 24,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    tab.label,
                    style: AppTextStyle.bodySmall.copyWith(
                      color: isSelected ? AppColors.secondary : AppColors.white,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  if (isSelected)
                    Container(
                      margin: const EdgeInsets.only(top: 4),
                      height: 2,
                      width: 24,
                      decoration: BoxDecoration(
                        color: AppColors.secondary,
                        borderRadius: BorderRadius.circular(1),
                      ),
                    ),
                ],
              ),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildPreviewSection() {
    return Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Live Preview',
            style: AppTextStyle.titleMedium.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
          _buildLivePreview(),
        ],
      ),
    );
  }

  ButtonStyle _noShadowButtonStyle({
    Color? backgroundColor,
    Color? foregroundColor,
    EdgeInsetsGeometry? padding,
    BorderRadiusGeometry borderRadius = const BorderRadius.all(Radius.circular(8)),
    BorderSide? side,
  }) {
    return ElevatedButton.styleFrom(
      elevation: 0,
      shadowColor: Colors.transparent,
      backgroundColor: backgroundColor,
      foregroundColor: foregroundColor,
      padding: padding,
      shape: RoundedRectangleBorder(
        borderRadius: borderRadius,
        side: side ?? BorderSide.none,
      ),
    );
  }

  Widget _buildBottomKeyboardPreview() {
    return Container(
      decoration: BoxDecoration(
        color: _currentTheme.background.color ?? Colors.grey[900],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildPreviewToolbar(),
          _buildPreviewSuggestions(),
          _buildPreviewKeyboard(),
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
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.15),
              blurRadius: 3,
              spreadRadius: 0,
              offset: const Offset(0, 2),
            ),
          ],
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
                      style: _noShadowButtonStyle(),
                      child: const Text('Dark Theme'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(ThemeManagerV2.createLightTheme()),
                      style: _noShadowButtonStyle(),
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
                      style: _noShadowButtonStyle(),
                      child: const Text('Blue'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createPinkTheme()),
                      style: _noShadowButtonStyle(),
                      child: const Text('Pink'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createGreenTheme()),
                      style: _noShadowButtonStyle(),
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
                      style: _noShadowButtonStyle(),
                      child: const Text('💕 Hearts'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createGoldStarTheme()),
                      style: _noShadowButtonStyle(),
                      child: const Text('⭐ Stars'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _updateTheme(KeyboardThemeV2.createNeonTheme()),
                      style: _noShadowButtonStyle(),
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
                      style: _noShadowButtonStyle(
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
                      style: _noShadowButtonStyle(),
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

  Widget _buildImageTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // Upload Photo Section
          GestureDetector(
            onTap: _uploadCustomImageForTheme,
            child: Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey[300]!, width: 2, style: BorderStyle.solid),
              ),
              child: Column(
                children: [
                  Container(
                    width: 80,
                    height: 80,
                    decoration: BoxDecoration(
                      color: Colors.blue[900],
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(
                      Icons.upload_file,
                      color: Colors.orange,
                      size: 40,
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Drag & drop or browse files',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Please upload Jpg image, size less than 100KB',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey[600],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          
          // Recently Uploaded Section
        
          // Brightness Control
          _buildSection('Brightness', [
            Row(
              children: [
                const Text('Brightness:'),
                Expanded(
                  child: Slider(
                    value: _currentTheme.background.imageOpacity,
                    min: 0.3,
                    max: 1.0,
                    divisions: 14,
                    label: '${(_currentTheme.background.imageOpacity * 100).round()}%',
                    onChanged: (value) {
                      _updateTheme(_currentTheme.copyWith(
                        background: _currentTheme.background.copyWith(imageOpacity: value),
                      ));
                    },
                  ),
                ),
                Text('${(_currentTheme.background.imageOpacity * 100).round()}%'),
              ],
            ),
          ]),
        ],
      ),
    );
  }

  Widget _buildImageThumbnail(int index) {
    final sampleImages = [
      'https://picsum.photos/200/150?random=$index',
    ];
    
    return GestureDetector(
      onTap: () => _applySampleImage(sampleImages[0]),
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey[300]!),
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Image.network(
            sampleImages[0],
            fit: BoxFit.cover,
            errorBuilder: (context, error, stackTrace) {
              return Container(
                color: Colors.grey[200],
                child: const Icon(Icons.image, size: 40),
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
        ),
      ),
    );
  }

  Future<void> _uploadCustomImageForTheme() async {
    // Navigate to custom image flow
    final customTheme = await Navigator.push<KeyboardThemeV2>(
      context,
      MaterialPageRoute(
        builder: (context) => const ChooseBaseThemeScreen(),
      ),
    );
    
    if (customTheme != null) {
      _updateTheme(customTheme);
    }
  }

  Future<void> _applySampleImage(String imageUrl) async {
    _updateTheme(_currentTheme.copyWith(
      background: _currentTheme.background.copyWith(
        type: 'image',
        imagePath: imageUrl,
      ),
    ));
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
    // Use the new visual button style selector without AppBar (already in a tab)
    return button_styles.ButtonStyleSelectorScreen(
      currentTheme: _currentTheme,
      onThemeUpdated: (theme) {
        _updateTheme(theme);
      },
      showAppBar: false,
    );
  }

  String _resolveFontOptionId(String family, bool bold, bool italic) {
    for (final option in _fontOptions) {
      if (option.themeFamily == family && option.bold == bold && option.italic == italic) {
        return option.id;
      }
    }
    return _customFontId;
  }

  Widget _buildEffectsTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // _buildSection('Press Effect', [
          //   DropdownButtonFormField<String>(
          //     value: _currentTheme.effects.pressAnimation,
          //     decoration: const InputDecoration(
          //       labelText: 'Effect Type',
          //       border: OutlineInputBorder(),
          //     ),
          //     items: const [
          //       DropdownMenuItem(value: 'none', child: Text('None')),
          //       DropdownMenuItem(value: 'ripple', child: Text('Ripple')),
          //       DropdownMenuItem(value: 'bounce', child: Text('Bounce')),
          //       DropdownMenuItem(value: 'glow', child: Text('Glow')),
          //     ],
          //     onChanged: (value) {
          //       if (value != null) {
          //         _updateTheme(_currentTheme.copyWith(
          //           effects: _currentTheme.effects.copyWith(pressAnimation: value),
          //         ));
          //       }
          //     },
          //   ),
          // ]),
          const SizedBox(height: 16),
          _buildSection('Overlay Effects', [
            _buildEffectGrid(),
            const SizedBox(height: 12),
            Text(
              'Tap an overlay to preview it. Choose “None” to keep the keyboard clean.',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Colors.grey[600],
                  ),
            ),
          ]),
        ],
      ),
    );
  }

  Widget _buildEffectGrid() {
    final activeEffects = _currentTheme.effects.globalEffects;
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      padding: EdgeInsets.zero,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 4,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 0.85,
      ),
      itemCount: _effectOptions.length,
      itemBuilder: (context, index) {
        final option = _effectOptions[index];
        final isSelected = _isEffectSelected(option.id, activeEffects);
        return _EffectOptionTile(
          option: option,
          isSelected: isSelected,
          onTap: () => _handleEffectSelection(option.id, isSelected),
        );
      },
    );
  }

  bool _isEffectSelected(String effectId, List<String> activeEffects) {
    if (effectId == 'none') {
      return activeEffects.isEmpty;
    }
    return activeEffects.contains(effectId);
  }

  void _handleEffectSelection(String effectId, bool wasSelected) {
    if (effectId == 'none') {
      if (_currentTheme.effects.globalEffects.isEmpty) {
        return;
      }
      _updateTheme(_currentTheme.copyWith(
        effects: _currentTheme.effects.copyWith(globalEffects: const []),
      ));
      return;
    }

    if (wasSelected) {
      // Tapping an active effect clears back to none for quick disable.
      _updateTheme(_currentTheme.copyWith(
        effects: _currentTheme.effects.copyWith(globalEffects: const []),
      ));
      return;
    }

    _updateTheme(_currentTheme.copyWith(
      effects: _currentTheme.effects.copyWith(globalEffects: [effectId]),
    ));
  }


  Widget _buildFontTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Choose Font Style',
            style: AppTextStyle.titleMedium.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 16),
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: _fontOptions.length,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 5,
              mainAxisSpacing: 18,
              crossAxisSpacing: 12,
              childAspectRatio: 0.85,
            ),
            itemBuilder: (context, index) {
              final option = _fontOptions[index];
              final isSelected = option.id == _selectedFontId;
              return _buildFontOptionTile(option, isSelected);
            },
          ),
          if (_selectedFontId == _customFontId) ...[
            const SizedBox(height: 12),
            Text(
              'Custom font combo active — adjust Bold/Italic to return to a preset or keep your unique look.',
              style: AppTextStyle.bodySmall.copyWith(
                color: AppColors.grey,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
          const SizedBox(height: 24),
          _buildSection('Fine-tune Font', [
            _buildFontSizeControl(),
            const SizedBox(height: 12),
            _buildFontStyleChips(),
          ]),
        ],
      ),
    );
  }

  Widget _buildFontOptionTile(_FontOption option, bool isSelected) {
    final previewStyle = option.previewStyleBuilder(isSelected).copyWith(
      color: AppColors.black,
    );
    return GestureDetector(
      onTap: () => _selectFontOption(option),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                width: 68,
                height: 68,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white,
                  border: Border.all(
                    color: isSelected ? AppColors.secondary : Colors.transparent,
                    width: isSelected ? 3 : 2,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.08),
                      blurRadius: 12,
                      offset: const Offset(0, 6),
                    ),
                  ],
                ),
                child: Center(
                  child: Text(
                    option.previewText,
                    style: previewStyle,
                  ),
                ),
              ),
              if (isSelected)
                Positioned(
                  bottom: -6,
                  right: -6,
                  child: Container(
                    width: 22,
                    height: 22,
                    decoration: const BoxDecoration(
                      shape: BoxShape.circle,
                      color: AppColors.secondary,
                    ),
                    child: const Icon(Icons.check, color: Colors.white, size: 14),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 6),
          SizedBox(
            width: 68,
            child: Text(
              option.displayName,
              style: AppTextStyle.bodySmall.copyWith(
                color: isSelected ? AppColors.secondary : AppColors.grey,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
              ),
              maxLines: 1,
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  void _selectFontOption(_FontOption option) {
    _updateTheme(_currentTheme.copyWith(
      keys: _currentTheme.keys.copyWith(
        font: _currentTheme.keys.font.copyWith(
          family: option.themeFamily,
          bold: option.bold,
          italic: option.italic,
        ),
      ),
    ));
  }

  Widget _buildFontSizeControl() {
    final fontSize = _currentTheme.keys.font.sizeSp.clamp(12.0, 24.0);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Font Size',
          style: AppTextStyle.bodySmall.copyWith(
            color: AppColors.black,
            fontWeight: FontWeight.w600,
          ),
        ),
        Slider(
          value: fontSize,
          min: 12.0,
          max: 24.0,
          divisions: 12,
          label: '${fontSize.round()}sp',
          onChanged: (value) {
            _updateTheme(_currentTheme.copyWith(
              keys: _currentTheme.keys.copyWith(
                font: _currentTheme.keys.font.copyWith(sizeSp: value),
              ),
            ));
          },
        ),
        Align(
          alignment: Alignment.centerRight,
          child: Text(
            '${fontSize.round()} sp',
            style: AppTextStyle.bodySmall.copyWith(
              color: AppColors.grey,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFontStyleChips() {
    final isBold = _currentTheme.keys.font.bold;
    final isItalic = _currentTheme.keys.font.italic;
    return Wrap(
      spacing: 12,
      children: [
        FilterChip(
          label: const Text('Bold'),
          selected: isBold,
          selectedColor: AppColors.secondary.withOpacity(0.18),
          checkmarkColor: AppColors.secondary,
          onSelected: (selected) {
            _updateTheme(_currentTheme.copyWith(
              keys: _currentTheme.keys.copyWith(
                font: _currentTheme.keys.font.copyWith(bold: selected),
              ),
            ));
          },
        ),
        FilterChip(
          label: const Text('Italic'),
          selected: isItalic,
          selectedColor: AppColors.secondary.withOpacity(0.18),
          checkmarkColor: AppColors.secondary,
          onSelected: (selected) {
            _updateTheme(_currentTheme.copyWith(
              keys: _currentTheme.keys.copyWith(
                font: _currentTheme.keys.font.copyWith(italic: selected),
              ),
            ));
          },
        ),
      ],
    );
  }

  void _showFullFontPicker() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (context) {
        return SizedBox(
          height: MediaQuery.of(context).size.height * 0.7,
          child: FontPicker(
            currentFont: _currentTheme.keys.font.family,
            availableFonts: _fontOptions.map((option) => option.themeFamily).toSet().toList(),
            onFontSelected: (font) {
              _updateTheme(_currentTheme.copyWith(
                keys: _currentTheme.keys.copyWith(
                  font: ThemeKeysFont(
                    family: font,
                    sizeSp: _currentTheme.keys.font.sizeSp,
                    bold: _currentTheme.keys.font.bold,
                    italic: _currentTheme.keys.font.italic,
                  ),
                ),
              ));
              Navigator.pop(context);
            },
          ),
        );
      },
    );
  }
  Widget _buildSoundTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSection('Sound Pack', [
            DropdownButtonFormField<String>(
              value: _currentTheme.sounds.pack,
              decoration: const InputDecoration(
                labelText: 'Sound Pack',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'default', child: Text('Default')),
                DropdownMenuItem(value: 'soft', child: Text('Soft Clicks')),
                DropdownMenuItem(value: 'clicky', child: Text('Clicky')),
                DropdownMenuItem(value: 'mechanical', child: Text('Mechanical')),
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
                Text('${(_currentTheme.sounds.volume * 100).round()}%'),
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

class _EffectOption {
  final String id;
  final String label;
  final IconData icon;
  final List<Color> gradientColors;
  final Color? iconColor;

  const _EffectOption({
    required this.id,
    required this.label,
    required this.icon,
    required this.gradientColors,
    this.iconColor,
  });
}

class _FontOption {
  final String id;
  final String displayName;
  final String themeFamily;
  final String previewText;
  final TextStyle Function(bool isSelected) previewStyleBuilder;
  final bool bold;
  final bool italic;

  const _FontOption({
    required this.id,
    required this.displayName,
    required this.themeFamily,
    required this.previewText,
    required this.previewStyleBuilder,
    this.bold = false,
    this.italic = false,
  });
}

class _EffectOptionTile extends StatelessWidget {
  final _EffectOption option;
  final bool isSelected;
  final VoidCallback onTap;

  const _EffectOptionTile({
    required this.option,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final gradient = option.gradientColors;
    final gradientPainter = gradient.length > 1 ? LinearGradient(colors: gradient) : null;
    final fallbackColor = gradient.first;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Stack(
            alignment: Alignment.center,
            children: [
              AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                curve: Curves.easeInOut,
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: gradientPainter,
                  color: gradientPainter == null ? fallbackColor : null,
                  border: Border.all(
                    color: isSelected ? colorScheme.primary : Colors.transparent,
                    width: 3,
                  ),
                  boxShadow: isSelected
                      ? [
                          BoxShadow(
                            color: colorScheme.primary.withOpacity(0.28),
                            blurRadius: 16,
                            offset: const Offset(0, 8),
                          ),
                        ]
                      : [],
                ),
                child: Icon(
                  option.icon,
                  color: option.iconColor ?? Colors.white,
                  size: 28,
                ),
              ),
              if (isSelected)
                Positioned(
                  right: 6,
                  top: 6,
                  child: Container(
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                    ),
                    padding: const EdgeInsets.all(3),
                    child: Icon(
                      Icons.check,
                      size: 14,
                      color: colorScheme.primary,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          // Text(
          //   option.label,
          //   maxLines: 1,
          //   overflow: TextOverflow.ellipsis,
          //   style: Theme.of(context).textTheme.bodySmall?.copyWith(
          //         fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
          //       ),
          //   textAlign: TextAlign.center,
          // ),
        ],
      ),
    );
  }
}

class _EditorTab {
  final IconData icon;
  final String label;
  final Widget Function() builder;

  const _EditorTab({
    required this.icon,
    required this.label,
    required this.builder,
  });
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
