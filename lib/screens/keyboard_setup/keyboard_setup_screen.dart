import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:ai_keyboard/screens/main%20screens/mainscreen.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';

class KeyboardSetupScreen extends StatefulWidget {
  const KeyboardSetupScreen({super.key});

  @override
  State<KeyboardSetupScreen> createState() => _KeyboardSetupScreenState();
}

class _KeyboardSetupScreenState extends State<KeyboardSetupScreen>
    with TickerProviderStateMixin {
  bool _isStep1Completed = false;
  bool _isStep2Completed = false;
  late AnimationController _sparkleController;
  late AnimationController _keyboardController;
  late Animation<double> _sparkleAnimation;
  late Animation<double> _keyboardAnimation;

  @override
  void initState() {
    super.initState();
    _initializeAnimations();
  }

  void _initializeAnimations() {
    _sparkleController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    );
    _keyboardController = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    );

    _sparkleAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _sparkleController, curve: Curves.easeInOut),
    );

    _keyboardAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _keyboardController, curve: Curves.elasticOut),
    );

    _sparkleController.repeat(reverse: true);
    _keyboardController.forward();
  }

  @override
  void dispose() {
    _sparkleController.dispose();
    _keyboardController.dispose();
    super.dispose();
  }

  void _onStep1Tap() {
    setState(() {
      _isStep1Completed = !_isStep1Completed;
    });

    if (_isStep1Completed) {
      HapticFeedback.lightImpact();
      _showStep1Success();
    }
  }

  void _onStep2Tap() {
    if (_isStep1Completed) {
      setState(() {
        _isStep2Completed = !_isStep2Completed;
      });

      if (_isStep2Completed) {
        HapticFeedback.mediumImpact();
        _showStep2Success();
      }
    } else {
      _showStep1RequiredDialog();
    }
  }

  void _showStep1Success() {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Row(
          children: [
            Icon(Icons.check_circle, color: Colors.white),
            SizedBox(width: 8),
            Text('Emoji Keyboard enabled!'),
          ],
        ),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  void _showStep2Success() {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Row(
          children: [
            Icon(Icons.check_circle, color: Colors.white),
            SizedBox(width: 8),
            Text('Ready to switch to AI Keyboard!'),
          ],
        ),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  void _showStep1RequiredDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.info_outline, color: Colors.orange),
              SizedBox(width: 8),
              Text('Complete Step 1 First'),
            ],
          ),
          content: const Text(
            'Please enable the Emoji Keyboard in Step 1 before proceeding to Step 2.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('OK'),
            ),
          ],
        );
      },
    );
  }

  void _onContinueTap() {
    if (_isStep1Completed && _isStep2Completed) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const mainscreen()),
      );
    } else {
      _showIncompleteSetupDialog();
    }
  }

  void _showIncompleteSetupDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.warning, color: Colors.orange),
              SizedBox(width: 8),
              Text('Setup Incomplete'),
            ],
          ),
          content: const Text(
            'Please complete both steps before continuing to the main app.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('OK'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.primary,
      body: SafeArea(
        bottom: false,
        child: Stack(
          children: [
            Column(
              mainAxisAlignment: MainAxisAlignment.start,
              children: [
                Expanded(
                  child: SvgPicture.asset(AppAssets.keyboardSetupIllustration),
                ),
                Spacer(),
              ],
            ),
            Positioned(
              bottom: 0,
              left: -100,
              right: -100,
              child: Container(
                height: 450,
                width: MediaQuery.of(context).size.height,
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(300),
                    topRight: Radius.circular(300),
                  ),
                ),
                child: Column(
                  children: [
                    SizedBox(height: 80),
                    Container(
                      padding: EdgeInsets.symmetric(horizontal: 8),
                      height: 50,
                      width: MediaQuery.of(context).size.width * 0.7,
                      decoration: BoxDecoration(
                        color: AppColors.grey.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(50),
                      ),
                      child: Row(
                        children: [
                          CircleAvatar(
                            backgroundColor: AppColors.white,
                            child: Icon(Icons.check, color: AppColors.grey),
                          ),
                          SizedBox(width: 16),
                          Text(
                            'Enable Emoji Keyboard',
                            style: AppTextStyle.bodyMedium.copyWith(
                              color: AppColors.grey,
                            ),
                          ),
                        ],
                      ),
                    ),
                    SizedBox(height: 16),
                    Container(
                      padding: EdgeInsets.symmetric(horizontal: 8),
                      height: 50,
                      width: MediaQuery.of(context).size.width * 0.7,
                      decoration: BoxDecoration(
                        color: AppColors.primary,
                        borderRadius: BorderRadius.circular(50),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.1),
                            spreadRadius: 2,
                            blurRadius: 8,
                            offset: Offset(0, 2),
                          ),
                        ],
                      ),
                      child: Row(
                        children: [
                          CircleAvatar(
                            backgroundColor: AppColors.secondary,
                            child: Text(
                              '2',
                              style: AppTextStyle.bodyMedium.copyWith(
                                color: AppColors.white,
                              ),
                            ),
                          ),
                          SizedBox(width: 16),
                          Text(
                            'Switch to Emoji Keyboard',
                            style: AppTextStyle.bodyMedium.copyWith(
                              color: AppColors.white,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Spacer(),
                    Text(
                      'by installing or using the product, you agree to',
                      style: AppTextStyle.bodySmall.copyWith(
                        color: Colors.grey[600],
                        fontSize: 14,
                      ),
                    ),
                    Text.rich(
                      TextSpan(
                        children: [
                          TextSpan(
                            text: 'Privacy Policy',
                            style: TextStyle(
                              color: AppColors.secondary,
                              fontSize: 14,
                            ),
                          ),
                          TextSpan(
                            text: ' and ',
                            style: TextStyle(
                              color: Colors.grey[600],
                              fontSize: 14,
                            ),
                          ),
                          TextSpan(
                            text: 'Term of Use Agreement',
                            style: TextStyle(
                              color: AppColors.secondary,
                              fontSize: 14,
                            ),
                          ),
                        ],
                      ),
                    ),
                    SizedBox(height: 24),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTitle() {
    return Column(
      children: [
        Text(
          'Kvive Ai Keyboard',
          style: AppTextStyle.displayMedium.copyWith(
            color: AppColors.secondary,
            fontSize: 32,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Setup your AI-powered keyboard',
          style: AppTextStyle.bodyMedium.copyWith(
            color: Colors.white70,
            fontSize: 16,
          ),
        ),
      ],
    );
  }

  Widget _buildKeyboardIllustration() {
    return AnimatedBuilder(
      animation: _keyboardAnimation,
      builder: (context, child) {
        return Transform.scale(
          scale: 0.8 + (0.2 * _keyboardAnimation.value),
          child: Container(
            width: 280,
            height: 200,
            child: Stack(
              alignment: Alignment.center,
              children: [
                // Background keyboard
                _buildKeyboardBackground(),

                // AI Logo
                _buildAILogo(),

                // Sparkle effects
                ..._buildSparkleEffects(),

                // Energy lines
                ..._buildEnergyLines(),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildKeyboardBackground() {
    return Container(
      width: 200,
      height: 120,
      decoration: BoxDecoration(
        color: AppColors.secondary,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: AppColors.secondary.withOpacity(0.3),
            spreadRadius: 4,
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          const SizedBox(height: 8),
          _buildKeyboardRow(['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P']),
          const SizedBox(height: 4),
          _buildKeyboardRow(['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L']),
          const SizedBox(height: 4),
          _buildKeyboardRow(['Z', 'X', 'C', 'V', 'B', 'N', 'M']),
        ],
      ),
    );
  }

  Widget _buildKeyboardRow(List<String> keys) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: keys.map((key) => _buildKey(key)).toList(),
    );
  }

  Widget _buildKey(String letter) {
    return Container(
      width: 16,
      height: 20,
      margin: const EdgeInsets.symmetric(horizontal: 1),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(3),
      ),
      child: Center(
        child: Text(
          letter,
          style: const TextStyle(
            fontSize: 8,
            fontWeight: FontWeight.bold,
            color: Color(0xFF031D40),
          ),
        ),
      ),
    );
  }

  Widget _buildAILogo() {
    return AnimatedBuilder(
      animation: _sparkleAnimation,
      builder: (context, child) {
        return Container(
          width: 60,
          height: 60,
          decoration: BoxDecoration(
            color: Colors.white,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: AppColors.secondary.withOpacity(0.5),
                spreadRadius: 2,
                blurRadius: 8,
              ),
            ],
          ),
          child: Center(
            child: Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: AppColors.secondary,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.psychology,
                color: Colors.white,
                size: 24,
              ),
            ),
          ),
        );
      },
    );
  }

  List<Widget> _buildSparkleEffects() {
    return [
      Positioned(
        top: 20,
        left: 40,
        child: AnimatedBuilder(
          animation: _sparkleAnimation,
          builder: (context, child) {
            return Opacity(
              opacity: 0.3 + (0.4 * _sparkleAnimation.value),
              child: Icon(Icons.star, color: AppColors.secondary, size: 12),
            );
          },
        ),
      ),
      Positioned(
        top: 60,
        right: 30,
        child: AnimatedBuilder(
          animation: _sparkleAnimation,
          builder: (context, child) {
            return Opacity(
              opacity: 0.2 + (0.5 * _sparkleAnimation.value),
              child: Icon(Icons.star, color: AppColors.secondary, size: 16),
            );
          },
        ),
      ),
      Positioned(
        bottom: 30,
        left: 20,
        child: AnimatedBuilder(
          animation: _sparkleAnimation,
          builder: (context, child) {
            return Opacity(
              opacity: 0.4 + (0.3 * _sparkleAnimation.value),
              child: Icon(Icons.star, color: AppColors.secondary, size: 10),
            );
          },
        ),
      ),
    ];
  }

  List<Widget> _buildEnergyLines() {
    return List.generate(3, (index) {
      return Positioned(
        child: AnimatedBuilder(
          animation: _sparkleAnimation,
          builder: (context, child) {
            return CustomPaint(
              painter: EnergyLinePainter(
                opacity: 0.1 + (0.2 * _sparkleAnimation.value),
                angle: (index * 120) * (3.14159 / 180),
              ),
              size: const Size(280, 200),
            );
          },
        ),
      );
    });
  }

  Widget _buildSetupSteps() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [_buildStep1(), const SizedBox(height: 16), _buildStep2()],
      ),
    );
  }

  Widget _buildStep1() {
    return GestureDetector(
      onTap: _onStep1Tap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: _isStep1Completed ? AppColors.secondary : Colors.grey[300],
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.1),
              spreadRadius: 2,
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              width: 24,
              height: 24,
              decoration: BoxDecoration(
                color: _isStep1Completed ? Colors.white : Colors.grey[600],
                shape: BoxShape.circle,
              ),
              child: Icon(
                _isStep1Completed ? Icons.check : Icons.circle_outlined,
                color: _isStep1Completed
                    ? AppColors.secondary
                    : Colors.grey[400],
                size: 16,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Text(
                'Enable Emoji Keyboard',
                style: AppTextStyle.titleMedium.copyWith(
                  color: _isStep1Completed ? Colors.white : Colors.grey[700],
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStep2() {
    return GestureDetector(
      onTap: _onStep2Tap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: _isStep2Completed ? AppColors.secondary : AppColors.primary,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.1),
              spreadRadius: 2,
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              width: 24,
              height: 24,
              decoration: BoxDecoration(
                color: AppColors.secondary,
                shape: BoxShape.circle,
              ),
              child: Center(
                child: Text(
                  '2',
                  style: AppTextStyle.labelMedium.copyWith(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Text(
                'Switch to Emoji Keyboard',
                style: AppTextStyle.titleMedium.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            if (_isStep2Completed)
              const Icon(Icons.check_circle, color: Colors.white, size: 20),
          ],
        ),
      ),
    );
  }

  Widget _buildLegalFooter() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(20),
          topRight: Radius.circular(20),
        ),
      ),
      child: Column(
        children: [
          Text(
            'by installing or using the product, you agree to',
            style: AppTextStyle.bodySmall.copyWith(color: Colors.grey[600]),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              GestureDetector(
                onTap: () {
                  // Navigate to Privacy Policy
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Privacy Policy clicked')),
                  );
                },
                child: Text(
                  'Privacy Policy',
                  style: AppTextStyle.bodySmall.copyWith(
                    color: AppColors.secondary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              Text(
                ' and ',
                style: AppTextStyle.bodySmall.copyWith(color: Colors.grey[600]),
              ),
              GestureDetector(
                onTap: () {
                  // Navigate to Terms of Use
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Terms of Use clicked')),
                  );
                },
                child: Text(
                  'Term of Use Agreement',
                  style: AppTextStyle.bodySmall.copyWith(
                    color: AppColors.secondary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: ElevatedButton(
              onPressed: _onContinueTap,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                elevation: 2,
              ),
              child: Text(
                'Continue to App',
                style: AppTextStyle.buttonPrimary.copyWith(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class EnergyLinePainter extends CustomPainter {
  final double opacity;
  final double angle;

  EnergyLinePainter({required this.opacity, required this.angle});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFFFFA203).withOpacity(opacity)
      ..strokeWidth = 1.0
      ..style = PaintingStyle.stroke;

    final center = Offset(size.width / 2, size.height / 2);
    final radius = 80.0;

    for (int i = 0; i < 3; i++) {
      final startAngle = angle + (i * 2 * 3.14159 / 3);
      final endAngle = startAngle + 0.5;

      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius),
        startAngle,
        endAngle - startAngle,
        false,
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
