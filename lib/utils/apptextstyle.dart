import 'package:flutter/material.dart';

import 'appassets.dart';

class AppTextStyle {
  // Display
  static TextStyle displayLarge = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 48,
    fontWeight: FontWeight.w700,
    height: 1.2,
    color: AppColors.secondary,
  );

  static TextStyle displayMedium = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 40,
    fontWeight: FontWeight.w700,
    height: 1.2,
    color: AppColors.secondary,
  );

  static TextStyle displaySmall = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 34,
    fontWeight: FontWeight.w700,
    height: 1.2,
    color: AppColors.secondary,
  );

  // Headline
  static TextStyle headlineLarge = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 28,
    fontWeight: FontWeight.w700,
    height: 1.25,
    color: AppColors.black,
  );

  static TextStyle headlineMedium = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 24,
    fontWeight: FontWeight.w600,
    height: 1.25,
    color: AppColors.black,
  );

  static TextStyle headlineSmall = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 20,
    fontWeight: FontWeight.w600,
    height: 1.3,
    color: AppColors.black,
  );

  // Title
  static TextStyle titleLarge = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 18,
    fontWeight: FontWeight.w600,
    height: 1.35,
    color: AppColors.black,
  );

  static TextStyle titleMedium = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 16,
    fontWeight: FontWeight.w500,
    height: 1.4,
    color: AppColors.black,
  );

  static TextStyle titleSmall = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 14,
    fontWeight: FontWeight.w500,
    height: 1.4,
    color: AppColors.black,
  );

  // // Body (Roboto preferred for readability)
  // static TextStyle bodyLarge = TextStyle(
  //   fontFamily: 'roboto',
  //   fontSize: 16,
  //   fontWeight: FontWeight.w400,
  //   height: 1.5,
  //   color: AppColors.black,
  // );

  static TextStyle bodyMedium = TextStyle(
    fontFamily: 'roboto',
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.5,
    color: AppColors.black,
  );

  static TextStyle bodySmall = TextStyle(
    fontFamily: 'roboto',
    fontSize: 12,
    fontWeight: FontWeight.w400,
    height: 1.5,
    color: AppColors.grey,
  );

  // Labels
  static TextStyle labelLarge = TextStyle(
    fontFamily: 'roboto',
    fontSize: 14,
    fontWeight: FontWeight.w500,
    letterSpacing: 0.1,
    height: 1.3,
    color: AppColors.black,
  );

  static TextStyle labelMedium = TextStyle(
    fontFamily: 'roboto',
    fontSize: 12,
    fontWeight: FontWeight.w500,
    letterSpacing: 0.1,
    height: 1.3,
    color: AppColors.black,
  );

  static TextStyle labelSmall = TextStyle(
    fontFamily: 'roboto',
    fontSize: 11,
    fontWeight: FontWeight.w500,
    letterSpacing: 0.2,
    height: 1.2,
    color: AppColors.grey,
  );

  // Buttons
  static TextStyle buttonPrimary = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.3,
    color: AppColors.white,
  );

  static TextStyle buttonSecondary = TextStyle(
    fontFamily: 'noto_sans',
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.3,
    color: AppColors.primary,
  );
}
