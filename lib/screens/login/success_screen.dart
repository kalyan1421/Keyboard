import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';

class SuccessScreen extends StatelessWidget {
  const SuccessScreen({super.key});

  // Template onTap handlers for this page
  void onTapGoHome(BuildContext context) {
    // Navigate to main app or home screen
    Navigator.popUntil(context, (route) => route.isFirst);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(16.0),
          child: Column(
            children: [
              Spacer(),
              // _buildIllustration(),
              Image.asset(AppAssets.successIllustration, fit: BoxFit.cover),
              SizedBox(height: 32),

              Text(
                'Congratualtion !',
                style: AppTextStyle.headlineLarge.copyWith(
                  fontWeight: FontWeight.bold,
                  color: AppColors.secondary,
                  fontSize: 28,
                  fontFamily: 'roboto',
                ),
              ),
              SizedBox(height: 32),
              Text(
                'Your Sign in successfully',
                style: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.black,
                  fontSize: 16,
                  fontFamily: 'roboto',
                ),
              ),
              SizedBox(height: 32),
              _buildGoHomeButton(context),
              Spacer(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildIllustration() {
    return Container(
      width: double.infinity,
      height: 400,
      decoration: BoxDecoration(
        color: Color(0xFFF5F5DC), // Light beige/cream color
        borderRadius: BorderRadius.circular(20),
      ),
      child: Stack(
        children: [
          // Decorative elements
          Positioned(
            top: 20,
            left: 30,
            child: Icon(Icons.add, color: Colors.black, size: 16),
          ),
          Positioned(
            top: 60,
            right: 40,
            child: Icon(Icons.add, color: Colors.black, size: 20),
          ),
          Positioned(
            top: 100,
            left: 20,
            child: Icon(Icons.add, color: Colors.black, size: 14),
          ),
          Positioned(
            top: 150,
            right: 20,
            child: Icon(Icons.add, color: Colors.black, size: 18),
          ),
          Positioned(
            top: 200,
            left: 50,
            child: Icon(Icons.add, color: Colors.black, size: 16),
          ),
          // Hollow circles
          Positioned(
            top: 80,
            left: 60,
            child: Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: Colors.black, width: 2),
              ),
            ),
          ),
          Positioned(
            top: 120,
            right: 60,
            child: Container(
              width: 16,
              height: 16,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: Colors.black, width: 2),
              ),
            ),
          ),
          Positioned(
            top: 180,
            left: 30,
            child: Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: Colors.black, width: 2),
              ),
            ),
          ),
          // Center content - placeholder for your custom illustration
          Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // Placeholder for mobile phone with checkmark
                Container(
                  width: 120,
                  height: 200,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: Colors.grey[300]!, width: 2),
                  ),
                  child: Column(
                    children: [
                      // Orange status bar
                      Container(
                        height: 20,
                        width: double.infinity,
                        decoration: BoxDecoration(
                          color: AppColors.secondary,
                          borderRadius: BorderRadius.vertical(
                            top: Radius.circular(18),
                          ),
                        ),
                      ),
                      // Phone content
                      Expanded(
                        child: Container(
                          padding: EdgeInsets.all(20),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              // Checkmark in circles
                              Container(
                                width: 60,
                                height: 60,
                                decoration: BoxDecoration(
                                  color: AppColors.secondary,
                                  shape: BoxShape.circle,
                                ),
                                child: Center(
                                  child: Container(
                                    width: 40,
                                    height: 40,
                                    decoration: BoxDecoration(
                                      color: Colors.white,
                                      shape: BoxShape.circle,
                                    ),
                                    child: Icon(
                                      Icons.check,
                                      color: AppColors.secondary,
                                      size: 24,
                                    ),
                                  ),
                                ),
                              ),
                              SizedBox(height: 16),
                              // Placeholder lines
                              Container(
                                height: 2,
                                width: 40,
                                color: Colors.grey[300],
                              ),
                              SizedBox(height: 8),
                              Container(
                                height: 2,
                                width: 30,
                                color: Colors.grey[300],
                              ),
                              SizedBox(height: 8),
                              Container(
                                height: 2,
                                width: 35,
                                color: Colors.grey[300],
                              ),
                            ],
                          ),
                        ),
                      ),
                      // Bottom dot
                      Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: Colors.grey[400],
                          shape: BoxShape.circle,
                        ),
                      ),
                      SizedBox(height: 8),
                    ],
                  ),
                ),
                SizedBox(height: 20),
                // Success message
                Text(
                  'Success!',
                  style: AppTextStyle.headlineLarge.copyWith(
                    fontWeight: FontWeight.bold,
                    color: AppColors.secondary,
                    fontSize: 28,
                    fontFamily: 'roboto',
                  ),
                ),
                SizedBox(height: 8),
                Text(
                  'Your account has been created successfully',
                  textAlign: TextAlign.center,
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: Colors.grey[600],
                    fontSize: 16,
                    fontFamily: 'roboto',
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGoHomeButton(BuildContext context) {
    return GestureDetector(
      onTap: () => onTapGoHome(context),
      child: Container(
        width: MediaQuery.of(context).size.width * 0.7,
        height: 50,
        decoration: BoxDecoration(
          color: AppColors.primary, // Dark blue color
          borderRadius: BorderRadius.circular(0),
        ),
        child: Center(
          child: Text(
            'Go Home',
            style: AppTextStyle.buttonPrimary.copyWith(
              color: AppColors.white,
              fontSize: 16,
              fontFamily: 'roboto',
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }
}
