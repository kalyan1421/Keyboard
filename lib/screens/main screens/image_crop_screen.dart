import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_cropper/image_cropper.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'dart:io';

class ImageCropScreen extends StatefulWidget {
  final File imageFile;

  const ImageCropScreen({super.key, required this.imageFile});

  @override
  State<ImageCropScreen> createState() => _ImageCropScreenState();
}

class _ImageCropScreenState extends State<ImageCropScreen> {
  File? croppedImage;
  bool isProcessing = false;
  bool hasStartedCropping = false;

  @override
  void initState() {
    super.initState();
    // Delay the cropping to ensure the screen is fully loaded
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!hasStartedCropping) {
        _cropImage();
      }
    });
  }

  Future<void> _cropImage() async {
    // Prevent multiple simultaneous cropping operations
    if (hasStartedCropping || isProcessing) {
      return;
    }

    setState(() {
      isProcessing = true;
      hasStartedCropping = true;
    });

    try {
      // Request permissions first
      await _requestPermissions();

      // Add timeout to prevent hanging
      final croppedFile = await Future.any([
        ImageCropper().cropImage(
          sourcePath: widget.imageFile.path,
          aspectRatio: const CropAspectRatio(
            ratioX: 16,
            ratioY: 9,
          ), // Keyboard aspect ratio
          compressFormat: ImageCompressFormat.jpg,
          compressQuality: 90,
          uiSettings: [
            AndroidUiSettings(
              toolbarTitle: 'Crop Image',
              toolbarColor: AppColors.primary,
              toolbarWidgetColor: AppColors.white,
              initAspectRatio: CropAspectRatioPreset.ratio16x9,
              lockAspectRatio: true,
              backgroundColor: AppColors.black,
              activeControlsWidgetColor: AppColors.secondary,
              cropFrameColor: AppColors.white,
              cropGridColor: AppColors.white.withOpacity(0.5),
              cropFrameStrokeWidth: 2,
              cropGridStrokeWidth: 1,
              hideBottomControls: false,
              statusBarColor: AppColors.primary,
            ),
            IOSUiSettings(
              title: 'Crop Image',
              doneButtonTitle: 'Done',
              cancelButtonTitle: 'Back',
              aspectRatioLockEnabled: true,
              resetAspectRatioEnabled: false,
              aspectRatioPickerButtonHidden: true,
              rotateButtonsHidden: false,
              rotateClockwiseButtonHidden: false,
              hidesNavigationBar: false,
              minimumAspectRatio: 1.0,
            ),
          ],
        ),
        Future.delayed(
          const Duration(minutes: 2),
          () => null,
        ), // 2 minute timeout
      ]);

      if (croppedFile != null) {
        setState(() {
          croppedImage = File(croppedFile.path);
          isProcessing = false;
        });
      } else {
        // User cancelled cropping, go back
        Navigator.of(context).pop();
      }
    } catch (e) {
      setState(() {
        isProcessing = false;
      });

      // Show detailed error message with fallback options
      String errorMessage = 'Error cropping image';
      bool isCompatibilityIssue = false;

      if (e.toString().contains('permission')) {
        errorMessage =
            'Permission denied. Please allow storage access in settings.';
      } else if (e.toString().contains('file')) {
        errorMessage =
            'File access error. Please try selecting the image again.';
      } else if (e.toString().contains('compile') ||
          e.toString().contains('symbol')) {
        errorMessage =
            'Image cropper compatibility issue. Using original image instead.';
        isCompatibilityIssue = true;
      } else if (e.toString().contains('Reply already submitted')) {
        errorMessage =
            'Cropping operation already in progress. Please try again.';
        isCompatibilityIssue = true;
      } else if (e.toString().contains('TimeoutException') ||
          e.toString().contains('timeout')) {
        errorMessage =
            'Cropping took too long. Please try again or use the original image.';
        isCompatibilityIssue = true;
      } else if (e.toString().contains('ActivityNotFoundException') ||
          e.toString().contains('UCropActivity')) {
        errorMessage =
            'Image cropping activity not found. Using original image instead.';
        isCompatibilityIssue = true;
        // Automatically use original image for this specific error
        setState(() {
          croppedImage = widget.imageFile;
          isProcessing = false;
        });
        return; // Skip showing dialog, just use original image
      } else {
        errorMessage = 'Error cropping image: ${e.toString()}';
      }

      // Show error dialog with options
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Cropping Error'),
          content: Text(errorMessage),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(); // Close dialog
                Navigator.of(context).pop(); // Go back to previous screen
              },
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(); // Close dialog
                // Use original image as fallback
                setState(() {
                  croppedImage = widget.imageFile;
                  isProcessing = false;
                });
              },
              child: const Text('Use Original'),
            ),
            if (!isCompatibilityIssue)
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop(); // Close dialog
                  // Reset the flag and retry
                  hasStartedCropping = false;
                  _cropImage(); // Retry
                },
                child: const Text('Retry'),
              ),
          ],
        ),
      );
    }
  }

  @override
  void dispose() {
    // Clean up any ongoing operations
    super.dispose();
  }

  Future<void> _requestPermissions() async {
    // Request storage permissions
    Map<Permission, PermissionStatus> statuses = await [
      Permission.storage,
      Permission.photos,
      Permission.camera,
    ].request();

    // Check if all permissions are granted
    bool allGranted = statuses.values.every((status) => status.isGranted);

    if (!allGranted) {
      // Show permission dialog
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Permissions Required'),
          content: const Text(
            'This app needs storage and camera permissions to crop images. '
            'Please grant these permissions in the app settings.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                openAppSettings();
              },
              child: const Text('Open Settings'),
            ),
          ],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // appBar: AppBar(
      //   leading: IconButton(
      //     onPressed: () => Navigator.of(context).pop(),
      //     icon: const Icon(Icons.arrow_back),
      //   ),
      //   backgroundColor: AppColors.black,
      //   title: const Text('Crop Image'),
      //   actions: [
      //     IconButton(
      //       onPressed: () => Navigator.of(context).pop(),
      //       icon: const Icon(Icons.notifications),
      //     ),
      //   ],
      // ),
      backgroundColor: AppColors.black,
      body: SafeArea(
        child: Column(
          children: [
            // Header
            Container(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  GestureDetector(
                    onTap: () => Navigator.of(context).pop(),
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: AppColors.white.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(
                        Icons.arrow_back,
                        color: AppColors.white,
                        size: 24,
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Text(
                      'Crop Image for Keyboard',
                      style: AppTextStyle.titleLarge.copyWith(
                        color: AppColors.white,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            // Instructions
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Text(
                'Adjust the crop area to fit your keyboard. The image will be resized to 16:9 aspect ratio.',
                style: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.white.withOpacity(0.8),
                ),
                textAlign: TextAlign.center,
              ),
            ),

            const SizedBox(height: 20),

            // Processing indicator or cropped image preview
            Expanded(
              child: Center(
                child: isProcessing
                    ? Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const CircularProgressIndicator(
                            color: AppColors.secondary,
                          ),
                          const SizedBox(height: 16),
                          Text(
                            'Processing image...',
                            style: AppTextStyle.bodyLarge.copyWith(
                              color: AppColors.white,
                            ),
                          ),
                        ],
                      )
                    : croppedImage != null
                    ? Column(
                        children: [
                          // Cropped image preview
                          Container(
                            margin: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(12),
                              boxShadow: [
                                BoxShadow(
                                  color: Colors.black.withOpacity(0.3),
                                  blurRadius: 10,
                                  offset: const Offset(0, 5),
                                ),
                              ],
                            ),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(12),
                              child: AspectRatio(
                                aspectRatio: 16 / 9,
                                child: Image.file(
                                  croppedImage!,
                                  fit: BoxFit.cover,
                                ),
                              ),
                            ),
                          ),

                          // Success message
                          Text(
                            'Image cropped successfully!',
                            style: AppTextStyle.bodyLarge.copyWith(
                              color: AppColors.white,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Your keyboard background is ready',
                            style: AppTextStyle.bodyMedium.copyWith(
                              color: AppColors.white.withOpacity(0.8),
                            ),
                          ),
                        ],
                      )
                    : const SizedBox(),
              ),
            ),

            // Action buttons
            if (croppedImage != null)
              Container(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    // Back button
                    Expanded(
                      child: GestureDetector(
                        onTap: () => Navigator.of(context).pop(),
                        child: Container(
                          height: 48,
                          decoration: BoxDecoration(
                            color: AppColors.white.withOpacity(0.2),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: AppColors.white.withOpacity(0.3),
                            ),
                          ),
                          child: Center(
                            child: Text(
                              'Back',
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

                    // Done button
                    Expanded(
                      child: GestureDetector(
                        onTap: () {
                          // Return the cropped image to the previous screen
                          Navigator.of(context).pop(croppedImage);
                        },
                        child: Container(
                          height: 48,
                          decoration: BoxDecoration(
                            color: AppColors.secondary,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Center(
                            child: Text(
                              'Done',
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
              ),
          ],
        ),
      ),
    );
  }
}
