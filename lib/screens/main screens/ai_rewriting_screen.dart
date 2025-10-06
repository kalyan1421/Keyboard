import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/custom_toggle_switch.dart';

class AiRewritingScreen extends StatefulWidget {
  const AiRewritingScreen({super.key});

  @override
  State<AiRewritingScreen> createState() => _AiRewritingScreenState();
}

class _AiRewritingScreenState extends State<AiRewritingScreen> {
  static const platform = MethodChannel('ai_keyboard/config');
  
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _instructionController = TextEditingController();
  
  List<CustomPrompt> _grammarPrompts = [];
  List<CustomPrompt> _tonePrompts = [];
  List<CustomPrompt> _assistantPrompts = [];
  
  int? _editingIndex;
  String _editingCategory = '';
  
  // Built-in AI actions
  bool grammarEnabled = true;
  bool formalEnabled = true;
  bool conciseEnabled = true;
  bool expandEnabled = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _instructionController.dispose();
    super.dispose();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    
    // Load built-in action toggles
    setState(() {
      grammarEnabled = prefs.getBool('flutter.ai_action_grammar') ?? true;
      formalEnabled = prefs.getBool('flutter.ai_action_formal') ?? true;
      conciseEnabled = prefs.getBool('flutter.ai_action_concise') ?? true;
      expandEnabled = prefs.getBool('flutter.ai_action_expand') ?? true;
    });
    
    // Load custom prompts
    final grammarJson = prefs.getString('flutter.ai_custom_grammar') ?? '[]';
    final toneJson = prefs.getString('flutter.ai_custom_tones') ?? '[]';
    final assistantJson = prefs.getString('flutter.ai_custom_assistants') ?? '[]';
    
