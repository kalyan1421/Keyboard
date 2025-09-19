import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';

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
              Spacer(),
              Image.asset(AppAssets.loginIllustration),
              Spacer(),

              Text(
                'Log in to Kivive',
                style: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.black,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  fontFamily: 'roboto',
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
