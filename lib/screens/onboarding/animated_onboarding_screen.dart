import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/screens/login/login_illustraion_screen.dart';
import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';
import 'dart:math';

class AnimatedOnboardingScreen extends StatefulWidget {
  const AnimatedOnboardingScreen({super.key});

  @override
  State<AnimatedOnboardingScreen> createState() =>
      _AnimatedOnboardingScreenState();
}

class _AnimatedOnboardingScreenState extends State<AnimatedOnboardingScreen>
    with TickerProviderStateMixin {
  late PageController _pageController;
  late AnimationController _transitionController;
  late AnimationController _rotationController;
  late AnimationController _pulseController;

  late Animation<double> _transitionAnimation;
  late Animation<double> _rotationAnimation;
  late Animation<double> _pulseAnimation;

  int _currentPage = 0;
  final int _totalPages = 3;

  // Onboarding data
  final List<OnboardingData> _onboardingData = [
    OnboardingData(
      title: 'Welcome to Kvive',
      description:
          'Transform your typing with AI-powered smart suggestions, effortless corrections, and more! Ready to type smarter?',
      centerIcon: AppIcons.chatgpt_icon,
      centerIconLabel: 'AI Features',
    ),
    OnboardingData(
      title: 'Kvive Grammar Expert',
      description:
          'Need help composing a message or idea? With ChatGPT built right into your keyboard, you can get AI-powered suggestions  & draft responses',
      centerIcon: AppIcons.spell_check_icon,
      centerIconLabel: 'Autocorrect',
    ),
    OnboardingData(
      title: 'Ai Rewriting',
      description:
          'Need help composing a message or idea? With ChatGPT built right into your keyboard, you can get AI-powered suggestions  & draft responses',
      centerIcon: AppIcons.sparkle_icon,
      centerIconLabel: 'Multilingual',
    ),
  ];

  @override
  void initState() {
    super.initState();

    _pageController = PageController();

    // Transition animation for icon swapping
    _transitionController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _transitionAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _transitionController, curve: Curves.easeInOut),
    );

    // Rotation animation for orbiting elements
    _rotationController = AnimationController(
      duration: const Duration(seconds: 8),
      vsync: this,
    );
    _rotationAnimation = Tween<double>(begin: 0, end: 2 * pi).animate(
      CurvedAnimation(parent: _rotationController, curve: Curves.linear),
    );

    // Pulse animation for central circle
    _pulseController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    );
    _pulseAnimation = Tween<double>(begin: 0.95, end: 1.05).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );

    // Start animations
    _rotationController.repeat();
    _pulseController.repeat(reverse: true);
  }

  @override
  void dispose() {
    _pageController.dispose();
    _transitionController.dispose();
    _rotationController.dispose();
    _pulseController.dispose();
    super.dispose();
  }

  void _nextPage() {
    if (_currentPage < _totalPages - 1) {
      // Start transition animation
      _transitionController.forward(from: 0).then((_) {
        setState(() {
          _currentPage++;
        });
        _transitionController.reset();
      });
    } else {
      // Navigate to login screen after onboarding
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (context) => const LoginIllustraionScreen()),
      );
    }
  }

  void _skipToEnd() {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (context) => const LoginIllustraionScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;

    return Scaffold(
      backgroundColor: const Color(0xFF1A233B),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              _buildHeaderText(),
              SizedBox(height: 20),
              Expanded(
                child: Container(
                  clipBehavior: Clip.none,
                  width: screenWidth,
                  height: screenWidth,
                  decoration: _currentPage == 0 ? null : BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: RadialGradient(
                      // radius: 0.5,
                      colors: [Color(0xffFFA203), Color(0x00333333)],
                      center: Alignment.center,
                    ),
                  ),
                  child: _buildAnimatedIllustration(screenWidth: screenWidth),
                ),
              ),
              // Expanded(
              //   child: _buildAnimatedIllustration(screenWidth: screenWidth),
              // ),
              SizedBox(height: 40),
              _buildFooter(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeaderText() {
    return Text(
      'Kvive',
      style: AppTextStyle.displaySmall.copyWith(
        color: const Color(0xFFFF9900),
        fontSize: 32,
        fontWeight: FontWeight.bold,
      ),
    );
  }

  Widget _buildAnimatedIllustration({required double screenWidth}) {
    // For the first screen, use Lottie animation
    if (_currentPage == 0) {
      return Center(
        child: SizedBox(
          width: screenWidth * 0.8,
          height: screenWidth * 0.8,
          child: Lottie.asset(
            'assets/onboarding/Scene-1.json',
            fit: BoxFit.contain,
            repeat: true,
            animate: true,
          ),
        ),
      );
    }
    
    // For other screens, use existing animated illustration
    return Center(
      child: SizedBox(
        width: screenWidth * 0.8,
        height: screenWidth * 0.8,
        child: AnimatedBuilder(
          animation: Listenable.merge([
            _transitionAnimation,
            _rotationAnimation,
            _pulseAnimation,
          ]),
          builder: (context, child) {
            return Stack(
              alignment: Alignment.center,
              children: [
                // Concentric rings
                _buildConcentricRings(),

                // Scattered stars
                _buildScatteredStars(),
                // Orbiting icons
                _buildOrbitingIcons(screenWidth: screenWidth),

                // Central orb with current page icon
                _buildCentralOrb(),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildConcentricRings() {
    return Stack(
      alignment: Alignment.center,
      children: [
        // Ring 1 - Inner orbit
        Container(
          width: 172,
          height: 172,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: AppColors.primary, width: 1),
          ),
        ),
        // Ring 2 - Outer orbit
        Container(
          width: 215,
          height: 215,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: AppColors.primary, width: 1),
          ),
        ),
        // Ring 3 - Extended orbit
        Container(
          width: 255,
          height: 255,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: AppColors.primary, width: 1),
          ),
        ),
        // Ring 4 - Further orbit
        Container(
          width: 288,
          height: 288,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: AppColors.primary, width: 1),
          ),
        ),
        // Ring 5 - Outermost orbit
        Container(
          width: 328,
          height: 328,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: AppColors.primary, width: 1),
          ),
        ),
      ],
    );
  }

  Widget _buildOrbitingIcons({required double screenWidth}) {
    final currentData = _onboardingData[_currentPage];
    final orbitingIcons = _onboardingData
        .where((data) => data != currentData)
        .toList();

    return Stack(
      alignment: Alignment.center,
      children: [
        // AI icon (top position around center)
        if (orbitingIcons.isNotEmpty)
          Positioned(
            top: 30, // Distance from center
            right: 30,
            child: Center(child: _buildOrbitingIcon(orbitingIcons[0])),
          ),

        // Features icon (bottom-left position around center)
        if (orbitingIcons.length > 1)
          Positioned(
            top: 80,
            left: 20,
            child: _buildOrbitingIcon(orbitingIcons[1]),
          ),

        // Global icon (bottom-right position around center)
        if (orbitingIcons.length > 2)
          Positioned(
            bottom: 50,
            right: 50,
            child: _buildOrbitingIcon(orbitingIcons[2]),
          ),
        // Decorative icon (top-left) - doesn't correspond to any page
        // Transform.translate(
        //   offset: Offset(
        //     -100 * cos(_rotationAnimation.value + 1.05), // 60 degrees offset
        //     -100 * sin(_rotationAnimation.value + 1.05),
        //   ),
        //   child: _buildDecorativeOrbitingIcon(),
        // ),
        // Another decorative icon (top-left position around center)
        Positioned(
          bottom: 40,
          right: 60,
          child: _buildDecorativeOrbitingIcon2(),
        ),
      ],
    );
  }

  Widget _buildOrbitingIcon(OnboardingData data) {
    return Container(
      padding: EdgeInsets.all(16),
      width: 70,
      height: 70,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.secondary,
      ),
      child: Image.asset(
        data.centerIcon,
        color: Colors.white,
        width: 16,
        height: 16,
      ),
    );
  }

  Widget _buildDecorativeOrbitingIcon() {
    return Container(
      padding: EdgeInsets.all(10),
      width: 50,
      height: 50,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.secondary,
      ),
      child: Icon(Icons.star, color: Colors.white, size: 16),
    );
  }

  Widget _buildDecorativeOrbitingIcon2() {
    return Container(
      padding: EdgeInsets.all(10),
      width: 50,
      height: 50,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.secondary,
      ),
      child: Image.asset(AppIcons.global_icon, color: Colors.white),
    );
  }

  Widget _buildCentralOrb() {
    final currentData = _onboardingData[_currentPage];

    return Container(
      width: 100,
      height: 100,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.secondary,
        // boxShadow: [
        //   BoxShadow(
        //     color: const Color(0xFFFF9900).withValues(alpha: 0.3),
        //     blurRadius: 20,
        //     spreadRadius: 5,
        //   ),
        // ],
      ),
      child: Center(
        child: Image.asset(
          currentData.centerIcon,
          color: Colors.white,
          width: 60,
          height: 60,
        ),
      ),
    );
  }

  Widget _buildScatteredStars() {
    return Stack(
      children: [
        Positioned(
          top: 30,
          left: 30,
          child: Icon(
            Icons.star_rate_rounded,
            color: const Color(0xFFFF9900),
            size: 24,
          ),
        ),
        Positioned(
          top: 40,
          right: 40,
          child: Icon(
            Icons.star_rate_rounded,
            color: const Color(0xFFFF9900),
            size: 12,
          ),
        ),
        Positioned(
          bottom: 60,
          left: 30,
          child: Icon(
            Icons.star_rate_rounded,
            color: const Color(0xFFFF9900),
            size: 32,
          ),
        ),
        Positioned(
          bottom: 30,
          right: 20,
          child: Icon(
            Icons.star_rate_rounded,
            color: const Color(0xFFFF9900),
            size: 18,
          ),
        ),
      ],
    );
  }

  Widget _buildFooter() {
    final currentData = _onboardingData[_currentPage];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          currentData.title,
          style: AppTextStyle.headlineLarge.copyWith(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 28,
          ),
        ),
        SizedBox(height: 16),
        Text(
          currentData.description,
          style: AppTextStyle.bodyMedium.copyWith(
            color: Colors.white,
            fontSize: 16,
            fontWeight: FontWeight.w400,
            height: 1.4,
          ),
        ),
        SizedBox(height: 40),
        Row(
          children: [
            // Skip Button
            OutlinedButton(
              style: ButtonStyle(
                shape: WidgetStatePropertyAll(
                  RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                    side: BorderSide(color: AppColors.secondary, width: 1.2),
                  ),
                ),
                padding: WidgetStatePropertyAll(
                  EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                ),
                backgroundColor: WidgetStatePropertyAll(Colors.transparent),
                foregroundColor: WidgetStatePropertyAll(Colors.white),
              ),
              onPressed: _skipToEnd,
              child: Text(
                'Skip',
                style: AppTextStyle.buttonPrimary.copyWith(
                  color: AppColors.secondary,
                  fontSize: 16,
                ),
              ),
            ),
            Spacer(),
            // Page Indicators
            Row(
              children: List.generate(_totalPages, (index) {
                return Container(
                  margin: EdgeInsets.symmetric(horizontal: 2),
                  width: 8,
                  height: 4,
                  decoration: BoxDecoration(
                    color: index == _currentPage
                        ? const Color(0xFFFF9900)
                        : Colors.grey.shade400,
                    borderRadius: BorderRadius.circular(4),
                  ),
                );
              }),
            ),
            Spacer(),
            // Next Button
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: const Color(0xFFFF9900),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Material(
                color: Colors.transparent,
                child: InkWell(
                  borderRadius: BorderRadius.circular(12),
                  onTap: _nextPage,
                  child: Center(
                    child: Icon(
                      _currentPage == _totalPages - 1
                          ? Icons.check
                          : Icons.arrow_forward_ios,
                      color: Colors.white,
                      size: 20,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class OnboardingData {
  final String title;
  final String description;
  final String centerIcon;
  final String centerIconLabel;

  OnboardingData({
    required this.title,
    required this.description,
    required this.centerIcon,
    required this.centerIconLabel,
  });
}
