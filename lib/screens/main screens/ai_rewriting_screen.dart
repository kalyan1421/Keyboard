import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';

// class AiRewritingScreen extends StatefulWidget {
//   const AiRewritingScreen({super.key});

//   @override
//   State<AiRewritingScreen> createState() => _AiRewritingScreenState();
// }

// class _AiRewritingScreenState extends State<AiRewritingScreen> {
//   // AI Writing Assist Settings
//   bool aiEnabled = true;
//   bool rewriteEnabled = true;
//   bool translateEnabled = true;
//   bool composeEnabled = true;
//   String targetLanguage = 'en';
//   String aiModel = 'gpt-3.5-turbo';
//   double aiTemperature = 0.7;
//   double aiMaxTokens = 150.0;
  
//   // AI Action Toggles
//   bool grammarAction = true;
//   bool formalAction = true;
//   bool friendlyAction = true;
//   bool conciseAction = true;
//   bool expandAction = true;
  
//   // Privacy Settings
//   bool perAppPrivacy = false;
//   Map<String, bool> appPrivacySettings = {};

//   @override
//   void initState() {
//     super.initState();
//     _loadSettings();
//   }

//   Future<void> _loadSettings() async {
//     final prefs = await SharedPreferences.getInstance();
//     setState(() {
//       aiEnabled = prefs.getBool('flutter.ai_enabled') ?? true;
//       rewriteEnabled = prefs.getBool('flutter.ai_rewrite_enabled') ?? true;
//       translateEnabled = prefs.getBool('flutter.ai_translate_enabled') ?? true;
//       composeEnabled = prefs.getBool('flutter.ai_compose_enabled') ?? true;
//       targetLanguage = prefs.getString('flutter.ai_target_language') ?? 'en';
//       aiModel = prefs.getString('flutter.ai_model') ?? 'gpt-3.5-turbo';
//       aiTemperature = prefs.getDouble('flutter.ai_temp') ?? 0.7;
//       aiMaxTokens = prefs.getDouble('flutter.ai_max_tokens') ?? 150.0;
      
//       grammarAction = prefs.getBool('flutter.ai_action_grammar') ?? true;
//       formalAction = prefs.getBool('flutter.ai_action_formal') ?? true;
//       friendlyAction = prefs.getBool('flutter.ai_action_friendly') ?? true;
//       conciseAction = prefs.getBool('flutter.ai_action_concise') ?? true;
//       expandAction = prefs.getBool('flutter.ai_action_expand') ?? true;
//       perAppPrivacy = prefs.getBool('flutter.ai_per_app_privacy') ?? false;
//     });
//   }

//   Future<void> _saveSettings() async {
//     final prefs = await SharedPreferences.getInstance();
//     await prefs.setBool('flutter.ai_enabled', aiEnabled);
//     await prefs.setBool('flutter.ai_rewrite_enabled', rewriteEnabled);
//     await prefs.setBool('flutter.ai_translate_enabled', translateEnabled);
//     await prefs.setBool('flutter.ai_compose_enabled', composeEnabled);
//     await prefs.setString('flutter.ai_target_language', targetLanguage);
//     await prefs.setString('flutter.ai_model', aiModel);
//     await prefs.setDouble('flutter.ai_temp', aiTemperature);
//     await prefs.setDouble('flutter.ai_max_tokens', aiMaxTokens);
    
//     await prefs.setBool('flutter.ai_action_grammar', grammarAction);
//     await prefs.setBool('flutter.ai_action_formal', formalAction);
//     await prefs.setBool('flutter.ai_action_friendly', friendlyAction);
//     await prefs.setBool('flutter.ai_action_concise', conciseAction);
//     await prefs.setBool('flutter.ai_action_expand', expandAction);
//     await prefs.setBool('flutter.ai_per_app_privacy', perAppPrivacy);
    
//     // Notify keyboard service
//     _notifyKeyboardService();
//   }
=======
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
>>>>>>> pr-2

//   void _notifyKeyboardService() async {
//     // Trigger settings update via broadcast
//     final prefs = await SharedPreferences.getInstance();
//     await prefs.setInt('flutter.settings_version', 
//         (prefs.getInt('flutter.settings_version') ?? 0) + 1);
//   }

