import 'package:ai_keyboard/screens/login/mobile_login_screen.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/widgets/orange_button.dart';
import 'package:flutter/material.dart';

// Tap handler templates for this page
void onTapContinueWithMobile(BuildContext context) {
  Navigator.push(
    context,
    MaterialPageRoute(builder: (context) => MobileLoginScreen()),
  );
}

void onTapContinueWithGoogle(BuildContext context) {}

void onTapDoItLater(BuildContext context) {}

class LoginIllustraionScreen extends StatelessWidget {
  const LoginIllustraionScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(16.0),
          child: Column(
            children: [
              Spacer(flex: 3),
              Image.asset(AppAssets.loginIllustration),
              Spacer(),

              Text(
                'Log in to Kivive',
                style: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.black,
                  fontSize: 24,
                  fontWeight: FontWeight.w900,
                ),
              ),
              SizedBox(height: 16),
              OrangeButton(
                text: 'Continue with Mobile',
                icon: Icons.mobile_friendly_outlined,
                onTap: () => onTapContinueWithMobile(context),
              ),
              SizedBox(height: 16),
              GestureDetector(
                onTap: () => onTapContinueWithGoogle(context),
                child: Container(
                  width: double.infinity,
                  height: 50,
                  decoration: BoxDecoration(
                    color: Colors.grey.shade200,
                    borderRadius: BorderRadius.circular(100),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Image.asset(AppIcons.google, width: 24, height: 24),
                      SizedBox(width: 10),
                      Text(
                        'Continue with Google',
                        style: AppTextStyle.buttonPrimary.copyWith(
                          color: AppColors.black,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              SizedBox(height: 32),
              GestureDetector(
                onTap: () => onTapDoItLater(context),
                child: Text(
                  'I will do it later',
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.black,
                  ),
                ),
              ),
              Spacer(flex: 1),
            ],
          ),
        ),
      ),
    );
  }
}
