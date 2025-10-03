import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';

class AiRewritingScreen extends StatefulWidget {
  const AiRewritingScreen({super.key});

  @override
  State<AiRewritingScreen> createState() => _AiRewritingScreenState();
}

class _AiRewritingScreenState extends State<AiRewritingScreen> {
  final TextEditingController _promptController = TextEditingController();
  final TextEditingController _configNameController = TextEditingController();
  List<PromptItem> _prompts = [
    PromptItem(title: 'Prompt1', description: 'This is example of prompt.'),
    PromptItem(title: 'Prompt2', description: 'This is example of prompt.'),
    PromptItem(title: 'Prompt3', description: 'This is example of prompt.'),
    PromptItem(title: 'Prompt4', description: 'This is example of prompt.'),
  ];
  int? _editingPromptIndex;

  @override
  void dispose() {
    _promptController.dispose();
    _configNameController.dispose();
    super.dispose();
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
          'Ai Rewriting',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),
        centerTitle: false,
        backgroundColor: AppColors.primary,
        elevation: 0,
        actionsPadding: const EdgeInsets.only(right: 16),
        actions: [
          Stack(
            children: [
              Icon(Icons.notifications, color: AppColors.white, size: 24),
              Positioned(
                right: 0,
                top: 0,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
      backgroundColor: AppColors.white,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 8),

            // Word Section
            _buildSectionTitle('Word'),
            const SizedBox(height: 12),
            _buildCustomToneCard(),

            const SizedBox(height: 24),

            // Prompt Section
            _buildSectionTitle('Prompt'),
            const SizedBox(height: 12),
            _buildGrammarPromptCard(),
            const SizedBox(height: 12),
            _buildPromptInputField(),
            const SizedBox(height: 16),
            _buildPromptList(),
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

  Widget _buildCustomToneCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Custom Tone',
            style: AppTextStyle.titleLarge.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Customize your self of Word Tone',
            style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
          ),
        ],
      ),
    );
  }

  Widget _buildGrammarPromptCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.secondary,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Grammar Prompt',
            style: AppTextStyle.titleLarge.copyWith(
              color: AppColors.white,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Add Grammar Prompt for Advance Grammar',
            style: AppTextStyle.bodySmall.copyWith(
              color: AppColors.white.withOpacity(0.8),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPromptInputField() {
    return Row(
      children: [
        Expanded(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.lightGrey,
              borderRadius: BorderRadius.circular(12),
            ),
            child: TextField(
              controller: _promptController,
              decoration: InputDecoration(
                hintText: 'Text or Past Prompt here...',
                hintStyle: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.grey,
                ),
                border: InputBorder.none,
              ),
              style: AppTextStyle.bodyMedium.copyWith(color: AppColors.black),
            ),
          ),
        ),
        const SizedBox(width: 12),
        GestureDetector(
          onTap: _addPrompt,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.secondary,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              '+ Add',
              style: AppTextStyle.buttonPrimary.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPromptList() {
    return Column(
      children: _prompts.asMap().entries.map((entry) {
        int index = entry.key;
        PromptItem prompt = entry.value;
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _buildPromptCard(prompt, index),
        );
      }).toList(),
    );
  }

  Widget _buildPromptCard(PromptItem prompt, int index) {
    return GestureDetector(
      onTap: () => _editPrompt(index),
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
                    prompt.description,
                    style: AppTextStyle.bodySmall.copyWith(
                      color: AppColors.grey,
                    ),
                  ),
                ],
              ),
            ),
            _buildDragHandle(),
          ],
        ),
      ),
    );
  }

  Widget _buildDragHandle() {
    return Column(
      children: List.generate(3, (rowIndex) {
        return Row(
          children: List.generate(2, (colIndex) {
            return Container(
              width: 4,
              height: 4,
              margin: const EdgeInsets.all(1),
              decoration: BoxDecoration(
                color: AppColors.grey,
                shape: BoxShape.circle,
              ),
            );
          }),
        );
      }),
    );
  }

  void _addPrompt() {
    _editingPromptIndex = null;
    _configNameController.clear();
    _promptController.clear();
    _showConfigurationDialog();
  }

  void _editPrompt(int index) {
    _editingPromptIndex = index;
    _configNameController.text = _prompts[index].title;
    _promptController.text = _prompts[index].description;
    _showConfigurationDialog();
  }

  void _showConfigurationDialog() {
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
                        'Configuration Name',
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
                        child: Icon(
                          Icons.close,
                          color: AppColors.black,
                          size: 20,
                        ),
                      ),
                    ),
                  ],
                ),

                // Separator
                const SizedBox(height: 16),
                Container(height: 1, color: AppColors.lightGrey),
                const SizedBox(height: 16),

                // Configuration Name Label
                Text(
                  'Configuration Name',
                  style: AppTextStyle.titleMedium.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 8),

                // Input Field
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.lightGrey,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: TextField(
                    controller: _configNameController,
                    decoration: InputDecoration(
                      hintText: 'Prompt1',
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
                            color: AppColors.secondary,
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
                        onTap: () => _savePrompt(),
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          decoration: BoxDecoration(
                            color: AppColors.secondary,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            'Save',
                            style: AppTextStyle.buttonPrimary.copyWith(
                              color: AppColors.black,
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
    if (_configNameController.text.trim().isNotEmpty) {
      setState(() {
        if (_editingPromptIndex != null) {
          // Editing existing prompt
          _prompts[_editingPromptIndex!] = PromptItem(
            title: _configNameController.text.trim(),
            description: _promptController.text.trim().isNotEmpty
                ? _promptController.text.trim()
                : _prompts[_editingPromptIndex!].description,
          );
        } else {
          // Adding new prompt
          _prompts.add(
            PromptItem(
              title: _configNameController.text.trim(),
              description: _promptController.text.trim().isNotEmpty
                  ? _promptController.text.trim()
                  : 'This is example of prompt.',
            ),
          );
        }
      });
      Navigator.of(context).pop();
      _configNameController.clear();
      _promptController.clear();
    }
  }
}

class PromptItem {
  final String title;
  final String description;

  PromptItem({required this.title, required this.description});
}