<<<<<<< HEAD
//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(
//         leading: IconButton(
//           icon: const Icon(Icons.arrow_back, color: AppColors.white),
//           onPressed: () => Navigator.pop(context),
//         ),
//         title: Text(
//           'Ai Rewriting',
//           style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
//         ),
//         centerTitle: false,
//         backgroundColor: AppColors.primary,
//         elevation: 0,
//         actionsPadding: const EdgeInsets.only(right: 16),
//         actions: [
//           Stack(
//             children: [
//               Icon(Icons.notifications, color: AppColors.white, size: 24),
//               Positioned(
//                 right: 0,
//                 top: 0,
//                 child: Container(
//                   width: 8,
//                   height: 8,
//                   decoration: BoxDecoration(
//                     color: AppColors.secondary,
//                     shape: BoxShape.circle,
//                   ),
//                 ),
//               ),
//             ],
//           ),
//         ],
//       ),
//       backgroundColor: AppColors.white,
//       body: SingleChildScrollView(
//         padding: const EdgeInsets.all(16),
//         child: Column(
//           crossAxisAlignment: CrossAxisAlignment.start,
//           children: [
//             const SizedBox(height: 8),

//             // Glide Typing Section
//             _buildSectionTitle('Glide typing'),
//             const SizedBox(height: 16),

//             // Glide typing
//             _buildToggleSetting(
//               title: 'Glide typing',
//               description: 'Enabled',
//               value: glideTyping,
//               onChanged: (value) => setState(() => glideTyping = value),
//             ),

//             const SizedBox(height: 12),

//             // Show glide trail
//             _buildToggleSetting(
//               title: 'Show glide trail',
//               description: 'Enabled',
//               value: showGlideTrail,
//               onChanged: (value) => setState(() => showGlideTrail = value),
//             ),

//             const SizedBox(height: 12),

//             // Glide trail fade time
//             _buildSliderSetting(
//               title: 'Glide trail fade time',
//               portraitValue: glideTrailFadeTime,
//               onPortraitChanged: (value) =>
//                   setState(() => glideTrailFadeTime = value),
//               min: 100.0,
//               max: 1000.0,
//               unit: 'ms',
//               portraitLabel: 'Time',
//               showLandscape: false,
//             ),

//             const SizedBox(height: 12),

//             // Always delete word
//             _buildToggleSetting(
//               title: 'Always delete word',
//               description: 'Enabled',
//               value: alwaysDeleteWord,
//               onChanged: (value) => setState(() => alwaysDeleteWord = value),
//             ),

//             const SizedBox(height: 32),

//             // General Section
//             _buildSectionTitle('General'),
//             const SizedBox(height: 16),

//             // Swipe up
//             _buildSwipeActionCard(
//               title: 'Swipe up',
//               subtitle: swipeUpAction,
//               onTap: () => _showSwipeActionDialog('Swipe up', (action) {
//                 setState(() => swipeUpAction = action);
//               }),
//             ),

//             const SizedBox(height: 12),

//             // Swipe down
//             _buildSwipeActionCard(
//               title: 'Swipe down',
//               subtitle: swipeDownAction,
//               onTap: () => _showSwipeActionDialog('Swipe down', (action) {
//                 setState(() => swipeDownAction = action);
//               }),
//             ),

//             const SizedBox(height: 12),

//             // Swipe left
//             _buildSwipeActionCard(
//               title: 'Swipe left',
//               subtitle: swipeLeftAction,
//               onTap: () => _showSwipeActionDialog('Swipe left', (action) {
//                 setState(() => swipeLeftAction = action);
//               }),
//             ),

//             const SizedBox(height: 12),

//             // Swipe right
//             _buildSwipeActionCard(
//               title: 'Swipe right',
//               subtitle: swipeRightAction,
//               onTap: () => _showSwipeActionDialog('Swipe right', (action) {
//                 setState(() => swipeRightAction = action);
//               }),
//             ),

//             const SizedBox(height: 32),

