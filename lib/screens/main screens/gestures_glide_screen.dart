import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/custom_toggle_switch.dart';

class GesturesGlideScreen extends StatefulWidget {
  const GesturesGlideScreen({super.key});

  @override
  State<GesturesGlideScreen> createState() => _GesturesGlideScreenState();
}

class _GesturesGlideScreenState extends State<GesturesGlideScreen> {
  // Glide Typing Settings
  bool glideTyping = true;
  bool showGlideTrail = true;
  double glideTrailFadeTime = 200.0;
  bool alwaysDeleteWord = true;

  // General Settings
  String swipeUpAction = 'Shift';
  String swipeDownAction = 'Shift';
  String swipeLeftAction = 'Shift';
  String swipeRightAction = 'Shift';

  // Space Bar Settings
  String spaceBarLongPress = 'No action';
  String spaceBarSwipeDown = 'Move cursor down';
  String spaceBarSwipeLeft = 'Move cursor left';
  String spaceBarSwipeRight = 'Move cursor right';

  // Other Gestures Settings
  String deleteKeySwipeLeft = 'Delete characters precisely';
  String deleteKeyLongPress = 'Delete character before cursor';
  double swipeVelocityThreshold = 1900.0;
  double swipeDistanceThreshold = 20.0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Gestures & Glide typing',
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

            // Glide Typing Section
            _buildSectionTitle('Glide typing'),
            const SizedBox(height: 16),

            // Glide typing
            _buildToggleSetting(
              title: 'Glide typing',
              description: 'Enabled',
              value: glideTyping,
              onChanged: (value) => setState(() => glideTyping = value),
            ),

            const SizedBox(height: 12),

            // Show glide trail
            _buildToggleSetting(
              title: 'Show glide trail',
              description: 'Enabled',
              value: showGlideTrail,
              onChanged: (value) => setState(() => showGlideTrail = value),
            ),

            const SizedBox(height: 12),

            // Glide trail fade time
            _buildSliderSetting(
              title: 'Glide trail fade time',
              portraitValue: glideTrailFadeTime,
              onPortraitChanged: (value) =>
                  setState(() => glideTrailFadeTime = value),
              min: 100.0,
              max: 1000.0,
              unit: 'ms',
              portraitLabel: 'Time',
              showLandscape: false,
            ),

            const SizedBox(height: 12),

            // Always delete word
            _buildToggleSetting(
              title: 'Always delete word',
              description: 'Enabled',
              value: alwaysDeleteWord,
              onChanged: (value) => setState(() => alwaysDeleteWord = value),
            ),

            const SizedBox(height: 32),

            // General Section
            _buildSectionTitle('General'),
            const SizedBox(height: 16),

            // Swipe up
            _buildSwipeActionCard(
              title: 'Swipe up',
              subtitle: swipeUpAction,
              onTap: () => _showSwipeActionDialog('Swipe up', (action) {
                setState(() => swipeUpAction = action);
              }),
            ),

            const SizedBox(height: 12),

            // Swipe down
            _buildSwipeActionCard(
              title: 'Swipe down',
              subtitle: swipeDownAction,
              onTap: () => _showSwipeActionDialog('Swipe down', (action) {
                setState(() => swipeDownAction = action);
              }),
            ),

            const SizedBox(height: 12),

            // Swipe left
            _buildSwipeActionCard(
              title: 'Swipe left',
              subtitle: swipeLeftAction,
              onTap: () => _showSwipeActionDialog('Swipe left', (action) {
                setState(() => swipeLeftAction = action);
              }),
            ),

            const SizedBox(height: 12),

            // Swipe right
            _buildSwipeActionCard(
              title: 'Swipe right',
              subtitle: swipeRightAction,
              onTap: () => _showSwipeActionDialog('Swipe right', (action) {
                setState(() => swipeRightAction = action);
              }),
            ),

            const SizedBox(height: 32),

