#!/bin/bash

# Firebase Setup Script for AI Keyboard
# This script helps automate Firebase configuration

echo "🔥 Firebase Setup for AI Keyboard"
echo "================================="

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI not found. Installing..."
    npm install -g firebase-tools
else
    echo "✅ Firebase CLI found"
fi

# Check if FlutterFire CLI is installed
if ! command -v flutterfire &> /dev/null; then
    echo "❌ FlutterFire CLI not found. Installing..."
    dart pub global activate flutterfire_cli
else
    echo "✅ FlutterFire CLI found"
fi

echo ""
echo "📋 Next Steps (Manual):"
echo "======================="
echo ""
echo "1. LOGIN TO FIREBASE:"
echo "   firebase login"
echo ""
echo "2. CREATE/SELECT PROJECT:"
echo "   firebase projects:list"
echo "   firebase use <your-project-id>"
echo ""
echo "3. CONFIGURE FLUTTER PROJECT:"
echo "   flutterfire configure"
echo "   # Select your Firebase project"
echo "   # Select Android and iOS platforms"
echo "   # Enter Android package name: com.example.ai_keyboard"
echo "   # Enter iOS bundle ID: com.example.aiKeyboard"
echo ""
echo "4. ENABLE FIREBASE SERVICES:"
echo "   - Go to Firebase Console (https://console.firebase.google.com/)"
echo "   - Select your project"
echo "   - Enable Authentication (Email/Password + Google)"
echo "   - Enable Firestore Database (Start in test mode)"
echo ""
echo "5. TEST THE SETUP:"
echo "   flutter run"
echo ""
echo "📁 Configuration Files:"
echo "======================"
echo "After running 'flutterfire configure', you should have:"
echo "- lib/firebase_options.dart (auto-generated)"
echo "- android/app/google-services.json (auto-generated)"
echo "- ios/Runner/GoogleService-Info.plist (auto-generated)"
echo ""
echo "🔐 Security Rules for Firestore:"
echo "==============================="
echo "Replace the default rules with:"
echo ""
cat << 'EOF'
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Users can read/write their own typing data
      match /typingData/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
EOF
echo ""
echo "📱 App Configuration:"
echo "==================="
echo "Android package: com.example.ai_keyboard"
echo "iOS bundle ID: com.example.aiKeyboard"
echo ""
echo "🎯 Features Implemented:"
echo "======================="
echo "✅ Firebase Authentication (Email/Password + Google)"
echo "✅ Firestore Database integration"
echo "✅ User settings sync across devices"
echo "✅ Typing data analytics"
echo "✅ Login/Signup screens"
echo "✅ Account management UI"
echo ""
echo "Run this script's commands manually, then test with: flutter run"
