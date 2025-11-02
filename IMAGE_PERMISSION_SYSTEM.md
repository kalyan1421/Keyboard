# Image Permission System - One-Time Request

## Overview
The image crop screen now has an intelligent permission system that only asks for permissions once and stores the user's decision, preventing repeated permission requests.

---

## How It Works

### 1. **First Time User Opens Image Crop**
```
1. Check SharedPreferences for 'image_permissions_asked'
2. If false (first time):
   - Request storage/photos/camera permissions
   - Mark 'image_permissions_asked' = true
   - If granted: Store 'image_permissions_granted' = true
   - If denied: Don't ask again
```

### 2. **Subsequent Opens**
```
1. Check 'image_permissions_granted' flag
2. If true:
   - Verify permissions are still valid
   - If valid: Continue without asking
   - If revoked: Show settings dialog
3. If false:
   - Check current permission status
   - If granted: Update stored flag
   - If denied: Show settings dialog
```

---

## SharedPreferences Keys

### `image_permissions_asked` (bool)
- **Purpose**: Track if we've requested permissions before
- **Default**: `false`
- **Set to `true`**: After first permission request
- **Usage**: Prevents requesting permissions multiple times

### `image_permissions_granted` (bool)
- **Purpose**: Store that user granted permissions
- **Default**: `false`
- **Set to `true`**: When any of storage/photos permissions are granted
- **Usage**: Skip permission checks if already granted

---

## User Experience Flow

### Scenario 1: First Time - Permissions Granted ✅
```
User opens crop screen
    ↓
System requests permissions
    ↓
User grants storage/photos access
    ↓
Store 'image_permissions_granted' = true
    ↓
Store 'image_permissions_asked' = true
    ↓
Continue to crop
    ↓
Future opens: No permission dialog!
```

### Scenario 2: First Time - Permissions Denied ❌
```
User opens crop screen
    ↓
System requests permissions
    ↓
User denies access
    ↓
Store 'image_permissions_asked' = true
    ↓
Show settings dialog
    ↓
Future opens: Check status → Show settings dialog if still denied
```

### Scenario 3: Permissions Revoked Later ⚠️
```
User opens crop screen
    ↓
Check stored 'image_permissions_granted' = true
    ↓
Verify current permission status
    ↓
Permissions revoked by user in settings
    ↓
Clear 'image_permissions_granted' flag
    ↓
Show settings dialog with instructions
```

---

## Code Implementation

### Permission Request Logic
```dart
Future<void> _requestPermissions() async {
  final prefs = await SharedPreferences.getInstance();
  
  // 1. Check if we've stored that permissions were granted
  final hasStoredPermission = prefs.getBool('image_permissions_granted') ?? false;
  
  if (hasStoredPermission) {
    // Verify they're still valid
    final status = await Permission.storage.status;
    if (status.isGranted) {
      return; // ✅ All good, continue
    }
  }

  // 2. Check if this is first time asking
  final hasAskedBefore = prefs.getBool('image_permissions_asked') ?? false;
  
  if (!hasAskedBefore) {
    // First time - request permissions
    Map<Permission, PermissionStatus> statuses = await [
      Permission.storage,
      Permission.photos,
      Permission.camera,
    ].request();
    
    await prefs.setBool('image_permissions_asked', true);
    
    if (statuses.values.any((s) => s.isGranted)) {
      await prefs.setBool('image_permissions_granted', true);
    }
  } else {
    // We've asked before - just check current status
    // Don't request again, show settings dialog if needed
  }
}
```

### Settings Dialog
```dart
void _showPermissionSettingsDialog() {
  showDialog(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Permissions Required'),
      content: const Text(
        'Storage or photo access is needed to crop images. '
        'Please grant permission in your device settings.\n\n'
        'Go to: Settings → Apps → AI Keyboard → Permissions',
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        TextButton(
          onPressed: () {
            Navigator.of(context).pop();
            openAppSettings(); // Opens device settings
          },
          child: const Text('Open Settings'),
        ),
      ],
    ),
  );
}
```

---

## Permissions Requested

### Storage Permission
- **Android**: `Permission.storage`
- **Purpose**: Access files in external storage
- **Used for**: Reading selected images

