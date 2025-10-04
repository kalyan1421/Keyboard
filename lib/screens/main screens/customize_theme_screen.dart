import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:image_picker/image_picker.dart';
import 'package:ai_keyboard/screens/main screens/image_crop_screen.dart';
import 'dart:io';

class CustomizeThemeScreen extends StatefulWidget {
  const CustomizeThemeScreen({super.key});

  @override
  State<CustomizeThemeScreen> createState() => _CustomizeThemeScreenState();
}

class _CustomizeThemeScreenState extends State<CustomizeThemeScreen> {
  int selectedTabIndex = 0;
  double brightnessValue = 0.75; // Default brightness at 75%
  double opacityValue = 0.75; // Default opacity at 75%
  int selectedButtonStyle = 6; // Index of selected button style
  int selectedEffectStyle = 0; // Index of selected effect style
  int selectedFontStyle = 6; // Index of selected font style
  double colorValue = 0.8; // Default color position on rainbow slider
  int selectedSoundStyle = 0; // Index of selected sound style
  double volumeValue = 0.75; // Default volume at 75%
  int selectedStickerStyle = 0; // Index of selected sticker style
  double stickersOpacityValue = 0.75; // Default stickers opacity at 75%
  final FocusNode _keyboardFocusNode = FocusNode();
  bool isKeyboardVisible = true; // Keyboard visibility state
  File? selectedImage; // Selected image file

