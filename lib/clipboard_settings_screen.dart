import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ClipboardSettingsScreen extends StatefulWidget {
  const ClipboardSettingsScreen({super.key});

  @override
  State<ClipboardSettingsScreen> createState() => _ClipboardSettingsScreenState();
}

class _ClipboardSettingsScreenState extends State<ClipboardSettingsScreen> {
  static const platform = MethodChannel('ai_keyboard/config');
  
  // Settings state
  bool _clipboardEnabled = true;
  double _maxHistorySize = 20.0;
  bool _autoExpiryEnabled = true;
  double _expiryDurationMinutes = 60.0;
  
  // Templates state
  List<ClipboardTemplate> _templates = [];
  final _templateController = TextEditingController();
  final _categoryController = TextEditingController();
  
  @override
  void initState() {
    super.initState();
    _loadSettings();
  }
  
  @override
  void dispose() {
    _templateController.dispose();
    _categoryController.dispose();
    super.dispose();
  }
  
  Future<void> _loadSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      setState(() {
        _clipboardEnabled = prefs.getBool('clipboard_enabled') ?? true;
        _maxHistorySize = (prefs.getInt('max_history_size') ?? 20).toDouble();
        _autoExpiryEnabled = prefs.getBool('auto_expiry_enabled') ?? true;
        _expiryDurationMinutes = (prefs.getInt('expiry_duration_minutes') ?? 60).toDouble();
      });
      
