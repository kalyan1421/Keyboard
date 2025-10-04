import 'package:flutter/material.dart';
import '../../theme/theme_v2.dart';
import '../../theme/theme_editor_v2.dart';

class ThemeScreen extends StatefulWidget {
  const ThemeScreen({super.key});

  @override
  State<ThemeScreen> createState() => _ThemeScreenState();
}

class _ThemeScreenState extends State<ThemeScreen> {
  KeyboardThemeV2? currentTheme;
  List<KeyboardThemeV2> availableThemes = [];

  @override
  void initState() {
    super.initState();
    _loadCurrentTheme();
    _loadAvailableThemes();
  }

  Future<void> _loadCurrentTheme() async {
    final theme = await ThemeManagerV2.loadThemeV2();
    if (mounted) {
      setState(() {
        currentTheme = theme;
      });
    }
  }

  Future<void> _loadAvailableThemes() async {
    // Load some preset themes
    final themes = [
      KeyboardThemeV2.createDefault(),
      ThemeManagerV2.createLightTheme(),
      // Add more preset themes as needed
    ];
    
    if (mounted) {
      setState(() {
        availableThemes = themes;
      });
    }
  }

  Future<void> _openThemeEditor({KeyboardThemeV2? theme}) async {
    final result = await Navigator.push<KeyboardThemeV2>(
      context,
      MaterialPageRoute(
        builder: (context) => ThemeEditorScreenV2(
          initialTheme: theme,
          isCreatingNew: theme == null,
        ),
      ),
    );

    if (result != null) {
      await ThemeManagerV2.saveThemeV2(result);
      _loadCurrentTheme();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Theme applied successfully!')),
      );
    }
  }

  Future<void> _applyTheme(KeyboardThemeV2 theme) async {
    await ThemeManagerV2.saveThemeV2(theme);
    _loadCurrentTheme();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('${theme.name} applied!')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Keyboard Themes',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                ElevatedButton.icon(
                  onPressed: () => _openThemeEditor(),
                  icon: const Icon(Icons.add),
                  label: const Text('Create Theme'),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Current Theme Section
            if (currentTheme != null) ...[
              const Text(
                'Current Theme',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 12),
              _buildThemeCard(
                currentTheme!,
                isActive: true,
                onEdit: () => _openThemeEditor(theme: currentTheme),
              ),
              const SizedBox(height: 24),
            ],

            // Available Themes Section
            const Text(
              'Available Themes',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 12),
            
            ...availableThemes.map((theme) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _buildThemeCard(
                theme,
                isActive: currentTheme?.id == theme.id,
                onApply: () => _applyTheme(theme),
                onEdit: () => _openThemeEditor(theme: theme),
              ),
            )),

            const SizedBox(height: 16),
            
            // Advanced Options
            Card(
              child: ListTile(
                leading: const Icon(Icons.file_upload),
                title: const Text('Import Theme'),
                subtitle: const Text('Import theme from JSON file'),
                onTap: () {
                  // TODO: Implement import functionality
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Import feature coming soon!')),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildThemeCard(
    KeyboardThemeV2 theme, {
    bool isActive = false,
    VoidCallback? onApply,
    VoidCallback? onEdit,
  }) {
    return Card(
      elevation: isActive ? 4 : 1,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(8),
          border: isActive ? Border.all(color: Colors.blue, width: 2) : null,
        ),
        child: Column(
          children: [
            // Theme Preview
            Container(
              height: 120,
              decoration: BoxDecoration(
                color: theme.background.color ?? Colors.grey[900],
                borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
              ),
              child: _buildMiniKeyboardPreview(theme),
            ),
            
            // Theme Info
            Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          theme.name,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Text(
                          'Mode: ${theme.mode}',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (isActive)
                    const Chip(
                      label: Text('Active', style: TextStyle(fontSize: 10)),
                      backgroundColor: Colors.blue,
                      labelStyle: TextStyle(color: Colors.white),
                    ),
                  const SizedBox(width: 8),
                  PopupMenuButton(
                    itemBuilder: (context) => [
                      if (!isActive && onApply != null)
                        PopupMenuItem(
                          onTap: onApply,
                          child: const Row(
                            children: [
                              Icon(Icons.check),
                              SizedBox(width: 8),
                              Text('Apply'),
                            ],
                          ),
                        ),
                      if (onEdit != null)
                        PopupMenuItem(
                          onTap: onEdit,
                          child: const Row(
                            children: [
                              Icon(Icons.edit),
                              SizedBox(width: 8),
                              Text('Edit'),
                            ],
                          ),
                        ),
                      PopupMenuItem(
                        onTap: () {
                          final exported = ThemeManagerV2.exportTheme(theme);
                          // TODO: Share exported theme
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Theme exported to clipboard!')),
                          );
                        },
                        child: const Row(
                          children: [
                            Icon(Icons.share),
                            SizedBox(width: 8),
                            Text('Export'),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMiniKeyboardPreview(KeyboardThemeV2 theme) {
    return Padding(
      padding: const EdgeInsets.all(8),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Toolbar preview
          Container(
            height: 20,
            decoration: BoxDecoration(
              color: theme.toolbar.inheritFromKeys ? theme.keys.bg : theme.toolbar.bg,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.mic, size: 12, color: theme.toolbar.inheritFromKeys ? theme.keys.text : theme.toolbar.icon),
                const SizedBox(width: 8),
                Icon(Icons.emoji_emotions, size: 12, color: theme.toolbar.activeAccent),
              ],
            ),
          ),
          const SizedBox(height: 4),
          
          // Keys preview
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: 'QWERTY'.split('').map((letter) {
              return Container(
                width: 24,
                height: 24,
                margin: const EdgeInsets.all(1),
                decoration: BoxDecoration(
                  color: theme.keys.bg,
                  borderRadius: BorderRadius.circular(theme.keys.radius / 3),
                  border: theme.keys.border.enabled 
                      ? Border.all(color: theme.keys.border.color, width: 0.5)
                      : null,
                ),
                child: Center(
                  child: Text(
                    letter,
                    style: TextStyle(
                      color: theme.keys.text,
                      fontSize: 8,
                      fontWeight: theme.keys.font.bold ? FontWeight.bold : FontWeight.normal,
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 2),
          
          // Special key (Enter) preview
          Container(
            width: 60,
            height: 20,
            decoration: BoxDecoration(
              color: theme.specialKeys.useAccentForEnter ? theme.specialKeys.accent : theme.keys.bg,
              borderRadius: BorderRadius.circular(theme.keys.radius / 3),
            ),
            child: Center(
              child: Text(
                'Enter',
                style: TextStyle(
                  color: theme.specialKeys.useAccentForEnter ? Colors.white : theme.keys.text,
                  fontSize: 8,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