  @override
  void initState() {
    super.initState();
    // Activate physical keyboard when entering the screen
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (isKeyboardVisible) {
        _keyboardFocusNode.requestFocus();
      }
    });
  }

  @override
  void dispose() {
    _keyboardFocusNode.dispose();
    super.dispose();
  }

  final List<TabItem> tabs = [
    TabItem(icon: Icons.camera_alt_outlined, label: 'Image', isSelected: true),
    TabItem(
      icon: Icons.format_color_text_rounded,
      label: 'Button',
      isSelected: false,
    ),
    TabItem(icon: Icons.auto_awesome, label: 'Effect', isSelected: false),
    TabItem(icon: Icons.font_download, label: 'Font', isSelected: false),
    TabItem(icon: Icons.music_note_rounded, label: 'Sound', isSelected: false),
    TabItem(
      icon: Icons.emoji_emotions_outlined,
      label: 'Stickers',
      isSelected: false,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.lightGrey,
      appBar: AppBar(
        toolbarHeight: 100,
        actionsPadding: const EdgeInsets.only(right: 16),
        backgroundColor: AppColors.primary,
        elevation: 0,
        leading: IconButton(
          onPressed: () => Navigator.pop(context),
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
        ),
        title: Text(
          'Customize Theme',
          style: AppTextStyle.titleLarge.copyWith(color: AppColors.white),
        ),
        actions: [
          ElevatedButton(
            onPressed: () {},
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
            child: Text(
              'Save',
              style: AppTextStyle.bodyLarge.copyWith(color: AppColors.black),
            ),
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(50),
          child: _buildTabNavigation(),
        ),
      ),

      body: Column(
        children: [
          // Invisible TextField to activate physical keyboard
          SizedBox(
            height: 1,
            child: TextField(
              focusNode: _keyboardFocusNode,
              autofocus: true,
              decoration: const InputDecoration(
                border: InputBorder.none,
                contentPadding: EdgeInsets.zero,
              ),
              style: const TextStyle(color: Colors.transparent, fontSize: 1),
            ),
          ),

          // Main Content
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.only(
                left: 16,
                right: 16,
                top: 16,
                bottom: 80, // Add space for slider
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Content based on selected tab
                  if (selectedTabIndex == 0) ...[
                    // Image Tab Content
                    _buildUploadPhotoSection(),
                    const SizedBox(height: 24),
                    _buildRecentlyUploadedSection(),
                  ] else if (selectedTabIndex == 1) ...[
                    // Button Tab Content
                    _buildButtonStyleGrid(),
                  ] else if (selectedTabIndex == 2) ...[
                    // Effect Tab Content
                    _buildEffectStyleGrid(),
                  ] else if (selectedTabIndex == 3) ...[
                    // Font Tab Content
                    _buildFontStyleGrid(),
                  ] else if (selectedTabIndex == 4) ...[
                    // Sound Tab Content
                    _buildSoundStyleGrid(),
                  ] else if (selectedTabIndex == 5) ...[
                    // Stickers Tab Content
                    _buildStickersStyleGrid(),
                  ] else ...[
                    // Placeholder for other tabs
                    _buildPlaceholderContent(),
                  ],
                ],
              ),
            ),
          ),

          // Bottom Section with Keyboard and Slider
          _buildBottomSection(),
        ],
      ),
    );
  }

  void _showImageUploadDialog() {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (context, setState) {
            return Dialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              child: Container(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Upload Area
                    GestureDetector(
                      onTap: () async {
                        await _pickImage();
                        setState(() {}); // Trigger rebuild of dialog
                      },
                      child: Container(
                        width: double.infinity,
                        height: 120,
                        decoration: BoxDecoration(
                          color: AppColors.lightGrey,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                            color: AppColors.grey.withOpacity(0.3),
                            style: BorderStyle.solid,
                          ),
                        ),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            // Upload Icon
                            Container(
                              width: 48,
                              height: 48,
                              decoration: BoxDecoration(
                                color: AppColors.primary,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: const Icon(
                                Icons.cloud_upload,
                                color: AppColors.white,
                                size: 24,
                              ),
                            ),
                            const SizedBox(height: 12),
                            Text(
                              'Drag & drop or browse files',
                              style: AppTextStyle.bodyMedium.copyWith(
                                color: AppColors.grey,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),

                    const SizedBox(height: 16),

                    // File Requirements
                    Text(
                      'Please upload Jpg image, size less than 100KB',
                      style: AppTextStyle.bodySmall.copyWith(
                        color: AppColors.grey,
                      ),
                    ),

                    const SizedBox(height: 20),

                    // Selected File Preview
                    if (selectedImage != null) ...[
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.lightGrey,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Row(
                          children: [
                            // Image Thumbnail
                            Container(
                              width: 40,
                              height: 40,
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(6),
                                image: DecorationImage(
                                  image: FileImage(selectedImage!),
                                  fit: BoxFit.cover,
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            // File Name
                            Expanded(
                              child: Text(
                                'demo.jpg',
                                style: AppTextStyle.bodyMedium.copyWith(
                                  color: AppColors.black,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 20),
                    ],

                    // Action Buttons
                    Row(
                      children: [
                        // Cancel Button
                        Expanded(
                          child: GestureDetector(
                            onTap: () => Navigator.of(context).pop(),
                            child: Container(
                              height: 48,
                              decoration: BoxDecoration(
                                color: AppColors.primary,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Center(
                                child: Text(
                                  'Cancel',
                                  style: AppTextStyle.bodyMedium.copyWith(
                                    color: AppColors.white,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),

                        const SizedBox(width: 12),

                        // Next Button
                        Expanded(
                          child: GestureDetector(
                            onTap: () async {
                              if (selectedImage != null) {
                                Navigator.of(
                                  context,
                                ).pop(); // Close modal first

                                // Navigate to crop screen
                                try {
                                  final croppedImage =
                                      await Navigator.of(context).push<File>(
                                        MaterialPageRoute(
                                          builder: (context) => ImageCropScreen(
                                            imageFile: selectedImage!,
                                          ),
                                        ),
                                      );

                                  // Update the selected image with cropped version if available
                                  if (croppedImage != null) {
                                    setState(() {
                                      selectedImage = croppedImage;
                                    });
                                  }
                                } catch (e) {
                                  // Show error message if cropping fails
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(
                                      content: Text(
                                        'Image cropping failed: ${e.toString()}',
                                      ),
                                      backgroundColor: Colors.red,
                                      duration: const Duration(seconds: 3),
                                    ),
                                  );
                                }
                              } else {
                                // Show message to select image first
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text(
                                      'Please select an image first',
                                    ),
                                    backgroundColor: Colors.orange,
                                  ),
                                );
                              }
                            },
                            child: Container(
                              height: 48,
                              decoration: BoxDecoration(
                                color: AppColors.secondary,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Center(
                                child: Text(
                                  'Next',
                                  style: AppTextStyle.bodyMedium.copyWith(
                                    color: AppColors.white,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
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
      },
    );
  }

  Future<void> _pickImage() async {
    final ImagePicker picker = ImagePicker();
    final XFile? image = await picker.pickImage(
      source: ImageSource.gallery,
      maxWidth: 1024,
      maxHeight: 1024,
      imageQuality: 85,
    );

    if (image != null) {
      setState(() {
        selectedImage = File(image.path);
      });
    }
  }

  Widget _buildBottomSection() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 0),
      child: _getCurrentSlider(),
    );
  }

  Widget _getCurrentSlider() {
    switch (selectedTabIndex) {
      case 0:
        return _buildBrightnessSection();
      case 1:
        return _buildOpacitySection();
      case 2:
        return _buildOpacitySection();
      case 3:
        return _buildColorPickerSection();
      case 4:
        return _buildVolumeSection();
      case 5:
        return _buildStickersOpacitySection();
      default:
        return _buildOpacitySection();
    }
  }

  Widget _buildTabNavigation() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 0),

      child: Row(
        children: tabs.asMap().entries.map((entry) {
          int index = entry.key;
          TabItem tab = entry.value;
          bool isSelected = selectedTabIndex == index;

          return Expanded(
            child: GestureDetector(
              onTap: () => setState(() => selectedTabIndex = index),
              child: Container(
                // padding: const EdgeInsets.symmetric(vertical: 8),
                child: Column(
                  children: [
                    // Icon
                    Container(
                      width: 50,
                      height: 50,
                      decoration: BoxDecoration(
                        color: isSelected
                            ? AppColors.secondary
                            : Color(0xff1c3453),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Icon(
                        tab.icon,
                        color: isSelected ? AppColors.primary : AppColors.white,
                        size: 24,
                      ),
                    ),

                    const SizedBox(height: 4),

                    // Label
                    Text(
                      tab.label,
                      style: AppTextStyle.bodySmall.copyWith(
                        color: isSelected
                            ? AppColors.secondary
                            : AppColors.white,
                        fontWeight: FontWeight.w500,
                      ),
                    ),

                    // Underline indicator
                    if (isSelected)
                      Container(
                        margin: const EdgeInsets.only(top: 4),
                        height: 2,
                        width: 20,
                        decoration: BoxDecoration(
                          color: AppColors.secondary,
                          borderRadius: BorderRadius.circular(1),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildUploadPhotoSection() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          // Avatar
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              image: const DecorationImage(
                image: AssetImage(AppIcons.image_icon),
                fit: BoxFit.fill,
              ),
            ),
          ),

          const SizedBox(width: 12),

          // Text Content
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Upload Photo',
                  style: AppTextStyle.bodyLarge.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Upload High Quality Photo',
                  style: AppTextStyle.bodySmall.copyWith(color: AppColors.grey),
                ),
              ],
            ),
          ),

          // Camera Icon
          GestureDetector(
            onTap: () => _showImageUploadDialog(),
            child: Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: AppColors.lightGrey,
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(
                Icons.camera_alt,
                color: AppColors.secondary,
                size: 24,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecentlyUploadedSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Recently Uploaded',
          style: AppTextStyle.titleMedium.copyWith(
            color: AppColors.black,
            fontWeight: FontWeight.w600,
          ),
        ),

        const SizedBox(height: 12),

        // Image Grid
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            crossAxisSpacing: 8,
            mainAxisSpacing: 8,
            childAspectRatio: 1.2,
          ),
          itemCount: 6,
          itemBuilder: (context, index) {
            return Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(8),
                color: AppColors.lightGrey,
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Placeholder(),
              ),
            );
          },
        ),
      ],
    );
  }

  Widget _buildFontStyleGrid() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 6,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1,
          ),
          itemCount: 20, // Total number of font styles
          itemBuilder: (context, index) {
            return _buildFontStyleItem(index);
          },
        ),
      ],
    );
  }

  Widget _buildFontStyleItem(int index) {
    bool isSelected = selectedFontStyle == index;

    return GestureDetector(
      onTap: () => setState(() => selectedFontStyle = index),
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AppColors.white,
          border: isSelected
              ? Border.all(color: AppColors.secondary, width: 2)
              : null,
        ),
        child: Stack(
          children: [
            Container(
              margin: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.white,
              ),
              child: Center(
                child: Text(
                  _getFontPreviewText(index),
                  style: _getFontStyle(index).copyWith(color: AppColors.black),
                ),
              ),
            ),
            if (isSelected)
              Positioned(
                top: 4,
                right: 4,
                child: Container(
                  width: 16,
                  height: 16,
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Icon(
                    Icons.check,
                    color: AppColors.white,
                    size: 12,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  String _getFontPreviewText(int index) {
    List<String> previewTexts = [
      'F', // 1 - Roboto
      'Aa', // 2 - Open Sans
      'Aa', // 3 - Lato
      'Aa', // 4 - Montserrat
      'Aa', // 5 - Poppins
      'Aa', // 6 - Playfair Display (Selected)
      'Aa', // 7 - Dancing Script
      'Aa', // 8 - Pacifico
      'Aa', // 9 - Lobster
      'Aa', // 10 - Righteous
      'Aa', // 11 - Bebas Neue
      'Aa', // 12 - Oswald
      'Aa', // 13 - Raleway
      'Aa', // 14 - Source Sans 3
      'Aa', // 15 - Nunito
      'Aa', // 16 - Inter
      'Aa', // 17 - Work Sans
      'Aa', // 18 - Fira Sans
      'Aa', // 19 - Ubuntu
      'Aa', // 20 - Noto Sans
    ];

    return previewTexts[index % previewTexts.length];
  }

  TextStyle _getFontStyle(int index) {
    List<TextStyle> fontStyles = [
      GoogleFonts.roboto(fontSize: 18, fontWeight: FontWeight.w500), // 1
      GoogleFonts.lilyScriptOne(fontSize: 18, fontWeight: FontWeight.w600), // 2
      GoogleFonts.lato(fontSize: 18, fontWeight: FontWeight.w700), // 3
      GoogleFonts.montserrat(fontSize: 18, fontWeight: FontWeight.w500), // 4
      GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.w600), // 5
      GoogleFonts.playfairDisplay(
        fontSize: 18,
        fontWeight: FontWeight.w700,
      ), // 6 - Selected by default
      GoogleFonts.dancingScript(fontSize: 20, fontWeight: FontWeight.w400), // 7
      GoogleFonts.pacifico(fontSize: 16, fontWeight: FontWeight.w400), // 8
      GoogleFonts.lobster(fontSize: 16, fontWeight: FontWeight.w400), // 9
      GoogleFonts.righteous(fontSize: 18, fontWeight: FontWeight.w400), // 10
      GoogleFonts.bebasNeue(fontSize: 20, fontWeight: FontWeight.w400), // 11
      GoogleFonts.oswald(fontSize: 18, fontWeight: FontWeight.w500), // 12
      GoogleFonts.raleway(fontSize: 18, fontWeight: FontWeight.w600), // 13
      GoogleFonts.sourceSans3(fontSize: 18, fontWeight: FontWeight.w500), // 14
      GoogleFonts.nunito(fontSize: 18, fontWeight: FontWeight.w600), // 15
      GoogleFonts.inter(fontSize: 18, fontWeight: FontWeight.w500), // 16
      GoogleFonts.workSans(fontSize: 18, fontWeight: FontWeight.w600), // 17
      GoogleFonts.firaSans(fontSize: 18, fontWeight: FontWeight.w500), // 18
      GoogleFonts.ubuntu(fontSize: 18, fontWeight: FontWeight.w500), // 19
      GoogleFonts.notoSans(fontSize: 18, fontWeight: FontWeight.w500), // 20
    ];

    return fontStyles[index % fontStyles.length];
  }

  Widget _buildSoundStyleGrid() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 6,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1,
          ),
          itemCount: 20, // Total number of sound styles
          itemBuilder: (context, index) {
            return _buildSoundStyleItem(index);
          },
        ),
      ],
    );
  }

  Widget _buildSoundStyleItem(int index) {
    bool isSelected = selectedSoundStyle == index;

    return GestureDetector(
      onTap: () => setState(() => selectedSoundStyle = index),
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AppColors.white,
          border: isSelected
              ? Border.all(color: AppColors.secondary, width: 2)
              : null,
        ),
        child: Stack(
          children: [
            Container(
              margin: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _getSoundColor(index),
              ),
              child: Center(
                child: Icon(
                  _getSoundIcon(index),
                  color: AppColors.white,
                  size: 20,
                ),
              ),
            ),
            if (isSelected)
              Positioned(
                top: 4,
                right: 4,
                child: Container(
                  width: 16,
                  height: 16,
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Icon(
                    Icons.check,
                    color: AppColors.white,
                    size: 12,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Color _getSoundColor(int index) {
    // Different colors for each sound effect
    List<Color> colors = [
      Colors.grey, // 0 - No sound (X)
      Colors.blue, // 1 - Typing sound
      Colors.blue, // 2 - Speech bubble
      Colors.orange, // 3 - Dog sound
      Colors.pink, // 4 - Bubbles
      Colors.green, // 5 - Phone
      Colors.purple, // 6 - Piano
      Colors.orange, // 7 - Guitar
      Colors.purple, // 8 - Rain
      Colors.orange, // 9 - Pepperoni
      Colors.purple, // 10 - Dice
      Colors.red, // 11 - Bell
      Colors.orange, // 12 - Balloon
      Colors.orange, // 13 - Dog
      Colors.green, // 14 - Football
      Colors.orange, // 15 - Lightning
      Colors.orange, // 16 - Bell
      Colors.orange, // 17 - Gun
      Colors.orange, // 18 - Explosion
      Colors.pink, // 19 - Ghost
    ];

    return colors[index % colors.length];
  }

  IconData _getSoundIcon(int index) {
    // Different icons for each sound effect
    List<IconData> icons = [
      Icons.close, // 0 - No sound
      Icons.keyboard, // 1 - Typing sound
      Icons.chat_bubble, // 2 - Speech bubble
      Icons.pets, // 3 - Dog sound
      Icons.bubble_chart, // 4 - Bubbles
      Icons.phone, // 5 - Phone
      Icons.piano, // 6 - Piano
      Icons.music_note, // 7 - Guitar
      Icons.water_drop, // 8 - Rain
      Icons.circle, // 9 - Pepperoni
      Icons.casino, // 10 - Dice
      Icons.notifications, // 11 - Bell
      Icons.celebration, // 12 - Balloon
      Icons.pets, // 13 - Dog
      Icons.sports_football, // 14 - Football
      Icons.flash_on, // 15 - Lightning
      Icons.notifications, // 16 - Bell
      Icons.sports_esports, // 17 - Gun
      Icons.local_fire_department, // 18 - Explosion
      Icons.face, // 19 - Ghost
    ];

    return icons[index % icons.length];
  }

  Widget _buildStickersStyleGrid() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 6,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1,
          ),
          itemCount: 20, // Total number of sticker styles
          itemBuilder: (context, index) {
            return _buildStickerStyleItem(index);
          },
        ),
      ],
    );
  }

  Widget _buildStickerStyleItem(int index) {
    bool isSelected = selectedStickerStyle == index;

    return GestureDetector(
      onTap: () => setState(() => selectedStickerStyle = index),
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AppColors.white,
          border: isSelected
              ? Border.all(color: AppColors.secondary, width: 2)
              : null,
        ),
        child: Stack(
          children: [
            Container(
              margin: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.white,
              ),
              child: Center(
                child: Icon(
                  _getStickerIcon(index),
                  color: _getStickerColor(index),
                  size: 24,
                ),
              ),
            ),
            if (isSelected)
              Positioned(
                top: 4,
                right: 4,
                child: Container(
                  width: 16,
                  height: 16,
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Icon(
                    Icons.check,
                    color: AppColors.white,
                    size: 12,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Color _getStickerColor(int index) {
    // Different colors for each sticker
    List<Color> colors = [
      Colors.yellow, // 0 - Smiley face
      Colors.yellow, // 1 - Surprised face
      Colors.yellow, // 2 - Angry face
      Colors.yellow, // 3 - Wide-eyed face
      Colors.blue, // 4 - Rainbow
      Colors.pink, // 5 - Heart with wings
      Colors.black, // 6 - Panda
      Colors.orange, // 7 - Cat and dog
      Colors.red, // 8 - Red heart
      Colors.orange, // 9 - Fox face
      Colors.red, // 10 - Speech bubble
      Colors.brown, // 11 - Dog
      Colors.pink, // 12 - Alarm clock
      Colors.pink, // 13 - Heart with text
      Colors.brown, // 14 - Circle with text
      Colors.blue, // 15 - Birds
      Colors.orange, // 16 - Butterflies
      Colors.purple, // 17 - Haunted house
      Colors.yellow, // 18 - Explosion
      Colors.pink, // 19 - Night text
    ];

    return colors[index % colors.length];
  }

  IconData _getStickerIcon(int index) {
    // Different icons for each sticker
    List<IconData> icons = [
      Icons.sentiment_satisfied, // 0 - Smiley face
      Icons.sentiment_very_dissatisfied, // 1 - Surprised face
      Icons.sentiment_very_dissatisfied, // 2 - Angry face
      Icons.sentiment_neutral, // 3 - Wide-eyed face
      Icons.wb_sunny, // 4 - Rainbow
      Icons.favorite, // 5 - Heart with wings
      Icons.pets, // 6 - Panda
      Icons.pets, // 7 - Cat and dog
      Icons.favorite, // 8 - Red heart
      Icons.pets, // 9 - Fox face
      Icons.chat_bubble, // 10 - Speech bubble
      Icons.pets, // 11 - Dog
      Icons.access_time, // 12 - Alarm clock
      Icons.favorite, // 13 - Heart with text
      Icons.circle, // 14 - Circle with text
      Icons.flight, // 15 - Birds
      Icons.flight, // 16 - Butterflies
      Icons.home, // 17 - Haunted house
      Icons.local_fire_department, // 18 - Explosion
      Icons.nightlight_round, // 19 - Night text
    ];

    return icons[index % icons.length];
  }

  Widget _buildStickersOpacitySection() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          // Opacity Label
          Text(
            'Opacity',
            style: AppTextStyle.bodyLarge.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w500,
            ),
          ),

          const SizedBox(width: 16),

          // Opacity Slider
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: AppColors.secondary,
                inactiveTrackColor: AppColors.lightGrey,
                thumbColor: AppColors.white,
                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 8),
                trackHeight: 4,
              ),
              child: Slider(
                value: stickersOpacityValue,
                onChanged: (value) {
                  setState(() {
                    stickersOpacityValue = value;
                  });
                },
                min: 0.0,
                max: 1.0,
              ),
            ),
          ),

          const SizedBox(width: 16),

          // Chevron Down Icon (Keyboard Toggle)
          GestureDetector(
            onTap: () {
              setState(() {
                isKeyboardVisible = !isKeyboardVisible;
                if (isKeyboardVisible) {
                  _keyboardFocusNode.requestFocus();
                } else {
                  _keyboardFocusNode.unfocus();
                }
              });
            },
            child: Icon(
              isKeyboardVisible
                  ? Icons.keyboard_arrow_down
                  : Icons.keyboard_arrow_up,
              color: AppColors.grey,
              size: 20,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildVolumeSection() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          // Volume Label
          Text(
            'Volume',
            style: AppTextStyle.bodyLarge.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w500,
            ),
          ),

          const SizedBox(width: 16),

          // Volume Slider
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: AppColors.secondary,
                inactiveTrackColor: AppColors.lightGrey,
                thumbColor: AppColors.white,
                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 8),
                trackHeight: 4,
              ),
              child: Slider(
                value: volumeValue,
                onChanged: (value) {
                  setState(() {
                    volumeValue = value;
                  });
                },
                min: 0.0,
                max: 1.0,
              ),
            ),
          ),

          const SizedBox(width: 16),

          // Chevron Down Icon (Keyboard Toggle)
          GestureDetector(
            onTap: () {
              setState(() {
                isKeyboardVisible = !isKeyboardVisible;
                if (isKeyboardVisible) {
                  _keyboardFocusNode.requestFocus();
                } else {
                  _keyboardFocusNode.unfocus();
                }
              });
            },
            child: Icon(
              isKeyboardVisible
                  ? Icons.keyboard_arrow_down
                  : Icons.keyboard_arrow_up,
              color: AppColors.grey,
              size: 20,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildColorPickerSection() {
    return Container(
      height: 70,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          // Colour Label
          Text(
            'Colour',
            style: AppTextStyle.bodyLarge.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w500,
            ),
          ),

          const SizedBox(width: 16),

          // Rainbow Gradient Slider
          Expanded(
            child: Container(
              height: 10,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: AppColors.grey),
                gradient: LinearGradient(
                  colors: [
                    Colors.white,
                    Colors.red,
                    Colors.orange,
                    Colors.yellow,
                    Colors.green,
                    Colors.blue,
                    Colors.indigo,
                    Colors.purple,
                    Colors.black,
                  ],
                  begin: Alignment.centerLeft,
                  end: Alignment.centerRight,
                ),
              ),
              child: SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  activeTrackColor: Colors.transparent,
                  inactiveTrackColor: Colors.transparent,
                  thumbColor: AppColors.white,
                  thumbShape: const RoundSliderThumbShape(
                    enabledThumbRadius: 12,
                  ),
                  trackHeight: 0,
                  overlayShape: const RoundSliderOverlayShape(
                    overlayRadius: 20,
                  ),
                ),
                child: Slider(
                  value: colorValue,
                  onChanged: (value) {
                    setState(() {
                      colorValue = value;
                    });
                  },
                  min: 0.0,
                  max: 1.0,
                ),
              ),
            ),
          ),

          const SizedBox(width: 32),

          // Chevron Down Icon (Keyboard Toggle)
          GestureDetector(
            onTap: () {
              setState(() {
                isKeyboardVisible = !isKeyboardVisible;
                if (isKeyboardVisible) {
                  _keyboardFocusNode.requestFocus();
                } else {
                  _keyboardFocusNode.unfocus();
                }
              });
            },
            child: Icon(
              isKeyboardVisible
                  ? Icons.keyboard_arrow_down
                  : Icons.keyboard_arrow_up,
              color: AppColors.grey,
              size: 20,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEffectStyleGrid() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 5,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1,
          ),
          itemCount: 20, // Total number of effect styles
          itemBuilder: (context, index) {
            return _buildEffectStyleItem(index);
          },
        ),
      ],
    );
  }

  Widget _buildEffectStyleItem(int index) {
    bool isSelected = selectedEffectStyle == index;

    return GestureDetector(
      onTap: () => setState(() => selectedEffectStyle = index),
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AppColors.white,
          border: isSelected
              ? Border.all(color: AppColors.secondary, width: 2)
              : null,
        ),
        child: Container(
          margin: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: _getEffectColor(index),
          ),
          child: Center(
            child: Text(
              '${index + 1}',
              style: AppTextStyle.bodyLarge.copyWith(
                color: AppColors.white,
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
          ),
        ),
      ),
    );
  }

  Color _getEffectColor(int index) {
    // Different colors for each effect placeholder
    List<Color> colors = [
      Colors.red, // 1 - Red heart
      Colors.grey, // 2 - Grey X
      Colors.pink, // 3 - Pink lips
      Colors.blue, // 4 - Blue bubbles
      Colors.purple, // 5 - Purple mushroom
      Colors.green, // 6 - Green leaves
      Colors.orange, // 7 - Orange maple leaf
      Colors.lightBlue, // 8 - Blue snowflake
      Colors.pink, // 9 - Pink hearts
      Colors.blue, // 10 - Blue heart balloon
      Colors.yellow, // 11 - Yellow star
      Colors.yellow, // 12 - Yellow balloon
      Colors.grey, // 13 - White cat
      Colors.brown, // 14 - Brown football
      Colors.yellow, // 15 - Yellow lightning
      Colors.amber, // 16 - Golden bell
      Colors.pink, // 17 - Pink lips
      Colors.pink, // 18 - Pink butterfly
      Colors.red, // 19 - Red cricket ball
      Colors.orange, // 20 - Orange explosion
    ];

    return colors[index % colors.length];
  }

  Widget _buildButtonStyleGrid() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 4,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1,
          ),
          itemCount: 20, // Total number of button styles
          itemBuilder: (context, index) {
            return _buildButtonStyleItem(index);
          },
        ),
      ],
    );
  }

  Widget _buildButtonStyleItem(int index) {
    bool isSelected = selectedButtonStyle == index;

    return GestureDetector(
      onTap: () => setState(() => selectedButtonStyle = index),
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AppColors.white,
          border: isSelected
              ? Border.all(color: AppColors.secondary, width: 2)
              : null,
        ),
        child: _getButtonStyleWidget(index, isSelected),
      ),
    );
  }

  Widget _getButtonStyleWidget(int index, bool isSelected) {
    switch (index) {
      case 0:
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          width: 10,
          height: 15,
          decoration: BoxDecoration(
            color: AppColors.lightGrey,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: AppColors.grey,
              width: 2,
              style: BorderStyle.solid,
            ),
          ),
        );
      case 1:
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          width: 10,
          height: 15,
          decoration: BoxDecoration(
            color: AppColors.secondary,
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Icon(
            Icons.text_format_outlined,
            color: AppColors.white,
            size: 28,
          ),
        );
      case 2:
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          width: 10,
          height: 15,
          decoration: BoxDecoration(
            color: AppColors.tertiary,
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Icon(
            Icons.text_format_outlined,
            color: AppColors.white,
            size: 28,
          ),
        );
      case 3:
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          width: 10,
          height: 15,
          decoration: BoxDecoration(
            color: Colors.red,
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Icon(
            Icons.text_fields,
            color: AppColors.white,
            size: 20,
          ),
        );
      case 4:
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          width: 10,
          height: 15,
          decoration: BoxDecoration(
            color: AppColors.secondary,
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Icon(
            Icons.text_format_outlined,
            color: AppColors.black,
            size: 28,
          ),
        );
      case 5:
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          width: 10,
          height: 15,
          decoration: BoxDecoration(
            color: AppColors.tertiary,
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Icon(
            Icons.text_format_outlined,
            color: AppColors.white,
            size: 20,
          ),
        );
      case 6:
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          width: 10,
          height: 15,
          decoration: BoxDecoration(
            color: AppColors.lightGrey,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.purple, width: 1),
          ),
        );
      case 7:
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          width: 10,
          height: 15,
          decoration: BoxDecoration(
            color: AppColors.lightGrey,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: AppColors.secondary, width: 1),
          ),
          child: const Icon(
            Icons.text_format_outlined,
            color: AppColors.black,
            size: 28,
          ),
        );
      case 8:
        return Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [Colors.green[300]!, Colors.green[600]!],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(
            Icons.text_fields,
            color: AppColors.white,
            size: 20,
          ),
        );
      case 9:
        return Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [AppColors.secondary, Colors.orange[600]!],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(
            Icons.text_fields,
            color: AppColors.white,
            size: 20,
          ),
        );
      case 10:
        return Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [AppColors.tertiary, Colors.blue[600]!],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(
            Icons.text_fields,
            color: AppColors.white,
            size: 20,
          ),
        );
      case 11:
        return Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [Colors.pink[300]!, Colors.purple[600]!],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(
            Icons.text_fields,
            color: AppColors.white,
            size: 20,
          ),
        );
      case 12:
        return Container(
          decoration: BoxDecoration(
            color: Colors.pink[300],
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(Icons.cake, color: AppColors.white, size: 20),
        );
      case 13:
        return Container(
          decoration: BoxDecoration(
            color: Colors.grey[400],
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(Icons.pets, color: AppColors.white, size: 20),
        );
      case 14:
        return Container(
          decoration: BoxDecoration(
            color: Colors.green[400],
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(Icons.park, color: AppColors.white, size: 20),
        );
      case 15:
        return Container(
          decoration: BoxDecoration(
            color: AppColors.tertiary,
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(
            Icons.chat_bubble,
            color: AppColors.white,
            size: 20,
          ),
        );
      case 16:
        return Container(
          decoration: BoxDecoration(
            color: Colors.red[400],
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(Icons.favorite, color: AppColors.white, size: 20),
        );
      case 17:
        return Container(
          decoration: BoxDecoration(
            color: Colors.orange[400],
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(Icons.star, color: AppColors.white, size: 20),
        );
      case 18:
        return Container(
          decoration: BoxDecoration(
            color: Colors.purple[400],
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(Icons.diamond, color: AppColors.white, size: 20),
        );
      case 19:
        return Container(
          decoration: BoxDecoration(
            color: Colors.teal[400],
            borderRadius: BorderRadius.circular(50),
          ),
          child: const Icon(
            Icons.auto_awesome,
            color: AppColors.white,
            size: 20,
          ),
        );
      default:
        return Container(
          decoration: BoxDecoration(
            color: AppColors.lightGrey,
            borderRadius: BorderRadius.circular(8),
          ),
        );
    }
  }

  Widget _buildOpacitySection() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          // Opacity Label
          Text(
            'Opacity',
            style: AppTextStyle.bodyLarge.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w500,
            ),
          ),

          const SizedBox(width: 16),

          // Slider
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: AppColors.secondary,
                inactiveTrackColor: AppColors.lightGrey,
                thumbColor: AppColors.white,
                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 8),
                trackHeight: 4,
              ),
              child: Slider(
                value: opacityValue,
                onChanged: (value) {
                  setState(() {
                    opacityValue = value;
                  });
                },
                min: 0.0,
                max: 1.0,
              ),
            ),
          ),

          const SizedBox(width: 16),

          // Chevron Down Icon (Keyboard Toggle)
          GestureDetector(
            onTap: () {
              setState(() {
                isKeyboardVisible = !isKeyboardVisible;
                if (isKeyboardVisible) {
                  _keyboardFocusNode.requestFocus();
                } else {
                  _keyboardFocusNode.unfocus();
                }
              });
            },
            child: Icon(
              isKeyboardVisible
                  ? Icons.keyboard_arrow_down
                  : Icons.keyboard_arrow_up,
              color: AppColors.grey,
              size: 20,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPlaceholderContent() {
    return Container(
      padding: const EdgeInsets.all(32),
      child: Center(
        child: Column(
          children: [
            Icon(Icons.construction, size: 64, color: AppColors.grey),
            const SizedBox(height: 16),
            Text(
              'Coming Soon',
              style: AppTextStyle.titleLarge.copyWith(
                color: AppColors.grey,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'This feature is under development',
              style: AppTextStyle.bodyMedium.copyWith(color: AppColors.grey),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBrightnessSection() {
    return Container(
      // margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          // Brightness Label
          Text(
            'Brightness',
            style: AppTextStyle.bodyLarge.copyWith(
              color: AppColors.black,
              fontWeight: FontWeight.w500,
            ),
          ),

          const SizedBox(width: 16),

          // Slider
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: AppColors.secondary,
                inactiveTrackColor: AppColors.lightGrey,
                thumbColor: AppColors.white,
                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 8),
                trackHeight: 4,
              ),
              child: Slider(
                value: brightnessValue,
                onChanged: (value) {
                  setState(() {
                    brightnessValue = value;
                  });
                },
                min: 0.0,
                max: 1.0,
              ),
            ),
          ),

          const SizedBox(width: 16),

          // Chevron Down Icon (Keyboard Toggle)
          GestureDetector(
            onTap: () {
              setState(() {
                isKeyboardVisible = !isKeyboardVisible;
                if (isKeyboardVisible) {
                  _keyboardFocusNode.requestFocus();
                } else {
                  _keyboardFocusNode.unfocus();
                }
              });
            },
            child: Icon(
              isKeyboardVisible
                  ? Icons.keyboard_arrow_down
                  : Icons.keyboard_arrow_up,
              color: AppColors.grey,
              size: 20,
            ),
          ),
        ],
      ),
    );
  }
}

class TabItem {
  final IconData icon;
  final String label;
  final bool isSelected;

  TabItem({required this.icon, required this.label, required this.isSelected});
}
