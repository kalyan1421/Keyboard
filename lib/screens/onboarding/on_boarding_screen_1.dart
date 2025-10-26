import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/screens/main screens/mainscreen.dart';
import 'package:flutter/material.dart';
import 'dart:math';

import 'package:lottie/lottie.dart';

class OnBoardingScreen1 extends StatefulWidget {
  const OnBoardingScreen1({super.key});

  @override
  State<OnBoardingScreen1> createState() => _OnBoardingScreen1State();
}

class _OnBoardingScreen1State extends State<OnBoardingScreen1> {
  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    return Scaffold(
      backgroundColor: const Color(0xFF1A233B), // Dark navy blue background
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            _BuiltHeaderText(),
            SizedBox(height: 20),
            // _BuiltAnimatedIllustration(screenWidth: screenWidth),
            // INSERT_YOUR_CODE
            Expanded(
              child: Center(
                child: Lottie.asset(
                  'assets/animations/onboarding1.json',
                  fit: BoxFit.fill,
                  repeat: true,
                ),
              ),
            ),
            // Expanded(
            //   child: Center(
            //     child: SizedBox(
            //       width: screenWidth * 0.8,
            //       height: screenWidth * 0.8,
            //       child: _SimpleGifWidget(
            //         assetPath: 'assets/images/onboarding_animation.gif',
            //         fallbackWidget: _BuiltAnimatedIllustration(
            //           screenWidth: screenWidth,
            //         ),
            //       ),
            //     ),
            //   ),
            // ),
            SizedBox(height: 40),
            Padding(padding: const EdgeInsets.all(16.0), child: _BuiltFooter()),
          ],
        ),
      ),
    );
  }
}

class _BuiltFooter extends StatelessWidget {
  const _BuiltFooter();

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Welcome to Kvive',
          style: AppTextStyle.headlineLarge.copyWith(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 28,
          ),
        ),
        SizedBox(height: 16),
        Text(
          'Transform your typing with AI-powered smart suggestions, effortless corrections, and more! Ready to type smarter?',
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
                    side: BorderSide(color: const Color(0xFFFF9900), width: 1),
                  ),
                ),
                padding: WidgetStatePropertyAll(
                  EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                ),
                backgroundColor: WidgetStatePropertyAll(Colors.transparent),
                foregroundColor: WidgetStatePropertyAll(Colors.white),
              ),
              onPressed: () {
                // Navigate to main screen
                Navigator.of(context).pushReplacement(
                  MaterialPageRoute(builder: (context) => const mainscreen()),
                );
              },
              child: Text(
                'Skip',
                style: AppTextStyle.buttonPrimary.copyWith(
                  color: Colors.white,
                  fontSize: 16,
                ),
              ),
            ),
            Spacer(),
            // Page Indicators
            Row(
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: const Color(0xFFFF9900),
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
                SizedBox(width: 8),
                Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: Colors.grey.shade400,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
                SizedBox(width: 8),
                Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: Colors.grey.shade400,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ],
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
                  onTap: () {
                    // Navigate to next onboarding screen (for now, go to main screen)
                    Navigator.of(context).pushReplacement(
                      MaterialPageRoute(
                        builder: (context) => const mainscreen(),
                      ),
                    );
                  },
                  child: Center(
                    child: Icon(
                      Icons.arrow_forward_ios,
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

// Simple GIF Widget - most reliable approach
class _SimpleGifWidget extends StatelessWidget {
  const _SimpleGifWidget({
    required this.assetPath,
    required this.fallbackWidget,
  });

  final String assetPath;
  final Widget fallbackWidget;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFFF9900).withValues(alpha: 0.2),
            blurRadius: 20,
            spreadRadius: 5,
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: Image.asset(
          assetPath,
          fit: BoxFit.contain,
          gaplessPlayback: true,
          isAntiAlias: true,
          filterQuality: FilterQuality.high,
          errorBuilder: (context, error, stackTrace) {
            print('GIF Error: $error');
            return fallbackWidget;
          },
          frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
            if (frame == null) {
              return Container(
                decoration: BoxDecoration(
                  color: const Color(0xFFFF9900).withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.animation,
                        color: const Color(0xFFFF9900),
                        size: 50,
                      ),
                      SizedBox(height: 10),
                      Text(
                        'Loading animation...',
                        style: TextStyle(
                          color: const Color(0xFFFF9900),
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }
            return child;
          },
        ),
      ),
    );
  }
}

// Safe GIF Widget that handles exceptions gracefully
class _SafeGifWidget extends StatefulWidget {
  const _SafeGifWidget({required this.assetPath, required this.fallbackWidget});

  final String assetPath;
  final Widget fallbackWidget;

  @override
  State<_SafeGifWidget> createState() => _SafeGifWidgetState();
}