//             // Space Bar Section
//             _buildSectionTitle('Space Bar'),
//             const SizedBox(height: 16),

//             // Space bar long press
//             _buildSwipeActionCard(
//               title: 'Space bar long press',
//               subtitle: spaceBarLongPress,
//               onTap: () =>
//                   _showSpaceBarActionDialog('Space bar long press', (action) {
//                     setState(() => spaceBarLongPress = action);
//                   }),
//             ),

//             const SizedBox(height: 12),

//             // Space bar Swipe down
//             _buildSwipeActionCard(
//               title: 'Space bar Swipe down',
//               subtitle: spaceBarSwipeDown,
//               onTap: () =>
//                   _showSpaceBarActionDialog('Space bar Swipe down', (action) {
//                     setState(() => spaceBarSwipeDown = action);
//                   }),
//             ),

//             const SizedBox(height: 12),

//             // Space bar Swipe left
//             _buildSwipeActionCard(
//               title: 'Space bar Swipe left',
//               subtitle: spaceBarSwipeLeft,
//               onTap: () =>
//                   _showSpaceBarActionDialog('Space bar Swipe left', (action) {
//                     setState(() => spaceBarSwipeLeft = action);
//                   }),
//             ),

//             const SizedBox(height: 12),

//             // Space bar Swipe right
//             _buildSwipeActionCard(
//               title: 'Space bar Swipe right',
//               subtitle: spaceBarSwipeRight,
//               onTap: () =>
//                   _showSpaceBarActionDialog('Space bar Swipe right', (action) {
//                     setState(() => spaceBarSwipeRight = action);
//                   }),
//             ),

//             const SizedBox(height: 32),

//             // Other gestures Section
//             _buildSectionTitle('Other gestures'),
//             const SizedBox(height: 16),

//             // Delete key swipe left
//             _buildSwipeActionCard(
//               title: 'Delete key swipe left',
//               subtitle: deleteKeySwipeLeft,
//               onTap: () =>
//                   _showDeleteKeyActionDialog('Delete key swipe left', (action) {
//                     setState(() => deleteKeySwipeLeft = action);
//                   }),
//             ),

//             const SizedBox(height: 12),

//             // Delete key long press
//             _buildSwipeActionCard(
//               title: 'Delete key long press',
//               subtitle: deleteKeyLongPress,
//               onTap: () =>
//                   _showDeleteKeyActionDialog('Delete key long press', (action) {
//                     setState(() => deleteKeyLongPress = action);
//                   }),
//             ),

//             const SizedBox(height: 12),

//             // Swipe velocity threshold
//             _buildSliderSetting(
//               title: 'Swipe velocity threshold',
//               portraitValue: swipeVelocityThreshold,
//               onPortraitChanged: (value) =>
//                   setState(() => swipeVelocityThreshold = value),
//               min: 1000.0,
//               max: 3000.0,
//               unit: ' dp/s',
//               portraitLabel: 'dp/s',
//               showLandscape: false,
//             ),

//             const SizedBox(height: 12),

//             // Swipe distance threshold
//             _buildSliderSetting(
//               title: 'Swipe distance threshold',
//               portraitValue: swipeDistanceThreshold,
//               onPortraitChanged: (value) =>
//                   setState(() => swipeDistanceThreshold = value),
//               min: 10.0,
//               max: 50.0,
//               unit: ' dp/s',
//               portraitLabel: 'dp/s',
//               showLandscape: false,
//             ),
=======
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
>>>>>>> pr-2

//             const SizedBox(height: 24),
//           ],
//         ),
//       ),
//     );
//   }

<<<<<<< HEAD
//   Widget _buildSectionTitle(String title) {
//     return Text(
//       title,
//       style: AppTextStyle.titleMedium.copyWith(
//         color: AppColors.secondary,
//         fontWeight: FontWeight.w600,
//       ),
//     );
//   }

