import 'package:ai_keyboard/screens/login/login_illustraion_screen.dart';
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
  bool _isAdvancing = false;
  int _animationLoopCount = 0;

  final List<OnboardingPageData> _pages = [
    OnboardingPageData(
      title: 'Welcome to Kvīve',
      description:
          'Transform your typing with AI-powered smart suggestions, effortless corrections, and more!',
      animationPath: 'assets/animations/onboarding1.json',
    ),
    OnboardingPageData(
      title: 'Smart AI Assistance',
      description:
          'Experience intelligent autocorrect, predictive text, and personalized suggestions that learn from you.',
      animationPath: 'assets/animations/onboarding2.json',
    ),
    OnboardingPageData(
      title: 'Ai Rewriting',
      description:
          'Rewrite any sentence in your perfect style. AI refines your words for clarity, tone, and impact.',
      animationPath: 'assets/animations/onboarding3.json',
    ),
  ];

  void _onAnimationComplete() {
    if (_userInteracted || _isAdvancing) return;
    _animationLoopCount++;
    if (_animationLoopCount >= 2) {
      _animationLoopCount = 0;
      _isAdvancing = true;
      Future.delayed(const Duration(milliseconds: 500), () {
        if (mounted && !_userInteracted) _nextPage();
      });
    }
  }

  void _markUserInteraction() {
    setState(() {
      _userInteracted = true;
      _isAdvancing = false;
    });
  }

  void _nextPage() {
    if (_currentPage < _pages.length - 1) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 600),
        curve: Curves.easeInOut,
      );
    } else {
      _navigateToHome();
    }
  }

  void _navigateToHome() {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const LoginIllustraionScreen()),
    );
  }

  void _onPageChanged(int page) {
    setState(() {
      _currentPage = page;
      _animationLoopCount = 0;
      _userInteracted = false;
      _isAdvancing = false;
    });
  }

  Widget _buildHeader() => Padding(
        padding: const EdgeInsets.only(top: 40.0),
        child: Center(
          child: Text(
            'Kvīve',
            style: const TextStyle(
              color: Color(0xFFFF9900),
              fontSize: 36,
              fontWeight: FontWeight.bold,
              letterSpacing: 1.2,
            ),
          ),
        ),
      );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A233B),
      body: GestureDetector(
        onTap: _markUserInteraction,
        onPanDown: (_) => _markUserInteraction(),
        onHorizontalDragStart: (_) => _markUserInteraction(),
        child: Stack(
          children: [
            /// PageView area
            PageView.builder(
              controller: _pageController,
              onPageChanged: _onPageChanged,
              itemCount: _pages.length,
              physics: const ClampingScrollPhysics(),
              itemBuilder: (context, i) => _OnboardingPage(
                data: _pages[i],
                onAnimationComplete: _onAnimationComplete,
                isCurrentPage: i == _currentPage,
                isAdvancing: _isAdvancing,
                onNextPressed: () {
                  if (i == _pages.length - 1) {
                    _navigateToHome();
                  } else {
                    _markUserInteraction();
                    _nextPage();
                  }
                },
              ),
            ),

            /// Kvive header
            Positioned(top: 10, left: 0, right: 0, child: _buildHeader()),

            /// Footer (Skip + dots)
            Positioned(
              bottom: 40,
              left: 24,
              right: 24,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  if (_currentPage < _pages.length - 1)
                    OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        side:
                            const BorderSide(color: Color(0xFFFF9900), width: 0.5),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 25, vertical: 10),
                      ),
                      onPressed: _navigateToHome,
                      child: const Text(
                        'Skip',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    )
                  else
                    const SizedBox(width: 70),
                  Row(
                    children: List.generate(
                      _pages.length,
                      (i) => AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        width: _currentPage == i ? 20 : 8,
                        height: 8,
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        decoration: BoxDecoration(
                          color: _currentPage == i
                              ? const Color(0xFFFF9900)
                              : Colors.grey.shade600,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                    ),
                  ),
                   GestureDetector(
                  onTap: _nextPage,
                  child: Container(
                    width: 70,
                    height: 50,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(10),
                      color: const Color(0xFFFF9900),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.3),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: const Icon(
                      Icons.keyboard_arrow_right,
                      color: Colors.white,
                      size: 40,
                    ),
                  ),
                ),
              
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class OnboardingPageData {
  final String title, description, animationPath;
  const OnboardingPageData({
    required this.title,
    required this.description,
    required this.animationPath,
  });
}

class _OnboardingPage extends StatefulWidget {
  final OnboardingPageData data;
  final VoidCallback onAnimationComplete;
  final bool isCurrentPage, isAdvancing;
  final VoidCallback onNextPressed;

  const _OnboardingPage({
    required this.data,
    required this.onAnimationComplete,
    required this.isCurrentPage,
    required this.isAdvancing,
    required this.onNextPressed,
  });

  @override
  State<_OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<_OnboardingPage>
    with TickerProviderStateMixin {
  AnimationController? _controller;

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.of(context).size.height;

    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        SizedBox(
          height: screenHeight * 0.58,
          child: Stack(
            alignment: Alignment.center,
            children: [
              Lottie.asset(
                widget.data.animationPath,
                fit: BoxFit.contain,
                repeat: false,
                controller: _controller,
                onLoaded: (comp) {
                  _controller?.dispose();
                  _controller = AnimationController(
                    vsync: this,
                    duration: comp.duration,
                  )
                    ..addStatusListener((status) {
                      if (status == AnimationStatus.completed &&
                          widget.isCurrentPage) {
                        widget.onAnimationComplete();
                        Future.delayed(const Duration(milliseconds: 300), () {
                          if (mounted &&
                              widget.isCurrentPage &&
                              !widget.isAdvancing) {
                            _controller?.reset();
                            _controller?.forward();
                          }
                        });
                      }
                    });
                  if (widget.isCurrentPage) _controller!.forward();
                },
              ),

            ],
          ),
        ),
        const SizedBox(height: 20),
        Text(
          widget.data.title,
          textAlign: TextAlign.left,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 28,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Text(
            widget.data.description,
            textAlign: TextAlign.left,
            style: const TextStyle(
              color: Colors.white70,
              fontSize: 16,
              height: 1.4,
            ),
          ),
        ),
      ],
    );
  }

  @override
  void didUpdateWidget(_OnboardingPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isAdvancing && _controller != null) {
      _controller!.stop();
    } else if (widget.isCurrentPage && !oldWidget.isCurrentPage) {
      _controller?.reset();
      _controller?.forward();
    }
  }
}
