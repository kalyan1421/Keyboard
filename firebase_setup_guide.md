# Firebase Setup Guide for AI Keyboard

## Manual Firebase Console Setup (Required)

Since Firebase CLI login requires interactive mode, please follow these steps manually:

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or "Add project"
3. Enter project name: `ai-keyboard` (or your preferred name)
4. Enable Google Analytics (optional)
5. Create project

### 2. Add Android App
1. In your Firebase project, click "Add app" → Android icon
2. Android package name: `com.example.ai_keyboard`
3. App nickname: `AI Keyboard Android`
4. Debug signing certificate SHA-1: (optional for now)
5. Download `google-services.json`
6. Save it to: `android/app/google-services.json`

### 3. Add iOS App
1. In your Firebase project, click "Add app" → iOS icon
2. iOS bundle ID: `com.example.aiKeyboard`
3. App nickname: `AI Keyboard iOS`
4. Download `GoogleService-Info.plist`
5. Save it to: `ios/Runner/GoogleService-Info.plist`

### 4. Enable Authentication
1. In Firebase Console, go to "Authentication"
2. Click "Get started"
3. Go to "Sign-in method" tab
4. Enable "Email/Password"
5. Optionally enable "Google" sign-in

### 5. Enable Firestore
1. In Firebase Console, go to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (for development)
4. Select a location (choose closest to your users)

## After Manual Setup

Once you've completed the manual steps above, run:

```bash
# Add Firebase CLI configuration
firebase init

# Select the following:
# - Firestore: Configure rules and indexes
# - Functions: Configure a Cloud Functions directory (optional)
# - Storage: Configure a security rules file for Cloud Storage (optional)

# Select existing project and choose your created project
```

## Configuration Files to Create

The following files need to be created/updated after downloading from Firebase Console:

1. `android/app/google-services.json` (from step 2)
2. `ios/Runner/GoogleService-Info.plist` (from step 3)

## Next Steps

After completing the Firebase Console setup:
1. Place the downloaded configuration files in the correct locations
2. Run the authentication implementation that has been prepared
3. Test the email/password authentication flow
