import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';

class OnBoardingScreen1 extends StatefulWidget {
  const OnBoardingScreen1({super.key});

  @override
  State<OnBoardingScreen1> createState() => _OnBoardingScreen1State();
}

class _OnBoardingScreen1State extends State<OnBoardingScreen1> {
  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final screenHeight = MediaQuery.of(context).size.height;
    return Scaffold(
      backgroundColor: AppColors.primary,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              _builtHeaderText(),
              _builtIllustration(screenWidth: screenWidth),
              SizedBox(height: 128),
              _builtFooter(),
            ],
          ),
        ),
      ),
    );
  }
}

class _builtFooter extends StatelessWidget {
  const _builtFooter({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Welcome to Kvive',
          style: AppTextStyle.headlineLarge.copyWith(
            color: AppColors.white,
            fontWeight: FontWeight.bold,
            fontSize: 25,
          ),
        ),
        SizedBox(height: 20),
        Text(
          'Transform your typing with AI-powered smart suggestions, effortless corrections, and more! Ready to type smarter?',
          style: AppTextStyle.bodyMedium.copyWith(
            color: AppColors.white,
            fontSize: 13,
            fontWeight: FontWeight.w500,
          ),
        ),
        SizedBox(height: 20),
        Row(
          children: [
            OutlinedButton(
              style: ButtonStyle(
                shape: WidgetStatePropertyAll(
                  RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                    side: BorderSide(color: AppColors.secondary),
                  ),
                ),
                padding: WidgetStatePropertyAll(
                  EdgeInsets.symmetric(horizontal: 20, vertical: 18),
                ),
                backgroundColor: WidgetStatePropertyAll(AppColors.primary),
                foregroundColor: WidgetStatePropertyAll(AppColors.secondary),
                side: WidgetStatePropertyAll(
                  BorderSide(color: AppColors.secondary),
                ),
              ),
              onPressed: () {},
              child: Text(
                'Skip',
                style: AppTextStyle.buttonPrimary.copyWith(
                  color: AppColors.secondary,
                ),
              ),
            ),
            Spacer(),
            Row(
              spacing: 10,
              children: [
                Container(
                  width: 10,
                  height: 10,
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                  ),
                ),
                Container(
                  width: 10,
                  height: 10,
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                  ),
                ),
                Container(
                  width: 10,
                  height: 10,
                  decoration: BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                  ),
                ),
              ],
            ),

            Spacer(),
            OutlinedButton(
              style: ButtonStyle(
                // fixedSize: WidgetStatePropertyAll(Size(40, 40)),
                shape: WidgetStatePropertyAll(
                  RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                    side: BorderSide(color: AppColors.secondary),
                  ),
                ),
                padding: WidgetStatePropertyAll(
                  EdgeInsets.symmetric(horizontal: 20, vertical: 18),
                ),
                backgroundColor: WidgetStatePropertyAll(AppColors.secondary),
                foregroundColor: WidgetStatePropertyAll(AppColors.white),
                side: WidgetStatePropertyAll(
                  BorderSide(color: AppColors.secondary),
                ),
              ),
              onPressed: () {},
              child: Icon(Icons.arrow_forward_ios),
            ),
          ],
        ),
      ],
    );
  }
}

class _builtIllustration extends StatelessWidget {
  const _builtIllustration({super.key, required this.screenWidth});

  final double screenWidth;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        width: screenWidth,
        decoration: BoxDecoration(
          image: DecorationImage(
            image: AssetImage(AppAssets.onboardingElement),
            fit: BoxFit.contain,
          ),
        ),
      ),
    );
  }
}

class _builtHeaderText extends StatelessWidget {
  const _builtHeaderText({super.key});

  @override
  Widget build(BuildContext context) {
    return Text(
      'Kvive',
      style: AppTextStyle.displaySmall.copyWith(color: AppColors.secondary),
    );
  }
}
