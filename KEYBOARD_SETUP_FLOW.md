# Keyboard Setup Flow Documentation

## Overview
The app implements a conditional navigation flow that ensures users properly set up the keyboard before accessing the main application features.

## Flow Logic

### 1. **First Install Check**
```dart
// In AuthWrapper - lib/screens/auth_wrapper.dart
Future<void> _checkFirstLaunch() async {
  final prefs = await SharedPreferences.getInstance();
  final isFirstLaunch = prefs.getBool('is_first_launch') ?? true;
  
  setState(() {
    _isFirstLaunch = isFirstLaunch;
  });
  
  if (isFirstLaunch) {
    await prefs.setBool('is_first_launch', false);
  }
}
```

### 2. **Keyboard Setup Check**
```dart
// Checks if keyboard is ADDED to system (enabled in Settings → Keyboard)
// NOT checking if it's the currently active input method
Future<void> _checkKeyboardStatus() async {
  final enabled = await platform.invokeMethod<bool>('isKeyboardEnabled') ?? false;
  
  // Only check if keyboard is in the system's enabled keyboard list
  final isSetup = enabled;
  
  setState(() {
    _isKeyboardSetup = isSetup;
  });
}
```

### 3. **Navigation Decision Tree**

```
App Launch
    │
    ├─> First Launch? (is_first_launch == true)
    │   └─> Show OnboardingView
    │       └─> After onboarding completion
    │           └─> Check keyboard status
    │
    └─> Subsequent Launch
        │
        ├─> Keyboard Setup? (_isKeyboardSetup == null)
        │   └─> Show Loading...
        │
        ├─> Keyboard Not Added to System? (_isKeyboardSetup == false)
        │   └─> Show KeyboardSetupScreen
        │       │
        │       └─> User adds keyboard in Settings → Keyboard
        │           └─> Navigate to MainScreen
        │
        └─> Keyboard Added to System (_isKeyboardSetup == true)
            └─> Check Authentication
                │
                ├─> Authenticated
                │   └─> Show MainScreen (with cloud sync)
                │
                └─> Not Authenticated
                    └─> Show MainScreen (guest mode)
```

## Screen Hierarchy

### AuthWrapper (lib/screens/auth_wrapper.dart)
- **Entry Point**: First screen after app initialization
- **Responsibilities**:
  1. Check first launch status
  2. Check keyboard setup status
  3. Route to appropriate screen
  4. Manage authentication state
  5. Start/stop cloud sync based on auth

### OnboardingView (lib/screens/onboarding/onboarding_view.dart)
- **When Shown**: First app install only
- **Purpose**: Introduce app features to new users
- **Next**: Returns to AuthWrapper which then checks keyboard setup

### KeyboardSetupScreen (lib/screens/keyboard_setup/keyboard_setup_screen.dart)
- **When Shown**: When keyboard is NOT added to system's keyboard list
- **Purpose**: Guide user through:
  1. Opening Settings → Keyboard
  2. Adding the app's keyboard to enabled keyboards list
- **Exit Condition**: Keyboard is added to system (appears in enabled keyboards list)
- **Note**: User does NOT need to switch to this keyboard immediately

### MainScreen (lib/screens/main screens/mainscreen.dart)
- **When Shown**: After keyboard setup is complete
- **Access**: Available for both authenticated and guest users
- **Features**: Full app functionality

## Key Methods

### isKeyboardEnabled()
```kotlin
// Android - checks if keyboard is in system's enabled keyboards list
// This means user has added the keyboard in Settings → Keyboard
fun isKeyboardEnabled(): Boolean {
  val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
  val enabledInputMethods = imm.enabledInputMethodList
  return enabledInputMethods.any { it.packageName == context.packageName }
}
```

**Important**: This check verifies if the keyboard has been **added to the system**, not if it's currently the active input method. Users can have multiple keyboards enabled and switch between them.

## State Persistence

### SharedPreferences Keys
- `is_first_launch`: Boolean - tracks first app launch
- `theme.v2.json`: String - stores default theme on first install
- `keyboard.theme`: String - stores active theme ID

### First Install Defaults (Bootstrapper)
```dart
// lib/services/keyboard_settings_bootstrapper.dart
final existingThemeJson = prefs.getString('theme.v2.json');
if (existingThemeJson == null || existingThemeJson.isEmpty) {
  final defaultTheme = KeyboardThemeV2.createDefault();
  await prefs.setString('theme.v2.json', jsonEncode(defaultTheme.toJson()));
  await prefs.setString('keyboard.theme', defaultTheme.id);
}
```

## User Experience Flow

### New User Journey
1. **Launch App** → See Onboarding (3 screens)
2. **Complete Onboarding** → Redirected to Keyboard Setup Screen
3. **Follow Setup Instructions**:
   - Open Settings → Keyboard
   - Add the app's keyboard to enabled keyboards list
4. **Setup Complete** → Automatically navigate to Main Screen
5. **Optional**: Login for cloud sync features
6. **Note**: User can switch to the keyboard anytime from their device's keyboard switcher

### Returning User Journey
1. **Launch App** → Check if keyboard is added to system
2. **If Keyboard Added** → Main Screen (instant access)
3. **If Keyboard Removed** → Keyboard Setup Screen (re-setup required)

## Benefits

### ✅ Seamless Onboarding
- Users guided through setup on first install
- No confusion about what to do next

### ✅ Persistent Setup Validation
- App always checks if keyboard is properly configured
- Redirects to setup if keyboard gets disabled

### ✅ Guest Mode Support
- Users can explore app without authentication
- Premium features prompt login when needed

### ✅ Intelligent Routing
- No dead-end screens
- Clear path from setup to usage

## Edge Cases Handled

1. **User removes keyboard from system after setup**
   - Next launch detects and shows setup screen again

2. **User skips adding keyboard**
   - Setup screen persists until keyboard is added to system

3. **App reinstall**
   - Treated as first install
   - Onboarding shown again

4. **User adds keyboard but doesn't switch to it**
   - App still grants access to main screen
   - User can switch to keyboard later via system keyboard switcher

5. **Hot restart during development**
   - State preserved via SharedPreferences
   - No duplicate Firebase initialization

## Testing Checklist

- [ ] First install → shows onboarding
- [ ] After onboarding → shows keyboard setup screen
- [ ] Add keyboard to system → navigates to main screen
- [ ] Access main screen without switching to keyboard → works
- [ ] Remove keyboard from system → next launch shows setup screen
- [ ] Reinstall app → treated as first install
- [ ] Login → cloud sync starts
- [ ] Logout → cloud sync stops
- [ ] Guest mode → main screen accessible

---

**Last Updated**: Current implementation as of latest commit
**Related Files**:
- `lib/main.dart` - App entry point
- `lib/screens/auth_wrapper.dart` - Navigation logic
- `lib/screens/onboarding/onboarding_view.dart` - First-time user experience
- `lib/screens/keyboard_setup/keyboard_setup_screen.dart` - Setup guidance
- `lib/services/keyboard_settings_bootstrapper.dart` - First install defaults