//   Widget _buildToggleSetting({
//     required String title,
//     required String description,
//     required bool value,
//     required ValueChanged<bool> onChanged,
//   }) {
//     return Container(
//       padding: const EdgeInsets.all(16),
//       decoration: BoxDecoration(
//         color: AppColors.lightGrey,
//         borderRadius: BorderRadius.circular(12),
//       ),
//       child: Row(
//         children: [
//           Expanded(
//             child: Column(
//               crossAxisAlignment: CrossAxisAlignment.start,
//               children: [
//                 Text(
//                   title,
//                   style: AppTextStyle.titleLarge.copyWith(
//                     color: AppColors.primary,
//                     fontWeight: FontWeight.w800,
//                   ),
//                 ),
//                 const SizedBox(height: 4),
//                 Text(
//                   description,
//                   style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
//                 ),
//               ],
//             ),
//           ),
//           CustomToggleSwitch(
//             value: value,
//             onChanged: onChanged,
//             width: 48.0,
//             height: 16.0,
//             knobSize: 24.0,
//           ),
//         ],
//       ),
//     );
//   }

//   Widget _buildSliderSetting({
//     required String title,
//     required double portraitValue,
//     double? landscapeValue,
//     required ValueChanged<double> onPortraitChanged,
//     ValueChanged<double>? onLandscapeChanged,
//     required double min,
//     required double max,
//     required String unit,
//     String portraitLabel = 'Portrait',
//     String? landscapeLabel,
//     bool showLandscape = true,
//   }) {
//     return Container(
//       padding: const EdgeInsets.all(16),
//       decoration: BoxDecoration(
//         color: AppColors.lightGrey,
//         borderRadius: BorderRadius.circular(12),
//       ),
//       child: Column(
//         crossAxisAlignment: CrossAxisAlignment.start,
//         children: [
//           Text(
//             title,
//             style: AppTextStyle.titleLarge.copyWith(
//               color: AppColors.black,
//               fontWeight: FontWeight.w800,
//             ),
//           ),
//           const SizedBox(height: 12),
//           // Portrait Slider
//           Row(
//             children: [
//               SizedBox(
//                 width: 80,
//                 child: Text(
//                   portraitLabel,
//                   style: AppTextStyle.bodyMedium.copyWith(
//                     color: AppColors.grey,
//                   ),
//                 ),
//               ),
//               Expanded(
//                 child: Slider(
//                   thumbColor: AppColors.white,
//                   value: portraitValue,
//                   min: min,
//                   max: max,
//                   onChanged: onPortraitChanged,
//                   activeColor: AppColors.secondary,
//                   inactiveColor: AppColors.white,
//                 ),
//               ),
//               SizedBox(
//                 width: 50,
//                 child: Text(
//                   '${portraitValue.toInt()}$unit',
//                   style: AppTextStyle.bodyMedium.copyWith(
//                     color: AppColors.black,
//                     fontWeight: FontWeight.w600,
//                   ),
//                   textAlign: TextAlign.right,
//                 ),
//               ),
//             ],
//           ),
//           // Landscape Slider (if enabled)
//           if (showLandscape &&
//               landscapeValue != null &&
//               onLandscapeChanged != null) ...[
//             const SizedBox(height: 8),
//             Row(
//               children: [
//                 SizedBox(
//                   width: 80,
//                   child: Text(
//                     landscapeLabel ?? 'Landscape',
//                     style: AppTextStyle.bodyMedium.copyWith(
//                       color: AppColors.grey,
//                     ),
//                   ),
//                 ),
//                 Expanded(
//                   child: Slider(
//                     thumbColor: AppColors.white,
//                     value: landscapeValue,
//                     min: min,
//                     max: max,
//                     onChanged: onLandscapeChanged,
//                     activeColor: AppColors.secondary,
//                     inactiveColor: AppColors.white,
//                   ),
//                 ),
//                 SizedBox(
//                   width: 50,
//                   child: Text(
//                     '${landscapeValue.toInt()}$unit',
//                     style: AppTextStyle.bodySmall.copyWith(
//                       color: AppColors.black,
//                       fontWeight: FontWeight.w600,
//                     ),
//                     textAlign: TextAlign.right,
//                   ),
//                 ),
//               ],
//             ),
//           ],
//         ],
//       ),
//     );
//   }