      // Load templates
      final templatesJson = prefs.getStringList('clipboard_templates') ?? [];
      _templates = templatesJson.map((json) => ClipboardTemplate.fromJson(json)).toList();
      
    } catch (e) {
      debugPrint('Error loading clipboard settings: $e');
    }
  }
  
  Future<void> _saveSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('clipboard_enabled', _clipboardEnabled);
      await prefs.setInt('max_history_size', _maxHistorySize.toInt());
      await prefs.setBool('auto_expiry_enabled', _autoExpiryEnabled);
      await prefs.setInt('expiry_duration_minutes', _expiryDurationMinutes.toInt());
      
      // Save templates
      final templatesJson = _templates.map((template) => template.toJson()).toList();
      await prefs.setStringList('clipboard_templates', templatesJson);
      
      // Notify Android keyboard service
      await _updateAndroidSettings();
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Clipboard settings saved!')),
        );
      }
    } catch (e) {
      debugPrint('Error saving clipboard settings: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error saving settings: $e')),
        );
      }
    }
  }
  
  Future<void> _updateAndroidSettings() async {
    try {
      final templatesData = _templates.map((template) => {
        'id': template.id,
        'text': template.text,
        'category': template.category,
      }).toList();
      
      await platform.invokeMethod('updateClipboardSettings', {
        'enabled': _clipboardEnabled,
        'maxHistorySize': _maxHistorySize.toInt(),
        'autoExpiryEnabled': _autoExpiryEnabled,
        'expiryDurationMinutes': _expiryDurationMinutes.toInt(),
        'templates': templatesData,
      });
    } catch (e) {
      debugPrint('Error updating Android clipboard settings: $e');
    }
  }
  
  void _addTemplate() {
    if (_templateController.text.trim().isEmpty) return;
    
    final template = ClipboardTemplate(
      text: _templateController.text.trim(),
      category: _categoryController.text.trim().isEmpty ? null : _categoryController.text.trim(),
    );
    
    setState(() {
      _templates.add(template);
    });
    
    _templateController.clear();
    _categoryController.clear();
    
    Navigator.of(context).pop();
    _saveSettings();
  }
  
  void _deleteTemplate(int index) {
    setState(() {
      _templates.removeAt(index);
    });
    _saveSettings();
  }
  
  void _showAddTemplateDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Add Template'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _templateController,
              decoration: const InputDecoration(
                labelText: 'Template Text',
                hintText: 'Enter template text...',
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _categoryController,
              decoration: const InputDecoration(
                labelText: 'Category (Optional)',
                hintText: 'e.g., Email, Address, etc.',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: _addTemplate,
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Clipboard Settings'),
        backgroundColor: Colors.deepPurple,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _saveSettings,
            tooltip: 'Save Settings',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          _buildGeneralSettings(),
          const SizedBox(height: 24),
          _buildHistorySettings(),
          const SizedBox(height: 24),
          _buildExpirySettings(),
          const SizedBox(height: 24),
          _buildTemplatesSection(),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddTemplateDialog,
        backgroundColor: Colors.deepPurple,
        child: const Icon(Icons.add, color: Colors.white),
        tooltip: 'Add Template',
      ),
    );
  }
  
  Widget _buildGeneralSettings() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'General Settings',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            SwitchListTile(
              title: const Text('Enable Clipboard History'),
              subtitle: const Text('Track copied text for quick access'),
              value: _clipboardEnabled,
              onChanged: (value) {
                setState(() {
                  _clipboardEnabled = value;
                });
              },
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildHistorySettings() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'History Size',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            Text(
              'Maximum items in history: ${_maxHistorySize.toInt()}',
              style: const TextStyle(fontSize: 16),
            ),
            Slider(
              value: _maxHistorySize,
              min: 5.0,
              max: 50.0,
              divisions: 9,
              label: '${_maxHistorySize.toInt()} items',
              onChanged: _clipboardEnabled ? (value) {
                setState(() {
                  _maxHistorySize = value;
                });
              } : null,
            ),
            const Text(
              'Older items beyond this limit will be automatically removed',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildExpirySettings() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Auto-Expiry',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            SwitchListTile(
              title: const Text('Auto-expire items'),
              subtitle: const Text('Remove old items automatically'),
              value: _autoExpiryEnabled,
              onChanged: _clipboardEnabled ? (value) {
                setState(() {
                  _autoExpiryEnabled = value;
                });
              } : null,
            ),
            if (_autoExpiryEnabled) ...[
              const SizedBox(height: 16),
              Text(
                'Expire after: ${_expiryDurationMinutes.toInt()} minutes',
                style: const TextStyle(fontSize: 16),
              ),
              Slider(
                value: _expiryDurationMinutes,
                min: 5.0,
                max: 1440.0, // 24 hours
                divisions: 20,
                label: _buildDurationLabel(_expiryDurationMinutes.toInt()),
                onChanged: (value) {
                  setState(() {
                    _expiryDurationMinutes = value;
                  });
                },
              ),
              const Text(
                'Pinned items and templates never expire',
                style: TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ],
          ],
        ),
      ),
    );
  }
  
  Widget _buildTemplatesSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Quick Templates',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                Text(
                  '${_templates.length} templates',
                  style: const TextStyle(color: Colors.grey),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_templates.isEmpty)
              const Padding(
                padding: EdgeInsets.all(32.0),
                child: Center(
                  child: Column(
                    children: [
                      Icon(Icons.content_paste, size: 48, color: Colors.grey),
                      SizedBox(height: 16),
                      Text(
                        'No templates yet',
                        style: TextStyle(fontSize: 16, color: Colors.grey),
                      ),
                      Text(
                        'Tap + to add frequently used text',
                        style: TextStyle(fontSize: 12, color: Colors.grey),
                      ),
                    ],
                  ),
                ),
              )
            else
              ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: _templates.length,
                itemBuilder: (context, index) {
                  final template = _templates[index];
                  return ListTile(
                    title: Text(
                      template.text,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    subtitle: template.category != null 
                      ? Text('Category: ${template.category}')
                      : null,
                    trailing: IconButton(
                      icon: const Icon(Icons.delete, color: Colors.red),
                      onPressed: () => _deleteTemplate(index),
                    ),
                    onTap: () {
                      // Copy template to clipboard
                      Clipboard.setData(ClipboardData(text: template.text));
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Copied: ${template.text}')),
                      );
                    },
                  );
                },
              ),
          ],
        ),
      ),
    );
  }
  
  String _buildDurationLabel(int minutes) {
    if (minutes < 60) {
      return '$minutes min';
    } else if (minutes < 1440) {
      final hours = minutes ~/ 60;
      return '$hours hr';
    } else {
      final days = minutes ~/ 1440;
      return '$days day';
    }
  }
}

class ClipboardTemplate {
  final String id;
  final String text;
  final String? category;
  
  ClipboardTemplate({
    String? id,
    required this.text,
    this.category,
  }) : id = id ?? DateTime.now().millisecondsSinceEpoch.toString();
  
  String toJson() {
    return '{"id":"$id","text":"$text","category":"${category ?? ""}"}';
  }
  
  static ClipboardTemplate fromJson(String json) {
    final map = json.split(',').fold<Map<String, String>>({}, (acc, pair) {
      final parts = pair.split(':');
      if (parts.length == 2) {
        final key = parts[0].replaceAll(RegExp(r'[{"}]'), '');
        final value = parts[1].replaceAll(RegExp(r'["}]'), '');
        acc[key] = value;
      }
      return acc;
    });
    
    return ClipboardTemplate(
      id: map['id'],
      text: map['text'] ?? '',
      category: map['category']?.isEmpty == true ? null : map['category'],
    );
  }
}