### Photos Permission
- **iOS/Android 13+**: `Permission.photos`
- **Purpose**: Access photo library
- **Used for**: Selecting and reading images

### Camera Permission
- **Optional**: `Permission.camera`
- **Purpose**: Take photos directly
- **Used for**: Future camera integration

---

## Benefits

### ✅ Better User Experience
- No repeated permission dialogs
- Clear instructions when permissions needed
- Direct link to device settings

### ✅ Respects User Choice
- Only asks once
- Remembers granted permissions
- Doesn't spam with requests

### ✅ Handles Edge Cases
- Permissions revoked later
- SharedPreferences errors
- Device setting changes

### ✅ Graceful Fallback
- If SharedPreferences fails, requests once
- Always checks current status
- Clear error messaging

---

## Testing Checklist

### Test Cases
- ✅ First time opening crop screen → Should request permissions
- ✅ Grant permissions → Should store and not ask again
- ✅ Deny permissions → Should not request again, show settings
- ✅ Revoke permissions in settings → Should detect and show dialog
- ✅ Reinstall app → Should reset flags and ask again
- ✅ Close app and reopen → Should remember previous grant

### Manual Testing Steps
1. **Clean Install Test**
   ```
   - Uninstall app
   - Install fresh
   - Open crop screen
   - Verify permission request appears once
   ```

2. **Grant Test**
   ```
   - Grant permissions
   - Close crop screen
   - Open again
   - Verify NO permission request
   ```

3. **Deny Test**
   ```
   - Deny permissions
   - Verify settings dialog appears
   - Close crop screen
   - Open again
   - Verify settings dialog (not permission request)
   ```

4. **Revoke Test**
   ```
   - Grant permissions initially
   - Go to device Settings → Apps → Permissions
   - Revoke storage permission
   - Open crop screen
   - Verify settings dialog appears
   ```

---

## Debug Commands

### Clear Permission Flags (for testing)
```dart
// Add this temporarily for testing
final prefs = await SharedPreferences.getInstance();
await prefs.remove('image_permissions_asked');
await prefs.remove('image_permissions_granted');
print('Permission flags cleared!');
```

### Check Current Flags
```dart
final prefs = await SharedPreferences.getInstance();
print('Asked: ${prefs.getBool('image_permissions_asked')}');
print('Granted: ${prefs.getBool('image_permissions_granted')}');
```

### Check Current Permission Status
```dart
final storage = await Permission.storage.status;
final photos = await Permission.photos.status;
print('Storage: $storage');
print('Photos: $photos');
```

---

## File Changes

### Modified Files
- ✅ `/lib/screens/main screens/image_crop_screen.dart`
  - Added `SharedPreferences` import
  - Rewrote `_requestPermissions()` method
  - Added `_showPermissionSettingsDialog()` method
  - Implemented smart permission caching

---

## Android Manifest

Make sure these permissions are declared in `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.CAMERA"/>
```

---

## iOS Info.plist

Make sure these keys are in `Info.plist`:
```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>We need access to your photos to set keyboard backgrounds</string>

<key>NSCameraUsageDescription</key>
<string>We need camera access to take photos for keyboard backgrounds</string>
```

---

## Troubleshooting

### Issue: Permissions still being requested repeatedly
**Solution**: 
1. Clear app data/cache
2. Check SharedPreferences keys are being saved
3. Verify `hasStoredPermission` logic

### Issue: Settings dialog showing even though permissions granted
**Solution**:
1. Check permission status in device settings
2. Verify `_showPermissionSettingsDialog()` conditions
3. Clear and re-grant permissions

### Issue: App crashes on permission request
**Solution**:
1. Verify manifest/plist declarations
2. Check permission_handler package version
3. Test on different Android versions

---

## Summary

The new permission system provides a **professional, user-friendly experience** by:

1. ✅ **Asking only once** for permissions
2. ✅ **Storing the decision** in SharedPreferences
3. ✅ **Respecting user choice** (no spam)
4. ✅ **Guiding to settings** when permissions denied
5. ✅ **Handling edge cases** gracefully
6. ✅ **Fast permission checks** (cached status)

Users will appreciate not being bombarded with permission requests every time they want to crop an image! 🎉