//   Widget _buildSwipeActionCard({
//     required String title,
//     required String subtitle,
//     required VoidCallback onTap,
//   }) {
//     return GestureDetector(
//       onTap: onTap,
//       child: Container(
//         padding: const EdgeInsets.all(16),
//         decoration: BoxDecoration(
//           color: AppColors.lightGrey,
//           borderRadius: BorderRadius.circular(12),
//         ),
//         child: Row(
//           children: [
//             Expanded(
//               child: Column(
//                 crossAxisAlignment: CrossAxisAlignment.start,
//                 children: [
//                   Text(
//                     title,
//                     style: AppTextStyle.titleLarge.copyWith(
//                       color: AppColors.primary,
//                       fontWeight: FontWeight.w800,
//                     ),
//                   ),
//                   const SizedBox(height: 4),
//                   Text(
//                     subtitle,
//                     style: AppTextStyle.bodySmall.copyWith(
//                       color: AppColors.grey,
//                     ),
//                   ),
//                 ],
//               ),
//             ),
//             Icon(Icons.arrow_forward_ios, color: AppColors.grey, size: 16),
//           ],
//         ),
//       ),
//     );
//   }

//   void _showSwipeActionDialog(
//     String gesture,
//     Function(String) onActionSelected,
//   ) {
//     showDialog(
//       context: context,
//       barrierDismissible: true,
//       builder: (BuildContext context) {
//         return Dialog(
//           shape: RoundedRectangleBorder(
//             borderRadius: BorderRadius.circular(16),
//           ),
//           child: Container(
//             padding: const EdgeInsets.all(24),
//             decoration: BoxDecoration(
//               color: AppColors.white,
//               borderRadius: BorderRadius.circular(16),
//             ),
//             child: Column(
//               mainAxisSize: MainAxisSize.min,
//               crossAxisAlignment: CrossAxisAlignment.start,
//               children: [
//                 // Header
//                 Text(
//                   '$gesture Action',
//                   style: AppTextStyle.titleLarge.copyWith(
//                     color: AppColors.primary,
//                     fontWeight: FontWeight.w800,
//                   ),
//                 ),
//                 Divider(color: AppColors.lightGrey, thickness: 1),

//                 // Action Options
//                 _buildActionOption(
//                   'Shift',
//                   gesture == 'Swipe up'
//                       ? swipeUpAction
//                       : gesture == 'Swipe down'
//                       ? swipeDownAction
//                       : gesture == 'Swipe left'
//                       ? swipeLeftAction
//                       : swipeRightAction,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildActionOption(
//                   'Space',
//                   gesture == 'Swipe up'
//                       ? swipeUpAction
//                       : gesture == 'Swipe down'
//                       ? swipeDownAction
//                       : gesture == 'Swipe left'
//                       ? swipeLeftAction
//                       : swipeRightAction,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildActionOption(
//                   'Enter',
//                   gesture == 'Swipe up'
//                       ? swipeUpAction
//                       : gesture == 'Swipe down'
//                       ? swipeDownAction
//                       : gesture == 'Swipe left'
//                       ? swipeLeftAction
//                       : swipeRightAction,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildActionOption(
//                   'Backspace',
//                   gesture == 'Swipe up'
//                       ? swipeUpAction
//                       : gesture == 'Swipe down'
//                       ? swipeDownAction
//                       : gesture == 'Swipe left'
//                       ? swipeLeftAction
//                       : swipeRightAction,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildActionOption(
//                   'None',
//                   gesture == 'Swipe up'
//                       ? swipeUpAction
//                       : gesture == 'Swipe down'
//                       ? swipeDownAction
//                       : gesture == 'Swipe left'
//                       ? swipeLeftAction
//                       : swipeRightAction,
//                 ),

//                 const SizedBox(height: 20),

