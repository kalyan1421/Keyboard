import 'package:flutter/material.dart';
import '../services/firebase_auth_service.dart';
import 'login_screen.dart';
import 'signup_screen.dart';

class WelcomeScreen extends StatefulWidget {
  const WelcomeScreen({super.key});

  @override
  State<WelcomeScreen> createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  final FirebaseAuthService _authService = FirebaseAuthService();
  bool _isLoading = false;

  Future<void> _signInWithGoogle() async {
    setState(() {
      _isLoading = true;
    });

    try {
      print('🔵 [WelcomeScreen] Initiating Google Sign-In...');
      final userCredential = await _authService.signInWithGoogle();
      
      if (userCredential == null) {
        // User cancelled the sign-in
        print('🟡 [WelcomeScreen] User cancelled Google Sign-In');
        return;
      }
      
      print('🟢 [WelcomeScreen] Google Sign-In successful');
      if (mounted) {
        final isNewUser = userCredential.additionalUserInfo?.isNewUser ?? false;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(isNewUser 
              ? 'Welcome ${userCredential.user?.displayName ?? 'User'}! Account created successfully.'
              : 'Welcome back ${userCredential.user?.displayName ?? 'User'}!'),
            backgroundColor: Colors.green,
          ),
        );
        // Navigation is handled automatically by AuthWrapper
        // No need to manually navigate
      }
    } catch (e) {
      print('🔴 [WelcomeScreen] Google Sign-In error: $e');
      if (mounted) {
        String errorMessage = e.toString();
        // Clean up error message for better user experience
        if (errorMessage.startsWith('Exception: ')) {
          errorMessage = errorMessage.substring(11);
        }
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(errorMessage),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 4),
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Widget _buildGoogleSignInButton() {
    return Container(
      width: double.infinity,
      height: 56,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.withOpacity(0.2)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            spreadRadius: 0,
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: _isLoading ? null : _signInWithGoogle,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (_isLoading)
                  const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                else ...[
                  Image.asset(
                    'assets/icons/google.png',
                    height: 24,
                    width: 24,
                  ),
                  const SizedBox(width: 12),
                  const Text(
                    'Continue with Google',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Colors.black87,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildActionButton({
    required String text,
    required VoidCallback onPressed,
    required Color backgroundColor,
    required Color textColor,
    bool isOutlined = false,
  }) {
    return Container(
      width: double.infinity,
      height: 56,
      decoration: BoxDecoration(
        color: isOutlined ? Colors.transparent : backgroundColor,
        borderRadius: BorderRadius.circular(16),
        border: isOutlined ? Border.all(color: backgroundColor, width: 2) : null,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: onPressed,
          child: Center(
            child: Text(
              text,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: isOutlined ? backgroundColor : textColor,
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              
              // App branding
              const Icon(
                Icons.keyboard,
                size: 80,
                color: Colors.blue,
              ),
              const SizedBox(height: 24),
              
              const Text(
                'AI Keyboard',
                style: TextStyle(
                  fontSize: 36,
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              
              const Text(
                'Sync your keyboard settings across all devices',
                style: TextStyle(
                  fontSize: 16,
                  color: Colors.grey,
                  height: 1.4,
                ),
                textAlign: TextAlign.center,
              ),
              
              const Spacer(),
              
              // Google Sign-In (primary option)
              _buildGoogleSignInButton(),
              const SizedBox(height: 16),
              
              // Divider
              const Row(
                children: [
                  Expanded(child: Divider()),
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16),
                    child: Text(
                      'OR',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  Expanded(child: Divider()),
                ],
              ),
              const SizedBox(height: 16),
              
              // Create account button
              _buildActionButton(
                text: 'Create Account',
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => const SignUpScreen(),
                    ),
                  );
                },
                backgroundColor: Colors.blue,
                textColor: Colors.white,
              ),
              const SizedBox(height: 12),
              
              // Sign in button
              _buildActionButton(
                text: 'Sign In',
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => const LoginScreen(),
                    ),
                  );
                },
                backgroundColor: Colors.blue,
                textColor: Colors.blue,
                isOutlined: true,
              ),
              
              const SizedBox(height: 48),
            ],
          ),
        ),
      ),
    );
  }
}