class _SafeGifWidgetState extends State<_SafeGifWidget> {
  bool _hasError = false;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadGif();
  }

  Future<void> _loadGif() async {
    try {
      // Try to precache the image to check if it's valid
      await precacheImage(AssetImage(widget.assetPath), context);
      if (mounted) {
        setState(() {
          _isLoading = false;
          _hasError = false;
        });
      }
    } catch (e) {
      print('GIF loading error: $e');
      if (mounted) {
        setState(() {
          _isLoading = false;
          _hasError = true;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Container(
        decoration: BoxDecoration(
          color: const Color(0xFFFF9900).withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(color: const Color(0xFFFF9900)),
              SizedBox(height: 16),
              Text(
                'Loading animation...',
                style: TextStyle(color: const Color(0xFFFF9900), fontSize: 16),
              ),
            ],
          ),
        ),
      );
    }

    if (_hasError) {
      return widget.fallbackWidget;
    }

    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFFF9900).withValues(alpha: 0.2),
            blurRadius: 20,
            spreadRadius: 5,
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: Image.asset(
          widget.assetPath,
          fit: BoxFit.contain,
          gaplessPlayback: true,
          isAntiAlias: true,
          filterQuality: FilterQuality.high,
          errorBuilder: (context, error, stackTrace) {
            print('Image.asset error: $error');
            return widget.fallbackWidget;
          },
          frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
            if (frame == null) {
              return Container(
                decoration: BoxDecoration(
                  color: const Color(0xFFFF9900).withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.animation,
                        color: const Color(0xFFFF9900),
                        size: 50,
                      ),
                      SizedBox(height: 10),
                      Text(
                        'Preparing animation...',
                        style: TextStyle(
                          color: const Color(0xFFFF9900),
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }
            return child;
          },
        ),
      ),
    );
  }
}

// Custom GIF Widget for better animation support
class _AnimatedGifWidget extends StatefulWidget {
  const _AnimatedGifWidget({
    required this.assetPath,
    required this.width,
    required this.height,
  });

  final String assetPath;
  final double width;
  final double height;

  @override
  State<_AnimatedGifWidget> createState() => _AnimatedGifWidgetState();
}

class _AnimatedGifWidgetState extends State<_AnimatedGifWidget>
    with TickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(seconds: 3),
      vsync: this,
    );
    _animation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
    _controller.repeat(reverse: true);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _animation,
      builder: (context, child) {
        return Transform.scale(
          scale: 0.95 + (_animation.value * 0.1),
          child: Container(
            width: widget.width,
            height: widget.height,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFFFF9900).withValues(alpha: 0.2),
                  blurRadius: 20,
                  spreadRadius: 5,
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(20),
              child: Image.asset(
                widget.assetPath,
                fit: BoxFit.contain,
                gaplessPlayback: true,
                isAntiAlias: true,
                filterQuality: FilterQuality.high,
                errorBuilder: (context, error, stackTrace) {
                  return Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFFFF9900).withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.image_not_supported,
                            color: const Color(0xFFFF9900),
                            size: 50,
                          ),
                          SizedBox(height: 10),
                          Text(
                            'GIF not found',
                            style: TextStyle(
                              color: const Color(0xFFFF9900),
                              fontSize: 16,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
        );
      },
    );
  }
}

class _BuiltAnimatedIllustration extends StatefulWidget {
  const _BuiltAnimatedIllustration({required this.screenWidth});

  final double screenWidth;

  @override
  State<_BuiltAnimatedIllustration> createState() =>
      _BuiltAnimatedIllustrationState();
}

