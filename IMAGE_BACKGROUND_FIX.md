# Image Background Upload & Theme Application Fix

## Problem Analysis

The image background feature wasn't working correctly due to several issues:

### 1. **File Path Access Issue**
- **Problem**: Images selected from gallery had paths like `/storage/emulated/0/...` that are not accessible to the keyboard service due to Android's scoped storage
- **Impact**: Custom uploaded images couldn't be displayed as keyboard backgrounds

### 2. **Network URL Handling**
- **Problem**: The Android ThemeManager couldn't load images from HTTP/HTTPS URLs
- **Impact**: Predefined sample images (from Picsum Photos) couldn't be loaded

### 3. **File URI Handling**  
- **Problem**: Some file paths came with `file://` prefix which wasn't being stripped
- **Impact**: File loading would fail with invalid path errors

## Solutions Implemented

### Flutter Side (lib/theme/theme_editor_v2.dart)

#### 1. Added `path_provider` Import
```dart
import 'package:path_provider/path_provider.dart';
```

#### 2. Created `_saveImageForKeyboard()` Method
This method copies the selected image to a shared location accessible by the keyboard service:

```dart
Future<String> _saveImageForKeyboard(File imageFile) async {
  // Get app's external files directory (accessible by keyboard service)
  final directory = await getExternalStorageDirectory();
  
  // Create themes directory
  final themesDir = Directory('${directory.path}/keyboard_themes');
  await themesDir.create(recursive: true);
  
  // Generate unique filename with timestamp
  final timestamp = DateTime.now().millisecondsSinceEpoch;
  final extension = imageFile.path.split('.').last;
  final targetPath = '${themesDir.path}/bg_$timestamp.$extension';
  
  // Copy file to accessible location
  await imageFile.copy(targetPath);
  
  return targetPath; // Returns absolute path: /storage/emulated/0/Android/data/com.example.ai_keyboard/files/keyboard_themes/bg_1234567890.jpg
}
```

**Why This Works:**
- `getExternalStorageDirectory()` returns a path like `/storage/emulated/0/Android/data/com.example.ai_keyboard/files/`
- This directory is accessible to both the Flutter app AND the keyboard service
- The keyboard service can read files from this shared app directory

#### 3. Updated `_uploadCustomImage()` Method
Modified to use the new save method:

```dart
Future<void> _uploadCustomImage() async {
  final ImagePicker picker = ImagePicker();
  final XFile? image = await picker.pickImage(
    source: ImageSource.gallery,
    maxWidth: 1920,
    maxHeight: 1080,
    imageQuality: 90,
  );
  
  if (image != null) {
    // ✅ Copy to accessible location
    final String savedPath = await _saveImageForKeyboard(File(image.path));
    
    // Create theme with accessible path
    final customImageTheme = KeyboardThemeV2.createPictureTheme().copyWith(
      id: 'custom_upload_${DateTime.now().millisecondsSinceEpoch}',
      name: 'Custom Image Theme',
      background: ThemeBackground(
        type: 'image',
        imagePath: savedPath, // ✅ Uses copied file path
        imageOpacity: 0.85,
        ...
      ),
    );
    
    await _applyTheme(customImageTheme);
  }
}
```

### Android Side (android/app/src/main/kotlin/com/example/ai_keyboard/ThemeManager.kt)

#### 1. Added FileOutputStream Import
```kotlin
import java.io.FileOutputStream
```

#### 2. Enhanced `loadImageBitmap()` Method
Added support for multiple image path formats:

```kotlin
private fun loadImageBitmap(path: String): Bitmap {
    return when {
        path.startsWith("http://") || path.startsWith("https://") -> {
            // ✅ Network URL - download and cache
            loadNetworkImage(path)
        }
        path.startsWith("file://") -> {
            // ✅ File URI - remove file:// prefix
            val filePath = path.substring(7)
            val file = File(filePath)
            BitmapFactory.decodeStream(FileInputStream(file))
        }
        path.startsWith("/") -> {
            // ✅ Absolute path (default behavior)
            val file = File(path)
            BitmapFactory.decodeStream(FileInputStream(file))
        }
        else -> {
            // ✅ Asset path
            val inputStream = context.assets.open(path)
            BitmapFactory.decodeStream(inputStream)
        }
    }
}
```

#### 3. Added `loadNetworkImage()` Method
Handles downloading and caching network images:

```kotlin
private fun loadNetworkImage(url: String): Bitmap {
    // Check cache first
    val cacheKey = "net_${url.hashCode()}"
    val cacheDir = context.cacheDir
    val cacheFile = File(cacheDir, cacheKey)
    
    if (cacheFile.exists()) {
        return BitmapFactory.decodeFile(cacheFile.absolutePath)
    }
    
    // Download from network
    val connection = java.net.URL(url).openConnection()
    connection.connectTimeout = 5000
    connection.readTimeout = 10000
    val inputStream = connection.getInputStream()
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    
    // Cache to file for faster loading next time
    try {
        val outputStream = FileOutputStream(cacheFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()
    } catch (e: Exception) {
        // Ignore cache errors
    }
    
    return bitmap
}
```

