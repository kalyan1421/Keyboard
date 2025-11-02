import 'package:ai_keyboard/screens/login/login_illustraion_screen.dart';
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
  bool _userInteracted = false;
  int _animationLoopCount = 0;

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

  void _onAnimationComplete() {
    print('_onAnimationComplete called. UserInteracted: $_userInteracted, AnimationLoopCount: $_animationLoopCount');
    
    // Don't auto-advance if user has interacted
    if (_userInteracted) return;
    
    _animationLoopCount++;
    
    // Auto-advance after 2 animation loops
    if (_animationLoopCount >= 2) {
      print('Auto-advancing to next page after $_animationLoopCount loops');
      _animationLoopCount = 0; // Reset for next page
      
      // Small delay before advancing
      Future.delayed(const Duration(milliseconds: 300), () {
        if (mounted && !_userInteracted) {
          _nextPage();
        }
      });
    }
  }

  void _markUserInteraction() {
    setState(() {
      _userInteracted = true;
    });
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
      MaterialPageRoute(builder: (context) => const LoginIllustraionScreen()),
    );
  }

  void _onPageChanged(int page) {
    setState(() {
      _currentPage = page;
      _animationLoopCount = 0; // Reset loop count for new page
      _userInteracted = false; // Reset interaction flag for new page
    });
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
              child: GestureDetector(
                onPanDown: (_) => _markUserInteraction(),
                onHorizontalDragStart: (_) => _markUserInteraction(),
                child: PageView.builder(
                  controller: _pageController,
                  onPageChanged: _onPageChanged,
                  itemCount: _pages.length,
                  itemBuilder: (context, index) {
                    return _OnboardingPage(
                      data: _pages[index],
                      onAnimationComplete: _onAnimationComplete,
                      isCurrentPage: index == _currentPage,
                    );
                  },
                ),
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

            // Swipe hint text (replaces Next button)
            Text(
              'Swipe →',
              style: AppTextStyle.bodyMedium.copyWith(
                color: const Color(0xFFFF9900),
                fontSize: 14,
                fontWeight: FontWeight.w600,
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
class _OnboardingPage extends StatefulWidget {
  final OnboardingPageData data;
  final VoidCallback onAnimationComplete;
  final bool isCurrentPage;

  const _OnboardingPage({
    required this.data,
    required this.onAnimationComplete,
    required this.isCurrentPage,
  });

  @override
  State<_OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<_OnboardingPage>
    with TickerProviderStateMixin {
  AnimationController? _lottieController;

  @override
  void dispose() {
    _lottieController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Lottie.asset(
        widget.data.animationPath,
        fit: BoxFit.contain,
        repeat: false, // We'll handle repeat manually
        onLoaded: (composition) {
          if (!mounted) return;
          
          _lottieController?.dispose();
          _lottieController = AnimationController(
            vsync: this,
            duration: composition.duration,
          );

          // Listen for animation completion
          _lottieController!.addStatusListener((status) {
            if (!mounted) return;
            
            if (status == AnimationStatus.completed) {
              print('Animation completed. IsCurrentPage: ${widget.isCurrentPage}');
              
              // Only trigger callback if this is the current page
              if (widget.isCurrentPage) {
                print('Calling onAnimationComplete callback');
                widget.onAnimationComplete();
              }
              
              // Repeat the animation
              if (mounted) {
                _lottieController!.forward(from: 0);
              }
            }
          });

          // Start the animation
          if (mounted && widget.isCurrentPage) {
            print('Starting animation for page: ${widget.data.title}');
            _lottieController!.forward();
          }
        },
        controller: _lottieController,
      ),
    );
  }

  @override
  void didUpdateWidget(_OnboardingPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    
    // Start animation if this page becomes current
    if (oldWidget.isCurrentPage != widget.isCurrentPage) {
      if (widget.isCurrentPage && _lottieController != null) {
        print('Page ${widget.data.title} became current, restarting animation');
        _lottieController!.forward(from: 0);
      }
    }
  }
}