class _BuiltAnimatedIllustrationState extends State<_BuiltAnimatedIllustration>
    with TickerProviderStateMixin {
  late AnimationController _rotationController;
  late AnimationController _pulseController;
  late AnimationController _orbitController;

  late Animation<double> _rotationAnimation;
  late Animation<double> _pulseAnimation;
  late Animation<double> _orbitAnimation;

  @override
  void initState() {
    super.initState();

    // Rotation animation for orbiting elements
    _rotationController = AnimationController(
      duration: const Duration(seconds: 8),
      vsync: this,
    );
    _rotationAnimation =
        Tween<double>(
          begin: 0,
          end: 2 * 3.14159, // Full rotation
        ).animate(
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

    // Orbit animation for orbiting icons
    _orbitController = AnimationController(
      duration: const Duration(seconds: 6),
      vsync: this,
    );
    _orbitAnimation = Tween<double>(
      begin: 0,
      end: 2 * 3.14159,
    ).animate(CurvedAnimation(parent: _orbitController, curve: Curves.linear));

    // Start animations
    _rotationController.repeat();
    _pulseController.repeat(reverse: true);
    _orbitController.repeat();
  }

  @override
  void dispose() {
    _rotationController.dispose();
    _pulseController.dispose();
    _orbitController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Center(
        child: SizedBox(
          width: widget.screenWidth * 0.8,
          height: widget.screenWidth * 0.8,
          child: Stack(
            alignment: Alignment.center,
            children: [
              // Central orange circle with glow effect
              AnimatedBuilder(
                animation: _pulseAnimation,
                builder: (context, child) {
                  return Transform.scale(
                    scale: _pulseAnimation.value,
                    child: Container(
                      width: 120,
                      height: 120,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: RadialGradient(
                          colors: [
                            const Color(0xFFFF9900),
                            const Color(0xFFFF9900).withValues(alpha: 0.3),
                          ],
                          center: Alignment.center,
                        ),
                        // color: const Color(0xFFFF9900),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(
                              0xFFFF9900,
                            ).withValues(alpha: 0.3),
                            blurRadius: 20,
                            spreadRadius: 5,
                          ),
                        ],
                      ),
                      child: Center(
                        child: Icon(
                          Icons.auto_awesome,
                          color: Colors.white,
                          size: 60,
                        ),
                      ),
                    ),
                  );
                },
              ),

              // Concentric rings
              AnimatedBuilder(
                animation: _rotationAnimation,
                builder: (context, child) {
                  return Transform.rotate(
                    angle: _rotationAnimation.value,
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        // Ring 1
                        Container(
                          width: 200,
                          height: 200,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: const Color(
                                0xFFFF9900,
                              ).withValues(alpha: 0.3),
                              width: 1,
                            ),
                          ),
                        ),
                        // Ring 2
                        Container(
                          width: 250,
                          height: 250,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: const Color(
                                0xFFFF9900,
                              ).withValues(alpha: 0.2),
                              width: 1,
                            ),
                          ),
                        ),
                        // Ring 3
                        Container(
                          width: 300,
                          height: 300,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: const Color(
                                0xFFFF9900,
                              ).withValues(alpha: 0.1),
                              width: 1,
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),

              // Orbiting icons
              AnimatedBuilder(
                animation: _orbitAnimation,
                builder: (context, child) {
                  return Stack(
                    alignment: Alignment.center,
                    children: [
                      // AI icon (top-right)
                      Transform.translate(
                        offset: Offset(
                          100 * cos(_orbitAnimation.value),
                          -100 * sin(_orbitAnimation.value),
                        ),
                        child: Container(
                          width: 50,
                          height: 50,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: const Color(0xFFFF9900),
                          ),
                          child: Icon(
                            Icons.psychology,
                            color: Colors.white,
                            size: 24,
                          ),
                        ),
                      ),
                      // Features icon (mid-left)
                      Transform.translate(
                        offset: Offset(
                          -120 *
                              cos(
                                _orbitAnimation.value + 2.09,
                              ), // 120 degrees offset
                          120 * sin(_orbitAnimation.value + 2.09),
                        ),
                        child: Container(
                          width: 50,
                          height: 50,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: const Color(0xFFFF9900),
                          ),
                          child: Icon(
                            Icons.auto_awesome,
                            color: Colors.white,
                            size: 24,
                          ),
                        ),
                      ),
                      // Global icon (bottom-right)
                      Transform.translate(
                        offset: Offset(
                          100 *
                              cos(
                                _orbitAnimation.value + 4.19,
                              ), // 240 degrees offset
                          100 * sin(_orbitAnimation.value + 4.19),
                        ),
                        child: Container(
                          width: 50,
                          height: 50,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: const Color(0xFFFF9900),
                          ),
                          child: Icon(
                            Icons.language,
                            color: Colors.white,
                            size: 24,
                          ),
                        ),
                      ),
                    ],
                  );
                },
              ),

              // Scattered stars
              Positioned(
                top: 20,
                left: 20,
                child: Icon(
                  Icons.star,
                  color: const Color(0xFFFF9900),
                  size: 16,
                ),
              ),
              Positioned(
                top: 40,
                right: 40,
                child: Icon(
                  Icons.star,
                  color: const Color(0xFFFF9900),
                  size: 12,
                ),
              ),
              Positioned(
                bottom: 60,
                left: 30,
                child: Icon(
                  Icons.star,
                  color: const Color(0xFFFF9900),
                  size: 14,
                ),
              ),
              Positioned(
                bottom: 30,
                right: 20,
                child: Icon(
                  Icons.star,
                  color: const Color(0xFFFF9900),
                  size: 18,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BuiltHeaderText extends StatelessWidget {
  const _BuiltHeaderText();

  @override
  Widget build(BuildContext context) {
    return Text(
      'Kvive',
      style: AppTextStyle.displaySmall.copyWith(
        color: const Color(0xFFFF9900), // Bright orange
        fontSize: 32,
        fontWeight: FontWeight.bold,
      ),
    );
  }
}