    setState(() {
      _grammarPrompts = _parsePrompts(grammarJson);
      _tonePrompts = _parsePrompts(toneJson);
      _assistantPrompts = _parsePrompts(assistantJson);
    });
  }

  List<CustomPrompt> _parsePrompts(String json) {
    try {
      final List<dynamic> decoded = jsonDecode(json);
      return decoded.map((item) => CustomPrompt.fromJson(item)).toList();
    } catch (e) {
      return [];
    }
  }

  Future<void> _saveBuiltInActions() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('flutter.ai_action_grammar', grammarEnabled);
    await prefs.setBool('flutter.ai_action_formal', formalEnabled);
    await prefs.setBool('flutter.ai_action_concise', conciseEnabled);
    await prefs.setBool('flutter.ai_action_expand', expandEnabled);
    await _notifyKeyboard();
  }

  Future<void> _saveCustomPrompts(String category) async {
    final prefs = await SharedPreferences.getInstance();
    List<CustomPrompt> prompts;
    String key;
    
    switch (category) {
      case 'grammar':
        prompts = _grammarPrompts;
        key = 'flutter.ai_custom_grammar';
        break;
      case 'tone':
        prompts = _tonePrompts;
        key = 'flutter.ai_custom_tones';
        break;
      case 'assistant':
        prompts = _assistantPrompts;
        key = 'flutter.ai_custom_assistants';
        break;
      default:
        return;
    }
    
    final json = jsonEncode(prompts.map((p) => p.toJson()).toList());
    await prefs.setString(key, json);
    await _notifyKeyboard();
  }

  Future<void> _notifyKeyboard() async {
    try {
      await platform.invokeMethod('updateCustomPrompts');
      await platform.invokeMethod('settingsChanged');
    } catch (e) {
      print('Error notifying keyboard: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'AI Rewriting',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),
        centerTitle: false,
        backgroundColor: AppColors.primary,
        elevation: 0,
        actions: [
          Stack(
            children: [
              const Icon(Icons.notifications, color: AppColors.white, size: 24),
              Positioned(
                right: 0,
                top: 0,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(width: 16),
        ],
      ),
      backgroundColor: AppColors.white,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 8),
            
            // Built-in AI Actions Section
            _buildSectionTitle('Built-in AI Actions'),
            const SizedBox(height: 16),
            _buildToggleSetting(
              title: 'Fix Grammar',
              description: 'Correct grammar and spelling errors',
              value: grammarEnabled,
              onChanged: (value) {
                setState(() => grammarEnabled = value);
                _saveBuiltInActions();
              },
            ),
            const SizedBox(height: 12),
            _buildToggleSetting(
              title: 'Formal Tone',
              description: 'Make text more professional',
              value: formalEnabled,
              onChanged: (value) {
                setState(() => formalEnabled = value);
                _saveBuiltInActions();
              },
            ),
            const SizedBox(height: 12),
            _buildToggleSetting(
              title: 'Make Concise',
              description: 'Shorten text while keeping meaning',
              value: conciseEnabled,
              onChanged: (value) {
                setState(() => conciseEnabled = value);
                _saveBuiltInActions();
              },
            ),
            const SizedBox(height: 12),
            _buildToggleSetting(
              title: 'Expand Text',
              description: 'Add more details and context',
              value: expandEnabled,
              onChanged: (value) {
                setState(() => expandEnabled = value);
                _saveBuiltInActions();
              },
            ),

            const SizedBox(height: 32),

            // Custom Grammar Fixers Section
            _buildSectionTitle('Custom Grammar Fixers'),
            const SizedBox(height: 12),
            _buildAddButton('Add Grammar Fixer', () => _showAddPromptDialog('grammar')),
            const SizedBox(height: 12),
            ..._grammarPrompts.asMap().entries.map((entry) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _buildPromptCard(entry.value, 'grammar', entry.key),
              );
            }).toList(),

            const SizedBox(height: 32),

            // Custom Tones Section
            _buildSectionTitle('Custom Tones'),
            const SizedBox(height: 12),
            _buildAddButton('Add Custom Tone', () => _showAddPromptDialog('tone')),
            const SizedBox(height: 12),
            ..._tonePrompts.asMap().entries.map((entry) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _buildPromptCard(entry.value, 'tone', entry.key),
              );
            }).toList(),

            const SizedBox(height: 32),

            // AI Assistants Section
            _buildSectionTitle('AI Assistants'),
            const SizedBox(height: 12),
            _buildAddButton('Add AI Assistant', () => _showAddPromptDialog('assistant')),
            const SizedBox(height: 12),
            ..._assistantPrompts.asMap().entries.map((entry) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _buildPromptCard(entry.value, 'assistant', entry.key),
              );
            }).toList(),

            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: AppTextStyle.titleMedium.copyWith(
        color: AppColors.secondary,
        fontWeight: FontWeight.w600,
      ),
    );
  }

  Widget _buildToggleSetting({
    required String title,
    required String description,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppTextStyle.titleLarge.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
                ),
              ],
            ),
          ),
          CustomToggleSwitch(
            value: value,
            onChanged: onChanged,
            width: 48.0,
            height: 16.0,
            knobSize: 24.0,
          ),
        ],
      ),
    );
  }

  Widget _buildAddButton(String text, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.secondary,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.add, color: AppColors.white),
            const SizedBox(width: 8),
            Text(
              text,
              style: AppTextStyle.titleMedium.copyWith(
                color: AppColors.white,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPromptCard(CustomPrompt prompt, String category, int index) {
    return GestureDetector(
      onTap: () => _showEditPromptDialog(category, index),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.lightGrey,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    prompt.title,
                    style: AppTextStyle.titleLarge.copyWith(
                      color: AppColors.black,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    prompt.instruction,
                    style: AppTextStyle.bodySmall.copyWith(
                      color: AppColors.grey,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            IconButton(
              icon: const Icon(Icons.delete, color: AppColors.grey),
              onPressed: () => _deletePrompt(category, index),
            ),
          ],
        ),
      ),
    );
  }

  void _showAddPromptDialog(String category) {
    _editingIndex = null;
    _editingCategory = category;
    _titleController.clear();
    _instructionController.clear();
    _showPromptDialog();
  }

  void _showEditPromptDialog(String category, int index) {
    _editingIndex = index;
    _editingCategory = category;
    
    CustomPrompt prompt;
    switch (category) {
      case 'grammar':
        prompt = _grammarPrompts[index];
        break;
      case 'tone':
        prompt = _tonePrompts[index];
        break;
      case 'assistant':
        prompt = _assistantPrompts[index];
        break;
      default:
        return;
    }
    
    _titleController.text = prompt.title;
    _instructionController.text = prompt.instruction;
    _showPromptDialog();
  }

  void _showPromptDialog() {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: AppColors.white,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        _editingIndex == null ? 'Add Prompt' : 'Edit Prompt',
                        style: AppTextStyle.titleLarge.copyWith(
                          color: AppColors.black,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    GestureDetector(
                      onTap: () => Navigator.of(context).pop(),
                      child: Container(
                        width: 32,
                        height: 32,
                        decoration: BoxDecoration(
                          color: AppColors.lightGrey,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.close,
                          color: AppColors.black,
                          size: 20,
                        ),
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 16),
                Container(height: 1, color: AppColors.lightGrey),
                const SizedBox(height: 16),

                // Title Field
                Text(
                  'Title',
                  style: AppTextStyle.titleMedium.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.lightGrey,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: TextField(
                    controller: _titleController,
                    decoration: InputDecoration(
                      hintText: 'e.g., Business Email',
                      hintStyle: AppTextStyle.bodyMedium.copyWith(
                        color: AppColors.grey,
                      ),
                      border: InputBorder.none,
                    ),
                    style: AppTextStyle.bodyMedium.copyWith(
                      color: AppColors.black,
                    ),
                  ),
                ),

                const SizedBox(height: 16),

                // Instruction Field
                Text(
                  'Instruction (use {text} for selected text)',
                  style: AppTextStyle.titleMedium.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.lightGrey,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: TextField(
                    controller: _instructionController,
                    maxLines: 4,
                    decoration: InputDecoration(
                      hintText: 'e.g., Rewrite {text} in a professional business tone',
                      hintStyle: AppTextStyle.bodyMedium.copyWith(
                        color: AppColors.grey,
                      ),
                      border: InputBorder.none,
                    ),
                    style: AppTextStyle.bodyMedium.copyWith(
                      color: AppColors.black,
                    ),
                  ),
                ),

                const SizedBox(height: 24),

                // Action Buttons
                Row(
                  children: [
                    Expanded(
                      child: GestureDetector(
                        onTap: () => Navigator.of(context).pop(),
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          decoration: BoxDecoration(
                            color: AppColors.lightGrey,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            'Cancel',
                            style: AppTextStyle.buttonPrimary.copyWith(
                              color: AppColors.black,
                              fontWeight: FontWeight.w600,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: GestureDetector(
                        onTap: () {
                          _savePrompt();
                          Navigator.of(context).pop();
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          decoration: BoxDecoration(
                            color: AppColors.secondary,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            'Save',
                            style: AppTextStyle.buttonPrimary.copyWith(
                              color: AppColors.white,
                              fontWeight: FontWeight.w600,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _savePrompt() {
    if (_titleController.text.trim().isEmpty || 
        _instructionController.text.trim().isEmpty) {
      return;
    }

    final prompt = CustomPrompt(
      title: _titleController.text.trim(),
      instruction: _instructionController.text.trim(),
    );

    setState(() {
      switch (_editingCategory) {
        case 'grammar':
          if (_editingIndex == null) {
            _grammarPrompts.add(prompt);
          } else {
            _grammarPrompts[_editingIndex!] = prompt;
          }
          break;
        case 'tone':
          if (_editingIndex == null) {
            _tonePrompts.add(prompt);
          } else {
            _tonePrompts[_editingIndex!] = prompt;
          }
          break;
        case 'assistant':
          if (_editingIndex == null) {
            _assistantPrompts.add(prompt);
          } else {
            _assistantPrompts[_editingIndex!] = prompt;
          }
          break;
      }
    });

    _saveCustomPrompts(_editingCategory);
  }

  void _deletePrompt(String category, int index) {
    setState(() {
      switch (category) {
        case 'grammar':
          _grammarPrompts.removeAt(index);
          break;
        case 'tone':
          _tonePrompts.removeAt(index);
          break;
        case 'assistant':
          _assistantPrompts.removeAt(index);
          break;
      }
    });
    _saveCustomPrompts(category);
  }
}

class CustomPrompt {
  final String title;
  final String instruction;

  CustomPrompt({
    required this.title,
    required this.instruction,
  });

  Map<String, dynamic> toJson() => {
    'title': title,
    'instruction': instruction,
  };

  factory CustomPrompt.fromJson(Map<String, dynamic> json) => CustomPrompt(
    title: json['title'] ?? '',
    instruction: json['instruction'] ?? '',
  );
}