//                 // Apply Button
//                 Row(
//                   children: [
//                     Expanded(
//                       child: ElevatedButton(
//                         onPressed: () => Navigator.of(context).pop(),
//                         style: ElevatedButton.styleFrom(
//                           backgroundColor: AppColors.secondary,
//                           foregroundColor: AppColors.white,
//                           padding: const EdgeInsets.symmetric(vertical: 16),
//                           shape: RoundedRectangleBorder(
//                             borderRadius: BorderRadius.circular(32),
//                           ),
//                           elevation: 0,
//                         ),
//                         child: Text(
//                           'Cancel',
//                           style: AppTextStyle.buttonSecondary.copyWith(
//                             fontWeight: FontWeight.w600,
//                           ),
//                         ),
//                       ),
//                     ),
//                     Expanded(
//                       child: ElevatedButton(
//                         onPressed: () => Navigator.of(context).pop(),
//                         style: ElevatedButton.styleFrom(
//                           backgroundColor: AppColors.secondary,
//                           foregroundColor: AppColors.white,
//                           padding: const EdgeInsets.symmetric(vertical: 16),
//                           shape: RoundedRectangleBorder(
//                             borderRadius: BorderRadius.circular(32),
//                           ),
//                           elevation: 0,
//                         ),
//                         child: Text(
//                           'OK',
//                           style: AppTextStyle.buttonSecondary.copyWith(
//                             fontWeight: FontWeight.w600,
//                           ),
//                         ),
//                       ),
//                     ),
//                   ],
//                 ),
//               ],
//             ),
//           ),
//         );
//       },
//     );
//   }

//   Widget _buildActionOption(String action, String currentAction) {
//     bool isSelected = currentAction == action;

//     return GestureDetector(
//       onTap: () {
//         // This would update the selected action
//         // For now, we'll just close the dialog
//         Navigator.of(context).pop();
//       },
//       child: Row(
//         children: [
//           // Radio Button
//           Radio<String>(
//             value: action,
//             groupValue: currentAction,
//             onChanged: (String? value) {
//               if (value != null) {
//                 // Update the action
//                 Navigator.of(context).pop();
//               }
//             },
//             activeColor: AppColors.secondary,
//             materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
//           ),
//           const SizedBox(width: 8),

//           // Content
//           Expanded(
//             child: Text(
//               action,
//               style: AppTextStyle.titleMedium.copyWith(
//                 color: AppColors.primary,
//                 fontWeight: FontWeight.w600,
//               ),
//             ),
//           ),
//         ],
//       ),
//     );
//   }

//   void _showSpaceBarActionDialog(
//     String gesture,
//     Function(String) onActionSelected,
//   ) {
//     showDialog(
//       context: context,
//       barrierDismissible: true,
//       builder: (BuildContext context) {
//         return Dialog(
//           shape: RoundedRectangleBorder(
//             borderRadius: BorderRadius.circular(16),
//           ),
//           child: Container(
//             padding: const EdgeInsets.all(24),
//             decoration: BoxDecoration(
//               color: AppColors.white,
//               borderRadius: BorderRadius.circular(16),
//             ),
//             child: Column(
//               mainAxisSize: MainAxisSize.min,
//               crossAxisAlignment: CrossAxisAlignment.start,
//               children: [
//                 // Header
//                 Text(
//                   '$gesture Action',
//                   style: AppTextStyle.titleLarge.copyWith(
//                     color: AppColors.primary,
//                     fontWeight: FontWeight.w800,
//                   ),
//                 ),
//                 Divider(color: AppColors.lightGrey, thickness: 1),

