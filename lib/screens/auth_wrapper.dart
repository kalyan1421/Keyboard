import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/firebase_auth_service.dart';
import 'welcome_screen.dart';
import '../main.dart';

class AuthWrapper extends StatefulWidget {
  const AuthWrapper({super.key});

  @override
  State<AuthWrapper> createState() => _AuthWrapperState();
}

class _AuthWrapperState extends State<AuthWrapper> {
  final FirebaseAuthService _authService = FirebaseAuthService();
  bool? _isFirstLaunch;

  @override
  void initState() {
    super.initState();
    _checkFirstLaunch();
  }

  Future<void> _checkFirstLaunch() async {
    print('🔵 [AuthWrapper] Checking if this is first app launch...');
    
    final prefs = await SharedPreferences.getInstance();
    final isFirstLaunch = prefs.getBool('is_first_launch') ?? true;
    
    print('🔵 [AuthWrapper] First launch: $isFirstLaunch');
    
    setState(() {
      _isFirstLaunch = isFirstLaunch;
    });

    // Mark that the app has been launched
    if (isFirstLaunch) {
      await prefs.setBool('is_first_launch', false);
      print('🔵 [AuthWrapper] Marked first launch as complete');
    }
  }

  @override
  Widget build(BuildContext context) {
    // Show loading while checking first launch status
    if (_isFirstLaunch == null) {
      return const Scaffold(
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(),
              SizedBox(height: 16),
              Text('Loading...'),
            ],
          ),
        ),
      );
    }

    // If it's first launch, show welcome screen
    if (_isFirstLaunch!) {
      print('🔵 [AuthWrapper] Showing welcome screen for first launch');
      return const WelcomeScreen();
    }

    // For subsequent launches, check authentication status
    return StreamBuilder<User?>(
      stream: _authService.authStateChanges,
      builder: (context, snapshot) {
        print('🔵 [AuthWrapper] Auth state changed - User: ${snapshot.data?.email ?? 'Not signed in'}');

        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(
            body: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text('Checking authentication...'),
                ],
              ),
            ),
          );
        }

        if (snapshot.hasData && snapshot.data != null) {
          // User is signed in, go to home screen
          print('🟢 [AuthWrapper] User authenticated, showing home screen');
          return const KeyboardConfigScreen();
        } else {
          // User is not signed in, show welcome screen
          print('🟡 [AuthWrapper] User not authenticated, showing welcome screen');
          return const WelcomeScreen();
        }
      },
    );
  }
}