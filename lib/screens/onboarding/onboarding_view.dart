import 'package:ai_keyboard/screens/main%20screens/mainscreen.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';

class OnboardingView extends StatefulWidget {
  const OnboardingView({super.key});

  @override
  State<OnboardingView> createState() => _OnboardingViewState();
}

class _OnboardingViewState extends State<OnboardingView> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  final List<OnboardingPageData> _pages = [
    OnboardingPageData(
      title: 'Welcome to Kvive',
      description:
          'Transform your typing with AI-powered smart suggestions, effortless corrections, and more! Ready to type smarter?',
      animationPath: 'assets/animations/onboarding1.json',
    ),
    OnboardingPageData(
      title: 'Smart AI Assistance',
      description:
          'Experience intelligent autocorrect, predictive text, and personalized suggestions that learn from your typing style.',
      animationPath: 'assets/animations/onboarding2.json',
    ),
    OnboardingPageData(
      title: 'Customize Your Experience',
      description:
          'Choose from beautiful themes, adjust settings, and make the keyboard truly yours. Let\'s get started!',
      animationPath: 'assets/animations/onboarding3.json',
    ),
  ];

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _nextPage() {
    if (_currentPage < _pages.length - 1) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    } else {
      _navigateToHome();
    }
  }

  void _navigateToHome() {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (context) => const mainscreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A233B),
      body: SafeArea(
        child: Column(
          children: [
            // Common Header
            _buildHeader(),
            const SizedBox(height: 20),

            // PageView with all onboarding pages
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                onPageChanged: (int page) {
                  setState(() {
                    _currentPage = page;
                  });
                },
                itemCount: _pages.length,
                itemBuilder: (context, index) {
                  return _OnboardingPage(data: _pages[index]);
                },
              ),
            ),

            const SizedBox(height: 40),

            // Common Footer with navigation buttons
            Padding(padding: const EdgeInsets.all(16.0), child: _buildFooter()),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.only(top: 16.0),
      child: Text(
        'Kvive',
        style: AppTextStyle.displaySmall.copyWith(
          color: const Color(0xFFFF9900),
          fontSize: 32,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildFooter() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          _pages[_currentPage].title,
          style: AppTextStyle.headlineLarge.copyWith(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 28,
          ),
        ),
        const SizedBox(height: 16),
        Text(
          _pages[_currentPage].description,
          style: AppTextStyle.bodyMedium.copyWith(
            color: Colors.white,
            fontSize: 16,
            fontWeight: FontWeight.w400,
            height: 1.4,
          ),
        ),
        const SizedBox(height: 40),
        Row(
          children: [
            // Skip Button
            OutlinedButton(
              style: ButtonStyle(
                shape: WidgetStatePropertyAll(
                  RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                    side: const BorderSide(color: Color(0xFFFF9900), width: 1),
                  ),
                ),
                padding: const WidgetStatePropertyAll(
                  EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                ),
                backgroundColor: const WidgetStatePropertyAll(
                  Colors.transparent,
                ),
                foregroundColor: const WidgetStatePropertyAll(Colors.white),
              ),
              onPressed: _navigateToHome,
              child: Text(
                'Skip',
                style: AppTextStyle.buttonPrimary.copyWith(
                  color: Colors.white,
                  fontSize: 16,
                ),
              ),
            ),
            const Spacer(),

            // Page Indicators
            Row(
              children: List.generate(
                _pages.length,
                (index) => Container(
                  width: 8,
                  height: 8,
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  decoration: BoxDecoration(
                    color: _currentPage == index
                        ? const Color(0xFFFF9900)
                        : Colors.grey.shade400,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ),
            ),
            const Spacer(),

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
                      _currentPage == _pages.length - 1
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

// Onboarding page data model
class OnboardingPageData {
  final String title;
  final String description;
  final String animationPath;

  OnboardingPageData({
    required this.title,
    required this.description,
    required this.animationPath,
  });
}

// Individual onboarding page widget
class _OnboardingPage extends StatelessWidget {
  final OnboardingPageData data;

  const _OnboardingPage({required this.data});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Lottie.asset(
        data.animationPath,
        fit: BoxFit.contain,
        repeat: true,
      ),
    );
  }
}
