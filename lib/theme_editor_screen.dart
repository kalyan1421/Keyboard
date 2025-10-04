import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
import 'dart:math' as math;
import 'theme_manager.dart';

/// Comprehensive Theme Editor Screen
/// Provides Gboard baseline + CleverType advanced customizations
class ThemeEditorScreen extends StatefulWidget {
  final KeyboardThemeData? initialTheme;
  final bool isCreatingNew;

  const ThemeEditorScreen({
    super.key,
    this.initialTheme,
    this.isCreatingNew = false,
  });

  @override
  State<ThemeEditorScreen> createState() => _ThemeEditorScreenState();
}

class _ThemeEditorScreenState extends State<ThemeEditorScreen>
    with TickerProviderStateMixin {
  late TabController _tabController;
  late KeyboardThemeData _currentTheme;
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();

  // Animation controllers for preview
  late AnimationController _previewController;
  late Animation<double> _previewAnimation;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 5, vsync: this);
    
    // Initialize with provided theme or create new one
    _currentTheme = widget.initialTheme ?? _createNewTheme();
    _nameController.text = _currentTheme.name;
    _descriptionController.text = _currentTheme.description;

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
    _descriptionController.dispose();
    _previewController.dispose();
    super.dispose();
  }

  KeyboardThemeData _createNewTheme() {
    return KeyboardThemeData(
      id: 'custom_${DateTime.now().millisecondsSinceEpoch}',
      name: 'Custom Theme',
      description: 'My custom keyboard theme',
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.isCreatingNew ? 'Create Theme' : 'Edit Theme'),
        backgroundColor: _currentTheme.accentColor,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.preview),
            onPressed: _showPreview,
            tooltip: 'Preview',
          ),
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _saveTheme,
            tooltip: 'Save Theme',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          tabs: const [
            Tab(icon: Icon(Icons.palette), text: 'Colors'),
            Tab(icon: Icon(Icons.wallpaper), text: 'Background'),
            Tab(icon: Icon(Icons.text_fields), text: 'Text'),
            Tab(icon: Icon(Icons.keyboard), text: 'Keys'),
            Tab(icon: Icon(Icons.tune), text: 'Advanced'),
          ],
        ),
      ),
      body: Column(
        children: [
          // Theme name and description
          _buildBasicInfoSection(),
          
          // Tab content
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildColorsTab(),
                _buildBackgroundTab(),
                _buildTextTab(),
                _buildKeysTab(),
                _buildAdvancedTab(),
              ],
            ),
          ),
          
          // Live preview panel
          _buildPreviewPanel(),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: _currentTheme.accentColor,
        onPressed: _applyTheme,
        child: const Icon(Icons.check, color: Colors.white),
        tooltip: 'Apply Theme',
      ),
    );
  }

  Widget _buildBasicInfoSection() {
    return Container(
      padding: const EdgeInsets.all(16.0),
      color: Colors.grey[50],
      child: Column(
        children: [
          TextField(
            controller: _nameController,
            decoration: const InputDecoration(
              labelText: 'Theme Name',
              border: OutlineInputBorder(),
              prefixIcon: Icon(Icons.label),
            ),
            onChanged: (value) {
              setState(() {
                _currentTheme = _currentTheme.copyWith(name: value);
              });
            },
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _descriptionController,
            decoration: const InputDecoration(
              labelText: 'Description',
              border: OutlineInputBorder(),
              prefixIcon: Icon(Icons.description),
            ),
            maxLines: 2,
            onChanged: (value) {
              setState(() {
                _currentTheme = _currentTheme.copyWith(description: value);
              });
            },
          ),
        ],
      ),
    );
  }

  Widget _buildColorsTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionHeader('Background Colors'),
          _buildColorPicker(
            'Keyboard Background',
            _currentTheme.backgroundColor,
            (color) => _updateTheme(_currentTheme.copyWith(backgroundColor: color)),
          ),
          _buildColorPicker(
            'Key Background',
            _currentTheme.keyBackgroundColor,
            (color) => _updateTheme(_currentTheme.copyWith(keyBackgroundColor: color)),
          ),
          _buildColorPicker(
            'Key Pressed',
            _currentTheme.keyPressedColor,
            (color) => _updateTheme(_currentTheme.copyWith(keyPressedColor: color)),
          ),
          
          const SizedBox(height: 24),
          _buildSectionHeader('Text Colors'),
          _buildColorPicker(
            'Key Text',
            _currentTheme.keyTextColor,
            (color) => _updateTheme(_currentTheme.copyWith(keyTextColor: color)),
          ),
          _buildColorPicker(
            'Pressed Text',
            _currentTheme.keyPressedTextColor,
            (color) => _updateTheme(_currentTheme.copyWith(keyPressedTextColor: color)),
          ),
          
          const SizedBox(height: 24),
          _buildSectionHeader('Accent Colors'),
          _buildColorPicker(
            'Primary Accent',
            _currentTheme.accentColor,
            (color) => _updateTheme(_currentTheme.copyWith(accentColor: color)),
          ),
          _buildColorPicker(
            'Special Keys',
            _currentTheme.specialKeyColor,
            (color) => _updateTheme(_currentTheme.copyWith(specialKeyColor: color)),
          ),
          _buildColorPicker(
            'Delete Key',
            _currentTheme.deleteKeyColor,
            (color) => _updateTheme(_currentTheme.copyWith(deleteKeyColor: color)),
          ),
          
          // Suggestion bar now uses unified key colors - no separate controls needed
        ],
      ),
    );
  }

  Widget _buildBackgroundTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionHeader('Background Type'),
          _buildBackgroundTypeSelector(),
          
          const SizedBox(height: 24),
          
          if (_currentTheme.backgroundType == 'gradient') ...[
            _buildSectionHeader('Gradient Settings'),
            _buildGradientControls(),
          ] else if (_currentTheme.backgroundType == 'image') ...[
            _buildSectionHeader('Image Settings'),
            _buildImageControls(),
          ],
          
          const SizedBox(height: 24),
          _buildSectionHeader('Background Preview'),
          _buildBackgroundPreview(),
        ],
      ),
    );
  }

  Widget _buildTextTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionHeader('Key Text'),
          _buildFontSizeSlider(
            'Font Size',
            _currentTheme.fontSize,
            8.0,
            32.0,
            (value) => _updateTheme(_currentTheme.copyWith(fontSize: value)),
          ),
          
          _buildFontFamilySelector(),
          
          _buildTextStyleToggles(
            'Bold',
            _currentTheme.isBold,
            (value) => _updateTheme(_currentTheme.copyWith(isBold: value)),
            'Italic',
            _currentTheme.isItalic,
            (value) => _updateTheme(_currentTheme.copyWith(isItalic: value)),
          ),
          
          // Suggestion text now uses unified key text settings - no separate controls needed
          
          const SizedBox(height: 24),
          _buildSectionHeader('Font Preview'),
          _buildFontPreview(),
        ],
      ),
    );
  }

  Widget _buildKeysTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionHeader('Key Appearance'),
          _buildFontSizeSlider(
            'Corner Radius',
            _currentTheme.keyCornerRadius,
            0.0,
            20.0,
            (value) => _updateTheme(_currentTheme.copyWith(keyCornerRadius: value)),
          ),
          
          _buildSwitchTile(
            'Key Shadows',
            _currentTheme.showKeyShadows,
            (value) => _updateTheme(_currentTheme.copyWith(showKeyShadows: value)),
          ),
          
          if (_currentTheme.showKeyShadows) ...[
            _buildFontSizeSlider(
              'Shadow Depth',
              _currentTheme.shadowDepth,
              0.0,
              10.0,
              (value) => _updateTheme(_currentTheme.copyWith(shadowDepth: value)),
            ),
            _buildColorPicker(
              'Shadow Color',
              _currentTheme.shadowColor,
              (color) => _updateTheme(_currentTheme.copyWith(shadowColor: color)),
            ),
          ],
          
          const SizedBox(height: 24),
          _buildSectionHeader('Key Borders'),
          _buildFontSizeSlider(
            'Border Width',
            _currentTheme.keyBorderWidth,
            0.0,
            5.0,
            (value) => _updateTheme(_currentTheme.copyWith(keyBorderWidth: value)),
          ),
          
          _buildColorPicker(
            'Border Color',
            _currentTheme.keyBorderColor,
            (color) => _updateTheme(_currentTheme.copyWith(keyBorderColor: color)),
          ),
          
          const SizedBox(height: 24),
          _buildSectionHeader('Key Sizing'),
          _buildFontSizeSlider(
            'Key Height',
            _currentTheme.keyHeight,
            32.0,
            80.0,
            (value) => _updateTheme(_currentTheme.copyWith(keyHeight: value)),
          ),
          
          _buildFontSizeSlider(
            'Key Spacing',
            _currentTheme.keySpacing,
            0.0,
            12.0,
            (value) => _updateTheme(_currentTheme.copyWith(keySpacing: value)),
          ),
          
          _buildFontSizeSlider(
            'Row Spacing',
            _currentTheme.rowSpacing,
            0.0,
            16.0,
            (value) => _updateTheme(_currentTheme.copyWith(rowSpacing: value)),
          ),
        ],
      ),
    );
  }

  Widget _buildAdvancedTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionHeader('Material You'),
          _buildSwitchTile(
            'Use Material You Colors',
            _currentTheme.useMaterialYou,
            (value) => _updateTheme(_currentTheme.copyWith(useMaterialYou: value)),
          ),
          
          _buildSwitchTile(
            'Follow System Theme',
            _currentTheme.followSystemTheme,
            (value) => _updateTheme(_currentTheme.copyWith(followSystemTheme: value)),
          ),
          
          if (_currentTheme.useMaterialYou) ...[
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    const Text(
                      'Material You Integration',
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Colors will automatically adapt to your wallpaper and system theme.',
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: _applyMaterialYou,
                      child: const Text('Apply Material You'),
                    ),
                  ],
                ),
              ),
            ),
          ],
          
          const SizedBox(height: 24),
          _buildSectionHeader('Performance'),
          _buildSwitchTile(
            'Enable Animations',
            _currentTheme.enableAnimations,
            (value) => _updateTheme(_currentTheme.copyWith(enableAnimations: value)),
          ),
          
          if (_currentTheme.enableAnimations) ...[
            _buildFontSizeSlider(
              'Animation Duration (ms)',
              _currentTheme.animationDuration.toDouble(),
              50.0,
              500.0,
              (value) => _updateTheme(_currentTheme.copyWith(animationDuration: value.toInt())),
            ),
          ],
          
          const SizedBox(height: 24),
          _buildSectionHeader('Theme Sharing'),
          _buildThemeSharingButtons(),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Text(
        title,
        style: const TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.bold,
          color: Colors.deepPurple,
        ),
      ),
    );
  }

  Widget _buildColorPicker(String label, Color color, Function(Color) onChanged) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4.0),
      child: ListTile(
        title: Text(label),
        trailing: GestureDetector(
          onTap: () => _showColorPicker(color, onChanged),
          child: Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildFontSizeSlider(
    String label,
    double value,
    double min,
    double max,
    Function(double) onChanged,
  ) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4.0),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '$label: ${value.toStringAsFixed(1)}',
              style: const TextStyle(fontWeight: FontWeight.w500),
            ),
            Slider(
              value: value,
              min: min,
              max: max,
              divisions: ((max - min) * 2).toInt(),
              activeColor: _currentTheme.accentColor,
              onChanged: onChanged,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSwitchTile(String title, bool value, Function(bool) onChanged) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4.0),
      child: SwitchListTile(
        title: Text(title),
        value: value,
        activeColor: _currentTheme.accentColor,
        onChanged: onChanged,
      ),
    );
  }

  Widget _buildBackgroundTypeSelector() {
    return Card(
      child: Column(
        children: [
          RadioListTile<String>(
            title: const Text('Solid Color'),
            value: 'solid',
            groupValue: _currentTheme.backgroundType,
            activeColor: _currentTheme.accentColor,
            onChanged: (value) => _updateTheme(
              _currentTheme.copyWith(backgroundType: value),
            ),
          ),
          RadioListTile<String>(
            title: const Text('Gradient'),
            value: 'gradient',
            groupValue: _currentTheme.backgroundType,
            activeColor: _currentTheme.accentColor,
            onChanged: (value) => _updateTheme(
              _currentTheme.copyWith(backgroundType: value),
            ),
          ),
          RadioListTile<String>(
            title: const Text('Custom Image'),
            value: 'image',
            groupValue: _currentTheme.backgroundType,
            activeColor: _currentTheme.accentColor,
            onChanged: (value) => _updateTheme(
              _currentTheme.copyWith(backgroundType: value),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGradientControls() {
    return Column(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Gradient Colors',
                  style: TextStyle(fontWeight: FontWeight.w500),
                ),
                const SizedBox(height: 8),
                Wrap(
                  children: _currentTheme.gradientColors.asMap().entries.map((entry) {
                    final index = entry.key;
                    final color = entry.value;
                    return GestureDetector(
                      onTap: () => _editGradientColor(index, color),
                      child: Container(
                        margin: const EdgeInsets.all(4.0),
                        width: 50,
                        height: 50,
                        decoration: BoxDecoration(
                          color: color,
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: Colors.grey),
                        ),
                      ),
                    );
                  }).toList(),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    ElevatedButton.icon(
                      onPressed: _addGradientColor,
                      icon: const Icon(Icons.add),
                      label: const Text('Add Color'),
                    ),
                    const SizedBox(width: 8),
                    if (_currentTheme.gradientColors.length > 2)
                      ElevatedButton.icon(
                        onPressed: _removeGradientColor,
                        icon: const Icon(Icons.remove),
                        label: const Text('Remove Color'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.red,
                          foregroundColor: Colors.white,
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
        ),
        _buildFontSizeSlider(
          'Gradient Angle',
          _currentTheme.gradientAngle,
          0.0,
          360.0,
          (value) => _updateTheme(_currentTheme.copyWith(gradientAngle: value)),
        ),
      ],
    );
  }

  Widget _buildImageControls() {
    return Column(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                if (_currentTheme.backgroundImagePath != null) ...[
                  Container(
                    height: 120,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8),
                      image: DecorationImage(
                        image: FileImage(File(_currentTheme.backgroundImagePath!)),
                        fit: BoxFit.cover,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton.icon(
                        onPressed: _selectBackgroundImage,
                        icon: const Icon(Icons.image),
                        label: const Text('Select Image'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    if (_currentTheme.backgroundImagePath != null)
                      ElevatedButton.icon(
                        onPressed: _removeBackgroundImage,
                        icon: const Icon(Icons.delete),
                        label: const Text('Remove'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.red,
                          foregroundColor: Colors.white,
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
        ),
        if (_currentTheme.backgroundImagePath != null) ...[
          _buildFontSizeSlider(
            'Image Opacity',
            _currentTheme.backgroundOpacity,
            0.1,
            1.0,
            (value) => _updateTheme(_currentTheme.copyWith(backgroundOpacity: value)),
          ),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Image Scale Type',
                    style: TextStyle(fontWeight: FontWeight.w500),
                  ),
                  RadioListTile<String>(
                    title: const Text('Cover (Fill entire background)'),
                    value: 'cover',
                    groupValue: _currentTheme.imageScaleType,
                    activeColor: _currentTheme.accentColor,
                    onChanged: (value) => _updateTheme(
                      _currentTheme.copyWith(imageScaleType: value),
                    ),
                  ),
                  RadioListTile<String>(
                    title: const Text('Contain (Fit within background)'),
                    value: 'contain',
                    groupValue: _currentTheme.imageScaleType,
                    activeColor: _currentTheme.accentColor,
                    onChanged: (value) => _updateTheme(
                      _currentTheme.copyWith(imageScaleType: value),
                    ),
                  ),
                  RadioListTile<String>(
                    title: const Text('Fill (Stretch to fit)'),
                    value: 'fill',
                    groupValue: _currentTheme.imageScaleType,
                    activeColor: _currentTheme.accentColor,
                    onChanged: (value) => _updateTheme(
                      _currentTheme.copyWith(imageScaleType: value),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildBackgroundPreview() {
    return Container(
      height: 120,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: _buildBackgroundWidget(),
      ),
    );
  }

  Widget _buildBackgroundWidget() {
    switch (_currentTheme.backgroundType) {
      case 'gradient':
        return Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: _currentTheme.gradientColors,
              begin: _getGradientAlignment(_currentTheme.gradientAngle),
              end: _getGradientAlignment(_currentTheme.gradientAngle + 180),
            ),
          ),
        );
      case 'image':
        if (_currentTheme.backgroundImagePath != null) {
          return Opacity(
            opacity: _currentTheme.backgroundOpacity,
            child: Image.file(
              File(_currentTheme.backgroundImagePath!),
              fit: _getImageFit(_currentTheme.imageScaleType),
            ),
          );
        }
        return Container(color: _currentTheme.backgroundColor);
      default:
        return Container(color: _currentTheme.backgroundColor);
    }
  }

  Alignment _getGradientAlignment(double angle) {
    final radians = (angle * 3.14159) / 180;
    return Alignment(
      math.cos(radians),
      math.sin(radians),
    );
  }

  BoxFit _getImageFit(String scaleType) {
    switch (scaleType) {
      case 'contain':
        return BoxFit.contain;
      case 'fill':
        return BoxFit.fill;
      default:
        return BoxFit.cover;
    }
  }

  Widget _buildFontFamilySelector() {
    final fonts = [
      'Roboto',
      'Roboto Mono',
      'Open Sans',
      'Lato',
      'Montserrat',
      'Source Sans Pro',
      'Poppins',
      'Inter',
    ];

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 8.0),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Font Family',
              style: TextStyle(fontWeight: FontWeight.w500),
            ),
            const SizedBox(height: 8),
            DropdownButton<String>(
              value: _currentTheme.fontFamily,
              isExpanded: true,
              items: fonts.map((font) {
                return DropdownMenuItem<String>(
                  value: font,
                  child: Text(
                    font,
                    style: TextStyle(
                      fontFamily: font == 'Roboto' ? null : font.toLowerCase().replaceAll(' ', ''),
                    ),
                  ),
                );
              }).toList(),
              onChanged: (value) {
                if (value != null) {
                  _updateTheme(_currentTheme.copyWith(fontFamily: value));
                }
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTextStyleToggles(
    String label1,
    bool value1,
    Function(bool) onChanged1,
    String label2,
    bool value2,
    Function(bool) onChanged2,
  ) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4.0),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Row(
          children: [
            Expanded(
              child: CheckboxListTile(
                title: Text(label1),
                value: value1,
                activeColor: _currentTheme.accentColor,
                onChanged: (value) => onChanged1(value ?? false),
                controlAffinity: ListTileControlAffinity.trailing,
              ),
            ),
            Expanded(
              child: CheckboxListTile(
                title: Text(label2),
                value: value2,
                activeColor: _currentTheme.accentColor,
                onChanged: (value) => onChanged2(value ?? false),
                controlAffinity: ListTileControlAffinity.trailing,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFontPreview() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Font Preview',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16.0),
              decoration: BoxDecoration(
                color: _currentTheme.keyBackgroundColor,
                borderRadius: BorderRadius.circular(_currentTheme.keyCornerRadius),
                border: Border.all(
                  color: _currentTheme.keyBorderColor,
                  width: _currentTheme.keyBorderWidth,
                ),
              ),
              child: Text(
                'Sample Key Text',
                style: TextStyle(
                  color: _currentTheme.keyTextColor,
                  fontSize: _currentTheme.fontSize,
                  fontFamily: _currentTheme.fontFamily,
                  fontWeight: _currentTheme.isBold ? FontWeight.bold : FontWeight.normal,
                  fontStyle: _currentTheme.isItalic ? FontStyle.italic : FontStyle.normal,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12.0),
              decoration: BoxDecoration(
                color: _currentTheme.suggestionBarColor,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                'Suggestion Sample',
                style: TextStyle(
                  color: _currentTheme.suggestionTextColor,
                  fontSize: _currentTheme.suggestionFontSize,
                  fontFamily: _currentTheme.fontFamily,
                  fontWeight: _currentTheme.suggestionBold ? FontWeight.bold : FontWeight.normal,
                  fontStyle: _currentTheme.suggestionItalic ? FontStyle.italic : FontStyle.normal,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildThemeSharingButtons() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ElevatedButton.icon(
              onPressed: _exportTheme,
              icon: const Icon(Icons.copy),
              label: const Text('Copy Theme JSON'),
            ),
            const SizedBox(height: 8),
            ElevatedButton.icon(
              onPressed: _importTheme,
              icon: const Icon(Icons.paste),
              label: const Text('Paste Theme JSON'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPreviewPanel() {
    return Container(
      height: 100,
      decoration: BoxDecoration(
        color: Colors.grey[100],
        border: const Border(top: BorderSide(color: Colors.grey)),
      ),
      child: SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0, 1),
          end: Offset.zero,
        ).animate(_previewAnimation),
        child: _buildKeyboardPreview(),
      ),
    );
  }

  Widget _buildKeyboardPreview() {
    return Container(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Mini keyboard preview
          Container(
            height: 60,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey),
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Stack(
                children: [
                  // Background
                  _buildBackgroundWidget(),
                  // Sample keys
                  Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        _buildPreviewKey('Q'),
                        _buildPreviewKey('W'),
                        _buildPreviewKey('E'),
                        _buildPreviewKey('R'),
                        _buildPreviewKey('T'),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _currentTheme.name,
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 16,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPreviewKey(String letter) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        color: _currentTheme.keyBackgroundColor,
        borderRadius: BorderRadius.circular(_currentTheme.keyCornerRadius),
        border: _currentTheme.keyBorderWidth > 0
            ? Border.all(
                color: _currentTheme.keyBorderColor,
                width: _currentTheme.keyBorderWidth,
              )
            : null,
        boxShadow: _currentTheme.showKeyShadows
            ? [
                BoxShadow(
                  color: _currentTheme.shadowColor,
                  blurRadius: _currentTheme.shadowDepth,
                  offset: Offset(0, _currentTheme.shadowDepth / 2),
                ),
              ]
            : null,
      ),
      child: Center(
        child: Text(
          letter,
          style: TextStyle(
            color: _currentTheme.keyTextColor,
            fontSize: 12,
            fontWeight: _currentTheme.isBold ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  void _updateTheme(KeyboardThemeData newTheme) {
    setState(() {
      _currentTheme = newTheme;
    });
  }

  void _showColorPicker(Color currentColor, Function(Color) onChanged) {
    // Implementation for color picker dialog
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Select Color'),
        content: SingleChildScrollView(
          child: BlockPicker(
            pickerColor: currentColor,
            onColorChanged: onChanged,
            availableColors: _getColorPalette(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Done'),
          ),
        ],
      ),
    );
  }

  List<Color> _getColorPalette() {
    return [
      // Material Design colors
      Colors.red,
      Colors.pink,
      Colors.purple,
      Colors.deepPurple,
      Colors.indigo,
      Colors.blue,
      Colors.lightBlue,
      Colors.cyan,
      Colors.teal,
      Colors.green,
      Colors.lightGreen,
      Colors.lime,
      Colors.yellow,
      Colors.amber,
      Colors.orange,
      Colors.deepOrange,
      Colors.brown,
      Colors.grey,
      Colors.blueGrey,
      Colors.black,
      Colors.white,
    ];
  }

  void _editGradientColor(int index, Color currentColor) {
    _showColorPicker(currentColor, (newColor) {
      final newColors = List<Color>.from(_currentTheme.gradientColors);
      newColors[index] = newColor;
      _updateTheme(_currentTheme.copyWith(gradientColors: newColors));
    });
  }

  void _addGradientColor() {
    final newColors = List<Color>.from(_currentTheme.gradientColors);
    newColors.add(Colors.blue);
    _updateTheme(_currentTheme.copyWith(gradientColors: newColors));
  }

  void _removeGradientColor() {
    if (_currentTheme.gradientColors.length > 2) {
      final newColors = List<Color>.from(_currentTheme.gradientColors);
      newColors.removeLast();
      _updateTheme(_currentTheme.copyWith(gradientColors: newColors));
    }
  }

  Future<void> _selectBackgroundImage() async {
    final picker = ImagePicker();
    final XFile? image = await picker.pickImage(
      source: ImageSource.gallery,
      maxWidth: 1920,
      maxHeight: 1080,
      imageQuality: 80,
    );

    if (image != null) {
      _updateTheme(_currentTheme.copyWith(backgroundImagePath: image.path));
    }
  }

  void _removeBackgroundImage() {
    _updateTheme(_currentTheme.copyWith(backgroundImagePath: null));
  }

  Future<void> _applyMaterialYou() async {
    try {
      await FlutterThemeManager.instance.applyMaterialYouColors();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Material You colors applied!')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Material You not available on this device')),
        );
      }
    }
  }

  Future<void> _exportTheme() async {
    final themeJson = FlutterThemeManager.instance.exportTheme(_currentTheme);
    
    // Copy to clipboard
    await Clipboard.setData(ClipboardData(text: themeJson));
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Theme exported to clipboard!')),
      );
    }
  }

  Future<void> _importTheme() async {
    // Show dialog to paste theme JSON
    final controller = TextEditingController();
    
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Import Theme'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Paste your theme JSON code below:'),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              maxLines: 8,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: 'Paste theme JSON here...',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () async {
              // Try to paste from clipboard if field is empty
              if (controller.text.isEmpty) {
                try {
                  final clipboardData = await Clipboard.getData(Clipboard.kTextPlain);
                  if (clipboardData?.text != null) {
                    controller.text = clipboardData!.text!;
                  }
                } catch (e) {
                  // Ignore clipboard errors
                }
              }
              Navigator.pop(context, true);
            },
            child: const Text('Import'),
          ),
        ],
      ),
    );

    if (result == true && controller.text.isNotEmpty) {
      try {
        final theme = await FlutterThemeManager.instance.importTheme(controller.text);
        
        if (theme != null && mounted) {
          setState(() {
            _currentTheme = theme;
            _nameController.text = theme.name;
            _descriptionController.text = theme.description;
          });
          
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Theme imported successfully!')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Error importing theme: Invalid JSON')),
          );
        }
      }
    }
  }

  void _showPreview() {
    showDialog(
      context: context,
      builder: (context) => Dialog(
        child: Container(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Theme Preview',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: _currentTheme.accentColor,
                ),
              ),
              const SizedBox(height: 16),
              Container(
                height: 200,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.grey),
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: _buildFullKeyboardPreview(),
                ),
              ),
              const SizedBox(height: 16),
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Close'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFullKeyboardPreview() {
    // This would show a more complete keyboard preview
    return Container(
      color: _currentTheme.backgroundColor,
      child: const Center(
        child: Text(
          'Full Keyboard Preview\n(Implementation needed)',
          textAlign: TextAlign.center,
        ),
      ),
    );
  }

  Future<void> _saveTheme() async {
    await FlutterThemeManager.instance.saveCustomTheme(_currentTheme);
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Theme saved!')),
      );
    }
  }

  Future<void> _applyTheme() async {
    await FlutterThemeManager.instance.applyTheme(_currentTheme);
    
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Theme applied to keyboard!')),
      );
      
      Navigator.pop(context, _currentTheme);
    }
  }
}

// Simple color picker widget (you might want to use a package like flutter_colorpicker)
class BlockPicker extends StatelessWidget {
  final Color pickerColor;
  final Function(Color) onColorChanged;
  final List<Color> availableColors;

  const BlockPicker({
    super.key,
    required this.pickerColor,
    required this.onColorChanged,
    required this.availableColors,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8.0,
      runSpacing: 8.0,
      children: availableColors.map((color) {
        final isSelected = color.value == pickerColor.value;
        return GestureDetector(
          onTap: () => onColorChanged(color),
          child: Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(
                color: isSelected ? Colors.black : Colors.grey,
                width: isSelected ? 3 : 1,
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}