            // Space Bar Section
            _buildSectionTitle('Space Bar'),
            const SizedBox(height: 16),

            // Space bar long press
            _buildSwipeActionCard(
              title: 'Space bar long press',
              subtitle: spaceBarLongPress,
              onTap: () =>
                  _showSpaceBarActionDialog('Space bar long press', (action) {
                    setState(() => spaceBarLongPress = action);
                  }),
            ),

            const SizedBox(height: 12),

            // Space bar Swipe down
            _buildSwipeActionCard(
              title: 'Space bar Swipe down',
              subtitle: spaceBarSwipeDown,
              onTap: () =>
                  _showSpaceBarActionDialog('Space bar Swipe down', (action) {
                    setState(() => spaceBarSwipeDown = action);
                  }),
            ),

            const SizedBox(height: 12),

            // Space bar Swipe left
            _buildSwipeActionCard(
              title: 'Space bar Swipe left',
              subtitle: spaceBarSwipeLeft,
              onTap: () =>
                  _showSpaceBarActionDialog('Space bar Swipe left', (action) {
                    setState(() => spaceBarSwipeLeft = action);
                  }),
            ),

            const SizedBox(height: 12),

            // Space bar Swipe right
            _buildSwipeActionCard(
              title: 'Space bar Swipe right',
              subtitle: spaceBarSwipeRight,
              onTap: () =>
                  _showSpaceBarActionDialog('Space bar Swipe right', (action) {
                    setState(() => spaceBarSwipeRight = action);
                  }),
            ),

            const SizedBox(height: 32),

            // Other gestures Section
            _buildSectionTitle('Other gestures'),
            const SizedBox(height: 16),

            // Delete key swipe left
            _buildSwipeActionCard(
              title: 'Delete key swipe left',
              subtitle: deleteKeySwipeLeft,
              onTap: () =>
                  _showDeleteKeyActionDialog('Delete key swipe left', (action) {
                    setState(() => deleteKeySwipeLeft = action);
                  }),
            ),

            const SizedBox(height: 12),

            // Delete key long press
            _buildSwipeActionCard(
              title: 'Delete key long press',
              subtitle: deleteKeyLongPress,
              onTap: () =>
                  _showDeleteKeyActionDialog('Delete key long press', (action) {
                    setState(() => deleteKeyLongPress = action);
                  }),
            ),

            const SizedBox(height: 12),

            // Swipe velocity threshold
            _buildSliderSetting(
              title: 'Swipe velocity threshold',
              portraitValue: swipeVelocityThreshold,
              onPortraitChanged: (value) =>
                  setState(() => swipeVelocityThreshold = value),
              min: 1000.0,
              max: 3000.0,
              unit: ' dp/s',
              portraitLabel: 'dp/s',
              showLandscape: false,
            ),

            const SizedBox(height: 12),

            // Swipe distance threshold
            _buildSliderSetting(
              title: 'Swipe distance threshold',
              portraitValue: swipeDistanceThreshold,
              onPortraitChanged: (value) =>
                  setState(() => swipeDistanceThreshold = value),
              min: 10.0,
              max: 50.0,
              unit: ' dp/s',
              portraitLabel: 'dp/s',
              showLandscape: false,
            ),

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

