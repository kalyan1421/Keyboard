import 'package:ai_keyboard/screens/main%20screens/chat_screen.dart';
import 'package:ai_keyboard/screens/main%20screens/home_screen.dart';
import 'package:ai_keyboard/screens/main%20screens/profile_screen.dart';
import 'package:ai_keyboard/screens/main%20screens/setting_screen.dart';
import 'package:ai_keyboard/screens/main%20screens/theme_screen.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/widgets/rate_app_modal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:async';

class mainscreen extends StatefulWidget {
  const mainscreen({super.key});

  @override
  State<mainscreen> createState() => _mainscreenState();
}

class _mainscreenState extends State<mainscreen> with TickerProviderStateMixin {
  int selectedIndex = 0;
  AnimationController? _fabAnimationController;
  Animation<double>? _fabAnimation;
  Timer? _animationTimer;
  bool _isExtended = false;
  bool _hasShownRateModal = false;

  final List<Widget> _pages = [
    const HomeScreen(),
    const ThemeScreen(),
    const SettingScreen(),
    const ProfileScreen(),
  ];

  @override
  void initState() {
    super.initState();
    _fabAnimationController = AnimationController(
      duration: const Duration(milliseconds: 500),
      vsync: this,
    );
    _fabAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _fabAnimationController!,
        curve: Curves.easeInOut,
      ),
    );

    _startAnimationTimer();
    _checkAndShowRateModal();
  }

  @override
  void dispose() {
    _fabAnimationController?.dispose();
    _animationTimer?.cancel();
    super.dispose();
  }

  void _startAnimationTimer() {
    _animationTimer = Timer.periodic(const Duration(seconds: 10), (timer) {
      if (_isExtended) {
        _fabAnimationController?.reverse();
        _isExtended = false;
      } else {
        _fabAnimationController?.forward();
        _isExtended = true;
      }
    });
  }

  Future<void> _checkAndShowRateModal() async {
    if (_hasShownRateModal) return;

    final prefs = await SharedPreferences.getInstance();
    final hasShownRateModal = prefs.getBool('has_shown_rate_modal') ?? false;

    if (!hasShownRateModal) {
      // Wait for the screen to be fully loaded
      await Future.delayed(const Duration(seconds: 2));

      if (mounted) {
        _showRateModal();
        await prefs.setBool('has_shown_rate_modal', true);
        _hasShownRateModal = true;
      }
    } else {
      _hasShownRateModal = true;
    }
  }

  void _showRateModal() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return const RateAppModal();
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _pages[selectedIndex],
      floatingActionButton: _fabAnimation != null
          ? AnimatedBuilder(
              animation: _fabAnimation!,
              builder: (context, child) {
                return Container(
                  height: 56,
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 500),
                        curve: Curves.easeInOut,
                        width: 56 + (_fabAnimation!.value * 120),
                        height: 56,
                        decoration: BoxDecoration(
                          color: AppColors.secondary,
                          borderRadius: BorderRadius.circular(12),
                          boxShadow: [
                            BoxShadow(
                              color: AppColors.secondary.withOpacity(0.3),
                              spreadRadius: 2,
                              blurRadius: 8,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: Material(
                          color: Colors.transparent,
                          child: InkWell(
                            borderRadius: BorderRadius.circular(12),
                            onTap: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) => const ChatScreen(),
                                ),
                              );
                            },
                            child: Stack(
                              children: [
                                // Icon positioned at the right side (stays in place)
                                Positioned(
                                  right: 16,
                                  top: 0,
                                  bottom: 0,
                                  child: const Icon(
                                    Icons.keyboard,
                                    size: 24,
                                    color: AppColors.white,
                                  ),
                                ),
                                // Text that extends from the left
                                Positioned(
                                  left: 16,
                                  top: 0,
                                  bottom: 0,
                                  child: AnimatedOpacity(
                                    opacity: _fabAnimation!.value,
                                    duration: const Duration(milliseconds: 300),
                                    child: AnimatedContainer(
                                      duration: const Duration(
                                        milliseconds: 500,
                                      ),
                                      curve: Curves.easeInOut,
                                      width: _fabAnimation!.value * 100,
                                      child: const Center(
                                        child: Text(
                                          'Try Keyboard',
                                          style: TextStyle(
                                            color: AppColors.white,
                                            fontSize: 14,
                                            fontWeight: FontWeight.w500,
                                          ),
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              },
            )
          : FloatingActionButton(
              foregroundColor: AppColors.white,
              backgroundColor: AppColors.secondary,
              onPressed: () {
                // TODO: Add keyboard functionality
              },
              child: const Icon(Icons.keyboard, size: 32),
            ),
      bottomNavigationBar: Container(
        height: 120,
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.only(
            topLeft: Radius.circular(20),
            topRight: Radius.circular(20),
          ),
          boxShadow: [
            BoxShadow(
              color: AppColors.grey.withOpacity(0.1),
              spreadRadius: 1,
              blurRadius: 10,
            ),
            BoxShadow(
              color: AppColors.grey.withOpacity(0.1),
              spreadRadius: 1,
              blurRadius: 10,
            ),
          ],
        ),
        child: BottomNavigationBar(
          elevation: 0,
          backgroundColor: Colors.transparent,
          selectedItemColor: AppColors.secondary,
          unselectedItemColor: AppColors.grey,
          type: BottomNavigationBarType.fixed,
          currentIndex: selectedIndex,
          onTap: (index) => setState(() => this.selectedIndex = index),
          items: [
            BottomNavigationBarItem(
              icon: SvgPicture.asset(
                AppIcons.home,
                colorFilter: ColorFilter.mode(
                  selectedIndex == 0 ? AppColors.secondary : AppColors.grey,
                  BlendMode.srcIn,
                ),
              ),
              label: 'Home',
            ),
            BottomNavigationBarItem(
              icon: SvgPicture.asset(
                AppIcons.theme,
                colorFilter: ColorFilter.mode(
                  selectedIndex == 1 ? AppColors.secondary : AppColors.grey,
                  BlendMode.srcIn,
                ),
              ),
              label: 'Theme',
            ),
            BottomNavigationBarItem(
              icon: SvgPicture.asset(
                AppIcons.settings,
                colorFilter: ColorFilter.mode(
                  selectedIndex == 2 ? AppColors.secondary : AppColors.grey,
                  BlendMode.srcIn,
                ),
              ),
              label: 'Settings',
            ),
            BottomNavigationBarItem(
              icon: SvgPicture.asset(
                AppIcons.profile,
                colorFilter: ColorFilter.mode(
                  selectedIndex == 3 ? AppColors.secondary : AppColors.grey,
                  BlendMode.srcIn,
                ),
              ),
              label: 'Profile',
            ),
          ],
        ),
      ),
    );
  }
}