//                 // Space Bar Action Options
//                 _buildSpaceBarActionOption(
//                   'No action',
//                   gesture == 'Space bar long press'
//                       ? spaceBarLongPress
//                       : gesture == 'Space bar Swipe down'
//                       ? spaceBarSwipeDown
//                       : gesture == 'Space bar Swipe left'
//                       ? spaceBarSwipeLeft
//                       : spaceBarSwipeRight,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildSpaceBarActionOption(
//                   'Move cursor up',
//                   gesture == 'Space bar long press'
//                       ? spaceBarLongPress
//                       : gesture == 'Space bar Swipe down'
//                       ? spaceBarSwipeDown
//                       : gesture == 'Space bar Swipe left'
//                       ? spaceBarSwipeLeft
//                       : spaceBarSwipeRight,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildSpaceBarActionOption(
//                   'Move cursor down',
//                   gesture == 'Space bar long press'
//                       ? spaceBarLongPress
//                       : gesture == 'Space bar Swipe down'
//                       ? spaceBarSwipeDown
//                       : gesture == 'Space bar Swipe left'
//                       ? spaceBarSwipeLeft
//                       : spaceBarSwipeRight,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildSpaceBarActionOption(
//                   'Move cursor left',
//                   gesture == 'Space bar long press'
//                       ? spaceBarLongPress
//                       : gesture == 'Space bar Swipe down'
//                       ? spaceBarSwipeDown
//                       : gesture == 'Space bar Swipe left'
//                       ? spaceBarSwipeLeft
//                       : spaceBarSwipeRight,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildSpaceBarActionOption(
//                   'Move cursor right',
//                   gesture == 'Space bar long press'
//                       ? spaceBarLongPress
//                       : gesture == 'Space bar Swipe down'
//                       ? spaceBarSwipeDown
//                       : gesture == 'Space bar Swipe left'
//                       ? spaceBarSwipeLeft
//                       : spaceBarSwipeRight,
//                 ),

//                 const SizedBox(height: 20),

//                 // Apply Button
//                 Row(
//                   children: [
//                     Expanded(
//                       child: ElevatedButton(
//                         onPressed: () => Navigator.of(context).pop(),
//                         style: ElevatedButton.styleFrom(
//                           backgroundColor: AppColors.secondary,
//                           foregroundColor: AppColors.white,
//                           padding: const EdgeInsets.symmetric(vertical: 16),
//                           shape: RoundedRectangleBorder(
//                             borderRadius: BorderRadius.circular(32),
//                           ),
//                           elevation: 0,
//                         ),
//                         child: Text(
//                           'Cancel',
//                           style: AppTextStyle.buttonSecondary.copyWith(
//                             fontWeight: FontWeight.w600,
//                           ),
//                         ),
//                       ),
//                     ),
//                     Expanded(
//                       child: ElevatedButton(
//                         onPressed: () => Navigator.of(context).pop(),
//                         style: ElevatedButton.styleFrom(
//                           backgroundColor: AppColors.secondary,
//                           foregroundColor: AppColors.white,
//                           padding: const EdgeInsets.symmetric(vertical: 16),
//                           shape: RoundedRectangleBorder(
//                             borderRadius: BorderRadius.circular(32),
//                           ),
//                           elevation: 0,
//                         ),
//                         child: Text(
//                           'OK',
//                           style: AppTextStyle.buttonSecondary.copyWith(
//                             fontWeight: FontWeight.w600,
//                           ),
//                         ),
//                       ),
//                     ),
//                   ],
//                 ),
//               ],
//             ),
//           ),
//         );
//       },
//     );
//   }

//   Widget _buildSpaceBarActionOption(String action, String currentAction) {
//     return GestureDetector(
//       onTap: () {
//         Navigator.of(context).pop();
//       },
//       child: Row(
//         children: [
//           Radio<String>(
//             value: action,
//             groupValue: currentAction,
//             onChanged: (String? value) {
//               if (value != null) {
//                 Navigator.of(context).pop();
//               }
//             },
//             activeColor: AppColors.secondary,
//             materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
//           ),
//           const SizedBox(width: 8),
//           Expanded(
//             child: Text(
//               action,
//               style: AppTextStyle.titleMedium.copyWith(
//                 color: AppColors.primary,
//                 fontWeight: FontWeight.w600,
//               ),
//             ),
//           ),
//         ],
//       ),
//     );
//   }

//   void _showDeleteKeyActionDialog(
//     String gesture,
//     Function(String) onActionSelected,
//   ) {
//     showDialog(
//       context: context,
//       barrierDismissible: true,
//       builder: (BuildContext context) {
//         return Dialog(
//           shape: RoundedRectangleBorder(
//             borderRadius: BorderRadius.circular(16),
//           ),
//           child: Container(
//             padding: const EdgeInsets.all(24),
//             decoration: BoxDecoration(
//               color: AppColors.white,
//               borderRadius: BorderRadius.circular(16),
//             ),
//             child: Column(
//               mainAxisSize: MainAxisSize.min,
//               crossAxisAlignment: CrossAxisAlignment.start,
//               children: [
//                 // Header
//                 Text(
//                   '$gesture Action',
//                   style: AppTextStyle.titleLarge.copyWith(
//                     color: AppColors.primary,
//                     fontWeight: FontWeight.w800,
//                   ),
//                 ),
//                 Divider(color: AppColors.lightGrey, thickness: 1),