**Features:**
- Downloads images from HTTP/HTTPS URLs
- Caches downloaded images to device storage
- Uses cached version on subsequent loads
- 5-second connection timeout
- 10-second read timeout
- 90% JPEG compression for cached files

#### 4. Enhanced `buildImageBackground()` Error Handling
Added better logging and fallback:

```kotlin
private fun buildImageBackground(): Drawable {
    val theme = getCurrentTheme()
    val imagePath = theme.background.imagePath
    
    if (imagePath.isNullOrEmpty()) {
        return buildSolidDrawable(theme.background.color ?: Color.BLACK)
    }
    
    val cacheKey = "bg_image_$imagePath"
    return imageCache.get(cacheKey) ?: run {
        try {
            val bitmap = loadImageBitmap(imagePath)
            val drawable = BitmapDrawable(context.resources, bitmap)
            drawable.alpha = (theme.background.imageOpacity * 255).toInt()
            
            imageCache.put(cacheKey, drawable)
            drawable
        } catch (e: Exception) {
            android.util.Log.e("ThemeManager", "Failed to load background image: $imagePath", e)
            // ✅ Fallback to solid color if image fails
            buildSolidDrawable(theme.background.color ?: Color.BLACK)
        }
    }
}
```

## How It Works Now

### For Custom Uploaded Images:
1. User taps "Upload Photo" button
2. Image picker opens, user selects an image
3. Flutter copies image to `/storage/.../Android/data/com.example.ai_keyboard/files/keyboard_themes/`
4. Theme is created with the copied file path
5. Theme is saved to SharedPreferences
6. Keyboard service reads the theme
7. ThemeManager loads the image from the accessible path
8. Image is displayed as keyboard background

### For Predefined Network Images:
1. User taps on a sample image (e.g., Nature image)
2. Theme is created with the network URL (e.g., `https://picsum.photos/300/200?random=1`)
3. Theme is saved to SharedPreferences
4. Keyboard service reads the theme
5. ThemeManager detects it's a network URL
6. `loadNetworkImage()` downloads the image
7. Image is cached to device storage
8. Subsequent loads use the cached version
9. Image is displayed as keyboard background

## Path Examples

### Custom Upload:
```
Original: /storage/emulated/0/DCIM/Camera/IMG_20250117_123456.jpg
Copied to: /storage/emulated/0/Android/data/com.example.ai_keyboard/files/keyboard_themes/bg_1705488896123.jpg
Saved in theme: /storage/emulated/0/Android/data/com.example.ai_keyboard/files/keyboard_themes/bg_1705488896123.jpg
```

### Network Image:
```
Original URL: https://picsum.photos/300/200?random=1
Cached to: /data/data/com.example.ai_keyboard/cache/net_-123456789
Saved in theme: https://picsum.photos/300/200?random=1
```

### Asset Image:
```
Asset path: backgrounds/nature.jpg
Loaded from: assets/backgrounds/nature.jpg
```

## Benefits

1. **Works with Scoped Storage**: Complies with Android's scoped storage requirements
2. **Fast Loading**: Network images are cached, subsequent loads are instant
3. **No Permissions Needed**: Uses app's own storage, no storage permissions required
4. **Robust Error Handling**: Falls back to solid color if image fails to load
5. **Support Multiple Sources**: Handles local files, network URLs, assets, and file URIs
6. **Memory Efficient**: Uses LRU cache to prevent memory leaks
7. **Offline Support**: Once cached, network images work offline

## Testing Checklist

- [x] Upload image from gallery
- [x] Apply predefined network image
- [x] Theme persists after keyboard restart
- [x] Image displays correctly on keyboard
- [x] Network images cache properly
- [x] Cached images load instantly
- [x] Error handling works (invalid URLs, missing files)
- [x] Multiple themes with different images
- [x] Image opacity setting works
- [x] No storage permission prompts

## Known Limitations

1. **Network Requirement**: Initial load of network images requires internet
2. **Cache Size**: Network images are cached indefinitely (no automatic cleanup)
3. **Sync Loading**: Images are loaded synchronously which may cause brief delay for large images
4. **No Progress Indicator**: No visual feedback while downloading network images

## Future Enhancements

1. **Async Image Loading**: Load images in background thread with placeholder
2. **Progress Indicator**: Show loading state for network images  
3. **Cache Management**: Add cache size limits and automatic cleanup
4. **Image Compression**: Optimize large images before saving
5. **Image Preview**: Show preview before applying theme
6. **Multiple Images**: Support multiple background images with transitions
7. **Image Filters**: Apply filters/effects to background images

## Files Modified

1. `lib/theme/theme_editor_v2.dart` - Added image saving and upload logic
2. `android/app/src/main/kotlin/com/example/ai_keyboard/ThemeManager.kt` - Added network and file URI support

## Dependencies

- `path_provider` - Already in pubspec.yaml
- `image_picker` - Already in pubspec.yaml
- No new Android permissions needed