  Widget _buildSliderSetting({
    required String title,
    required double portraitValue,
    double? landscapeValue,
    required ValueChanged<double> onPortraitChanged,
    ValueChanged<double>? onLandscapeChanged,
    required double min,
    required double max,
    required String unit,
    String portraitLabel = 'Portrait',
    String? landscapeLabel,
    bool showLandscape = true,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: AppTextStyle.titleLarge.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 12),
          // Portrait Slider
          Row(
            children: [
              SizedBox(
                width: 80,
                child: Text(
                  portraitLabel,
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.grey,
                  ),
                ),
              ),
              Expanded(
                child: Slider(
                  thumbColor: AppColors.white,
                  value: portraitValue,
                  min: min,
                  max: max,
                  onChanged: onPortraitChanged,
                  activeColor: AppColors.secondary,
                  inactiveColor: AppColors.white,
                ),
              ),
              SizedBox(
                width: 50,
                child: Text(
                  '${portraitValue.toInt()}$unit',
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.w600,
                  ),
                  textAlign: TextAlign.right,
                ),
              ),
            ],
          ),
          // Landscape Slider (if enabled)
          if (showLandscape &&
              landscapeValue != null &&
              onLandscapeChanged != null) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                SizedBox(
                  width: 80,
                  child: Text(
                    landscapeLabel ?? 'Landscape',
                    style: AppTextStyle.bodyMedium.copyWith(
                      color: AppColors.grey,
                    ),
                  ),
                ),
                Expanded(
                  child: Slider(
                    thumbColor: AppColors.white,
                    value: landscapeValue,
                    min: min,
                    max: max,
                    onChanged: onLandscapeChanged,
                    activeColor: AppColors.secondary,
                    inactiveColor: AppColors.white,
                  ),
                ),
                SizedBox(
                  width: 50,
                  child: Text(
                    '${landscapeValue.toInt()}$unit',
                    style: AppTextStyle.bodySmall.copyWith(
                      color: AppColors.black,
                      fontWeight: FontWeight.w600,
                    ),
                    textAlign: TextAlign.right,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildSwipeActionCard({
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
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
                    subtitle,
                    style: AppTextStyle.bodySmall.copyWith(
                      color: AppColors.grey,
                    ),
                  ),
                ],
              ),
            ),
            Icon(Icons.arrow_forward_ios, color: AppColors.grey, size: 16),
          ],
        ),
      ),
    );
  }

  String _getCurrentAction(String gesture) {
    switch (gesture) {
      case 'Swipe up':
        return swipeUpAction;
      case 'Swipe down':
        return swipeDownAction;
      case 'Swipe left':
        return swipeLeftAction;
      case 'Swipe right':
        return swipeRightAction;
      default:
        return 'No action';
    }
  }

  String _getCurrentSpaceBarAction(String gesture) {
    switch (gesture) {
      case 'Space bar long press':
        return spaceBarLongPress;
      case 'Space bar Swipe down':
        return spaceBarSwipeDown;
      case 'Space bar Swipe left':
        return spaceBarSwipeLeft;
      case 'Space bar Swipe right':
        return spaceBarSwipeRight;
      default:
        return 'No action';
    }
  }

  String _getCurrentDeleteKeyAction(String gesture) {
    switch (gesture) {
      case 'Delete key swipe left':
        return deleteKeySwipeLeft;
      case 'Delete key long press':
        return deleteKeyLongPress;
      default:
        return 'No action';
    }
  }

  void _showSwipeActionDialog(
    String gesture,
    Function(String) onActionSelected,
  ) {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Container(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.of(context).size.height * 0.8,
            ),
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
                Text(
                  '$gesture Action',
                  style: AppTextStyle.titleLarge.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Divider(color: AppColors.lightGrey, thickness: 1),

                // Scrollable Action Options
                Flexible(
                  child: SingleChildScrollView(
                    child: Column(
                      children: [
                        _buildActionOption(
                          'No action',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Cycle to previous keyboard mode',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Cycle to next keyboard mode',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Delete word before cursor',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Hide keyboard',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Insert space',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor up',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor down',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor left',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor right',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor to start of line',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor to end of line',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor to start of page',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor to end of page',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Shift',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Redo',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Undo',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Open clipboard manager/history',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Show input method picker',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Switch to previous Language',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Switch to next Language',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Toggle Smartbar visibility',
                          _getCurrentAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 20),

                // Apply Button
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.of(context).pop(),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.secondary,
                          foregroundColor: AppColors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(32),
                          ),
                          elevation: 0,
                        ),
                        child: Text(
                          'Cancel',
                          style: AppTextStyle.buttonSecondary.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.of(context).pop(),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.secondary,
                          foregroundColor: AppColors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(32),
                          ),
                          elevation: 0,
                        ),
                        child: Text(
                          'OK',
                          style: AppTextStyle.buttonSecondary.copyWith(
                            fontWeight: FontWeight.w600,
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

  Widget _buildActionOption(
    String action,
    String currentAction,
    String gesture,
    Function(String) onActionSelected,
  ) {
    return GestureDetector(
      onTap: () {
        onActionSelected(action);
        Navigator.of(context).pop();
      },
      child: Row(
        children: [
          // Radio Button
          Radio<String>(
            value: action,
            groupValue: currentAction,
            onChanged: (String? value) {
              if (value != null) {
                onActionSelected(value);
                Navigator.of(context).pop();
              }
            },
            activeColor: AppColors.secondary,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
          const SizedBox(width: 8),

          // Content
          Expanded(
            child: Text(
              action,
              style: AppTextStyle.titleMedium.copyWith(
                color: AppColors.primary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showSpaceBarActionDialog(
    String gesture,
    Function(String) onActionSelected,
  ) {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Container(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.of(context).size.height * 0.8,
            ),
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
                Text(
                  '$gesture Action',
                  style: AppTextStyle.titleLarge.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Divider(color: AppColors.lightGrey, thickness: 1),

                // Scrollable Action Options
                Flexible(
                  child: SingleChildScrollView(
                    child: Column(
                      children: [
                        _buildActionOption(
                          'No action',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor up',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor down',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor left',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor right',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor to start of line',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor to end of line',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor to start of page',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Move cursor to end of page',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Insert space',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Hide keyboard',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Open clipboard manager/history',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Show input method picker',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Switch to previous Language',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Switch to next Language',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Toggle Smartbar visibility',
                          _getCurrentSpaceBarAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 20),

                // Apply Button
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.of(context).pop(),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.secondary,
                          foregroundColor: AppColors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(32),
                          ),
                          elevation: 0,
                        ),
                        child: Text(
                          'Cancel',
                          style: AppTextStyle.buttonSecondary.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.of(context).pop(),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.secondary,
                          foregroundColor: AppColors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(32),
                          ),
                          elevation: 0,
                        ),
                        child: Text(
                          'OK',
                          style: AppTextStyle.buttonSecondary.copyWith(
                            fontWeight: FontWeight.w600,
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

  void _showDeleteKeyActionDialog(
    String gesture,
    Function(String) onActionSelected,
  ) {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Container(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.of(context).size.height * 0.8,
            ),
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
                Text(
                  '$gesture Action',
                  style: AppTextStyle.titleLarge.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Divider(color: AppColors.lightGrey, thickness: 1),

                // Scrollable Action Options
                Flexible(
                  child: SingleChildScrollView(
                    child: Column(
                      children: [
                        _buildActionOption(
                          'No action',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Delete characters precisely',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Delete character before cursor',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Delete word',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Delete line',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Delete word before cursor',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Undo',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Redo',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Hide keyboard',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Open clipboard manager/history',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Show input method picker',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Switch to previous Language',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Switch to next Language',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                        const SizedBox(height: 8),
                        _buildActionOption(
                          'Toggle Smartbar visibility',
                          _getCurrentDeleteKeyAction(gesture),
                          gesture,
                          onActionSelected,
                        ),
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 20),

                // Apply Button
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.of(context).pop(),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.secondary,
                          foregroundColor: AppColors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(32),
                          ),
                          elevation: 0,
                        ),
                        child: Text(
                          'Cancel',
                          style: AppTextStyle.buttonSecondary.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.of(context).pop(),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.secondary,
                          foregroundColor: AppColors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(32),
                          ),
                          elevation: 0,
                        ),
                        child: Text(
                          'OK',
                          style: AppTextStyle.buttonSecondary.copyWith(
                            fontWeight: FontWeight.w600,
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
}