//                 // Delete Key Action Options
//                 _buildDeleteKeyActionOption(
//                   'Delete characters precisely',
//                   gesture == 'Delete key swipe left'
//                       ? deleteKeySwipeLeft
//                       : deleteKeyLongPress,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildDeleteKeyActionOption(
//                   'Delete character before cursor',
//                   gesture == 'Delete key swipe left'
//                       ? deleteKeySwipeLeft
//                       : deleteKeyLongPress,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildDeleteKeyActionOption(
//                   'Delete word',
//                   gesture == 'Delete key swipe left'
//                       ? deleteKeySwipeLeft
//                       : deleteKeyLongPress,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildDeleteKeyActionOption(
//                   'Delete line',
//                   gesture == 'Delete key swipe left'
//                       ? deleteKeySwipeLeft
//                       : deleteKeyLongPress,
//                 ),
//                 const SizedBox(height: 12),
//                 _buildDeleteKeyActionOption(
//                   'No action',
//                   gesture == 'Delete key swipe left'
//                       ? deleteKeySwipeLeft
//                       : deleteKeyLongPress,
//                 ),

//                 const SizedBox(height: 20),

//                 // Apply Button
//                 Row(
//                   children: [
//                     Expanded(
//                       child: ElevatedButton(
//                         onPressed: () => Navigator.of(context).pop(),
//                         style: ElevatedButton.styleFrom(
//                           backgroundColor: AppColors.secondary,
//                           foregroundColor: AppColors.white,
//                           padding: const EdgeInsets.symmetric(vertical: 16),
//                           shape: RoundedRectangleBorder(
//                             borderRadius: BorderRadius.circular(32),
//                           ),
//                           elevation: 0,
//                         ),
//                         child: Text(
//                           'Cancel',
//                           style: AppTextStyle.buttonSecondary.copyWith(
//                             fontWeight: FontWeight.w600,
//                           ),
//                         ),
//                       ),
//                     ),
//                     Expanded(
//                       child: ElevatedButton(
//                         onPressed: () => Navigator.of(context).pop(),
//                         style: ElevatedButton.styleFrom(
//                           backgroundColor: AppColors.secondary,
//                           foregroundColor: AppColors.white,
//                           padding: const EdgeInsets.symmetric(vertical: 16),
//                           shape: RoundedRectangleBorder(
//                             borderRadius: BorderRadius.circular(32),
//                           ),
//                           elevation: 0,
//                         ),
//                         child: Text(
//                           'OK',
//                           style: AppTextStyle.buttonSecondary.copyWith(
//                             fontWeight: FontWeight.w600,
//                           ),
//                         ),
//                       ),
//                     ),
//                   ],
//                 ),
//               ],
//             ),
//           ),
//         );
//       },
//     );
//   }

//   Widget _buildDeleteKeyActionOption(String action, String currentAction) {
//     return GestureDetector(
//       onTap: () {
//         Navigator.of(context).pop();
//       },
//       child: Row(
//         children: [
//           Radio<String>(
//             value: action,
//             groupValue: currentAction,
//             onChanged: (String? value) {
//               if (value != null) {
//                 Navigator.of(context).pop();
//               }
//             },
//             activeColor: AppColors.secondary,
//             materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
//           ),
//           const SizedBox(width: 8),
//           Expanded(
//             child: Text(
//               action,
//               style: AppTextStyle.titleMedium.copyWith(
//                 color: AppColors.primary,
//                 fontWeight: FontWeight.w600,
//               ),
//             ),
//           ),
//         ],
//       ),
//     );
//   }
// }
=======
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
>>>>>>> pr-2
