import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class SwitchKeyBoardScreen extends StatefulWidget {
  const SwitchKeyBoardScreen({Key? key}) : super(key: key);

  @override
  State<SwitchKeyBoardScreen> createState() => _SwitchKeyBoardScreenState();
}

class _SwitchKeyBoardScreenState extends State<SwitchKeyBoardScreen>
    with TickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 1200),
      vsync: this,
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeInOut),
    );

    _scaleAnimation = Tween<double>(begin: 0.9, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.elasticOut),
    );

    _animationController.forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [AppColors.primary, AppColors.primary.withOpacity(0.95)],
          ),
        ),
        child: Stack(
          children: [
            // Background with stars
            _buildStarBackground(),

            // Keyboard illustration
            _buildKeyboardIllustration(),

            // Main content
            AnimatedBuilder(
              animation: _animationController,
              builder: (context, child) {
                return FadeTransition(
                  opacity: _fadeAnimation,
                  child: ScaleTransition(
                    scale: _scaleAnimation,
                    child: _buildMainContent(),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStarBackground() {
    return Positioned.fill(
      child: CustomPaint(painter: StarBackgroundPainter()),
    );
  }

  Widget _buildKeyboardIllustration() {
    return Positioned(
      top: MediaQuery.of(context).size.height * 0.15,
      left: 0,
      right: 0,
      child: Center(
        child: Container(
          width: 200,
          height: 120,
          decoration: BoxDecoration(
            color: AppColors.secondary.withOpacity(0.1),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: AppColors.secondary.withOpacity(0.3),
              width: 1,
            ),
          ),
          child: Stack(
            children: [
              // Keyboard base
              Positioned.fill(
                child: Container(
                  decoration: BoxDecoration(
                    color: AppColors.secondary.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(20),
                  ),
                ),
              ),

              // OpenAI logo circle
              Center(
                child: Container(
                  width: 60,
                  height: 60,
                  decoration: BoxDecoration(
                    color: AppColors.white,
                    shape: BoxShape.circle,
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.secondary.withOpacity(0.3),
                        blurRadius: 10,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                  child: Center(
                    child: Text(
                      'AI',
                      style: GoogleFonts.paytoneOne(
                        color: AppColors.secondary,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
              ),

              // Concentric circles
              ...List.generate(3, (index) {
                return Center(
                  child: Container(
                    width: 80 + (index * 20),
                    height: 80 + (index * 20),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: AppColors.secondary.withOpacity(
                          0.2 - (index * 0.05),
                        ),
                        width: 1,
                      ),
                    ),
                  ),
                );
              }),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMainContent() {
    return Column(
      children: [
        // Top spacing
        SizedBox(height: MediaQuery.of(context).size.height * 0.35),

        // Rate App Modal
        _buildRateModal(),

        // Bottom half-circle with buttons
        Expanded(child: _buildBottomSection()),
      ],
    );
  }

  Widget _buildRateModal() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.3),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Header with app name
          Container(
            padding: const EdgeInsets.only(top: 20, bottom: 10),
            child: Column(
              children: [
                Text(
                  'Kvive Ai Keyboard',
                  style: GoogleFonts.paytoneOne(
                    color: AppColors.secondary,
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Secure & Futuristic',
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.grey,
                  ),
                ),
              ],
            ),
          ),

          // Rating section
          Container(
            margin: const EdgeInsets.all(20),
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: AppColors.lightGrey,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              children: [
                Text(
                  'Rate our App',
                  style: AppTextStyle.titleLarge.copyWith(
                    color: AppColors.black,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 20),
                _buildStarRating(),
              ],
            ),
          ),

          // Action buttons
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
            child: Column(
              children: [
                _buildRateNowButton(),
                const SizedBox(height: 12),
                _buildLaterButton(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStarRating() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(5, (index) {
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 4),
          child: Icon(Icons.star, color: AppColors.secondary, size: 32),
        );
      }),
    );
  }

  Widget _buildRateNowButton() {
    return SizedBox(
      width: double.infinity,
      height: 48,
      child: ElevatedButton(
        onPressed: _onRateNowPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.secondary,
          foregroundColor: AppColors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          elevation: 0,
        ),
        child: Text(
          'Rate Now',
          style: AppTextStyle.titleMedium.copyWith(
            color: AppColors.white,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }

  Widget _buildLaterButton() {
    return GestureDetector(
      onTap: _onLaterPressed,
      child: Text(
        'I will do it later',
        style: AppTextStyle.bodyMedium.copyWith(
          color: AppColors.grey,
          decoration: TextDecoration.underline,
        ),
      ),
    );
  }

  Widget _buildBottomSection() {
    return Stack(
      children: [
        // Half-circle background
        Positioned(
          bottom: 0,
          left: 0,
          right: 0,
          child: CustomPaint(
            size: Size(MediaQuery.of(context).size.width, 200),
            painter: HalfCirclePainter(),
          ),
        ),

        // Buttons and content
        Positioned(
          bottom: 0,
          left: 0,
          right: 0,
          child: Container(
            height: 200,
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                // Setup buttons
                _buildSetupButton(
                  icon: Icons.check_circle,
                  title: 'Enable Emoji Keyboard',
                  isEnabled: false,
                ),
                const SizedBox(height: 12),
                _buildSetupButton(
                  icon: Icons.keyboard,
                  title: 'Switch to Emoji Keyboard',
                  isEnabled: true,
                  stepNumber: 2,
                ),
                const SizedBox(height: 20),

                // Legal text
                _buildLegalText(),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSetupButton({
    required IconData icon,
    required String title,
    required bool isEnabled,
    int? stepNumber,
  }) {
    return Container(
      width: double.infinity,
      height: 50,
      decoration: BoxDecoration(
        color: isEnabled ? AppColors.primary : AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: isEnabled ? _onSetupButtonPressed : null,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                if (stepNumber != null)
                  Container(
                    width: 32,
                    height: 32,
                    decoration: BoxDecoration(
                      color: AppColors.secondary,
                      shape: BoxShape.circle,
                    ),
                    child: Center(
                      child: Text(
                        '$stepNumber',
                        style: AppTextStyle.bodyMedium.copyWith(
                          color: AppColors.white,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  )
                else
                  Icon(icon, color: AppColors.grey, size: 24),
                const SizedBox(width: 12),
                Text(
                  title,
                  style: AppTextStyle.titleMedium.copyWith(
                    color: isEnabled ? AppColors.white : AppColors.grey,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildLegalText() {
    return Column(
      children: [
        Text(
          'by installing or using the product, you agree to',
          style: AppTextStyle.bodySmall.copyWith(
            color: AppColors.white.withOpacity(0.7),
          ),
        ),
        const SizedBox(height: 4),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            GestureDetector(
              onTap: _onPrivacyPolicyPressed,
              child: Text(
                'Privacy Policy',
                style: AppTextStyle.bodySmall.copyWith(
                  color: AppColors.secondary,
                  decoration: TextDecoration.underline,
                ),
              ),
            ),
            Text(
              ' and ',
              style: AppTextStyle.bodySmall.copyWith(
                color: AppColors.white.withOpacity(0.7),
              ),
            ),
            GestureDetector(
              onTap: _onTermsPressed,
              child: Text(
                'Term of Use Agreement',
                style: AppTextStyle.bodySmall.copyWith(
                  color: AppColors.secondary,
                  decoration: TextDecoration.underline,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  void _onRateNowPressed() {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Text('Coming Soon!'),
        backgroundColor: AppColors.secondary,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  void _onLaterPressed() {
    Navigator.pop(context);
  }

  void _onSetupButtonPressed() {
    // TODO: Implement setup functionality
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Text('Setup functionality coming soon!'),
        backgroundColor: AppColors.secondary,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  void _onPrivacyPolicyPressed() {
    // TODO: Navigate to privacy policy
  }

  void _onTermsPressed() {
    // TODO: Navigate to terms of use
  }
}

class StarBackgroundPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppColors.white.withOpacity(0.3)
      ..style = PaintingStyle.fill;

    // Draw random stars
    for (int i = 0; i < 50; i++) {
      final x = (i * 37.0) % size.width;
      final y = (i * 23.0) % size.height;
      final radius = (i % 3 + 1).toDouble();

      canvas.drawCircle(Offset(x, y), radius, paint);
    }

    // Draw some larger stars
    final largeStarPaint = Paint()
      ..color = AppColors.secondary.withOpacity(0.4)
      ..style = PaintingStyle.fill;

    for (int i = 0; i < 10; i++) {
      final x = (i * 67.0) % size.width;
      final y = (i * 41.0) % size.height;
      final radius = (i % 2 + 3).toDouble();

      canvas.drawCircle(Offset(x, y), radius, largeStarPaint);
    }
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => false;
}

class HalfCirclePainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppColors.white.withOpacity(0.1)
      ..style = PaintingStyle.fill;

    final path = Path();
    path.moveTo(0, size.height);
    path.quadraticBezierTo(
      size.width / 2,
      size.height - 100,
      size.width,
      size.height,
    );
    path.lineTo(size.width, size.height);
    path.lineTo(0, size.height);
    path.close();

    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => false;
}
